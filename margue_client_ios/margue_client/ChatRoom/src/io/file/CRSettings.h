//
//  CRSettings.h
//  ChatRoom
//
//  Created by Wujun Yang on 26/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface CRSettings : NSObject

@property (strong, nonatomic) NSDictionary *settingsDictionary;

+ (id)sharedSettings;

- (void) setServerUrl:(NSString *) url;
- (NSString*) getServerUrl;

@end
