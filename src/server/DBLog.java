/*

   DBLog.java

   This class manages recording events in the system log and
   generating reports from the system log based on specific criteria.

   Most of the methods in this class must be synchronized, both to
   keep the logfile itself orderly, and to allow the various
   log-processing methods iin DBLogEvent to re-use the 'multibuffer'
   StringBuffer.
   
   Created: 31 October 1997
   Release: $Name:  $
   Version: $Revision: 1.16 $
   Last Mod Date: $Date: 1999/04/16 22:52:44 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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

import java.net.*;
import java.io.*;
import java.util.*;

import Qsmtp;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBLog.java

------------------------------------------------------------------------------*/

/**
 * <p>This class manages recording events in the system log and generating
 * reports from the system log based on specific criteria.</p>
 *
 * <p>Most of the methods in this class must be synchronized, both to keep the
 * logfile itself orderly, and to allow the various log-processing methods
 * in DBLogEvent to re-use the 'multibuffer' StringBuffer.</p>
 */

public class DBLog {

  static final boolean debug = false;

  // -- 

  String logFileName = null;
  File logFile = null;
  FileOutputStream logStream = null;
  PrintWriter logWriter = null;

  /**
   *
   * The signature to be appended to any outgoing mail
   *
   */

  String signature = null;

  /**
   *
   * We use this Date object to track the time of the last log event
   * recorded.
   *
   */

  Date currentTime = new Date();

  /**
   *
   * This variable tracks whether the log file has been closed, or whether
   * it is open for append.  If true, the log file may not be written to.
   *
   */

  boolean closed = false;

  /**
   *
   * We keep a table of the system event codes to speed the logging process.
   * This hash maps system event classTokens to instances of systemEventType.
   *
   * @see arlut.csd.ganymede.systemEventType
   *
   */

  Hashtable sysEventCodes = new Hashtable();

  /**
   *
   * This field keeps track of when we last updated the sysEventCodes
   * hash, so that we can check against the timestamp held in the
   * System Event DBObjectBase to see whether we need to refresh the
   * sysEventCodes hash.
   * 
   */

  Date sysEventCodesTimeStamp = null;

  /**
   *
   * We keep a table of the system event codes to speed the logging process.
   * This hash maps object event classTokens to instances of objectEventType.
   *
   * @see arlut.csd.ganymede.objectEventType
   *
   */

  Hashtable objEventCodes = new Hashtable();

  /**
   *
   * This field keeps track of when we last updated the objEventCodes
   * hash, so that we can check against the timestamp held in the
   * Object Event DBObjectBase to see whether we need to refresh the
   * objEventCodes hash.
   * 
   */

  Date objEventCodesTimeStamp = null;

  /**
   *
   * Our mail relay.
   *
   */

  Qsmtp mailer;

  /**
   *
   * Reusable StringBuffer for transaction processing.
   * 
   */

  SharedStringBuffer multibuffer = new SharedStringBuffer();

  /**
   *
   * GanymedeSession reference to allow the log code to do searching
   * of the database, etc.
   *
   */

  GanymedeSession session = null;

  /* -- */

  public DBLog(String filename, GanymedeSession session) throws IOException
  {
    // get the signature to append to mail messages

    loadSignature();

    this.session = session;

    logFileName = filename;
    logStream = new FileOutputStream(logFileName, true); // append
    logWriter = new PrintWriter(logStream, true); // auto-flush on newline

    logWriter.println();	// emit newline to terminate any incomplete entry

    // now we need to initialize our hash of DBObjects so that we can do
    // speedy lookup of event codes without having to synchronize on
    // the main objectBase hashes during logging

    updateSysEventCodeHash();

    updateObjEventCodeHash();

    // initalize our mailer

    mailer = new Qsmtp(Ganymede.mailHostProperty);
  }

  /**
   *
   * This method closes out the log file.
   *
   */

  synchronized void close() throws IOException
  {
    logWriter.close();
    logStream.close();
    closed = true;

    if (debug)
      {
	System.err.println("DBLog:" + logFileName + " closed.");
      }
  }

  /**
   * This method is used to handle an email notification event, where
   * the mail title should reflect detailed information about the
   * event, and extra descriptive information is to be sent out.
   *
   * mailNotify() will send the message to the owners of any objects
   * referenced by event if mailToOwners is true.
   *
   * description and/or title may be null, in which case the proper
   * strings will be extracted from the event's database record
   *
   * @param event A single event to be logged, with its own timestamp.
   * @param mailToOwners If true, this event's mail will go to the owners
   * of any objects referenced by event.
   *  
   */

