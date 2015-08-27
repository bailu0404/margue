/******************************************************************************
 * File:	ParseHTTP.java
 * Date:	2011/08/19
 * Author:	Joson_Zhang
 * Description:
 *	HTTP protocol parser implementation
 *
 *			Copyright 2011 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2011/08/19:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.lib.proto.http;

import java.io.*;
import java.util.*;

import com.joson.lib.comm.*;

public class ParseHTTP {
	private	float		verHttp		= 0.0F;		// 1.0, 1.1, 2.0, ...
	private	String		proType		= null;		// HTTP, RTSP, ...

	private	String		reqCMD		= null;
	private	String		reqURI		= null;

	private	int			rspCode		= -1;		// 200, 302, 404, ...
	private	String		rspDesc		= null;		// OK, Not Found, ...

	private	int			lenCont		= -1;		// Content-Length

	private Hashtable	mapKeys		= new Hashtable();
	private	Hashtable	mapParam	= new Hashtable();

	private	Object		atchObj		= null;

	public ParseHTTP() {
	}

	private boolean parseFirstLine(String fstr) {
		if ((null == fstr) || (fstr.length() <= 0)) {
			return false;
		}

		int i, j;
		String s;

		i = fstr.indexOf(' ');
		if (i <= 0) {							// Invalid HTTP reponse header
			return false;
		}

		s = fstr.substring(0, i);
		j = s.indexOf('/');
		if (j <= 0) {	// GET xxx HTTP/1.x
			reqCMD = fstr.substring(0, i);
			s = fstr.substring(i+1);
			i = s.indexOf(' ');
			if (i <= 0) {
				return false;
			}
			reqURI = s.substring(0, i);
			s = s.substring(i+1);
			i = s.indexOf('/');
			if (i <= 0) {
				return false;
			}
			proType = s.substring(0, i);
			try {
				verHttp = Float.parseFloat(s.substring(i+1));
			} catch (Exception e) {
				return false;
			}
		} else {		// HTTP/1.x xxx value
			proType = s.substring(0, j);
			try {
				verHttp = Float.parseFloat(s.substring(j+1));
			} catch (Exception e) {
				return false;
			}

			s = fstr.substring(i+1);
			i = s.indexOf(' ');
			if (i <= 0) {
				return false;
			}
			try {
				rspCode = Integer.parseInt(s.substring(0, i));
			} catch (Exception e) {
				return false;						// Invalid Response Code
			}
			rspDesc = s.substring(i+1);
		}

		return true;
	}

	void	reset() {
		proType = null;
		verHttp = 0.0F;
		reqCMD	= null;
		reqURI	= null;
		rspCode = 0;
		rspDesc = null;

		lenCont = -1;
		mapParam.clear();
	}

	public boolean parse(String http) {
//System.out.println("### HTTP\r\n"+http);
		if ((null == http) || (http.length() <= 0)) {	// ????
			return false;
		}
		String[] aryLine = http.split("\r\n");
		if ((null == aryLine) || (aryLine.length <= 0)) {
			return false;
		}

		reset();

		parseFirstLine(aryLine[0]);

		for (int i=1; i<aryLine.length; i++) {
			int j = aryLine[i].indexOf(':');
			if (j <= 0) {
				continue;
			}
			String sk = aryLine[i].substring(0, j).trim();
			String sv = aryLine[i].substring(j+1).trim();
			mapKeys.put(sk.toUpperCase(), sk);
			mapParam.put(sk, sv);
		}

		return true;
	}
	/**
	 * Description:
	 *	parse http header
	 *	if parse ok, it will remove http header from [baos], just remain pure http data
	 *
	 * Input:
	 *	baos	http stream data
	 *
	 * Output:
	 *	boolean
	 *		true	ok (baos remain http data)
	 *		false	fail (baos no any change)
	 */
	public boolean	parse(ByteArrayOutputStream baos) {
		if (null == baos) {
			return false;
		}

		String hdr = baos.toString();

		int n = hdr.length();
		String sep = null;
		for (int i=0; i<n; i++) {
			char c = hdr.charAt(i);
			if ('\r' == c) {
				if ((i+1) >= n) {
					break;
				}
				if ('\n' != hdr.charAt(i+1)) {
					sep = "\r";
				} else {
					sep = "\r\n";
				}
				break;
			} else if ('\n' == c) {
				sep = "\n";
				break;
			}
		}
		if (null == sep) {
			return false;
		}
		n = hdr.indexOf(sep+sep);
		if (n <= 0) {
			return false;
		}

		hdr = hdr.substring(0, n+sep.length());
		String[] aryLine = Util.strSplit(hdr, sep);
		if ((null == aryLine) || (aryLine.length <= 0)) {
			return false;
		}

		byte[] bs = baos.toByteArray();
		baos.reset();
		n += sep.length()*2;
		baos.write(bs, n, bs.length-n);

		reset();
		parseFirstLine(aryLine[0]);

		for (int i=1; i<aryLine.length; i++) {
			int j = aryLine[i].indexOf(':');
			if (j <= 0) {
				continue;
			}
			String sk = aryLine[i].substring(0, j).trim();
			String sv = aryLine[i].substring(j+1).trim();
			mapKeys.put(sk.toUpperCase(), sk);
			mapParam.put(sk, sv);
		}

		return true;
	}

	public int		getLength() {
		if (lenCont < 0) {
			String s = getParam("Content-Length");
			if ((null == s) || (s.length() <= 0)) {
				lenCont = -1;
			} else {
				try {
					lenCont = Integer.parseInt(s.trim());
				} catch (Exception e) {
					lenCont = -1;
					e.printStackTrace();
				}
			}
		}
		return lenCont;
	}
	public String	getProtocol() {
		return proType;
	}
	public float	getVersion() {
		return verHttp;
	}
	public String	getRequCommand() {
		return reqCMD;
	}
	public String	getRequURI() {
		return reqURI;
	}
	public int		getRespCode() {
		return rspCode;
	}
	public String	getRespDesc() {
		return rspDesc;
	}

	public boolean	isMixed() {
		String s = getParam("Content-Type");
		if ((null == s) || (s.length() <= 0)) {
			return false;
		}
		s = s.toLowerCase();
		if (s.indexOf("multipart/x-mixed-replace") < 0) {
			return false;
		}
		return true;
	}
	public boolean isChunked() {
		String s = getParam("Transfer-Encoding");
		if ((null == s) || (s.length() <= 0)) {
			return false;
		}
		s = s.toLowerCase();
		if (s.indexOf("chunked") < 0) {
			return false;
		}
		return true;
	}

	public String getMixedBoundary() {
		String s = getParam("Content-Type");
		if ((null == s) || (s.length() <= 0)) {
			return null;
		}
		String s2 = s.toLowerCase();
		int i = s2.indexOf("boundary=");
		if (i < 0) {
			return null;
		}
		s = s.substring(i+9);
		i = s.indexOf(';');
		if (i > 0) {
			s = s.substring(0, i);
		}
		return s;
	}

	public String[]	getParamNames() {
		return (String[])mapParam.keySet().toArray(new String[0]);
	}
	public String	getParam(String key) {
		if ((null == key) || (key.length() <= 0)) {
			return null;
		}
		key = (String)mapKeys.get(key.toUpperCase());
		if ((null == key) || (key.length() <= 0)) {
			return null;
		}
		return (String)mapParam.get(key);
	}
	public int		getParam(String key, int defval) {
		String s = getParam(key);
		if (null != s) {
			try {
				defval = Integer.parseInt(s);
			} catch (Throwable e) {
			}
		}
		return defval;
	}
	public long		getParam(String key, long defval) {
		String s = getParam(key);
		if (null != s) {
			try {
				defval = Long.parseLong(s);
			} catch (Throwable e) {
			}
		}
		return defval;
	}

	public float	getParam(String key, float defval) {
		String s = getParam(key);
		if (null != s) {
			try {
				defval = Float.parseFloat(s);
			} catch (Throwable e) {
			}
		}
		return defval;
	}

	private Hashtable	mapURLParam	= null;

	public String	getURLParam(String key) {
		if (null == key) {
			return null;
		}
		if (null == mapURLParam) {
			if (null == reqURI) {
				return null;
			}
			int i = reqURI.indexOf('?');
			if (i < 0) {
				return null;
			}
			String s = reqURI.substring(i+1);
			String[] ary = Util.strSplit(s, '&');
			if (null == ary) {
				return null;
			}
			mapURLParam = new Hashtable();
			for (i=0; i<ary.length; i++) {
				if (null == ary[i]) {
					continue;
				}
				int j = ary[i].indexOf('=');
				if (j <= 0) {
					continue;
				}
				mapURLParam.put(ary[i].substring(0, j).toUpperCase(), ary[i].substring(j+1));
			}
		}
		return (String)mapURLParam.get(key.toUpperCase());
	}
	public int		getURLParam(String key, int defv) {
		String s = getURLParam(key);
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
		}
		return defv;
	}
	public long		getURLParam(String key, long defv) {
		String s = getURLParam(key);
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
		}
		return defv;
	}
	public float		getURLParam(String key, float defv) {
		String s = getURLParam(key);
		try {
			return Float.parseFloat(s);
		} catch (Exception e) {
		}
		return defv;
	}

	public void		setAttachment(Object obj) {
		atchObj = obj;
	}
	public Object	getAttachment() {
		return atchObj;
	}
}
