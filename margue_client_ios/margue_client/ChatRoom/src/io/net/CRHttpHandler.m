//
//  CRHttpHandler.m
//  ChatRoom
//
//  Created by Wujun Yang on 23/8/14.
//  Copyright (c) 2014 MyCompany. All rights reserved.
//

#import "CRHttpHandler.h"

@implementation CRHttpHandler

@synthesize boundary = _boundary;

- (CRHttpHandler*)init
{
    self.boundary = @"MULTIPART_FORM_BOUNDARY";
    return self;
}

#pragma defined the response block type

void(^httpGetResponseBlock)(BOOL success, NSData *response, NSError *error);
void(^httpPostResponseBlock)(BOOL success, NSData *response, NSError *error);
void(^httpUploadResponseBlock)(BOOL success, NSData *response, NSError *error);
void(^httpDownloadResponseBlock)(BOOL success, NSData *response, NSError *error);

#pragma implement the requests

- (void)httpGetRequest:(NSString *)url withCallback:(GetCompletionBlock)callback
{
    httpGetResponseBlock = callback;
 
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setHTTPMethod:@"GET"];
    [request setURL:[NSURL URLWithString:[self getServerUrl:url]]];

    dispatch_queue_t taskQ = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
     
    dispatch_async(taskQ, ^{
        
        NSError *error = nil;
        NSHTTPURLResponse *responseCode = nil;
        NSData *oResponseData = [NSURLConnection sendSynchronousRequest:request returningResponse:&responseCode error:&error];
        
        dispatch_sync(dispatch_get_main_queue(), ^{
            if(error!=nil){
                NSString* errorMsg = [error description];
                NSData* data = [errorMsg dataUsingEncoding:NSUTF8StringEncoding];
                [self onGetResponse:data withSuccess:NO error:error];
            }
            if([responseCode statusCode] != 200){
                NSString* str = [NSString stringWithFormat:@"Error getting %@, HTTP status code %i", url, [responseCode statusCode]];
                NSData* data = [str dataUsingEncoding:NSUTF8StringEncoding];
                [self onGetResponse:data withSuccess:NO error:nil];
            }else{
                [self onGetResponse:oResponseData withSuccess:YES error:nil];
            }
        });
    });
}

// --------------
- (void)httpPostRequest:(NSString *)url withData:(NSData*)postData withCallback:(GetCompletionBlock)callback
{
    httpPostResponseBlock = callback;

    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
    [request setHTTPMethod:@"POST"];
    [request setURL:[NSURL URLWithString:[self getServerUrl:url]]];
    
    dispatch_queue_t taskQ = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
    
    dispatch_async(taskQ, ^{
        
        NSError *error = nil;
        NSHTTPURLResponse *responseCode = nil;
        NSData *oResponseData = [NSURLConnection sendSynchronousRequest:request returningResponse:&responseCode error:&error];
        
        dispatch_sync(dispatch_get_main_queue(), ^{
            if(error!=nil){
                NSString* errorMsg = [error description];
                NSData* data = [errorMsg dataUsingEncoding:NSUTF8StringEncoding];
                [self onPostResponse:data withSuccess:NO error:error];
            }
            if([responseCode statusCode] != 200){
                NSString* str = [NSString stringWithFormat:@"Error getting %@, HTTP status code %i", url, [responseCode statusCode]];
                NSData* data = [str dataUsingEncoding:NSUTF8StringEncoding];
                [self onPostResponse:data withSuccess:NO error:nil];
            }else{
                [self onPostResponse:oResponseData withSuccess:YES error:nil];
            }
        });
    });
}

- (void)httpUploadRequest:(NSString *)url withData:(NSData*)uploadData withCallback:(UploadCompletionBlock)callback
{
    httpUploadResponseBlock = callback;
    
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
	[request setURL:[NSURL URLWithString:[self getServerUrl:url]]];
	[request setHTTPMethod:@"POST"];
    
    NSString* contentType = @"image/jpg";
    NSString* fileName = @"wujun.jpg";
    [request addValue:contentType forHTTPHeaderField:@"content-type"];
    [request addValue:fileName forHTTPHeaderField:@"filename"];

    NSMutableData *body = [NSMutableData data];
    [body appendData:uploadData];
    [request setHTTPBody:body];
    
    dispatch_queue_t taskQ = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
    
    dispatch_async(taskQ, ^{
        NSError *error = nil;
        NSHTTPURLResponse *responseCode = nil;
        // now lets make the connection to the web
        NSData *oResponseData = [NSURLConnection sendSynchronousRequest:request returningResponse:&responseCode error:&error];
        
        dispatch_sync(dispatch_get_main_queue(), ^{
            if(error!=nil){
                NSString* errorMsg = [error description];
                NSData* data = [errorMsg dataUsingEncoding:NSUTF8StringEncoding];
                [self onUploadResponse:data withSuccess:NO error:error];
            }
            if([responseCode statusCode] != 200){
                NSString* str = [NSString stringWithFormat:@"Error getting %@, HTTP status code %i", url, [responseCode statusCode]];
                NSData* data = [str dataUsingEncoding:NSUTF8StringEncoding];
                [self onUploadResponse:data withSuccess:NO error:nil];
            }else{
                [self onUploadResponse:oResponseData withSuccess:YES error:nil];
            }
        });
    });
}

- (void)httpDownloadRequest:(NSString *)url withCallback:(DownloadCompletionBlock)callback
{
    httpDownloadResponseBlock = callback;
    
    dispatch_queue_t taskQ = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
    
    dispatch_async(taskQ, ^{
  
        dispatch_sync(dispatch_get_main_queue(), ^{
            
        });
    });
}

#pragma call-back

- (void)onGetResponse:(NSData *)response withSuccess:(BOOL)success error:(NSError *)error
{
    httpGetResponseBlock(success, response, error);
}

- (void)onPostResponse:(NSData *)response withSuccess:(BOOL)success error:(NSError *)error
{
    httpPostResponseBlock(success, response, error);
}

- (void)onUploadResponse:(NSData *)response withSuccess:(BOOL)success error:(NSError *)error
{
    httpUploadResponseBlock(success, response, error);
}

- (void)onDownloadResponse:(NSData *)response withSuccess:(BOOL)success error:(NSError *)error
{
    httpDownloadResponseBlock(success, response, error);
}

#pragma Form ServerUrl
- (NSString*) getServerUrl:(NSString*) apiName
{
    return [NSString stringWithFormat:@"%@%@", [[CRSettings sharedSettings] getServerUrl], apiName];
}

@end
