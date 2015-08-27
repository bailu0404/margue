#import <Foundation/Foundation.h>
#import <AudioToolbox/AudioQueue.h>
#import <AudioToolbox/AudioFile.h>

#define NUM_BUFFERS 3
#define SECONDS_TO_RECORD 10

// Struct defining recording state
typedef struct {
	AudioStreamBasicDescription	dataFormat;
	AudioQueueRef				queue;
	AudioQueueBufferRef			buffers[NUM_BUFFERS];
	SInt64						currentPacket;
	bool						recording;    
} RecdInfo;

// Struct defining playback state
typedef struct {
	AudioStreamBasicDescription	dataFormat;
	AudioQueueRef				queue;
	AudioQueueBufferRef			buffers[NUM_BUFFERS];
	SInt64						currentPacket;
	bool						playing;
} PlayInfo;

@protocol DHSocketDelegate;

@interface AudioConference : NSObject {
	int							sock;
	BOOL						bConnected;

	UInt8						sequence;
	void*						last;	// last received packet

	NSMutableArray				*rArray;

	RecdInfo					recd;
	PlayInfo					play;
}

@property BOOL bConnected;
@property (retain, nonatomic) id delegate;

// connect to server
- (void)connect:(NSString *)host port:(int)port roomID:(UInt32)roomID randCode:(NSString *)randCode;
// close the connection
- (void)close;
// open upload stream channel
- (BOOL)openUpStream;
// close upload stream channel
- (void)closeUpStream;
// open download stream channel
- (BOOL)openDownStream;
// close download stream channel
- (void)closeDownStream;

@end


@protocol AudioConferenceDelegate <NSObject>

- (void)conerror:(AudioConference *)ac;
- (void)connected:(AudioConference *)ac;
- (void)disconnected:(AudioConference *)ac;

- (void)broadcastPacket:(AudioConference *)ac data:(Byte *)data length:(int)length;

@end
