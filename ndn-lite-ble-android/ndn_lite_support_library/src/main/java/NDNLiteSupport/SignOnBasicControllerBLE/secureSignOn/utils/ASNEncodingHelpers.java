
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.utils;

import org.spongycastle.crypto.DataLengthException;

import java.util.Arrays;

import NDNLiteSupport.LogHelpers;

public class ASNEncodingHelpers {

    public static final String TAG = ASNEncodingHelpers.class.getSimpleName();

    public static final int ASN1_SEQUENCE_TYPE = 0x30;
    public static final int ASN1_INTEGER_TYPE = 0x02;

    public static boolean signedIntegerIsNegative(byte[] signedInteger) {
        if ((signedInteger[0] & 0x80) != 0x00) {
            return true;
        }

        return false;
    }

    public static byte[] asnEncodeInteger(byte[] integer) {

        int asnEncodingLength = integer.length + 2;

        if (signedIntegerIsNegative(integer)) {
            asnEncodingLength += 1;
        }

        byte[] asnEncodedInteger = new byte[asnEncodingLength];

        asnEncodedInteger[0] = ASN1_INTEGER_TYPE;
        asnEncodedInteger[1] = (byte) (asnEncodingLength - 2);

        if (asnEncodingLength > (integer.length + 2)) {
            asnEncodedInteger[2] = 0x00;
            System.arraycopy(integer, 0, asnEncodedInteger, 3, integer.length);
        }
        else {
            System.arraycopy(integer, 0, asnEncodedInteger, 2, integer.length);
        }

        return asnEncodedInteger;

    }

    public static byte[] asnEncodeRawECDSASignature(byte[] rawEcdsaSignature) throws DataLengthException {

        int finalAsnEncodedSignatureLength = 0;

        if (rawEcdsaSignature.length % 2 != 0) {
            throw new DataLengthException("Raw ECDSA signature length passed into asnEncodeRawEcdsaSignature was not even.");
        }

        int rawIntegerLength = rawEcdsaSignature.length / 2;

        byte[] rValue = Arrays.copyOfRange(rawEcdsaSignature, 0, rawIntegerLength);
        LogHelpers.LogByteArrayDebug(TAG, "Value of raw R value in ecdsa signature:", rValue);
        byte[] rValueASNEncoded = asnEncodeInteger(rValue);
        LogHelpers.LogByteArrayDebug(TAG, "Value of R value asn encoded:", rValueASNEncoded);

        byte[] sValue = Arrays.copyOfRange(rawEcdsaSignature, rawIntegerLength, rawEcdsaSignature.length);
        LogHelpers.LogByteArrayDebug(TAG, "Value of raw S value in ecdsa signature:", sValue);
        byte[] sValueASNEncoded = asnEncodeInteger(sValue);
        LogHelpers.LogByteArrayDebug(TAG, "Value of S value asn encoded:", sValueASNEncoded);

        finalAsnEncodedSignatureLength += rValueASNEncoded.length + sValueASNEncoded.length;
        finalAsnEncodedSignatureLength += 2; // to account for ASN 1 sequence tlv type and length

        LogHelpers.LogDebug(TAG, "Value of finalAsnEncodedSignatureLength after asn encoding " +
                "r and s values" + finalAsnEncodedSignatureLength);

        byte[] asnEncodedEcdsaSignature = new byte[finalAsnEncodedSignatureLength];

        asnEncodedEcdsaSignature[0] = ASN1_SEQUENCE_TYPE;
        asnEncodedEcdsaSignature[1] = (byte) (finalAsnEncodedSignatureLength - 2); // subtract two to account for ASN 1 sequence tlv type and length

        System.arraycopy(rValueASNEncoded, 0, asnEncodedEcdsaSignature, 2, rValueASNEncoded.length);
        System.arraycopy(sValueASNEncoded, 0, asnEncodedEcdsaSignature, 2 + rValueASNEncoded.length, sValueASNEncoded.length);

        LogHelpers.LogByteArrayDebug(TAG, "Final asn encoded signature:", asnEncodedEcdsaSignature);

        return asnEncodedEcdsaSignature;
    }

}
