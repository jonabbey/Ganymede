/*

   scheduleHandle.java

   This class is used to keep track of background tasks running on the
   Ganymede Server.  It is also used to pass data to the admin console.
   
   Created: 3 February 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  scheduleHandle

------------------------------------------------------------------------------*/

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
  transient GanymedeScheduler scheduler;

  /* -- */

  public scheduleHandle(GanymedeScheduler scheduler,
			Date time, int interval, 
			Runnable task, String name)
  {
    if (time == null)
      {
	throw new IllegalArgumentException("can't schedule a task without a start time");
      }

    this.scheduler = scheduler;
    this.startTime = time;
    this.interval = interval;
    this.task = task;
    this.name = name;
  }

  synchronized void runTask()
  {
    // start our task

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
   * completes
   *
   */

  synchronized void notifyCompletion()
  {
    monitor = null;
    isRunning = false;
    scheduler.notifyCompletion(this);
  }

  synchronized boolean reschedule()
  {
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

  synchronized void stop()
  {
    monitor.stop();
    thread.stop();
  }

  synchronized void disable()
  {
    suspend = true;
  }

  synchronized void enable()
  {
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
