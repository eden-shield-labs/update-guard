package com.game.hotupdateguard;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Guard Configuration Manager
 */
public class GuardConfig {
    private static final String TAG = "HotUpdateGuard";
    private static final String CONFIG_FILE = "guard.properties";

    private Properties properties;
    private Set<String> blockedCountries;
    private Set<String> whitelistedDevices;

    public GuardConfig(Context context) {
        properties = new Properties();
        loadConfig(context);
        parseBlockedCountries();
        parseWhitelistedDevices();
    }

    private void loadConfig(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(CONFIG_FILE);
            properties.load(inputStream);
            inputStream.close();
            Log.i(TAG, "Configuration loaded successfully");
        } catch (Exception e) {
            Log.w(TAG, "Failed to load config, using defaults", e);
            loadDefaults();
        }
    }

    private void loadDefaults() {
        properties.setProperty("failover.enabled", "true");
        properties.setProperty("failover.probability", "0.5");
        properties.setProperty("failover.endpoint", "https://backup-cdn.example.com/hotupdate");
        properties.setProperty("geo.blocked.countries", "JP,TW");
        properties.setProperty("device.whitelist", "");
        properties.setProperty("domain.whitelist", "s3.amazonaws.com,cloudfront.net");
        properties.setProperty("signature.verification.enabled", "true");
        properties.setProperty("md5.verification.enabled", "true");
        properties.setProperty("force.https", "true");
    }

    private void parseBlockedCountries() {
        String blocked = properties.getProperty("geo.blocked.countries", "JP,TW");
        blockedCountries = new HashSet<>();
        if (!blocked.isEmpty()) {
            blockedCountries.addAll(Arrays.asList(blocked.split(",")));
        }
    }

    private void parseWhitelistedDevices() {
        String whitelist = properties.getProperty("device.whitelist", "");
        whitelistedDevices = new HashSet<>();
        if (!whitelist.isEmpty()) {
            whitelistedDevices.addAll(Arrays.asList(whitelist.split(",")));
        }
    }

    public boolean isFailoverEnabled() {
        return Boolean.parseBoolean(properties.getProperty("failover.enabled", "true"));
    }

    public double getFailoverProbability() {
        try {
            return Double.parseDouble(properties.getProperty("failover.probability", "0.5"));
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }

    public String getFailoverEndpoint() {
        return properties.getProperty("failover.endpoint", "https://backup-cdn.example.com/hotupdate");
    }

    public boolean isCountryBlocked(String countryCode) {
        return blockedCountries.contains(countryCode.toUpperCase());
    }

    public boolean isDeviceWhitelisted(String deviceId) {
        return whitelistedDevices.contains(deviceId);
    }

    public String getDomainWhitelist() {
        return properties.getProperty("domain.whitelist", "s3.amazonaws.com,cloudfront.net");
    }

    public boolean isSignatureVerificationEnabled() {
        return Boolean.parseBoolean(properties.getProperty("signature.verification.enabled", "true"));
    }

    public boolean isMD5VerificationEnabled() {
        return Boolean.parseBoolean(properties.getProperty("md5.verification.enabled", "true"));
    }

    public boolean isForceHttps() {
        return Boolean.parseBoolean(properties.getProperty("force.https", "true"));
    }
}
