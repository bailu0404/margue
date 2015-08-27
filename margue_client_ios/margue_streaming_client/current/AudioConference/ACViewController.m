//
//  ACViewController.m
//  AudioConference
//
//  Created by joson zhang on 10/31/14.
//  Copyright (c) 2014 Joson_Zhang. All rights reserved.
//

#import "ACViewController.h"
#import "sdk/AudioConference.h"

@interface ACViewController ()<AudioConferenceDelegate,UITextViewDelegate>
{
	bool	recding;
	bool	playing;
    AudioConference *ac;
}
@end

@implementation ACViewController

@synthesize tfIP;
@synthesize tfRoomID;
@synthesize bnOK;
@synthesize laStatus;
@synthesize bnTalk;
@synthesize bnListen;

- (void)viewDidLoad
{
    [super viewDidLoad];

	// Do any additional setup after loading the view, typically from a nib.
	recding = false;
	playing = false;

	bnTalk.hidden = true;
	bnListen.hidden = true;

	laStatus.text = @"";
}

- (void)viewDidUnload
{
    [self setTfIP:nil];
    [self setTfRoomID:nil];
    [self setLaStatus:nil];
	[self setBnOK:nil];
	[self setBnTalk:nil];
	[self setBnListen:nil];
    [super viewDidUnload];
    // Release any retained subviews of the main view.
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    return (interfaceOrientation != UIInterfaceOrientationPortraitUpsideDown);
}

- (IBAction)bnOKPressed:(UIButton *)sender {
	laStatus.text = @"";

	if ([tfIP.text length] <= 0) {
		tfIP.selected = true;
		return;
	}
	if ([tfRoomID.text length] <= 0) {
		tfRoomID.selected = true;
		return;
	}

	laStatus.text = @"Connecting ...";

	NSString *host = tfIP.text;
	UInt32 rid = (UInt32)atoi([tfRoomID.text UTF8String]);

	ac = [[AudioConference alloc] init];
	ac.delegate = self;
	[ac connect:host port:8000 roomID:rid randCode:@"abc-xyz"];
}

- (IBAction)bnTalkPressed:(UIButton *)sender {
	if (recding) {
		recding = false;
		[ac closeUpStream];
	} else {
		recding = [ac openUpStream];
	}
}

- (IBAction)bnListenPressed:(UIButton *)sender {
	if (playing) {
		playing = false;
		[ac closeDownStream];
	} else {
		playing = [ac openDownStream];
	}
}

#pragma mark - AudioConferenceDelegate
//连接失败
- (void)conerror:(AudioConference *)ac
{
	bnOK.hidden = false;
	bnTalk.hidden = true;
	bnListen.hidden = true;

	laStatus.text = @"Failed to connect server!";    
}
//连接成功
- (void)connected:(AudioConference *)ac
{
	bnOK.hidden = true;
	bnTalk.hidden = false;
	bnListen.hidden = false;

	laStatus.text = @"Connected!";
}
//断开链接
- (void)disconnected:(AudioConference *)ac
{
	bnOK.hidden = false;
	bnTalk.hidden = true;
	bnListen.hidden = true;

    laStatus.text = @"Disconnected";
}

//收到消息
- (void)broadcastPacket:(AudioConference *)ac data:(Byte *)data length:(int)length
{
	//laStatus.text = message;
}


- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
