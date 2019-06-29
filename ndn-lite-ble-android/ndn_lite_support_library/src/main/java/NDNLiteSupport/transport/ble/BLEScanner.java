
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
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import NDNLiteSupport.LogHelpers;

public class BLEScanner {

    private static final String TAG = BLEScanner.class.getSimpleName();

    // constants related to BLE scan cycle
    public final static long DEFAULT_SCAN_PERIOD = 3500;
    public final static long DEFAULT_BACKOFF_THRESHOLD = Long.MAX_VALUE;
    public final static double DEFAULT_SCAN_INTERVAL_MULTIPLIER = 2;
    public final static long DEFAULT_SCAN_INTERVAL_MAX = 3000;
    public final static long DEFAULT_SCAN_INTERVAL_MIN = 3000;
    public final static long DEFAULT_MAX_TOLERABLE_MISSED_SCANS = 0;

    // see https://stackoverflow.com/questions/45681711/app-is-scanning-too-frequently-with-scansettings-scan-mode-opportunistic
    private static final long PREDEFINED_SCAN_MEASUREMENT_PERIOD = 30000;
    private static final long MAX_SCAN_STARTS_PER_PREDEFINED_SCAN_MEASUREMENT_PERIOD = 5;
    // constants for broadcasts to start waiting for a scan interval or to start another scan
    private final static String START_SCAN_INTERVAL = "START_SCAN_INTERVAL";
    private final static String START_SCAN_PERIOD = "START_SCAN_PERIOD";

    // this is used to asynchronously wait for scan periods and scan intervals to finish
    private Handler handler_;
    // context object for sending local broadcasts
    private Context ctx_;

    private BluetoothManager bluetoothManager_;
    private BluetoothAdapter bluetoothAdapter_;
    private BluetoothLeScanner bluetoothLeScanner_;

    private ArrayList<ScanFilter> scanFilters_;
    private ScanSettings scanSettings_;

    // HashMap to hold scanRecord information for devices in range, maps device address to scan record
    private HashMap<String, ScanRecord> scanRecords_;
    // ArrayList to hold BluetoothDevice objects corresponding to devices in range, maps device address to BluetoothDevice
    private HashMap<String, BluetoothDevice> bluetoothDeviceObjects_;
    // HashSet to detect duplicate device detections on scans; stores device's mac address as string
    // ** after each scan, is copied into lastDeviceAddresses_ and then wiped after being used for processing in
    //    the checkForDevicesToRemoveAndUpdateMissedScanCounts function
    private HashSet<String> deviceAddresses_;
    // Hashset of previous list of mac addresses of discovered devices; this is so that calculations can be done on what devices
    // are still considered within range while a new scan starts in the scan cycle
    private HashSet<String> lastDeviceAddresses_;
    // Hashmap that maps a device mac address to its missed scan count; this missed scan count is used to decide whether a device
    // should still be considered in range or not
    // *** all of the device addresses in the keyset of this data structure are considered in range
    private HashMap<String, Integer> devicesInRangeMissedScanCounters_;
    // Hashmap to hold BluetoothDeviceAndScanRecord objects for all the devices considered in range; maps these devices' mac
    // addresses to their corresponding BluetoothDeviceAndScanRecord object
    public HashMap<String, BluetoothDeviceAndScanRecord> bluetoothDeviceAndScanRecordsForDevicesInRange_;

    // the length of time that scans last for
    private long scanPeriod_;
    // the time period between scans; decreases to scan more frequently as new devices are discovered,
    // increases to scan less frequently when no devices have been detected for backoffThreshold_ scans
    private long scanInterval_;
    // after backoffThreshold_ scans have passed where no BLE devices are detected, the scanInterval will increase
    // to decrease scanning frequency
    private long backoffThreshold_;
    // this is what the scanInterval will be multiplied by when backoffThreshold_ scans have passed where no
    // devices were discovered; scanInterval will be multiplied by this at the end of each scan until it either
    // reaches scanIntervalMax_ or devices are discovered on a scan
    private double scanIntervalMultiplier_;

