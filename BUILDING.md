# Build-System CLI

## Non-interactive
```
python build.py -h, --help

usage: build.py [-h] --target {android,linux,macos,win32} --arch {x86,x64,arm}
                [--vendor VENDOR] [--keep-native-libs] [--node-enabled]
                [--docker] [--vagrant] [--sys-image SYS_IMAGE] [--no-shutdown]
                [--interactive]
                [build-steps [build-steps ...]]
```
```
python build.py -v alpine -t linux -a x64 -dkr -img openjdk:8u131-alpine -ne j2v8
```

## Interactive
```
python build.py --i, --interactive

entering interactive mode...

[0] Docker >> android-x86 >> NODE_ENABLED
[1] Docker >> android-arm >> NODE_ENABLED
[2] Docker >> alpine-linux-x64 >> NODE_ENABLED
[3] Docker >> linux-x64 >> NODE_ENABLED
[4] Docker >> linux-x86 >> NODE_ENABLED
[5] Vagrant >> macosx-x64 >> NODE_ENABLED
[6] Vagrant >> macosx-x86 >> NODE_ENABLED
[7] Native >> windows-x64 >> NODE_ENABLED
[8] Docker >> windows-x64 >> NODE_ENABLED
[9] Vagrant >> windows-x64 >> NODE_ENABLED

Select a predefined build-configuration to run: 2
Building: Docker >> alpine-linux-x64 >> NODE_ENABLED

Override build-steps ? (leave empty to run pre-configured steps): j2v8
```

# Build-Steps

The J2V8 build-system performs several build steps in a fixed order to produce the final J2V8 packages for usage on the designated target platforms. What follows is a short summary for what each of the executed build-steps does and what output artifacts are produced by each step.

```
Node.js --> CMake --> JNI --> C++ --> Optimize --> Java/Android --> JUnit
```
---
## Node.js

Builds the [Node.js](https://nodejs.org/en/) & [V8](https://developers.google.com/v8/) dependency artifacts that are later linked into the J2V8 native bridge code.
(only works if the Node.js source was checked out into the J2V8 `./node` directory)

__Inputs:__
- Node.js source code
    - see [Github](https://github.com/nodejs/node)
- Node.js GIT patches with customizations for integrating Node.js into J2V8
    - `./node.patches/*.diff`

__Artifacts:__
- Node.js & V8 static link libraries
    - `./node/out/`
    - *win32 specific*
        - `./node/build/`
        - `./node/Debug/`
        - `./node/Release/`
---
## CMake

Uses [CMake](https://cmake.org/) to generate the native Makefiles / IDE project files to later build the J2V8 C++ native bridge shared libraries.

__Inputs__:
- Node.js / V8 static link libraries
    - `./cmake/NodeJsUtils.cmake`
- CMakeLists & CMake utilities
    - `CMakeLists.txt`
    - `./cmake/*.cmake`

__Artifacts:__
- CMake generated Makefiles / IDE Project-files
    - `./cmake.out/{platform}.{architecture}/`
---
## JNI Header Generation

Generate the JNI glue header file from the native method definitions of the Java `V8` class.

__Inputs__:
- Java V8.class file
    - `./target/classes/com/eclipsesource/v8/V8.class`

__Artifacts:__
- J2V8 C++ JNI header file
    - `./jni/com_eclipsesource_v8_V8Impl.h`
---
## C++

Compile and link the J2V8 native shared libraries (.so/.dylib/.dll), which contain the C++ JNI bridge code to interop with the embedded Node.js / V8 parts.

__Inputs__:
- CMake generated Makefiles / IDE Project-files
- Node.js / V8 static link libraries & C++ header files
- J2V8 C++ JNI source code
    - `./jni/com_eclipsesource_v8_V8Impl.h`
    - `./jni/com_eclipsesource_v8_V8Impl.cpp`

__Artifacts:__
- J2V8 native shared libraries
    - `./cmake.out/{platform}.{architecture}/libj2v8-[vendor-]{platform}-{abi}.{ext}`
    - e.g. `./cmake.out/linux.x64/libj2v8-alpine-linux-x86_64.so`
---
## Optimize

The native J2V8 libraries are optimized for performance and/or filesize by using the available tools of the target-platform / compiler-toolchain.

__Inputs__:
- <u>unoptimized</u> J2V8 native shared libraries
    - `./cmake.out/{platform}.{architecture}/libj2v8-[vendor-]{platform}-{abi}.{ext}`
    - e.g. `./cmake.out/linux.x64/libj2v8-alpine-linux-x86_64.so`
- platform-specific optimization tools:
    - Android: -
    - Linux: `execstack`, `strip`
    - MacOSX: -
    - Windows: -

__Artifacts:__
- <u>optimized</u> J2V8 native shared libraries
    - `./cmake.out/{platform}.{architecture}/libj2v8-[vendor-]{platform}-{abi}.{ext}`
    - e.g. `./cmake.out/linux.x64/libj2v8-alpine-linux-x86_64.so`
---
## Java / Android

Compiles the Java source code and packages it, including the previously built native libraries, into the final package artifacts. For the execution of this build-step [Maven](https://maven.apache.org/) (Java) or [Gradle](https://gradle.org/) (Android) are used for the respective target platforms.

__Inputs__:
- J2V8 native shared libraries (will be automatically copied to the required Java / Android project directories to be included in the .jar/.aar packages)
    - `./src/main/resources/` (Java)
    - `./src/main/jniLibs/{abi}/libj2v8.so` (Android)
- J2V8 Java source code
    - `./src/main/`
- J2V8 Java test source code
    - `./src/test/`
- J2V8 build settings
    - `./build_settings.py`

__Artifacts:__
- Maven platform-specific packages
    - `./build.out/j2v8_{platform}_{abi}-{j2v8_version}.jar`
    - e.g. `./build.out/j2v8_linux_x86_64-4.8.0-SNAPSHOT.jar`
- Gradle Android packages
    - `./build/outputs/aar/j2v8-release.aar`
---
## JUnit

Runs the Java ([JUnit](http://junit.org/)) unit tests.

__Inputs__:
- J2V8 platform-specific packages
- J2V8 Java test source code
    - `./src/test/`

__Artifacts:__
- Maven Surefire test reports (Desktop platforms)
    - `./target/surefire-reports/`
- Gradle Spoon test reports (Android only)
    - `./build/spoon/debug/`
---
