#include <jni.h>
#include "AudioSynthesizerEngine.h"

static JavaVM *gJavaVM = nullptr;
static jobject gNativeEngineObj = nullptr;
static jmethodID gOnFrequencyUpdateMethodID = nullptr;

static void onNativeFrequencyUpdate(float freq) {
    if (gJavaVM == nullptr || gNativeEngineObj == nullptr || gOnFrequencyUpdateMethodID == nullptr) {
        return;
    }

    JNIEnv *env = nullptr;
    jint getEnvResult = gJavaVM->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);

    bool shouldDetach = false;
    if (getEnvResult == JNI_EDETACHED) {
        if (gJavaVM->AttachCurrentThread(&env, nullptr) == JNI_OK) {
            shouldDetach = true;
        } else {
            return;
        }
    }

    if (env != nullptr) {
        env->CallVoidMethod(gNativeEngineObj, gOnFrequencyUpdateMethodID, freq);
    }

    if (shouldDetach) {
        gJavaVM->DetachCurrentThread();
    }
}

extern "C"
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    gJavaVM = vm;
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_audiotest_NativeSynthesizerEngine_nativeCreate(JNIEnv *env, jobject thiz) {
    if (gNativeEngineObj != nullptr) {
        env->DeleteGlobalRef(gNativeEngineObj);
    }
    gNativeEngineObj = env->NewGlobalRef(thiz);

    jclass clazz = env->GetObjectClass(thiz);
    gOnFrequencyUpdateMethodID = env->GetMethodID(clazz, "onFrequencyUpdateFromNative", "(F)V");

    auto *engine = new AudioSynthesizerEngine();
    engine->setFrequencyCallback(onNativeFrequencyUpdate);
    return reinterpret_cast<jlong>(engine);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_audiotest_NativeSynthesizerEngine_nativeDestroy(JNIEnv *env, jobject thiz, jlong engine_ptr) {
    auto *engine = reinterpret_cast<AudioSynthesizerEngine *>(engine_ptr);
    if (engine != nullptr) {
        engine->stop();
        delete engine;
    }
    if (gNativeEngineObj != nullptr) {
        env->DeleteGlobalRef(gNativeEngineObj);
        gNativeEngineObj = nullptr;
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_audiotest_NativeSynthesizerEngine_nativeStart(JNIEnv *env, jobject thiz, jlong engine_ptr) {
    auto *engine = reinterpret_cast<AudioSynthesizerEngine *>(engine_ptr);
    if (engine != nullptr) {
        return engine->start() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_audiotest_NativeSynthesizerEngine_nativeStop(JNIEnv *env, jobject thiz, jlong engine_ptr) {
    auto *engine = reinterpret_cast<AudioSynthesizerEngine *>(engine_ptr);
    if (engine != nullptr) {
        engine->stop();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_audiotest_NativeSynthesizerEngine_nativeSetCenterFrequency(JNIEnv *env, jobject thiz, jlong engine_ptr, jfloat freq) {
    auto *engine = reinterpret_cast<AudioSynthesizerEngine *>(engine_ptr);
    if (engine != nullptr) {
        engine->setCenterFrequency(freq);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_audiotest_NativeSynthesizerEngine_nativeSetModulationDepth(JNIEnv *env, jobject thiz, jlong engine_ptr, jfloat depth) {
    auto *engine = reinterpret_cast<AudioSynthesizerEngine *>(engine_ptr);
    if (engine != nullptr) {
        engine->setModulationDepth(depth);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_audiotest_NativeSynthesizerEngine_nativeSetModulationRate(JNIEnv *env, jobject thiz, jlong engine_ptr, jfloat rate) {
    auto *engine = reinterpret_cast<AudioSynthesizerEngine *>(engine_ptr);
    if (engine != nullptr) {
        engine->setModulationRate(rate);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_audiotest_NativeSynthesizerEngine_nativeSetVolume(JNIEnv *env, jobject thiz, jlong engine_ptr, jfloat volume) {
    auto *engine = reinterpret_cast<AudioSynthesizerEngine *>(engine_ptr);
    if (engine != nullptr) {
        engine->setVolume(volume);
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_audiotest_NativeSynthesizerEngine_nativeIsSynthesizing(JNIEnv *env, jobject thiz, jlong engine_ptr) {
    auto *engine = reinterpret_cast<AudioSynthesizerEngine *>(engine_ptr);
    if (engine != nullptr) {
        return engine->isSynthesizing() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}
