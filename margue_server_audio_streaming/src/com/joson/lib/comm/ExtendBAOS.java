/******************************************************************************
 * File:	ExtendBAOS.java
 * Date:	2011/08/19
 * Author:	Joson_Zhang
 * Description:
 *	Extended ByteArrayOutputStream for save memory used.
 *
 *			Copyright 2011 Joson_Zhang.
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2011/08/19:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.lib.comm;

import java.io.*;

public class ExtendBAOS extends ByteArrayOutputStream {
	public ExtendBAOS() {
	}

	public ExtendBAOS(int size) {
		super(size);
	}

	public void		write(String s) {
		byte[] data = s.getBytes();
		write(data, 0, data.length);
	}
	public void		write(String s, String enc) {
		try {
			write(s.getBytes(enc));
		} catch (Throwable localThrowable) {
			write(s);
		}
	}

	public int		indexOf(int c) {
		return indexOf(0, c);
	}
	public int		indexOf(int off, int c) {
		int n = size();
		if ((off >= 0) && (off < n)) {
			byte x = (byte)(c & 0xFF);
			byte[] ptr = this.buf;
			for (int i=off; i<n; i++) {
				if (ptr[i] == x) {
					return i;
				}
			}
		}
		return -1;
	}

	public synchronized boolean	setByte(int index, int value) {
		if ((index < 0) || (index >= this.count)) {
			return false;
		}
		this.buf[index] = (byte)(value & 0xFF);
		return true;
	}

	public synchronized int	getByte(int index) {
		if ((index < 0) || (index >= this.count)) {
			return -1;
		}
		return (this.buf[index] & 0xFF);
	}

	public synchronized int	getLastByte() {
		return getByte(size() - 1);
	}

	public synchronized void delBytes(int idx, int num) {
		int i = idx + num;
		if ((idx < 0) || (idx >= this.count) || (num <= 0) || (i > this.count)) {
			return;
		}

//Util.echoByteArray(toByteArray());
		this.count -= i;
		System.arraycopy(this.buf, i, this.buf, idx, this.count);
//Util.echoByteArray(toByteArray());
/*
		byte[] tmp = new byte[idx + this.count - i];
		if (idx > 0) {
			System.arraycopy(this.buf, 0, tmp, 0, idx);
		}
		System.arraycopy(this.buf, i, tmp, idx, this.count - i);
		this.buf	= tmp;
		this.count	= tmp.length;
*/
	}

	public synchronized byte[] toByteArray(boolean clone) {
		if (clone) {
			return super.toByteArray();
		}
		return this.buf;
	}

	public synchronized byte[] toByteArray(int idx, int num) {
		int i = idx + num;
		if ((idx < 0) || (idx > this.count) || (num <= 0) || (i > this.count)) {
			return null;
		}

		byte[] tmp = new byte[num];
		System.arraycopy(this.buf, idx, tmp, 0, num);
		return tmp;
	}

	public synchronized String toString2(String enc) {
		try {
			return new String(this.buf, 0, size(), enc);
		} catch (Throwable e) {
		}
		return new String(this.buf, 0, size());
	}
}
