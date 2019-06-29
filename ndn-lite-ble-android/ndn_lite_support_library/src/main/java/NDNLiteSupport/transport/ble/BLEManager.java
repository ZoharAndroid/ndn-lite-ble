
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.transport.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import NDNLiteSupport.LogHelpers;

public class BLEManager {

    private static String TAG = BLEManager.class.getSimpleName();

    private boolean initialized_ = false;

    private BLEManager(){}

    private static class SingletonHelper{
        private static final BLEManager INSTANCE = new BLEManager();
    }

    public static BLEManager getInstance(){
        return SingletonHelper.INSTANCE;
    }

    private Context ctx_;

    private BluetoothManager bluetoothManager_;
    private BluetoothAdapter bluetoothAdapter_;

    private BLEScanner bleScanner_;

    private BLEAdvertiser bleAdvertiser_;

    ArrayList<BLEManagerObserver> bleManagerObservers_;

    // place to manager the ble transports to connected devices; maps the device's
    // mac address to its transport object used for communication
    private HashMap<String, BLECentralTransport> bleTransports_;

    public interface BLEManagerObserver {

        void onDeviceEnteredRange(BLEScanner.BluetoothDeviceAndScanRecord deviceAndScanRecord);

        void onDevicesLeftRange(ArrayList<BLEScanner.BluetoothDeviceAndScanRecord> devicesAndScanRecords);

        void onScanFailed(int errorCode);

        void onScanCycleEnded();

        void onConnectionStateChange(BluetoothGatt gatt, BLECentralTransport.ConnectionState newState,
                                     BLECentralTransport.BCTResultCode resultCode);

        void onSendDataResult(BluetoothGatt gatt, UUID serviceUuid,
                              BLECentralTransport.BCTResultCode resultCode, long sendCode, byte[] dataSent);

        void onReceivedDataResult(BluetoothGatt gatt, UUID serviceUuid,
                                  BLECentralTransport.BCTResultCode resultCode, byte[] data);

    }

    public boolean initialize(Context ctx) throws NullPointerException {
        if (!initialized_) {
            ctx_ = ctx;

            bleManagerObservers_ = new ArrayList<>();

            if (bluetoothManager_ == null) {
                bluetoothManager_ = (BluetoothManager) ctx_.getSystemService(Context.BLUETOOTH_SERVICE);
                if (bluetoothManager_ == null) {
                    Log.e(TAG, "Unable to initialize BluetoothManager.");
                    throw new NullPointerException("Unable to initialize BluetoothManager.");
                }
            }

            bluetoothAdapter_ = bluetoothManager_.getAdapter();
            if (bluetoothAdapter_ == null) {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                throw new NullPointerException("Unable to obtain a BluetoothAdapter.");
            }

           // if (bluetoothAdapter_.isLeExtendedAdvertisingSupported())
            //    LogHelpers.LogDebug(TAG, "The bluetooth adapter supports BLE extended advertising.");

            bleTransports_ = new HashMap<>();
            initialized_ = true;
            return true;
        }
        else {
            Log.e(TAG, "BLEManager was already initialized.");
            return false;
        }
    }

    public void addObserver(BLEManagerObserver observer) {
        bleManagerObservers_.add(observer);
    }

    public void removeObserver(BLEManagerObserver observer) {
        bleManagerObservers_.remove(observer);
    }

    public void initializeBLEScanner() throws NullPointerException, IllegalArgumentException {
        initializeBLEScanner(ctx_, null);
    }

    public void initializeBLEScanner(ArrayList<UUID> serviceUUIDs) throws NullPointerException, IllegalArgumentException {
        initializeBLEScanner(ctx_, serviceUUIDs);
    }