  public synchronized void mailNotify(String title, String description,
				      DBLogEvent event, boolean mailToOwners)
  {
    systemEventType type;

    /* -- */

    if (closed)
      {
	throw new RuntimeException("log already closed.");
      }

    if (debug)
      {
	System.err.println("DBLopg.mailNotify(): Writing log event " + event.eventClassToken);
      }

    updateSysEventCodeHash();

    // we calculate the list of email addresses that we want to send this
    // event's notifcation to.. we do it before the writeEntry() call so
    // that the log will record who the mail was sent to.

    if (mailToOwners)
      {
	calculateMailTargets(event);
      }

    // log the event to the log file.

    currentTime.setTime(System.currentTimeMillis());
    event.writeEntry(logWriter, currentTime, null);
    
    type = (systemEventType) sysEventCodes.get(event.eventClassToken);

    if (type == null)
      {
	Ganymede.debug("Error in DBLog.mailNotify(): unrecognized eventClassToken: " + 
		       event.eventClassToken);
	return;
      }

    if (!type.mail)
      {
	Ganymede.debug("Logic error in DBLog.mailNotify():  eventClassToken not configured for mail delivery: " + 
		       event.eventClassToken);
	return;
      }
    
    if (debug)
      {
	System.err.println("Attempting to email log event " + event.eventClassToken);
      }

    // prepare our message, word wrap it

    String message = type.description + "\n\n" + event.description + "\n\n";

    if (description != null)
      {
	message = message + description + "\n\n";
      }
    
    message = arlut.csd.Util.WordWrap.wrap(message, 78);

    // the signature is pre-wrapped
	
    message = message + "\n\n" + signature;

    // get our list of recipients from the event's enumerated list of recipients
    // and the event code's address list.

    Vector emailList = VectorUtils.union(event.notifyVect, type.addressVect);

    // and now..
    
    try
      {
	// bombs away!

	mailer.sendmsg(Ganymede.returnaddrProperty,
		       emailList,
		       "Ganymede: " + ((title == null) ? type.name : title),
		       message);
      }
    catch (UnknownHostException ex)
      {
	throw new RuntimeException("Couldn't figure address " + ex);
      }
    catch (IOException ex)
      {
	throw new RuntimeException("IO problem " + ex);
      }

    if (debug)
      {
	System.err.println("Completed emailing log event " + event.eventClassToken);
      }
  }

  /**
   *
   * This method is used to log an event such as server shutdown/restart,
   * user log-in, persona change, etc.  Basically any thing not associated
   * with a transaction.
   *
   * @param event A single event to be logged, with its own timestamp.
   *
   */

  public synchronized void logSystemEvent(DBLogEvent event)
  {
    systemEventType type;

    /* -- */

    if (closed)
      {
	throw new RuntimeException("log already closed.");
      }

    if (debug)
      {
	System.err.println("DBLog.logSystemEvent(): Writing log event " + event.eventClassToken);
      }

    updateSysEventCodeHash();

    currentTime.setTime(System.currentTimeMillis());

    // We haven't augmented event with the mail targets here.. the log
    // won't record who gets notified for system events.  This is OK.

    event.writeEntry(logWriter, currentTime, null);
    
    type = (systemEventType) sysEventCodes.get(event.eventClassToken);

    if (type == null)
      {
	return;
      }

    if (type.mail)
      {
	if (debug)
	  {
	    System.err.println("Attempting to email log event " + event.eventClassToken);
	  }

	// prepare our message, word wrap it

	String message = type.description + "\n\n" + event.description + "\n\n";

	message = arlut.csd.Util.WordWrap.wrap(message, 78);
	
	message = message + "\n\n" + signature;

	// get our list of recipients

	Vector emailList = new Vector();

	if (event.notifyVect != null)
	  {
	    for (int i = 0; i < event.notifyVect.size(); i++)
	      {
		emailList.addElement(event.notifyVect.elementAt(i));
	      }
	  }

	if (type.addressVect != null)
	  {
	    for (int i = 0; i < type.addressVect.size(); i++)
	      {
		emailList.addElement(type.addressVect.elementAt(i));
	      }
	  }

	// and now..

	try
	  {
	    // bombs away!

	    mailer.sendmsg(Ganymede.returnaddrProperty,
			   emailList,
			   "Ganymede: " + type.name,
			   message);
	  }
	catch (UnknownHostException ex)
	  {
	    throw new RuntimeException("Couldn't figure address " + ex);
	  }
	catch (IOException ex)
	  {
	    throw new RuntimeException("IO problem " + ex);
	  }

	if (debug)
	  {
	    System.err.println("Completed emailing log event " + event.eventClassToken);
	  }
      }
  }

  /**
   *
   * This method is used to log all events associated with a transaction.
   *
   * @param logEvents a Vector of DBLogEvent objects.
   *
   */

