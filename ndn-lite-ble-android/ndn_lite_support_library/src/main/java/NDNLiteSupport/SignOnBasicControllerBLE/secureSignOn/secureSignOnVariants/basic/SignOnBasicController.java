
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic;

import android.util.Log;

import net.named_data.jndn.Name;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.Common;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import NDNLiteSupport.LogHelpers;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnController;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnControllerConsts;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnControllerResultCodes;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnControllerResults;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnDeviceInfo;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.SignOnBasicControllerResults.ParseBootstrappingRequestResult;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.SignOnBasicControllerResults.ParseCertificateRequestResult;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.SignOnBasicControllerResults.ParseFinishMessageResult;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvConsts;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvParsingHelpers;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvResultCodes;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvResults;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.utils.SecurityHelpers;

import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.SignOnBasicControllerResultCodes.ParseSignOnMessageResultCode;

public abstract class SignOnBasicController extends SignOnController {

    // TAG for logging.
    private final static String TAG = SignOnBasicController.class.getSimpleName();

    /**
     * Create a SignOnBasicController object with a given trust anchor and SecureSignOnControllerCallbacks.
     * Children of this sign on controller will only work properly with sign on clients using the
     * basic sign on protocol.
     * @param trustAnchorCertificate Trust anchor certificate to initialize SignOnController with. This will
     *                               be sent to devices undergoing the sign on process.
     */
    public SignOnBasicController(CertificateV2 trustAnchorCertificate) throws Exception {

        super(trustAnchorCertificate);

    }

    /**
     * Implementation of processBootstrappingRequest for the basic version of the sign on protocol.
     * @param bootstrappingRequest Byte array containing bootstrapping request to process.
     */
    public SignOnControllerResults.ProcessSignOnMessageResult processBootstrappingRequest(byte[] bootstrappingRequest) {

        LogHelpers.LogDebug(TAG, "processBootstrappingRequest of SignOnBasicController got called.");

        evaluationTimer_.lastOnboardingReceivedBootstrappingRequestTime = System.nanoTime();

        ParseBootstrappingRequestResult parseResult = parseBootstrappingRequest(bootstrappingRequest);
        if (!parseResult.resultCode.equals(ParseSignOnMessageResultCode.SUCCESS)) {
            Log.e(TAG, "Error parsing bootstrapping request: " + parseResult.resultCode);
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_BOOTSTRAPPING_REQUEST_ERROR_PARSING,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE,
                    new Blob(parseResult.deviceIdentifier).toHex()
            );
        }

        LogHelpers.LogByteArrayDebug(TAG,
                "Bytes of device identifier received from bootstrapping request: ", parseResult.deviceIdentifier);

        LogHelpers.LogByteArrayDebug(TAG,
                "Bytes of device capabilities received from bootstrapping request: ", parseResult.deviceCapabilities);

        LogHelpers.LogByteArrayDebug(TAG,
                "Bytes of N1pub received from bootstrapping request: ", parseResult.N1pub);

        LogHelpers.LogByteArrayDebug(TAG,
                "Hex string of signature received from bootstrapping request: ", parseResult.signature);

