#!/bin/bash
set -e
rm -rf j2v8
git clone https://github.com/eclipsesource/j2v8
cd j2v8

### Add the commit ID for the latest commit to V8
COMMIT_ID=`git rev-parse HEAD`
sed s/Unknown\ revision\ ID/$COMMIT_ID/ ./src/main/java/com/eclipsesource/v8/V8.java > ./src/main/java/com/eclipsesource/v8/V8.java.updated
mv ./src/main/java/com/eclipsesource/v8/V8.java.updated ./src/main/java/com/eclipsesource/v8/V8.java

TARGET_PLATFORM=$PWD/../../../node.out-7_4_0.tar.gz
if [ -f $TARGET_PLATFORM ]; then
  cp $TARGET_PLATFORM .
else 
  curl -O http://download.eclipsesource.com/j2v8/v8/node.out-7_4_0.tar.gz
fi
./buildAll.sh
./gradlew clean build uploadArchives -x test

SUFFIX="-P release"
#SUFFIX=""

cp pom.xml pom_template.xml

echo "Deploying Linux"
mkdir src/main/resources
cp jni/libj2v8_linux_x86_64.so src/main/resources/libj2v8_linux_x86_64.so
sed s/\$\{os\}/linux/g < pom_template.xml  > pom1.xml
sed s/\$\{arch\}/x86_64/g < pom1.xml  > pom2.xml
sed s/\$\{ws\}/gtk/g < pom2.xml  > pom.xml
mvn -Dos=linux -Darch=x86_64 clean deploy $SUFFIX -Dmaven.test.skip=true
STATUS=$?
cp pom_template.xml pom.xml
rm pom_template.xml
rm pom1.xml
rm pom2.xml
if [ $STATUS -eq 0 ]; then
 echo "Linux Deployment Successful"
else
 echo "Linux Deployment Failed"
 exit $STATUS
fi