    // this is the maximum value the scan interval can reach
    private long scanIntervalMax_;
    // this is the minimum value the scan interval can reach
    private long scanIntervalMin_;

    // a counter that increments when there are no devices discovered on a scan, and is reset to zero
    // when any devices are discovered on a scan
    private long noDevicesInRangeOnScanCounter_;
    // the maximum number of scans that a device can be missed before it is considered out of range
    private long maxTolerableMissedScans_;

    // set to true when we are in a scan cycle (this means we are either currently scanning in a scanPeriod, or we are
    // currently waiting for the next scan in a scanInterval)
    private boolean currentlyInScanCycle_;

    public interface BLEScannerCallbacks {

        void onDeviceEnteredRange(BluetoothDeviceAndScanRecord deviceAndScanRecord);

        void onDevicesLeftRange(ArrayList<BluetoothDeviceAndScanRecord> devicesAndScanRecords);

        void onScanFailed(int errorCode);

        void onScanCycleEnded();

    }

    // callback functions for the user to implement
    private BLEScannerCallbacks bleScannerCallbacks_;

    public class BluetoothDeviceAndScanRecord {

        public BluetoothDevice bluetoothDevice;
        public ScanRecord scanRecord;

        public BluetoothDeviceAndScanRecord(BluetoothDevice bluetoothDevice, ScanRecord scanRecord) {
            this.bluetoothDevice = bluetoothDevice;
            this.scanRecord = scanRecord;
        }

    }

