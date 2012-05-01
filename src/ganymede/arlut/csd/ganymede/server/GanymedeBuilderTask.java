/*

   GanymedeBuilderTask.java

   This class provides a template for code to be attached to the server to
   handle propagating data from the Ganymede object store into the wide
   world, via NIS, DNS, NIS+, LDAP, JNDI, JDBC, X, Y, Z, etc.

   Created: 17 February 1998

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import arlut.csd.Util.FileOps;
import arlut.csd.Util.PathComplete;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.zipIt;
import arlut.csd.ganymede.common.ClientMessage;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.scheduleHandle;
import arlut.csd.ganymede.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                             GanymedeBuilderTask

------------------------------------------------------------------------------*/

/**
 * <p>This class is designed to be subclassed in order to handle
 * full-state synchronization from the Ganymede server to external
 * directory service targets.</p>
 *
 * <p>GanymedeBuilderTask is really only intended to support directory
 * service targets that are 'dump and replace' in nature, like NIS and
 * classical DNS.  The reason for this is that GanymedeBuilderTasks
 * are executed asynchronously with respect to transaction commits.
 * By the time a GanymedeBuilderTask subclass runs, the Ganymede
 * server has forgotten all previous states of the data in the server.
 * All the GanymedeBuilderTask can do is check to see what kinds of
 * objects (Users, Groups, Systems, etc.) have changed since the last
 * time it was run.  It can also check to see which fields have been
 * changed, within those object types.  It cannot check to see whether
 * a particular field in a specific object has changed, however.
 * Since there's no way for a GanymedeBuilderTask to do a
 * before-and-after comparison, all it can reliably do is to write out
 * absolutely everything relevant to its synchronization channel, and
 * then hand off the build to an external process.  If you want to do
 * a more granular, before-and-after incremental build, you can
 * instead choose to use the more synchronous {@link
 * arlut.csd.ganymede.server.SyncRunner} class, which uses a standard
 * XML format to represent changes to external synchronization
 * processes.</p>
 *
 * <p>Subclasses of GanymedeBuilderTask need to implement the {@link
 * arlut.csd.ganymede.server.GanymedeBuilderTask#builderPhase1()} and
 * {@link
 * arlut.csd.ganymede.server.GanymedeBuilderTask#builderPhase2()}
 * methods.  builderPhase1() is run while a {@link
 * arlut.csd.ganymede.server.DBDumpLock} is asserted on the server's
 * {@link arlut.csd.ganymede.server.DBStore DBStore}, guaranteeing a
 * transaction-consistent database state that can be examined.
 * builderPhase1() should do whatever is required to examine the
 * database and to determine whether this builder task needs to carry
 * out a build.  If so, builderPhase1() should write out the data
 * files that will be needed for builderPhase2() and return true.
 * When builderPhase1() completes, the dump lock is released.  If
 * builderPhase1() returns true, the builderPhase2() method is then
 * run.  This method is intended to run external scripts (typically
 * written in Perl or Python) that process the files written out by
 * the builderPhase1() method.</p>
 *
 * <p>All subclasses of GanymedeBuilderTask need to be registered in
 * the Ganymede database via the Task object type.
 * GanymedeBuilderTasks registered to be run on database commit will
 * automatically be issued by the {@link
 * arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler} when
 * transactions commit.  The GanymedeScheduler is designed to run only
 * a single instance of a task at a time, waiting to issue any new
 * execution of the task until the previous execution completes.  A
 * GanymedeBuilderTask doesn't finish executing until its
 * builderPhase2() method returns.  This protects your builderPhase2()
 * method from having its input data being overwritten by the next
 * builderPhase1() method writing out new data.  It also sets a
 * minimum build-to-build latency, according to how long your
 * builderPhase1() and builderPhase2() methods take to complete.  No
 * matter how long builderPhase2() takes, the GanymedeScheduler will
 * run the builds as fast as it can, back-to-back.</p>
 *
 * <p>Your builderPhase1() method, however, should execute and
 * complete as fast as possible.  The dump lock protecting
 * builderPhase1() prevents any transactions from committing while a
 * builderPhase1() method is executing.  Any significant delay in
 * transaction commits may cause noticeable delays for your users.
 * Fortunately, since builderPhase1() implementations just scan the
 * Ganymede in-memory database and write out some text files, this
 * usually doesn't take very long.  If you find that your
 * builderPhase1() method is taking too long, you may want to consider
 * splitting your build into multiple builder tasks.  The
 * GanymedeBuilderTask is designed so that the server can execute
 * multiple distinct builder tasks concurrently.  Splitting your build
 * into multiple pieces that can be run concurrently can also improve
 * your build latency by reducing the amount of work that a given
 * external process has to do when called by builderPhase2().</p>
 *
 * <p>GanymedeBuilderTask includes a set of helper methods that
 * subclasses can take advantage of in order to facilitate their
 * operation.</p>
 *
 * <ul>
 * <li>The {@link
 * arlut.csd.ganymede.server.GanymedeBuilderTask#baseChanged(short)}
 * method can be used from {@link
 * arlut.csd.ganymede.server.GanymedeBuilderTask#builderPhase1()} to
 * check to see whether objects of a given type have been changed
 * since the builder task was last run.</li>
 *
 * <li>The {@link
 * arlut.csd.ganymede.server.GanymedeBuilderTask#getOptionValue(java.lang.String)}
 * method makes it possible for a builder task to retrieve
 * configuration information from the task object in the Ganymede
 * database which links the task into the server's scheduling.</li>
 *
 * <li>The {@link
 * arlut.csd.ganymede.server.GanymedeBuilderTask#openOutFile(java.lang.String,
 * java.lang.String)} method not only opens files for writing, it also
 * takes care to manage the archiving of old versions of the emitted
 * file.  The <code>ganymede.builder.backups</code> property controls
 * this behavior.  It should be set to a path for keeping zipped
 * copies of previous builder outputs if you want this archiving
 * feature to be in effect.  If it is, whenever files are opened by a
 * builder task using openOutFile(), previous copies of those files
 * will be moved to the directory set in
 * <code>ganymede.builder.backups</code>.  The first time after
 * midnight that openOutFile() is called by a builder task, all of the
 * files written out the previous day by the builder task will be
 * collected into a single zip file.  This can be handy, but you may
 * not actually want to have certain files archived.  In that case,
 * you should have your external sync script take care to remove such
 * files after processing them..</li>
 *
 * <li>The {@link
 * arlut.csd.ganymede.server.GanymedeBuilderTask#enumerateObjects(short)}
 * method can be used by builderPhase1() to get an Enumeration of
 * objects of a given type to examine for writing.</li>
 *</ul>
 *
 * <p>In addition, the GanymedeBuilderTask base class logic is
 * responsible for interfacing with the rest of the Ganymede server to
 * display each builder task's status, both in the admin console and
 * in the client.  The little conveyor belt icon in the lower left
 * corner of the Ganymede graphical client is controlled by the action
 * of the GanymedeBuilderTask and SyncRunner objects being run after a
 * transaction commit.</p>
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public abstract class GanymedeBuilderTask implements Runnable {

  private static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.GanymedeBuilderTask");

  private static String currentBackUpDirectory = null;
  private static String oldBackUpDirectory = null;

  /**
   * This hashtable maps directory paths to an Integer
   * counting the number of tasks that are currently
   * copying backup files to it.  If the current day's
   * directory path changes and this count goes to
   * zero, the old directory will be zipped up and
   * deleted.
   */

  private static Hashtable backupsBusy = new Hashtable();
  private static String basePath = null;
  private static long rollunderTime = 0;
  private static long rolloverTime = 0;
  private static boolean firstRun = true;
  private static int id = 0;

  /**
   * Count of the number of builder tasks currently
   * running in phase 1.
   */

  private static int phase1Count = 0;

  /**
   * Count of the number of builder tasks currently
   * running in phase 1.
   */

  private static int phase2Count = 0;

  /* --- */

  protected Date lastRunTime;
  protected Date oldLastRunTime;
  GanymedeSession session = null;
  DBDumpLock lock;
  Vector optionsCache = null;

  /**
   * If this flag is true, baseChanged() will always return true,
   * as a way of forcing consideration of all databases that might
   * be examined by GanymedeBuilderTask subclasses.
   */

  private boolean forceAllBases;

  /**
   * Must be protected so subclasses in a different package can
   * set this.
   */

  protected Invid taskDefObjInvid = null;

  /**
   * Will be true if this builder task should be scheduled when a
   * transaction is committed.
   */

  private boolean runOnCommit;

  /**
   * A scheduleHandle that we can use to update the admin consoles as
   * to our build status.
   */

  private scheduleHandle handle;

  /* -- */

  /**
   * Method used by the Ganymede scheduler to pass us a handle that we
   * can use to signal the admin console as to our success or failure.
   */

  public void setScheduleHandle(scheduleHandle handle)
  {
    this.handle = handle;
  }

  /**
   * This method is the main entry point for the GanymedeBuilderTask.  It
   * is responsible for setting up the environment for a builder task to
   * operate under, and for actually invoking the builder method.
   */

  public final void run()
  {
    this.run(null);
  }

  /**
   * This method is the main entry point for the GanymedeBuilderTask.  It
   * is responsible for setting up the environment for a builder task to
   * operate under, and for actually invoking the builder method.
   */

  public final void run(Object options[])
  {
    String label = null;
    Thread currentThread = java.lang.Thread.currentThread();
    boolean
      success1 = false;
    boolean alreadyDecdCount = false;

    /* -- */

    String shutdownState = GanymedeServer.shutdownSemaphore.checkEnabled();

    if (shutdownState != null)
      {
	// "Aborting builder task {0} for shutdown condition: {1}"
	Ganymede.debug(ts.l("run.shutting_down", this.getClass().getName(), shutdownState));
	return;
      }

    if (options == null)
      {
	this.forceAllBases = false;
      }
    else
      {
	for (int i = 0; i < options.length; i++)
	  {
	    if (options[i] instanceof String)
	      {
		String x = (String) options[i];

		if (x.equals("forcebuild"))
		  {
		    this.forceAllBases = true;
		  }
	      }
	  }
      }

    try
      {
	// the scheduler should make sure we are never in progress
	// more than once concurrently, but it won't hurt to clear the
	// optionsCache up front just in case

	optionsCache = null;

	incPhase1(true);

	try
	  {
	    // we need a unique label for our session so that multiple
	    // builder tasks can have their own lock keys.. our label will
	    // start at builder:0 and work our way up as we go along
	    // during the server's lifetime

	    synchronized (GanymedeBuilderTask.class)
	      {
		// "builder: {0,number,#}"
		label = ts.l("run.label_pattern", Integer.valueOf(id++));
	      }

	    session = new GanymedeSession(label);

	    try
	      {
		lock = session.getSession().openDumpLock();
	      }
	    catch (InterruptedException ex)
	      {
		// "Could not run task {0}, couldn''t get dump lock."
		Ganymede.debug(ts.l("run.failed_lock_acquisition", this.getClass().getName()));
		return;
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
	  }
	catch (Exception ex)
	  {
	    decPhase1(true);
	    alreadyDecdCount = true;

	    Ganymede.logError(ex);
	    return;
	  }
	finally
	  {
	    if (!alreadyDecdCount)
	      {
		decPhase1(false); // false since we don't want to force stat update yet
	      }

	    // release the lock, and so on

	    if (session != null)
	      {
		try
		  {
		    session.logout();	// will clear the dump lock
		  }
		catch (NotLoggedInException ex)
		  {
		  }

		session = null;
		lock = null;
	      }
	  }

	if (currentThread.isInterrupted())
	  {
	    // "Builder task {0} interrupted, not doing network build."
	    Ganymede.debug(ts.l("run.task_interrupted", this.getClass().getName()));
	    Ganymede.updateBuildStatus();
	    return;
	  }

	try
	  {
	    incPhase2(true);

	    if (success1)
	      {
                shutdownState = GanymedeServer.shutdownSemaphore.increment();

                if (shutdownState != null)
                  {
                    // "Aborting builder task {0} for shutdown condition: {1}"
                    Ganymede.debug(ts.l("run.shutting_down", this.getClass().getName(), shutdownState));
                    return;
                  }

		try
		  {
		    this.builderPhase2();

		    handle.setTaskStatus(scheduleHandle.TaskStatus.OK, 0, "");
		  }
		catch (ServiceNotFoundException ex)
		  {
		    handle.setTaskStatus(scheduleHandle.TaskStatus.SERVICEERROR, 0, ex.getMessage());
		  }
		catch (ServiceFailedException ex)
		  {
		    handle.setTaskStatus(scheduleHandle.TaskStatus.SERVICEFAIL, 0, ex.getMessage());
		  }
		catch (Exception ex)
		  {
		    handle.setTaskStatus(scheduleHandle.TaskStatus.FAIL, 0, ex.getMessage());
		  }
		finally
		  {
		    GanymedeServer.shutdownSemaphore.decrement();
		  }
	      }
	  }
	finally
	  {
	    decPhase2(true);
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
	    catch (NotLoggedInException ex)
	      {
	      }
	    finally
	      {
		session = null;
		lock = null;
	      }
	  }

	// and again, just in case

	optionsCache = null;
      }
  }

  /**
   * <p>This method is used by subclasses of GanymedeBuilderTask to
   * determine whether a particular base has had any modifications
   * made to it since the last time this builder task was run.  This
   * method works because each GanymedeBuilderTask object keeps a
   * timestamp which records the last time the builder task ran.
   * baseChanged() just compares that time stamp against the {@link
   * arlut.csd.ganymede.server.DBObjectBase#lastChange} time stamp
   * that the {@link arlut.csd.ganymede.server.DBObjectBase} class
   * maintains at transaction commit.  Note that this method will
   * always return true the first time a particular builder task is
   * run after the server is started.  This means that the first time
   * a transaction is committed when you start your server, your
   * builder task will wind up doing a full build.</p>
   *
   * <p>See also the {@link
   * arlut.csd.ganymede.server.GanymedeBuilderTask#baseChanged(short,
   * java.util.List)} version of this method, which allows you to
   * specify a list of fields that you are interested in testing.</p>
   *
   * @param baseid The id number of the base to be checked
   */

  protected final boolean baseChanged(short baseid)
  {
    if (forceAllBases || (oldLastRunTime == null))
      {
	return true;
      }
    else
      {
	DBObjectBase base = Ganymede.db.getObjectBase(baseid);

        if (base == null)
          {
            Ganymede.debug("GanymedeBuilderTask.baseChanged(): attempted to lookup non-existent object base " + baseid);

            return false;
          }

	if (base.getTimeStamp() == null)
	  {
	    return false;
	  }
	else
	  {
	    return base.getTimeStamp().after(oldLastRunTime);
	  }
      }
  }

  /**
   * <p>This method is used by subclasses of GanymedeBuilderTask to
   * determine whether a particular base has had any modifications
   * made to it since the last time this builder task was run.  This
   * method works because each GanymedeBuilderTask object keeps a
   * timestamp which records the last time the builder task ran.
   * baseChanged() just compares that time stamp against the {@link
   * arlut.csd.ganymede.server.DBObjectBase#lastChange} time stamp
   * that the {@link arlut.csd.ganymede.server.DBObjectBase} class
   * maintains at transaction commit.  Note that this method will
   * always return true the first time a particular builder task is
   * run after the server is started.  This means that the first time
   * a transaction is committed when you start your server, your
   * builder task will wind up doing a full build.</p>
   *
   * <p>See also the {@link
   * arlut.csd.ganymede.server.GanymedeBuilderTask#baseChanged(short,
   * java.util.List)} version of this method, which allows you to
   * specify a list of fields that you are interested in testing.</p>
   *
   * <p>Note: This variant of baseChanged() takes an int and casts it to
   * a short to remove the need from casting literals on the caller's
   * behalf.  If the baseid does not fit in the 16 bit two's
   * complement short range, an IllegalArgumentExceptio will be
   * thrown.</p>
   *
   * @param baseid The id number of the base to be checked
   */

  protected final boolean baseChanged(int baseid)
  {
    if (baseid < 0 || baseid > Short.MAX_VALUE)
      {
	throw new IllegalArgumentException("Out of range value: " + baseid);
      }

    return this.baseChanged((short) baseid);
  }

  /**
   * <p>This method is used by subclasses of GanymedeBuilderTask to
   * determine whether a particular field of a particular base has had
   * any modifications made to it since the last time this builder
   * task was run.  This method works because each GanymedeBuilderTask
   * object keeps a set of timestamp which records the last time the
   * builder task ran.  baseChanged() just compares that time stamp
   * against the {@link
   * arlut.csd.ganymede.server.DBObjectBaseField#lastChange} time
   * stamp that the {@link
   * arlut.csd.ganymede.server.DBObjectBaseField} class maintains at
   * transaction commit.  Note that this method will always return
   * true the first time a particular builder task is run after the
   * server is started.  This means that the first time a transaction
   * is committed when you start your server, your builder task will
   * wind up doing a full build.</p>
   *
   * <p>Otherwise, if none of the fields listed have changed since
   * this GanymedeBuilderTask was last run, baseChanged() will return
   * false.  If any of the fields in the fieldIds list have changed
   * since this GanymedeBuilderTask last ran, baseChanged() will
   * return true.</p>
   *
   * @param baseid The id number of the base to be checked
   * @param fieldIds A list of java.lang.Short's containing the
   * id numbers of the fields to examine.
   */

  protected final boolean baseChanged(short baseid, List fieldIds)
  {
    if (forceAllBases || (oldLastRunTime == null))
      {
	return true;
      }

    DBObjectBase base = Ganymede.db.getObjectBase(baseid);

    if (base == null)
      {
        Ganymede.debug("GanymedeBuilderTask.baseChanged(): attempted to lookup non-existent object base " + baseid);

        return false;
      }

    // if the base in question hasn't changed at all since our last
    // build, we don't need to worry about looking at the individual
    // fields.

    if (base.getTimeStamp() == null || !base.getTimeStamp().after(oldLastRunTime))
      {
	return false;
      }

    // now we check out each field

    if (fieldIds == null || fieldIds.size() == 0)
      {
	// "Null or empty fieldIds arguments"
	throw new IllegalArgumentException(ts.l("baseChanged.empty"));
      }

    DBObjectBaseField fieldDef = null;

    Iterator iterator = fieldIds.iterator();

    while (iterator.hasNext())
      {
	Short idObj = (Short) iterator.next();
	fieldDef = (DBObjectBaseField) base.getField(idObj);

	if (fieldDef != null && fieldDef.getTimeStamp().after(oldLastRunTime))
	  {
	    return true;
	  }
      }

    return false;
  }

  /**
   * <p>This method is used by subclasses of GanymedeBuilderTask to
   * determine whether a particular field of a particular base has had
   * any modifications made to it since the last time this builder
   * task was run.  This method works because each GanymedeBuilderTask
   * object keeps a set of timestamp which records the last time the
   * builder task ran.  baseChanged() just compares that time stamp
   * against the {@link
   * arlut.csd.ganymede.server.DBObjectBaseField#lastChange} time
   * stamp that the {@link
   * arlut.csd.ganymede.server.DBObjectBaseField} class maintains at
   * transaction commit.  Note that this method will always return
   * true the first time a particular builder task is run after the
   * server is started.  This means that the first time a transaction
   * is committed when you start your server, your builder task will
   * wind up doing a full build.</p>
   *
   * <p>Otherwise, if none of the fields listed have changed since
   * this GanymedeBuilderTask was last run, baseChanged() will return
   * false.  If any of the fields in the fieldIds list have changed
   * since this GanymedeBuilderTask last ran, baseChanged() will
   * return true.</p>
   *
   * <p>Note: This variant of baseChanged() takes an int and casts it
   * to a short to remove the need from casting literals on the
   * caller's behalf.  If the baseid does not fit in the 16 bit two's
   * complement short range, an IllegalArgumentException will be
   * thrown.</p>
   *
   * @param baseid The id number of the base to be checked
   * @param fieldIds A list of java.lang.Short's containing the
   * id numbers of the fields to examine.
   */

  protected final boolean baseChanged(int baseid, List fieldIds)
  {
    if (baseid < 0 || baseid > Short.MAX_VALUE)
      {
	throw new IllegalArgumentException("Out of range value: " + baseid);
      }

    return this.baseChanged((short) baseid, fieldIds);
  }

  /**
   * <p>This method is used by subclasses of GanymedeBuilderTask to
   * obtain a list of DBObject references of the requested
   * type.</p>
   *
   * <p>Note that the Enumeration returned by this method MUST NOT
   * be used after builderPhase1() returns.  This Enumeration is
   * only valid while the base in question is locked with the
   * global dumpLock obtained before builderPhase1() is run and
   * which is released after builderPhase1() returns.</p>
   *
   * @param baseid The id number of the base to be listed
   *
   * @return An Enumeration of {@link arlut.csd.ganymede.server.DBObject DBObject} references
   */

  protected final Enumeration enumerateObjects(short baseid)
  {
    // this works only because we've already got our lock
    // established..  otherwise, we'd have to use the query system.

    if (lock == null)
      {
	// "Can''t call enumerateObjects without a lock."
	throw new IllegalArgumentException(ts.l("enumerateObjects.no_lock"));
      }

    DBObjectBase base = Ganymede.db.getObjectBase(baseid);

    return base.getObjectsEnum();
  }

  /**
   * <p>This method is used by subclasses of GanymedeBuilderTask to
   * obtain a list of DBObject references of the requested
   * type.</p>
   *
   * <p>Note that the Enumeration returned by this method MUST NOT
   * be used after builderPhase1() returns.  This Enumeration is
   * only valid while the base in question is locked with the
   * global dumpLock obtained before builderPhase1() is run and
   * which is released after builderPhase1() returns.</p>
   *
   * <p>Note: This variant of enumerateObjects takes an int and casts it
   * to a short to remove the need from casting constants on the
   * caller's behalf.  If the baseid does not fit in the 16 bit two's
   * complement short range, an IllegalArgumentException will be
   * thrown.</p>
   *
   * @param baseid The id number of the base to be listed
   *
   * @return An Enumeration of {@link arlut.csd.ganymede.server.DBObject DBObject} references
   */

  protected final Enumeration enumerateObjects(int baseid)
  {
    if (baseid < 0 || baseid > Short.MAX_VALUE)
      {
	throw new IllegalArgumentException("Out of range value: " + baseid);
      }

    return this.enumerateObjects((short) baseid);
  }

  /**
   * <p>This method is used by subclasses of GanymedeBuilderTask to
   * obtain a list of DBObject references of the requested
   * type.</p>
   *
   * <p>Note that the Iterable returned by this method MUST NOT
   * be used after builderPhase1() returns.  This Iterable is
   * only valid while the base in question is locked with the
   * global dumpLock obtained before builderPhase1() is run and
   * which is released after builderPhase1() returns.</p>
   *
   * @param baseid The id number of the base to be listed
   *
   * @return An Iterable of {@link arlut.csd.ganymede.server.DBObject
   * DBObject} references
   */

  protected final Iterable<DBObject> getObjects(short baseid)
  {
    // this works only because we've already got our lock
    // established..  otherwise, we'd have to use the query system.

    if (lock == null)
      {
	// "Can''t call enumerateObjects without a lock."
	throw new IllegalArgumentException(ts.l("enumerateObjects.no_lock"));
      }

    DBObjectBase base = Ganymede.db.getObjectBase(baseid);

    return base.getObjects();
  }

  /**
   * <p>This method is used by subclasses of GanymedeBuilderTask to
   * obtain a list of DBObject references of the requested
   * type.</p>
   *
   * <p>Note that the Iterable returned by this method MUST NOT
   * be used after builderPhase1() returns.  This Iterable is
   * only valid while the base in question is locked with the
   * global dumpLock obtained before builderPhase1() is run and
   * which is released after builderPhase1() returns.</p>
   *
   * <p>Note: This variant of getObjects() takes an int and casts it
   * to a short to remove the need from casting constants on the
   * caller's behalf.  If the baseid does not fit in the 16 bit two's
   * complement short range, an IllegalArgumentException will be
   * thrown.</p>
   *
   * @param baseid The id number of the base to be listed
   *
   * @return An Iterable of {@link arlut.csd.ganymede.server.DBObject
   * DBObject} references
   */

  protected final Iterable<DBObject> getObjects(int baseid)
  {
    if (baseid < 0 || baseid > Short.MAX_VALUE)
      {
	throw new IllegalArgumentException("Out of range value: " + baseid);
      }

    return this.getObjects((short) baseid);
  }

  /**
   * This method is used by subclasses of GanymedeBuilderTask to
   * obtain a reference to a {@link arlut.csd.ganymede.server.DBObject DBObject}
   * matching a given invid.
   *
   * @param invid The object id of the object to be viewed
   */

  protected final DBObject getObject(Invid invid)
  {
    return session.getSession().viewDBObject(invid);
  }

  /**
   * This method is used by subclasses of GanymedeBuilderTask to
   * obtain the label for an object.
   *
   * @param invid The object id of the object label to be retrieved
   */

  protected final String getLabel(Invid invid)
  {
    return session.getSession().getObjectLabel(invid);
  }

  /**
   * Finds an invid by title.
   */

  protected final Invid findLabeledObject(String label, short type)
  {
    try
      {
	return session.findLabeledObject(label, type);
      }
    catch (NotLoggedInException ex)
      {
	throw new RuntimeException(ex);
      }
  }

  /**
   * <p>This method is intended to be overridden by subclasses of
   * GanymedeBuilderTask.</p>
   *
   * <p>This method runs with a dumpLock obtained for the builder task.</p>
   *
   * <p>Code run in builderPhase1() can call enumerateObjects() and
   * baseChanged().  Note that the Enumeration of objects returned
   * by enumerateObjects() is only valid and should only be consulted
   * while builderPhase1 is running.. as soon as builderPhase1 returns,
   * the dumpLock used to make the enumerateObjects() call safe to
   * use is relinquished, and any Enumerations obtained will then
   * be unsafe to depend on.</p>
   *
   * @return true if builderPhase1 made changes necessitating the
   * execution of builderPhase2.
   */

  abstract public boolean builderPhase1();

  /**
   * <p>This method is intended to be overridden by subclasses of
   * GanymedeBuilderTask.</p>
   *
   * <p>This method runs after this task's dumpLock has been
   * relinquished.  This method is intended to be used to finish off a
   * build process by running (probably external) code that does not
   * require direct access to the database.</p>
   *
   * <p>For instance, for an NIS builder task, builderPhase1() would scan
   * the Ganymede object store and write out NIS-compatible source
   * files.  builderPhase1() would return, the run() method drops the
   * dump lock so that other transactions can be committed, and then
   * builderPhase2() can be run to turn those on-disk files written by
   * builderPhase1() into NIS maps.  This generally involves executing
   * an external Makefile, which can take an indeterminate period of
   * time.</p>
   *
   * <p>It is important that any external process run by
   * builderPhase2() blocks until it is finished doing the build.  If
   * the external script tries to put itself into the background and
   * return early, the Ganymede server will conclude that the external
   * build has completely finished, and it will feel free to
   * immediately schedule this builder task again, which may mean that
   * the builderPhase1() method will overwrite files the backgrounded
   * external builder process is still using.</p>
   *
   * <p>Note as well that the Ganymede server makes no guarantee as to
   * what the environment variables or current working directory will
   * be set to when any external builder scripts are executed.  If
   * your external scripts depend on these things, you should make
   * sure that your external builder script sets them itself.</p>
   *
   * <p>By releasing the dumpLock before we get to that point, we
   * minimize contention for users of the system.</p>
   *
   * <p>As a result of having dropped the dumpLock, enumerateObjects()
   * cannot be called by this method.</p>
   *
   * <p>builderPhase2() can throw a ServiceNotFoundException or
   * ServiceFailedException to indicate to the admin console that the
   * build failed, or it can simply return a boolean result to
   * indicate the same in a less specific fashion.</p>
   *
   * <p>builderPhase2 is only run if builderPhase1 returns true.</p>
   */

  abstract public boolean builderPhase2();

  /**
   * <p>This method looks in the optionStrings field in the task
   * object associated with this task and determines whether the given
   * option name is present in the field.  This works only if this
   * builder task was registered with taskDefObjInvid set by a
   * subclass whose constructor takes an Invid parameter and which
   * sets taskDefObjInvid in GanymedeBuilderTask.</p>
   *
   * <p>That is, if the task object for this task has an option strings
   * vector with the following contents:</p>
   *
   * <pre>
   *  useMD5
   *  useShadow
   * </pre>
   *
   * <p>then a call to isOptionSet with 'useMD5' or 'useShadow', of
   * any capitalization, will return true.  Any other parameter
   * provided to isOptionSet() will cause null to be returned.</p>
   */

  protected boolean isOptionSet(String option)
  {
    if (option != null && !option.equals(""))
      {
	Vector options = optionsCache;

	if (options == null)
	  {
	    optionsCache = getOptionStrings();
	    options = optionsCache;
	  }

	if (options == null)
	  {
	    return false;
	  }

	for (int i = 0; i < options.size(); i++)
	  {
	    String x = (String) options.elementAt(i);

	    if (x.equalsIgnoreCase(option))
	      {
		return true;
	      }
	  }
      }

    return false;
  }

  /**
   * <p>This method retrieves the value associated with the provided
   * option name if this builder task was registered with taskDefObjInvid
   * set by a subclass whose constructor takes an Invid parameter and which
   * sets taskDefObjInvid in GanymedeBuilderTask.</p>
   *
   * <p>getOptionValue() will search through the option strings for
   * the task object associated with this task and return the
   * substring after the '=' character, if the option name is found on
   * the left.</p>
   *
   * <p>That is, if the task object for this task has an option strings
   * vector with the following contents:</p>
   *
   * <pre>
   *  useMD5
   *  buildPath=/var/ganymede/schema/NT
   *  useShadow
   * </pre>
   *
   * <p>then a call to getOptionValue() with 'buildPath', of any capitalization,
   * as the parameter will return '/var/ganymede/schema/NT'.</p>
   *
   * <p>Any other parameter provided to getOptionValue() will cause null to
   * be returned.</p>
   */

  protected String getOptionValue(String option)
  {
    if (option != null && !option.equals(""))
      {
	Vector options = optionsCache;

	if (options == null)
	  {
	    optionsCache = getOptionStrings();
	    options = optionsCache;
	  }

	if (options == null)
	  {
	    return null;
	  }

	// get the prefix we'll search for

	String matchPat = option + "=";

	// and spin til we find it

	for (int i = 0; i < options.size(); i++)
	  {
	    String x = (String) options.elementAt(i);

	    if (x.startsWith(matchPat))
	      {
		return x.substring(matchPat.length());
	      }
	  }
      }

    return null;
  }

  /**
   * This method returns the Vector of option strings registered
   * for this task object in the Ganymede database, or null if no
   * option strings are defined.
   */

  final Vector getOptionStrings()
  {
    if (taskDefObjInvid != null)
      {
	DBObject taskDefObj = getObject(taskDefObjInvid);

	Vector options = taskDefObj.getFieldValuesLocal(SchemaConstants.TaskOptionStrings);

	if (options == null || options.size() == 0)
	  {
	    return null;
	  }

	// dup the vector for safety, since we are getting direct
	// access to the Vector in the database

	return (Vector) options.clone();
      }

    return null;
  }

  /**
   * Returns true if this builder task should be scheduled when a
   * transaction commits.
   */

  public final boolean runsOnCommit()
  {
    return this.runOnCommit;
  }

  /**
   * Call this method to control whether or not this builder task
   * should be run when the Ganymede server commits a transaction.
   */

  public void runOnCommit(boolean state)
  {
    this.runOnCommit = state;
  }

  /**
   * <p>This method opens the specified file for writing out a text stream.</p>
   *
   * <p>If the <code>ganymede.builder.backups</code> property is set to a
   * path in the Ganymede server's ganymede.properties file,
   * openOutFile() will look to see if the filename provided as a
   * parameter already exists.  If it does, it will be copied to a
   * subdirectory of the <code>ganymede.builder.backups</code>
   * directory.  This subdirectory will be named with the date in
   * which the backups therein were copied.</p>
   *
   * <p>If <code>ganymede.builder.backups</code> is set, the first time
   * openOutFile() is called after midnight, openOutFile will zip all
   * the files in any preceding days' backup subdirectories into one
   * zip file per day.</p>
   *
   * @param filename The fully specified path to the file to open
   */

  protected synchronized PrintWriter openOutFile(String filename) throws IOException
  {
    return openOutFile(filename, null);
  }

  /**
   * <p>This method opens the specified file for writing out a text stream.</p>
   *
   * <p>If the <code>ganymede.builder.backups</code> property is set to a
   * path in the Ganymede server's ganymede.properties file,
   * openOutFile() will look to see if the filename provided as a
   * parameter already exists.  If it does, it will be copied to a
   * subdirectory of the <code>ganymede.builder.backups</code>
   * directory.  This subdirectory will be named with the date in
   * which the backups therein were copied.</p>
   *
   * <p>If <code>ganymede.builder.backups</code> is set, the first time
   * openOutFile() is called after midnight, openOutFile will zip all
   * the files in any preceding days' backup subdirectories into one
   * zip file per day.</p>
   *
   * @param filename The name of the file to open
   * @param taskName The name of the builder task that is writing this file.  Used
   * to create a unique name (across tasks) for the backup copy of the file when
   * we overwrite an existing file.
   */

  protected synchronized PrintWriter openOutFile(String filename, String taskName) throws IOException
  {
    String backupFileName = null;
    File file, backupFile;
    String directory;

    /* -- */

    openBackupDirectory(filename);

    synchronized (GanymedeBuilderTask.class)
      {
	directory = currentBackUpDirectory;

	if (directory != null && !directory.equals(""))
	  {
	    incBusy(directory);
	  }
      }

    if (directory != null && !directory.equals(""))
      {
	try
	  {
	    // see if we have a file by the given name in the backup directory..
	    // if we do, we can't overwrite it

	    file = new File(filename);

	    if (file.exists())
	      {
		Date oldTime = new Date(file.lastModified());

		DateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH:mm:ss",
							    java.util.Locale.US);

		String label = formatter.format(oldTime);

		if (taskName != null)
		  {
		    backupFileName = directory + File.separator + taskName + "_" + label + "_" + file.getName();
		  }
		else
		  {
		    backupFileName = directory + File.separator + label + "_" + file.getName();
		  }

		backupFile = new File(backupFileName);

		// now, we could in principle have more than one copy
		// of a given file name written out in the same
		// second, so just for grins we'll make sure to
		// distinguish

		char subSec = 'a';

		while (backupFile.exists())
		  {
		    String extName = backupFileName + subSec++;

		    backupFile = new File(extName);
		  }

		if (!arlut.csd.Util.FileOps.copyFile(filename, backupFile.getCanonicalPath()))
		  {
		    return null;
		  }
	      }
	  }
	finally
	  {
	    decBusy(currentBackUpDirectory);
	  }
      }

    // we'll go ahead and write over the file if it exists.. that
    // way, we preserve directory permissions

    return new PrintWriter(new BufferedWriter(new FileWriter(filename)));
  }

  //
  // static methods
  //

  private static synchronized void incBusy(String path)
  {
    Integer x = (Integer) backupsBusy.get(path);

    if (x == null)
      {
	backupsBusy.put(path, Integer.valueOf(1));
      }
    else
      {
	backupsBusy.put(path, Integer.valueOf(x.intValue() + 1));
      }
  }

  private static synchronized void decBusy(String path)
  {
    Integer x = (Integer) backupsBusy.get(path);

    // we never should get a null value here.. go ahead
    // and throw NullPointerException if we do

    int val = x.intValue();

    if (val == 1)
      {
	backupsBusy.remove(path);
      }
    else
      {
	backupsBusy.put(path, Integer.valueOf(val - 1));
      }

    if (oldBackUpDirectory != null && oldBackUpDirectory.equals(path))
      {
	if (val == 0)
	  {
	    // ah, no one is busy writing back ups into
	    // path any more

	    String zipName = oldBackUpDirectory + ".zip";

	    try
	      {
		if (zipIt.zipDirectory(oldBackUpDirectory, zipName))
		  {
		    FileOps.deleteDirectory(oldBackUpDirectory);
		  }

		oldBackUpDirectory = null;
	      }
	    catch (IOException ex)
	      {
	      }
	  }
      }
  }

  private static synchronized int busyCount(String path)
  {
    Integer x = (Integer) backupsBusy.get(path);

    if (x == null)
      {
	return 0;
      }

    return x.intValue();
  }

  /**
   * This method is called before the server's builder
   * tasks are run and creates a backup directory for
   * files to be copied to.
   */

  private static synchronized void openBackupDirectory(String filename) throws IOException
  {
    if (basePath == null)
      {
	basePath = System.getProperty("ganymede.builder.backups");

	if (basePath == null || basePath.equals(""))
	  {
	    // "GanymedeBuilderTask not able to determine backups directory from Ganymede property file."
	    Ganymede.debug(ts.l("openBackupDirectory.no_directory_defined"));

	    return;
	  }

	basePath = PathComplete.completePath(basePath);
      }

    File directory = new File(basePath);

    if (!directory.exists())
      {
	// "Warning, can''t find ganymede.builder.backup directory {0}.  Not backing up {1}."
	Ganymede.debug(ts.l("openBackupDirectory.no_such_directory", basePath, filename));

	return;
      }

    if (!directory.isDirectory())
      {
	// "Warning, ganymede.builder.backup path {0} is not a directory.  Not backing up {1}."
	Ganymede.debug(ts.l("openBackupDirectory.not_a_directory",  basePath, filename));

	return;
      }

    if (!directory.canWrite())
      {
	// "Warning, can''t write to ganymede.builder.backup path {0}.  Not backing up {1}."
	Ganymede.debug(ts.l("openBackupDirectory.not_writeable", basePath, filename));

	return;
      }

    // okay, we've located our backup directory.. now make sure we
    // know what subdirectory thereunder we're going to use for
    // backups

    if ((currentBackUpDirectory == null) ||
	(System.currentTimeMillis() > rolloverTime) ||
	(System.currentTimeMillis() < rollunderTime))
      {
	Calendar nowCal = new GregorianCalendar();

	int year = nowCal.get(Calendar.YEAR);
	int month = nowCal.get(Calendar.MONTH);
	int day = nowCal.get(Calendar.DAY_OF_MONTH);

	// get a calendar representing 12am midnight local time

	Calendar cal = new GregorianCalendar(year, month, day);

	// first get our roll under time, in case the system
	// clock is ever set back

	Date todayMidnight = cal.getTime();
	rollunderTime = todayMidnight.getTime();

	// and now our roll over time

	cal.add(Calendar.DATE, 1);

	Date tomorrowMidnight = cal.getTime();
	rolloverTime = tomorrowMidnight.getTime();

	// if this is our first run of a builder task's file io prep,
	// sweep through the backup directory and zip up any directories
	// that match our pattern for day directories, before we create
	// one for today's date

	if (firstRun)
	  {
	    try
	      {
		cleanBackupDirectory();
	      }
	    finally
	      {
		firstRun = false;
	      }
	  }

	// okay, we've got our goal posts fixed, now handle the
	// old directory and get a label for the new

	DateFormat formatter = new SimpleDateFormat("yyyy_MM_dd", java.util.Locale.US);

	oldBackUpDirectory = currentBackUpDirectory;
	currentBackUpDirectory = basePath + File.separator + formatter.format(todayMidnight);

	File newDirectory = new File(currentBackUpDirectory);

	if (!newDirectory.exists())
	  {
	    if (!newDirectory.mkdir())
              {
                throw new IOException("Couldn't mkdir " + currentBackUpDirectory);
              }
	  }
      }

    // if we haven't zipped up our old directory, do that

    if (oldBackUpDirectory != null)
      {
	try
	  {
	    if (busyCount(oldBackUpDirectory) == 0)
	      {
		String zipName = oldBackUpDirectory + ".zip";

		// "GanymedeBuilderTask.openBackupDirectory(): trying to zip {0}."
		Ganymede.debug(ts.l("openBackupDirectory.zipping", oldBackUpDirectory));

		if (zipIt.zipDirectory(oldBackUpDirectory, zipName))
		  {
		    // "GanymedeBuilderTask.openBackupDirectory(): zipped {0}."
		    Ganymede.debug(ts.l("openBackupDirectory.zipped", zipName));
		    FileOps.deleteDirectory(oldBackUpDirectory);
		  }
		else
		  {
		    File dirFile = new File(oldBackUpDirectory);

		    if (dirFile.canRead())
		      {
			String[] list = dirFile.list();

			if (list == null || list.length == 0)
			  {
			    // "GanymedeBuilderTask.openBackupDirectory(): directory {0} is empty, deleting it."
			    Ganymede.debug(ts.l("openBackupDirectory.skipping_empty", oldBackUpDirectory));

			    FileOps.deleteDirectory(oldBackUpDirectory);
			  }
		      }
		  }
	      }
	  }
	finally
	  {
	    oldBackUpDirectory = null;
	  }
      }
  }

  /**
   * This static method is run before the first time a builder task
   * writes any file on server start-up.  It is responsible for sweeping
   * through the system backup directory and zipping up any day directories
   * that are lingering from earlier runs.
   */

  private static synchronized void cleanBackupDirectory() throws IOException
  {
    if (basePath == null || basePath.equals(""))
      {
	return;
      }

    File directory = new File(basePath);

    if (!directory.exists() || !directory.isDirectory() || !directory.canWrite())
      {
	return;
      }

    java.util.regex.Pattern regexp = null;

    try
      {
	regexp = java.util.regex.Pattern.compile("(\\d\\d\\d\\d)_(\\d\\d)_(\\d\\d)");
      }
    catch (java.util.regex.PatternSyntaxException ex)
      {
	// assuming we get the pattern right, this shouldn't happen

	Ganymede.logError(ex);
	return;
      }

    String names[] = directory.list();

    for (int i = 0; i < names.length; i++)
      {
	String dirName = basePath + names[i];

	if (names[i].endsWith(".zip"))
	  {
	    continue;
	  }

	File test = new File(directory, names[i]);

	if (!test.isDirectory())
	  {
	    continue;
	  }

	if (debug)
	  {
	    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): trying to match " + names[i]);
	  }

	java.util.regex.Matcher match = regexp.matcher(names[i]);

	if (match == null || !match.find())
	  {
	    continue;
	  }

	if (debug)
	  {
	    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): matched " + names[i]);
	  }

	String yearString, monthString, dateString;

	yearString = names[i].substring(match.start(1), match.end(1));
	monthString = names[i].substring(match.start(2), match.end(2));
	dateString = names[i].substring(match.start(3), match.end(3));

	if (debug)
	  {
	    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): yearString " + yearString);
	    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): monthString " + monthString);
	    Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): dateString " + dateString);
	  }

	try
	  {
	    if (debug)
	      {
		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): trying to zip " + basePath + names[i]);
	      }

	    int year = Integer.parseInt(yearString);
	    int month = Integer.parseInt(monthString);
	    int date = Integer.parseInt(dateString);

	    Calendar cal = new GregorianCalendar(year, month-1, date-1); // midnight start of day
	    cal.add(Calendar.DATE, 1); // end of day

	    if (debug)
	      {
		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): old directory time is " + cal.getTime());
		Ganymede.debug("GanymedeBuilderTask.cleanBackupDirectory(): rollunder time is " + new Date(rollunderTime));
	      }

	    if (cal.getTime().getTime() < rollunderTime)
	      {
		String zipName = dirName + ".zip";

		// "GanymedeBuilderTask.cleanBackupDirectory(): zipping {0}."
		Ganymede.debug(ts.l("cleanBackupDirectory.zipping", dirName));

		// it is conceivable that we have successfully zipped
		// a directory before, but did not delete the
		// directory for some reason.. if so, just leave
		// everything alone so that a human can deal with it

		File zipFile = new File(zipName);

		if (!zipFile.exists())
		  {
		    if (zipIt.zipDirectory(dirName, zipName))
		      {
			// "GanymedeBuilderTask.cleanBackupDirectory(): zipped {0}."
			Ganymede.debug(ts.l("cleanBackupDirectory.zipped", zipName));

			try
			  {
			    FileOps.deleteDirectory(dirName);
			  }
			catch (IOException ex)
			  {
			    // "GanymedeBuilderTask.cleanBackupDirectory(): could not remove {0}."
			    Ganymede.debug(ts.l("cleanBackupDirectory.bad_delete", dirName));
			  }
		      }
		    else
		      {
			File dirFile = new File(dirName);

			if (dirFile.canRead())
			  {
			    String[] list = dirFile.list();

			    if (list == null || list.length == 0)
			      {
				// "GanymedeBuilderTask.cleanBackupDirectory(): directory {0} is empty, deleting it."
				Ganymede.debug(ts.l("cleanBackupDirectory.skipping_empty", dirName));

				FileOps.deleteDirectory(dirName);
			      }
			  }
		      }
		  }
		else
		  {
		    // "GanymedeBuilderTask.cleanBackupDirectory(): {0} zip file already exists, not deleting."
		    Ganymede.debug(ts.l("cleanBackupDirectory.zip_already", dirName));
		  }
	      }
	    else
	      {
		// "GanymedeBuilderTask.cleanBackupDirectory(): don''t need to zip {0} yet."
		Ganymede.debug(ts.l("cleanBackupDirectory.no_zip_yet", dirName));
	      }
	  }
	catch (NumberFormatException ex)
	  {
	    continue;
	  }
      }
  }

  /**
   * This is public for GanymedeSession.openTransaction(), as a
   * hack to support proper updating of the client's status icon on
   * client connect.
   */

  public static int getPhase1Count()
  {
    return phase1Count;
  }

  /**
   * This is public for GanymedeSession.openTransaction(), as a
   * hack to support proper updating of the client's status icon on
   * client connect.
   */

  public static int getPhase2Count()
  {
    return phase2Count;
  }

  static synchronized void incPhase1(boolean update)
  {
    phase1Count++;

    if (update)
      {
	updateBuildStatus();
      }
  }

  static synchronized void decPhase1(boolean update)
  {
    phase1Count--;

    if (update)
      {
	updateBuildStatus();
      }
  }

  static synchronized void incPhase2(boolean update)
  {
    phase2Count++;

    if (update)
      {
	updateBuildStatus();
      }
  }

  static synchronized void decPhase2(boolean update)
  {
    phase2Count--;

    if (update)
      {
	updateBuildStatus();
      }
  }

  /**
   * This method is called by the GanymedeBuilderTask base class to
   * record that the server is processing a build.
   */

  static synchronized void updateBuildStatus()
  {
    // phase 1 can have the database locked, so show that
    // for preference

    if (phase1Count > 0)
      {
	GanymedeServer.sendMessageToRemoteSessions(ClientMessage.BUILDSTATUS, "building");
      }
    else if (phase2Count > 0)
      {
	GanymedeServer.sendMessageToRemoteSessions(ClientMessage.BUILDSTATUS, "building2");
      }
    else
      {
	GanymedeServer.sendMessageToRemoteSessions(ClientMessage.BUILDSTATUS, "idle");
      }
  }
}
