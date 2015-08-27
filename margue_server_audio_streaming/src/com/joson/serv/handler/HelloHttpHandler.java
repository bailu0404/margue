/******************************************************************************
 * File:	HelloHttpHandler.java
 * Date:	2014/09/28
 * Author:	Joson_Zhang
 * Description:
 *	Sample http handler class
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/09/28:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.serv.handler;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import javax.imageio.*;

import org.json.*;

import com.joson.lib.comm.*;
import com.joson.lib.proto.http.*;
import com.joson.serv.db.*;
import com.joson.serv.sdk.*;

public class HelloHttpHandler implements HttpHandler {
	public static boolean	index(ParseHTTP http, HttpResponser resp) {
		return resp.send("Hello, World!");
	}
	public static boolean	connDB(ParseHTTP http, HttpResponser resp) {
		JSONObject json = new JSONObject();
		Connection conn = DB.getDB();
		try {
			String sql = "SELECT id,name FROM hello";
			PreparedStatement ps = conn.prepareStatement(sql);
			try {
				ResultSet rs = ps.executeQuery();
				try {
					while (rs.next()) {
						json.put(rs.getString("id"), rs.getString("name"));
					}
				} finally {
					DB.closeAll(conn, ps, rs);
				}
			} catch (Exception e) {
				SysLog.eror(e);
				DB.closeAll(conn, ps, null);
			}
		} catch (Exception e) {
			SysLog.eror(e);
			DB.closeAll(conn, null, null);
		}
		return resp.send(json.toString());
	}
	public static boolean	postJSON(ParseHTTP http, HttpResponser resp) {
		if (!"POST".equalsIgnoreCase(http.getRequCommand())) {
			return resp.send("Invalid request method!");
		}
		Object obj = http.getAttachment();
		StringBuffer sb = new StringBuffer();
		JSONObject json = new JSONObject(obj.toString());
		for (Iterator i=json.keys(); i.hasNext(); ) {
			String key = (String)i.next();
			String val = json.get(key).toString();
			sb.append(key).append(": ").append(val).append("\r\n");
		}
		return resp.send(sb.toString());
	}
	public static boolean	getPicture(ParseHTTP http, HttpResponser resp) {
		int		width = 192;
		int		height = 48;
		char	mapTable[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		g.setColor(new Color(0xDCCCCC));
		g.fillRect(0, 0, width, height);
		g.setColor(Color.black);
		g.drawRect(0, 0, width - 1, height - 1);

		String s = "";
		for (int i=0; i<4; i++) {
			s += mapTable[(int)(mapTable.length * Math.random())];
		}

		g.setColor(Color.red);
		g.setFont(new Font("Atlantic Inline", Font.PLAIN, 14));
		g.drawString(s.substring(0, 1), 8, 14);
		g.drawString(s.substring(1, 2), 20, 15);
		g.drawString(s.substring(2, 3), 35, 18);
		g.drawString(s.substring(3, 4), 45, 15);

		g.dispose();

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "JPEG", baos);
			resp.setKeyValue("Content-Type", "image/jpeg");
			byte[] bs = baos.toByteArray();
			resp.send(bs, 0, bs.length);
			return true;
		} catch (IOException e) {
		}
		return false;
	}
}
