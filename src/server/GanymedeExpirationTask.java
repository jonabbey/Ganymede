/*

   GanymedeExpirationTask.java

   This class goes through all objects in the database and processes
   any expirations or removals.
   
   Created: 4 February 1998
   Version: $Revision: 1.1 $ %D%
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
	
	ReturnVal retVal = mySession.openTransaction("expiration task");

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("Expiration Task: Couldn't establish session");
	  }

	started = true;

	Query q;
	Vector results;
	Result result;
	Invid invid;
	Date currentTime = new Date();
	DBObjectBase base;
	Enumeration enum;

	Enumeration baseEnum = Ganymede.db.objectBases.elements();

	while (baseEnum.hasMoreElements())
	  {
	    base = (DBObjectBase) baseEnum.nextElement();

	    if (base.isEmbedded())
	      {
		continue;
	      }
	    
	    Ganymede.debug("Sweeping base " + base.getName() + " for expired objects");

	    q = new Query(base.getTypeID(), 
			  new QueryDataNode(SchemaConstants.ExpirationField, 
					    QueryDataNode.LESSEQ,
					    currentTime),
			  false);

	    results = mySession.internalQuery(q);

	    enum = results.elements();

	    while (enum.hasMoreElements())
	      {
		result = (Result) enum.nextElement();

		invid = result.getInvid();

		Ganymede.debug("Need to inactivate object " + base.getName() + ":" + mySession.viewObjectLabel(invid));
	      }
							      
	  }

	// now the removals

	baseEnum = Ganymede.db.objectBases.elements();

	while (baseEnum.hasMoreElements())
	  {
	    base = (DBObjectBase) baseEnum.nextElement();

	    if (base.isEmbedded())
	      {
		continue;
	      }
	    
	    Ganymede.debug("Sweeping base " + base.getName() + " for objects to be removed");

	    q = new Query(base.getTypeID(), 
			  new QueryDataNode(SchemaConstants.RemovalField, 
					    QueryDataNode.LESSEQ,
					    currentTime),
			  false);

	    results = mySession.internalQuery(q);

	    enum = results.elements();

	    while (enum.hasMoreElements())
	      {
		result = (Result) enum.nextElement();

		invid = result.getInvid();

		Ganymede.debug("Need to remove object " + base.getName() + ":" + mySession.viewObjectLabel(invid));
	      }
	  }
	
	retVal = mySession.commitTransaction();

	mySession.logout();

	finished = true;	// minimize chance of aborting an open transaction in finally

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("Expiration Task: Couldn't successfully commit transaction");
	  }
	else
	  {
	    Ganymede.debug("Expiration Task: Couldn't transaction committed");
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
