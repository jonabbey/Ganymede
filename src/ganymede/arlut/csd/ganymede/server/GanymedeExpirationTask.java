/*

   GanymedeExpirationTask.java

   This class goes through all objects in the database and processes
   any expirations or removals.

   Created: 4 February 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                          GanymedeExpirationTask

------------------------------------------------------------------------------*/

/**
 * <p>This is a Ganymede server task, for use with the {@link
 * arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler}.</p>
 *
 * <p>The GanymedeExpirationTask scans through all objects in the
 * database and handles expiration and/or removal for any object whose
 * Expiration or Removal timestamps have passed.</p>
 *
 * <p>GanymedeExpirationTask is designed to be run once a day by the
 * GanymedeScheduler, but running it more often won't hurt
 * anything.</p>
 *
 * <p>The standard GanymedeExpirationTask is paired with the standard
 * {@link arlut.csd.ganymede.server.GanymedeWarningTask
 * GanymedeWarningTask} task, which sends out email warning of
 * expirations and removals to occur in the near future.</p>
 */

public class GanymedeExpirationTask implements Runnable {

  public static final boolean debug = false;

  // ---

  public GanymedeExpirationTask()
  {
  }

  /**
   * Just Do It (tm)
   *
   * @see java.lang.Runnable
   */

