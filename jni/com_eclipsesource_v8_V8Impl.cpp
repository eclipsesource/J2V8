/*******************************************************************************
* Copyright (c) 2014 EclipseSource and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    EclipseSource - initial API and implementation
******************************************************************************/
#include <jni.h>
#include <libplatform/libplatform.h>
#include <iostream>
#include <v8.h>
#include <v8-inspector.h>
#include <functional>
#include <string.h>
#include <map>
#include <cstdlib>
#include "com_eclipsesource_v8_V8Impl.h"

#define TAG "J2V8_V8Impl"

#pragma comment(lib, "userenv.lib")
#pragma comment(lib, "IPHLPAPI.lib")
#pragma comment(lib, "Ws2_32.lib")
#pragma comment(lib, "WINMM.lib")
#pragma comment( lib, "psapi.lib" )

using namespace std;
using namespace v8;

inline std::string convertStringViewToSTDString(Isolate* isolate, const v8_inspector::StringView stringView) {
  int length = static_cast<int>(stringView.length());
  v8::Local<v8::String> message = (
        stringView.is8Bit()
          ? v8::String::NewFromOneByte(isolate, reinterpret_cast<const uint8_t*>(stringView.characters8()), v8::NewStringType::kNormal, length)
          : v8::String::NewFromTwoByte(isolate, reinterpret_cast<const uint16_t*>(stringView.characters16()), v8::NewStringType::kNormal, length)
      ).ToLocalChecked();
  v8::String::Utf8Value result(isolate, message);
  return *result;
}

inline v8_inspector::StringView convertSTDStringToStringView(const std::string &str) {
  auto* stringView = reinterpret_cast<const uint8_t*>(str.c_str());
  return { stringView, str.length() };
}

class MethodDescriptor {
public:
  jlong methodID;
  jlong v8RuntimePtr;
  Persistent<External> obj;
};

class WeakReferenceDescriptor {
public:
  jlong v8RuntimePtr;
  jlong objectHandle;
};

class InspectorDelegate {
public:
  InspectorDelegate(const function<void(std::string)> &onResponse, const function<void(void)> &waitFrontendMessage) {
    onResponse_ = onResponse;
    waitFrontendMessage_ = waitFrontendMessage;
  }

  void emitOnResponse(string message) {
    onResponse_(message);
  }

  void emitWaitFrontendMessage() {
    waitFrontendMessage_();
  }

private:
  std::function<void(std::string)> onResponse_;
  std::function<void(void)> waitFrontendMessage_;
};

class V8InspectorChannelImpl final: public v8_inspector::V8Inspector::Channel
{
public:
  V8InspectorChannelImpl(Isolate* isolate, InspectorDelegate* inspectorDelegate) {
    isolate_ = isolate;
    inspectorDelegate_ = inspectorDelegate;
  }

  void sendResponse(int, unique_ptr<v8_inspector::StringBuffer> message) override {
    const std::string response = convertStringViewToSTDString(isolate_, message->string());
    inspectorDelegate_->emitOnResponse(response);
  }

  void sendNotification(unique_ptr<v8_inspector::StringBuffer> message) override {
    const std::string notification = convertStringViewToSTDString(isolate_, message->string());
    inspectorDelegate_->emitOnResponse(notification);
  }

  void flushProtocolNotifications() override {}

  uint8_t waitFrontendMessageOnPause() {
    inspectorDelegate_->emitWaitFrontendMessage();
    return 1;
  }

private:
  v8::Isolate* isolate_;
  InspectorDelegate* inspectorDelegate_;
};

class V8InspectorClientImpl final: public v8_inspector::V8InspectorClient {
public:
  V8InspectorClientImpl(Isolate* isolate, const std::unique_ptr<v8::Platform> &platform, InspectorDelegate* inspectorDelegate, std::string contextName) {
    isolate_ = isolate;
    context_ = isolate->GetCurrentContext();
    platform_ = platform.get();
    channel_ = std::unique_ptr<V8InspectorChannelImpl>(new V8InspectorChannelImpl(isolate, inspectorDelegate));
    inspector_ = v8_inspector::V8Inspector::create(isolate, this);
    session_ = inspector_->connect(kContextGroupId, channel_.get(), v8_inspector::StringView());
    context_->SetAlignedPointerInEmbedderData(1, this);

    inspector_->contextCreated(
      v8_inspector::V8ContextInfo(isolate->GetCurrentContext(),
      kContextGroupId,
      convertSTDStringToStringView(contextName))
    );
  }

  void dispatchProtocolMessage(const v8_inspector::StringView &message_view) {
    session_->dispatchProtocolMessage(message_view);
  }

  void runMessageLoopOnPause(int) override {
    if (run_nested_loop_) {
        return;
    }
    terminated_ = false;
    run_nested_loop_ = true;
    while (!terminated_ && channel_->waitFrontendMessageOnPause()) {
        while (v8::platform::PumpMessageLoop(platform_, isolate_)) {}
    }
    terminated_ = true;
    run_nested_loop_ = false;
  }

  void quitMessageLoopOnPause() override {
    terminated_ = true;
  }

  void schedulePauseOnNextStatement(const v8_inspector::StringView &reason) {
    session_->schedulePauseOnNextStatement(reason, reason);
  }

private:
  static const int kContextGroupId = 1;
  v8::Isolate* isolate_;
  v8::Handle<v8::Context> context_;
  v8::Platform* platform_;
  unique_ptr<v8_inspector::V8Inspector> inspector_;
  unique_ptr<v8_inspector::V8InspectorSession> session_;
  unique_ptr<V8InspectorChannelImpl> channel_;
  uint8_t terminated_ = 0;
  uint8_t run_nested_loop_ = 0;
};

class V8Inspector {
public:
  jobject delegate = nullptr;
  V8InspectorClientImpl* client = nullptr;

  void dispatchProtocolMessage(const std::string &message) {
    if (client == nullptr) {
      return;
    }
    v8_inspector::StringView protocolMessage = convertSTDStringToStringView(message);
    client->dispatchProtocolMessage(protocolMessage);
  }

  void schedulePauseOnNextStatement(const std::string reason) {
    if (client == nullptr) {
      return;
    }
    auto reason_ = convertSTDStringToStringView(reason);
    client->schedulePauseOnNextStatement(reason_);
  }

  void onResponse(const string& message);

  void waitFrontendMessage();
};

class V8Runtime {
public:
  Isolate* isolate;
  Persistent<Context> context_;
  Persistent<Object>* globalObject;
  Locker* locker;
  jobject v8;
  jthrowable pendingException;
  V8Inspector* inspector;
};

std::unique_ptr<v8::Platform> v8Platform = nullptr;

const char* ToCString(const String::Utf8Value& value) {
  return *value ? *value : "<string conversion failed>";
}

JavaVM* jvm = nullptr;
jclass v8cls = nullptr;
jclass v8InspectorCls = nullptr;
jclass v8InspectorDelegateCls = nullptr;
jclass v8ObjectCls = nullptr;
jclass v8ArrayCls = nullptr;
jclass v8TypedArrayCls = nullptr;
jclass v8ArrayBufferCls = nullptr;
jclass v8FunctionCls = nullptr;
jclass undefinedV8ObjectCls = nullptr;
jclass undefinedV8ArrayCls = nullptr;
jclass v8ResultsUndefinedCls = nullptr;
jclass v8ScriptCompilationCls = nullptr;
jclass v8ScriptExecutionException = nullptr;
jclass v8RuntimeExceptionCls = nullptr;
jclass throwableCls = nullptr;
jclass stringCls = nullptr;
jclass integerCls = nullptr;
jclass doubleCls = nullptr;
jclass booleanCls = nullptr;
jclass errorCls = nullptr;
jclass unsupportedOperationExceptionCls = nullptr;
jmethodID v8ArrayInitMethodID = nullptr;
jmethodID v8TypedArrayInitMethodID = nullptr;
jmethodID v8ArrayBufferInitMethodID = nullptr;
jmethodID v8ArrayGetHandleMethodID = nullptr;
jmethodID v8CallVoidMethodID = nullptr;
jmethodID v8ObjectReleaseMethodID = nullptr;
jmethodID v8DisposeMethodID = nullptr;
jmethodID v8WeakReferenceReleased = nullptr;
jmethodID v8ArrayReleaseMethodID = nullptr;
jmethodID v8ObjectIsUndefinedMethodID = nullptr;
jmethodID v8ObjectGetHandleMethodID = nullptr;
jmethodID throwableGetMessageMethodID = nullptr;
jmethodID integerIntValueMethodID = nullptr;
jmethodID booleanBoolValueMethodID = nullptr;
jmethodID doubleDoubleValueMethodID = nullptr;
jmethodID v8CallObjectJavaMethodMethodID = nullptr;
jmethodID v8ScriptCompilationInitMethodID = nullptr;
jmethodID v8ScriptExecutionExceptionInitMethodID = nullptr;
jmethodID undefinedV8ArrayInitMethodID = nullptr;
jmethodID undefinedV8ObjectInitMethodID = nullptr;
jmethodID integerInitMethodID = nullptr;
jmethodID doubleInitMethodID = nullptr;
jmethodID booleanInitMethodID = nullptr;
jmethodID v8FunctionInitMethodID = nullptr;
jmethodID v8ObjectInitMethodID = nullptr;
jmethodID v8RuntimeExceptionInitMethodID = nullptr;
jmethodID v8InspectorDelegateOnResponseMethodID = nullptr;
jmethodID v8InspectorDelegateWaitFrontendMessageMethodID = nullptr;

void throwParseException(JNIEnv *env, const Local<Context>& context, Isolate* isolate, TryCatch* tryCatch);
void throwExecutionException(JNIEnv *env, const Local<Context>& context, Isolate* isolate, TryCatch* tryCatch, jlong v8RuntimePtr);
void throwError(JNIEnv *env, const char *message);
void disposeMethod(v8::WeakCallbackInfo<MethodDescriptor> const& data);
void throwV8RuntimeException(JNIEnv *env,  String::Value *message);
void throwResultUndefinedException(JNIEnv *env, const char *message);
Isolate* getIsolate(JNIEnv *env, jlong handle);
int getType(Handle<Value> v8Value);
jobject getResult(JNIEnv *env, const Local<Context>& context, jobject &v8, jlong v8RuntimePtr, Handle<Value> &result, jint expectedType);

#define SETUP(env, v8RuntimePtr, errorReturnResult) getIsolate(env, v8RuntimePtr);\
    if ( isolate == nullptr ) {\
      return errorReturnResult;\
                                }\
    V8Runtime* runtime = reinterpret_cast<V8Runtime*>(v8RuntimePtr);\
    Isolate::Scope isolateScope(isolate);\
    HandleScope handle_scope(isolate);\
    Local<Context> context = Local<Context>::New(isolate,runtime->context_);\
    Context::Scope context_scope(context);
