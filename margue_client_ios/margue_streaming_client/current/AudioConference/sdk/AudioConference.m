#import "AudioConference.h"

#include <fcntl.h>
#include <netdb.h>
#include <unistd.h>
#include <sys/ioccom.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>

typedef struct {
	unsigned int	size;
	unsigned char*	data;
	unsigned int	used;
} membufr;

typedef struct {
	unsigned char	start_code;
	unsigned short	length;
	unsigned char	sequence;
	unsigned char	command;
	unsigned char*	data;
} acpacket;

#define S2C_AUDIODATA		0x00
#define S2C_BROADCAST		0x01
#define S2C_CLIENTRESP		0x02

#define C2S_TALK_START		0x00
#define C2S_TALK_STOP		0x01
#define C2S_LISTEN_START	0x02
#define C2S_LISTEN_STOP		0x03
#define C2S_AUDIODATA		0x09

@interface AudioConference()
-(acpacket *)pkt_send:(UInt8)seq cmd:(UInt8)cmd data:(Byte *)data length:(UInt16)length recv:(BOOL)recv;
-(acpacket *)pkt_send:(UInt8)cmd data:(Byte *)data length:(UInt16)length recv:(BOOL)recv;
@end

@implementation AudioConference

@synthesize delegate;
@synthesize bConnected;

#pragma mark socket layer

