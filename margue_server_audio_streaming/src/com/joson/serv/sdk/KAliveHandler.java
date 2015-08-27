/******************************************************************************
 * File:	KAliveHandler.java
 * Date:	2014/10/18
 * Author:	Joson_Zhang
 * Description:
 *	Keep-Alive handler interface
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/10/18:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.serv.sdk;

import java.nio.*;

import com.joson.lib.comm.*;

public interface KAliveHandler {
	/******************************************************
	 * Description:
	 *	notify session close
	 *
	 * Input:
	 *	resp	responser interface for this session
	 *
	 * Output:
	 *	NONE
	******************************************************/
	public void		close(KAliveResponser resp);

	/******************************************************
	 * Description:
	 *	process keep-alive session request function
	 *
	 * Input:
	 *	baos	client request data
	 *	resp	responser interface for this session
	 *
	 * Output:
	 *	true	need keep the session
	 *	false	close the session
	******************************************************/
	public boolean	process(ExtendBAOS baos, KAliveResponser resp);
}
