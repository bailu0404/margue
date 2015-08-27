/******************************************************************************
 * File:	AudioConferenceHandler.java
 * Date:	2014/10/18
 * Author:	Joson_Zhang
 * Description:
 *	Audio conference keep-alive handler, it will manager all channels/clients
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/10/18:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.serv.handler;

import java.io.*;
import java.nio.*;
import java.util.*;

import javax.sound.sampled.*;

import com.joson.lib.comm.*;
import com.joson.lib.audio.codec.g711.*;
import com.joson.serv.*;
import com.joson.serv.sdk.*;

public class AudioConferenceHandler implements KAliveHandler {
	private static final int	S2C_AUDIODATA	= 0x00;
	private static final int	S2C_BROADCAST	= 0x01;
	private static final int	S2C_CLIENTRESP	= 0x02;

	private static final int	C2S_TALK_START	= 0x00;
	private static final int	C2S_TALK_STOP	= 0x01;
	private static final int	C2S_LISTEN_START= 0x02;
	private static final int	C2S_LISTEN_STOP	= 0x03;
	private static final int	C2S_AUDIODATA	= 0x09;

	private static final byte[]	S2C_ACCEPT		= new byte[] {'\1'};
	private static final byte[]	S2C_REJECT		= new byte[] {'\0'};

	private class Action {
		int		sequence;
		int		command;
		byte[]	data;

		Action() {
		}
	}
	private class Configuration {
		boolean	listening	= false;

		Configuration() {
		}
	}

	private	long					roomid	= 0L;
	private G711uLaw				g711u	= new G711uLaw();
	private KAliveResponser			talking	= null;
	private FileOutputStream		recfile	= null;
	private Vector<KAliveResponser>	vResps	= new Vector<KAliveResponser>();

	public AudioConferenceHandler(long id) {
		roomid = id;
		record(true);
	}

	public boolean	addMember(String rac, HttpResponser resp) {
		resp.setKAliveHandler(this);
		return true;
	}

	public boolean	broadcast(byte[] data) {
		if ((null == data) || (data.length <= 0)) {
			return false;
		}

		byte[] bufr = genAction(S2C_BROADCAST, 0, data);

		synchronized (vResps) {
			int n = vResps.size();
			for (int i=n-1; i>=0; i--) {
				KAliveResponser kar = (KAliveResponser)vResps.get(i);
				if (null == kar) {
					continue;
				}
				if (kar.isClosed()) {
					vResps.remove(i);
					continue;
				}
				Configuration cfg = (Configuration)kar.getConfiguration();
				if (null == cfg) {
					continue;
				}
				if (!cfg.listening) {
					continue;
				}
				sndAction(kar, bufr);
			}
		}

		return true;
	}

	public synchronized boolean	record(boolean on) {
		if (on) {
			if (null == recfile) {
				try {
					recfile = new FileOutputStream(Main.WWWROOT+File.separator+roomid+".raw");
				} catch (Exception e) {
					SysLog.eror(e);
					return false;
				}
			}
		} else {
			if (null == recfile) {
				return false;
			}
			try {
				recfile.close();
			} catch (Exception e) {
				SysLog.eror(e);
			} finally {
				recfile = null;
				AudioFormat afmt = new AudioFormat(8000.0f, 16, 1, true, false);
				try {
					File raw = new File(Main.WWWROOT+File.separator+roomid+".raw");
					try {
						FileInputStream fis = new FileInputStream(Main.WWWROOT+File.separator+roomid+".raw");
						FileOutputStream fos = new FileOutputStream(Main.WWWROOT+File.separator+roomid+".wav");
						AudioSystem.write(new AudioInputStream(fis, afmt, raw.length()/2), AudioFileFormat.Type.WAVE, fos);
					} finally {
						if (!raw.delete()) {
							raw.deleteOnExit();
						}
					}
				} catch (Exception e) {
					SysLog.eror(e);
				}
			}
		}
		return true;
	}

	public boolean	stop() {
		// stop record
		record(false);
		// close all client's connection
		synchronized (vResps) {
			int n = vResps.size();
			for (int i=n-1; i>=0; i--) {
				KAliveResponser resp = (KAliveResponser)vResps.get(i);
				if (null != resp) {
					resp.close();
				}
				vResps.remove(i);
			}
			talking = null;
		}
		return true;
	}

	private Action	getAction(ExtendBAOS baos) {
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
	private byte[]	genAction(int cmd, int seq, byte[] data) {
		int len = ((null == data) ? 0 : data.length)+5;
		byte[] bufr = new byte[len];
		bufr[0]	= '#';
		bufr[1]	= (byte)((len & 0xFF00) >> 1);
		bufr[2]	= (byte)(len & 0x00FF);
		bufr[3]	= (byte)(seq & 0x00FF);
		bufr[4]	= (byte)(cmd & 0x00FF);
		if (null != data) {
			System.arraycopy(data, 0, bufr, 5, len-5);
		}
		return bufr;
	}
	private void	sndAction(KAliveResponser resp, byte[] data) {
		if ((null != resp) && (null != data)) {
			resp.send(data, 0, data.length);
		}
	}
	private void	sndAction(KAliveResponser resp, int cmd, int seq, byte[] data) {
		sndAction(resp, genAction(cmd, seq, data));
	}
	private void	rspAction(KAliveResponser resp, Action a, byte[] data) {
		if (null == a) {
			return;
		}
		sndAction(resp, S2C_CLIENTRESP, a.sequence, data);
	}
	public void		close(KAliveResponser resp) {
		synchronized (vResps) {
			vResps.remove(resp);
			if (resp == talking) {
				talking = null;
			}
		}
	}
	public boolean	process(ExtendBAOS baos, KAliveResponser resp) {
		Configuration cfg = (Configuration)resp.getConfiguration();
		if (null == cfg) {
			cfg = new Configuration();
			resp.setConfiguration(cfg);
		}
		synchronized (vResps) {
			if (!vResps.contains(resp)) {
				vResps.add(resp);
			}
		}
		do {
			Action a = getAction(baos);
			if (null != a) {
				switch (a.command) {
					case C2S_TALK_START:
						if (null != talking) {
							rspAction(resp, a, S2C_REJECT);
						} else {
							talking = resp;
							rspAction(resp, a, S2C_ACCEPT);
						}
						break;
					case C2S_TALK_STOP:
						if (talking == resp) {
							talking = null;
							rspAction(resp, a, S2C_ACCEPT);
						} else {
							rspAction(resp, a, S2C_REJECT);
						}
						break;
					case C2S_LISTEN_START:
						if (cfg.listening) {
							rspAction(resp, a, S2C_REJECT);
						} else {
							cfg.listening = true;
							rspAction(resp, a, S2C_ACCEPT);
						}
						break;
					case C2S_LISTEN_STOP:
						if (cfg.listening) {
							cfg.listening = false;
							rspAction(resp, a, S2C_ACCEPT);
						} else {
							rspAction(resp, a, S2C_REJECT);
						}
						break;
					case C2S_AUDIODATA:
						if (talking != resp) {
							resp.close();
						} else if ((null != a.data) && (a.data.length > 0)) {
							// record audio data
							synchronized (this) {
								if (null != recfile) {
									byte[] pcm = g711u.decode(a.data, 0, a.data.length);
									try {
										recfile.write(pcm, 0, pcm.length);
									} catch (Exception e) {
										SysLog.eror(e);
									}
								}
							}
							// send audio data to all listen client
							byte[] bufr = genAction(S2C_AUDIODATA, 0, a.data);
							synchronized (vResps) {
								int n = vResps.size();
								for (int i=n-1; i>=0; i--) {
									KAliveResponser kar = (KAliveResponser)vResps.get(i);
									if (null == kar) {
										vResps.remove(i);
										continue;
									}
									cfg = (Configuration)kar.getConfiguration();
									if (null == cfg) {
										continue;
									}
									if (!cfg.listening) {
										continue;
									}
									sndAction(kar, bufr);
								}
							}
						}
						break;
					default:
						rspAction(resp, a, S2C_REJECT);
						break;
				}
			} else {
				break;
			}
		} while (true);
		return true;
	}
}
