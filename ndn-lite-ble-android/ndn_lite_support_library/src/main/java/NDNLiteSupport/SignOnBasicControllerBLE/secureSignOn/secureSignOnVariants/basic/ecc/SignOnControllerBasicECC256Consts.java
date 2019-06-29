
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.ecc;

public class SignOnControllerBasicECC256Consts {

    public static final int KD_PUBLIC_KEY_LENGTH = 64; // length of the "raw" formatted KD public key
    public static final int KD_PRIVATE_KEY_LENGTH = 32; // length of the "raw" formatted KD private key
    public static final int ECDH_PUBLIC_KEY_LENGTH = 64; // length of the "raw" formatted ECDH public key
    public static final int ECDH_PRIVATE_KEY_LENGTH = 32; // length of the "raw" formatted ECDH private key
    public static final String ECDH_CURVE = "secp256r1"; // curve used to generate keys for ECDH
    public static final String KD_CURVE = "secp256r1"; // curve used to generate the KD key pair
    public static final String KS_CURVE = "secp256r1"; // curve of the KS key pair
    public static final int SECURE_SIGN_ON_CODE_SIZE = 16; // size of the secure sign on code

}
