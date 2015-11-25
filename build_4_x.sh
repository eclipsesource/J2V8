cd target/classes
javah com.eclipsesource.v8.V8
cp com_eclipsesource_v8_V8.h ../../jni/com_eclipsesource_v8_V8Impl.h
cd ../../jni
clang++  -stdlib=libstdc++ -Iinclude ../v8_bleeding_edge/libv8_libbase.a ../v8_bleeding_edge/libv8_libplatform.a ../v8_bleeding_edge/libv8_base.a ../v8_bleeding_edge/libv8_nosnapshot.a ../v8_bleeding_edge/libicudata.a ../v8_bleeding_edge/libicuuc.a ../v8_bleeding_edge/libicui18n.a -I ../v8_bleeding_edge -I /Library/Java/JavaVirtualMachines/jdk1.7.0_71.jdk/Contents/Home/include -I /Library/Java/JavaVirtualMachines/jdk1.7.0_71.jdk/Contents/Home/include/darwin/ -shared -o libj2v8_macosx_x86_64.dylib com_eclipsesource_v8_V8Impl.cpp  -std=c++11