#define ASSERT_IS_NUMBER(v8Value) \
    if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsNumber()) {\
      throwResultUndefinedException(env, "");\
      return 0;\
                                }
#define ASSERT_IS_STRING(v8Value)\
    if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsString()) {\
      if ( v8Value->IsNull() ) {\
        return 0;\
      }\
      throwResultUndefinedException(env, "");\
      return 0;\
                                }
#define ASSERT_IS_BOOLEAN(v8Value)\
    if (v8Value.IsEmpty() || v8Value->IsUndefined() || !v8Value->IsBoolean() ) {\
      throwResultUndefinedException(env, "");\
      return 0;\
                                }
void release(JNIEnv* env, jobject object) {
  env->CallVoidMethod(object, v8ObjectReleaseMethodID);
}

void releaseArray(JNIEnv* env, jobject object) {
  env->CallVoidMethod(object, v8ArrayReleaseMethodID);
}

int isUndefined(JNIEnv* env, jobject object) {
  return env->CallBooleanMethod(object, v8ObjectIsUndefinedMethodID);
}

jlong getHandle(JNIEnv* env, jobject object) {
  return env->CallLongMethod(object, v8ObjectGetHandleMethodID);
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1getVersion (JNIEnv *env, jclass) {
  const char* utfString = v8::V8::GetVersion();
  return env->NewStringUTF(utfString);
}


JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1getConstructorName
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  String::Value unicodeString(isolate, object->GetConstructorName());
  return env->NewString(*unicodeString, unicodeString.length());
}

Local<String> createV8String(JNIEnv *env, Isolate *isolate, jstring &string) {
  const uint16_t* unicodeString = env->GetStringChars(string, nullptr);
  int length = env->GetStringLength(string);
  MaybeLocal<String> twoByteString = String::NewFromTwoByte(isolate, unicodeString, v8::NewStringType::kNormal, length);
  if (twoByteString.IsEmpty()) {
    return Local<String>();
  }
  Local<String> result = twoByteString.ToLocalChecked();
  env->ReleaseStringChars(string, unicodeString);
  return result;
}

std::string createString(JNIEnv *env, Isolate *isolate, jstring &str) {
  Local<String> v8Str = createV8String(env, isolate, str);
  v8::String::Utf8Value stdString(isolate, v8Str);
  return ToCString(stdString);
}

Handle<Value> getValueWithKey(JNIEnv* env, const Local<Context>& context, Isolate* isolate, jlong &v8RuntimePtr, jlong &objectHandle, jstring &key) {
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<PersistentBase<Object>*>(objectHandle));
  Local<String> v8Key = createV8String(env, isolate, key);
  return object->Get(context, v8Key).ToLocalChecked();
}

void addValueWithKey(JNIEnv* env, const Local<Context> context, Isolate* isolate, jlong &v8RuntimePtr, jlong &objectHandle, jstring &key, Handle<Value> value) {
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<PersistentBase<Object>*>(objectHandle));
  const uint16_t* unicodeString_key = env->GetStringChars(key, NULL);
  int length = env->GetStringLength(key);
  Local<String> v8Key = String::NewFromTwoByte(isolate, unicodeString_key, v8::NewStringType::kNormal, length).ToLocalChecked();
  object->Set(context, v8Key, value);
  env->ReleaseStringChars(key, unicodeString_key);
}

