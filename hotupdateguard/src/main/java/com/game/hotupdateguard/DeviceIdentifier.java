package com.game.hotupdateguard;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

/**
 * Device Identifier
 */
public class DeviceIdentifier {
    private static final String TAG = "HotUpdateGuard";

    /**
     * Get device unique identifier (Android ID)
     */
    public static String getDeviceId(Context context) {
        try {
            String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
            );

            if (androidId != null && !androidId.isEmpty()) {
                Log.d(TAG, "Device ID: " + androidId);
                return androidId;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get device ID", e);
        }
        return "";
    }
}
