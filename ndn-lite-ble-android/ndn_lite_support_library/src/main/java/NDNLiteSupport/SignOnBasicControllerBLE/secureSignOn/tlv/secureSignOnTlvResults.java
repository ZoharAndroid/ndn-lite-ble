
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv;

public class secureSignOnTlvResults {

    public static class ParseTlvValueResult {

        public ParseTlvValueResult(secureSignOnTlvResultCodes.ParseTlvValueResultCode resultCode, byte[] tlvValue, int tlvLength) {
            this.resultCode = resultCode;
            this.tlvValue = tlvValue;
            this.tlvLength = tlvLength;
        }

        public ParseTlvValueResult(secureSignOnTlvResultCodes.ParseTlvValueResultCode resultCode) {
            this.resultCode = resultCode;
        }

        public int tlvLength;
        public byte[] tlvValue;
        public secureSignOnTlvResultCodes.ParseTlvValueResultCode resultCode;

    }

}