void getJNIEnv(JNIEnv*& env) {
  int getEnvStat = jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
  if (getEnvStat == JNI_EDETACHED) {
#ifdef __ANDROID_API__
    if (jvm->AttachCurrentThread(&env, NULL) != 0) {
#else
    if (jvm->AttachCurrentThread((void **)&env, nullptr) != 0) {
#endif
      std::cout << "Failed to attach" << std::endl;
    }
  }
  else if (getEnvStat == JNI_OK) {
  }
  else if (getEnvStat == JNI_EVERSION) {
    std::cout << "GetEnv: version not supported" << std::endl;
  }
}

static void jsWindowObjectAccessor(Local<String> property,
  const PropertyCallbackInfo<Value>& info) {
  info.GetReturnValue().Set(info.GetIsolate()->GetCurrentContext()->Global());
}

class ShellArrayBufferAllocator : public v8::ArrayBuffer::Allocator {
 public:
  virtual void* Allocate(size_t length) {
    void* data = AllocateUninitialized(length);
    return data == NULL ? data : memset(data, 0, length);
  }
  virtual void* AllocateUninitialized(size_t length) { return malloc(length); }
  virtual void Free(void* data, size_t) { free(data); }
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void*) {
    JNIEnv *env;
    jint onLoad_err = -1;
    if ( vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK ) {
        return onLoad_err;
    }
    if (env == nullptr) {
        return onLoad_err;
    }

    v8::V8::InitializeICU();
    v8Platform = v8::platform::NewDefaultPlatform();
    v8::V8::InitializePlatform(v8Platform.get());
    v8::V8::Initialize();

    // on first creation, store the JVM and a handle to J2V8 classes
    jvm = vm;
    v8cls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8"));
    v8InspectorCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/inspector/V8Inspector"));
    v8InspectorDelegateCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/inspector/V8InspectorDelegate"));
    v8ObjectCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8Object"));
    v8ArrayCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8Array"));
    v8TypedArrayCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8TypedArray"));
    v8ArrayBufferCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8ArrayBuffer"));
    v8FunctionCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8Function"));
    undefinedV8ObjectCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8Object$Undefined"));
    undefinedV8ArrayCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8Array$Undefined"));
    stringCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/String"));
    integerCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/Integer"));
    doubleCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/Double"));
    booleanCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/Boolean"));
    throwableCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/Throwable"));
    v8ResultsUndefinedCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8ResultUndefined"));
    v8ScriptCompilationCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8ScriptCompilationException"));
    v8ScriptExecutionException = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8ScriptExecutionException"));
    v8RuntimeExceptionCls = (jclass)env->NewGlobalRef((env)->FindClass("com/eclipsesource/v8/V8RuntimeException"));
    errorCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/Error"));
    unsupportedOperationExceptionCls = (jclass)env->NewGlobalRef((env)->FindClass("java/lang/UnsupportedOperationException"));

    // Get all method IDs
    v8ArrayInitMethodID = env->GetMethodID(v8ArrayCls, "<init>", "(Lcom/eclipsesource/v8/V8;)V");
	  v8TypedArrayInitMethodID = env->GetMethodID(v8TypedArrayCls, "<init>", "(Lcom/eclipsesource/v8/V8;)V");
    v8ArrayBufferInitMethodID = env->GetMethodID(v8ArrayBufferCls, "<init>", "(Lcom/eclipsesource/v8/V8;Ljava/nio/ByteBuffer;)V");
    v8ArrayGetHandleMethodID = env->GetMethodID(v8ArrayCls, "getHandle", "()J");
    v8CallVoidMethodID = (env)->GetMethodID(v8cls, "callVoidJavaMethod", "(JLcom/eclipsesource/v8/V8Object;Lcom/eclipsesource/v8/V8Array;)V");
    v8ObjectReleaseMethodID = env->GetMethodID(v8ObjectCls, "release", "()V");
    v8ArrayReleaseMethodID = env->GetMethodID(v8ArrayCls, "release", "()V");
    v8ObjectIsUndefinedMethodID = env->GetMethodID(v8ObjectCls, "isUndefined", "()Z");
    v8ObjectGetHandleMethodID = env->GetMethodID(v8ObjectCls, "getHandle", "()J");
    throwableGetMessageMethodID = env->GetMethodID(throwableCls, "getMessage", "()Ljava/lang/String;");
    integerIntValueMethodID = env->GetMethodID(integerCls, "intValue", "()I");
    booleanBoolValueMethodID = env->GetMethodID(booleanCls, "booleanValue", "()Z");
    doubleDoubleValueMethodID = env->GetMethodID(doubleCls, "doubleValue", "()D");
    v8CallObjectJavaMethodMethodID = (env)->GetMethodID(v8cls, "callObjectJavaMethod", "(JLcom/eclipsesource/v8/V8Object;Lcom/eclipsesource/v8/V8Array;)Ljava/lang/Object;");
    v8DisposeMethodID = (env)->GetMethodID(v8cls, "disposeMethodID", "(J)V");
    v8WeakReferenceReleased = (env)->GetMethodID(v8cls, "weakReferenceReleased", "(J)V");
    v8ScriptCompilationInitMethodID = env->GetMethodID(v8ScriptCompilationCls, "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;II)V");
    v8ScriptExecutionExceptionInitMethodID = env->GetMethodID(v8ScriptExecutionException, "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;IILjava/lang/String;Ljava/lang/Throwable;)V");
    undefinedV8ArrayInitMethodID = env->GetMethodID(undefinedV8ArrayCls, "<init>", "()V");
    undefinedV8ObjectInitMethodID = env->GetMethodID(undefinedV8ObjectCls, "<init>", "()V");
    v8RuntimeExceptionInitMethodID = env->GetMethodID(v8RuntimeExceptionCls, "<init>", "(Ljava/lang/String;)V");
    integerInitMethodID = env->GetMethodID(integerCls, "<init>", "(I)V");
    doubleInitMethodID = env->GetMethodID(doubleCls, "<init>", "(D)V");
    booleanInitMethodID = env->GetMethodID(booleanCls, "<init>", "(Z)V");
    v8FunctionInitMethodID = env->GetMethodID(v8FunctionCls, "<init>", "(Lcom/eclipsesource/v8/V8;)V");
    v8ObjectInitMethodID = env->GetMethodID(v8ObjectCls, "<init>", "(Lcom/eclipsesource/v8/V8;)V");

    v8InspectorDelegateOnResponseMethodID = env->GetMethodID(v8InspectorDelegateCls, "onResponse", "(Ljava/lang/String;)V");
    v8InspectorDelegateWaitFrontendMessageMethodID = env->GetMethodID(v8InspectorDelegateCls, "waitFrontendMessageOnPause", "()V");

    return JNI_VERSION_1_6;
}

void V8Inspector::onResponse(const string& message) {
    JNIEnv * env;
    getJNIEnv(env);
    env->CallVoidMethod(delegate, v8InspectorDelegateOnResponseMethodID, env->NewStringUTF(message.c_str()));
}

void V8Inspector::waitFrontendMessage() {
    JNIEnv * env;
    getJNIEnv(env);
    env->CallVoidMethod(delegate, v8InspectorDelegateWaitFrontendMessageMethodID);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1setFlags
 (JNIEnv *env, jclass, jstring v8flags) {
    if (v8flags) {
        char const* str = env->GetStringUTFChars(v8flags, nullptr);
        v8::V8::SetFlagsFromString(str, env->GetStringUTFLength(v8flags));
        env->ReleaseStringUTFChars(v8flags, str);
        v8::V8::Initialize();
    }
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1getBuildID
  (JNIEnv *, jclass) {
  return 2;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1startNodeJS
  (JNIEnv * env, jclass, jlong, jstring) {
    (env)->ThrowNew(unsupportedOperationExceptionCls, "startNodeJS Not Supported.");
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1pumpMessageLoop
  (JNIEnv * env, jclass, jlong) {
    (env)->ThrowNew(unsupportedOperationExceptionCls, "pumpMessageLoop Not Supported.");
 return false;
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1isRunning
  (JNIEnv *env, jclass, jlong v8RuntimePtr) {
 (env)->ThrowNew(unsupportedOperationExceptionCls, "isRunning Not Supported.");
 return false;
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1isNodeCompatible
  (JNIEnv * env, jclass) {
    (env)->ThrowNew(unsupportedOperationExceptionCls, "isNodeCompatible Not Supported.");
   return false;
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1createIsolate
 (JNIEnv *env, jobject v8, jstring globalAlias) {
    V8Runtime* runtime = new V8Runtime();
    v8::Isolate::CreateParams create_params;
    create_params.array_buffer_allocator = v8::ArrayBuffer::Allocator::NewDefaultAllocator();
    runtime->isolate = v8::Isolate::New(create_params);
    Locker locker(runtime->isolate);
    v8::Isolate::Scope isolate_scope(runtime->isolate);
    runtime->v8 = env->NewGlobalRef(v8);
    runtime->pendingException = nullptr;
    HandleScope handle_scope(runtime->isolate);
    Handle<ObjectTemplate> globalObject = ObjectTemplate::New(runtime->isolate);
    if (globalAlias == nullptr) {
      Handle<Context> context = Context::New(runtime->isolate, nullptr, globalObject);
      runtime->context_.Reset(runtime->isolate, context);
      runtime->globalObject = new Persistent<Object>;
      runtime->globalObject->Reset(runtime->isolate, context->Global()->GetPrototype()->ToObject(context).ToLocalChecked());
    }
    else {
      Local<String> utfAlias = createV8String(env, runtime->isolate, globalAlias);
      globalObject->SetAccessor(utfAlias, jsWindowObjectAccessor);
      Handle<Context> context = Context::New(runtime->isolate, nullptr, globalObject);
      runtime->context_.Reset(runtime->isolate, context);
      runtime->globalObject = new Persistent<Object>;
      runtime->globalObject->Reset(runtime->isolate, context->Global()->GetPrototype()->ToObject(context).ToLocalChecked());
    }
    return reinterpret_cast<jlong>(runtime);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1createInspector
  (JNIEnv *env, jobject, jlong v8RuntimePtr, jobject inspectorDelegateObj, jstring jcontextName) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)

  runtime->inspector = new V8Inspector();
  runtime->inspector->delegate = env->NewGlobalRef(inspectorDelegateObj);
  
  InspectorDelegate* delegate = new InspectorDelegate(
    std::bind(&V8Inspector::onResponse, runtime->inspector, std::placeholders::_1),
    std::bind(&V8Inspector::waitFrontendMessage, runtime->inspector)
  );

  std::string contextName = jcontextName != nullptr ? createString(env, runtime->isolate, jcontextName) : "";
  runtime->inspector->client = new V8InspectorClientImpl(runtime->isolate, v8Platform, delegate, contextName);

  return reinterpret_cast<jlong>(runtime->inspector);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1dispatchProtocolMessage
  (JNIEnv *env, jobject, jlong v8RuntimePtr, jlong v8InspectorPtr, jstring protocolMessage) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, )
  V8Inspector* inspector = reinterpret_cast<V8Inspector*>(v8InspectorPtr);
  inspector->dispatchProtocolMessage(createString(env, isolate, protocolMessage));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1schedulePauseOnNextStatement
  (JNIEnv *env, jobject, jlong v8RuntimePtr, jlong v8InspectorPtr, jstring reason) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, )
  V8Inspector* inspector = reinterpret_cast<V8Inspector*>(v8InspectorPtr);
  inspector->schedulePauseOnNextStatement(createString(env, isolate, reason));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1acquireLock
  (JNIEnv *env, jobject, jlong v8RuntimePtr) {
  V8Runtime* runtime = reinterpret_cast<V8Runtime*>(v8RuntimePtr);
  if(runtime->isolate->InContext()) {
    jstring exceptionString = env->NewStringUTF("Cannot acquire lock while in a V8 Context");
    jthrowable exception = static_cast<jthrowable>(env->NewObject(v8RuntimeExceptionCls, v8RuntimeExceptionInitMethodID, exceptionString));
    (env)->Throw(exception);
    env->DeleteLocalRef(exceptionString);
    return;
  }
  runtime->locker = new Locker(runtime->isolate);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1releaseLock
  (JNIEnv *env, jobject, jlong v8RuntimePtr) {
  V8Runtime* runtime = reinterpret_cast<V8Runtime*>(v8RuntimePtr);
    if(runtime->isolate->InContext()) {
    jstring exceptionString = env->NewStringUTF("Cannot release lock while in a V8 Context");
    jthrowable exception = static_cast<jthrowable>(env->NewObject(v8RuntimeExceptionCls, v8RuntimeExceptionInitMethodID, exceptionString));
    (env)->Throw(exception);
    env->DeleteLocalRef(exceptionString);
    return;
  }
  delete(runtime->locker);
  runtime->locker = nullptr;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1lowMemoryNotification
  (JNIEnv*, jobject, jlong v8RuntimePtr) {
  V8Runtime* runtime = reinterpret_cast<V8Runtime*>(v8RuntimePtr);
  runtime->isolate->LowMemoryNotification();
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initEmptyContainer
(JNIEnv *env, jobject, jlong v8RuntimePtr) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Persistent<Object>* container = new Persistent<Object>;
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Object
(JNIEnv *env, jobject, jlong v8RuntimePtr) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Local<Object> obj = Object::New(isolate);
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, obj);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1getGlobalObject
  (JNIEnv *env, jobject, jlong v8RuntimePtr) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  return reinterpret_cast<jlong>(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->globalObject);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1createTwin
  (JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jlong twinObjectHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, )
  Handle<Object> obj = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  reinterpret_cast<Persistent<Object>*>(twinObjectHandle)->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, obj);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Array
(JNIEnv *env, jobject, jlong v8RuntimePtr) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Local<Array> array = Array::New(isolate);
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, array);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Int8Array
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong bufferHandle, jint offset, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<ArrayBuffer> arrayBuffer = Local<ArrayBuffer>::New(isolate, *reinterpret_cast<Persistent<ArrayBuffer>*>(bufferHandle));
  Local<Int8Array> array = Int8Array::New(arrayBuffer, static_cast<size_t>(offset), static_cast<size_t>(length));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, array);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8UInt8Array
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong bufferHandle, jint offset, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<ArrayBuffer> arrayBuffer = Local<ArrayBuffer>::New(isolate, *reinterpret_cast<Persistent<ArrayBuffer>*>(bufferHandle));
  Local<Uint8Array> array = Uint8Array::New(arrayBuffer, static_cast<size_t>(offset), static_cast<size_t>(length));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, array);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8UInt8ClampedArray
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong bufferHandle, jint offset, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<ArrayBuffer> arrayBuffer = Local<ArrayBuffer>::New(isolate, *reinterpret_cast<Persistent<ArrayBuffer>*>(bufferHandle));
  Local<Uint8ClampedArray> array = Uint8ClampedArray::New(arrayBuffer, static_cast<size_t>(offset), static_cast<size_t>(length));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, array);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Int32Array
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong bufferHandle, jint offset, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<ArrayBuffer> arrayBuffer = Local<ArrayBuffer>::New(isolate, *reinterpret_cast<Persistent<ArrayBuffer>*>(bufferHandle));
  Local<Int32Array> array = Int32Array::New(arrayBuffer, static_cast<size_t>(offset), static_cast<size_t>(length));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, array);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8UInt32Array
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong bufferHandle, jint offset, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<ArrayBuffer> arrayBuffer = Local<ArrayBuffer>::New(isolate, *reinterpret_cast<Persistent<ArrayBuffer>*>(bufferHandle));
  Local<Uint32Array> array = Uint32Array::New(arrayBuffer, static_cast<size_t>(offset), static_cast<size_t>(length));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, array);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8UInt16Array
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong bufferHandle, jint offset, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<ArrayBuffer> arrayBuffer = Local<ArrayBuffer>::New(isolate, *reinterpret_cast<Persistent<ArrayBuffer>*>(bufferHandle));
  Local<Uint16Array> array = Uint16Array::New(arrayBuffer, static_cast<size_t>(offset), static_cast<size_t>(length));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, array);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Int16Array
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong bufferHandle, jint offset, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<ArrayBuffer> arrayBuffer = Local<ArrayBuffer>::New(isolate, *reinterpret_cast<Persistent<ArrayBuffer>*>(bufferHandle));
  Local<Int16Array> array = Int16Array::New(arrayBuffer, static_cast<size_t>(offset), static_cast<size_t>(length));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, array);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Float32Array
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong bufferHandle, jint offset, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<ArrayBuffer> arrayBuffer = Local<ArrayBuffer>::New(isolate, *reinterpret_cast<Persistent<ArrayBuffer>*>(bufferHandle));
  Local<Float32Array> array = Float32Array::New(arrayBuffer, static_cast<size_t>(offset), static_cast<size_t>(length));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, array);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Float64Array
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong bufferHandle, jint offset, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<ArrayBuffer> arrayBuffer = Local<ArrayBuffer>::New(isolate, *reinterpret_cast<Persistent<ArrayBuffer>*>(bufferHandle));
  Local<Float64Array> array = Float64Array::New(arrayBuffer, static_cast<size_t>(offset), static_cast<size_t>(length));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, array);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8ArrayBuffer__JI
(JNIEnv *env, jobject, jlong v8RuntimePtr, jint capacity) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  std::unique_ptr<v8::BackingStore> backing_store = v8::ArrayBuffer::NewBackingStore(isolate, static_cast<size_t>(capacity));
  Local<ArrayBuffer> arrayBuffer = ArrayBuffer::New(isolate, std::move(backing_store));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, arrayBuffer);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1initNewV8ArrayBuffer__JLjava_nio_ByteBuffer_2I
