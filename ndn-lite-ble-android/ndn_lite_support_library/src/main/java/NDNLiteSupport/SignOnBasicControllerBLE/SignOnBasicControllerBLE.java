
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE;

import android.bluetooth.BluetoothGatt;
import android.util.Log;

import net.named_data.jndn.security.v2.CertificateV2;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import NDNLiteSupport.LogHelpers;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnController;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnControllerResults;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic.ecc.SignOnBasicControllerECC256;
import NDNLiteSupport.transport.ble.BLECentralTransport;
import NDNLiteSupport.transport.ble.BLEManager;
import NDNLiteSupport.transport.ble.BLEScanner;

import static NDNLiteSupport.NDNLiteAndroidSupportConsts.NDN_LITE_BLE_UNICAST_SERVICE_UUID;
import static NDNLiteSupport.NDNLiteAndroidSupportConsts.SIGN_ON_CONTROLLER_BLE_SEND_CODE;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnControllerResultCodes.SignOnControllerResultCode;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.SecureSignOnVariantStrings.SIGN_ON_VARIANT_BASIC_ECC_256;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.tlv.secureSignOnTlvConsts.SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE;

public class SignOnBasicControllerBLE {

    // TAG for logging.
    private static final String TAG = SignOnBasicControllerBLE.class.getSimpleName();

    private SignOnBasicControllerBLE(){}

    private static class SingletonHelper{
        private static final SignOnBasicControllerBLE INSTANCE = new SignOnBasicControllerBLE();
    }

    /**
     * Get a reference to the SignOnBasicControllerBLE singleton.
     */
    public static SignOnBasicControllerBLE getInstance(){
        return SingletonHelper.INSTANCE;
    }

    /**
     * Callbacks for users of the SecureSignOnBasicControllerBLE to implement.
     */
    public interface SecureSignOnBasicControllerBLECallbacks {

        /**
         * When the sign on process completes successfully for a device.
         *
         * @param deviceIdentifierHexString The bytes of the device identifier of the device for which
         *                                  the sign on process completed successfully.
         */
        void onDeviceSignOnComplete(String deviceIdentifierHexString);

        /**
         * When an error occurs during the sign on process. This does not necessarily mean that
         * sign on has failed for a particular device (if deviceIdentifierHexString is not null),
         * just that something strange occurred during processing / responding to sign on messages.
         *
         * NOTE: If a device sends an improperly formatted sign on message, but then sends the
         * same message again properly formatted, you may receive a callback for a sign on error
         * for that device, followed by a onDeviceSignOnComplete call for that device. The rationale
         * for this being acceptable behavior is that the onDeviceSignOnError is not meant to indicate
         * sign on failure, it is only meant to be extra useful information for the user.
         *
         * NOTE: This callback will not report BLE transport related errors, it will only report errors
         * related to the processing / creation of sign on protocol messages. This is not ideal behavior,
         * and future work is to edit this callback to also report on BLE transport errors, since this
         * should be the job of this SignOnBasicControllerBLE object, as it encapsulates both transport
         * and sign on protocol behavior.
         *
         * @param deviceIdentifierHexString The bytes of the device identifier of the device for which
         *                                  the sign on process had an error. If processing for a sign on
         *                                  message fails such that the device identifier hex string cannot
         *                                  be parsed from it, this will be null.
         * @param resultCode Provides information on what sign on error occurred.
         */
        void onDeviceSignOnError(String deviceIdentifierHexString, SignOnControllerResultCode resultCode);

    }