    public void initializeBLEScanner(Context context, ArrayList<UUID> serviceFilterUUIDs)
            throws NullPointerException, IllegalArgumentException {
        try {
            bleScanner_ = new BLEScanner(context, serviceFilterUUIDs, bleScannerCallbacks_);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            throw e;
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void startScanCycle() throws NullPointerException {

        if (bleScanner_ == null)
            throw new NullPointerException("BLEScanner not initialized.");

        bleScanner_.startScanCycle();
    }

    public void stopScanCycle() throws NullPointerException {

        if (bleScanner_ == null)
            throw new NullPointerException("BLEScanner not initialized.");

        bleScanner_.stopScanCycle();
    }

    public void connect(String deviceAddress) {

        LogHelpers.LogDebug(TAG, "Connect got called for device with address: " + deviceAddress);

        String allBleTransportEntries = "";
        for (String address : bleTransports_.keySet()) {
            allBleTransportEntries += address + "\n";
        }
        LogHelpers.LogDebug(TAG, "All addresses in bleTransports_: " + "\n" + allBleTransportEntries);

        BLECentralTransport bleTransport = new BLECentralTransport(
                ctx_, bluetoothManager_, bluetoothAdapter_, deviceAddress,
                bleCentralTransportCallbacks_);

        bleTransport.connectAndDiscoverServicesAndNegotiateMTU(deviceAddress);
        bleTransports_.put(deviceAddress, bleTransport);

    }

    public void requestMtu(String deviceAddress, int newMtu) {
        if (!bleTransports_.containsKey(deviceAddress)) {
            LogHelpers.LogWarning(TAG, "In requestMtu, device address " + deviceAddress + " not found in " +
                    "bleTransports_.");
            return;
        }

        BLECentralTransport transport = bleTransports_.get(deviceAddress);

        if (transport.currentConnectionState_ == null) {
            LogHelpers.LogWarning(TAG, "in requestMtu, current connection state of transport was null.");
            return;
        }

        if (!transport.currentConnectionState_.equals(BLECentralTransport.ConnectionState.STATE_CONNECTED) &&
                !transport.currentConnectionState_.equals(BLECentralTransport.ConnectionState.STATE_MTU_NEGOTIATED)) {
            LogHelpers.LogWarning(TAG, "in requestMtu, current connection state of transport was: " +
            transport.currentConnectionState_);
            return;
        }

        transport.requestMtu(newMtu);
    }

    public void disconnect(String deviceAddress) {
        BLECentralTransport currentTransport = bleTransports_.get(deviceAddress);
        currentTransport.disconnect();
    }

    public void sendData(UUID serviceUuid, String deviceAddress, byte[] data,
                             long sendCode) {

        if (!bleTransports_.containsKey(deviceAddress)) {
            notifyObserversOnSendDataResult(null, serviceUuid,
                    BLECentralTransport.BCTResultCode.NO_TRANSPORT_FOR_DEVICE,
                    sendCode, null);
            return;
        }

        BLECentralTransport transport = bleTransports_.get(deviceAddress);

        transport.sendData(serviceUuid, data, sendCode);

    }

    public void shutdown() {
        Iterator iterator = bleTransports_.keySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry)iterator.next();
            bleTransports_.get(pair.getKey()).shutdown();
        }
    }

    // bridging the bleManagerCallback callbacks to the BLEScanner and BLECentralTransport callbacks

    BLEScanner.BLEScannerCallbacks bleScannerCallbacks_ = new BLEScanner.BLEScannerCallbacks() {
        @Override
        public void onDeviceEnteredRange(BLEScanner.BluetoothDeviceAndScanRecord deviceAndScanRecord) {

            String deviceAddress = deviceAndScanRecord.bluetoothDevice.getAddress();

            notifyObserversOnDeviceEnteredRange(deviceAndScanRecord);

        }

        @Override
        public void onDevicesLeftRange(ArrayList<BLEScanner.BluetoothDeviceAndScanRecord> devicesAndScanRecords) {

            notifyObserversOnDevicesLeftRange(devicesAndScanRecords);

        }

        @Override
        public void onScanFailed(int errorCode) {
            notifyObserversOnScanFailed(errorCode);
        }

        @Override
        public void onScanCycleEnded() {
            notifyObserversOnScanCycleEnded();
        }
    };

