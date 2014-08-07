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
    std::map <int, Persistent<Object>* > objects;
};

std::map <int, V8Runtime*> v8Isolates;

void throwError( JNIEnv *env, const char *message );
void throwExecutionException( JNIEnv *env, const char *message );
void throwResultUndefinedException( JNIEnv *env, const char *message );
Isolate* getIsolate(JNIEnv *env, int handle);

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1createIsolate
  (JNIEnv *, jobject, jint handle) {
	v8Isolates[handle] = new V8Runtime();
	v8Isolates[handle]->isolate = Isolate::New();
	v8Isolates[handle]->isolate_scope = new Isolate::Scope(v8Isolates[handle]->isolate);
	HandleScope handle_scope(v8Isolates[handle]->isolate);
	v8Isolates[handle]->globalObjectTemplate.Reset(v8Isolates[handle]->isolate, ObjectTemplate::New(v8Isolates[handle]->isolate));
	Handle<Context> context = Context::New(v8Isolates[handle]->isolate, NULL, Local<ObjectTemplate>::New(v8Isolates[handle]->isolate, v8Isolates[handle]->globalObjectTemplate));
	v8Isolates[handle]->context_.Reset(v8Isolates[handle]->isolate, context);
	v8Isolates[handle]->objects[0] = new Persistent<Object>;
	v8Isolates[handle]->objects[0]->Reset(v8Isolates[handle]->isolate, context->Global());
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
	v8Isolates[v8RuntimeHandle]->objects[objectHandle] = new Persistent<Object>;
	v8Isolates[v8RuntimeHandle]->objects[objectHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, obj);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1getObject
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint parentHandle, jstring objectKey, jint resultHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	const char* utf_string = env -> GetStringUTFChars(objectKey, NULL);
	Local<String> v8Key = v8::String::NewFromUtf8(v8::Isolate::GetCurrent(), utf_string);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parentHandle]);

	Handle<Object> obj = parentObject->Get(v8Key)->ToObject();
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, obj);
	env->ReleaseStringUTFChars(objectKey, utf_string);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1release
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle) {
	if ( v8Isolates.count(v8RuntimeHandle) == 0 ) {
		return;
	}
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	HandleScope handle_scope(isolate);
	v8Isolates[v8RuntimeHandle]->objects[objectHandle]->Reset();
	delete(v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	v8Isolates[v8RuntimeHandle]->objects.erase(objectHandle);
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
  (JNIEnv *env, jobject, jint handle) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return NULL;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = context->Global();
	Local<Array> properties = global->GetPropertyNames();

	int size = properties->Length();
	jclass jStringObject = (env)->FindClass("java/lang/String");
	jobjectArray keys = (env)->NewObjectArray(size, jStringObject, NULL);
	for ( int i = 0; i < size; i++ ) {
		jobject key = (env)->NewStringUTF( *String::Utf8Value( properties->Get(i)->ToString() ) );
		(env)->SetObjectArrayElement(keys, i, key);
	}
	return keys;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeVoidScript
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

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1executeDoubleScript
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

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1executeBooleanScript
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

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1executeStringScript
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

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1executeIntScript
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

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeObjectScript
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jstring jjstring, jint resultHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
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

	if (result.IsEmpty() || result->IsUndefined() || !result->IsObject()) {
		throwResultUndefinedException(env, "");
		return;
	}
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, result->ToObject());
	env->ReleaseStringUTFChars(jjstring, js);
	return;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeObjectFunction
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jobject, jint resultHandle) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	if ( isolate == NULL ) {
		return;
	}
	const char* functionName = env -> GetStringUTFChars(jfunctionName, NULL);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);

	Handle<v8::Value> value = parentObject->Get(v8::String::NewFromUtf8(v8::Isolate::GetCurrent(), functionName));
	Handle<v8::Function> func = v8::Handle<v8::Function>::Cast(value);
	Handle<Value> result = func->Call(parentObject, 0, NULL);
	if (result.IsEmpty() || result->IsUndefined() || !result->IsObject()) {
		throwResultUndefinedException(env, "");
		return;
	}
	v8Isolates[v8RuntimeHandle]->objects[resultHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, result->ToObject());
	env->ReleaseStringUTFChars(jfunctionName, functionName);
	return;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1executeIntFunction
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

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1executeDoubleFunction
  (JNIEnv *env, jobject, jint handle, jstring jfunctionName, jobject) {
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
	if (result.IsEmpty() || result->IsUndefined() || !result->IsNumber()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->NumberValue();
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1executeBooleanFunction
  (JNIEnv *env, jobject, jint handle, jstring jfunctionName, jobject) {
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
	if (result.IsEmpty() || result->IsUndefined() || !result->IsBoolean()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return result->BooleanValue();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1executeStringFunction
  (JNIEnv *env, jobject, jint handle, jstring jfunctionName, jobject) {
	Isolate* isolate = getIsolate(env, handle);
	if ( isolate == NULL ) {
		return NULL;
	}
	const char* functionName = env -> GetStringUTFChars(jfunctionName, NULL);
	HandleScope handle_scope(isolate);
	v8::Local<v8::Context> context = v8::Local<v8::Context>::New(isolate,v8Isolates[handle]->context_);
	Context::Scope context_scope(context);
	Handle<v8::Object> global = context->Global();
	Handle<v8::Value> value = global->Get(v8::String::NewFromUtf8(v8::Isolate::GetCurrent(), functionName));
	Handle<v8::Function> func = v8::Handle<v8::Function>::Cast(value);
	Handle<Value> result = func->Call(global, 0, NULL);
	if (result.IsEmpty() || result->IsUndefined() || !result->IsString()) {
		throwResultUndefinedException(env, "");
		return NULL;
	}
	String::Utf8Value utf(result->ToString());
	return env->NewStringUTF(*utf);
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

	Local<String> v8Key = v8::String::NewFromUtf8(v8::Isolate::GetCurrent(), env -> GetStringUTFChars(key, NULL));
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

	Local<String> v8Key = v8::String::NewFromUtf8(v8::Isolate::GetCurrent(), env -> GetStringUTFChars(key, NULL));
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

	Local<String> v8Key = v8::String::NewFromUtf8(v8::Isolate::GetCurrent(), env -> GetStringUTFChars(key, NULL));
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

	Local<String> v8Key = v8::String::NewFromUtf8(v8::Isolate::GetCurrent(), env -> GetStringUTFChars(key, NULL));
	Handle<v8::Value> v8Value = global->Get(v8Key);
	if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsBoolean()) {
		throwResultUndefinedException(env, "");
		return 0;
	}
	return v8Value->BooleanValue();
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
