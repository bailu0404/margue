//
//  CRActorCell.m
//  ChatRoom
//
//  Created by Wujun Yang on 17/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import "CRActorCell.h"

@implementation CRActorCell

- (void) initAvatarImage
{
    self.playerImage.layer.cornerRadius = self.playerImage.frame.size.height /2;
    self.playerImage.layer.masksToBounds = YES;
    self.playerImage.layer.borderWidth = 0;
    
    self.content.layer.shadowColor = [[UIColor blackColor] CGColor];
    self.content.layer.shadowOffset = CGSizeMake(1.0f, 1.0f);
    self.content.layer.shadowRadius = 2.0f;
    self.content.layer.shadowOpacity = 1.0f;
    
    self.playerUserId.font = [UIFont fontWithName:@"TimesNewRomanPS-ItalicMT" size:14.0f];
}

- (void)awakeFromNib
{
    // Initialization code
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated
{
    //[super setSelected:selected animated:animated];
    if(!selected){
        self.content.layer.shadowColor = [[UIColor blackColor] CGColor];
        self.content.layer.shadowOffset = CGSizeMake(1.0f, 1.0f);
        self.content.layer.shadowRadius = 2.0f;
        self.content.layer.shadowOpacity = 1.0f;
        
        self.microphoneImage.hidden = YES;
    }else{
        self.content.layer.shadowColor = [[UIColor blueColor] CGColor];
        self.content.layer.shadowOffset = CGSizeMake(1.5f, 1.5f);
        self.content.layer.shadowRadius = 3.0f;
        self.content.layer.shadowOpacity = 1.0f;
        
        self.microphoneImage.hidden = NO;
    }
}

@end
