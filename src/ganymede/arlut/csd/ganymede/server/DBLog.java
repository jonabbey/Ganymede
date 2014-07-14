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

   Copyright (C) 1996-2014
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import Qsmtp.Qsmtp;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.SchemaConstants;

import arlut.csd.Util.StringUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;

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
 * <p>Most of the methods in this class must be synchronized to keep
 * the logfile orderly.</p>
 */

final public class DBLog implements java.io.Closeable {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBLog");

  // --

  /**
   * <p>Enum used to specify whether the logging system should
   * calculate additional addresses to send an event to.</p>
   *
   * <p>Logging methods that receive a mode of NONE will not calculate
   * any additional email addresses to send an event to.</p>
   *
   * <p>Logging methods that receive a mode of USERS will include any
   * addresses returned by DBEditObject.getEmailTargets() for a given
   * Invid.</p>
   *
   * <p>Logging methods that receive a mode of OWNERS will calculate
   * and include the addresses for any administrative owners of the
   * objects referenced by Invid.</p>
   *
   * <p>Logging methods that receive a mode of BOTH will do both of
   * the above.</p>
   */

  public enum MailMode
  {
    /**
     * No addresses are derived from Invids passed to sendMail().
     */

    NONE,

    /**
     * Email will be sent to any email addresses provided by a custom
     * DBEditObject.getEmailTargets() method associated with the
     * Invids passed to sendMail().. i.e., to the email addresses for
     * Users modified in this transaction.
     */

    USERS,

    /**
     * Email will be sent to the admins and notification lists
     * associated with the owners of the Invids passed.
     */

    OWNERS,

    /**
     * Email will be sent to both email addresses associated with the
     * objects corresponding to the Invids passed as well as to the
     * admins and notification lists associated with the owners of the
     * Invids.
     */

    BOTH;
  }

  // --

  /**
   * The log controller responsible for logging events to disk or to a
   * database.
   */

  private DBLogController logController;

  /**
   * The log controller responsible for logging 'mailout' events to
   * disk or to a database.
   */

  private DBLogController mailController;

  /**
   * The signature to be appended to any outgoing mail
   */

  private String signature = null;

  /**
   * This variable tracks whether the log file has been closed, or whether
   * it is open for append.  If true, the log file may not be written to.
   */

  private boolean closed = false;

  /**
   * We keep a table of the system event codes to speed the logging process.
   * This hash maps system event classTokens to instances of systemEventType.
   *
   * @see arlut.csd.ganymede.server.systemEventType
   */

  private Map<String, systemEventType> sysEventCodes = Collections.synchronizedMap(new HashMap<String, systemEventType>());

  /**
   * This field keeps track of when we last updated the sysEventCodes
   * hash, so that we can check against the timestamp held in the
   * System Event DBObjectBase to see whether we need to refresh the
   * sysEventCodes hash.
   */

  private Date sysEventCodesTimeStamp = null;

  /**
   * We keep a table of the system event codes to speed the logging process.
   * This hash maps object event classTokens to instances of objectEventType.
   *
   * @see arlut.csd.ganymede.server.objectEventType
   */

  private Map<String, objectEventType> objEventCodes = Collections.synchronizedMap(new HashMap<String, objectEventType>());

  /**
   * This field keeps track of when we last updated the objEventCodes
   * hash, so that we can check against the timestamp held in the
   * Object Event DBObjectBase to see whether we need to refresh the
   * objEventCodes hash.
   */

  private Date objEventCodesTimeStamp = null;

  /**
   * Our mail relay.
   */

  private Qsmtp mailer = null;

  /**
   * GanymedeSession reference to allow the log code to do searching
   * of the database, etc.
   */

  private GanymedeSession gSession = null;

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
   *
   * <p>Note that this only works because we serialize log commit.</p>
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
   * This method closes out the log file.
   */

  public synchronized void close() throws IOException
  {
    try
      {
        try
          {
            logController.close();
          }
        finally
          {
            try
              {
                if (mailController != null)
                  {
                    mailController.close();
                  }
              }
            finally
              {
                if (mailer != null)
                  {
                    mailer.close(); // we'll block here while the mailer's email thread drains
                  }
              }
          }
      }
    finally
      {
        closed = true;
      }
  }

  /**
   * This method sends out a generic mail message that will not be logged.
   *
   * @param recipients a List of email addresses to send this
   * message to.  Should never be null or empty.
   *
   * @param title The email subject for this message, will have the
   * Ganymede.subjectPrefixProperty prepended to it.
   *
   * @param description The message itself
   */

  public void sendMail(List<String> recipients, String title, String description)
  {
    this.sendMail(recipients, title, description, MailMode.NONE, null);
  }

  /**
   * <p>This method sends out a generic mail message that will not be
   * logged.  According to the mode enum value, mail may be sent to
   * email addresses associated with the objects in the invids List,
   * in addition to the recipients list.</p>
   *
   * @param recipients a List of email addresses to send this message
   * to.  May legally be null or empty, in which case mail will be sent
   * to anyone needed according to the mode parameter.
   * @param title The email subject for this message, will have the
   * Ganymede.subjectPrefixProperty prepended to it.
   * @param description The message itself
   * @param mode Enum value controlling whether and how to send email
   * to parties connected to the objects referenced by the contents of
   * invids.
   * @param invids A List of Invids to consult for possible mail targeting
   */

  public void sendMail(List<String> recipients,
                       String title, String description,
                       MailMode mode, List<Invid> invids)
  {
    DBLogEvent event;

    /* -- */

    // create a log event

    event = new DBLogEvent("mailout", description, null, null, invids, recipients);

    // we've already put the description in the event, don't need
    // to provide a separate description string to mailNotify

    this.mailNotify(title, null, event, mode, null);
  }

