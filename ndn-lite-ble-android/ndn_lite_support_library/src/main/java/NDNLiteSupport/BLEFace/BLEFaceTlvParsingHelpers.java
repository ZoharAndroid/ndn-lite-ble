
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.BLEFace;

import android.util.Log;

import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.util.Blob;

import java.nio.ByteBuffer;

import NDNLiteSupport.LogHelpers;

public class BLEFaceTlvParsingHelpers {

    // TAG for logging.
    private static final String TAG = BLEFaceTlvParsingHelpers.class.getSimpleName();

    // https://named-data.net/doc/NDN-packet-spec/current/types.html
    public static final int INTEREST_TLV_TYPE = 0x05;
    public static final int DATA_TLV_TYPE = 0x06;

    enum NDNPacketType {
        INTEREST,
        DATA
    }

    public static class ExtractNdnPacketFromByteArrayResult {
        public ExtractNdnPacketFromByteArrayResult(NDNPacketType packetType, byte[] bytes) {
            this.packetType = packetType;
            this.bytes = bytes;
        }
        NDNPacketType packetType;
        byte[] bytes;
    }

    // given that I can't seem to find an easy way to stop the nRF52840 from sending me the maximum
    // size of the characteristic when it sends me notifications, this is a temporary hack to extract an
    // ndn packet from the longer array so that it can be properly processed
    public static ExtractNdnPacketFromByteArrayResult extractNDNPacketFromByteArray(byte[] arr) {

        int ndnPacketLength = -1;
        NDNPacketType packetType = null;
        int packetTlvValueOffset = -1;

        ByteBuffer byteBuffer = ByteBuffer.wrap(arr);
        TlvDecoder decoder = new TlvDecoder(byteBuffer);
        switch (arr[0]) {
            case INTEREST_TLV_TYPE: {
                try {
                    ndnPacketLength = decoder.readTypeAndLength(INTEREST_TLV_TYPE);
                    packetType = NDNPacketType.INTEREST;
                } catch (EncodingException e) {
                    e.printStackTrace();
                }
                break;
            }
            case DATA_TLV_TYPE: {
                try {
                    ndnPacketLength = decoder.readTypeAndLength(DATA_TLV_TYPE);
                    packetType = NDNPacketType.DATA;
                } catch (EncodingException e) {
                    e.printStackTrace();
                }
                break;
            }
            default: {
                Log.e(TAG, "Got unexpected tlv type: " + arr[0]);
                return null;
            }
        }

        if (ndnPacketLength != -1) {
            // to account for the ndn packet tlv type
            ndnPacketLength += 1;
            // to account for the ndn packet type and length, have to take into account
            // how long the length will be: see https://named-data.net/doc/NDN-packet-spec/current/tlv.html#variable-size-encoding-for-type-t-and-length-l
            int ndnPacketLengthFieldFirstByte = arr[1];
            if (ndnPacketLengthFieldFirstByte < 253) {
                ndnPacketLength += 1; // length field is one byte
            }
            else if (ndnPacketLengthFieldFirstByte == 253) {
                ndnPacketLength += 3; // length field is 3 bytes
            }
            else if (ndnPacketLengthFieldFirstByte == 254) {
                ndnPacketLength += 5; // length field is 5 bytes
            }
            else if (ndnPacketLengthFieldFirstByte == 255) {
                ndnPacketLength += 9; // length field is 9 bytes
            }

            LogHelpers.LogDebug(TAG, "Successfully extracted NDN packet from byte array, tlv type: " + arr[0]);
            byte[] result = new byte[ndnPacketLength];
            System.arraycopy(arr, 0, result, 0, ndnPacketLength);
            LogHelpers.LogDebug(TAG, "Hex string of extracted NDN packet: " + new Blob(result).toHex());
            return new ExtractNdnPacketFromByteArrayResult(
                    packetType,
                    result
            );
        }
        else {
            Log.e(TAG, "Failed to extract NDN packet from byte array.");
            return null;
        }

    }

}
