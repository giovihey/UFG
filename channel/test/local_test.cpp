#include <rtc/rtc.hpp>                                                                         
#include <iostream>
#include <memory>                                                                              
#include <thread>
#include <chrono>                                                                              
#include <atomic>

using namespace std;

int main() {                                                                                   
    // No STUN needed for localhost
    rtc::Configuration config;                                                                 
                                                                                                
    // Create two peers
    auto peerA = make_shared<rtc::PeerConnection>(config);                                     
    auto peerB = make_shared<rtc::PeerConnection>(config);

    // Track connection state                                                                  
    atomic<bool> dataChannelOpen{false};
                                                                                                
    // ---- Wire up Peer A ----                                                                

    // When A generates an SDP offer, give it to B                                             
    peerA -> onLocalDescription([&peerB](rtc::Description desc) {
        cout << "[A] Generated local description, passing to B" << endl;                       
        peerB->setRemoteDescription(desc);                                                     
    });                                                                                        
                                                                                                
    // When A finds an ICE candidate, give it to B                                             
    peerA -> onLocalCandidate([&peerB](rtc::Candidate candidate) {
        peerB->addRemoteCandidate(candidate);                                                  
    });         

    // ---- Wire up Peer B ----                                                                

    // When B generates an SDP answer, give it to A                                            
    peerB->onLocalDescription([&peerA](rtc::Description desc) {
        cout << "[B] Generated local description, passing to A" << endl;
        peerA->setRemoteDescription(desc);
    });                                                                                        

    // When B finds an ICE candidate, give it to A                                             
    peerB->onLocalCandidate([&peerA](rtc::Candidate candidate) {
        peerA->addRemoteCandidate(candidate);
    });                                                                                        

    // When B receives a data channel from A                                                   
    peerB->onDataChannel([&dataChannelOpen](shared_ptr<rtc::DataChannel> dc) {
        cout << "[B] Received data channel: " << dc->label() << endl;                          

        dc->onMessage([](auto message) {                                                       
            if (holds_alternative<string>(message)) {
                cout << "[B] Received: " << get<string>(message) << endl;                      
            }
        });                                                                                    
                
        dc->onOpen([dc, &dataChannelOpen]() {                                                  
            cout << "[B] Data channel open, sending reply" << endl;
            dc->send("hello from B");                                                          
            dataChannelOpen = true;
        });                                                                                    
    });         

    // ---- Peer A creates the data channel and initiates ----                                 
    auto dc = peerA->createDataChannel("input");
                                                                                                
    dc->onOpen([dc]() {
        cout << "[A] Data channel open, sending message" << endl;                              
        dc->send("hello from A");
    });                                                                                        

    dc->onMessage([](auto message) {                                                           
        if (holds_alternative<string>(message)) {
            cout << "[A] Received: " << get<string>(message) << endl;
        }                                                                                      
    });
                                                                                                
    // Wait for the exchange to happen
    cout << "Waiting for connection..." << endl;
    for (int i = 0; i < 50; i++) {  // 5 second timeout
        if (dataChannelOpen) break;                                                            
        this_thread::sleep_for(chrono::milliseconds(100));
    }                                                                                          
                
    if (!dataChannelOpen) {                                                                    
        cout << "FAILED: connection did not open" << endl;
        return 1;                                                                              
    }
                                                                                                
    // Give messages time to arrive
    this_thread::sleep_for(chrono::seconds(1));

    cout << "TEST PASSED" << endl;                                                             
    return 0;
}       