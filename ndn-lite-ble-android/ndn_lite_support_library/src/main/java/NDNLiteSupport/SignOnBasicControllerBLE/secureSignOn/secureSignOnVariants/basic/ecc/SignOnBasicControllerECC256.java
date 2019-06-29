
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.ecc;

import android.util.Log;

import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import NDNLiteSupport.LogHelpers;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.SecureSignOnVariantStrings;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.SignOnBasicController;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.utils.EncodingHelpers;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.utils.SecurityHelpers;

import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.ecc.SignOnControllerBasicECC256Consts.ECDH_CURVE;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.ecc.SignOnControllerBasicECC256Consts.ECDH_PRIVATE_KEY_LENGTH;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.ecc.SignOnControllerBasicECC256Consts.ECDH_PUBLIC_KEY_LENGTH;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.ecc.SignOnControllerBasicECC256Consts.KD_CURVE;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.ecc.SignOnControllerBasicECC256Consts.KD_PRIVATE_KEY_LENGTH;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.ecc.SignOnControllerBasicECC256Consts.KD_PUBLIC_KEY_LENGTH;

public class SignOnBasicControllerECC256 extends SignOnBasicController {

    // TAG for logging.
    private static final String TAG = SignOnBasicControllerECC256.class.getSimpleName();

    /**
     * NOTE: The "raw" format of public and private keys is the same format that is generated and
     * used by the uECC library, which can be found here: https://github.com/kmackay/micro-ecc
     */

    /**
     * Create a SignOnBasicControllerECC256 object with a given trust anchor and SecureSignOnControllerCallbacks.
     * This sign on controller will only work properly with sign on clients using the "ecc_256" variant of
     * the basic version of the sign on protocol.
     * @param trustAnchorCertificate Trust anchor certificate to initialize SignOnController with. This will
     *                               be sent to devices undergoing the sign on process.
     */
    public SignOnBasicControllerECC256(CertificateV2 trustAnchorCertificate) throws Exception {
        super(trustAnchorCertificate);

        secureSignOnVariantType_ = SecureSignOnVariantStrings.SIGN_ON_VARIANT_BASIC_ECC_256;
    }