  public synchronized void logTransaction(Vector logEvents, String adminName, 
					  Invid admin, DBEditSet transaction)
  {
    String transactionID;
    String transdescrip = transaction.description;
    boolean found;
    DBLogEvent event;
    Enumeration enum;
    Hashtable mailOuts = new Hashtable();
    Object ref;

    /* -- */
    
    multibuffer.setLength(0);

    if (closed)
      {
	throw new RuntimeException("log already closed.");
      }

    if (debug)
      {	
	System.err.println("DBLog.logTransaction(): Logging transaction for  " + adminName);
      }

    updateSysEventCodeHash();
    updateObjEventCodeHash();

    currentTime.setTime(System.currentTimeMillis());
    transactionID = adminName + ":" + currentTime.getTime();

    Vector objects = new Vector();

    // get a list of all objects affected by this transaction
    
    for (int i = 0; i < logEvents.size(); i++)
      {
	ref = logEvents.elementAt(i);

	if (ref instanceof DBLogEvent)
	  {
	    event = (DBLogEvent) ref;

	    if (event.objects != null)
	      {
		for (int j = 0; j < event.objects.size(); j++)
		  {
		    Invid inv = (Invid) event.objects.elementAt(j);
		    
		    if (!objects.contains(inv))
		      {
			objects.addElement(inv);
		      }
		  }
	      }
	  }
      }

    // write out a start-of-transaction line to the log

    new DBLogEvent("starttransaction",
		   "Start Transaction: " + transdescrip,
		   admin,
		   adminName,
		   objects,
		   null,
		   multibuffer).writeEntry(logWriter, currentTime, transactionID);

    // write out all the log events in this transaction

    for (int i = 0; i < logEvents.size(); i++)
      {
	event = (DBLogEvent) logEvents.elementAt(i);

	if (debug)
	  {
	    System.err.println("DBLog.logTransaction(): logging event: " + event.description);
	  }

	if (event.subject == null)
	  {
	    // first, if we have a recognizable object-specific event
	    // happening, send out the notification for it.  Note that by
	    // doing so, we are doing this independently of the
	    // transaction notification consolidation done by
	    // appendMailOut()..
	
	    sendObjectMail(event, transdescrip);
	
	    // now, go ahead and add to the mail buffers we are prepping
	    // to describe this whole transaction
	
	    // we are keeping a bunch of buffers, one for each combination
	    // of email addresses that we've encountered.. different
	    // addresses or groups of addresses may get a different subset
	    // of the mail for this transaction, the mailOut logic handles
	    // that.
	
	    // appendMailOut() takes care of calling calculateMailTargets()
	    // on event, which handles calculating who needs to receive
	    // email about this event.
	
	    appendMailOut(event, mailOuts);
	    event.writeEntry(logWriter, currentTime, transactionID);
	  }
	else
	  {
	    // we've got a generic transactional mail event, process it.

	    event.writeEntry(logWriter, currentTime, transactionID);

	    try
	      {
		String message = event.description;

		message = arlut.csd.Util.WordWrap.wrap(message, 78);

		message = message + "\n\n" + signature;

		// bombs away!
		
		mailer.sendmsg(Ganymede.returnaddrProperty,
			       event.notifyVect,
			       "Ganymede: " + event.subject,
			       message);
	      }
	    catch (UnknownHostException ex)
	      {
		throw new RuntimeException("Couldn't figure address " + ex);
	      }
	    catch (IOException ex)
	      {
		throw new RuntimeException("IO problem " + ex);
	      }
	  }
      }

    // write out an end-of-transaction line to the log

    new DBLogEvent("finishtransaction",
		   "Finish Transaction: " + transdescrip,
		   admin,
		   adminName,
		   null,
		   null,
		   multibuffer).writeEntry(logWriter, currentTime, transactionID);

    // now, for each distinct set of recipients, mail them their summary

    enum = mailOuts.elements();

    while (enum.hasMoreElements())
      {
	MailOut mailout = (MailOut) enum.nextElement();
	String description = arlut.csd.Util.WordWrap.wrap(mailout.toString(), 78) + 
	  "\n\n" + signature;

	if (debug)
	  {
	    System.err.println("Sending mail to " + (String) mailout.addresses.elementAt(0));
	  }

	try
	  {
	    mailer.sendmsg(Ganymede.returnaddrProperty,
			   mailout.addresses,
			   "Ganymede: transaction notification",
			   description);
	  }
	catch (UnknownHostException ex)
	  {
	    throw new RuntimeException("Couldn't figure address " + ex);
	  }
	catch (IOException ex)
	  {
	    throw new RuntimeException("IO problem " + ex);
	  }
      }
  }

  /**
   *
   * This method is used to scan the log file for log events that match
   * invid and that have occurred since sinceTime.
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
   * @return A human-readable multiline string containing a list of history events
   *
   */

