/*

   GanymedeScheduler.java

   This class is designed to be run in a thread.. it allows the main server
   to register tasks to be run on a periodic basis.
   
   Created: 26 January 1998
   Release: $Name:  $
   Version: $Revision: 1.25 $
   Last Mod Date: $Date: 2001/02/13 06:36:24 $
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.util.*;
import java.lang.reflect.*;

import arlut.csd.Util.VectorUtils;

/*------------------------------------------------------------------------------
                                                                           class
                                                               GanymedeScheduler

------------------------------------------------------------------------------*/

/**
 * <P>Background task scheduler for the Ganymede server.  It is similar in
 * function and behavior to the UNIX cron facility, but is designed
 * to run arbitrary Java Runnable objects in separate threads within the server.</P>
 *
 * <P>The Ganymede server's 
 * {@link arlut.csd.ganymede.Ganymede#main(java.lang.String[]) main()} routine
 * creates a GanymedeScheduler at server start time.  Once created, the
 * server's GanymedeScheduler runs as an asynchronous thread, sleeping until
 * a task is scheduled to be run.  GanymedeScheduler is designed to operate
 * in a multi-threaded fashion, with a background task executing the 
 * {@link arlut.csd.ganymede.GanymedeScheduler#run() run()} method spending
 * most of its time waiting for something to happen, and various scheduling
 * methods being called interactively to change the behavior of the run() method's
 * on-going execution. (I.e., to schedule new tasks or to change task scheduling)</P>
 *
 * <P>The GanymedeScheduler tracks tasks by name.  Only one task with a given name
 * may be registered with the scheduler at a time.  Registering a new task
 * with a given name will cause the scheduler to forget about an old task by the
 * same name.</P>
 *
 * <P>GanymedeScheduler is closely bound to the 
 * {@link arlut.csd.ganymede.scheduleHandle scheduleHandle} and
 * {@link arlut.csd.ganymede.taskMonitor taskMonitor} classes.  Together, these
 * three classes form a robust and flexible task scheduling system.</P>
 *
 * <P>The GanymedeScheduler supports updating the Ganymede admin console's
 * task monitor display by way of the {@link arlut.csd.ganymede.Admin Admin} 
 * interface.  Likewise, the Ganymede server's admin console interface,
 * {@link arlut.csd.ganymede.GanymedeAdmin GanymedeAdmin} supports several
 * remote methods that the admin console can call to affect GanymedeScheduler.</P>
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class GanymedeScheduler extends Thread {

  static final int minsPerDay = 1440;
  static final boolean debug = false;
  static final boolean logStuff = true;

  /**
   *
   * Debug rig
   *
   */

  public static void main(String[] argv)
  {
    GanymedeScheduler scheduler = new GanymedeScheduler(false);
    new Thread(scheduler).start();

    Date time, currentTime;

    currentTime = new Date();

    scheduler.addTimedAction(currentTime,
			     new sampleTask("sample task 1"), 
			     "sample task 1");

    currentTime = new Date(currentTime.getTime() + 1000*60);

    scheduler.addPeriodicAction(currentTime, 1,
				new sampleTask("sample task 2"), 
				"sample task 2");

    currentTime = new Date(currentTime.getTime() + 1000*60);
    
    scheduler.addTimedAction(currentTime,
			     new sampleTask("sample task 3"),
			     "sample task 3");

    currentTime = new Date(currentTime.getTime() + 1000*60);

    scheduler.addPeriodicAction(currentTime, 1,
				new sampleTask("sample task 4"), 
				"sample task 4");
  }

  // --- end statics

  Date nextAction = null;
  private Hashtable currentlyScheduled = new Hashtable();
  private Hashtable currentlyRunning = new Hashtable();
  private Hashtable onDemand = new Hashtable();

  private Vector taskList = new Vector();	// for reporting to admin consoles
  private boolean taskListInitialized = false;

  /**
   *
   * if true, the scheduler will attempt to notify the GanymedeAdmin class when
   * tasks are scheduled and/or completed.
   *
   */

  private boolean reportTasks;

  /**
   *
   * Constructor
   *
   * @param reportTasks if true, the scheduler will attempt to notify
   *                    the GanymedeAdmin class when tasks are scheduled
   *                    and/or completed.
   *
   */

  public GanymedeScheduler(boolean reportTasks)
  {
    this.reportTasks = reportTasks;
  }

  /**
   * <P>This method is used to register a task
   * {@link arlut.csd.ganymede.DBObject DBObject} record from
   * the Ganymede database in this
   * scheduler, loading the named Runnable class via the Java
   * class loader and scheduling the Runnable for execution according
   * to the parameters specified in the task object.</P>
   */

  public synchronized void registerTaskObject(DBObject object)
  {
    String taskName;
    String taskClass;
    Invid taskDefInvid;
    Class classdef = null;

    /* -- */

    taskName = (String) object.getFieldValue(SchemaConstants.TaskName);
    taskClass = (String) object.getFieldValue(SchemaConstants.TaskClass);
    taskDefInvid = object.getInvid();

    if (taskName == null || taskClass == null)
      {
	throw new IllegalArgumentException("task object not filled out adequately");
      }

    try
      {
	classdef = Class.forName(taskClass);
      }
    catch (ClassNotFoundException ex)
      {
	ex.printStackTrace();

	System.err.println("GanymedeScheduler.registerTaskObject(): class definition could not be found: " + ex);
	return;
      }
		
    Runnable task = null;

    // see if we can find a new-style constructor to take the Invid
    // parameter

    Constructor c = null;
    
    try
      {
	c = classdef.getConstructor(new Class[] {arlut.csd.ganymede.Invid.class});
      }
    catch (NoSuchMethodException ex)
      {
	// oh, well
      }
    
    if (c != null)
      {
	try
	  {
	    task = (Runnable) c.newInstance(new Object[] {taskDefInvid});
	  }
	catch (Exception ex)
	  {
	    System.err.println("Error, ran into exception trying to construct task with Invid constructor");
	    ex.printStackTrace();
	  }
      }

    // if we weren't able to find a constructor taking an Invid, use
    // the no-arg constructor

    if (task == null)
      {
	try
	  {
	    task = (Runnable) classdef.newInstance(); // using no param constructor
	  }
	catch (IllegalAccessException ex)
	  {
	    System.err.println("IllegalAccessException " + ex);
	  }
	catch (InstantiationException ex)
	  {
	    System.err.println("InstantiationException " + ex);
	  }
      }
    
    if (task == null)
      {
	return;
      }

    // if we're not doing a periodic task, just add the task to our
    // demand queue and let the Ganymede initialization code take
    // care of associating the task with the transaction commit
    // process.

    if (!object.isSet(SchemaConstants.TaskRunPeriodically))
      {
	addActionOnDemand(task, taskName);
	return;
      }

    // We've got a periodic task.. set er up.

    String periodType = (String) object.getFieldValueLocal(SchemaConstants.TaskPeriodUnit);
    int periodMinutes;
    int periodCount = ((Integer) object.getFieldValueLocal(SchemaConstants.TaskPeriodCount)).intValue();
    Date periodAnchor = (Date) object.getFieldValueLocal(SchemaConstants.TaskPeriodAnchor);
    long periodInterval, anchorTime, nowTime, nextRunTime;
    
    if (periodType.equals("Minutes"))
      {
	periodMinutes = 1;
      }
    else if (periodType.equals("Hours"))
      {
	periodMinutes = 60;
      }
    else if (periodType.equals("Days"))
      {
	periodMinutes = 1440;
      }
    else if (periodType.equals("Weeks"))
      {
	periodMinutes = 10080;
      }
    else 
      {
	throw new RuntimeException("GanymedeScheduler Error.. can't register " + 
				   taskName + " with unknown period type.");
      }
    
    periodMinutes *= periodCount;
    
    periodInterval = periodMinutes * 60 * 1000;
    
    // we need to find the next moment to run the task with, using the periodAnchor
    
    if (periodAnchor == null)
      {
	anchorTime = System.currentTimeMillis();
	nextRunTime = anchorTime + periodInterval;
      }
    else
      {
	anchorTime = periodAnchor.getTime();
	nowTime = System.currentTimeMillis();

	if (anchorTime <= nowTime)
	  {
	    // we want the first time this task is run to be the
	    // first time after the current time that is an even
	    // multiple of periodInterval after anchorTime

	    long periodsPassed = (nowTime - anchorTime) / periodInterval;
	    nextRunTime = anchorTime + periodInterval * (periodsPassed + 1);
	  }
	else
	  {
	    // hm. We could just take anchorTime as the first time to run
	    // the task, but I'd prefer to use the periodAnchor solely as
	    // a sign for what time of day, what day of week to run the
	    // task on.  So, we need to calculate when we next come up
	    // to an even number of intervals before the anchor time.

	    long periodsAway = (anchorTime - nowTime) / periodInterval;
	    nextRunTime = anchorTime - periodInterval * periodsAway;
	  }
      }

    addPeriodicAction(new Date(nextRunTime), periodMinutes, task, taskName);
  }

  /**
   * <P>This method is used to add a task to the scheduler that will not
   * be scheduled until specifically requested.</P>
   *
   * <P>If a task with the given name is already registered with the
   * scheduler, that task will be removed from the scheduling queue
   * and registered anew as an on-demand task.</P>
   */

  public synchronized void addActionOnDemand(Runnable task,
					     String name)
  {
    scheduleHandle handle;

    /* -- */

    if (task == null || name == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    handle = unregisterTask(name);

    if (handle == null)
      {
	handle = new scheduleHandle(this, null, 0, task, name);
      }
    else
      {
	handle.startTime = null;
	handle.setInterval(0);
	handle.task = task;
	handle.reregister = true;
      }

    onDemand.put(handle.name, handle);
  }

  /**
   * <P>This method is used to add an action to be run once, at a specific time.</P>
   *
   * <P>If a task with the given name is already registered with the
   * scheduler, that task will be removed from the scheduling queue
   * and registered anew as a single-execution task.</P>
   */

  public synchronized void addTimedAction(Date time, 
					  Runnable task,
					  String name)
  {
    scheduleHandle handle;

    /* -- */

    if (time == null || task == null || name == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    handle = unregisterTask(name);

    if (handle == null)
      {
	handle = new scheduleHandle(this, time, 0, task, name);
      }
    else
      {
	handle.startTime = time;
	handle.setInterval(0);
	handle.task = task;
	handle.reregister = true;
      }

    scheduleTask(handle);

    if (logStuff)
      {
	System.err.println("Ganymede Scheduler: Scheduled task " + name + " for execution at " + time);
      }
  }

  /**
   * <P>This method is used to add an action to be run every day at a specific time.</P>
   *
   * <P>If a task with the given name is already registered with the
   * scheduler, that task will be removed from the scheduling queue
   * and registered anew as a periodic task.</P>
   */

  public synchronized void addDailyAction(int hour, int minute, 
					  Runnable task, String name)
  {
    scheduleHandle handle;
    Date time, currentTime;
    Calendar cal = Calendar.getInstance();

    /* -- */

    if (task == null || name == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    handle = unregisterTask(name);

    currentTime = new Date();

    cal.setTime(currentTime);

    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);

    time = cal.getTime();

    if (time.before(currentTime))
      {
	cal.add(Calendar.DATE, 1); // advance to this time tomorrow
      }

    time = cal.getTime();

    if (handle == null)
      {
	handle = new scheduleHandle(this, time, minsPerDay, task, name);
      }
    else
      {
	handle.startTime = time;
	handle.setInterval(minsPerDay);
	handle.task = task;
	handle.reregister = true;
      }

    scheduleTask(handle);

    if (logStuff)
      {
	System.err.println("Ganymede Scheduler: Scheduled task " + name + " for daily execution at " + time);
      }
  }

  /**
   * <P>This method is used to add an action to be run at a specific
   * initial time, and every &lt;intervalMinutes&gt; thereafter.</P>
   *
   * <P>The scheduler will not reschedule a task until the last scheduled
   * instance of the task has completed.</P>
   *
   * <P>If a task with the given name is already registered with the
   * scheduler, that task will be removed from the scheduling queue
   * and registered anew as a periodic task.</P>
   */

  public synchronized void addPeriodicAction(Date firstTime,
					     int intervalMinutes, 
					     Runnable task,
					     String name)
  {
    scheduleHandle handle;

    /* -- */

    if (task == null || name == null)
      {
	throw new IllegalArgumentException("bad params to GanymedeScheduler.addAction()");
      }

    handle = unregisterTask(name);

    if (handle == null)
      {
	handle = new scheduleHandle(this, firstTime, intervalMinutes, task, name);
      }
    else
      {
	handle.startTime = firstTime;
	handle.setInterval(intervalMinutes);
	handle.task = task;
	handle.reregister = true;
      }

    scheduleTask(handle);

    if (logStuff)
      {
	System.err.println("Ganymede Scheduler: Scheduled task " + name + " for periodic execution at " + firstTime);
	System.err.println("                    Task will repeat every " + intervalMinutes + " minutes");
      }
  }

  /**
   * <P>This method unregisters the named task so that it can be
   * rescheduled with different parameters, or simply removed.</P>
   *
   * <P>Note that this method will not prevent the scheduler's
   * {@link arlut.csd.ganymede.GanymedeScheduler#run() run()} method from
   * briefly waking up unnecessarily if the named task was the next scheduled to be
   * executed.  Easier to have the run() method check to see if any
   * tasks actually need to be run than to try and persuade the run()
   * method not to wake up for the removed task.</P>
   */

  public synchronized scheduleHandle unregisterTask(String name)
  {
    scheduleHandle oldHandle = null;

    /* -- */

    if (currentlyScheduled.containsKey(name))
      {
	oldHandle = (scheduleHandle) currentlyScheduled.remove(name);
      }
    
    if (onDemand.containsKey(name))
      {
	oldHandle = (scheduleHandle) onDemand.remove(name);
      }

    if (oldHandle != null)
      {
	oldHandle.unregister();
      }

    return oldHandle;
  }

  /**
   * <P>This method is provided to allow an admin console to cause a registered
   * task to be immediately spawned.</P>
   *
   * @return true if the task is either currently running or was started, 
   *         or false if the task could not be found in the list of currently
   *         registered tasks.
   */

  public synchronized boolean runTaskNow(String name)
  {
    if (currentlyRunning.containsKey(name))
      {
	return true;		// it's already running
      }
    else
      {
	scheduleHandle handle = (scheduleHandle) currentlyScheduled.get(name);

	if (handle == null)
	  {
	    handle = (scheduleHandle) onDemand.get(name);
	  }

	if (handle == null)
	  {
	    return false;
	  }

	runTask(handle);

	return true;
      }
  }

  /**
   * <P>This method is provided to allow the server to request that a task
   * listed as being registered 'on-demand' be run as soon as possible.</P>
   *
   * <P>If the task is currently running, it will be flagged to run again
   * as soon as the current run completes.  This is intended to support
   * the need for the server to be able to do back-to-back nis/dns builds.</P>
   *
   * @return false if the task name could not be found on the on-demand
   *         or currently running lists.
   */

  public synchronized boolean demandTask(String name)
  {
    if (!currentlyRunning.containsKey(name) &&
	!onDemand.containsKey(name))
      {
	return false;
      }
    else
      {
	scheduleHandle handle = (scheduleHandle) currentlyRunning.get(name);

	if (handle != null)
	  {
	    // currently running.. tell it to reschedule itself when
	    // it finishes

	    handle.runOnCompletion();
	    updateTaskInfo(true);
	    return true;
	  }

	handle = (scheduleHandle) onDemand.get(name);

	if (handle == null)
	  {
	    return false;
	  }

	runTask(handle);

	return true;
      }
  }
  
  /**
   * <P>This method is provided to allow an admin console to put an
   * immediate halt to a running background task.</P>
   *
   * @return true if the task was either not running, or was
   *         running and was told to stop.
   */

  public synchronized boolean stopTask(String name)
  {
    if (!currentlyRunning.containsKey(name))
      {
	return true;		// it's not running
      }
    else
      {
	scheduleHandle handle = (scheduleHandle) currentlyRunning.get(name);

	if (handle == null)
	  {
	    return false;	// couldn't find task
	  }

	handle.stop();

	//	updateTaskInfo(true);

	return true;
      }
  }

  /**
   * <P>This method is provided to allow an admin console to specify
   * that a task be suspended.  Suspended tasks will not be
   * scheduled until later enabled.  If the task is currently running,
   * it will not be interfered with, but the task will not be
   * scheduled for execution in future until re-enabled.</P>
   *
   * @return true if the task was found and disabled
   */

  public synchronized boolean disableTask(String name)
  {
    scheduleHandle handle = null;

    /* -- */

    handle = (scheduleHandle) currentlyRunning.get(name);

    if (handle == null)
      {
	handle = (scheduleHandle) currentlyScheduled.get(name);
      }

    if (handle == null)
      {
	return false;		// couldn't find it
      }
    else
      {
	handle.disable();
	updateTaskInfo(true);
	return true;
      }
  }

  /**
   * <P>This method is provided to allow an admin console to specify
   * that a task be re-enabled after a suspension.</P>
   *
   * <P>A re-enabled task will be scheduled for execution according
   * to its original schedule, with any runtimes that would have
   * been issued during the time the task was suspended simply
   * skipped.</P>
   *
   * @return true if the task was found and enabled
   */

  public synchronized boolean enableTask(String name)
  {
    scheduleHandle handle = null;

    /* -- */

    handle = (scheduleHandle) currentlyRunning.get(name);

    if (handle == null)
      {
	handle = (scheduleHandle) currentlyScheduled.get(name);
      }

    if (handle == null)
      {
	return false;		// couldn't find it
      }
    else
      {
	handle.enable();

	updateTaskInfo(true);
	return true;
      }
  }

  /**
   * <P>This method is responsible for carrying out the scheduling
   * work of this class on a background thread.</P>
   *
   * <P>The basic logic is to wait until the next action is due to run,
   * move the task from our scheduled list to our running list, and
   * run it.  Other synchronized methods such as
   * {@link arlut.csd.ganymede.GanymedeScheduler#runTask(arlut.csd.ganymede.scheduleHandle) runTask()},
   * {@link arlut.csd.ganymede.GanymedeScheduler#scheduleTask(arlut.csd.ganymede.scheduleHandle) scheduleTask()},
   * and
   * {@link arlut.csd.ganymede.GanymedeScheduler#notifyCompletion(arlut.csd.ganymede.scheduleHandle) notifyCompletion()},
   * may be called while this method is waiting for something to
   * happen.  These methods modify the data structures that run()
   * uses to determine its scheduling needs.</P>
   */

  public synchronized void run()
  {
    long currentTime, sleepTime;
    scheduleHandle handle;

    /* -- */

    try
      {
	if (logStuff)
	  {
	    System.err.println("Ganymede Scheduler: scheduling task started");
	  }

	while (true)
	  {
	    if (isInterrupted())
	      {
		System.err.println("Scheduler interrupted");
		return;	// jump to finally, then return
	      }

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

		    wait();	// scheduleTask() can wake us up here via notify()
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

			    wait(sleepTime);	// scheduleTask() can wake us up here via notify()
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
		    Enumeration enum;
		    
		    enum = currentlyScheduled.elements();

		    // enum may be empty if the task that we woke ourselves
		    // up for was unregistered while we were sleeping

		    while (enum.hasMoreElements())
		      {
			handle = (scheduleHandle) enum.nextElement();
			
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
	if (logStuff)
	  {
	    System.err.println("Ganymede Scheduler going down");
	  }

	cleanUp();		// send interrupts to all running tasks

	if (logStuff)
	  {
	    System.err.println("Ganymede Scheduler exited");
	  }
      }
  }

  /**
   * <P>This private method is used by the GanymedeScheduler thread's main
   * loop to put a task in the scheduled hash onto the run hash</P>
   */

  private synchronized void runTask(scheduleHandle handle)
  {
    if ((currentlyScheduled.remove(handle.name) != null) ||
	(onDemand.remove(handle.name) != null))
      {
	if (logStuff && !(handle.task instanceof silentTask))
	  {
	    System.err.println("Ganymede Scheduler: running " + handle.name);
	  }

	currentlyRunning.put(handle.name, handle);
	handle.runTask();
	updateTaskInfo(true);
      }
  }

  /**
   * <P>This method is used by instances of
   * {@link arlut.csd.ganymede.scheduleHandle scheduleHandle} to let the
   * GanymedeScheduler thread know when their tasks have run to
   * completion.  This method is responsible for rescheduling
   * the task if it is a periodic task.</P>
   */

  synchronized void notifyCompletion(scheduleHandle handle)
  {
    if (currentlyRunning.remove(handle.name) != null)
      {
	if (logStuff && !(handle.task instanceof silentTask))
	  {
	    System.err.println("Ganymede Scheduler: " + handle.name + " completed");
	  }

	// we need to check to see if the task was ordinarily scheduled to
	// start at some time in the future to handle the case where a
	// console forced us to run a task early.. if the task wasn't
	// yet due to run, we don't want to make it skip its normally
	// scheduled next run

	if (handle.startTime != null && handle.startTime.after(new Date()))
	  {
	    scheduleTask(handle);
	  }
	else
	  {
	    if (handle.reschedule())
	      {
		if (logStuff && !(handle.task instanceof silentTask))
		  {
		    System.err.println("Ganymede Scheduler: rescheduling task " + 
				       handle.name + " for " + handle.startTime);
		  }
		
		scheduleTask(handle);
	      }
	    else if (handle.startTime == null)
	      {
		onDemand.put(handle.name, handle); // put it back on the onDemand track

		if (handle.runAgain())
		  {
		    runTask(handle); // task was demanded during its execution.. immediately re-run
		  }
	      }
	  }

	updateTaskInfo(true);
      }
    else
      {
	System.err.println("Ganymede Scheduler: confusion! Couldn't find task " + 
			   handle.name + " on the runnng list");
      }
  }

  /**  
   * <P>This private method takes a task that needs to be scheduled
   * and adds it to the scheduler.  All scheduling additions or
   * changes are handled by this method.  This is the only method in
   * GanymedeScheduler that can notify the run() method that it may
   * need to wake up early to handle a newly registered task.</P> 
   */

  private synchronized void scheduleTask(scheduleHandle handle)
  {
    currentlyScheduled.put(handle.name, handle);

    if (debug)
      {
	System.err.println("Ganymede Scheduler: scheduled task " + handle.name + 
			   " for initial execution at " + handle.startTime);
      }

    if (nextAction == null)
      {
	nextAction = new Date(handle.startTime.getTime());
	notify();	// let the scheduler know about our newly scheduled event
	return;
      }

    if (handle.startTime.getTime() < nextAction.getTime())
      {
	nextAction.setTime(handle.startTime.getTime());
	notify();	// let the scheduler know about our newly scheduled event
      }

    // notice that we use notify() rather than notifyAll() for efficiency..
    // if for some unforseeable reason someone decides that something else
    // in Ganymede besides the GanymedeScheduler run() method just *has*
    // to wait() on this object, this will need to be changed.
  }

  /**
   * <P>This method is run when the GanymedeScheduler thread is
   * terminated.  It kills off any background processes currently
   * running.  Those threads should have a finally clause that can
   * handle abrupt termination.</P>
   */

  private synchronized void cleanUp()
  {
    scheduleHandle handle;
    Enumeration enum;

    /* -- */

    // note that it is only safe to do this loop on the
    // currentlyRunning enumeration because cleanUp() is
    // synchronized.. any threads that finish up will have to wait
    // until we return before getting into
    // GanymedeScheduler.notifyCompletion()

    enum = currentlyRunning.elements();

    while (enum.hasMoreElements())
      {
	handle = (scheduleHandle) enum.nextElement();
	
	handle.stop();
      }
  }

  /**
   * <P>This method is used to report to the Ganymede server (and thence
   * the admin console(s) the status of background tasks scheduled
   * and/or running.</P>
   */

  private synchronized void updateTaskInfo(boolean updateConsoles)
  {
    Enumeration enum;

    /* -- */

    if (reportTasks)
      {    
	taskList.setSize(0);

	enum = currentlyScheduled.elements();

	while (enum.hasMoreElements())
	  {
	    VectorUtils.unionAdd(taskList, enum.nextElement());
	  }
    
	enum = currentlyRunning.elements();
	while (enum.hasMoreElements())
	  {
	    VectorUtils.unionAdd(taskList, enum.nextElement());
	  }

	enum = onDemand.elements();
	while (enum.hasMoreElements())
	  {
	    VectorUtils.unionAdd(taskList, enum.nextElement());
	  }

	taskListInitialized = true;

	if (updateConsoles)
	  {
	    GanymedeAdmin.refreshTasks();
	  }
      }
  }

  /**
   * <P>Returns a Vector of {@link arlut.csd.ganymede.scheduleHandle scheduleHandle}
   * objects suitable for reporting to the admin console.</P>
   */

  synchronized Vector reportTaskInfo()
  {
    if (!taskListInitialized)
      {
	updateTaskInfo(false);
      }

    // we need to clone the taskList, since the server is now
    // communicating with the admin consoles asynchronously, using the
    // serverAdminProxy.

    return (Vector) taskList.clone();
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
