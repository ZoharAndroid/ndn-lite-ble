
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.utils;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.util.Blob;

import java.io.IOException;

import NDNLiteSupport.LogHelpers;

import static NDNLiteSupport.LogHelpers.LogByteArrayDebug;

public class OtherHelpers {

    private static final String TAG = OtherHelpers.class.getSimpleName();

    public static void signDataBySymmetricKeyAndPublish(final byte[] key, final Face face, final Data data) {

        LogByteArrayDebug(TAG, "Signing data with key:", key);

        KeyChain.signWithHmacWithSha256(data, new Blob(key));

        class OneShotTask implements Runnable {
            Data data;

            OneShotTask(Data d) {
                data = d;
            }

            public void run() {
                try {
                    LogHelpers.LogDebug(TAG, "Sending data with name: " + data.getName());
                    LogHelpers.LogDebug(TAG, "Length of data sent: " + data.wireEncode().getImmutableArray().length);

                    LogByteArrayDebug(TAG, "Hex string of data being published:", data.wireEncode().getImmutableArray());

                    face.putData(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Thread t = new Thread(new OneShotTask(data));
        t.start();

    }

}
