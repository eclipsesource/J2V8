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
    Persistent<ObjectTemplate> globalObjectTemplate;
    Persistent<Context> context_;
};

std::map <int, V8Runtime*> v8Isolates;

void throwError( JNIEnv *env, const char *message );
void throwExecutionException( JNIEnv *env, const char *message );
void throwResultUndefinedException( JNIEnv *env, const char *message );
Isolate* getIsolate(JNIEnv *env, int handle);

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1createIsolate
  (JNIEnv *, jobject, jint handle) {
	v8Isolates[handle] = new V8Runtime();
	v8Isolates[handle]->isolate = Isolate::New();
	v8Isolates[handle]->isolate_scope = new Isolate::Scope(v8Isolates[handle]->isolate);
	HandleScope handle_scope(v8Isolates[handle]->isolate);
	v8Isolates[handle]->globalObjectTemplate.Reset(v8Isolates[handle]->isolate, ObjectTemplate::New(v8Isolates[handle]->isolate));
	Handle<Context> context = Context::New(v8Isolates[handle]->isolate, NULL, Local<ObjectTemplate>::New(v8Isolates[handle]->isolate, v8Isolates[handle]->globalObjectTemplate));
	v8Isolates[handle]->context_.Reset(v8Isolates[handle]->isolate, context);
}


JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1release
  (JNIEnv *env, jobject obj, jint handle) {
	if ( v8Isolates.count(handle) == 0 ) {
		return;
	}
	Isolate* isolate = getIsolate(env, handle);
	HandleScope handle_scope(isolate);
	v8Isolates[handle]->context_.Reset();
	delete(v8Isolates[handle]->isolate_scope);
	v8Isolates[handle]->isolate->Dispose();
	delete(v8Isolates[handle]);
	v8Isolates.erase(handle);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1executeVoidScript
  (JNIEnv * env, jobject, jint handle, jstring jjstring) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source);
	if ( tryCatch.HasCaught() ) {
		throwExecutionException(env, "");
		return;
	}
	Local<Value> result = script->Run();
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8Impl__1executeDoubleScript
  (JNIEnv * env, jobject, jint handle, jstring jjstring) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source);
	if ( tryCatch.HasCaught() ) {
		throwExecutionException(env, "");
		return 0;
	}
	Local<Value> result = script->Run();

	if (result.IsEmpty() || result->IsUndefined() || !result->IsNumber() ) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->NumberValue();
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8Impl__1executeBooleanScript
  (JNIEnv *env, jobject, jint handle, jstring jjstring) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source);
	if ( tryCatch.HasCaught() ) {
		throwExecutionException(env, "");
		return 0;
	}
	Local<Value> result = script->Run();

	if (result.IsEmpty() || result->IsUndefined() || !result->IsBoolean() ) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->BooleanValue();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8Impl__1executeStringScript
  (JNIEnv *env, jobject, jint handle, jstring jjstring) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return NULL;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source);
	if ( tryCatch.HasCaught() ) {
		throwExecutionException(env, "");
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

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8Impl__1executeIntScript
  (JNIEnv * env, jobject, jint handle, jstring jjstring) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return 0;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	const char* js = env -> GetStringUTFChars(jjstring, NULL);
	Local<String> source = String::NewFromUtf8(isolate, js);

	TryCatch tryCatch;
	Local<Script> script = Script::Compile(source);
	if ( tryCatch.HasCaught() ) {
		throwExecutionException(env, "");
		return 0;
	}
	Local<Value> result = script->Run();

	if (result.IsEmpty() || result->IsUndefined() || !result->IsInt32()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->Int32Value();
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8Impl__1executeIntFunction
  (JNIEnv * env, jobject, jint handle, jstring jfunctionName, jobject) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return 0;
	}
	const char* functionName = env -> GetStringUTFChars(jfunctionName, NULL);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = context->Global();
	Handle<v8::Value> value = global->Get(v8::String::NewFromUtf8(v8::Isolate::GetCurrent(), functionName));
	Handle<v8::Function> func = v8::Handle<v8::Function>::Cast(value);
	Handle<Value> result = func->Call(global, 0, NULL);
	if (result.IsEmpty() || result->IsUndefined() || !result->IsInt32()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->Int32Value();
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1add__ILjava_lang_String_2I
  (JNIEnv * env, jobject, jint handle, jstring key, jint value) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = context->Global();

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Local<Value> v8Value = v8::Int32::New(isolate, value);
	global->Set( v8Key,  v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1add__ILjava_lang_String_2D
  (JNIEnv * env, jobject, jint handle, jstring key, jdouble value) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = context->Global();

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Local<Value> v8Value = v8::Number::New(isolate, value);
	global->Set( v8Key,  v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1add__ILjava_lang_String_2Ljava_lang_String_2
  (JNIEnv *env, jobject, jint handle, jstring key, jstring value) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = context->Global();

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Local<String> v8Value = String::NewFromUtf8(isolate, env -> GetStringUTFChars(value, NULL));
	global->Set( v8Key,  v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8Impl__1add__ILjava_lang_String_2Z
  (JNIEnv *env, jobject, jint handle, jstring key, jboolean value) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = context->Global();

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	Local<Value> v8Value = v8::Boolean::New(isolate, value);
	global->Set( v8Key,  v8Value);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8Impl__1contains
  (JNIEnv *env, jobject, jint handle, jstring key) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return false;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = context->Global();

	Local<String> v8Key = String::NewFromUtf8(isolate, env -> GetStringUTFChars(key, NULL));
	return global->Has( v8Key );
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

void throwExecutionException( JNIEnv *env, const char *message ) {
    jclass exClass;
    const char *className = "com/eclipsesource/v8/V8ExecutionException";

    exClass = (env)->FindClass(className);
    (env)->ThrowNew(exClass, message );
}

void throwError( JNIEnv *env, const char *message ) {
    jclass exClass;
    const char *className = "java/lang/Error";

    exClass = (env)->FindClass(className);
    (env)->ThrowNew(exClass, message );
}
