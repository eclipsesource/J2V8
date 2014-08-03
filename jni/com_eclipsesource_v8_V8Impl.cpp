#include <jni.h>
#include <iostream>
#include <v8.h>
#include "com_eclipsesource_v8_V8Impl.h"

using namespace std;

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1createIsolate
  (JNIEnv *, jobject) {
	cout << "Creating v8 runtime" << endl;
}


JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1release
  (JNIEnv *, jobject) {

}
