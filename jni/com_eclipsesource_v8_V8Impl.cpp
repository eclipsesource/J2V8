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
    Persistent<Context> context_;
    std::map <int, Persistent<Object>* > objects;
    jobject v8;
};

const char* ToCString(const String::Utf8Value& value) {
  return *value ? *value : "<string conversion failed>";
}

std::map <int, V8Runtime*> v8Isolates;
JavaVM* jvm = NULL;
jclass v8cls = NULL;
jclass v8ObjectCls = NULL;
jclass v8ArrayCls = NULL;
jclass v8ResultsUndefinedCls = NULL;
jclass v8ScriptCompilationCls = NULL;
jclass v8ScriptExecutionException = NULL;
jclass v8RuntimeException = NULL;
jclass stringCls = NULL;
jclass integerCls = NULL;
jclass doubleCls = NULL;
jclass booleanCls = NULL;
jclass errorCls = NULL;

void throwParseException( JNIEnv *env, Isolate* isolate, TryCatch* tryCatch);
void throwExecutionException( JNIEnv *env, Isolate* isolate, TryCatch* tryCatch);
void throwError( JNIEnv *env, const char *message );
void throwV8RuntimeException( JNIEnv *env, const char *message );
void throwResultUndefinedException( JNIEnv *env, const char *message );
Isolate* getIsolate(JNIEnv *env, int handle);
int getType(Handle<Value> v8Value);

#define SETUP(env, v8RuntimeHandle, errorReturnResult) getIsolate(env, v8RuntimeHandle);\
		if ( isolate == NULL ) {\
			return errorReturnResult;\
		}\
		Isolate::Scope isolateScope(isolate);\
		HandleScope handle_scope(isolate);\
		Local<Context> context = Local<Context>::New(isolate,v8Isolates[v8RuntimeHandle]->context_);\
		Context::Scope context_scope(context);
#define ASSERT_IS_NUMBER(v8Value) \
		if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsNumber()) {\
			throwResultUndefinedException(env, "");\
			return 0;\
		}
#define ASSERT_IS_INTEGER(v8Value)\
		if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsInt32()) {\
			throwResultUndefinedException(env, "");\
			return 0;\
		}
#define ASSERT_IS_STRING(v8Value)\
		if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsString()) {\
			throwResultUndefinedException(env, "");\
			return 0;\
		}
#define ASSERT_IS_OBJECT(v8Value)\
		if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsObject()) {\
			throwResultUndefinedException(env, "");\
			return NULL;\
		}
#define ASSERT_IS_BOOLEAN(v8Value)\
		if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsBoolean() ) {\
			throwResultUndefinedException(env, "");\
			return 0;\
		}
#define ASSERT_IS_ARRAY(v8Value)\
		if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsArray()) {\
			throwResultUndefinedException(env, "");\
			return NULL;\
		}

void createPersistentContainer(V8Runtime* runtime, int handle) {
	if ( runtime->objects.find(handle) == runtime->objects.end() ) {
		runtime->objects[handle] = new Persistent<Object>;
	}
}

Local<String> createV8String(JNIEnv *env, Isolate *isolate, jstring string) {
	const char* utfString = env -> GetStringUTFChars(string, NULL);
	Local<String> result = String::NewFromUtf8(isolate, utfString);
	env->ReleaseStringUTFChars(string, utfString);
	return result;
}

Handle<Value> getValueWithKey(JNIEnv* env, Isolate* isolate, jint v8RuntimeHandle, jint objectHandle, jstring key) {
		Handle<Object> object = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
		Local<String> v8Key = createV8String(env, isolate, key);
		return object->Get(v8Key);
}

void addValueWithKey(JNIEnv* env, Isolate* isolate, jint v8RuntimeHandle, jint objectHandle, jstring key, Handle<Value> value) {
	Handle<Object> object = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	const char* utfString_key = env -> GetStringUTFChars(key, NULL);
	Local<String> v8Key = String::NewFromUtf8(isolate, utfString_key);
	object->Set(v8Key,  value);
	env->ReleaseStringUTFChars(key, utfString_key);
}

void getJNIEnv(JNIEnv*& env) {
	int getEnvStat = jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED) {
		if (jvm->AttachCurrentThread((void **) &env, NULL) != 0) {
			std::cout << "Failed to attach" << std::endl;
		}
	} else if (getEnvStat == JNI_OK) {
	} else if (getEnvStat == JNI_EVERSION) {
		std::cout << "GetEnv: version not supported" << std::endl;
	}
}

