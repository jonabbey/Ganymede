/*

   GanymedeWarningTask.java

   This class goes through all objects in the database and sends
   out any warnings for objects that are going to expire within
   a whole number of weeks in the future.
   
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
                                                             GanymedeWarningTask

------------------------------------------------------------------------------*/

/**
 *
 * This class goes through all objects in the database and sends
 * out any warnings for objects that are going to expire within
 * a whole number of weeks in the future.
 *
 */

public class GanymedeWarningTask implements Runnable {

  public GanymedeWarningTask()
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

    Ganymede.debug("Warning Task: Starting");

    try
      {
	try
	  {
	    mySession = new GanymedeSession();
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("Warning Task: Couldn't establish session");
	    return;
	  }
	
	started = true;

	Query q;
	Vector results;
	Result result;
	Invid invid;
	Date currentTime = new Date();
	Calendar cal = Calendar.getInstance();
	Calendar cal2;
	Date loTime, hiTime;
	DBObjectBase base;
	Enumeration baseEnum, enum;
	QueryNode expireNode, removeNode;

	// --

	cal.setTime(currentTime);


	for (int i = 0; i < 3; i++)
	  {
	    cal.add(Calendar.DATE, 7);
	    loTime = cal.getTime();

	    cal2 = Calendar.getInstance();
	    cal2.setTime(loTime);
	    cal2.add(Calendar.DATE, 1);
	    hiTime = cal2.getTime();

	    expireNode = new QueryAndNode(new QueryDataNode(SchemaConstants.ExpirationField,
							    QueryDataNode.GREATEQ,
							    loTime),
					  new QueryDataNode(SchemaConstants.ExpirationField,
							    QueryDataNode.LESSEQ,
							    hiTime));

	    baseEnum = Ganymede.db.objectBases.elements();

	    while (baseEnum.hasMoreElements())
	      {
		base = (DBObjectBase) baseEnum.nextElement();

		if (base.isEmbedded())
		  {
		    continue;
		  }
	    
		q = new Query(base.getTypeID(), expireNode, false);

		results = mySession.internalQuery(q);

		enum = results.elements();

		while (enum.hasMoreElements())
		  {
		    result = (Result) enum.nextElement();

		    invid = result.getInvid();

		    Ganymede.debug("Object " + base.getName() + ":" + mySession.viewObjectLabel(invid) +
				   " is going to expire in " + (i+1) + " weeks");
		  }
							      
	      }

	    // now the removals

	    removeNode = new QueryAndNode(new QueryDataNode(SchemaConstants.RemovalField,
							    QueryDataNode.LESSEQ,
							    currentTime),
					  new QueryDataNode(SchemaConstants.RemovalField,
							    QueryDataNode.GREATEQ,
							    currentTime));

	    baseEnum = Ganymede.db.objectBases.elements();

	    while (baseEnum.hasMoreElements())
	      {
		base = (DBObjectBase) baseEnum.nextElement();

		if (base.isEmbedded())
		  {
		    continue;
		  }
	    
		q = new Query(base.getTypeID(), removeNode, false);

		results = mySession.internalQuery(q);

		enum = results.elements();

		while (enum.hasMoreElements())
		  {
		    result = (Result) enum.nextElement();

		    invid = result.getInvid();

		    Ganymede.debug("Object " + base.getName() + ":" + mySession.viewObjectLabel(invid) +
				   " is going to be removed in " + (i+1) + " weeks");
		  }
	      }
	  }
	
	mySession.logout();

	finished = true;
      }
    finally
      {
	if (started && !finished)
	  {
	    // we'll get here if this task's thread is stopped early

	    Ganymede.debug("Warning Task: Forced to terminate early, aborting.");
	    mySession.logout();
	  }
      }
  }


}
