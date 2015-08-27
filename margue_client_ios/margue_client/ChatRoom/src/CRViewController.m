//
//  CRViewController.m
//  ChatRoom
//
//  Created by Wujun Yang on 16/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import "CRViewController.h"

@implementation CRViewController

static NSInteger SPEECH_BUTTON = 1;

int recordEncoding;
enum
{
    ENC_AAC = 1,
    ENC_ALAC = 2,
    ENC_IMA4 = 3,
    ENC_ILBC = 4,
    ENC_ULAW = 5,
    ENC_PCM = 6,
} encodingTypes;


#pragma ViewController lifecycle

- (void)viewWillLayoutSubviews
{
    CGRect topViewFrame = [self.view frame];
    int screenHeight = topViewFrame.size.height;
    
    CGRect frame = [self.menuBar frame];
    int menuBarHeight = frame.size.height;
    frame.origin.y = screenHeight - menuBarHeight;
    self.menuBar.frame = frame;
    
    CGRect frameTable = [self.tableView frame];
    frameTable.size.height = screenHeight - menuBarHeight -20;
    frameTable.origin.y = 20;
    self.tableView.frame = frameTable;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [self initSpeakerButton];
    [self loadDebators];
    
}
    
- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void) dealloc {
}

#pragma initialization

// initialize the button
- (void) initSpeakerButton
{
    self.imageButton_1.layer.cornerRadius = self.imageButton_1.frame.size.height /2;
    self.imageButton_1.layer.masksToBounds = YES;
    self.imageButton_1.layer.borderWidth = 0;
    [self.imageButton_1 setUserInteractionEnabled:YES];
    [self.imageButton_1 setTag:SPEECH_BUTTON];
}

- (void) loadDebators
{
    CRActor* actor1 = [[CRActor alloc] initWithId:@"Wujun"];
    CRActor* actor2 = [[CRActor alloc] initWithId:@"Bai Lu"];
    CRActor* actor3 = [[CRActor alloc] initWithId:@"Lily"];
    CRActor* actor4 = [[CRActor alloc] initWithId:@"James"];
    
    [actor1 setGender:@"Male"];
    [actor2 setGender:@"Male"];
    [actor3 setGender:@"Female"];
    [actor4 setGender:@"Male"];
    
    [actor1 setSpeaking:YES];
    [actor2 setSpeaking:NO];
    [actor3 setSpeaking:NO];
    [actor4 setSpeaking:NO];
    
    [actor1 setPhotoId:@"wujun_1.jpg"];
    
    self.actorsArrayTeam1 = [[NSMutableArray alloc] init];
    [self.actorsArrayTeam1 addObject:actor1];
    [self.actorsArrayTeam1 addObject:actor2];
    
    self.actorsArrayTeam2 = [[NSMutableArray alloc] init];
    [self.actorsArrayTeam2 addObject:actor3];
    [self.actorsArrayTeam2 addObject:actor4];
    
    [self.tableView reloadData];
}

#pragma TableView implementation

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return 60;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 2;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if(section == 0){
        return [self.actorsArrayTeam1 count];
    }else{
        return [self.actorsArrayTeam2 count];
    }
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    CRActorCell * cell = [tableView dequeueReusableCellWithIdentifier:@"CRActorCell"];
    
    if(!cell){
        [tableView registerNib:[UINib nibWithNibName:@"CRActorCell" bundle:nil] forCellReuseIdentifier:@"CRActorCell"];
        cell = [tableView dequeueReusableCellWithIdentifier:@"CRActorCell"];
    }
    [cell initAvatarImage];
    
    CRActor *actor = nil;
    
    if(indexPath.section==0){
        actor = [self.actorsArrayTeam1 objectAtIndex:indexPath.row];
    }else{
        actor = [self.actorsArrayTeam2 objectAtIndex:indexPath.row];
    }
    
    [cell.playerImage setImage: [UIImage imageNamed:[actor photoId]]];
    cell.playerUserId.text = [actor userId];
    if([[actor gender] isEqualToString:@"Female"]){
        cell.playerImage.backgroundColor = [CRUtilities colorFromHexString:@"#FFB6C1"];
    }
    [cell setSelected:actor.speaking animated:NO];
    
    return cell;
}

- (void) tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    
}

#pragma TableView header
- (UIView *) tableView:(UITableView *)tableView viewForHeaderInSection:(NSInteger)section
{
    CGRect topViewFrame = [self.view frame];
    int screenWidth = topViewFrame.size.width;
    
    CGRect frame = CGRectMake(15, 0, screenWidth-30, 30);
    UIView* view = [[UIView alloc] initWithFrame:frame];
    view.backgroundColor = [CRUtilities colorFromHexString:@"#123456"];
    UILabel* headerLabel = [[UILabel alloc] initWithFrame:CGRectMake(20, 5, frame.size.width-10, 20)];
    [headerLabel setTextColor:[CRUtilities colorFromHexString:@"#FFFFFF"]];
    headerLabel.font = [UIFont fontWithName:@"TimesNewRomanPS-ItalicMT" size:18.0f];
    if(section == 0){
        [headerLabel setText:@"Proposition"];
    }else{
        [headerLabel setText:@"Opposition"];
    }
    [view addSubview:headerLabel];
    
    return view;
}

- (CGFloat) tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section
{
    return 30;
}


#pragma audio recorder functions

