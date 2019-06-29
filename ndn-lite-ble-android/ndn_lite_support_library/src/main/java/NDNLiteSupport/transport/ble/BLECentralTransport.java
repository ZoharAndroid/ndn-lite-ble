
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.transport.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import NDNLiteSupport.LogHelpers;
import NDNLiteSupport.transport.ble.detail.BLEUtils;

import static NDNLiteSupport.transport.ble.BLECentralTransport.SendBulkDataState.CurrentPhase.NOT_SENDING;
import static NDNLiteSupport.transport.ble.BLECentralTransport.SendBulkDataState.CurrentPhase.SENDING_BULK_DATA_PAYLOAD;
import static NDNLiteSupport.transport.ble.BLECentralTransport.SendBulkDataState.CurrentPhase.SENDING_BULK_DATA_PAYLOAD_HASH;
import static NDNLiteSupport.transport.ble.BLECentralTransport.SendBulkDataState.CurrentPhase.SENDING_BULK_DATA_PAYLOAD_LENGTH;
import static NDNLiteSupport.transport.ble.BLECentralTransport.SendBulkDataState.CurrentPhase.SENDING_INITIAL_SIGNAL;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGatt.STATE_CONNECTED;
import static android.bluetooth.BluetoothGatt.STATE_DISCONNECTED;

/*
 * Adapted from:
 * https://github.com/googlesamples/android-BluetoothLeGatt/blob/master/Application/src/main/java/com/example/android/bluetoothlegatt/BluetoothLeService.java
 */
public class BLECentralTransport {

    private final String TAG = BLECentralTransport.class.getSimpleName();

    Handler handler_;

    public final static String CLIENT_CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    // see https://stackoverflow.com/questions/38741928/what-is-the-length-limit-for-ble-characteristics
    public final static int MAX_CHARACTERISTIC_VALUE_SIZE = 512;

    public final static String BLE_DATA_TRANSFER_CHARACTERISTIC_UUID = "0e1524fd-760f-439a-a15d-a2cad8973d15";

    public final static int MAX_MTU_NEGOTIATION_WAIT_TIME = 5000;

    public final static byte[] INITIAL_SIGNAL = new byte[] {'~', '~', '~', '~', '~'};

    private Context ctx_;

    private String remoteAddress_;

    private BluetoothManager bluetoothManager_;
    private BluetoothAdapter bluetoothAdapter_;

    private BluetoothGatt gatt_;
    // when we first connect to the device, we iterate through all services on the device to check to see if
    // they have a data transfer characteristic, and enable notifications on that characteristic if we find it;
    // this variable keeps track of which index of dataTransferContainingServiceUuids_ we are currently enabling notifications for
    private int dataTransferServicesEnableNotificationsIndex_ = 0;
    private ArrayList<UUID> dataTransferContainingServiceUuids_;
    // mapping of service uuid's to data transfer characteristics found on the device
    private HashMap<UUID, BluetoothGattCharacteristic> dataTransferCharacteristics_;

    // only one sendSingleByteArrayFragment and one read are allowed at a time (a sendSingleByteArrayFragment and read can happen at the
    // same time, however)
    SendBulkDataState sendBulkDataState_;

    // keep track of what our current connection state is
    ConnectionState currentConnectionState_;

    // before we do any MTU negotiation; max payload size is MTU - 3, to account for headers
    // inserted by the BLE stack
    private final int DEFAULT_MTU = 23;
    private int currentMTU_ = DEFAULT_MTU;
    private int currentMaxPayloadSize_;

    private int getCurrentMaxPayloadSize() {
        // subtract 3 to account for header bytes inserted by BLE stack
        return currentMTU_ - 3;
    }

    public static enum BCTResultCode {
        SUCCESS, // 0

        BLUETOOTH_ADAPTER_NOT_INITIALIZED,
        ADDRESS_NULL,
        GATT_OBJECT_WAS_NULL,
        NO_ACTIVE_CONNECTION,

        SERVICE_DISCOVERY_FAILED,
        MTU_NEGOTATIATION_FAILURE,

        CONNECTION_STATE_CHANGE_ERROR,

        NO_NOTIFY_PROPERTY_ON_CHARACTERISTIC,
        FAILED_TO_ENABLE_NOTIFICATIONS_ON_CHARACTERISTIC,
        DESCRIPTOR_RETRIEVAL_FAILED,

        SERVICE_RETRIEVAL_FAILED,
        CHARACTERISTIC_RETRIEVAL_FAILED,

        SEND_CODE_MUST_BE_POSITIVE,
        EXISTING_SEND_IN_PROGRESS,
        NO_WRITE_PROPERTY_ON_CHARACTERISTIC,
        BULK_DATA_SERVICE_NOT_FOUND,
        BULK_DATA_CHARACTERISTIC_NOT_FOUND,
        PROBLEM_GENERATING_MESSAGE_HASH,

        FAILED_TO_WRITE_INITIAL_SIGNAL,
        FAILED_TO_WRITE_BYTE_ARRAY_HASH,
        FAILED_TO_WRITE_BYTE_ARRAY_LENGTH,
        FAILED_TO_WRITE_BYTE_ARRAY,

        FAILED_TO_WRITE_SINGLE_TRANSMISSION_UNIT_PAYLOAD,

        ATTEMPTED_TO_SEND_DATA_LARGER_THAN_MAX_PAYLOAD_SIZE_IN_ONE_CHARACTERISTIC_WRITE,

