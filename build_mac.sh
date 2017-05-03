cp pom.xml pom_template.xml
echo "Building for MacOS"
rm src/main/resources/*j2v8*
cp jni/libj2v8_macosx_x86_64.dylib src/main/resources/libj2v8_macosx_x86_64.dylib
sed s/\$\{os\}/macosx/g < pom_template.xml  > pom1.xml
sed s/\$\{arch\}/x86_64/g < pom1.xml  > pom.xml
mvn -Dos=macosx -Darch=x86_64 clean install
cp pom_template.xml pom.xml
rm pom1.xml
rm pom_template.xml
STATUS=$?
if [ $STATUS -eq 0 ]; then
 echo "MacOS Build Successful"
else
 echo "MacOS Build Failed"
 exit $STATUS
fi
