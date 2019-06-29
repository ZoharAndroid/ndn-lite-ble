
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic;

public class SignOnBasicControllerResultCodes {

    protected enum ParseSignOnMessageResultCode {
        SUCCESS,
        ERROR_PARSING_PACKET_HEADER,
        ERROR_PARSING_DEVICE_IDENTIFIER,
        ERROR_PARSING_DEVICE_CAPABILITIES,
        ERROR_PARSING_N1_PUB,
        ERROR_PARSING_N2_PUB_DIGEST,
        ERROR_PARSING_TRUST_ANCHOR_CERTIFICATE_DIGEST,
        ERROR_PARSING_SIGNATURE,
    }

}
