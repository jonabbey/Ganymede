/*

   GanymedeScheduler.java

   This class is designed to be run in a thread.. it allows the main server
   to register tasks to be run on a periodic basis.
   
   Created: 26 January 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               GanymedeScheduler

------------------------------------------------------------------------------*/

/**
 * This class is intended to serve as a process running concurrently
 * with the RMI-dispatched GanymedeServer object.  Its primary duty is
 * to take care of any objects that have passed their inactivation or
 * expiration times.  Periodically (once per day?  per half-day?),
 * this thread will wake up, obtain a lock on all bases in the database,
 * sweep through them looking for objects that need to be inactivated or
 * removed, and perform the appropriate actions.
 *
 * This class is in the very earliest design stages, and it is unclear
 * whether the proper design course at this point is to have a single,
 * continously running thread to wake up at the appropriate times, or
 * whether the server should have a method defined that could be
 * synchronously executed whenever there are no users logged in to the
 * server and the time for the next cleaning has passed.
 *
 * Most generically, we could have a simple timer thread that kept track
 * of a number of possible runnable actions that could be woken up and run
 * by an asynchronous thread that could sleep until the next action time 
 * has been met.
 *
 */

public class GanymedeScheduler extends Thread {

  static final int minsPerDay = 1440;
  
  Date nextAction = null;
  private Vector currentlyScheduled = new Vector();
  private Vector currentlyRunning = new Vector();

  /* -- */

  /**
   *
   *
   *
   */

  public GanymedeScheduler()
  {
  }

  public synchronized void addAction(Date time, 
				     Runnable task,
				     String name)
  {
    scheduleHandle handle;

    /* -- */

    if (time == null || task == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    handle = new scheduleHandle(this, time, 0, task, name == null ? "" : name);
    scheduleTask(handle);
  }

  public synchronized void addDailyAction(int hour, int minute, 
					  Runnable task,
					  String name)
  {
    scheduleHandle handle;
    Date time, currentTime;
    Calendar cal = Calendar.getInstance();

    /* -- */

    if (task == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    currentTime = new Date();

    cal.setTime(currentTime);

    cal.set(Calendar.HOUR, hour);
    cal.set(Calendar.MINUTE, minute);

    time = cal.getTime();

    if (time.before(currentTime))
      {
	cal.add(Calendar.DATE, 1); // advance to this time tomorrow
      }

    time = cal.getTime();

    handle = new scheduleHandle(this, time, minsPerDay, task, name == null ? "" : name);
    scheduleTask(handle);
  }

  public synchronized void addPeriodicAction(int hour, int minute, 
					     int intervalMinutes, 
					     Runnable task,
					     String name)
  {
    scheduleHandle handle;
    Date time, currentTime;
    Calendar cal = Calendar.getInstance();

    /* -- */

    if (task == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    currentTime = new Date();

    cal.setTime(currentTime);

    cal.set(Calendar.HOUR, hour);
    cal.set(Calendar.MINUTE, minute);

    time = cal.getTime();

    if (time.before(currentTime))
      {
	cal.roll(Calendar.DATE, true); // advance to this time tomorrow
      }

    time = cal.getTime();

    handle = new scheduleHandle(this, time, intervalMinutes, task, name == null ? "" : name);

    scheduleTask(handle);
  }
  
  /**
   *
   * This method is responsible for carrying out the scheduling
   * work of this class.
   *
   * The basic logic is to wait until the next action is due to run,
   * move the task from our scheduled list to our running list, and
   * run it.  When the task completes, it will call our reschedule()
   * method if necessary
   *
   */

  public synchronized void run()
  {
    long currentTime, sleepTime;
    scheduleHandle handle;

    /* -- */

    while (true)
      {
	if (nextAction == null)
	  {
	    try
	      {
		wait();
	      }
	    catch (InterruptedException ex)
	      {
		System.err.println("Scheduler caught interruption.. exiting");
		return;
	      }
	  }
	else
	  {
	    currentTime = System.currentTimeMillis();

	    if (currentTime > nextAction.getTime())
	      {
		sleepTime = nextAction.getTime() - currentTime;

		if (sleepTime > 0)
		  {
		    try
		      {
			wait(sleepTime);
		      }
		    catch (InterruptedException ex)
		      {
			System.err.println("Scheduler caught interruption.. exiting");
			return;
		      }
		  }

		currentTime = System.currentTimeMillis();

		if (currentTime >= nextAction.getTime())
		  {
		    for (int i = 0; i < currentlyScheduled.size(); i++)
		      {
			handle = (scheduleHandle) currentlyScheduled.elementAt(i);
			
			if (handle.startTime.getTime() <= currentTime)
			  {
			    runTask(handle);
			  }
		      }
		  }
	      }
	  }
      }
  }

  synchronized void runTask(scheduleHandle handle)
  {
    if (currentlyScheduled.removeElement(handle))
      {
	currentlyRunning.addElement(handle);
	handle.runTask();
      }
  }

  synchronized void notifyCompletion(scheduleHandle handle)
  {
    if (currentlyRunning.removeElement(handle))
      {
	if (handle.reschedule())
	  {
	    scheduleTask(handle);
	  }
      }
  }

  /**
   *
   * This method takes a task that needs to be scheduled and
   * adds it to the scheduler.
   *
   */

  private synchronized void scheduleTask(scheduleHandle handle)
  {
    currentlyScheduled.addElement(handle);

    if (handle.startTime.getTime() < nextAction.getTime())
      {
	nextAction.setTime(handle.startTime.getTime());
	notify();	// let the scheduler know about our newly scheduled event
      }
  }

  private synchronized void cleanUp()
  {
    scheduleHandle handle;

    /* -- */

    for (int i = 0; i < currentlyRunning.size(); i++)
      {
	handle = (scheduleHandle) currentlyRunning.elementAt(i);

	handle.stop();
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                  scheduleHandle

------------------------------------------------------------------------------*/

class scheduleHandle {

  Date startTime;
  int interval;			// 0 if this is a one-shot, otherwise, the count in minutes
  Runnable task;
  Thread thread, monitor;
  String name;
  GanymedeScheduler scheduler;

  /* -- */

  public scheduleHandle(GanymedeScheduler scheduler,
			Date time, int interval, Runnable task, String name)
  {
    this.scheduler = scheduler;
    this.startTime = time;
    this.interval = interval;
    this.task = task;
    this.name = name;
  }

  synchronized void runTask()
  {
    // start our task

    thread = new Thread(task, name);
    thread.start();

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
	startTime = new Date(startTime.getTime() + (long) (1000 * interval));
	return true;
      }
  }

  synchronized void stop()
  {
    monitor.stop();
    thread.stop();
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