        // THIS SHOULD NOT BE HERE, THIS IS A RESULT CODE SPECIFIC TO THE BLE MANAGER
        // BUT I JUST PUT IT IN THE BLECentralTransport RESULT CODES AS A QUICK HACK
        NO_TRANSPORT_FOR_DEVICE

    }

    public static enum ConnectionState {
        STATE_CONNECTED,
        STATE_DISCOVERED_SERVICES,
        STATE_NOTIFICATIONS_ENABLED,
        STATE_MTU_NEGOTIATED,
        STATE_DISCONNECTED,
    }

    public interface BLECentralTransportCallbacks {

        void onConnectionStateChange(BluetoothGatt gatt, ConnectionState newState,
                                     BCTResultCode resultCode);

        void onSendDataResult(BluetoothGatt gatt, UUID serviceUuid, BCTResultCode resultCode, long sendCode, byte[] dataSent);

        void onReceivedDataResult(BluetoothGatt gatt, UUID serviceUuid, BCTResultCode resultCode, byte[] data);

    }

    BLECentralTransportCallbacks bleCentralTransportCallbacks_;

    protected static class SendBulkDataState {

        boolean sendingBulkData = false;

        public enum CurrentPhase {
            NOT_SENDING,
            SENDING_INITIAL_SIGNAL,
            SENDING_BULK_DATA_PAYLOAD_HASH,
            SENDING_BULK_DATA_PAYLOAD_LENGTH,
            SENDING_BULK_DATA_PAYLOAD,
        }

        CurrentPhase currentPhase = NOT_SENDING;
        // the sending code passed in through the sendData function call
        private long currentSendingCode = -1;

        byte[] bulkDataPayloadHash = {};
        byte[] bulkDataPayload = {};

        // holds the state for whatever byte array is currently being sent
        // (could be the byte array hash
        SendingByteArrayState sendingByteArrayState = new SendingByteArrayState();

    }

    protected static class SendingByteArrayState {

        // if the byte array was larger than the max packet size, this will be true
        boolean sendingOutFragmentsMode = false;
        // is updated to reflect what fragment is being sent during the sending process
        int fragmentOffset = 0;

        // the byte array currently being sent; this is incrementally deleted as data is
        // sent, and it being null indicates that sending is finished
        byte[] currentSendingByteArray = {};

    }

