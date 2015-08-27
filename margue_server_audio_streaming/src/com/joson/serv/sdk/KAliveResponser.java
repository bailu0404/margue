/******************************************************************************
 * File:	KAliveResponser.java
 * Date:	2014/10/18
 * Author:	Joson_Zhang
 * Description:
 *	Keep-Alive session responser interface
 *
 *			Copyright 2014 Joson_Zhang
 *****************************************************************************/
/******************************************************************************
 * Modify History:
 *	2014/10/18:	Joson_Zhang
 *		1. initial create
 *****************************************************************************/
package com.joson.serv.sdk;

public interface KAliveResponser extends HttpResponser {
	/******************************************************
	 * Description:
	 *	close the session
	 *
	 * Input:
	 *	NONE
	 *
	 * Output:
	 *	NONE
	 *****************************************************/
	public void		close();

	/******************************************************
	 * Description:
	 *	check the session is closed
	 *
	 * Input:
	 *	NONE
	 *
	 * Output:
	 *	boolean
	 *****************************************************/
	public boolean	isClosed();

	/******************************************************
	 * Description:
	 *	get configuration data
	 *
	 * Input:
	 *	obj		configuration object
	 *
	 * Output:
	 *	NONE
	 *****************************************************/
	public void		setConfiguration(Object obj);

	/******************************************************
	 * Description:
	 *	set configuration data
	 *
	 * Input:
	 *	NONE
	 *
	 * Output:
	 *	configuration object
	 *****************************************************/
	public Object	getConfiguration();
}