- (BOOL)sock_connect:(NSString *)host port:(int)port
{
	sock = socket(AF_INET, SOCK_STREAM, 0);
	if (sock < 0) {
		return NO;
	}

	struct hostent *p;
	struct sockaddr_in addr;
	memset(&addr, 0, sizeof(addr));
	addr.sin_len			= sizeof(addr);
	addr.sin_family			= AF_INET;
	addr.sin_port			= htons(port);
	p = gethostbyname([host UTF8String]);
	if (NULL == p) {
		addr.sin_addr.s_addr= inet_addr([host UTF8String]);
	} else {
		memcpy(&addr.sin_addr.s_addr, p->h_addr, p->h_length);
	}
	if (connect(sock, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
		close(sock);
		return NO;
	}

	bConnected = YES;
    
    return YES;
}
- (void)sock_close
{
	close(sock);
	sock = -1;
	bConnected = NO;
	dispatch_async(dispatch_get_main_queue(), ^{
		if (self.delegate && [self.delegate respondsToSelector:@selector(disconnected:)]) {
			[self.delegate disconnected:self];
		}
	});
}
- (BOOL)sock_send:(Byte *)data length:(int)length
{
	if (sock < 0) {
		return NO;
	}

	while (length > 0) {
		int i = send(sock, data, length, 0);
		if (i < 0) {
			if ((EAGAIN == errno) || (EINTR == errno)) {
				continue;
			}
			[self sock_close];
			return NO;
		} else if (0 == i) {
			continue;
		}
		length -= i;
		data += i;
	}

	return YES;
}
- (int)sock_recv:(Byte *)buf length:(int)length
{
	if ((sock < 0) || (NULL == buf) || (length <= 0)) {
		return -1;
	}

	fd_set fds_r;
	struct timeval tv;

	tv.tv_sec	= 0;
	tv.tv_usec	= 500;
	FD_ZERO(&fds_r);
	FD_SET(sock, &fds_r);
	int n = select(sock+1, &fds_r, NULL, NULL, &tv);
	if (n <= 0) {
		if (n < 0) {
			if ((EAGAIN != errno) && (EINTR != errno)) {
				[self sock_close];
				return -2;
			}
		}
		return 0;
	}
	if (!FD_ISSET(sock, &fds_r)) {
		return 0;
	}

	int i = recv(sock, buf, length, 0);
	if (i < 0) {
		if ((EAGAIN == errno) || (EINTR == errno)) {
			return 0;
		}
		[self sock_close];
		return -3;
	}

	return i;
}

#pragma mark audio layer

-(void)aud_afmt:(AudioStreamBasicDescription *)afmt
{
	if (NULL == afmt) {
		return;
	}
	afmt->mSampleRate		= 8000.0;
	afmt->mFormatID			= kAudioFormatULaw;
	afmt->mFramesPerPacket	= 1;
	afmt->mChannelsPerFrame	= 1;
	afmt->mBytesPerFrame	= 2;
	afmt->mBytesPerPacket	= 2;
	afmt->mBitsPerChannel	= 16;
	afmt->mReserved			= 0;
	afmt->mFormatFlags		= kAudioFormatFlagIsSignedInteger|kAudioFormatFlagIsPacked;
}

// Takes a filled buffer and writes it to disk, "emptying" the buffer
void aud_recd(void* inUserData, AudioQueueRef inAQ, AudioQueueBufferRef inBuffer, const AudioTimeStamp* inStartTime, UInt32 inNumberPacketDescriptions, const AudioStreamPacketDescription* inPacketDescs)
{
	AudioConference *p = (__bridge AudioConference *)inUserData;
	if (!p->recd.recording) {
		printf("Not recording, returning\n");
		return;
	}

	//printf("Writing buffer %lld\n", recd->currentPacket);

	[p pkt_send:C2S_AUDIODATA data:(unsigned char *)inBuffer->mAudioData length:inBuffer->mAudioDataByteSize recv:FALSE];

	AudioQueueEnqueueBuffer(p->recd.queue, inBuffer, 0, NULL);
}

// Fills an empty buffer with data and sends it to the speaker
void aud_play(void* inUserData, AudioQueueRef outAQ, AudioQueueBufferRef outBuffer)
{
	AudioConference *p = (__bridge AudioConference *)inUserData;
	if (!p->play.playing) {
		printf("Not playing, returning\n");
		return;
	}

	//printf("Queuing buffer %lld for playback\n", play->currentPacket);

	int n = outBuffer->mAudioDataBytesCapacity;
	outBuffer->mAudioDataByteSize = 0;
	//outBuffer->mAudioData
	@synchronized (p) {
		if ([p->rArray count] > 0) {
			NSData *tmp = (NSData *)[p->rArray objectAtIndex:0];
			[p->rArray removeObjectAtIndex:0];
			if (Nil != tmp) {
				int n2 = [tmp length];
				unsigned char *data = (unsigned char *)[tmp bytes];
				if ((NULL != data) && (n > 0)) {
					outBuffer->mAudioDataByteSize = n2 > n ? n : n2;
					memcpy(outBuffer->mAudioData, data, outBuffer->mAudioDataByteSize);
				}
			}
		}
	}
	if (outBuffer->mAudioDataByteSize <= 0) {
		outBuffer->mAudioDataByteSize = n;
		memset(outBuffer->mAudioData, 0, outBuffer->mAudioDataByteSize);
	}
	AudioQueueEnqueueBuffer(p->play.queue, outBuffer, 0, NULL);
}

#pragma mark protocol layer

-(acpacket *)pkt_send:(UInt8)seq cmd:(UInt8)cmd data:(Byte *)data length:(UInt16)length recv:(BOOL)recv
{
	if ((NULL == data) && (length > 0)) {
		return NULL;
	}

	int n = 5+length;
	unsigned char *p = (unsigned char *)malloc(n);
	if (NULL == p) {
		return NULL;
	}
	
	memset(p, 0, n);
	p[0]	= '#';
	p[1]	= (unsigned char)((n & 0xFF00) >> 1);
	p[2]	= (unsigned char)(n & 0x00FF);
	p[3]	= (unsigned char)(seq & 0x00FF);
	p[4]	= (unsigned char)(cmd & 0x00FF);
	if (NULL != data) {
		memcpy(p+5, data, length);
	}
	BOOL b = [self sock_send:p length:n];
	free(p);
	if (b && recv) {
		for (int i=0; i<1000 && sock>=0; i++) {
			@synchronized (self) {
				acpacket *pkt = (acpacket *)last;
				if ((NULL != pkt) && (pkt->sequence == seq)) {
					last = NULL;
					return pkt;
				}
			}
			usleep(1000);
		}
	}
	return NULL;
}
-(acpacket *)pkt_send:(UInt8)cmd data:(Byte *)data length:(UInt16)length recv:(BOOL)recv
{
	return [self pkt_send:++sequence cmd:cmd data:data length:length recv:recv];
}
-(acpacket *)pkt_recv:(membufr)mb
{
	int idx = -1;
	for (int i=0; i<mb.used; i++) {
		if ('#' == mb.data[i]) {
			idx = i;
			break;
		}
	}
	if (idx < 0) {
		mb.used = 0;
		return NULL;
	}
	if ((idx+5) > mb.used) {
		if (idx > 0) {
			memmove(mb.data, mb.data+idx, mb.used-idx);
			mb.used -= idx;
		}
		return NULL;
	}
	unsigned short len = (mb.data[idx+1] << 1) + mb.data[idx+2];
	if (len > mb.used) {
		if (idx > 0) {
			memmove(mb.data, mb.data+idx, mb.used-idx);
			mb.used -= idx;
		}
		return NULL;
	}
	acpacket *pkt = (acpacket *)malloc(sizeof(acpacket));
	if (NULL == pkt) {
		return NULL;
	}
	pkt->start_code	= mb.data[idx];
	pkt->length		= len;
	pkt->sequence	= mb.data[idx+3];
	pkt->command	= mb.data[idx+4];
	if (len > 5) {
		pkt->data = (unsigned char *)malloc(pkt->length-5);
		if (NULL == pkt->data) {
			free(pkt);
			return NULL;
		}
		memcpy(pkt->data, &mb.data[idx+5], pkt->length-5);

		int n = idx+pkt->length;
		mb.used -= n;
		memmove(mb.data, mb.data+n, mb.used);
	}
	return pkt;
}
-(void)pkt_free:(acpacket *)pkt
{
	if (NULL != pkt) {
		if (NULL != pkt->data) {
			free(pkt->data);
		}
		free(pkt);
	}
}
-(BOOL)pkt_isok_and_free:(acpacket *)pkt
{
	if (NULL != pkt) {
		if ((S2C_CLIENTRESP == pkt->command) && (NULL != pkt->data) && (0x01 == pkt->data[0])) {
			[self pkt_free:pkt];
			return YES;
		}
		[self pkt_free:pkt];
	}
	return NO;
}

-(BOOL)pkt_goroom:(NSString *)host port:(UInt16)port roomID:(UInt32)roomID randCode:(NSString *)randCode
{
	char buf[256];
	membufr mbuf;
	mbuf.size	= sizeof(buf);
	mbuf.data	= (unsigned char *)buf;
	mbuf.used	= 0;

	memset(buf, 0, sizeof(buf));
	snprintf(buf, sizeof(buf)-1, "GET /Sys/AConf?roomID=%lu&randCode=%s HTTP/1.1\r\n"\
			 "Host: %s:%d\r\n"\
			 "Connection: keep-alive\r\n"\
			 "\r\n", roomID, [randCode UTF8String], [host UTF8String], port);
	if (![self sock_send:(Byte *)buf length:strlen(buf)]) {
		return NO;
	}
	for (int i=0; i<100; i++) {
		int n = [self sock_recv:mbuf.data+mbuf.used length:mbuf.size-mbuf.used];
		if (n < 0) {
			return NO;
		}
		mbuf.used += n;
		acpacket* pkt = [self pkt_recv:mbuf];
		if (NULL == pkt) {
			continue;
		}
		if ((S2C_CLIENTRESP == pkt->command) && (NULL != pkt->data) && (0x01 == pkt->data[0])) {
			[self pkt_free:pkt];
			return YES;
		}
		[self pkt_free:pkt];
		break;
	}
	return NO;
}

-(void)pkt_thread:(void *)arg
{
	Byte bufr[512];
	membufr mbuf;
	acpacket *pkt;

	memset(&mbuf, 0, sizeof(mbuf));
	mbuf.size = 1024;
	mbuf.data = (unsigned char *)malloc(mbuf.size);
	if (NULL == mbuf.data) {
		return;
	}
	while (bConnected) {
		int n = [self sock_recv:bufr length:sizeof(bufr)];
		if (n <= 0) {
			continue;
		}
		if ((mbuf.used+n) > mbuf.size) {
			unsigned int size = mbuf.used+n*2;
			unsigned char *p = (unsigned char *)realloc(mbuf.data, size);
			if (NULL == p) {
				break;
			}
			mbuf.size	= size;
			mbuf.data	= p;
		}
		memcpy(mbuf.data+mbuf.used, bufr, n);
		mbuf.used += n;
		pkt = [self pkt_recv:mbuf];
		if (NULL != pkt) {
			if (S2C_AUDIODATA == pkt->command) {
				int n = pkt->length - 5;
				if (n > 0) {
					// append the audio data into buffer, wait aud_play retrieve
					@synchronized (self) {
						if ([rArray count] > 10) {
							[rArray removeObjectAtIndex:0];
						}
						[rArray addObject:[NSData dataWithBytes:pkt->data length:n]];
					}
				}
				[self pkt_free:pkt];
			} else if (S2C_BROADCAST == pkt->command) {
				dispatch_async(dispatch_get_main_queue(), ^{
					//连接成功的回调
					if (self.delegate && [self.delegate respondsToSelector:@selector(broadcastPacket:data:length:)]) {
						[self.delegate broadcastPacket:self data:pkt->data length:pkt->length-5];
					}
					[self pkt_free:pkt];
				});
			} else {
				@synchronized (self) {
					if (NULL != last) {
						[self pkt_free:(acpacket *)last];
					}
					last = pkt;
				}
			}
		}
	}
	if (NULL != mbuf.data) {
		free(mbuf.data);
	}
}

#pragma mark export functions

-(id)init
{
	if (self = [super init]) {
		sequence = 0;
		bConnected = NO;
		rArray = [[NSMutableArray alloc]init];
	}
	return self;
}

- (void)connect:(NSString *)host port:(int)port roomID:(UInt32)roomID randCode:(NSString *)randCode
{
//	[self close];
	dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
		// socket连接服务器
		BOOL a = [self sock_connect:host port:port];
		BOOL b = [self pkt_goroom:host port:port roomID:roomID randCode:randCode];
		if (a && b) {
			dispatch_async(dispatch_get_main_queue(), ^{
				//连接成功的回调
				if (self.delegate && [self.delegate respondsToSelector:@selector(connected:)]) {
					[self.delegate connected:self];
				}
			});
			[NSThread detachNewThreadSelector:@selector(pkt_thread:) toTarget:self withObject:self];
		} else {
			[self close];
			dispatch_async(dispatch_get_main_queue(), ^{
				//连接失败
				if (self.delegate && [self.delegate respondsToSelector:@selector(conerror:)]) {
					[self.delegate conerror:self];
				}
			});
		}
	});
}

