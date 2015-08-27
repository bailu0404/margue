/******************************************************************************
 * File:	DBPool.java
 * Date:	2013/05/23
 * Author:	Joson_Zhang
 * Description:
 *	DB Pool class
 *
 *			Copyright 2013 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2013/05/23:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.lib.db;

import java.sql.*;
import java.util.*;

import com.joson.lib.comm.*;

public class DBPool {
	static class Config {
		String		URL			= null;
		String		USER		= null;
		String		PSWD		= null;
		String		Option		= null;
		String		Encode		= null;
		String		IdleQuery	= null;
		int			MaxConnNum	= 3;
		long		MaxIdleTime	= 5;	// minutes
		String[]	LibraryPath	= null;

		Config() {
		}

		static Config	loadConfig(String path) {
			Config cfg = new Config();

			IniReader ir = new IniReader();
			ir.setPath(path);
			cfg.URL			= ir.getStr("DBPool", "URL",		"jdbc:mysql://localhost/mydb");
			cfg.USER		= ir.getStr("DBPool", "USER",		"root");
			cfg.PSWD		= ir.getStr("DBPool", "PSWD",		"");
			cfg.Option		= ir.getStr("DBPool", "Option",		"");
			cfg.Encode		= ir.getStr("DBPool", "Encode",		"utf-8");
			cfg.IdleQuery	= ir.getStr("DBPool", "IdleQuery",	"");
			cfg.MaxConnNum	= ir.getInt("DBPool", "MaxConnNum",	3);
			cfg.MaxIdleTime	= ir.getInt("DBPool", "MaxIdleTime",5);
			String libpath	= ir.getStr("DBPool", "LibPath",	"com.mysql.jdbc.Driver");
			if (null != libpath) {
				cfg.LibraryPath = libpath.split(" ");
			}

			return cfg;
		}
	}

	class SchedTask extends TimerTask {
		SchedTask() {
		}

		void kaliveConn(Connection conn, String sql) throws Exception {
			if ((null == sql) || (sql.length() <= 0)) {
				return;
			}
			PreparedStatement ps = conn.prepareStatement(sql);
			try {
				ResultSet rs = ps.executeQuery();
				try {
					if (rs.next()) {
						return;
					}
				} finally {
					rs.close();
				}
			} finally {
				ps.close();
			}
		}

		public void run() {
			Connection conn;
			for (int i=0; i<vCons.size(); i++) {
				try {
					conn = (Connection)vCons.elementAt(i);
					if (!conn.isClosed()) {
						try {
							kaliveConn(conn, cfg.IdleQuery);
							continue;
						} catch (Exception e) {
						}
					}
					if (vCons.remove(conn)) {
						--i;
						SysLog.debg("Remove an invalid DB connection from [" + sPoolName + "]: "+conn.hashCode());
					}
				} catch (SQLException e) {
					SysLog.eror(e);
				}
			}
		}
	}

	static final String CONN_KEY_NAME	= "DBPool.name";

	private String	sPoolName	= null;
	private	Config	cfg			= null;

	private Timer	timer		= null;
	private Vector	vDrvs		= new Vector();

	private int		nCons		= 0;
	private Vector	vCons		= new Vector();

	public DBPool(String name, String cnfg) {
		sPoolName = name;

		cfg = Config.loadConfig(cnfg);
	}

	public void	start() {
		stop();

		loadDrivers();

		timer = new Timer();
		timer.schedule(new SchedTask(), 60*1000, cfg.MaxIdleTime*60*1000);
	}
	public void	stop() {
		if (null != timer) {
			timer.cancel();
			timer = null;
		}

		synchronized (this) {
			Enumeration allConnections = vCons.elements();
			while (allConnections.hasMoreElements()) {
				Connection con = (Connection)allConnections.nextElement();
				try {
					con.close();
					SysLog.debg("Close DB connection success for [" + sPoolName + "].");
				} catch (SQLException e) {
					SysLog.eror(e);
				}
			}
			vCons.removeAllElements();
		}

		unloadDrivers();
	}

	public Connection	get() {
		return get(0L);
	}
	public Connection	get(long timeout) {
		Connection conn = null;

		synchronized (this) {
			long beg = System.currentTimeMillis();

			while (nCons >= cfg.MaxConnNum) {	// reach maximum connection, need wait other release
				try {
					if (timeout <= 0L) {	// 0 means wait forever (milli-seconds)
						wait();
					} else {
						wait(timeout);
					}
				} catch (InterruptedException e) {
				}
				if (timeout > 0L) {
					long now = System.currentTimeMillis();
					if ((now-beg) > timeout) {
						break;
					}
					timeout = timeout - (now - beg);
				}
			}

			if (vCons.size() > 0) {
				conn = (Connection)vCons.remove(0);
				try {
					if (conn.isClosed()) {
						conn = get(timeout);
					}
				} catch (SQLException e) {
					conn = get(timeout);
				}
			} else if (nCons < cfg.MaxConnNum) {
				conn = add();
			}

			if (null != conn) {
				nCons++;
//				SysLog.debg("Borrow a DB connection: "+conn.hashCode());
			}
		}

		return conn;
	}

	public boolean	release(Connection conn) {
		if (null == conn) {
			return false;
		}

		String s = null;
		try {
			s = conn.getClientInfo(CONN_KEY_NAME);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if ((null == s) || !s.equals(sPoolName)) {
			return false;
		}

		synchronized (this) {
			vCons.add(conn);
			--nCons;
			notifyAll();
		}
//		SysLog.debg("Release a DB connection: "+conn.hashCode());

		return true;
	}

	private Connection	add() {
		Connection conn = null;

		StringBuffer sb = new StringBuffer();
		if ((null != cfg.Encode) && (cfg.Encode.length() > 0)) {
			if (sb.length() > 0) {
				sb.append("&");
			}
			sb.append("characterEncoding=").append(cfg.Encode);
		}
		if ((null != cfg.Option) && (cfg.Option.length() > 0)) {
			if (sb.length() > 0) {
				sb.append("&");
			}
			sb.append(cfg.Option);
		}
		String s = cfg.URL;
		if (sb.length() > 0) {
			s = s + "?" + sb.toString();
		}
		try {
			if ((null == cfg.USER) || (cfg.USER.length() <= 0)) {
				conn = DriverManager.getConnection(s);
			} else {
				conn = DriverManager.getConnection(s, cfg.USER, cfg.PSWD);
			}
			conn.setClientInfo(CONN_KEY_NAME, sPoolName);
			SysLog.debg("Add new DB connection for [" + sPoolName + "]: "+conn.hashCode());
		} catch (SQLException e) {
			SysLog.eror(e);
		}

		return conn;
	}

	private void	loadDrivers() {
		if (null == cfg.LibraryPath) {
			return;
		}

		for (int i=0; i<cfg.LibraryPath.length; i++) {
			String path = cfg.LibraryPath[i];
			if ((null == path) || (path.length() <= 0)) {
				continue;
			}

			try {
				Driver driver = (Driver)Class.forName(path).newInstance();
				if (null != driver) {
					DriverManager.registerDriver(driver);
					vDrvs.addElement(driver);
					SysLog.debg("Successed register DB driver: " + path);
				} else {
					SysLog.eror("Failed to register DB driver: " + path);
				}
			} catch (Exception e) {
				SysLog.eror(e);
			}
		}
	}
	private void	unloadDrivers() {
		Enumeration allDrivers = vDrvs.elements();
		while (allDrivers.hasMoreElements()) {
			Driver driver = (Driver)allDrivers.nextElement();
			try {
				DriverManager.deregisterDriver(driver);
			} catch (SQLException e) {
			}
		}
	}
}
