//
//  CRViewController.h
//  ChatRoom
//
//  Created by Wujun Yang on 16/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import <CoreAudio/CoreAudioTypes.h>

#import "CRActor.h"
#import "CRActorCell.h"
#import "CRUtilities.h"
#import "CRHttpHandler.h"

@interface CRViewController : UIViewController <UITableViewDelegate, UITableViewDataSource, AVAudioPlayerDelegate>

@property (strong, nonatomic) IBOutlet UIView *view;

@property (weak, nonatomic) IBOutlet UIView *menuBar;
@property (weak, nonatomic) IBOutlet UIImageView *imageButton_1;

@property (weak, nonatomic) IBOutlet UITableView *tableView;


@property (strong, nonatomic) NSMutableArray *actorsArrayTeam1;
@property (strong, nonatomic) NSMutableArray *actorsArrayTeam2;



@property (strong, nonatomic) AVAudioPlayer *audioPlayer;
@property (strong, nonatomic) AVAudioRecorder *audioRecorder;

@end
