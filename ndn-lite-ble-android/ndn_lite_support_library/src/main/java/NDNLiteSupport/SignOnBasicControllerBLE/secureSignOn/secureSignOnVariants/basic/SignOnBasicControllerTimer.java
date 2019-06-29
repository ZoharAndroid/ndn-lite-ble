
/*
 * Copyright (C) 2018-2019 Edward Lu
 *
 * This file is subject to the terms and conditions of the GNU Lesser
 * General Public License v2.1. See the file LICENSE in the top level
 * directory for more details.
 */

package NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.basic;

import android.util.Log;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SignOnBasicControllerTimer {

    private static final String TAG = SignOnBasicControllerTimer.class.getSimpleName();

    // temporary variables for timing and evaluation
    public long lastOnboardingReceivedBootstrappingRequestTime;
    public long lastOnboardingValidatedBootstrappingRequestTime;
    public long lastOnboardingSentBootstrappingResponseTime;
    public long lastOnboardingReceivedCertificateRequestTime;
    public long lastOnboardingValidatedCertificateRequestTime;
    public long lastOnboardingSentCertificateRequestResponseTime;

    public void printLastTimes() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        Log.d(TAG, "End of last test (clock time): " + dtf.format(now));
        Log.d(TAG, "Times for the last sign-on procedure:");
        long nanoSecondToMicroSecondDivider = 1000;
        long T1 = (lastOnboardingValidatedBootstrappingRequestTime - lastOnboardingReceivedBootstrappingRequestTime)/nanoSecondToMicroSecondDivider;
        long T2 = (lastOnboardingSentBootstrappingResponseTime - lastOnboardingValidatedBootstrappingRequestTime)/nanoSecondToMicroSecondDivider;
        long T3 = (lastOnboardingReceivedCertificateRequestTime - lastOnboardingSentBootstrappingResponseTime)/nanoSecondToMicroSecondDivider;
        long T4 = (lastOnboardingValidatedCertificateRequestTime - lastOnboardingReceivedCertificateRequestTime)/nanoSecondToMicroSecondDivider;
        long T5 = (lastOnboardingSentCertificateRequestResponseTime - lastOnboardingValidatedCertificateRequestTime)/nanoSecondToMicroSecondDivider;
        long T6 = (lastOnboardingSentCertificateRequestResponseTime - lastOnboardingValidatedBootstrappingRequestTime)/nanoSecondToMicroSecondDivider;
        long T7 = (T1 + T2 + T4 + T5);
        Log.d(TAG, "T1: " + asUnsignedDecimalString(T1));
        Log.d(TAG, "T2: " + asUnsignedDecimalString(T2));
        Log.d(TAG, "T3: " + asUnsignedDecimalString(T3));
        Log.d(TAG, "T4: " + asUnsignedDecimalString(T4));
        Log.d(TAG, "T5: " + asUnsignedDecimalString(T5));
        Log.d(TAG, "T6: " + asUnsignedDecimalString(T6));
        Log.d(TAG, "T7: " + asUnsignedDecimalString(T7));
        Log.d(TAG, "CSV format: ");
        Log.d(TAG, "," + asUnsignedDecimalString(T1) + ","
                + asUnsignedDecimalString(T2) + "," + asUnsignedDecimalString(T3) + "," +
                asUnsignedDecimalString(T4) + "," + asUnsignedDecimalString(T5) + "," +
                asUnsignedDecimalString(T6) + "," + asUnsignedDecimalString(T7) + ",");
    }

    /** the constant 2^64 */
    private static final BigInteger TWO_64 = BigInteger.ONE.shiftLeft(64);

    public String asUnsignedDecimalString(long l) {
        BigInteger b = BigInteger.valueOf(l);
        if(b.signum() < 0) {
            b = b.add(TWO_64);
        }
        return b.toString();
    }

}