(JNIEnv *env, jobject, jlong v8RuntimePtr, jobject byteBuffer, jint capacity) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  std::unique_ptr<v8::BackingStore> backing_store = ArrayBuffer::NewBackingStore(
    env->GetDirectBufferAddress(byteBuffer),
    static_cast<size_t>(capacity),
    [](void*, size_t, void*){},
    nullptr
  );
  Local<ArrayBuffer> arrayBuffer = ArrayBuffer::New(isolate, std::move(backing_store));
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, arrayBuffer);
  return reinterpret_cast<jlong>(container);
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1createV8ArrayBufferBackingStore
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jint capacity) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, nullptr)
  Handle<ArrayBuffer> arrayBuffer = Local<ArrayBuffer>::New(isolate, *reinterpret_cast<Persistent<ArrayBuffer>*>(objectHandle));
  void* dataPtr = arrayBuffer->GetBackingStore()->Data();
  jobject byteBuffer = env->NewDirectByteBuffer(dataPtr, capacity);
  return byteBuffer;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1release
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle) {
  if (v8RuntimePtr == 0) {
    return;
  }
  Isolate* isolate = getIsolate(env, v8RuntimePtr);
  Locker locker(isolate);
  HandleScope handle_scope(isolate);
  reinterpret_cast<Persistent<Object>*>(objectHandle)->Reset();
  delete(reinterpret_cast<Persistent<Object>*>(objectHandle));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1terminateExecution
  (JNIEnv * env, jobject, jlong v8RuntimePtr) {
	if (v8RuntimePtr == 0) {
	  return;
	}
	Isolate* isolate = getIsolate(env, v8RuntimePtr);
	isolate->TerminateExecution();
	return;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1releaseRuntime
(JNIEnv *env, jobject, jlong v8RuntimePtr) {
  if (v8RuntimePtr == 0) {
    return;
  }
  Isolate* isolate = getIsolate(env, v8RuntimePtr);
  reinterpret_cast<V8Runtime*>(v8RuntimePtr)->context_.Reset();
  reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate->Dispose();
  env->DeleteGlobalRef(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->v8);
  V8Runtime* runtime = reinterpret_cast<V8Runtime*>(v8RuntimePtr);
  delete(runtime);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1contains
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, false);
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  Local<String> v8Key = createV8String(env, isolate, key);
  return object->Has(context, v8Key).FromMaybe(false);
}

JNIEXPORT jobjectArray JNICALL Java_com_eclipsesource_v8_V8__1getKeys
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL);
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  MaybeLocal<Array> properties = object->GetOwnPropertyNames(context);
  if (!properties.IsEmpty()) {
    int size = properties.ToLocalChecked()->Length();
    jobjectArray keys = (env)->NewObjectArray(size, stringCls, nullptr);
    for (int i = 0; i < size; i++) {
      MaybeLocal<Value> property = properties.ToLocalChecked()->Get(context, i);
      if (property.IsEmpty()) {
        continue;
      }
      String::Value unicodeString(isolate, property.ToLocalChecked());
      jobject key = (env)->NewString(*unicodeString, unicodeString.length());
      (env)->SetObjectArrayElement(keys, i, key);
      (env)->DeleteLocalRef(key);
    }
    return keys;
  }
  return (env)->NewObjectArray(0, stringCls, nullptr);
}

ScriptOrigin* createScriptOrigin(JNIEnv * env, Isolate* isolate, jstring &jscriptName, jint jlineNumber = 0) {
  Local<String> scriptName = createV8String(env, isolate, jscriptName);
  return new ScriptOrigin(isolate, scriptName, jlineNumber);
}

bool compileScript(const Local<Context>& context, Isolate *isolate, jstring &jscript, JNIEnv *env, jstring jscriptName, jint &jlineNumber, Local<Script> &script, TryCatch* tryCatch) {
  Local<String> source = createV8String(env, isolate, jscript);
  ScriptOrigin* scriptOriginPtr = nullptr;
  if (jscriptName != nullptr) {
    scriptOriginPtr = createScriptOrigin(env, isolate, jscriptName, jlineNumber);
  }
  MaybeLocal<Script> script_result = Script::Compile(context, source, scriptOriginPtr);
  if (!script_result.IsEmpty()) {
      script = script_result.ToLocalChecked();
      if (scriptOriginPtr != nullptr) {
        delete(scriptOriginPtr);
      }
  }
  if (tryCatch->HasCaught()) {
    throwParseException(env, context, isolate, tryCatch);
    return false;
  }
  return true;
}

bool runScript(const Local<Context>& context, Isolate* isolate, JNIEnv *env, Local<Script> *script, TryCatch* tryCatch, jlong v8RuntimePtr) {
  (*script)->Run(context);
  if (tryCatch->HasCaught()) {
    throwExecutionException(env, context, isolate, tryCatch, v8RuntimePtr);
    return false;
  }
  return true;
}

bool runScript(const Local<Context>& context, Isolate* isolate, JNIEnv *env, Local<Script> *script, TryCatch* tryCatch, Local<Value> &result, jlong v8RuntimePtr) {
  MaybeLocal<Value> local_result = (*script)->Run(context);
  if (!local_result.IsEmpty()) {
    result = local_result.ToLocalChecked();
    return true;
  }
  if (tryCatch->HasCaught()) {
    throwExecutionException(env, context, isolate, tryCatch, v8RuntimePtr);
    return false;
  }
  return true;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeVoidScript
(JNIEnv * env, jobject v8, jlong v8RuntimePtr, jstring jjstring, jstring jscriptName = nullptr, jint jlineNumber = 0) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, )
  TryCatch tryCatch(isolate);
  Local<Script> script;
  if (!compileScript(context, isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch))
    return;
  runScript(context, isolate, env, &script, &tryCatch, v8RuntimePtr);
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1executeDoubleScript
(JNIEnv * env, jobject v8, jlong v8RuntimePtr, jstring jjstring, jstring jscriptName = nullptr, jint jlineNumber = 0) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  TryCatch tryCatch(isolate);
  Local<Script> script;
  Local<Value> result;
  if (!compileScript(context, isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch))
    return 0;
  if (!runScript(context, isolate, env, &script, &tryCatch, result, v8RuntimePtr))
    return 0;
  ASSERT_IS_NUMBER(result);
  return result->NumberValue(context).FromMaybe(0);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1executeBooleanScript
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jstring jjstring, jstring jscriptName = nullptr, jint jlineNumber = 0) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, false)
  TryCatch tryCatch(isolate);
  Local<Script> script;
  Local<Value> result;
  if (!compileScript(context, isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch))
    return false;
  if (!runScript(context, isolate, env, &script, &tryCatch, result, v8RuntimePtr))
    return false;
  ASSERT_IS_BOOLEAN(result);
  return result->BooleanValue(isolate);
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1executeStringScript
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jstring jjstring, jstring jscriptName = nullptr, jint jlineNumber = 0) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL)
  TryCatch tryCatch(isolate);
  Local<Script> script;
  Local<Value> result;
  if (!compileScript(context, isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch))
    return nullptr;
  if (!runScript(context, isolate, env, &script, &tryCatch, result, v8RuntimePtr))
    return nullptr;
  ASSERT_IS_STRING(result)
  String::Value unicodeString(isolate, result);

  return env->NewString(*unicodeString, unicodeString.length());
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1executeIntegerScript
(JNIEnv * env, jobject v8, jlong v8RuntimePtr, jstring jjstring, jstring jscriptName = nullptr, jint jlineNumber = 0) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  TryCatch tryCatch(isolate);
  Local<Script> script;
  Local<Value> result;
  if (!compileScript(context, isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch))
    return 0;
  if (!runScript(context, isolate, env, &script, &tryCatch, result, v8RuntimePtr))
    return 0;
  ASSERT_IS_NUMBER(result);
  return result->Int32Value(context).FromJust();
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1executeScript
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jint expectedType, jstring jjstring, jstring jscriptName = nullptr, jint jlineNumber = 0) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL)
  TryCatch tryCatch(isolate);
  Local<Script> script;
  Local<Value> result;
  if (!compileScript(context, isolate, jjstring, env, jscriptName, jlineNumber, script, &tryCatch)) { return nullptr; }
  if (!runScript(context, isolate, env, &script, &tryCatch, result, v8RuntimePtr)) { return nullptr; }
  return getResult(env, context, v8, v8RuntimePtr, result, expectedType);
}

bool invokeFunction(JNIEnv *env, const Local<Context>& context, Isolate* isolate, jlong &v8RuntimePtr, jlong &receiverHandle, jlong &functionHandle, jlong &parameterHandle, Handle<Value> &result) {
  int size = 0;
  Handle<Value>* args = nullptr;
  if (parameterHandle != 0) {
    Handle<Object> parameters = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(parameterHandle));
    size = Array::Cast(*parameters)->Length();
    args = new Handle<Value>[size];
    for (int i = 0; i < size; i++) {
      args[i] = parameters->Get(context, i).ToLocalChecked();
    }
  }
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(functionHandle));
  Handle<Object> receiver = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(receiverHandle));
  Handle<Function> func = Handle<Function>::Cast(object);
  TryCatch tryCatch(isolate);
  MaybeLocal<Value> function_call_result = func->Call(context, receiver, size, args);
  if (!function_call_result.IsEmpty()) {
      result = function_call_result.ToLocalChecked();
  }
  if (args != nullptr) {
    delete(args);
  }
  if (tryCatch.HasCaught()) {
    throwExecutionException(env, context, isolate, &tryCatch, v8RuntimePtr);
    return false;
  }
  return true;
}

