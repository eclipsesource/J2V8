#include <jni.h>
#include <iostream>
#include <v8-debug.h>
#include <v8.h>
#include <map>
#include "com_eclipsesource_v8_V8Impl.h"

using namespace std;
using namespace v8;

class V8Runtime {
public:
    Isolate* isolate;
    Isolate::Scope* isolate_scope;
    Persistent<ObjectTemplate> globalObjectTemplate;
    Persistent<Context> context_;
    std::map <int, Persistent<Object>* > objects;
    JNIEnv* env;
    jobject v8;
};

const char* ToCString(const v8::String::Utf8Value& value) {
  return *value ? *value : "<string conversion failed>";
}

std::map <int, V8Runtime*> v8Isolates;
JavaVM* jvm = NULL;
jclass v8cls = NULL;
jclass stringCls = NULL;

void throwParseException( JNIEnv *env, Isolate* isolate, TryCatch* tryCatch);
void throwError( JNIEnv *env, const char *message );
void throwExecutionException( JNIEnv *env, const char *message );
void throwResultUndefinedException( JNIEnv *env, const char *message );
Isolate* getIsolate(JNIEnv *env, int handle);
void setupJNIContext(int v8RuntimeHandle, JNIEnv *env, jobject v8 );

#define DELETE_SCRIPT_ORIGIN_PTR(ptr)\
		if ( ptr != NULL ) { \
			delete(ptr); \
		}

#define SCRIPT_ORIGIN_PTR(result, name, number) result = NULL;\
		if ( name != NULL ) { \
			result = createScriptOrigin(env, isolate, name, number); \
		}

