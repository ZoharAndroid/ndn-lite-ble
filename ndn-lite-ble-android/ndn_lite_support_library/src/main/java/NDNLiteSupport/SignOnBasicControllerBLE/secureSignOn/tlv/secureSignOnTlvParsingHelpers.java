
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv;

import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvConsts.SECURE_SIGN_ON_MAX_TLV_LENGTH;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvResults.ParseTlvValueResult;

public class secureSignOnTlvParsingHelpers {

    // tag for logging
    private static String TAG = secureSignOnTlvParsingHelpers.class.getSimpleName();

    // - takes in a tlv block, and tries to find the value of the specified tlv type in that block
    // - it expects to be passed in a tlv block with no nested tlv's, i.e. it will not account for tlv blocks
    // within the values of tlv's
    // - will set value of retrievedTlvValue to the value of the tlv block it finds if successful
    // - parses according to the TLV encoding rules of NDN (https://named-data.net/doc/NDN-packet-spec/current/tlv.html#variable-size-encoding-for-type-t-and-length-l_
    //      BUT assumes that there are no lengths larger than 252
    public static ParseTlvValueResult parseTlvValue(byte[] tlvBlock,
                                                    int tlvTypeToFindValueOf) {

        int i = 0;

        while (i < tlvBlock.length) {
            int currentTlvType = Byte.toUnsignedInt(tlvBlock[i]);
            int currentTlvLength = Byte.toUnsignedInt(tlvBlock[i+1]);
            // LogHelpers.LogDebug(TAG, "Current tlv type: " + currentTlvType);
            // LogHelpers.LogDebug(TAG, "Current tlv length: " + currentTlvLength);
            if (currentTlvLength > SECURE_SIGN_ON_MAX_TLV_LENGTH) {
                return new ParseTlvValueResult(
                        secureSignOnTlvResultCodes.ParseTlvValueResultCode.PARSING_OF_TLV_LENGTHS_LARGER_THAN_252_NOT_SUPPORTED
                );
            }
            if (currentTlvType == tlvTypeToFindValueOf) {
                byte[] retrievedTlvValue = new byte[currentTlvLength];
                try {
                    System.arraycopy(tlvBlock, i + SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE, retrievedTlvValue, 0, currentTlvLength);
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                    return new ParseTlvValueResult(
                            secureSignOnTlvResultCodes.ParseTlvValueResultCode.ARRAY_COPY_OF_RETRIEVED_TLV_VALUE_FAILED
                    );
                }
                return new ParseTlvValueResult(
                        secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS,
                        retrievedTlvValue,
                        currentTlvLength
                );
            }
            else {
                i += SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE + currentTlvLength;
            }
        }

        return new ParseTlvValueResult(
                    secureSignOnTlvResultCodes.ParseTlvValueResultCode.TLV_VALUE_NOT_FOUND
        );
    }

}
