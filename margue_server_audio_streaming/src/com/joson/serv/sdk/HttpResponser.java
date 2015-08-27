/******************************************************************************
 * File:	HttpResponser.java
 * Date:	2014/09/28
 * Author:	Joson_Zhang
 * Description:
 *	http responser interface
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/09/28:	Joson_Zhang
 *		1. initial create
 *	2014/10/18:	Joson_Zhang
 *		1. Add setKAliveHandler function for tell server this session need
 *			keep the session live (maybe not follow HTTP protocol, :-), it is
 *			for our audio conference application.
 *		2. Add outAsRaw function for tell server response data not follow HTTP
 *			protocol (no HTTP header response), it must call before send().
 *****************************************************************************/
package com.joson.serv.sdk;

public interface HttpResponser {
	public void		outAsRaw(boolean b);

	public boolean	head(int code, String msg);

	public boolean	setKeyValue(String key, String value);

	public boolean	send(String s);
	public boolean	send(Object o);
	public boolean	send(byte[] bs, int off, int len);

	public void		setKAliveHandler(KAliveHandler handler);
}
