
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn;

import android.util.Log;

import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import NDNLiteSupport.LogHelpers;

import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.SecureSignOnVariantStrings.SIGN_ON_VARIANT_BASIC_ECC_160;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.SecureSignOnVariantStrings.SIGN_ON_VARIANT_BASIC_ECC_256;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.SecureSignOnVariantStrings.SIGN_ON_VARIANT_BASIC_ECC_HYBRID;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.utils.SecurityHelpers.convertASNEncodedPublicKeyToPublicKeyObject;

/**
 * The SignOnController class provides methods for parsing and constructing secure sign-on protocol
 * related messages.
 */
public abstract class SignOnController {

    // TAG for logging.
    protected final String TAG = SignOnController.class.getSimpleName();

    /**
     * Create a SignOnController object with a given trust anchor and SecureSignOnControllerCallbacks.
     * @param trustAnchorCertificate Trust anchor certificate to initialize SignOnController with. This will
     *                               be sent to devices undergoing the sign on process.
     */
    public SignOnController(CertificateV2 trustAnchorCertificate) throws Exception {

        devices_ = new HashMap<>();

        if (trustAnchorCertificate == null)
            throw new Exception("Attempted to initialize secure sign-on controller with a null trust anchor certificate.");

        anchorCertificate_ = trustAnchorCertificate;

    }

    /**
     * Get the KD public key certificate of an onboarded device, based on its device
     * identifier in hex string format. If the device cannot be found in the list of devices that have
     * undergone the sign on process, or the device has not yet completed onboarding, will return null.
     * @param deviceIdentifierHexString The bytes of the device identifier of the device for which
     *                                  to retrieve the KD public key certificate.
     */
    public CertificateV2 getKDPubCertificateForDevice(String deviceIdentifierHexString) {
        if (!devices_.containsKey(deviceIdentifierHexString)) {
            return null;
        }
        return devices_.get(deviceIdentifierHexString).KDPubCertificate;
    }

    /**
     * Return a array list of the device identifiers of devices that have successfully undergone
     * the sign on protocol, in hex string format.
     */
    public ArrayList<String> getSuccessfullySignedOnDeviceIdentifiers() {
        ArrayList<String> successfullySignedOnDeviceIdentifiers = new ArrayList<>();

        for (SignOnDeviceInfo info : devices_.values()) {
            if (info.onboarded) {
                successfullySignedOnDeviceIdentifiers.add(info.deviceIdentifierHexString);
            }
        }

        return successfullySignedOnDeviceIdentifiers;
    }

