#include <jni.h>
#include <iostream>
#include <v8.h>
#include <map>
#include "com_eclipsesource_v8_V8Impl.h"

using namespace std;
using namespace v8;

class V8Runtime {
public:
    Isolate* isolate;
    Isolate::Scope* isolate_scope;
    Context::Scope* context_scope;
    Persistent<ObjectTemplate> globalObjectTemplate;
    Persistent<Context> context_;
};

std::map <int, V8Runtime*> v8Isolates;

void throwError( JNIEnv *env, const char *message );

Isolate* getIsolate(JNIEnv *env, int handle);

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1createIsolate
  (JNIEnv *, jobject, jint handle) {
	v8Isolates[handle] = new V8Runtime();
	v8Isolates[handle]->isolate = Isolate::New();
	v8Isolates[handle]->isolate_scope = new Isolate::Scope(v8Isolates[handle]->isolate);
	HandleScope handle_scope(v8Isolates[handle]->isolate);
	v8Isolates[handle]->globalObjectTemplate.Reset(v8Isolates[handle]->isolate, ObjectTemplate::New(v8Isolates[handle]->isolate));
	Handle<Context> context = Context::New(v8Isolates[handle]->isolate, NULL, Local<ObjectTemplate>::New(v8Isolates[handle]->isolate, v8Isolates[handle]->globalObjectTemplate));
	v8Isolates[handle]->context_scope = new Context::Scope(context);
	v8Isolates[handle]->context_.Reset(v8Isolates[handle]->isolate, context);
}


JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1release
  (JNIEnv *env, jobject obj, jint handle) {
	if ( v8Isolates.count(handle) == 0 ) {
		return;
	}
	Isolate* isolate = getIsolate(env, handle);
	HandleScope handle_scope(isolate);
	delete(v8Isolates[handle]->isolate_scope);
	delete(v8Isolates[handle]->context_scope);
	v8Isolates[handle]->isolate->Dispose();
	delete(v8Isolates[handle]);
	v8Isolates.erase(handle);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1executeVoidScript
  (JNIEnv *env, jobject, jint handle, jstring, jobject) {
	Isolate* isolate = getIsolate(env, handle);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8Impl__1executeIntScript
  (JNIEnv * env, jobject, jint handle, jstring jjstring) {
	Isolate* isolate = getIsolate(env, handle);
	HandleScope handle_scope(isolate);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);
	Local<Script> script = Script::Compile(source);
	Local<Value> result = script->Run();
	return result->Int32Value();
}

Isolate* getIsolate(JNIEnv *env, int handle) {
	if ( v8Isolates.find(handle) == v8Isolates.end() ) {
		throwError(env, "V8 isolate not found.");
		return NULL;
	}
	return v8Isolates[handle]->isolate;
}

void throwError( JNIEnv *env, const char *message )
{
    jclass exClass;
    const char *className = "java/lang/Error";

    exClass = (env)->FindClass(className);
    (env)->ThrowNew(exClass, message );
}
