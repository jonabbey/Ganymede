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
   Version: $Revision: 1.44 $
   Last Mod Date: $Date: 2002/03/16 02:13:13 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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

import java.net.*;
import java.io.*;
import java.util.*;

import Qsmtp.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBLog.java

------------------------------------------------------------------------------*/

/**
 * <p>This class manages recording events in the system log and generating
 * reports from the system log based on specific criteria.  The DBLog class
 * is responsible for logging events to an on-disk file, for emailing
 * notification of events to users, admins, and other interested parties,
 * and for scanning through a Ganymede log file for events pertaining to
 * a designated object invid.</p>
 *
 * <p>Most of the methods in this class must be synchronized, both to keep the
 * logfile itself orderly, and to allow the various log-processing methods
 * in {@link arlut.csd.ganymede.DBLogEvent DBLogEvent} to re-use the
 * 'multibuffer' StringBuffer.</p>
 */

public class DBLog {

  static final boolean debug = false;

  // -- 

  String logFileName = null;
  FileOutputStream logStream = null;
  PrintWriter logWriter = null;

  String mailFilename = null;
  FileOutputStream mailLogStream = null;
  PrintWriter mailLogWriter = null;

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

  GanymedeSession gSession = null;

  /* -- */

  /**
   * <p>Constructor for a Ganymede log object.</p>
   *
   * @param filename Filename for an on-disk log file.  Must point to a valid file
   * @param mailFilename Filename for an optional mail events log file.  May be null or empty if
   * no disk-logging of advisory email events is desired.
   * @param gSession GanymedeSession reference used to allow DBLog code to do queries
   * on the Ganymede database
   */

  public DBLog(String filename, String mailFilename, GanymedeSession gSession) throws IOException
  {
    // get the signature to append to mail messages

    loadSignature();

    this.gSession = gSession;

    logFileName = filename;
    logStream = new FileOutputStream(logFileName, true); // append
    logWriter = new PrintWriter(logStream, true); // auto-flush on newline

    logWriter.println();	// emit newline to terminate any incomplete entry

    // if the user wants to log mailout events, set up the mail log

    if (mailFilename != null && !mailFilename.equals(""))
      {
	this.mailFilename = mailFilename;
	mailLogStream = new FileOutputStream(mailFilename, true); // append
	mailLogWriter = new PrintWriter(mailLogStream, true); // auto-flush on newline

	mailLogWriter.println();	// emit newline to terminate any incomplete entry
      }

    // now we need to initialize our hash of DBObjects so that we can do
    // speedy lookup of event codes without having to synchronize on
    // the main objectBase hashes during logging

    updateSysEventCodeHash();

    updateObjEventCodeHash();

    // initalize our mailer

    mailer = new Qsmtp(Ganymede.mailHostProperty);

    // run the Qsmtp mailer in non-blocking mode

    mailer.goThreaded();
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

    if (mailLogStream != null)
      {
	mailLogWriter.close();
	mailLogStream.close();
      }

    mailer.stopThreaded();	// we'll block here while the mailer's email thread drains

    closed = true;

    if (debug)
      {
	System.err.println("DBLog:" + logFileName + " closed.");

	if (mailFilename != null)
	  {
	    System.err.println("DBLog:" + mailFilename + " closed.");
	  }
      }
  }

  /**
   * <p>This method sends out a generic mail message that will not be logged.
   * If mailToObjects and/or mailToObjects are true, mail may be sent
   * to email addresses associated with the objects in the invids Vector,
   * in addition to the recipients list.</p>
   *
   * @param recipients a Vector of email addresses to send this message
   * to.  May legally be null or empty, in which case mail will be sent
   * to anyone needed according to the mailToObjects and mailToOwners
   * parameters
   * @param title The email subject for this message, will have 'Ganymede: ' prepended
   * to it.
   * @param description The message itself
   * @param mailToObjects If true, this event's mail will go to any
   * email addresses associated with objects referenced by event.
   * @param mailToOwners If true, this event's mail will go to the owners
   * of any objects referenced by event.
   * @param invids A vector of Invids to consult for possible mail targetting
   */

  public void sendMail(Vector recipients, String title, String description,
		       boolean mailToObjects, boolean mailToOwners, Vector invids)
  {
    DBLogEvent event;

    /* -- */

    // create a log event
		    
    event = new DBLogEvent("mailout", description, null, null, invids, recipients);

    // we've already put the description in the event, don't need
    // to provide a separate description string to mailNotify
    
    this.mailNotify(title, null, event, mailToObjects, mailToOwners, null);
  }

