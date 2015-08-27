/******************************************************************************
 * File:	Util.java
 * Date:	2009/02/23
 * Author:	Joson_Zhang
 * Description:
 *	Util class implementation for ALL (J2ME, J2SE and J2EE)
 *
 *			Copyright 2009 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2009/02/23:	Joson_Zhang
 *		1. initial create
 *	2009/06/06:	Joson_Zhang
 *		1. Fixed bug compile error when using CLDC 1.0 condition.
 *	2009/06/12:	Joson_Zhang
 *		1. Fixed bug strSplit function not check parameters is null.
 *	2009/07/24:	Joson_Zhang
 *		1. Enhance getAbsJarPath function for support MacOS, On mac get property
 *	java.class.path is like: ...xxx.jar:/.../yyy.jar
 *	2010/04/14:	Joson_Zhang
 *		1. Disable SysLog.eror(e) function for avoid StackOverflowError
 *			*** because in error catcher will call audio again.
 *****************************************************************************/
package com.joson.lib.comm;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.security.*;

public class Util {
	public static final String		ENC_UTF8	= "utf-8";

	private	static final long		MEGA		= 1024*1024;
	private static final long		GIGA		= MEGA*1024;
	private static StringBuffer		sBuffer		= new StringBuffer();

	public static long		crc32(byte[] data, int offset, int length) {
		int ofs = 0;
		long crc = 0L;

		if ((null != data) && (offset >= 0) && (length > 0) && ((offset+length) <= data.length)) {
			for (int i=offset; i<length; i++) {
				crc = crc ^ (data[i] << ofs);
				ofs = (ofs >= 24) ? 0 : (ofs+8);
			}
		}

		return crc;
	}

