# Getting started / building from source

1. clone the source code from the [J2V8 GitHub repository](https://github.com/eclipsesource/J2V8)
2. run `j2v8-cli.cmd` (on Win32) or `source j2v8-cli.sh` on MacOS / Linux
3. `nodejs git clone` to clone the Node.js/V8 source code
4. `nodejs diff apply` to apply the required modifications to the Node.js source code
5. start the desired J2V8 build either via `build -i` or `build ...args` (see below for details)

# Build-System CLI

## Interactive
```shell
build --i, --interactive
# or
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

## Non-interactive
```shell
build -h, --help
# or
python build.py -h, --help

usage: build [-h] --target {android,linux,macos,win32} --arch {x86,x64,arm}
             [--vendor VENDOR] [--keep-native-libs] [--node-enabled]
             [--docker] [--vagrant] [--sys-image SYS_IMAGE] [--no-shutdown]
             [--redirect-stdout] [--interactive]
             [build-steps [build-steps ...]]
```

### Basic Examples

Build for Alpine-Linux x64 using Docker and Node.js features included:<br/>
`build -v alpine -t linux -a x64 -dkr -ne`

Build for MacOSX x64 using Vagrant excluding Node.js features:<br/>
`build -t macos -a x64 -vgr`

Build for Windows x64 directly on the host-system, Node.js features included:<br/>
`build -t win32 -a x64 -ne`

### Build-Step syntax

If no build-steps are specified, then the CLI will run `all` available build-steps by default.
To see a list of available build-steps run `build --help` or see the ***Build-Steps*** section below.

For ease of use, there are also some advanced build-step aliases that when specified will run a collection of some of the base-steps:

- `all` ... is the default, and will run all known build-steps
- `native` ... will run only the build-steps that are relevant for building **native** artifacts
    - `node_js`, `j2v8_cmake`, `j2v8_jni`, `j2v8_cpp`, `j2v8_optimize`
- `j2v8` ... runs all build-steps, except for `nodejs` and `j2v8test`
- `java` ... alias for the single `j2v8java` step
- `test` ... alias for the single `j2v8test` step

#### Anti-Steps
provide a way to remove a particular step, or a step-alias from the set of build-steps that should be run. To use such an anti-step, just prefix any of the available build-steps with the "~" symbol.

Build everything but do not optimize and do not run J2V8 unit tests:<br/>
`build <...other-args> all ~j2v8optimize ~test`

Build only the Java parts and also run tests:<br/>
`build <...other-args> all ~native`

#### Step-Arguments

For some of the build-steps, you can pass additional command-line parameters that will be added as arguments when the CLI build-tool of this particular build-step is run.

Run the `j2v8test` step with additional args that will be passed to maven:<br/>
(e.g. run only the `LibraryLoaderTest`)<br/>
`build -t linux -a x64 --j2v8test="-Dtest=LibraryLoaderTest"`



# Build-Steps

The J2V8 build-system performs several build steps in a fixed order to produce the final J2V8 packages for usage on the designated target platforms. What follows is a short summary for what each of the executed build-steps does and what output artifacts are produced by each step.

```
Node.js --> CMake --> JNI --> C++ --> Optimize --> Java/Android Build --> Java/Android Test
```
---
## Node.js
CLI name: `nodejs`

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
CLI name: `j2v8cmake`

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
CLI name: `j2v8jni`

Generate the JNI glue header file from the native method definitions of the Java `V8` class.

__Inputs__:
- Java V8.class file
    - `./target/classes/com/eclipsesource/v8/V8.class`

__Artifacts:__
- J2V8 C++ JNI header file
    - `./jni/com_eclipsesource_v8_V8Impl.h`
---
## C++
CLI name: `j2v8cpp`

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
CLI name: `j2v8optimize`

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
CLI name: `j2v8java` / `java`

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
## Java Tests
CLI name: `j2v8test` / `test`

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
