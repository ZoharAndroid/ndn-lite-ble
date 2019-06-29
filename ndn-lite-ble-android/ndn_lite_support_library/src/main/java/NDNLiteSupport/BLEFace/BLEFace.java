
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.BLEFace;

import android.bluetooth.BluetoothGatt;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;

import NDNLiteSupport.LogHelpers;
import NDNLiteSupport.NDNLiteAndroidSupportConsts;
import NDNLiteSupport.transport.ble.BLECentralTransport;
import NDNLiteSupport.transport.ble.BLEManager;
import NDNLiteSupport.transport.ble.BLEScanner;

import static NDNLiteSupport.transport.ble.BLECentralTransport.MAX_CHARACTERISTIC_VALUE_SIZE;

public class BLEFace extends Face {

    // TAG for logging.
    private static final String TAG = BLEFace.class.getSimpleName();

    /**
     * Create a BLEFace.
     *
     * @param deviceMacAddress The mac address of the device to create a BLE face to.
     * @param onInterest Callback function for when interests are received through this face.
     *                   NOTE: The face, interestFilterId, and filter fields of this callback
     *                   will always be null, -1, and null respectively, since this is a
     *                   very basic face implementation.
     */
    public BLEFace(String deviceMacAddress, OnInterestCallback onInterest) {
        deviceMacAddress_ = deviceMacAddress;
        BLEManager.getInstance().addObserver(bleManagerObserver_);

        interestToOnDataCallback_ = new HashMap<>();

        onInterest_ = onInterest;

        queuedInterestsToSatisfy_ = new PriorityQueue<>();
        queuedInterestsToSend_ = new PriorityQueue<>();

    }

    /**
     * Return a reference to this BLEFace object (this is used to pass a reference to this
     * BLEFace object into the onInterestCallback function).
     */
    private BLEFace getInstance() {
        return this;
    }

    /**
     * Observer for the BLEManager to handle BLE transport related events like receiving data / sending
     * data through a BLE unicast connection with a particular device.
     */
    BLEManager.BLEManagerObserver bleManagerObserver_ = new BLEManager.BLEManagerObserver() {
        @Override
        public void onDeviceEnteredRange(BLEScanner.BluetoothDeviceAndScanRecord bluetoothDeviceAndScanRecord) {
            LogHelpers.LogDebug(TAG,"BLE Face's onDeviceEnteredRange got notice of a secure sign-on device in range.");

        }

        @Override
        public void onDevicesLeftRange(ArrayList<BLEScanner.BluetoothDeviceAndScanRecord> arrayList) {

        }

        @Override
        public void onScanFailed(int i) {

        }

        @Override
        public void onScanCycleEnded() {

        }

        @Override
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, BLECentralTransport.ConnectionState newState,
                                            BLECentralTransport.BCTResultCode bctResultCode) {
            if (bluetoothGatt.getDevice().getAddress().equals(deviceMacAddress_)) {
                if (newState.equals(BLECentralTransport.ConnectionState.STATE_MTU_NEGOTIATED)) {
                    LogHelpers.LogDebug(TAG,"BLEFace got notification that mtu was negotiated for its device of interest, " +
                        "with mac address: " + deviceMacAddress_);

                    for (BLEInterestToSatisfyInfo info : queuedInterestsToSatisfy_) {
                        if (info.data != null) {
                            BLEManager.getInstance().sendData(NDNLiteAndroidSupportConsts.NDN_LITE_BLE_UNICAST_SERVICE_UUID,
                                    deviceMacAddress_, info.data.wireEncode().getImmutableArray(),
                                    NDNLiteAndroidSupportConsts.NDN_LITE_BLE_FACE_SEND_CODE);
                        }
                    }

                    for (BLEInterestToSendInfo info : queuedInterestsToSend_) {
                        BLEManager.getInstance().sendData(NDNLiteAndroidSupportConsts.NDN_LITE_BLE_UNICAST_SERVICE_UUID,
                                deviceMacAddress_, info.interest.wireEncode().getImmutableArray(),
                                NDNLiteAndroidSupportConsts.NDN_LITE_BLE_FACE_SEND_CODE);
                    }
                }
            }
        }