  /**
   * <p>This method is used to handle an email notification event, where
   * the mail title should reflect detailed information about the
   * event, and extra descriptive information is to be sent out.</p>
   *
   * <p>mailNotify() will send the message to the owners of any objects
   * referenced by event if mode so specifies.</p>
   *
   * <p>description and/or title may be null, in which case the proper
   * strings will be extracted from the event's database record</p>
   *
   * @param title The email subject for this message, will have the
   * Ganymede.subjectPrefixProperty prepended to it.
   * @param description The message itself
   * @param event A single event to be logged, with its own timestamp.
   * @param mode Enum value controlling whether and how to send email
   * to parties connected to the objects referenced by this transaction.
   * @param session The DBSession used to reference this transaction.
   */

  public synchronized void mailNotify(String title, String description,
                                      DBLogEvent event, MailMode mode,
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

    calculateMailTargets(event, session, null, mode);

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
            // "DBLog.mailNotify(): Skipping logging mailout event ({0}) to disk due to mail logging being disabled at startup."
            Ganymede.debug(ts.l("mailNotify.no_mail", title));
          }
      }
    else
      {
        logController.writeEvent(event);

        type = sysEventCodes.get(event.eventClassToken);

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
            message = event.description;
          }
        else
          {
            message = type.description + "\n\n" + event.description ;
          }

        if (description != null)
          {
            message = message + description;
          }

        message = StringUtils.ensureEndsWith(message, "\n\n");

        message = arlut.csd.Util.WordWrap.wrap(message, 78);

        // the signature is pre-wrapped

        message = message + signature;

        // get our list of recipients from the event's enumerated list of recipients
        // and the event code's address list.

        List<String> emailList;

        if (type == null)
          {
            emailList = event.getMailTargets();
          }
        else
          {
            Set<String> addressSet = new HashSet<String>(event.getMailTargets());
            addressSet.addAll(type.addressList);

            emailList = new ArrayList<String>(addressSet);
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

        // bombs away!

        mailer.sendmsg(Ganymede.returnaddrProperty,
                       emailList,
                       Ganymede.returnaddrdescProperty,
                       titleString,
                       message);
      }

    if (debug)
      {
        System.err.println("Completed emailing log event " + event.eventClassToken);
      }
  }

  /**
   * <p>This method is used to log an event such as server shutdown/restart,
   * user log-in, persona change, etc.  Basically any thing not associated
   * with a transaction.</p>
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
   * <p>This method is used to start logging events for a transaction.  It is called from
   * {@link arlut.csd.ganymede.server.DBEditSet#commit_logTransaction(java.util.Set)},
   * which is responsible for sequencing this call with calls to streamLogEvent() and
   * endTransactionLog().</p>
   *
   * <p>DBEditSet.commit_logTransaction() is responsible for
   * synchronizing on Ganymede.log, and thereby excluding all other
   * synchronized log calls from being initiated during a
   * transaction's commit.</p>
   *
   * @param invids a List of Invid objects modified by this transaction
   * @param adminName Human readable string identifying the admin responsible for this transaction
   * @param admin Invid representing the user or admin responsible for this transaction
   * @param comment If not null, a comment to attach to logging and email generated in response to this transaction.
   * @param transaction The {@link arlut.csd.ganymede.server.DBEditSet} representing the transaction to be logged
   */

  public synchronized void startTransactionLog(List<Invid> invids, String adminName, Invid admin, String comment, DBEditSet transaction)
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

    this.transactionControl = sysEventCodes.get("starttransaction");
  }

  /**
   * <p>This method should only be called after a
   * startTransactionLog() call and before the corresponding
   * endTransactionLog() call, made by {@link
   * arlut.csd.ganymede.server.DBEditSet#commit_logTransaction(java.util.Set)}.</p>
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

            // first, if we have a recognizable object-specific event
            // happening, queue up notification for it to any interested
            // parties, for later transmission with sendObjectMail().

            Set<String> sentTo = new HashSet<String>(appendObjectMail(event, this.objectOuts,
                                                                      transaction.description,
                                                                      transaction.session));

            // we may have a system event instead, in which case we handle
            // mailing it here

            sentTo.addAll(sendSysEventMail(event, transaction.description));

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

            sentTo.addAll(appendMailOut(event,
                                        this.transactionMailOuts,
                                        transaction.session,
                                        this.transactionControl));

            // and we want to make sure and send this event to any
            // addresses listed in the starttransaction system event
            // object.

            sentTo.addAll(this.transactionControl.addressList);

            sentTo = cleanupAddresses(sentTo);

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
            String returnAddrDesc = null;

            // who should we say the mail is from?

            if (event.admin != null)
              {
                returnAddr = adminPersonaCustom.convertAdminInvidToString(event.admin,
                                                                          transaction.session);
              }
            else
              {
                returnAddr = Ganymede.returnaddrProperty;
                returnAddrDesc = Ganymede.returnaddrdescProperty;
              }

            String message = event.description;

            message = arlut.csd.Util.WordWrap.wrap(message, 78);

            if (this.transactionComment != null)
              {
                // "{0}\n----\n\n{1}\n\n{2}"
                message = ts.l("streamEvent.comment_template", message, this.transactionComment, signature);
              }
            else
              {
                // "{0}\n{1}"
                message = ts.l("streamEvent.no_comment_template", message, signature);
              }

            // bombs away!

            mailer.sendmsg(returnAddr,
                           event.getMailTargets(),
                           returnAddrDesc,
                           Ganymede.subjectPrefixProperty + event.subject,
                           message);
          }
      }

    // and write it to our log.  NB this has to happen last so that we
    // can record in the event the list of email addresses the event
    // was sent to in our log.

    logController.writeEvent(event);
  }

  /**
   * <p>This method should only be called after a
   * startTransactionLog() call and any corresponding
   * endTransactionLog() calls, made by {@link
   * arlut.csd.ganymede.server.DBEditSet#commit_logTransaction(java.util.Set)}.</p>
   *
   * <p>DBEditSet.commit_logTransaction() is responsible for
   * synchronizing on Ganymede.log, and thereby excluding all other
   * synchronized log calls from being initiated during a
   * transaction's commit, when calling this function</p>
   *
   * @param invids a List of Invid objects modified by this transaction
   * @param adminName Human readable string identifying the admin responsible for this transaction
   * @param admin Invid representing the user or admin responsible for this transaction
   * @param transaction The {@link arlut.csd.ganymede.server.DBEditSet} representing the transaction to be logged
   */

  public synchronized void endTransactionLog(List<Invid> invids, String adminName, Invid admin, DBEditSet transaction)
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
    String returnAddrDesc = null;

    // who should we say the mail is from?

    if (admin != null)
      {
        returnAddr = adminPersonaCustom.convertAdminInvidToString(admin,
                                                                  gSession.getDBSession());
        returnAddrDesc = returnAddr;
      }

    // if there was no email address registered for the admin persona,
    // use the return address listed in the ganymede.properties file

    if (returnAddr == null)
      {
        returnAddr = Ganymede.returnaddrProperty;
        returnAddrDesc = Ganymede.returnaddrdescProperty;
      }

    // send out object event reporting mail to anyone who has signed up for it

    if (mailer != null)
      {
        sendObjectMail(returnAddr, adminName, returnAddrDesc, this.objectOuts, this.transactionTimeStamp, transaction);
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
                // "Transaction summary: User {0} {1,date,EEE MMM dd HH:mm:ss zzz yyyy}\n\n----\n\n{2}\n\n{3}{4}"
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
                System.err.println("Sending mail to " + mailout.addresses.get(0));
              }

            mailer.sendmsg(returnAddr,
                           mailout.addresses,
                           returnAddrDesc,
                           Ganymede.subjectPrefixProperty + describeTransaction(mailout, transaction),
                           description);
          }
      }

    logController.flushAndSync();

    this.transactionMailOuts.clear();
    this.transactionMailOuts = null;
  }

  /**
   * <p>Emergency cleanup function called by {@link
   * arlut.csd.ganymede.server.DBEditSet#commit_logTransaction(java.util.Set)} in
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
   * <p>This method is used to scan the log for log events that match
   * invid and that have occurred since sinceTime.</p>
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
   * <p>This sends out system ("global") event mail to the appropriate
   * users, based on the system event record's flags.</p>
   *
   * @return List of email addresses this event was sent to for system
   * event notification.  All email addresses in this List will have
   * been normalized with the contents of the ganymede.defaultdomain
   * property, if set.
   */

  private List<String> sendSysEventMail(DBLogEvent event, String transdescrip)
  {
    systemEventType type;
    String returnAddr = null;
    String returnAddrDesc = null;
    List<String> emailList = new ArrayList<String>();

    /* -- */

    // If we're suppressing sending out all email, then do a no-op

    if (mailer == null)
      {
        return emailList;
      }

    updateSysEventCodeHash();

    type = sysEventCodes.get(event.eventClassToken);

    if (type == null || !type.mail)
      {
        return emailList;
      }

    Set<String> addressSet = new HashSet<String>();

    if (debug)
      {
        System.err.println("Attempting to email log event " + event.eventClassToken);
      }

    // prepare our message, word wrap it

    String message;

    if (transdescrip != null && (!transdescrip.equals("null")))
      {
        message = transdescrip + "\n\n" + type.description + "\n\n" + event.description;
      }
    else if (type.description != null && (!type.description.equals("")))
      {
        message = type.description + "\n\n" + event.description;
      }
    else
      {
        message = event.description;
      }

    message = StringUtils.ensureEndsWith(message, "\n\n");

    message = arlut.csd.Util.WordWrap.wrap(message, 78);

    message = message + signature;

    // get our list of recipients

    addressSet.addAll(event.getMailTargets());

    if (type.addressList != null)
      {
        addressSet.addAll(type.addressList);
      }

    if (type.ccToSelf)
      {
        String name = null;

        if (event.admin != null)
          {
            name = adminPersonaCustom.convertAdminInvidToString(event.admin, gSession.getDBSession());
          }
        else
          {
            // If we're reporting a bad login, we'll try to ccToSelf
            // even if we don't have u reasonable user/admin Invid for
            // this event.
            //
            // This will let us send warning mail to users who enter
            // their username/admin name correctly, but muff their
            // password.

            if (event.eventClassToken.equals("badpass"))
              {
                name = event.adminName;

                if (name != null)
                  {
                    // skip any persona info after a colon in case the
                    // user tried logging in with admin privileges

                    if (name.indexOf(':') != -1)
                      {
                        name = name.substring(0, name.indexOf(':'));
                      }

                    // don't bother trying to send mail if the username
                    // attempted has a space in it, we know that won't fly
                    // as valid email address.

                    if (name.indexOf(' ') != -1)
                      {
                        name = null;
                      }
                  }
              }
          }

        if (name != null)
          {
            addressSet.add(name);
          }
      }

    if (type.ccToOwners)
      {
        addressSet.addAll(calculateOwnerAddresses(event.getInvids(), MailMode.BOTH));
      }

    // who should we say the mail is from?

    if (event.admin != null)
      {
        returnAddr = adminPersonaCustom.convertAdminInvidToString(event.admin,
                                                                  gSession.getDBSession());
        returnAddrDesc = returnAddr;
      }
    else
      {
        returnAddr = Ganymede.returnaddrProperty;
        returnAddrDesc = Ganymede.returnaddrdescProperty;
      }

    // and now..

    emailList.addAll(cleanupAddresses(addressSet));

    // bombs away!

    mailer.sendmsg(returnAddr,
                   emailList,
                   returnAddrDesc,
                   Ganymede.subjectPrefixProperty + type.name,
                   message);

    if (debug)
      {
        System.err.println("Completed emailing log event " + event.eventClassToken);
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
   * @return List of email addresses this event will be sent to for
   * object event notification.
   */

  private List<String> appendObjectMail(DBLogEvent event,
                                        HashMap<String, HashMap<String, MailOut>> objectOuts,
                                        String transdescrip,
                                        DBSession transSession)
  {
    if (event == null || event.getInvids() == null || event.getInvids().size() != 1)
      {
        return Collections.emptyList();
      }

    // --

    Set<String> mailSet = new HashSet<String>();
    Invid objectInvid = event.getInvids().get(0);
    String key = event.eventClassToken + ":" + objectInvid.getType(); // pattern to match
    objectEventType type = objEventCodes.get(key); // does anyone care about this pattern?

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

        return Collections.emptyList();
      }

    // ok.  now we've got an objectEventType, so check to see who we
    // want to send mail to for this event.

    if (type.ccToSelf)
      {
        if (event.admin != null)
          {
            String name = adminPersonaCustom.convertAdminInvidToString(event.admin, transSession);

            if (name != null)
              {
                mailSet.add(name);
              }
          }
      }

    if (type.ccToOwners)
      {
        mailSet.addAll(calculateOwnerAddresses(event.getInvids(), MailMode.BOTH, transSession));
      }

    mailSet.addAll(type.addressList);

    if (mailSet.size() == 0)
      {
        return Collections.emptyList();
      }

    mailSet = cleanupAddresses(mailSet);

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

    for (String address: mailSet)
      {
        MailOut mailout = addresses.get(address);

        if (mailout == null)
          {
            mailout = new MailOut(address);
            addresses.put(address, mailout);
          }

        mailout.append(event);
      }

    return new ArrayList<String>(mailSet);
  }

  /**
   * Send out type-specific email notifications to email addresses
   * that have signed up for per-object-type mail notifications.
   */

  private void sendObjectMail(String returnAddr, String adminName, String returnAddrDesc, HashMap<String, HashMap<String, MailOut>> objectOuts, Date currentTime, DBEditSet transaction)
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
                // "{0} summary: User {1} {2,date,EEE MMM dd HH:mm:ss zzz yyyy}\n\n----\n\n{3}\n\n{4}{5}"
                description = ts.l("sendObjectMail.comment_template",
                                   type.name,
                                   adminName,
                                   currentTime,
                                   this.transactionComment,
                                   arlut.csd.Util.WordWrap.wrap(mailout.toString(), 78),
                                   signature);
              }

            String title;

            if (mailout.entryCount < 5)
              {
                if (type.name != null)
                  {
                    title = Ganymede.subjectPrefixProperty + type.name;
                  }
                else
                  {
                    title = Ganymede.subjectPrefixProperty + type.token;
                  }

                List<String> name_list = new ArrayList<String>();

                for (Invid invid: mailout.getInvids())
                  {
                    DBEditObject object = transaction.findObject(invid);

                    name_list.add(object.getLabel());
                  }

                if (name_list.size() > 0)
                  {
                    title = title + " (\"" + VectorUtils.vectorString(name_list, ", ") + "\")";
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

            mailer.sendmsg(returnAddr,
                           mailout.addresses,
                           returnAddrDesc,
                           title,
                           description);
          }
      }
  }

  /**
   * <p>Private helper method to (re)initialize our local hash of system
   * event codes.</p>
   */

  private void updateSysEventCodeHash()
  {
    List<Result> eventCodeList;

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

        if (!eventBase.changedSince(sysEventCodesTimeStamp))
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

    eventCodeList = gSession.internalQuery(new Query(SchemaConstants.EventBase));

    if (eventCodeList == null)
      {
        Ganymede.debug("DBLog.updateSysEventCodeHash(): no event records found in database");
        return;
      }

    for (Result entry: eventCodeList)
      {
        sysEventCodes.put(entry.toString(),
                          new systemEventType(gSession.getDBSession().viewDBObject(entry.getInvid())));
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
   * <p>Private helper method to (re)initialize our local hash of object
   * event codes.</p>
   */

  private void updateObjEventCodeHash()
  {
    List<Result> eventCodeList;
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

        if (!eventBase.changedSince(objEventCodesTimeStamp))
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

    eventCodeList = gSession.internalQuery(new Query(SchemaConstants.ObjectEventBase));

    if (eventCodeList == null)
      {
        Ganymede.debug("DBLog.updateObjEventCodeHash(): no event records found in database");
        return;
      }

    for (Result entry: eventCodeList)
      {
        objEventobj = gSession.getDBSession().viewDBObject(entry.getInvid());
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
   * <p>This method generates a list of additional email addresses that
   * notification for this event should be sent to, based on the
   * event's type and the ownership of objects referenced by this
   * event.</p>
   *
   * <p>Note that the email addresses added to this event's mail list
   * will be in addition to any that were previously specified by the
   * code that originally generated the log event.</p>
   */

  private void calculateMailTargets(DBLogEvent event, DBSession session,
                                    systemEventType eventType, MailMode mode)
  {
    Set<String> mailSet = new HashSet<String>();

    /* -- */

    if (session == null)
      {
        session = gSession.getDBSession();
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
        mailSet.addAll(event.getMailTargets());
        mailSet.addAll(calculateOwnerAddresses(event.getInvids(), mode, session));
      }
    else if (eventType.ccToOwners)
      {
        mailSet.addAll(event.getMailTargets());
        mailSet.addAll(calculateOwnerAddresses(event.getInvids(), MailMode.BOTH, session));
      }

    if (eventType == null || eventType.ccToSelf)
      {
        // always include the email address for the admin who
        // initiated the action.

        if (event.admin != null)
          {
            mailSet.add(adminPersonaCustom.convertAdminInvidToString(event.admin,
                                                                     session));
          }
      }

    mailSet = cleanupAddresses(mailSet);

    event.setMailTargets(mailSet);

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
   * <p>This method takes a DBLogEvent object, scans it to determine
   * what mailing lists should be notified of the event in the
   * context of a transaction, and adds a description of the
   * passed in event to each {@link arlut.csd.ganymede.server.MailOut MailOut}
   * object held in map.</p>
   *
   * <p>That is, the map passed in maps each discrete recipient
   * list to a running MailOut object which has the complete
   * text that will be mailed to that recipient when the
   * transaction's records are mailed out.</p>
   *
   * @return List of email addresses this event was sent to for
   * system event notification
   */

  private List<String> appendMailOut(DBLogEvent event, HashMap<String, MailOut> map,
                                     DBSession session, systemEventType transactionType)
  {
    Iterator iter;
    MailOut mailout;

    /* -- */

    if (transactionType == null || transactionType.ccToOwners)
      {
        calculateMailTargets(event, session, transactionType, MailMode.BOTH);
      }
    else
      {
        calculateMailTargets(event, session, transactionType, MailMode.NONE);
      }

    for (String str: event.getMailTargets())
      {
        if (debug)
          {
            System.err.println("Going to be mailing to " + str);
          }

        if (str == null)
          {
            // if a custom DBEditObject subclass includes a null value
            // in a List<String>, we don't want to choke on it.

            continue;
          }

        mailout = (MailOut) map.get(str);

        if (mailout == null)
          {
            mailout = new MailOut(str);
            map.put(str, mailout);
          }

        mailout.append(event);
      }

    return event.getMailTargets();
  }

  /**
   * This method gets the signature file loaded.
   */

  private void loadSignature() throws IOException
  {
    StringBuilder buffer = new StringBuilder();
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
   * <p>This method takes a List of {@link
   * arlut.csd.ganymede.common.Invid Invid}'s representing objects
   * touched during a transaction, and returns a Set of email
   * addresses that should be notified of operations affecting the
   * objects in the &lt;objects&gt; list.</p>
   */

  public Set<String> calculateOwnerAddresses(List<Invid> objects, MailMode mode)
  {
    return DBLog.calculateOwnerAddresses(objects, mode, gSession.getDBSession());
  }

  //
  //
  //
  // STATIC methods
  //
  //
  //

  /**
   * <p>This method takes a List of {@link
   * arlut.csd.ganymede.common.Invid Invid}'s representing objects
   * touched during a transaction, and returns a Set of email
   * addresses that should be notified of operations affecting the
   * objects in the &lt;objects&gt; list.</p>
   */

  static public Set<String> calculateOwnerAddresses(List<Invid> objects, DBSession session)
  {
    return calculateOwnerAddresses(objects, MailMode.BOTH, session);
  }

  /**
   * <p>This method takes a List of {@link
   * arlut.csd.ganymede.common.Invid Invid}'s representing objects
   * touched during a transaction, and returns a Set of email
   * addresses that should be notified of operations affecting the
   * objects in the &lt;objects&gt; list.</p>
   */

  static public Set<String> calculateOwnerAddresses(List<Invid> objects, MailMode mode, DBSession session)
  {
    Set<Invid> ownerGroupInvids = new HashSet<Invid>();
    Set<String> addresses = new HashSet<String>();

    /* -- */

    if (debug)
      {
        System.err.println("DBLog.java: calculateOwnerAddresses");
      }

    if (objects == null)
      {
        return addresses;
      }

    for (Invid invid: objects)
      {
        DBObject object = session.viewDBObject(invid);

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

        if (mode == MailMode.USERS || mode == MailMode.BOTH)
          {
            if (object.hasEmailTarget())
              {
                List<String> targets = (List<String>) object.getEmailTargets();

                if (targets != null)
                  {
                    addresses.addAll(targets);
                  }
              }

            // get the email targets from the original version of the
            // object, if present, in case our transaction changed the
            // object's list of addressees along the way

            if (object instanceof DBEditObject)
              {
                DBEditObject eObject = (DBEditObject) object;

                switch (eObject.getStatus())
                  {
                  case ObjectStatus.DELETING:
                  case ObjectStatus.EDITING:
                    DBObject originalObject = eObject.getOriginal();

                    if (originalObject.hasEmailTarget())
                      {
                        List<String> targets = (List<String>) originalObject.getEmailTargets();

                        if (targets != null)
                          {
                            addresses.addAll(targets);
                          }
                      }
                  }
              }
          }

        // Do we need to notify the owners?

        if (mode != MailMode.OWNERS && mode != MailMode.BOTH)
          {
            continue;           // Nope
          }

        // yep, we need to notify the owners

        Set<DBObject> objectVersions = new HashSet<DBObject>();

        // if we're working with a DBEditObject, we may need to
        // consider the original version of the object, the modified
        // version of the object, both, or neither.

        if (object instanceof DBEditObject)
          {
            DBEditObject eObject = (DBEditObject) object;

            switch (eObject.getStatus())
              {
              case ObjectStatus.CREATING:
                objectVersions.add(eObject);
                break;

              case ObjectStatus.DELETING:
                objectVersions.add(eObject.getOriginal());
                break;

              case ObjectStatus.EDITING:
                objectVersions.add(eObject);
                objectVersions.add(eObject.getOriginal());
                break;

              case ObjectStatus.DROPPING:
                // no point in logging a transient object
                break;
              }
          }
        else
          {
            objectVersions.add(object);
          }

        DBObject[] versionArray = objectVersions.toArray(new DBObject[0]);

        for (int i = 0; i < versionArray.length; i++)
          {
            if (versionArray[i].isEmbedded())
              {
                objectVersions.remove(versionArray[i]);

                try
                  {
                    objectVersions.add(session.getGSession().getContainingObj(versionArray[i]));
                  }
                catch (IntegrityConstraintException ex)
                  {
                    Ganymede.debug("Couldn't find container for " + versionArray[i].getLabel());

                    continue;
                  }
              }
          }

        for (DBObject versionOfObject: objectVersions)
          {
            // get a list of owner invids for this object

            InvidDBField ownersField = versionOfObject.getInvidField(SchemaConstants.OwnerListField);

            if (ownersField == null)
              {
                if (debug)
                  {
                    System.err.println("calculateOwnerAddresses(): disregarding supergash-owned invid " +
                                       invid.toString());
                  }

                continue;
              }

            ownerGroupInvids.addAll((List<Invid>) ownersField.getValuesLocal());
          }
      }

    // okay, we have a set of owner invids for all of the objects.
    // For each of these owners, we need to see what email lists and
    // addresses are to receive notification

    for (Invid ownerInvid: ownerGroupInvids)
      {
        if (debug)
          {
            System.err.println("DBLog.calculateOwnerAddresses(): processing owner group " +
                               session.getGSession().getDBSession().getObjectLabel(ownerInvid));
          }

        addresses.addAll(ownerCustom.getAddresses(ownerInvid, session));
      }

    return addresses;
  }

  /**
   * <p>Takes a Set of email addresses and returns a Set of the same
   * addresses after cleaning.</p>
   *
   * <p>Cleaning consists of making sure that any naked email
   * addresses (i.e., without an @ sign or hostname) have the Ganymede
   * server's default domain, if any, appended.  This may reduce the
   * number of addresses in the resulting Set if the parameter Set had
   * both naked and default domain variants of the same address.</p>
   *
   * <p>To set the default domain for a Ganymede server, set the
   * 'ganymede.defaultdomain' property in the server's
   * ganymede.properties file.</p>
   */

  public static Set<String> cleanupAddresses(Set<String> addresses)
  {
    if (Ganymede.defaultDomainProperty == null)
      {
        return addresses;
      }

    Set<String> results = new HashSet<String>();

    for (String address: addresses)
      {
        if (address.indexOf('@') != -1)
          {
            results.add(address);
          }
        else
          {
            results.add(address + "@" + Ganymede.defaultDomainProperty);
          }
      }

    return results;
  }

  /**
   * Synthesize a descriptive subject for transaction summary email.
   */

  private static String describeTransaction(MailOut mailOut, DBEditSet transaction)
  {
    int count = 0;

    for (Invid invid: mailOut.getInvids())
      {
        DBObjectBase base = Ganymede.db.getObjectBase(invid.getType());

        if (base.isEmbedded())
          {
            continue;
          }

        count++;
      }

    if (count < 5)
      {
        return describeSmallTransaction(mailOut, transaction);
      }
    else
      {
        return describeLargeTransaction(mailOut, transaction);
      }
  }

  /**
   * <p>describeSmallTransaction provides a subject line with the types
   * and names of the objects created, modified, or deleted by
   * transaction.</p>
   *
   * <p>describeSmallTransaction should be used instead of
   * describeLargeTransaction if the number of events in a transaction
   * is small enough that a more complete summary can reasonably be
   * included in the generated email subject.</p>
   */

  private static String describeSmallTransaction(MailOut mailOut, DBEditSet transaction)
  {
    StringBuilder subject = new StringBuilder();
    Set<String> types = new TreeSet<String>();

    for (Invid invid: mailOut.getInvids())
      {
        DBObjectBase base = Ganymede.db.getObjectBase(invid.getType());

        if (base.isEmbedded())
          {
            continue;
          }

        types.add(base.getName());
      }

    // in describeSmallTransaction we have three loops, over the
    // action done to the objects, over their types, and finally over
    // the items themselves
    //
    // group by edit mode (0 = creating, 1 = deleting, 2 = editing)
    //
    // we try to sort these with the most significant/unusual up front
    // on the subject line

    for (int mode = 0; mode < 3; mode++)
      {
        boolean declared_action = false;

        // group by type

        for (String type: types)
          {
            DBObjectBase base = Ganymede.db.getObjectBase(type);
            boolean declared_type = false;

            // finally loop over the actual invids, which we'll
            // include or disinclude based on the outer loops

            for (Invid invid: mailOut.getInvids())
              {
                if (invid.getType() == base.getTypeID())
                  {
                    DBEditObject object = transaction.findObject(invid);

                    switch (object.getStatus())
                      {
                      case ObjectStatus.CREATING:
                        if (mode == 0)
                          {
                            if (!declared_action)
                              {
                                if (subject.length() > 0)
                                  {
                                    // ". "
                                    subject.append(ts.l("describeSmallTransaction.append"));
                                  }

                                // "Created {0} "{1}"
                                subject.append(ts.l("describeSmallTransaction.creation_first",
                                                    base.getName(),
                                                    object.getLabel()));
                                declared_action = true;
                                declared_type = true;
                              }
                            else
                              {
                                if (!declared_type)
                                  {
                                    // ", {0} "{1}"
                                    subject.append(ts.l("describeSmallTransaction.creation_later",
                                                        base.getName(),
                                                        object.getLabel()));
                                    declared_type = true;
                                  }
                                else
                                  {
                                    // ", "{0}"
                                    subject.append(ts.l("describeSmallTransaction.creation_later_sametype",
                                                        object.getLabel()));
                                  }
                              }
                          }
                        break;

                      case ObjectStatus.DELETING:
                        if (mode == 1)
                          {
                            if (!declared_action)
                              {
                                if (subject.length() > 0)
                                  {
                                    // ". "
                                    subject.append(ts.l("describeSmallTransaction.append"));
                                  }

                                // "Deleted {0} "{1}"
                                subject.append(ts.l("describeSmallTransaction.deletion_first",
                                                    base.getName(),
                                                    object.getLabel()));
                                declared_action = true;
                                declared_type = true;
                              }
                            else
                              {
                                if (!declared_type)
                                  {
                                    // ", {0} "{1}"
                                    subject.append(ts.l("describeSmallTransaction.deletion_later",
                                                        base.getName(),
                                                        object.getLabel()));
                                    declared_type = true;
                                  }
                                else
                                  {
                                    // ", "{0}"
                                    subject.append(ts.l("describeSmallTransaction.deletion_later_sametype",
                                                        object.getLabel()));
                                  }
                              }
                          }
                        break;

                      case ObjectStatus.EDITING:
                        if (mode == 2)
                          {
                            if (!declared_action)
                              {
                                if (subject.length() > 0)
                                  {
                                    // ". "
                                    subject.append(ts.l("describeSmallTransaction.append"));
                                  }

                                // "Edited {0} "{1}"
                                subject.append(ts.l("describeSmallTransaction.editing_first",
                                                    base.getName(),
                                                    object.getLabel()));
                                declared_action = true;
                                declared_type = true;
                              }
                            else
                              {
                                if (!declared_type)
                                  {
                                    // ", {0} "{1}"
                                    subject.append(ts.l("describeSmallTransaction.editing_later",
                                                        base.getName(),
                                                        object.getLabel()));
                                    declared_type = true;
                                  }
                                else
                                  {
                                    // ", "{0}"
                                    subject.append(ts.l("describeSmallTransaction.editing_later_sametype",
                                                        object.getLabel()));
                                  }
                              }
                          }
                        break;

                      }
                  }
              }
          }
      }

    if (subject.length() != 0)
      {
        // "."
        subject.append(ts.l("describeSmallTransaction.end_subject"));
      }

    return subject.toString();
  }

  /**
   * <p>describeLargeTransaction provides a subject line with the
   * types and count of objects created, modified, or deleted by
   * transaction, but without the names of the objects that
   * describeSmallTransaction provides.</p>
   */

  private static String describeLargeTransaction(MailOut mailOut, DBEditSet transaction)
  {
    String subject = null;
    Set<String> types = new TreeSet<String>();

    for (Invid invid: mailOut.getInvids())
      {
        DBObjectBase base = Ganymede.db.getObjectBase(invid.getType());

        if (base.isEmbedded())
          {
            continue;
          }

        types.add(base.getName());
      }

    if (types.size() <= 3)
      {
        // prepare a count of create, edit, delete for each type

        for (String type: types)
          {
            DBObjectBase base = Ganymede.db.getObjectBase(type);
            int create = 0;
            int edit = 0;
            int delete = 0;

            for (Invid invid: mailOut.getInvids())
              {
                if (invid.getType() == base.getTypeID())
                  {
                    DBEditObject object = transaction.findObject(invid);

                    switch (object.getStatus())
                      {
                      case ObjectStatus.CREATING:
                        create++;
                        break;

                      case ObjectStatus.EDITING:
                        edit++;
                        break;

                      case ObjectStatus.DELETING:
                        delete++;
                        break;
                      }
                  }
              }

            String createString = null;
            String editString = null;
            String deleteString = null;

            if (create > 0)
              {
                // "Created {0,number}"
                createString = ts.l("describeLargeTransaction.typed_create", create);
              }

            if (edit > 0)
              {
                // "Edited {0, number}"
                editString = ts.l("describeLargeTransaction.typed_edit", edit);
              }

            if (delete > 0)
              {
                // "Deleted {0, number}"
                deleteString = ts.l("describeLargeTransaction.typed_delete", delete);
              }

            int paramCount = (createString != null ? 1 : 0) +
              (editString != null ? 1 : 0) +
              (deleteString != null ? 1 : 0);

            String objectSummary = null;

            switch (paramCount)
              {
              case 1:
                if (createString != null)
                  {
                    // "{0} {1} objects."
                    objectSummary = ts.l("describeLargeTransaction.typed_subject_template", createString, type.toLowerCase());
                  }
                else if (editString != null)
                  {
                    // "{0} {1} objects."
                    objectSummary = ts.l("describeLargeTransaction.typed_subject_template", editString, type.toLowerCase());
                  }
                else if (deleteString != null)
                  {
                    // "{0} {1} objects."
                    objectSummary = ts.l("describeLargeTransaction.typed_subject_template", deleteString, type.toLowerCase());
                  }

                break;

              case 2:
                if (createString == null)
                  {
                    // "{0} {1} objects."
                    // "{0}, {1}"
                    objectSummary = ts.l("describeLargeTransaction.typed_subject_template",
                                         ts.l("describeLargeTransaction.typed_subject_duplex_pattern", deleteString, editString),
                                         type.toLowerCase());
                  }
                else if (editString == null)
                  {
                    // "{0} {1} objects."
                    // "{0}, {1}"
                    objectSummary = ts.l("describeLargeTransaction.typed_subject_template",
                                         ts.l("describeLargeTransaction.typed_subject_duplex_pattern", createString, deleteString),
                                         type.toLowerCase());
                  }
                else if (deleteString == null)
                  {
                    // "{0} {1} objects."
                    // "{0}, {1}"
                    objectSummary = ts.l("describeLargeTransaction.typed_subject_template",
                                         ts.l("describeLargeTransaction.typed_subject_duplex_pattern", createString, editString),
                                         type.toLowerCase());
                  }

                break;

              case 3:
                // "{0} {1} objects."
                // "{0}, {1}, {2}"
                objectSummary = ts.l("describeLargeTransaction.typed_subject_template",
                                     ts.l("describeLargeTransaction.typed_subject_triplex_pattern", createString, deleteString, editString),
                                     type.toLowerCase());
              }

            if (subject == null)
              {
                subject = objectSummary;
              }
            else
              {
                // concatenation between object types

                subject = ts.l("describeLargeTransaction.concatenation", subject, objectSummary);
              }
          }
      }
    else
      {
        // prepare a count of create, edit, delete

        int create = 0;
        int edit = 0;
        int delete = 0;

        for (Invid invid: mailOut.getInvids())
          {
            DBEditObject object = transaction.findObject(invid);

            switch (object.getStatus())
              {
              case ObjectStatus.CREATING:
                create++;
                break;

              case ObjectStatus.EDITING:
                edit++;
                break;

              case ObjectStatus.DELETING:
                delete++;
                break;
              }
          }

        String createString = null;
        String editString = null;
        String deleteString = null;

        if (create > 0)
          {
            // "Created {0,number}"
            createString = ts.l("describeLargeTransaction.create", create);
          }

        if (edit > 0)
          {
            // "Edited {0, number}"
            editString = ts.l("describeLargeTransaction.edit", edit);
          }

        if (delete > 0)
          {
            // "Deleted {0, number}"
            deleteString = ts.l("describeLargeTransaction.delete", delete);
          }

        int paramCount = (createString != null ? 1 : 0) +
          (editString != null ? 1 : 0) +
          (deleteString != null ? 1 : 0);

        String objectSummary = null;

        switch (paramCount)
          {
          case 1:
            if (createString != null)
              {
                // "{0} {1} objects."
                objectSummary = ts.l("describeLargeTransaction.subject_template", createString);
              }
            else if (editString != null)
              {
                // "{0} {1} objects."
                objectSummary = ts.l("describeLargeTransaction.subject_template", editString);
              }
            else if (deleteString != null)
              {
                // "{0} {1} objects."
                objectSummary = ts.l("describeLargeTransaction.subject_template", deleteString);
              }

            break;

          case 2:
            if (createString == null)
              {
                // "{0} objects."
                // "{0}, {1}"
                objectSummary = ts.l("describeLargeTransaction.subject_template",
                                     ts.l("describeLargeTransaction.subject_duplex_pattern", deleteString, editString));
              }
            else if (editString == null)
              {
                // "{0} objects."
                // "{0}, {1}"
                objectSummary = ts.l("describeLargeTransaction.subject_template",
                                     ts.l("describeLargeTransaction.subject_duplex_pattern", createString, deleteString));
              }
            else if (deleteString == null)
              {
                // "{0} objects."
                // "{0}, {1}"
                objectSummary = ts.l("describeLargeTransaction.subject_template",
                                     ts.l("describeLargeTransaction.subject_duplex_pattern", createString, editString));
              }

            break;

          case 3:
            // "{0} objects."
            // "{0}, {1}, {2}"
            objectSummary = ts.l("describeLargeTransaction.subject_template",
                                 ts.l("describeLargeTransaction.subject_triplex_pattern", createString, deleteString, editString));
          }

        subject = objectSummary;
      }

    return subject;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 systemEventType

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to store system event information derived from
 * the Ganymede database.</p>
 */

class systemEventType {

  String name;
  String description;
  boolean mail;
  List<String> addressList;
  boolean ccToSelf;
  boolean ccToOwners;

  private DBField f;

  /* -- */

  systemEventType(DBObject obj)
  {
    name = getString(obj, SchemaConstants.EventName);
    description = getString(obj, SchemaConstants.EventDescription);
    mail = getBoolean(obj, SchemaConstants.EventMailBoolean);
    ccToSelf = getBoolean(obj, SchemaConstants.EventMailToSelf);
    ccToOwners = getBoolean(obj, SchemaConstants.EventMailOwners);

    // and calculate the addresses that always need to be notified
    // of this type of system event

    addressList = getAddresses(obj);
  }

  private String getString(DBObject obj, short fieldId)
  {
    f = obj.getField(fieldId);

    if (f == null)
      {
        return "";
      }

    return (String) f.getValueLocal();
  }

  private boolean getBoolean(DBObject obj, short fieldId)
  {
    f = obj.getField(fieldId);

    if (f == null)
      {
        return false;
      }

    return ((Boolean) f.getValueLocal()).booleanValue();
  }

  /**
   * <p>This method takes an event definition object and extracts
   * a list of email addresses to which mail will be sent when
   * an event of this type is logged.</p>
   */

  private List<String> getAddresses(DBObject obj)
  {
    StringDBField strF;
    Set<String> set = new HashSet<String>();

    /* -- */

    // Get the list of addresses from the object's external email
    // string list.. we use a Set here so that we don't get
    // duplicates.

    strF = obj.getStringField(SchemaConstants.EventExternalMail);

    if (strF != null)
      {
        set.addAll((List<String>)strF.getValuesLocal());
      }

    set = DBLog.cleanupAddresses(set);

    return new ArrayList<String>(set);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 objectEventType

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to store object event information derived
 * from the Ganymede database for the {@link
 * arlut.csd.ganymede.server.DBLog DBLog} class.</p>
 */

class objectEventType {

  String token;
  short objType;
  String name;
  List<String> addressList;
  boolean ccToSelf;
  boolean ccToOwners;
  String hashKey;

  private DBField f;

  /* -- */

  objectEventType(DBObject obj)
  {
    token = getString(obj, SchemaConstants.ObjectEventToken);
    name = getString(obj, SchemaConstants.ObjectEventName);
    addressList = getAddresses(obj);
    ccToSelf = getBoolean(obj, SchemaConstants.ObjectEventMailToSelf);
    ccToOwners = getBoolean(obj, SchemaConstants.ObjectEventMailOwners);
    objType = (short) getInt(obj, SchemaConstants.ObjectEventObjectType);

    hashKey = token + ":" + objType;
  }

  private String getString(DBObject obj, short fieldId)
  {
    f = obj.getField(fieldId);

    if (f == null)
      {
        return "";
      }

    return (String) f.getValueLocal();
  }

  private boolean getBoolean(DBObject obj, short fieldId)
  {
    f = obj.getField(fieldId);

    if (f == null)
      {
        return false;
      }

    return ((Boolean) f.getValueLocal()).booleanValue();
  }

  private int getInt(DBObject obj, short fieldId)
  {
    f = obj.getField(fieldId);

    // we'll go ahead and throw a NullPointerException if
    // f isn't defined.

    return ((Integer) f.getValueLocal()).intValue();
  }

  /**
   * <p>This method takes an event definition object and extracts
   * a list of email addresses to which mail will be sent when
   * an event of this type is logged.</p>
   */

  private List<String> getAddresses(DBObject obj)
  {
    StringDBField strF;
    Set<String> set = new HashSet<String>();

    /* -- */

    // Get the list of addresses from the object's external email
    // string list.. we a Set here so that we don't get duplicates.

    strF = obj.getStringField(SchemaConstants.ObjectEventExternalMail);

    if (strF != null)
      {
        set.addAll((List<String>)strF.getValuesLocal());
      }

    set = DBLog.cleanupAddresses(set);

    return new ArrayList<String>(set);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                         MailOut

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to store event information derived from the
 * Ganymede database for the {@link arlut.csd.ganymede.server.DBLog
 * DBLog} class.</p>
 */

class MailOut {

  StringBuilder description = new StringBuilder();
  List<String> addresses;
  Set<Invid> invids = new HashSet<Invid>();
  int entryCount = 0;

  /* -- */

  MailOut(String address)
  {
    if (address == null)
      {
        throw new NullPointerException("bad address");
      }

    addresses = new ArrayList<String>();
    addresses.add(address);
  }

  void addEntryCount()
  {
    entryCount++;
  }

  synchronized void append(DBLogEvent event)
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

    StringUtils.ensureEndsWith(description, "\n\n");

    invids.addAll(event.getInvids());
  }

  public Set<Invid> getInvids()
  {
    return invids;
  }

  public String toString()
  {
    return description.toString();
  }
}
