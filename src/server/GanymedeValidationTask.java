/*

   GanymedeValidationTask.java

   This class goes through all objects in the database and checks to
   make sure all required fields are set in all objects.
   
   Created: 26 January 1999
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 2001/12/05 19:47:52 $
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

import arlut.csd.Util.VectorUtils;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                          GanymedeValidationTask

------------------------------------------------------------------------------*/

/**
 *
 * This class goes through all objects in the database and checks to
 * make sure all required fields are set in all objects.
 *
 */

public class GanymedeValidationTask implements Runnable {

  public static final boolean debug = true;

  // ---

  public GanymedeValidationTask()
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
    boolean everythingsfine = true;
    Vector objects, missingFields;
    DBObject object;
    DBObjectBase base;
    Enumeration baseEnum;
    Thread currentThread = java.lang.Thread.currentThread();

    /* -- */

    Ganymede.debug("Validation Task: Starting");

    String error = GanymedeServer.lSemaphore.checkEnabled();
	
    if (error != null)
      {
	Ganymede.debug("Deferring validation task - semaphore disabled: " + error);
	return;
      }

    try
      {
	try
	  {
	    mySession = new GanymedeSession("validation");
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("Validation Task: Couldn't establish session");
	    return;
	  }

	// we do each query on one object type.. we have to iterate
	// over all the object types defined in the server and scan
	// each for objects to be inactivated and/or removed.

	baseEnum = Ganymede.db.objectBases.elements();

	while (baseEnum.hasMoreElements())
	  {
	    if (currentThread.isInterrupted())
	      {
		throw new InterruptedException("task interrupted.");
	      }

	    base = (DBObjectBase) baseEnum.nextElement();

	    objects = mySession.getObjects(base.getTypeID());
	    
	    if (debug)
	      {
		Ganymede.debug("Scanning base " + base.getName() + " for invalid objects");
	      }

	    for (int i = 0; i < objects.size(); i++)
	      {
		if (currentThread.isInterrupted())
		  {
		    throw new InterruptedException("task interrupted.");
		  }

		object = (DBObject) objects.elementAt(i);

		missingFields = object.checkRequiredFields();

		if (missingFields != null)
		  {
		    Ganymede.debug(base.getName() + ":" + object.getLabel() + " is missing fields " +
				   VectorUtils.vectorString(missingFields));
		    everythingsfine = false;
		  }

		ReturnVal retVal = object.getBase().getObjectHook().consistencyCheck(object);

		if (retVal != null && !retVal.didSucceed())
		  {
		    Ganymede.debug(base.getName() + ":" + object.getLabel() + " failed consistency check");
		    everythingsfine = false;
		  }
	      }
	  }

	if (everythingsfine)
	  {
	    Ganymede.debug("Validation Task: All objects in database checked out fine.");
	  }
	else
	  {
	    Ganymede.debug("Validation Task: Some objects had missing fields.");
	  }
      }
    catch (InterruptedException ex)
      {
	Ganymede.debug("GanymedeValidationTask interrupted by GanymedeScheduler, validation incomplete.");
      }
    finally
      {
	if (mySession != null)
	  {
	    mySession.logout();
	  }
      }
  }
}