    /**
     * Add a device that is pending sign on. This is how the SignOnController knows to expect a
     * bootstrapping request from a device with a particular device identifier. If this function is
     * not called for a device with a particular device identifier, bootstrapping requests from that
     * device will not be treated as valid.
     * @param deviceIdentifierBytes The bytes of the device identifier of the device for which
     *                              to expect a bootstrapping request.
     * @param secureSignOnCodeBytes The bytes of the pre-shared secure sign on code between the controller
     *                              and device.
     * @param KSpubCertificate The public key certificate of the KS key pair (the pre-shared asymmetric
     *                         key pair between the controller and device). The parsing of the certificate is
     *                         done using the secureSignOnVariantType_ member variable, which is set
     *                         in the constructors of child classes of SignOnController.
     */
    public void addDevicePendingSignOn(byte[] deviceIdentifierBytes, byte[] secureSignOnCodeBytes,
                                       CertificateV2 KSpubCertificate) {

        Blob deviceIdentifierBlob = new Blob(deviceIdentifierBytes);
        String deviceIdentifierHexString = deviceIdentifierBlob.toHex();
        LogHelpers.LogDebug(TAG, "Device identifier hex string being added to device list: " + deviceIdentifierHexString);

        Blob secureSignOnCodeBlob = new Blob(secureSignOnCodeBytes);
        String secureSignOnCodeString = secureSignOnCodeBlob.toHex();
        LogHelpers.LogDebug(TAG, "Hex string of secure sign on code for this device: " + secureSignOnCodeString);

        if (!devices_.containsKey(deviceIdentifierHexString)) {

            SignOnDeviceInfo deviceInfo = new SignOnDeviceInfo();

            deviceInfo.deviceIdentifierHexString = deviceIdentifierHexString;
            deviceInfo.KSpubCertificate = KSpubCertificate;
            deviceInfo.secureSignOnCode = Arrays.copyOf(secureSignOnCodeBytes, secureSignOnCodeBytes.length);

            if (secureSignOnVariantType_.equals(SIGN_ON_VARIANT_BASIC_ECC_160) ||
                    secureSignOnVariantType_.equals(SIGN_ON_VARIANT_BASIC_ECC_HYBRID) ||
                    secureSignOnVariantType_.equals(SIGN_ON_VARIANT_BASIC_ECC_256)) {

                byte[] BKpubASNEncoded = null;
                try {
                    BKpubASNEncoded = KSpubCertificate.getPublicKey().getImmutableArray();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                Blob BKpubASNEncodedBlob = new Blob(BKpubASNEncoded);
                LogHelpers.LogDebug(TAG, "Hex string of KSpub ASN encoded: " + BKpubASNEncodedBlob.toHex());

                PublicKey BKpub = null;
                try {
                    BKpub = convertASNEncodedPublicKeyToPublicKeyObject(BKpubASNEncoded, "EC");
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                    return;
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return;
                }

                deviceInfo.KSpub = BKpub;

            }
            else {
                LogHelpers.LogDebug(TAG, "Unrecognized secure sign on variant type: " + secureSignOnVariantType_);
            }

            devices_.put(deviceIdentifierHexString, deviceInfo);
        }

    }

    /**
     * Process a sign on message.
     * @param data The bytes of the sign on message to process.
     * @return Will return a ProcessSignOnMessageResult with a result code of
     *         SignOnControllerResultCode.SUCCESS if it is passed a properly
     *         formatted sign-on message, that is also a valid sign on message for
     *         the corresponding device's current sign on process (i.e., even if the
     *         sign on message is properly formatted, if the sign on message's signature
     *
     */
    public SignOnControllerResults.ProcessSignOnMessageResult processSignOnMessage(byte[] data) {

        LogHelpers.LogDebug(TAG, "Received sign on message for processing.");

        LogHelpers.LogByteArrayDebug(TAG, "Raw bytes of sign-on message being processed:", data);

        long signOnMessageType =  Byte.toUnsignedInt(data[0]);

        LogHelpers.LogDebug(TAG, "Value of first byte of potential sign on message: " + signOnMessageType);

        if (signOnMessageType == SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE) {

                LogHelpers.LogDebug(TAG, "Received a bootstrapping request.");

                SignOnControllerResults.ProcessSignOnMessageResult processResult = processBootstrappingRequest(data);
                if (!processResult.resultCode.equals(SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS)) {
                    Log.e(TAG, "Error processing bootstrapping request: " + processResult);
                    return processResult;
                }

                return new SignOnControllerResults.ProcessSignOnMessageResult(
                        SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS,
                        signOnMessageType,
                        processResult.deviceIdentifierHexString
                );
        }
        else if (signOnMessageType == SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE) {

                LogHelpers.LogDebug(TAG, "Received a certificate request.");

                SignOnControllerResults.ProcessSignOnMessageResult processResult = processCertificateRequest(data);
                if (!processResult.resultCode.equals(SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS)) {
                    Log.e(TAG, "Error processing certificate request: " + processResult.resultCode);
                    return processResult;
                }

                return new SignOnControllerResults.ProcessSignOnMessageResult(
                        SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS,
                        signOnMessageType,
                        processResult.deviceIdentifierHexString
                );
        }
        else if (signOnMessageType == SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE) {
                LogHelpers.LogDebug(TAG, "Received a finish message.");

                SignOnControllerResults.ProcessSignOnMessageResult processResult = processFinishMessage(data);
                if (!processResult.resultCode.equals(SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS)) {
                    Log.e(TAG, "Error processing finish message: " + processResult);
                    return processResult;
                }

                LogHelpers.LogDebug(TAG, "Successfully onboarded a device with device identifier hex string: " +
                        processResult.deviceIdentifierHexString);
                devices_.get(processResult.deviceIdentifierHexString).onboarded = true;

                return new SignOnControllerResults.ProcessSignOnMessageResult(
                        SignOnControllerResultCodes.SignOnControllerResultCode.SUCCESS,
                        signOnMessageType,
                        processResult.deviceIdentifierHexString
                );
        }
        else {
                return new SignOnControllerResults.ProcessSignOnMessageResult(
                        SignOnControllerResultCodes.SignOnControllerResultCode.ERROR_PROCESSING_UNRECOGNIZED_MESSAGE_TYPE,
                        data[0]
                );
        }
    }

    /**
     * Construct a bootstrapping request response for a device undergoing the sign on process based on
     * its device identifier in hex string format. Information stored within the SignOnController object
     * will be used to properly construct the bootstrapping request response for the appropriate device.
     * @param deviceIdentifierHexString The bytes of the device identifier of the device for which
     *                                  to generate a bootstrapping request response.
     */
    public abstract SignOnControllerResults.ConstructBootstrappingRequestResponseResult constructBootstrappingRequestResponse(String deviceIdentifierHexString);

    /**
     * Construct a certificate request response for a device undergoing the sign on process based on
     * its device identifier in hex string format. Information stored within the SignOnController object
     * will be used to properly construct the certificate request response for the appropriate device.
     * @param deviceIdentifierHexString The bytes of the device identifier of the device for which
     *                                  to generate a certificate request response.
     */
    public abstract SignOnControllerResults.ConstructCertificateRequestResponseResult constructCertificateRequestResponse(String deviceIdentifierHexString);

    /**
     * Process a bootstrapping request for a device undergoing the sign on process based on
     * its device identifier in hex string format.
     * @param bootstrappingRequest Byte array containing bootstrapping request to process.
     */
    protected abstract SignOnControllerResults.ProcessSignOnMessageResult processBootstrappingRequest(byte[] bootstrappingRequest);

    /**
     * Process a certificate request for a device undergoing the sign on process based on
     * its device identifier in hex string format.
     * @param certificateRequest Byte array containing certificate request to process.
     */
    protected abstract SignOnControllerResults.ProcessSignOnMessageResult processCertificateRequest(byte[] certificateRequest);

    /**
     * Process a finish message for a device undergoing the sign on process based on
     * its device identifier in hex string format.
     * @param finishMessage Byte array containing finish message to process.
     */
    protected abstract SignOnControllerResults.ProcessSignOnMessageResult processFinishMessage(byte[] finishMessage);

    protected CertificateV2 anchorCertificate_;
    // mapping of device identifier hex strings to sign on related state for a device
    protected HashMap<String, SignOnDeviceInfo> devices_;
    // the variant of sign on. this is used to properly parse the BKpubCertificate passed in through
    // addDevicePendingSignOn
    protected String secureSignOnVariantType_;

}
