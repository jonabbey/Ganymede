/*

   GUIDGeneratorTask.java

   This task is a simple one-shot intended to create GUID's in users
   that do not have them.
   
   Created: 25 September 2002

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

import org.doomdark.uuid.EthernetAddress;
import org.doomdark.uuid.UUIDGenerator;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeServer;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                               PasswordAgingTask

------------------------------------------------------------------------------*/

/**
 *
 * This task is a simple one-shot intended to create GUID's in users
 * that do not have them.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class GUIDGeneratorTask implements Runnable {

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

    Ganymede.debug("GUIDGenerator Task: Starting");

    String error = GanymedeServer.checkEnabled();
	
    if (error != null)
      {
	Ganymede.debug("Deferring GUIDGenerator task - semaphore disabled: " + error);
	return;
      }

    try
      {
	try
	  {
	    mySession = new GanymedeSession("GUIDGeneratorTask");
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("GUIDGenerator Task: Couldn't establish session");
	    return;
	  }

	// we don't want interactive handholding

	mySession.enableWizards(false);

	// and we want forced required fields oversight..

	mySession.enableOversight(true);
	
	ReturnVal retVal = mySession.openTransaction("GUIDGenerator conversion task");

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("GUIDGenerator Task: Couldn't open transaction");
	    return;
	  }

	transactionOpen = true;
	
	// do the stuff

	if (!createGUIDs())
	  {
	    Ganymede.debug("Create GUIDs bailed");

	    mySession.abortTransaction();
	    return;
	  }

	retVal = mySession.commitTransaction();

	if (retVal != null && !retVal.didSucceed())
	  {
	    // if doNormalProcessing is true, the
	    // transaction was not cleared, but was
	    // left open for a re-try.  Abort it.

	    if (retVal.doNormalProcessing)
	      {
		Ganymede.debug("GUIDGenerator Task: couldn't fully commit, trying to abort.");

		mySession.abortTransaction();
	      }

	    Ganymede.debug("GUIDGenerator Task: Couldn't successfully commit transaction");
	  }
	else
	  {
	    Ganymede.debug("GUIDGenerator Task: Transaction committed");
	  }

	transactionOpen = false;
      }
    catch (NotLoggedInException ex)
      {
      }
    catch (Throwable ex)
      {
	Ganymede.debug("Caught " + ex.getMessage());
      }
    finally
      {
	if (transactionOpen)
	  {
	    Ganymede.debug("GUIDGenerator Task: Forced to terminate early, aborting transaction");
	  }

	mySession.logout();
      }
  }

  public boolean createGUIDs() throws NotLoggedInException
  {
    List<DBObject> users = mySession.getObjects(SchemaConstants.UserBase);
    UUIDGenerator gen = UUIDGenerator.getInstance();
    EthernetAddress myAddress = new EthernetAddress("8:0:20:fd:6b:7");
    boolean success = true;

    /* -- */
    
    for (DBObject user: users)
      {
	if (user.isDefined(userSchema.GUID))
	  {
	    continue;
	  }

	if (!user.isValid())
	  {
	    Ganymede.debug("Skipping user " + user + " due to pre-existing inconsistency.");
	    continue;
	  }

	Invid invid = user.getInvid();

	ReturnVal retVal = mySession.edit_db_object(invid);

	if (retVal != null && retVal.didSucceed())
	  {
	    DBEditObject eo = (DBEditObject) retVal.getObject();

	    StringDBField guidField = (StringDBField) eo.getField(userSchema.GUID);

	    if (!guidField.isDefined())
	      {
		org.doomdark.uuid.UUID uuid = gen.generateTimeBasedUUID(myAddress);
		String uuidString = uuid.toString();

		retVal = guidField.setValueLocal(uuidString);

		if (retVal != null && !retVal.didSucceed())
		  {
		    success = false;
		  }
	      }
	  }
      }

    List<DBObject> groups = mySession.getObjects((short) 257);

    for (DBObject group: groups)
      {
	if (group.isDefined(groupSchema.GUID))
	  {
	    continue;
	  }

	if (!group.isValid())
	  {
	    Ganymede.debug("Skipping group " + group + " due to pre-existing inconsistency.");
	    continue;
	  }

	Invid invid = group.getInvid();

	ReturnVal retVal = mySession.edit_db_object(invid);

	if (retVal != null && retVal.didSucceed())
	  {
	    DBEditObject eo = (DBEditObject) retVal.getObject();

	    StringDBField guidField = (StringDBField) eo.getField(groupSchema.GUID);

	    if (!guidField.isDefined())
	      {
		org.doomdark.uuid.UUID uuid = gen.generateTimeBasedUUID(myAddress);
		String uuidString = uuid.toString();

		retVal = guidField.setValueLocal(uuidString);

		if (retVal != null && !retVal.didSucceed())
		  {
		    success = false;
		  }
	      }
	  }
      }

    return success;
  }
}

