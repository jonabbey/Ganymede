/*

   GanymedeWarningTask.java

   This class goes through all objects in the database and sends
   out any warnings for objects that are going to expire within
   a whole number of weeks in the future.
   
   Created: 4 February 1998
   Release: $Name:  $
   Version: $Revision: 1.9 $
   Last Mod Date: $Date: 2000/01/27 06:03:24 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                             GanymedeWarningTask

------------------------------------------------------------------------------*/

/**
 *
 * This class goes through all objects in the database and sends
 * out any warnings for objects that are going to expire within
 * a whole number of weeks in the future.
 *
 */

public class GanymedeWarningTask implements Runnable {

  public GanymedeWarningTask()
  {
  }
  
  /**
   *
   * Just Do It (tm)
   *
   * @see java.lang.Runnable
   *
   */

  public void run()
  {
    GanymedeSession mySession = null;
    boolean started = false;
    boolean finished = false;

    /* -- */

    Ganymede.debug("Warning Task: Starting");

    String semaphoreCondition;

    try
      {
	semaphoreCondition = GanymedeServer.lSemaphore.increment(0);
	
	if (semaphoreCondition != null)
	  {
	    Ganymede.debug("Deferring warning task - semaphore disabled: " + semaphoreCondition);
	    return;
	  }
      }
    catch (InterruptedException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException(ex.getMessage());
      }

    try
      {
	try
	  {
	    mySession = new GanymedeSession("warning");
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("Warning Task: Couldn't establish session");
	    return;
	  }
	
	started = true;

	Query q;
	Vector results;
	Result result;
	Invid invid;
	Date currentTime = new Date();
	Calendar cal = Calendar.getInstance();
	Calendar cal2;
	Date loTime, hiTime;
	DBObjectBase base;
	Enumeration baseEnum, enum;
	QueryNode expireNode, removeNode;
	DBLogEvent event;
	String title;
	StringBuffer tempString = new StringBuffer();
	Vector objects = new Vector();
	DBObject obj;
	Date actionDate;

	// --

	cal.setTime(currentTime);

	// do the weekly warnings

	for (int i = 0; i < 3; i++)
	  {
	    cal.add(Calendar.DATE, 7);
	    loTime = cal.getTime();

	    cal2 = Calendar.getInstance();
	    cal2.setTime(loTime);
	    cal2.add(Calendar.DATE, 1);
	    hiTime = cal2.getTime();

	    expireNode = new QueryAndNode(new QueryDataNode(SchemaConstants.ExpirationField,
							    QueryDataNode.GREATEQ,
							    loTime),
					  new QueryDataNode(SchemaConstants.ExpirationField,
							    QueryDataNode.LESSEQ,
							    hiTime));

	    baseEnum = Ganymede.db.objectBases.elements();

	    while (baseEnum.hasMoreElements())
	      {
		base = (DBObjectBase) baseEnum.nextElement();

		if (base.isEmbedded())
		  {
		    continue;
		  }
	    
		q = new Query(base.getTypeID(), expireNode, false);

		results = mySession.internalQuery(q);

		enum = results.elements();

		while (enum.hasMoreElements())
		  {
		    result = (Result) enum.nextElement();

		    invid = result.getInvid();

		    tempString.setLength(0);
		    tempString.append(base.getName());
		    tempString.append(" ");
		    tempString.append(mySession.viewObjectLabel(invid));
		    tempString.append(" expires in " + (i+1));

		    if (i == 0)
		      {
			tempString.append(" week");
		      }
		    else
		      {
			tempString.append(" weeks");
		      }

		    title = tempString.toString();

		    obj = mySession.session.viewDBObject(invid);

		    actionDate = (Date) obj.getFieldValue(SchemaConstants.ExpirationField);

		    tempString.append("\n\nExpiration scheduled to take place on or after " + actionDate.toString());

		    tempString.append("\n\nObject expiration typically means that the object in question is ");
		    tempString.append("to be made unusable, but the record will not be immediately removed from ");
		    tempString.append("the database.  The object will typically be scheduled for permanent removal ");
		    tempString.append("at a later time.\n\n");
		    tempString.append("Depending on the type of object, the object may be made usable again by ");
		    tempString.append("a Ganymede administrator taking the appropriate action prior to the object's ");
		    tempString.append("formal removal.\n\n");
		    tempString.append("As with all Ganymede messages, if you have questions about this action, please ");
		    tempString.append("contact your Ganymede management team.");
		    
		    objects.setSize(0);
		    objects.addElement(invid);

		    sendMail("expirationwarn", title, tempString.toString(), objects);
		  }
	      }

	    // now the removals

	    removeNode = new QueryAndNode(new QueryDataNode(SchemaConstants.RemovalField,
							    QueryDataNode.GREATEQ,
							    loTime),
					  new QueryDataNode(SchemaConstants.RemovalField,
							    QueryDataNode.LESSEQ,
							    hiTime));

	    baseEnum = Ganymede.db.objectBases.elements();

	    while (baseEnum.hasMoreElements())
	      {
		base = (DBObjectBase) baseEnum.nextElement();

		if (base.isEmbedded())
		  {
		    continue;
		  }
	    
		q = new Query(base.getTypeID(), removeNode, false);

		results = mySession.internalQuery(q);

		enum = results.elements();

		while (enum.hasMoreElements())
		  {
		    result = (Result) enum.nextElement();

		    invid = result.getInvid();

		    tempString.setLength(0);
		    tempString.append(base.getName());
		    tempString.append(" ");
		    tempString.append(mySession.viewObjectLabel(invid));
		    tempString.append(" will be removed in " + (i+1));

		    if (i == 0)
		      {
			tempString.append(" week");
		      }
		    else
		      {
			tempString.append(" weeks");
		      }

		    title = tempString.toString();

		    obj = mySession.session.viewDBObject(invid);

		    actionDate = (Date) obj.getFieldValue(SchemaConstants.RemovalField);

		    tempString.append("\n\nRemoval scheduled to take place on or after " + actionDate.toString()); 
		    
		    objects.setSize(0);
		    objects.addElement(invid);

		    sendMail("removalwarn", title, tempString.toString(), objects);
		  }
	      }
	  }

	// now the next-day warnings

	loTime = currentTime;

	cal2 = Calendar.getInstance();
	cal2.setTime(loTime);
	cal2.add(Calendar.DATE, 1);
	hiTime = cal2.getTime();
	
	expireNode = new QueryAndNode(new QueryDataNode(SchemaConstants.ExpirationField,
							QueryDataNode.GREATEQ,
							loTime),
				      new QueryDataNode(SchemaConstants.ExpirationField,
							QueryDataNode.LESSEQ,
							hiTime));
	
	baseEnum = Ganymede.db.objectBases.elements();
	
	while (baseEnum.hasMoreElements())
	  {
	    base = (DBObjectBase) baseEnum.nextElement();
	    
	    if (base.isEmbedded())
	      {
		continue;
	      }
	    
	    q = new Query(base.getTypeID(), expireNode, false);
	    
	    results = mySession.internalQuery(q);

	    enum = results.elements();

	    while (enum.hasMoreElements())
	      {
		result = (Result) enum.nextElement();
		
		invid = result.getInvid();
		
		tempString.setLength(0);
		tempString.append("** ");
		tempString.append(base.getName());
		tempString.append(" ");
		tempString.append(mySession.viewObjectLabel(invid));
		tempString.append(" expires within 24 hours **");

		title = tempString.toString();

		obj = mySession.session.viewDBObject(invid);

		actionDate = (Date) obj.getFieldValue(SchemaConstants.ExpirationField);

		tempString.append("\n\nExpiration scheduled to take place on or after " + actionDate.toString());

		tempString.append("\n\nObject expiration typically means that the object in question is ");
		tempString.append("to be made unusable, but the record will not be immediately removed from ");
		tempString.append("the database.  The object will typically be scheduled for permanent removal ");
		tempString.append("at a later time.\n\n");
		tempString.append("Depending on the type of object, the object may be made usable again by ");
		tempString.append("a Ganymede administrator taking the appropriate action prior to the object's ");
		tempString.append("formal removal.\n\n");
		tempString.append("As with all Ganymede messages, if you have questions about this action, please ");
		tempString.append("contact your Ganymede management team.");
		    
		objects.setSize(0);
		objects.addElement(invid);
		
		sendMail("expirationwarn", title, tempString.toString(), objects);
	      }
	  }

	// now the removals

	removeNode = new QueryAndNode(new QueryDataNode(SchemaConstants.RemovalField,
							QueryDataNode.GREATEQ,
							loTime),
				      new QueryDataNode(SchemaConstants.RemovalField,
							QueryDataNode.LESSEQ,
							hiTime));
	
	baseEnum = Ganymede.db.objectBases.elements();
	
	while (baseEnum.hasMoreElements())
	  {
	    base = (DBObjectBase) baseEnum.nextElement();
	    
	    if (base.isEmbedded())
	      {
		continue;
	      }
	    
	    q = new Query(base.getTypeID(), removeNode, false);

	    results = mySession.internalQuery(q);

	    enum = results.elements();

	    while (enum.hasMoreElements())
	      {
		result = (Result) enum.nextElement();

		invid = result.getInvid();

		tempString.setLength(0);
		tempString.append("** ");
		tempString.append(base.getName());
		tempString.append(" ");
		tempString.append(mySession.viewObjectLabel(invid));
		tempString.append(" will be removed within the next 24 hours! **");

		title = tempString.toString();

		obj = mySession.session.viewDBObject(invid);

		actionDate = (Date) obj.getFieldValue(SchemaConstants.RemovalField);

		tempString.append("\n\nRemoval scheduled to take place on or after " + actionDate.toString()); 
		    
		objects.setSize(0);
		objects.addElement(invid);

		sendMail("removalwarn", title, tempString.toString(), objects);
	      }
	  }
	
	mySession.logout();

	Ganymede.debug("Warning Task: Completed");

	finished = true;
      }
    finally
      {
	if (started && !finished)
	  {
	    // we'll get here if this task's thread is stopped early

	    Ganymede.debug("Warning Task: Forced to terminate early, aborting.");
	    mySession.logout();
	  }
      }
  }

  private void sendMail(String type, String title, String description, Vector invids)
  {
    DBLogEvent event;

    /* -- */

    // create a log event
		    
    event = new DBLogEvent(type, description, null, null, invids, null);

    // we've already put the description in the event, don't need
    // to provide a separate description string to mailNotify
    
    Ganymede.log.mailNotify(title, null, event, true, null);

    // Ganymede.debug(description);
  }

}