bool invokeFunction(JNIEnv *env, const Local<Context>& context, Isolate* isolate, jlong &v8RuntimePtr, jlong &objectHandle, jstring &jfunctionName, jlong &parameterHandle, Handle<Value> &result) {
  Local<String> functionName = createV8String(env, isolate, jfunctionName);
  Handle<Object> parentObject = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  int size = 0;
  Handle<Value>* args = nullptr;
  if (parameterHandle != 0) {
    Handle<Object> parameters = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(parameterHandle));
    size = Array::Cast(*parameters)->Length();
    args = new Handle<Value>[size];
    for (int i = 0; i < size; i++) {
      args[i] = parameters->Get(context, i).ToLocalChecked();
    }
  }
  TryCatch tryCatch(isolate);
  MaybeLocal<Value> result_value = parentObject->Get(context, functionName);
  if (!result_value.IsEmpty()) {
      Handle<Value> value = result_value.ToLocalChecked();
      Handle<Function> func = Handle<Function>::Cast(value);
      MaybeLocal<Value> function_call_result = func->Call(context, parentObject, size, args);
      if (!function_call_result.IsEmpty()) {
        result = function_call_result.ToLocalChecked();
      }
  }
  if (args != nullptr) {
    delete(args);
  }
  if (tryCatch.HasCaught()) {
    throwExecutionException(env, context, isolate, &tryCatch, v8RuntimePtr);
    return false;
  }
  return true;
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1executeFunction__JJJJ
  (JNIEnv *env, jobject v8, jlong v8RuntimePtr, jlong receiverHandle, jlong functionHandle, jlong parameterHandle) {
    Isolate* isolate = SETUP(env, v8RuntimePtr, NULL)
    Handle<Value> result;
    if (!invokeFunction(env, context, isolate, v8RuntimePtr, receiverHandle, functionHandle, parameterHandle, result))
        return nullptr;
    return getResult(env, context, v8, v8RuntimePtr, result, com_eclipsesource_v8_V8_UNKNOWN);
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1executeFunction__JIJLjava_lang_String_2J
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jint expectedType, jlong objectHandle, jstring jfunctionName, jlong parameterHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL)
  Handle<Value> result;
  if (!invokeFunction(env, context, isolate, v8RuntimePtr, objectHandle, jfunctionName, parameterHandle, result))
    return nullptr;
  return getResult(env, context, v8, v8RuntimePtr, result, expectedType);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1executeIntegerFunction
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jlong objectHandle, jstring jfunctionName, jlong parameterHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<Value> result;
  if (!invokeFunction(env, context, isolate, v8RuntimePtr, objectHandle, jfunctionName, parameterHandle, result))
    return 0;
  ASSERT_IS_NUMBER(result);
  return result->Int32Value(context).FromJust();
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1executeDoubleFunction
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jlong objectHandle, jstring jfunctionName, jlong parameterHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Value> result;
  if (!invokeFunction(env, context, isolate, v8RuntimePtr, objectHandle, jfunctionName, parameterHandle, result))
    return 0;
  ASSERT_IS_NUMBER(result);
  return result->NumberValue(context).FromJust();
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1executeBooleanFunction
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jlong objectHandle, jstring jfunctionName, jlong parameterHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, false);
  Handle<Value> result;
  if (!invokeFunction(env, context, isolate, v8RuntimePtr, objectHandle, jfunctionName, parameterHandle, result))
    return false;
  ASSERT_IS_BOOLEAN(result);
  return result->BooleanValue(isolate);
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1executeStringFunction
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jlong objectHandle, jstring jfunctionName, jlong parameterHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL)
  Handle<Value> result;
  if (!invokeFunction(env, context, isolate, v8RuntimePtr, objectHandle, jfunctionName, parameterHandle, result))
    return nullptr;
  ASSERT_IS_STRING(result)
  String::Value unicodeString(isolate, result->ToString(context).ToLocalChecked());

  return env->NewString(*unicodeString, unicodeString.length());
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1executeVoidFunction
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jlong objectHandle, jstring jfunctionName, jlong parameterHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  Handle<Value> result;
  invokeFunction(env, context, isolate, v8RuntimePtr, objectHandle, jfunctionName, parameterHandle, result);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addUndefined
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  addValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key, Undefined(isolate));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addNull
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  addValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key, Null(isolate));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__JJLjava_lang_String_2I
(JNIEnv * env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key, jint value) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  addValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key, Int32::New(isolate, value));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__JJLjava_lang_String_2D
(JNIEnv * env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key, jdouble value) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  addValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key, Number::New(isolate, value));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__JJLjava_lang_String_2Ljava_lang_String_2
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key, jstring value) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  Handle<Value> v8Value = createV8String(env, isolate, value);
  addValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1add__JJLjava_lang_String_2Z
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key, jboolean value) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  addValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key, Boolean::New(isolate, value));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addObject
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key, jlong valueHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  Handle<Value> value = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(valueHandle));
  addValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key, value);
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1get
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jint expectedType, jlong objectHandle, jstring key) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, nullptr)
  Handle<Value> result = getValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key);
  return getResult(env, context, v8, v8RuntimePtr, result, expectedType);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getInteger
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Value> v8Value = getValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key);
  ASSERT_IS_NUMBER(v8Value);
  return v8Value->Int32Value(context).FromJust();
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1getDouble
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Value> v8Value = getValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key);
  ASSERT_IS_NUMBER(v8Value);
  return v8Value->NumberValue(context).FromJust();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1getString
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Value> v8Value = getValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key);
  ASSERT_IS_STRING(v8Value);
  String::Value unicode(isolate, v8Value);

  return env->NewString(*unicode, unicode.length());
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1getBoolean
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, false);
  Handle<Value> v8Value = getValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key);
  ASSERT_IS_BOOLEAN(v8Value);
  return v8Value->BooleanValue(isolate);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getType__JJLjava_lang_String_2
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring key) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Value> v8Value = getValueWithKey(env, context, isolate, v8RuntimePtr, objectHandle, key);
  int type = getType(v8Value);
  if (type < 0) {
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

bool isNumber(int type1, int type2) {
  return isNumber(type1) && isNumber(type2);
}

bool isObject(int type1, int type2) {
  return isObject(type1) && isObject(type2);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getArrayType
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  int length = 0;
  if ( array->IsTypedArray() ) {
      if ( array->IsFloat64Array() ) {
        return com_eclipsesource_v8_V8_DOUBLE;
      } else if ( array->IsFloat32Array() ) {
        return com_eclipsesource_v8_V8_FLOAT_32_ARRAY;
      } else if ( array->IsInt32Array() ) {
        return com_eclipsesource_v8_V8_INT_32_ARRAY;
      } else if ( array->IsUint32Array() ) {
        return com_eclipsesource_v8_V8_UNSIGNED_INT_32_ARRAY;
      } else if ( array->IsInt16Array() ) {
        return com_eclipsesource_v8_V8_INT_16_ARRAY;
      } else if ( array->IsUint16Array() ) {
        return com_eclipsesource_v8_V8_UNSIGNED_INT_16_ARRAY;
      } else if ( array->IsInt8Array() ) {
        return com_eclipsesource_v8_V8_INT_8_ARRAY;
      } else if ( array->IsUint8Array() ) {
        return com_eclipsesource_v8_V8_UNSIGNED_INT_8_ARRAY;
      } else if ( array->IsUint8ClampedArray() ) {
        return com_eclipsesource_v8_V8_UNSIGNED_INT_8_CLAMPED_ARRAY;
      }
      return com_eclipsesource_v8_V8_INTEGER;
  } else {
      length = Array::Cast(*array)->Length();
  }
  int arrayType = com_eclipsesource_v8_V8_UNDEFINED;
  for (int index = 0; index < length; index++) {
    int type = getType(array->Get(context, index).ToLocalChecked());
    if (type < 0) {
      throwResultUndefinedException(env, "");
    }
    else if (index == 0) {
      arrayType = type;
    }
    else if (type == arrayType) {
      // continue
    }
    else if (isNumber(arrayType, type)) {
      arrayType = com_eclipsesource_v8_V8_DOUBLE;
    }
    else if (isObject(arrayType, type)) {
      arrayType = com_eclipsesource_v8_V8_V8_OBJECT;
    }
    else {
      return com_eclipsesource_v8_V8_UNDEFINED;
    }
  }
  return arrayType;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetSize
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  if ( array->IsTypedArray() ) {
      return (jint) TypedArray::Cast(*array)->Length();
  }
  return Array::Cast(*array)->Length();
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetInteger
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint index) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  Handle<Value> v8Value = array->Get(context, index).ToLocalChecked();
  ASSERT_IS_NUMBER(v8Value);
  return v8Value->Int32Value(context).FromJust();
}

int fillIntArray(JNIEnv *env, const Local<Context> context, Handle<Object> &array, int start, int length, jintArray &result) {
  jint * fill = new jint[length];
  for (int i = start; i < start + length; i++) {
    Handle<Value> v8Value = array->Get(context, i).ToLocalChecked();
    ASSERT_IS_NUMBER(v8Value);
    fill[i - start] = v8Value->Int32Value(context).FromJust();
  }
  (env)->SetIntArrayRegion(result, 0, length, fill);
  delete[] fill;
  return length;
}

int fillDoubleArray(JNIEnv *env, const Local<Context> context, Handle<Object> &array, int start, int length, jdoubleArray &result) {
  jdouble * fill = new jdouble[length];
  for (int i = start; i < start + length; i++) {
    Handle<Value> v8Value = array->Get(context, i).ToLocalChecked();
    ASSERT_IS_NUMBER(v8Value);
    fill[i - start] = v8Value->NumberValue(context).FromJust();
  }
  (env)->SetDoubleArrayRegion(result, 0, length, fill);
  delete[] fill;
  return length;
}

int fillByteArray(JNIEnv *env, const Local<Context> context, Handle<Object> &array, int start, int length, jbyteArray &result) {
  jbyte * fill = new jbyte[length];
  for (int i = start; i < start + length; i++) {
    Handle<Value> v8Value = array->Get(context, i).ToLocalChecked();
    ASSERT_IS_NUMBER(v8Value);
    fill[i - start] = (jbyte)v8Value->Int32Value(context).FromJust();
  }
  (env)->SetByteArrayRegion(result, 0, length, fill);
  delete[] fill;
  return length;
}

int fillBooleanArray(JNIEnv *env, const Local<Context> context, Isolate* isolate, Handle<Object> &array, int start, int length, jbooleanArray &result) {
  jboolean * fill = new jboolean[length];
  for (int i = start; i < start + length; i++) {
    Handle<Value> v8Value = array->Get(context, i).ToLocalChecked();
    ASSERT_IS_BOOLEAN(v8Value);
    fill[i - start] = v8Value->BooleanValue(isolate);
  }
  (env)->SetBooleanArrayRegion(result, 0, length, fill);
  delete[] fill;
  return length;
}

int fillStringArray(JNIEnv *env, const Local<Context> context, Isolate* isolate, Handle<Object> &array, int start, int length, jobjectArray &result) {
  for (int i = start; i < start + length; i++) {
    Handle<Value> v8Value = array->Get(context, i).ToLocalChecked();
    ASSERT_IS_STRING(v8Value);
    String::Value unicodeString(isolate, v8Value);
    jstring string = env->NewString(*unicodeString, unicodeString.length());
    env->SetObjectArrayElement(result, i - start, string);
    (env)->DeleteLocalRef(string);
  }

  return length;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetIntegers__JJII_3I
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length, jintArray result) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));

  return fillIntArray(env, context, array, start, length, result);
}

JNIEXPORT jintArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetIntegers__JJII
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  jintArray result = env->NewIntArray(length);
  fillIntArray(env, context, array, start, length, result);

  return result;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetDoubles__JJII_3D
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length, jdoubleArray result) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  return fillDoubleArray(env, context, array, start, length, result);
}

JNIEXPORT jdoubleArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetDoubles__JJII
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  jdoubleArray result = env->NewDoubleArray(length);
  fillDoubleArray(env, context, array, start, length, result);
  return result;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetBooleans__JJII_3Z
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length, jbooleanArray result) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  return fillBooleanArray(env, context, isolate, array, start, length, result);
}

JNIEXPORT jbyteArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetBytes__JJII
  (JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  jbyteArray result = env->NewByteArray(length);
  fillByteArray(env, context, array, start, length, result);
  return result;
}

JNIEXPORT jbooleanArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetBooleans__JJII
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  jbooleanArray result = env->NewBooleanArray(length);
  fillBooleanArray(env, context, isolate, array, start, length, result);
  return result;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetBytes__JJII_3B
  (JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length, jbyteArray result) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  return fillByteArray(env, context, array, start, length, result);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1arrayGetStrings__JJII_3Ljava_lang_String_2
(JNIEnv * env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length, jobjectArray result) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));

  return fillStringArray(env, context, isolate, array, start, length, result);
}

