cd target/classes
javah com.eclipsesource.v8.V8
cp com_eclipsesource_v8_V8.h ../../jni/com_eclipsesource_v8_V8Impl.h
cd ../../jni
clang++  -stdlib=libstdc++ -Iinclude -I../node/include -I../node -Wl,-force_load,../node/libnode.a ../node/libv8_libbase.a ../node/libv8_libplatform.a ../node/libv8_base.a ../node/libv8_nosnapshot.a ../node/libuv.a ../node/libopenssl.a ../node/libhttp_parser.a ../node/libgtest.a ../node/libzlib.a ../node/libcares.a -I../v8_bleeding_edge -I /Library/Java/JavaVirtualMachines/jdk1.7.0_71.jdk/Contents/Home/include -I /Library/Java/JavaVirtualMachines/jdk1.7.0_71.jdk/Contents/Home/include/darwin/ -shared -o libj2v8_macosx_x86_64.dylib com_eclipsesource_v8_V8Impl.cpp  -std=c++11 -Wwritable-strings

