// guard to prevent "already defined" errors
#ifndef WEBRTC_WRAPPER_H
#define WEBRTC_WRAPPER_H

#include <jni.h>

// These are the functions Kotlin will call via JNI.
// The naming convention is: Java_<package>_<class>_<method>
// Dots in the package become underscores.
//
// For package: com.heyteam.ufg.infrastructure.adapter.network
// Class: WebRtcBridge
// The prefix is: Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge

#ifdef __cplusplus
extern "C" {
#endif
  // Initialize a WebRTC peer connection with a STUN server
  JNIEXPORT void JNICALL
  Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_initialize(
      JNIEnv *env, jobject obj, jstring stunServer);

  // Create an SDP offer (you're the one initiating)
  JNIEXPORT jstring JNICALL
  Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_createOffer(
      JNIEnv *env, jobject obj);

  // Set the remote SDP (offer or answer from the other player)
  JNIEXPORT void JNICALL
  Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_setRemoteDescription(
      JNIEnv *env, jobject obj, jstring sdp);

  // Add an ICE candidate from the other player
  JNIEXPORT void JNICALL
  Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_addIceCandidate(
      JNIEnv *env, jobject obj, jstring candidate, jstring mid);

  // Send input data to the other player
  JNIEXPORT void JNICALL
  Java_com_heyteam_ufg_infrastructure_adapter_network_WebRtcBridge_sendInput(
      JNIEnv *env, jobject obj, jint inputMask, jlong frameNumber);

#ifdef __cplusplus
}
#endif
#endif