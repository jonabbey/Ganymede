/*

   DNSBuilderTask.java

   This class is intended to dump the Ganymede datastore to DNS.
   
   Created: 18 February 1998
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;
import arlut.csd.Util.PathComplete;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  DNSBuilderTask

------------------------------------------------------------------------------*/

/**
 *
 * This class is intended to dump the Ganymede datastore to DNS.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public class DNSBuilderTask extends GanymedeBuilderTask {

  private static String path = null;

  // ---

  /**
   *
   * This method is intended to be overridden by subclasses of
   * DNSBuilderTask.
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

  public boolean builderPhase1()
  {
    boolean result = false;

    /* -- */

    if (path == null)
      {
	path = System.getProperty("ganymede.dns.output");

	if (path == null)
	  {
	    throw new RuntimeException("DNSBuilder not able to determine output directory.");
	  }

	path = PathComplete.completePath(path);
      }

    if (baseChanged((short) 263) ||
	baseChanged((short) 267) ||
	baseChanged((short) 268) ||
	baseChanged((short) 264) ||
	baseChanged((short) 265) ||
	baseChanged((short) 266))
      {
	Ganymede.debug("Need to build DNS tables");
	result = true;
      }

    return result;
   }

  /**
   *
   * This method runs after this task's dumpLock has been
   * relinquished.  This method is intended to be used to finish off a
   * build process by running (probably external) code that does not
   * require direct access to the database.
   *
   * builderPhase2 is only run if builderPhase1 returns true.
   *
   */

  public boolean builderPhase2()
  {
    Ganymede.debug("Need to do DNS build script");

    return true;
  }
}