  public synchronized StringBuffer retrieveHistory(Invid invid, Date sinceTime, boolean keyOnAdmin)
  {
    FileReader reader;

    try
      {
	reader = new FileReader(logFileName);
      }
    catch (FileNotFoundException ex)
      {
	return null;
      }

    BufferedReader in = new BufferedReader(reader);
    StringBuffer buffer = new StringBuffer();
    DBLogEvent event = null;
    String line;
    String transactionID = null;

    boolean afterSinceTime = false;
    String dateString;
    long timeCode;
    Date time;

    /* -- */

    try
      {
	while (true)
	  {
	    line = in.readLine();

	    if (line == null)
	      {
		break;
	      }

	    if (line.trim().equals(""))
	      {
		continue;
	      }

	    // check to see if we've gotten to the requested start point

	    if (sinceTime != null && !afterSinceTime)
	      {
		dateString = line.substring(0, line.indexOf('|'));
    
		try
		  {
		    timeCode = new Long(dateString).longValue();
		  }
		catch (NumberFormatException ex)
		  {
		    throw new IOException("couldn't parse time code");
		  }
		
		time = new Date(timeCode);
		
		if (time.before(sinceTime))
		  {
		    continue;	// don't even bother parsing the rest of the line
		  }
		else
		  {
		    afterSinceTime = true;
		  }
	      }

	    if (event == null)
	      {
		event = new DBLogEvent(line);
	      }
	    else
	      {
		event.loadLine(line);
	      }

	    boolean found = false;

	    if (keyOnAdmin)
	      {
		if (event.admin != null)
		  {
		    found = invid.equals(event.admin);
		  }
	      }
	    else
	      {
		for (int i = 0; i < event.objects.size(); i++)
		  {
		    if (invid.equals((Invid) event.objects.elementAt(i)))
		      {
			found = true;
		      }
		  }
		    
		if (transactionID != null)
		  {
		    if (transactionID.equals(event.transactionID))
		      {
			found = true;
		      }
		  }
	      }

	    if (found)
	      {
		if (event.eventClassToken.equals("starttransaction"))
		  {
		    transactionID = event.transactionID;

		    String tmp2 = "---------- Transaction " + event.time.toString() + ": " + event.adminName + 
		      " ----------\n";
		    
		    buffer.append(tmp2);
		  }
		else if (event.eventClassToken.equals("finishtransaction"))
		  {
		    String tmp2 = "---------- End Transaction " + event.time.toString() + ": " + event.adminName + 
		      " ----------\n\n";
		    
		    buffer.append(tmp2);
		    transactionID = null;
		  }
		else if (transactionID != null)
		  {
		    String tmp = event.eventClassToken + "\n" +
		      WordWrap.wrap(event.description, 78, "\t") + "\n";
		
		    buffer.append(tmp);
		  }
		else
		  {
		    String tmp = event.time.toString() + ": " + event.adminName + "  " + event.eventClassToken +
		      WordWrap.wrap(event.description, 78, "\t") + "\n";

		    buffer.append(tmp);
		  }
	      }
	  }
      }
    catch (IOException ex)
      {
	// eof
      }

    try
      {
	in.close();
	reader.close();
      }
    catch (IOException ex)
      {
	// shrug
      }

    return buffer;
  }

  // -----

  /**
   *
   * This mail sends out the 'auxiliary' type specific log information
   * to designated users, using the object event objects in the Ganymede
   * database.  This mail is sent for a distinct eventcode/object type pair,
   * outside of the context of a transaction.
   *
   */

  private void sendObjectMail(DBLogEvent event, String transdescrip)
  {
    if (event == null || event.objects == null || event.objects.size() != 1)
      {
	return;
      }

    /* - */

    Invid objectInvid = (Invid) event.objects.elementAt(0);
    String key = event.eventClassToken + ":" + objectInvid.getType();
    objectEventType eventRec = (objectEventType) objEventCodes.get(key);

    /* -- */

    if (debug)
      {
	System.err.println("DBLog.sendObjectMail(): processing object Event " + key);
      }

    if (eventRec == null)
      {
	if (debug)
	  {
	    System.err.println("DBLog.sendObjectMail(): couldn't find objectEventType " + key);
	  }

	return;
      }

    // ok.  now we've got an objectEventType, so we will want to send out
    // mail for this event.

    Vector mailList = new Vector();

    if (eventRec.ccToSelf)
      {
	String name;

	name = convertAdminInvidToString(event.admin);

	if (name == null)
	  {
	    name = event.adminName;
	  }

	mailList.addElement(name);
      }

    if (eventRec.ccToOwners)
      {
	mailList = VectorUtils.union(mailList, calculateOwnerAddresses(event.objects));
      }

    mailList = VectorUtils.union(mailList, eventRec.addressVect);

    // okay, we want to tell mailList about what happened.

    String title;

    if (eventRec.name != null)
      {
	title = "Ganymede: " + eventRec.name;
      }
    else
      {
	title = "Ganymede: " + eventRec.token;
      }

    String message = transdescrip + "\n\n" + eventRec.description + "\n\n" + event.description;

    message = arlut.csd.Util.WordWrap.wrap(message, 78);

    message = message + "\n\n" + signature;

    try
      {
	// bombs away!

	mailer.sendmsg(Ganymede.returnaddrProperty,
		       mailList,
		       title,
		       message);
      }
    catch (UnknownHostException ex)
      {
	throw new RuntimeException("Couldn't figure address " + ex);
      }
    catch (IOException ex)
      {
	throw new RuntimeException("IO problem " + ex);
      }
  }

