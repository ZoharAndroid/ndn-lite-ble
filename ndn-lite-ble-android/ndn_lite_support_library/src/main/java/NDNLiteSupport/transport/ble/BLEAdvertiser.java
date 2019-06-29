
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

// THIS CLASS IS NOT FINISHED, DO NOT USE IT

package NDNLiteSupport.transport.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.UUID;

import NDNLiteSupport.LogHelpers;

public class BLEAdvertiser {

    public static final String random_service_uuid = "23c3e3e7-2e1e-47e1-9ba2-eaf9c6785a8d";
    public static final String HEART_RATE_SERVICE_UUID = "0000180d-0000-1000-8000-00805f9b34fb";

    private static final String TAG = BLEAdvertiser.class.getSimpleName();

    private Context ctx_;
    private BluetoothManager bluetoothManager_;
    private BluetoothAdapter bluetoothAdapter_;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser_;

    private AdvertisingSet currentAdvertisingSet_;

    public BLEAdvertiser(Context ctx)
            throws NullPointerException, IllegalArgumentException {

        ctx_ = ctx;

        if (bluetoothManager_ == null) {
            bluetoothManager_ = (BluetoothManager) ctx_.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager_ == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                throw new NullPointerException();
            }
        }

        bluetoothAdapter_ = bluetoothManager_.getAdapter();
        if (bluetoothAdapter_ == null) {
            Log.e(TAG, "Unable to obtain a BluetoothbluetoothLeAdvertiser_.");
            throw new NullPointerException();
        }

        bluetoothLeAdvertiser_ = bluetoothAdapter_.getBluetoothLeAdvertiser();
        if (bluetoothLeAdvertiser_ == null) {
            Log.e(TAG, "Unable to obtain a BluetoothLeAdvertiser.");
            throw new NullPointerException();
        }

    }

    public void startAdvertising() {

        // Check if all features are supported
        if (!bluetoothAdapter_.isLe2MPhySupported()) {
            Log.e(TAG, "2M PHY not supported!");
            return;
        }
        if (!bluetoothAdapter_.isLeExtendedAdvertisingSupported()) {
            Log.e(TAG, "LE Extended Advertising not supported!");
            return;
        }

        int maxDataLength = bluetoothAdapter_.getLeMaximumAdvertisingDataLength() - 100;

        LogHelpers.LogDebug(TAG, "Creating testData of length " + maxDataLength);
        int maxDataLengthDividedByTen = maxDataLength / 10;
        LogHelpers.LogDebug(TAG, "Value of max data lenght divided by ten: " + maxDataLengthDividedByTen);

        byte[] testData = new byte[maxDataLength];
        byte[] testPattern = new byte[] { 0x01, 0x03, 0x05, 0x07, 0x09, 0x00, 0x02, 0x04, 0x06, 0x08 };
        for (int i = 0; i  < maxDataLength; i++) {
            testData[i] = (byte) (i / maxDataLengthDividedByTen + 1);
        }
        byte arbitraryMarker = (byte) 0x0b;
        testData[20] = arbitraryMarker;
        testData[31] = arbitraryMarker;
        testData[50] = arbitraryMarker;

        LogHelpers.LogByteArrayDebug(TAG, "Contents of testData: ", testData);

        AdvertisingSetParameters.Builder parameters = (new AdvertisingSetParameters.Builder())
                .setLegacyMode(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
                .setSecondaryPhy(BluetoothDevice.PHY_LE_1M);

        AdvertiseData data = (new AdvertiseData.Builder()).addServiceData(new
                        ParcelUuid(UUID.fromString(HEART_RATE_SERVICE_UUID)),
                        testData
        ).build();

        AdvertisingSetCallback callback = new AdvertisingSetCallback() {
            @Override
            public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                Log.i(TAG, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                        + status);
                currentAdvertisingSet_ = advertisingSet;

                currentAdvertisingSet_.enableAdvertising(true, 30000, 255);

            }

            @Override
            public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
                Log.i(TAG, "onAdvertisingSetStopped():");
            }

            @Override
            public void onAdvertisingEnabled(AdvertisingSet advertisingSet, boolean enable, int status) {
                super.onAdvertisingEnabled(advertisingSet, enable, status);

                Log.i(TAG, "Advertising enabled.");
            }
        };

        bluetoothLeAdvertiser_.startAdvertisingSet(parameters.build(), data, null, null, null, callback);

        // bluetoothLeAdvertiser_.startAdvertising(buildAdvertiseSettings(), buildAdvertiseData(), new SampleAdvertiseCallback());

//        // After the set starts, you can modify the data and parameters of currentAdvertisingSet_.
//        currentAdvertisingSet_.setAdvertisingData((new
//                AdvertiseData.Builder()).addServiceData(new ParcelUuid(UUID.randomUUID()),
//                ("Without disabling the advertiser first, you can set the data, if new data is " +
//                        "less than 251 bytes long.").getBytes()).build());
//
//        // Wait for onAdvertisingDataSet callback...
//
//        // Can also stop and restart the advertising
//        currentAdvertisingSet_.enableAdvertising(false, 0, 0);
//        // Wait for onAdvertisingEnabled callback...
//        currentAdvertisingSet_.enableAdvertising(true, 0, 0);
//        // Wait for onAdvertisingEnabled callback...
//
//        // Or modify the parameters - i.e. lower the tx power
//        currentAdvertisingSet_.enableAdvertising(false, 0, 0);
//        // Wait for onAdvertisingEnabled callback...
//        currentAdvertisingSet_.setAdvertisingParameters(parameters.setTxPowerLevel
//                (AdvertisingSetParameters.TX_POWER_LOW).build());
//        // Wait for onAdvertisingParametersUpdated callback...
//        currentAdvertisingSet_.enableAdvertising(true, 0, 0);
//        // Wait for onAdvertisingEnabled callback...
//
//        // When done with the advertising:
//        advertiser.stopAdvertisingSet(callback);

    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private AdvertiseData buildAdvertiseData() {

        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        //dataBuilder.addServiceUuid(ParcelUuid.fromString(UUID.randomUUID().toString()));
        dataBuilder.setIncludeDeviceName(true);

        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());

        return dataBuilder.build();
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class SampleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            LogHelpers.LogDebug(TAG, "Advertising failed");

        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            LogHelpers.LogDebug(TAG, "Advertising successfully started");
        }
    }

}