void debugHandler() {
	JNIEnv * g_env;
	// double check it's all ok
	int getEnvStat = jvm->GetEnv((void **) &g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED) {
		if (jvm->AttachCurrentThread((void **) &g_env, NULL) != 0) {
			std::cout << "Failed to attach" << std::endl;
		}
	} else if (getEnvStat == JNI_OK) {
		//
	} else if (getEnvStat == JNI_EVERSION) {
		std::cout << "GetEnv: version not supported" << std::endl;
	}

	jmethodID processDebugMessage = g_env->GetStaticMethodID(v8cls, "debugMessageReceived", "()V");
	g_env->CallStaticVoidMethod(v8cls, processDebugMessage);

	if (g_env->ExceptionCheck()) {
		g_env->ExceptionDescribe();
	}

	jvm->DetachCurrentThread();
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1enableDebugSupport
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint port, jboolean waitForConnection) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return false;
	}
	v8::Isolate::Scope isolateScope(isolate);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	bool result = v8::Debug::EnableAgent("j2v8", port, waitForConnection);
	v8::Debug::SetDebugMessageDispatchHandler(&debugHandler);
	return result;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1disableDebugSupport
  (JNIEnv *env, jobject, jint v8RuntimeHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	v8::Isolate::Scope isolateScope(isolate);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	v8::Debug::DisableAgent();
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1processDebugMessages
  (JNIEnv *env, jobject, jint v8RuntimeHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	v8::Isolate::Scope isolateScope(isolate);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	v8::Debug::ProcessDebugMessages();
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1createIsolate
  (JNIEnv *env, jobject, jint handle) {
	if (jvm == NULL ) {
		// on first creation, store the JVM and a handle to V8.class
		env->GetJavaVM(&jvm);
		v8cls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8"));
		stringCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/String"));

	}
	v8Isolates[handle] = new V8Runtime();
	v8Isolates[handle]->isolate = Isolate::New();
	v8Isolates[handle]->isolate_scope = new Isolate::Scope(v8Isolates[handle]->isolate);
	HandleScope handle_scope(v8Isolates[handle]->isolate);
	v8Isolates[handle]->globalObjectTemplate.Reset(v8Isolates[handle]->isolate, ObjectTemplate::New(v8Isolates[handle]->isolate));
	Handle<Context> context = Context::New(v8Isolates[handle]->isolate, NULL, Local<ObjectTemplate>::New(v8Isolates[handle]->isolate, v8Isolates[handle]->globalObjectTemplate));
	v8Isolates[handle]->context_.Reset(v8Isolates[handle]->isolate, context);
	v8Isolates[handle]->objects[0] = new Persistent<Object>;
	v8Isolates[handle]->objects[0]->Reset(v8Isolates[handle]->isolate, context->Global()->GetPrototype()->ToObject());
}

void createPersistentContainer(V8Runtime* runtime, int handle) {
	if ( runtime->objects.find(handle) == runtime->objects.end() ) {
		runtime->objects[handle] = new Persistent<Object>;
	}
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Object
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Local<Object> obj = Object::New(isolate);
	createPersistentContainer(v8Isolates[v8RuntimeHandle], objectHandle);
	v8Isolates[v8RuntimeHandle]->objects[objectHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, obj);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Array
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Local<Array> array = Array::New(isolate);
	createPersistentContainer(v8Isolates[v8RuntimeHandle], arrayHandle);
	v8Isolates[v8RuntimeHandle]->objects[arrayHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, array);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1getObject
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint parentHandle, jstring objectKey, jint resultHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	const char* utf_string = env -> GetStringUTFChars(objectKey, NULL);
	Local<String> v8Key = v8::String::NewFromUtf8(isolate, utf_string);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parentHandle]);
	Handle<Value> v8Value = parentObject->Get(v8Key);

	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsObject()) {
		throwResultUndefinedException(env, "");
		return;
	}
	createPersistentContainer(v8Isolates[v8RuntimeHandle], resultHandle);
	Handle<Object> obj = v8Value->ToObject();
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, obj);
	env->ReleaseStringUTFChars(objectKey, utf_string);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1getArray
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint parentHandle, jstring objectKey, jint resultHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	const char* utf_string = env -> GetStringUTFChars(objectKey, NULL);
	Local<String> v8Key = v8::String::NewFromUtf8(isolate, utf_string);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parentHandle]);
	Handle<Value> v8Value = parentObject->Get(v8Key);

	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsArray()) {
			throwResultUndefinedException(env, "");
			return;
	}
	createPersistentContainer(v8Isolates[v8RuntimeHandle], resultHandle);
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, v8Value->ToObject());
	env->ReleaseStringUTFChars(objectKey, utf_string);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1release
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle) {
	if ( v8Isolates.count(v8RuntimeHandle) == 0  ) {
		return;
	} else if ( v8Isolates[v8RuntimeHandle]->objects.find(objectHandle) == v8Isolates[v8RuntimeHandle]->objects.end() ) {
		return;
	}
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	HandleScope handle_scope(isolate);
	v8Isolates[v8RuntimeHandle]->objects[objectHandle]->Reset();
	delete(v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	v8Isolates[v8RuntimeHandle]->objects.erase(objectHandle);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1releaseArray
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle) {
	if ( v8Isolates.count(v8RuntimeHandle) == 0 ) {
		return;
	} else if ( v8Isolates[v8RuntimeHandle]->objects.find(arrayHandle) == v8Isolates[v8RuntimeHandle]->objects.end() ) {
		return;
	}
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	HandleScope handle_scope(isolate);
	v8Isolates[v8RuntimeHandle]->objects[arrayHandle]->Reset();
	delete(v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	v8Isolates[v8RuntimeHandle]->objects.erase(arrayHandle);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1releaseRuntime
  (JNIEnv *env, jobject, jint v8RuntimeHandle) {
	if ( v8Isolates.count(v8RuntimeHandle) == 0 ) {
		return;
	}
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	HandleScope handle_scope(isolate);
	v8Isolates[v8RuntimeHandle]->context_.Reset();
	delete(v8Isolates[v8RuntimeHandle]->isolate_scope);
	v8Isolates[v8RuntimeHandle]->isolate->Dispose();
	delete(v8Isolates[v8RuntimeHandle]);
	v8Isolates.erase(v8RuntimeHandle);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1contains
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return false;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	const char * utf_string = env -> GetStringUTFChars(key, NULL);
	Local<String> v8Key = String::NewFromUtf8(isolate, utf_string);
	bool result = global->Has( v8Key );
	env->ReleaseStringUTFChars(key, utf_string);
	return result;
}

JNIEXPORT jobjectArray JNICALL Java_com_eclipsesource_v8_V8__1getKeys
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return NULL;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Local<Array> properties = global->GetPropertyNames();

	int size = properties->Length();
	jobjectArray keys = (env)->NewObjectArray(size, stringCls, NULL);
	for ( int i = 0; i < size; i++ ) {
		jobject key = (env)->NewStringUTF( *String::Utf8Value( properties->Get(i)->ToString() ) );
		(env)->SetObjectArrayElement(keys, i, key);
	}
	return keys;
}

ScriptOrigin* createScriptOrigin(JNIEnv * env, Isolate* isolate, jstring jscriptName, jint jlineNumber = 0) {
	const char* cscriptName = env -> GetStringUTFChars(jscriptName, NULL);
	Local<String> scriptName = String::NewFromUtf8(isolate, cscriptName);
	Local<Integer> lineNumber = v8::Integer::New(isolate, jlineNumber);
	ScriptOrigin* result =  new v8::ScriptOrigin(scriptName, lineNumber);
	env->ReleaseStringUTFChars(jscriptName, cscriptName);
	return result;
}



JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeVoidScript
  (JNIEnv * env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	ScriptOrigin* SCRIPT_ORIGIN_PTR(scriptOriginPtr, jscriptName, jlineNumber);
	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source, scriptOriginPtr);
	DELETE_SCRIPT_ORIGIN_PTR(scriptOriginPtr);

	if ( tryCatch.HasCaught() ) {
		throwParseException(env, isolate, &tryCatch);
		return;
	}
	script->Run();
	env->ReleaseStringUTFChars(jjstring, js);
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1executeDoubleScript
  (JNIEnv * env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	ScriptOrigin* SCRIPT_ORIGIN_PTR(scriptOriginPtr, jscriptName, jlineNumber);
	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source, scriptOriginPtr);
	DELETE_SCRIPT_ORIGIN_PTR(scriptOriginPtr);

	if ( tryCatch.HasCaught() ) {
		throwParseException(env, isolate, &tryCatch);
		return 0;
	}
	Local<Value> result = script->Run();

	if (result.IsEmpty() || result->IsUndefined() || !result->IsNumber() ) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->NumberValue();
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1executeBooleanScript
  (JNIEnv *env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	ScriptOrigin* SCRIPT_ORIGIN_PTR(scriptOriginPtr, jscriptName, jlineNumber);
	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source, scriptOriginPtr);
	DELETE_SCRIPT_ORIGIN_PTR(scriptOriginPtr);

	if ( tryCatch.HasCaught() ) {
		throwParseException(env, isolate, &tryCatch);
		return 0;
	}
	Local<Value> result = script->Run();

	if (result.IsEmpty() || result->IsUndefined() || !result->IsBoolean() ) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->BooleanValue();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1executeStringScript
  (JNIEnv *env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return NULL;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);
	env->ReleaseStringUTFChars(jjstring, js);

	ScriptOrigin* SCRIPT_ORIGIN_PTR(scriptOriginPtr, jscriptName, jlineNumber);
	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source, scriptOriginPtr);
	DELETE_SCRIPT_ORIGIN_PTR(scriptOriginPtr);

	if ( tryCatch.HasCaught() ) {
		throwParseException(env, isolate, &tryCatch);
		return 0;
	}
	Local<Value> result = script->Run();

	if (result.IsEmpty() || result->IsUndefined() || !result->IsString()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	String::Utf8Value utf(result->ToString());
	return env->NewStringUTF(*utf);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1executeIntScript
  (JNIEnv * env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	ScriptOrigin* SCRIPT_ORIGIN_PTR(scriptOriginPtr, jscriptName, jlineNumber);
	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source, scriptOriginPtr);
	DELETE_SCRIPT_ORIGIN_PTR(scriptOriginPtr);

	if ( tryCatch.HasCaught() ) {
		throwParseException(env, isolate, &tryCatch);
		return 0;
	}
	Local<Value> result = script->Run();

	if (result.IsEmpty() || result->IsUndefined() || !result->IsInt32()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->Int32Value();
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeObjectScript
  (JNIEnv *env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jint resultHandle, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	ScriptOrigin* SCRIPT_ORIGIN_PTR(scriptOriginPtr, jscriptName, jlineNumber);
	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source, scriptOriginPtr);
	DELETE_SCRIPT_ORIGIN_PTR(scriptOriginPtr);

	if ( tryCatch.HasCaught() ) {
		throwParseException(env, isolate, &tryCatch);
		return;
	}
	Local<Value> result = script->Run();

	if (result.IsEmpty() || result->IsUndefined() || !result->IsObject()) {
		throwResultUndefinedException(env, "");
		return;
	}
	createPersistentContainer(v8Isolates[v8RuntimeHandle], resultHandle);
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, result->ToObject());
	env->ReleaseStringUTFChars(jjstring, js);
	return;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeArrayScript
  (JNIEnv *env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jint resultHandle, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	ScriptOrigin* SCRIPT_ORIGIN_PTR(scriptOriginPtr, jscriptName, jlineNumber);
	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source, scriptOriginPtr);
	DELETE_SCRIPT_ORIGIN_PTR(scriptOriginPtr);

	if ( tryCatch.HasCaught() ) {
		throwParseException(env, isolate, &tryCatch);
		return;
	}
	Local<Value> result = script->Run();

	if (result.IsEmpty() || result->IsUndefined() || !result->IsArray()) {
		throwResultUndefinedException(env, "");
		return;
	}
	createPersistentContainer(v8Isolates[v8RuntimeHandle], resultHandle);
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, result->ToObject());
	env->ReleaseStringUTFChars(jjstring, js);
	return;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeArrayFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle, jint resultHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	const char* functionName = env -> GetStringUTFChars(jfunctionName, NULL);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	int size = 0;
	Handle<Value>* args = NULL;
	if ( parameterHandle >= 0 ) {
		Handle<v8::Object> parameters = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parameterHandle]);
		size = Array::Cast(*parameters)->Length();
		args = new Handle<Value> [size];
		for (int i = 0; i < size; i++) {
			args[i] = parameters->Get(i);
		}
	}

	Handle<v8::Value> value = parentObject->Get(v8::String::NewFromUtf8(isolate, functionName));
	Handle<v8::Function> func = v8::Handle<v8::Function>::Cast(value);
	Handle<Value> result = func->Call(parentObject, size, args);
	if (result.IsEmpty() || result->IsUndefined() || !result->IsArray()) {
		throwResultUndefinedException(env, "");
		return;
	}
	createPersistentContainer(v8Isolates[v8RuntimeHandle], resultHandle);
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, result->ToObject());
	env->ReleaseStringUTFChars(jfunctionName, functionName);
	return;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeObjectFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle, jint resultHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	const char* functionName = env -> GetStringUTFChars(jfunctionName, NULL);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	int size = 0;
	Handle<Value>* args = NULL;
	if ( parameterHandle >= 0 ) {
		Handle<v8::Object> parameters = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parameterHandle]);
		size = Array::Cast(*parameters)->Length();
		args = new Handle<Value> [size];
		for (int i = 0; i < size; i++) {
			args[i] = parameters->Get(i);
		}
	}

	Handle<v8::Value> value = parentObject->Get(v8::String::NewFromUtf8(isolate, functionName));
	Handle<v8::Function> func = v8::Handle<v8::Function>::Cast(value);
	Handle<Value> result = func->Call(parentObject, size, args);
	if (result.IsEmpty() || result->IsUndefined() || !result->IsObject()) {
		throwResultUndefinedException(env, "");
		return;
	}
	createPersistentContainer(v8Isolates[v8RuntimeHandle], resultHandle);
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, result->ToObject());
	env->ReleaseStringUTFChars(jfunctionName, functionName);
	return;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1executeIntFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	const char* functionName = env -> GetStringUTFChars(jfunctionName, NULL);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	int size = 0;
	Handle<Value>* args = NULL;
	if ( parameterHandle >= 0 ) {
		Handle<v8::Object> parameters = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parameterHandle]);
		size = Array::Cast(*parameters)->Length();
		args = new Handle<Value> [size];
		for (int i = 0; i < size; i++) {
			args[i] = parameters->Get(i);
		}
	}

	Handle<v8::Value> value = parentObject->Get(v8::String::NewFromUtf8(isolate, functionName));
	Handle<v8::Function> func = v8::Handle<v8::Function>::Cast(value);
	Handle<Value> result = func->Call(parentObject, size, args);
	if (result.IsEmpty() || result->IsUndefined() || !result->IsInt32()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->Int32Value();
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1executeDoubleFunction
 (JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	const char* functionName = env -> GetStringUTFChars(jfunctionName, NULL);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	int size = 0;
	Handle<Value>* args = NULL;
	if ( parameterHandle >= 0 ) {
		Handle<v8::Object> parameters = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parameterHandle]);
		size = Array::Cast(*parameters)->Length();
		args = new Handle<Value> [size];
		for (int i = 0; i < size; i++) {
			args[i] = parameters->Get(i);
		}
	}

	Handle<v8::Value> value = parentObject->Get(v8::String::NewFromUtf8(isolate, functionName));
	Handle<v8::Function> func = v8::Handle<v8::Function>::Cast(value);
	Handle<Value> result = func->Call(parentObject, size, args);

	if (result.IsEmpty() || result->IsUndefined() || !result->IsNumber()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->NumberValue();
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1executeBooleanFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	const char* functionName = env -> GetStringUTFChars(jfunctionName, NULL);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	int size = 0;
	Handle<Value>* args = NULL;
	if ( parameterHandle >= 0 ) {
		Handle<v8::Object> parameters = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parameterHandle]);
		size = Array::Cast(*parameters)->Length();
		args = new Handle<Value> [size];
		for (int i = 0; i < size; i++) {
			args[i] = parameters->Get(i);
		}
	}

	Handle<v8::Value> value = parentObject->Get(v8::String::NewFromUtf8(isolate, functionName));
	Handle<v8::Function> func = v8::Handle<v8::Function>::Cast(value);
	Handle<Value> result = func->Call(parentObject, size, args);

	if (result.IsEmpty() || result->IsUndefined() || !result->IsBoolean()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->BooleanValue();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1executeStringFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	const char* functionName = env -> GetStringUTFChars(jfunctionName, NULL);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	int size = 0;
	Handle<Value>* args = NULL;
	if ( parameterHandle >= 0 ) {
		Handle<v8::Object> parameters = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parameterHandle]);
		size = Array::Cast(*parameters)->Length();
		args = new Handle<Value> [size];
		for (int i = 0; i < size; i++) {
			args[i] = parameters->Get(i);
		}
	}

	Handle<v8::Value> value = parentObject->Get(v8::String::NewFromUtf8(isolate, functionName));
	Handle<v8::Function> func = v8::Handle<v8::Function>::Cast(value);
	Handle<Value> result = func->Call(parentObject, size, args);

	if (result.IsEmpty() || result->IsUndefined() || !result->IsString()) {
		throwResultUndefinedException(env, "");
		return NULL;
	}
	String::Utf8Value utf(result->ToString());
	return env->NewStringUTF(*utf);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeVoidFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	setupJNIContext(v8RuntimeHandle, env, v8);
	const char* functionName = env -> GetStringUTFChars(jfunctionName, NULL);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	int size = 0;
	Handle<Value>* args = NULL;
	if ( parameterHandle >= 0 ) {
		Handle<v8::Object> parameters = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parameterHandle]);
		size = Array::Cast(*parameters)->Length();
		args = new Handle<Value> [size];
		for (int i = 0; i < size; i++) {
			args[i] = parameters->Get(i);
		}
	}
	Handle<v8::Value> value = parentObject->Get(v8::String::NewFromUtf8(isolate, functionName));
	Handle<v8::Function> func = v8::Handle<v8::Function>::Cast(value);
	func->Call(parentObject, size, args);
	env->ReleaseStringUTFChars(jfunctionName, functionName);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addUndefined
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	global->Set(v8Key, v8::Undefined(isolate));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__IILjava_lang_String_2I
  (JNIEnv * env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jint value) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Local<Value> v8Value = v8::Int32::New(isolate, value);
	global->Set(v8Key,  v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__IILjava_lang_String_2D
  (JNIEnv * env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jdouble value) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Local<Value> v8Value = v8::Number::New(isolate, value);
	global->Set( v8Key,  v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__IILjava_lang_String_2Ljava_lang_String_2
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jstring value) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Local<String> v8Value = String::NewFromUtf8(isolate, env -> GetStringUTFChars(value, NULL));
	global->Set( v8Key,  v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__IILjava_lang_String_2Z
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jboolean value) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Local<Value> v8Value = v8::Boolean::New(isolate, value);
	global->Set( v8Key,  v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addObject
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jint valueHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Local<Value> v8Value = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[valueHandle]);
	global->Set( v8Key,  v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArray
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jint valueHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Local<Value> v8Value = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[valueHandle]);
	global->Set( v8Key,  v8Value);
}


JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getInteger
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = v8::String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Handle<v8::Value> v8Value = global->Get(v8Key);
	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsInt32()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return v8Value->Int32Value();
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1getDouble
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = v8::String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Handle<v8::Value> v8Value = global->Get(v8Key);
	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsNumber()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return v8Value->NumberValue();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1getString
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = v8::String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Handle<v8::Value> v8Value = global->Get(v8Key);
	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsString()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	String::Utf8Value utf(v8Value->ToString());
	return env->NewStringUTF(*utf);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1getBoolean
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Local<String> v8Key = v8::String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Handle<v8::Value> v8Value = global->Get(v8Key);
	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsBoolean()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return v8Value->BooleanValue();
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetSize
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	return Array::Cast(*array)->Length();
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetInteger
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);

	Handle<Value> v8Value = array->Get(index);
	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsInt32()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return v8Value->Int32Value();
}

JNIEXPORT jintArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetInts
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint start, jint length) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);

	jintArray result = env->NewIntArray(length);
	jint fill[length];
	for (int i = start; i < start+length; i++) {
		Handle<Value> v8Value = array->Get(i);
		if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsInt32()) {
			throwResultUndefinedException(env, "");
			return NULL;
		}
		fill[i-start] = v8Value->Int32Value();
	}
	(env)->SetIntArrayRegion(result, 0, length, fill);
	return result;
}

