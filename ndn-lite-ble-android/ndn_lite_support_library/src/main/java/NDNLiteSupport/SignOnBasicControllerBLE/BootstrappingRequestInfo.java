
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE;

import java.util.UUID;

import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnControllerResults;

public class BootstrappingRequestInfo {

    BootstrappingRequestInfo(byte[] bootstrappingRequest,
                             SignOnControllerResults.ProcessSignOnMessageResult processResult,
                             UUID serviceUuid) {
        this.bootstrappingRequest = bootstrappingRequest;
        this.processResult = processResult;
        this.serviceUuid = serviceUuid;
    }

    byte[] bootstrappingRequest;
    SignOnControllerResults.ProcessSignOnMessageResult processResult;
    UUID serviceUuid;
}
