/*

   PasswordAgingTask.java

   This task is run periodically to check all user accounts in Ganymede
   for their password expiration time.
   
   Created: 14 June 2001
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2001/06/14 23:36:28 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.text.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               PasswordAgingTask

------------------------------------------------------------------------------*/

/**
 *
 * This task is run periodically to check all user accounts in Ganymede
 * for their password expiration time.
 *
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class PasswordAgingTask implements Runnable {

  static final boolean debug = true;

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

    Ganymede.debug("Password Aging Task: Starting");

    String error = GanymedeServer.lSemaphore.checkEnabled();
	
    if (error != null)
      {
	Ganymede.debug("Deferring password aging task - semaphore disabled: " + error);
	return;
      }

    try
      {
	try
	  {
	    mySession = new GanymedeSession("PasswordAgingTask");
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("PasswordAging Task: Couldn't establish session");
	    return;
	  }

	// we don't want no wizards

	mySession.enableWizards(false);

	// and we don't want forced required fields oversight..  this
	// can leave us with some invalid objects, but we can do a
	// query to scan for them, and if someone edits the objects
	// later, they'll be requested to fix the problem.

	mySession.enableOversight(false);
	
	ReturnVal retVal = mySession.openTransaction("password aging task");

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("PasswordAging Task: Couldn't open transaction");
	    return;
	  }

	transactionOpen = true;
	
	// do the stuff

	warnOldPasswords();

	inactivateUsers();

	retVal = mySession.commitTransaction();

	if (retVal != null && !retVal.didSucceed())
	  {
	    // if doNormalProcessing is true, the
	    // transaction was not cleared, but was
	    // left open for a re-try.  Abort it.
	    
	    if (retVal.doNormalProcessing)
	      {
		Ganymede.debug("PasswordAging Task: couldn't fully commit, trying to abort.");
		
		mySession.abortTransaction();
	      }

	    Ganymede.debug("PasswordAging Task: Couldn't successfully commit transaction");
	  }
	else
	  {
	    Ganymede.debug("PasswordAging Task: Transaction committed");
	  }

	transactionOpen = false;
      }
    catch (InterruptedException ex)
      {
      }
    finally
      {
	if (transactionOpen)
	  {
	    Ganymede.debug("PasswordAging Task: Forced to terminate early, aborting transaction");
	  }

	mySession.logout();
      }
  }

  private void warnOldPasswords()
  {
    Query q;
    Vector results;
    Result result;
    Invid invid;
    DBObject object;
    Date currentTime = new Date();
    Calendar lowerBound = new GregorianCalendar();
    Calendar upperBound = new GregorianCalendar();
    Enumeration enum;

    QueryDataNode matchNode = new QueryDataNode(userSchema.PASSWORDCHANGETIME,
						QueryDataNode.DEFINED,
						null);
    /* -- */
    
    q = new Query(SchemaConstants.UserBase, matchNode, false);
    
    results = mySession.internalQuery(q);
    
    enum = results.elements();
    
    while (enum.hasMoreElements())
      {
	if (currentThread.isInterrupted())
	  {
	    throw new InterruptedException("scheduler ordering shutdown");
	  }
	    
	result = (Result) enum.nextElement();

	invid = result.getInvid();

	object = mySession.view_db_object(invid);

	// get the password expiration threshold for this user, if any

	DateDBField dateField = (DateDBField) object.getFieldValueLocal(userSchema.PASSWORDCHANGETIME);

	if (dateField == null)
	  {
	    continue;
	  }

	Date passwordTime = dateField.value();

	if (passwordTime == null)
	  {
	    continue;
	  }

	// first, check for one month into the future

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.MONTH, 1);
	lowerBound.add(Calendar.DATE, -1);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.MONTH, 1);

	if (passwordTime.after(lowerBound) && passwordTime.before(upperBound))
	  {
	    warnUpcomingPasswordExpire(object);
	    continue;
	  }

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.WEEK_OF_MONTH, 2);
	lowerBound.add(Calendar.DATE, -1);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.WEEK_OF_MONTH, 2);

	if (passwordTime.after(lowerBound) && passwordTime.before(upperBound))
	  {
	    warnUpcomingPasswordExpire(object);
	    continue;
	  }

	lowerBound.setTime(currentTime);
	lowerBound.add(Calendar.WEEK_OF_MONTH, 1);
	lowerBound.add(Calendar.DATE, -1);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.WEEK_OF_MONTH, 1);

	if (passwordTime.after(lowerBound) && passwordTime.before(upperBound))
	  {
	    warnRealSoonNowPasswordExpire(object);
	    continue;
	  }

	lowerBound.setTime(currentTime);

	upperBound.setTime(currentTime);
	upperBound.add(Calendar.DATE, 1);

	if (passwordTime.after(lowerBound) && passwordTime.before(upperBound))
	  {
	    warnRealSoonNowPasswordExpire(object);
	    continue;
	  }

	// call object.getEmailTargets()

	// we'll also want to get the owner groups for this user
	// and send a warning message to the owners
      }
  }

  private void warnUpcomingPasswordExpire(DBObject userObject)
  {
  }

  private void warnRealSoonNowPasswordExpire(DBObject userObject)
  {
  }

  private void warnOvertimePassword(DBObject userObject)
  {
  }

  private void inactivateUsers()
  {
    Query q;
    Vector results;
    Result result;
    Invid invid;
    Enumeration enum;

    /* -- */

    // if the 'PASSWORDCHANGETIME' field's value is earlier than five days
    // ago, it's time to inactivate the account

    Calendar myCal = new GregorianCalendar();
    myCal.add(Calendar.DAY_OF_MONTH, -5);
    Date gracePeriodEnd = new Date();

    QueryDataNode agedNode = new QueryDataNode(userSchema.PASSWORDCHANGETIME,
					       QueryDataNode.LESSEQ,
					       gracePeriodEnd);
    
    q = new Query(SchemaConstants.UserBase, agedNode, false);
    
    results = mySession.internalQuery(q);
    
    enum = results.elements();
    
    while (enum.hasMoreElements())
      {
	if (currentThread.isInterrupted())
	  {
	    throw new InterruptedException("scheduler ordering shutdown");
	  }
	    
	result = (Result) enum.nextElement();

	invid = result.getInvid();

	if (debug)
	  {
	    Ganymede.debug("Need to inactivate object " + base.getName() + ":" + 
			   result.toString());
	  }

	retVal = mySession.inactivate_db_object(invid);

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("PasswordAging task was not able to inactivate user " + 
			   result.toString());
	  }
	else
	  {
	    Ganymede.debug("PasswordAging task inactivated user " + 
			   result.toString());
	  }
      }
  }

  public static void main(String argv[])
  {
    new PasswordAgingTask().run();
  }
}
