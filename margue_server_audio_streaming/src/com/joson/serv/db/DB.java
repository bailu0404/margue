/******************************************************************************
 * File:	DB.java
 * Date:	2014/09/28
 * Author:	Joson_Zhang
 * Description:
 *	base DB class
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/09/28:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.serv.db;

import java.io.*;
import java.sql.*;
import java.text.*;
import javax.sql.*;

import com.joson.lib.comm.*;
import com.joson.lib.db.*;

public class DB {
	private static DBPool	pool	= null;

	public static Connection	getDB() {
		try {
			if (null == pool) {
				pool = new DBPool("MySQL", "db-mysql.ini");
				pool.start();
			}
			return pool.get();
		} catch (Exception e) {
			SysLog.eror(e);
		}
		return null;
	}

	public static long		lastID(Connection c) {
		try {
			String sql = "SELECT LAST_INSERT_ID()";
			PreparedStatement ps = c.prepareStatement(sql);
			try {
				ResultSet rs = ps.executeQuery();
				try {
					if (rs.next()) {
						return rs.getLong(1);
					}
				} finally {
					closeAll(null, ps, rs);
				}
			} catch (Exception e) {
				SysLog.eror(e);
				closeAll(null, ps, null);
			}
		} catch (Exception e) {
			SysLog.eror(e);
		}

		return 0L;
	}

	public static void		closeAll(Connection c, Statement s, ResultSet r) {
		try {
			if (null != r) {
				try {
					r.close();
				} catch (Exception e) {
					SysLog.eror(e);
				}
			}
			if (null != s) {
				try {
					s.close();
				} catch (Exception e) {
					SysLog.eror(e);
				}
			}
/*
			if (null != c) {
				try {
					c.close();
				} catch (Exception e) {
					SysLog.eror(e);
				}
			}
*/
		} finally {
			if (null != pool) {
				pool.release(c);
			}
		}
	}

	public static int		getBool(boolean b) {
		return b ? 1 : 0;
	}
	public static int		getGen(int gen) {
		if ((1 == gen) || (2 == gen)) {
			return gen;
		}
		return 0;
	}
	public static Date		getDate(java.util.Date d) {
		return (null == d) ? null : new Date(d.getTime());
	}
	public static Date		getDate(String s) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			java.util.Date d = sdf.parse(s);
			return new Date(d.getTime());
		} catch (Exception e) {
			SysLog.eror(e);
		}
		return null;
	}
	public static Timestamp	getTimestamp(java.util.Date d) {
		return (null == d) ? null : new Timestamp(d.getTime());
	}
	public static Timestamp	getTimestamp(String s) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			java.util.Date d = sdf.parse(s);
			return new Timestamp(d.getTime());
		} catch (Exception e) {
			SysLog.eror(e);
		}
		return null;
	}
	public static String	getTimestamp(Timestamp t) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(t);
	}
	public static void		setStringOrNull(PreparedStatement ps, int id, String val) throws SQLException {
		if ((null == val) || (val.length() <= 0)) {
			ps.setNull(id, Types.VARCHAR);
		} else {
			ps.setString(id, val);
		}
	}
}