  /**
   *
   * Private helper method to (re)initialize our local hash of system
   * event codes.
   * 
   */

  private void updateSysEventCodeHash()
  {
    Vector eventCodeVector;
    Result entry;

    /* -- */

    // check our time stamp, make sure we aren't unnecessarily
    // refreshing

    if (debug)
      {
	System.err.println("updateSysEventCodeHash(): entering..");
      }

    if (sysEventCodesTimeStamp != null)
      {
	DBObjectBase eventBase = Ganymede.db.getObjectBase(SchemaConstants.EventBase);

	if (!eventBase.getTimeStamp().after(sysEventCodesTimeStamp))
	  {
	    if (debug)
	      {
		System.err.println("updateSysEventCodeHash(): exiting, no work needed..");
	      }

	    return;
	  }
      }
    
    if (debug)
      {
	System.err.println("updateSysEventCodeHash(): updating..");
      }

    sysEventCodes.clear();

    eventCodeVector = session.internalQuery(new Query(SchemaConstants.EventBase));

    if (eventCodeVector == null)
      {
	Ganymede.debug("DBLog.updateSysEventCodeHash(): no event records found in database");
	return;
      }
      
    for (int i = 0; i < eventCodeVector.size(); i++)
      {
	if (debug)
	  {
	    System.err.println("Processing sysEvent object # " + i);
	  }
	
	entry = (Result) eventCodeVector.elementAt(i);
	
	sysEventCodes.put(entry.toString(), new systemEventType(session.session.viewDBObject(entry.getInvid())));
      }

    // remember when we updated our local cache

    if (sysEventCodesTimeStamp == null)
      {
	sysEventCodesTimeStamp = new Date();
      }
    else
      {
	sysEventCodesTimeStamp.setTime(System.currentTimeMillis());
      }

    if (debug)
      {
	System.err.println("updateSysEventCodeHash(): exiting, sysEventCodeHash updated.");
      }
  }

  /**
   *
   * Private helper method to (re)initialize our local hash of object
   * event codes.
   * 
   */

  private void updateObjEventCodeHash()
  {
    Vector eventCodeVector;
    Result entry;
    DBObject objEventobj;
    objectEventType objEventItem;

    /* -- */

    if (debug)
      {
	System.err.println("updateObjEventCodeHash(): entering..");
      }

    // check our time stamp, make sure we aren't unnecessarily
    // refreshing

    if (objEventCodesTimeStamp != null)
      {
	DBObjectBase eventBase = Ganymede.db.getObjectBase(SchemaConstants.ObjectEventBase);

	if (!eventBase.getTimeStamp().after(objEventCodesTimeStamp))
	  {
	    if (debug)
	      {
		System.err.println("updateObjEventCodeHash(): exiting, no work done..");
	      }

	    return;
	  }
      }

    if (debug)
      {
	System.err.println("updateObjEventCodeHash(): updating..");
      }

    objEventCodes.clear();

    eventCodeVector = session.internalQuery(new Query(SchemaConstants.ObjectEventBase));

    if (eventCodeVector == null)
      {
	Ganymede.debug("DBLog.updateObjEventCodeHash(): no event records found in database");
	return;
      }
      
    for (int i = 0; i < eventCodeVector.size(); i++)
      {
	if (debug)
	  {
	    System.err.println("Processing objEvent object # " + i);
	  }

	entry = (Result) eventCodeVector.elementAt(i);

	objEventobj = (DBObject) session.session.viewDBObject(entry.getInvid());
	objEventItem = new objectEventType(objEventobj);
	objEventCodes.put(objEventItem.hashKey, objEventItem);
      }

    // remember when we updated our local cache

    if (objEventCodesTimeStamp == null)
      {
	objEventCodesTimeStamp = new Date();
      }
    else
      {
	objEventCodesTimeStamp.setTime(System.currentTimeMillis());
      }

    if (debug)
      {
	System.err.println("updateObjEventCodeHash(): exiting..");
      }
  }

  /**
   *
   * This method generates a list of additional email addresses that
   * notification for this event should be sent to, based on the
   * event's type and the ownership of objects referenced by this
   * event.<br><br>
   *
   * Note that the email addresses added to this event's mail list
   * will be in addition to any that were previously specified by the
   * code that originally generated the log event.
   *    
   */

  private void calculateMailTargets(DBLogEvent event)
  {
    Vector 
      notifyVect,
      appendVect;

    /* -- */

    // if the DBLogEvent has aleady been processed by us, we don't
    // want to redundantly add entries.

    if (event.augmented)
      {
	return;
      }

    if (debug)
      {
	System.err.println("calculateMailTargets: entering");
      }

    // If the event's notifyVect is null, we'll create a new vector,
    // otherwise, we'll be appending to the existing list.

    notifyVect = event.notifyVect;

    // first we calculate what email addresses we should notify based
    // on the ownership of the objects

    notifyVect = VectorUtils.union(notifyVect, calculateOwnerAddresses(event.objects));

    // now update notifyList

    event.setMailTargets(notifyVect);

    // The DBLogEvent needs to remember that we've already expanded
    // its email list.

    event.augmented = true;
  }

