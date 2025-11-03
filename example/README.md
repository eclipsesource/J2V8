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
- An ARM Android device or emulator (armeabi-v7a)

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

### Option 1: Using Android Studio

1. **First, ensure J2V8 is built:**
   ```bash
   cd ..  # Go to J2V8 root
   python build.py -t android --arch arm --docker j2v8
   ```

2. **Open the project:**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to and select the `example/` directory

3. **Sync Gradle:**
   - Android Studio should automatically sync
   - If not, click "Sync Project with Gradle Files" in the toolbar

4. **Run the app:**
   - Connect an Android device or start an emulator
   - Click the "Run" button (green triangle)
   - Select your device/emulator

### Option 2: Using Command Line

1. **Ensure J2V8 AAR exists:**
   ```bash
   cd ..  # Go to J2V8 root
   ls build/outputs/aar/j2v8-release.aar  # Should exist
   ```

2. **Build the APK:**
   ```bash
   cd example
   ./gradlew assembleDebug
   ```

3. **Install on device:**
   ```bash
   adb install build/outputs/apk/debug/app-debug.apk
   ```

4. **Launch the app:**
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

### App Crashes on Launch

**Problem:** Native library not loaded
**Solution:** Ensure the AAR contains `jni/armeabi-v7a/libj2v8.so`:
```bash
unzip -l ../build/outputs/aar/j2v8-release.aar | grep libj2v8.so
```

### Wrong Architecture

**Problem:** App only works on ARM devices
**Solution:** J2V8 was built for `armeabi-v7a`. To support other architectures:
```bash
# For 64-bit ARM
python build.py -t android --arch arm64 --docker j2v8

# For x86 emulators
python build.py -t android --arch x86 --docker j2v8
```

Then update `build.gradle`:
```gradle
ndk {
    abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
}
```

### Gradle Sync Issues

**Problem:** Cannot resolve J2V8 dependency
**Solution:** The AAR path in `build.gradle` is relative. Ensure:
```
J2V8/
├── build/outputs/aar/j2v8-release.aar  ← Must exist here
└── example/                             ← You are here
    └── build.gradle
```

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