JNIEXPORT jobjectArray JNICALL Java_com_eclipsesource_v8_V8__1arrayGetStrings__JJII
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  jobjectArray result = env->NewObjectArray(length, stringCls, NULL);
  fillStringArray(env, context, isolate, array, start, length, result);

  return result;
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1arrayGetBoolean
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint index) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, false);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  Handle<Value> v8Value = array->Get(context, index).ToLocalChecked();
  ASSERT_IS_BOOLEAN(v8Value);
  return v8Value->BooleanValue(isolate);
}

JNIEXPORT jbyte JNICALL Java_com_eclipsesource_v8_V8__1arrayGetByte
  (JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint index) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, false);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  Handle<Value> v8Value = array->Get(context, index).ToLocalChecked();
  ASSERT_IS_NUMBER(v8Value);
  return v8Value->Int32Value(context).FromJust();
}

JNIEXPORT jdouble JNICALL Java_com_eclipsesource_v8_V8__1arrayGetDouble
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint index) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  Handle<Value> v8Value = array->Get(context, index).ToLocalChecked();
  ASSERT_IS_NUMBER(v8Value);
  return v8Value->NumberValue(context).FromJust();
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1arrayGetString
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint index) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  Handle<Value> v8Value = array->Get(context, index).ToLocalChecked();
  ASSERT_IS_STRING(v8Value);
  String::Value unicodeString(isolate, v8Value);

  return env->NewString(*unicodeString, unicodeString.length());
}

JNIEXPORT jobject JNICALL Java_com_eclipsesource_v8_V8__1arrayGet
(JNIEnv *env, jobject v8, jlong v8RuntimePtr, jint expectedType, jlong arrayHandle, jint index) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, NULL);
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  Handle<Value> result = array->Get(context, static_cast<uint32_t>(index)).ToLocalChecked();
  return getResult(env, context, v8, v8RuntimePtr, result, expectedType);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayNullItem
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  if ( array->IsTypedArray() ) {
     Local<String> string = String::NewFromUtf8(isolate, "Cannot push to a Typed Array.").ToLocalChecked();
     v8::String::Value strValue(isolate, string);
     throwV8RuntimeException(env, &strValue);
     return;
  }
  uint32_t index = Array::Cast(*array)->Length();
  array->Set(context, index, Null(isolate));
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayUndefinedItem
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  if ( array->IsTypedArray() ) {
     Local<String> string = String::NewFromUtf8(isolate, "Cannot push to a Typed Array.").ToLocalChecked();
     v8::String::Value strValue(isolate, string);
     throwV8RuntimeException(env, &strValue);
     return;
  }
  int index = Array::Cast(*array)->Length();
  array->Set(context, index, Undefined(isolate));
}


JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayIntItem
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint value) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  if ( array->IsTypedArray() ) {
     Local<String> string = String::NewFromUtf8(isolate, "Cannot push to a Typed Array.").ToLocalChecked();
     v8::String::Value strValue(isolate, string);
     throwV8RuntimeException(env, &strValue);
     return;
  }
  Local<Value> v8Value = Int32::New(isolate, value);
  int index = Array::Cast(*array)->Length();
  array->Set(context, index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayDoubleItem
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jdouble value) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  if ( array->IsTypedArray() ) {
     Local<String> string = String::NewFromUtf8(isolate, "Cannot push to a Typed Array.").ToLocalChecked();
     v8::String::Value strValue(isolate, string);
     throwV8RuntimeException(env, &strValue);
     return;
  }
  Local<Value> v8Value = Number::New(isolate, value);
  int index = Array::Cast(*array)->Length();
  array->Set(context, index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayBooleanItem
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jboolean value) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  if ( array->IsTypedArray() ) {
     Local<String> string = String::NewFromUtf8(isolate, "Cannot push to a Typed Array.").ToLocalChecked();
     v8::String::Value strValue(isolate, string);
     throwV8RuntimeException(env, &strValue);
     return;
  }
  Local<Value> v8Value = Boolean::New(isolate, value);
  int index = Array::Cast(*array)->Length();
  array->Set(context, index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayStringItem
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jstring value) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  if ( array->IsTypedArray() ) {
     Local<String> string = String::NewFromUtf8(isolate, "Cannot push to a Typed Array.").ToLocalChecked();
     v8::String::Value strValue(isolate, string);
     throwV8RuntimeException(env, &strValue);
     return;
  }
  int index = Array::Cast(*array)->Length();
  Local<String> v8Value = createV8String(env, isolate, value);
  array->Set(context, index, v8Value);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1addArrayObjectItem
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jlong valueHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, );
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  if ( array->IsTypedArray() ) {
     Local<String> string = String::NewFromUtf8(isolate, "Cannot push to a Typed Array.").ToLocalChecked();
     v8::String::Value strValue(isolate, string);
     throwV8RuntimeException(env, &strValue);
     return;
  }
  int index = Array::Cast(*array)->Length();
  Local<Value> v8Value = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(valueHandle));
  array->Set(context, index, v8Value);
}

int getType(Handle<Value> v8Value) {
  if (v8Value.IsEmpty() || v8Value->IsUndefined()) {
    return com_eclipsesource_v8_V8_UNDEFINED;
  }
  else if (v8Value->IsNull()) {
    return com_eclipsesource_v8_V8_NULL;
  }
  else if (v8Value->IsInt32()) {
    return com_eclipsesource_v8_V8_INTEGER;
  }
  else if (v8Value->IsNumber()) {
    return com_eclipsesource_v8_V8_DOUBLE;
  }
  else if (v8Value->IsBoolean()) {
    return com_eclipsesource_v8_V8_BOOLEAN;
  }
  else if (v8Value->IsString()) {
    return com_eclipsesource_v8_V8_STRING;
  }
  else if (v8Value->IsFunction()) {
    return com_eclipsesource_v8_V8_V8_FUNCTION;
  }
  else if (v8Value->IsArrayBuffer()) {
    return com_eclipsesource_v8_V8_V8_ARRAY_BUFFER;
  }
  else if (v8Value->IsTypedArray()) {
    return com_eclipsesource_v8_V8_V8_TYPED_ARRAY;
  }
  else if (v8Value->IsArray()) {
    return com_eclipsesource_v8_V8_V8_ARRAY;
  }
  else if (v8Value->IsObject()) {
    return com_eclipsesource_v8_V8_V8_OBJECT;
  }
  return -1;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getType__JJI
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jint index) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  Handle<Value> v8Value = array->Get(context, static_cast<uint32_t>(index)).ToLocalChecked();
  int type = getType(v8Value);
  if (type < 0) {
    throwResultUndefinedException(env, "");
  }
  return type;
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getType__JJ
  (JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<Value> v8Value = Local<Value>::New(isolate, *reinterpret_cast<Persistent<Value>*>(objectHandle));
  return getType(v8Value);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1getType__JJII
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong arrayHandle, jint start, jint length) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  Handle<Object> array = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(arrayHandle));
  int result = -1;
  for (int i = start; i < start + length; i++) {
    Handle<Value> v8Value = array->Get(context, i).ToLocalChecked();
    int type = getType(v8Value);
    if (result >= 0 && result != type) {
      throwResultUndefinedException(env, "");
      return -1;
    }
    else if (type < 0) {
      throwResultUndefinedException(env, "");
      return -1;
    }
    result = type;
  }
  if (result < 0) {
    throwResultUndefinedException(env, "");
  }
  return result;
}

jobject createParameterArray(JNIEnv* env, const Local<Context>& context, jlong v8RuntimePtr, jobject v8, int size, const FunctionCallbackInfo<Value>& args) {
  Isolate* isolate = getIsolate(env, v8RuntimePtr);
  jobject result = env->NewObject(v8ArrayCls, v8ArrayInitMethodID, v8);
  jlong parameterHandle = env->CallLongMethod(result, v8ArrayGetHandleMethodID);
  Handle<Object> parameters = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(parameterHandle));
  for (int i = 0; i < size; i++) {
    Maybe<bool> unusedResult = parameters->Set(context, static_cast<uint32_t>(i), args[i]);
    unusedResult.Check();
  }
  return result;
}

void voidCallback(const FunctionCallbackInfo<Value>& args) {
  int size = args.Length();
  Local<External> data = Local<External>::Cast(args.Data());
  void *methodDescriptorPtr = data->Value();
  MethodDescriptor* md = static_cast<MethodDescriptor*>(methodDescriptorPtr);
  V8Runtime* v8Runtime = reinterpret_cast<V8Runtime*>(md->v8RuntimePtr);
  jobject v8 = v8Runtime->v8;
  Isolate* isolate = v8Runtime->isolate;
  Isolate::Scope isolateScope(isolate);
  Local<Context> context = v8Runtime->context_.Get(isolate);
  JNIEnv * env;
  getJNIEnv(env);
  jobject parameters = createParameterArray(env, context, md->v8RuntimePtr, v8, size, args);
  Handle<Value> receiver = args.This();
  jobject jreceiver = getResult(env, context, v8, md->v8RuntimePtr, receiver, com_eclipsesource_v8_V8_UNKNOWN);
  env->CallVoidMethod(v8, v8CallVoidMethodID, md->methodID, jreceiver, parameters);
  if (env->ExceptionCheck()) {
    Isolate* isolate = getIsolate(env, md->v8RuntimePtr);
    reinterpret_cast<V8Runtime*>(md->v8RuntimePtr)->pendingException = env->ExceptionOccurred();
    env->ExceptionClear();
    jstring exceptionMessage = static_cast<jstring>(env->CallObjectMethod(reinterpret_cast<V8Runtime*>(md->v8RuntimePtr)->pendingException, throwableGetMessageMethodID));
    if (exceptionMessage != nullptr) {
      Local<String> v8String = createV8String(env, isolate, exceptionMessage);
      isolate->ThrowException(v8String);
    }
    else {
      isolate->ThrowException(String::NewFromUtf8(isolate, "Unhandled Java Exception").ToLocalChecked());
    }
  }
  env->CallVoidMethod(parameters, v8ArrayReleaseMethodID);
  env->CallVoidMethod(jreceiver, v8ObjectReleaseMethodID);
  env->DeleteLocalRef(jreceiver);
  env->DeleteLocalRef(parameters);
}

int getReturnType(JNIEnv* env, jobject &object) {
  int result = com_eclipsesource_v8_V8_NULL;
  if (env->IsInstanceOf(object, integerCls)) {
    result = com_eclipsesource_v8_V8_INTEGER;
  }
  else if (env->IsInstanceOf(object, doubleCls)) {
    result = com_eclipsesource_v8_V8_DOUBLE;
  }
  else if (env->IsInstanceOf(object, booleanCls)) {
    result = com_eclipsesource_v8_V8_BOOLEAN;
  }
  else if (env->IsInstanceOf(object, stringCls)) {
    result = com_eclipsesource_v8_V8_STRING;
  }
  else if (env->IsInstanceOf(object, v8ArrayCls)) {
    result = com_eclipsesource_v8_V8_V8_ARRAY;
  }
  else if (env->IsInstanceOf(object, v8ObjectCls)) {
    result = com_eclipsesource_v8_V8_V8_OBJECT;
  }
  else if (env->IsInstanceOf(object, v8ArrayBufferCls)) {
    result = com_eclipsesource_v8_V8_V8_ARRAY_BUFFER;
  }
  return result;
}

