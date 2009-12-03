/*

   DBLog.java

   This class manages recording events in the system log and
   generating reports from the system log based on specific criteria.

   Most of the methods in this class must be synchronized to keep the
   logfile itself orderly.

   Created: 31 October 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2009
   The University of Texas at Austin

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

package arlut.csd.ganymede.server;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import Qsmtp.Qsmtp;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.SchemaConstants;

import arlut.csd.Util.TranslationService;

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
 * in {@link arlut.csd.ganymede.server.DBLogEvent DBLogEvent} to re-use the
 * 'multibuffer' StringBuffer.</p>
 */

public class DBLog {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBLog");

  // --

  DBLogController logController;
  DBLogController mailController;

  /**
   *
   * The signature to be appended to any outgoing mail
   *
   */

  String signature = null;

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
   * @see arlut.csd.ganymede.server.systemEventType
   *
   */

  Map<String, systemEventType> sysEventCodes = Collections.synchronizedMap(new HashMap<String, systemEventType>());

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
   * @see arlut.csd.ganymede.server.objectEventType
   *
   */

  Map<String, objectEventType> objEventCodes = Collections.synchronizedMap(new HashMap<String, objectEventType>());

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

  Qsmtp mailer = null;

  /**
   *
   * GanymedeSession reference to allow the log code to do searching
   * of the database, etc.
   *
   */

  GanymedeSession gSession = null;

  /**
   * <p>This instance variable is used to track the transaction identifier
   * across a sequence of startTransactionLog(), streamLogEvent(), and endTransactionLog()
   * calls.</p>
   */

  private String transactionID;

  /**
   * <p>This instance variable is used to track the timestamp for a transaction
   * log across a sequence of startTransactionLog(), streamLogEvent(), and endTransactionLog()
   * calls.</p>
   */

  private Date transactionTimeStamp;

  /**
   * <p>This instance variable is used to track the comment for a
   * transaction log across a sequence of startTransactionLog(),
   * streamLogEvent(), and endTransactionLog() calls.</p>
   */

  private String transactionComment;

  /**
   * <p>This instance variable is used to track all mail messages that we accumulate
   * during the course of logging events for a transaction between startTransactionLog()
   * and endTransactionLog(), by mapping email address to MailOut objects, which hold
   * the mail that we are composing for users who need to know about a full transaction's
   * worth of email.</p>
   */

  private HashMap<String, MailOut> transactionMailOuts;

  /**
   * <p>This instance variable is used to track all mail messages that
   * we accumulate during the course of logging events for a
   * transaction between startTransactionLog() and
   * endTransactionLog(), by mapping objevent tags to HashMaps which
   * in turn map email addresses to the MailOut objects that we are
   * accumulating.</p>
   */

  private HashMap<String, HashMap<String, MailOut>> objectOuts;

  /**
   * <p>This instance variable is used to track the transaction
   * logging instruction in the Ganymede server's System Event
   * DBStore, so that we don't have to keep looking it up during
   * transaction processing.</p>
   */

  private systemEventType transactionControl;

  /* -- */

  /**
   * <p>Constructor for a Ganymede log object.</p>
   *
   * @param logController
   * @param mailController
   * @param gSession GanymedeSession reference used to allow DBLog code to do queries
   * on the Ganymede database
   * @param suppressEmail A boolean that indicates whether we should switch off the sending of
   * emails
   */

  public DBLog(DBLogController logController, DBLogController mailController, GanymedeSession gSession, boolean suppressEmail) throws IOException
  {
    this.gSession = gSession;
    this.logController = logController;
    this.mailController = mailController;

    // get the signature to append to mail messages

    loadSignature();

    // now we need to initialize our hash of DBObjects so that we can do
    // speedy lookup of event codes without having to synchronize on
    // the main objectBase hashes during logging

    updateSysEventCodeHash();

    updateObjEventCodeHash();

    // initalize our mailer

    if (!suppressEmail)
      {
      	mailer = new Qsmtp(Ganymede.mailHostProperty);

      	// run the Qsmtp mailer in non-blocking mode

      	mailer.goThreaded();
      }
  }

