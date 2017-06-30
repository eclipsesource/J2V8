# Build-Steps

The J2V8 build-system performs several build steps in a fixed order to produce the final J2V8 packages for usage on the designated target platforms. What follows is a short summary for what each of the executed build-steps does and what output artifacts are produced by each step.

---
## Node.js

Builds the [Node.js](https://nodejs.org/en/) & [V8](https://developers.google.com/v8/) dependency artifacts that are later linked against by the J2V8 native bridge code.

__Inputs:__
- Node.js source code
    - see [Github](https://github.com/nodejs/node)
- Node.js GIT patches with customizations for integrating Node.js into J2V8
    - `./node.patches/*.diff`

__Artifacts:__
- Node.js & V8 static link libraries
    - `./node/out/`
    - `./node/build/`
    - `./node/Debug/`
    - `./node/Release/`
---
## CMake

Uses [CMake](https://cmake.org/) to generate the native Makefiles / IDE project files to later build the J2V8 C++ native bridge shared libraries (.so/.dylib/.dll)

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
## JNI

The previously generated Makefiles / IDE project files are used to compile and link the J2V8 C++ source code, which provides the JNI bridge to interop between the Java code and the C++ code of Node.js / V8.

__Inputs__:
- CMake generated Makefiles / IDE Project-files
- J2V8 C++ JNI source code
    - `./jni/com_eclipsesource_v8_V8Impl.h`
    - `./jni/com_eclipsesource_v8_V8Impl.cpp`

__Artifacts:__
- J2V8 native shared libraries
    - `./cmake.out/{platform}.{architecture}/libj2v8_{platform}_{abi}.{ext}`
    - e.g. `./cmake.out/linux.x64/libj2v8_linux_x86_64.so`
- The built shared libraries will also be automatically copied to the required Java / Android project directories to be included in the .jar/.aar packages that will be built later.
    - `./src/main/resources/` (Java)
    - `./src/main/jniLibs/{abi}/libj2v8.so` (Android)
---
## Java / Android

Compiles the Java source code and packages it, including the previously built native libraries, into the final package artifacts. For the execution of this build-step [Maven](https://maven.apache.org/) (Java) or [Gradle](https://gradle.org/) (Android) are used for the respective target platforms.

__Inputs__:
- J2V8 native shared libraries
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
- Maven Surefire test reports
    - `./target/surefire-reports/`
- Gradle connected-test reports
    - `./build/outputs/androidTest-results/connected/`
---
