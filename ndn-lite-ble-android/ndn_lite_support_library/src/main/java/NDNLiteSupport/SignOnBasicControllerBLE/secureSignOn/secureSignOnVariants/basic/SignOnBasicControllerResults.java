
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic;

public class SignOnBasicControllerResults {

    protected static class ParseBootstrappingRequestResult {

        public ParseBootstrappingRequestResult(SignOnBasicControllerResultCodes.ParseSignOnMessageResultCode resultCode,
                                               int bootstrappingRequestTlvLength,
                                               byte[] deviceIdentifier, int deviceIdentifierLength,
                                               byte[] deviceCapabilities, int deviceCapabilitiesLength,
                                               byte[] N1pub, int N1pubLength,
                                               byte[] signature, int signatureLength) {
            this.resultCode = resultCode;
            this.bootstrappingRequestTlvLength = bootstrappingRequestTlvLength;
            this.deviceIdentifier = deviceIdentifier;
            this.deviceIdentifierLength = deviceIdentifierLength;
            this.deviceCapabilities = deviceCapabilities;
            this.deviceCapabilitiesLength = deviceCapabilitiesLength;
            this.N1pub = N1pub;
            this.N1pubLength = N1pubLength;
            this.signature = signature;
            this.signatureLength = signatureLength;
        }

        public ParseBootstrappingRequestResult(SignOnBasicControllerResultCodes.ParseSignOnMessageResultCode resultCode) {
            this.resultCode = resultCode;
        }

        public SignOnBasicControllerResultCodes.ParseSignOnMessageResultCode resultCode;
        int bootstrappingRequestTlvLength;
        public byte[] deviceIdentifier;
        public int deviceIdentifierLength;
        public byte[] deviceCapabilities;
        public int deviceCapabilitiesLength;
        public byte[] N1pub;
        public int N1pubLength;
        public byte[] signature;
        public int signatureLength;

    }

    protected static class ParseCertificateRequestResult {

        public ParseCertificateRequestResult(SignOnBasicControllerResultCodes.ParseSignOnMessageResultCode resultCode,
                                             int certificateRequestTlvLength,
                                             byte[] deviceIdentifier, int deviceIdentifierLength,
                                             byte[] N1pub, int N1pubLength,
                                             byte[] N2pubDigest, int N2pubDigestLength,
                                             byte[] trustAnchorCertDigest, int trustAnchorCertDigestLength,
                                             byte[] signature, int signatureLength) {
            this.resultCode = resultCode;
            this.certificateRequestTlvLength = certificateRequestTlvLength;
            this.deviceIdentifier = deviceIdentifier;
            this.deviceIdentifierLength = deviceIdentifierLength;
            this.N1pub = N1pub;
            this.N1pubLength = N1pubLength;
            this.N2pubDigest = N2pubDigest;
            this.N2pubDigestLength = N2pubDigestLength;
            this.trustAnchorCertDigest = trustAnchorCertDigest;
            this.trustAnchorCertDigestLength = trustAnchorCertDigestLength;
            this.signature = signature;
            this.signatureLength = signatureLength;
        }

        public ParseCertificateRequestResult(SignOnBasicControllerResultCodes.ParseSignOnMessageResultCode resultCode) {
            this.resultCode = resultCode;
        }

        SignOnBasicControllerResultCodes.ParseSignOnMessageResultCode resultCode;
        int certificateRequestTlvLength;
        byte[] deviceIdentifier;
        int deviceIdentifierLength;
        byte[] N1pub;
        int N1pubLength;
        byte[] N2pubDigest;
        int N2pubDigestLength;
        byte[] trustAnchorCertDigest;
        int trustAnchorCertDigestLength;
        byte[] signature;
        int signatureLength;
    }

    protected static class ParseFinishMessageResult {

        public ParseFinishMessageResult(SignOnBasicControllerResultCodes.ParseSignOnMessageResultCode resultCode,
                                        int finishMessageTlvLength,
                                        byte[] deviceIdentifier, int deviceIdentifierLength,
                                        byte[] signature, int signatureLength) {
            this.resultCode = resultCode;
            this.finishMessageTlvLength = finishMessageTlvLength;
            this.deviceIdentifier = deviceIdentifier;
            this.deviceIdentifierLength = deviceIdentifierLength;
            this.signature = signature;
            this.signatureLength = signatureLength;
        }

        public ParseFinishMessageResult(SignOnBasicControllerResultCodes.ParseSignOnMessageResultCode resultCode) {
            this.resultCode = resultCode;
        }

        public SignOnBasicControllerResultCodes.ParseSignOnMessageResultCode resultCode;
        int finishMessageTlvLength;
        public byte[] deviceIdentifier;
        public int deviceIdentifierLength;
        public byte[] signature;
        public int signatureLength;

    }

}
