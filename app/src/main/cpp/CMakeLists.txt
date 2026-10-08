cmake_minimum_required(VERSION 3.22.1)
project("audio_synth")

find_package(oboe REQUIRED CONFIG)

add_library(
    audio_synth
    SHARED
    AudioSynthesizerEngine.cpp
    audio_synth_jni.cpp
)

target_include_directories(audio_synth PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(
    audio_synth
    oboe::oboe
    log
)
