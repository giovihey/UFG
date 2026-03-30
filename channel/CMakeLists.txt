cmake_minimum_required(VERSION 3.16)
project(webrtc_wrapper LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)        # C++17 is the sweet spot right now
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# locates jni.h that we need for the bridge
find_package(JNI REQUIRED)

# locates the library that we will use to make the link with other peers
find_package(LibDataChannel REQUIRED)

# instead of add_executable(...) — produces a .dylib not a binary
add_library(webrtc_wrapper SHARED src/webrtc_wrapper.cpp)

target_include_directories(webrtc_wrapper PRIVATE
      ${JNI_INCLUDE_DIRS}          # jni.h
      ${CMAKE_SOURCE_DIR}/include  
      ${LibDataChannel_INCLUDE_DIRS}
)

target_link_libraries(webrtc_wrapper PRIVATE 
    LibDataChannel::LibDataChannel
)

add_executable(local_test test/local_test.cpp
        test/local_test.cpp)

target_link_libraries(local_test PRIVATE                                                       
    LibDataChannel::LibDataChannel
)

add_executable(protocol_test test/protocol_test.cpp)
target_link_libraries(protocol_test PRIVATE LibDataChannel::LibDataChannel)