  /**
   * <P>This method is used to handle an email notification event, where
   * the mail title should reflect detailed information about the
   * event, and extra descriptive information is to be sent out.</P>
   *
   * <P>mailNotify() will send the message to the owners of any objects
   * referenced by event if mailToOwners is true.</P>
   *
   * <P>description and/or title may be null, in which case the proper
   * strings will be extracted from the event's database record</P>
   *
   * @param event A single event to be logged, with its own timestamp.
   * @param mailToOwners If true, this event's mail will go to the owners
   * of any objects referenced by event.
   */

  public void mailNotify(String title, String description,
			 DBLogEvent event,
			 boolean mailToOwners,
			 DBSession session)
  {
    this.mailNotify(title, description, event, true, mailToOwners, session);
  }

  /**
   * <P>This method is used to handle an email notification event, where
   * the mail title should reflect detailed information about the
   * event, and extra descriptive information is to be sent out.</P>
   *
   * <P>mailNotify() will send the message to the owners of any objects
   * referenced by event if mailToOwners is true.</P>
   *
   * <P>description and/or title may be null, in which case the proper
   * strings will be extracted from the event's database record</P>
   *
   * @param event A single event to be logged, with its own timestamp.
   * @param mailToObjects If true, this event's mail will go to any
   * email addresses associated with objects referenced by event.
   * @param mailToOwners If true, this event's mail will go to the owners
   * of any objects referenced by event.
   */

  public synchronized void mailNotify(String title, String description,
				      DBLogEvent event, boolean mailToObjects,
				      boolean mailToOwners,
				      DBSession session)
  {
    systemEventType type = null;

    /* -- */

    if (closed)
      {
	throw new RuntimeException("log already closed.");
      }

    if (debug)
      {
	System.err.println("DBLog.mailNotify(): Writing log event " + event.eventClassToken);
      }

    updateSysEventCodeHash();

    // we calculate the list of email addresses that we want to send this
    // event's notifcation to.. we do it before the writeEntry() call so
    // that the log will record who the mail was sent to.

    calculateMailTargets(event, session, null, mailToObjects, mailToOwners);

    currentTime.setTime(System.currentTimeMillis());

    // If we're processing a generic mailout, log the mail message to a mail log
    // if we're keeping one, otherwise skip logging

    if (event.eventClassToken.equals("mailout") && mailLogWriter != null)
      {
	event.writeEntry(mailLogWriter, currentTime, null);
      }
    else
      {
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
      }

    if (debug)
      {
	System.err.println("Attempting to email log event " + event.eventClassToken);
      }

    // prepare our message, word wrap it

    String message;

    if (type == null)
      {
	message = event.description + "\n\n";
      }
    else
      {
	message = type.description + "\n\n" + event.description + "\n\n";
      }

    if (description != null)
      {
	message = message + description + "\n\n";
      }
    
    message = arlut.csd.Util.WordWrap.wrap(message, 78);

    // the signature is pre-wrapped
	
    message = message + signature;

    // get our list of recipients from the event's enumerated list of recipients
    // and the event code's address list.

    Vector emailList;

    if (type == null)
      {
	emailList = event.notifyVect;
      }
    else
      {
	emailList = VectorUtils.union(event.notifyVect, type.addressVect);
      }

    String titleString;

    if (type == null)
      {
	titleString = "Ganymede: " + title;
      }
    else
      {
	if (title == null)
	  {
	    titleString = "Ganymede: " + type.name;
	  }
	else
	  {
	    titleString = "Ganymede: " + title;
	  }
      }

    // and now..
    
    try
      {
	// bombs away!

	mailer.sendmsg(Ganymede.returnaddrProperty,
		       emailList,
		       titleString,
		       message);
      }
    catch (IOException ex)
      {
	Ganymede.debug("DBLog.mailNotify(): mailer error " + ex);
      }

    if (debug)
      {
	System.err.println("Completed emailing log event " + event.eventClassToken);
      }
  }

  /**
   * <P>This method is used to log an event such as server shutdown/restart,
   * user log-in, persona change, etc.  Basically any thing not associated
   * with a transaction.</P>
   *
   * @param event A single event to be logged, with its own timestamp.
   */

