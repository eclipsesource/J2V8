export JAVA_HOME=$(/usr/libexec/java_home)
V8=../../v8
V8_BUILD=$V8/out/x64.release

cd target/classes
javah com.eclipsesource.v8.V8
cp com_eclipsesource_v8_V8.h ../../jni/com_eclipsesource_v8_V8Impl.h
cd ../../jni

clang++  -stdlib=libstdc++ \
        -Iinclude $V8_BUILD/libv8_libbase.a \
        $V8_BUILD/libv8_libplatform.a \
        $V8_BUILD/libv8_base.a \
        $V8_BUILD/libv8_nosnapshot.a \
        $V8_BUILD/libicudata.a \
        $V8_BUILD/libicuuc.a \
        $V8_BUILD/libicui18n.a \
        -I $V8 \
        -I $JAVA_HOME/include \
        -I $JAVA_HOME/include/darwin/ \
        -shared -o libj2v8_macosx_x86_64.dylib \
        com_eclipsesource_v8_V8Impl.cpp \
        -std=c++11

echo fini
