#include "webrtc_wrapper.h"
#include <functional>
#include <iostream>
#include <memory> // for shared_ptr
#include <rtc/rtc.hpp> // libdatachannel
#include <string>

// Global state — one peer connection and one data channel
static std::shared_ptr<rtc::PeerConnection> peerConnection;
static std::shared_ptr<rtc::DataChannel> dataChannel;

// Store the JVM reference for callbacks from C++ → Kotlin
static JavaVM *jvm = nullptr;
static jobject callbackObj = nullptr;

JNIEXPORT void JNICALL Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_initialize(JNIEnv *env,
                                                                                                   jobject obj,
                                                                                                   jstring stunServer) {
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
        JNIEnv *env;
        jvm->AttachCurrentThread((void **) &env, nullptr);

        std::string sdp = std::string(desc);
        jstring jSdp = env->NewStringUTF(sdp.c_str());

        jclass cls = env->GetObjectClass(callbackObj);
        jmethodID mid = env->GetMethodID(cls, "onLocalDescription", "(Ljava/lang/String;)V");
        env->CallVoidMethod(callbackObj, mid, jSdp);

        jvm->DetachCurrentThread();
        // std::cout << "Local description: " << std::string(desc) << std::endl;
    });

    peerConnection->onLocalCandidate([](rtc::Candidate candidate) {
        // Called when a new ICE candidate is found
        JNIEnv *env;
        jvm->AttachCurrentThread((void **) &env, nullptr);

        jstring jCand = env->NewStringUTF(std::string(candidate).c_str());
        jstring jMid = env->NewStringUTF(candidate.mid().c_str());

        jclass cls = env->GetObjectClass(callbackObj);
        jmethodID mid = env->GetMethodID(cls, "onLocalCandidate", "(Ljava/lang/String;Ljava/lang/String;)V");
        env->CallVoidMethod(callbackObj, mid, jCand, jMid);

        jvm->DetachCurrentThread();
        // std::cout << "Local candidate: " << std::string(candidate) << std::endl;
    });

    peerConnection->onDataChannel([](std::shared_ptr<rtc::DataChannel> dc) {
        // Called when the REMOTE side creates a data channel
        dataChannel = dc;
        std::cout << "Data channel received" << std::endl;

        dc->onOpen([]() {
            std::cout << "Data channel open" << std::endl;
            JNIEnv *env;
            jvm->AttachCurrentThread((void **) &env, nullptr);

            jclass cls = env->GetObjectClass(callbackObj);
            jmethodID mid = env->GetMethodID(cls, "onDataChannelOpen", "()V");
            env->CallVoidMethod(callbackObj, mid);
            jvm->DetachCurrentThread();
        });

        dc->onMessage([](auto message) {
            // Called when we receive data from the other player
            // 1. get a JNI env for this thread
            JNIEnv *env;
            jvm->AttachCurrentThread((void **) &env, nullptr);

            // 2. Unpack the bytes (reverse of sendInput)
            auto data = std::get<rtc::binary>(message);
            int32_t inputMask;
            int64_t frameNumber;
            memcpy(&inputMask, data.data(), 4);
            memcpy(&frameNumber, data.data() + 4, 8);

            // 3. Call WebRtcBridge.onRemoteInput(inputMask, frameNumber)
            jclass cls = env->GetObjectClass(callbackObj);
            jmethodID mid = env->GetMethodID(cls, "onRemoteInput", "(IJ)V");
            env->CallVoidMethod(callbackObj, mid, inputMask, frameNumber);

            jvm->DetachCurrentThread();
        });
    });
}

JNIEXPORT void JNICALL Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_sendInput(JNIEnv *env,
                                                                                                  jobject obj,
                                                                                                  jint inputMask,
                                                                                                  jlong frameNumber) {
    if (dataChannel && dataChannel->isOpen()) {
        // Pack input into bytes: 4 bytes for input + 8 bytes for frame number
        // This is what goes over the wire every frame
        uint8_t buffer[12];
        memcpy(buffer, &inputMask, 4);
        memcpy(buffer + 4, &frameNumber, 8);

        dataChannel->send(reinterpret_cast<std::byte *>(buffer), 12);
    }
}

JNIEXPORT void JNICALL Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_setRemoteDescription(
        JNIEnv *env, jobject obj, jstring sdp) {
    const char *sdpChars = env->GetStringUTFChars(sdp, nullptr);
    std::string sdpStr(sdpChars);
    env->ReleaseStringUTFChars(sdp, sdpChars);

    rtc::Description desc(sdpStr, sdpStr.find("a=group") != std::string::npos ? rtc::Description::Type::Offer
                                                                              : rtc::Description::Type::Answer);
    peerConnection->setRemoteDescription(desc);
}

JNIEXPORT void JNICALL Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_addIceCandidate(
        JNIEnv *env, jobject obj, jstring candidate, jstring mid) {
    const char *candChars = env->GetStringUTFChars(candidate, nullptr);
    const char *midChars = env->GetStringUTFChars(mid, nullptr);

    peerConnection->addRemoteCandidate(rtc::Candidate(candChars, midChars));

    env->ReleaseStringUTFChars(candidate, candChars);
    env->ReleaseStringUTFChars(mid, midChars);
}

JNIEXPORT jstring JNICALL Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_createOffer(JNIEnv *env,
                                                                                               jobject obj) {
    // The offerer creates the data channel
    dataChannel = peerConnection->createDataChannel("input");

    dataChannel->onOpen([]() {
        std::cout << "Data channel open" << std::endl;
        JNIEnv *env;
        jvm->AttachCurrentThread((void **) &env, nullptr);
        jclass cls = env->GetObjectClass(callbackObj);
        jmethodID mid = env->GetMethodID(cls, "onDataChannelOpen", "()V");
        env->CallVoidMethod(callbackObj, mid);
        jvm->DetachCurrentThread();
    });

    dataChannel->onMessage([](auto message) {
        // Same onMessage handler you already have —
        // unpack inputMask + frameNumber and call onRemoteInput
        JNIEnv *env;
        jvm->AttachCurrentThread((void **) &env, nullptr);

        auto data = std::get<rtc::binary>(message);
        int32_t inputMask;
        int64_t frameNumber;
        memcpy(&inputMask, data.data(), 4);
        memcpy(&frameNumber, data.data() + 4, 8);

        jclass cls = env->GetObjectClass(callbackObj);
        jmethodID mid = env->GetMethodID(cls, "onRemoteInput", "(IJ)V");
        env->CallVoidMethod(callbackObj, mid, inputMask, frameNumber);

        jvm->DetachCurrentThread();
    });

    // Trigger SDP generation — the onLocalDescription callback will fire
    peerConnection->setLocalDescription();

    return env->NewStringUTF("offer-created");
}