- (void)close
{
	[self closeUpStream];
	[self closeDownStream];
	[self sock_close];
}

- (BOOL)openUpStream
{
	if (recd.recording) {
		return YES;
	}

	acpacket *pkt = [self pkt_send:C2S_TALK_START data:NULL length:0 recv:TRUE];
	if (![self pkt_isok_and_free:pkt]) {
		return NO;
	}

//	[self aud_afmt:&recd.dataFormat];
	recd.dataFormat.mSampleRate			= 8000.0;
	recd.dataFormat.mFormatID			= kAudioFormatULaw;
	recd.dataFormat.mFramesPerPacket	= 1;
	recd.dataFormat.mChannelsPerFrame	= 1;
	recd.dataFormat.mBytesPerFrame		= 2;
	recd.dataFormat.mBytesPerPacket		= 2;
	recd.dataFormat.mBitsPerChannel		= 16;
	recd.dataFormat.mReserved			= 0;
	recd.dataFormat.mFormatFlags		= kAudioFormatFlagIsSignedInteger|kAudioFormatFlagIsPacked;

	recd.currentPacket = 0;

	OSStatus status;
	status = AudioQueueNewInput(&recd.dataFormat, aud_recd, (__bridge void*)self, CFRunLoopGetCurrent(), kCFRunLoopCommonModes, 0, &recd.queue);
	if (status == 0) {
		// Prime recording buffers with empty data
		for (int i = 0; i < NUM_BUFFERS; i++) {
			AudioQueueAllocateBuffer(recd.queue, 160, &recd.buffers[i]);
			AudioQueueEnqueueBuffer(recd.queue, recd.buffers[i], 0, NULL);
		}
		status = AudioQueueStart(recd.queue, NULL);
		if (status == 0) {
			recd.recording = true;
			return YES;
		}
	}
	[self closeUpStream];
	return NO;
}
- (void)closeUpStream
{
	recd.recording = false;
	[self pkt_send:C2S_TALK_STOP data:NULL length:0 recv:FALSE];
	AudioQueueStop(recd.queue, true);
	for(int i = 0; i < NUM_BUFFERS; i++) {
		AudioQueueFreeBuffer(recd.queue, recd.buffers[i]);
	}
	AudioQueueDispose(recd.queue, true);
}

