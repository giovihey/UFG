#include "webrtc_wrapper.h"
#include <rtc/rtc.hpp>        // libdatachannel
#include <memory>              // for shared_ptr
#include <string>
#include <iostream>
#include <functional>

// Global state — one peer connection and one data channel
static std::shared_ptr<rtc::PeerConnection> peerConnection;
static std::shared_ptr<rtc::DataChannel> dataChannel;

// Store the JVM reference for callbacks from C++ → Kotlin
static JavaVM *jvm = nullptr;
static jobject callbackObj = nullptr;

JNIEXPORT void JNICALL
Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_initialize(
    JNIEnv *env, jobject obj, jstring stunServer) {

    // Save JVM reference for callbacks later
    env->GetJavaVM(&jvm);
    callbackObj = env->NewGlobalRef(obj);

    // Convert jstring → C++ string
    const char *stunChars = env->GetStringUTFChars(stunServer, nullptr);
    std::string stunUrl(stunChars);
    env->ReleaseStringUTFChars(stunServer, stunChars);

    // Configure the peer connection
    rtc::Configuration config;
    config.iceServers.emplace_back(stunUrl);

    // Create the peer connection
    peerConnection = std::make_shared<rtc::PeerConnection>(config);

    // Set up event callbacks
    peerConnection->onLocalDescription([](rtc::Description desc) {
        // Called when the local SDP is ready
        // You'll want to send this to Kotlin → then to the signaling server
        std::cout << "Local description: " << std::string(desc) << std::endl;
    });

    peerConnection->onLocalCandidate([](rtc::Candidate candidate) {
        // Called when a new ICE candidate is found
        std::cout<<"Local candidate: "<<std::string(candidate)<<std::endl;
    });

    peerConnection->onDataChannel([](std::shared_ptr<rtc::DataChannel> dc) {
        // Called when the REMOTE side creates a data channel
        dataChannel = dc;
        std::cout << "Data channel received" << std::endl;

        dc->onMessage([](auto message) {
            // Called when we receive data from the other player
            // This is where you'll read their InputState
        });
    });
}

JNIEXPORT void JNICALL
Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_sendInput(
    JNIEnv *env, jobject obj, jint inputMask, jlong frameNumber) {

    if (dataChannel && dataChannel->isOpen()) {
        // Pack input into bytes: 4 bytes for input + 8 bytes for frame number
        // This is what goes over the wire every frame
        uint8_t buffer[12];
        memcpy(buffer, &inputMask, 4);
        memcpy(buffer + 4, &frameNumber, 8);

        dataChannel->send(reinterpret_cast<std::byte*>(buffer), 12);
    }
}
