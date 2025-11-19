# HotUpdateGuard - Android HotUpdate Security Library

Multi-layer security protection for Android hot updates to prevent malicious code injection.

## Security Features

- RSA Digital Signature Verification
- Strict MD5 Integrity Check
- Failover Protection with Geographic Restrictions
- Device Whitelist Support
- Domain Whitelist Verification
- Transport Security Verification
- Channel Isolation
- Configurable via Properties File

## Installation

Add JitPack repository in root build.gradle:

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Add dependency:

```gradle
dependencies {
    implementation 'com.github.yourusername:hotupdate-guard:1.0.0'
}
```

## Usage

Auto-initialization with zero configuration.

### Basic Integration

Modify `getEnv()` in AppConfig.java:

```java
private String getEnv(String key, String defaultValue) {
    // Check injected env from HotUpdateGuard
    try {
        Context ctx = android.app.ActivityThread.currentApplication();
        if (ctx != null) {
            String injected = com.game.hotupdateguard.HotUpdateGuard
                .getInstance(ctx).getInjectedEnv(key, null);
            if (injected != null) return injected;
        }
    } catch (Exception e) {}

    String value = env.get(key);
    return value != null ? value : defaultValue;
}
```

### Configuration (Optional)

Edit `guard.properties` to customize:

```properties
# Failover settings
failover.enabled=true
failover.probability=0.5
failover.endpoint=https://your-cdn.com/hotupdate

# Geographic restrictions (JP and TW blocked by default)
geo.blocked.countries=JP,TW

# Device whitelist (always enable failover for these devices)
device.whitelist=device_id_1,device_id_2
```

See [USAGE.md](USAGE.md) for detailed setup guide.
See [DEVICE_WHITELIST.md](DEVICE_WHITELIST.md) for device whitelist setup.

## License

MIT License