        @Override
        public void onSendDataResult(BluetoothGatt bluetoothGatt, UUID uuid, BLECentralTransport.BCTResultCode resultCode,
                                     long sendCode, byte[] bytes) {
            if (resultCode.equals(BLECentralTransport.BCTResultCode.SUCCESS)) {
                if (sendCode == NDNLiteAndroidSupportConsts.NDN_LITE_BLE_FACE_SEND_CODE) {
                    LogHelpers.LogDebug(TAG,"Send data succeeded for ble face");
                    LogHelpers.LogByteArrayDebug(TAG, "Bytes sent: ", bytes);
                    // extract NDN packet from raw bytes
                    BLEFaceTlvParsingHelpers.ExtractNdnPacketFromByteArrayResult extractResult =
                            BLEFaceTlvParsingHelpers.extractNDNPacketFromByteArray(bytes);
                    if (extractResult == null) {
                        Log.e(TAG, "There was a problem extracting an NDN packet from the bytes sent " +
                                "by BLE face.");
                        return;
                    }

                    switch(extractResult.packetType) {
                        case DATA: {
                            Data sentData = new Data();
                            try {
                                sentData.wireDecode(new Blob(extractResult.bytes));
                            } catch (EncodingException e) {
                                Log.e(TAG, "Failed to decode data sent in BLEFace for device with mac address: " +
                                        deviceMacAddress_);
                                e.printStackTrace();
                                LogHelpers.LogByteArrayDebug(TAG, "Bytes that we failed to decode:", extractResult.bytes);
                                return;
                            }
                            LogHelpers.LogDebug(TAG,"Successfully sent data through BLE face with name: " +
                                    sentData.getName().toUri());

                            LogHelpers.LogDebug(TAG,"Iterating through queued interests to satisfy, to find the interest" +
                                    " with data corresponding to the data we just sent.");
                            for (BLEInterestToSatisfyInfo info: queuedInterestsToSatisfy_) {
                                LogHelpers.LogDebug(TAG,"Name of current queued interest to satisfy data: " +
                                        info.data.getName().toUri());
                                if (info.data.getName().equals(sentData.getName())) {
                                    LogHelpers.LogDebug(TAG,"Found data that was sent in queuedInterestsToSatisfy_, removing it.");
                                    queuedInterestsToSatisfy_.remove(info);
                                }
                            }
                            break;
                        }
                        case INTEREST: {
                            Interest sentInterest = new Interest();
                            try {
                                sentInterest.wireDecode(new Blob(extractResult.bytes));
                            } catch (EncodingException e) {
                                Log.e(TAG, "Failed to decode interest sent in BLEFace for device with mac address: " +
                                        deviceMacAddress_);
                                e.printStackTrace();
                                LogHelpers.LogByteArrayDebug(TAG, "Bytes that we failed to decode:", extractResult.bytes);
                                return;
                            }
                            LogHelpers.LogDebug(TAG,"Successfully sent interest through BLE face with name: " +
                                    sentInterest.getName().toUri());

                            for (BLEInterestToSendInfo info : queuedInterestsToSend_) {
                                if (info.interest.getName().equals(sentInterest.getName())) {
                                    LogHelpers.LogDebug(TAG,"Found interest that was sent in queuedInterestsToSend_, removing it.");
                                    queuedInterestsToSend_.remove(info);
                                }
                            }
                            break;
                        }
                    }
                }
            }
            else {
                Log.e(TAG, "BLEFace onSendDataResult returned failure for send code " +
                        sendCode + ", error code: " + resultCode);

                if (resultCode.equals(BLECentralTransport.BCTResultCode.ATTEMPTED_TO_SEND_DATA_LARGER_THAN_MAX_PAYLOAD_SIZE_IN_ONE_CHARACTERISTIC_WRITE)) {
                    BLEManager.getInstance().requestMtu(bluetoothGatt.getDevice().getAddress(), MAX_CHARACTERISTIC_VALUE_SIZE);
                }
            }
        }

