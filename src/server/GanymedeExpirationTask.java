/*

   GanymedeExpirationTask.java

   This class goes through all objects in the database and processes
   any expirations or removals.
   
   Created: 4 February 1998
   Version: $Revision: 1.7 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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

    try
      {
	try
	  {
	    mySession = new GanymedeSession();
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("Expiration Task: Couldn't establish session");
	    return;
	  }

	mySession.enableWizards(false);
	
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
