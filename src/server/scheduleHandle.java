/*

   scheduleHandle.java

   This class is used to keep track of background tasks running on the
   Ganymede Server.  It is also used to pass data to the admin console.
   
   Created: 3 February 1998
   Version: $Revision: 1.2 $ %D%
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

  boolean isRunning = false;
  boolean suspend = false;
  Date startTime;
  int interval;			// 0 if this is a one-shot, otherwise, the count in minutes
  String name;

  // non-serializable, for use on the server only

  transient Runnable task;
  transient Thread thread, monitor;
  transient GanymedeScheduler scheduler = null;

  /* -- */

  public scheduleHandle(GanymedeScheduler scheduler,
			Date time, int interval, 
			Runnable task, String name)
  {
    if (time == null)
      {
	throw new IllegalArgumentException("can't schedule a task without a start time");
      }

    if (scheduler == null)
      {
	throw new IllegalArgumentException("can't create schedule handle without scheduler reference");
      }

    this.scheduler = scheduler;
    this.startTime = time;
    this.interval = interval;
    this.task = task;
    this.name = name;
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
	return;
      }

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

    if (interval == 0)
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
