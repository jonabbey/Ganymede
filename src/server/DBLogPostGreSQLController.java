/*

   DBLogPostGreSQLController.java

   This controller class manages the recording and retrieval of
   DBLogEvents for the DBLog class, using a PostGreSQL database for
   the storage format.
   
   Created: 26 February 2003
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2003/02/27 03:23:29 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.ganymede;

import arlut.csd.Util.WordWrap;

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.io.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.Timestamp;

/*------------------------------------------------------------------------------
                                                                           class
                                                       DBLogPostGreSQLController

------------------------------------------------------------------------------*/

/**
 * <p>This controller class manages the recording and retrieval of
 * {@link arlut.csd.ganymede.DBLogEvent DBLogEvents} for the {@link
 * arlut.csd.ganymede.DBLog DBLog} class, using a PostGreSQL database
 * for the storage format.</p>
 *
 * @version $Revision: 1.1 $ $Date: 2003/02/27 03:23:29 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBLogPostGreSQLController implements DBLogController {

  static int primaryKey = 0;

  // ---

  Connection con = null;
  PreparedStatement statement = null;
  PreparedStatement emailState = null;
  PreparedStatement invidState = null;

  /**
   * If we're in the middle of a transaction, transactionID will hold
   * the transaction identifier.  We use this to match up startTransaction
   * and finishTransaction, yo.
   */

  String transactionID;
  Vector transInvids;

  public DBLogPostGreSQLController(String hostname, String databaseName,
				   String username, String password)
  {
  } 

  /**
   * <p>This method writes the given event to the persistent storage managed by
   * this controller.</p>
   *
   * @param event The DBLogEvent to be recorded
   */

  public void writeEvent(DBLogEvent event)
  {


    if (con == null)
      {
	throw new IllegalArgumentException("no connection to postgres database");
      }

    try
      {
	if (statement == null)
	  {
	    statement = con.prepareStatement("insert into event (event_id, javatime, sqltime, classtoken, " +
					     "admin_invid, admin_name, trans_id, text) " +
					     "values (?,?,?,?,?,?,?,?)");
	  }

	if (event.eventClassToken.equals("starttransaction"))
	  {
	    transactionID = event.transactionID;
	    transInvids = (Vector) event.objects.clone();
	  }

	primaryKey++;
	statement.setInt(1, primaryKey);
	statement.setLong(2, event.time.getTime());
	statement.setTimestamp(3, new java.sql.Timestamp(event.time.getTime()));
	statement.setString(4, event.eventClassToken);
		
	if (event.admin != null)
	  {
	    statement.setString(5, event.admin.toString());
	  }
	else
	  {
	    statement.setNull(5, java.sql.Types.VARCHAR);
	  }
		
	if (event.adminName != null)
	  {
	    statement.setString(6, event.adminName);
	  }
	else
	  {
	    statement.setNull(5, java.sql.Types.VARCHAR);
	  }
		
	if (event.transactionID != null)
	  {
	    statement.setString(7, event.transactionID);
	  }
	else
	  {
	    statement.setNull(5, java.sql.Types.VARCHAR);
	  }
		
	if (event.description != null)
	  {
	    statement.setString(8, event.description);
	  }
	else
	  {
	    statement.setNull(5, java.sql.Types.VARCHAR);
	  }

	statement.addBatch();

	for (int i = 0; event.notifyVect != null && i < event.notifyVect.size(); i++)
	  {
	    statement.addBatch("insert into email (event_id, address) values(" +
			       primaryKey + ", '" + 
			       (String) event.notifyVect.elementAt(i) +
			       "')");
	  }

	for (int i = 0; event.objects != null && i < event.objects.size(); i++)
	  {
	    statement.addBatch("insert into invids (invid, event_id) values('" +
			       event.objects.elementAt(i).toString() + "'," + 
			       primaryKey + ")");
	  }

	// if we had successfully logged our entire transaction,
	// record a mapping in the transactions table from each
	// invid in the transaction (which are recorded in the
	// starttransaction line) to the transaction id
	//
	// if we get a transactionID mismatch we won't record anything
	//
	// we reset the transactionID and transInvids vector
	// pointer in any event
		
	if (event.eventClassToken.equals("finishtransaction"))
	  {
	    if (transactionID != null && transactionID.equals(event.transactionID))
	      {
		for (int i = 0; i < transInvids.size(); i++)
		  {
		    Invid z = (Invid) transInvids.elementAt(i);
			    
		    statement.addBatch("insert into transactions (invid, trans_id) values('" +
				       z.toString() + "','" + transactionID + "')");
		  }
	      }
		    
	    transactionID = null;

	    if (transInvids != null)
	      {
		transInvids.removeAllElements();
	      }

	    transInvids = null;
	  }

	statement.executeBatch();
	statement.clearParameters();
      }
    catch (SQLException ex)
      {
	Ganymede.debug("SQLException in spinLog: " + ex.getMessage());
	Ganymede.debug("** SQLState: " + ex.getSQLState());
	Ganymede.debug("** SQL Error Code: " + ex.getErrorCode());
	Ganymede.debug(Ganymede.stackTrace(ex));
	Ganymede.debug("Event was " + event.toString() );
	return;
      }
  }

  /**
   * <P>This method is used to scan the persistent log storage for log events that match
   * invid and that have occurred since sinceTime.</P>
   *
   * @param invid If not null, retrieveHistory() will only return events involving
   * this object invid.
   *
   * @param sinceTime if not null, retrieveHistory() will only return events
   * occuring on or after the time specified in this Date object.
   *
   * @param keyOnAdmin if true, rather than returning a string containing events
   * that involved &lt;invid&gt;, retrieveHistory() will return a string containing events
   * performed on behalf of the administrator with invid &lt;invid&gt;.
   *
   * @param fullTransactions if true, the buffer returned will include all events in any
   * transactions that involve the given invid.  if false, only those events in a transaction
   * directly affecting the given invid will be returned.
   *
   * @return A human-readable multiline string containing a list of history events
   */

  public StringBuffer retrieveHistory(Invid invid, java.util.Date sinceTime, boolean keyOnAdmin,
				      boolean fullTransactions)
  {
    if (con == null)
      {
	throw new IllegalArgumentException("no connection to postgres database");
      }

    StringBuffer buffer = new StringBuffer();
    DBLogEvent event = null;
    String line;

    java.util.Date transDate = null;
    String prevTransID = null;
    String transAdmin = null;

    String dateString;
    long timeCode;

    ResultSet rs = null;

    /* -- */

    try
      {
	if (keyOnAdmin)
	  {
	    rs = queryEventsByAdmin(invid, sinceTime);
	  }
	else if (fullTransactions)
	  {
	    rs = queryEventsByTransactions(invid, sinceTime);
	  }
	else
	  {
	    rs = queryEvents(invid, sinceTime);
	  }

	while (rs.next())
	  {
	    long time = rs.getLong(1);
	    String token = rs.getString(2);
	    String adminName = rs.getString(3);
	    String text = rs.getString(4);
	    String transactionID = rs.getString(5);

	    if (prevTransID != null && !prevTransID.equals(transactionID))
	      {
		buffer.append("---------- End Transaction " + transDate.toString() + ": " + transAdmin + 
			      " ----------\n\n");

		transAdmin = null;
		transDate = null;
		prevTransID = null;
	      }

	    if (token.equals("starttransaction"))
	      {
		transDate = new java.util.Date(time);

		String tmp2 = "---------- Transaction " + transDate.toString() + ": " + adminName + 
		  " ----------\n";

		// remember our admin name and transaction id so we
		// can put out a notice about closing the previous
		// transaction when we see it

		transAdmin = adminName;
		prevTransID = transactionID;
		
		buffer.append(tmp2);
	      }
	    else if (token.equals("finishtransaction"))
	      {
		String tmp2 = "---------- End Transaction " + new java.util.Date(time).toString() + ": " + adminName + 
		  " ----------\n\n";

		prevTransID = null;
		transAdmin = null;
		transDate = null;
		
		buffer.append(tmp2);
	      }
	    else
	      {
		String tmp = token + "\n" + WordWrap.wrap(text, 78, "\t") + "\n";
		
		buffer.append(tmp);
	      }
	  }
      }
    catch (SQLException ex)
      {
	System.err.println("********************");
	System.err.println("SQLException in retrieveHistory: " + ex.getMessage());
	System.err.println("** SQLState: " + ex.getSQLState());
	System.err.println("** SQL Error Code: " + ex.getErrorCode());
	ex.printStackTrace();
	System.err.println("********************");
	return buffer;
      }

    return buffer;
  }

  public ResultSet queryEvents(Invid invid, java.util.Date date) throws SQLException
  {
    Statement stmt = con.createStatement();

    if (date == null)
      {
	return stmt.executeQuery("SELECT e.javatime, e.classtoken, e.admin_name, e.text, e.trans_id from event e, invids v " +
				 "WHERE e.event_id = v.event_id AND v.invid = '" + invid.toString() +
				 "' ORDER BY e.event_id");
      }
    else
      {
	return stmt.executeQuery("SELECT e.javatime, e.classtoken, e.admin_name, e.text, e.trans_id from event e, invids v " +
				 "WHERE e.event_id = v.event_id AND v.invid = '" + invid.toString() +
				 "' AND e.javatime > " + date.getTime() + 
				 " ORDER BY e.event_id");
      }
  }

  public ResultSet queryEventsByAdmin(Invid invid, java.util.Date date) throws SQLException
  {
    Statement stmt = con.createStatement();

    if (date == null)
      {
	return stmt.executeQuery("SELECT javatime, classtoken, admin_name, text, trans_id from event " +
				 "WHERE admin_invid = '" + invid.toString() + "' ORDER BY event_id");
      }
    else
      {
	return stmt.executeQuery("SELECT javatime, classtoken, admin_name, text, trans_id from event " +
				 "WHERE admin_invid = '" + invid.toString() +
				 "' AND javatime > " + date.getTime() + 
				 " ORDER BY event_id");
      }
  }

  public ResultSet queryEventsByTransactions(Invid invid, java.util.Date date) throws SQLException
  {
    Statement stmt = con.createStatement();

    if (date == null)
      {
	return stmt.executeQuery("SELECT e.javatime, e.classtoken, e.admin_name, e.text, e.trans_id from event e, transactions t " +
				 "WHERE e.trans_id = t.trans_id AND t.invid =  '" + invid.toString() + "' ORDER BY e.event_id");
      }
    else
      {
	return stmt.executeQuery("SELECT e.javatime, e.classtoken, e.admin_name, e.text, e.trans_id from event e, transactions t " +
				 "WHERE e.trans_id = t.trans_id AND t.invid =  '" + invid.toString() + 
				 "' AND e.javatime > " + date.getTime() + " ORDER BY e.event_id");
      }
  }

  /**
   * <p>This method shuts down this controller, freeing up any resources used by this
   * controller.</p>
   */

  public void close()
  {
  }
}
