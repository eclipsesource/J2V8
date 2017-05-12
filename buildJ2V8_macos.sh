cd jni
clang++  -stdlib=libc++ -Iinclude -I../dep_includes_macosx \
-I../node -I../node/deps/v8 -I../node/deps/v8/include -I../node/src \
-Wl,-force_load,../node/out/Release/libnode.a \
  ../node/out/Release/libuv.a \
  ../node/out/Release/libopenssl.a \
  ../node/out/Release/libhttp_parser.a \
  ../node/out/Release/libgtest.a \
  ../node/out/Release/libzlib.a \
  ../node/out/Release/libcares.a \
  ../node/out/Release/libv8_base.a \
  ../node/out/Release/libv8_nosnapshot.a \
  ../node/out/Release/libv8_libplatform.a \
  ../node/out/Release/libv8_libbase.a \
  ../node/out/Release/libv8_libsampler.a \
  ../node/out/Release/libstandalone_inspector.a \
  ../node/out/Release/libicui18n.a \
  ../node/out/Release/libicuucx.a \
  ../node/out/Release/libicudata.a \
  ../node/out/Release/libicustubdata.a \
-shared -o libj2v8_macosx_x86_64.dylib com_eclipsesource_v8_V8Impl.cpp  -std=c++11 -Wwritable-strings \
-D NODE_COMPATIBLE=1
