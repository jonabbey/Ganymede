/*

   GanymedeBuilderTask.java

   This class provides a template for code to be attached to the server to
   handle propagating data from the Ganymede object store into the wide
   world, via NIS, DNS, NIS+, LDAP, JNDI, JDBC, X, Y, Z, etc.
   
   Created: 17 February 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                             GanymedeBuilderTask

------------------------------------------------------------------------------*/

/**
 *
 * This class provides a template for code to be attached to the server to
 * handle propagating data from the Ganymede object store into the wide
 * world, via NIS, DNS, NIS+, LDAP, JNDI, JDBC, X, Y, Z, etc.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public abstract class GanymedeBuilderTask implements Runnable {

  Date lastRunTime;
  GanymedeSession session = null;
  DBDumpLock lock;

  /* -- */

  /**
   *
   * This method is the main entry point for the GanymedeBuilderTask.  It
   * is responsible for setting up the environment for a builder task to
   * operate under, and for actually invoking the builder method.
   *
   */

  public final void run()
  {
    boolean success = false;

    /* -- */

    try
      {
	try
	  {
	    session = new GanymedeSession();
	  }
	catch (java.rmi.RemoteException ex)
	  {
	    throw new RuntimeException("bizarro remote exception " + ex);
	  }

	try
	  {
	    lock = session.getSession().openDumpLock();
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("Could not run task " + this.getClass().toString() + ", couldn't get dump lock");
	  }

	success = this.builder();
      }
    finally
      {
	// we need the finally in case our thread is stopped

	if (session != null)
	  {
	    session.logout();
	  }

	if (success)
	  {
	    if (lastRunTime == null)
	      {
		lastRunTime = new Date();
	      }
	    else
	      {
		lastRunTime.setTime(System.currentTimeMillis());
	      }
	  }
      }
  }

  /**
   *
   * This method is used by subclasses of GanymedeBuilderTask to
   * determine whether a particular base has had any modifications
   * made to it since the last time this builder task was run.
   *
   * @param baseid The id number of the base to be checked
   * 
   */

  protected final boolean baseChanged(short baseid)
  {
    if (lastRunTime == null)
      {
	return true;
      }
    else
      {
	DBObjectBase base = Ganymede.db.getObjectBase(baseid);
	
	if (base.lastChange == null)
	  {
	    return false;
	  }
	else 
	  {
	    return base.lastChange.after(lastRunTime);
	  }
      }
  }

  /**
   *
   * This method is used by subclasses of GanymedeBuilderTask to
   * obtain a list of DBObject references of the requested
   * type.
   *
   * @param baseid The id number of the base to be listed
   *
   * @return A Vector of DBObject references
   *
   */

  protected final Vector enumerateObjects(short baseid)
  {
    QueryResult result;
    Query query;

    /* -- */

    // since we are using an internal session, our query
    // will pick up all objects

    query = new Query(baseid);
    result = session.queryDispatch(query, true, false, lock);
    return result.getObjects();
  }

  /**
   *
   * This method is intended to be overridden by subclasses of
   * GanymedeBuilderTask.
   *
   * This method actually performs the work of the builder task.  The
   * builder task is intended to take advantage of the methods defined
   * in this class to do the work.
   *
   */

  abstract public boolean builder();
}
