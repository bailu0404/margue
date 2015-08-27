//
//  CRActor.h
//  ChatRoom
//
//  Created by Wujun Yang on 16/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface CRActor : NSObject

@property (strong, nonatomic) NSString *userId;
@property (strong, nonatomic) NSString *photoId;
@property (strong, nonatomic) NSString *gender;
@property (nonatomic) BOOL speaking;

- (CRActor*) initWithId:(NSString*) userId;

@end
