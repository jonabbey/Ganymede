/*

   scheduleHandle.java

   This class is used to keep track of background tasks running on the
   Ganymede Server.  It is also used to pass data to the admin console.
   
   Created: 3 February 1998
   Release: $Name:  $
   Version: $Revision: 1.14 $
   Last Mod Date: $Date: 2001/03/27 07:30:32 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  scheduleHandle

------------------------------------------------------------------------------*/

/**
 * <P>Handle object used to help manage background tasks registered in the 
 * Ganymede Server's
 * {@link arlut.csd.ganymede.GanymedeScheduler GanymedeScheduler}.  In addition
 * to being used by the server's task scheduler to organize and track
 * registered tasks, vectors of serialized scheduleHandle objects are passed to the
 * Ganymede admin console's 
 * {@link arlut.csd.ganymede.Admin#changeTasks(java.util.Vector) changeTasks} 
 * method.</P>
 * 
 * <P>Within the Ganymede server, scheduleHandle objects are held within the 
 * GanymedeScheduler to track the status of each registered task.  When the 
 * GanymedeScheduler needs to run a background task, the scheduleHandle's
 * {@link arlut.csd.ganymede.scheduleHandle#runTask() runTask()} method
 * is called.  runTask() creates a pair of threads, one to run the task
 * and another {@link arlut.csd.ganymede.taskMonitor taskMonitor} thread
 * to wait for the task to be completed.  When the thread running
 * the task completes, the task's taskMonitor calls the scheduleHandle's 
 * {@link arlut.csd.ganymede.scheduleHandle#notifyCompletion notifyCompletion()}
 * method, which in turn notifies the GanymedeScheduler that the task
 * has completed its execution.</P>
 *
 * <P>The various scheduling methods in scheduleHandle will throw an
 * IllegalArgumentException if called post-serialization on the Ganymede client.</P>
 */

public class scheduleHandle implements java.io.Serializable {

  static final boolean debug = false;

  // ---

  // we pass these attributes along to the admin console for it to display

  boolean isRunning;
  boolean suspend;

  /**
   * if we are doing a on-demand and we get a request while running it,
   * we'll want to immediately re-run it on completion
   */

  boolean rerun;

  /**
   * When was this task last issued?
   */

  Date lastTime;

  /**
   * When will this task next be issued?
   */

  Date startTime;

  /**
   * when was this task first registered?  used to present a
   * consistently sorted list on the client
   */

  Date incepDate;

  /**
   * For reporting our interval status to the admin console
   */

  String intervalString;

  /**
   * For reporting our name to the admin console
   */

  String name;

  //
  // non-serializable, for use on the server only
  //

  /**
   * Any options that we need to pass to the task?
   */

  transient Object options[];

  /**
   * 0 if this is a one-shot, otherwise, the count in minutes
   */

  transient int interval;

  /**
   * if this field is set to false, the Ganymede Scheduler will
   * not register this task for subsequent execution on completion
   * of execution.
   */

  transient boolean reregister = true;

  /**
   * The task to run
   */

  transient Runnable task;

  /**
   * If this task is currently running, this field will
   * point to the running thread, otherwise it will be null.
   */

  transient Thread thread; 

  /**
   * If this task is currently running, this field will
   * point to a monitor thread that is waiting for the task's
   * thread to complete before reporting completion.  The
   * monitor thread is effectively a wrapper thread that
   * lets any arbitrary Runnable be scheduled and managed
   * by GanymedeScheduler.
   */

  transient Thread monitor;

  /**
   * The GanymedeScheduler that this task is registered in.
   */

  transient GanymedeScheduler scheduler = null;

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
    this.rerun = false;

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

  synchronized void setOptions(Object _options[])
  {
    this.options = _options;
  }

  /**
   * <P>Runs this task in a background thread.  A second background thread
   * is created to handle a {@link arlut.csd.ganymede.taskMonitor taskMonitor}
   * to wait and report when the task completes.</P>
   *
   * <P>This method is invalid on the client.</P>
   */

  synchronized void runTask()
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

    if (suspend)
      {
	System.err.println("Ganymede Scheduler: Task " + name + " skipped at " + new Date());
	scheduler.notifyCompletion(this);
	return;
      }

    rerun = false;

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

    isRunning = true;

    // and have our monitor watch for it
    