  public void run()
  {
    GanymedeSession mySession = null;
    DBSession myDBSession = null;
    boolean started = false;
    boolean finished = false;
    Thread currentThread = java.lang.Thread.currentThread();

    /* -- */

    Ganymede.debug("Expiration Task: Starting");

    String error = GanymedeServer.lSemaphore.checkEnabled();

    if (error != null)
      {
        Ganymede.debug("Deferring expiration task - semaphore disabled: " + error);
        return;
      }

    try
      {
        try
          {
            mySession = new GanymedeSession("expiration");
            myDBSession = mySession.getDBSession();
          }
        catch (RemoteException ex)
          {
            Ganymede.debug("Expiration Task: Couldn't establish session");
            return;
          }

        // we don't want no wizards

        mySession.enableWizards(false);

        // and we don't want forced required fields oversight..  this
        // can leave us with some invalid objects, but we can do a
        // query to scan for them, and if someone edits the objects
        // later, they'll be requested to fix the problem.

        mySession.enableOversight(false);

        ReturnVal retVal = mySession.openTransaction("expiration task", false); // non-interactive

        if (!ReturnVal.didSucceed(retVal))
          {
            Ganymede.debug("Expiration Task: Couldn't open transaction");
            return;
          }

        started = true;

        Date currentTime = new Date();
        QueryDataNode expireNode = new QueryDataNode(SchemaConstants.ExpirationField,
                                                     QueryDataNode.LESSEQ,
                                                     currentTime);

        QueryDataNode removeNode = new QueryDataNode(SchemaConstants.RemovalField,
                                                     QueryDataNode.LESSEQ,
                                                     currentTime);

        // --

        // we do each query on one object type.. we have to iterate
        // over all the object types defined in the server and scan
        // each for objects to be inactivated and/or removed.

        for (DBObjectBase base: Ganymede.db.bases())
          {
            if (currentThread.isInterrupted())
              {
                throw new InterruptedException("scheduler ordering shutdown");
              }

            // embedded objects are inactivated with their parents, we don't
            // handle them separately

            if (base.isEmbedded())
              {
                continue;
              }

            if (debug)
              {
                Ganymede.debug("Sweeping base " + base.getName() + " for expired objects");
              }

            Query q = new Query(base.getTypeID(), expireNode, false);

            for (Result result: mySession.internalQuery(q))
              {
                if (currentThread.isInterrupted())
                  {
                    throw new InterruptedException("scheduler ordering shutdown");
                  }

                Invid invid = result.getInvid();

                if (debug)
                  {
                    Ganymede.debug("Need to inactivate object " + base.getName() + ":" +
                                   result.toString());
                  }

                String checkpointKey = "inactivating " + invid.toString();

                myDBSession.checkpoint(checkpointKey);

                try
                  {
                    retVal = mySession.inactivate_db_object(invid);
                  }
                catch (Throwable ex)
                  {
                    myDBSession.rollback(checkpointKey);

                    throw new RuntimeException(ex);
                  }

                if (!ReturnVal.didSucceed(retVal))
                  {
                    myDBSession.rollback(checkpointKey);

                    Ganymede.debug("Expiration task was not able to inactivate object " +
                                   base.getName() + ":" + result.toString());
                  }
                else
                  {
                    myDBSession.popCheckpoint(checkpointKey);

                    Ganymede.debug("Expiration task inactivated object " +
                                   base.getName() + ":" + result.toString());
                  }
              }
          }

        // now the removals

        for (DBObjectBase base: Ganymede.db.bases())
          {
            if (currentThread.isInterrupted())
              {
                throw new InterruptedException("scheduler ordering shutdown");
              }

            // embedded objects are removed with their parents, we don't
            // handle them separately

            if (base.isEmbedded())
              {
                continue;
              }

            if (debug)
              {
                Ganymede.debug("Sweeping base " + base.getName() +
                               " for objects to be removed");
              }

            Query q = new Query(base.getTypeID(), removeNode, false);

            for (Result result: mySession.internalQuery(q))
              {
                if (currentThread.isInterrupted())
                  {
                    throw new InterruptedException("scheduler ordering shutdown");
                  }

                Invid invid = result.getInvid();

                if (debug)
                  {
                    Ganymede.debug("Need to remove object " + base.getName() + ":" +
                                   result.toString());
                  }

                String checkpointKey = "removing " + invid.toString();

                myDBSession.checkpoint(checkpointKey);

                try
                  {
                    retVal = mySession.remove_db_object(invid);
                  }
                catch (Throwable ex)
                  {
                    myDBSession.rollback(checkpointKey);

                    throw new RuntimeException(ex);
                  }

                if (!ReturnVal.didSucceed(retVal))
                  {
                    myDBSession.rollback(checkpointKey);

                    Ganymede.debug("Expiration task was not able to remove object " +
                                   base.getName() + ":" + result.toString());
                  }
                else
                  {
                    myDBSession.popCheckpoint(checkpointKey);

                    Ganymede.debug("Expiration task deleted object " +
                                   base.getName() + ":" + result.toString());
                  }
              }
          }

        retVal = mySession.commitTransaction();

        if (!ReturnVal.didSucceed(retVal))
          {
            // if doNormalProcessing is true, the
            // transaction was not cleared, but was
            // left open for a re-try.  Abort it.

            if (retVal.doNormalProcessing)
              {
                Ganymede.debug("Expiration Task: couldn't fully commit, trying to abort.");

                mySession.abortTransaction();
              }
          }

        mySession.logout();

        finished = true;        // minimize chance of attempting to abort an open transaction in finally

        if (!ReturnVal.didSucceed(retVal))
          {
            Ganymede.debug("Expiration Task: Couldn't successfully commit transaction");
          }
        else
          {
            Ganymede.debug("Expiration Task: Transaction committed");
          }
      }
    catch (NotLoggedInException ex)
      {
        Ganymede.debug("Mysterious not logged in exception: " + ex.getMessage());
      }
    catch (InterruptedException ex)
      {
      }
    finally
      {
        if (started && !finished)
          {
            // we'll get here if this task's thread is stopped early

            Ganymede.debug("Expiration Task: Forced to terminate early, aborting transaction");

            if (mySession != null)
              {
                try
                  {
                    mySession.abortTransaction();
                    Ganymede.debug("Expiration Task: Transaction aborted");
                    mySession.logout();
                  }
                catch (NotLoggedInException ex)
                  {
                  }
              }
          }
      }
  }
}