  public synchronized void logSystemEvent(DBLogEvent event)
  {
    if (closed)
      {
	throw new RuntimeException("log already closed.");
      }

    if (debug)
      {
	System.err.println("DBLog.logSystemEvent(): Writing log event " + event.eventClassToken);
      }

    currentTime.setTime(System.currentTimeMillis());

    // We haven't augmented event with the mail targets here.. the log
    // won't record who gets notified for system events.  This is OK.

    event.writeEntry(logWriter, currentTime, null);

    sendSysEventMail(event, null);
  }

  /**
   * <P>This method is used to log all events associated with a transaction.</P>
   *
   * @param logEvents a Vector of DBLogEvent objects.
   */

  public synchronized void logTransaction(Vector logEvents, String adminName, 
					  Invid admin, DBEditSet transaction)
  {
    String transactionID;
    String transdescrip = transaction.description;
    boolean found;
    DBLogEvent event;
    Enumeration enum;
    Hashtable mailOuts = new Hashtable(); // maps address to MailOut objects
    Hashtable objectOuts = new Hashtable(); // objevent tag to hash mapping address to MailOut objects
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

    // check out the 'starttransaction' system event object to see if we're going
    // to do mailing for transaction summaries

    systemEventType transactionType = (systemEventType) sysEventCodes.get("starttransaction");

    // write out all the log events in this transaction

    for (int i = 0; i < logEvents.size(); i++)
      {
	event = (DBLogEvent) logEvents.elementAt(i);

	if (debug)
	  {
	    System.err.println("DBLog.logTransaction(): logging event: \n** " + 
			       event.eventClassToken + " **\n" + event.description);
	  }

	// if the event has its own subject set, assume that it is a
	// mailout event with its own list of designated email targets

	if (event.subject == null)
	  {
	    // we track all email addresses that we send mail to in
	    // response to this particular event so that we can record
	    // in the log who got told about this

	    Vector sentTo = new Vector();

	    // first, if we have a recognizable object-specific event
	    // happening, queue up notification for it to any intersted
	    // parties, for later transmission with sendObjectMail().

	    sentTo = VectorUtils.union(sentTo, appendObjectMail(event, objectOuts,
								transdescrip, transaction.session));

	    // we may have a system event instead, in which case we handle
	    // mailing it here

	    sentTo = VectorUtils.union(sentTo, sendSysEventMail(event, transdescrip));
	
	    // now, go ahead and add to the mail buffers we are prepping
	    // to describe this whole transaction
	
	    // we are keeping a bunch of buffers, one for each
	    // combination of email addresses that we've
	    // encountered.. different addresses or groups of
	    // addresses may get a different subset of the mail for
	    // this transaction, the appendMailOut() logic handles
	    // that.
	
	    // appendMailOut() takes care of calling
	    // calculateMailTargets() on event, which handles
	    // calculating who needs to receive owner-group related
	    // generic email about this event.
	
	    sentTo = VectorUtils.union(sentTo, appendMailOut(event, mailOuts, 
							     transaction.session,
							     transactionType));

	    // and we want to make sure and send this event to any
	    // addresses listed in the starttransaction system event
	    // object.

	    sentTo = VectorUtils.union(sentTo, transactionType.addressVect);

	    // now we record in the event who we actually sent the
	    // mail to, so it is logged properly

	    event.notifyVect = sentTo;

	    // and write it to our log

	    event.writeEntry(logWriter, currentTime, transactionID);
	  }
	else
	  {
	    // we've got a generic transactional mail event, process
	    // it.. note that we don't lump it with the transaction
	    // summary.

	    event.writeEntry(logWriter, currentTime, transactionID);

	    String returnAddr = null;

	    // who should we say the mail is from?

	    if (event.admin != null)
	      {
		returnAddr = adminPersonaCustom.convertAdminInvidToString(event.admin, 
									  transaction.session);
	      }
	    else
	      {
		returnAddr = Ganymede.returnaddrProperty;
	      }

	    try
	      {
		String message = event.description;

		message = arlut.csd.Util.WordWrap.wrap(message, 78);

		message = message + "\n" + signature;

		// bombs away!
		
		mailer.sendmsg(returnAddr,
			       event.notifyVect,
			       "Ganymede: " + event.subject,
			       message);
	      }
	    catch (IOException ex)
	      {
		Ganymede.debug("DBLog.logTransaction(): mailer error " + ex);
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

    String returnAddr = null;

    // who should we say the mail is from?
    
    if (admin != null)
      {
	returnAddr = adminPersonaCustom.convertAdminInvidToString(admin, 
								  gSession.getSession());
      }

    // if there was no email address registered for the admin persona,
    // use the return address listed in the ganymede.properties file

    if (returnAddr == null)
      {
	returnAddr = Ganymede.returnaddrProperty;
      }

    // send out object event mail to anyone who has signed up for it

    sendObjectMail(returnAddr, adminName, objectOuts);

    objectOuts.clear();

    // send out the transaction summaries if the starttransaction
    // system event has the mail checkbox turned on.

    if (transactionType.mail)
      {
	enum = mailOuts.elements();

	while (enum.hasMoreElements())
	  {
	    MailOut mailout = (MailOut) enum.nextElement();
	    String description = "Transaction summary: User " + adminName + ":" + 
	      currentTime.toString() + "\n\n" + 
	      arlut.csd.Util.WordWrap.wrap(mailout.toString(), 78) + signature;

	    // we don't want any \n's between wordwrap and signature above,
	    // since appendMailOut() adds "\n\n" at the end of each transaction
	    // summary segment

	    if (debug)
	      {
		System.err.println("Sending mail to " + (String) mailout.addresses.elementAt(0));
	      }

	    try
	      {
		mailer.sendmsg(returnAddr,
			       mailout.addresses,
			       "Ganymede: Transaction Log",
			       description);
	      }
	    catch (IOException ex)
	      {
		Ganymede.debug("DBLog.logTransaction(): mailer error " + ex);
	      }
	  }
      }

    mailOuts.clear();
  }

  /**
   * <P>This method is used to scan the log file for log events that match
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

  public synchronized StringBuffer retrieveHistory(Invid invid, Date sinceTime, boolean keyOnAdmin,
						   boolean fullTransactions)
  {
    StringBuffer buffer = new StringBuffer();
    DBLogEvent event = null;
    String line;
    String transactionID = null;

    boolean afterSinceTime = false;
    String dateString;
    long timeCode;
    Date time;

    BufferedReader in = null;
    FileReader reader = null;

    // if we don't have a log helper or we aren't looking for a
    // specific invid, just read from the file directly

    if (Ganymede.logHelperProperty != null && invid != null)
      {
	java.lang.Runtime runtime = java.lang.Runtime.getRuntime();

	try
	  {
	    java.lang.Process helperProcess;

	    if (keyOnAdmin)
	      {
		helperProcess = runtime.exec(Ganymede.logHelperProperty + " -a " + invid.toString());
	      }
	    else
	      {
		helperProcess = runtime.exec(Ganymede.logHelperProperty + " " + invid.toString());
	      }

	    in = new BufferedReader(new InputStreamReader(helperProcess.getInputStream()));
	  }
	catch (IOException ex)
	  {
	    System.err.println("DBLog.retrieveHistory(): Couldn't use helperProcess " + Ganymede.logHelperProperty);
	    in = null;
	  }
      }
    
    if (in == null)
      {
	try
	  {
	    reader = new FileReader(logFileName);
	  }
	catch (FileNotFoundException ex)
	  {
	    return null;
	  }

	in = new BufferedReader(reader);
      }

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
		for (int i = 0; !found && i < event.objects.size(); i++)
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
			if (fullTransactions || event.eventClassToken.equals("finishtransaction"))
			  {
			    found = true;
			  }
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
    finally
      {
	try
	  {
	    in.close();
	  }
	catch (IOException ex)
	  {
	    // shrug
	  }

	try
	  {
	    if (reader != null)
	      {
		reader.close();
	      }
	  }
	catch (IOException ex)
	  {
	    // shrug
	  }
      }

    return buffer;
  }

  // -----

  /**
   * <P>This sends out system event mail to the appropriate users,
   * based on the system event record's flags.</P>
   *
   * @return vector of email addresses this event was sent to for 
   * system event notification
   */

  private Vector sendSysEventMail(DBLogEvent event, String transdescrip)
  {
    systemEventType type;
    String returnAddr;
    Vector emailList = new Vector();

    /* -- */

    updateSysEventCodeHash();
    
    type = (systemEventType) sysEventCodes.get(event.eventClassToken);

    if (type == null)
      {
	return emailList;
      }

    if (type.mail)
      {
	if (debug)
	  {
	    System.err.println("Attempting to email log event " + event.eventClassToken);
	  }

	// prepare our message, word wrap it

	String message;

	if (transdescrip != null && (!transdescrip.equals("null")))
	  {
	    message = transdescrip + "\n\n" + type.description + "\n\n" + event.description + "\n\n";
	  }
	else
	  {
	    message = type.description + "\n\n" + event.description + "\n\n";
	  }

	message = arlut.csd.Util.WordWrap.wrap(message, 78);
	
	message = message + signature;

	// get our list of recipients

	if (event.notifyVect != null)
	  {
	    for (int i = 0; i < event.notifyVect.size(); i++)
	      {
		VectorUtils.unionAdd(emailList, event.notifyVect.elementAt(i));
	      }
	  }

	if (type.addressVect != null)
	  {
	    for (int i = 0; i < type.addressVect.size(); i++)
	      {
		VectorUtils.unionAdd(emailList, type.addressVect.elementAt(i));
	      }
	  }

	if (type.ccToSelf)
	  {
	    String name = null;

	    if (event.admin != null)
	      {
		name = adminPersonaCustom.convertAdminInvidToString(event.admin, gSession.getSession());
	      }
	    else
	      {
		name = event.adminName;	// hopefully this will be a valid email target.. used for bad login attempts

		// skip any persona info after a colon in case the
		// user tried logging in with admin privileges

		if (name != null && name.indexOf(':') != -1)
		  {
		    name = name.substring(0, name.indexOf(':'));
		  }
	      }

	    if (name != null)
	      {
		VectorUtils.unionAdd(emailList, name);
	      }
	  }

	if (type.ccToOwners)
	  {
	    emailList = VectorUtils.union(emailList, calculateOwnerAddresses(event.objects, true, true));
	  }

	// who should we say the mail is from?

	if (event.admin != null)
	  {
	    returnAddr = adminPersonaCustom.convertAdminInvidToString(event.admin, 
								      gSession.getSession());
	  }
	else
	  {
	    returnAddr = Ganymede.returnaddrProperty;
	  }

	// and now..

	try
	  {
	    // bombs away!

	    mailer.sendmsg(returnAddr,
			   emailList,
			   "Ganymede: " + type.name,
			   message);
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("DBLog.sendSysEventMail(): mailer error " + ex);
	    ex.printStackTrace();
	    Ganymede.debug("\n\nwhile processing " + event);
	  }

	if (debug)
	  {
	    System.err.println("Completed emailing log event " + event.eventClassToken);
	  }
      }

    return emailList;
  }

  /**
   * <P>This sends out the 'auxiliary' type specific log information mail
   * to designated users, using the object event objects in the Ganymede
   * database.  This mail is sent for a distinct eventcode/object type pair,
   * outside of the context of a transaction.</P>
   *
   * @return vector of email addresses this event was sent to for 
   * system event notification
   */

  private Vector appendObjectMail(DBLogEvent event, Hashtable objectOuts,
				  String transdescrip, DBSession transSession)
  {
    if (event == null || event.objects == null || event.objects.size() != 1)
      {
	return null;
      }

    // --

    String returnAddr;
    Vector mailList = new Vector();
    Invid objectInvid = (Invid) event.objects.elementAt(0);
    String key = event.eventClassToken + ":" + objectInvid.getType();
    objectEventType type = (objectEventType) objEventCodes.get(key);

    /* -- */

    if (debug)
      {
	System.err.println("DBLog.appendObjectMail(): processing object Event " + key);
      }

    if (type == null)
      {
	if (debug)
	  {
	    System.err.println("DBLog.appendObjectMail(): couldn't find objectEventType " + key);
	  }

	return null;
      }

    // ok.  now we've got an objectEventType, so check to see if we
    // want to send out mail for this event.

    if (type.ccToSelf)
      {
	String name = null;

	if (event.admin != null)
	  {
	    name = adminPersonaCustom.convertAdminInvidToString(event.admin, transSession);

	    if (name != null)
	      {
		mailList.addElement(name);
	      }
	  }
      }

    if (type.ccToOwners)
      {
	mailList = VectorUtils.union(mailList, calculateOwnerAddresses(event.objects, true, true));
      }

    mailList = VectorUtils.union(mailList, type.addressVect);

    if (mailList.size() == 0)
      {
	return null;
      }

    // looking up the object name can be pricey, so we wait until we
    // know we probably need to do it, here

    String objectName = transSession.getGSession().viewObjectLabel(objectInvid);

    // okay, we have some users interested in getting notified about this
    // object event..

    // get the address hash for this object type.. we use this second
    // hash to maintain separate object event summaries for different
    // users, in case one transaction involves objects with different
    // owner groups

    Hashtable addresses = (Hashtable) objectOuts.get(key);

    if (addresses == null)
      {
	addresses = new Hashtable();

	objectOuts.put(key, addresses);
      }

    for (int i = 0; i < mailList.size(); i++)
      {
	String address = (String) mailList.elementAt(i);

	MailOut mailout = (MailOut) addresses.get(address);

	if (mailout == null)
	  {
	    mailout = new MailOut(address);
	    addresses.put(address, mailout);

	    // we always create an object event MailOut with the name
	    // of the object that inspired us to create
	    // it.. sendObjectMail will include this information on
	    // the subject mail if the MailOut winds up with only one
	    // object

	    mailout.setObjectName(objectName);
	  }
	
	mailout.append(event);
      }

    return mailList;
  }

  private void sendObjectMail(String returnAddr, String adminName, Hashtable objectOuts)
  {
    Enumeration enum = objectOuts.keys();

    while (enum.hasMoreElements())
      {
	String key = (String) enum.nextElement();
	Hashtable addresses = (Hashtable) objectOuts.get(key);

	Enumeration enum2 = addresses.keys();

	while (enum2.hasMoreElements())
	  {
	    String address = (String) enum2.nextElement();
	    MailOut mailout = (MailOut) addresses.get(address);

	    objectEventType type = (objectEventType) objEventCodes.get(key);

	    String description = type.name + " summary: User " + adminName + ":" + 
	      currentTime.toString() + "\n\n" + 
	      arlut.csd.Util.WordWrap.wrap(mailout.toString(), 78) + signature;

	    String title;

	    if (mailout.entryCount == 1)
	      {
		if (type.name != null)
		  {
		    title = "Ganymede: " + type.name; 
		  }
		else
		  {
		    title = "Ganymede: " + type.token;
		  }

		if (mailout.objName != null)
		  {
		    title = title + " (\"" + mailout.objName + "\")";
		  }
	      }
	    else
	      {
		if (type.name != null)
		  {
		    title = "Ganymede: " + type.name + " (x" + mailout.entryCount + ")";
		  }
		else
		  {
		    title = "Ganymede: " + type.token + " (x" + mailout.entryCount + ")";
		  }
	      }

	    try
	      {
		mailer.sendmsg(returnAddr,
			       mailout.addresses,
			       title,
			       description);
	      }
	    catch (IOException ex)
	      {
		Ganymede.debug("DBLog.logTransaction(): mailer error " + ex);
	      }
	  }
      }
  }

  /**
   * <P>Private helper method to (re)initialize our local hash of system
   * event codes.</P>
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

	// this method can be called during a transaction commit
	// sequence, in which this thread may have already locked the
	// object base we would otherwise need to query..

	if (eventBase.isLocked())
	  {
	    if (debug)
	      {
		System.err.println("updateSysEventCodeHash(): exiting, sysEvent Base locked..");
	      }

	    return;
	  }
      }
    
    if (debug)
      {
	System.err.println("updateSysEventCodeHash(): updating..");
      }

    sysEventCodes.clear();

    // this query would lock if eventBase is already locked on this thread 

    eventCodeVector = gSession.internalQuery(new Query(SchemaConstants.EventBase));

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
	
	sysEventCodes.put(entry.toString(), 
			  new systemEventType(gSession.getSession().viewDBObject(entry.getInvid())));
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
   * <P>Private helper method to (re)initialize our local hash of object
   * event codes.</P>
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

	// this method can be called during a transaction commit
	// sequence, in which this thread may have already locked the
	// object base we would otherwise need to query..

	if (eventBase.isLocked())
	  {
	    if (debug)
	      {
		System.err.println("updateObjEventCodeHash(): exiting, objEvent Base locked..");
	      }

	    return;
	  }
      }

    if (debug)
      {
	System.err.println("updateObjEventCodeHash(): updating..");
      }

    objEventCodes.clear();

    // would deadlock here if eventBase was locked on this thread

    eventCodeVector = gSession.internalQuery(new Query(SchemaConstants.ObjectEventBase));

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

	objEventobj = (DBObject) gSession.getSession().viewDBObject(entry.getInvid());
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

  private void calculateMailTargets(DBLogEvent event, DBSession session, 
				    systemEventType eventType)
  {
    if (eventType == null)
      {
	this.calculateMailTargets(event, session, eventType, true, true);
      }
    else
      {
	this.calculateMailTargets(event, session, eventType, eventType.ccToOwners,
				  eventType.ccToOwners);
      }
  }

  /**
   * <P>This method generates a list of additional email addresses that
   * notification for this event should be sent to, based on the
   * event's type and the ownership of objects referenced by this
   * event.</P>
   *
   * <P>Note that the email addresses added to this event's mail list
   * will be in addition to any that were previously specified by the
   * code that originally generated the log event.</P>
   */

  private void calculateMailTargets(DBLogEvent event, DBSession session, 
				    systemEventType eventType, boolean mailToObjects,
				    boolean mailToOwners)
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
	System.err.println("DBLog.java: calculateMailTargets: entering");
	System.err.println(event.toString());
      }

    if (eventType == null)
      {
	notifyVect = VectorUtils.union(event.notifyVect, calculateOwnerAddresses(event.objects, mailToObjects, mailToOwners));
      }
    else if (eventType.ccToOwners)
      {
	notifyVect = VectorUtils.union(event.notifyVect, calculateOwnerAddresses(event.objects, true, true));
      }
    else
      {
	notifyVect = new Vector();
      }

    if (eventType == null || eventType.ccToSelf)
      {
	// always include the email address for the admin who
	// initiated the action.

	if (event.admin != null)
	  {
	    VectorUtils.unionAdd(notifyVect, 
				 adminPersonaCustom.convertAdminInvidToString(event.admin, 
									      session));
	  }
      }

    // now update notifyList

    event.setMailTargets(notifyVect);

    // The DBLogEvent needs to remember that we've already expanded
    // its email list.

    event.augmented = true;

    if (debug)
      {
	System.err.println("DBLog.java: calculateMailTargets: exiting");
	System.err.println(event.toString());
      }
  }

  /**
   * <P>This method takes a DBLogEvent object, scans it to determine
   * what mailing lists should be notified of the event in the
   * context of a transaction, and adds a description of the
   * passed in event to each {@link arlut.csd.ganymede.MailOut MailOut}
   * object held in map.</P>
   *
   * <P>That is, the map passed in maps each discrete recipient
   * list to a running MailOut object which has the complete
   * text that will be mailed to that recipient when the
   * transaction's records are mailed out.</P>
   *
   * @return vector of email addresses this event was sent to for 
   * system event notification
   */

  private Vector appendMailOut(DBLogEvent event, Hashtable map, 
			       DBSession session, systemEventType transactionType)
  {
    Enumeration enum;
    String str;
    MailOut mailout;

    /* -- */

    calculateMailTargets(event, session, transactionType);
    
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
	    map.put(str, mailout);
	  }

	mailout.append(event);
      }

