/*

   scheduleHandle.java

   This class is used to keep track of background tasks running on the
   Ganymede Server.  It is also used to pass data to the admin console.
   
   Created: 3 February 1998
   Version: $Revision: 1.5 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  scheduleHandle

------------------------------------------------------------------------------*/

/**
 *
 * This class is used to keep track of background tasks running on the
 * Ganymede Server.  It is also used to pass data to the admin console.
 *
 * This class works hand in glove with the GanymedeScheduler class.
 *
 * @see arlut.csd.ganymede.GanymedeScheduler
 *
 */

public class scheduleHandle implements java.io.Serializable {

  static final boolean debug = false;

  // we pass these attributes along to the admin console for it to display

  boolean isRunning;
  boolean suspend;
  boolean rerun;		// if we are doing a on-demand and we get a request while running it,
				// we'll want to immediately re-run it
  Date startTime;
  Date incepDate;		// when was this task first registered?  used to present a
				// consistent list on the client

  String intervalString;
  String name;

  // non-serializable, for use on the server only

  transient int interval;			// 0 if this is a one-shot, otherwise, the count in minutes
  transient Runnable task;
  transient Thread thread, monitor;
  transient GanymedeScheduler scheduler = null;
  

  /* -- */

  public scheduleHandle(GanymedeScheduler scheduler,
			Date time, int interval, 
			Runnable task, String name)
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't create schedule handle without scheduler reference");
      }

    this.scheduler = scheduler;
    this.startTime = time;
    this.task = task;
    this.name = name;
    this.rerun = false;

    setInterval(interval);

    incepDate = new Date();
  }

  /**
   *
   * This server-side method causes the task represented by this scheduleHandle to
   * be spawned into a new thread.
   *
   * This method is invalid on the server.
   *
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

    thread = new Thread(task, name);
    thread.start();

    isRunning = true;

    // and have our monitor watch for it
    
    monitor = new Thread(new taskMonitor(thread, this), name);
    monitor.start();
  }

  /**
   *
   * This method is called by our task monitor when our task
   * completes.  This method has no meaning outside of the context of
   * the taskMonitor spawned by this handle, and should not be called
   * from any other code.
   * 
   */

  synchronized void notifyCompletion()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    monitor = null;
    isRunning = false;
    scheduler.notifyCompletion(this);
  }

  /**
   *
   * Server-side method to determine whether this task should be rescheduled
   *
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
   *
   * This method lets the GanymedeScheduler check to see whether this task
   * should be re-run when it terminates
   *
   */

  synchronized boolean runAgain()
  {
    return rerun;
  }

  /**
   *
   * Server-side method to request that this task be re-run after
   * its current completion.  Intended for on-demand tasks.
   *
   */

  synchronized void runOnCompletion()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    rerun = true;
  }

  /**
   *
   * Server-side method to bring this task to an abrupt halt.
   *
   */

  synchronized void stop()
  {
    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't run this method on the client");
      }

    monitor.stop();
    thread.stop();

    isRunning = false;
  }

  /**
   *
   * Server-side method to disable future invocations of this task
   *
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
   *
   * Server-side method to enable future invocations of this task
   *
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
   *
   * Server-side method to change the interval for this task
   *
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
	return;			// if interrupted, assume the scheduler is going down
      }

    handle.notifyCompletion(); // tell the scheduler it has completed
  }
}
