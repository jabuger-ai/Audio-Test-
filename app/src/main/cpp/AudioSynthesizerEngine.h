#ifndef AUDIO_SYNTHESIZER_ENGINE_H
#define AUDIO_SYNTHESIZER_ENGINE_H

#include <oboe/Oboe.h>
#include <atomic>
#include <cmath>
#include <android/log.h>

#define LOG_TAG "AudioSynthesizerEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef void (*FrequencyCallback)(float currentFreq);

class AudioSynthesizerEngine : public oboe::AudioStreamDataCallback {
public:
    AudioSynthesizerEngine();
    ~AudioSynthesizerEngine() override;

    bool start();
    void stop();
    bool isSynthesizing() const { return mIsRunning.load(); }

    void setCenterFrequency(float freq) { mCenterFrequency.store(freq); }
    float getCenterFrequency() const { return mCenterFrequency.load(); }

    void setModulationDepth(float depth) { mModulationDepth.store(depth); }
    float getModulationDepth() const { return mModulationDepth.load(); }

    void setModulationRate(float rate) { mModulationRate.store(rate); }
    float getModulationRate() const { return mModulationRate.load(); }

    void setVolume(float vol) { mVolume.store(vol); }
    float getVolume() const { return mVolume.load(); }

    void setFrequencyCallback(FrequencyCallback callback) { mFrequencyCallback = callback; }

    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream *audioStream,
            void *audioData,
            int32_t numFrames) override;

private:
    std::shared_ptr<oboe::AudioStream> mAudioStream;

    std::atomic<bool> mIsRunning{false};
    std::atomic<float> mCenterFrequency{440.0f};
    std::atomic<float> mModulationDepth{150.0f};
    std::atomic<float> mModulationRate{1.0f};
    std::atomic<float> mVolume{0.5f};

    double mCarrierPhase{0.0};
    double mLfoPhase{0.0};
    int32_t mSampleRate{44100};

    int32_t mUpdateCounter{0};
    FrequencyCallback mFrequencyCallback{nullptr};

    static constexpr double TWO_PI = 6.28318530717958647692;
};

#endif // AUDIO_SYNTHESIZER_ENGINE_H
