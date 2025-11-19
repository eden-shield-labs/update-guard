package com.game.hotupdateguard;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Geographic Location Detector
 */
public class GeoLocationDetector {
    private static final String TAG = "HotUpdateGuard";
    private static final String GEO_API_URL = "https://ipapi.co/country/";
    private static final int TIMEOUT = 3000;

    private Context context;

    public GeoLocationDetector(Context context) {
        this.context = context;
    }

    /**
     * Get country code (ISO 3166-1 alpha-2)
     * Priority:
     * 1. SIM card country
     * 2. Network country
     * 3. IP-based detection (fallback)
     */
    public String getCountryCode() {
        // Method 1: Try SIM card country
        String simCountry = getSimCountry();
        if (simCountry != null && !simCountry.isEmpty()) {
            Log.i(TAG, "Country detected from SIM: " + simCountry);
            return simCountry;
        }

        // Method 2: Try network country
        String networkCountry = getNetworkCountry();
        if (networkCountry != null && !networkCountry.isEmpty()) {
            Log.i(TAG, "Country detected from network: " + networkCountry);
            return networkCountry;
        }

        // Method 3: Try locale
        String localeCountry = Locale.getDefault().getCountry();
        if (localeCountry != null && !localeCountry.isEmpty()) {
            Log.i(TAG, "Country detected from locale: " + localeCountry);
            return localeCountry;
        }

        // Method 4: IP-based detection (async fallback)
        Log.i(TAG, "No local country info, skipping IP detection for performance");
        return "";
    }

    /**
     * Get country from SIM card
     */
    private String getSimCountry() {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String simCountry = tm.getSimCountryIso();
                if (simCountry != null && simCountry.length() == 2) {
                    return simCountry.toUpperCase();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get SIM country", e);
        }
        return null;
    }

    /**
     * Get country from network operator
     */
    private String getNetworkCountry() {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) {
                    return networkCountry.toUpperCase();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get network country", e);
        }
        return null;
    }

    /**
     * Get country from IP address (blocking call, use in background thread)
     */
    public String getCountryFromIP() {
        try {
            URL url = new URL(GEO_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String countryCode = reader.readLine();
                reader.close();

                if (countryCode != null && countryCode.length() == 2) {
                    Log.i(TAG, "Country detected from IP: " + countryCode);
                    return countryCode.toUpperCase();
                }
            }

            conn.disconnect();
        } catch (Exception e) {
            Log.w(TAG, "IP-based geo detection failed", e);
        }
        return "";
    }

    /**
     * Check if current location is in blocked countries
     */
    public boolean isInBlockedCountry(GuardConfig config) {
        String countryCode = getCountryCode();
        if (countryCode == null || countryCode.isEmpty()) {
            // If cannot detect country, allow failover
            return false;
        }

        boolean blocked = config.isCountryBlocked(countryCode);
        if (blocked) {
            Log.i(TAG, "Country " + countryCode + " is in blocked list, failover disabled");
        }
        return blocked;
    }
}
