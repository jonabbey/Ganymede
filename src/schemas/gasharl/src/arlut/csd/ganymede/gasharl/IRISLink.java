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
import javax.sql.*;
import com.mchange.v2.c3p0.*;

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
  static private ComboPooledDataSource source = null;

  /* -- */

  private synchronized static ComboPooledDataSource getSource()
  {
    if (source != null)
      {
	return source;
      }

    source = new ComboPooledDataSource();

    try
      {
	source.setDriverClass("oracle.jdbc.OracleDriver");
      }
    catch (java.beans.PropertyVetoException ex)
      {
	throw new RuntimeException(ex);
      }

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

    StringBuffer url = new StringBuffer("jdbc:oracle:thin:");

    url.append("@");
    url.append(hostProperty);
    url.append(":");
    url.append(portProperty);
    url.append(":");
    url.append(schemaProperty);

    System.err.println("**** ---- ****");
    System.err.println("**** URL is " + url + " ****");
    System.err.println("**** ---- ****");

    source.setJdbcUrl(url.toString());

    String usernameProperty = System.getProperty("iris.username");

    if (usernameProperty == null)
      {
	throw new NullPointerException("iris.username not found");
      }

    source.setUser(usernameProperty);

    String passwordProperty = System.getProperty("iris.password");

    if (passwordProperty == null)
      {
	throw new NullPointerException("iris.password not found");
      }

    source.setPassword(passwordProperty);

    source.setInitialPoolSize(3);
    source.setMinPoolSize(3);
    source.setMaxPoolSize(10);
    source.setMaxStatements(5);

    return source;
  }

  private Connection getConnection()
  {
    try
      {
	return getSource().getConnection();
      }
    catch (SQLException ex)
      {
	ex.printStackTrace();

	return null;
      }
  }

  public boolean okayToUseName(String username, String badge)
  {
    Connection myConn = null;
    boolean success = false;

    try
      {
	myConn = getConnection();

	String queryString = "select BADGE_NUMBER from HR_EMPLOYEES_GA_VW where NETWORK_USER_ID = ?";
	PreparedStatement queryName = myConn.prepareStatement(queryString);

	queryName.setString(1, username);

	ResultSet rs = queryName.executeQuery();

	try
	  {
	    if (rs.next())
	      {
		String archivedBadge = rs.getString(1);

		System.err.println("BADGE_NUMBER is " + archivedBadge + " for NETWORK_USER_ID " + username);

		if (!badge.equals(archivedBadge))
		  {
		    success = false;
		  }
		else
		  {
		    success = true;
		  }
	      }
	    else
	      {
		System.err.println("No record found for NETWORK_USER_ID " + username);

		success = true;
	      }
	  }
	finally
	  {
	    rs.close();
	  }
      }
    catch (SQLException ex)
      {
	ex.printStackTrace();

	throw new RuntimeException(ex);
      }
    finally
      {
	try
	  {
	    myConn.close();
	  }
	catch (SQLException ex)
	  {
	  }
      }

    return success;
  }

  public static void main(String args[])
  {
    IRISLink test = new IRISLink();

    test.okayToUseName(args[0], args[1]);
  }
}
