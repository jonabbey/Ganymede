/*

   GanymedeExpirationTask.java

   This class goes through all objects in the database and processes
   any expirations or removals.
   
   Created: 4 February 1998
   Release: $Name:  $
   Version: $Revision: 1.13 $
   Last Mod Date: $Date: 2000/01/27 06:03:21 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                          GanymedeExpirationTask

------------------------------------------------------------------------------*/

/**
 *
 * This class goes through all objects in the database and processes
 * any expirations or removals.
 *
 */

public class GanymedeExpirationTask implements Runnable {

  public static final boolean debug = false;

  // ---

  public GanymedeExpirationTask()
  {
  }
  
  /**
   *
   * Just Do It (tm)
   *
   * @see java.lang.Runnable
   *
   */

  public void run()
  {
    GanymedeSession mySession = null;
    boolean started = false;
    boolean finished = false;

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
	
	ReturnVal retVal = mySession.openTransaction("expiration task");

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("Expiration Task: Couldn't open transaction");
	    return;
	  }

	started = true;

	Query q;
	Vector results;
	Result result;
	Invid invid;
	Date currentTime = new Date();
	DBObjectBase base;
	Enumeration baseEnum, enum;
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

	baseEnum = Ganymede.db.objectBases.elements();

	while (baseEnum.hasMoreElements())
	  {
	    base = (DBObjectBase) baseEnum.nextElement();

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

	    q = new Query(base.getTypeID(), expireNode, false);

	    results = mySession.internalQuery(q);

	    enum = results.elements();

	    while (enum.hasMoreElements())
	      {
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
		    Ganymede.debug("Expiration task was not able to inactivate object " + 
				   base.getName() + ":" + result.toString());
		  }
		else
		  {
		    Ganymede.debug("Expiration task inactivated object " + 
				   base.getName() + ":" + result.toString());
		  }
	      }
	  }

	// now the removals

	baseEnum = Ganymede.db.objectBases.elements();

	while (baseEnum.hasMoreElements())
	  {
	    base = (DBObjectBase) baseEnum.nextElement();

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

	    q = new Query(base.getTypeID(), removeNode, false);

	    results = mySession.internalQuery(q);

	    enum = results.elements();

	    while (enum.hasMoreElements())
	      {
		result = (Result) enum.nextElement();

		invid = result.getInvid();

		if (debug)
		  {
		    Ganymede.debug("Need to remove object " + base.getName() + ":" + 
				   result.toString());
		  }

		retVal = mySession.remove_db_object(invid);

		if (retVal != null && !retVal.didSucceed())
		  {
		    Ganymede.debug("Expiration task was not able to remove object " + 
				   base.getName() + ":" + result.toString());
		  }
		else
		  {
		    Ganymede.debug("Expiration task deleted object " + 
				   base.getName() + ":" + result.toString());
		  }
	      }
	  }
	
	retVal = mySession.commitTransaction();

	if (retVal != null && !retVal.didSucceed())
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

	finished = true;	// minimize chance of attempting to abort an open transaction in finally

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("Expiration Task: Couldn't successfully commit transaction");
	  }
	else
	  {
	    Ganymede.debug("Expiration Task: Transaction committed");
	  }
      }
    finally
      {
	if (started && !finished)
	  {
	    // we'll get here if this task's thread is stopped early

	    Ganymede.debug("Expiration Task: Forced to terminate early, aborting transaction");
	    mySession.abortTransaction();
	    Ganymede.debug("Expiration Task: Transaction aborted");
	    mySession.logout();
	  }
      }
  }


}
