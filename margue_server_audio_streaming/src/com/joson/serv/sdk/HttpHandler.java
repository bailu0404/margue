/******************************************************************************
 * File:	HttpHandler.java
 * Date:	2014/09/28
 * Author:	Joson_Zhang
 * Description:
 *	Client http handler interface
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/09/28:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.serv.sdk;

import com.joson.lib.proto.http.*;

public interface HttpHandler {
	/******************************************************
	 * Description:
	 *	process http request callback function
	 *	all functions are public and static
	 *	default is index function
	 *
	 * Input:
	 *	http	request information
	 *	resp	responser interface for this session
	 *
	 * Output:
	 *	true	OK, success process it
	 *	false	Sorry, I can't process it.
	******************************************************/
}