  /**
   *
   * This method closes out the log file.
   *
   */

  synchronized void close() throws IOException
  {
    logController.close();

    if (mailController != null)
      {
	mailController.close();

	if (mailer != null)
	  {
            mailer.close(); // we'll block here while the mailer's email thread drains
	  }
      }

    closed = true;
  }

  /**
   * This method sends out a generic mail message that will not be logged.
   *
   * @param recipients a Vector of email addresses to send this
   * message to.  Should never be null or empty.
   *
   * @param title The email subject for this message, will have the
   * Ganymede.subjectPrefixProperty prepended to it.
   *
   * @param description The message itself
   */

  public void sendMail(Vector recipients, String title, String description)
  {
    this.sendMail(recipients, title, description, false, false, null);
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
   * @param title The email subject for this message, will have the
   * Ganymede.subjectPrefixProperty prepended to it.
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
	// "log already closed."
	throw new RuntimeException(ts.l("global.log_closed"));
      }

    if (debug)
      {
	System.err.println("DBLog.mailNotify(): Writing log event " + event.eventClassToken);
      }

    updateSysEventCodeHash();

    // we calculate the list of email addresses that we want to send
    // this event's notifcation to.. we do it before we pass it to the
    // logController so that the log will record who the mail was sent
    // to.

    calculateMailTargets(event, session, null, mailToObjects, mailToOwners);

    event.setLogTime(System.currentTimeMillis());

    // If we're processing a generic mailout, log the mail message to
    // a mail log if we're keeping one, otherwise log it to our primary
    // log

    if (event.eventClassToken.equals("mailout"))
      {
	if (mailController != null)
	  {
	    mailController.writeEvent(event);
	  }
	else
	  {
	    // "DBLog.mailNotify(): Skipping mailout event ({0}) due to mail being disabled at startup."
	    Ganymede.debug(ts.l("mailNotify.no_mail", title));
	  }
      }
    else
      {
	logController.writeEvent(event);

	type = (systemEventType) sysEventCodes.get(event.eventClassToken);

	if (type == null)
	  {
	    // "Error in DBLog.mailNotify(): unrecognized eventClassToken: {0}."
	    Ganymede.debug(ts.l("mailNotify.unrecognized_token", event.eventClassToken));

	    return;
	  }

	if (!type.mail)
	  {
	    // "Logic error in DBLog.mailNotify(): eventClassToken not configured for mail delivery: {0}."
	    Ganymede.debug(ts.l("mailNotify.whaaa", event.eventClassToken));

	    return;
	  }
      }

    if (mailer != null)
      {
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
            titleString = Ganymede.subjectPrefixProperty + title;
          }
        else
          {
            if (title == null)
              {
                titleString = Ganymede.subjectPrefixProperty + type.name;
              }
            else
              {
                titleString = Ganymede.subjectPrefixProperty + title;
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
	    // "DBLog.mailNotify(): mailer error:\n{0}\n\nwhile processing: {1}"
	    Ganymede.debug(ts.l("mailNotify.mailer_error",
				Ganymede.stackTrace(ex),
				event));
          }
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
	throw new RuntimeException(ts.l("global.log_closed"));
      }

    if (debug)
      {
	System.err.println("DBLog.logSystemEvent(): Writing log event " + event.eventClassToken);
      }

    event.setLogTime(System.currentTimeMillis());

    // We haven't augmented event with the mail targets here.. the log
    // won't record who gets notified for system events.  This is OK.

    logController.writeEvent(event);

