//
//  CRSettings.m
//  ChatRoom
//
//  Created by Wujun Yang on 26/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import "CRSettings.h"

@implementation CRSettings

#pragma mark Singleton Methods

+ (id)sharedSettings {
    static CRSettings *sharedSetting = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedSetting = [[self alloc] init];
    });
    return sharedSetting;
}

- (id)init {
    if (self = [super init]) {
        NSString *plistCatPath = [[NSBundle mainBundle] pathForResource:@"Settings" ofType:@"plist"];
        self.settingsDictionary = [[NSDictionary alloc] initWithContentsOfFile:plistCatPath];
    }
    return self;
}

- (void)dealloc {
    // Should never be called, but just here for clarity really.
}


#pragma methods

- (void) setServerUrl:(NSString *) url
{
    [self.settingsDictionary setValue:url forKey:@"SERVER_URL"];
}

- (NSString*) getServerUrl
{
    return [self.settingsDictionary objectForKey:@"SERVER_URL"];
}

@end