jobject instantiateV8Object(JNIEnv *env, Handle<Value> value, int v8RuntimeHandle, jclass clazz) {
	jobject v8 = v8Isolates[v8RuntimeHandle]->v8;
	jmethodID methodID = env->GetMethodID(clazz, "<init>", "(Lcom/eclipsesource/v8/V8;)V");
	jmethodID getHandle = env->GetMethodID(clazz, "getHandle", "()I");
	jobject v8Object = env->NewObject(clazz, methodID, v8);
	jint handle = env->CallIntMethod(v8Object, getHandle);
	Handle<Object> obj = value->ToObject();
	createPersistentContainer(v8Isolates[v8RuntimeHandle], handle);
	v8Isolates[v8RuntimeHandle]->objects[handle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, obj);
	return v8Object;
}

jobject createV8Object(JNIEnv *env, Handle<Value> value, int v8RuntimeHandle ) {
	if ( value->IsArray() ) {
		return instantiateV8Object(env, value, v8RuntimeHandle, v8ArrayCls);
	}
	return instantiateV8Object(env, value, v8RuntimeHandle, v8ObjectCls);
}

void debugHandler() {
	JNIEnv * g_env;
	getJNIEnv(g_env);
	jmethodID processDebugMessage = g_env->GetStaticMethodID(v8cls, "debugMessageReceived", "()V");
	g_env->CallStaticVoidMethod(v8cls, processDebugMessage);
	if (g_env->ExceptionCheck()) {
		g_env->ExceptionDescribe();
	}
	jvm->DetachCurrentThread();
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1enableDebugSupport
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint port, jboolean waitForConnection) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, false);
	bool result = Debug::EnableAgent("j2v8", port, waitForConnection);
	Debug::SetDebugMessageDispatchHandler(&debugHandler);
	return result;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1disableDebugSupport
  (JNIEnv *env, jobject, jint v8RuntimeHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Debug::DisableAgent();
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1processDebugMessages
  (JNIEnv *env, jobject, jint v8RuntimeHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Debug::ProcessDebugMessages();
}

static void jsWindowObjectAccessor(Local<String> property,
		const PropertyCallbackInfo<Value>& info) {
	info.GetReturnValue().Set(info.GetIsolate()->GetCurrentContext()->Global());
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1createIsolate
  (JNIEnv *env, jobject v8, jint handle, jstring globalAlias) {
	if (jvm == NULL ) {
		// on first creation, store the JVM and a handle to V8.class
		env->GetJavaVM(&jvm);
		v8cls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8"));
		v8ObjectCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8Object"));
		v8ArrayCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8Array"));
		stringCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/String"));
		integerCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/Integer"));
		doubleCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/Double"));
		booleanCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/Boolean"));
		v8ResultsUndefinedCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8ResultUndefined"));
		v8ScriptCompilationCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8ScriptCompilationException"));
		v8ScriptExecutionException = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8ScriptExecutionException"));
		v8RuntimeException = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8RuntimeException"));
		errorCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/Error"));
	}
	v8Isolates[handle] = new V8Runtime();
	v8Isolates[handle]->isolate = Isolate::New();
	v8Isolates[handle]->isolate_scope = new Isolate::Scope(v8Isolates[handle]->isolate);
	v8Isolates[handle]->v8 = env->NewGlobalRef(v8);
	HandleScope handle_scope(v8Isolates[handle]->isolate);
	Handle<ObjectTemplate> globalObject = ObjectTemplate::New();
	if ( globalAlias == NULL ) {
		Handle<Context> context = Context::New(v8Isolates[handle]->isolate, NULL, globalObject);
		v8Isolates[handle]->context_.Reset(v8Isolates[handle]->isolate, context);
		v8Isolates[handle]->objects[0] = new Persistent<Object>;
		v8Isolates[handle]->objects[0]->Reset(v8Isolates[handle]->isolate, context->Global()->GetPrototype()->ToObject());
	} else {
		Local<String> utfAlias = createV8String(env, v8Isolates[handle]->isolate, globalAlias);
		globalObject->SetAccessor(utfAlias, jsWindowObjectAccessor);
		Handle<Context> context = Context::New(v8Isolates[handle]->isolate, NULL, globalObject);
		v8Isolates[handle]->context_.Reset(v8Isolates[handle]->isolate, context);
		v8Isolates[handle]->objects[0] = new Persistent<Object>;
		v8Isolates[handle]->objects[0]->Reset(v8Isolates[handle]->isolate, context->Global()->GetPrototype()->ToObject());
	}
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Object
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Local<Object> obj = Object::New(isolate);
	createPersistentContainer(v8Isolates[v8RuntimeHandle], objectHandle);
	v8Isolates[v8RuntimeHandle]->objects[objectHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, obj);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Array
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Local<Array> array = Array::New(isolate);
	createPersistentContainer(v8Isolates[v8RuntimeHandle], arrayHandle);
	v8Isolates[v8RuntimeHandle]->objects[arrayHandle]->Reset(v8Isolates[v8RuntimeHandle]->isolate, array);
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
	env->DeleteGlobalRef(v8Isolates[v8RuntimeHandle]->v8);
	delete(v8Isolates[v8RuntimeHandle]);
	v8Isolates.erase(v8RuntimeHandle);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1contains
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, false);
	Handle<Object> object = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Local<String> v8Key = createV8String(env, isolate, key);
	return object->Has( v8Key );
}

JNIEXPORT jobjectArray JNICALL Java_com_eclipsesource_v8_V8__1getKeys
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Object> object = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Local<Array> properties = object->GetPropertyNames();
	int size = properties->Length();
	jobjectArray keys = (env)->NewObjectArray(size, stringCls, NULL);
	for ( int i = 0; i < size; i++ ) {
		jobject key = (env)->NewStringUTF( *String::Utf8Value( properties->Get(i)->ToString() ) );
		(env)->SetObjectArrayElement(keys, i, key);
	}
	return keys;
}

