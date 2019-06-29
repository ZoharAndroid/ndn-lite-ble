
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.utils;

import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.UnrecognizedKeyFormatException;
import net.named_data.jndn.util.Blob;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateKey;
import org.spongycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.util.encoders.Base64;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import NDNLiteSupport.LogHelpers;

import static NDNLiteSupport.LogHelpers.LogByteArrayDebug;

public class SecurityHelpers {

    private final static String TAG = SecurityHelpers.class.getSimpleName();

    private static final byte UNCOMPRESSED_POINT_INDICATOR = 0x04;

    public static byte[] sha256(byte[] payload) throws NoSuchAlgorithmException {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw exception;
        }

        sha256.update(payload);

        return sha256.digest();
    }

    public static byte[] hmacSha256(byte[] key, byte[] message)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = null;
        byte[] mac;
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key, "HmacSHA256");
            sha256_HMAC.init(secret_key);
            mac = sha256_HMAC.doFinal(message);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (InvalidKeyException e) {
            throw e;
        }

        return mac;
    }

    public static boolean checkBuffersMatch(byte[] buf1, byte[] buf2) {

        if (buf1.length != buf2.length)
            return false;

        for (int i = 0; i < buf1.length; i++) {
            if (buf1[i] != buf2[i])
                return false;
        }

        return true;
    }

    public static byte[] asnEncodeRawECPublicKeyBytes(byte[] rawPublicKeyBytes) throws Exception {

        byte[] pubKeyASNHeader = null;

        if (rawPublicKeyBytes.length == 64) {
            pubKeyASNHeader = new byte[]{
                    0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2A, (byte) 0x86, 0x48, (byte) 0xCE,
                    0x3D, 0x02, 0x01, 0x06, 0x08, 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D,
                    0x03, 0x01, 0x07, 0x03, 0x42, 0x00, 0x04
            };
        } else if (rawPublicKeyBytes.length == 48) {
            pubKeyASNHeader = new byte[]{
                    0x30, 0x49, 0x30, 0x13, 0x06, 0x07, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x02,
                    0x01, 0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x03, 0x01, 0x01, 0x03,
                    0x32, 0x00, 0x04
            };
        } else if (rawPublicKeyBytes.length == 40) {
            pubKeyASNHeader = new byte[]{
                    0x30, 0x3e, 0x30, 0x10, 0x06, 0x07, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x02, 0x01, 0x06,
                    0x05, 0x2b, (byte) 0x81, 0x04, 0x00, 0x08, 0x03, 0x2a, 0x00, 0x04
            };

        } else {
            throw new Exception("Currently only asn encoding of 64 byte / 48 byte / 40 byte raw public keys is supported.");
        }

        byte[] pubKeyASNEncoded = new byte[rawPublicKeyBytes.length + pubKeyASNHeader.length];
        System.arraycopy(pubKeyASNHeader, 0, pubKeyASNEncoded, 0, pubKeyASNHeader.length);
        System.arraycopy(rawPublicKeyBytes, 0, pubKeyASNEncoded, pubKeyASNHeader.length, rawPublicKeyBytes.length);

        return pubKeyASNEncoded;
    }

    // security helper function specific to ecdh implementation

    public static KeyPair generateECKeyPair(String curve) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(curve);
        KeyPairGenerator g = null;
        try {
            g = KeyPairGenerator.getInstance("EC");
            g.initialize(ecSpec, new SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return g.generateKeyPair();
    }

    public static PublicKey convertASNEncodedPublicKeyToPublicKeyObject(byte[] ASNEncodedpubKey, String algorithm)
            throws InvalidKeySpecException, NoSuchAlgorithmException {

        PublicKey pubKey;
        try {
            pubKey = KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(ASNEncodedpubKey));
        } catch (InvalidKeySpecException e) {
            throw e;
        } catch (NoSuchAlgorithmException e) {
            throw e;
        }

        return pubKey;
    }

    public static byte[] deriveSharedSecretECDH(PublicKey pubKey, PrivateKey priKey)
            throws NoSuchAlgorithmException, InvalidKeyException, NullPointerException {

        if (pubKey == null)
            throw new NullPointerException("Pubkey was null.");

        if (priKey == null)
            throw new NullPointerException("Prikey was null.");

        KeyAgreement aKeyAgreement = null;
        try {
            aKeyAgreement = KeyAgreement.getInstance("ECDH");
        } catch (NoSuchAlgorithmException e) {
            throw e;
        }

        try {
            aKeyAgreement.init(priKey);
        } catch (InvalidKeyException e) {
            throw e;
        }

        try {
            aKeyAgreement.doPhase(pubKey, true);
        } catch (InvalidKeyException e) {
            return null;
        }

        byte[] sharedSecret = aKeyAgreement.generateSecret();

        if (sharedSecret == null)
            throw new NullPointerException("Attempted to generate shared secret, got null array.");

        return sharedSecret;
    }

    public static String getEcPublicKeyAsHex(PublicKey publicKey, int ecPubKeyLength, int ecPriKeyLength) {

        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        ECPoint ecPoint = ecPublicKey.getW();

        LogHelpers.LogDebug(TAG, "Length of x: " + ecPoint.getAffineX().toByteArray().length);
        LogHelpers.LogDebug(TAG, "Length of y: " + ecPoint.getAffineY().toByteArray().length);

        byte[] publicKeyBytes = new byte[ecPubKeyLength];
        writeToStream(publicKeyBytes, 0, ecPoint.getAffineX(), ecPriKeyLength);
        writeToStream(publicKeyBytes, ecPriKeyLength, ecPoint.getAffineY(), ecPriKeyLength);

        Blob hexBlob = new Blob(publicKeyBytes);
        String hex = hexBlob.toHex();

        return hex;
    }

    public static String getEcPrivateKeyAsHex(PrivateKey privateKey) {

        ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
        BigInteger ecPoint = ecPrivateKey.getS();

        byte[] privateKeyBytes = ecPoint.toByteArray();

        Blob hexBlob = new Blob(privateKeyBytes);
        String hex = hexBlob.toHex();

        if (hex.substring(0, 2).equals("00")) {
            return hex.substring(2);
        }

        return hex;

    }

    public static byte[] encryptAesCbcPkcs5Padding(byte[] data, byte[] keyBytes) throws Exception {
        try {
            LogByteArrayDebug(TAG, "Bytes being encrypted: ", data);
            //Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            Cipher cipher = Cipher.getInstance("AES_128/CBC/NoPadding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            byte[] empty = new byte[16];
            for (int i = 0; i < 16; i++) {
                empty[i] = 0;
            }
            IvParameterSpec ivps = new IvParameterSpec(empty);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivps);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw e;
        }
    }

    private static void writeToStream(byte[] stream, int start, BigInteger value, int size) {
        byte[] data = value.toByteArray();
        int length = Math.min(size, data.length);
        int writeStart = start + size - length;
        int readStart = data.length - length;
        System.arraycopy(data, readStart, stream, writeStart, length);
    }

    public static boolean verifySignatureECC(PublicKey publicKey, byte[] signature, byte[] payload) {
        net.named_data.jndn.security.certificate.PublicKey ndnPublicKey = null;
        try {
            ndnPublicKey = new
                    net.named_data.jndn.security.certificate.PublicKey(new Blob(publicKey.getEncoded()));
        } catch (UnrecognizedKeyFormatException e) {
            e.printStackTrace();
            return false;
        }

        ByteBuffer buffer = ByteBuffer.wrap(payload);

        LogByteArrayDebug(TAG, "Input over which signature is being verified: ", buffer.array());

        if (!VerificationHelpersCustom.verifySignature(buffer, signature, ndnPublicKey))
            return false;

        return true;
    }

    public static boolean verifyApplicationLevelInterestAsymmetricSignatureECC
            (PublicKey publicKey, Interest interest) {

        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new Error("MessageDigest: SHA-256 is not supported: " + exception.getMessage());
        }

        Name interestNameWithoutLastComponent = interest.getName().getPrefix(interest.getName().size() - 1);
        sha256.update(interestNameWithoutLastComponent.wireEncode().getImmutableArray());
        byte[] digestOfName = sha256.digest();
        Blob digestOfNameBlob = new Blob(digestOfName);
        LogHelpers.LogDebug(TAG, "Hex string of signed portion hash: " + digestOfNameBlob.toHex());

        byte[] interestSignature = interest.getName().get(-1).getValue().getImmutableArray();
        Blob interestSignatureBlob = new Blob(interestSignature);
        LogHelpers.LogDebug(TAG, "Hex string of interest signature: " + interestSignatureBlob.toHex());

        return verifySignatureECC(publicKey, interest.getName().get(-1).getValue().getImmutableArray(),
                digestOfName);
    }

    public static boolean verifyApplicationLevelInterestAsymmetricSignatureRSA(byte[] signed, PublicKey publicKey,
                                                                               byte[] signature) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {

        Signature sig = null;
        try {
            sig = Signature.getInstance("SHA256WithRSA");
            sig.initVerify(publicKey);
            sig.update(signed);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (SignatureException e) {
            throw e;
        } catch (InvalidKeyException e) {
            throw e;
        }

        Blob signatureBytesBlob = new Blob(signature);

        try {
            return sig.verify(signature);
        } catch (SignatureException e) {
            throw e;
        }
    }

    public static PrivateKey getPemPrivateKey(String privKeyPEM, String algorithm) throws Exception {

        if (algorithm.equals("RSA")) {
            privKeyPEM = privKeyPEM.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
            privKeyPEM = privKeyPEM.replace("-----END RSA PRIVATE KEY-----\n", "");

            LogHelpers.LogDebug(TAG, "PriKeyPEM: " + privKeyPEM);

            Base64 b64 = new Base64();
            byte[] decoded = b64.decode(privKeyPEM);

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            return kf.generatePrivate(spec);
        }
        else if (algorithm.equals("EC")) {
            privKeyPEM = privKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
            privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----\n", "");

            LogHelpers.LogDebug(TAG, "PriKeyPEM: " + privKeyPEM);

            Base64 b64 = new Base64();
            byte[] decoded = b64.decode(privKeyPEM);

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            return kf.generatePrivate(spec);
        }
        else {
            throw new Exception("Unrecognized algorithm: " + algorithm);
        }


    }

    public static PublicKey getPemPublicKey(String publicKeyPEM, String algorithm) throws Exception {

        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----\n", "");

        LogHelpers.LogDebug(TAG, "PublicKeyPEM: " + publicKeyPEM);

        Base64 b64 = new Base64();
        byte[] decoded = b64.decode(publicKeyPEM);

        X509EncodedKeySpec spec =
                new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePublic(spec);
    }

    public static DHParameterSpec loadPemDHParameters(String dhParametersPEM, int randExpBitSize) throws Exception {

        dhParametersPEM = dhParametersPEM.replace("-----BEGIN DH PARAMETERS-----\n", "");
        dhParametersPEM = dhParametersPEM.replace("-----END DH PARAMETERS-----", "");

        LogHelpers.LogDebug(TAG, "DhParametersPEM: " + dhParametersPEM);

        Base64 b64 = new Base64();
        byte[] decoded = b64.decode(dhParametersPEM);

        ASN1InputStream inputStream = new ASN1InputStream(decoded);
        ASN1Sequence PandG = (ASN1Sequence) inputStream.readObject();
        BigInteger P = new BigInteger(PandG.getObjectAt(0).toString());
        BigInteger G = new BigInteger(PandG.getObjectAt(1).toString());

        LogHelpers.LogDebug(TAG, "Value of P: " + P);
        LogHelpers.LogDebug(TAG, "Value of G: " + G);

        DHParameterSpec spec = new DHParameterSpec(P, G, randExpBitSize);

        return spec;

    }

    public static byte[] asnEncodeRawDHPublicKeyBytes(byte[] rawDHPublicKeyBytesUnedited) throws Exception {

        LogHelpers.LogDebug(TAG, "Length of raw dH public key bytes unedited:" + rawDHPublicKeyBytesUnedited.length);

        byte rawDHPublicKeyBytes[] = null;

        // logic to add padding for 0 byte, since mbedtls ignores the 0 padding for ASN.1 / BER encoding of integers, which
        // requires that the leading bit always be 0
        if ((rawDHPublicKeyBytesUnedited.length == 384 || rawDHPublicKeyBytesUnedited.length == 128) &&
                (rawDHPublicKeyBytesUnedited[0] & 0x80) != 0) {
            rawDHPublicKeyBytes = new byte[rawDHPublicKeyBytesUnedited.length + 1];
            System.arraycopy(rawDHPublicKeyBytesUnedited, 0, rawDHPublicKeyBytes, 1, rawDHPublicKeyBytesUnedited.length);
            rawDHPublicKeyBytes[0] = 0x00;
        } else {
            rawDHPublicKeyBytes = new byte[rawDHPublicKeyBytesUnedited.length];
            System.arraycopy(rawDHPublicKeyBytesUnedited, 0, rawDHPublicKeyBytes, 0, rawDHPublicKeyBytesUnedited.length);
        }

        byte[] asnEncodedKey = null;
        if (rawDHPublicKeyBytes.length == 385) {
            asnEncodedKey = new byte[DH3072PublicKeyWith0PadASNHeader.length + rawDHPublicKeyBytes.length];
            System.arraycopy(DH3072PublicKeyWith0PadASNHeader, 0, asnEncodedKey, 0, DH3072PublicKeyWith0PadASNHeader.length);
            System.arraycopy(rawDHPublicKeyBytes, 0, asnEncodedKey, DH3072PublicKeyWith0PadASNHeader.length, rawDHPublicKeyBytes.length);
        } else if (rawDHPublicKeyBytes.length == 384) {
            asnEncodedKey = new byte[DH3072PublicKeyNo0PadASNHeader.length + rawDHPublicKeyBytes.length];
            System.arraycopy(DH3072PublicKeyNo0PadASNHeader, 0, asnEncodedKey, 0, DH3072PublicKeyNo0PadASNHeader.length);
            System.arraycopy(rawDHPublicKeyBytes, 0, asnEncodedKey, DH3072PublicKeyNo0PadASNHeader.length, rawDHPublicKeyBytes.length);
        } else if (rawDHPublicKeyBytes.length == 129) {
            asnEncodedKey = new byte[DH1024PublicKeyWith0PadASNHeader.length + rawDHPublicKeyBytes.length];
            System.arraycopy(DH1024PublicKeyWith0PadASNHeader, 0, asnEncodedKey, 0, DH1024PublicKeyWith0PadASNHeader.length);
            System.arraycopy(rawDHPublicKeyBytes, 0, asnEncodedKey, DH1024PublicKeyWith0PadASNHeader.length, rawDHPublicKeyBytes.length);
        } else if (rawDHPublicKeyBytes.length == 128) {
            asnEncodedKey = new byte[DH1024PublicKeyNo0PadASNHeader.length + rawDHPublicKeyBytes.length];
            System.arraycopy(DH1024PublicKeyNo0PadASNHeader, 0, asnEncodedKey, 0, DH1024PublicKeyNo0PadASNHeader.length);
            System.arraycopy(rawDHPublicKeyBytes, 0, asnEncodedKey, DH1024PublicKeyNo0PadASNHeader.length, rawDHPublicKeyBytes.length);
        } else {
            LogHelpers.LogDebug(TAG, "problem, got unexpected raw DH public key bytes length: " + rawDHPublicKeyBytes.length);
            Exception e = new Exception("Unexpected DH raw public key bytes length: " + rawDHPublicKeyBytes.length);
            e.printStackTrace();
            throw e;
        }

        return asnEncodedKey;
    }

    public static KeyPair generateDHKeyPair(String DHParametersPEM, int dhPrivateExponentBitsSize) throws Exception {
        DHParameterSpec spec = null;
        try {
            spec = loadPemDHParameters(DHParametersPEM, dhPrivateExponentBitsSize);
        } catch (Exception e) {
            throw e;
        }

        KeyPairGenerator keyGen = null;
        try {
            keyGen = KeyPairGenerator.getInstance("DH");
            try {
                keyGen.initialize(spec, new SecureRandom());
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return keyGen.generateKeyPair();
    }

    public static byte[] deriveSharedSecretDH(PublicKey pubKey, PrivateKey priKey)
            throws NoSuchAlgorithmException, InvalidKeyException {

        KeyAgreement sharedSecretGenerator = null;
        try {
            sharedSecretGenerator = KeyAgreement.getInstance("DH");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] sharedSecret = null;
        try {
            sharedSecretGenerator.init(priKey);
            sharedSecretGenerator.doPhase(pubKey, true);
            sharedSecret = sharedSecretGenerator.generateSecret();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return sharedSecret = sharedSecretGenerator.generateSecret();
    }

    public static KeyPair generateBCRSAKeyPair(int keySize) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(keySize);
        KeyPair keyPair = kpg.genKeyPair();
        return keyPair;
    }

    public static String getBCRSAPublicKeyAsHex(PublicKey publicKey) {

        BCRSAPublicKey rsaPublicKey = (BCRSAPublicKey) publicKey;
        BigInteger publicExponent = rsaPublicKey.getPublicExponent();

        Blob hexBlob = new Blob(publicExponent.toByteArray());
        String hex = hexBlob.toHex();

        return hex;
    }

    public static String getBCRSAPrivateKeyAsHex(PrivateKey privateKey) {
        BCRSAPrivateKey rsaPrivateKey = (BCRSAPrivateKey) privateKey;
        BigInteger privateExponent = rsaPrivateKey.getPrivateExponent();

        byte[] privateKeyBytes = privateExponent.toByteArray();

        Blob hexBlob = new Blob(privateKeyBytes);
        String hex = hexBlob.toHex();

        return hex;
    }

    public static byte[] asnEncodeRawRSAPublicKeyBytes(byte[] rawRSAPublicKeyBytes) {
        return rawRSAPublicKeyBytes;
    }

    // HACK HACK HACK HACK VERY BAD
    private static byte[] DH3072PublicKeyNo0PadASNHeader = new byte[]{
            (byte) 0x30, (byte) 0x82, (byte) 0x03, (byte) 0x28, (byte) 0x30, (byte) 0x82, (byte) 0x01, (byte) 0x9b, (byte) 0x06, (byte) 0x09,
            (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x30,
            (byte) 0x82, (byte) 0x01, (byte) 0x8c, (byte) 0x02, (byte) 0x82, (byte) 0x01, (byte) 0x81, (byte) 0x00, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xc9, (byte) 0x0f, (byte) 0xda, (byte) 0xa2,
            (byte) 0x21, (byte) 0x68, (byte) 0xc2, (byte) 0x34, (byte) 0xc4, (byte) 0xc6, (byte) 0x62, (byte) 0x8b, (byte) 0x80, (byte) 0xdc,
            (byte) 0x1c, (byte) 0xd1, (byte) 0x29, (byte) 0x02, (byte) 0x4e, (byte) 0x08, (byte) 0x8a, (byte) 0x67, (byte) 0xcc, (byte) 0x74,
            (byte) 0x02, (byte) 0x0b, (byte) 0xbe, (byte) 0xa6, (byte) 0x3b, (byte) 0x13, (byte) 0x9b, (byte) 0x22, (byte) 0x51, (byte) 0x4a,
            (byte) 0x08, (byte) 0x79, (byte) 0x8e, (byte) 0x34, (byte) 0x04, (byte) 0xdd, (byte) 0xef, (byte) 0x95, (byte) 0x19, (byte) 0xb3,
            (byte) 0xcd, (byte) 0x3a, (byte) 0x43, (byte) 0x1b, (byte) 0x30, (byte) 0x2b, (byte) 0x0a, (byte) 0x6d, (byte) 0xf2, (byte) 0x5f,
            (byte) 0x14, (byte) 0x37, (byte) 0x4f, (byte) 0xe1, (byte) 0x35, (byte) 0x6d, (byte) 0x6d, (byte) 0x51, (byte) 0xc2, (byte) 0x45,
            (byte) 0xe4, (byte) 0x85, (byte) 0xb5, (byte) 0x76, (byte) 0x62, (byte) 0x5e, (byte) 0x7e, (byte) 0xc6, (byte) 0xf4, (byte) 0x4c,
            (byte) 0x42, (byte) 0xe9, (byte) 0xa6, (byte) 0x37, (byte) 0xed, (byte) 0x6b, (byte) 0x0b, (byte) 0xff, (byte) 0x5c, (byte) 0xb6,
            (byte) 0xf4, (byte) 0x06, (byte) 0xb7, (byte) 0xed, (byte) 0xee, (byte) 0x38, (byte) 0x6b, (byte) 0xfb, (byte) 0x5a, (byte) 0x89,
            (byte) 0x9f, (byte) 0xa5, (byte) 0xae, (byte) 0x9f, (byte) 0x24, (byte) 0x11, (byte) 0x7c, (byte) 0x4b, (byte) 0x1f, (byte) 0xe6,
            (byte) 0x49, (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xec, (byte) 0xe4, (byte) 0x5b, (byte) 0x3d, (byte) 0xc2, (byte) 0x00,
            (byte) 0x7c, (byte) 0xb8, (byte) 0xa1, (byte) 0x63, (byte) 0xbf, (byte) 0x05, (byte) 0x98, (byte) 0xda, (byte) 0x48, (byte) 0x36,
            (byte) 0x1c, (byte) 0x55, (byte) 0xd3, (byte) 0x9a, (byte) 0x69, (byte) 0x16, (byte) 0x3f, (byte) 0xa8, (byte) 0xfd, (byte) 0x24,
            (byte) 0xcf, (byte) 0x5f, (byte) 0x83, (byte) 0x65, (byte) 0x5d, (byte) 0x23, (byte) 0xdc, (byte) 0xa3, (byte) 0xad, (byte) 0x96,
            (byte) 0x1c, (byte) 0x62, (byte) 0xf3, (byte) 0x56, (byte) 0x20, (byte) 0x85, (byte) 0x52, (byte) 0xbb, (byte) 0x9e, (byte) 0xd5,
            (byte) 0x29, (byte) 0x07, (byte) 0x70, (byte) 0x96, (byte) 0x96, (byte) 0x6d, (byte) 0x67, (byte) 0x0c, (byte) 0x35, (byte) 0x4e,
            (byte) 0x4a, (byte) 0xbc, (byte) 0x98, (byte) 0x04, (byte) 0xf1, (byte) 0x74, (byte) 0x6c, (byte) 0x08, (byte) 0xca, (byte) 0x18,
            (byte) 0x21, (byte) 0x7c, (byte) 0x32, (byte) 0x90, (byte) 0x5e, (byte) 0x46, (byte) 0x2e, (byte) 0x36, (byte) 0xce, (byte) 0x3b,
            (byte) 0xe3, (byte) 0x9e, (byte) 0x77, (byte) 0x2c, (byte) 0x18, (byte) 0x0e, (byte) 0x86, (byte) 0x03, (byte) 0x9b, (byte) 0x27,
            (byte) 0x83, (byte) 0xa2, (byte) 0xec, (byte) 0x07, (byte) 0xa2, (byte) 0x8f, (byte) 0xb5, (byte) 0xc5, (byte) 0x5d, (byte) 0xf0,
            (byte) 0x6f, (byte) 0x4c, (byte) 0x52, (byte) 0xc9, (byte) 0xde, (byte) 0x2b, (byte) 0xcb, (byte) 0xf6, (byte) 0x95, (byte) 0x58,
            (byte) 0x17, (byte) 0x18, (byte) 0x39, (byte) 0x95, (byte) 0x49, (byte) 0x7c, (byte) 0xea, (byte) 0x95, (byte) 0x6a, (byte) 0xe5,
            (byte) 0x15, (byte) 0xd2, (byte) 0x26, (byte) 0x18, (byte) 0x98, (byte) 0xfa, (byte) 0x05, (byte) 0x10, (byte) 0x15, (byte) 0x72,
            (byte) 0x8e, (byte) 0x5a, (byte) 0x8a, (byte) 0xaa, (byte) 0xc4, (byte) 0x2d, (byte) 0xad, (byte) 0x33, (byte) 0x17, (byte) 0x0d,
            (byte) 0x04, (byte) 0x50, (byte) 0x7a, (byte) 0x33, (byte) 0xa8, (byte) 0x55, (byte) 0x21, (byte) 0xab, (byte) 0xdf, (byte) 0x1c,
            (byte) 0xba, (byte) 0x64, (byte) 0xec, (byte) 0xfb, (byte) 0x85, (byte) 0x04, (byte) 0x58, (byte) 0xdb, (byte) 0xef, (byte) 0x0a,
            (byte) 0x8a, (byte) 0xea, (byte) 0x71, (byte) 0x57, (byte) 0x5d, (byte) 0x06, (byte) 0x0c, (byte) 0x7d, (byte) 0xb3, (byte) 0x97,
            (byte) 0x0f, (byte) 0x85, (byte) 0xa6, (byte) 0xe1, (byte) 0xe4, (byte) 0xc7, (byte) 0xab, (byte) 0xf5, (byte) 0xae, (byte) 0x8c,
            (byte) 0xdb, (byte) 0x09, (byte) 0x33, (byte) 0xd7, (byte) 0x1e, (byte) 0x8c, (byte) 0x94, (byte) 0xe0, (byte) 0x4a, (byte) 0x25,
            (byte) 0x61, (byte) 0x9d, (byte) 0xce, (byte) 0xe3, (byte) 0xd2, (byte) 0x26, (byte) 0x1a, (byte) 0xd2, (byte) 0xee, (byte) 0x6b,
            (byte) 0xf1, (byte) 0x2f, (byte) 0xfa, (byte) 0x06, (byte) 0xd9, (byte) 0x8a, (byte) 0x08, (byte) 0x64, (byte) 0xd8, (byte) 0x76,
            (byte) 0x02, (byte) 0x73, (byte) 0x3e, (byte) 0xc8, (byte) 0x6a, (byte) 0x64, (byte) 0x52, (byte) 0x1f, (byte) 0x2b, (byte) 0x18,
            (byte) 0x17, (byte) 0x7b, (byte) 0x20, (byte) 0x0c, (byte) 0xbb, (byte) 0xe1, (byte) 0x17, (byte) 0x57, (byte) 0x7a, (byte) 0x61,
            (byte) 0x5d, (byte) 0x6c, (byte) 0x77, (byte) 0x09, (byte) 0x88, (byte) 0xc0, (byte) 0xba, (byte) 0xd9, (byte) 0x46, (byte) 0xe2,
            (byte) 0x08, (byte) 0xe2, (byte) 0x4f, (byte) 0xa0, (byte) 0x74, (byte) 0xe5, (byte) 0xab, (byte) 0x31, (byte) 0x43, (byte) 0xdb,
            (byte) 0x5b, (byte) 0xfc, (byte) 0xe0, (byte) 0xfd, (byte) 0x10, (byte) 0x8e, (byte) 0x4b, (byte) 0x82, (byte) 0xd1, (byte) 0x20,
            (byte) 0xa9, (byte) 0x3a, (byte) 0xd2, (byte) 0xca, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0x02, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x04, (byte) 0x00, (byte) 0x03,
            (byte) 0x82, (byte) 0x01, (byte) 0x85, (byte) 0x00, (byte) 0x02, (byte) 0x82, (byte) 0x01, (byte) 0x80,
    };

    private static byte[] DH3072PublicKeyWith0PadASNHeader = new byte[]{
            (byte) 0x30, (byte) 0x82, (byte) 0x03, (byte) 0x29, (byte) 0x30, (byte) 0x82, (byte) 0x01, (byte) 0x9b, (byte) 0x06, (byte) 0x09,
            (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x30,
            (byte) 0x82, (byte) 0x01, (byte) 0x8c, (byte) 0x02, (byte) 0x82, (byte) 0x01, (byte) 0x81, (byte) 0x00, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xc9, (byte) 0x0f, (byte) 0xda, (byte) 0xa2,
            (byte) 0x21, (byte) 0x68, (byte) 0xc2, (byte) 0x34, (byte) 0xc4, (byte) 0xc6, (byte) 0x62, (byte) 0x8b, (byte) 0x80, (byte) 0xdc,
            (byte) 0x1c, (byte) 0xd1, (byte) 0x29, (byte) 0x02, (byte) 0x4e, (byte) 0x08, (byte) 0x8a, (byte) 0x67, (byte) 0xcc, (byte) 0x74,
            (byte) 0x02, (byte) 0x0b, (byte) 0xbe, (byte) 0xa6, (byte) 0x3b, (byte) 0x13, (byte) 0x9b, (byte) 0x22, (byte) 0x51, (byte) 0x4a,
            (byte) 0x08, (byte) 0x79, (byte) 0x8e, (byte) 0x34, (byte) 0x04, (byte) 0xdd, (byte) 0xef, (byte) 0x95, (byte) 0x19, (byte) 0xb3,
            (byte) 0xcd, (byte) 0x3a, (byte) 0x43, (byte) 0x1b, (byte) 0x30, (byte) 0x2b, (byte) 0x0a, (byte) 0x6d, (byte) 0xf2, (byte) 0x5f,
            (byte) 0x14, (byte) 0x37, (byte) 0x4f, (byte) 0xe1, (byte) 0x35, (byte) 0x6d, (byte) 0x6d, (byte) 0x51, (byte) 0xc2, (byte) 0x45,
            (byte) 0xe4, (byte) 0x85, (byte) 0xb5, (byte) 0x76, (byte) 0x62, (byte) 0x5e, (byte) 0x7e, (byte) 0xc6, (byte) 0xf4, (byte) 0x4c,
            (byte) 0x42, (byte) 0xe9, (byte) 0xa6, (byte) 0x37, (byte) 0xed, (byte) 0x6b, (byte) 0x0b, (byte) 0xff, (byte) 0x5c, (byte) 0xb6,
            (byte) 0xf4, (byte) 0x06, (byte) 0xb7, (byte) 0xed, (byte) 0xee, (byte) 0x38, (byte) 0x6b, (byte) 0xfb, (byte) 0x5a, (byte) 0x89,
            (byte) 0x9f, (byte) 0xa5, (byte) 0xae, (byte) 0x9f, (byte) 0x24, (byte) 0x11, (byte) 0x7c, (byte) 0x4b, (byte) 0x1f, (byte) 0xe6,
            (byte) 0x49, (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xec, (byte) 0xe4, (byte) 0x5b, (byte) 0x3d, (byte) 0xc2, (byte) 0x00,
            (byte) 0x7c, (byte) 0xb8, (byte) 0xa1, (byte) 0x63, (byte) 0xbf, (byte) 0x05, (byte) 0x98, (byte) 0xda, (byte) 0x48, (byte) 0x36,
            (byte) 0x1c, (byte) 0x55, (byte) 0xd3, (byte) 0x9a, (byte) 0x69, (byte) 0x16, (byte) 0x3f, (byte) 0xa8, (byte) 0xfd, (byte) 0x24,
            (byte) 0xcf, (byte) 0x5f, (byte) 0x83, (byte) 0x65, (byte) 0x5d, (byte) 0x23, (byte) 0xdc, (byte) 0xa3, (byte) 0xad, (byte) 0x96,
            (byte) 0x1c, (byte) 0x62, (byte) 0xf3, (byte) 0x56, (byte) 0x20, (byte) 0x85, (byte) 0x52, (byte) 0xbb, (byte) 0x9e, (byte) 0xd5,
            (byte) 0x29, (byte) 0x07, (byte) 0x70, (byte) 0x96, (byte) 0x96, (byte) 0x6d, (byte) 0x67, (byte) 0x0c, (byte) 0x35, (byte) 0x4e,
            (byte) 0x4a, (byte) 0xbc, (byte) 0x98, (byte) 0x04, (byte) 0xf1, (byte) 0x74, (byte) 0x6c, (byte) 0x08, (byte) 0xca, (byte) 0x18,
            (byte) 0x21, (byte) 0x7c, (byte) 0x32, (byte) 0x90, (byte) 0x5e, (byte) 0x46, (byte) 0x2e, (byte) 0x36, (byte) 0xce, (byte) 0x3b,
            (byte) 0xe3, (byte) 0x9e, (byte) 0x77, (byte) 0x2c, (byte) 0x18, (byte) 0x0e, (byte) 0x86, (byte) 0x03, (byte) 0x9b, (byte) 0x27,
            (byte) 0x83, (byte) 0xa2, (byte) 0xec, (byte) 0x07, (byte) 0xa2, (byte) 0x8f, (byte) 0xb5, (byte) 0xc5, (byte) 0x5d, (byte) 0xf0,
            (byte) 0x6f, (byte) 0x4c, (byte) 0x52, (byte) 0xc9, (byte) 0xde, (byte) 0x2b, (byte) 0xcb, (byte) 0xf6, (byte) 0x95, (byte) 0x58,
            (byte) 0x17, (byte) 0x18, (byte) 0x39, (byte) 0x95, (byte) 0x49, (byte) 0x7c, (byte) 0xea, (byte) 0x95, (byte) 0x6a, (byte) 0xe5,
            (byte) 0x15, (byte) 0xd2, (byte) 0x26, (byte) 0x18, (byte) 0x98, (byte) 0xfa, (byte) 0x05, (byte) 0x10, (byte) 0x15, (byte) 0x72,
            (byte) 0x8e, (byte) 0x5a, (byte) 0x8a, (byte) 0xaa, (byte) 0xc4, (byte) 0x2d, (byte) 0xad, (byte) 0x33, (byte) 0x17, (byte) 0x0d,
            (byte) 0x04, (byte) 0x50, (byte) 0x7a, (byte) 0x33, (byte) 0xa8, (byte) 0x55, (byte) 0x21, (byte) 0xab, (byte) 0xdf, (byte) 0x1c,
            (byte) 0xba, (byte) 0x64, (byte) 0xec, (byte) 0xfb, (byte) 0x85, (byte) 0x04, (byte) 0x58, (byte) 0xdb, (byte) 0xef, (byte) 0x0a,
            (byte) 0x8a, (byte) 0xea, (byte) 0x71, (byte) 0x57, (byte) 0x5d, (byte) 0x06, (byte) 0x0c, (byte) 0x7d, (byte) 0xb3, (byte) 0x97,
            (byte) 0x0f, (byte) 0x85, (byte) 0xa6, (byte) 0xe1, (byte) 0xe4, (byte) 0xc7, (byte) 0xab, (byte) 0xf5, (byte) 0xae, (byte) 0x8c,
            (byte) 0xdb, (byte) 0x09, (byte) 0x33, (byte) 0xd7, (byte) 0x1e, (byte) 0x8c, (byte) 0x94, (byte) 0xe0, (byte) 0x4a, (byte) 0x25,
            (byte) 0x61, (byte) 0x9d, (byte) 0xce, (byte) 0xe3, (byte) 0xd2, (byte) 0x26, (byte) 0x1a, (byte) 0xd2, (byte) 0xee, (byte) 0x6b,
            (byte) 0xf1, (byte) 0x2f, (byte) 0xfa, (byte) 0x06, (byte) 0xd9, (byte) 0x8a, (byte) 0x08, (byte) 0x64, (byte) 0xd8, (byte) 0x76,
            (byte) 0x02, (byte) 0x73, (byte) 0x3e, (byte) 0xc8, (byte) 0x6a, (byte) 0x64, (byte) 0x52, (byte) 0x1f, (byte) 0x2b, (byte) 0x18,
            (byte) 0x17, (byte) 0x7b, (byte) 0x20, (byte) 0x0c, (byte) 0xbb, (byte) 0xe1, (byte) 0x17, (byte) 0x57, (byte) 0x7a, (byte) 0x61,
            (byte) 0x5d, (byte) 0x6c, (byte) 0x77, (byte) 0x09, (byte) 0x88, (byte) 0xc0, (byte) 0xba, (byte) 0xd9, (byte) 0x46, (byte) 0xe2,
            (byte) 0x08, (byte) 0xe2, (byte) 0x4f, (byte) 0xa0, (byte) 0x74, (byte) 0xe5, (byte) 0xab, (byte) 0x31, (byte) 0x43, (byte) 0xdb,
            (byte) 0x5b, (byte) 0xfc, (byte) 0xe0, (byte) 0xfd, (byte) 0x10, (byte) 0x8e, (byte) 0x4b, (byte) 0x82, (byte) 0xd1, (byte) 0x20,
            (byte) 0xa9, (byte) 0x3a, (byte) 0xd2, (byte) 0xca, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0x02, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x04, (byte) 0x00, (byte) 0x03,
            (byte) 0x82, (byte) 0x01, (byte) 0x86, (byte) 0x00, (byte) 0x02, (byte) 0x82, (byte) 0x01, (byte) 0x81,
    };

    private static byte[] DH1024PublicKeyNo0PadASNHeader = new byte[]{
            (byte) 0x30, (byte) 0x82, (byte) 0x01, (byte) 0x23, (byte) 0x30, (byte) 0x81, (byte) 0x99, (byte) 0x06, (byte) 0x09, (byte) 0x2a,
            (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x30, (byte) 0x81,
            (byte) 0x8b, (byte) 0x02, (byte) 0x81, (byte) 0x81, (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xc9, (byte) 0x0f, (byte) 0xda, (byte) 0xa2, (byte) 0x21, (byte) 0x68, (byte) 0xc2,
            (byte) 0x34, (byte) 0xc4, (byte) 0xc6, (byte) 0x62, (byte) 0x8b, (byte) 0x80, (byte) 0xdc, (byte) 0x1c, (byte) 0xd1, (byte) 0x29,
            (byte) 0x02, (byte) 0x4e, (byte) 0x08, (byte) 0x8a, (byte) 0x67, (byte) 0xcc, (byte) 0x74, (byte) 0x02, (byte) 0x0b, (byte) 0xbe,
            (byte) 0xa6, (byte) 0x3b, (byte) 0x13, (byte) 0x9b, (byte) 0x22, (byte) 0x51, (byte) 0x4a, (byte) 0x08, (byte) 0x79, (byte) 0x8e,
            (byte) 0x34, (byte) 0x04, (byte) 0xdd, (byte) 0xef, (byte) 0x95, (byte) 0x19, (byte) 0xb3, (byte) 0xcd, (byte) 0x3a, (byte) 0x43,
            (byte) 0x1b, (byte) 0x30, (byte) 0x2b, (byte) 0x0a, (byte) 0x6d, (byte) 0xf2, (byte) 0x5f, (byte) 0x14, (byte) 0x37, (byte) 0x4f,
            (byte) 0xe1, (byte) 0x35, (byte) 0x6d, (byte) 0x6d, (byte) 0x51, (byte) 0xc2, (byte) 0x45, (byte) 0xe4, (byte) 0x85, (byte) 0xb5,
            (byte) 0x76, (byte) 0x62, (byte) 0x5e, (byte) 0x7e, (byte) 0xc6, (byte) 0xf4, (byte) 0x4c, (byte) 0x42, (byte) 0xe9, (byte) 0xa6,
            (byte) 0x37, (byte) 0xed, (byte) 0x6b, (byte) 0x0b, (byte) 0xff, (byte) 0x5c, (byte) 0xb6, (byte) 0xf4, (byte) 0x06, (byte) 0xb7,
            (byte) 0xed, (byte) 0xee, (byte) 0x38, (byte) 0x6b, (byte) 0xfb, (byte) 0x5a, (byte) 0x89, (byte) 0x9f, (byte) 0xa5, (byte) 0xae,
            (byte) 0x9f, (byte) 0x24, (byte) 0x11, (byte) 0x7c, (byte) 0x4b, (byte) 0x1f, (byte) 0xe6, (byte) 0x49, (byte) 0x28, (byte) 0x66,
            (byte) 0x51, (byte) 0xec, (byte) 0xe6, (byte) 0x53, (byte) 0x81, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x02, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x04, (byte) 0x00,
            (byte) 0x03, (byte) 0x81, (byte) 0x84, (byte) 0x00, (byte) 0x02, (byte) 0x81, (byte) 0x80,
    };

    private static byte[] DH1024PublicKeyWith0PadASNHeader = new byte[]{
            (byte) 0x30, (byte) 0x82, (byte) 0x01, (byte) 0x24, (byte) 0x30, (byte) 0x81, (byte) 0x99, (byte) 0x06, (byte) 0x09, (byte) 0x2a,
            (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x30, (byte) 0x81,
            (byte) 0x8b, (byte) 0x02, (byte) 0x81, (byte) 0x81, (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xc9, (byte) 0x0f, (byte) 0xda, (byte) 0xa2, (byte) 0x21, (byte) 0x68, (byte) 0xc2,
            (byte) 0x34, (byte) 0xc4, (byte) 0xc6, (byte) 0x62, (byte) 0x8b, (byte) 0x80, (byte) 0xdc, (byte) 0x1c, (byte) 0xd1, (byte) 0x29,
            (byte) 0x02, (byte) 0x4e, (byte) 0x08, (byte) 0x8a, (byte) 0x67, (byte) 0xcc, (byte) 0x74, (byte) 0x02, (byte) 0x0b, (byte) 0xbe,
            (byte) 0xa6, (byte) 0x3b, (byte) 0x13, (byte) 0x9b, (byte) 0x22, (byte) 0x51, (byte) 0x4a, (byte) 0x08, (byte) 0x79, (byte) 0x8e,
            (byte) 0x34, (byte) 0x04, (byte) 0xdd, (byte) 0xef, (byte) 0x95, (byte) 0x19, (byte) 0xb3, (byte) 0xcd, (byte) 0x3a, (byte) 0x43,
            (byte) 0x1b, (byte) 0x30, (byte) 0x2b, (byte) 0x0a, (byte) 0x6d, (byte) 0xf2, (byte) 0x5f, (byte) 0x14, (byte) 0x37, (byte) 0x4f,
            (byte) 0xe1, (byte) 0x35, (byte) 0x6d, (byte) 0x6d, (byte) 0x51, (byte) 0xc2, (byte) 0x45, (byte) 0xe4, (byte) 0x85, (byte) 0xb5,
            (byte) 0x76, (byte) 0x62, (byte) 0x5e, (byte) 0x7e, (byte) 0xc6, (byte) 0xf4, (byte) 0x4c, (byte) 0x42, (byte) 0xe9, (byte) 0xa6,
            (byte) 0x37, (byte) 0xed, (byte) 0x6b, (byte) 0x0b, (byte) 0xff, (byte) 0x5c, (byte) 0xb6, (byte) 0xf4, (byte) 0x06, (byte) 0xb7,
            (byte) 0xed, (byte) 0xee, (byte) 0x38, (byte) 0x6b, (byte) 0xfb, (byte) 0x5a, (byte) 0x89, (byte) 0x9f, (byte) 0xa5, (byte) 0xae,
            (byte) 0x9f, (byte) 0x24, (byte) 0x11, (byte) 0x7c, (byte) 0x4b, (byte) 0x1f, (byte) 0xe6, (byte) 0x49, (byte) 0x28, (byte) 0x66,
            (byte) 0x51, (byte) 0xec, (byte) 0xe6, (byte) 0x53, (byte) 0x81, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x02, (byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x04, (byte) 0x00,
            (byte) 0x03, (byte) 0x81, (byte) 0x85, (byte) 0x00, (byte) 0x02, (byte) 0x81, (byte) 0x81,
    };

}