  /**
   *
   * This method takes a vector of Invid's representing objects touched
   * during a transaction, and returns a Vector of email addresses that
   * should be notified of operations affecting the objects in the
   * &lt;objects&gt; list.
   *
   */

  private Vector calculateOwnerAddresses(Vector objects)
  {
    Enumeration objectsEnum, ownersEnum;
    Invid invid, ownerInvid;
    InvidDBField ownersField, invidField2;
    DBObject object, object2;
    Vector vect;
    Vector results = new Vector();
    Hashtable seenOwners = new Hashtable();

    /* -- */

    objectsEnum = objects.elements();

    while (objectsEnum.hasMoreElements())
      {
	invid = (Invid) objectsEnum.nextElement();
	object = Ganymede.internalSession.session.viewDBObject(invid);

	if (object == null)
	  {
	    if (debug)
	      {
		System.err.println("calculateOwnerAddresses(): Couldn't find invid " + 
				   invid.toString());
	      }

	    continue;
	  }

	if (debug)
	  {
	    System.err.println("DBLog.calculateOwnerAddresses(): processing " + object.getLabel());
	  }

	// okay, we've got a reference to (one of) the DBObject's being
	// modified by this event.. what do we want to do with it?

	// we don't want to mess with embedded objects

	if (object.isEmbedded())
	  {
	    if (debug)
	      {
		System.err.println("calculateOwnerAddresses(): Skipping Embeded invid " + 
				   invid.toString());
	      }

	    continue;
	  }

	// get a list of owners invid's for this object

	ownersField = (InvidDBField) object.getField(SchemaConstants.OwnerListField);
	
	if (ownersField == null)
	  {
	    if (debug)
	      {
		System.err.println("calculateOwnerAddresses(): Couldn't access owner list for invid " + 
				   invid.toString());
	      }

	    continue;
	  }

	vect = ownersField.getValuesLocal();

	// *** Caution!  getValuesLocal() does not clone the field's contents..
	// 
	// DO NOT modify vect here!

	if (vect == null)
	  {
	    if (debug)
	      {
		System.err.println("calculateOwnerAddresses(): Empty owner list for invid " + 
				   invid.toString());
	      }

	    continue;
	  }

	// okay, we have the list of owner invid's for this object.  For each
	// of these owners, we need to see what email lists and addresses are 
	// to receive notification

	ownersEnum = vect.elements(); // this object's owner list

	while (ownersEnum.hasMoreElements())
	  {
	    ownerInvid = (Invid) ownersEnum.nextElement();

	    if (!seenOwners.containsKey(ownerInvid))
	      {
		if (debug)
		  {
		    System.err.println("DBLog.calculateOwnerAddresses(): processing owner group " + 
				       session.viewObjectLabel(ownerInvid));
		  }

		results = VectorUtils.union(results, getOwnerGroupAddresses(ownerInvid));
		
		seenOwners.put(ownerInvid, ownerInvid);
	      }
	  }
      }

    if (debug)
      {
	System.err.print("DBLog.calculateOwnerAddresses(): returning ");

	for (int i = 0; i < results.size(); i++)
	  {
	    if (i > 0)
	      {
		System.err.print(", ");
	      }

	    System.err.print(results.elementAt(i));
	  }

	System.err.println();
      }


    return results;
  }

  /**
   *
   * This method takes an Invid for an Owner Group DBObject
   * and returns a Vector of Strings containing the list
   * of email addresses for that owner group.
   *
   */

  private Vector getOwnerGroupAddresses(Invid ownerInvid)
  {
    DBObject ownerGroup;
    Vector result = new Vector();
    InvidDBField emailInvids;
    StringDBField externalAddresses;

    /* -- */

    ownerGroup = session.session.viewDBObject(ownerInvid);

    if (ownerGroup == null)
      {
	if (debug)
	  {
	    System.err.println("getOwnerGroupAddresses(): Couldn't look up owner group " + 
			       ownerInvid.toString());
	  }
	
	return result;
      }

    // should we cc: the admins?

    Boolean cc = (Boolean) ownerGroup.getFieldValueLocal(SchemaConstants.OwnerCcAdmins);

    if (cc != null && cc.booleanValue())
      {
	Vector adminList = new Vector();
	Vector adminInvidList;
	Invid adminInvid;
	String adminAddr;

	adminInvidList = ownerGroup.getFieldValuesLocal(SchemaConstants.OwnerMembersField);

	for (int i = 0; i < adminInvidList.size(); i++)
	  {
	    adminInvid = (Invid) adminInvidList.elementAt(i);
	    adminAddr = convertAdminInvidToString(adminInvid);

	    if (adminAddr != null)
	      {
		adminList.addElement(adminAddr);
	      }
	  }

	result = VectorUtils.union(result, adminList);
      }

    // do we have any external addresses?

    externalAddresses = (StringDBField) ownerGroup.getField(SchemaConstants.OwnerExternalMail);

    if (externalAddresses == null)
      {
	if (debug)
	  {
	    System.err.println("getOwnerGroupAddresses(): No external mail list defined for owner group " + 
			       ownerInvid.toString());
	  }
      }
    else
      {
	// we don't have to clone externalAddresses.getValuesLocal()
	// since union() will copy the elements rather than just
	// setting result to the vector returned by
	// externalAddresses.getValuesLocal() if result is currently
	// null.

	result = VectorUtils.union(result, externalAddresses.getValuesLocal());
      }

    if (debug)
      {
	System.err.print("getOwnerGroupAddresses(): returning: ");

	for (int i = 0; i < result.size(); i++)
	  {
	    if (i > 0)
	      {
		System.err.print(", ");
	      }

	    System.err.print(result.elementAt(i));
	  }

	System.err.println();
      }

    return result;
  }

