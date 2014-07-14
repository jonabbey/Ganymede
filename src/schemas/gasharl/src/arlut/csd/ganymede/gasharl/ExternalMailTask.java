/*

   ExternalMailTask.java

   This task is run nightly to check all User accounts in Ganymede
   for their external mail access expiration time.

   Created: 19 Oct 2009

   Module By: James Ratcliff, falazar@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.gasharl;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryNode;
import arlut.csd.ganymede.common.QueryAndNode;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DateDBField;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBLog;
import arlut.csd.ganymede.server.DBSession;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeServer;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.StringDBField;
import arlut.csd.ganymede.server.PasswordDBField;
import arlut.csd.Util.RandomUtils;

/*------------------------------------------------------------------------------
                                                                           class
                                                                ExternalMailTask

------------------------------------------------------------------------------*/

/**
 * This task is run nightly to check all user accounts in Ganymede
 * for their External Mail expiration time.
 *
 * @author James Ratcliff falazar@arlut.utexas.edu
 */

public class ExternalMailTask implements Runnable {

  static final boolean debug = true;
  static final public String name = "external mail task";

  /* -- */

  private GanymedeSession mySession = null;
  private DBSession myDBSession = null;
  private Thread currentThread = null;

  /**
   *
   * Just Do It (tm)
   *
   * @see java.lang.Runnable
   *
   */

  public void run()
  {
    currentThread = java.lang.Thread.currentThread();

    Ganymede.debug("External Mail Task: Starting");

    String error = GanymedeServer.checkEnabled();

    if (error != null)
      {
        Ganymede.debug("Deferring External Mail task - semaphore disabled: " + error);
        return;
      }

    try
      {
        try
          {
            mySession = new GanymedeSession("ExternalMailTask");
            myDBSession = mySession.getDBSession();
          }
        catch (RemoteException ex)
          {
            Ganymede.debug("ExternalMail Task: Couldn't establish session");
            return;
          }

        // we don't want no wizards

        mySession.enableWizards(false);

        ReturnVal retVal = mySession.openTransaction(ExternalMailTask.name);

        if (!ReturnVal.didSucceed(retVal))
          {
            Ganymede.debug("ExternalMail Task: Couldn't open transaction");
            return;
          }

        // do the stuff

        checkExpiringCredentials();

        retVal = mySession.commitTransaction();

        if (!ReturnVal.didSucceed(retVal))
          {
            // if doNormalProcessing is true, the
            // transaction was not cleared, but was
            // left open for a re-try.  Abort it.

            if (retVal.doNormalProcessing)
              {
                Ganymede.debug("ExternalMail Task: couldn't fully commit, trying to abort.");

                mySession.abortTransaction();
              }

            Ganymede.debug("ExternalMail Task: Couldn't successfully commit transaction");
          }
        else
          {
            Ganymede.debug("ExternalMail Task: Transaction committed");
          }
      }
    catch (InterruptedException ex)
      {
      }
    catch (NotLoggedInException ex)
      {
      }
    finally
      {
        if (myDBSession.isTransactionOpen())
          {
            Ganymede.debug("ExternalMail Task: Forced to terminate early, aborting transaction");
          }

        mySession.logout();
      }
  }

  /**
   * <p>Checks the users external account expiration date</p>
   *
   * <ul>
   * <li>If 1 month before, we assign a new set of credentials, and save old ones.</li>
   * <li>If 1 day before, we send a warning letter.</li>
   * <li>If expired, we remove old ones, and reset exp date, 6 months ahead.</li>
   * </ul>
   */

