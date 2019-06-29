
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv;

public class secureSignOnTlvResultCodes {

    public enum ParseTlvValueResultCode {
        SUCCESS,
        PARSING_OF_TLV_LENGTHS_LARGER_THAN_252_NOT_SUPPORTED,
        TLV_VALUE_NOT_FOUND,
        ARRAY_COPY_OF_RETRIEVED_TLV_VALUE_FAILED,
    }

}
