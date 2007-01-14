/*

   scheduleHandle.java

   This class is used to keep track of background tasks running on the
   Ganymede Server.  It is also used to pass data to the admin console.
   
   Created: 3 February 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2007
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.common;

import java.util.Date;

import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeBuilderTask;
import arlut.csd.ganymede.server.GanymedeScheduler;
import arlut.csd.ganymede.server.taskMonitor;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  scheduleHandle

------------------------------------------------------------------------------*/

/**
 * <P>Handle object used to help manage background tasks registered in the 
 * Ganymede Server's
 * {@link arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler}.  In addition
 * to being used by the server's task scheduler to organize and track
 * registered tasks, vectors of serialized scheduleHandle objects are passed to the
 * Ganymede admin console's 
 * {@link arlut.csd.ganymede.admin#changeTasks(java.util.Vector) changeTasks} 
 * method.</P>
 * 
 * <P>Within the Ganymede server, scheduleHandle objects are held within the 
 * GanymedeScheduler to track the status of each registered task.  When the 
 * GanymedeScheduler needs to run a background task, the scheduleHandle's
 * {@link arlut.csd.ganymede.common.scheduleHandle#runTask() runTask()} method
 * is called.  runTask() creates a pair of threads, one to run the task
 * and another {@link arlut.csd.ganymede.server.taskMonitor taskMonitor} thread
 * to wait for the task to be completed.  When the thread running
 * the task completes, the task's taskMonitor calls the scheduleHandle's 
 * {@link arlut.csd.ganymede.common.scheduleHandle#notifyCompletion notifyCompletion()}
 * method, which in turn notifies the GanymedeScheduler that the task
 * has completed its execution.</P>
 *
 * <P>The various scheduling methods in scheduleHandle will throw an
 * IllegalArgumentException if called post-serialization on the
 * Ganymede client.</P>
 *
 * <P>In order to avoid nested monitor deadlock, all calls from
 * scheduleHandle to synchronized methods on the GanymedeScheduler
 * must be made from locally unsynchronized code blocks.</P>
 */

public class scheduleHandle implements java.io.Serializable {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.common.scheduleHandle");

  // ---

  // we pass these attributes along to the admin console for it to display

  /**
   * This booleanSemaphore will be set to true if the task associated
   * with this scheduleHandle is currently running.
   *
   * It is a booleanSemaphore so that we can have an appropriate
   * memory barrier for multiprocessor access without having to
   * synchronize on the scheduleHandle upon calls to isRunning().
   */

  private booleanSemaphore isRunning = new booleanSemaphore(false);

  /**
   * This booleanSemaphore will be set to true if the task associated
   * with this scheduleHandle is currently suspended.
   *
   * It is a booleanSemaphore so that we can have an appropriate
   * memory barrier for multiprocessor access without having to
   * synchronize on the scheduleHandle upon calls to isSuspended().
   */

  private booleanSemaphore suspend = new booleanSemaphore(false);

  /**
   * isSyncTask will be true if the task in question is instanceof
   * either arlut.csd.ganymede.server.GanymedeBuilderTask or
   * arlut.csd.ganymede.server.SyncRunner.
   */

  public boolean isSyncTask;

  /**
   * This booleanSemaphore will be set to true if we are doing a
   * on-demand and we get a request while running it, which signifies
   * that we'll want to immediately re-run it on completion.
   *
   * It is a booleanSemaphore so that we can have an appropriate
   * memory barrier for multiprocessor access without having to
   * synchronize on the scheduleHandle upon calls to
   * runAgain().
   */

  private booleanSemaphore rerun = new booleanSemaphore(false);

  /**
   * When was this task last issued?
   */

  public Date lastTime;

  /**
   * When will this task next be issued?
   */

  public Date startTime;

  /**
   * when was this task first registered?  used to present a
   * consistently sorted list on the client
   */

  public Date incepDate;

  /**
   * For reporting our interval status to the admin console
   */

  public String intervalString;

  /**
   * For reporting our name to the admin console
   */

  public String name;

  //
  // non-serializable, for use on the server only
  //

  /**
   * Any options that we need to pass to the task?
   */

  transient public Object options[];

  /**
   * 0 if this is a one-shot, otherwise, the count in minutes
   */

  transient public int interval;

  /**
   * The task to run
   */

  transient public Runnable task;

  /**
   * If this task is currently running, this field will
   * point to the running thread, otherwise it will be null.
   */

  transient public Thread thread; 