  private void checkExpiringCredentials() throws InterruptedException, NotLoggedInException
  {
    Query q;
    DBObject object;
    Date currentTime = new Date();
    Calendar lowerBound = new GregorianCalendar();
    Calendar upperBound = new GregorianCalendar();
    Enumeration en;

    QueryNode matchNode = new QueryAndNode(new QueryDataNode(userSchema.MAILEXPDATE,
                                                             QueryDataNode.DEFINED,
                                                             null),
                                           new QueryDataNode(userSchema.ALLOWEXTERNAL,
                                                             QueryDataNode.DEFINED,
                                                             null));
    /* -- */

    q = new Query(SchemaConstants.UserBase, matchNode, false);

    for (Result result: mySession.internalQuery(q))
      {
        if (currentThread.isInterrupted())
          {
            throw new InterruptedException("scheduler ordering shutdown");
          }

        object = myDBSession.viewDBObject(result.getInvid());

        if (object == null || object.isInactivated())
          {
            continue;
          }

        Date mailExpDate = (Date) object.getFieldValueLocal(userSchema.MAILEXPDATE);

        if (mailExpDate == null)
          {
            continue;
          }

        // four weeks before expiration, assign new credentials and
        // send mail about that.  the old credentials are kept and
        // both are usable until the old ones are actually expired.

        lowerBound.setTime(currentTime);
        lowerBound.add(Calendar.DATE, 27);

        upperBound.setTime(currentTime);
        upperBound.add(Calendar.DATE, 28);

        if (mailExpDate.after(lowerBound.getTime()) && mailExpDate.before(upperBound.getTime()))
          {
            String ckpLabel = "credentialing" + object.getInvid();

            myDBSession.checkpoint(ckpLabel);

            ReturnVal retVal = assignNewCredentials(object);

            if (ReturnVal.didSucceed(retVal))
              {
                myDBSession.popCheckpoint(ckpLabel);
              }
            else
              {
                Ganymede.debug("External Mail Task failure in assignNewCredentials(" + object.getLabel() + ")\n" + retVal.getDialogText());
                myDBSession.rollback(ckpLabel);
              }

            continue;
          }

        // then one day before, we send a warning.

        lowerBound.setTime(currentTime);

        upperBound.setTime(currentTime);
        upperBound.add(Calendar.DATE, 1);

        if (mailExpDate.after(lowerBound.getTime()) && mailExpDate.before(upperBound.getTime()))
          {
            warnPasswordExpire(object);
            continue;
          }

        // on or after the expiration date, we clear the old
        // credentials and assign a new expiration date in the future.

        if (mailExpDate.before(currentTime))
          {
            String ckpLabel = "clearing" + object.getInvid();

            myDBSession.checkpoint(ckpLabel);

            ReturnVal retVal = clearOldCredentials(object);

            if (ReturnVal.didSucceed(retVal))
              {
                myDBSession.popCheckpoint(ckpLabel);
              }
            else
              {
                Ganymede.debug("External Mail Task failure in clearOldCredentials(" + object.getLabel() + ")\n" + retVal.getDialogText());
                myDBSession.rollback(ckpLabel);
              }

            continue;
          }
      }
  }

  /**
   * <p>Assigns a new set of random external credentials and triggers
   * email describing the change to the user.
   *
   * <p>Note that the password text is held in a PasswordDBField, and
   * so would not ordinarily be sent over email through Ganymede's
   * normal transaction email mechanism.  The userCustom class
   * controlling the gasharl User object contains custom logic to
   * describe the credential change and the reason for it in the
   * userCustom.preCommitHook() method.</p>
   */

  private ReturnVal assignNewCredentials(DBObject userObject)
  {
    try
      {
        ReturnVal result = mySession.edit_db_object(userObject.getInvid());

        if (!ReturnVal.didSucceed(result))
          {
            return result;
          }

        DBEditObject editUserObject = (DBEditObject) result.getObject();

        StringDBField usernameField = editUserObject.getStringField(userSchema.MAILUSER);
        PasswordDBField passwordField = editUserObject.getPassField(userSchema.MAILPASSWORD2);

        String username = null;
        String password = null;

        // if we already have MAILUSER and MAILPASSWORD2 fields set,
        // we'll need to remember those values for later

        if (editUserObject.isDefined(userSchema.MAILUSER) &&
            editUserObject.isDefined(userSchema.MAILPASSWORD2))
          {
            username = (String) usernameField.getValueLocal();
            password = passwordField.getPlainText();
          }

        // Set new values.

        result = usernameField.setValueLocal(RandomUtils.getRandomUsername());

        if (!ReturnVal.didSucceed(result))
          {
            return result;
          }

        result = ReturnVal.merge(result, passwordField.setPlainTextPass(RandomUtils.getRandomPassword(20)));

        if (!ReturnVal.didSucceed(result))
          {
            return result;
          }

        // if we had MAILUSER and MAILPASSWORD2 set when we entered,
        // we'll load those old values into the OLDMAILUSER and
        // OLDMAILPASSWORD2 fields for use during the interval between
        // assigning new credentials and revoking the old ones

        if (username != null && password != null)
          {
            StringDBField oldUsernameField = editUserObject.getStringField(userSchema.OLDMAILUSER);
            PasswordDBField oldPasswordField = editUserObject.getPassField(userSchema.OLDMAILPASSWORD2);

            // Copy the (now deprecated) values to the
            // oldUsernameField and oldPasswordField fields

            result = ReturnVal.merge(result, oldUsernameField.setValueLocal(username));

            if (!ReturnVal.didSucceed(result))
              {
                return result;
              }

            result = ReturnVal.merge(result, oldPasswordField.setPlainTextPass(password));
          }

        return result;
      }
    catch (NotLoggedInException ex)
      {
        return Ganymede.loginError(ex);
      }
  }