- (void) startRecording
{
    self.audioRecorder = nil;
    
    // Init audio with record capability
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    [audioSession setCategory:AVAudioSessionCategoryRecord error:nil];
    
    NSMutableDictionary *recordSettings = [[NSMutableDictionary alloc] initWithCapacity:10];
    if(recordEncoding == ENC_PCM)
    {
        [recordSettings setObject:[NSNumber numberWithInt: kAudioFormatLinearPCM] forKey: AVFormatIDKey];
        [recordSettings setObject:[NSNumber numberWithFloat:44100.0] forKey: AVSampleRateKey];
        [recordSettings setObject:[NSNumber numberWithInt:2] forKey:AVNumberOfChannelsKey];
        [recordSettings setObject:[NSNumber numberWithInt:16] forKey:AVLinearPCMBitDepthKey];
        [recordSettings setObject:[NSNumber numberWithBool:NO] forKey:AVLinearPCMIsBigEndianKey];
        [recordSettings setObject:[NSNumber numberWithBool:NO] forKey:AVLinearPCMIsFloatKey];
    }
    else
    {
        NSNumber *formatObject;
        
        switch (recordEncoding)
        {
            case (ENC_AAC):
                formatObject = [NSNumber numberWithInt: kAudioFormatMPEG4AAC];
                break;
            case (ENC_ALAC):
                formatObject = [NSNumber numberWithInt: kAudioFormatAppleLossless];
                break;
            case (ENC_IMA4):
                formatObject = [NSNumber numberWithInt: kAudioFormatAppleIMA4];
                break;
            case (ENC_ILBC):
                formatObject = [NSNumber numberWithInt: kAudioFormatiLBC];
                break;
            case (ENC_ULAW):
                formatObject = [NSNumber numberWithInt: kAudioFormatULaw];
                break;
            default:
                formatObject = [NSNumber numberWithInt: kAudioFormatAppleIMA4];
        }
        
        [recordSettings setObject:formatObject forKey: AVFormatIDKey];
        [recordSettings setObject:[NSNumber numberWithFloat:44100.0] forKey: AVSampleRateKey];
        [recordSettings setObject:[NSNumber numberWithInt:2] forKey:AVNumberOfChannelsKey];
        [recordSettings setObject:[NSNumber numberWithInt:12800] forKey:AVEncoderBitRateKey];
        [recordSettings setObject:[NSNumber numberWithInt:16] forKey:AVLinearPCMBitDepthKey];
        [recordSettings setObject:[NSNumber numberWithInt: AVAudioQualityMedium] forKey: AVEncoderAudioQualityKey];
    }
    
    NSURL *url = [NSURL fileURLWithPath:[NSString stringWithFormat:@"%@/recordTest.caf", [[NSBundle mainBundle] resourcePath]]];
    
    NSError *error = nil;
    self.audioRecorder = [[AVAudioRecorder alloc] initWithURL:url settings:recordSettings error:&error];
    
    if ([self.audioRecorder prepareToRecord] == YES){
        [self.audioRecorder record];
    }else {
        int errorCode = CFSwapInt32HostToBig ([error code]);
        NSLog(@"Error: %@ [%4.4s])" , [error localizedDescription], (char*)&errorCode);
        
    }
}

-(void) stopRecording
{
    [self.audioRecorder stop];
}

-(void) startPlaying
{
    // Init audio with playback capability
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    [audioSession setCategory:AVAudioSessionCategoryPlayback error:nil];
    
    NSURL *url = [NSURL fileURLWithPath:[NSString stringWithFormat:@"%@/recordTest.caf", [[NSBundle mainBundle] resourcePath]]];
    NSError *error;
    self.audioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:url error:&error];
    self.audioPlayer.numberOfLoops = 0;
    [self.audioPlayer setDelegate:self];
    [self.audioPlayer play];
}

-(void) stopPlaying
{
    [self.audioPlayer stop];
}

#pragma audio player delegate

- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag
{
    NSLog(@"Track completed!");
}

/* if an error occurs while decoding it will be reported to the delegate. */
- (void)audioPlayerDecodeErrorDidOccur:(AVAudioPlayer *)player error:(NSError *)error
{
    
}


#pragma Tap Gesture

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    UITouch* touch = [[event allTouches] anyObject];
    if([touch view] == [self.imageButton_1 viewWithTag:SPEECH_BUTTON]){
        NSLog(@"Start talking");
        [self startRecording];
    }
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    UITouch* touch = [[event allTouches] anyObject];
    if([touch view] == [self.imageButton_1 viewWithTag:SPEECH_BUTTON]){
        NSLog(@"End talking");
        [self stopRecording];
        //[self startPlaying];

        NSString* filePath = [NSString stringWithFormat:@"%@/recordTest.caf", [[NSBundle mainBundle] resourcePath]];
        NSData* fileData = [NSData dataWithContentsOfFile:filePath];
        
        UIImage* img = [UIImage imageNamed:@"wujun_1.jpg"];
        NSData *imgData= UIImageJPEGRepresentation(img, 1);
        
        CRHttpHandler *httpHandler = [[CRHttpHandler alloc] init];
        NSString *url = @"upload";
        
        NSString* test = @"1234567890qwertyuiosdfgh";
        NSData* data = [test dataUsingEncoding:NSUTF8StringEncoding];
        
        /*[httpHandler httpGetRequest:url withCallback:^(BOOL success, NSData *response, NSError *error)
         {
             if (success)
             {
                 NSLog(@"Request successful");
                 NSString *ret = [[NSString alloc] initWithData:response encoding:NSUTF8StringEncoding];
                 NSLog(@"%@", ret);
             }
             else
             {
                 NSLog(@"Request failed");
             }
         }];*/
        [httpHandler httpUploadRequest:url withData: imgData withCallback:^(BOOL success, NSData *response, NSError *error)
        {
            if (success)
            {
                NSLog(@"Request successful");
                NSString *ret = [[NSString alloc] initWithData:response encoding:NSUTF8StringEncoding];
                NSLog(@"%@", ret);
            }
            else
            {
                NSLog(@"Request failed");
            }
        }];
    }
}

@end