    return event.notifyVect;
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

  /**
   * <P>This method takes a vector of {@link arlut.csd.ganymede.Invid Invid}'s
   * representing objects touched
   * during a transaction, and returns a Vector of email addresses that
   * should be notified of operations affecting the objects in the
   * &lt;objects&gt; list.</P>
   */

  public Vector calculateOwnerAddresses(Vector objects, boolean mailToObjects, boolean mailToOwners)
  {
    return DBLog.calculateOwnerAddresses(objects, mailToObjects, mailToOwners, gSession.getSession());
  }

  //
  //
  //
  // STATIC methods
  //
  //
  //

  
  /**
   * <P>This method takes a vector of {@link arlut.csd.ganymede.Invid Invid}'s
   * representing objects touched
   * during a transaction, and returns a Vector of email addresses that
   * should be notified of operations affecting the objects in the
   * &lt;objects&gt; list.</P>
   */

  static public Vector calculateOwnerAddresses(Vector objects, DBSession session)
  {
    return calculateOwnerAddresses(objects, true, true, session);
  }
  
  /**
   * <P>This method takes a vector of {@link arlut.csd.ganymede.Invid Invid}'s
   * representing objects touched
   * during a transaction, and returns a Vector of email addresses that
   * should be notified of operations affecting the objects in the
   * &lt;objects&gt; list.</P>
   */