  /**
   * <p>Send out a warning that the old credentials are going to be removed tomorrow,
   * to the user's and admin groups' email addresses.</p>
   */

  private void warnPasswordExpire(DBObject userObject)
  {
    Vector objVect = new Vector();

    objVect.add(userObject.getInvid());

    String mailUsername = (String) userObject.getFieldValueLocal(userSchema.MAILUSER);
    PasswordDBField mailPasswordField = userObject.getPassField(userSchema.MAILPASSWORD2);
    String mailPassword = mailPasswordField.getPlainText();

    String titleString = "Old External Email Credentials Expiring Very Soon For User " + userObject.getLabel();

    String messageString = "The old external email credentials for user account " + userObject.getLabel() + " will be expiring within 24 hours. \n" +
      "You have been granted access to laboratory email from outside the internal ARL:UT network.\n\n" +
      "In order to send mail from outside the laboratory, you will need to configure your external email client " +
      "to send mail through smail.arlut.utexas.edu using TLS-encrypted SMTP.\n\n" +
      "The user name you should be using for external access is:\n\n" +
      "\tUsername: " + mailUsername + "\n\n" +
      "and the new external access password for your account is:\n\n" +
      "\tPassword: " + mailPassword + "\n\n" +
      "The previously assigned external password will soon cease functioning.\n\n" +
      "You should continue to use your internal email username and password for reading email from mailboxes.arlut.utexas.edu " +
      "via SSL-protected IMAP.";

    Ganymede.log.sendMail(null, titleString, messageString, DBLog.MailMode.USERS, objVect);
  }

  /**
   * <p>Clear out old credentials and reset the expiration date.</p>
   *
   * <p>Send email to the user's and admin groups' email addresses.</p>
   */

  private ReturnVal clearOldCredentials(DBObject userObject)
  {
    try
      {
        ReturnVal result = mySession.edit_db_object(userObject.getInvid());

        if (!ReturnVal.didSucceed(result))
          {
            return result;
          }

        DBEditObject editUserObject = (DBEditObject) result.getObject();

        DateDBField mailExpDateField = editUserObject.getDateField(userSchema.MAILEXPDATE);
        StringDBField oldUsernameField = editUserObject.getStringField(userSchema.OLDMAILUSER);
        PasswordDBField oldPasswordField = editUserObject.getPassField(userSchema.OLDMAILPASSWORD2);

        Calendar myCal = new GregorianCalendar();
        myCal.add(Calendar.DATE, 168); // 24 weeks from today.

        result = mailExpDateField.setValueLocal(myCal.getTime());

        if (!ReturnVal.didSucceed(result))
          {
            return result;
          }

        result = ReturnVal.merge(result, oldUsernameField.setValueLocal(null));

        if (!ReturnVal.didSucceed(result))
          {
            return result;
          }

        result = ReturnVal.merge(result, oldPasswordField.setUndefined(true));

        if (!ReturnVal.didSucceed(result))
          {
            return result;
          }

        Vector objVect = new Vector();

        objVect.add(userObject.getInvid());

        String mailUsername = (String) userObject.getFieldValueLocal(userSchema.MAILUSER);
        PasswordDBField newPasswordField = userObject.getPassField(userSchema.MAILPASSWORD2);
        String mailPassword = newPasswordField.getPlainText();

        String titleString = "External Email Credentials Changed For User " + userObject.getLabel();

        String messageString = "The external email credentials for User account " + userObject.getLabel() + " have been changed. \n" +
          "You have been granted access to laboratory email from outside the internal ARL:UT network.\n\n" +
          "In order to send mail from outside the laboratory, you will need to configure your external email client " +
          "to send email through smail.arlut.utexas.edu using TLS-encrypted SMTP.\n\n" +
          "The user name you should be using for external access is:\n\n" +
          "\tUsername: " + mailUsername + "\n\n" +
          "and the new external access password for your account is:\n\n" +
          "\tPassword: " + mailPassword + "\n\n" +
          "The previously assigned external password will no longer function. \n\n" +
          "You should continue to use your internal email username and password for reading email from mailboxes.arlut.utexas.edu " +
          "via SSL-protected IMAP.";

        Ganymede.log.sendMail(null, titleString, messageString, DBLog.MailMode.USERS, objVect);

        return result;

      }
    catch (NotLoggedInException ex)
      {
        return Ganymede.loginError(ex);
      }
  }
}
