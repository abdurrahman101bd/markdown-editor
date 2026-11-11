package com.ar.markdowneditor.util;

import android.content.Context;
import androidx.biometric.BiometricManager;

public class BiometricHelper {

    public static boolean isAvailable(Context context) {
        BiometricManager bm = BiometricManager.from(context);
        int result = bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean isBiometricOnlyAvailable(Context context) {
        BiometricManager bm = BiometricManager.from(context);
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }
}