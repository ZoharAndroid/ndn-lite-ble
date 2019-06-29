
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.BLEFace;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

public class BLEInterestToSatisfyInfo {

    /**
     * Create a BLEInterestToSatisfyInfo. This is a class that contains information
     * related to an interest that has been satisfied by data. The reason for this class
     * is that there might not be an active connection to the device when BLEFace.putData
     * is called; therefore, there is a queue of BLEInterestToSatisfyInfo's in the BLEFace
     * so that it can buffer them and send them whenever there is an available connection.
     *
     * @param interest Interest that will be satisfied.
     * @param deviceMacAddress Mac address of device to send data to.
     */
    public BLEInterestToSatisfyInfo(Interest interest, String deviceMacAddress) {
        this.interest = interest;
        this.deviceMacAddress = deviceMacAddress;
    }

    /**
     * Set the data that will be sent when a connection to the device is available.
     *
     * @param data Data that will be sent.
     */
    public void setData(Data data) {
        this.data = data;
    }

    Interest interest; // interest that should be satisfied
    Data data; // data to satisfy interest with
    String deviceMacAddress; // mac address of device to send data to
}
