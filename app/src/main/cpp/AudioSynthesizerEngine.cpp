#include "AudioSynthesizerEngine.h"
#include <algorithm>

AudioSynthesizerEngine::AudioSynthesizerEngine() {}

AudioSynthesizerEngine::~AudioSynthesizerEngine() {
    stop();
}

bool AudioSynthesizerEngine::start() {
    if (mIsRunning.load()) {
        return true;
    }

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Shared);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(oboe::ChannelCount::Mono);
    builder.setUsage(oboe::Usage::Media);
    builder.setContentType(oboe::ContentType::Music);
    builder.setDataCallback(this);

    oboe::Result result = builder.openStream(mAudioStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open Oboe audio stream: %s", oboe::convertToText(result));
        return false;
    }

    mSampleRate = mAudioStream->getSampleRate();
    LOGI("Oboe Audio Stream opened successfully with sample rate: %d", mSampleRate);

    result = mAudioStream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start Oboe audio stream: %s", oboe::convertToText(result));
        mAudioStream->close();
        return false;
    }

    mIsRunning.store(true);
    LOGI("AudioSynthesizerEngine started successfully.");
    return true;
}

void AudioSynthesizerEngine::stop() {
    if (!mIsRunning.load()) {
        return;
    }

    mIsRunning.store(false);

    if (mAudioStream) {
        mAudioStream->stop();
        mAudioStream->close();
        mAudioStream.reset();
    }
    LOGI("AudioSynthesizerEngine stopped successfully.");
}

oboe::DataCallbackResult AudioSynthesizerEngine::onAudioReady(
        oboe::AudioStream *audioStream,
        void *audioData,
        int32_t numFrames) {

    auto *floatData = static_cast<float *>(audioData);

    float center = mCenterFrequency.load();
    float depth = mModulationDepth.load();
    float rate = mModulationRate.load();
    float vol = mVolume.load();

    double lfoStep = (TWO_PI * rate) / mSampleRate;
    int32_t updateIntervalSamples = mSampleRate / 30; // ~30 updates per sec

    for (int32_t i = 0; i < numFrames; ++i) {
        double lfoValue = std::sin(mLfoPhase);
        float instantaneousFreq = std::max(10.0f, center + depth * static_cast<float>(lfoValue));

        double carrierStep = (TWO_PI * instantaneousFreq) / mSampleRate;

        mCarrierPhase += carrierStep;
        if (mCarrierPhase >= TWO_PI) {
            mCarrierPhase -= TWO_PI;
        }

        mLfoPhase += lfoStep;
        if (mLfoPhase >= TWO_PI) {
            mLfoPhase -= TWO_PI;
        }

        floatData[i] = static_cast<float>(std::sin(mCarrierPhase) * vol);

        mUpdateCounter++;
        if (mUpdateCounter >= updateIntervalSamples) {
            mUpdateCounter = 0;
            if (mFrequencyCallback != nullptr) {
                mFrequencyCallback(instantaneousFreq);
            }
        }
    }

    return oboe::DataCallbackResult::Continue;
}
