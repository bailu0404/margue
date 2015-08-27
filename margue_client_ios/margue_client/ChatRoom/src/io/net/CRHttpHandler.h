//
//  CRHttpHandler.h
//  ChatRoom
//
//  Created by Wujun Yang on 23/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "CRSettings.h"

@interface CRHttpHandler : NSObject

@property (strong, nonatomic) NSString* boundary;

typedef void (^GetCompletionBlock)(BOOL success, NSData *response, NSError *error);
typedef void (^PostCompletionBlock)(BOOL success, NSData *response, NSError *error);
typedef void (^UploadCompletionBlock)(BOOL success, NSData *response, NSError *error);
typedef void (^DownloadCompletionBlock)(BOOL success, NSData *response, NSError *error);

- (void)httpGetRequest:(NSString *)url withCallback:(GetCompletionBlock)callback;
- (void)httpPostRequest:(NSString *)url withData:(NSData*)data withCallback:(PostCompletionBlock)callback;
- (void)httpUploadRequest:(NSString *)url withData:(NSData*)data withCallback:(UploadCompletionBlock)callback;
- (void)httpDownloadRequest:(NSString *)url withCallback:(DownloadCompletionBlock)callback;

- (NSString*) getServerUrl:(NSString*) apiName;

@end
