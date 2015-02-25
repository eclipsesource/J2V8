SUFFIX=""

cp pom.xml pom_template.xml

echo "Deploying Android ARM"
rm src/main/resources/*j2v8*
cp jni/libj2v8_android_armeabi-v7a.so src/main/resources/libj2v8_android_armv7l.so
sed s/\$\{os\}/android/g < pom_template.xml  > pom1.xml
sed s/\$\{arch\}/armv7l/g < pom1.xml  > pom.xml
mvn -Dos=android -Darch=armv7l clean deploy $SUFFIX
cp pom_template.xml pom.xml
rm pom1.xml
STATUS=$?
if [ $STATUS -eq 0 ]; then
 echo "Android ARM Deployment Successful"
else
 echo "Android ARM Deployment Failed"
 exit $STATUS
fi

echo "Deploying Android x86"
rm src/main/resources/*j2v8*
cp jni/libj2v8_android_x86.so src/main/resources/libj2v8_android_x86.so
sed s/\$\{os\}/android/g < pom_template.xml  > pom1.xml
sed s/\$\{arch\}/x86/g < pom1.xml  > pom.xml
mvn -Dos=android -Darch=x86 clean deploy $SUFFIX
cp pom_template.xml pom.xml
rm pom1.xml
STATUS=$?
if [ $STATUS -eq 0 ]; then
 echo "Android x86 Deployment Successful"
else
 echo "Android Deployment Failed"
 exit $STATUS
fi

echo "Deploying Android All In"
rm src/main/resources/*j2v8*
cp jni/libj2v8_android_x86.so src/main/resources/libj2v8_android_x86.so
cp jni/libj2v8_android_armeabi-v7a.so src/main/resources/libj2v8_android_armv7l.so
sed s/\$\{os\}/android/g < pom_template.xml  > pom1.xml
sed s/_\$\{arch\}//g < pom1.xml  > pom.xml
mvn -Dos=android -Darch=_ clean deploy $SUFFIX
cp pom_template.xml pom.xml
rm pom1.xml
STATUS=$?
if [ $STATUS -eq 0 ]; then
 echo "Android AllIn Deployment Successful"
else
 echo "Android AllIn Deployment Failed"
 exit $STATUS
fi

echo "Deploying MacOS"
rm src/main/resources/*j2v8*
cp jni/libj2v8_macosx_x86_64.dylib src/main/resources/libj2v8_macosx_x86_64.dylib
sed s/\$\{os\}/macosx/g < pom_template.xml  > pom1.xml
sed s/\$\{arch\}/x86_64/g < pom1.xml  > pom.xml
mvn -Dos=macosx -Darch=x86_64 clean deploy $SUFFIX
cp pom_template.xml pom.xml
rm pom1.xml
STATUS=$?
if [ $STATUS -eq 0 ]; then
 echo "MacOS Deployment Successful"
else
 echo "MacOS Deployment Failed"
 exit $STATUS
fi

echo "Deploying Win32"
rm src/main/resources/*j2v8*
cp jni/libj2v8_win32_x86.dll src/main/resources/libj2v8_win32_x86.dll
sed s/\$\{os\}/win32/g < pom_template.xml  > pom1.xml
sed s/\$\{arch\}/x86/g < pom1.xml  > pom.xml
mvn -Dos=win32 -Darch=x86 clean deploy $SUFFIX
cp pom_template.xml pom.xml
rm pom1.xml
STATUS=$?
if [ $STATUS -eq 0 ]; then
 echo "Win32 Deployment Successful"
else
 echo "Win32 Deployment Failed"
 exit $STATUS
fi

#echo "Deploying Linux"
#rm src/main/resources/*j2v8*
#cp jni/libj2v8_linux_amd64.so src/main/resources/libj2v8_linux_amd64.so
#sed s/\$\{os\}/linux/g < pom_template.xml  > pom1.xml
#sed s/\$\{arch\}/amd64/g < pom1.xml  > pom.xml
#mvn -Dos=linux -Darch=amd64 clean deploy $SUFFIX
#cp pom_template.xml pom.xml
#rm pom1.xml
#STATUS=$?
#if [ $STATUS -eq 0 ]; then
#echo "Linux Deployment Successful"
#else
#echo "Linux Deployment Failed"
#exit $STATUS
#fi
