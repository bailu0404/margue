/******************************************************************************
 * File:	ProtoHTTP.java
 * Date:	2011/08/19
 * Author:	Joson_Zhang
 * Description:
 *	HTTP protocol implementation
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
import java.net.*;

import com.joson.lib.comm.*;
import com.joson.lib.crypto.base64.*;

public class ProtoHTTP extends ParseHTTP {
	public static class Proxy {
		String	sHOST	= null;
		int		uPORT	= 0;
		String	sUSER	= null;
		String	sPSWD	= null;
		String	sAUTH	= null;
		public Proxy(String host, int port, String user, String pswd) {
			sHOST = host;
			uPORT = port;
			sUSER = user;
			sPSWD = pswd;
			if ((null != user) && (null != pswd)) {
				sAUTH = Base64.encode(user+":"+pswd);
			}
		}
		public String	getHost() {
			return sHOST;
		}
		public int		getPort() {
			return uPORT;
		}
		public String	getUser() {
			return sUSER;
		}
		public String	getPswd() {
			return sPSWD;
		}
		public String	getAuth() {
			return sAUTH;
		}
	}

	private	String		sHostORG	= null;	// for proxy
	private int			uPortORG	= 0;	// for proxy
	private	String		sHost		= null;
	private	int			uPort		= 0;
	private	String		sURI		= null;
	private	byte[]		aTemp		= new byte[256];

	private	int			maxPackSize	= 50 * 1024;
	private	float		verHttp		= 1.1F;
	private	boolean		getHttp		= true;
	private	MyMap		mapParamSet	= new MyMap();
	private	CbintfHTTP	cbiHttp		= null;

	private Proxy		pxyHTTP		= null;
	private	Socket		sckHTTP		= null;

	private	boolean		m_hasHost	= true;
	private	boolean		m_hasConn	= true;

	public ProtoHTTP(Socket sock) {
		sckHTTP = sock;
	}
	public ProtoHTTP(String host, int port, String uri, String user, String pswd) {
		sHost = host;
		uPort = port;
		sURI  = uri;
		if ((null != user) && (null != pswd)) {
			setParameter("Authorization", "Basic "+Base64.encode(user+":"+pswd));
		}
	}

	protected void finalize() {
		close();
	}

	public Socket getSocket() {
		return sckHTTP;
	}

	public boolean	setProxy(Proxy pxy) {
		if ((null == pxy) || (null != sckHTTP)) {
			return false;
		}

		pxyHTTP = pxy;
		if (null != pxy.sAUTH) {
			ProtoHTTP.this.setParameter("Proxy-Authorization", "Basic "+pxy.sAUTH);
		}

		return true;
	}
	public Proxy	getProxy() {
		return pxyHTTP;
	}

	public void setNonHttpParam(boolean bHost, boolean bConnection) {
		m_hasHost	= bHost;
		m_hasConn	= bConnection;
	}

	public void setParameter(String key, String val) {
		if ((null == key) || (key.length() <= 0)) {
			return;
		}
		if ((null == val) || (val.length() <= 0)) {
			return;
		}
		mapParamSet.put(key, val);
	}
	public void setUserAgent(String val) {
		setParameter("User-Agent", val);
	}

	public int getMaxPacketSize() {
		return maxPackSize;
	}
	public void setMaxPacketSize(int size) {
		maxPackSize = size;
	}

	public boolean isMethodGet() {
		return getHttp;
	}
	public void setMethodGet(boolean flag) {
		getHttp = flag;
	}

	public void setHttpVersion(float ver) {
		verHttp = ver;
	}

	public void setChunkListener(CbintfHTTP cbi) {
		cbiHttp = cbi;
	}

	public String rcvHttpHeader(int tmoMSEC) {
		return rcvHttpHeader(tmoMSEC, false);
	}
	public String rcvHttpHeader(int tmoMSEC, boolean singleLine) {
		InputStream is = null;
		try {
			is = sckHTTP.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		int nCR = 0;
		int nLF = 0;
		long beg = System.currentTimeMillis();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		do {
			try {
				if (is.available() > 0) {
					int n = is.read(aTemp, 0, 1);
					if (n <= 0) {
						break;
					}
					baos.write(aTemp, 0, n);
					switch (aTemp[0]) {
						case '\r':
							if ((0 == nCR) && (0 == nLF)) {
								++nCR;
							} else if ((1 == nCR) && (1 == nLF)) {
								++nCR;
							} else {
								nCR = nLF = 0;
							}
							break;
						case '\n':
							if ((1 == nCR) && (0 == nLF)) {
								++nLF;
							} else if ((2 == nCR) && (1 == nLF)) {
								++nLF;
							} else {
								nCR = nLF = 0;
							}
							break;
						default:
							nCR = 0;
							nLF = 0;
							break;
					}
					if (singleLine && ((nCR >= 1) && (nLF >= 1))) {
						break;
					}
					if ((nCR >= 2) && (nLF >= 2)) {
						break;
					}
					if (baos.size() >= maxPackSize) {
						break;
					}
				} else {
					if (sckHTTP.isClosed()) {
						break;
					}

					long offs = System.currentTimeMillis() - beg;
					if (offs >= tmoMSEC) {
						break;
					}
					try {	// sleep for lighten CPU loading.
						Thread.sleep(10L);
					} catch (Exception e) {
					}
				}
			} catch (Exception e) {
//				e.printStackTrace();
				break;
			}
		} while (true);
		if (!singleLine && ((nCR < 2) || (nLF < 2))) {			// Timeout or Broken
			baos.reset();
			return null;
		}
		return baos.toString();
	}

	/**
	 * Description:
	 *	Connect to server
	 *
	 * Input:
	 *	tmoMSEC		Timeout value wait server response in Milli-Second
	 *	bNoRequest	Don't send GET/POST/... request
	 *
	 * Output:
	 *	true		connect ok and recieve valid response
	 *	false		connect fail or recieve invalid response
	 */
	public boolean connect(int tmoMSEC, boolean bNoRequest, boolean bNoResponse) {
		String	host = (null == pxyHTTP) ? sHost : pxyHTTP.sHOST;
		int		port = (null == pxyHTTP) ? uPort : pxyHTTP.uPORT;
		String	suri = (null == pxyHTTP) ? sURI : ("http://"+sHost+":"+uPort+sURI);
		try {
			sckHTTP = new Socket(host, port);
		} catch (ConnectException e) {
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		if (bNoRequest) {
			return true;
		}

		OutputStream os = null;
		try {
			os = sckHTTP.getOutputStream();
		} catch (Exception e) {
			close();
			e.printStackTrace();
			return false;
		}

		String s = (getHttp?"GET":"POST")+" "+suri+" HTTP/"+Util.Double2Str(verHttp, 1)+"\r\n";
		if (m_hasHost) {
			s += "Host: "+((null == sHostORG) ? sHost : sHostORG)+":"+((null == sHostORG) ? uPort : uPortORG)+"\r\n";
		}
		for (int i=0; i<mapParamSet.size(); i++) {
			String k = (String)mapParamSet.getKey(i);
			String v = (String)mapParamSet.getVal(i);
			if ((null == k) || (k.length() <= 0)) {
				continue;
			}
			if ((null == v) || (v.length() <= 0)) {
				continue;
			}
			s = s + k+": "+v+"\r\n";
		}
		if (m_hasConn) {
			s = s + "Connection: close\r\n";
		}
		s = s + "\r\n";
//System.out.println(s);
		try {
			os.write(s.getBytes());
		} catch (Exception e) {
			close();
			e.printStackTrace();
			return false;
		}

		if (bNoResponse) {
			return true;
		}

		if (!parse(rcvHttpHeader(tmoMSEC))) {
			close();
			return false;
		}

		if (3 == (getRespCode()/100)) {	// Redirect
			String l = getParam("Location");
			if ((null == l) || (l.length() <= 0)) {
				close();
				return false;
			}
			close();
			if (null == sHostORG) {
				sHostORG = sHost;
				uPortORG = uPort;
			}
			if ('/' == l.charAt(0)) {
				sURI = l;
			} else {
				try {
					URL u = new URL(l);
					sHost = u.getHost();
					uPort = u.getPort();
					if (uPort < 0) {
						uPort = u.getDefaultPort();
					}
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			return connect(tmoMSEC, bNoRequest, bNoResponse);
		}

		return (200 == getRespCode()) ? true : false;
	}

	public boolean sndRequest(String data) {
		if ((null == data) || (data.length() <= 0)) {
			return false;
		}
		byte[] ary = data.getBytes();
		return sndRequest(ary, 0, ary.length);
	}
	/**
	 * Description:
	 *	Send data to server for HTTP-POST command
	 *
	 * Input:
	 *	data	post data byte array
	 *	off		begin offset
	 *	len		post data len
	 *
	 * Output:
	 *	true	send ok
	 *	false	send fail
	 */
	public boolean sndRequest(byte[] data, int off, int len) {
		if (null == sckHTTP) {
			return false;
		}

		try {
			sckHTTP.getOutputStream().write(data, off, len);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Description:
	 *	Recieve server response data
	 *
	 * Input:
	 *	tmoMSEC		Timeout value in Milli-Second, <0 will recieve to end.
	 *	rcvWait		For support NO Content-Length HTTP response, the parameter
	 *				equal with 0 will be ignored, otherwise it will used at when 
	 *				socket get more than 1 bytes response, and wait continue reponse
	 *				timeout within Milli-Seconds.
	 *	baos		Return data buffer
	 *
	 * Output:
	 *	true		successful, maybe has more data for recieve if tmoMSEC set
	 *	false		fail, maybe network broken or other case
	 */
	public boolean rcvResponse(int tmoMSEC, int rcvWait, ByteArrayOutputStream baos) {
		if (null == baos) {
			return false;
		}
		baos.reset();

		InputStream is = null;
		try {
			is = sckHTTP.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		int iLen = getLength();
		String sMixed = getMixedBoundary();
		boolean bMixed = isMixed();
		boolean bChunk = isChunked();
		ParseHTTP chkParser = new ParseHTTP();
		ExtendBAOS chkHdrOS = null;

		if (bMixed && ((null == sMixed) || (sMixed.length() <= 0))) {
			return false;
		}

		byte[] xMixed = ("\r\n"+sMixed).getBytes();

		int n;
		int len = iLen;
		long beg = System.currentTimeMillis();
		do {
			if ((len <= 0) && (null == chkHdrOS)) {
				if (iLen >= 0) {
					break;
				}
				if ((null != cbiHttp) && (baos.size() > 0)) {
					cbiHttp.rcvData(baos);
				}
				if (bMixed) {
					baos.reset();
				}
				if (bChunk) {
					String slen;
					if (tmoMSEC < 0) {
						slen = rcvHttpHeader(5000, true);
					} else {
						slen = rcvHttpHeader(tmoMSEC-(int)(System.currentTimeMillis()-beg), true);
					}
					if (null == slen) {
						return false;
					}
					n = slen.indexOf("\r\n");
					if (n <= 0) {
						return false;
					}
					try {
						len = Integer.parseInt(slen.substring(0, n), 16);
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					}
					if (0 == len) {
						break;
					}
					len += 2;	// \r\n
				} else if (bMixed) {
					boolean bResult;
					if (tmoMSEC < 0) {
						bResult = chkParser.parse(rcvHttpHeader(5000));
					} else {
						bResult = chkParser.parse(rcvHttpHeader(tmoMSEC-(int)(System.currentTimeMillis()-beg)));
					}
					if (!bResult) {	// Network broken, or invalid header
						return false;
					}
					len = chkParser.getLength();
					if ((len < 0) && (null == chkHdrOS)) {
						chkHdrOS = new ExtendBAOS();
					}
				} else {
					if (iLen > 0) {	// complete
						break;
					} else {		// Unknown size
						len = 4096;
					}
				}
//System.out.println("len: "+len);
			}
			try {
				if (is.available() <= 0) {
					if (!bChunk && !bMixed) {
						int tmo = tmoMSEC;
						if ((tmoMSEC < 0) && (iLen < 0)) {
							tmo = 1000;
						}
						if ((System.currentTimeMillis() - beg) > tmo) {
							break;
						}
						if ((iLen < 0) && (rcvWait > 0) && ((System.currentTimeMillis() - beg) > rcvWait)) {
							if (baos.size() > 0) {	// has more than 1 bytes recieved.
								break;
							}
						}
					}
					try {	// sleep for lighten CPU loading.
						Thread.sleep(10L);
					} catch (Exception e) {
					}
					continue;
				}
				n = is.read(aTemp, 0, (len>aTemp.length)?aTemp.length:((len<0)?aTemp.length:len));
				if (n <= 0) {
					return false;
				}
				len -= n;
				if (null != chkHdrOS) {
					chkHdrOS.write(aTemp, 0, n);
					int i = Util.binSearch(chkHdrOS.toByteArray(false), 0, chkHdrOS.size(), xMixed, 0, xMixed.length);
					if (i < 0) {
						i = chkHdrOS.size();
						if (i > xMixed.length) {
							i -= xMixed.length;
							baos.write(chkHdrOS.toByteArray(false), 0, i);
							chkHdrOS.delBytes(0, i);
						}
					} else {
						int sz = chkHdrOS.size();
						sz -= i;
						baos.write(chkHdrOS.toByteArray(false), 0, i);
						chkHdrOS.delBytes(0, i);
						if (null != cbiHttp) {
							cbiHttp.rcvData(baos);
						}
						baos.reset();
						i = Util.binSearch(chkHdrOS.toByteArray(false), 0, chkHdrOS.size(), "\r\n\r\n".getBytes(), 0, 4);
						if (i < 0) {
							if (null == rcvHttpHeader(5000)) {
								return false;
							}
							chkHdrOS.reset();
						} else {
							chkHdrOS.delBytes(0, i+4);
						}
					}
				} else {
					baos.write(aTemp, 0, n);
				}
				if (!bMixed && (maxPackSize > 0) && (baos.size() >= maxPackSize)) {
					if (null != cbiHttp) {
						cbiHttp.rcvData(baos);
					}
					baos.reset();
				}
			} catch (Exception e) {
				return false;
			}
		} while (true);

		return true;
	}

	public void close() {
		try {
			if ((null != sckHTTP) && sckHTTP.isConnected()) {
				sckHTTP.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		sckHTTP = null;
	}

	public static boolean	download(String host, int port, String suri, String user, String pswd, ByteArrayOutputStream baos) {
		return download(host, port, suri, user, pswd, 15000, baos);
	}
	public static boolean	download(String host, int port, String suri, String user, String pswd, int tmo_ms, ByteArrayOutputStream baos) {
		if (null == baos) {
			return false;
		}

		ProtoHTTP http = new ProtoHTTP(host, port, suri, user, pswd);
		if (!http.connect(30000, false, false)) {
			return false;
		}

		try {
			int len = http.getLength();
			do {
				if (!http.rcvResponse(tmo_ms, 1000, baos)) {
					return false;
				}
				if (len < 0) {
					break;
				}
				if (baos.size() >= len) {
					break;
				}
			} while (true);
		} finally {
			http.close();
		}

		return true;
	}
}
