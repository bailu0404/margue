/******************************************************************************
 * File:	AudioConference.java
 * Date:	2014/10/10
 * Author:	Joson_Zhang
 * Description:
 *	Audio conference call SDK library
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/10/10:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
//package com.joson.sdk;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.sound.sampled.*;

//import com.joson.lib.audio.codec.g711.*;

public final class AudioConference implements Runnable {
	static void	echoBytes(byte[] bs) {
		if (null != bs) {
			for (int i=0; i<bs.length; i++) {
				if (i > 0) {
					System.out.print(" ");
				}
				System.out.print(Integer.toString(bs[i]));
			}
			System.out.println();
		}
	}

	public interface Callback {
		public void	receiveData(byte[] bs, int len);
	}
	public abstract class Stream {
		protected boolean	open() {
			return false;
		}
		protected void		close() {
		}
	}

	private class ExtendBAOS extends ByteArrayOutputStream {
		ExtendBAOS() {
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

		public synchronized int	getByte(int index) {
			if ((index < 0) || (index >= this.count)) {
				return -1;
			}
			return (this.buf[index] & 0xFF);
		}

		public synchronized void delBytes(int idx, int num) {
			int i = idx + num;
			if ((idx < 0) || (idx > this.count) || (num <= 0) || (i > this.count)) {
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

		public synchronized byte[] toByteArray(int idx, int num) {
			int i = idx + num;
			if ((idx < 0) || (idx > this.count) || (num <= 0) || (i > this.count)) {
				return null;
			}

			byte[] tmp = new byte[num];
			System.arraycopy(this.buf, idx, tmp, 0, num);
			return tmp;
		}
	}

	private static final int	S2C_AUDIODATA	= 0x00;
	private static final int	S2C_BROADCAST	= 0x01;
	private static final int	S2C_CLIENTRESP	= 0x02;

	private static final int	C2S_TALK_START	= 0x00;
	private static final int	C2S_TALK_STOP	= 0x01;
	private static final int	C2S_LISTEN_START= 0x02;
	private static final int	C2S_LISTEN_STOP	= 0x03;
	private static final int	C2S_AUDIODATA	= 0x09;

	private static final AudioFormat	audioFormat		= new AudioFormat(8000.0f, 16, 1, true, false);

	private int				seq		= 0;
	private	Socket			sock	= null;
	private	InputStream		skis	= null;
	private	OutputStream	skos	= null;

	private	G711uLaw		g711u	= new G711uLaw();

	private	Callback		cbf		= null;
	private	UpStream		up		= null;
	private	DownStream		down	= null;
	private volatile Action	last	= null;	// last receive packet (except S2C_AUDIODATA)

	public AudioConference() {
	}

	private class Action {
		int		sequence;
		int		command;
		byte[]	data;
	}
	private class UpStream extends Stream implements Runnable {
		private boolean	running	= false;

		UpStream() {
		}

		protected boolean	open() {
			Action a = AudioConference.this.send(C2S_TALK_START, null, true);
			if ((null != a) && (S2C_CLIENTRESP == a.command) && (null != a.data) && (a.data.length > 0) && (a.data[0] == 1)) {
				running = true;
				new Thread(this, "UpStream").start();
				return true;
			}
			return false;
		}

		protected void		close() {
			running = false;
		}

		public void	run() {
//			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

//			int	bsRecd = AudioRecord.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_IN_MONO, audioFormat)*10;
//			AudioRecord aRecd = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, AudioFormat.CHANNEL_IN_MONO, audioFormat, bsRecd);
			TargetDataLine aRecd = null;
			try {
				aRecd = (TargetDataLine)AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, audioFormat));
				aRecd.open(audioFormat);
				aRecd.start();
			} catch (Exception e) {
				if (aRecd.isOpen()) {
					aRecd.close();
				}
				e.printStackTrace();
				return;
			}

			byte[] bufr = new byte[160];
			try {
				// loop
				while (running && !AudioConference.this.sock.isClosed()) {
					int n = aRecd.read(bufr, 0, bufr.length);
					if (n < 0) {
						break;
					}
					byte[] audio = g711u.encode(bufr, 0, n);
					AudioConference.this.send(C2S_AUDIODATA, audio, false);
				}
			} finally {
				if (aRecd.isOpen()) {
					aRecd.close();
				}
			}

			AudioConference.this.send(C2S_TALK_STOP, null, false);
		}
	}
	private class DownStream extends Stream {
		private SourceDataLine	aPlay	= null;

		DownStream() {
		}

		protected boolean	open() {
			Action a = AudioConference.this.send(C2S_LISTEN_START, null, true);
			if ((null != a) && (S2C_CLIENTRESP == a.command) && (null != a.data) && (a.data.length > 0) && (a.data[0] == 1)) {
				try {
					aPlay = (SourceDataLine)AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));
					aPlay.open(audioFormat);
					aPlay.start();
				} catch (Exception e) {
					if ((null != aPlay) && aPlay.isOpen()) {
						aPlay.close();
					}
					aPlay = null;
					return false;
				}

				return true;
			}
			return false;
		}

		protected void		close() {
			if ((null != aPlay) && aPlay.isOpen()) {
				aPlay.close();
			}
			aPlay = null;
			AudioConference.this.send(C2S_LISTEN_STOP, null, false);
		}

		protected void		recv(Action a) {
			byte[] audio = g711u.decode(a.data, 0, a.data.length);
			if (null != aPlay) {
				aPlay.write(audio, 0, audio.length);
			}
		}
	}

	private synchronized Action	send(byte[] bs, int off, int len, boolean ack) {
		OutputStream os = skos;
		if (null == os) {
			return null;
		}

		try {
			os.write(bs, off, len);
		} catch (SocketException e) {
			try {
				sock.close();
			} catch (Exception e2) {
			}
			return null;
		} catch (Exception e) {
			return null;
		}

		if (ack) {
			// need response, wait receive thread get the response packet
			long beg = System.currentTimeMillis();
			do {
				Action a = last;
				if ((null != a) && (seq == a.sequence)) {
					return a;
				}
				if ((System.currentTimeMillis()-beg) > 30000) {
					// Maximum wait 30 seconds
					break;
				}
				try {
					Thread.sleep(10L);
				} catch (Exception e) {
				}
			} while (true);
		}

		return null;
	}
	private synchronized Action send(String s, boolean ack) {
		if ((null == s) || (s.length() <= 0)) {
			return null;
		}
		byte[] bs = s.getBytes();
		return send(bs, 0, bs.length, ack);
	}
	private synchronized Action	send(int cmd, byte[] bs, int off, int len, boolean ack) {
		if ((off < 0) || (len < 0) || ((len > 0) && (null == bs))) {
			return null;
		}

		if ((len > 0) && (bs.length < (off+len))) {
			return null;
		}

		++seq;
		if (seq > 0xFF) {
			seq = 0x01;
		}
		int n = len+5;
		byte[] pkt = new byte[n];
		pkt[0]	= '#';
		pkt[1]	= (byte)((n & 0xFF00) >> 1);
		pkt[2]	= (byte)(n & 0x00FF);
		pkt[3]	= (byte)(seq & 0xFF);
		pkt[4]	= (byte)(cmd & 0xFF);
		if (len > 0) {
			System.arraycopy(bs, off, pkt, 5, len);
		}
		return send(pkt, 0, pkt.length, ack);
	}
	private synchronized Action	send(int cmd, byte[] bs, boolean ack) {
		return send(cmd, (null == bs) ? null : bs, 0, (null == bs) ? 0 : bs.length, ack);
	}

	private Action chck(ExtendBAOS baos) {
		// minimal packet size is 5 bytes
		if ((null == baos) || (baos.size() < 5)) {
			return null;
		}
		if ('#' != baos.getByte(0)) {
			int i = baos.indexOf('#');
			if (i < 0) {	// not found start code, clear it
				baos.reset();
				return null;
			}
			baos.delBytes(0, i);
		}
		int n = baos.getByte(1) << 1 | baos.getByte(2);
		if (n < 5) {
			baos.delBytes(0, 1);
			return null;
		}
		if (baos.size() < n) {
			return null;
		}
		Action a = new Action();
		a.sequence	= baos.getByte(3);
		a.command	= baos.getByte(4);
		a.data		= baos.toByteArray(5, n-5);
		baos.delBytes(0, n);
		return a;
	}
	private Action	recv(ExtendBAOS baos, byte[] bufr) {
		InputStream is = skis;
		if (null == is) {
			return null;
		}
		while (true) {
			Action a = chck(baos);
			if (null != a) {
				return a;
			}
			try {
				int n = is.read(bufr);
				if (n < 0) {
					break;
				}
				baos.write(bufr, 0, n);
			} catch (Exception e) {
				break;
			}
		}
		return null;
	}
	public void run() {
//		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

		byte[] buf = new byte[128];
		ExtendBAOS baos = new ExtendBAOS();
		while ((null != AudioConference.this.sock) && !AudioConference.this.sock.isClosed()) {
			Action a = recv(baos, buf);
			if (null == a) {
				continue;
			}
			if (S2C_AUDIODATA == a.command) {
				// listen audio data
				DownStream ds = down;
				if (null != ds) {
					ds.recv(a);
				}
			} else if (S2C_BROADCAST == a.command) {
				if ((null != cbf) && (null != a.data)) {
					cbf.receiveData(a.data, a.data.length);
				}
			} else {
				// other data, put to queue for wait other thread get
				last = a;
			}
		}
	}

	public boolean	isConnected() {
		return (null == sock) ? false : sock.isConnected();
	}

	public boolean	connect(String host, int port, String roomID, String randCode) {
		if ((null == host) || (host.length() <= 0) || (port <= 0)) {
			return false;
		}
		if ((null == roomID) || (roomID.length() <= 0) || (null == randCode) || (randCode.length() <= 0)) {
			return false;
		}

		close();

		StringBuffer sb = new StringBuffer();
		sb.append("GET /Sys/AConf?roomID=").append(roomID).append("&randCode=").append(randCode).append(" HTTP/1.1\r\n");
		sb.append("Host: ").append(host).append(":").append(port).append("\r\n");
		sb.append("Connection: keep-alive\r\n");
		sb.append("\r\n");

		sock = new Socket();
		try {
			sock.connect(new InetSocketAddress(host, port), 15000);	// 15 seconds timeout

			// for reduce audio latency
			try {
				sock.setTcpNoDelay(true);
			} catch (Exception e) {
			}

			skis = sock.getInputStream();
			skos = sock.getOutputStream();

			send(sb.toString(), false);
			Action a = recv(new ExtendBAOS(), new byte[128]);
			if ((null == a) || (S2C_CLIENTRESP != a.command) || (null == a.data) || (a.data.length < 1) || (0x01 != a.data[0])) {
				close();
				return false;
			}

			new Thread(this, getClass().getName()).start();

			return true;
		} catch (Exception e) {
			close();
		}

		return false;
	}

	public void		close() {
		if (null != sock) {
			try {
				sock.close();
			} catch (Exception e) {
			}
			sock = null;
			skis = null;
			skos = null;
		}
	}

	public void		setCallback(Callback cb) {
		cbf = cb;
	}

	public Stream	getUpStream() {
		if (null == up) {
			UpStream s = new UpStream();
			if (!s.open()) {
				return null;
			}
			up = s;
		}
		return up;
	}

	public Stream	getDownStream() {
		if (null == up) {
			DownStream s = new DownStream();
			if (!s.open()) {
				return null;
			}
			down = s;
		}
		return down;
	}

	public void		closeStream(Stream s) {
		if (null == s) {
			return;
		}
		s.close();
		if (s == up) {
			up = null;
		} else if (s == down) {
			down = null;
		}
	}
}