- (BOOL)openDownStream
{
	if (play.playing) {
		return YES;
	}

	acpacket *pkt = [self pkt_send:C2S_LISTEN_START data:NULL length:0 recv:TRUE];
	if (![self pkt_isok_and_free:pkt]) {
		return NO;
	}

//	[self aud_afmt:&play.dataFormat];
	play.dataFormat.mSampleRate			= 8000.0;
	play.dataFormat.mFormatID			= kAudioFormatULaw;
	play.dataFormat.mFramesPerPacket	= 1;
	play.dataFormat.mChannelsPerFrame	= 1;
	play.dataFormat.mBytesPerFrame		= 1;
	play.dataFormat.mBytesPerPacket		= 1;
	play.dataFormat.mBitsPerChannel		= 8;
	play.dataFormat.mReserved			= 0;
	play.dataFormat.mFormatFlags		= kAudioFormatFlagIsSignedInteger|kAudioFormatFlagIsPacked;

	play.currentPacket = 0;

	OSStatus status;
	status = AudioQueueNewOutput(&play.dataFormat, aud_play, (__bridge void*)self, CFRunLoopGetCurrent(), kCFRunLoopCommonModes, 0, &play.queue);
	if (status == 0) {
		play.playing = true;
		// Allocate and prime playback buffers
		for (int i = 0; i < NUM_BUFFERS; i++) {
			AudioQueueAllocateBuffer(play.queue, 320, &play.buffers[i]);
			// initial audio data
			play.buffers[i]->mAudioDataByteSize = 0;
			memset(play.buffers[i]->mAudioData, 0, play.buffers[i]->mAudioDataBytesCapacity);
			aud_play((__bridge void*)self, play.queue, play.buffers[i]);
//			AudioQueueEnqueueBuffer(play.queue, play.buffers[i], 0, NULL);
		}
		status = AudioQueueStart(play.queue, NULL);
		if (status == 0) {
			return YES;
		} else {
			play.playing = false;
		}
	}
	[self closeDownStream];
	return NO;
}
- (void)closeDownStream
{
    play.playing = false;
	[self pkt_send:C2S_LISTEN_STOP data:NULL length:0 recv:FALSE];
	for(int i = 0; i < NUM_BUFFERS; i++) {
		AudioQueueFreeBuffer(play.queue, play.buffers[i]);
	}
	AudioQueueDispose(play.queue, true);
}

@end