JNIEXPORT jdoubleArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetDoubles
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint start, jint length) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);

	jdoubleArray result = env->NewDoubleArray(length);
	jdouble fill[length];
	for (int i = start; i < start+length; i++) {
		Handle<Value> v8Value = array->Get(i);
		if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsNumber()) {
			throwResultUndefinedException(env, "");
			return NULL;
		}
		fill[i-start] = v8Value->NumberValue();
	}
	(env)->SetDoubleArrayRegion(result, 0, length, fill);
	return result;
}

JNIEXPORT jbooleanArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetBooleans
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint start, jint length) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);

	jbooleanArray result = env->NewBooleanArray(length);
	jboolean fill[length];
	for (int i = start; i < start+length; i++) {
		Handle<Value> v8Value = array->Get(i);
		if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsBoolean()) {
			throwResultUndefinedException(env, "");
			return NULL;
		}
		fill[i-start] = v8Value->BooleanValue();
	}
	(env)->SetBooleanArrayRegion(result, 0, length, fill);
	return result;
}

JNIEXPORT jobjectArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetStrings
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint start, jint length) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);

	jobjectArray result = env->NewObjectArray(length, stringCls, NULL);
	for (int i = start; i < start+length; i++) {
		Handle<Value> v8Value = array->Get(i);
		if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsString()) {
			throwResultUndefinedException(env, "");
			return NULL;
		}
		String::Utf8Value utf(v8Value->ToString());
		env->SetObjectArrayElement(result, i-start, env->NewStringUTF(*utf));
	}
	return result;
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1arrayGetBoolean
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);

	Handle<Value> v8Value = array->Get(index);
	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsBoolean()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return v8Value->BooleanValue();
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1arrayGetDouble
	(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);

	Handle<Value> v8Value = array->Get(index);
	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsNumber()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return v8Value->NumberValue();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1arrayGetString
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);

	Handle<Value> v8Value = array->Get(index);
	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsString()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	String::Utf8Value utf(v8Value->ToString());
	return env->NewStringUTF(*utf);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1arrayGetObject
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index, jint resultHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Handle<Value> v8Value = array->Get(index);

	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsObject()) {
		throwResultUndefinedException(env, "");
		return;
	}
	createPersistentContainer(v8Isolates[v8RuntimeHandle], resultHandle);
	Handle<Object> obj = v8Value->ToObject();
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, obj);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1arrayGetArray
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index, jint resultHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Handle<Value> v8Value = array->Get(index);

	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsArray()) {
		throwResultUndefinedException(env, "");
		return;
	}
	createPersistentContainer(v8Isolates[v8RuntimeHandle], resultHandle);
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, v8Value->ToObject());
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayUndefinedItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	int index = Array::Cast(*array)->Length();
	array->Set(index, v8::Undefined(isolate));
}


JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayIntItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint value) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Local<Value> v8Value = v8::Int32::New(isolate, value);
	int index = Array::Cast(*array)->Length();
	array->Set(index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayDoubleItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jdouble value) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Local<Value> v8Value = v8::Number::New(isolate, value);
	int index = Array::Cast(*array)->Length();
	array->Set(index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayBooleanItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jboolean value) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Local<Value> v8Value = v8::Boolean::New(isolate, value);
	int index = Array::Cast(*array)->Length();
	array->Set(index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayStringItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jstring value) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	int index = Array::Cast(*array)->Length();
	const char* utfString = env -> GetStringUTFChars(value, NULL);
	Local<String> v8Value = String::NewFromUtf8(isolate, utfString);
	array->Set(index, v8Value);
	env->ReleaseStringUTFChars(value, utfString);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayArrayItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint valueHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);

	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	int index = Array::Cast(*array)->Length();
	Local<Value> v8Value = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[valueHandle]);
	array->Set(index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayObjectItem
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint valueHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);

	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	int index = Array::Cast(*array)->Length();
	Local<Value> v8Value = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[valueHandle]);
	array->Set(index, v8Value);
}

int getType(Handle<Value> v8Value) {
	if (v8Value.IsEmpty() || v8Value->IsUndefined()) {
		return com_eclipsesource_v8_V8_UNDEFINED;
	} else if ( v8Value->IsInt32() ) {
		return com_eclipsesource_v8_V8_INTEGER;
	} else if ( v8Value->IsNumber() ) {
		return com_eclipsesource_v8_V8_DOUBLE;
	} else if (v8Value->IsBoolean() ) {
		return com_eclipsesource_v8_V8_BOOLEAN;
	} else if (v8Value->IsString() ) {
		return com_eclipsesource_v8_V8_STRING;
	} else if ( v8Value->IsArray() ) {
		return com_eclipsesource_v8_V8_V8_ARRAY;
	} else if ( v8Value->IsObject() ) {
		return com_eclipsesource_v8_V8_V8_OBJECT;
	}
	return -1;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getType__IILjava_lang_String_2
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	const char* utf_string = env -> GetStringUTFChars(key, NULL);

	Local<String> v8Key = v8::String::NewFromUtf8(isolate, utf_string);
	Handle<v8::Value> v8Value = global->Get(v8Key);
	env->ReleaseStringUTFChars(key, utf_string);
	int type = getType(v8Value);
	if ( type < 0 ) {
		throwResultUndefinedException(env, "");
	}
	return type;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getType__III
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jint index) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Handle<Value> v8Value = array->Get(index);
	int type = getType(v8Value);
	if ( type < 0 ) {
		throwResultUndefinedException(env, "");
	}
	return type;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getType__IIII
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint start, jint length) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);

	int result = -1;
	for (int i = start; i < start+length; i++) {
		Handle<Value> v8Value = array->Get(i);
		int type = getType(v8Value);
		if ( result >= 0 && result != type ) {
			throwResultUndefinedException(env, "");
			return -1;
		} else if ( type < 0 ) {
			throwResultUndefinedException(env, "");
			return -1;
		}
		result = type;
	}
	if ( result < 0 ) {
		throwResultUndefinedException(env, "");
	}
	return result;
}

