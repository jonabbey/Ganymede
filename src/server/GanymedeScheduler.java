/*

   GanymedeScheduler.java

   This class is designed to be run in a thread.. it allows the main server
   to register tasks to be run on a periodic basis.
   
   Created: 26 January 1998
   Version: $Revision: 1.4 $ %D%
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
  static final boolean debug = false;
  
  Date nextAction = null;
  private Vector currentlyScheduled = new Vector();
  private Vector currentlyRunning = new Vector();

  /* -- */

  /**
   *
   * Debug rig
   *
   */

  public static void main(String[] argv)
  {
    GanymedeScheduler scheduler = new GanymedeScheduler();
    new Thread(scheduler).start();

    Date time, currentTime;
    Calendar cal = Calendar.getInstance();

    currentTime = new Date();

    cal.setTime(currentTime);

    cal.add(Calendar.MINUTE, 1);

    scheduler.addAction(cal.getTime(), 
			new sampleTask("sample task 1"), 
			"sample task 1");

    scheduler.addPeriodicAction(cal.get(Calendar.HOUR), 
				cal.get(Calendar.MINUTE), 1,
				new sampleTask("sample task 2"), "sample task 2");

    cal.add(Calendar.MINUTE, 1);

    scheduler.addAction(cal.getTime(), 
			new sampleTask("sample task 3"),
			"sample task 3");

    cal.add(Calendar.MINUTE, 1);

    scheduler.addPeriodicAction(cal.get(Calendar.HOUR), 
				cal.get(Calendar.MINUTE), 1,
				new sampleTask("sample task 4"), "sample task 4");
  }

  public GanymedeScheduler()
  {
  }

  /**
   *
   * This method is used to add an action to be run once, at a specific time.
   *
   */

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
    
    System.err.println("Ganymede Scheduler: Scheduled task " + name + " for execution at " + time);
  }

  /**
   *
   * This method is used to add an action to be run every day at a specific time.
   *
   */

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

    System.err.println("Ganymede Scheduler: Scheduled task " + name + " for daily execution at " + time);
  }

  /**
   * This method is used to add an action to be run at a specific
   * initial time, and every <intervalMinutes> thereafter.  
   *
   * The scheduler will not reschedule a task until the last scheduled
   * instance of the task has completed.
   *
   */

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
	cal.add(Calendar.DATE, 1); // advance to this time tomorrow
      }

    time = cal.getTime();

    handle = new scheduleHandle(this, time, intervalMinutes, task, name == null ? "" : name);

    scheduleTask(handle);

    System.err.println("Ganymede Scheduler: Scheduled task " + name + " for periodic execution at " + time);
    System.err.println("                    Task will repeat every " + intervalMinutes + " minutes");
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

    try
      {
	System.err.println("Ganymede Scheduler: scheduling task started");

	while (true)
	  {
	    if (debug)
	      {
		System.err.println("loop");
	      }

	    if (nextAction == null)
	      {
		try
		  {
		    if (debug)
		      {
			System.err.println("*** snooze");
		      }

		    wait();
		  }
		catch (InterruptedException ex)
		  {
		    System.err.println("Scheduler interrupted");
		    return;	// jump to finally, then return
		  }

		if (debug)
		  {
		    System.err.println("*** snort?");
		  }
	      }
	    else
	      {
		currentTime = System.currentTimeMillis();

		if (currentTime < nextAction.getTime())
		  {
		    sleepTime = nextAction.getTime() - currentTime;

		    if (sleepTime > 0)
		      {
			try
			  {
			    if (debug)
			      {
				System.err.println("*** snooze");
			      }

			    wait(sleepTime);
			  }
			catch (InterruptedException ex)
			  {
			    System.err.println("Scheduler interrupted");
			    return; // jump to finally, then return
			  }

			if (debug)
			  {
			    System.err.println("*** snort?");
			  }
		      }
		  }
		else
		  {
		    if (debug)
		      {
			System.err.println("XX: Next action was scheduled at " + nextAction);
			System.err.println("XX: Processing current actions");
		      }
		  }

		currentTime = System.currentTimeMillis();
		
		if (currentTime >= nextAction.getTime())
		  {
		    Vector toRun = new Vector();
		    Date nextRun = null;

		    for (int i = 0; i < currentlyScheduled.size(); i++)
		      {
			handle = (scheduleHandle) currentlyScheduled.elementAt(i);
			
			if (handle.startTime.getTime() <= currentTime)
			  {
			    toRun.addElement(handle);
			  }
			else
			  {
			    if (nextRun == null)
			      {
				nextRun = new Date(handle.startTime.getTime());
			      }
			    else if (handle.startTime.before(nextRun))
			      {
				nextRun.setTime(handle.startTime.getTime());
			      }
			  }
		      }

		    nextAction = nextRun;

		    for (int i = 0; i < toRun.size(); i++)
		      {
			handle = (scheduleHandle) toRun.elementAt(i);

			runTask(handle);
		      }
		  }
	      }
	  }
      }
    finally 
      {
	System.err.println("Ganymede Scheduler going down");
	cleanUp();
	System.err.println("Ganymede Scheduler exited");
      }
  }

  synchronized void runTask(scheduleHandle handle)
  {
    if (currentlyScheduled.removeElement(handle))
      {
	System.err.println("Ganymede Scheduler: running " + handle.name);

	currentlyRunning.addElement(handle);
	handle.runTask();
      }
  }

  synchronized void notifyCompletion(scheduleHandle handle)
  {
    if (currentlyRunning.removeElement(handle))
      {
	System.err.println("Ganymede Scheduler: " + handle.name + " completed");

	if (handle.reschedule())
	  {
	    System.err.println("Ganymede Scheduler: rescheduling task " + handle.name + " for " + handle.startTime);

	    scheduleTask(handle);
	  }
      }
    else
      {
	System.err.println("Ganymede Scheduler: confusion! Couldn't find task " + handle.name + " on the runnng list");
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

    if (debug)
      {
	System.err.println("Ganymede Scheduler: scheduled task " + handle.name + 
			   " for initial execution at " + handle.startTime);
      }

    if (nextAction == null)
      {
	nextAction = new Date(handle.startTime.getTime());
	notify();
	return;
      }

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

  static final boolean debug = false;

  Date startTime;
  int interval;			// 0 if this is a one-shot, otherwise, the count in minutes
  Runnable task;
  Thread thread, monitor;
  String name;
  GanymedeScheduler scheduler;

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
	startTime.setTime(startTime.getTime() + (long) (60000 * interval));
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

/*------------------------------------------------------------------------------
                                                                           class
                                                                      sampleTask

------------------------------------------------------------------------------*/

class sampleTask implements Runnable {

  String name;

  /* -- */

  public sampleTask(String name)
  {
    this.name = name;
  }

  public synchronized void run()
  {
    System.err.println(name + " reporting in: " + new Date());

    try
      {
	wait(1000);
      }
    catch (InterruptedException ex)
      {
	return;
      }
  }
}
