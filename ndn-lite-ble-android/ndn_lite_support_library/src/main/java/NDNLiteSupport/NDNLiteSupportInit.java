
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport;

import java.security.Security;

public class NDNLiteSupportInit {
    public static void NDNLiteSupportInit() {
        // this line is to allow for the use of various spongycastle API's in the SecurityHelpers
        // see https://stackoverflow.com/questions/6898801/how-to-include-the-spongy-castle-jar-in-android
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

    }
}
