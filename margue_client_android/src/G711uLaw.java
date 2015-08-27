/******************************************************************************
 * File:	G711uLaw.java
 * Date:	2008/11/19
 * Author:	Joson_Zhang
 * Description:
 *	G.711 uLaw audio codec
 *
 *			Copyright 2008 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2008/11/19:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
//package com.joson.audio.codec.g711;

import java.io.*;

public final class G711uLaw extends G711 {
	public G711uLaw() {
	}

	public byte[] encode(byte[] data, int off, int len) {
		short pcmv;

		m_baos.reset();
		len = len - (len % 2);
		for (int i=0; i<len; i+=2) {
			pcmv = data[off+i];
			pcmv += (data[off+i+1] << 8) & 0xFF00;
			m_baos.write((short)linear2ulaw((int)pcmv));
		}

		return m_baos.toByteArray();
	}

	public byte[] decode(byte[] data, int off, int len) {
		short sample;

		m_baos.reset();
		for(int i=0; i<len; i++) {
			sample = (short)ulaw2linear(data[off+i]);
			m_baos.write(sample & 0xFF);
			m_baos.write((sample & 0xFF00) >> 8);
		}

		return m_baos.toByteArray();
	}
}
