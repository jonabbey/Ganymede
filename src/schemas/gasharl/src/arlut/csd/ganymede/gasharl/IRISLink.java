/*

   IRISLink.java

   This class uses JDBC to connect to ARL:UT's Oracle databases to
   pull data from IRIS.

   Created: 6 June 2009

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2009
   The University of Texas at Austin

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.gasharl;

import java.sql.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        IRISLink

------------------------------------------------------------------------------*/

/**
 * This class uses JDBC to connect to ARL:UT's Oracle databases to
 * pull data from IRIS, so that we can do verifications for unique
 * user id over historical time, pull biographical details from the HR
 * databases, and etc.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class IRISLink {

  static final boolean debug = true;
  static private Connection conn = null;

  /* -- */

  private Connection getConnection() throws ClassNotFoundException,
					    IllegalAccessException,
					    InstantiationException,
					    SQLException
  {
    /*
      XXX need to implement a connection pooling driver here, see
      http://java.sun.com/developer/onlineTraining/Programming/JDCBook/conpool.html
    */

    if (IRISLink.conn != null)
      {
	return IRISLink.conn;
      }

    synchronized (IRISLink.class)
      {
	Class.forName("oracle.jdbc.OracleDriver").newInstance();

	String hostProperty = System.getProperty("iris.host");

	if (hostProperty == null)
	  {
	    throw new NullPointerException("iris.host not found");
	  }

	String portProperty = System.getProperty("iris.port");

	if (portProperty == null)
	  {
	    throw new NullPointerException("iris.port not found");
	  }

	String schemaProperty = System.getProperty("iris.schema");

	if (schemaProperty == null)
	  {
	    throw new NullPointerException("iris.schema not found");
	  }

	String usernameProperty = System.getProperty("iris.username");

	if (usernameProperty == null)
	  {
	    throw new NullPointerException("iris.username not found");
	  }

	String passwordProperty = System.getProperty("iris.password");

	if (passwordProperty == null)
	  {
	    throw new NullPointerException("iris.password not found");
	  }

	StringBuffer url = new StringBuffer("jdbc:oracle:thin:");

	url.append("@");
	url.append(hostProperty);
	url.append(":");
	url.append(portProperty);
	url.append(":");
	url.append(schemaProperty);

	IRISLink.conn = DriverManager.getConnection(url,
						    usernameProperty,
						    passwordProperty);
      }

    return IRISLink.conn;
  }

  public boolean okayToUseName(String username, String badge)
  {
    try
      {
	Connection myConn = getConnection();

	String queryString = "select badge from arlwfsys where username = ?";
	PreparedStatement queryName = myConn.prepareStatement(queryString);

	queryName.setString(1, username);

	ResultSet rs = queryName.executeQuery();

	if (rs.next())
	  {
	    String archivedBadge = rs.getString(1);

	    if (!badge.equals(archivedBadge))
	      {
	      }
	  }
	else
	  {
	    return true;
	  }
      }
  }
}