ScriptOrigin* createScriptOrigin(JNIEnv * env, Isolate* isolate, jstring jscriptName, jint jlineNumber = 0) {
	Local<String> scriptName = createV8String(env, isolate, jscriptName);
	return new ScriptOrigin(scriptName, Integer::New(isolate, jlineNumber));
}

bool compileScript( Isolate *isolate, jstring jscript, JNIEnv *env, jstring jscriptName, jint jlineNumber, Local<Script> &script, TryCatch* tryCatch) {
	Local<String> source = createV8String(env, isolate, jscript);
	ScriptOrigin* scriptOriginPtr = NULL;
	if ( jscriptName != NULL ) {
		scriptOriginPtr = createScriptOrigin(env, isolate, jscriptName, jlineNumber);
	}
	script = Script::Compile(source, scriptOriginPtr);
	if ( scriptOriginPtr != NULL ) {
		delete(scriptOriginPtr);
	}
	if ( tryCatch->HasCaught() ) {
		throwParseException(env, isolate, tryCatch);
		return false;
	}
	return true;
}

bool runScript( Isolate* isolate, JNIEnv *env, Local<Script> *script, TryCatch* tryCatch) {
	(*script)->Run();
	if ( tryCatch->HasCaught() ) {
		throwExecutionException(env, isolate, tryCatch);
		return false;
	}
	return true;
}

