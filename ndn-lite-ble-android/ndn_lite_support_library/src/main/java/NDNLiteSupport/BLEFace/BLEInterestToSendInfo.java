
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.BLEFace;

import net.named_data.jndn.Interest;

public class BLEInterestToSendInfo {

    /**
     * Create a BLEInterestToSendInfo. This is a class that contains information
     * related to an interest that has been sent through a BLEFace. The reason for this class
     * is that there might not be an active connection to the device when BLEFace.expressInterest
     * is called; therefore, there is a queue of BLEInterestToSendInfo's in the BLEFace
     * so that it can buffer them and send them whenever there is an available connection.
     *
     * @param interest Interest that will be sent.
     * @param deviceMacAddress Mac address of device to send interest to.
     */
    public BLEInterestToSendInfo(Interest interest, String deviceMacAddress) {
        this.interest = interest;
        this.deviceMacAddress = deviceMacAddress;
    }

    Interest interest; // interest to send
    String deviceMacAddress; // mac address of device to send interest to
}
