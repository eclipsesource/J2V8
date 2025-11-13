# Publishing J2V8 to Maven Central

This guide explains how to build and publish J2V8 as a multi-architecture Android AAR to Maven Central.

## Prerequisites

- Docker installed and running
- GPG key for signing artifacts
- Maven Central account and credentials

## Required Environment Variables

Before running the publish commands, set these environment variables:

```bash
# GPG Signing
export KEYSTORE_PASSWORD="your-gpg-passphrase"
export KEY_ID="12345678"  # Last 8 characters of your GPG key ID

# Maven Central Credentials
export MAVEN_REPO_TOKEN="your-maven-central-token"
```

### Getting Your GPG Key ID

```bash
# List your GPG keys
gpg --list-secret-keys --keyid-format LONG

# The key ID is the 8-character suffix after "sec   rsa4096/"
# Example: sec   rsa4096/ABCD12345678EFGH
# KEY_ID would be "5678EFGH" (last 8 chars)
```

### Getting Your Maven Central Token

1. Log in to https://central.sonatype.com
2. Go to your account settings
3. Generate a user token
4. Use the token value for `MAVEN_REPO_TOKEN`

## Build Process

### Step 1: Build V8 for All Android Architectures

Build the V8 JavaScript engine for each architecture (runs in Docker):

```bash
python build.py -t android -a arm --docker v8
python build.py -t android -a arm64 --docker v8
python build.py -t android -a x86 --docker v8
python build.py -t android -a x86_64 --docker v8
```

**Note:** These V8 builds only need to be done once, or when upgrading V8 versions.

### Step 2: Build J2V8 JNI Libraries for All Architectures

Build the J2V8 JNI wrapper libraries (runs in Docker):

```bash
python build.py -t android -a arm --docker j2v8
python build.py -t android -a arm64 --docker j2v8
python build.py -t android -a x86 --docker j2v8
python build.py -t android -a x86_64 --docker j2v8
```

Each command will:
- Compile the JNI C++ code
- Create the native `.so` library
- Copy it to `src/main/jniLibs/{arch}/libj2v8.so`
- Build the Java code with Gradle

### Step 3: Package Multi-Architecture AAR

Package all four architecture libraries into a single AAR (runs in Docker):

```bash
python build.py -t android --docker j2v8package
```

This creates `build/outputs/aar/j2v8-release.aar` containing:
- `armeabi-v7a/libj2v8.so`
- `arm64-v8a/libj2v8.so`
- `x86/libj2v8.so`
- `x86_64/libj2v8.so`

Android will automatically select the correct library based on device architecture.

### Step 4: Publish to Maven Central

Publish the multi-architecture AAR (runs locally, not in Docker):

```bash
python build.py -t android j2v8publish
```

This will:
1. Update `pom.xml` with artifact information
2. Copy the AAR to the Maven target directory
3. Run Maven to create sources and javadoc JARs
4. Sign all artifacts with GPG
5. Generate MD5 and SHA1 checksums
6. Create a Maven Central bundle with proper directory structure
7. Upload the bundle to Maven Central

## Version Management

### When to Increment Version

You must increment the version number when:
- Publishing a new release (Maven Central doesn't allow republishing the same version)
- Making any changes to the published artifact
- Fixing bugs or adding features

### Version Number Location

The version is defined in **`build_system/build_settings.py`** as the single source of truth:

```python
J2V8_VERSION_MAJOR, J2V8_VERSION_MINOR, J2V8_VERSION_PATCH = 6, 3, 1
```

### How to Update the Version

1. **Edit `build_system/build_settings.py`:**
   ```python
   # Change the version numbers
   J2V8_VERSION_MAJOR, J2V8_VERSION_MINOR, J2V8_VERSION_PATCH = 6, 3, 2
   ```

2. **The publish process automatically updates:**
   - `pom.xml` - Maven will use the new version from build_settings.py
   - Build artifacts will be named with the new version

3. **Manually update reference files:**
   - `example/build.gradle` - Update the dependency version
   - `PUBLISHING.md` - Update version references in examples (optional)

### Semantic Versioning Guidelines

Follow semantic versioning (MAJOR.MINOR.PATCH):

- **MAJOR** (6.x.x): Breaking API changes, incompatible with previous versions
- **MINOR** (x.3.x): New features, backwards compatible
- **PATCH** (x.x.1): Bug fixes, backwards compatible

Examples:
- Bug fix or packaging fix: `6.3.0` → `6.3.1`
- New feature: `6.3.1` → `6.4.0`
- Breaking change: `6.4.0` → `7.0.0`

## Verification

After publishing, verify the upload:

1. Check https://central.sonatype.com/publishing
2. Look for your deployment (`com.eclipsesource.j2v8:j2v8:6.3.1`)
3. Verify all files are present and properly signed

## Published Artifact

Users can add J2V8 to their Android projects with:

```gradle
dependencies {
    implementation 'com.eclipsesource.j2v8:j2v8:6.3.1'
}
```

The AAR contains native libraries for all Android architectures, so it works on any Android device without additional configuration.

## Troubleshooting

### GPG Signing Fails

If you get GPG errors, verify:
- Your GPG key is in your keyring: `gpg --list-secret-keys`
- The `KEYSTORE_PASSWORD` matches your key's passphrase
- The `KEY_ID` is correct (last 8 characters)

### Maven Central Rejects Upload

Common issues:
- Missing signatures (`.asc` files)
- Missing checksums (`.md5`, `.sha1` files)
- Incorrect directory structure in bundle
- Missing required artifacts (sources, javadoc)

Check the bundle contents:
```bash
unzip -l target/j2v8-6.3.1-central-bundle.zip
```

The structure should be:
```
com/eclipsesource/j2v8/j2v8/6.3.1/
  ├── j2v8-6.3.1.pom
  ├── j2v8-6.3.1.aar
  ├── j2v8-6.3.1-sources.jar
  ├── j2v8-6.3.1-javadoc.jar
  └── (all .asc, .md5, .sha1 files)
```

### Docker Container Fails

If Docker builds fail:
- Ensure Docker is running
- Try rebuilding the Docker image: `docker build -t j2v8-android ./docker/android`
- Check Docker logs for specific errors

## Notes

- The entire build process takes significant time (V8 compilation is slow)
- V8 builds can be reused across J2V8 versions (only rebuild when V8 version changes)
- The package step preserves all architecture libraries (doesn't run `clean`)
- Publishing automatically generates correct artifact names and Maven metadata