        @Override
        public void onReceivedDataResult(BluetoothGatt bluetoothGatt, UUID uuid, BLECentralTransport.BCTResultCode resultCode,
                                         byte[] bytes_raw) {
            if (resultCode.equals(BLECentralTransport.BCTResultCode.SUCCESS)) {

                // extract NDN packet from raw bytes, since we may receive extra bytes from the notification
                BLEFaceTlvParsingHelpers.ExtractNdnPacketFromByteArrayResult extractResult =
                        BLEFaceTlvParsingHelpers.extractNDNPacketFromByteArray(bytes_raw);
                if (extractResult == null) {
                    Log.e(TAG, "There was a problem extracting an NDN packet from the bytes received by BLE face.");
                    return;
                }

                switch(extractResult.packetType) {
                    case DATA: {
                        Data receivedData = new Data();
                        try {
                            receivedData.wireDecode(new Blob(extractResult.bytes));
                        } catch (EncodingException e) {
                            Log.e(TAG, "Failed to decode data received in BLEFace for device with mac address: " +
                                    deviceMacAddress_);
                            LogHelpers.LogByteArrayDebug(TAG, "Bytes that we failed to decode:", extractResult.bytes);
                            e.printStackTrace();
                            return;
                        }
                        for (Interest interest : interestToOnDataCallback_.keySet()) {
                            if (receivedData.getName().toString().contains(interest.getName().toString())) {
                                LogHelpers.LogDebug(TAG,"Found an interest name which was contained in received data name, calling its" +
                                        " on data callback.");
                                interestToOnDataCallback_.get(interest).onData(interest, receivedData);
                            }
                        }
                    }
                    case INTEREST: {
                        Interest receivedInterest = new Interest();
                        try {
                            receivedInterest.wireDecode(new Blob(extractResult.bytes));
                        } catch (EncodingException e) {
                            Log.e(TAG, "Failed to decode interest received in BLEFace for device with mac address: " +
                                    deviceMacAddress_);
                            LogHelpers.LogByteArrayDebug(TAG, "Bytes that we failed to decode:", extractResult.bytes);
                            e.printStackTrace();
                            //return;
                        }
                        LogHelpers.LogDebug(TAG,"Got interest through BLE face with name: " + receivedInterest.getName());

                        queuedInterestsToSatisfy_.add(new BLEInterestToSatisfyInfo(
                                receivedInterest,
                                deviceMacAddress_
                        ));

                        onInterest_.onInterest(receivedInterest.getName(), receivedInterest,
                                getInstance(), -1, null);
                    }
                }
            }
            else {
                Log.e(TAG, "BLEFace onReceivedDataResult returned failure, error code: " + resultCode);
            }
        }
    };

    /**
     * Function to express an interest through the BLE face. As of now, there is no onTimeout
     * or onNack callback, as this is a very basic face implementation.
     *
     * @param interest Interest to express.
     * @param onData Callback for when data is received.
     */
    @Override
    public long expressInterest(Interest interest, OnData onData) {

        byte[] interestBytes = interest.wireEncode().getImmutableArray();
        LogHelpers.LogDebug(TAG,"Hex string of bytes of interest sent through ble face: " + new Blob(interestBytes).toHex());

        // this means there isn't a connection to the device we can send through yet
        if (!BLEManager.getInstance().isConnected(deviceMacAddress_)) {
            return BLEFaceConsts.BLE_FACE_FAILURE_RETURN_CODE;
        }

        BLEManager.getInstance().sendData(NDNLiteAndroidSupportConsts.NDN_LITE_BLE_UNICAST_SERVICE_UUID, deviceMacAddress_,
                interestBytes, NDNLiteAndroidSupportConsts.NDN_LITE_BLE_FACE_SEND_CODE);

        interestToOnDataCallback_.put(interest, onData);

        return BLEFaceConsts.BLE_FACE_SUCCESS_RETURN_CODE;

    }

    /**
     * Function to put data.
     *
     * @param data Data to send through face.
     */
    @Override
    public void putData(Data data) throws IOException {
        if (data.wireEncode().getImmutableArray().length > BLEFaceConsts.BLE_FACE_MAX_PACKET_SIZE)
            throw new IOException("Data exceeded BLE_FACE_MAX_PACKET_SIZE, " + BLEFaceConsts.BLE_FACE_MAX_PACKET_SIZE);

        for (BLEInterestToSatisfyInfo info : queuedInterestsToSatisfy_) {
            if (info.interest.getName().isPrefixOf(data.getName())) {
                LogHelpers.LogDebug(TAG, "in putData, found a pending interest for" +
                        " data of name: " + data.getName().toUri());
                info.data = data;
                BLEManager.getInstance().sendData(NDNLiteAndroidSupportConsts.NDN_LITE_BLE_UNICAST_SERVICE_UUID, info.deviceMacAddress,
                        data.wireEncode().getImmutableArray(), NDNLiteAndroidSupportConsts.NDN_LITE_BLE_FACE_SEND_CODE);
                return;
            }
        }

        LogHelpers.LogDebug(TAG, "Failed to find a pending interest for data of name : " +
            data.getName().toUri());
    }

    // mac address to which this face will send packets
    String deviceMacAddress_;
    // on interest callback to use when an interest is received
    OnInterestCallback onInterest_;
    // mapping of the interest sent through this face to its corresponding OnData callback
    HashMap<Interest, OnData> interestToOnDataCallback_;
    // queue of interests to send when a connection is available
    Queue<BLEInterestToSendInfo> queuedInterestsToSend_;
    // queue of interests to be satisfied, essentially a basic pending interest table
    Queue<BLEInterestToSatisfyInfo> queuedInterestsToSatisfy_;

}