  static public Vector calculateOwnerAddresses(Vector objects, boolean mailToObjects, boolean mailToOwners, DBSession session)
  {
    Enumeration objectsEnum, ownersEnum;
    Invid invid, ownerInvid;
    InvidDBField ownersField, invidField2;
    DBObject object, object2;
    Vector vect;
    Vector results = new Vector();
    Hashtable seenOwners = new Hashtable();

    /* -- */

    if (debug)
      {
	System.err.println("DBLog.java: calculateOwnerAddresses");
      }

    objectsEnum = objects.elements();

    while (objectsEnum.hasMoreElements())
      {
	invid = (Invid) objectsEnum.nextElement();
	object = session.viewDBObject(invid);

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

	// first off, does the object itself have anyone it wants to notify?

	if (mailToObjects && object.hasEmailTarget())
	  {
	    results = VectorUtils.union(results, object.getEmailTargets());
	  }

	// okay, now we've got to see about notifying the owners..

	if (!mailToOwners)
	  {
	    return results;
	  }

	if (object.isEmbedded())
	  {
	    if (debug)
	      {
		System.err.println("calculateOwnerAddresses(): Looking up owner for Embeded invid " + 
				   invid.toString());
	      }

	    DBObject refObj = object;

	    // if we have are getting rid of an embedded object, we'll need to look
	    // at the original version of the object to get its parent.

	    if (refObj instanceof DBEditObject)
	      {
		DBEditObject refEObj = (DBEditObject) refObj;

		if (refEObj.getStatus()== ObjectStatus.DELETING)
		  {
		    refObj = refEObj.getOriginal();
		  }
	      }

	    try
	      {
		refObj = session.getGSession().getContainingObj(refObj);
	      }
	    catch (IntegrityConstraintException ex)
	      {
		Ganymede.debug("Couldn't find container for " + refObj.getLabel());

		continue;
	      }

	    ownersField = (InvidDBField) refObj.getField(SchemaConstants.OwnerListField);
	  }
	else
	  {
	    // get a list of owners invid's for this object

	    DBObject refObj = object;
	    
	    // if we are deleting an object, we'll need to look at the
	    // original to get the list of owners for it
	    
	    if (refObj instanceof DBEditObject)
	      {
		DBEditObject refEObj = (DBEditObject) refObj;

		if (refEObj.getStatus()== ObjectStatus.DELETING)
		  {
		    refObj = refEObj.getOriginal();
		  }
	      }

	    ownersField = (InvidDBField) refObj.getField(SchemaConstants.OwnerListField);
	  }
	
	if (ownersField == null)
	  {
	    if (debug)
	      {
		System.err.println("calculateOwnerAddresses(): disregarding supergash-owned invid " + 
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
				       session.getGSession().viewObjectLabel(ownerInvid));
		  }

		results = VectorUtils.union(results, ownerCustom.getAddresses(ownerInvid, 
									      session));
		
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
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 systemEventType

------------------------------------------------------------------------------*/

/**
 * <P>This class is used to store system event information derived from
 * the Ganymede database.</P>
 */

class systemEventType {

  String token;
  String name;
  String description;
  boolean mail;
  String addresses;
  Vector addressVect;
  boolean ccToSelf;
  boolean ccToOwners;

  private DBField f;

  /* -- */

  systemEventType(DBObject obj)
  {
    token = getString(obj, SchemaConstants.EventToken);
    name = getString(obj, SchemaConstants.EventName);
    description = getString(obj, SchemaConstants.EventDescription);
    mail = getBoolean(obj, SchemaConstants.EventMailBoolean);
    ccToSelf = getBoolean(obj, SchemaConstants.EventMailToSelf);
    ccToOwners = getBoolean(obj, SchemaConstants.EventMailOwners);

    // and calculate the addresses that always need to be notified
    // of this type of system event

    addresses = getAddresses(obj);
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
   * <P>This method takes an event definition object and extracts
   * a list of email addresses to which mail will be sent when
   * an event of this type is logged.</P>
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
 * <P>This class is used to store object event information derived from
 * the Ganymede database for the {@link arlut.csd.ganymede.DBLog DBLog} class.</P>
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
   * <P>This method takes an event definition object and extracts
   * a list of email addresses to which mail will be sent when
   * an event of this type is logged.</P>
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
 * <P>This class is used to store event information derived from the Ganymede
 * database for the {@link arlut.csd.ganymede.DBLog DBLog} class.</P>
 */

class MailOut {

  StringBuffer description = new StringBuffer();
  Vector addresses;
  int entryCount = 0;
  String objName;

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

  void setObjectName(String objName)
  {
    this.objName = objName;
  }

  void addEntryCount()
  {
    entryCount++;
  }

  void append(DBLogEvent event)
  {
    entryCount++;
    description.append("------------------------------------------------------------\n\n");
    description.append(event.eventClassToken);
    description.append("\n");
    
    for (int i = 0; i < event.eventClassToken.length(); i++)
      {
	description.append("-");
      }
	
    description.append("\n\n");
    description.append(event.description);
  }

  public String toString()
  {
    return description.toString();
  }
}
