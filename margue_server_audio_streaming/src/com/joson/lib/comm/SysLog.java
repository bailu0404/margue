/******************************************************************************
 * File:	SysLog.java
 * Date:	2013/05/20
 * Author:	Friendlysoft
 * Description:
 *	SysLog
 *
 *			Copyright 2013 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2013/05/20:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.lib.comm;

import java.io.*;
import java.util.*;
import java.util.logging.*;

public class SysLog {
	public interface Listener {
		boolean	log(int level, String message);
	}

	public	static final	int		SYSLOG_DEBG	= 0x01;
	public	static final	int		SYSLOG_INFO	= 0x02;
	public	static final	int		SYSLOG_WARN	= 0x04;
	public	static final	int		SYSLOG_EROR	= 0x08;
	public	static final	int		SYSLOG_SYST	= 0x10;

	private	static		Object		lock		= new Object();
	private	static		int			level		= 0x0F;
	private	static		Listener	listener	= null;

	public SysLog(String path) {
	}

	public static int	getLevel() {
		return level;
	}
	public static void	setLevel(int lev) {
		level = lev;
	}

	public static void		setListener(Listener l) {
		synchronized (lock) {
			listener = l;
		}
	}
	public static Listener	getListener() {
		synchronized (lock) {
			return listener;
		}
	}

	static void log(int lev, String msg, int tlev) {
		if (0 == (level & lev)) {
			return;
		}

		Listener l = getListener();
		if (null != l) {
			String s = msg;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Throwable th = new Throwable();
			th.printStackTrace(new PrintStream(baos));
			s = s + "\n" + baos.toString();
			if (l.log(lev, s)) {
				return;
			}
		}

		String str = null;
		switch (lev) {
			case SYSLOG_DEBG:
				str = "DEBG";
				break;
			case SYSLOG_INFO:
				str = "INFO";
				break;
			case SYSLOG_WARN:
				str = "WARN";
				break;
			case SYSLOG_EROR:
				str = "EROR";
				break;
			case SYSLOG_SYST:
				str = "SYST";
				break;
			default:
				str = "UNKW";
				break;
		}

		String inf = "";
		Throwable ta = new Throwable();
		StackTraceElement[] stack = ta.getStackTrace();

		int skip_traces = tlev;
		if (stack.length <= skip_traces) {
			skip_traces = stack.length-1;
		}
		if (skip_traces >= 0) {
			inf = "\r\n > "+
				stack[skip_traces].getClassName()+
				"("+
					stack[skip_traces].getMethodName()+
					":"+
					stack[skip_traces].getLineNumber()+
				")";
//			inf = String.format(" - %s(%s:%d)",
//				stack[skip_traces].getClassName(),
//				stack[skip_traces].getMethodName(),
//				stack[skip_traces].getLineNumber());
		}
//		MyDate d = new MyDate();
		System.out.println("["+str+"] - "+new Date()+inf+"\r\n > "+msg);
	}
	public static void	debg(String msg) {
		log(SYSLOG_DEBG, msg, 2);
	}
	public static void	info(String msg) {
		log(SYSLOG_INFO, msg, 2);
	}
	public static void	warn(String msg) {
		log(SYSLOG_WARN, msg, 2);
	}
	public static void	eror(String msg) {
		log(SYSLOG_EROR, msg, 2);
	}
	public static void	eror(Throwable e) {
		if (e instanceof OutOfMemoryError) {
			System.gc();
		}
		String s = System.getProperty("syslog.eror.printStackTrace");
		boolean b = false;
		if ((null != s) && (s.length() > 0)) {
			try {
				b = (0 != Integer.parseInt(s));
			} catch (Throwable ex) {
			}
		}
		if ((null != e) && b) {
			e.printStackTrace();
		}
		String msg = e.toString();
		int i = msg.indexOf(':');
		if (i > 0) {
			msg = msg.substring(i+1).trim();
		}
		log(SYSLOG_EROR, msg, 3);
	}
	public static void	syst(String msg) {
		log(SYSLOG_SYST, msg, 2);
	}
}