        Blob deviceIdentifierBlob = new Blob(parseResult.deviceIdentifier);
        String deviceIdentifierHexString = deviceIdentifierBlob.toHex();
        if (!devices_.containsKey(deviceIdentifierHexString)) {
            LogHelpers.LogDebug(TAG, "Received a bootstrapping interest from a device not scanned yet. Device identifier hex string: "
                    + deviceIdentifierHexString);
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_BOOTSTRAPPING_REQUEST_DEVICE_IDENTIFIER_NOT_FOUND_IN_DEVICE_LIST,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        SignOnDeviceInfo currentDeviceInfo = devices_.get(deviceIdentifierHexString);

        try {
            // the bytes over which the signature was calculated; this is all of the bytes of the bootstrapping request
            // excluding the packet header (as in, excluding bootstrapping request tlv type and length) and signature tlv block
            // (as in, excluding the signature tlv, type, length, and value)
            LogHelpers.LogDebug(TAG, "Signature length of bootstrapping request from parse result: " + parseResult.signatureLength);
            int signaturePayloadBegOffset = secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE;
            int signaturePayloadEndOffset = parseResult.bootstrappingRequestTlvLength -
                    parseResult.signatureLength;
            LogHelpers.LogDebug(TAG, "Signature payload beginning offset: " + signaturePayloadBegOffset);
            LogHelpers.LogDebug(TAG, "Signature payload end offset: " + signaturePayloadEndOffset);
            byte[] signaturePayload = Arrays.copyOfRange(bootstrappingRequest, signaturePayloadBegOffset, signaturePayloadEndOffset);
            LogHelpers.LogByteArrayDebug(TAG, "Bytes of signature payload (bytes over which signature was calculated) of " +
                    "bootstrapping request:", signaturePayload);
            if (!verifyBootstrappingRequestSignature(parseResult.signature, signaturePayload, currentDeviceInfo.KSpub)) {
                LogHelpers.LogDebug(TAG, "Failed to verify bootstrapping request from device with device identifier string: " +
                        deviceIdentifierHexString);
                return new SignOnControllerResults.ProcessSignOnMessageResult(
                        SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_BOOTSTRAPPING_REQUEST_FAILED_TO_VERIFY,
                        secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE,
                        deviceIdentifierHexString
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_BOOTSTRAPPING_REQUEST_FAILED_TO_VERIFY,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        evaluationTimer_.lastOnboardingValidatedBootstrappingRequestTime = System.nanoTime();

        KeyPair N2KeyPair = null;
        try {
            N2KeyPair = generateDiffieHellmanKeyPair();
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to generate N2 keypair for bootstrapping request response.");
            e.printStackTrace();
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_BOOTSTRAPPING_REQUEST_FAILED_TO_GENERATE_N2_KEY_PAIR,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        currentDeviceInfo.N2KeyPair = N2KeyPair;

        byte[] N2pubASNEncoded = N2KeyPair.getPublic().getEncoded();
        Blob N2pubASNEncodedBlob = new Blob(N2pubASNEncoded);
        LogHelpers.LogDebug(TAG, "Hex string of N2pub ASN encoded: " + N2pubASNEncodedBlob.toHex());

        byte[] N1pubRaw = parseResult.N1pub;
        currentDeviceInfo.N1pubRawBytes = Arrays.copyOf(N1pubRaw, N1pubRaw.length);
        Blob N1pubRawBlob = new Blob(N1pubRaw);
        LogHelpers.LogDebug(TAG, "Hex of received N1pubRaw: " + N1pubRawBlob.toHex());

        byte[] N2pubRawBytes = getN2pubRawBytes(N2KeyPair.getPublic());
        Blob N2pubRawCheckerBlob = new Blob(N2pubRawBytes);
        LogHelpers.LogDebug(TAG, "Hex string of N2pubRaw: " + N2pubRawCheckerBlob.toHex());

        currentDeviceInfo.N2pubRawBytes = Arrays.copyOf(N2pubRawBytes, N2pubRawBytes.length);

        return new SignOnControllerResults.ProcessSignOnMessageResult(
                SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS,
                secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE,
                deviceIdentifierHexString
        );

    }

    /**
     * Implementation of processCertificateRequest for the basic version of the sign on protocol.
     * @param certificateRequest Byte array containing certificate request to process.
     */
    public SignOnControllerResults.ProcessSignOnMessageResult processCertificateRequest(byte[] certificateRequest) {

        LogHelpers.LogDebug(TAG, "processCertificateRequest of SignOnBasicController got called.");

        evaluationTimer_.lastOnboardingReceivedCertificateRequestTime = System.nanoTime();

        ParseCertificateRequestResult parseResult = parseCertificateRequest(certificateRequest);
        if (!parseResult.resultCode.equals(ParseSignOnMessageResultCode.SUCCESS)) {
            Log.e(TAG, "Error parsing certificate request: " + parseResult.resultCode);
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_ERROR_PARSING,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    new Blob(parseResult.deviceIdentifier).toHex()
            );
        }

        LogHelpers.LogByteArrayDebug(TAG,
                "Bytes of device identifier received from certificate request: ", parseResult.deviceIdentifier);

        LogHelpers.LogByteArrayDebug(TAG,
                "Bytes of N1pub received from certificate request: ", parseResult.N1pub);

        LogHelpers.LogByteArrayDebug(TAG,
                "Bytes of N2 pub digest received from certificate request: ", parseResult.N2pubDigest);

        LogHelpers.LogByteArrayDebug(TAG,
                "Bytes of trust anchor digest received from certificate request: ", parseResult.trustAnchorCertDigest);

        LogHelpers.LogByteArrayDebug(TAG,
                "Hex string of signature received from certificate request: ", parseResult.signature);

        Blob deviceIdentifierBlob = new Blob(parseResult.deviceIdentifier);
        String deviceIdentifierHexString = deviceIdentifierBlob.toHex();
        LogHelpers.LogDebug(TAG, "Hex string of device identifier received from certificate request interest: " + deviceIdentifierHexString);

        if (!devices_.containsKey(deviceIdentifierHexString)) {
            LogHelpers.LogDebug(TAG, "Received a certificate request interest from a device not scanned yet. Device identifier hex string: "
                    + deviceIdentifierHexString);
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_DEVICE_IDENTIFIER_NOT_FOUND_IN_DEVICE_LIST,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        SignOnDeviceInfo currentDeviceInfo = devices_.get(deviceIdentifierHexString);

        try {
            // the bytes over which the signature was calculated; this is all of the bytes of the certificate request
            // excluding the packet header (as in, excluding certificate request tlv type and length) and signature tlv block
            // (as in, excluding the signature tlv, type, length, and value)
            LogHelpers.LogDebug(TAG, "Value of parseResult.signatureLength in processCertificateRequest: " + parseResult.signatureLength);
            byte[] signaturePayload = Arrays.copyOfRange(certificateRequest, secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE,
                    parseResult.certificateRequestTlvLength -
                            //(parseResult.signatureLength + SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE));
                            parseResult.signatureLength);
            if (!verifyCertificateRequestSignature(parseResult.signature, signaturePayload, currentDeviceInfo.KSpub)) {
                LogHelpers.LogDebug(TAG, "Failed to verify certificate request interest from device. " +
                        "Device identifier hex string: " + deviceIdentifierHexString);
                return new SignOnControllerResults.ProcessSignOnMessageResult(
                        SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_FAILED_TO_VERIFY,
                        secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                        deviceIdentifierHexString
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_FAILED_TO_VERIFY,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        byte[] trustAnchorDigest = new byte[0];
        try {
            trustAnchorDigest = SecurityHelpers.sha256(anchorCertificate_.wireEncode().getImmutableArray());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_FAILED_TO_GENERATE_TRUST_ANCHOR_DIGEST,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }
        byte[] trustAnchorDigestFromDevice = parseResult.trustAnchorCertDigest;

        if (!SecurityHelpers.checkBuffersMatch(trustAnchorDigest, trustAnchorDigestFromDevice)) {
            LogHelpers.LogDebug(TAG, "Got a wrong digest of trust anchor from device. Device identifier hex string: " +
                    deviceIdentifierHexString);
            Blob trustAnchorDigestFromDeviceBlob = new Blob(trustAnchorDigestFromDevice);
            LogHelpers.LogDebug(TAG, "Trust anchor digest we received from device: " + trustAnchorDigestFromDeviceBlob.toHex());
            Blob trustAnchorDigestBlob = new Blob(trustAnchorDigest);
            LogHelpers.LogDebug(TAG, "Trust anchor digest we calculated: " + trustAnchorDigestBlob.toHex());
            Blob trustAnchorWireEncodedBlob = new Blob(anchorCertificate_.wireEncode());
            LogHelpers.LogDebug(TAG, "Hex string of wire encoded trust anchor: " + trustAnchorWireEncodedBlob.toHex());
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_TRUST_ANCHOR_DIGEST_DID_NOT_MATCH,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        byte[] N1pubRawBytesFromDevice = parseResult.N1pub;
        byte[] N2sha256DigestFromDevice = parseResult.N2pubDigest;

        if (!SecurityHelpers.checkBuffersMatch(currentDeviceInfo.N1pubRawBytes, N1pubRawBytesFromDevice)) {
            LogHelpers.LogDebug(TAG, "Got a wrong N1pub from device. Device identifier hex string: " +
                    deviceIdentifierHexString);
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_N1_PUB_DID_NOT_MATCH,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        byte[] N2sha256Digest = new byte[0];
        try {
            N2sha256Digest = SecurityHelpers.sha256(currentDeviceInfo.N2pubRawBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_FAILED_TO_GENERATE_N2_PUB_DIGEST,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }
        Blob N2sha256DigestBlob = new Blob(N2sha256Digest);
        LogHelpers.LogDebug(TAG, "N2pub sha256 we calculated: " + N2sha256DigestBlob.toHex());
        Blob N2sha256DigestFromDeviceBlob = new Blob(N2sha256DigestFromDevice);
        LogHelpers.LogDebug(TAG, "N2pub sha256 digest from device: " + N2sha256DigestFromDeviceBlob.toHex());

        if (!SecurityHelpers.checkBuffersMatch(N2sha256Digest, N2sha256DigestFromDevice)) {
            LogHelpers.LogDebug(TAG, "Got a wrong N2pub sha256 digest from device. Device identifier hex srting: " +
                    deviceIdentifierHexString);
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_N2_PUB_DIGEST_DID_NOT_MATCH,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        evaluationTimer_.lastOnboardingValidatedCertificateRequestTime = System.nanoTime();

        KeyPair N2KeyPair = currentDeviceInfo.N2KeyPair;

        PublicKey N1pub = null;
        try {
            N1pub = convertRawN1PubBytesToPublicKeyObject(currentDeviceInfo.N1pubRawBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_FAILED_TO_CONVERT_N1_PUB_TO_PUBLIC_KEY_OBJECT,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        byte[] sharedSecret = null;
        try {
            sharedSecret = deriveDiffieHellmanSharedSecret(N1pub, N2KeyPair.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_FAILED_TO_DERIVE_DIFFIE_HELLMAN_SHARED_SECRET,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_FAILED_TO_DERIVE_DIFFIE_HELLMAN_SHARED_SECRET,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_CERTIFICATE_REQUEST_FAILED_TO_DERIVE_DIFFIE_HELLMAN_SHARED_SECRET,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        LogHelpers.LogByteArrayDebug(TAG, "Generated shared secret: ", sharedSecret);

        currentDeviceInfo.KT = Arrays.copyOf(sharedSecret, sharedSecret.length);

        return new SignOnControllerResults.ProcessSignOnMessageResult(
                SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS,
                secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE,
                deviceIdentifierHexString
        );

    }

    /**
     * Implementation of processFinishMessage for the basic version of the sign on protocol.
     * @param finishMessage Byte array containing finish message to process.
     */
    public SignOnControllerResults.ProcessSignOnMessageResult processFinishMessage(byte[] finishMessage) {

        LogHelpers.LogDebug(TAG, "processFinishMessage of SignOnBasicController got called.");

        ParseFinishMessageResult parseResult = parseFinishMessage(finishMessage);
        if (!parseResult.resultCode.equals(ParseSignOnMessageResultCode.SUCCESS)) {
            Log.e(TAG, "Error parsing finish message: " + parseResult.resultCode);
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_FINISH_MESSAGE_ERROR_PARSING,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE,
                    new Blob(parseResult.deviceIdentifier).toHex()
            );
        }

        LogHelpers.LogByteArrayDebug(TAG,
                "Hex string of signature received from finish message: ", parseResult.signature);

        Blob deviceIdentifierBlob = new Blob(parseResult.deviceIdentifier);
        String deviceIdentifierHexString = deviceIdentifierBlob.toHex();
        LogHelpers.LogDebug(TAG, "Hex string of device identifier received from finish message: " + deviceIdentifierHexString);

        if (!devices_.containsKey(deviceIdentifierHexString)) {
            LogHelpers.LogDebug(TAG, "Received a finish message from a device not scanned yet. Device identifier hex string: "
                    + deviceIdentifierHexString);
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_FINISH_MESSAGE_DEVICE_IDENTIFIER_NOT_FOUND_IN_DEVICE_LIST,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        SignOnDeviceInfo currentDeviceInfo = devices_.get(deviceIdentifierHexString);

        try {
            // the bytes over which the signature was calculated; this is all of the bytes of the finish message
            // excluding the packet header (as in, excluding finish message tlv type and length) and signature tlv block
            // (as in, excluding the signature tlv, type, length, and value)
            LogHelpers.LogDebug(TAG, "Value of parseResult.signatureLength in processFinishMessage: " + parseResult.signatureLength);
            byte[] signaturePayload = Arrays.copyOfRange(finishMessage, secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE,
                    parseResult.finishMessageTlvLength -
                            parseResult.signatureLength);
            if (!verifyFinishMessageSignature(parseResult.signature, signaturePayload, currentDeviceInfo.KSpub)) {
                LogHelpers.LogDebug(TAG, "Failed to verify finish message from device. " +
                        "Device identifier hex string: " + deviceIdentifierHexString);
                return new SignOnControllerResults.ProcessSignOnMessageResult(
                        SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_FINISH_MESSAGE_FAILED_TO_VERIFY,
                        secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE,
                        deviceIdentifierHexString
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new SignOnControllerResults.ProcessSignOnMessageResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_FINISH_MESSAGE_FAILED_TO_VERIFY,
                    secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE,
                    deviceIdentifierHexString
            );
        }

        evaluationTimer_.printLastTimes();

        return new SignOnControllerResults.ProcessSignOnMessageResult(
                SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS,
                secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE,
                deviceIdentifierHexString
        );
    }

    // Expected format of bootstrapping request:
    // {bootstrapping request type and length of whole packet (this is the packet header)},
    // {device identifier type and length, and device identifier},
    // {device capabilities type and length, and device capabilities},
    // {N1 pub type and length, and N1 pub bytes},
    // {signature type and length, and signature bytes (signature is over all previous bytes, including packet header)}

    /**
     * Parse a bootstrapping request to extract primitives related to the sign on protocol.
     * @param bootstrappingRequest Byte array containing bootstrapping request to parse.
     */
    ParseBootstrappingRequestResult parseBootstrappingRequest(byte[] bootstrappingRequest) {

        LogHelpers.LogByteArrayDebug(TAG, "Bytes of bootstrapping request before parsing:", bootstrappingRequest);

        secureSignOnTlvResults.ParseTlvValueResult result;

        byte[] deviceIdentifier, deviceCapabilities, N1pub, signature;
        int bootstrappingRequestTlvLength, deviceIdentifierLength, deviceCapabilitiesLength, N1pubLength, signatureLength;

        byte[] bootstrappingRequestTlvValue = new byte[] {};
        result = secureSignOnTlvParsingHelpers.parseTlvValue(bootstrappingRequest,
                secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for bootstrapping request TLV value: " + result);
            return new ParseBootstrappingRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_PACKET_HEADER
            );
        }
        bootstrappingRequestTlvValue = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        bootstrappingRequestTlvLength = result.tlvLength;

        LogHelpers.LogByteArrayDebug(TAG, "Bytes of bootstrappingRequestTlvValue:", bootstrappingRequestTlvValue);

        result = secureSignOnTlvParsingHelpers.parseTlvValue(bootstrappingRequestTlvValue, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_DEVICE_IDENTIFIER_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for device identifier: " + result);
            return new ParseBootstrappingRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_DEVICE_IDENTIFIER
            );
        }
        deviceIdentifier = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        deviceIdentifierLength = result.tlvLength;

        result = secureSignOnTlvParsingHelpers.parseTlvValue(bootstrappingRequestTlvValue, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_DEVICE_CAPABILITIES_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for device capabilities: " + result);
            return new ParseBootstrappingRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_DEVICE_CAPABILITIES
            );
        }
        deviceCapabilities = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        deviceCapabilitiesLength = result.tlvLength;

        result = secureSignOnTlvParsingHelpers.parseTlvValue(bootstrappingRequestTlvValue, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_N1_PUB_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for N1pub: " + result);
            return new ParseBootstrappingRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_N1_PUB
            );
        }
        N1pub = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        N1pubLength = result.tlvLength;

        result = secureSignOnTlvParsingHelpers.parseTlvValue(bootstrappingRequestTlvValue,
                secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_SIGNATURE_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for signature: " + result);
            return new ParseBootstrappingRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_SIGNATURE
            );
        }
        signature = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        signatureLength = result.tlvLength;

        return new ParseBootstrappingRequestResult(
                ParseSignOnMessageResultCode.SUCCESS,
                bootstrappingRequestTlvLength,
                deviceIdentifier, deviceIdentifierLength,
                deviceCapabilities, deviceCapabilitiesLength,
                N1pub, N1pubLength,
                signature, signatureLength
        );
    }

    /**
     * Implementation of constructBootstrappingRequestResponse for the basic version of the sign on protocol.
     * @param deviceIdentifierHexString The bytes of the device identifier of the device for which
     *                                  to generate a certificate request response.
     */
    public SignOnControllerResults.ConstructBootstrappingRequestResponseResult
    constructBootstrappingRequestResponse(String deviceIdentifierHexString) {

        SignOnDeviceInfo deviceInfo = devices_.get(deviceIdentifierHexString);

        byte[] N2pubRawBytes = deviceInfo.N2pubRawBytes;
        byte[] secureSignOnCode = deviceInfo.secureSignOnCode;

        int lengthNoPacketHeaderOrSignature =
                secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE + // make room for the N2pub type and length
                        N2pubRawBytes.length + // make room for N2pub
                        secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE + // make room for the anchor certificate type and length
                        anchorCertificate_.wireEncode().getImmutableArray().length; // make room for the trust anchor certificate


        byte[] bootstrappingRequestResponseNoPacketHeaderOrSignature =
                new byte[lengthNoPacketHeaderOrSignature];

        int currentOffset = 0;

        bootstrappingRequestResponseNoPacketHeaderOrSignature[currentOffset] = (byte) secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_N2_PUB_TLV_TYPE;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_SIZE;
        bootstrappingRequestResponseNoPacketHeaderOrSignature[currentOffset] = (byte) N2pubRawBytes.length;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_LENGTH_SIZE;
        try {
            System.arraycopy(N2pubRawBytes, 0, bootstrappingRequestResponseNoPacketHeaderOrSignature, currentOffset, N2pubRawBytes.length);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new SignOnControllerResults.ConstructBootstrappingRequestResponseResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_CONSTRUCTING_BOOTSTRAPPING_REQUEST_RESPONSE_ERROR_ADDING_N2_PUB_RAW_BYTES
            );
        }
        currentOffset += N2pubRawBytes.length;

        byte[] anchorCertificateBytes = anchorCertificate_.wireEncode().getImmutableArray();
        bootstrappingRequestResponseNoPacketHeaderOrSignature[currentOffset] = (byte) secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_ANCHOR_CERTIFICATE_TLV_TYPE;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_SIZE;
        bootstrappingRequestResponseNoPacketHeaderOrSignature[currentOffset] = (byte) anchorCertificateBytes.length;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_LENGTH_SIZE;
        try {
            System.arraycopy(anchorCertificateBytes, 0, bootstrappingRequestResponseNoPacketHeaderOrSignature, currentOffset,
                    anchorCertificateBytes.length);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new SignOnControllerResults.ConstructBootstrappingRequestResponseResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_CONSTRUCTING_BOOTSTRAPPING_REQUEST_RESPONSE_ERROR_ADDING_TRUST_ANCHOR_CERTIFICATE
            );
        }
        currentOffset += anchorCertificateBytes.length;

        LogHelpers.LogByteArrayDebug(TAG, "Calculating bootstrapping request signature over these bytes:",
                bootstrappingRequestResponseNoPacketHeaderOrSignature);

        byte[] signature = Common.computeHmacWithSha256(secureSignOnCode,
                ByteBuffer.wrap(bootstrappingRequestResponseNoPacketHeaderOrSignature));

        LogHelpers.LogByteArrayDebug(TAG, "Signature calculated for bootstrapping request:", signature);

        int totalLength =
                secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE + // account for the packet header (bootstrapping request response tlv type and length)
                        bootstrappingRequestResponseNoPacketHeaderOrSignature.length + // account for the signature payload portion
                        secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE + // account for the signature tlv type and length
                        signature.length; // account for the actual signature bytes

        byte[] bootstrappingRequestResponse = new byte[totalLength];

        currentOffset = 0;

        bootstrappingRequestResponse[currentOffset] = (byte) secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_RESPONSE_TLV_TYPE;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_SIZE;
        bootstrappingRequestResponse[currentOffset] = (byte) (totalLength - secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE);
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_LENGTH_SIZE;

        // copy over the signature payload bytes into the final bootstrapping request response byte array
        System.arraycopy(bootstrappingRequestResponseNoPacketHeaderOrSignature, 0, bootstrappingRequestResponse, currentOffset,
                bootstrappingRequestResponseNoPacketHeaderOrSignature.length);
        currentOffset += bootstrappingRequestResponseNoPacketHeaderOrSignature.length;

        bootstrappingRequestResponse[currentOffset] = (byte) secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_SIGNATURE_TLV_TYPE;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_SIZE;
        bootstrappingRequestResponse[currentOffset] = (byte) signature.length;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_LENGTH_SIZE;
        System.arraycopy(signature, 0, bootstrappingRequestResponse, currentOffset, signature.length);

        return new SignOnControllerResults.ConstructBootstrappingRequestResponseResult(
                SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS,
                deviceIdentifierHexString,
                bootstrappingRequestResponse
        );

    }

    // Expected format of certificate request:
    // {certificate request type and length of whole packet (this is the packet header)},
    // {device identifier type and length, and device identifier},
    // {N1 pub type and length, and N1 pub bytes},
    // {N2 pub digest type and length, and N2 pub digest bytes},
    // {trust anchor digest type and length, and trust anchor digest bytes},
    // {signature type and length, and signature bytes (signature is over all previous bytes, including packet header)}

    /**
     * Parse a certificate request to extract primitives related to the sign on protocol.
     * @param certificateRequest Byte array containing certificate request to parse.
     */
    ParseCertificateRequestResult parseCertificateRequest(byte[] certificateRequest) {
        LogHelpers.LogByteArrayDebug(TAG, "Bytes of certificate request before parsing:", certificateRequest);

        secureSignOnTlvResults.ParseTlvValueResult result;

        byte[] deviceIdentifier, N1pub, N2pubDigest, trustAnchorCertDigest, signature;
        int certificateRequestTlvLength, deviceIdentifierLength, N1pubLength, N2pubDigestLength, trustAnchorCertDigestLength, signatureLength;

        byte[] certificateRequestTlvValue = new byte[] {};
        result = secureSignOnTlvParsingHelpers.parseTlvValue(certificateRequest, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for certificate request TLV value: " + result);
            return new ParseCertificateRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_PACKET_HEADER
            );
        }
        certificateRequestTlvValue = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        certificateRequestTlvLength = result.tlvLength;

        LogHelpers.LogByteArrayDebug(TAG, "Bytes of certificateRequestTlvValue:", certificateRequestTlvValue);

        result = secureSignOnTlvParsingHelpers.parseTlvValue(certificateRequestTlvValue, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_DEVICE_IDENTIFIER_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for device identifier: " + result);
            return new ParseCertificateRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_DEVICE_IDENTIFIER
            );
        }
        deviceIdentifier = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        deviceIdentifierLength = result.tlvLength;

        result = secureSignOnTlvParsingHelpers.parseTlvValue(certificateRequestTlvValue, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_N1_PUB_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for N1 pub: " + result);
            return new ParseCertificateRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_N1_PUB
            );
        }
        N1pub = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        N1pubLength = result.tlvLength;

        result = secureSignOnTlvParsingHelpers.parseTlvValue(certificateRequestTlvValue, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_N2_PUB_DIGEST_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for N2 pub digest: " + result);
            return new ParseCertificateRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_N2_PUB_DIGEST
            );
        }
        N2pubDigest = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        N2pubDigestLength = result.tlvLength;

        result = secureSignOnTlvParsingHelpers.parseTlvValue(certificateRequestTlvValue, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_TRUST_ANCHOR_CERTIFICATE_DIGEST_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for trust anchor certificate digest: " + result);
            return new ParseCertificateRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_TRUST_ANCHOR_CERTIFICATE_DIGEST
            );
        }
        trustAnchorCertDigest = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        trustAnchorCertDigestLength = result.tlvLength;

        result = secureSignOnTlvParsingHelpers.parseTlvValue(certificateRequestTlvValue, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_SIGNATURE_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for signature: " + result);
            return new ParseCertificateRequestResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_SIGNATURE
            );
        }

        LogHelpers.LogDebug(TAG, "Length of signature from parsing certificate request:" + result.tlvLength);
        LogHelpers.LogByteArrayDebug(TAG, "Value of signature from certificate request:", result.tlvValue);

        signature = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        signatureLength = result.tlvLength;

        return new ParseCertificateRequestResult(
                ParseSignOnMessageResultCode.SUCCESS,
                certificateRequestTlvLength,
                deviceIdentifier, deviceIdentifierLength,
                N1pub, N1pubLength,
                N2pubDigest, N2pubDigestLength,
                trustAnchorCertDigest, trustAnchorCertDigestLength,
                signature, signatureLength
        );
    }

    /**
     * Implementation of constructCertificateRequestResponse for teh basic version of the sign on protocol.
     * @param deviceIdentifierHexString The bytes of the device identifier of the device for which
     *                                  to generate a certificate request response.
     */
    public SignOnControllerResults.ConstructCertificateRequestResponseResult
    constructCertificateRequestResponse(String deviceIdentifierHexString) {

        SignOnDeviceInfo currentDeviceInfo = devices_.get(deviceIdentifierHexString);

        LogHelpers.LogDebug(TAG, "constructCertificateRequestResponse of SignOnBasicController got called.");

        KeyPair KdKeyPair = null;
        try {
            KdKeyPair = generateKDKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new SignOnControllerResults.ConstructCertificateRequestResponseResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_CONSTRUCTING_CERTIFICATE_REQUEST_RESPONSE_FAILED_TO_GENERATE_KEYPAIR_FOR_DEVICE
            );
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return new SignOnControllerResults.ConstructCertificateRequestResponseResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_CONSTRUCTING_CERTIFICATE_REQUEST_RESPONSE_FAILED_TO_GENERATE_KEYPAIR_FOR_DEVICE
            );
        }

        byte[] KdPubRawBytes = getKDPubRawBytes(KdKeyPair.getPublic());
        byte[] KdPriRawBytes = getKDPriRawBytes(KdKeyPair.getPrivate());

        Blob KdPriRawBlob = new Blob(KdPriRawBytes);
        LogHelpers.LogDebug(TAG, "Hex string of KdPriRawBytes: " + KdPriRawBlob.toHex());

        long currentTime = System.currentTimeMillis();

        CertificateV2 KDPubCertificate = new CertificateV2();
        try {
            KDPubCertificate.setContent(new Blob(SecurityHelpers.asnEncodeRawECPublicKeyBytes(KdPubRawBytes)));
        } catch (Exception e) {
            e.printStackTrace();
            return new SignOnControllerResults.ConstructCertificateRequestResponseResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_CONSTRUCTING_CERTIFICATE_REQUEST_RESPONSE_FAILED_TO_GENERATE_CERTIFICATE_FOR_DEVICE
            );
        }

        KDPubCertificate.setName(new Name(SignOnControllerConsts.KD_PUB_CERTIFICATE_NAME_PREFIX + deviceIdentifierHexString));

        // need to add code here later to sign the KD pub certificate by trust anchor

        LogHelpers.LogDebug(TAG, "Hex string of generated KDpubCertificate: " +
                new Blob(KDPubCertificate.wireEncode()).toHex());

        try {
            LogHelpers.LogDebug(TAG, "Hex string of KD public key within KDpubCertificate: " +
                    KDPubCertificate.getPublicKey());
        } catch (CertificateV2.Error error) {
            error.printStackTrace();
        }

        currentDeviceInfo.KDKeyPair = KdKeyPair;
        currentDeviceInfo.KDPubCertificate = KDPubCertificate;

        //byte[] KdPriRawBytes = getKDPriRawBytes(currentDeviceInfo.KDKeyPair.getPrivate());

        LogHelpers.LogByteArrayDebug(TAG, "Hex string of Kd pri: ", KdPriRawBytes);

        // only use the first 16 bytes of symmetric key
        byte[] encryptionKey = new byte[16];
        System.arraycopy(devices_.get(deviceIdentifierHexString).KT, 0, encryptionKey, 0, 16);

        LogHelpers.LogByteArrayDebug(TAG, "Encrypting Kd pri with this key", encryptionKey);

        byte[] encryptedKdPri = null;
        try {
            encryptedKdPri = SecurityHelpers.encryptAesCbcPkcs5Padding(KdPriRawBytes, encryptionKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        LogHelpers.LogByteArrayDebug(TAG, "Bytes of encrypted KDpri", encryptedKdPri);

        CertificateV2 KdPubCertificate = devices_.get(deviceIdentifierHexString).KDPubCertificate;

        byte[] diffieHellmanSharedSecret = currentDeviceInfo.KT;

        int lengthWithoutPacketHeaderOrSignature =
                secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE + // make room for the N2pub type and length
                        encryptedKdPri.length + // make room for kd pri encrypted
                        secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE + // make room for the kd pub certificate type and length
                        KdPubCertificate.wireEncode().getImmutableArray().length; // make room for the Kd pub certificate

        byte[] certificateRequestResponseNoPacketHeaderOrSignature =
                new byte[lengthWithoutPacketHeaderOrSignature];

        int currentOffset = 0;

        certificateRequestResponseNoPacketHeaderOrSignature[currentOffset] = (byte) secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_KD_PRI_ENCRYPTED_TLV_TYPE;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_SIZE;
        certificateRequestResponseNoPacketHeaderOrSignature[currentOffset] = (byte) encryptedKdPri.length;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_LENGTH_SIZE;
        try {
            System.arraycopy(encryptedKdPri, 0, certificateRequestResponseNoPacketHeaderOrSignature, currentOffset, encryptedKdPri.length);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new SignOnControllerResults.ConstructCertificateRequestResponseResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_CONSTRUCTING_CERTIFICATE_REQUEST_RESPONSE_ERROR_ADDING_ENCRYPTED_KD_PRI
            );
        }
        currentOffset += encryptedKdPri.length;

        byte[] kdPubCertificateBytes = KdPubCertificate.wireEncode().getImmutableArray();
        certificateRequestResponseNoPacketHeaderOrSignature[currentOffset] = (byte) secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_KD_PUB_CERTIFICATE_TLV_TYPE;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_SIZE;
        certificateRequestResponseNoPacketHeaderOrSignature[currentOffset] = (byte) kdPubCertificateBytes.length;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_LENGTH_SIZE;
        try {
            System.arraycopy(kdPubCertificateBytes, 0, certificateRequestResponseNoPacketHeaderOrSignature, currentOffset,
                    kdPubCertificateBytes.length);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new SignOnControllerResults.ConstructCertificateRequestResponseResult(
                    SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_CONSTRUCTING_CERTIFICATE_REQUEST_RESPONSE_ERROR_ADDING_KD_PUB_CERTIFICATE
            );
        }
        currentOffset += kdPubCertificateBytes.length;

        LogHelpers.LogByteArrayDebug(TAG, "Calculating certificate request signature over these bytes:",
                certificateRequestResponseNoPacketHeaderOrSignature);

        byte[] signature = Common.computeHmacWithSha256(diffieHellmanSharedSecret,
                ByteBuffer.wrap(certificateRequestResponseNoPacketHeaderOrSignature));

        LogHelpers.LogByteArrayDebug(TAG, "Signature calculated for certificate request:", signature);

        int totalLength =
                secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE + // account for the packet header (certificate request response tlv type and length)
                        certificateRequestResponseNoPacketHeaderOrSignature.length + // account for the signature payload portion
                        secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE + // account for the signature tlv type and length
                        signature.length; // account for the actual signature bytes

        byte[] certificateRequestResponse = new byte[totalLength];

        currentOffset = 0;

        certificateRequestResponse[currentOffset] = (byte) secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_RESPONSE_TLV_TYPE;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_SIZE;
        certificateRequestResponse[currentOffset] = (byte) (totalLength - secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_AND_LENGTH_SIZE);
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_LENGTH_SIZE;

        // copy over the signature payload bytes into the final certificate request response byte array
        System.arraycopy(certificateRequestResponseNoPacketHeaderOrSignature, 0, certificateRequestResponse,
                currentOffset, certificateRequestResponseNoPacketHeaderOrSignature.length);
        currentOffset += certificateRequestResponseNoPacketHeaderOrSignature.length;

        certificateRequestResponse[currentOffset] = (byte) secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_SIGNATURE_TLV_TYPE;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_TYPE_SIZE;
        certificateRequestResponse[currentOffset] = (byte) signature.length;
        currentOffset += secureSignOnTlvConsts.SECURE_SIGN_ON_TLV_LENGTH_SIZE;
        System.arraycopy(signature, 0, certificateRequestResponse, currentOffset, signature.length);

        return new SignOnControllerResults.ConstructCertificateRequestResponseResult(
                SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS,
                deviceIdentifierHexString,
                certificateRequestResponse
        );

    }

    // Expected format of finish message:
    // {finish message type and length of whole packet (this is the packet header)},
    // {device identifier type and length, and device identifier},
    // {signature type and length, and signature bytes (signature is over all previous bytes, including packet header)}

    /**
     * Parse a finish message to extract primitives related to the sign on protocol.
     * @param finishMessage Byte array containing finish message to parse.
     */
    ParseFinishMessageResult parseFinishMessage(byte[] finishMessage) {

        LogHelpers.LogByteArrayDebug(TAG, "Bytes of finish message before parsing:", finishMessage);

        secureSignOnTlvResults.ParseTlvValueResult result;

        byte[] deviceIdentifier, signature;
        int deviceIdentifierLength, finishMessageTlvLength, signatureLength;

        byte[] finishMessageTlvValue = new byte[] {};
        result = secureSignOnTlvParsingHelpers.parseTlvValue(finishMessage, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for certificate request TLV value: " + result);
            return new ParseFinishMessageResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_PACKET_HEADER
            );
        }
        finishMessageTlvValue = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        finishMessageTlvLength = result.tlvLength;

        LogHelpers.LogByteArrayDebug(TAG, "Bytes of finishMessageTlvValue:", finishMessageTlvValue);

        result = secureSignOnTlvParsingHelpers.parseTlvValue(finishMessageTlvValue, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_DEVICE_IDENTIFIER_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for device identifier: " + result);
            return new ParseFinishMessageResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_DEVICE_IDENTIFIER
            );
        }
        deviceIdentifier = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        deviceIdentifierLength = result.tlvLength;