  /**
   *
   * This method takes an Invid pointing to an Admin persona
   * record, and returns a string that can be used to send
   * email to that person.  This method will return null
   * if no address could be determined for this administrator.
   *
   */

  private String convertAdminInvidToString(Invid adminInvid)
  {
    DBObject admin;
    String address;
    int colondex;

    /* -- */

    if (adminInvid.getType() != SchemaConstants.PersonaBase)
      {
	throw new RuntimeException("not an administrator invid");
      }

    admin = session.session.viewDBObject(adminInvid);

    address = (String) admin.getFieldValueLocal(SchemaConstants.PersonaMailAddr);

    if (address == null)
      {
	// okay, we got no address pre-registered for this
	// admin.. we need now to try to guess at one, by looking
	// to see this admin's name is of the form user:role, in
	// which case we can just try to send to 'user', which will
	// work as long as Ganymede's users cohere with the user names
	// at Ganymede.mailHostProperty.

	String adminName = session.viewObjectLabel(adminInvid);

	colondex = adminName.indexOf(':');
	
	if (colondex == -1)
	  {
	    return null;
	  }
    
	address = adminName.substring(0, colondex);
      }

    return address;
  }

  /**
   *
   * This method takes a DBLogEvent object, scans it to determine
   * what mailing lists should be notified of the event in the
   * context of a transaction, and adds a description of the
   * passed in event to each MailOut object held in map.
   *
   * That is, the map passed in maps each discrete recipient
   * list to a running MailOut object which has the complete
   * text that will be mailed to that recipient when the
   * transaction's records are mailed out.
   *
   */

  private void appendMailOut(DBLogEvent event, Hashtable map)
  {
    Enumeration enum;
    String str;
    MailOut mailout;

    /* -- */

    calculateMailTargets(event);
    
    enum = event.notifyVect.elements();

    while (enum.hasMoreElements())
      {
	str = (String) enum.nextElement();

	if (debug)
	  {
	    System.err.println("Going to be mailing to " + str);
	  }

	mailout = (MailOut) map.get(str);

	if (mailout == null)
	  {
	    mailout = new MailOut(str);
	    mailout.append(event.description);
	    map.put(str, mailout);
	  }
	else
	  {
	    mailout.append("\n");
	    mailout.append(event.description);
	  }
      }
  }

  /**
   *
   * This method gets the signature file loaded.
   *
   */