    BLECentralTransport.BLECentralTransportCallbacks bleCentralTransportCallbacks_ =
            new BLECentralTransport.BLECentralTransportCallbacks() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, BLECentralTransport.ConnectionState newState,
                                            BLECentralTransport.BCTResultCode resultCode) {
            notifyObserversOnConnectionStateChange(gatt, newState, resultCode);
        }

        @Override
        public void onSendDataResult(BluetoothGatt gatt, UUID serviceUuid,
                                     BLECentralTransport.BCTResultCode resultCode, long sendCode, byte[] dataSent) {
            notifyObserversOnSendDataResult(gatt, serviceUuid, resultCode, sendCode, dataSent);
        }

        @Override
        public void onReceivedDataResult(BluetoothGatt gatt, UUID serviceUuid,
                                         BLECentralTransport.BCTResultCode resultCode, byte[] data) {
            notifyObserversOnReceivedDataResult(gatt, serviceUuid, resultCode, data);
        }
    };

    public boolean isConnected(String deviceMacAddress) {
        if (!bleTransports_.containsKey(deviceMacAddress)) {
            Log.e(TAG, "in isConnected function of BLEManager, there was no transport for device " +
                    "with mac address: " + deviceMacAddress);
            return false;
        }
        if (bleTransports_.get(deviceMacAddress) == null) {
            Log.e(TAG, "in isConnected function of BLEManager, bletransports_ had a null object " +
                    "for device with mac address: " + deviceMacAddress);
            return false;
        }
        BLECentralTransport currentTransport = bleTransports_.get(deviceMacAddress);
        if (currentTransport.currentConnectionState_ == null) {
            Log.e(TAG, "in isConnected function of BLEManager, currentTransport had a null object" +
                    "for currentConnectionState, for device with mac address: " + deviceMacAddress);
            return false;
        }
        if (currentTransport.currentConnectionState_.equals(BLECentralTransport.ConnectionState.STATE_DISCONNECTED)) {
            Log.e(TAG, "in isConnected function of BLEManager, the connection state for the device was " +
                bleTransports_.get(deviceMacAddress).currentConnectionState_);
            return false;
        }
        return true;
    }

    private void notifyObserversOnSendDataResult(BluetoothGatt gatt, UUID serviceUuid,
                                                 BLECentralTransport.BCTResultCode resultCode,
                                                 long sendCode, byte[] dataSent) {
        for (BLEManagerObserver observer : bleManagerObservers_) {
            observer.onSendDataResult(gatt, serviceUuid, resultCode, sendCode, dataSent);
        }
    }

    private void notifyObserversOnReceivedDataResult(BluetoothGatt gatt, UUID serviceUuid,
                                                     BLECentralTransport.BCTResultCode resultCode,
                                                     byte data[]) {
        for (BLEManagerObserver observer : bleManagerObservers_) {
            observer.onReceivedDataResult(gatt, serviceUuid, resultCode, data);
        }
    }

    private void notifyObserversOnConnectionStateChange(BluetoothGatt gatt, BLECentralTransport.ConnectionState newState,
                                                        BLECentralTransport.BCTResultCode resultCode) {
        for (BLEManagerObserver observer : bleManagerObservers_) {
            observer.onConnectionStateChange(gatt, newState, resultCode);
        }
    }

    private void notifyObserversOnScanFailed(int errorCode) {
        for (BLEManagerObserver observer : bleManagerObservers_) {
            observer.onScanFailed(errorCode);
        }
    }

    private void notifyObserversOnScanCycleEnded() {
        for (BLEManagerObserver observer : bleManagerObservers_) {
            observer.onScanCycleEnded();
        }
    }

    private void notifyObserversOnDeviceEnteredRange(BLEScanner.BluetoothDeviceAndScanRecord deviceAndScanRecord) {
        for (BLEManagerObserver observer : bleManagerObservers_) {
            observer.onDeviceEnteredRange(deviceAndScanRecord);
        }
    }

    private void notifyObserversOnDevicesLeftRange(ArrayList<BLEScanner.BluetoothDeviceAndScanRecord> devicesAndScanRecords) {
        for (BLEManagerObserver observer : bleManagerObservers_) {
            observer.onDevicesLeftRange(devicesAndScanRecords);
        }
    }

}
