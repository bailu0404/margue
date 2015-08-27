/******************************************************************************
 * File:	MyMap.java
 * Date:	2011/08/19
 * Author:	Joson_Zhang
 * Description:
 *	Simple HashMap implementation, only for MAP key/value
 *		(For support JDK 1.1.x)
 *
 *			Copyright 2011 Joson_Zhang
 *****************************************************************************/
package com.joson.lib.comm;

import java.util.*;

public class MyMap {
	private class Xval {
		Object	key;
		Object	val;
		public Xval(Object k, Object v) {
			key = k;
			val = v;
		}
	}

	private	Vector	vec	= new Vector();

	public MyMap() {
	}

	public int size() {
		return vec.size();
	}
	public void clear() {
		vec.clear();
	}

	public void del(int i) {
		if ((i < 0) || (i >= size())) {
			return;
		}
		vec.remove(i);
	}

	public void movHead(int i) {
		if ((i < 0) || (i >= size())) {
			return;
		}
		vec.add(0, vec.remove(i));
	}

	public void put(Object key, Object val) {
		put(size(), key, val);
	}
	public void put(int i, Object key, Object val) {
		if ((null == key) || (null == val)) {
			return;
		}
		vec.add(i, new Xval(key, val));
	}

	public Object get(Object key) {
		if (null != key) {
			for (int i=0; i<vec.size(); i++) {
				Xval v = (Xval)vec.get(i);
				if (v.key.equals(key)) {
					return v.val;
				}
			}
		}
		return null;
	}

	private Xval get(int i) {
		if ((i < 0) || (i >= size())) {
			return null;
		}
		return (Xval)vec.get(i);
	}
	public Object getKey(int i) {
		Xval v = get(i);
		if (null == v) {
			return null;
		}
		return v.key;
	}
	public Object getVal(int i) {
		Xval v = get(i);
		if (null == v) {
			return null;
		}
		return v.val;
	}
}
