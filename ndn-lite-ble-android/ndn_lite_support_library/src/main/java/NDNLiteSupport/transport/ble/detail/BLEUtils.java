
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.transport.ble.detail;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

public class BLEUtils {

    private static final String TAG = BLEUtils.class.getSimpleName();

    public enum BUResultCode {
        SUCCESS, // 0

        SERVICE_RETRIEVAL_FAILED,
        CHARACTERISTIC_RETRIEVAL_FAILED,
    }

    // kind of overkill, but see https://stackoverflow.com/questions/356248/best-way-to-return-status-flag-and-message-from-a-method-in-java
    public static class GetCharacteristicResult {

        public BluetoothGattCharacteristic characteristic;
        public BUResultCode resultCode;

        GetCharacteristicResult(BluetoothGattCharacteristic characteristic, BUResultCode resultCode) {
            this.characteristic = characteristic;
            this.resultCode = resultCode;
        }

        GetCharacteristicResult(BUResultCode resultCode) {
            this.resultCode = resultCode;
        }
    }

    public static GetCharacteristicResult getCharacteristic(BluetoothGatt gattObject, String serviceUuid, String characteristicUuid) {

        BluetoothGattService service = gattObject.getService(UUID.fromString(serviceUuid));
        BluetoothGattCharacteristic characteristic;

        if (service == null) {
            Log.e(TAG, "no service found for given service uuid");
            return new GetCharacteristicResult(BUResultCode.SERVICE_RETRIEVAL_FAILED);
        } else {
            characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid));

            if (characteristic == null) {
                Log.e(TAG, "no characteristic found for given characteristic uuid");
                return new GetCharacteristicResult(BUResultCode.CHARACTERISTIC_RETRIEVAL_FAILED);
            }
        }

        return new GetCharacteristicResult(characteristic, BUResultCode.SUCCESS);
    }

    public static boolean checkForCharacteristicProperty(BluetoothGattCharacteristic characteristic,
                                                   int property) {

        final int properties = characteristic.getProperties();

        if ((properties & property) > 0)
            return true;

        return false;

    }

}
