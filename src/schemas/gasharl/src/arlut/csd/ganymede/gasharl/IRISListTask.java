/*

   IRISListTask.java

   This task is used to update members in IRISList objects in the
   Ganymede user base.

   Created: 23 Feburary 2011

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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
                                                                    IRISListTask

------------------------------------------------------------------------------*/

/**
 * <p>This task is used to update members in IRISList objects in the
 * Ganymede user base.</p>
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class IRISListTask implements Runnable {

  static final boolean debug = true;

  /* -- */

  GanymedeSession mySession = null;

  /**
   * Just Do It (tm)
   *
   * @see java.lang.Runnable
   */

  public void run()
  {
    boolean transactionOpen = false;

    /* -- */

    Ganymede.debug("IRISListTask: Starting");

    String error = GanymedeServer.checkEnabled();

    if (error != null)
      {
        Ganymede.debug("Deferring IRISListTask task - semaphore disabled: " + error);
        return;
      }

    try
      {
        try
          {
            // supergash level session

            mySession = new GanymedeSession("IRIS Email List Task");
          }
        catch (RemoteException ex)
          {
            Ganymede.debug("IRISListTask: Couldn't establish session");
            return;
          }

        // we don't want interactive handholding

        mySession.enableWizards(false);

        mySession.enableOversight(false); // don't bother us about inconsistencies

        ReturnVal retVal = mySession.openTransaction("IRIS Email List Update");

        if (retVal != null && !retVal.didSucceed())
          {
            Ganymede.debug("IRISListTask: Couldn't open transaction");
            return;
          }

        transactionOpen = true;

        if (updateLists())
          {
            mySession.commitTransaction(true);
          }
        else
          {
            mySession.abortTransaction();
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

        ex.printStackTrace();
      }
    finally
      {
        if (transactionOpen)
          {
            Ganymede.debug("IRISListTask: Forced to terminate early, aborting transaction");
          }

        mySession.logout();
      }
  }

  private boolean updateLists() throws InterruptedException, NotLoggedInException
  {
    boolean needCommit = false;

    List<DBObject> lists = mySession.getDBSession().getTransactionalObjects(IRISListSchema.BASE);

    for (DBObject list: lists)
      {
        ReturnVal retVal = IRISListCustom.handleUpdate(list, mySession);

        if (retVal != null && ReturnVal.didSucceed(retVal))
          {
            needCommit = true;
          }
      }

    return needCommit;
  }
}
