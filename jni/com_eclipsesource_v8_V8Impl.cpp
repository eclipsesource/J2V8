#include <jni.h>
#include <iostream>
#include <v8.h>
#include <map>
#include "com_eclipsesource_v8_V8Impl.h"

using namespace std;
using namespace v8;
std::map <int, Isolate*> v8Isolates;

void throwError( JNIEnv *env, const char *message );

Isolate* getIsolate(JNIEnv *env, int handle);

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1createIsolate
  (JNIEnv *, jobject, jint handle) {
	v8Isolates[handle] = Isolate::New();
}


JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1release
  (JNIEnv *env, jobject obj, jint handle) {
	if ( v8Isolates.count(handle) == 0 ) {
		return;
	}
	v8Isolates[handle]->Dispose();
	v8Isolates.erase(handle);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1executeVoidScript
  (JNIEnv *env, jobject, jint handle, jstring, jobject) {
	Isolate* isolate = getIsolate(env, handle);
}

Isolate* getIsolate(JNIEnv *env, int handle) {
	if ( v8Isolates.find(handle) == v8Isolates.end() ) {
		throwError(env, "V8 isolate not found.");
		return NULL;
	}
	return v8Isolates[handle];
}

void throwError( JNIEnv *env, const char *message )
{
    jclass exClass;
    const char *className = "java/lang/Error";

    exClass = (env)->FindClass(className);
    (env)->ThrowNew(exClass, message );

}
