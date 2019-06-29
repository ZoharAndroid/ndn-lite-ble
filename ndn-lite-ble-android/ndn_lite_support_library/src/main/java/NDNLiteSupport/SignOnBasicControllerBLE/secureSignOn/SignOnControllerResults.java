
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn;

public class SignOnControllerResults {

    public static class ProcessSignOnMessageResult {
        public ProcessSignOnMessageResult(SignOnControllerResultCodes.SignOnControllerResultCode resultCode,
                                          long signOnMessageTlvType,
                                          String deviceIdentifierHexString) {
            this.resultCode = resultCode;
            this.signOnMessageTlvType = signOnMessageTlvType;
            this.deviceIdentifierHexString = deviceIdentifierHexString;
        }

        public ProcessSignOnMessageResult(SignOnControllerResultCodes.SignOnControllerResultCode resultCode,
                                          long signOnMessageTlvType) {
            this.resultCode = resultCode;
            this.signOnMessageTlvType = signOnMessageTlvType;
        }

        public ProcessSignOnMessageResult(SignOnControllerResultCodes.SignOnControllerResultCode resultCode,
                                          String deviceIdentifierHexString) {
            this.resultCode = resultCode;
            this.deviceIdentifierHexString = deviceIdentifierHexString;
        }

        public ProcessSignOnMessageResult(SignOnControllerResultCodes.SignOnControllerResultCode resultCode) {
            this.resultCode = resultCode;
        }

        public SignOnControllerResultCodes.SignOnControllerResultCode resultCode;
        public long signOnMessageTlvType;
        public String deviceIdentifierHexString;
    }

    public static class ConstructBootstrappingRequestResponseResult {
        public ConstructBootstrappingRequestResponseResult(
                SignOnControllerResultCodes.SignOnControllerResultCode resultCode,
                                                           String deviceIdentifierHexString,
                                                           byte[] bootstrappingRequestResult) {
            this.resultCode = resultCode;
            this.deviceIdentifierHexString = deviceIdentifierHexString;
            this.bootstrappingRequestResponse = bootstrappingRequestResult;
        }

        public ConstructBootstrappingRequestResponseResult(
                SignOnControllerResultCodes.SignOnControllerResultCode resultCode) {
            this.resultCode = resultCode;
        }

        public SignOnControllerResultCodes.SignOnControllerResultCode resultCode;
        public byte[] bootstrappingRequestResponse;
        public String deviceIdentifierHexString;

    }

    public static class ConstructCertificateRequestResponseResult {
        public ConstructCertificateRequestResponseResult(
                SignOnControllerResultCodes.SignOnControllerResultCode resultCode,
                                                         String deviceIdentifierHexString,
                                                         byte[] certificateRequestResponse) {
            this.resultCode = resultCode;
            this.deviceIdentifierHexString = deviceIdentifierHexString;
            this.certificateRequestResponse = certificateRequestResponse;
        }

        public ConstructCertificateRequestResponseResult(
                SignOnControllerResultCodes.SignOnControllerResultCode resultCode) {
            this.resultCode = resultCode;
        }

        public SignOnControllerResultCodes.SignOnControllerResultCode resultCode;
        public String deviceIdentifierHexString;
        public byte[] certificateRequestResponse;
    }

}