	public static long		crc32(File file) {
		if (null != file) {
			try {
				int n;
				long crc = 0L;
				byte[] t = new byte[1024];
				FileInputStream fis = new FileInputStream(file);
				try {
					while (fis.available() > 0) {
						n = fis.read(t);
						if (n <= 0) {
							break;
						}
						crc ^= crc32(t, 0, n);
					}
				} finally {
					fis.close();
				}
				return crc;
			} catch (Throwable e) {
			}
		}
		return -1L;
	}
	public static String	crc32s(File file) {
		long v = crc32(file);
		return (-1L == v) ? null : Lng2HexStrN(v, 8);
	}
	public static String	md5(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(s.getBytes());
			byte[] bs = md.digest();

			int k = 0;
			char str[] = new char[16 * 2];
			char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',  'e', 'f'};

			if ((null != bs) && (16 == bs.length)) {
				for (int i=0; i<16; i++) {
					byte byte0 = bs[i];
					str[k++] = hexDigits[byte0 >>> 4 & 0xf];
					str[k++] = hexDigits[byte0 & 0xf];
				}
				return new String(str);
			}

			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String	crc32s(byte[] data) {
		return Lng2HexStrN(crc32(data, 0, data.length), 8);
	}

	public static String	encURL(String s, String enc) {
		if (null == s)  {
			return s;
		}

		enc = (null == enc) ? ENC_UTF8 : enc;
		try {
			byte[] bs = s.getBytes(enc);
			synchronized (sBuffer) {
				sBuffer.setLength(0);
				for (int i=0; i<bs.length; i++) {
					if (bs[i] <= 0x7F) {	// 127
						if (((bs[i] >= '0') && (bs[i] <= '9')) || ((bs[i] >= 'a') && (bs[i] <= 'z')) || ((bs[i] >= 'A') && (bs[i] <= 'Z'))) {
							sBuffer.append((char)bs[i]);
						} else {
							switch (bs[i]) {
								case '.':
								case '*':
								case '-':
								case '_':
									sBuffer.append((char)bs[i]);
									break;
								case ' ':
									sBuffer.append('+');
									break;
								default:
									sBuffer.append('%');
									sBuffer.append(Int2HexStrN(bs[i]&0xFF, 2));
									break;
							}
						}
					} else {
						sBuffer.append('%');
						sBuffer.append(Int2HexStrN(bs[i]&0xFF, 2));
					}
				}
				return sBuffer.toString();
			}
		} catch (Exception e) {
		}

		return s;
	}
	public static String	decURL(String s, String enc) {
		if (null == s) {
			return s;
		}

		// maybe not encoded
		if (s.indexOf('%') < 0) {
			return s;
		}

		enc = (null == enc) ? ENC_UTF8 : enc;
		try {
			synchronized (sBuffer) {
				int n = 0;
				int len = s.length();
				char ch;
				byte[] bs = new byte[len/3];
				sBuffer.setLength(0);
				for (int i=0; i<len; i++) {
					ch = s.charAt(i);
					switch (ch) {
						case '%':
							if ((i+2) >= len) {
								return null;
							}
							bs[n++] = (byte)Integer.parseInt(s.substring(i+1, i+3), 16);
							i += 2;
							break;
						default:
							if (n > 0) {
								sBuffer.append(new String(bs, 0, n, enc));
								n = 0;
							}
							if ('+' == ch) {
								ch = ' ';
							}
							sBuffer.append(ch);
							break;
					}
				}
				if (n > 0) {
					sBuffer.append(new String(bs, 0, n, enc));
					n = 0;
				}
				return sBuffer.toString();
			}
		} catch (Exception e) {
		}

		return s;
	}

	public static String	getFileExt(String s) {
		if (null != s) {
			int i = s.lastIndexOf('.');
			if (i > 0) {
				return s.substring(i+1);
			}
		}
		return null;
	}

	public static String	omitString(String s) {
		if ((null == s) || (s.length() <= 1)) {
			return s;
		}
		return s.substring(0, s.length()/2) + "..";
	}

	public static String	getAbsJarDir() {
		String s = getAbsJarPath();
		int n = s.lastIndexOf(File.separatorChar);
		return s.substring(0, n+1);
	}
	public static String	getAbsJarPath() {
/*
		StringBuffer sb = new StringBuffer();
		sb.append(System.getProperty("user.dir"));
		sb.append(System.getProperty("file.separator"));
		sb.append(System.getProperty("java.class.path"));

		String s = sb.toString();
		int i = s.indexOf(System.getProperty("path.separator"));
		if (i > 0) {
			s = s.substring(0, i);
		}
		return new File(s).getAbsolutePath();
*/
		String s = System.getProperty("java.class.path");
		int i = s.indexOf(System.getProperty("path.separator"));
		if (i > 0) {
			s = s.substring(0, i);
		}
		return new File(s).getAbsolutePath();
	}

	public static String[]	getClassNamesFromPackage(String pkgName) {
		ArrayList<String> names = new ArrayList<String>();

		try {
			URL packageURL;
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

			pkgName = pkgName.replace(".", "/");
			packageURL = classLoader.getResource(pkgName);
			if (null == packageURL) {
				return null;
			}

			if(packageURL.getProtocol().equals("jar")) {
				JarFile jf;
				String entryName;
				String jarFileName;
				Enumeration<JarEntry> jarEntries;

				// build jar file name, then loop through zipped entries
				jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
				jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
//System.out.println(">"+jarFileName);
				jf = new JarFile(jarFileName);
				jarEntries = jf.entries();
				while (jarEntries.hasMoreElements()) {
					entryName = jarEntries.nextElement().getName();
					if (entryName.startsWith(pkgName) && (entryName.length() > (pkgName.length()+5))) {
						entryName = entryName.substring(pkgName.length(), entryName.lastIndexOf('.'));
						if (entryName.startsWith("/")) {
							entryName = entryName.substring(1);
						}
						names.add(entryName);
					}
				}
			} else {// loop through files in classpath
				URI uri = new URI(packageURL.toString());
				File[] files = new File(uri.getPath()).listFiles();
				for (int i=0; i<files.length; i++) {
					String entryName = files[i].getName();
					entryName = entryName.substring(0, entryName.lastIndexOf('.'));
					names.add(entryName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return (String[])names.toArray(new String[0]);
	}

	public static String	delChar(String s, int c) {
		if (null == s) {
			return s;
		}

		do {
			int i = s.indexOf(c);
			if (i < 0) {
				break;
			}
			s = s.substring(0, i) + s.substring(i+1);
		} while (true);

		return s;
	}

	public static Object[]	aryDel(Object[] os, int index) {
		if ((null == os) || (index < 0) || (index >= os.length)) {
			return os;
		}
		if (os.length <= 1) {
			return null;
		}
		Object[] temp = new Object[os.length-1];
		System.arraycopy(os, 0, temp, 0, index);
		System.arraycopy(os, index+1, temp, index, os.length-index-1);
		return temp;
	}

	public static String	omitSize(long size) {
		if (size >= GIGA) {
			double a = size;
			double b = GIGA;
			return Double2Str(a/b, 2)+"G";
		} else if (size >= MEGA) {
			double a = size;
			double b = MEGA;
			return Double2Str(a/b, 2)+"M";
		} else if (size >= 1024) {
			double a = size;
			double b = 1024;
			return Double2Str(a/b, 2)+"K";
		}
		return size+"B";
	}

	public static void		fillArray(byte[] a, int off, int len, byte val) {
		Arrays.fill(a, off, off+len, val);
	}
	public static int		binSearch(byte[] a, int a_off, int a_len, byte[] b, int b_off, int b_len) {
		if ((null != a) && (a_off >= 0) && (a_len > 0) && (null != b) && (b_off >= 0) && (b_len > 0)) {
			int a_max = a_off+a_len;
			int b_max = b_off+b_len;

			int k;
			boolean found;
			for (int i=a_off; i<a_max; i++) {
				if ((a_len-(i-a_off)) < b_len) {
					break;
				}
				k = i;
				found = true;
				for (int j=b_off; k<a_max && j<b_max; j++) {
					if (a[k++] != b[j]) {
						found = false;
						break;
					}
				}
				if (found) {
					return i;
				}
			}
		}
		return -1;
	}
	public static String[]	strSplit(String s, char key) {
		return strSplit(s, String.valueOf(key));
	}
	public static String[]	strSplit(String s, String key) {
		if ((null == s) || (null == key) || (key.length() <= 0)) {
			return null;
		}

		String[] aRes = null;

		int iPtr = 0;
		int nRes = 0;
		int nKey = key.length();
		do {
			int i = s.indexOf(key, iPtr);
			if (i < 0) {
				String t = s.substring(iPtr);
				if ((null == t) || (t.length() <= 0)) {
					if (nRes <= 0) {
						return null;
					}
					aRes = new String[nRes];
				} else {
					aRes = new String[++nRes];
					aRes[nRes-1] = t;
				}
				break;
			}
			++nRes;
			iPtr = i + nKey;
		} while (true);

		iPtr = 0;
		int iRes = 0;
		do {
			int i = s.indexOf(key, iPtr);
			if (i < 0) {
				break;
			}
			aRes[iRes++] = s.substring(iPtr, i);
			iPtr = i + nKey;
		} while (true);

		return aRes;
	}
	public static String	strReplace(String s, String k, String v) {
		if ((null == s) || (null == k) || (null == v)) {
			return s;
		}
		if (k.equals(v)) {	// no need replace
			return s;
		}

		synchronized (sBuffer) {
			sBuffer.setLength(0);
			do {
				int i = s.indexOf(k);
				if (i < 0) {
					break;
				}

				sBuffer.setLength(0);
				sBuffer.append(s.substring(0, i));
				sBuffer.append(v);
				sBuffer.append(s.substring(i+k.length()));

				s = sBuffer.toString();
			} while (true);
		}

		return s;
	}
	public static int		BytesToInt(byte[] ary, int off, int len, boolean bigEndian) {
		int value = 0;
		for (int i=0; i<len; i++) {
			int shift = !bigEndian ? i : (len-i-1);
			value |= (ary[off+i] & 0xFF) << (shift * 8);
		}
		return value;
	}
	public static byte[]	IntTo4Bytes(int val, boolean bigEndian) {
		byte[] ary = new byte[4];
		if (bigEndian) {
			ary[0] = (byte)((val & 0xFF000000) >> 24);
			ary[1] = (byte)((val & 0x00FF0000) >> 16);
			ary[2] = (byte)((val & 0x0000FF00) >> 8);
			ary[3] = (byte)((val & 0x000000FF));
		} else {
			ary[0] = (byte)((val & 0x000000FF));
			ary[1] = (byte)((val & 0x0000FF00) >> 8);
			ary[2] = (byte)((val & 0x00FF0000) >> 16);
			ary[3] = (byte)((val & 0xFF000000) >> 24);
		}
		return ary;
	}
	public static byte[]	IntTo3Bytes(int val, boolean bigEndian) {
		byte[] ary = new byte[3];
		if (bigEndian) {
			ary[0] = (byte)((val & 0x00FF0000) >> 16);
			ary[1] = (byte)((val & 0x0000FF00) >> 8);
			ary[2] = (byte)((val & 0x000000FF));
		} else {
			ary[0] = (byte)((val & 0x000000FF));
			ary[1] = (byte)((val & 0x0000FF00) >> 8);
			ary[2] = (byte)((val & 0x00FF0000) >> 16);
		}
		return ary;
	}
	public static byte[]	IntTo2Bytes(int val, boolean bigEndian) {
		byte[] ary = new byte[2];
		if (bigEndian) {
			ary[0] = (byte)((val & 0x0000FF00) >> 8);
			ary[1] = (byte)((val & 0x000000FF));
		} else {
			ary[0] = (byte)((val & 0x000000FF));
			ary[1] = (byte)((val & 0x0000FF00) >> 8);
		}
		return ary;
	}
	static String Val2StrN(String s, int n) {
		if ((null == s) || (s.length() >= n)) {
			return s;
		}

		synchronized (sBuffer) {
			sBuffer.setLength(0);
			for (int i=s.length(); i<n; i++) {
				sBuffer.append("0");
			}
			sBuffer.append(s);
			return sBuffer.toString();
		}
	}
	public static String	Int2StringN(int v, int n) {
		return Val2StrN(Integer.toString(v), n);
	}
	public static String	Int2HexStrN(int v, int n) {
		return Val2StrN(Integer.toString(v, 16).toUpperCase(), n);
	}
	public static String	Lng2StringN(long v, int n) {
		return Val2StrN(Long.toString(v), n);
	}
	public static String	Lng2HexStrN(long v, int n) {
		return Val2StrN(Long.toString(v, 16).toUpperCase(), n);
	}
	public static String	Double2Str(double v, int npostfix) {
		int nx = 1;
		for (int i=0; i<npostfix; i++) {
			nx *= 10;
		}
		int t = (int)(v * nx);
		String s = Integer.toString(t);
		int n = s.length();
		while (n < npostfix) {
			n++;
			s = "0"+s;
		}
		if (n == npostfix) {
			return "0."+s;
		}
		return s.substring(0, n-npostfix)+"."+s.substring(n-npostfix);
	}
	public static String	FormatBigValue(long v) {
		String s = Long.toString(v);
		if (s.length() <= 3) {
			return s;
		}
		synchronized (sBuffer) {
			sBuffer.setLength(0);
			do {
				int n = s.length();
				if (n <= 0) {
					break;
				}
				int i = n % 3;
				if (0 != i) {
					sBuffer.append(s.substring(0, i));
					s = s.substring(i);
				} else {
					sBuffer.append(s.substring(0, 3));
					if (n > 3) {
						s = s.substring(3);
					} else {
						break;
					}
				}
				sBuffer.append(",");
			} while (true);
			return sBuffer.toString();
		}
	}
	public static String	convertHttpURL(String str) {
		int n = str.length();
		char c;
		synchronized (sBuffer) {
			sBuffer.setLength(0);
			for (int i=0; i<n; ++i) {
				switch (c = str.charAt(i)) {
					case '<':
						sBuffer.append("&lt;");
						break;
					case '>':
						sBuffer.append("&gt;");
						break;
					case '&':
						sBuffer.append("&amp;");
						break;
					case '\'':
						sBuffer.append("&apos;");
						break;
					case '"':
						sBuffer.append("&quot;");
						break;
					default:
						sBuffer.append(c);
				}
			}
			return (sBuffer.length() <= 0) ? "" : sBuffer.toString();
		}
	}
	static String	repSpecialChar(String s) {
		return s.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
	}
	public static void	echoByteArray(byte[] data, int ofs, int len) {
		if (null == data) {
			return;
		}
		int max = ofs+len;
		for (int i=ofs; i<max; i++) {
			System.out.print(Int2HexStrN(data[i]&0xFF, 2));
			if (0 == ((i-ofs+1)%16)) {
				System.out.print(" | ");
				System.out.println(repSpecialChar(new String(data, i-15, 16)));
			} else {
				System.out.print(" ");
			}
		}
		int i=len%16;
		if (0 == i) {
			System.out.println();
		} else {
			for (int j=i; j<16; j++) {
				System.out.print("   ");
			}
			System.out.print("| ");
			System.out.println(repSpecialChar(new String(data, ofs+len-i, i)));
		}
	}
	public static void	echoByteArray(byte[] data) {
		echoByteArray(data, 0, data.length);
	}

	public static ByteArrayOutputStream	readIS2BAOS(InputStream is, boolean close) throws Throwable {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			int ch;
			do {
				ch = is.read();
				if (ch < 0) {
					break;
				}
				baos.write(ch);
			} while (true);
		} finally {
			if (close) {
				is.close();
			}
		}
		return baos;
	}

	public static void		lstThread() {
		Thread[] tary = new Thread[Thread.activeCount()];
		Thread.enumerate(tary);
		for (int i=0; i<tary.length; i++) {
			if (null == tary[i]) {
				continue;
			}
			System.out.print(i+1);
			System.out.print("\t");
			System.out.print(tary[i].isAlive());
			System.out.print("\t");
			System.out.println(tary[i].getName());
		}
		System.out.println("Thread Count: "+tary.length);
	}

	public static String	getLocale() {
		String s;
		Locale l = Locale.getDefault();
		synchronized (sBuffer) {
			sBuffer.setLength(0);
			s = l.getLanguage();
			if ((null != s) && (s.length() > 0)) {
				int idx = s.indexOf('_');
				if (idx < 0) {
					sBuffer.append(s.toLowerCase());
				} else {
					sBuffer.append(s.substring(0, idx).toLowerCase());
					sBuffer.append("-");
					sBuffer.append(s.substring(idx+1).toUpperCase());
				}
			}
			if (sBuffer.indexOf("-", 0) < 0) {
				sBuffer.append("-");
				s = l.getCountry();
				if ((null != s) && (s.length() > 0)) {
					sBuffer.append(s.toUpperCase());
				}
			}
			return sBuffer.toString();
		}
	}
	public static String	getEncoding() {
		return System.getProperty("file.encoding");
	}
	public static String	getPlatform() {
		return System.getProperty("os.name")+"/"+System.getProperty("os.version");
	}

	public static InputStream	getResourceAsStream(String path) {
		return System.out.getClass().getResourceAsStream(path);
	}

	private static Object		lSysProp	= new Object();
	private static Hashtable	mSysProp	= null;

	public static boolean	addSystemProperty(String k, String v) {
		if ((null == k) || (k.length() <= 0) || (null == v)) {
			return false;
		}

		synchronized (lSysProp) {
			if (null == mSysProp) {
				mSysProp = new Hashtable();
			}
			mSysProp.put(k, v);
		}

		return true;
	}
	public static String	getSystemProperty(String k) {
		if ((null == k) || (k.length() <= 0)) {
			return null;
		}

		synchronized (lSysProp) {
			if (null == mSysProp) {
				return null;
			}
			return (String)mSysProp.get(k);
		}
	}
	public static int		getSystemProperty(String k, int defv) {
		String v = getSystemProperty(k);
		try {
			return Integer.parseInt(v);
		} catch (Exception e) {
		}
		return defv;
	}
	public static boolean	delSystemProperty(String k) {
		if ((null == k) || (k.length() <= 0)) {
			return false;
		}

		synchronized (lSysProp) {
			if (null == mSysProp) {
				return false;
			}
			if (null == mSysProp.remove(k)) {
				return false;
			}
			return true;
		}
	}
}
