
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport;

import java.util.UUID;

public class NDNLiteAndroidSupportConsts {

    // UUID of the ndn lite ble unicast transport service, which is advertised by all
    // devices of interest (i.e., can undergo the sign on protcool, and receive data from the
    // controller after sign on)
    public static final UUID NDN_LITE_BLE_UNICAST_SERVICE_UUID =
            UUID.fromString("E54B0000-67F5-479E-8711-B3B99198CE6C");

    // the BLEManager can distinguish different send requests by the send code associated with them;
    // I am using it here to distinguish between writes from the SignOnBasicControllerBLE object and the
    // BLEFace object
    public static final int SIGN_ON_CONTROLLER_BLE_SEND_CODE = 1;
    public static final int NDN_LITE_BLE_FACE_SEND_CODE = 2;

}
