//
//  AudioQ.m
//  ChatRoom
//
//  Created by Wujun Yang on 19/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import "AudioQ.h"

@implementation AudioQ

static const int nBuffer = 3;

struct AQRecorderState{
    AudioStreamBasicDescription mDataFormat;
    AudioQueueRef               mQueue;
    AudioQueueBufferRef         mBuffers[nBuffer];
    AudioFileID                 mAudioFile;
    UInt32                      bufferByteSize;
    SInt64                      mCurrentPacket;
    bool                        mIsRunning;
};

AQRecorderState aqData;
CFURLRef url;
static OSStatus BufferFilledHandler(
                                    void *                               inUserData,
                                    SInt64                               inPosition,
                                    UInt32                               requestCount,
                                    const void *                         buffer,
                                    UInt32 *                             actualCount
                                    
                                    ){
    // callback when you write to the file
    // you can handle audio packet and send them for broadcasting
    return 0;
}

static void HandleInputBuffer(
                              void                              *aqData,
                              AudioQueueRef                     inAq,
                              AudioQueueBufferRef                   inBuffer,
                              const AudioTimeStamp              *inStartTime,
                              UInt32                                inNumPackets,
                              const AudioStreamPacketDescription    *inPacketDesc
                              ) {
    AQRecorderState *pAqData = (AQRecorderState*) aqData;
    if (AudioFileWritePackets (
                               pAqData->mAudioFile,
                               false,
                               inBuffer->mAudioDataByteSize,
                               inPacketDesc,
                               pAqData->mCurrentPacket,
                               &inNumPackets,
                               inBuffer->mAudioData
                               ) == noErr) {
        pAqData->mCurrentPacket += inNumPackets;
    } else {
        NSLog(@"err writing packet");
    }
    if (pAqData->mIsRunning == 0)
        return;
    AudioQueueEnqueueBuffer(pAqData->mQueue,inBuffer,0,NULL);
}

-(OSStatus) initializeAQ{
    
    //--- set the output format ---//
    aqData.mDataFormat.mSampleRate = 22050;
    aqData.mDataFormat.mFormatID = kAudioFormatMPEG4AAC;
    aqData.mDataFormat.mFormatFlags = kMPEG4Object_AAC_Main;
    aqData.mDataFormat.mBytesPerPacket = 0;
    aqData.mDataFormat.mFramesPerPacket = 1024;
    aqData.mDataFormat.mBytesPerFrame = 0;
    aqData.mDataFormat.mChannelsPerFrame = 1;
    aqData.mDataFormat.mBitsPerChannel = 0;
    AudioFileTypeID fileType = kAudioFileAAC_ADTSType;
    
    aqData.bufferByteSize = 0x5000; // ??
    
    AudioQueueNewInput(&aqData.mDataFormat, HandleInputBuffer, &aqData, CFRunLoopGetMain(), kCFRunLoopCommonModes, 0, &aqData.mQueue);
    aqData.mCurrentPacket = 0;
    aqData.mIsRunning = true;
    
    
    //--- record in a file get the callback when writing ---//
    AQRecorderState *pAqData = &aqData;
    AudioFileInitializeWithCallbacks((void*)&pAqData,
                                     nil,
                                     BufferFilledHandler,
                                     nil,
                                     nil,
                                     fileType,
                                     &aqData.mDataFormat,
                                     kAudioFileFlags_EraseFile,
                                     &aqData.mAudioFile);
    
    //--- prepare set of audio queue buffers ---//
    for(int i = 0 ; i < nBuffer ; i++){
        AudioQueueAllocateBuffer(aqData.mQueue, aqData.bufferByteSize, &aqData.mBuffers[i]);
        AudioQueueEnqueueBuffer(aqData.mQueue, aqData.mBuffers[i], 0, NULL);
    }
    return 0;
}

-(void) start{
    AudioQueueStart(aqData.mQueue, NULL);
}

-(void) stop{
    NSLog(@"stoping");
    AudioQueueStop(aqData.mQueue, true);
    aqData.mIsRunning = false;
    AudioQueueDispose (aqData.mQueue,true);
    AudioFileClose (aqData.mAudioFile);
}
@end