    /**
     * Callbacks for the BLEManager, which handles BLE transport related issues like sending and
     * receiving data from devices that undergo the sign on protocol.
     */
    private BLEManager.BLEManagerObserver bleManagerCallbacks_ = new BLEManager.BLEManagerObserver() {
        @Override
        public void onDeviceEnteredRange(BLEScanner.BluetoothDeviceAndScanRecord deviceAndScanRecord) {

        }

        @Override
        public void onDevicesLeftRange(ArrayList<BLEScanner.BluetoothDeviceAndScanRecord> devicesAndScanRecords) {

        }

        @Override
        public void onScanFailed(int errorCode) {

        }

        @Override
        public void onScanCycleEnded() {

        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, BLECentralTransport.ConnectionState newState, BLECentralTransport.BCTResultCode resultCode) {
            String deviceMacAddress = gatt.getDevice().getAddress();
            if (resultCode.equals(BLECentralTransport.BCTResultCode.SUCCESS)) {
                if (newState.equals(BLECentralTransport.ConnectionState.STATE_MTU_NEGOTIATED)) {

                    m_successfullyNegotiatedMtuDevices.add(deviceMacAddress);
                    if (m_bootstrappingRequests.containsKey(deviceMacAddress)) {
                        LogHelpers.LogDebug(TAG, "Found this mac address had a buffered bootstrapping " +
                                "request: " + deviceMacAddress);
                        BootstrappingRequestInfo info = m_bootstrappingRequests.get(deviceMacAddress);
                        respondToBootstrappingRequest(info.processResult, info.serviceUuid, deviceMacAddress);
                        m_bootstrappingRequests.remove(info);
                    }
                }
            }
            if (newState.equals(BLECentralTransport.ConnectionState.STATE_DISCONNECTED)) {
                if (m_successfullyNegotiatedMtuDevices.contains(deviceMacAddress)) {
                    m_successfullyNegotiatedMtuDevices.remove(deviceMacAddress);
                }
            }
        }

        @Override
        public void onSendDataResult(BluetoothGatt gatt, UUID serviceUuid, BLECentralTransport.BCTResultCode resultCode, long sendCode, byte[] dataSent) {
            if (resultCode == BLECentralTransport.BCTResultCode.SUCCESS) {
                if (sendCode == SIGN_ON_CONTROLLER_BLE_SEND_CODE) {
                    LogHelpers.LogDebug(TAG, "Successfully sent for sign on basic controller BLE.");
                    LogHelpers.LogByteArrayDebug(TAG, "Bytes sent", dataSent);

                    String dataSentAsString = null;
                    try {
                        dataSentAsString = new String(dataSent, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    LogHelpers.LogDebug(TAG, "Data that got written (as string): " + dataSentAsString);
                    LogHelpers.LogByteArrayDebug(TAG, "Data that got written (hex dump): ", dataSent);
                }
            }
            else {
                Log.e(TAG, "Error in send data result: " + resultCode);
            }
        }

        @Override
        public void onReceivedDataResult(BluetoothGatt gatt, UUID serviceUuid, BLECentralTransport.BCTResultCode resultCode, byte[] data) {
            if (resultCode == BLECentralTransport.BCTResultCode.SUCCESS) {
                if (serviceUuid.equals(NDN_LITE_BLE_UNICAST_SERVICE_UUID)) {

                    SignOnControllerResults.ProcessSignOnMessageResult processResult =
                            m_SignOnController.processSignOnMessage(data);

                    if (!processResult.resultCode.equals(SignOnControllerResultCode.SUCCESS)) {
                        Log.e(TAG, "Error processing sign on message, error: " + processResult.resultCode);
                        m_callbacks.onDeviceSignOnError(processResult.deviceIdentifierHexString, processResult.resultCode);
                        return;
                    }

                    m_deviceIdentifierHexStringToMacAddress.put(processResult.deviceIdentifierHexString, gatt.getDevice().getAddress());

                    if (processResult.signOnMessageTlvType == SECURE_SIGN_ON_BLE_BOOTSTRAPPING_REQUEST_TLV_TYPE) {
                            String deviceMacAddress = gatt.getDevice().getAddress();
                            if (m_successfullyNegotiatedMtuDevices.contains(deviceMacAddress)) {
                                LogHelpers.LogDebug(TAG, "Got a bootstrapping request when " +
                                        "mtu was already successfully negotiated for device with mac " +
                                        "address : " + deviceMacAddress + "; proceeding to process.");
                                respondToBootstrappingRequest(processResult, serviceUuid,
                                        deviceMacAddress);
                            }
                            else {
                                LogHelpers.LogDebug(TAG, "Got a bootstrapping request when " +
                                "mtu was not already successfully negotiated for device with mac " +
                                "address : " + deviceMacAddress + "; buffering bootstrapping request " +
                                        "until mtu is successfully negotiated with device.");
                                m_bootstrappingRequests.put(deviceMacAddress,
                                        new BootstrappingRequestInfo(data, processResult, serviceUuid));
                            }

                    }
                    else if (processResult.signOnMessageTlvType == SECURE_SIGN_ON_BLE_CERTIFICATE_REQUEST_TLV_TYPE) {
                            respondToCertificateRequest(processResult, serviceUuid,
                                    gatt.getDevice().getAddress());

                    }
                    else if (processResult.signOnMessageTlvType == SECURE_SIGN_ON_BLE_FINISH_MESSAGE_TLV_TYPE) {

                            LogHelpers.LogDebug(TAG, "Finished processing sign on ble finish message for device with deviceIdentifierHexString: " +
                                processResult.deviceIdentifierHexString);

                            m_callbacks.onDeviceSignOnComplete(processResult.deviceIdentifierHexString);
                    }
                    else {
                            Log.e(TAG, "Got an unexpected sign on message type: " + processResult.signOnMessageTlvType);
                            m_callbacks.onDeviceSignOnError(processResult.deviceIdentifierHexString, processResult.resultCode);

                    }
                }
            }
            else {
                Log.e(TAG, "Error in received data result: " + resultCode);
            }
        }
    };

    /**
     * Process and respond to a bootstrapping request.
     *
     * @param processResult Result of processing the bootstrapping request.
     * @param
     */
    private void respondToBootstrappingRequest(SignOnControllerResults.ProcessSignOnMessageResult processResult, UUID serviceUuid,
                                               String deviceMacAddress) {
        SignOnControllerResults.ConstructBootstrappingRequestResponseResult constructBootstrappingRequestResponseResult =
                m_SignOnController.constructBootstrappingRequestResponse(processResult.deviceIdentifierHexString);
        if (!constructBootstrappingRequestResponseResult.resultCode.equals(
                SignOnControllerResultCode.SUCCESS)) {
            Log.e(TAG, "Error constructing bootstrapping request response, error: " +
                    constructBootstrappingRequestResponseResult.resultCode);
            m_callbacks.onDeviceSignOnError(processResult.deviceIdentifierHexString,
                    constructBootstrappingRequestResponseResult.resultCode);
            return;
        }

        LogHelpers.LogByteArrayDebug(TAG, "Bytes of constructed bootstrapping request response:",
                constructBootstrappingRequestResponseResult.bootstrappingRequestResponse);

        BLEManager.getInstance().sendData(serviceUuid, deviceMacAddress,
                constructBootstrappingRequestResponseResult.bootstrappingRequestResponse, SIGN_ON_CONTROLLER_BLE_SEND_CODE);
    }

    /**
     * Process and respond to a certificate request.
     */
    private void respondToCertificateRequest(SignOnControllerResults.ProcessSignOnMessageResult processResult, UUID serviceUuid,
                                             String deviceMacAddress) {
        SignOnControllerResults.ConstructCertificateRequestResponseResult constructCertificateRequestResponseResult =
                m_SignOnController.constructCertificateRequestResponse(processResult.deviceIdentifierHexString);
        if (!constructCertificateRequestResponseResult.resultCode.equals(
                SignOnControllerResultCode.SUCCESS)) {
            Log.e(TAG, "Error constructing certificate request respone, error: " +
                    constructCertificateRequestResponseResult.resultCode);
            m_callbacks.onDeviceSignOnError(processResult.deviceIdentifierHexString,
                    constructCertificateRequestResponseResult.resultCode);
            return;
        }

        LogHelpers.LogByteArrayDebug(TAG, "Bytes of constructed certificate request response::",
                constructCertificateRequestResponseResult.certificateRequestResponse);

        BLEManager.getInstance().sendData(serviceUuid, deviceMacAddress,
                constructCertificateRequestResponseResult.certificateRequestResponse, SIGN_ON_CONTROLLER_BLE_SEND_CODE);
    }

    /**
     * Initialize the SignOnBasicControllerBLE singleton.
     *
     * @param signOnVariant Variant of sign on to initialize the SignOnBasicControllerBLE singleton with.
     *                      If null is passed in, then the sign on basic ecc_256 variant will be used.
     * @param callbacks Callbacks for the user of the SignOnBasicControllerBLE to implement.
     * @param trustAnchorCertificate Trust anchor certificate that the SignOnController will be initialized with.
     */
    public void initialize(String signOnVariant, SecureSignOnBasicControllerBLECallbacks callbacks,
                           CertificateV2 trustAnchorCertificate) {

        if (signOnVariant == null) {

            try {
                m_SignOnController = new SignOnBasicControllerECC256(
                        trustAnchorCertificate);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

        }
        else if (signOnVariant.equals(SIGN_ON_VARIANT_BASIC_ECC_256)) {

            try {
                m_SignOnController = new SignOnBasicControllerECC256(
                        trustAnchorCertificate);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

        }
        else {
            Log.e(TAG, "CURRENTLY ONLY " + SIGN_ON_VARIANT_BASIC_ECC_256 + " IS SUPPORTED.");
            return;
        }

        m_callbacks = callbacks;

        m_deviceIdentifierHexStringToMacAddress = new HashMap<>();

        BLEManager.getInstance().addObserver(bleManagerCallbacks_);

        m_bootstrappingRequests = new HashMap<>();
        m_successfullyNegotiatedMtuDevices = new ArrayList<>();

    }

    /**
     * Get the mac address of a device based on its device identifier in hex string format. The SignOnBasicControllerBLE
     * will associate a mac address with a device's device identifier as soon as it successfully processes any
     * sign on message from that device.
     *
     * @param deviceIdentifierHexString Device identifier of the device to retrieve the mac address of, in hex
     *                                  string format.
     */
    public String getMacAddressOfDevice(String deviceIdentifierHexString) {
        if (!m_deviceIdentifierHexStringToMacAddress.containsKey(deviceIdentifierHexString)) {
            Log.w(TAG, "In the SignOnBasicControllerBLE, there was no mac address for device identifier " +
                    "hex string: " + deviceIdentifierHexString);
            return "";
        }
        return m_deviceIdentifierHexStringToMacAddress.get(deviceIdentifierHexString);
    }

    /**
     * A wrapper function for the function of the same name of the SignOnController object; go there
     * for a more detailed description of the function.
     */
    public CertificateV2 getKDPubCertificateOfDevice(String deviceIdentifierHexString) {
        return m_SignOnController.getKDPubCertificateForDevice(deviceIdentifierHexString);
    }

    /**
     * A wrapper function for the function of the same name of the SignOnController object; go there
     * for a more detailed description of the function.
     */
    public ArrayList<String> getSuccessfullySignedOnDeviceIdentifiers() {
        return m_SignOnController.getSuccessfullySignedOnDeviceIdentifiers();
    }

    /**
     * A wrapper function for the function of the same name of the SignOnController object; go there
     * for a more detailed description of the function.
     */
    public void addDevicePendingSignOn(CertificateV2 KSpubCertificate, byte[] device_identifier,
                                  byte[] secure_sign_on_code) {

        m_SignOnController.addDevicePendingSignOn(device_identifier, secure_sign_on_code,
                KSpubCertificate);
    }

    SignOnController m_SignOnController;
    private HashMap<String, String> m_deviceIdentifierHexStringToMacAddress;
    SecureSignOnBasicControllerBLECallbacks m_callbacks;
    // just a temporary hack for now; the devices might send us its bootstrapping request before
    // we have gotten the mtu successfully changed callback, so we will simply buffer bootstrapping
    // requests here, and only begin processing them after we have gotten a callback that the mtu
    // was successfully changed; ideally the device should not send us its bootstrapping request until
    // after it actually knows that we (the central) know that the mtu was negotiated successfully
    HashMap<String, BootstrappingRequestInfo> m_bootstrappingRequests;
    // just a temporary hack for now; remember mac addresses of devices we have successfully negotiated
    // mtu for, so that we only process a bootstrapping request on the spot if we have already successfully
    // negotiated mtu for that device; if we have not, then we buffer that bootstrapping request in
    // m_bootstrappingRequests so that we can process the bootstrapping request after mtu negotiation
    // has already occurred successfully
    ArrayList<String> m_successfullyNegotiatedMtuDevices;
}