    monitor = new Thread(new taskMonitor(thread, this), name);
    monitor.start();
  }

  /** 
   * <P>This method is called by our {@link
   * arlut.csd.ganymede.taskMonitor taskMonitor} when our task
   * completes.  This method has no meaning outside of the context of
   * the taskMonitor spawned by this handle, and should not be called
   * from any other code.</P> 
   */

  synchronized void notifyCompletion()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    monitor = null;
    thread = null;

    isRunning = false;
    lastTime = new Date();
    scheduler.notifyCompletion(this);
  }

  /**
   * <P>Server-side method to determine whether this task should be rescheduled.
   * Increments the startTime and returns true if this is a periodically
   * executed task.</P>
   */

  synchronized boolean reschedule()
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

  synchronized boolean runAgain()
  {
    return rerun;
  }

  /**
   * <P>Returns true if this task is not scheduled for periodic execution</P>
   */

  synchronized boolean isOnDemand()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    return interval == 0;
  }

  /**
   * <P>Server-side method to request that this task be re-run after
   * its current completion.  Intended for on-demand tasks that are
   * requested by the {@link arlut.csd.ganymede.GanymedeScheduler
   * GanymedeScheduler} while they are already running.</P>
   */

  void runOnCompletion()
  {
    this.runOnCompletion(null);
  }

  /**
   * <P>Server-side method to request that this task be re-run after
   * its current completion.  Intended for on-demand tasks that are
   * requested by the {@link arlut.csd.ganymede.GanymedeScheduler
   * GanymedeScheduler} while they are already running.</P>
   */

  synchronized void runOnCompletion(Object _options[])
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    this.options = _options;

    rerun = true;
  }

  /**
   * <P>Server-side method to request that this task not be kept after its
   * current completion.  Used to remove a task from the Ganymede scheduler.</P>
   */

  synchronized void unregister()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    reregister = false;
    rerun = false;
  }

  /**
   * <P>Server-side method to bring this task to an abrupt halt.</P>
   */

  synchronized void stop()
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

  synchronized void disable()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    suspend = true;
  }

  /**
   * <P>Server-side method to enable future invocations of this task</P>
   */

  synchronized void enable()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    suspend = false;
  }

  /**
   * <P>Server-side method to change the interval for this task</P>
   *
   * @param interval Number of seconds between runs of this task
   */

  synchronized void setInterval(int interval)
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    // set the interval time

    this.interval = interval;

    if (interval == 0)
      {
	intervalString = "n/a";
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
	buff.append(String.valueOf(weeks));

	if (weeks > 1)
	  {
	    buff.append(" weeks");
	  }
	else
	  {
	    buff.append(" week");
	  }
      }

    if (days != 0)
      {
	if (buff.length() != 0)
	  {
	    buff.append(", ");
	  }

	buff.append(String.valueOf(days));

	if (days > 1)
	  {
	    buff.append(" days");
	  }
	else
	  {
	    buff.append(" day");
	  }
      }

    if (hours != 0)
      {
	if (buff.length() != 0)
	  {
	    buff.append(", ");
	  }

	buff.append(String.valueOf(hours));

	if (hours > 1)
	  {
	    buff.append(" hours");
	  }
	else
	  {
	    buff.append(" hour");
	  }
      }

    if (minutes != 0)
      {
	if (buff.length() != 0)
	  {
	    buff.append(", ");
	  }

	buff.append(String.valueOf(minutes));

	if (minutes > 1)
	  {
	    buff.append(" minutes");
	  }
	else
	  {
	    buff.append(" minute");
	  }
      }

    // and set the string

    intervalString = buff.toString();
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                     taskMonitor

------------------------------------------------------------------------------*/

/** 
 * <P>{@link arlut.csd.ganymede.GanymedeScheduler GanymedeScheduler}
 * helper class used to notify the scheduler when a background task
 * has completed.</P>
 *
 * @see arlut.csd.ganymede.scheduleHandle
 */

class taskMonitor implements Runnable {

  Thread task;
  scheduleHandle handle;

  /* -- */

  public taskMonitor(Thread task, scheduleHandle handle)
  {
    this.task = task;
    this.handle = handle;
  }

  public void run()
  {
    try
      {
	task.join();		// wait for our task to finish
      }
    catch (InterruptedException ex)
      {
      }
    finally
      {
	handle.notifyCompletion(); // tell the scheduler it has completed
      }

    // we're a one-shot monitor, forget everything for GC

    this.task = null;
    this.handle = null;
  }
}
