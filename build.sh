cd bin
javah com.eclipsesource.v8.V8Impl
cp com_eclipsesource_v8_V8Impl.h ../jni
cd ../jni
clang++ -stdlib=libstdc++ -Iinclude ../v8/libv8_base.x64.a ../v8/libv8_nosnapshot.x64.a ../v8/libicudata.a ../v8/libicuuc.a ../v8/libicui18n.a -I ../v8 -I /Library/Java/JavaVirtualMachines/jdk1.7.0_25.jdk/Contents/Home/include -I /Library/Java/JavaVirtualMachines/jdk1.7.0_25.jdk/Contents/Home/include/darwin/ -shared -o libj2v8.dylib com_eclipsesource_v8_V8Impl.cpp 