bool runScript( Isolate* isolate, JNIEnv *env, Local<Script> *script, TryCatch* tryCatch, Local<Value> &result) {
	result = (*script)->Run();
	if ( tryCatch->HasCaught() ) {
		throwExecutionException(env, isolate, tryCatch);
		return false;
	}
	return true;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeVoidScript
  (JNIEnv * env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	TryCatch tryCatch;
	Local<Script> script;
	if ( !compileScript(isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch) )
		return;
	runScript(isolate, env, &script, &tryCatch);
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1executeDoubleScript
  (JNIEnv * env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	TryCatch tryCatch;
	Local<Script> script;
	Local<Value> result;
	if ( !compileScript(isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch) )
		return 0;
	if ( !runScript(isolate, env, &script, &tryCatch, result ) )
		return 0;
	ASSERT_IS_NUMBER(result);
	return result->NumberValue();
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1executeBooleanScript
  (JNIEnv *env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, false);
	TryCatch tryCatch;
	Local<Script> script;
	Local<Value> result;
	if ( !compileScript(isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch) )
		return false;
	if ( !runScript(isolate, env, &script, &tryCatch, result ) )
		return false;
	ASSERT_IS_BOOLEAN(result);
	return result->BooleanValue();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1executeStringScript
  (JNIEnv *env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	TryCatch tryCatch;
	Local<Script> script;
	Local<Value> result;
	if ( !compileScript(isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch) )
		return NULL;
	if ( !runScript(isolate, env, &script, &tryCatch, result ) )
		return NULL;
	ASSERT_IS_STRING(result);
	String::Utf8Value utf(result->ToString());
	return env->NewStringUTF(*utf);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1executeIntScript
  (JNIEnv * env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	TryCatch tryCatch;
	Local<Script> script;
	Local<Value> result;
	if ( !compileScript(isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch) )
		return 0;
	if ( !runScript(isolate, env, &script, &tryCatch, result ) )
		return 0;
	ASSERT_IS_INTEGER(result);
	return result->Int32Value();
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1executeObjectScript
  (JNIEnv *env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	TryCatch tryCatch;
	Local<Script> script;
	Local<Value> result;
	if ( !compileScript(isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch) )
		return NULL;
	if ( !runScript(isolate, env, &script, &tryCatch, result ) )
		return NULL;
	ASSERT_IS_OBJECT(result);
	return createV8Object(env, result, v8RuntimeHandle);
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1executeArrayScript
  (JNIEnv *env, jobject v8, jint v8RuntimeHandle, jstring jjstring, jstring jscriptName = NULL, jint jlineNumber = 0) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	TryCatch tryCatch;
	Local<Script> script;
	Local<Value> result;
	if ( !compileScript(isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch) )
		return NULL;
	if ( !runScript(isolate, env, &script, &tryCatch, result ) )
		return NULL;
	ASSERT_IS_ARRAY(result);
	return createV8Object(env, result, v8RuntimeHandle);
}

bool invokeFunction(JNIEnv *env, Isolate* isolate, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle, Handle<Value> &result) {
	Local<String> functionName = createV8String(env, isolate, jfunctionName);
	Handle<Object> parentObject = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	int size = 0;
	Handle<Value>* args = NULL;
	if ( parameterHandle >= 0 ) {
		Handle<Object> parameters = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parameterHandle]);
		size = Array::Cast(*parameters)->Length();
		args = new Handle<Value> [size];
		for (int i = 0; i < size; i++) {
			args[i] = parameters->Get(i);
		}
	}
	Handle<Value> value = parentObject->Get(functionName);
	Handle<Function> func = Handle<Function>::Cast(value);
	TryCatch tryCatch;
	result = func->Call(parentObject, size, args);
	if ( args != NULL ) {
		delete(args);
	}
	if ( tryCatch.HasCaught() ) {
		throwExecutionException(env, isolate, &tryCatch);
		return false;
	}
	return true;
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1executeArrayFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Value> result;
	if (!invokeFunction(env, isolate, v8RuntimeHandle, objectHandle, jfunctionName, parameterHandle, result) )
		return NULL;
	ASSERT_IS_ARRAY(result);
	return createV8Object(env, result, v8RuntimeHandle);
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1executeObjectFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Value> result;
	if (!invokeFunction(env, isolate, v8RuntimeHandle, objectHandle, jfunctionName, parameterHandle, result) )
		return NULL;
	ASSERT_IS_OBJECT(result);
	return createV8Object(env, result, v8RuntimeHandle);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1executeIntFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Value> result;
	if (!invokeFunction(env, isolate, v8RuntimeHandle, objectHandle, jfunctionName, parameterHandle, result) )
		return 0;
	ASSERT_IS_INTEGER(result);
	return result->Int32Value();
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1executeDoubleFunction
 (JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Value> result;
	if (!invokeFunction(env, isolate, v8RuntimeHandle, objectHandle, jfunctionName, parameterHandle, result) )
		return 0;
	ASSERT_IS_NUMBER(result);
	return result->NumberValue();
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1executeBooleanFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, false);
	Handle<Value> result;
	if (!invokeFunction(env, isolate, v8RuntimeHandle, objectHandle, jfunctionName, parameterHandle, result) )
		return false;
	ASSERT_IS_BOOLEAN(result);
	return result->BooleanValue();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1executeStringFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Value> result;
	if (!invokeFunction(env, isolate, v8RuntimeHandle, objectHandle, jfunctionName, parameterHandle, result) )
		return NULL;
	ASSERT_IS_STRING(result);
	String::Utf8Value utf(result->ToString());
	return env->NewStringUTF(*utf);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeVoidFunction
(JNIEnv *env, jobject v8, jint v8RuntimeHandle, jint objectHandle, jstring jfunctionName, jint parameterHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Handle<Value> result;
	invokeFunction(env, isolate, v8RuntimeHandle, objectHandle, jfunctionName, parameterHandle, result);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addUndefined
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	addValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key, Undefined(isolate));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__IILjava_lang_String_2I
  (JNIEnv * env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jint value) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	addValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key, Int32::New(isolate, value));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__IILjava_lang_String_2D
  (JNIEnv * env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jdouble value) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	addValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key, Number::New(isolate, value));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__IILjava_lang_String_2Ljava_lang_String_2
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jstring value) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Handle<Value> v8Value = createV8String(env, isolate, value);
	addValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__IILjava_lang_String_2Z
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jboolean value) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	addValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key, Boolean::New(isolate, value));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addObject
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key, jint valueHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Handle<Value> value = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[valueHandle]);
	addValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key, value);
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1getObject
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Value> v8Value = getValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key);
	ASSERT_IS_OBJECT(v8Value);
	return createV8Object(env, v8Value, v8RuntimeHandle);
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1getArray
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Value> v8Value = getValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key);
	ASSERT_IS_ARRAY(v8Value);
	return createV8Object(env, v8Value, v8RuntimeHandle);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getInteger
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Value> v8Value = getValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key);
	ASSERT_IS_INTEGER(v8Value);
	return v8Value->Int32Value();
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1getDouble
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Value> v8Value = getValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key);
	ASSERT_IS_NUMBER(v8Value);
	return v8Value->NumberValue();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1getString
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Value> v8Value = getValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key);
	ASSERT_IS_STRING(v8Value);
	String::Utf8Value utf(v8Value->ToString());
	return env->NewStringUTF(*utf);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1getBoolean
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, false);
	Handle<Value> v8Value = getValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key);
	ASSERT_IS_BOOLEAN(v8Value);
	return v8Value->BooleanValue();
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getType__IILjava_lang_String_2
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring key) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Value> v8Value = getValueWithKey(env, isolate, v8RuntimeHandle, objectHandle, key);
	int type = getType(v8Value);
	if ( type < 0 ) {
		throwResultUndefinedException(env, "");
	}
	return type;
}

