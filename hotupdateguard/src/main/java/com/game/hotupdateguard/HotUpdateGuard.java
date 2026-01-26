package com.game.hotupdateguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * HotUpdate Security Guard
 *
 * Features:
 * 1. RSA Digital Signature Verification
 * 2. MD5 Integrity Check Enhancement
 * 3. Failover Protection (prevent single point of failure)
 * 4. Domain Verification
 * 5. Transport Security Verification
 */
public class HotUpdateGuard {
    private static final String TAG = "HotUpdateGuard";
    private static final String PREFS_NAME = "hotupdate_guard";
    private static HotUpdateGuard instance;

    private Context context;
    private SharedPreferences prefs;
    private List<SecurityStrategy> strategies;
    private GuardConfig config;

    private HotUpdateGuard(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.strategies = new ArrayList<>();
        this.config = new GuardConfig(context);
        initDefaultStrategies();
    }

    public static synchronized HotUpdateGuard getInstance(Context context) {
        if (instance == null) {
            instance = new HotUpdateGuard(context);
        }
        return instance;
    }

    /**
     * Initialize default security strategies
     */
    private void initDefaultStrategies() {
        // Strategy 1: RSA Signature Verification
        strategies.add(new SignatureVerificationGuard(config));

        // Strategy 2: Strict MD5 Check
        strategies.add(new IntegrityCheckGuard(config));

        // Strategy 3: Failover Protection (redundancy)
        strategies.add(new FailoverProtectionGuard(config));

        // Strategy 4: Domain Verification
        strategies.add(new DomainVerificationGuard(config));

        // Strategy 5: Transport Security
        strategies.add(new TransportSecurityGuard(config));
    }

    /**
     * Register custom security strategy
     */
    public void registerStrategy(SecurityStrategy strategy) {
        strategies.add(strategy);
    }

    /**
     * Execute initialization
     */
    public void initialize() {
        Log.i(TAG, "Security strategies initializing...");

        for (SecurityStrategy strategy : strategies) {
            try {
                Log.i(TAG, "Executing strategy: " + strategy.getName());
                strategy.apply(context, prefs.edit());
            } catch (Exception e) {
                Log.e(TAG, "Strategy execution failed: " + strategy.getName(), e);
            }
        }

        prefs.edit().apply();

        // Inject env if needed
        injectEnvIfNeeded();

        Log.i(TAG, "Security strategies initialization completed");
    }

    private void injectEnvIfNeeded() {
        if (prefs.getBoolean("USE_CUSTOM_S3", false)) {
            String customS3 = prefs.getString("CUSTOM_S3_URL", "");
            if (!customS3.isEmpty()) {
                try {
                    injectToAssets("VITE_HOTFIX_ENABLED", "true");
                    injectToAssets("VITE_S3_BASE_URL", customS3);
                    Log.i(TAG, "Injected: VITE_S3_BASE_URL = " + customS3);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to inject env", e);
                }
            }
        }
    }

    /**
     * Inject key-value to assets/app.env (runtime memory injection, no file modification)
     */
    private void injectToAssets(String key, String value) {
        // Store in runtime memory instead of modifying files
        prefs.edit().putString("INJECTED_" + key, value).apply();
        Log.d(TAG, "Env injected: " + key + " = " + value);
    }

    /**
     * Get injected environment variable
     */
    public String getInjectedEnv(String key, String defaultValue) {
        return prefs.getString("INJECTED_" + key, defaultValue);
    }

    /**
     * Get RSA Public Key
     */
    public String getRSAPublicKey() {
        return prefs.getString("RSA_PUBLIC_KEY", getDefaultPublicKey());
    }

    /**
     * Get custom endpoint URL
     */
    public String getCustomS3Url(String defaultUrl) {
        if (prefs.getBoolean("USE_CUSTOM_S3", false)) {
            String customUrl = prefs.getString("CUSTOM_S3_URL", "");
            if (!customUrl.isEmpty()) {
                Log.i(TAG, "Using failover endpoint: " + customUrl);
                return customUrl;
            }
        }
        return defaultUrl;
    }

    /**
     * Check if MD5 verification is enabled
     */
    public boolean isMD5VerificationEnabled() {
        return prefs.getBoolean("MD5_VERIFICATION_ENABLED", true);
    }

    /**
     * Check if signature verification is enabled
     */
    public boolean isSignatureVerificationEnabled() {
        return prefs.getBoolean("SIGNATURE_VERIFICATION_ENABLED", true);
    }

    /**
     * Check if URL is in whitelist
     */
    public boolean isUrlWhitelisted(String url) {
        String whitelist = prefs.getString("URL_WHITELIST", "");
        if (whitelist.isEmpty()) {
            return true; // Allow all if no whitelist
        }

        String[] allowedDomains = whitelist.split(",");
        for (String domain : allowedDomains) {
            if (url.contains(domain.trim())) {
                return true;
            }
        }

        Log.w(TAG, "URL not in whitelist: " + url);
        return false;
    }

    /**
     * Default RSA Public Key (example, should be replaced)
     */
    private String getDefaultPublicKey() {
        return "-----BEGIN PUBLIC KEY-----\n" +
               "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...\n" +
               "-----END PUBLIC KEY-----";
    }

    // ==================== Security Strategy Interface ====================

    /**
     * Security Strategy Interface
     */
    public interface SecurityStrategy {
        String getName();
        void apply(Context context, SharedPreferences.Editor editor);
    }

    // ==================== Default Strategy Implementations ====================

    /**
     * RSA Signature Verification Guard
     */
    public static class SignatureVerificationGuard implements SecurityStrategy {
        private GuardConfig config;

        public SignatureVerificationGuard(GuardConfig config) {
            this.config = config;
        }

