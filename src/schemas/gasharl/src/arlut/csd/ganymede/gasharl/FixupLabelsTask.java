/*

   FixupLabelsTask.java

   This task is a simple one-shot intended to assign labels to all
   embedded objects lacking them in the new Ganymede 2.0 schema.
   
   Created: 18 August 2005

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2012
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
                                                                 FixupLabelsTask

------------------------------------------------------------------------------*/

/**
 *
 * This task is a simple one-shot intended to fill the new label fields in the
 * embedded Automounter Entry and interface objects.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class FixupLabelsTask implements Runnable {

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

    Ganymede.debug("FixupLabels Conversion Task: Starting");

    String error = GanymedeServer.checkEnabled();
	
    if (error != null)
      {
	Ganymede.debug("Deferring FixupLabels Conversion task - semaphore disabled: " + error);
	return;
      }

    try
      {
	try
	  {
	    mySession = new GanymedeSession("FixupLabelsTask");
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("FixupLabels Task: Couldn't establish session");
	    return;
	  }

	// we don't want interactive handholding

	mySession.enableWizards(false);

	mySession.enableOversight(false); // don't bother us about inconsistencies
	
	ReturnVal retVal = mySession.openTransaction("FixupLabels conversion task");

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("FixupLabels Task: Couldn't open transaction");
	    return;
	  }

	transactionOpen = true;
	
	// do the stuff

	if (!assignAutomounterLabels())
	  {
	    Ganymede.debug("assignAutomounterLabels bailed");

	    mySession.abortTransaction();
	    return;
	  }

	if (!fixupInterfaceLabels())
	  {
	    Ganymede.debug("fixupInterfaceLabels bailed");

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
		Ganymede.debug("FixupLabels Task: couldn't fully commit, trying to abort.");

		mySession.abortTransaction();
	      }

	    Ganymede.debug("FixupLabels Task: Couldn't successfully commit transaction");
	  }
	else
	  {
	    Ganymede.debug("FixupLabels Task: Transaction committed");
	  }

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
      }
    finally
      {
	if (transactionOpen)
	  {
	    Ganymede.debug("FixupLabels Task: Forced to terminate early, aborting transaction");
	  }

	mySession.logout();
      }
  }

  private boolean assignAutomounterLabels() throws InterruptedException, NotLoggedInException
  {
    List<DBObject> users = mySession.getDBSession().getTransactionalObjects(SchemaConstants.UserBase);

    for (DBObject user: users)
      {
	Invid invid = user.getInvid();

	// all we need to do is to edit the systems and the
	// preCommitHook() will clean up the mess

	ReturnVal retVal = mySession.edit_db_object(invid);

	if (retVal != null && !retVal.didSucceed())
	  {
	    return false;
	  }
      }

    return true;
  }

  private boolean fixupInterfaceLabels() throws InterruptedException, NotLoggedInException
  {
    List<DBObject> systems = mySession.getDBSession().getTransactionalObjects(interfaceSchema.BASE);
    
    for (DBObject system: systems)
      {
	Invid invid = system.getInvid();

	// all we need to do is to edit the systems and the
	// preCommitHook() will clean up the mess

	ReturnVal retVal = mySession.edit_db_object(invid);

	if (retVal != null && !retVal.didSucceed())
	  {
	    return false;
	  }
      }

    return true;
  }
}
