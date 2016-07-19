SUFFIX="-P release"
#SUFFIX=""

cp pom.xml pom_template.xml

echo "Deploying MacOS"
rm src/main/resources/*j2v8*
cp jni/libj2v8_macosx_x86_64.dylib src/main/resources/libj2v8_macosx_x86_64.dylib
sed s/\$\{os\}/macosx/g < pom_template.xml  > pom1.xml
sed s/\$\{arch\}/x86_64/g < pom1.xml  > pom2.xml
sed s/\$\{ws\}/cocoa/g < pom2.xml  > pom.xml
mvn -Dos=macosx -Darch=x86_64 clean deploy $SUFFIX
STATUS=$?
cp pom_template.xml pom.xml
rm pom1.xml
rm pom2.xml
if [ $STATUS -eq 0 ]; then
 echo "MacOS Deployment Successful"
else
 echo "MacOS Deployment Failed"
 exit $STATUS
fi
cp target/j2v8_macosx_x86_64-*.jar releng/plugins

echo "Deploying Win32/x86"
rm src/main/resources/*j2v8*
cp jni/libj2v8_win32_x86.dll src/main/resources/libj2v8_win32_x86.dll
sed s/\$\{os\}/win32/g < pom_template.xml  > pom1.xml
sed s/\$\{arch\}/x86/g < pom1.xml  > pom2.xml
sed s/\$\{ws\}/win32/g < pom2.xml  > pom.xml
mvn -Dos=win32 -Darch=x86 clean deploy $SUFFIX
STATUS=$?
cp pom_template.xml pom.xml
rm pom1.xml
rm pom2.xml
if [ $STATUS -eq 0 ]; then
 echo "Win32 Deployment Successful"
else
 echo "Win32 Deployment Failed"
 exit $STATUS
fi
cp target/j2v8_win32_x86-*.jar releng/plugins

echo "Deploying Win32/x64"
rm src/main/resources/*j2v8*
cp jni/libj2v8_win32_x86_64.dll src/main/resources/libj2v8_win32_x86_64.dll
sed s/\$\{os\}/win32/g < pom_template.xml  > pom1.xml
sed s/\$\{arch\}/x86_64/g < pom1.xml  > pom2.xml
sed s/\$\{ws\}/win32/g < pom2.xml  > pom.xml
mvn -Dos=win32 -Darch=x86_64 clean deploy $SUFFIX
STATUS=$?
cp pom_template.xml pom.xml
rm pom1.xml
rm pom2.xml
if [ $STATUS -eq 0 ]; then
 echo "Win32_64 Deployment Successful"
else
 echo "Win32_64 Deployment Failed"
 exit $STATUS
fi
cp target/j2v8_win32_x86_64-*.jar releng/plugins

echo "Deploying Linux"
rm src/main/resources/*j2v8*
cp jni/libj2v8_linux_x86_64.so src/main/resources/libj2v8_linux_x86_64.so
sed s/\$\{os\}/linux/g < pom_template.xml  > pom1.xml
sed s/\$\{arch\}/x86_64/g < pom1.xml  > pom2.xml
sed s/\$\{ws\}/gtk/g < pom2.xml  > pom.xml
mvn -Dos=linux -Darch=x86_64 clean deploy $SUFFIX
STATUS=$?
cp pom_template.xml pom.xml
rm pom1.xml
rm pom2.xml
if [ $STATUS -eq 0 ]; then
 echo "Linux Deployment Successful"
else
 echo "Linux Deployment Failed"
 exit $STATUS
fi
cp target/j2v8_linux_x86_64-*.jar releng/plugins

rm src/main/resources/*j2v8*

echo "Deploying Android"
./gradlew clean build uploadArchives