    public BLEScanner(Context ctx, long scanPeriod,
                      long backoffThreshold, long maxTolerableMissedScans, double scanIntervalMultiplier,
                      long scanIntervalMax, long scanIntervalMin, List<UUID> serviceUUIDs,
                      BLEScannerCallbacks bleScannerCallbacks)
            throws NullPointerException, IllegalArgumentException {

        handler_ = new Handler();

        ctx_ = ctx;
        scanPeriod_ = scanPeriod;
        backoffThreshold_ = backoffThreshold;
        maxTolerableMissedScans_ = maxTolerableMissedScans;
        scanIntervalMultiplier_ = scanIntervalMultiplier;
        scanIntervalMax_ = scanIntervalMax;
        scanIntervalMin_ = scanIntervalMin;
        scanInterval_ = scanIntervalMin_;

        try {
            checkForValidScanParameters(scanPeriod_, scanIntervalMin_, scanIntervalMax_);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }

        bleScannerCallbacks_ = bleScannerCallbacks;

        scanFilters_ = new ArrayList<>();
        if (serviceUUIDs != null) {
            LogHelpers.LogDebug(TAG, "Service UUIDs was not null, adding service uuid's to filter for scan.");
            for (UUID uuid : serviceUUIDs) {
                LogHelpers.LogDebug(TAG, "This UUID is being added to filter for scan: " + uuid.toString());
                scanFilters_.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuid)).build());
            }
        }

        scanSettings_ = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();

        LocalBroadcastManager.getInstance(ctx_).registerReceiver(startScanIntervalSignalReceiver,
                new IntentFilter(START_SCAN_INTERVAL));
        LocalBroadcastManager.getInstance(ctx_).registerReceiver(startScanPeriodSignalReceiver,
                new IntentFilter(START_SCAN_PERIOD));

        if (bluetoothManager_ == null) {
            bluetoothManager_ = (BluetoothManager) ctx_.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager_ == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                throw new NullPointerException();
            }
        }

        bluetoothAdapter_ = bluetoothManager_.getAdapter();
        if (bluetoothAdapter_ == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            throw new NullPointerException();
        }

        bluetoothLeScanner_ = bluetoothAdapter_.getBluetoothLeScanner();
        if (bluetoothLeScanner_ == null) {
            Log.e(TAG, "Unable to obtain a BluetoothLeScanner.");
            throw new NullPointerException();
        }

        deviceAddresses_ = new HashSet<>();
        lastDeviceAddresses_ = new HashSet<>();
        scanRecords_ = new HashMap<>();
        bluetoothDeviceObjects_ = new HashMap<>();
        devicesInRangeMissedScanCounters_ = new HashMap<>();
        bluetoothDeviceAndScanRecordsForDevicesInRange_ = new HashMap<>();

        LogHelpers.LogDebug(TAG, "Initialized a BLE Scanner object.");

    }

    public BLEScanner(Context ctx, List<UUID> serviceUUIDs, BLEScannerCallbacks bleScannerCallbacks) {
        this(ctx, DEFAULT_SCAN_PERIOD, DEFAULT_BACKOFF_THRESHOLD, DEFAULT_MAX_TOLERABLE_MISSED_SCANS,
                DEFAULT_SCAN_INTERVAL_MULTIPLIER, DEFAULT_SCAN_INTERVAL_MAX, DEFAULT_SCAN_INTERVAL_MIN,
                serviceUUIDs, bleScannerCallbacks);
    }

    public BLEScanner(Context ctx, BLEScannerCallbacks bleScannerCallbacks) {
        this(ctx, DEFAULT_SCAN_PERIOD, DEFAULT_BACKOFF_THRESHOLD, DEFAULT_MAX_TOLERABLE_MISSED_SCANS,
                DEFAULT_SCAN_INTERVAL_MULTIPLIER, DEFAULT_SCAN_INTERVAL_MAX, DEFAULT_SCAN_INTERVAL_MIN,
                null, bleScannerCallbacks);
    }

    private ScanCallback bleScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    BluetoothDevice device = result.getDevice();
                    ScanRecord scanRecord = result.getScanRecord();

                    String deviceAddress = device.getAddress();

                    if (!deviceAddresses_.contains(deviceAddress)) {

                        if (!scanRecords_.containsKey(deviceAddress))
                            scanRecords_.put(deviceAddress, scanRecord);
                        if (!bluetoothDeviceObjects_.containsKey(deviceAddress))
                            bluetoothDeviceObjects_.put(deviceAddress, device);

                        deviceAddresses_.add(deviceAddress);

                        if (!devicesInRangeMissedScanCounters_.containsKey(deviceAddress)) {
                            devicesInRangeMissedScanCounters_.put(deviceAddress, 0);
                            bluetoothDeviceAndScanRecordsForDevicesInRange_.put(deviceAddress,
                                    new BluetoothDeviceAndScanRecord(device, scanRecord));
                            bleScannerCallbacks_.onDeviceEnteredRange(new BluetoothDeviceAndScanRecord(device, scanRecord));
                        }

                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);

                    Log.e(TAG, "BLE SCAN FAILED, ERROR CODE: " + errorCode);
                    bleScannerCallbacks_.onScanFailed(errorCode);
                }
            };

    // function to wait for the scan interval time period before starting the next scan
    private void waitForScanInterval() {

        LogHelpers.LogDebug(TAG, "Entered the waitForScanInterval function.");
        // Stops scanning after a pre-defined scan period.
        handler_.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentlyInScanCycle_) {
                    LogHelpers.LogDebug(TAG, "In the waitForScanInterval function, sending a broadcast to start a scan period.");
                    LocalBroadcastManager.getInstance(ctx_).sendBroadcast(new Intent(START_SCAN_PERIOD));
                }
                else {
                    LogHelpers.LogDebug(TAG, "In the waitForScanInterval function, did not sendData broadcast to start scan period since we are not" +
                            " currently in a scan cycle.");
                }
            }
        }, scanInterval_);
    }

    private final BroadcastReceiver startScanIntervalSignalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentlyInScanCycle_) {
                LogHelpers.LogDebug(TAG, "Start scan interval signal receiver got signal to wait for scan interval.");
                waitForScanInterval();
            }
            else {
                LogHelpers.LogDebug(TAG, "In start scan interval signal receiver, we were not in scan cycle.");
            }
        }
    };

    private final BroadcastReceiver startScanPeriodSignalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentlyInScanCycle_) {
                LogHelpers.LogDebug(TAG, "Start scan period signal receiver got signal to scan for devices.");
                scanForBLEDevices();
            }
            else {
                LogHelpers.LogDebug(TAG, "In start scan period signal receiver, we were not in scan cycle.");
            }
        }
    };

    // Stops scanning after scanPeriod milliseconds to save battery
    private void scanForBLEDevices() {

        LogHelpers.LogDebug(TAG, "Entered the scanForBLEDevices function");
        // Stops scanning after a pre-defined scan period.
        handler_.postDelayed(new Runnable() {
            @Override
            public void run() {
                LogHelpers.LogDebug(TAG, "Post delayed code for handler got triggered.");
                bluetoothLeScanner_.stopScan(bleScanCallback);

                LogHelpers.LogDebug(TAG, "Within scan cycle: stopped LE scan after timer ran out");

                bleScannerCallbacks_.onScanCycleEnded();

                updateDeviceRelatedInformation();

                if (currentlyInScanCycle_)
                    LocalBroadcastManager.getInstance(ctx_).sendBroadcast(new Intent(START_SCAN_INTERVAL));
                else
                    LogHelpers.LogDebug(TAG, "Did not broadcast to start scan interval, since we are not currently in a scan cycle.");

            }
        }, scanPeriod_);

        LogHelpers.LogDebug(TAG, "Within scan cycle: started BLE scanner");

        bluetoothLeScanner_.startScan(scanFilters_, scanSettings_, bleScanCallback);

        LogHelpers.LogDebug(TAG, "Within scan cycle: after calling startScan");
    }

    private void updateDeviceRelatedInformation() {

        LogHelpers.LogDebug(TAG, "Entered the update device related information function.");

        lastDeviceAddresses_.clear();
        lastDeviceAddresses_.addAll(deviceAddresses_);

        LogHelpers.LogDebug(TAG, "After replacing the lastDeviceAddresses with the addresses of the devices we just scanned.");

        ArrayList<BluetoothDeviceAndScanRecord> devicesToRemove =
                checkForDevicesToRemoveAndUpdateMissedScanCounts();

        LogHelpers.LogDebug(TAG, "After getting the arraylist of devices to remove.");

        for (BluetoothDeviceAndScanRecord deviceAndScanRecord : devicesToRemove) {

            LogHelpers.LogDebug(TAG, "Removing device with this address from devicesInRangeMissedScanCounters_: " +
                    deviceAndScanRecord.bluetoothDevice.getAddress());

            String deviceAddress = deviceAndScanRecord.bluetoothDevice.getAddress();

            devicesInRangeMissedScanCounters_.remove(deviceAddress);
            bluetoothDeviceAndScanRecordsForDevicesInRange_.remove(deviceAddress);

            // although it might be more efficient to not remove a device's BluetoothDevice and ScanRecord
            // information once they leave range, I do it in case the device updates and its BluetoothDevice related
            // information or ScanRecord change the next time the device comes in range
            scanRecords_.remove(deviceAddress);
            bluetoothDeviceObjects_.remove(deviceAddress);

        }

        LogHelpers.LogDebug(TAG, "After removing all devices that went out of range.");

        if (devicesToRemove.size() != 0) {
            // call some function to notify the user that devices left range, and give them that list
            // of devices that left range
            bleScannerCallbacks_.onDevicesLeftRange(devicesToRemove);
        }

        LogHelpers.LogDebug(TAG, "After calling the onDevicesLeftRange callback, if it was necessary.");

        if (noDevicesInRangeOnScanCounter_ > backoffThreshold_) {

            if (scanInterval_ != scanIntervalMax_) {
                LogHelpers.LogDebug(TAG, "Multiplying scan interval by " + scanIntervalMultiplier_ + ".");
                scanInterval_ *= scanIntervalMultiplier_;

                if (scanInterval_ > scanIntervalMax_)
                    scanInterval_ = scanIntervalMax_;
            }

        } else {
            scanInterval_ = scanIntervalMin_;
        }

        LogHelpers.LogDebug(TAG, "After increasing the scanInterval if necessary, according to the backoffThreshold");

        LogHelpers.LogDebug(TAG, "Current value of scanInterval_: " + scanInterval_);

        String deviceNamesAndAddresses = "";
        for (String address : deviceAddresses_) {
            deviceNamesAndAddresses += "Name: " + bluetoothDeviceObjects_.get(address).getName() + "\n";
            deviceNamesAndAddresses += "Address: " + address + "\n";
        }
        LogHelpers.LogDebug(TAG, "Devices discovered on the last scan of the scan cycle: " + "\n" +
                deviceNamesAndAddresses);

        if (deviceAddresses_.size() == 0)
            noDevicesInRangeOnScanCounter_++;
        else
            noDevicesInRangeOnScanCounter_ = 0;

        deviceAddresses_.clear();

        LogHelpers.LogDebug(TAG, "Got to the end of the updateDeviceRelatedInformation function.");

    }

    private ArrayList<BluetoothDeviceAndScanRecord> checkForDevicesToRemoveAndUpdateMissedScanCounts() {

        ArrayList<BluetoothDeviceAndScanRecord> devicesToRemove = new ArrayList<>();

        for (String deviceAddress : devicesInRangeMissedScanCounters_.keySet()) {

            int deviceMissedScanCount = devicesInRangeMissedScanCounters_.get(deviceAddress);

            if (!deviceAddresses_.contains(deviceAddress)) {
                // increments the deviceMissedScanCount by one for this device, since it wasn't discovered on the last scan
                devicesInRangeMissedScanCounters_.put(deviceAddress,
                        ++deviceMissedScanCount);
            }
            else if (deviceMissedScanCount > 0){
                // resets the missed scan count of devices that were detected on this scan
                devicesInRangeMissedScanCounters_.put(deviceAddress, 0);
            }

            if (deviceMissedScanCount > maxTolerableMissedScans_) {
                try {
                    devicesToRemove.add(new BluetoothDeviceAndScanRecord(bluetoothDeviceObjects_.get(deviceAddress),
                            scanRecords_.get(deviceAddress)));
                }
                catch (Exception e) {
                    Log.e(TAG, "Caught exception while trying to remove device from list of devices considered in range: " +
                        e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return devicesToRemove;

    }

    private boolean checkForValidScanParameters(long scanPeriod, long scanIntervalMin, long scanIntervalMax)
            throws IllegalArgumentException {

        // see https://stackoverflow.com/questions/45681711/app-is-scanning-too-frequently-with-scansettings-scan-mode-opportunistic
        if ((scanPeriod + scanIntervalMin)* MAX_SCAN_STARTS_PER_PREDEFINED_SCAN_MEASUREMENT_PERIOD < PREDEFINED_SCAN_MEASUREMENT_PERIOD)
            throw new IllegalArgumentException("scanPeriod and scanInterval values are too low; try increasing one or both");

        if (scanIntervalMin > scanIntervalMax)
            throw new IllegalArgumentException("scanIntervalMin must be less than or equal to scanIntervalMax");

        return true;
    }

    // function to start scan cycle
    public void startScanCycle () {

        currentlyInScanCycle_ = true;

        deviceAddresses_.clear();
        scanRecords_.clear();
        bluetoothDeviceObjects_.clear();

        noDevicesInRangeOnScanCounter_ = 0;

        scanForBLEDevices();

    }

    // function to stop scan cycle
    public void stopScanCycle() {

        currentlyInScanCycle_ = false;

        bluetoothLeScanner_.stopScan(bleScanCallback);

        handler_.removeCallbacksAndMessages(null);

    }

    public boolean isCurrentlyInScanCycle() {
        return currentlyInScanCycle_;
    }

    public ArrayList<BluetoothDeviceAndScanRecord> getDevicesInRange() {

        return new ArrayList<>(bluetoothDeviceAndScanRecordsForDevicesInRange_.values());

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        LocalBroadcastManager.getInstance(ctx_).unregisterReceiver(startScanIntervalSignalReceiver);
        LocalBroadcastManager.getInstance(ctx_).unregisterReceiver(startScanPeriodSignalReceiver);

    }
}
