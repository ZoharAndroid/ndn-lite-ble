
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn;

public class TempCertificate {

        // just a very simple temporary certificate format:
        //    ***********************************
        //    * Name length (2 bytes)           *
        //    * Name (variable length, < 32767) *
        //    * Key length (2 bytes)            *
        //    * Key (variable length, < 32767)  *
        //    ***********************************

        short nameLength;
        String name;
        short keyLength;
        byte[] key;

        public TempCertificate(String name, byte[] key) {
            this.name = name;
            this.nameLength = (short) name.length();
            this.key = key;
            this.keyLength = (short) key.length;
        }

        public byte[] getRawPublicKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public byte[] wireEncode() {
            byte[] wireEncoded = new byte[2 + nameLength + 2 + keyLength];
            byte[] shortToByteArray = new byte[2];

            int currentOffset = 0;

            // copy over the name length (network byte order, big endian)
            shortToByteArray[1] = (byte)(nameLength & 0xff);
            shortToByteArray[0] = (byte)((nameLength >> 8) & 0xff);
            System.arraycopy(shortToByteArray, 0, wireEncoded, currentOffset, 2);
            // copy over the name
            currentOffset += 2; // add the 2 for the name length
            System.arraycopy(name.getBytes(), 0, wireEncoded, currentOffset, name.getBytes().length);
            // copy over the key length (network byte order, big endian)
            shortToByteArray[1] = (byte)(keyLength & 0xff);
            shortToByteArray[0] = (byte)((keyLength >> 8) & 0xff);
            currentOffset += name.getBytes().length; // add the name length
            System.arraycopy(shortToByteArray, 0, wireEncoded, currentOffset, 2);
            // copy over the key
            currentOffset += 2; // add the 2 for the key length
            System.arraycopy(key, 0, wireEncoded, currentOffset, keyLength);

            return wireEncoded;
        }

}
