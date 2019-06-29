
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.BLEUnicastConnectionMaintainer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.UUID;

import NDNLiteSupport.LogHelpers;
import NDNLiteSupport.transport.ble.BLECentralTransport;
import NDNLiteSupport.transport.ble.BLEManager;
import NDNLiteSupport.transport.ble.BLEScanner;

import static NDNLiteSupport.NDNLiteAndroidSupportConsts.NDN_LITE_BLE_UNICAST_SERVICE_UUID;

public class BLEUnicastConnectionMaintainer {

    // TAG for logging.
    private static final String TAG = BLEUnicastConnectionMaintainer.class.getSimpleName();

    private BLEUnicastConnectionMaintainer(){}

    private static class SingletonHelper{
        private static final BLEUnicastConnectionMaintainer INSTANCE = new BLEUnicastConnectionMaintainer();
    }

    /**
     * Get a reference to the BLEUnicastConnectionMaintainer singleton. The point of this
     * class is to scan for BLE devices advertising the ndn lite ble unicast transport service
     * uuid, and connect to them whenever they are detected. This is necessary as to share the
     * unicast and extended advertising on the device side for the ndn-lite ble face, the device
     * must disconnect from the controller to send data through extended advertising (extended
     * advertising cannot be done while in a unicast connection on the device side). Therefore,
     * it becomes the controller's job to proactively maintain connections to devices that are using
     * the ndn lite library.
     */
    public static BLEUnicastConnectionMaintainer getInstance(){
        return SingletonHelper.INSTANCE;
    }

    /**
     * Initialize the SignOnBasicControllerBLE singleton.
     *
     * @param ctx Context used to initialize the BLE manager.
     */
    public void initialize(Context ctx) {
        signOnDevicesFound_ = new ArrayList<>();

        BLEManager.getInstance().initialize(ctx);
        BLEManager.getInstance().addObserver(bleManagerObserver_);

        ArrayList<UUID> filterUuids = new ArrayList<>();
        filterUuids.add(NDN_LITE_BLE_UNICAST_SERVICE_UUID);
        BLEManager.getInstance().initializeBLEScanner(filterUuids);

        BLEManager.getInstance().startScanCycle();
    }

    BLEManager.BLEManagerObserver bleManagerObserver_ = new BLEManager.BLEManagerObserver() {
        @Override
        public void onDeviceEnteredRange(BLEScanner.BluetoothDeviceAndScanRecord deviceAndScanRecord) {
            BluetoothDevice device = deviceAndScanRecord.bluetoothDevice;

            LogHelpers.LogDebug(TAG, "Found a secure sign-on device with name " + device.getName() + ". Connecting to it now.");

            LogHelpers.LogByteArrayDebug(TAG, "Bytes of device's scan record:", deviceAndScanRecord.scanRecord.getBytes());

            signOnDevicesFound_.add(deviceAndScanRecord);

            BLEManager.getInstance().connect(device.getAddress());
        }

        @Override
        public void onDevicesLeftRange(ArrayList<BLEScanner.BluetoothDeviceAndScanRecord> arrayList) {

        }

        @Override
        public void onScanFailed(int i) {

        }

        @Override
        public void onScanCycleEnded() {
            LogHelpers.LogDebug(TAG, "in BLEUnicastConnectionMaintainer, onScanCycleEnded got called.");

            for (BLEScanner.BluetoothDeviceAndScanRecord deviceAndScanRecord : signOnDevicesFound_) {
                String deviceAddress = deviceAndScanRecord.bluetoothDevice.getAddress();
                if (!BLEManager.getInstance().isConnected(deviceAddress)) {
                    LogHelpers.LogDebug(TAG, "in onScanCycleEnded, attempting to connect to device with mac " +
                     "address deviceAddress");
                    BLEManager.getInstance().connect(deviceAddress);
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, BLECentralTransport.ConnectionState newState,
                                            BLECentralTransport.BCTResultCode resultCode) {
            if (resultCode.equals(BLECentralTransport.BCTResultCode.SUCCESS)) {
                LogHelpers.LogDebug(TAG, "Connection state changed.");
                if (newState.equals(BLECentralTransport.ConnectionState.STATE_CONNECTED)) {
                    LogHelpers.LogDebug(TAG, "Successfully connected to device with name: " + gatt.getDevice().getName());
                }
                else if (newState.equals(BLECentralTransport.ConnectionState.STATE_DISCOVERED_SERVICES)) {
                    LogHelpers.LogDebug(TAG, "Services were discovered for device with name: " + gatt.getDevice().getName());
                }
                else if (newState.equals(BLECentralTransport.ConnectionState.STATE_NOTIFICATIONS_ENABLED)) {
                    LogHelpers.LogDebug(TAG, "Notifications were enabled for device with name: " + gatt.getDevice().getName());
                }
                else if (newState.equals(BLECentralTransport.ConnectionState.STATE_MTU_NEGOTIATED)) {
                    LogHelpers.LogDebug(TAG, "MTU has been negotiated, meaning that we can now communicate with the device.");
                }
                else if (newState.equals(BLECentralTransport.ConnectionState.STATE_DISCONNECTED)) {
                    LogHelpers.LogDebug(TAG, "Disconnected from device with name: " + gatt.getDevice().getName());

                    BLEManager.getInstance().connect(gatt.getDevice().getAddress());
                }
            }
            else {
                Log.e(TAG, "Error in connection state change: " + resultCode);

                BLEManager.getInstance().connect(gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onSendDataResult(BluetoothGatt bluetoothGatt, UUID uuid, BLECentralTransport.BCTResultCode bctResultCode, long l, byte[] bytes) {

        }

        @Override
        public void onReceivedDataResult(BluetoothGatt bluetoothGatt, UUID uuid, BLECentralTransport.BCTResultCode bctResultCode, byte[] bytes) {
        }
    };

    // keeps track of the bluetooth device object / scan record of all devices advertising the
    // ndn lite ble unicast transport service uuid that have ever entered range
    ArrayList<BLEScanner.BluetoothDeviceAndScanRecord> signOnDevicesFound_;

}
