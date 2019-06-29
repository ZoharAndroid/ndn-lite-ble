
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv;

public class secureSignOnTlvConsts {

    // since the implementation assumes no TLV block will exceed length 252
    public static final int SECURE_SIGN_ON_TLV_TYPE_SIZE = 1;
    public static final int SECURE_SIGN_ON_TLV_LENGTH_SIZE = 1;
    public static final int SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE =
            (SECURE_SIGN_ON_TLV_TYPE_SIZE + SECURE_SIGN_ON_TLV_LENGTH_SIZE);
    public static final int SECURE_SIGN_ON_MAX_TLV_LENGTH = 252;

    public static final int SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET = 138;

    // sign-on TLV types for basic
    public static final int SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_RESPONSE_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET;
    public static final int SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_RESPONSE_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 1;
    public static final int SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 2;
    public static final int SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 3;

    public static final int SECURE_SIGN_ON_BLE_DEVICE_IDENTIFIER_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 4;
    public static final int SECURE_SIGN_ON_BLE_DEVICE_CAPABILITIES_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 5;
    public static final int SECURE_SIGN_ON_BLE_N1_PUB_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 6;

    public static final int SECURE_SIGN_ON_BLE_SIGNATURE_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 7;

    public static final int SECURE_SIGN_ON_BLE_N2_PUB_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 8;
    public static final int SECURE_SIGN_ON_BLE_ANCHOR_CERTIFICATE_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 9;

    public static final int SECURE_SIGN_ON_BLE_TRUST_ANCHOR_CERTIFICATE_DIGEST_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 10;
    public static final int SECURE_SIGN_ON_BLE_N2_PUB_DIGEST_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 11;

    public static final int SECURE_SIGN_ON_BLE_KD_PRI_ENCRYPTED_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 12;
    public static final int SECURE_SIGN_ON_BLE_KD_PUB_CERTIFICATE_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 13;

    public static final int SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE = SECURE_SIGN_ON_BLE_BASE_TLV_OFFSET + 14;

}
