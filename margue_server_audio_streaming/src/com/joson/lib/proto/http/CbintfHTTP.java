/******************************************************************************
 * File:	CbintfHTTP.java
 * Date:	2011/08/19
 * Author:	Joson_Zhang
 * Description:
 *	HTTP protocol Call back interface
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

public interface CbintfHTTP {
	public void rcvData(ByteArrayOutputStream baos);
}