        result = secureSignOnTlvParsingHelpers.parseTlvValue(finishMessageTlvValue, secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_SIGNATURE_TLV_TYPE);
        if (result.resultCode != secureSignOnTlvResultCodes.ParseTlvValueResultCode.SUCCESS) {
            Log.e(TAG, "Error parsing for signature: " + result);
            return new ParseFinishMessageResult(
                    ParseSignOnMessageResultCode.ERROR_PARSING_SIGNATURE
            );
        }
        signature = Arrays.copyOf(result.tlvValue, result.tlvValue.length);
        signatureLength = result.tlvLength;

        return new ParseFinishMessageResult(
                ParseSignOnMessageResultCode.SUCCESS,
                finishMessageTlvLength,
                deviceIdentifier, deviceIdentifierLength,
                signature, signatureLength
        );
    }

    /**
     * Generate a key pair for the diffie hellman exchange of the sign on basic protocol. Specific details
     * regarding the keys generated (type of key, curve if ecc key, etc) are described in the sign on basic
     * variant implementation.
     */
    protected abstract KeyPair generateDiffieHellmanKeyPair() throws Exception;

    /**
     * Generate the shared secret of the diffie hellman exchange of the sign on basic protocol. Specific details
     * regarding the keys that shuold be passed in (type of key, curve if ecc key, etc) are described in the sign
     * on basic variant implementation.
     * @param N1publicKey N1 key pair public key in PublicKey format.
     * @param N2privateKey N2 key pair private key in PrivateKey format.
     */
    protected abstract byte[] deriveDiffieHellmanSharedSecret(PublicKey N1publicKey, PrivateKey N2privateKey)
            throws NoSuchAlgorithmException, InvalidKeyException;

    /**
     * Verify the signature of a bootstrapping request. Specific details regarding the signature of the bootstrapping
     * request are described in the sign on basic variant implementation.
     * @param bootstrappingRequestSignature The bytes of the signature to verify.
     * @param bootstrappingRequestSignaturePayload The bytes over which the signature was calculated. Currently,
     *                                             this is all of the bytes of the bootstrapping request, excluding
     *                                             the packet header (as in, excluding the bootstrapping request
     *                                             tlv type and length) and signature tlv block (as in, excluding
     *                                             the signature tlv, type, length, and value).
     * @param publicKey Key to verify the bootstrapping request signature with, in PublicKey format.
     */
    protected abstract boolean verifyBootstrappingRequestSignature(byte[] bootstrappingRequestSignature,
            byte[] bootstrappingRequestSignaturePayload, PublicKey publicKey) throws Exception;

    /**
     * Verify the signature of a certificate request.
     * @param certificateRequestSignature The bytes of the signature to verify.
     * @param certificateRequestSignaturePayload The bytes over which the signature was calculated. Currently,
     *                                             this is all of the bytes of the certificate request, excluding
     *                                             the packet header (as in, excluding the certificate request
     *                                             tlv type and length) and signature tlv block (as in, excluding
     *                                             the signature tlv, type, length, and value).
     * @param publicKey Key to verify the certificate request signature with, in PublicKey format.
     */
    protected abstract boolean verifyCertificateRequestSignature(byte[] certificateRequestSignature,
                                                                 byte[] certificateRequestSignaturePayload,
                                                                 PublicKey publicKey) throws Exception;

    /**
     * Verify the signature of a finish message.
     * @param finishMessageSignature The bytes of the signature to verify.
     * @param finishMessageSignaturePayload The bytes over which the signature was calculated. Currently,
     *                                             this is all of the bytes of the certificate request, excluding
     *                                             the packet header (as in, excluding the certificate request
     *                                             tlv type and length) and signature tlv block (as in, excluding
     *                                             the signature tlv, type, length, and value).
     * @param publicKey Key to verify the finish message signature with, in PublicKey format.
     */
    protected abstract boolean verifyFinishMessageSignature(byte[] finishMessageSignature, byte[] finishMessageSignaturePayload,
                                                            PublicKey publicKey) throws Exception;

    /**
     * Convert the N1 key pair public key in "raw" format into a PublicKey object. The "raw" format is defined in the
     * sign on basic variant implementation.
     * @param N1pubRawBytes The N1 key pair public key in "raw" format.
     */
    protected abstract PublicKey convertRawN1PubBytesToPublicKeyObject(byte[] N1pubRawBytes) throws Exception;

    /**
     * Get the N2 key pair public key in "raw" format. The "raw" format is defined in the sign on basic variant
     * implementation.
     * @param publicKey The N2 key pair public key in PublicKey object format.
     */
    protected abstract byte[] getN2pubRawBytes(PublicKey publicKey);

    /**
     * Get the KD key pair public key in "raw" format. The "raw" format is defined in the sign on basic variant
     * implementation.
     * @param publicKey The KD key pair public key in PublicKey object format.
     */
    protected abstract byte[] getKDPubRawBytes(PublicKey publicKey);

    /**
     * Get the KD key pair private key in "raw" format. The "raw" format is defined in the sign on basic variant
     * implementation.
     * @param privateKey The KD key pair private key in PrivateKey object format.
     */
    protected abstract byte[] getKDPriRawBytes(PrivateKey privateKey);

    /**
     * Generate the "Key Device" key pair for the device.
     */
    protected abstract KeyPair generateKDKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    // timer for evaluations for potential future paper
    SignOnBasicControllerTimer evaluationTimer_ = new SignOnBasicControllerTimer();

}
