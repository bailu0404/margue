//
//  CRActorCell.h
//  ChatRoom
//
//  Created by Wujun Yang on 17/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface CRActorCell : UITableViewCell

@property (weak, nonatomic) IBOutlet UIView *content;
@property (weak, nonatomic) IBOutlet UIImageView *playerImage;
@property (weak, nonatomic) IBOutlet UILabel *playerUserId;
@property (weak, nonatomic) IBOutlet UIImageView *microphoneImage;

- (void) initAvatarImage;
@end
