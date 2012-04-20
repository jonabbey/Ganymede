/*

   GanymedeValidationTask.java

   This class goes through all objects in the database and checks to
   make sure all required fields are set in all objects.

   Created: 26 January 1999

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

package arlut.csd.ganymede.server;

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;

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

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.GanymedeValidationTask");


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
    List<DBObject> objects;
    Vector missingFields;
    DBObjectBase base;
    Enumeration baseEnum;
    Thread currentThread = java.lang.Thread.currentThread();

    /* -- */

    // "Validation Task: Starting"
    Ganymede.debug(ts.l("run.starting"));

    String error = GanymedeServer.lSemaphore.checkEnabled();
	
    if (error != null)
      {
	// "Deferring validation task = semaphore disabled: {0}"
	Ganymede.debug(ts.l("run.disabled", error));
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
	    // "Validation Task: Couldn''t establish session:\n{0}"
	    Ganymede.debug(ts.l("run.nosession", Ganymede.stackTrace(ex)));
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
		// "task interrupted."
		throw new InterruptedException(ts.l("run.interrupted"));
	      }

	    base = (DBObjectBase) baseEnum.nextElement();

	    objects = mySession.getObjects(base.getTypeID());
	    
	    if (debug)
	      {
		// "Scanning base {0} for invalid objects"
		Ganymede.debug(ts.l("run.scanning", base.getName()));
	      }

	    for (DBObject object: objects)
	      {
		if (currentThread.isInterrupted())
		  {
		    throw new InterruptedException(ts.l("run.interrupted"));
		  }

		missingFields = object.checkRequiredFields();

		if (missingFields != null)
		  {
		    // "{0}:{1} is missing fields {2}"
		    Ganymede.debug(ts.l("run.missing", base.getName(), object.getLabel(), VectorUtils.vectorString(missingFields)));

		    everythingsfine = false;
		  }

		ReturnVal retVal;

		try
		  {
		    retVal = object.getBase().getObjectHook().consistencyCheck(object);

		    if (!ReturnVal.didSucceed(retVal))
		      {
			String dialogText = retVal.getDialogText();
			
			if (dialogText != null)
			  {
			    // {0}:{1} failed consistency check: {2}
			    Ganymede.debug(ts.l("run.inconsistent", base.getName(), object.getLabel(), dialogText));
			  }
			else
			  {
			    // {0}:{1} failed consistency check
			    Ganymede.debug(ts.l("run.inconsistent_notext", base.getName(), object.getLabel()));
			  }
			
			everythingsfine = false;
		      }
		  }
		catch (Throwable ex)
		  {
		    // "{0}:{1} threw exception in consistencyCheck():\n{2}"
		    Ganymede.debug(ts.l("run.exceptioned", base.getName(), object.getLabel(), Ganymede.stackTrace(ex)));

		    everythingsfine = false;
		  }

		try
		  {
		    retVal = object.validateFieldIntegrity();  // no merge since we don't return the retVal

		    if (!ReturnVal.didSucceed(retVal))
		      {
			String dialogText = retVal.getDialogText();
			
			if (dialogText != null)
			  {
			    // {0}:{1} failed field-level consistency check: {2}
			    Ganymede.debug(ts.l("run.field_inconsistent", base.getName(), object.getLabel(), dialogText));
			  }
			else
			  {
			    // {0}:{1} failed field-level consistency check
			    Ganymede.debug(ts.l("run.field_inconsistent_notext", base.getName(), object.getLabel()));
			  }
			
			everythingsfine = false;
		      }
		  }
		catch (Throwable ex)
		  {
		    // "{0}:{1} threw exception in validateFieldIntegrity():\n{2}"
		    Ganymede.debug(ts.l("run.field_exceptioned", base.getName(), object.getLabel(), Ganymede.stackTrace(ex)));

		    everythingsfine = false;
		  }
	      }
	  }

	if (everythingsfine)
	  {
	    // "Validation Task: All objects in database checked out fine."
	    Ganymede.debug(ts.l("run.ok"));
	  }
	else
	  {
	    // "Validation Task: Some objects had missing fields or were otherwise inconsistent."
	    Ganymede.debug(ts.l("run.bad"));
	  }
      }
    catch (InterruptedException ex)
      {
	// "GanymedeValidationTask interrupted by GanymedeScheduler, validation incomplete."
	Ganymede.debug(ts.l("run.interrupted_explanation"));
      }
    catch (NotLoggedInException ex)
      {
	// "Mysterious not logged in exception: {0}"
	Ganymede.debug(ts.l("run.mysterious_nologin", Ganymede.stackTrace(ex)));
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
