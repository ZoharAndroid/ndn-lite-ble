
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport;

import android.util.Log;

import net.named_data.jndn.util.Blob;

public class LogHelpers {

    public static void LogDebug(String TAG, String message) {
        Log.d(TAG, message);
    }

    public static void LogWarning(String TAG, String message) { Log.w(TAG, message); }

    public static void LogByteArrayDebug(String TAG, String message, byte[] buffer) {
        Blob bufferBlob = new Blob(buffer);
        String bufferString = bufferBlob.toHex();
        String outputString = "";
        for (int i = 0; i+1 < bufferString.length(); i+=2) {
            outputString += String.valueOf(bufferString.charAt(i));
            outputString += String.valueOf(bufferString.charAt(i+1));
            outputString += " ";
            if (((i+2) % 20 == 0) && i >= 18)
                outputString += "\n";
        }
        LogDebug(TAG, message + "\n" + outputString);
    }

    public static String byteArrayToHexString(byte[] array) {
        StringBuffer hexString = new StringBuffer();
        for (byte b : array) {
            int intVal = b & 0xff;
            if (intVal < 0x10)
                hexString.append("0");
            hexString.append(Integer.toHexString(intVal));
        }
        return hexString.toString();
    }



}