class MethodDescriptor {
public:
	int methodID;
	int v8RuntimeHandle;
};

void release(JNIEnv* env, jobject object) {
	jclass cls = env->FindClass("com/eclipsesource/v8/V8Object");
	jmethodID release = env->GetMethodID(cls, "release", "()V");
	env->CallVoidMethod(object, release);
	env->DeleteLocalRef(cls);
}

void releaseArray(JNIEnv* env, jobject object) {
	jclass cls = env->FindClass("com/eclipsesource/v8/V8Array");
	jmethodID release = env->GetMethodID(cls, "release", "()V");
	env->CallVoidMethod(object, release);
	env->DeleteLocalRef(cls);
}

int getHandle(JNIEnv* env, jobject object) {
	jclass cls = env->FindClass("com/eclipsesource/v8/V8Object");
	jmethodID getHandle = env->GetMethodID(cls, "getHandle", "()I");
	jint handle = env->CallIntMethod(object, getHandle);
	env->DeleteLocalRef(cls);
	return handle;
}

int getArrayHandle(JNIEnv* env, jobject object) {
	jclass cls = env->FindClass("com/eclipsesource/v8/V8Array");
	jmethodID getHandle = env->GetMethodID(cls, "getHandle", "()I");
	jint handle = env->CallIntMethod(object, getHandle);
	env->DeleteLocalRef(cls);
	return handle;
}

jobject createParameterArray(JNIEnv* env, int v8RuntimeHandle, jobject v8, int size, const v8::FunctionCallbackInfo<v8::Value>& args) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	jclass cls = env->FindClass("com/eclipsesource/v8/V8Array");
	jmethodID methodID = env->GetMethodID(cls, "<init>", "(Lcom/eclipsesource/v8/V8;)V");
	jmethodID getHandle = env->GetMethodID(cls, "getHandle", "()I");
	jobject result = env->NewObject(cls, methodID, v8);
	jint parameterHandle = env->CallIntMethod(result, getHandle);

	Handle<v8::Object> parameters = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parameterHandle]);

	for ( int i = 0; i < size; i++) {
		parameters->Set(i, args[i]);
	}

	env->DeleteLocalRef(cls);
	return result;
}

void voidCallback(const v8::FunctionCallbackInfo<v8::Value>& args) {
	int size = args.Length();
	Local<External> data = Local<External>::Cast(args.Data());
	void *methodDescriptorPtr = data->Value();
	MethodDescriptor* md = static_cast<MethodDescriptor*>(methodDescriptorPtr);

	jobject v8 = v8Isolates[md->v8RuntimeHandle]->v8;
	JNIEnv* env = v8Isolates[md->v8RuntimeHandle]->env;

	jobject parameters = createParameterArray(env, md->v8RuntimeHandle, v8, size, args);

	jclass cls = (env)->FindClass("com/eclipsesource/v8/V8");
	jmethodID callVoidMethod = (env)->GetMethodID(cls, "callVoidJavaMethod", "(ILcom/eclipsesource/v8/V8Array;)V");

	env->CallVoidMethod(v8, callVoidMethod, md->methodID, parameters);
	if ( env -> ExceptionCheck() ) {
		Isolate* isolate = getIsolate(env, md->v8RuntimeHandle);
		isolate->ThrowException(v8::String::NewFromUtf8(isolate, "Java Exception Caught"));
	}

	jclass arrayCls = env->FindClass("com/eclipsesource/v8/V8Array");
	jmethodID release = env->GetMethodID(arrayCls, "release", "()V");
	env->CallVoidMethod(parameters, release);

	env->DeleteLocalRef(parameters);
	env->DeleteLocalRef(arrayCls);
	env->DeleteLocalRef(cls);
}