    public BLECentralTransport(Context ctx, BluetoothManager bluetoothManager,
                               BluetoothAdapter bluetoothAdapter, String remoteAddress,
                               BLECentralTransportCallbacks bleCentralTransportCallbacks) {

        handler_ = new Handler();

        ctx_ = ctx;

        bleCentralTransportCallbacks_ = bleCentralTransportCallbacks;

        bluetoothManager_ = bluetoothManager;
        bluetoothAdapter_ = bluetoothAdapter;
        remoteAddress_ = remoteAddress;
        currentMaxPayloadSize_ = getCurrentMaxPayloadSize();

        sendBulkDataState_ = new SendBulkDataState();

        dataTransferCharacteristics_ = new HashMap<>();
        dataTransferContainingServiceUuids_ = new ArrayList<>();

        currentConnectionState_ = ConnectionState.STATE_DISCONNECTED;

    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback gattCallback_ = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == GATT_SUCCESS) {
                switch (newState) {
                    case STATE_CONNECTED:
                        gatt_ = gatt;
                        gatt_.discoverServices();
                        returnConnectionStateChange(gatt, ConnectionState.STATE_CONNECTED,
                                BCTResultCode.SUCCESS);
                        currentConnectionState_ = ConnectionState.STATE_CONNECTED;
                        break;
                    case STATE_DISCONNECTED:
                        gatt_.close();
                        gatt.close();
                        returnConnectionStateChange(gatt, ConnectionState.STATE_DISCONNECTED,
                                BCTResultCode.SUCCESS);
                        currentConnectionState_ = ConnectionState.STATE_DISCONNECTED;
                        gatt_ = null;
                        break;
                    default:
                        break;
                }
            } else {
                Log.e(TAG, "Error in onConnectionStateChange: " + status);
                switch (newState) {
                    case STATE_CONNECTED:
                        gatt_ = gatt;
                        gatt.discoverServices();
                        returnConnectionStateChange(gatt, ConnectionState.STATE_CONNECTED,
                                BCTResultCode.CONNECTION_STATE_CHANGE_ERROR);
                        currentConnectionState_ = ConnectionState.STATE_CONNECTED;
                        break;
                    case STATE_DISCONNECTED:
                        gatt_.close();
                        gatt.close();
                        returnConnectionStateChange(gatt, ConnectionState.STATE_DISCONNECTED,
                                BCTResultCode.CONNECTION_STATE_CHANGE_ERROR);
                        currentConnectionState_ = ConnectionState.STATE_DISCONNECTED;
                        gatt_ = null;
                        break;
                    default:
                        break;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == GATT_SUCCESS) {

                returnConnectionStateChange(gatt, ConnectionState.STATE_DISCOVERED_SERVICES, BCTResultCode.SUCCESS);
                currentConnectionState_ = ConnectionState.STATE_DISCOVERED_SERVICES;

//                getDataTransferCharacteristicResult getDataTransferCharacteristicResult = getDataTransferCharacteristic(gatt_);
//                if (getDataTransferCharacteristicResult.resultCode != BCTResultCode.SUCCESS) {
//                    //returnConnectResult(getDataTransferCharacteristicResult.resultCode);
//                    return;
//                }
//
//                BluetoothGattCharacteristic characteristic = getDataTransferCharacteristicResult.characteristic;
//
//                BCTResultCode setCharacteristicNotificationResultCode =
//                        setCharacteristicNotification(characteristic, true);
//
//                if (setCharacteristicNotificationResultCode != BCTResultCode.SUCCESS) {
//                    //returnConnectResult(setCharacteristicNotificationResultCode);
//                    return;
//                }

                // get all of the dataTransfer characteristics on the device
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    BluetoothGattCharacteristic dataTransferCharacteristic =
                            service.getCharacteristic(UUID.fromString(BLE_DATA_TRANSFER_CHARACTERISTIC_UUID));
                    if (dataTransferCharacteristic == null)
                        continue;
                    else {
                        LogHelpers.LogDebug(TAG, "Found a data transfer characteristic in service with uuid: " + service.getUuid());
                        dataTransferContainingServiceUuids_.add(service.getUuid());
                        dataTransferCharacteristics_.put(service.getUuid(), dataTransferCharacteristic);
                    }
                }

                // set off cycle of enabling all data transfer characteristic notifications until
                // dataTransferCharacteristics_ is empty (after notifications are enabled, the characteristic
                // is removed from the data structure); it cannot be done in a simple for loop because
                // gatt operations will fail if not done serially
                setCharacteristicNotification(dataTransferCharacteristics_.get(
                        dataTransferContainingServiceUuids_.get(dataTransferServicesEnableNotificationsIndex_)
                ), true);


            } else {
                Log.e(TAG, "Error in onServicesDiscovered: " + status);
                //returnConnectResult(BCTResultCode.SERVICE_DISCOVERY_FAILED);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            if (status == GATT_SUCCESS) {
                LogHelpers.LogDebug(TAG, "Successfully negotiated mtu: " + mtu);
                if (mtu < MAX_CHARACTERISTIC_VALUE_SIZE) {
                    Log.e(TAG, "Failed to negotiate max characteristic value size mtu of" +
                            " " + MAX_CHARACTERISTIC_VALUE_SIZE + ".");
                    disconnect();
                    return;
                }
                currentMTU_ = mtu;
                cancelMtuNegotiationTimer();

                returnConnectionStateChange(gatt, ConnectionState.STATE_MTU_NEGOTIATED, BCTResultCode.SUCCESS);
                currentConnectionState_ = ConnectionState.STATE_MTU_NEGOTIATED;

                gatt_ = gatt;

                //returnConnectResult(BCTResultCode.SUCCESS);

            } else {
                Log.e(TAG, "Error in onMtuChanged: " + status);
                cancelMtuNegotiationTimer();
                //returnConnectResult(BCTResultCode.MTU_NEGOTATIATION_FAILURE);
                disconnect();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            LogHelpers.LogDebug(TAG, "Got a notification that a characteristic changed.");
            // if the BLE bulk data transfer protocol is finished, then this code should involve the checking
            // of some kind of "receiving packet" state; for now, to keep things simpler, all notifications are considered
            // data to be received in full
            if (characteristic.getUuid().equals(UUID.fromString(BLE_DATA_TRANSFER_CHARACTERISTIC_UUID))) {
                LogHelpers.LogDebug(TAG, "Got notification of change in data transfer characteristic.");
                returnReceivedDataResult(gatt, characteristic.getService().getUuid(),
                        BCTResultCode.SUCCESS, characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == GATT_SUCCESS) {

            } else {
                Log.e(TAG, "Error in onCharacteristicRead: " + status);
            }
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            String deviceAddress = gatt.getDevice().getAddress();

            LogHelpers.LogDebug(TAG, "Entered on characteristic write.");

            if (status == GATT_SUCCESS) {

                if (sendBulkDataState_.sendingBulkData) {
                    if (sendBulkDataState_.sendingByteArrayState.sendingOutFragmentsMode) {
                        if (sendBulkDataState_.sendingByteArrayState.currentSendingByteArray != null) {
                            LogHelpers.LogDebug(TAG, "Current byte array length: " +
                                    sendBulkDataState_.sendingByteArrayState.currentSendingByteArray.length);

                            LogHelpers.LogDebug(TAG, "Current values in byte array: ");
                            for (int i = 0; i < sendBulkDataState_.sendingByteArrayState.currentSendingByteArray.length; i++) {
                                LogHelpers.LogDebug(TAG, Integer.toString(Byte.toUnsignedInt(
                                        sendBulkDataState_.sendingByteArrayState.currentSendingByteArray[i]))
                                );
                            }

                            LogHelpers.LogDebug(TAG, "Calling finishSendingByteArrayOverBLE.");
                            finishSendingByteArrayOverBLE(characteristic.getService().getUuid(), characteristic);

                        } else {
                            handleSendBulkDataTransition(characteristic.getService().getUuid(), sendBulkDataState_.bulkDataPayload);
                        }
                    } else {
                        handleSendBulkDataTransition(characteristic.getService().getUuid(), sendBulkDataState_.bulkDataPayload);
                    }
                } else {
                    LogHelpers.LogDebug(TAG, "Sending bulk data mode was false for a successful characteristic write, meaning that" +
                            " we succeeded in sending an unfragmented piece of data.");

                    returnSendResultAndResetSendBulkDataState(BCTResultCode.SUCCESS, characteristic.getService().getUuid(),
                            sendBulkDataState_.currentSendingCode, characteristic.getValue());

                }
            } else {
                Log.w(TAG, "We got a callback that we failed to write a characteristic.");
                Log.w(TAG, "Error code for characteristic write failure: " + status);
                if (sendBulkDataState_.sendingBulkData) {
                    handleSendBulkDataFailure(characteristic.getService().getUuid());
                }
                else {
                    returnSendResultAndResetSendBulkDataState(BCTResultCode.FAILED_TO_WRITE_SINGLE_TRANSMISSION_UNIT_PAYLOAD,
                            characteristic.getService().getUuid(), sendBulkDataState_.currentSendingCode, null);
                }
            }

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == GATT_SUCCESS) {

            } else {
                Log.e(TAG, "Error in onDescriptorRead: " + status);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == GATT_SUCCESS) {
                LogHelpers.LogDebug(TAG, "Successfully wrote a descriptor.");

                // we assume that the first descriptor write to the device is to enable notifications,
                // and we always enable notifications before negotiating MTU
                if (currentConnectionState_.equals(ConnectionState.STATE_DISCOVERED_SERVICES) &&
                        descriptor.getUuid().equals(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG_UUID))) {

                    // increment by one so that we enable notifications for the next data transfer characteristic
                    // that was found on the device
                    dataTransferServicesEnableNotificationsIndex_++;

                    // if there are still data transfer characteristics in the dataTransferCharacteristics_ arraylist,
                    // we are done enabling all data transfer characteristic notifications and can request mtu change
                    if (dataTransferServicesEnableNotificationsIndex_ != dataTransferContainingServiceUuids_.size()) {
                        setCharacteristicNotification(
                                dataTransferCharacteristics_.get(dataTransferServicesEnableNotificationsIndex_),
                                true);
                    }

                    LogHelpers.LogDebug(TAG, "Successfully enabled notifications for all data transfer characteristics on device.");
                    returnConnectionStateChange(gatt, ConnectionState.STATE_NOTIFICATIONS_ENABLED, BCTResultCode.SUCCESS);
                    currentConnectionState_ = ConnectionState.STATE_NOTIFICATIONS_ENABLED;
                    // try to negotiate the maximum characteristic value size to increase throughput
                    LogHelpers.LogDebug(TAG, "Attempting to request mtu size of " + MAX_CHARACTERISTIC_VALUE_SIZE);
                    gatt_.requestMtu(MAX_CHARACTERISTIC_VALUE_SIZE);
                    startMtuNegotiationTimer();
                }

            } else {
                Log.e(TAG, "Error in onDescriptorWrite: " + status);

                // if a descriptor write fails and dataTransferCharacteristics_ size is not equal to zero, it means
                // that enabling notifications for a data transfer characteristic failed; the assumption here is that
                // the first descriptor writes are all to enable notifications, and no other descriptor writes occur
                // until after all data transfer characteristic notifications on the device have been enabled successfully
                if (dataTransferCharacteristics_.size() != 0) {
                    Log.e(TAG, "Enabling a data transfer characteristic notification failed.");
                    Log.e(TAG, "Service uuid of failed data transfer characteristic notification enabling: " +
                        descriptor.getCharacteristic().getService().getUuid());

                }

            }
        }


    };

    private BCTResultCode setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                        boolean enabled) {

        if (bluetoothAdapter_ == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return BCTResultCode.BLUETOOTH_ADAPTER_NOT_INITIALIZED;
        }

        if (!BLEUtils.checkForCharacteristicProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
            LogHelpers.LogDebug(TAG, "no notify property for characteristic to set notification");
            return BCTResultCode.NO_NOTIFY_PROPERTY_ON_CHARACTERISTIC;
        }

        boolean writeSuccess = gatt_.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG_UUID));
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean descriptorWriteSuccess = gatt_.writeDescriptor(descriptor);

            if (writeSuccess && descriptorWriteSuccess) {
                LogHelpers.LogDebug(TAG, "setting notification was successful");
            } else {
                return BCTResultCode.FAILED_TO_ENABLE_NOTIFICATIONS_ON_CHARACTERISTIC;
            }
        }

        return BCTResultCode.SUCCESS;
    }

    // kind of overkill, but see https://stackoverflow.com/questions/356248/best-way-to-return-status-flag-and-message-from-a-method-in-java
    private class getDataTransferCharacteristicResult {

        BluetoothGattCharacteristic characteristic;
        BCTResultCode resultCode;

        getDataTransferCharacteristicResult(BluetoothGattCharacteristic characteristic, BCTResultCode resultCode) {
            this.characteristic = characteristic;
            this.resultCode = resultCode;
        }

        getDataTransferCharacteristicResult(BCTResultCode resultCode) {
            this.resultCode = resultCode;
        }
    }

    private getDataTransferCharacteristicResult getDataTransferCharacteristic(String serviceUuid, BluetoothGatt gattObject) {

        BLEUtils.GetCharacteristicResult result = BLEUtils.getCharacteristic(gattObject,
                serviceUuid, BLE_DATA_TRANSFER_CHARACTERISTIC_UUID);

        if (result.resultCode.equals(BLEUtils.BUResultCode.SERVICE_RETRIEVAL_FAILED)) {
            return new getDataTransferCharacteristicResult(BCTResultCode.BULK_DATA_SERVICE_NOT_FOUND);
        } else if (result.resultCode.equals(BLEUtils.BUResultCode.CHARACTERISTIC_RETRIEVAL_FAILED)) {
            return new getDataTransferCharacteristicResult(BCTResultCode.BULK_DATA_CHARACTERISTIC_NOT_FOUND);
        }

        return new getDataTransferCharacteristicResult(result.characteristic, BCTResultCode.SUCCESS);

    }

    // functions for sending byte arrays over BLE using the bulk data transfer characteristic defined by us

    //**//

    private void sendInitialSignal(UUID serviceUuid) {

        LogHelpers.LogDebug(TAG, "Sending initial signal: " + INITIAL_SIGNAL.toString());

        BluetoothGattCharacteristic dataTransferCharacteristic = dataTransferCharacteristics_.get(serviceUuid);

        sendBulkDataState_.currentPhase = SENDING_INITIAL_SIGNAL;
        startSendingByteArrayOverBLE(serviceUuid, dataTransferCharacteristic, INITIAL_SIGNAL);

    }

    private void sendBulkDataPayloadHash(UUID serviceUuid) {

        LogHelpers.LogDebug(TAG, "Sending bulk data payload hash: " + sendBulkDataState_.bulkDataPayloadHash.toString());

        BluetoothGattCharacteristic dataTransferCharacteristic = dataTransferCharacteristics_.get(serviceUuid);

        sendBulkDataState_.currentPhase = SENDING_BULK_DATA_PAYLOAD_HASH;
        resetSendingByteArrayState();
        startSendingByteArrayOverBLE(serviceUuid, dataTransferCharacteristic,
                sendBulkDataState_.bulkDataPayloadHash);

    }

    private void sendBulkDataPayloadLength(UUID serviceUuid) {

        short dataShort = (short) (sendBulkDataState_.bulkDataPayload.length & 0xffff);

        byte[] length = {(byte) (dataShort >> 8), (byte) (dataShort & 0xff)};

        LogHelpers.LogDebug(TAG, "Sending bulk data payload length: " + Short.toString(dataShort));

        BluetoothGattCharacteristic dataTransferCharacteristic = dataTransferCharacteristics_.get(serviceUuid);

        sendBulkDataState_.currentPhase = SENDING_BULK_DATA_PAYLOAD_LENGTH;
        resetSendingByteArrayState();
        startSendingByteArrayOverBLE(serviceUuid, dataTransferCharacteristic, length);
    }

    private void sendBulkDataPayload(UUID serviceUuid) {

        LogHelpers.LogDebug(TAG, "Sending bulk data payload.");

        BluetoothGattCharacteristic dataTransferCharacteristic = dataTransferCharacteristics_.get(serviceUuid);

        sendBulkDataState_.currentPhase = SENDING_BULK_DATA_PAYLOAD;
        resetSendingByteArrayState();
        startSendingByteArrayOverBLE(serviceUuid, dataTransferCharacteristic,
                sendBulkDataState_.bulkDataPayload);

    }

    //**//

    // more general functions for sending byte arrays over BLE, used by the bulk data transfer protocol implementation

    //**//

    // this function sets off the series of finishSendingByteArrayOverBLE calls to send the payload of a byte array,
    // after the byte array's length is sent
    private void startSendingByteArrayOverBLE(UUID serviceUuid, BluetoothGattCharacteristic characteristic, byte[] byteArray) {

        LogHelpers.LogDebug(TAG, "Send byte array over BLE called.");

        if (bluetoothAdapter_ == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.BLUETOOTH_ADAPTER_NOT_INITIALIZED,
                    serviceUuid, sendBulkDataState_.currentSendingCode, null
            );
            return;
        }

        if (gatt_ == null || bluetoothManager_.getConnectionState(gatt_.getDevice(),
                BluetoothProfile.GATT) != STATE_CONNECTED) {
            Log.w(TAG, "No active connection to device.");
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.NO_ACTIVE_CONNECTION,
                    serviceUuid, sendBulkDataState_.currentSendingCode, null
            );
            return;
        }

        int currentMaxPayloadSize = getCurrentMaxPayloadSize();

        if (byteArray.length <= currentMaxPayloadSize) {
            sendSingleByteArrayFragment(serviceUuid, characteristic, byteArray);
            return;
        }

        LogHelpers.LogDebug(TAG, "Got a request to sendSingleByteArrayFragment out an array larger than the max packet size; starting fragmentation process.");

        sendBulkDataState_.sendingByteArrayState.sendingOutFragmentsMode = true;
        sendBulkDataState_.sendingByteArrayState.fragmentOffset++;

        sendBulkDataState_.sendingByteArrayState.currentSendingByteArray = Arrays.copyOfRange(byteArray,
                currentMaxPayloadSize, byteArray.length);

        byte[] arrayToSend = Arrays.copyOfRange(byteArray, 0, currentMaxPayloadSize);

        sendSingleByteArrayFragment(serviceUuid, characteristic, arrayToSend);

    }

    // this function is repeatedly called within BLEService to finish sending out a BLE packet; it is triggered by the
    // asynchronous callbacks in the gattCallback_ object
    private void finishSendingByteArrayOverBLE(UUID serviceUuid, BluetoothGattCharacteristic characteristic) {

        LogHelpers.LogDebug(TAG, "Entered the finish sending byte array over BLE function.");

        if (bluetoothAdapter_ == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.BLUETOOTH_ADAPTER_NOT_INITIALIZED,
                    serviceUuid, sendBulkDataState_.currentSendingCode, null
            );
            return;
        }

        if (gatt_ == null || bluetoothManager_.getConnectionState(gatt_.getDevice(),
                BluetoothProfile.GATT) != STATE_CONNECTED) {
            Log.w(TAG, "No active connection to device.");
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.NO_ACTIVE_CONNECTION,
                    serviceUuid, sendBulkDataState_.currentSendingCode, null
            );
            return;
        }

        int currentMaxPayloadSize = getCurrentMaxPayloadSize();

        if (sendBulkDataState_.sendingByteArrayState.currentSendingByteArray.length >= currentMaxPayloadSize) {

            LogHelpers.LogDebug(TAG, "Sending a middle fragment with offset: " + sendBulkDataState_.sendingByteArrayState.fragmentOffset);

            sendBulkDataState_.sendingByteArrayState.fragmentOffset++;

            byte[] arrayToSend = Arrays.copyOfRange(sendBulkDataState_.sendingByteArrayState.currentSendingByteArray,
                    0, currentMaxPayloadSize);

            LogHelpers.LogDebug(TAG, "Values of this middle fragment: ");
            for (int i = 0; i < arrayToSend.length; i++) {
                LogHelpers.LogDebug(TAG, Integer.toString(Byte.toUnsignedInt(arrayToSend[i])));
            }

            sendSingleByteArrayFragment(serviceUuid, characteristic, arrayToSend);

            byte[] tempCopyRemainder = Arrays.copyOfRange(sendBulkDataState_.sendingByteArrayState.currentSendingByteArray,
                    currentMaxPayloadSize,
                    sendBulkDataState_.sendingByteArrayState.currentSendingByteArray.length);
            sendBulkDataState_.sendingByteArrayState.currentSendingByteArray =
                    new byte[sendBulkDataState_.sendingByteArrayState.currentSendingByteArray.length - currentMaxPayloadSize];
            sendBulkDataState_.sendingByteArrayState.currentSendingByteArray =
                    Arrays.copyOfRange(tempCopyRemainder, 0, tempCopyRemainder.length);
        } else if (sendBulkDataState_.sendingByteArrayState.currentSendingByteArray.length > 0) {

            LogHelpers.LogDebug(TAG, "Sending an end fragment with offset:" + sendBulkDataState_.sendingByteArrayState.fragmentOffset);

            sendBulkDataState_.sendingByteArrayState.fragmentOffset++;

            byte[] arrayToSend = Arrays.copyOfRange(sendBulkDataState_.sendingByteArrayState.currentSendingByteArray,
                    0,
                    sendBulkDataState_.sendingByteArrayState.currentSendingByteArray.length);

            LogHelpers.LogDebug(TAG, "Values in end fragment: ");
            for (int i = 0; i < arrayToSend.length; i++) {
                LogHelpers.LogDebug(TAG, Integer.toString(Byte.toUnsignedInt(arrayToSend[i])));
            }

            sendSingleByteArrayFragment(serviceUuid, characteristic, arrayToSend);

            LogHelpers.LogDebug(TAG, "Setting sendingOutFragmentsMode to false.");
            sendBulkDataState_.sendingByteArrayState.sendingOutFragmentsMode = false;
            sendBulkDataState_.sendingByteArrayState.currentSendingByteArray = null;
        }
        else {

            LogHelpers.LogDebug(TAG, "Current byte array was empty, ending sending process");

            returnSendResultAndResetSendBulkDataState(BCTResultCode.SUCCESS,
                    serviceUuid, sendBulkDataState_.currentSendingCode, sendBulkDataState_.bulkDataPayload);

        }
    }

    // a function that does no fragmentation; if it is passed something larger than the max packet size, will return error
    // *** it is assumed this function will only be called because of a sendData call, so the errors it returns are through the
    //     returnSendResultAndResetSendBulkDataState function
    private void sendSingleByteArrayFragment(UUID serviceUuid, BluetoothGattCharacteristic characteristicArgument, byte[] data) {

        int currentMaxPayloadSize = getCurrentMaxPayloadSize();

        if (data.length > currentMaxPayloadSize) {
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.ATTEMPTED_TO_SEND_DATA_LARGER_THAN_MAX_PAYLOAD_SIZE_IN_ONE_CHARACTERISTIC_WRITE,
                    serviceUuid, sendBulkDataState_.currentSendingCode, null
            );
            return;
        }

        if (bluetoothAdapter_ == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.BLUETOOTH_ADAPTER_NOT_INITIALIZED,
                    serviceUuid, sendBulkDataState_.currentSendingCode, null
            );
            return;
        }

        if (gatt_ == null || bluetoothManager_.getConnectionState(gatt_.getDevice(),
                BluetoothProfile.GATT) != STATE_CONNECTED) {
            Log.w(TAG, "No active connection to device.");
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.NO_ACTIVE_CONNECTION,
                    serviceUuid, sendBulkDataState_.currentSendingCode, null
            );
            return;
        }

        BluetoothGattService service = gatt_.getService(characteristicArgument.getService().getUuid());
        if (service == null) {
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.BULK_DATA_SERVICE_NOT_FOUND,
                    serviceUuid, sendBulkDataState_.currentSendingCode, null
            );
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicArgument.getUuid());
        if (service == null) {
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.BULK_DATA_CHARACTERISTIC_NOT_FOUND,
                    serviceUuid, sendBulkDataState_.currentSendingCode, null
            );
        }

        if (!BLEUtils.checkForCharacteristicProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE)) {
            Log.w(TAG, "No write property on characteristic with uuid: " + characteristic.getUuid().toString());
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.NO_WRITE_PROPERTY_ON_CHARACTERISTIC,
                    serviceUuid, sendBulkDataState_.currentSendingCode, null
            );
            return;
        }

        LogHelpers.LogDebug(TAG, "sendSingleByteArrayFragment function was called correctly");

        LogHelpers.LogDebug(TAG, characteristic.getUuid().toString());

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        if (!gatt_.writeCharacteristic(characteristic)) {
            Log.e(TAG,"writeCharacteristic returned false.");
            Log.w(TAG, "Write failed on characteristic with uuid: " + characteristic.getUuid().toString());
            handleSendBulkDataFailure(service.getUuid());
            return;
        }
    }

    private void resetSendingByteArrayState() {
        // resetting variables associated with sending a packet byte array over BLE
        sendBulkDataState_.sendingByteArrayState.sendingOutFragmentsMode = false;
        sendBulkDataState_.sendingByteArrayState.fragmentOffset = 0;
        sendBulkDataState_.sendingByteArrayState.currentSendingByteArray = null;
    }

    private void resetSendBulkDataState() {
        // resetting variables associated with a single bulk data transfer over BLE
        sendBulkDataState_.sendingBulkData = false;
        sendBulkDataState_.bulkDataPayloadHash = null;
        sendBulkDataState_.currentPhase = NOT_SENDING;
        sendBulkDataState_.currentSendingCode = -1;

        resetSendingByteArrayState();
    }

    //**//

    // functions for handling events of the bulk data transfer protocol

    private void handleSendBulkDataTransition(UUID serviceUuid, byte[] dataSent) {
        switch (sendBulkDataState_.currentPhase) {
            case SENDING_INITIAL_SIGNAL:
                sendBulkDataPayloadHash(serviceUuid);
                break;
            case SENDING_BULK_DATA_PAYLOAD_HASH:
                sendBulkDataPayloadLength(serviceUuid);
                break;
            case SENDING_BULK_DATA_PAYLOAD_LENGTH:
                sendBulkDataPayload(serviceUuid);
                break;
            case SENDING_BULK_DATA_PAYLOAD:
                returnSendResultAndResetSendBulkDataState(BCTResultCode.SUCCESS, serviceUuid,
                        sendBulkDataState_.currentSendingCode, dataSent);
                break;
            default:
                break;
        }
    }

    private void handleSendBulkDataFailure(UUID serviceUuid) {

        if (sendBulkDataState_.sendingBulkData = false) {
            returnSendResultAndResetSendBulkDataState(BCTResultCode.FAILED_TO_WRITE_BYTE_ARRAY, serviceUuid,
                    sendBulkDataState_.currentSendingCode, null);
        }

        switch (sendBulkDataState_.currentPhase) {
            case SENDING_INITIAL_SIGNAL:
                returnSendResultAndResetSendBulkDataState(
                        BCTResultCode.FAILED_TO_WRITE_INITIAL_SIGNAL, serviceUuid,
                        sendBulkDataState_.currentSendingCode, null
                );
                break;
            case SENDING_BULK_DATA_PAYLOAD_HASH:
                returnSendResultAndResetSendBulkDataState(
                        BCTResultCode.FAILED_TO_WRITE_BYTE_ARRAY_HASH, serviceUuid,
                        sendBulkDataState_.currentSendingCode, null
                );
                break;
            case SENDING_BULK_DATA_PAYLOAD_LENGTH:
                returnSendResultAndResetSendBulkDataState(
                        BCTResultCode.FAILED_TO_WRITE_BYTE_ARRAY_LENGTH, serviceUuid,
                        sendBulkDataState_.currentSendingCode, null
                );
                break;
            case SENDING_BULK_DATA_PAYLOAD:
                returnSendResultAndResetSendBulkDataState(
                        BCTResultCode.FAILED_TO_WRITE_BYTE_ARRAY, serviceUuid,
                        sendBulkDataState_.currentSendingCode, null
                );
                break;
            default:
                Log.e(TAG, "Unexpected phase for sending bulk data: " + sendBulkDataState_.currentPhase.toString());
                break;
        }
    }

    // wrapper functions for returning results of function calls to callbacks

    private void returnSendResultAndResetSendBulkDataState(BCTResultCode resultCode, UUID serviceUuid, long sendCode, byte[] dataSent) {

        bleCentralTransportCallbacks_.onSendDataResult(gatt_, serviceUuid, resultCode, sendCode, dataSent);

        resetSendBulkDataState();

    }

    private void returnConnectionStateChange(BluetoothGatt gatt, ConnectionState newState, BCTResultCode resultCode) {
        bleCentralTransportCallbacks_.onConnectionStateChange(gatt, newState, resultCode);
    }

    private void returnReceivedDataResult(BluetoothGatt gatt, UUID serviceUuid, BCTResultCode resultCode, byte[] data) {
        bleCentralTransportCallbacks_.onReceivedDataResult(gatt, serviceUuid, resultCode, data);
    }

    // functions for starting and cancelling the MtuNegotiationTimer

    private void startMtuNegotiationTimer() {
        handler_.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "MTU negotiation failed due to time out.");
            }
        }, MAX_MTU_NEGOTIATION_WAIT_TIME);
    }

    private void cancelMtuNegotiationTimer() {
        handler_.removeCallbacksAndMessages(null);
    }

    // public functions for user

    public void connectAndDiscoverServicesAndNegotiateMTU(final String address) {

        LogHelpers.LogDebug(TAG, "entering the connect function of bleService");

        if (bluetoothAdapter_ == null) {
            Log.e(TAG, "BluetoothAdapter not initialized.");
            //returnConnectResult(BCTResultCode.BLUETOOTH_ADAPTER_NOT_INITIALIZED);
            return;
        }

        if (address == null) {
            Log.e(TAG, "Address was null.");
            //returnConnectResult(BCTResultCode.ADDRESS_NULL);
            return;
        }

        final BluetoothDevice device = bluetoothAdapter_.getRemoteDevice(address);

        if (gatt_ == null && bluetoothManager_.getConnectionState(device, BluetoothProfile.GATT) != STATE_CONNECTED)
            device.connectGatt(ctx_, false, gattCallback_, BluetoothDevice.TRANSPORT_LE);

    }

    public void requestMtu(int newMtu) {
        if (bluetoothAdapter_ == null) {
            Log.e(TAG, "BluetoothAdapter not initialized.");
            //returnConnectResult(BCTResultCode.BLUETOOTH_ADAPTER_NOT_INITIALIZED);
            return;
        }

        if (gatt_ == null)
            gatt_.requestMtu(newMtu);
    }

    public void disconnect() {

        if (bluetoothAdapter_ == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            //returnDisconnectResult(BCTResultCode.BLUETOOTH_ADAPTER_NOT_INITIALIZED);
            return;
        }

        if (gatt_ == null || bluetoothManager_.getConnectionState(gatt_.getDevice(), BluetoothProfile.GATT) != STATE_CONNECTED) {
            Log.w(TAG, "No active connection to device.");
            //returnDisconnectResult(BCTResultCode.NO_ACTIVE_CONNECTION);
            return;
        }

        //returnDisconnectResult(BCTResultCode.SUCCESS);

        gatt_.disconnect();

    }

    public void shutdown() {
        if (gatt_ != null) {
            gatt_.disconnect();
            gatt_.close();
        }
    }

    public void sendData(UUID serviceUuid, byte[] data, long sendCode) {

        LogHelpers.LogDebug(TAG, "Send for a characteristic got called.");

        if (sendCode <= 0) {
            Log.w(TAG, "sendCode must be positive.");
            returnSendResultAndResetSendBulkDataState(BCTResultCode.SEND_CODE_MUST_BE_POSITIVE, serviceUuid,
                    sendCode, null);
            return;
        }

        if (bluetoothAdapter_ == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            returnSendResultAndResetSendBulkDataState(BCTResultCode.BLUETOOTH_ADAPTER_NOT_INITIALIZED, serviceUuid,
                    sendCode, null);
            return;
        }

        if (gatt_ == null) {
            Log.w(TAG, "gatt_ was null.");
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.GATT_OBJECT_WAS_NULL, serviceUuid, sendCode, null
            );
            return;
        }

        if (bluetoothManager_.getConnectionState(gatt_.getDevice(),
                BluetoothProfile.GATT) != STATE_CONNECTED) {
            Log.w(TAG, "No active connection to device, current connection state: " +
             bluetoothManager_.getConnectionState(gatt_.getDevice(), BluetoothProfile.GATT));
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.NO_ACTIVE_CONNECTION, serviceUuid, sendCode, null
            );
            return;
        }

        if (!sendBulkDataState_.currentPhase.equals(NOT_SENDING)) {
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.EXISTING_SEND_IN_PROGRESS, serviceUuid, sendCode, null);
            return;
        }

        sendBulkDataState_.currentSendingCode = sendCode;

        int currentMaxPayloadSize = getCurrentMaxPayloadSize();

        if (data.length < currentMaxPayloadSize) {
            LogHelpers.LogDebug(TAG, "Call to sendData had length of " + data.length + ", sending as normal data.");

            BluetoothGattService service = gatt_.getService(serviceUuid);
            if (service == null) {
                returnSendResultAndResetSendBulkDataState(BCTResultCode.SERVICE_RETRIEVAL_FAILED,
                        serviceUuid, sendCode, null);
                return;
            }

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(
                    UUID.fromString(BLE_DATA_TRANSFER_CHARACTERISTIC_UUID));
            if (characteristic == null) {
                returnSendResultAndResetSendBulkDataState(BCTResultCode.CHARACTERISTIC_RETRIEVAL_FAILED,
                        serviceUuid, sendCode, null);
                return;
            }

            sendSingleByteArrayFragment(serviceUuid, characteristic, data);
            return;
        }
        else {
            Log.e(TAG, "TRIED TO SEND DATA LARGER THAN MAX PACKET SIZE - 3, WHICH IS CURRENTLY " + currentMaxPayloadSize
                    + " IS NOT SUPPORTED.");
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.ATTEMPTED_TO_SEND_DATA_LARGER_THAN_MAX_PAYLOAD_SIZE_IN_ONE_CHARACTERISTIC_WRITE,
                    serviceUuid, sendCode, null);
            return;
        }

        /*
        sendBulkDataState_.currentPhase = SENDING_INITIAL_SIGNAL;

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            returnSendResultAndResetSendBulkDataState(
                    BCTResultCode.PROBLEM_GENERATING_MESSAGE_HASH, sendCode, null);
            return;

        }

        sendBulkDataState_.bulkDataPayloadHash = digest.digest(
                sendBulkDataState_.bulkDataPayload
        );

        LogHelpers.LogDebug(TAG, "Hash generated over byte array to be sent: " + sendBulkDataState_.bulkDataPayloadHash.toString());

        sendBulkDataState_.sendingBulkData = true;
        sendBulkDataState_.bulkDataPayload = data;
        sendInitialSignal();
        */
    }

}
