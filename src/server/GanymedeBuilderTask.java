/*

   GanymedeBuilderTask.java

   This class provides a template for code to be attached to the server to
   handle propagating data from the Ganymede object store into the wide
   world, via NIS, DNS, NIS+, LDAP, JNDI, JDBC, X, Y, Z, etc.
   
   Created: 17 February 1998
   Version: $Revision: 1.2 $ %D%
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

  protected Date lastRunTime;
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
    boolean
      success1 = false,
      success2 = false;

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

	success1 = this.builderPhase1();

	// release the lock, and so on

	if (session != null)
	  {
	    session.logout();
	    session = null;
	    lock = null;
	  }

	if (success1)
	  {
	    success2 = this.builderPhase2();
	  }
      }
    finally
      {
	// we need the finally in case our thread is stopped

	if (session != null)
	  {
	    session.logout();
	  }

	if (success2)
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

  protected final Enumeration enumerateObjects(short baseid)
  {
    // this works only because we've already got our lock
    // established..  otherwise, we'd have to use the query system.

    if (lock == null)
      {
	throw new IllegalArgumentException("Can't call enumerateObjects without a lock");
      }

    DBObjectBase base = Ganymede.db.getObjectBase(baseid);

    return base.objectHash.elements();
  }

  /**
   *
   * This method is used by subclasses of GanymedeBuilderTask to
   * obtain a reference to a DBObject matching a given invid.
   *
   * @param invid The object id of the object to be viewed
   *
   */

  protected final DBObject getObject(Invid invid)
  {
    // this works only because we've already got our lock
    // established..  otherwise, we'd have to use the query system.

    if (lock == null)
      {
	throw new IllegalArgumentException("Can't call enumerateObjects without a lock");
      }

    return session.session.viewDBObject(invid);
  }

  /**
   *
   * This method is used by subclasses of GanymedeBuilderTask to
   * obtain the label for an object.
   *
   * @param baseid The object id of the object label to be retrieved
   *
   */

  protected final String getLabel(Invid invid)
  {
    return session.viewObjectLabel(invid);
  }

  /**
   *
   * This method is intended to be overridden by subclasses of
   * GanymedeBuilderTask.
   *
   * This method runs with a dumpLock obtained for the builder task.
   *
   * Code run in builderPhase1() can call enumerateObjects() and
   * baseChanged().
   *
   * @return true if builderPhase1 made changes necessitating the
   * execution of builderPhase2.
   *
   */

  abstract public boolean builderPhase1();

  /**
   * This method is intended to be overridden by subclasses of
   * GanymedeBuilderTask.
   *
   * This method runs after this task's dumpLock has been
   * relinquished.  This method is intended to be used to finish off a
   * build process by running (probably external) code that does not
   * require direct access to the database.
   *
   * For instance, for an NIS builder task, builderPhase1() would scan
   * the Ganymede object store and write out NIS-compatible source
   * files.  builderPhase1() would return, the run() method drops the
   * dump lock so that other transactions can be committed, and then
   * builderPhase2() can be run to turn those on-disk files written by
   * builderPhase1() into NIS maps.  This generally involves executing
   * an external Makefile, which can take an indeterminate period of
   * time.
   *
   * By releasing the dumpLock before we get to that point, we
   * minimize contention for users of the system.
   *
   * As a result of having dropped the dumpLock, enumerateObjects()
   * cannot be called by this method.
   *
   * builderPhase2 is only run if builderPhase1 returns true.
   *  
   */

  abstract public boolean builderPhase2();
}