int getReturnType(JNIEnv* env, jobject &object) {
	jclass integerCls = (env)->FindClass("java/lang/Integer");
	jclass doubleCls = (env)->FindClass("java/lang/Double");
	jclass booleanCls = (env)->FindClass("java/lang/Boolean");
	jclass v8ObjectCls = (env)->FindClass("com/eclipsesource/v8/V8Object");
	jclass v8ArrayCls = (env)->FindClass("com/eclipsesource/v8/V8Array");
	int result = com_eclipsesource_v8_V8_VOID;
	if ( env->IsInstanceOf(object,integerCls) ) {
		result = com_eclipsesource_v8_V8_INTEGER;
	} else if ( env->IsInstanceOf(object, doubleCls)) {
		result = com_eclipsesource_v8_V8_DOUBLE;
	} else if ( env->IsInstanceOf(object, booleanCls)) {
		result = com_eclipsesource_v8_V8_BOOLEAN;
	} else if ( env->IsInstanceOf(object, stringCls)) {
		result = com_eclipsesource_v8_V8_STRING;
	} else if ( env->IsInstanceOf(object,v8ArrayCls)) {
		result = com_eclipsesource_v8_V8_V8_ARRAY;
	} else if ( env->IsInstanceOf(object,v8ObjectCls)) {
		result = com_eclipsesource_v8_V8_V8_OBJECT;
	}
	env->DeleteLocalRef(integerCls);
	env->DeleteLocalRef(doubleCls);
	env->DeleteLocalRef(booleanCls);
	env->DeleteLocalRef(v8ObjectCls);
	env->DeleteLocalRef(v8ArrayCls);
	return result;
}

int getInteger(JNIEnv* env, jobject &object) {
	jclass integerCls = (env)->FindClass("java/lang/Integer");
	jmethodID intValueMethod = env->GetMethodID(integerCls, "intValue", "()I");
	int result = env->CallIntMethod(object, intValueMethod);
	env->DeleteLocalRef(integerCls);
	return result;
}

bool getBoolean(JNIEnv* env, jobject &object) {
	jclass booleanCls = (env)->FindClass("java/lang/Boolean");
	jmethodID boolValueMethod = env->GetMethodID(booleanCls, "booleanValue", "()Z");
	bool result = env->CallBooleanMethod(object, boolValueMethod);
	env->DeleteLocalRef(booleanCls);
	return result;
}

double getDouble(JNIEnv* env, jobject &object) {
	jclass doubleCls = (env)->FindClass("java/lang/Double");
	jmethodID doubleValueMethod = env->GetMethodID(doubleCls, "doubleValue", "()D");
	double result = env->CallDoubleMethod(object, doubleValueMethod);
	env->DeleteLocalRef(doubleCls);
	return result;
}

