/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

/**
 * {@link Worker} uses an implementation of this interface to run
 * {@link Task}s in a thread that is not the Event Dispatch Thread. <br>
 * Implementations should extend {@link AbstractWorkerThread}.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.3 $
 */
public interface WorkerThread
{
   /**
    * Starts this WorkerThread, responsible for running {@link Task}s (not in the
    * Event Dispatch Thread).
    * Applets can stop threads used by implementations of this WorkerThread in any moment,
    * and {@link Worker} will call this method also to restart this WorkerThread
    * if it results that it is not alive anymore.
    * @see #isAlive
    */
   public void start();

   /**
    * Returns whether this WorkerThread is alive. It is not enough to return
    * whether this WorkerThread has been started, because Applets can stop threads
    * used by implementations of this WorkerThread in any moment.
    * If this WorkerThread is not alive, {@link Worker} will restart it.
    * @see #start
    */
   public boolean isAlive();

   /**
    * Returns whether the current thread is a thread used by the implementation of
    * this WorkerThread to run {@link Task}s.
    */
   public boolean isWorkerThread();

   /**
    * Posts a Task to be run by this WorkerThread in a thread that is not the
    * Event Dispatch Thread. This method is called by {@link Worker} from the
    * Event Dispatch Thread and should return immediately.
    * @see #runTask
    */
   public void postTask(Task task);

   /**
    * Runs the given Task. This method must be called by a thread that is not the
    * Event Dispatch Thread.
    * @see #postTask
    */
   public void runTask(Task task);
}
