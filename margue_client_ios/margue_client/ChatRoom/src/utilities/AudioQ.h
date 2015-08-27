//
//  AudioQ.h
//  ChatRoom
//
//  Created by Wujun Yang on 19/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <CoreAudio/CoreAudioTypes.h>
#import <AudioToolbox/AudioQueue.h>
#import <AudioToolbox/AudioFile.h>
#import <AudioToolbox/AudioConverter.h>

@interface AudioQ : NSObject

static void HandleInputBuffer(
                              void                                  *aqData,
                              AudioQueueRef                         inAq,
                              AudioQueueBufferRef                   inBuffer,
                              const AudioTimeStamp                  *inStartTime,
                              UInt32                                inNumPackets,
                              const AudioStreamPacketDescription    *inPacketDesc
                              );

static OSStatus BufferFilledHandler(
                                    void *                               inUserData,
                                    SInt64                               inPosition,
                                    UInt32                               requestCount,
                                    const void *                         buffer,
                                    UInt32 *                             actualCount
                                    );

-(OSStatus)initializeAQ;
-(void)stop;
-(void)start;

@end