int getInteger(JNIEnv* env, jobject &object) {
  return env->CallIntMethod(object, integerIntValueMethodID);
}

bool getBoolean(JNIEnv* env, jobject &object) {
  return env->CallBooleanMethod(object, booleanBoolValueMethodID);
}

double getDouble(JNIEnv* env, jobject &object) {
  return env->CallDoubleMethod(object, doubleDoubleValueMethodID);
}

void objectCallback(const FunctionCallbackInfo<Value>& args) {
  int size = args.Length();
  Local<External> data = Local<External>::Cast(args.Data());
  void *methodDescriptorPtr = data->Value();
  MethodDescriptor* md = static_cast<MethodDescriptor*>(methodDescriptorPtr);
  V8Runtime* v8Runtime = reinterpret_cast<V8Runtime*>(md->v8RuntimePtr);
  jobject v8 = v8Runtime->v8;
  Isolate* isolate = v8Runtime->isolate;
  Isolate::Scope isolateScope(isolate);
  Local<Context> context = v8Runtime->context_.Get(isolate);
  JNIEnv * env;
  getJNIEnv(env);
  jobject parameters = createParameterArray(env, context, md->v8RuntimePtr, v8, size, args);
  Handle<Value> receiver = args.This();
  jobject jreceiver = getResult(env, context, v8, md->v8RuntimePtr, receiver, com_eclipsesource_v8_V8_UNKNOWN);
  jobject resultObject = env->CallObjectMethod(v8, v8CallObjectJavaMethodMethodID, md->methodID, jreceiver, parameters);
  if (env->ExceptionCheck()) {
    resultObject = nullptr;
    Isolate* isolate = getIsolate(env, md->v8RuntimePtr);
    reinterpret_cast<V8Runtime*>(md->v8RuntimePtr)->pendingException = env->ExceptionOccurred();
    env->ExceptionClear();
    jstring exceptionMessage = (jstring)env->CallObjectMethod(reinterpret_cast<V8Runtime*>(md->v8RuntimePtr)->pendingException, throwableGetMessageMethodID);
    if (exceptionMessage != nullptr) {
      Local<String> v8String = createV8String(env, isolate, exceptionMessage);
      isolate->ThrowException(v8String);
    }
    else {
      isolate->ThrowException(String::NewFromUtf8(isolate, "Unhandled Java Exception").ToLocalChecked());
    }
  }
  else if (resultObject == nullptr) {
    args.GetReturnValue().SetNull();
  }
  else {
    int returnType = getReturnType(env, resultObject);
    if (returnType == com_eclipsesource_v8_V8_INTEGER) {
      args.GetReturnValue().Set(getInteger(env, resultObject));
    }
    else if (returnType == com_eclipsesource_v8_V8_BOOLEAN) {
      args.GetReturnValue().Set(getBoolean(env, resultObject));
    }
    else if (returnType == com_eclipsesource_v8_V8_DOUBLE) {
      args.GetReturnValue().Set(getDouble(env, resultObject));
    }
    else if (returnType == com_eclipsesource_v8_V8_STRING) {
      jstring stringResult = (jstring)resultObject;
      Local<String> result = createV8String(env, reinterpret_cast<V8Runtime*>(md->v8RuntimePtr)->isolate, stringResult);
      args.GetReturnValue().Set(result);
    }
    else if (returnType == com_eclipsesource_v8_V8_V8_ARRAY) {
      if (isUndefined(env, resultObject)) {
        args.GetReturnValue().SetUndefined();
      }
      else {
        jlong resultHandle = getHandle(env, resultObject);
        Handle<Object> result = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(resultHandle));
        releaseArray(env, resultObject);
        args.GetReturnValue().Set(result);
      }
    }
    else if (returnType == com_eclipsesource_v8_V8_V8_OBJECT) {
      if (isUndefined(env, resultObject)) {
        args.GetReturnValue().SetUndefined();
      }
      else {
        jlong resultHandle = getHandle(env, resultObject);
        Handle<Object> result = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(resultHandle));
        release(env, resultObject);
        args.GetReturnValue().Set(result);
      }
    }
    else if (returnType == com_eclipsesource_v8_V8_V8_ARRAY_BUFFER) {
      if (isUndefined(env, resultObject)) {
        args.GetReturnValue().SetUndefined();
      }
      else {
        jlong resultHandle = getHandle(env, resultObject);
        Handle<Object> result = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(resultHandle));
        release(env, resultObject);
        args.GetReturnValue().Set(result);
      }
    }
    else {
      args.GetReturnValue().SetUndefined();
    }
  }
  if (resultObject != nullptr) {
    env->DeleteLocalRef(resultObject);
  }
  env->CallVoidMethod(parameters, v8ArrayReleaseMethodID);
  env->CallVoidMethod(jreceiver, v8ObjectReleaseMethodID);
  env->DeleteLocalRef(jreceiver);
  env->DeleteLocalRef(parameters);
}

JNIEXPORT jlongArray JNICALL Java_com_eclipsesource_v8_V8__1initNewV8Function
(JNIEnv *env, jobject, jlong v8RuntimePtr) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  MethodDescriptor* md = new MethodDescriptor();
  Local<External> ext = External::New(isolate, md);
  isolate->IdleNotificationDeadline(1);

  Local<Function> function = Function::New(context, objectCallback, ext).ToLocalChecked();
  md->v8RuntimePtr = v8RuntimePtr;
  Persistent<Object>* container = new Persistent<Object>;
  container->Reset(reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate, function);
  md->methodID = reinterpret_cast<jlong>(md);
  md->obj.Reset(isolate, ext);
  md->obj.SetWeak(md, &disposeMethod, WeakCallbackType::kParameter);

  // Position 0 is the pointer to the container, position 1 is the pointer to the descriptor
  jlongArray result = env->NewLongArray(2);
  jlong * fill = new jlong[2];
  fill[0] = reinterpret_cast<jlong>(container);
  fill[1] = md->methodID;
  (env)->SetLongArrayRegion(result, 0, 2, fill);
  return result;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1setWeak
  (JNIEnv * env, jobject, jlong v8RuntimePtr, jlong objectHandle) {
    Isolate* isolate = SETUP(env, v8RuntimePtr, );
    WeakReferenceDescriptor* wrd = new WeakReferenceDescriptor();
    wrd->v8RuntimePtr = v8RuntimePtr;
    wrd->objectHandle = objectHandle;
    reinterpret_cast<Persistent<Object>*>(objectHandle)->SetWeak(wrd, [](v8::WeakCallbackInfo<WeakReferenceDescriptor> const& data) {
      WeakReferenceDescriptor* wrd = data.GetParameter();
      JNIEnv * env;
      getJNIEnv(env);
      jobject v8 = reinterpret_cast<V8Runtime*>(wrd->v8RuntimePtr)->v8;
      env->CallVoidMethod(v8, v8WeakReferenceReleased, wrd->objectHandle);
      delete(wrd);
    }, WeakCallbackType::kParameter);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1clearWeak
  (JNIEnv * env, jobject, jlong v8RuntimePtr, jlong objectHandle) {
    Isolate* isolate = SETUP(env, v8RuntimePtr, )
    reinterpret_cast<Persistent<Object>*>(objectHandle)->ClearWeak();
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1isWeak
  (JNIEnv * env, jobject, jlong v8RuntimePtr, jlong objectHandle) {
    Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
    return reinterpret_cast<Persistent<Object>*>(objectHandle)->IsWeak();
}

JNIEXPORT jlong JNICALL Java_com_eclipsesource_v8_V8__1registerJavaMethod
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jstring functionName, jboolean voidMethod) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0)
  FunctionCallback callback = voidCallback;
  if (!voidMethod) {
    callback = objectCallback;
  }
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  Local<String> v8FunctionName = createV8String(env, isolate, functionName);
  isolate->IdleNotificationDeadline(1);
  MethodDescriptor* md= new MethodDescriptor();
  Local<External> ext = External::New(isolate, md);

  md->methodID = reinterpret_cast<jlong>(md);
  md->v8RuntimePtr = v8RuntimePtr;

  MaybeLocal<Function> func = Function::New(context, callback, ext);
  if (!func.IsEmpty()) {
    Maybe<bool> unusedResult = object->Set(context, v8FunctionName, func.ToLocalChecked());
    unusedResult.Check();
  }

  md->obj.Reset(isolate, ext);
  md->obj.SetWeak(md, &disposeMethod, WeakCallbackType::kParameter);

  return md->methodID;
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1releaseMethodDescriptor
  (JNIEnv *, jobject, jlong, jlong methodDescriptorPtr) {
  MethodDescriptor* md = reinterpret_cast<MethodDescriptor*>(methodDescriptorPtr);
  delete(md);
}

JNIEXPORT void JNICALL Java_com_eclipsesource_v8_V8__1setPrototype
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jlong prototypeHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, )
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  Handle<Object> prototype = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(prototypeHandle));
  object->SetPrototype(context, prototype);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1equals
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jlong thatHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, false);
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  Handle<Object> that = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  if (objectHandle == 0) {
    object = context->Global();
  }
  if (thatHandle == 0) {
  	that = context->Global();
  }
  return object->Equals(context, that).FromMaybe(false);
}

JNIEXPORT jstring JNICALL Java_com_eclipsesource_v8_V8__1toString
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, 0);
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  String::Value unicodeString(isolate, object);

  return env->NewString(*unicodeString, unicodeString.length());
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1strictEquals
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jlong thatHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, false);
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  Handle<Object> that = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(thatHandle));
  if (objectHandle == reinterpret_cast<jlong>(runtime->globalObject)) {
    object = context->Global();
  }
  if (thatHandle == reinterpret_cast<jlong>(runtime->globalObject)) {
  	that = context->Global();
  }
  return object->StrictEquals(that);
}

JNIEXPORT jboolean JNICALL Java_com_eclipsesource_v8_V8__1sameValue
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle, jlong thatHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, false);
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  Handle<Object> that = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  if (objectHandle == reinterpret_cast<jlong>(runtime->globalObject)) {
    object = context->Global();
  }
  if (thatHandle == reinterpret_cast<jlong>(runtime->globalObject)) {
  	that = context->Global();
  }
  return object->SameValue(that);
}

