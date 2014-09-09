cd target/classes
javah com.eclipsesource.v8.V8
cp com_eclipsesource_v8_V8.h ../../jni/com_eclipsesource_v8_V8Impl.h
cd ../../jni
clang++  -stdlib=libstdc++ -Iinclude ../v8_x64_working/libv8_base.x64.a ../v8_x64_working/libv8_nosnapshot.x64.a ../v8_x64_working/libicudata.a ../v8_x64_working/libicuuc.a ../v8_x64_working/libicui18n.a -I ../v8 -I /Library/Java/JavaVirtualMachines/jdk1.7.0_25.jdk/Contents/Home/include -I /Library/Java/JavaVirtualMachines/jdk1.7.0_25.jdk/Contents/Home/include/darwin/ -shared -o libj2v8.dylib com_eclipsesource_v8_V8Impl.cpp 

