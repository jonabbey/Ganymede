/*

   GanymedeScheduler.java

   This class is designed to be run in a thread.. it allows the main server
   to register tasks to be run on a periodic basis.

   Created: 26 January 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.common.scheduleHandle;

/*------------------------------------------------------------------------------
                                                                           class
                                                               GanymedeScheduler

------------------------------------------------------------------------------*/

/**
 * <p>Background task scheduler for the Ganymede server.  It is similar
 * in function and behavior to the UNIX cron facility, but is designed
 * to run arbitrary Java Runnable objects in separate threads within
 * the server.</p>
 *
 * <p>The Ganymede server's {@link
 * arlut.csd.ganymede.server.Ganymede#main(java.lang.String[]) main()}
 * routine creates a GanymedeScheduler at server start time.  Once
 * created and start()'ed, the server's GanymedeScheduler runs as an
 * asynchronous thread, sleeping until a task is scheduled to be run.
 * GanymedeScheduler is designed to operate in a multi-threaded
 * fashion, with a background task executing the {@link
 * arlut.csd.ganymede.server.GanymedeScheduler#run() run()} method
 * spending most of its time waiting for something to happen, and
 * various scheduling methods being called interactively to change the
 * behavior of the run() method's on-going execution. (I.e., to
 * schedule new tasks or to change task scheduling)</p>
 *
 * <p>The GanymedeScheduler tracks tasks by name.  Only one task with a
 * given name may be registered with the scheduler at a time.
 * Registering a new task with a given name will cause the scheduler
 * to forget about an old task by the same name.</p>
 *
 * <p>GanymedeScheduler is closely bound to the {@link
 * arlut.csd.ganymede.common.scheduleHandle scheduleHandle} and {@link
 * arlut.csd.ganymede.server.taskMonitor taskMonitor} classes.
 * Together, these three classes form a robust and flexible task
 * scheduling system.</p>
 *
 * <p>The GanymedeScheduler supports updating the Ganymede admin
 * console's task monitor display by way of the {@link
 * arlut.csd.ganymede.rmi.AdminAsyncResponder} interface.  Likewise,
 * the Ganymede server's admin console interface, {@link
 * arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin} supports
 * several remote methods that the admin console can call to affect
 * GanymedeScheduler.</p>
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public final class GanymedeScheduler extends Thread {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.GanymedeScheduler");

  public static final String minutes_str = ts.l("global.minutes"); // "Minutes"
  public static final String hours_str = ts.l("global.hours"); // "Hours"
  public static final String days_str = ts.l("global.days"); // "Days"
  public static final String weeks_str = ts.l("global.weeks"); // "Weeks"

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
    scheduler.start();

    Date currentTime;

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
  private Hashtable<String, scheduleHandle> currentlyScheduled = new Hashtable<String, scheduleHandle>();
  private Hashtable<String, scheduleHandle> currentlyRunning = new Hashtable<String, scheduleHandle>();
  private Hashtable<String, scheduleHandle> onDemand = new Hashtable<String, scheduleHandle>();

  private Vector<scheduleHandle> taskList = new Vector<scheduleHandle>(); // for reporting to admin consoles
  private boolean taskListInitialized = false;

  /**
   * If true, the scheduler will attempt to notify the GanymedeAdmin
   * class when tasks are scheduled and/or completed.
   */

  private boolean reportTasks;

  /**
   * <p>Constructor.  The GanymedeScheduler must be created and then
   * started by calling start() on the constructed
   * GanymedeScheduler.</p>
   *
   * @param reportTasks if true, the scheduler will attempt to notify
   *                    the GanymedeAdmin class when tasks are scheduled
   *                    and/or completed.
   */

  public GanymedeScheduler(boolean reportTasks)
  {
    super("Ganymede Scheduler Thread");
    this.reportTasks = reportTasks;
  }

  /**
   * <p>This method is used to register a task {@link
   * arlut.csd.ganymede.server.DBObject DBObject} record from the
   * Ganymede database in this scheduler, loading the named Runnable
   * class via the Java class loader and scheduling the Runnable for
   * execution according to the parameters specified in the task
   * object.</p>
   */

  public synchronized void registerTaskObject(DBObject object)
  {
    String taskName;
    String taskClass;
    Invid taskDefInvid;
    Class classdef = null;

    /* -- */

    taskName = (String) object.getFieldValueLocal(SchemaConstants.TaskName);
    taskClass = (String) object.getFieldValueLocal(SchemaConstants.TaskClass);
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
        Ganymede.logError(ex, "GanymedeScheduler.registerTaskObject(): class definition could not be found");
        return;
      }

    Runnable task = null;

    // see if we can find a new-style constructor to take the Invid
    // parameter

    Constructor c = null;

    try
      {
        c = classdef.getConstructor(new Class[] {arlut.csd.ganymede.common.Invid.class});
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
            Ganymede.logError(ex, "Error, ran into exception trying to construct task with Invid constructor");
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

    if (task instanceof GanymedeBuilderTask &&
        object.isSet(SchemaConstants.TaskRunOnCommit))
      {
        GanymedeBuilderTask builder = (GanymedeBuilderTask) task;

        builder.runOnCommit(true);
      }

    // if we're not doing a periodic task, just add the task to our
    // demand queue and let the Ganymede initialization code take
    // care of associating the task with the transaction commit
    // process.

    if (!object.isSet(SchemaConstants.TaskRunPeriodically))
      {
        scheduleHandle handle = addActionOnDemand(task, taskName);

        if (task instanceof GanymedeBuilderTask)
          {
            ((GanymedeBuilderTask) task).setScheduleHandle(handle);
          }

        return;
      }

    // We've got a periodic task.. set er up.

    String periodType = (String) object.getFieldValueLocal(SchemaConstants.TaskPeriodUnit);
    int periodMinutes;
    int periodCount = ((Integer) object.getFieldValueLocal(SchemaConstants.TaskPeriodCount)).intValue();
    Date periodAnchor = (Date) object.getFieldValueLocal(SchemaConstants.TaskPeriodAnchor);
    long periodInterval, anchorTime, nowTime, nextRunTime;

    if (periodType.equals(minutes_str))
      {
        periodMinutes = 1;
      }
    else if (periodType.equals(hours_str))
      {
        periodMinutes = 60;
      }
    else if (periodType.equals(days_str))
      {
        periodMinutes = 1440;
      }
    else if (periodType.equals(weeks_str))
      {
        periodMinutes = 10080;
      }
    else
      {
        throw new RuntimeException("GanymedeScheduler Error.. can't register " +
                                   taskName + " with unknown period type.");
      }

    periodMinutes *= periodCount;

    periodInterval = periodMinutes * 60 * 1000L;

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
   * <p>This method is used to add a task to the scheduler that will
   * not be scheduled until specifically requested.</p>
   *
   * <p>If a task with the given name is already registered with the
   * scheduler, that task will be removed from the scheduling queue
   * and registered anew as an on-demand task.</p>
   *
   * @return A reference to the schedulers scheduleHandle for this
   * task.  Modify with extreme caution from outside the scheduler.
   * Generally the task status can be tweaked by the caller, but all
   * other fields should be viewed as private to the scheduler, or at
   * best read-only.
   */

  public synchronized scheduleHandle addActionOnDemand(Runnable task,
                                                       String name)
  {
    scheduleHandle handle;

    /* -- */

    if (task == null || name == null)
      {
        throw new IllegalArgumentException("bad params to GanymedeScheduler.addActionOnDemand()");
      }

    scheduleHandle.TaskType type = scheduleHandle.TaskType.MANUAL;

    if (task instanceof GanymedeBuilderTask)
      {
        if (((GanymedeBuilderTask) task).runsOnCommit())
          {
            type = scheduleHandle.TaskType.BUILDER;
          }
        else
          {
            type = scheduleHandle.TaskType.UNSCHEDULEDBUILDER;
          }
      }
    else if (task instanceof SyncRunner)
      {
        SyncRunner runner = (SyncRunner) task;

        if (runner.isFullState())
          {
            type = scheduleHandle.TaskType.SYNCFULLSTATE;
          }
        else if (runner.isIncremental())
          {
            type = scheduleHandle.TaskType.SYNCINCREMENTAL;
          }
        else
          {
            type = scheduleHandle.TaskType.SYNCMANUAL;
          }
      }

    handle = unregisterTask(name);

    if (handle == null)
      {
        handle = new scheduleHandle(this, null, 0, task, name, type);
      }
    else
      {
        handle.startTime = null;
        handle.setInterval(0);
        handle.task = task;
        handle.tasktype = type;
        handle.setTaskStatus(scheduleHandle.TaskStatus.OK, 0, "");
      }

    onDemand.put(handle.name, handle);

    updateTaskInfo(true);

    return handle;
  }

  /**
   * <p>This method is used to add an action to be run once, at a
   * specific time.</p>
   *
   * <p>If a task with the given name is already registered with the
   * scheduler, that task will be removed from the scheduling queue
   * and registered anew as a single-execution task.</p>
   *
   * @return A reference to the schedulers scheduleHandle for this
   * task.  Modify with extreme caution from outside the scheduler.
   * Generally the task status can be tweaked by the caller, but all
   * other fields should be viewed as private to the scheduler, or at
   * best read-only.
   */

  public synchronized scheduleHandle addTimedAction(Date time,
                                                    Runnable task,
                                                    String name)
  {
    scheduleHandle handle;

    /* -- */

    if (time == null || task == null || name == null)
      {
        throw new IllegalArgumentException("bad params to GanymedeScheduler.addTimedAction()");
      }

    handle = unregisterTask(name);

    if (handle == null)
      {
        handle = new scheduleHandle(this, time, 0, task, name,
                                    scheduleHandle.TaskType.SCHEDULED);
      }
    else
      {
        handle.startTime = time;
        handle.setInterval(0);
        handle.task = task;
        handle.setTaskStatus(scheduleHandle.TaskStatus.OK, 0, "");
      }

    scheduleTask(handle);

    if (logStuff)
      {
        Ganymede.debug("sched: Scheduled task " + name + " for execution at " + time);
      }

    updateTaskInfo(true);

    return handle;
  }

  /**
   * <p>This method is used to add an action to be run every day at a
   * specific time.</p>
   *
   * <p>If a task with the given name is already registered with the
   * scheduler, that task will be removed from the scheduling queue
   * and registered anew as a periodic task.</p>
   *
   * @return A reference to the schedulers scheduleHandle for this
   * task.  Modify with extreme caution from outside the scheduler.
   * Generally the task status can be tweaked by the caller, but all
   * other fields should be viewed as private to the scheduler, or at
   * best read-only.
   */

  public synchronized scheduleHandle addDailyAction(int hour, int minute,
                                                    Runnable task, String name)
  {
    scheduleHandle handle;
    Date time, currentTime;
    Calendar cal = Calendar.getInstance();

    /* -- */

    if (task == null || name == null)
      {
        throw new IllegalArgumentException("bad params to GanymedeScheduler.addDailyAction()");
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
        handle = new scheduleHandle(this, time, minsPerDay, task, name,
                                    scheduleHandle.TaskType.SCHEDULED);
      }
    else
      {
        handle.startTime = time;
        handle.setInterval(minsPerDay);
        handle.task = task;
        handle.setTaskStatus(scheduleHandle.TaskStatus.OK, 0, "");
      }

    scheduleTask(handle);

    if (logStuff)
      {
        Ganymede.debug("sched: Scheduled task " + name + " for daily execution at " + time);
      }

    updateTaskInfo(true);

    return handle;
  }

  /**
   * <p>This method is used to add an action to be run at a specific
   * initial time, and every &lt;intervalMinutes&gt; thereafter.</p>
   *
   * <p>The scheduler will not reschedule a task until the last
   * scheduled instance of the task has completed.</p>
   *
   * <p>If a task with the given name is already registered with the
   * scheduler, that task will be removed from the scheduling queue
   * and registered anew as a periodic task.</p>
   *
   * @return A reference to the schedulers scheduleHandle for this
   * task.  Modify with extreme caution from outside the scheduler.
   * Generally the task status can be tweaked by the caller, but all
   * other fields should be viewed as private to the scheduler, or at
   * best read-only.
   */

  public synchronized scheduleHandle addPeriodicAction(Date firstTime,
                                                       int intervalMinutes,
                                                       Runnable task,
                                                       String name)
  {
    scheduleHandle handle;

    /* -- */

    if (task == null || name == null)
      {
        throw new IllegalArgumentException("bad params to GanymedeScheduler.addPeriodicAction()");
      }

    handle = unregisterTask(name);

    if (handle == null)
      {
        handle = new scheduleHandle(this, firstTime, intervalMinutes, task, name,
                                    scheduleHandle.TaskType.SCHEDULED);
      }
    else
      {
        handle.startTime = firstTime;
        handle.setInterval(intervalMinutes);
        handle.task = task;
        handle.setTaskStatus(scheduleHandle.TaskStatus.OK, 0, "");
      }

    scheduleTask(handle);

    if (logStuff)
      {
        Ganymede.debug("sched: Scheduled task " + name + " for periodic execution at " + firstTime);
        Ganymede.debug("                       Task will repeat every " + intervalMinutes + " minutes");
      }

    updateTaskInfo(true);

    return handle;
  }

  /**
   * <p>This method unregisters the named task so that it can be
   * rescheduled with different parameters, or simply removed.</p>
   *
   * <p>Note that this method will not prevent the scheduler's {@link
   * arlut.csd.ganymede.server.GanymedeScheduler#run() run()} method
   * from briefly waking up unnecessarily if the named task was the
   * next scheduled to be executed.  Easier to have the run() method
   * check to see if any tasks actually need to be run than to try and
   * persuade the run() method not to wake up for the removed
   * task.</p>
   */

  public synchronized scheduleHandle unregisterTask(String name)
  {
    scheduleHandle oldHandle = null;

    /* -- */

    if (currentlyScheduled.containsKey(name))
      {
        oldHandle = currentlyScheduled.remove(name);
      }

    if (onDemand.containsKey(name))
      {
        oldHandle = onDemand.remove(name);
      }

    if (oldHandle != null)
      {
        oldHandle.unregister();
      }

    updateTaskInfo(true);

    return oldHandle;
  }

  /**
   * <p>This method is provided to allow an admin console to cause a
   * registered task to be immediately spawned.</p>
   *
   * @return true if the task is either currently running or was started,
   *         or false if the task could not be found in the list of currently
   *         registered tasks.
   */

  public synchronized boolean runTaskNow(String name)
  {
    if (currentlyRunning.containsKey(name))
      {
        return true;            // it's already running
      }
    else
      {
        scheduleHandle handle = currentlyScheduled.get(name);

        if (handle == null)
          {
            handle = onDemand.get(name);
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
   * <p>This method is provided to allow the server to request that a
   * task listed as being registered 'on-demand' be run as soon as
   * possible.</p>
   *
   * <p>If the task is currently running, it will be flagged to run
   * again as soon as the current run completes.  This is intended to
   * support the need for the server to be able to do back-to-back
   * nis/dns builds.</p>
   *
   * @return false if the task name could not be found on the on-demand
   *         or currently running lists.
   */

  public boolean demandTask(String name)
  {
    return demandTask(name, null);
  }

  /**
   * <p>This method is provided to allow the server to request that a
   * task listed as being registered 'on-demand' be run as soon as
   * possible.</p>
   *
   * <p>If the task is currently running, it will be flagged to run
   * again as soon as the current run completes.  This is intended to
   * support the need for the server to be able to do back-to-back
   * nis/dns builds.</p>
   *
   * @return false if the task name could not be found on the on-demand
   *         or currently running lists.
   */

  public synchronized boolean demandTask(String name, Object options[])
  {
    if (!currentlyRunning.containsKey(name) &&
        !onDemand.containsKey(name))
      {
        return false;
      }
    else
      {
        scheduleHandle handle = currentlyRunning.get(name);

        if (handle != null)
          {
            // currently running.. tell it to reschedule itself when
            // it finishes

            handle.runOnCompletion(options);
            updateTaskInfo(true);
            return true;
          }

        handle = onDemand.get(name);

        if (handle == null)
          {
            return false;
          }

        handle.setOptions(options);

        runTask(handle);

        return true;
      }
  }

  /**
   * <p>This method is provided to allow an admin console to put an
   * immediate halt to a running background task.  Calling this method
   * will result in an interrupt being sent to the running background
   * task.</p>
   *
   * @return true if the task was either not running, or was
   *         running and was told to stop.
   */

  public synchronized boolean stopTask(String name)
  {
    if (!currentlyRunning.containsKey(name))
      {
        return true;            // it's not running
      }
    else
      {
        scheduleHandle handle = currentlyRunning.get(name);

        if (handle == null)
          {
            return false;       // couldn't find task
          }

        handle.stop();

        //      updateTaskInfo(true);

        return true;
      }
  }

  /**
   * <p>This method is provided to allow an admin console to specify
   * that a task be suspended.  Suspended tasks will not be scheduled
   * until later enabled.  If the task is currently running, it will
   * not be interfered with, but the task will not be scheduled for
   * execution in future until re-enabled.</p>
   *
   * @return true if the task was found and disabled
   */

  public synchronized boolean disableTask(String name)
  {
    scheduleHandle handle = null;

    /* -- */

    handle = findHandle(name);

    if (handle == null)
      {
        return false;           // couldn't find it
      }
    else
      {
        handle.disable();
        updateTaskInfo(true);
        return true;
      }
  }

  /**
   * <p>This method is provided to allow an admin console to specify
   * that a task be re-enabled after a suspension.</p>
   *
   * <p>A re-enabled task will be scheduled for execution according to
   * its original schedule, with any runtimes that would have been
   * issued during the time the task was suspended simply skipped.</p>
   *
   * @return true if the task was found and enabled
   */

  public synchronized boolean enableTask(String name)
  {
    scheduleHandle handle = null;

    /* -- */

    handle = findHandle(name);

    if (handle == null)
      {
        return false;           // couldn't find it
      }
    else
      {
        handle.enable();

        updateTaskInfo(true);
        return true;
      }
  }

  /**
   * <p>This method returns a reference to the runnable registered
   * in the scheduler by the given name.</p>
   */

  public synchronized Runnable getTask(String name)
  {
    scheduleHandle handle = findHandle(name);

    if (handle == null)
      {
        return null;
      }

    return handle.task;
  }

  /**
   * <p>This method returns a List of handles known to the scheduler
   * of the given type.</p>
   */

  public synchronized List<scheduleHandle> getTasksByType(scheduleHandle.TaskType type)
  {
    ArrayList<scheduleHandle> results = new ArrayList<scheduleHandle>();

    for (scheduleHandle handle: taskList)
      {
        if (handle.getTaskType() == type)
          {
            results.add(handle);
          }
      }

    return results;
  }

  /**
   * <p>This method returns a List of handles known to the scheduler
   * whose Runnable inherits from classObj.</p>
   */

  public synchronized List<scheduleHandle> getTasksByClass(Class classObj)
  {
    ArrayList<scheduleHandle> results = new ArrayList<scheduleHandle>();

    for (scheduleHandle handle: taskList)
      {
        if (classObj.isInstance(handle.task))
          {
            results.add(handle);
          }
      }

    return results;
  }

  /**
   * <p>Private helper to find a given handle by name amongst the
   * various internal structures.  Unsynchronized, so call from
   * a synchronized method if you care.</p>
   */

  private final scheduleHandle findHandle(String name)
  {
    scheduleHandle handle = null;

    /* -- */

    handle = currentlyRunning.get(name);

    if (handle == null)
      {
        handle = currentlyScheduled.get(name);
      }

    if (handle == null)
      {
        handle = onDemand.get(name);
      }

    return handle;
  }

  /**
   * <p>This method is responsible for carrying out the scheduling
   * work of this class on a background thread.</p>
   *
   * <p>The basic logic is to wait until the next action is due to
   * run, move the task from our scheduled list to our running list,
   * and run it.  Other synchronized methods such as {@link
   * arlut.csd.ganymede.server.GanymedeScheduler#runTask(arlut.csd.ganymede.common.scheduleHandle)
   * runTask()}, {@link
   * arlut.csd.ganymede.server.GanymedeScheduler#scheduleTask(arlut.csd.ganymede.common.scheduleHandle)
   * scheduleTask()}, and {@link
   * arlut.csd.ganymede.server.GanymedeScheduler#notifyCompletion(arlut.csd.ganymede.common.scheduleHandle)
   * notifyCompletion()}, may be called while this method is waiting
   * for something to happen.  These methods modify the data
   * structures that run() uses to determine its scheduling needs.</p>
   */

  public synchronized void run()
  {
    long currentTime, sleepTime;

    /* -- */

    try
      {
        if (logStuff)
          {
            Ganymede.debug("Ganymede Scheduler: scheduling task started");
          }

        while (true)
          {
            try
              {
                if (isInterrupted())
                  {
                    Ganymede.debug("Scheduler interrupted");
                    return;     // jump to finally, then return
                  }

                if (debug)
                  {
                    System.err.println("Ganymede Scheduler at top of loop");
                  }

                if (nextAction == null)
                  {
                    try
                      {
                        if (debug)
                          {
                            System.err.println("*** snooze");
                          }

                        if (debug)
                          {
                            System.err.println("Ganymede Scheduler loop waiting for wakeup");
                          }

                        wait(); // scheduleTask() can wake us up here via notify()
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
                                    System.err.println("Ganymede Scheduler loop hitting the snooze button");
                                  }

                                wait(sleepTime);        // scheduleTask() can wake us up here via notify()
                              }
                            catch (InterruptedException ex)
                              {
                                System.err.println("Scheduler interrupted");
                                return; // jump to finally, then return
                              }

                            if (debug)
                              {
                                System.err.println("Ganymede Scheduler loop waking from snooze");
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
                        Vector<scheduleHandle> toRun = new Vector<scheduleHandle>();
                        Date nextRun = null;

                        for (scheduleHandle handle: currentlyScheduled.values())
                          {
                            if (handle.startTime.getTime() <= currentTime)
                              {
                                toRun.add(handle);
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

                        for (scheduleHandle handle: toRun)
                          {
                            if (debug)
                              {
                                System.err.println("Ganymede Scheduler loop running " + handle);
                              }

                            runTask(handle);
                          }
                      }
                  }
              }
            catch (Throwable ex)
              {
                System.err.println("Ganymede scheduler caught exception");
                System.err.println(Ganymede.stackTrace(ex));

                try
                  {
                    // we want to sleep a little bit so that if the Throwable we caught was non-transient
                    // we won't completely eat the CPU.

                    wait(5);
                  }
                catch (InterruptedException ex2)
                  {
                    System.err.println("Scheduler interrupted");
                    return; // jump to finally, then return
                  }
              }
          }
      }
    finally
      {
        if (logStuff)
          {
            Ganymede.debug("Ganymede Scheduler going down");
          }

        cleanUp();              // send interrupts to all running tasks

        if (logStuff)
          {
            Ganymede.debug("Ganymede Scheduler exited");
          }
      }
  }

  /**
   * <p>This private method is used by the GanymedeScheduler thread's
   * main loop to put a task in the scheduled hash onto the run
   * hash</p>
   */

  private synchronized void runTask(scheduleHandle handle)
  {
    if ((currentlyScheduled.remove(handle.name) != null) ||
        (onDemand.remove(handle.name) != null))
      {
        if (logStuff && !(handle.task instanceof silentTask))
          {
            Ganymede.debug("sched: Running " + handle.name);
          }

        currentlyRunning.put(handle.name, handle);
        handle.runTask();
        updateTaskInfo(true);
      }
  }

  /**
   * <p>This method is used by instances of {@link
   * arlut.csd.ganymede.common.scheduleHandle scheduleHandle} to let
   * the GanymedeScheduler thread know when their tasks have run to
   * completion.  This method is responsible for rescheduling the task
   * if it is a periodic task.</p>
   */

  public synchronized void notifyCompletion(scheduleHandle handle)
  {
    if (currentlyRunning.remove(handle.name) != null)
      {
        if (logStuff && !(handle.task instanceof silentTask))
          {
            Ganymede.debug("sched: " + handle.name + " completed");
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
                    Ganymede.debug("sched: rescheduling task " +
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
        Ganymede.debug("sched: confusion! Couldn't find task " +
                       handle.name + " on the runnng list");
      }
  }

  /**
   * <p>This private method takes a task that needs to be scheduled
   * and adds it to the scheduler.  All scheduling additions or
   * changes are handled by this method.  This is the only method in
   * GanymedeScheduler that can notify the run() method that it may
   * need to wake up early to handle a newly registered task.</p>
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
        notify();       // let the scheduler know about our newly scheduled event
        return;
      }

    if (handle.startTime.getTime() < nextAction.getTime())
      {
        nextAction.setTime(handle.startTime.getTime());
        notify();       // let the scheduler know about our newly scheduled event
      }

    // notice that we use notify() rather than notifyAll() for efficiency..
    // if for some unforseeable reason someone decides that something else
    // in Ganymede besides the GanymedeScheduler run() method just *has*
    // to wait() on this object, this will need to be changed.
  }

  /**
   * <p>This method is run when the GanymedeScheduler thread is
   * terminated.  It kills off any background processes currently
   * running.  Those threads should have a finally clause that can
   * handle abrupt termination.</p>
   */

  private synchronized void cleanUp()
  {
    // note that it is only safe to do this loop on the
    // currentlyRunning enumeration because cleanUp() is
    // synchronized.. any threads that finish up will have to wait
    // until we return before getting into
    // GanymedeScheduler.notifyCompletion()

    for (scheduleHandle handle: currentlyRunning.values())
      {
        handle.stop();
      }
  }

  /**
   * <p>This method is used to report to the Ganymede server (and
   * thence the admin console(s) the status of background tasks
   * scheduled and/or running.</p>
   */

  private synchronized void updateTaskInfo(boolean updateConsoles)
  {
    if (debug)
      {
        System.err.println("Ganymede Scheduler running updateTaskInfo()");
      }

    HashSet<scheduleHandle> tempSet = new HashSet<scheduleHandle>();

    tempSet.addAll(currentlyScheduled.values());
    tempSet.addAll(currentlyRunning.values());
    tempSet.addAll(onDemand.values());

    taskList = new Vector<scheduleHandle>(tempSet);
    taskListInitialized = true;

    if (reportTasks && updateConsoles)
      {
        GanymedeAdmin.refreshTasks();
      }

    if (debug)
      {
        System.err.println("Ganymede Scheduler exiting updateTaskInfo()");
      }
  }

  /**
   * <p>Returns a Vector of {@link
   * arlut.csd.ganymede.common.scheduleHandle scheduleHandle} objects
   * suitable for reporting to the admin console.</p>
   */

  synchronized Vector<scheduleHandle> reportTaskInfo()
  {
    if (!taskListInitialized)
      {
        updateTaskInfo(false);
      }

    // we need to clone the taskList, since the server is now
    // communicating with the admin consoles asynchronously, using the
    // serverAdminProxy.

    return new Vector<scheduleHandle>(taskList);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      sampleTask

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to produce test task objects for the {@link
 * arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler}
 * class.  It print a message when it is run, then waits one second
 * before returning.</p>
 */

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

    // wait to take up a bit of time for this task.

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