bool isNumber(int type) {
	return type == com_eclipsesource_v8_V8_DOUBLE || type == com_eclipsesource_v8_V8_INTEGER;
}

bool isObject(int type) {
	return type == com_eclipsesource_v8_V8_V8_OBJECT || type == com_eclipsesource_v8_V8_V8_ARRAY;
}

bool isNumber(int type1, int type2 ) {
	return isNumber(type1) && isNumber(type2);
}

bool isObject(int type1, int type2) {
	return isObject(type1) && isObject(type2);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getArrayType
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	int length = Array::Cast(*array)->Length();
	int arrayType = com_eclipsesource_v8_V8_UNDEFINED;
	for (int index = 0; index < length; index++) {
		int type = getType(array->Get(index));
		if ( type < 0 ) {
				throwResultUndefinedException(env, "");
		} else if ( index == 0 ) {
			arrayType = type;
		} else if ( type == arrayType ) {
			// continue
		} else if ( isNumber(arrayType, type)) {
			arrayType = com_eclipsesource_v8_V8_DOUBLE;
		} else if ( isObject(arrayType, type)) {
			arrayType = com_eclipsesource_v8_V8_V8_OBJECT;
		} else {
			return com_eclipsesource_v8_V8_UNDEFINED;
		}
	}
	return arrayType;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetSize
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	return Array::Cast(*array)->Length();
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetInteger
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Handle<Value> v8Value = array->Get(index);
	ASSERT_IS_INTEGER(v8Value);
	return v8Value->Int32Value();
}

JNIEXPORT jintArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetInts
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint start, jint length) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	jintArray result = env->NewIntArray(length);
	jint fill[length];
	for (int i = start; i < start+length; i++) {
		Handle<Value> v8Value = array->Get(i);
		ASSERT_IS_INTEGER(v8Value);
		fill[i-start] = v8Value->Int32Value();
	}
	(env)->SetIntArrayRegion(result, 0, length, fill);
	return result;
}

JNIEXPORT jdoubleArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetDoubles
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint start, jint length) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	jdoubleArray result = env->NewDoubleArray(length);
	jdouble fill[length];
	for (int i = start; i < start+length; i++) {
		Handle<Value> v8Value = array->Get(i);
		ASSERT_IS_NUMBER(v8Value);
		fill[i-start] = v8Value->NumberValue();
	}
	(env)->SetDoubleArrayRegion(result, 0, length, fill);
	return result;
}

JNIEXPORT jbooleanArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetBooleans
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint start, jint length) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	jbooleanArray result = env->NewBooleanArray(length);
	jboolean fill[length];
	for (int i = start; i < start+length; i++) {
		Handle<Value> v8Value = array->Get(i);
		ASSERT_IS_BOOLEAN(v8Value);
		fill[i-start] = v8Value->BooleanValue();
	}
	(env)->SetBooleanArrayRegion(result, 0, length, fill);
	return result;
}