JNIEXPORT jint JNICALL Java_com_eclipsesource_v8_V8__1identityHash
(JNIEnv *env, jobject, jlong v8RuntimePtr, jlong objectHandle) {
  Isolate* isolate = SETUP(env, v8RuntimePtr, false);
  Handle<Object> object = Local<Object>::New(isolate, *reinterpret_cast<Persistent<Object>*>(objectHandle));
  if (objectHandle == reinterpret_cast<jlong>(runtime->globalObject)) {
    object = context->Global();
  }
  return object->GetIdentityHash();
}

Isolate* getIsolate(JNIEnv *env, jlong v8RuntimePtr) {
  if (v8RuntimePtr == 0) {
    throwError(env, "V8 isolate not found.");
    return nullptr;
  }
  V8Runtime* runtime = reinterpret_cast<V8Runtime*>(v8RuntimePtr);
  return runtime->isolate;
}

void throwResultUndefinedException(JNIEnv *env, const char *message) {
  (env)->ThrowNew(v8ResultsUndefinedCls, message);
}

void throwParseException(JNIEnv *env, const char* fileName, int lineNumber, String::Value *message,
  String::Value *sourceLine, int startColumn, int endColumn) {
  jstring jfileName = env->NewStringUTF(fileName);
  jstring jmessage = env->NewString(**message, message->length());
  jstring jsourceLine = env->NewString(**sourceLine, sourceLine->length());
  jthrowable result = (jthrowable)env->NewObject(v8ScriptCompilationCls, v8ScriptCompilationInitMethodID, jfileName, lineNumber, jmessage, jsourceLine, startColumn, endColumn);
  env->DeleteLocalRef(jfileName);
  env->DeleteLocalRef(jmessage);
  env->DeleteLocalRef(jsourceLine);
  (env)->Throw(result);
}

void throwExecutionException(JNIEnv *env, const char* fileName, int lineNumber, String::Value *message,
  String::Value* sourceLine, int startColumn, int endColumn, const char* stackTrace, jlong v8RuntimePtr) {
  jstring jfileName = env->NewStringUTF(fileName);
  jstring jmessage = env->NewString(**message, message->length());
  jstring jsourceLine = env->NewString(**sourceLine, sourceLine->length());
  jstring jstackTrace = nullptr;
  if (stackTrace != nullptr) {
    jstackTrace = env->NewStringUTF(stackTrace);
  }
  jthrowable wrappedException = nullptr;
  if (env->ExceptionCheck()) {
    wrappedException = env->ExceptionOccurred();
    env->ExceptionClear();
  }
  if (reinterpret_cast<V8Runtime*>(v8RuntimePtr)->pendingException != nullptr) {
    wrappedException = reinterpret_cast<V8Runtime*>(v8RuntimePtr)->pendingException;
    reinterpret_cast<V8Runtime*>(v8RuntimePtr)->pendingException = nullptr;
  }
  if ( wrappedException != nullptr && !env->IsInstanceOf( wrappedException, throwableCls) ) {
    std::cout << "Wrapped Exception is not a Throwable" << std::endl;
    wrappedException = nullptr;
  }
  jthrowable result = static_cast<jthrowable>(env->NewObject(v8ScriptExecutionException, v8ScriptExecutionExceptionInitMethodID, jfileName, lineNumber, jmessage, jsourceLine, startColumn, endColumn, jstackTrace, wrappedException));
  env->DeleteLocalRef(jfileName);
  env->DeleteLocalRef(jmessage);
  env->DeleteLocalRef(jsourceLine);
  (env)->Throw(result);
}

void throwParseException(JNIEnv *env, const Local<Context>& context, Isolate* isolate, TryCatch* tryCatch) {
 String::Value exception(isolate, tryCatch->Exception());
 Handle<Message> message = tryCatch->Message();
 if (message.IsEmpty()) {
   throwV8RuntimeException(env, &exception);
 }
 else {
   String::Utf8Value filename(isolate, message->GetScriptResourceName());
   int lineNumber = message->GetLineNumber(context).FromJust();
   String::Value sourceline(isolate, message->GetSourceLine(context).ToLocalChecked());
   int start = message->GetStartColumn();
   int end = message->GetEndColumn();
   const char* filenameString = ToCString(filename);
   throwParseException(env, filenameString, lineNumber, &exception, &sourceline, start, end);
 }
}

void throwExecutionException(JNIEnv *env, const Local<Context>& context, Isolate* isolate, TryCatch* tryCatch, jlong v8RuntimePtr) {
 String::Value exception(isolate, tryCatch->Exception());
 Handle<Message> message = tryCatch->Message();
 if (message.IsEmpty()) {
   throwV8RuntimeException(env, &exception);
 }
 else {
   String::Utf8Value filename(isolate, message->GetScriptResourceName());
   int lineNumber = message->GetLineNumber(context).FromMaybe(-1);
   String::Value sourceline(isolate, message->GetSourceLine(context).ToLocalChecked());
   int start = message->GetStartColumn();
   int end = message->GetEndColumn();
   const char* filenameString = ToCString(filename);
   MaybeLocal<Value> v8StackTrace = tryCatch->StackTrace(context);
   if (!v8StackTrace.IsEmpty()) {
       const char* stackTrace;
       String::Utf8Value stack_trace(isolate, v8StackTrace.ToLocalChecked());
       if (stack_trace.length() > 0) {
         stackTrace = ToCString(stack_trace);
       }
       throwExecutionException(env, filenameString, lineNumber, &exception, &sourceline, start, end, stackTrace, v8RuntimePtr);
   } else {
       throwExecutionException(env, filenameString, lineNumber, &exception, &sourceline, start, end, nullptr, v8RuntimePtr);
   }
 }
}

void throwV8RuntimeException(JNIEnv *env, String::Value *message) {
  jstring exceptionString = env->NewString(**message, message->length());
  jthrowable exception = (jthrowable)env->NewObject(v8RuntimeExceptionCls, v8RuntimeExceptionInitMethodID, exceptionString);
  (env)->Throw(exception);
  env->DeleteLocalRef(exceptionString);
}

void throwError(JNIEnv *env, const char *message) {
  (env)->ThrowNew(errorCls, message);
}

void disposeMethod(v8::WeakCallbackInfo<MethodDescriptor> const& data) {
    MethodDescriptor* md = data.GetParameter();
    jobject v8 = reinterpret_cast<V8Runtime*>(md->v8RuntimePtr)->v8;
    JNIEnv* env;
    getJNIEnv(env);
    env->CallVoidMethod(v8, v8DisposeMethodID, md->methodID);
    if (!md->obj.IsEmpty()) {
        md->obj.ClearWeak();
        md->obj.Reset();
    }
    delete(md);
    md = nullptr;
}

jobject getResult(JNIEnv *env, const Local<Context>& context, jobject &v8, jlong v8RuntimePtr, Handle<Value> &result, jint expectedType) {
  v8::Isolate* isolate = reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate;
  if (result->IsUndefined() && expectedType == com_eclipsesource_v8_V8_V8_ARRAY) {
    jobject objectResult = env->NewObject(undefinedV8ArrayCls, undefinedV8ArrayInitMethodID, v8);
    return objectResult;
  }
  else if (result->IsUndefined() && (expectedType == com_eclipsesource_v8_V8_V8_OBJECT || expectedType == com_eclipsesource_v8_V8_NULL)) {
    jobject objectResult = env->NewObject(undefinedV8ObjectCls, undefinedV8ObjectInitMethodID, v8);
    return objectResult;
  }
  else if (result->IsInt32()) {
    return env->NewObject(integerCls, integerInitMethodID, result->Int32Value(context).FromJust());
  }
  else if (result->IsNumber()) {
    return env->NewObject(doubleCls, doubleInitMethodID, result->NumberValue(context).FromJust());
  }
  else if (result->IsBoolean()) {
    return env->NewObject(booleanCls, booleanInitMethodID, result->BooleanValue(isolate));
  }
  else if (result->IsString()) {
    String::Value unicodeString(isolate, result->ToString((context)).ToLocalChecked());

    return env->NewString(*unicodeString, unicodeString.length());
  }
  else if (result->IsFunction()) {
    jobject objectResult = env->NewObject(v8FunctionCls, v8FunctionInitMethodID, v8);
    jlong resultHandle = getHandle(env, objectResult);

    v8::Isolate* isolate = reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate;

    reinterpret_cast<Persistent<Object>*>(resultHandle)->Reset(isolate, result->ToObject(context).ToLocalChecked());

    return objectResult;
  }
  else if (result->IsArray()) {
    jobject objectResult = env->NewObject(v8ArrayCls, v8ArrayInitMethodID, v8);
    jlong resultHandle = getHandle(env, objectResult);

    v8::Isolate* isolate = reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate;

    reinterpret_cast<Persistent<Object>*>(resultHandle)->Reset(isolate, result->ToObject(context).ToLocalChecked());

    return objectResult;
  }
  else if (result->IsTypedArray()) {
      jobject objectResult = env->NewObject(v8TypedArrayCls, v8TypedArrayInitMethodID, v8);
      jlong resultHandle = getHandle(env, objectResult);

      v8::Isolate* isolate = reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate;

      reinterpret_cast<Persistent<Object>*>(resultHandle)->Reset(isolate, result->ToObject(context).ToLocalChecked());

      return objectResult;
  }
  else if (result->IsArrayBuffer()) {
    ArrayBuffer* arrayBuffer = ArrayBuffer::Cast(*result);
    if ( arrayBuffer->ByteLength() == 0 || arrayBuffer->GetBackingStore()->Data() == nullptr ) {
      jobject objectResult = env->NewObject(v8ArrayBufferCls, v8ArrayBufferInitMethodID, v8, NULL);
      jlong resultHandle = getHandle(env, objectResult);
      v8::Isolate* isolate = reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate;
      reinterpret_cast<Persistent<Object>*>(resultHandle)->Reset(isolate, result->ToObject(context).ToLocalChecked());
      return objectResult;
    }
    jobject byteBuffer = env->NewDirectByteBuffer(arrayBuffer->GetBackingStore()->Data(), static_cast<jlong>(arrayBuffer->ByteLength()));
    jobject objectResult = env->NewObject(v8ArrayBufferCls, v8ArrayBufferInitMethodID, v8, byteBuffer);
    jlong resultHandle = getHandle(env, objectResult);

    v8::Isolate* isolate = reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate;

    reinterpret_cast<Persistent<Object>*>(resultHandle)->Reset(isolate, result->ToObject(context).ToLocalChecked());

    return objectResult;
  }
  else if (result->IsObject()) {
    jobject objectResult = env->NewObject(v8ObjectCls, v8ObjectInitMethodID, v8);
    jlong resultHandle = getHandle(env, objectResult);

    v8::Isolate* isolate = reinterpret_cast<V8Runtime*>(v8RuntimePtr)->isolate;

    reinterpret_cast<Persistent<Object>*>(resultHandle)->Reset(isolate, result->ToObject(context).ToLocalChecked());

    return objectResult;
  }

  return nullptr;
}
