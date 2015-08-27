/******************************************************************************
 * File:	Base64.java
 * Date:	2011/08/19
 * Author:	Josn_Zhang
 * Description:
 *	Base 64 Encoder/Decoder implement for java
 *
 *			Copyright 2011 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2011/08/19:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.lib.crypto.base64;

import java.io.*;

public class Base64 {
	private static char[] base64EncodeChars = new char[] {
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
		'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
		'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
		'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
		'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
		'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
		'w', 'x', 'y', 'z', '0', '1', '2', '3',
		'4', '5', '6', '7', '8', '9', '+', '/' };

	public static String encode(String str) {
		if (null == str) {
			return null;
		}

		int i = 0;
		int b1;
		int b2;
		int b3;
		int len;
		byte[] data = null;
		StringBuffer sb = new StringBuffer();

		data = str.getBytes();
		len = data.length;
		while (i < len) {
			b1 = data[i++] & 0xff;
			if (i == len) {
				sb.append(base64EncodeChars[b1 >>> 2]);
				sb.append(base64EncodeChars[(b1 & 0x3) << 4]);
				sb.append("==");
				break;
			}
			b2 = data[i++] & 0xff;
			if (i == len) {
				sb.append(base64EncodeChars[b1 >>> 2]);
				sb.append(base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
				sb.append(base64EncodeChars[(b2 & 0x0f) << 2]);
				sb.append("=");
				break;
			}
			b3 = data[i++] & 0xff;
			sb.append(base64EncodeChars[b1 >>> 2]);
			sb.append(base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
			sb.append(base64EncodeChars[((b2 & 0x0f) << 2) | ((b3 & 0xc0) >>> 6)]);
			sb.append(base64EncodeChars[b3 & 0x3f]);
		}
		return sb.toString();
	}

    private static byte[] base64DecodeChars = new byte[] {  
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,  
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,  
        -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,  
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,  
        -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,  
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1};
    
    public static String decode(String str) {
    	if (null == str) {
			return null;
		} 
        
    	int i = 0;
		int b1;
		int b2;
		int b3;
		int b4;
		int len;
		byte[] data = null;
		StringBuffer sb = new StringBuffer();
		
		data = str.getBytes();  
		len = data.length;  

		while (i < len) {  
        	/*   b1   */  
			do {  
				b1 = base64DecodeChars[data[i++]];  
			} while (i < len && b1 == -1);  
        	
        	if (b1 == -1) break;  
        	/*   b2   */  
        	do {  
        		b2 = base64DecodeChars[data[i++]];  
        	} while (i < len && b2 == -1);  
        	if (b2 == -1) break;  
        	sb.append((char)((b1 << 2) | ((b2 & 0x30) >>> 4)));  
        	/*   b3   */  
        	do {  
        		b3 = data[i++];  
        		if (b3 == 61) return sb.toString();  
        		b3 = base64DecodeChars[b3];  
        	} while (i < len && b3 == -1);  
        	if (b3 == -1) break;  
        	sb.append((char)(((b2 & 0x0f) << 4) | ((b3 & 0x3c) >>> 2)));  
        	/*   b4   */  
        	do {  
        		b4 = data[i++];  
        		if (b4 == 61) return sb.toString();  
        		b4 = base64DecodeChars[b4];  
        	} while (i < len && b4 == -1);  
        	if (b4 == -1) break;  
        	sb.append((char)(((b3 & 0x03) << 6) | b4));  
        }  
        return sb.toString();  
    }   

	static final byte[] key = {
		(byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F', (byte)'G', (byte)'H',
		(byte)'I', (byte)'J', (byte)'K', (byte)'L', (byte)'M', (byte)'N', (byte)'O', (byte)'P',
		(byte)'Q', (byte)'R', (byte)'S', (byte)'T', (byte)'U', (byte)'V', (byte)'W', (byte)'X',
		(byte)'Y', (byte)'Z', (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f',
		(byte)'g', (byte)'h', (byte)'i', (byte)'j', (byte)'k', (byte)'l', (byte)'m', (byte)'n',
		(byte)'o', (byte)'p', (byte)'q', (byte)'r', (byte)'s', (byte)'t', (byte)'u', (byte)'v',
		(byte)'w', (byte)'x', (byte)'y', (byte)'z', (byte)'0', (byte)'1', (byte)'2', (byte)'3',
		(byte)'4', (byte)'5', (byte)'6', (byte)'7', (byte)'8', (byte)'9', (byte)'+', (byte)'/',
		(byte)'='};
	public static byte[] decodeToByteArray(String str) {
		byte[] i_buf = str.getBytes();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		//These are the 3 bytes to be encoded
		int chr1 = 0;
		int chr2 = 0;
		int chr3 = 0;

		int i = 0;
		int j = 0; //Position counter

		//These are the 4 encoded bytes
		int enc1 = 0;
		int enc2 = 0;
		int enc3 = 0;
		int enc4 = 0; 

		// remove all characters that are not A-Z, a-z, 0-9, +, /, or =
		//var base64test = /[^A-Za-z0-9\+\/\=]/g;
		//inp = inp.replace(/[^A-Za-z0-9\+\/\=]/g, "");

		String skey = new String(key);

		do {	//Here's the decode loop.
			//Grab 4 bytes of encoded content.
			enc1 = skey.indexOf((int)i_buf[i++]);
			if (i < i_buf.length) {
				enc2 = skey.indexOf((int)i_buf[i++]);
			}
			if (i < i_buf.length) {
				enc3 = skey.indexOf((int)i_buf[i++]);
			}
			if (i < i_buf.length) {
				enc4 = skey.indexOf((int)i_buf[i++]);
			}

			//Heres the decode part. There's really only one way to do it.
			chr1 = (byte)((enc1 << 2) | (enc2 >>> 4));
			chr2 = (byte)(((enc2 & 15) << 4) | (enc3 >>> 2));
			chr3 = (byte)(((enc3 & 3) << 6) | enc4);

			//Start to output decoded content
			//out = out + String.fromCharCode(chr1);

			//strcat(out, (char)chr1);
			baos.write(chr1);
			j++;

			if (64 != enc3) {
				//out = out + String.fromCharCode(chr2);
				//strcat(out, (char)chr2);
				baos.write(chr2);
				j++;
			}
			if (64 != enc4) {
				//out = out + String.fromCharCode(chr3);
				//strcat(out, (char)chr3);
				baos.write(chr3);
				j++;
			}

			//now clean out the variables used
			chr1 = 0;
			chr2 = 0;
			chr3 = 0;

			enc1 = 0;
			enc2 = 0;
			enc3 = 0;
			enc4 = 0;
		} while (i < i_buf.length);	//finish off the loop


		//Now return the decoded values.
		return baos.toByteArray();
	}

/**********************************************************
	public static void main(String[] args) {
		String s = "hello world!";
		s = Base64.encode(s);
		System.out.println("Encode: "+s);
		s = Base64.decode(s);
		System.out.println("Decode: "+s);
	}
 *********************************************************/
}
