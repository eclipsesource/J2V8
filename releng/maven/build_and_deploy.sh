#!/bin/bash
set -e
rm -rf j2v8
git clone https://github.com/eclipsesource/j2v8
cd j2v8

### Add the commit ID for the latest commit to V8
COMMIT_ID=`git rev-parse HEAD`
sed s/Unknown\ revision\ ID/$COMMIT_ID/ ./src/main/java/com/eclipsesource/v8/V8.java > ./src/main/java/com/eclipsesource/v8/V8.java.updated
mv ./src/main/java/com/eclipsesource/v8/V8.java.updated ./src/main/java/com/eclipsesource/v8/V8.java
exit 0;

TARGET_PLATFORM=$PWD/../../../node.out-7_4_0.tar.gz
if [ -f $TARGET_PLATFORM ]; then
  cp $TARGET_PLATFORM .
else 
  curl -O http://download.eclipsesource.com/j2v8/v8/node.out-7_4_0.tar.gz
fi
./buildAll.sh
./gradlew clean build uploadArchives -x test