  /**
   * If this task is currently running, this field will
   * point to a monitor thread that is waiting for the task's
   * thread to complete before reporting completion.  The
   * monitor thread is effectively a wrapper thread that
   * lets any arbitrary Runnable be scheduled and managed
   * by GanymedeScheduler.
   */

  transient public Thread monitor;

  /**
   * The GanymedeScheduler that this task is registered in.
   */

  transient public GanymedeScheduler scheduler = null;

  /* -- */

  /**
   * @param scheduler Reference to the server's GanymedeScheduler
   * @param time Anchor point for interval calculations
   * @param interval Number of seconds between runs of this task
   * @param task Java Runnable object to be run in a thread
   * @param name Name to report for this task
   */

  public scheduleHandle(GanymedeScheduler scheduler,
			Date time, int interval, 
			Runnable task, String name)
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't create schedule handle without scheduler reference");
      }

    this.scheduler = scheduler;
    this.lastTime = null;
    this.startTime = time;
    this.task = task;
    this.name = name;

    if (task instanceof arlut.csd.ganymede.server.GanymedeBuilderTask ||
	task instanceof arlut.csd.ganymede.server.SyncRunner)
      {
	this.isSyncTask = true;
      }

    setInterval(interval);

    // remember when we were created

    incepDate = new Date();
  }

  /**
   * <p>This method is used to set an options array for the next run
   * of the task associated with this handle, if that task is a GanymedeBuilderTask.</p>
   *
   * <p>If the task associated with this handle is not a GanymedeBuilderTask,
   * the options will be ignored.  Since setOptions() is synchronized, options
   * may only be set at a time when runTask() is not busy issuing the
   * task in the background.  runTask() clears the options set, so setOptions()
   * only affects the next launching of the task.</p>
   */

  public void setOptions(Object _options[])
  {
    this.options = _options;
  }

  /**
   * <P>Runs this task in a background thread.  A second background thread
   * is created to handle a {@link arlut.csd.ganymede.server.taskMonitor taskMonitor}
   * to wait and report when the task completes.</P>
   *
   * <P>This method is invalid on the client.</P>
   */

  public void runTask()
  {
    // start our task

    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    if (debug)
      {
	System.err.println("Ganymede Scheduler: Starting task " + name + " at " + new Date());
      }

    if (suspend.isSet())
      {
	Ganymede.debug("Ganymede Scheduler: Task " + name + " skipped at " + new Date());

	// XXX must not be synchronized on this scheduleHandle here,
	// else possible nested monitor deadlock

	scheduler.notifyCompletion(this); 

	return;
      }

    synchronized (this)
      {
	rerun.set(false);

	// grab options for this run

	if (options == null || (!(task instanceof GanymedeBuilderTask)))
	  {
	    thread = new Thread(task, name);
	    thread.start();
	  }
	else
	  {
	    // we're running a GanymedeBuilderTask with options set

	    final Object _options[] = options;
	    final GanymedeBuilderTask _task = (GanymedeBuilderTask) task;

	    thread = new Thread(new Runnable() {
		public void run() {
		  _task.run(_options);
		}
	      }, name);

	    thread.start();
	
	    // clear options
	
	    this.options = null;
	  }

	isRunning.set(true);

	// and have our monitor watch for it
    
	monitor = new Thread(new taskMonitor(thread, this), name);
	monitor.start();
      }
  }

  /** 
   * <P>This method is called by our {@link
   * arlut.csd.ganymede.server.taskMonitor taskMonitor} when our task
   * completes.  This method has no meaning outside of the context of
   * the taskMonitor spawned by this handle, and should not be called
   * from any other code.</P> 
   */

  public void notifyCompletion()
  {
    synchronized (this)
      {
	if (scheduler == null)
	  {
	    throw new IllegalArgumentException("can't run this method on the client");
	  }

	monitor = null;
	thread = null;

	isRunning.set(false);
	lastTime = new Date();
      }

    // XXX must not be synchronized on this scheduleHandle here,
    // else possible nested monitor deadlock

    scheduler.notifyCompletion(this);
  }

  /**
   * <P>Server-side method to determine whether this task should be rescheduled.
   * Increments the startTime and returns true if this is a periodically
   * executed task.</P>
   */

  public synchronized boolean reschedule()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    if (startTime == null || interval == 0)
      {
	return false;
      }
    else
      {
	startTime.setTime(startTime.getTime() + (long) (60000 * interval));
	return true;
      }
  }

  /**
   * <P>This method lets the GanymedeScheduler check to see whether this task
   * should be re-run when it terminates</P>
   */

  public boolean runAgain()
  {
    return rerun.isSet();
  }

  /**
   * <P>Returns true if this task is not scheduled for periodic execution</P>
   */

  public boolean isOnDemand()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    return interval == 0;
  }

  /**
   * Returns true if the task is currently running.
   */

  public boolean isRunning()
  {
    return isRunning.isSet();
  }

  /**
   * Returns true if the task is suspended.
   */

  public boolean isSuspended()
  {
    return suspend.isSet();
  }

  /**
   * <P>Server-side method to request that this task be re-run after
   * its current completion.  Intended for on-demand tasks that are
   * requested by the {@link arlut.csd.ganymede.server.GanymedeScheduler
   * GanymedeScheduler} while they are already running.</P>
   */

  public void runOnCompletion()
  {
    this.runOnCompletion(null);
  }

  /**
   * <P>Server-side method to request that this task be re-run after
   * its current completion.  Intended for on-demand tasks that are
   * requested by the {@link arlut.csd.ganymede.server.GanymedeScheduler
   * GanymedeScheduler} while they are already running.</P>
   */

  public synchronized void runOnCompletion(Object _options[])
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    this.options = _options;

    rerun.set(true);
  }

  /**
   * <P>Server-side method to request that this task not be kept after its
   * current completion.  Used to remove a task from the Ganymede scheduler.</P>
   */

  public synchronized void unregister()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    rerun.set(false);
  }

  /**
   * <P>Server-side method to interrupt this task.  Tasks must be specifically
   * written to properly respond to an interruption.</P>
   */

  public synchronized void stop()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    if (thread != null)
      {
	thread.interrupt();	// this used to be a stop, but stop is deprecated
				// as unsafe in 1.2, so we do the next best thing
      }
  }

  /**
   * <P>Server-side method to disable future invocations of this task</P>
   */

  public synchronized void disable()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    suspend.set(true);
  }

  /**
   * <P>Server-side method to enable future invocations of this task</P>
   */

  public synchronized void enable()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    suspend.set(false);
  }

  /**
   * <P>Server-side method to change the interval for this task</P>
   *
   * @param interval Number of seconds between runs of this task
   */

  public synchronized void setInterval(int interval)
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    // set the interval time

    this.interval = interval;

    if (interval == 0)
      {
	intervalString = "";
	return;
      }

    // ok, we need to calculate a description for how long
    // between invocations of this task

    StringBuffer buff = new StringBuffer();

    int minutes = 0;
    int hours = 0;
    int days = 0;
    int weeks = 0;

    int temp = interval;

    weeks = temp / 10080;    temp %= 10080;
    days = temp / 1440;    temp %= 1440;
    hours = temp / 60;    temp %= 60;
    minutes = temp;

    if (weeks != 0)
      {
	if (weeks > 1)
	  {
	    // "{0,num,#} weeks"
	    buff.append(ts.l("setInterval.weeks_pattern", new Integer(weeks)));
	  }
	else
	  {
	    // "1 week"
	    buff.append(ts.l("setInterval.week_pattern"));
	  }
      }

    if (days != 0)
      {
	if (buff.length() != 0)
	  {
	    buff.append(", ");
	  }

	if (days > 1)
	  {
	    // "{0,num,#} days"
	    buff.append(ts.l("setInterval.days_pattern", new Integer(days)));
	  }
	else
	  {
	    // "1 day"
	    buff.append(ts.l("setInterval.day_pattern"));
	  }
      }

    if (hours != 0)
      {
	if (buff.length() != 0)
	  {
	    buff.append(", ");
	  }

	if (hours > 1)
	  {
	    // "{0,num,#} hours"
	    buff.append(ts.l("setInterval.hours_pattern", new Integer(hours)));
	  }
	else
	  {
	    // "1 hour"
	    buff.append(ts.l("setInterval.hour_pattern"));
	  }
      }

    if (minutes != 0)
      {
	if (buff.length() != 0)
	  {
	    buff.append(", ");
	  }

	if (minutes > 1)
	  {
	    // "{0,num,#} minutes"
	    buff.append(ts.l("setInterval.minutes_pattern", new Integer(minutes)));
	  }
	else
	  {
	    // "1 minute"
	    buff.append(ts.l("setInterval.minute_pattern"));
	  }
      }

    // and set the string

    intervalString = buff.toString();
  }
}

