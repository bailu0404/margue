/******************************************************************************
 * File:	IniReader.java
 * Date:	2013/06/10
 * Author:	Joson_Zhang
 * Description:
 *	INI file reader class
 *
 *			Copyright 2013 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2013/06/10:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.lib.comm;

import java.io.*;
import java.util.*;

public class IniReader {
	private	String			sPath	= null;
	private	InputStream		srcis	= null;
	private	ExtendBAOS		xBAOS	= new ExtendBAOS();

	public IniReader() {
	}

	public String		getPath() {
		return sPath;
	}
	public void			setPath(String path) {
		sPath = path;
	}

	public InputStream	getInputStream() {
		return srcis;
	}
	public void			setInputStream(InputStream is) {
		srcis = is;
	}

	private	InputStream	getIS() {
		if (null != srcis) {
			try {
				srcis.reset();
				return srcis;
			} catch (Throwable e) {
//SysLog.eror(e);
			}
		}

		InputStream is = null;

		if (null == is) {
			try {
				is = new FileInputStream(sPath);
			} catch (Throwable e) {
//SysLog.eror(e);
			}
		}

		return is;
	}
	synchronized boolean	getSec(InputStream is, String sect) {
		int c, last=-1;
		boolean bSect = false;

		while (true) {
			try {
				c = is.read();
			} catch (Throwable e) {
				c = -1;
			}
			if (c < 0) {
				break;
			}

			if (bSect) {
				switch (c) {
					case ']':
						bSect = false;
						String s = xBAOS.toString2("utf-8");
						if (s.equals(sect)) {
							return true;
						}
						break;
					case '\r':
					case '\n':
						break;
					default:
						try {
							xBAOS.write(c);
						} catch (Throwable e) {
							return false;
						}
						break;
				}
			} else if ('[' == c) {
				if ((-1 == last) || (('\r' == last) || ('\n' == last))) {
					bSect = true;
					xBAOS.reset();
				}
			}
			last = c;
		}

		return false;
	}
	synchronized String	getKey(InputStream is, String key) {
		int c, last=-1;
		boolean bFind = false;
		boolean bSkip = false;

		while (true) {
			try {
				c = is.read();
			} catch (Throwable e) {
				c = -1;
			}
			if (c < 0) {
				break;
			}

			switch (c) {
				case ';':
				case '#':
					if (('\r' == last) || ('\n' == last)) {
						bFind = false;
						bSkip = true;
					}
					break;
				case '[':
					if (('\r' == last) || ('\n' == last)) {
						return null;
					}
					break;
				case '=':
					if (bFind) {
						try {
							xBAOS.write(c);
						} catch (Throwable e) {
							return null;
						}
						break;
					}
					String s = xBAOS.toString2("utf-8");
					if (s.trim().equals(key)) {
						bFind = true;
						bSkip = false;
						xBAOS.reset();
					} else {
						bSkip = true;
					}
					break;
				case '\r':
				case '\n':
					if (bFind) {
						return xBAOS.toString2("utf-8");
					} else {
						bSkip = false;
						xBAOS.reset();
					}
					break;
				default:
					if (!bSkip) {
						try {
							xBAOS.write(c);
						} catch (Throwable e) {
							return null;
						}
					}
					break;
			}
			last = c;
		}

		return null;
	}

	public String	getStr(String sec, String key, String defval) {
		InputStream is = getIS();
		if (null == is) {
			return defval;
		}

		try {
			if (!getSec(is, sec)) {
				return defval;
			}
			String s = getKey(is, key);
			s = (null == s) ? defval : s.trim();
			s = Util.strReplace(s, "\\n", "\n");
			return s;
		} catch (Throwable e) {
		} finally {
			try {
				is.close();
			} catch (Throwable e) {
			}
		}

		return null;
	}
	public String	getStrUTF8(String sec, String key, String defval) {
		return getStr(sec, key, defval);
	}
	public int		getInt(String sec, String key, int defval) {
		String s = getStr(sec, key, null);
		if (null != s) {
			try {
				return Integer.parseInt(s);
			} catch (Throwable e) {
			}
		}
		return defval;
	}
	public boolean	getBool(String sec, String key, boolean defval) {
		int i = getInt(sec, key, 2);
		return (0 == i) ? false : ((1 == i) ? true : defval);
	}

	int lastch = -1;
	synchronized String	readLine(InputStream is) {
		xBAOS.reset();

		int c;
		boolean done = false;
		do {
			try {
				c = is.read();
			} catch (Throwable e) {
				c = -1;
			}
			if (-1 == c) {
				done = true;
				break;
			}
			if (('\r' == c) || ('\n' == c)) {
				if ((-1 != lastch) && (c != lastch)) {
					continue;
				}
				break;
			}
			try {
				xBAOS.write(c);
			} catch (Throwable e) {
				return null;
			}
		} while (true);
		lastch = c;

		return done ? null : xBAOS.toString2("utf-8");
	}
	public boolean	lstSecKey(Hashtable data) {
		lastch = -1;
		InputStream is = getIS();
		if (null == is) {
			return false;
		}

		int i, n;
		String sect = "";
		StringBuffer sb = new StringBuffer();
		do {
			String s = readLine(is);
			if (null == s) {
				break;
			}
			s = s.trim();
			n = s.length();
			if (n <= 2) {	// []/x=
				continue;
			}
			if ((';' == s.charAt(0)) || ('#' == s.charAt(0))) {		// comment
				continue;
			}
			if (('[' == s.charAt(0)) && (']' == s.charAt(n-1))) {	// [SECT]
				sect = s.substring(1, n-1);
				continue;
			}
			i = s.indexOf('=');
			if (i <= 0) {	// Invalid key value
				continue;
			}
			sb.setLength(0);
			sb.append(sect);
			sb.append(".");
			sb.append(s.substring(0, i).trim());
			s = s.substring(i+1).trim();
			s = Util.strReplace(s, "\\n", "\n");
			data.put(sb.toString(), s);
		} while (true);

		return true;
	}

	static int			cLAST	= -1;
	static String	readLine(InputStream is, ExtendBAOS baos) {
		baos.reset();

		int c;
		boolean done = false;
		do {
			try {
				c = is.read();
			} catch (Throwable e) {
				c = -1;
			}
			if (-1 == c) {
				done = true;
				break;
			}
			if (('\r' == c) || ('\n' == c)) {
				if ((-1 != cLAST) && (c != cLAST)) {
					continue;
				}
				break;
			}
			try {
				baos.write(c);
			} catch (Throwable e) {
				return null;
			}
		} while (true);

		cLAST = c;

		return done ? null : baos.toString2("utf-8");
	}
	public static boolean	lstSecKey(InputStream is, Hashtable data) {
		if ((null == is) || (null == data)) {
			return false;
		}

		ExtendBAOS xbaos = new ExtendBAOS();
		try {
			cLAST = -1;

			int i, n;
			String sect = "";
			StringBuffer sb = new StringBuffer();
			do {
				String s = readLine(is, xbaos);
				if (null == s) {
					break;
				}
				s = s.trim();
				n = s.length();
				if (n <= 2) {	// []/x=
					continue;
				}
				if ((';' == s.charAt(0)) || ('#' == s.charAt(0))) {		// comment
					continue;
				}
				if (('[' == s.charAt(0)) && (']' == s.charAt(n-1))) {	// [SECT]
					sect = s.substring(1, n-1);
					continue;
				}
				i = s.indexOf('=');
				if (i <= 0) {	// Invalid key value
					continue;
				}
				sb.setLength(0);
				sb.append(sect);
				sb.append(".");
				sb.append(s.substring(0, i).trim());
				s = s.substring(i+1).trim();
				s = Util.strReplace(s, "\\n", "\n");
				data.put(sb.toString(), s);
			} while (true);
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return true;
	}
}