void unknownCallback(const v8::FunctionCallbackInfo<v8::Value>& args) {
	int size = args.Length();
	Local<External> data = Local<External>::Cast(args.Data());
	void *methodDescriptorPtr = data->Value();
	MethodDescriptor* md = static_cast<MethodDescriptor*>(methodDescriptorPtr);

	jobject v8 = v8Isolates[md->v8RuntimeHandle]->v8;
	Isolate* isolate = v8Isolates[md->v8RuntimeHandle]->isolate;
	JNIEnv* env = v8Isolates[md->v8RuntimeHandle]->env;

	jobject parameters = createParameterArray(env, md->v8RuntimeHandle, v8, size, args);

	jclass cls = (env)->FindClass("com/eclipsesource/v8/V8");
	jmethodID callObjectMethod = (env)->GetMethodID(cls, "callObjectJavaMethod", "(ILcom/eclipsesource/v8/V8Array;)Ljava/lang/Object;");

	jobject resultObject = env->CallObjectMethod(v8, callObjectMethod, md->methodID, parameters);

	if ( env -> ExceptionCheck() ) {
		Isolate* isolate = getIsolate(env, md->v8RuntimeHandle);
		isolate->ThrowException(v8::String::NewFromUtf8(isolate, "Java Exception Caught"));
	} else if ( resultObject == NULL ) {
		args.GetReturnValue().SetUndefined();
	} else {
		int returnType = getReturnType(env, resultObject);
		if ( returnType == com_eclipsesource_v8_V8_INTEGER) {
			args.GetReturnValue().Set(getInteger(env, resultObject));
		} else if ( returnType == com_eclipsesource_v8_V8_BOOLEAN ) {
			args.GetReturnValue().Set(getBoolean(env, resultObject));
		} else if ( returnType == com_eclipsesource_v8_V8_DOUBLE ) {
			args.GetReturnValue().Set(getDouble(env, resultObject));
		} else if ( returnType == com_eclipsesource_v8_V8_STRING ) {
			const char* utf_string = env -> GetStringUTFChars((jstring)resultObject, NULL);
			Local<Value> result = String::NewFromUtf8(v8Isolates[md->v8RuntimeHandle]->isolate, utf_string);
			args.GetReturnValue().Set(result);
			env->ReleaseStringUTFChars((jstring) resultObject, utf_string);
		} else if ( returnType == com_eclipsesource_v8_V8_V8_ARRAY ) {
			int resultHandle = getArrayHandle(env, resultObject);
			Handle<v8::Object> result = Local<Object>::New(isolate, *v8Isolates[md->v8RuntimeHandle]->objects[resultHandle]);
			releaseArray(env, resultObject);
			args.GetReturnValue().Set(result);
		} else if ( returnType == com_eclipsesource_v8_V8_V8_OBJECT ) {
			int resultHandle = getHandle(env, resultObject);
			Handle<v8::Object> result = Local<Object>::New(isolate, *v8Isolates[md->v8RuntimeHandle]->objects[resultHandle]);
			release(env, resultObject);
			args.GetReturnValue().Set(result);
		} else {
			args.GetReturnValue().SetUndefined();
		}
	}

	jclass arrayCls = env->FindClass("com/eclipsesource/v8/V8Array");
	jmethodID release = env->GetMethodID(arrayCls, "release", "()V");
	env->CallVoidMethod(parameters, release);

	env->DeleteLocalRef(parameters);
	env->DeleteLocalRef(arrayCls);
	env->DeleteLocalRef(cls);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1registerJavaMethod
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring functionName, jint methodID, jboolean voidMethod) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	FunctionCallback callback = voidCallback;
	if ( voidMethod ) {
		callback = voidCallback;
	} else {
		callback = unknownCallback;
	}

	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	const char* utf_string = env -> GetStringUTFChars(functionName, NULL);
	MethodDescriptor* md = new MethodDescriptor();
	md ->  methodID = methodID;
	md -> v8RuntimeHandle = v8RuntimeHandle;
	global->Set(String::NewFromUtf8(isolate, utf_string), Function::New(isolate, callback, External::New(isolate, md)));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1setPrototype
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jint prototypeHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Handle<v8::Object> prototype = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[prototypeHandle]);
	global->SetPrototype(prototype);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1equals
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jint thatHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return false;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Handle<v8::Object> that = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[thatHandle]);
	return global->Equals(that);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1strictEquals
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jint thatHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return false;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Handle<v8::Object> that = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[thatHandle]);
	return global->StrictEquals(that);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1sameValue
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jint thatHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return false;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Handle<v8::Object> that = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[thatHandle]);
	return global->SameValue(that);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1identityHash
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	return global->GetIdentityHash();
}

void setupJNIContext(int v8RuntimeHandle, JNIEnv *env, jobject v8 ) {
	v8Isolates[v8RuntimeHandle]->env = env;
	v8Isolates[v8RuntimeHandle]->v8 = v8;
}

Isolate* getIsolate(JNIEnv *env, int handle) {
	if ( v8Isolates.find(handle) == v8Isolates.end() ) {
		throwError(env, "V8 isolate not found.");
		return NULL;
	}
	return v8Isolates[handle]->isolate;
}

void throwResultUndefinedException( JNIEnv *env, const char *message ) {
    jclass exClass;
    const char *className = "com/eclipsesource/v8/V8ResultUndefined";

    exClass = (env)->FindClass(className);
    (env)->ThrowNew(exClass, message );
}

void throwParseException(JNIEnv *env, const char* fileName, int lineNumber, const char* message,
		const char* sourceLine, int startColumn, int endColumn) {
    const char *className = "com/eclipsesource/v8/V8ParseException";
    jclass exClass = (env)->FindClass(className);
    jmethodID methodID = env->GetMethodID(exClass, "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;II)V");
    jstring jfileName = env->NewStringUTF(fileName);
    jstring jmessage = env->NewStringUTF(message);
    jstring jsourceLine = env->NewStringUTF(sourceLine);
    jthrowable result = (jthrowable) env->NewObject(exClass, methodID, jfileName, lineNumber, jmessage, jsourceLine, startColumn, endColumn);
    (env)->Throw( result );
}

void throwParseException( JNIEnv *env, Isolate* isolate, TryCatch* tryCatch) {
	v8::HandleScope handle_scope(isolate);
	v8::String::Utf8Value exception(tryCatch->Exception());
	const char* exceptionString = ToCString(exception);
	v8::Handle<v8::Message> message = tryCatch->Message();
	if (message.IsEmpty()) {
		throwExecutionException(env, exceptionString);
	} else {
	    v8::String::Utf8Value filename(message->GetScriptResourceName());
	    int lineNumber = message->GetLineNumber();
	    v8::String::Utf8Value sourceline(message->GetSourceLine());
	    int start = message->GetStartColumn();
	    int end = message->GetEndColumn();
	    const char* filenameString = ToCString(filename);
	    const char* sourcelineString = ToCString(sourceline);
	    throwParseException(env, filenameString, lineNumber, exceptionString, sourcelineString, start, end);
	}
}

void throwExecutionException( JNIEnv *env, const char *message ) {
    const char *className = "com/eclipsesource/v8/V8ExecutionException";

    jclass exClass = (env)->FindClass(className);
    (env)->ThrowNew(exClass, message );
}

void throwError( JNIEnv *env, const char *message ) {
    jclass exClass;
    const char *className = "java/lang/Error";

    exClass = (env)->FindClass(className);
    (env)->ThrowNew(exClass, message );
}
