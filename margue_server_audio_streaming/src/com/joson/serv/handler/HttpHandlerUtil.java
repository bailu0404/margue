/******************************************************************************
 * File:	HttpHandlerUtil.java
 * Date:	2014/10/10
 * Author:	Joson_Zhang
 * Description:
 *	Util class for Http Handler
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/10/10:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.serv.handler;

import java.sql.*;
import java.util.*;
import javax.sql.*;

import org.json.*;

import com.joson.lib.comm.*;
import com.joson.lib.proto.http.*;
import com.joson.serv.db.*;
import com.joson.serv.sdk.*;

public class HttpHandlerUtil {
	private static Hashtable	hMesg	= new Hashtable();

	static {
		hMesg.put(0,	"OK");
		hMesg.put(1,	"Server Busy");
		hMesg.put(2,	"Database Error");
		hMesg.put(101,	"Invalid Parameter");
		hMesg.put(102,	"Invalid Request Method");
		hMesg.put(103,	"Invalid Password");
		hMesg.put(104,	"Invalid Verify Code");
		hMesg.put(105,	"Invalid New Password");
		hMesg.put(201,	"Exists");
	}

	public static JSONObject	getResult(int rc) {
		return getResult(rc, null);
	}
	public static JSONObject	getResult(int rc, String msg) {
		return getResult(rc, msg, null);
	}
	public static JSONObject	getResult(int rc, String msg, JSONObject data) {
		JSONObject json = new JSONObject();
		json.put("rc", rc);
		if ((null != msg) && (msg.length() > 0)) {
			json.put("msg", msg);
		} else {
			json.put("msg", (String)hMesg.get(rc));
		}
		if (null != data) {
			json.put("data", data);
		}
		return json;
	}
}
