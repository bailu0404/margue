//
//  CRActor.m
//  ChatRoom
//
//  Created by Wujun Yang on 16/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import "CRActor.h"

@implementation CRActor

@synthesize userId = _userId;
@synthesize photoId = _photoId;
@synthesize speaking = _speaking;
@synthesize gender = _gender;

- (CRActor*) initWithId:(NSString*) userId
{
    self.userId = userId;
    self.speaking = NO;
    return self;
}

- (NSString*) photoId
{
    if(_photoId){
        return _photoId;
    }else if([[self gender] isEqualToString:@"Female"]){
        return @"female_avatar";
    }else{
        return @"male_avatar";
    }
}

@end