  private void loadSignature() throws IOException
  {
    StringBuffer buffer = new StringBuffer();
    FileInputStream in = new FileInputStream(Ganymede.signatureFileProperty);
    BufferedInputStream inBuf = new BufferedInputStream(in);
    int c;
    
    /* -- */

    c = inBuf.read();

    while (c != -1)
      {
	buffer.append((char) c);
	c = inBuf.read();
      }

    inBuf.close();
    in.close();

    signature = buffer.toString();

    if (debug)
      {
	System.err.println("Loaded signature string:");
	System.err.println(signature);
	System.err.println("----");
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 systemEventType

------------------------------------------------------------------------------*/

/**
 *
 * This class is used to store system event information derived from
 * the Ganymede database.
 * 
 */

class systemEventType {

  String token;
  String name;
  String description;
  boolean mail;
  String addresses;
  Vector addressVect;
  boolean ccToSelf;

  private DBField f;

  /* -- */

  systemEventType(DBObject obj)
  {
    token = getString(obj, SchemaConstants.EventToken);
    name = getString(obj, SchemaConstants.EventName);
    description = getString(obj, SchemaConstants.EventDescription);
    mail = getBoolean(obj, SchemaConstants.EventMailBoolean);
    addresses = getAddresses(obj);
    ccToSelf = getBoolean(obj, SchemaConstants.EventMailToSelf);
  }

  private String getString(DBObject obj, short fieldId)
  {
    f = (DBField) obj.getField(fieldId);

    if (f == null)
      {
	return "";
      }

    return (String) f.getValueLocal();
  }

  private boolean getBoolean(DBObject obj, short fieldId)
  {
    f = (DBField) obj.getField(fieldId);

    if (f == null)
      {
	return false;
      }

    return ((Boolean) f.getValueLocal()).booleanValue();
  }

  /**
   *
   * This method takes an event definition object and extracts
   * a list of email addresses to which mail will be sent when
   * an event of this type is logged.
   *
   */

  private String getAddresses(DBObject obj)
  {
    StringBuffer result = new StringBuffer();
    InvidDBField invF;
    StringDBField strF;
    Enumeration enum;
    Invid tmpI;
    String tmpS;

    /* -- */

    addressVect = new Vector();

    // Get the list of addresses from the object's external email
    // string list.. we use union here so that we don't get
    // duplicates.

    strF = (StringDBField) obj.getField(SchemaConstants.EventExternalMail);

    if (strF != null)
      {
	// we don't have to clone strF.getValuesLocal()
	// since union() will copy the elements rather than just
	// setting addressVect to the vector returned by
	// strF.getValuesLocal() if addressVect is currently
	// null.

	addressVect = VectorUtils.union(addressVect, strF.getValuesLocal());
      }
	
    // and create the address string
    
    for (int i = 0; i < addressVect.size(); i++)
      {
	if (i > 0)
	  {
	    result.append(", ");
	  }
	
	result.append((String) addressVect.elementAt(i));
      }

    return result.toString();
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 objectEventType

------------------------------------------------------------------------------*/

/**
 *
 * This class is used to store object event information derived from
 * the Ganymede database.
 * 
 */

class objectEventType {

  String token;
  short objType;
  String name;
  String description;
  String addresses;
  Vector addressVect;
  boolean ccToSelf;
  boolean ccToOwners;
  String hashKey;

  private DBField f;

  /* -- */

  objectEventType(DBObject obj)
  {
    token = getString(obj, SchemaConstants.ObjectEventToken);
    name = getString(obj, SchemaConstants.ObjectEventName);
    description = getString(obj, SchemaConstants.ObjectEventDescription);
    addresses = getAddresses(obj);
    ccToSelf = getBoolean(obj, SchemaConstants.ObjectEventMailToSelf);
    ccToOwners = getBoolean(obj, SchemaConstants.ObjectEventMailOwners);
    objType = (short) getInt(obj, SchemaConstants.ObjectEventObjectType);

    hashKey = token + ":" + objType;
  }

  private String getString(DBObject obj, short fieldId)
  {
    f = (DBField) obj.getField(fieldId);

    if (f == null)
      {
	return "";
      }

    return (String) f.getValueLocal();
  }

  private boolean getBoolean(DBObject obj, short fieldId)
  {
    f = (DBField) obj.getField(fieldId);

    if (f == null)
      {
	return false;
      }

    return ((Boolean) f.getValueLocal()).booleanValue();
  }

  private int getInt(DBObject obj, short fieldId)
  {
    f = (DBField) obj.getField(fieldId);

    // we'll go ahead and throw a NullPointerException if
    // f isn't defined.

    return ((Integer) f.getValueLocal()).intValue();
  }

  /**
   *
   * This method takes an event definition object and extracts
   * a list of email addresses to which mail will be sent when
   * an event of this type is logged.
   *
   */

  private String getAddresses(DBObject obj)
  {
    StringBuffer result = new StringBuffer();
    InvidDBField invF;
    StringDBField strF;
    Enumeration enum;
    Invid tmpI;
    String tmpS;

    /* -- */

    addressVect = new Vector();

    // Get the list of addresses from the object's external email
    // string list.. we use union here so that we don't get
    // duplicates.

    strF = (StringDBField) obj.getField(SchemaConstants.ObjectEventExternalMail);

    if (strF != null)
      {
	// we don't have to clone strF.getValuesLocal()
	// since union() will copy the elements rather than just
	// setting addressVect to the vector returned by
	// strF.getValuesLocal() if addressVect is currently
	// null.

	addressVect = VectorUtils.union(addressVect, strF.getValuesLocal());

	// and create the address string

	for (int i = 0; i < addressVect.size(); i++)
	  {
	    if (i > 0)
	      {
		result.append(", ");
	      }
	    
	    result.append((String) addressVect.elementAt(i));
	  }
      }

    return result.toString();
  }

}

/*------------------------------------------------------------------------------
                                                                           class
								         MailOut

------------------------------------------------------------------------------*/

/**
 *
 * This class is used to store event information derived from the Ganymede
 * database.
 *
 */

class MailOut {

  StringBuffer description = new StringBuffer();
  Vector addresses;

  /* -- */

  MailOut(String address)
  {
    if (address == null)
      {
	throw new NullPointerException("bad address");
      }

    addresses = new Vector();
    addresses.addElement(address);
  }

  void append(String text)
  {
    description.append(text);
  }

  public String toString()
  {
    return description.toString();
  }
}