JNIEXPORT jobjectArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetStrings
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint start, jint length) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	jobjectArray result = env->NewObjectArray(length, stringCls, NULL);
	for (int i = start; i < start+length; i++) {
		Handle<Value> v8Value = array->Get(i);
		ASSERT_IS_STRING(v8Value);
		String::Utf8Value utf(v8Value->ToString());
		env->SetObjectArrayElement(result, i-start, env->NewStringUTF(*utf));
	}
	return result;
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1arrayGetBoolean
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, false);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Handle<Value> v8Value = array->Get(index);
	ASSERT_IS_BOOLEAN(v8Value);
	return v8Value->BooleanValue();
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1arrayGetDouble
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Handle<Value> v8Value = array->Get(index);
	ASSERT_IS_NUMBER(v8Value);
	return v8Value->NumberValue();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1arrayGetString
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Handle<Value> v8Value = array->Get(index);
	ASSERT_IS_STRING(v8Value);
	String::Utf8Value utf(v8Value->ToString());
	return env->NewStringUTF(*utf);
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1arrayGetObject
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Handle<Value> v8Value = array->Get(index);
	ASSERT_IS_OBJECT(v8Value);
	return createV8Object(env, v8Value, v8RuntimeHandle);
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1arrayGetArray
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint index) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, NULL);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Handle<Value> v8Value = array->Get(index);
	ASSERT_IS_ARRAY(v8Value);
	return createV8Object(env, v8Value, v8RuntimeHandle);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayUndefinedItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	int index = Array::Cast(*array)->Length();
	array->Set(index, Undefined(isolate));
}


JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayIntItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint value) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Local<Value> v8Value = Int32::New(isolate, value);
	int index = Array::Cast(*array)->Length();
	array->Set(index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayDoubleItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jdouble value) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Local<Value> v8Value = Number::New(isolate, value);
	int index = Array::Cast(*array)->Length();
	array->Set(index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayBooleanItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jboolean value) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	Local<Value> v8Value = Boolean::New(isolate, value);
	int index = Array::Cast(*array)->Length();
	array->Set(index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayStringItem
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jstring value) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	int index = Array::Cast(*array)->Length();
	Local<String> v8Value = createV8String(env, isolate, value);
	array->Set(index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayObjectItem
 (JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint valueHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
	int index = Array::Cast(*array)->Length();
	Local<Value> v8Value = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[valueHandle]);
	array->Set(index, v8Value);
}

int getType(Handle<Value> v8Value) {
	if (v8Value.IsEmpty() || v8Value->IsUndefined() || v8Value->IsNull()) {
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

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getType__III
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jint index) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Handle<Value> v8Value = array->Get(index);
	int type = getType(v8Value);
	if ( type < 0 ) {
		throwResultUndefinedException(env, "");
	}
	return type;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getType__IIII
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint arrayHandle, jint start, jint length) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, 0);
	Handle<Object> array = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[arrayHandle]);
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
	jmethodID release = env->GetMethodID(v8ObjectCls, "release", "()V");
	env->CallVoidMethod(object, release);
}

void releaseArray(JNIEnv* env, jobject object) {
	jmethodID release = env->GetMethodID(v8ArrayCls, "release", "()V");
	env->CallVoidMethod(object, release);
}

int getHandle(JNIEnv* env, jobject object) {
	jmethodID getHandle = env->GetMethodID(v8ObjectCls, "getHandle", "()I");
	jint handle = env->CallIntMethod(object, getHandle);
	return handle;
}

jobject createParameterArray(JNIEnv* env, int v8RuntimeHandle, jobject v8, int size, const FunctionCallbackInfo<Value>& args) {
	Isolate* isolate = getIsolate(env, v8RuntimeHandle);
	jmethodID methodID = env->GetMethodID(v8ArrayCls, "<init>", "(Lcom/eclipsesource/v8/V8;)V");
	jmethodID getHandle = env->GetMethodID(v8ArrayCls, "getHandle", "()I");
	jobject result = env->NewObject(v8ArrayCls, methodID, v8);
	jint parameterHandle = env->CallIntMethod(result, getHandle);
	Handle<Object> parameters = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[parameterHandle]);
	for ( int i = 0; i < size; i++) {
		parameters->Set(i, args[i]);
	}
	return result;
}

void voidCallback(const FunctionCallbackInfo<Value>& args) {
	int size = args.Length();
	Local<External> data = Local<External>::Cast(args.Data());
	void *methodDescriptorPtr = data->Value();
	MethodDescriptor* md = static_cast<MethodDescriptor*>(methodDescriptorPtr);
	jobject v8 = v8Isolates[md->v8RuntimeHandle]->v8;
	JNIEnv * env;
	getJNIEnv(env);
	jobject parameters = createParameterArray(env, md->v8RuntimeHandle, v8, size, args);
	jmethodID callVoidMethod = (env)->GetMethodID(v8cls, "callVoidJavaMethod", "(ILcom/eclipsesource/v8/V8Array;)V");
	env->CallVoidMethod(v8, callVoidMethod, md->methodID, parameters);
	if ( env -> ExceptionCheck() ) {
		Isolate* isolate = getIsolate(env, md->v8RuntimeHandle);
		isolate->ThrowException(String::NewFromUtf8(isolate, "Unhandled Java Exception"));
	}
	jmethodID release = env->GetMethodID(v8ArrayCls, "release", "()V");
	env->CallVoidMethod(parameters, release);
	env->DeleteLocalRef(parameters);
}