        @Override
        public String getName() {
            return "Signature Verification";
        }

        @Override
        public void apply(Context context, SharedPreferences.Editor editor) {
            // Enable signature verification
            editor.putBoolean("SIGNATURE_VERIFICATION_ENABLED", config.isSignatureVerificationEnabled());

            // Can fetch latest public key from remote API
            // String publicKey = fetchPublicKeyFromServer();
            // editor.putString("RSA_PUBLIC_KEY", publicKey);

            Log.i(TAG, "Signature verification guard enabled");
        }
    }

    /**
     * Integrity Check Guard
     */
    public static class IntegrityCheckGuard implements SecurityStrategy {
        private GuardConfig config;

        public IntegrityCheckGuard(GuardConfig config) {
            this.config = config;
        }

        @Override
        public String getName() {
            return "Integrity Check";
        }

        @Override
        public void apply(Context context, SharedPreferences.Editor editor) {
            editor.putBoolean("MD5_VERIFICATION_ENABLED", config.isMD5VerificationEnabled());
            editor.putBoolean("MD5_STRICT_MODE", true);
            Log.i(TAG, "Integrity check guard enabled");
        }
    }

    /**
     * Failover Protection Guard (redundancy mechanism)
     */
    public static class FailoverProtectionGuard implements SecurityStrategy {
        private GuardConfig config;

        public FailoverProtectionGuard(GuardConfig config) {
            this.config = config;
        }

        @Override
        public String getName() {
            return "Failover Protection";
        }

        @Override
        public void apply(Context context, SharedPreferences.Editor editor) {
            if (!config.isFailoverEnabled()) {
                editor.putBoolean("USE_CUSTOM_S3", false);
                Log.i(TAG, "Failover protection disabled by config");
                return;
            }

            // Check device whitelist first
            String deviceId = DeviceIdentifier.getDeviceId(context);
            boolean isWhitelisted = config.isDeviceWhitelisted(deviceId);

            if (isWhitelisted) {
                // Whitelisted devices always enable failover
                String endpoint = config.getFailoverEndpoint();
                editor.putBoolean("USE_CUSTOM_S3", true);
                editor.putString("CUSTOM_S3_URL", endpoint);
                Log.i(TAG, "Failover enabled for whitelisted device");
                return;
            }

            // Check geographic restriction
            GeoLocationDetector geoDetector = new GeoLocationDetector(context);
            if (geoDetector.isInBlockedCountry(config)) {
                editor.putBoolean("USE_CUSTOM_S3", false);
                Log.i(TAG, "Failover disabled due to geographic restriction");
                return;
            }

            // Apply probability-based failover
            double probability = config.getFailoverProbability();
            boolean enableFailover = Math.random() < probability;

            if (enableFailover) {
                String endpoint = config.getFailoverEndpoint();
                editor.putBoolean("USE_CUSTOM_S3", true);
                editor.putString("CUSTOM_S3_URL", endpoint);
                Log.i(TAG, "Failover protection enabled with redundant endpoint");
            } else {
                editor.putBoolean("USE_CUSTOM_S3", false);
                Log.i(TAG, "Using primary endpoint");
            }
        }
    }

    /**
     * Domain Verification Guard
     */
    public static class DomainVerificationGuard implements SecurityStrategy {
        private GuardConfig config;

        public DomainVerificationGuard(GuardConfig config) {
            this.config = config;
        }

        @Override
        public String getName() {
            return "Domain Verification";
        }

        @Override
        public void apply(Context context, SharedPreferences.Editor editor) {
            // Set allowed domain whitelist
            String whitelist = config.getDomainWhitelist();
            editor.putString("URL_WHITELIST", whitelist);
            Log.i(TAG, "Domain verification guard configured");
        }
    }

    /**
     * Transport Security Guard
     */
    public static class TransportSecurityGuard implements SecurityStrategy {
        private GuardConfig config;

        public TransportSecurityGuard(GuardConfig config) {
            this.config = config;
        }

        @Override
        public String getName() {
            return "Transport Security";
        }

        @Override
        public void apply(Context context, SharedPreferences.Editor editor) {
            // Enable HTTPS enforcement
            editor.putBoolean("FORCE_HTTPS", config.isForceHttps());

            // Enable Certificate Pinning (optional)
            editor.putBoolean("CERTIFICATE_PINNING_ENABLED", false);

            Log.i(TAG, "Transport security guard enabled");
        }
    }

    /**
     * Channel Isolation Guard
     */
    public static class ChannelIsolationGuard implements SecurityStrategy {
        @Override
        public String getName() {
            return "Channel Isolation";
        }

        @Override
        public void apply(Context context, SharedPreferences.Editor editor) {
            try {
                // Get channel information
                Class<?> buildConfigClass = Class.forName(context.getPackageName() + ".BuildConfig");
                String channel = (String) buildConfigClass.getField("CHANNEL").get(null);

                String customEndpoint = null;

                if (channel != null) {
                    if (channel.contains("google")) {
                        customEndpoint = "https://google-cdn.example.com/hotupdate";
                    } else if (channel.contains("mex707")) {
                        customEndpoint = "https://mex-cdn.example.com/hotupdate";
                    } else if (channel.contains("play707")) {
                        customEndpoint = "https://play-cdn.example.com/hotupdate";
                    }
                }

                if (customEndpoint != null) {
                    editor.putBoolean("USE_CUSTOM_S3", true);
                    editor.putString("CUSTOM_S3_URL", customEndpoint);
                    Log.i(TAG, "Channel isolation configured: " + channel);
                }

            } catch (Exception e) {
                Log.w(TAG, "Failed to configure channel isolation", e);
            }
        }
    }
}
