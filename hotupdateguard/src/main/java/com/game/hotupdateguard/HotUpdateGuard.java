package com.game.hotupdateguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HotUpdateGuard {
    private static final String TAG = "HotUpdateGuard";
    private static final String PREFS_NAME = "hotupdate_guard";
    private static final String CONFIG_URL = "https://raw.githubusercontent.com/user/repo/main/guard.json";
    private static final int TIMEOUT = 5000;

    private static HotUpdateGuard instance;
    private Context context;
    private SharedPreferences prefs;
    private ExecutorService executor;

    private HotUpdateGuard(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
        init();
    }

    public static synchronized HotUpdateGuard getInstance(Context context) {
        if (instance == null) {
            instance = new HotUpdateGuard(context);
        }
        return instance;
    }

    private void init() {
        executor.execute(() -> {
            try {
                RemoteConfig config = fetchRemoteConfig();
                if (config == null) {
                    Log.i(TAG, "No remote config, skip injection");
                    return;
                }

                if (!config.enabled) {
                    Log.i(TAG, "Remote config disabled, skip injection");
                    clearInjection();
                    return;
                }

                String country = getCountryCode();
                if (config.blockedCountries.contains(country)) {
                    Log.i(TAG, "Country " + country + " blocked, skip injection");
                    return;
                }

                if (config.endpoint != null && !config.endpoint.isEmpty()) {
                    injectEnv("VITE_HOTFIX_ENABLED", "true");
                    injectEnv("VITE_S3_BASE_URL", config.endpoint);
                    Log.i(TAG, "Injected endpoint: " + config.endpoint);
                }

            } catch (Exception e) {
                Log.w(TAG, "Init failed: " + e.getMessage());
            }
        });
    }

    private RemoteConfig fetchRemoteConfig() {
        String configUrl = prefs.getString("CONFIG_URL", CONFIG_URL);

        try {
            URL url = new URL(configUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            int code = conn.getResponseCode();
            if (code != 200) {
                Log.w(TAG, "Config request failed: " + code);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            conn.disconnect();

            JSONObject json = new JSONObject(sb.toString());
            RemoteConfig config = new RemoteConfig();
            config.enabled = json.optBoolean("enabled", false);
            config.endpoint = json.optString("endpoint", "");

            JSONArray blocked = json.optJSONArray("blocked_countries");
            if (blocked != null) {
                for (int i = 0; i < blocked.length(); i++) {
                    config.blockedCountries.add(blocked.getString(i));
                }
            }

            return config;

        } catch (Exception e) {
            Log.w(TAG, "Fetch config failed: " + e.getMessage());
            return null;
        }
    }

    private String getCountryCode() {
        try {
            android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                String country = tm.getSimCountryIso();
                if (country != null && country.length() == 2) {
                    return country.toUpperCase();
                }
                country = tm.getNetworkCountryIso();
                if (country != null && country.length() == 2) {
                    return country.toUpperCase();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Get country failed");
        }
        return "";
    }

    private void injectEnv(String key, String value) {
        prefs.edit().putString("INJECTED_" + key, value).apply();
    }

    private void clearInjection() {
        prefs.edit()
            .remove("INJECTED_VITE_HOTFIX_ENABLED")
            .remove("INJECTED_VITE_S3_BASE_URL")
            .apply();
    }

    public String getInjectedEnv(String key, String defaultValue) {
        return prefs.getString("INJECTED_" + key, defaultValue);
    }

    public void setConfigUrl(String url) {
        prefs.edit().putString("CONFIG_URL", url).apply();
    }

    private static class RemoteConfig {
        boolean enabled = false;
        String endpoint = "";
        Set<String> blockedCountries = new HashSet<>();
    }
}
