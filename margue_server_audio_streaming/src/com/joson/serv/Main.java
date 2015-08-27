/******************************************************************************
 * File:	Main.java
 * Date:	2014/09/28
 * Author:	Joson_Zhang
 * Description:
 *	Main class
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/09/28:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.serv;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.lang.reflect.*;

import ch.unifr.nio.framework.*;
import ch.unifr.nio.framework.transform.*;

import com.joson.lib.comm.*;
import com.joson.lib.proto.http.*;

import com.joson.serv.sdk.*;

public class Main extends AbstractAcceptor {
	static HashMap	mime	= new HashMap();
	static {
		mime.put("htm",		"text/html");
		mime.put("html",	"text/html");
		mime.put("gif",		"image/gif");
		mime.put("jpg",		"image/jpeg");
		mime.put("wav",		"audio/x-wav");
	}

	public static final String	WWWROOT		= "www";
	private static final String	CHARSET		= "UTF-8";
	private CharsetEncoder			sEnc	= null;
	private	CharsetDecoder			sDec	= null;

	public Main(Dispatcher dispatcher, SocketAddress addr) throws IOException, UnsupportedCharsetException {
		super(dispatcher, addr);

		Charset c = (null == CHARSET) ? Charset.defaultCharset() : Charset.forName(CHARSET);
		if (null == c) {
			throw new UnsupportedCharsetException(CHARSET);
		}
		sEnc = c.newEncoder();
		sDec = c.newDecoder();

//for (Enumeration e=java.util.logging.LogManager.getLogManager().getLoggerNames(); e.hasMoreElements(); ) {
//	System.out.println(e.nextElement());
//}
	}

	protected ChannelHandler	getHandler(SocketChannel sc) {
		return new ClientHandler(sc);
	}

	private String		ByteBufferToString(ByteBuffer bb) throws CharacterCodingException {
		CharBuffer cb = sDec.decode(bb);
		return cb.toString();
	}
	private ByteBuffer	StringToByteBuffer(String s) throws CharacterCodingException {
		CharBuffer cb = CharBuffer.allocate(s.length());
		cb.put(s);
		cb.flip();
		return sEnc.encode(cb);
	}

	private class HttpReceiver extends AbstractForwarder<ByteBuffer, ByteBuffer> implements BufferSizeListener, HttpResponser, KAliveResponser {
		private	boolean			bClosed	= false;
		private	boolean			bRsp	= false;
		private	int				hcod	= 200;
		private	String			hmsg	= "OK";
		private HashMap			kmap	= new HashMap();
		private	ExtendBAOS		baos	= new ExtendBAOS();
		private SocketChannel	sock	= null;
		private KAliveHandler	kahdlr	= null;
		private Object			kaconf	= null;
		private	boolean			parsed	= false;
		private ParseHTTP		parser	= new ParseHTTP();

		HttpReceiver(SocketChannel sc, ClientHandler handler) {
			sock = sc;

			handler.getChannelWriter().addBufferSizeListener(this);
			setKeyValue("Content-Type", "application/json; charset=utf-8");
		}

		void	notifyClose() {
			bClosed = true;
			if (null != kahdlr) {
				kahdlr.close(this);
			}
		}

		public void		bufferSizeChanged(Object source, int newLevel) {
			if ((null == kahdlr) && (newLevel <= 0)) {
				try {
					sock.close();
				} catch (Exception e) {
				}
			}
		}

		public void		outAsRaw(boolean b) {
			bRsp = b;
		}

		public boolean	head(int code, String msg) {
			if (bRsp || (code <= 0) || (null == msg) || (msg.length() <= 0)) {
				return false;
			}

			hcod = code;
			hmsg = msg;

			return true;
		}

		public boolean	setKeyValue(String key, String value) {
			if (bRsp || (null == key) || (key.length() <= 0)) {
				return false;
			}

			String key2 = key.toUpperCase();

			if ((null == value) || (value.length() <= 0)) {
				kmap.remove(key2);
			} else {
				kmap.put(key2, key+": "+value);
			}

			return true;
		}

		private void	sendHeader() {
			if (bRsp) {
				return;
			}

			bRsp = true;

			StringBuffer sb = new StringBuffer();
			sb.append("HTTP/1.1 ").append(hcod).append(" ").append(hmsg).append("\r\n");
			sb.append("Server: ws(joson)/1.0.1\r\n");
			for (Iterator i=kmap.values().iterator(); i.hasNext();) {
				sb.append(i.next()).append("\r\n");
			}
			sb.append("Connection: close\r\n");
			sb.append("\r\n");
			if (send(sb.toString())) {
				return;
			}

			bRsp = false;
		}

		public boolean	send(String s) {
			sendHeader();
			if ((null != s) && (s.length() > 0)) {
				try {
					nextForwarder.forward(StringToByteBuffer(s));
					return true;
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
			return false;
		}
		public boolean	send(Object o) {
			return send((null == o) ? null : o.toString());
		}
		public boolean	send(byte[] bs, int off, int len) {
			sendHeader();
			if ((null != bs) && (off >= 0) && (len > 0) && (bs.length >= (off+len))) {
				try {
					nextForwarder.forward(ByteBuffer.wrap(bs, off, len));
					return true;
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
			return false;
		}

		public void		setKAliveHandler(KAliveHandler handler) {
			try {
				sock.socket().setTcpNoDelay(true);
			} catch (Exception e) {
			}
			kahdlr = handler;
		}

		public void		close() {
			try {
				sock.close();
			} catch (Exception e) {
			}
		}

		public boolean	isClosed() {
			return bClosed;
		}

		public void		setConfiguration(Object obj) {
			kaconf = obj;
		}

		public Object	getConfiguration() {
			return kaconf;
		}

		private void	error(int code, String mesg, String data) throws IOException {
			head(code, "Invalid Request");
			setKeyValue("Content-Type", "text/plain");
			sendHeader();
			send((null == data) ? hmsg : data);
			if (!((ChannelWriter)nextForwarder).hasRemaining()) {
				sock.close();
			}
		}
		private boolean	actFile(String uri) {
			String path = WWWROOT+File.separator+uri.replace('/', File.separatorChar);
			File f = new File(path);
			if (f.exists()) {
				String m = (String)mime.get(Util.getFileExt(path));
				if (null != m) {
					setKeyValue("Content-Type", m);
					setKeyValue("Content-Length", Long.toString(f.length()));
					try {
						FileInputStream fis = new FileInputStream(f);
						try {
							byte[] buf = new byte[2048];
							do {
								int n = fis.read(buf);
								if (n <= 0) {
									break;
								}
								send(buf, 0, n);
							} while (true);
							return true;
						} finally {
							fis.close();
						}
					} catch (Exception e) {
					}
				}
			}
			return false;
		}
		private boolean	actAction(String uri) {
			String clas = uri;
			String func = "index";
			int i = uri.lastIndexOf('/');
			if (i > 0) {
				clas = uri.substring(0, i).replace('/', '.');
				func = uri.substring(i+1);
				if ((null == func) || (func.length() <= 0)) {
					func = "index";
				}
			}
			String s = getClass().getPackage().getName()+".handler."+clas+"HttpHandler";
			try {
				Class clsz = getClass().getClassLoader().loadClass(s);
				Method mthd = clsz.getDeclaredMethod(func, ParseHTTP.class, HttpResponser.class);
				if (null != mthd) {
					Object o = mthd.invoke(null, parser, this);
					if ((o instanceof Boolean) && ((Boolean)o).booleanValue()) {
						return true;
					}
				}
			} catch (Exception e) {
				//e.printStackTrace();
			}
			return false;
		}
		public void forward(ByteBuffer input) throws IOException {
			if (input.position() > 0) {
				input.flip();
			}
			try {
				baos.write(input.array(), 0, input.limit());//ByteBufferToString(input), "utf-8");
				input.flip();
			} catch (Exception e) {
				sock.close();
				e.printStackTrace();
				return;
			}

			// RAW processor
			if (null != kahdlr) {
				if (!kahdlr.process(baos, this)) {
					kahdlr = null;
					// Don't close channel here, it will cause data no flush to client.
					if (!((ChannelWriter)nextForwarder).hasRemaining()) {
						sock.close();
					}
				}
				return;
			}

			// HTTP processor
			if (!parsed && !parser.parse(baos)) {
				return;
			}

			parsed = true;

			String method = parser.getRequCommand();
			if ((null == method) || (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method))) {
				error(401, "Invalid Request", null);
				return;
			}

			boolean bGET = "GET".equalsIgnoreCase(method);
			if (!bGET) {
				int n = parser.getLength();
				if (n <= 0) {
					error(401, "Invalid Request", "No Content-Length in HTTP header!");
					return;
				} else if (baos.size() < n) {
					// Need wait all data ready!
					return;
				}
				parser.setAttachment(baos);
			}

			String uri = parser.getRequURI();
System.out.println("New request: ["+sock.socket().getRemoteSocketAddress()+"] "+uri);
			int i = uri.indexOf('?');
			if (i > 0) {
				uri = uri.substring(0, i);
			}
			uri = uri.substring(1);	// remove first / char

			boolean bProcessed = bGET ? actFile(uri) : false;
			if (!bProcessed) {
				bProcessed = actAction(uri);
			}
			if (!bProcessed) {	// Not found process
				error(404, "NOT Found", "URL="+parser.getRequURI());
				return;
			}

			// Don't close channel here, it will cause data no flush to client.
			if ((null == kahdlr) && !((ChannelWriter)nextForwarder).hasRemaining()) {
				sock.close();
			}

			baos.reset();
		}
	}

	private class ClientHandler extends AbstractChannelHandler {
		private HttpReceiver	http	= null;

		ClientHandler(SocketChannel sc) {
			http = new HttpReceiver(sc, this);

			reader.setNextForwarder(http);
			http.setNextForwarder(writer);
		}

		public void inputClosed() {
			try {
				this.handlerAdapter.closeChannel();
				if (null != http) {
					http.notifyClose();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void channelException(Exception exception) {
			try {
				this.handlerAdapter.closeChannel();
				if (null != http) {
					http.notifyClose();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		if (1 != args.length) {
			System.out.println("Usage: Main <port>");
			return;
		}

		try {
			Dispatcher d = new Dispatcher();
			d.start();

			int port = Integer.parseInt(args[0]);
			Main main = new Main(d, new InetSocketAddress(port));
			main.start();

			System.out.println("Server is ready, listening at port "+port+" ...");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
