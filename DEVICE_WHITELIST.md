# Device Whitelist Configuration

## How to Get Device ID

The device ID is the Android ID. You can get it in two ways:

### Method 1: Using ADB

```bash
adb shell settings get secure android_id
```

Example output:
```
abc1234567890def
```

### Method 2: Check Logs

When the app starts, HotUpdateGuard will log the device ID:

```bash
adb logcat | grep "Device ID"
```

You will see:
```
HotUpdateGuard: Device ID: abc1234567890def
```

## Add Device to Whitelist

Edit `guard.properties`:

```properties
# Add multiple device IDs separated by commas
device.whitelist=abc1234567890def,xyz9876543210fed
```

## Whitelist Behavior

- Whitelisted devices will **ALWAYS** enable failover
- Geographic restrictions do NOT apply to whitelisted devices
- Probability settings do NOT apply to whitelisted devices
- Useful for testing or specific user groups

## Example Use Cases

1. **Testing**: Add your test devices to always use backup endpoint
2. **VIP Users**: Add specific user devices to always use faster CDN
3. **Regional Testing**: Test failover for specific devices in JP/TW
4. **Debugging**: Ensure certain devices always trigger failover for monitoring

## Get Device ID from App

You can also add a debug option in your app:

```java
import com.game.hotupdateguard.DeviceIdentifier;

String deviceId = DeviceIdentifier.getDeviceId(context);
Log.i("MyApp", "Device ID: " + deviceId);
// Or show in UI for users to copy
```

## Security Notes

- Device IDs are unique per device and app installation
- If user factory resets or reinstalls, the ID may change
- Store device IDs securely if using for user identification