int getReturnType(JNIEnv* env, jobject &object) {
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
	return result;
}

int getInteger(JNIEnv* env, jobject &object) {
	jmethodID intValueMethod = env->GetMethodID(integerCls, "intValue", "()I");
	int result = env->CallIntMethod(object, intValueMethod);
	return result;
}

bool getBoolean(JNIEnv* env, jobject &object) {
	jmethodID boolValueMethod = env->GetMethodID(booleanCls, "booleanValue", "()Z");
	bool result = env->CallBooleanMethod(object, boolValueMethod);
	return result;
}

double getDouble(JNIEnv* env, jobject &object) {
	jmethodID doubleValueMethod = env->GetMethodID(doubleCls, "doubleValue", "()D");
	double result = env->CallDoubleMethod(object, doubleValueMethod);
	return result;
}

void objectCallback(const FunctionCallbackInfo<Value>& args) {
	int size = args.Length();
	Local<External> data = Local<External>::Cast(args.Data());
	void *methodDescriptorPtr = data->Value();
	MethodDescriptor* md = static_cast<MethodDescriptor*>(methodDescriptorPtr);
	jobject v8 = v8Isolates[md->v8RuntimeHandle]->v8;
	Isolate* isolate = v8Isolates[md->v8RuntimeHandle]->isolate;
	JNIEnv * env;
	getJNIEnv(env);
	jobject parameters = createParameterArray(env, md->v8RuntimeHandle, v8, size, args);
	jmethodID callObjectMethod = (env)->GetMethodID(v8cls, "callObjectJavaMethod", "(ILcom/eclipsesource/v8/V8Array;)Ljava/lang/Object;");
	jobject resultObject = env->CallObjectMethod(v8, callObjectMethod, md->methodID, parameters);
	if ( env -> ExceptionCheck() ) {
		Isolate* isolate = getIsolate(env, md->v8RuntimeHandle);
		isolate->ThrowException(String::NewFromUtf8(isolate, "Unhandled Java Exception"));
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
			Local<String> result = createV8String(env, v8Isolates[md->v8RuntimeHandle]->isolate, (jstring)resultObject);
			args.GetReturnValue().Set(result);
		} else if ( returnType == com_eclipsesource_v8_V8_V8_ARRAY ) {
			int resultHandle = getHandle(env, resultObject);
			Handle<Object> result = Local<Object>::New(isolate, *v8Isolates[md->v8RuntimeHandle]->objects[resultHandle]);
			releaseArray(env, resultObject);
			args.GetReturnValue().Set(result);
		} else if ( returnType == com_eclipsesource_v8_V8_V8_OBJECT ) {
			int resultHandle = getHandle(env, resultObject);
			Handle<Object> result = Local<Object>::New(isolate, *v8Isolates[md->v8RuntimeHandle]->objects[resultHandle]);
			release(env, resultObject);
			args.GetReturnValue().Set(result);
		} else {
			args.GetReturnValue().SetUndefined();
		}
	}
	jmethodID release = env->GetMethodID(v8ArrayCls, "release", "()V");
	env->CallVoidMethod(parameters, release);
	env->DeleteLocalRef(parameters);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1registerJavaMethod
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jstring functionName, jint methodID, jboolean voidMethod) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	FunctionCallback callback = voidCallback;
	if ( !voidMethod ) {
		callback = objectCallback;
	}
	Handle<Object> object = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Local<String> v8FunctionName = createV8String(env, isolate, functionName);
	MethodDescriptor* md = new MethodDescriptor();
	md -> methodID = methodID;
	md -> v8RuntimeHandle = v8RuntimeHandle;
	object->Set(v8FunctionName, Function::New(isolate, callback, External::New(isolate, md)));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1setPrototype
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jint prototypeHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, );
	Handle<Object> object = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Handle<Object> prototype = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[prototypeHandle]);
	object->SetPrototype(prototype);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1equals
  (JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jint thatHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, false);
	Handle<Object> object = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Handle<Object> that = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[thatHandle]);
	return object->Equals(that);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1strictEquals
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jint thatHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, false);
	Handle<Object> object = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Handle<Object> that = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[thatHandle]);
	return object->StrictEquals(that);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1sameValue
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle, jint thatHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, false);
	Handle<Object> object = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	Handle<Object> that = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[thatHandle]);
	return object->SameValue(that);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1identityHash
