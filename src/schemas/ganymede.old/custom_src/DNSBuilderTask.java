/*

   DNSBuilderTask.java

   This class is intended to dump the Ganymede datastore to DNS.
   
   Created: 18 February 1998
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 1999/10/13 20:00:49 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;
import arlut.csd.Util.PathComplete;

import java.util.*;
import java.io.*;

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
  private static String buildScript = null;
  private static Runtime runtime = null;

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
    File
      file;

    /* -- */

    if (buildScript == null)
      {
	buildScript = path + "dnsbuilder";
      }

    file = new File(buildScript);

    if (file.exists())
      {
	if (runtime == null)
	  {
	    runtime = Runtime.getRuntime();
	  }

	Process process = null;

	/* -- */

	try
	  {
	    process = runtime.exec(buildScript);

	    process.waitFor();
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("Couldn't exec buildScript (" + buildScript + ") due to IOException: " + ex);
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug("Failure during exec of buildScript (" + buildScript + "): " + ex);
	  }
	finally
	  {
	    // the following is mentioned as a work-around for the
	    // fact that Process keeps its file descriptors open by
	    // default until Garbage Collection

	    try
	      {
		process.getInputStream().close();
	      }
	    catch (NullPointerException ex)
	      {
	      }
	    catch (IOException ex)
	      {
	      }

	    try
	      {
		process.getOutputStream().close();
	      }
	    catch (NullPointerException ex)
	      {
	      }
	    catch (IOException ex)
	      {
	      }

	    try
	      {
		process.getErrorStream().close();
	      }
	    catch (NullPointerException ex)
	      {
	      }
	    catch (IOException ex)
	      {
	      }
	  }
      }
    else
      {
	Ganymede.debug(buildScript + " doesn't exist, not running external DNS build script");
      }

    return true;
  }
}
