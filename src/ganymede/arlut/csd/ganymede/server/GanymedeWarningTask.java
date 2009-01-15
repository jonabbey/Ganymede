/*

   GanymedeWarningTask.java

   This class goes through all objects in the database and sends
   out any warnings for objects that are going to expire within
   a whole number of weeks in the future.
   
   Created: 4 February 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

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

package arlut.csd.ganymede.server;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryAndNode;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryNode;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                             GanymedeWarningTask

------------------------------------------------------------------------------*/

/**
 * This is a Ganymede server task, for use with the {@link
 * arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler}.
 * 
 * The standard GanymedeWarningTask class scans through all objects in
 * the database and mails out warnings for those objects that are
 * going to expire on this day one, two, or three weeks in the future,
 * as well as those objects that are going to expire within the
 * following 24 hours.  The email messages sent are based on the
 * server's Object Events configuration settings, and will also be
 * sent to the list of email addresses returned by the {@link
 * arlut.csd.ganymede.server.DBEditObject#getEmailTargets(arlut.csd.ganymede.server.DBObject)
 * getEmailTargets()} customization method in each object's {@link
 * arlut.csd.ganymede.server.DBEditObject DBEditObject} customization
 * class, if any such is defined.
 *
 * GanymedeWarningTask must not be run more than once a day by the
 * GanymedeScheduler, or else users and admins may receive redundant
 * warnings.
 *
 * The GanymedeWarningTask is paired with the standard {@link
 * arlut.csd.ganymede.server.GanymedeExpirationTask
 * GanymedeExpirationTask} task, which handles the actual expiration
 * and removal of database objects.
 */

public class GanymedeWarningTask implements Runnable {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.GanymedeWarningTask");

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
    String semaphoreCondition;
    Thread currentThread = java.lang.Thread.currentThread();

    /* -- */

    // we have to increment the GanymedeServer loginSemaphore, because
    // we are using a non-semaphored local GanymedeSession

    semaphoreCondition = GanymedeServer.lSemaphore.increment();

    if (semaphoreCondition != null)
      {
        Ganymede.debug("Deferring warning task - semaphore disabled: " + semaphoreCondition);
        return;
      }

