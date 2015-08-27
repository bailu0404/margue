/******************************************************************************
 * File:	SysHttpHandler.java
 * Date:	2014/10/14
 * Author:	Joson_Zhang
 * Description:
 *	System http handler class implementation
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/10/14:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.serv.handler;

import com.joson.lib.comm.*;
import com.joson.lib.proto.http.*;
import com.joson.serv.sdk.*;

public final class SysHttpHandler implements HttpHandler {
	private static byte[]					ok	= new byte[] {'#', '\0', '\6', '\0', '\2', '\1'};
	private static byte[]					fail= new byte[] {'#', '\0', '\6', '\0', '\2', '\0'};
	private static ConferenceManager		cmgr= new ConferenceManager();

	public static boolean	AConf(ParseHTTP http, HttpResponser resp) {
		resp.outAsRaw(true);

		long rid = http.getURLParam("roomID", 0L);
		String rac = http.getURLParam("randCode");
		if ((rid <= 0L) || (null == rac) || (rac.length() <= 0)) {
			resp.send(fail, 0, fail.length);
			return true;
		}

		if (!cmgr.chkRID_RAC(rid, rac)) {
			resp.send(fail, 0, fail.length);
			return true;
		}

		if (cmgr.addMember(rid, rac, resp)) {
			resp.send(ok, 0, ok.length);
		} else {
			resp.send(fail, 0, fail.length);
		}

		return true;
	}

	public static boolean	bcst(ParseHTTP http, HttpResponser resp) {
		long rid = http.getURLParam("room_id", 0L);
		if (rid <= 0L) {
			return resp.send(HttpHandlerUtil.getResult(101));
		}
		if (!"POST".equalsIgnoreCase(http.getRequCommand())) {
			return resp.send(HttpHandlerUtil.getResult(102));
		}
		ExtendBAOS baos = (ExtendBAOS)http.getAttachment();
		if (null == baos) {
			return resp.send(HttpHandlerUtil.getResult(101, "Invalid post data!"));
		}
		if (!cmgr.broadcast(rid, baos.toByteArray(true))) {
			return resp.send(HttpHandlerUtil.getResult(1));
		}
		return resp.send(HttpHandlerUtil.getResult(0));
	}

	public static boolean	krec(ParseHTTP http, HttpResponser resp) {
		long rid = http.getURLParam("room_id", 0L);
		int rec = http.getURLParam("rec", -1);
		if ((rid <= 0L) || (rec < 0)) {
			return resp.send(HttpHandlerUtil.getResult(101));
		}
		if (!cmgr.record(rid, 0!=rec)) {
			return resp.send(HttpHandlerUtil.getResult(1));
		}
		return resp.send(HttpHandlerUtil.getResult(0));
	}

	public static boolean	stop(ParseHTTP http, HttpResponser resp) {
		long rid = http.getURLParam("room_id", 0L);
		if (rid <= 0L) {
			return resp.send(HttpHandlerUtil.getResult(101));
		}
		if (!cmgr.stop(rid)) {
			return resp.send(HttpHandlerUtil.getResult(1));
		}
		return resp.send(HttpHandlerUtil.getResult(0));
	}
}
