/*

   GanymedeBuilderTask.java

   This class provides a template for code to be attached to the server to
   handle propagating data from the Ganymede object store into the wide
   world, via NIS, DNS, NIS+, LDAP, JNDI, JDBC, X, Y, Z, etc.
   
   Created: 17 February 1998
   Release: $Name:  $
   Version: $Revision: 1.11 $
   Last Mod Date: $Date: 2000/01/29 02:32:57 $
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
import java.util.zip.*;
import java.io.*;

import arlut.csd.Util.PathComplete;
import arlut.csd.Util.zipIt;

/*------------------------------------------------------------------------------
                                                                           class
                                                             GanymedeBuilderTask

------------------------------------------------------------------------------*/

/**
 * <P>This class provides a template for code to be attached to the server to
 * handle propagating data from the Ganymede object store into the wide
 * world, via NIS, DNS, NIS+, LDAP, JNDI, JDBC, X, Y, Z, etc.</P>
 *
 * <P>Subclasses of GanymedeBuilderTask need to implement builderPhase1()
 * and builderPhase2().  builderPhase1() is run while a dumpLock is established
 * on {@link arlut.csd.ganymede.DBStore DBStore}, guaranteeing a 
 * transaction-consistent database state.  builderPhase1() should do whatever
 * is required to write out files or otherwise propagate data out from the
 * database.  If builderPhase1() returns true, the dump lock is released and
 * builderPhase2() is run.  This method is intended to run external scripts 
 * and/or code that can process the files/data written out by builderPhase1()
 * without needing the database to remain locked.</P>
 *
 * <P>All subclasses of GanymedeBuilderTask need to be registered in the Ganymede
 * database via the task object type.  GanymedeBuilderTasks registered to be
 * run on database commit will automatically be issued by the
 * {@link arlut.csd.ganymede.GanymedeScheduler GanymedeScheduler} when transactions
 * commit.  The GanymedeScheduler is designed so that it will not re-issue a specific
 * task while a previous instance of the task is still running, so you don't have
 * to worry about builderPhase2() taking a fair amount of time.  builderPhase1() should
 * be as fast as possible, however, as no additional transactions will be able to
 * be committed until builderPhase1() completes.</P>
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public abstract class GanymedeBuilderTask implements Runnable, FilenameFilter {

  protected Date lastRunTime;
  protected Date oldLastRunTime;
  GanymedeSession session = null;
  DBDumpLock lock;
  String basePath;

  /* -- */

  /**
   * <P>This method is the main entry point for the GanymedeBuilderTask.  It
   * is responsible for setting up the environment for a builder task to
   * operate under, and for actually invoking the builder method.</P>
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
	    session = new GanymedeSession("builder");
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

	// update our time as soon as possible, so that any changes
	// that are made in the database after we release the dump
	// lock will have a time stamp after our 'last build' time
	// stamp.

	if (lastRunTime == null)
	  {
	    lastRunTime = new Date();
	  }
	else
	  {
	    if (oldLastRunTime == null)
	      {
		oldLastRunTime = new Date(lastRunTime.getTime());
	      }
	    else
	      {
		oldLastRunTime.setTime(lastRunTime.getTime());
	      }

	    lastRunTime.setTime(System.currentTimeMillis());
	  }

	success1 = this.builderPhase1();

	// release the lock, and so on
	
	if (session != null)
	  {
	    session.logout();	// will clear the dump lock
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
	    try
	      {
		session.logout();	// this will clear the dump lock if need be.
	      }
	    finally
	      {
		session = null;
		lock = null;
	      }
	  }
      }
  }

  /**
   * <P>This method is used by subclasses of GanymedeBuilderTask to
   * determine whether a particular base has had any modifications
   * made to it since the last time this builder task was run.</P>
   *
   * @param baseid The id number of the base to be checked
   */

  protected final boolean baseChanged(short baseid)
  {
    if (oldLastRunTime == null)
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
	    return base.lastChange.after(oldLastRunTime);
	  }
      }
  }

  /**
   * <P>This method is used by subclasses of GanymedeBuilderTask to
   * obtain a list of DBObject references of the requested
   * type.</P>
   *
   * <P>Note that the Enumeration returned by this method MUST NOT
   * be used after builderPhase1() returns.  This Enumeration is
   * only valid while the base in question is locked with the
   * global dumpLock obtained before builderPhase1() is run and
   * which is released after builderPhase1() returns.</P>
   *
   * @param baseid The id number of the base to be listed
   *
   * @return An Enumeration of {@link arlut.csd.ganymede.DBObject DBObject} references
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

    return base.objectTable.elements();
  }

  /**
   * <P>This method is used by subclasses of GanymedeBuilderTask to
   * obtain a reference to a {@link arlut.csd.ganymede.DBObject DBObject}
   * matching a given invid.</P>
   *
   * @param invid The object id of the object to be viewed
   */

  protected final DBObject getObject(Invid invid)
  {
    // this works only because we've already got our lock
    // established..  otherwise, we'd have to use the query system.

    if (lock == null)
      {
	throw new IllegalArgumentException("Can't call getObject without a lock");
      }

    return session.session.viewDBObject(invid);
  }

  /**
   * <P>This method is used by subclasses of GanymedeBuilderTask to
   * obtain the label for an object.</P>
   *
   * @param baseid The object id of the object label to be retrieved
   */

  protected final String getLabel(Invid invid)
  {
    return session.viewObjectLabel(invid);
  }

  /**
   * <P>This method is intended to be overridden by subclasses of
   * GanymedeBuilderTask.</P>
   *
   * <P>This method runs with a dumpLock obtained for the builder task.</P>
   *
   * <P>Code run in builderPhase1() can call enumerateObjects() and
   * baseChanged().  Note that the Enumeration of objects returned
   * by enumerateObjects() is only valid and should only be consulted
   * while builderPhase1 is running.. as soon as builderPhase1 returns,
   * the dumpLock used to make the enumerateObjects() call safe to
   * use is relinquished, and any Enumerations obtained will then
   * be unsafe to depend on.</P>
   *
   * @return true if builderPhase1 made changes necessitating the
   * execution of builderPhase2.
   */

  abstract public boolean builderPhase1();

  /**
   * <P>This method is intended to be overridden by subclasses of
   * GanymedeBuilderTask.</P>
   *
   * <P>This method runs after this task's dumpLock has been
   * relinquished.  This method is intended to be used to finish off a
   * build process by running (probably external) code that does not
   * require direct access to the database.</P>
   *
   * <P>For instance, for an NIS builder task, builderPhase1() would scan
   * the Ganymede object store and write out NIS-compatible source
   * files.  builderPhase1() would return, the run() method drops the
   * dump lock so that other transactions can be committed, and then
   * builderPhase2() can be run to turn those on-disk files written by
   * builderPhase1() into NIS maps.  This generally involves executing
   * an external Makefile, which can take an indeterminate period of
   * time.</P>
   *
   * <P>By releasing the dumpLock before we get to that point, we
   * minimize contention for users of the system.</P>
   *
   * <P>As a result of having dropped the dumpLock, enumerateObjects()
   * cannot be called by this method.</P>
   *
   * <P>builderPhase2 is only run if builderPhase1 returns true.</P>
   */

  abstract public boolean builderPhase2();

  /**
   * <P>This method takes care of backing up the existing output
   * files into a zip file.</P>
   */

  protected void backupFiles(String label) throws IOException
  {
    if (basePath == null)
      {
	basePath = System.getProperty("ganymede.builder.output");

	if (basePath == null)
	  {
	    throw new RuntimeException("GanymedeBuilder not able to determine output directory.");
	  }
	
	basePath = PathComplete.completePath(basePath);
      }

    File directory = new File(basePath);

    if (!directory.isDirectory())
      {
	throw new IOException("Error, couldn't find output directory to backup.");
      }

    File oldDirectory = new File(basePath + File.separator + "old");

    if (!oldDirectory.exists())
      {
	oldDirectory.mkdir();
      }

    String zipFileName = basePath + "old" + File.separator + label + ".zip";

    //    System.err.println("GanymedeBuilderTask.backups(): " + zipFileName);

    String filenames[] = directory.list(this);

    if (filenames.length > 0)
      {
	Vector filenameVect = new Vector();

	//    System.err.print("Zipping: ");

	for (int i = 0; i < filenames.length; i++)
	  {
	    System.err.print(filenames[i]);
	    System.err.print(" ");

	    File testFile = new File(basePath + filenames[i]);

	    if (!testFile.isDirectory())
	      {
		filenameVect.addElement(basePath + filenames[i]);
	      }
	  }
	
	if (filenameVect.size() > 0)
	  {
	    zipIt.createZipFile(zipFileName, filenameVect);
	  }
      }
  }

  /**
   * <P>This method comprises the FileNameFilter body, and is used to avoid
   * zipping existing zip files into new backups.</P>
   */

  public boolean accept(File dir, String name)
  {
    if (name.endsWith(".zip") || (name.endsWith(".ZIP")))
      {
	return false;
      }

    return true;
  }

}