    try
      {
        Ganymede.debug("Warning Task: Starting");

	try
	  {
	    mySession = new GanymedeSession("warning");	// supergash
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
	Enumeration baseEnum, en;
	QueryNode expireNode, removeNode;
	String title;
	StringBuffer tempString = new StringBuffer();
	Vector objects = new Vector();
	DBObject obj;
        DBEditObject objectHook;

	// --

	cal.setTime(currentTime);

	// do the weekly warnings

	for (int i = 0; i < 3; i++)
	  {
	    if (currentThread.isInterrupted())
	      {
		throw new InterruptedException("scheduler ordering shutdown");
	      }

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

                objectHook = base.getObjectHook();
	    
		if (currentThread.isInterrupted())
		  {
		    throw new InterruptedException("scheduler ordering shutdown");
		  }
	    
		q = new Query(base.getTypeID(), expireNode, false);

		results = mySession.internalQuery(q);

		en = results.elements();

		while (en.hasMoreElements())
		  {
		    if (currentThread.isInterrupted())
		      {
			throw new InterruptedException("scheduler ordering shutdown");
		      }

		    result = (Result) en.nextElement();

		    invid = result.getInvid();

		    if (i == 0)
		      {
			// "{0} {1} expires in one week"
			title = ts.l("run.expire_one_week_email_subj",
				     base.getName(), mySession.viewObjectLabel(invid));
		      }
		    else
		      {
			// "{0} {1} expires in {2,num,#} weeks"
			title = ts.l("run.expire_multi_week_email_subj",
				     base.getName(), mySession.viewObjectLabel(invid), Integer.valueOf(i+1));
		      }

		    obj = mySession.session.viewDBObject(invid);

                    if (!objectHook.reactToExpirationWarning(obj, 7 * i))
                      {
                        tempString.setLength(0);
                        tempString.append(title);

                        tempString.append(getExpirationWarningMesg(obj));
		    
                        objects.setSize(0);
                        objects.addElement(invid);

                        sendMail("expirationwarn", title, tempString.toString(), objects);
                      }
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
		if (currentThread.isInterrupted())
		  {
		    throw new InterruptedException("scheduler ordering shutdown");
		  }

		base = (DBObjectBase) baseEnum.nextElement();

		if (base.isEmbedded())
		  {
		    continue;
		  }

                objectHook = base.getObjectHook();
	    
		q = new Query(base.getTypeID(), removeNode, false);

		results = mySession.internalQuery(q);

		en = results.elements();

		while (en.hasMoreElements())
		  {
		    if (currentThread.isInterrupted())
		      {
			throw new InterruptedException("scheduler ordering shutdown");
		      }

		    result = (Result) en.nextElement();

		    invid = result.getInvid();

		    if (i == 0)
		      {
			// "{0} {1} will be removed in one week"
			title = ts.l("run.remove_one_week_email_subj",
				     base.getName(), mySession.viewObjectLabel(invid));
		      }
		    else
		      {
			// "{0} {1} will be removed in {2,num,#} weeks"
			title = ts.l("run.remove_multi_week_email_subj",
				     base.getName(), mySession.viewObjectLabel(invid), Integer.valueOf(i+1));
		      }

                    obj = mySession.session.viewDBObject(invid);

                    if (!objectHook.reactToRemovalWarning(obj, 7 * i))
                      {
                        Date actionDate = (Date) obj.getFieldValueLocal(SchemaConstants.RemovalField);

                        tempString.setLength(0);
                        tempString.append(title);
                        // "\n\nRemoval scheduled to take place on or after {0,date}."
                        tempString.append(ts.l("run.removal_scheduled", actionDate));
		    
                        objects.setSize(0);
                        objects.addElement(invid);

                        sendMail("removalwarn", title, tempString.toString(), objects);
                      }
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
	    if (currentThread.isInterrupted())
	      {
		throw new InterruptedException("scheduler ordering shutdown");
	      }

	    base = (DBObjectBase) baseEnum.nextElement();
	    
	    if (base.isEmbedded())
	      {
		continue;
	      }
	    
	    q = new Query(base.getTypeID(), expireNode, false);
	    
	    results = mySession.internalQuery(q);

	    en = results.elements();

	    while (en.hasMoreElements())
	      {
		if (currentThread.isInterrupted())
		  {
		    throw new InterruptedException("scheduler ordering shutdown");
		  }
		
		result = (Result) en.nextElement();
		
		invid = result.getInvid();

		// "** {0} {1} expires within 24 hours **"
		title = ts.l("run.expire_real_soon_now", base.getName(), mySession.viewObjectLabel(invid));

		tempString.setLength(0);
		tempString.append(title);
		
		obj = mySession.session.viewDBObject(invid);

		tempString.append(getExpirationWarningMesg(obj));
		    
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
	    if (currentThread.isInterrupted())
	      {
		throw new InterruptedException("scheduler ordering shutdown");
	      }

	    base = (DBObjectBase) baseEnum.nextElement();
	    
	    if (base.isEmbedded())
	      {
		continue;
	      }
	    
	    q = new Query(base.getTypeID(), removeNode, false);

	    results = mySession.internalQuery(q);

	    en = results.elements();

	    while (en.hasMoreElements())
	      {
		if (currentThread.isInterrupted())
		  {
		    throw new InterruptedException("scheduler ordering shutdown");
		  }

		result = (Result) en.nextElement();

		invid = result.getInvid();

		// "** {0} {1} will be removed within the next 24 hours! **"
		title = ts.l("run.remove_real_soon_now", base.getName(), mySession.viewObjectLabel(invid));

		obj = mySession.session.viewDBObject(invid);
		Date actionDate = (Date) obj.getFieldValueLocal(SchemaConstants.RemovalField);

		tempString.setLength(0);
		tempString.append(title);

		// "\n\nRemoval scheduled to take place on or after {0,date}."
		tempString.append(ts.l("run.removal_scheduled", actionDate));
		    
		objects.setSize(0);
		objects.addElement(invid);

		sendMail("removalwarn", title, tempString.toString(), objects);
	      }
	  }
	
	mySession.logout();

	finished = true;
      }
    catch (InterruptedException ex)
      {
	Ganymede.debug("Warning task aborted due to task stop command.");
      }
    finally
      {
	GanymedeServer.lSemaphore.decrement();

	if (started && !finished)
	  {
	    // we'll get here if this task's thread is stopped early

	    Ganymede.debug("Warning Task: Forced to terminate early, aborting.");
	    mySession.logout();
	  }
	else
	  {
	    Ganymede.debug("Warning Task: Completed");
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

  public String getExpirationWarningMesg(DBObject object)
  {
    Date actionDate = (Date) object.getFieldValueLocal(SchemaConstants.ExpirationField);
    String typeName = object.getTypeName();
    String label = object.getLabel();

    /*
      \n\
      \n\
      {0} {1} is scheduled to expire after {2,date}.  In order to prevent this {0} from \
      expiring, this object''s Expiration Date field must be cleared or changed in Ganymede.\n \
      \n\
      Object expiration typically means that the object in question is to be rendered unusable, \
      but the object will not be immediately removed from the Ganymede database.  Objects that \
      have expired will typically be scheduled for removal from the \
      Ganymede database after a delay period.\n\
      \n\
      Depending on the type of object, the object may be made usable again by a Ganymede administrator \
      taking the appropriate action prior to the object''s formal removal.\n\
      \n\
      As with all Ganymede messages, if you have questions about this action, please contact \
      your Ganymede management team.
    */

    return ts.l("getExpirationWarningMesg.message", typeName, label, actionDate);
  }
}
