# J2V8 Hello World Example

A minimal Android app demonstrating how to use J2V8 to execute JavaScript code.

## What This App Does

This simple Android app:
- Creates a V8 JavaScript runtime
- Executes JavaScript code: `'Hello from ' + 'J2V8!'`
- Performs a math calculation: `7 * 6`
- Displays the V8 version
- Shows all results on screen

## Prerequisites

- Android Studio (latest version recommended)
- Android SDK with API 21+ (Android 5.0+)
- An Android device or emulator (ARM, ARM64, x86, or x86_64)

## Project Structure

```
example/
├── build.gradle              # Gradle build configuration
├── settings.gradle           # Gradle settings
├── src/
│   └── main/
│       ├── AndroidManifest.xml
│       ├── java/
│       │   └── com/example/j2v8hello/
│       │       └── MainActivity.java
│       └── res/
│           └── layout/
│               └── activity_main.xml
└── README.md (this file)
```

## Building the App

This example uses J2V8 from Maven Central, so there's no need to build J2V8 locally!

### Option 1: Using Android Studio

1. **Open the project:**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to and select the `example/` directory

2. **Sync Gradle:**
   - Android Studio will automatically download J2V8 from Maven Central
   - If not, click "Sync Project with Gradle Files" in the toolbar

3. **Run the app:**
   - Connect an Android device or start an emulator
   - Click the "Run" button (green triangle)
   - Select your device/emulator

### Option 2: Using Command Line

1. **Build the APK:**
   ```bash
   cd example
   ./gradlew assembleDebug
   ```
   Gradle will automatically download J2V8 6.3.0 from Maven Central.

2. **Install on device:**
   ```bash
   adb install build/outputs/apk/debug/app-debug.apk
   ```

3. **Launch the app:**
   ```bash
   adb shell am start -n com.example.j2v8hello/.MainActivity
   ```

## Expected Output

When you run the app, you should see:

```
JavaScript Result:
Hello from J2V8!

Math Result (7 * 6):
42

V8 Version:
[V8 version number]
```

## Troubleshooting

### Gradle Cannot Resolve J2V8

**Problem:** Gradle cannot download J2V8 from Maven Central
**Solution:** 
- Ensure you have internet connectivity
- Check that Maven Central is accessible
- Verify the version number in `build.gradle` matches the published version
- Try invalidating caches: `./gradlew clean --refresh-dependencies`

### App Crashes on Launch

**Problem:** Native library not loaded
**Solution:** The J2V8 AAR from Maven Central includes native libraries for all Android architectures (armeabi-v7a, arm64-v8a, x86, x86_64). If crashes persist:
- Check Android Studio's Logcat for specific error messages
- Verify your device/emulator architecture is supported
- Ensure minSdk version is 21 or higher

### Architecture Compatibility

The J2V8 6.3.0 AAR includes native libraries for:
- **armeabi-v7a** (32-bit ARM devices)
- **arm64-v8a** (64-bit ARM devices) 
- **x86** (32-bit x86 emulators/devices)
- **x86_64** (64-bit x86 emulators/devices)

Android automatically selects the correct library for your device/emulator, so the app works everywhere without additional configuration.

## Modifying the JavaScript Code

Edit `MainActivity.java` to run different JavaScript:

```java
// String operations
String result = v8.executeStringScript("'a' + 'b' + 'c'");

// Math operations  
int result = v8.executeIntegerScript("Math.pow(2, 10)");

// Boolean logic
boolean result = v8.executeBooleanScript("1 < 2");

// Complex operations with V8Object
V8Object obj = new V8Object(v8);
obj.add("x", 10);
obj.add("y", 20);
v8.add("myObj", obj);
int result = v8.executeIntegerScript("myObj.x + myObj.y");
obj.release();
```

## Learn More

- [J2V8 Documentation](https://github.com/eclipsesource/J2V8)
- [V8 JavaScript Engine](https://v8.dev/)
- [Android NDK Guide](https://developer.android.com/ndk/guides)

## License

This example is provided as-is for demonstration purposes.
