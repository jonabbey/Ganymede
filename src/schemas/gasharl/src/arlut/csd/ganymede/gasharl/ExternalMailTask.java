/*

   ExternalMailTask.java

   This task is run nightly to check all User accounts in Ganymede
   for their external mail access expiration time.
   
   Created: 19 Oct 2009

   Module By: James Ratcliff, falazar@arlut.utexas.edu

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

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeServer;
import arlut.csd.ganymede.server.GanymedeSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                               ExternalMailTask

------------------------------------------------------------------------------*/

/**
 *
 * This task is run nightly to check all user accounts in Ganymede
 * for their External Mail expiration time.
 *
 *
 * @author James Ratcliff falazar@arlut.utexas.edu
 */

public class ExternalMailTask implements Runnable {

  static final boolean debug = true;
  static final public String name = "external mail task";

  /* -- */

  GanymedeSession mySession = null;
  Thread currentThread = null;

  /**
   *
   * Just Do It (tm)
   *
   * @see java.lang.Runnable
   *
   */

  public void run()
  {
    boolean transactionOpen = false;

    /* -- */

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
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("ExternalMail Task: Couldn't establish session");
	    return;
	  }

	// we don't want no wizards

	mySession.enableWizards(false);

	ReturnVal retVal = mySession.openTransaction(ExternalMailTask.name);

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("ExternalMail Task: Couldn't open transaction");
	    return;
	  }

	transactionOpen = true;
	
	// do the stuff

	checkExpiringCredentials();

	retVal = mySession.commitTransaction();

	if (retVal != null && !retVal.didSucceed())
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

	transactionOpen = false;
      }
    catch (InterruptedException ex)
      {
      }
    catch (NotLoggedInException ex)
      {
      }
    finally
      {
	if (transactionOpen)
	  {
	    Ganymede.debug("ExternalMail Task: Forced to terminate early, aborting transaction");
	  }

	mySession.logout();
      }
  }

  /**
  * Checks the users external account expiration date
  * If 1 month before, we assign a new set of credentials, and save old ones.
  * If 1 day before, we send a warning letter.
  * If expired, we remove old ones, and reset exp date, 6 months ahead.
  */
  private void checkExpiringCredentials() throws InterruptedException, NotLoggedInException
  {
    Query q;
    Vector results;
    Result result;
    Invid invid;
    DBObject object;
    Date currentTime = new Date();
    Calendar lowerBound = new GregorianCalendar();
    Calendar upperBound = new GregorianCalendar();
    Enumeration en;

    QueryDataNode matchNode = new QueryDataNode(userSchema.MAILEXPDATE,
						QueryDataNode.DEFINED,
						null);
    /* -- */
    
    q = new Query(SchemaConstants.UserBase, matchNode, false);
    
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

	object = mySession.getSession().viewDBObject(invid);

	if (object.isInactivated())
	  {
	    continue;
	  }

	// get the mail expiration date for this user, if any

	Date mailExpDate = (Date) object.getFieldValueLocal(userSchema.MAILEXPDATE);

	if (mailExpDate == null)
	  {
	    continue;
	  }

	// so, we just go for four weeks of notice

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.DATE, 27);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.DATE, 28);

	if (mailExpDate.after(lowerBound.getTime()) && mailExpDate.before(upperBound.getTime()))
	  {
	    assignNewCredentials(object);
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


	// then we remove old credentials and reset password.

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.DATE, -1);

	upperBound.setTime(currentTime);

	if (mailExpDate.after(lowerBound.getTime()) && mailExpDate.before(upperBound.getTime()))
	  {
	    clearOldCredentials(object);
	    continue;
	  }

      }
  }


  /**
   * <p>Send out a warning that the password is going to expire soon, to the user's
   * and admin groups' email addresses.</p>
   */

  private void warnPasswordExpire(DBObject userObject)
  {
    Date passwordChangeTime = (Date) userObject.getFieldValueLocal(userSchema.PASSWORDCHANGETIME);

    Vector objVect = new Vector();

    objVect.addElement(userObject.getInvid());

    String titleString = "Password Expiring Very Soon For User " + userObject.getLabel() + ", at " + passwordChangeTime;

    String messageString = "The External Account " + userObject.getLabel() + 
      " will expire very soon.  The account will be updated on " + passwordChangeTime +
      " or else the account will be inactivated.\n\n" +
      "If you need assistance with this matter, please contact one of your lab unit's Ganymede administrators, " +
      "or CSD.";

    Ganymede.log.sendMail(null, titleString, messageString, true, true, objVect);
  }


  /**
   * <p>Clear out old credentials and reset the expiration date
   * send email to the user's and admin groups' email addresses.</p>
   */

  private void clearOldCredentials(DBObject userObject)
  {

    // TODO clear out old user and old password
    // reset exp date to 6 months ahead

    Date passwordChangeTime = (Date) userObject.getFieldValueLocal(userSchema.MAILEXPDATE);

    Vector objVect = new Vector();

    objVect.addElement(userObject.getInvid());

    String titleString = "Old External Account Has Expired For User " + userObject.getLabel() + "!!!";

    String messageString = "The password for user account " + userObject.getLabel() + 
      " expired at " + passwordChangeTime + ".\n\n" +
      "If you need assistance with this matter, please contact one of your lab unit's Ganymede administrators, " +
      "or CSD.";

    Ganymede.log.sendMail(null, titleString, messageString, true, true, objVect);
  }


  /**
   * <p>Assigns a new set of external credentials,
   * sends email to the user's and admin groups' email addresses.</p>
   */

  private void assignNewCredentials(DBObject userObject)
  {

    // TODO copy over new to old mail user and password, 
    // create new mail user and password.

    Date mailExpDate = (Date) userObject.getFieldValueLocal(userSchema.MAILEXPDATE);

    Vector objVect = new Vector();

    objVect.addElement(userObject.getInvid());

    String titleString = "Old External Account Has Expired For User " + userObject.getLabel() + "!!!";

    String messageString = "The password for user account " + userObject.getLabel() + 
      " expired at " + mailExpDate + ".\n\n" +
      "If you need assistance with this matter, please contact one of your lab unit's Ganymede administrators, " +
      "or CSD.";

    Ganymede.log.sendMail(null, titleString, messageString, true, true, objVect);
  }
}
