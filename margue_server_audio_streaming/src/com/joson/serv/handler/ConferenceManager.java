/******************************************************************************
 * File:	ConferenceManager.java
 * Date:	2014/10/19
 * Author:	Joson_Zhang
 * Description:
 *	Conference manager class
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/10/19:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.serv.handler;

import java.util.*;

import com.joson.serv.sdk.*;

public final class ConferenceManager {
	private Hashtable<Long, AudioConferenceHandler>	hRooms	= new Hashtable<Long, AudioConferenceHandler>();

	public ConferenceManager() {
	}

	public boolean	addMember(long rid, String rac, HttpResponser resp) {
		synchronized (hRooms) {
			AudioConferenceHandler ach = hRooms.get(rid);
			if (null == ach) {
				ach = new AudioConferenceHandler(rid);
				hRooms.put(rid, ach);
			}
			ach.addMember(rac, resp);
		}
		return true;
	}

	public boolean	broadcast(long rid, byte[] data) {
		AudioConferenceHandler ach = null;
		synchronized (hRooms) {
			ach = hRooms.get(rid);
		}
		if (null == ach) {
			return false;
		}
		return ach.broadcast(data);
	}

	public boolean	record(long rid, boolean on) {
		AudioConferenceHandler ach = null;
		synchronized (hRooms) {
			ach = hRooms.get(rid);
		}
		if (null == ach) {
			return false;
		}
		return ach.record(on);
	}

	public boolean	stop(long rid) {
		AudioConferenceHandler ach = null;
		synchronized (hRooms) {
			ach = hRooms.get(rid);
		}
		if (null == ach) {
			return false;
		}
		return ach.stop();
	}

	public boolean	chkRID_RAC(long rid, String rac) {
		return true;
	}
}