    sendSysEventMail(event, null);
  }

  /**
   * <P>This method is used to start logging events for a transaction.  It is called from
   * {@link arlut.csd.ganymede.server.DBEditSet#commit_logTransaction(java.util.HashMap)},
   * which is responsible for sequencing this call with calls to streamLogEvent() and
   * endTransactionLog().</P>
   *
   * <p>DBEditSet.commit_logTransaction() is responsible for
   * synchronizing on Ganymede.log, and thereby excluding all other
   * synchronized log calls from being initiated during a
   * transaction's commit.</p>
   *
   * @param invids a HashMap mapping Invid objects to identity
   * @param adminName Human readable string identifying the admin responsible for this transaction
   * @param admin Invid representing the user or admin responsible for this transaction
   * @param comment If not null, a comment to attach to logging and email generated in response to this transaction.
   * @param transaction The {@link arlut.csd.ganymede.server.DBEditSet} representing the transaction to be logged
   */

  public synchronized void startTransactionLog(Vector invids, String adminName, Invid admin, String comment, DBEditSet transaction)
  {
    if (closed)
      {
	throw new RuntimeException(ts.l("global.log_closed"));
      }

    if (debug)
      {
	System.err.println("DBLog.startTransactionLog(): Logging transaction for  " + adminName);
      }

    updateSysEventCodeHash();
    updateObjEventCodeHash();

    this.transactionTimeStamp = new Date(System.currentTimeMillis());
    this.transactionID = adminName + ":" + this.transactionTimeStamp.getTime();

    if (comment != null && !"".equals(comment.trim()))
      {
	this.transactionComment = comment;
      }
    else
      {
	this.transactionComment = null;
      }

    // write out a start-of-transaction line to the log

    // "Start Transaction: {0}"
    DBLogEvent start = new DBLogEvent("starttransaction",
				      ts.l("startTransactionLog.start_template", transaction.description),
				      admin,
				      adminName,
				      invids,
				      null);

    start.setTransactionID(this.transactionID);
    start.setLogTime(this.transactionTimeStamp);

    logController.writeEvent(start);

    // write out the comment line to the log

    if (this.transactionComment != null)
      {
	DBLogEvent commentEvent = new DBLogEvent("comment",
						 this.transactionComment,
						 admin,
						 adminName,
						 invids,
						 null);

	commentEvent.setTransactionID(this.transactionID);
	commentEvent.setLogTime(this.transactionTimeStamp);

	logController.writeEvent(commentEvent);
      }

    this.transactionMailOuts = new HashMap<String, MailOut>();
    this.objectOuts = new HashMap<String, HashMap<String, MailOut>>();

    // check out the 'starttransaction' system event object to see if we're going
    // to do mailing for transaction summaries

    this.transactionControl = (systemEventType) sysEventCodes.get("starttransaction");
  }

  /**
   * <p>This method should only be called after a
   * startTransactionLog() call and before the corresponding
   * endTransactionLog() call, made by {@link
   * arlut.csd.ganymede.server.DBEditSet#commit_logTransaction(java.util.HashMap)}.</p>
   *
   * <p>DBEditSet.commit_logTransaction() is responsible for
   * synchronizing on Ganymede.log, and thereby excluding all other
   * synchronized log calls from being initiated during a
   * transaction's commit.</p>
   */

  public synchronized void streamEvent(DBLogEvent event, DBEditSet transaction)
  {
    if (closed)
      {
	throw new RuntimeException(ts.l("global.log_closed"));
      }

    if (transactionID == null)
      {
	// "Not in a transaction."
	throw new RuntimeException(ts.l("streamEvent.no_transaction"));
      }

    event.setTransactionID(this.transactionID);
    event.setLogTime(this.transactionTimeStamp);

    if (debug)
      {
	System.err.println("DBLog.streamEvent(): logging event: \n** " +
			   event.eventClassToken + " **\n" + event.description);
      }

    if (mailer != null)
      {
	// if the event doesn't have its own subject set, we'll assume
	// that it is not a pre-generated mail message, and that we
	// need to calculate the list of email targets from the
	// Ganymede server's system and object event control objects.

	if (event.subject == null)
	  {
	    // we track all email addresses that we send mail to in
	    // response to this particular event so that we can record
	    // in the log who got told about this

	    Vector sentTo = new Vector();

	    // first, if we have a recognizable object-specific event
	    // happening, queue up notification for it to any interested
	    // parties, for later transmission with sendObjectMail().

	    sentTo = VectorUtils.union(sentTo, appendObjectMail(event, this.objectOuts,
								transaction.description,
								transaction.session));

	    // we may have a system event instead, in which case we handle
	    // mailing it here

	    sentTo = VectorUtils.union(sentTo, sendSysEventMail(event, transaction.description));

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

	    sentTo = VectorUtils.union(sentTo, appendMailOut(event, this.transactionMailOuts,
							     transaction.session,
							     this.transactionControl));

	    // and we want to make sure and send this event to any
	    // addresses listed in the starttransaction system event
	    // object.

	    sentTo = VectorUtils.union(sentTo, this.transactionControl.addressVect);

	    // now we record in the event who we actually sent the
	    // mail to, so it is logged properly

	    event.setMailTargets(sentTo);
	  }
	else
	  {
	    // we've got a generic transactional mail event and we're
	    // allowed to send out emails, so we can process
	    // it.. note that we don't lump it with the transaction
	    // summary.

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

		if (this.transactionComment != null)
		  {
		    // "{0}\n----\nAbout this transaction:\n{1}\n----\n{2}"
		    message = ts.l("streamEvent.comment_template", message, this.transactionComment, signature);
		  }
		else
		  {
		    // "{0}\n{1}"
		    message = ts.l("streamEvent.no_comment_template", message, signature);
		  }

		// bombs away!

		mailer.sendmsg(returnAddr,
			       event.notifyVect,
			       Ganymede.subjectPrefixProperty + event.subject,
			       message);
	      }
	    catch (IOException ex)
	      {
		// "DBLog.streamEvent(): mailer error:\n{0}\n\nwhile processing: {1}"
		Ganymede.debug(ts.l("streamEvent.mailer_error",
				    Ganymede.stackTrace(ex),
				    event));
	      }
	  }
      }

    // and write it to our log

    logController.writeEvent(event);
  }

  /**
   * <p>This method should only be called after a
   * startTransactionLog() call and any corresponding
   * endTransactionLog() calls, made by {@link
   * arlut.csd.ganymede.server.DBEditSet#commit_logTransaction(java.util.HashMap)}.</p>
   *
   * <p>DBEditSet.commit_logTransaction() is responsible for
   * synchronizing on Ganymede.log, and thereby excluding all other
   * synchronized log calls from being initiated during a
   * transaction's commit, when calling this function</p>
   *
   * @param adminName Human readable string identifying the admin responsible for this transaction
   * @param admin Invid representing the user or admin responsible for this transaction
   * @param transaction The {@link arlut.csd.ganymede.server.DBEditSet} representing the transaction to be logged
   */

  public synchronized void endTransactionLog(String adminName, Invid admin, DBEditSet transaction)
  {
    Iterator iter;

    /* -- */

    // write out an end-of-transaction line to the log

    // "Finish Transaction: {0}"
    DBLogEvent finish = new DBLogEvent("finishtransaction",
				       ts.l("endTransactionLog.finish_template", transaction.description),
				       admin,
				       adminName,
				       null,
				       null);
    finish.setTransactionID(transactionID);
    finish.setLogTime(this.transactionTimeStamp);

    logController.writeEvent(finish);

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

    if (mailer != null)
      {
      	sendObjectMail(returnAddr, adminName, this.objectOuts, this.transactionTimeStamp);
      }

    this.objectOuts.clear();
    this.objectOuts = null;

    // send out the transaction summaries if the starttransaction
    // system event has the mail checkbox turned on.

    if (mailer != null && this.transactionControl.mail)
      {
	for (MailOut mailout: transactionMailOuts.values())
	  {
	    String description = null;

	    if (this.transactionComment != null)
	      {
		// "Transaction summary: User {0} {1,date,EEE MMM dd HH:mm:ss zzz yyyy}\n\n----\nAbout this transaction:\n{2}\n----\n{3}{4}"
		description = ts.l("endTransactionLog.summary_comment_template",
				   adminName, this.transactionTimeStamp,
				   this.transactionComment,
				   arlut.csd.Util.WordWrap.wrap(mailout.toString(), 78),
				   signature);
	      }
	    else
	      {
		// "Transaction summary: User {0} {1,date,EEE MMM dd HH:mm:ss zzz yyyy}\n\n{2}{3}"
		description = ts.l("endTransactionLog.summary_template",
				   adminName, this.transactionTimeStamp,
				   arlut.csd.Util.WordWrap.wrap(mailout.toString(), 78),
				   signature);
	      }

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
			       Ganymede.subjectPrefixProperty + "Transaction Log",
			       description);
	      }
	    catch (IOException ex)
	      {
		// "DBLog.endTransactionLog(): mailer error:\n{0}\n\nwhile processing: {1}"
		Ganymede.debug(ts.l("endTransactionLog.mailer_error",
				    Ganymede.stackTrace(ex),
				    finish));
	      }
	  }
      }

    this.transactionMailOuts.clear();
    this.transactionMailOuts = null;
  }

  /**
   * <p>Emergency cleanup function called by {@link
   * arlut.csd.ganymede.server.DBEditSet#commit_logTransaction(java.util.HashMap)} in
   * the event of a problem during logging.</p>
   */

  public synchronized void cleanupTransaction()
  {
    this.transactionID = null;
    this.transactionTimeStamp = null;
    this.transactionComment = null;
    this.transactionMailOuts = null;
    this.objectOuts = null;
    this.transactionControl = null;
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
   * @param beforeTime if not null, retrieveHistory() will only return
   * events occurring on or before the time specified in this Date
   * object.
   *
   * @param keyOnAdmin if true, rather than returning a string containing events
   * that involved &lt;invid&gt;, retrieveHistory() will return a string containing events
   * performed on behalf of the administrator with invid &lt;invid&gt;.
   *
   * @param fullTransactions if true, the buffer returned will include all events in any
   * transactions that involve the given invid.  if false, only those events in a transaction
   * directly affecting the given invid will be returned.
   *
   * @param getLoginEvents if true, this method will return only login
   * and logout events.  if false, this method will return no login
   * and logout events.
   *
   * @return A human-readable multiline string containing a list of history events
   */

  public synchronized StringBuffer retrieveHistory(Invid invid, Date sinceTime, Date beforeTime,
                                                   boolean keyOnAdmin,
						   boolean fullTransactions,
                                                   boolean getLoginEvents)
  {
    return logController.retrieveHistory(invid, sinceTime, beforeTime, keyOnAdmin, fullTransactions, getLoginEvents);
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

    // If we're suppressing sending out all email, then do a no-op

    if (mailer == null)
      {
      	return emailList;
      }

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
	else if (type.description != null && (!type.description.equals("")))
	  {
	    message = type.description + "\n\n" + event.description + "\n\n";
	  }
	else
	  {
	    message = event.description + "\n\n";
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
			   Ganymede.subjectPrefixProperty + type.name,
			   message);
	  }
	catch (IOException ex)
	  {
	    // "DBLog.sendSysEventMail(): mailer error:\n{0}\n\nwhile processing: {1}"
	    Ganymede.debug(ts.l("sendSysEventMail.mailer_error",
				Ganymede.stackTrace(ex),
				event));
	  }

	if (debug)
	  {
	    System.err.println("Completed emailing log event " + event.eventClassToken);
	  }
      }

    return emailList;
  }

  /**
   * <p>This creates and records a MailOut object for custom Object
   * Event notifications if necessary for the DBLogEvent passed.  The
   * MailOut object created, if any, will cause mail to be sent to any
   * email addresses signed up for Object Event notification in the
   * Ganymede database.</p>
   *
   * @return Vector of email addresses this event will be sent to for
   * object event notification, or null if no object event mail is
   * generated
   */

  private Vector appendObjectMail(DBLogEvent event, HashMap<String, HashMap<String, MailOut>> objectOuts,
				  String transdescrip, DBSession transSession)
  {
    if (event == null || event.objects == null || event.objects.size() != 1)
      {
	return null;
      }

    // --

    Vector<String> mailList = new Vector<String>();
    Invid objectInvid = (Invid) event.objects.elementAt(0);
    String key = event.eventClassToken + ":" + objectInvid.getType();
    objectEventType type = objEventCodes.get(key);

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
	mailList = (Vector<String>) VectorUtils.union(mailList, calculateOwnerAddresses(event.objects, true, true, transSession));
      }

    mailList = (Vector<String>) VectorUtils.union(mailList, type.addressVect);

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

    HashMap<String, MailOut> addresses = objectOuts.get(key);

    if (addresses == null)
      {
	addresses = new HashMap<String, MailOut>();

	objectOuts.put(key, addresses);
      }

    for (String address: mailList)
      {
	MailOut mailout = addresses.get(address);

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

  /**
   * Send out type-specific email notifications to email addresses
   * that have signed up for per-object-type mail notifications.
   */

  private void sendObjectMail(String returnAddr, String adminName, HashMap<String, HashMap<String, MailOut>> objectOuts, Date currentTime)
  {
    for (Map.Entry<String, HashMap<String, MailOut>> item: objectOuts.entrySet())
      {
	String key = item.getKey();

	for (Map.Entry<String, MailOut> item2: item.getValue().entrySet())
	  {
	    String address = item2.getKey();
	    MailOut mailout = item2.getValue();

	    objectEventType type = objEventCodes.get(key);

	    String description = null;

	    if (this.transactionComment == null)
	      {
		// "{0} summary: User {1} {2,date,EEE MMM dd HH:mm:ss zzz yyyy}\n\n{3}{4}"
		description = ts.l("sendObjectMail.template",
				   type.name,
				   adminName,
				   currentTime,
				   arlut.csd.Util.WordWrap.wrap(mailout.toString(), 78),
				   signature);
	      }
	    else
	      {
		// "{0} summary: User {1} {2,date,EEE MMM dd HH:mm:ss zzz yyyy}\n\n----\nAbout this transaction:\n{3}\n----\n{4}{5}"
		description = ts.l("sendObjectMail.comment_template",
				   type.name,
				   adminName,
				   currentTime,
				   this.transactionComment,
				   arlut.csd.Util.WordWrap.wrap(mailout.toString(), 78),
				   signature);
	      }

	    String title;

	    if (mailout.entryCount == 1)
	      {
		if (type.name != null)
		  {
		    title = Ganymede.subjectPrefixProperty + type.name;
		  }
		else
		  {
		    title = Ganymede.subjectPrefixProperty + type.token;
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
		    // "{0}{1} (x{2,number,#})"
		    title = ts.l("sendObjectMail.multi_object_subject",
				 Ganymede.subjectPrefixProperty,
				 type.name,
				 Integer.valueOf(mailout.entryCount));
		  }
		else
		  {
		    // "{0}{1} (x{2,number,#})"
		    title = ts.l("sendObjectMail.multi_object_subject",
				 Ganymede.subjectPrefixProperty,
				 type.token,
				 Integer.valueOf(mailout.entryCount));
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
		// "DBLog.sendObjectMail(): mailer error:\n{0}\n\nwhile processing: {1}"
		Ganymede.debug(ts.l("sendObjectMail.mailer_error",
				    Ganymede.stackTrace(ex),
				    title));
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
				    systemEventType eventType,
				    boolean mailToObjects,
				    boolean mailToOwners)
  {
    Vector
      notifyVect;

    /* -- */

    if (session == null)
      {
        session = gSession.getSession();
      }

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
	notifyVect = VectorUtils.union(event.notifyVect,
				       calculateOwnerAddresses(event.objects,
							       mailToObjects,
							       mailToOwners,
                                                               session));
      }
    else if (eventType.ccToOwners)
      {
	notifyVect = VectorUtils.union(event.notifyVect,
				       calculateOwnerAddresses(event.objects,
							       true, true,
                                                               session));
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
   * passed in event to each {@link arlut.csd.ganymede.server.MailOut MailOut}
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

  private Vector appendMailOut(DBLogEvent event, HashMap<String, MailOut> map,
			       DBSession session, systemEventType transactionType)
  {
    Iterator iter;
    String str;
    MailOut mailout;

    /* -- */

    if (transactionType == null)
      {
	calculateMailTargets(event, session, null, true, true);  // null explicitly to quiet FindBugs
      }
    else
      {
	calculateMailTargets(event, session, transactionType, transactionType.ccToOwners,
			     transactionType.ccToOwners);
      }

    iter = event.notifyVect.iterator();

    while (iter.hasNext())
      {
	str = (String) iter.next();

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
   * <P>This method takes a vector of {@link arlut.csd.ganymede.common.Invid Invid}'s
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
   * <P>This method takes a vector of {@link arlut.csd.ganymede.common.Invid Invid}'s
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
   * <P>This method takes a vector of {@link arlut.csd.ganymede.common.Invid Invid}'s
   * representing objects touched
   * during a transaction, and returns a Vector of email addresses that
   * should be notified of operations affecting the objects in the
   * &lt;objects&gt; list.</P>
   */

  static public Vector calculateOwnerAddresses(Vector objects, boolean mailToObjects, boolean mailToOwners, DBSession session)
  {
    Iterator objectsIter, ownersIter;
    Invid invid, ownerInvid;
    InvidDBField ownersField;
    DBObject object;
    Vector vect;
    Vector results = new Vector();
    HashSet<Invid> seenOwners = new HashSet<Invid>();

    /* -- */

    if (debug)
      {
	System.err.println("DBLog.java: calculateOwnerAddresses");
      }

    if (objects == null)
      {
	return results;
      }

    objectsIter = objects.iterator();

    while (objectsIter.hasNext())
      {
	invid = (Invid) objectsIter.next();
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

	ownersIter = vect.iterator(); // this object's owner list

	while (ownersIter.hasNext())
	  {
	    ownerInvid = (Invid) ownersIter.next();

	    if (!seenOwners.contains(ownerInvid))
	      {
		if (debug)
		  {
		    System.err.println("DBLog.calculateOwnerAddresses(): processing owner group " +
				       session.getGSession().viewObjectLabel(ownerInvid));
		  }

		results = VectorUtils.union(results, ownerCustom.getAddresses(ownerInvid,
									      session));

		seenOwners.add(ownerInvid);
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

    addressVect = getAddresses(obj);
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

  private Vector getAddresses(DBObject obj)
  {
    StringDBField strF;

    /* -- */

    Vector addressVect = new Vector();

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

    return addressVect;
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 objectEventType

------------------------------------------------------------------------------*/

/**
 * <P>This class is used to store object event information derived from
 * the Ganymede database for the {@link arlut.csd.ganymede.server.DBLog DBLog} class.</P>
 */

class objectEventType {

  String token;
  short objType;
  String name;
  String description;
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
    addressVect = getAddresses(obj);
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

  private Vector getAddresses(DBObject obj)
  {
    StringDBField strF;

    /* -- */

    Vector addressVect = new Vector();

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
      }

    return addressVect;
  }

}

/*------------------------------------------------------------------------------
                                                                           class
								         MailOut

------------------------------------------------------------------------------*/

/**
 * <P>This class is used to store event information derived from the Ganymede
 * database for the {@link arlut.csd.ganymede.server.DBLog DBLog} class.</P>
 */

class MailOut {

  StringBuffer description = new StringBuffer();
  Vector<String> addresses;
  int entryCount = 0;
  String objName;

  /* -- */

  MailOut(String address)
  {
    if (address == null)
      {
	throw new NullPointerException("bad address");
      }

    addresses = new Vector<String>();
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