(JNIEnv *env, jobject, jint v8RuntimeHandle, jint objectHandle) {
	Isolate* isolate = SETUP(env, v8RuntimeHandle, false);
	Handle<Object> object = Local<Object>::New(isolate, *v8Isolates[v8RuntimeHandle]->objects[objectHandle]);
	return object->GetIdentityHash();
}

Isolate* getIsolate(JNIEnv *env, int handle) {
	if ( v8Isolates.find(handle) == v8Isolates.end() ) {
		throwError(env, "V8 isolate not found.");
		return NULL;
	}
	return v8Isolates[handle]->isolate;
}

void throwResultUndefinedException( JNIEnv *env, const char *message ) {
    (env)->ThrowNew(v8ResultsUndefinedCls, message );
}

void throwParseException(JNIEnv *env, const char* fileName, int lineNumber, const char* message,
		const char* sourceLine, int startColumn, int endColumn) {
    jmethodID methodID = env->GetMethodID(v8ScriptCompilationCls, "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;II)V");
    jstring jfileName = env->NewStringUTF(fileName);
    jstring jmessage = env->NewStringUTF(message);
    jstring jsourceLine = env->NewStringUTF(sourceLine);
    jthrowable result = (jthrowable) env->NewObject(v8ScriptCompilationCls, methodID, jfileName, lineNumber, jmessage, jsourceLine, startColumn, endColumn);
    (env)->Throw( result );
}

void throwExecutionException(JNIEnv *env, const char* fileName, int lineNumber, const char* message,
		const char* sourceLine, int startColumn, int endColumn, const char* stackTrace) {
    jmethodID methodID = env->GetMethodID(v8ScriptExecutionException, "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;IILjava/lang/String;Ljava/lang/Throwable;)V");
    jstring jfileName = env->NewStringUTF(fileName);
    jstring jmessage = env->NewStringUTF(message);
    jstring jsourceLine = env->NewStringUTF(sourceLine);
    jstring jstackTrace = NULL;
    if ( stackTrace != NULL ) {
    	jstackTrace = env->NewStringUTF(stackTrace);
    }
    jthrowable wrappedException = NULL;
    if ( env -> ExceptionCheck() ) {
    	wrappedException = env->ExceptionOccurred();
    }
    jthrowable result = (jthrowable) env->NewObject(v8ScriptExecutionException, methodID, jfileName, lineNumber, jmessage, jsourceLine, startColumn, endColumn, jstackTrace, wrappedException);
    (env)->Throw( result );
}

void throwParseException( JNIEnv *env, Isolate* isolate, TryCatch* tryCatch) {
	HandleScope handle_scope(isolate);
	String::Utf8Value exception(tryCatch->Exception());
	const char* exceptionString = ToCString(exception);
	Handle<Message> message = tryCatch->Message();
	if (message.IsEmpty()) {
		throwV8RuntimeException(env, exceptionString);
	} else {
	    String::Utf8Value filename(message->GetScriptResourceName());
	    int lineNumber = message->GetLineNumber();
	    String::Utf8Value sourceline(message->GetSourceLine());
	    int start = message->GetStartColumn();
	    int end = message->GetEndColumn();
	    const char* filenameString = ToCString(filename);
	    const char* sourcelineString = ToCString(sourceline);
	    throwParseException(env, filenameString, lineNumber, exceptionString, sourcelineString, start, end);
	}
}

void throwExecutionException( JNIEnv *env, Isolate* isolate, TryCatch* tryCatch) {
	HandleScope handle_scope(isolate);
	String::Utf8Value exception(tryCatch->Exception());
	const char* exceptionString = ToCString(exception);
	Handle<Message> message = tryCatch->Message();
	if (message.IsEmpty()) {
		throwV8RuntimeException(env, exceptionString);
	} else {
	    String::Utf8Value filename(message->GetScriptResourceName());
	    int lineNumber = message->GetLineNumber();
	    String::Utf8Value sourceline(message->GetSourceLine());
	    int start = message->GetStartColumn();
	    int end = message->GetEndColumn();
	    const char* filenameString = ToCString(filename);
	    const char* sourcelineString = ToCString(sourceline);
	    String::Utf8Value stack_trace(tryCatch->StackTrace());
	    const char* stackTrace = NULL;
	    if (stack_trace.length() > 0) {
	      stackTrace = ToCString(stack_trace);
	    }
	    throwExecutionException(env, filenameString, lineNumber, exceptionString, sourcelineString, start, end, stackTrace);
	}
}

void throwV8RuntimeException( JNIEnv *env, const char *message ) {
    (env)->ThrowNew(v8RuntimeException, message );
}

void throwError( JNIEnv *env, const char *message ) {
    (env)->ThrowNew(errorCls, message );
}
