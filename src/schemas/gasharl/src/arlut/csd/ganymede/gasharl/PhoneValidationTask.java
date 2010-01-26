/*

   PhoneValidationTask.java

   This task is used to validate/update phone numbers in the Ganymede
   user base.
   
   Created: 29 June 2009

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.gasharl;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeServer;
import arlut.csd.ganymede.server.GanymedeSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                             PhoneValidationTask

------------------------------------------------------------------------------*/

/**
 *
 * This task is used to validate/update phone numbers in the Ganymede
 * user base.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class PhoneValidationTask implements Runnable {

  static final boolean debug = true;

  /* -- */

  GanymedeSession mySession = null;

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

    Ganymede.debug("PhoneValidationTask: Starting");

    String error = GanymedeServer.checkEnabled();
	
    if (error != null)
      {
	Ganymede.debug("Deferring PhoneValidationTask task - semaphore disabled: " + error);
	return;
      }

    try
      {
	try
	  {
	    // supergash level session

	    mySession = new GanymedeSession("IRIS Phone Number Validation Task");
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("PhoneValidationTask: Couldn't establish session");
	    return;
	  }

	// we don't want interactive handholding

	mySession.enableWizards(false);

	mySession.enableOversight(false); // don't bother us about inconsistencies
	
	ReturnVal retVal = mySession.openTransaction("IRIS Phone Number Validation");

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("PhoneValidationTask: Couldn't open transaction");
	    return;
	  }

	transactionOpen = true;

	scanPhones();

	mySession.commitTransaction(true);

	transactionOpen = false;
      }
    catch (NotLoggedInException ex)
      {
      }
    catch (InterruptedException ex)
      {
      }
    catch (Throwable ex)
      {
	Ganymede.debug("Caught " + ex.getMessage());

	ex.printStackTrace();
      }
    finally
      {
	if (transactionOpen)
	  {
	    Ganymede.debug("PhoneValidationTask: Forced to terminate early, aborting transaction");
	  }

	mySession.logout();
      }
  }

  private void scanPhones() throws InterruptedException, NotLoggedInException
  {
    List<DBObject> users = mySession.getObjects(SchemaConstants.UserBase);

    for (DBObject user: users)
      {
	if (user.isInactivated())
	  {
	    continue;
	  }

	if (!user.isDefined(userSchema.BADGE))
	  {
	    Ganymede.debug("User " + user.getLabel() + " has no badge");
	  }

	String badge = (String) user.getFieldValueLocal(userSchema.BADGE);
	String irisPhone = IRISLink.getPhone(badge);

	if (irisPhone == null)
	  {
	    continue;
	  }

	if (!user.isDefined(userSchema.OFFICEPHONE))
	  {
	    ReturnVal retVal = mySession.edit_db_object(user.getInvid());

	    if (!ReturnVal.didSucceed(retVal))
	      {
		continue;
	      }

	    DBEditObject userObject = (DBEditObject) retVal.getObject();

	    userObject.setFieldValueLocal(userSchema.OFFICEPHONE, irisPhone);
	  }
	else
	  {
	    String phone = (String) user.getFieldValueLocal(userSchema.OFFICEPHONE);

	    if (!isCompatible(irisPhone, phone))
	      {
		Ganymede.debug("User " + user.getLabel() + " has a recorded phone of " + phone + " but IRIS thinks it should be " + irisPhone);

		ReturnVal retVal = mySession.edit_db_object(user.getInvid());

		if (!ReturnVal.didSucceed(retVal))
		  {
		    continue;
		  }

		DBEditObject userObject = (DBEditObject) retVal.getObject();

		userObject.setFieldValueLocal(userSchema.OFFICEPHONE, irisPhone);
	      }
	  }
      }
  }

  private boolean isCompatible(String irisPhone, String userPhone)
  {
    if (irisPhone.equals(userPhone))
      {
	return true;
      }

    if (("835" + irisPhone).equals(userPhone))
      {
	return true;
      }

    if (("835-" + irisPhone).equals(userPhone))
      {
	return true;
      }

    if (("(512) 835-" + irisPhone).equals(userPhone))
      {
	return true;
      }

    if (("512-835-" + irisPhone).equals(userPhone))
      {
	return true;
      }

    if (("X" + irisPhone).equals(userPhone))
      {
	return true;
      }

    if (("x" + irisPhone).equals(userPhone))
      {
	return true;
      }

    return false;
  }
}
