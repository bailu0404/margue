//
//  ACViewController.h
//  AudioConference
//
//  Created by joson zhang on 10/31/14.
//  Copyright (c) 2014 Joson_Zhang. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface ACViewController : UIViewController

@property (weak, nonatomic) IBOutlet UITextField *tfIP;
@property (weak, nonatomic) IBOutlet UITextField *tfRoomID;

@property (weak, nonatomic) IBOutlet UIButton *bnOK;
@property (weak, nonatomic) IBOutlet UIButton *bnTalk;
@property (weak, nonatomic) IBOutlet UIButton *bnListen;

@property (weak, nonatomic) IBOutlet UILabel *laStatus;

- (IBAction)bnOKPressed:(UIButton *)sender;
- (IBAction)bnTalkPressed:(UIButton *)sender;
- (IBAction)bnListenPressed:(UIButton *)sender;

@end
