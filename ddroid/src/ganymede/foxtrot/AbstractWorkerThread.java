/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import javax.swing.SwingUtilities;

/**
 * Partial implementation of the WorkerThread interface.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.4 $
 */
public abstract class AbstractWorkerThread implements WorkerThread
{
   private static final Runnable EMPTY_EVENT = new Runnable()
   {
      public final void run()
      {
      }
   };

   /**
    * Creates a new instance of this AbstractWorkerThread, called by subclasses.
    */
   protected AbstractWorkerThread()
   {
   }

   public void runTask(final Task task)
   {
      if (Worker.debug) System.out.println("[AbstractWorkerThread] Executing task " + task);

      try
      {
         Object obj = AccessController.doPrivileged(new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return task.run();
            }
         }, task.getSecurityContext());

         task.setResult(obj);
      }
      catch (PrivilegedActionException x)
      {
         task.setThrowable(x.getException());
      }
      catch (Throwable x)
      {
         task.setThrowable(x);
      }
      finally
      {
         // Mark the task as completed
         task.setCompleted(true);

         // Needed in case that no events are posted on the AWT Event Queue
         // via the normal mechanisms (mouse movements, key typing, etc):
         // the AWT Event Queue is waiting in EventQueue.getNextEvent(),
         // posting this one will wake it up and allow the event pump to
         // finish its job and release control to the original pump
         SwingUtilities.invokeLater(EMPTY_EVENT);
      }
   }
}