    /**
     * In the "ecc_256" variant, the diffie hellman keypair is an ECC key pair, generated using the
     * ECDH_CURVE as defined in SignOnControllerBasicECC256Consts.
     */
    protected KeyPair generateDiffieHellmanKeyPair() throws Exception {
        KeyPair N2KeyPair = null;
        try {
            N2KeyPair = SecurityHelpers.generateECKeyPair(ECDH_CURVE);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (InvalidAlgorithmParameterException e) {
            throw e;
        }

        return N2KeyPair;
    }

    /**
     * In the "ecc_256" variant, the N1publicKey and N2privateKey should be ecc keys, based on the
     * ECDH_CURVE as defined in SignOnControllerBasicECC256Consts.
     * @param N1publicKey N1 key pair public key in PublicKey format.
     * @param N2privateKey N2 key pair private key in PrivateKey format.
     */
    protected byte[] deriveDiffieHellmanSharedSecret(PublicKey N1publicKey, PrivateKey N2privateKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] sharedSecret = null;
        try {
            sharedSecret = SecurityHelpers.deriveSharedSecretECDH(N1publicKey, N2privateKey);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (InvalidKeyException e) {
            throw e;
        } catch (NullPointerException e) {
            throw e;
        }

        return sharedSecret;
    }

    /**
     * The bootstrapping request signature should be a sh256 ecdsa signature generated using the KS
     * ("Key Shared") key pair private key by the device. The KS key pair private key should be an
     * ecc key based on the KS_CURVE as defined in SignOnControllerBasicECC256Consts.
     */
    protected boolean verifyBootstrappingRequestSignature(byte[] bootstrappingRequestSignature,
                                                          byte[] bootstrappingRequestSignaturePayload,
                                                          PublicKey publicKey) throws Exception {
        if (!verifySha256ECDSASignature(bootstrappingRequestSignature, bootstrappingRequestSignaturePayload, publicKey)) {
            Log.e(TAG, "FAILED TO VERIFY BOOTSTRAPPING REQUEST SIGNATURE");
            return false;
        }

        Log.w(TAG, "SUCCESSFULLY VERIFIED BOOTSTRAPPING REQUEST SIGNATURE");
        return true;
    }

    /**
     * The certificate request signature should be a sh256 ecdsa signature generated using the KS
     * ("Key Shared") key pair private key by the device. The KS key pair private key should be an
     * ecc key based on the KS_CURVE as defined in SignOnControllerBasicECC256Consts.
     */
    protected boolean verifyCertificateRequestSignature(byte[] certificateRequestSignature,
                                                        byte[] certificateRequestSignaturePayload,
                                                        PublicKey publicKey) throws Exception {
        if (!verifySha256ECDSASignature(certificateRequestSignature, certificateRequestSignaturePayload, publicKey)) {
            Log.e(TAG, "FAILED TO VERIFY CERTIFICATE REQUEST SIGNATURE");
            return false;
        }

        Log.w(TAG, "SUCCESSFULLY VERIFIED CERTIFICATE REQUEST SIGNATURE");
        return true;
    }

    /**
     * The finish message signature should be a sh256 ecdsa signature generated using the KS
     * ("Key Shared") key pair private key by the device. The KS key pair private key should be an
     * ecc key based on the KS_CURVE as defined in SignOnControllerBasicECC256Consts.
     */
    protected boolean verifyFinishMessageSignature(byte[] finishMessageSignature, byte[] finishMessageSignaturePayload,
                                                   PublicKey publicKey) throws Exception {
        if (!verifySha256ECDSASignature(finishMessageSignature, finishMessageSignaturePayload, publicKey)) {
            Log.e(TAG, "FAILED TO VERIFY FINISH MESSAGE SIGNATURE");
            return false;
        }

        Log.w(TAG, "SUCCESSFULLY VERIFIED FINISH MESSAGE SIGNATURE");
        return true;
    }

    /**
     * See the NOTE at the top of the page for the "raw" format.
     */
    protected byte[] getN2pubRawBytes(PublicKey publicKey) {
        byte[] N2pubRawBytes = EncodingHelpers.hexStringToByteArray(SecurityHelpers.getEcPublicKeyAsHex(publicKey, ECDH_PUBLIC_KEY_LENGTH,
                ECDH_PRIVATE_KEY_LENGTH));
        return N2pubRawBytes;
    }

    /**
     * See the NOTE at the top of the page for the "raw" format.
     */
    protected byte[] getKDPubRawBytes(PublicKey publicKey) {
        return EncodingHelpers.hexStringToByteArray(SecurityHelpers.getEcPublicKeyAsHex(publicKey, KD_PUBLIC_KEY_LENGTH,
                KD_PRIVATE_KEY_LENGTH));
    }

    /**
     * See the NOTE at the top of the page for the "raw" format.
     */
    protected byte[] getKDPriRawBytes(PrivateKey privateKey) {
        return EncodingHelpers.hexStringToByteArray(SecurityHelpers.getEcPrivateKeyAsHex(privateKey));
    }

    /**
     * See the NOTE at the top of the page for the "raw" format.
     */
    protected PublicKey convertRawN1PubBytesToPublicKeyObject(byte[] N1pubRawBytes) throws Exception {
        byte[] N1pubASNEncoded = new byte[0];
        try {
            N1pubASNEncoded = SecurityHelpers.asnEncodeRawECPublicKeyBytes(N1pubRawBytes);
        } catch (Exception e) {
            throw e;
        }

        PublicKey N1pub = null;
        try {
            N1pub = SecurityHelpers.convertASNEncodedPublicKeyToPublicKeyObject(N1pubASNEncoded, "EC");
        } catch (InvalidKeySpecException e) {
            throw e;
        } catch (NoSuchAlgorithmException e) {
            throw e;
        }

        return N1pub;
    }

    /**
     * In the "ecc_256" variant, the diffie hellman keypair is an ECC key pair, generated using the
     * KD_CURVE as defined in SignOnControllerBasicECC256Consts.
     */
    protected KeyPair generateKDKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPair KdKeyPair = null;
        try {
            KdKeyPair = SecurityHelpers.generateECKeyPair(KD_CURVE);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (InvalidAlgorithmParameterException e) {
            throw e;
        }
        return KdKeyPair;
    }

    /**
     * Verify a sha256 ecdsa signature.
     * @param signature Bytes of signature to verify.
     * @param signaturePayload Bytes over which the signature was calculated.
     * @param publicKey Public key to verify signature with, in PublicKey format.
     */
    boolean verifySha256ECDSASignature(byte[] signature, byte[] signaturePayload, PublicKey publicKey) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new Error("MessageDigest: SHA-256 is not supported: " + exception.getMessage());
        }

        LogHelpers.LogByteArrayDebug(TAG, "Signature payload:", signaturePayload);
        LogHelpers.LogByteArrayDebug(TAG, "Signature bytes:", signature);

        sha256.update(signaturePayload);
        byte[] digestOfSignaturePayload = sha256.digest();
        Blob digestOfCertificateRequestBlob = new Blob(digestOfSignaturePayload);
        LogHelpers.LogDebug(TAG, "Hex string of signed portion hash: " + digestOfCertificateRequestBlob.toHex());

        if (!SecurityHelpers.verifySignatureECC(publicKey, signature, signaturePayload)) {
            return false;
        }

        return true;
    }

}
