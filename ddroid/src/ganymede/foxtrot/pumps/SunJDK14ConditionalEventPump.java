/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.pumps;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import foxtrot.EventPump;
import foxtrot.Task;

/**
 * This implementation of EventPump calls the package protected method
 * <code>java.awt.EventDispatchThread.pumpEvents(Conditional)</code> and can be used with Sun JDK 1.4+ only.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.3 $
 */
public abstract class SunJDK14ConditionalEventPump implements EventPump
{
   private static final boolean debug = false;

   private static Class conditionalClass;
   private static Method pumpEventsMethod;
   protected static Class sequencedEventClass;

   static
   {
      try
      {
         AccessController.doPrivileged(new PrivilegedExceptionAction()
         {
            public Object run() throws ClassNotFoundException, NoSuchMethodException
            {
               ClassLoader loader = ClassLoader.getSystemClassLoader();
               conditionalClass = loader.loadClass("java.awt.Conditional");
               sequencedEventClass = loader.loadClass("java.awt.SequencedEvent");
               Class dispatchThreadClass = loader.loadClass("java.awt.EventDispatchThread");
               pumpEventsMethod = dispatchThreadClass.getDeclaredMethod("pumpEvents", new Class[]{conditionalClass});
               pumpEventsMethod.setAccessible(true);

               // See remarks for use of this property in java.awt.EventDispatchThread
               String property = "sun.awt.exception.handler";
               String handler = System.getProperty(property);
               if (handler == null)
               {
                  handler = ThrowableHandler.class.getName();
                  System.setProperty(property, handler);
                  if (debug) System.out.println("[SunJDK14ConditionalEventPump] Installing AWT Throwable Handler " + handler);
               }
               else
               {
                  if (debug) System.out.println("[SunJDK14ConditionalEventPump] Using already installed AWT Throwable Handler " + handler);
               }
               return null;
            }
         });
      }
      catch (Throwable x)
      {
         if (debug) x.printStackTrace();
         throw new Error(x.toString());
      }
   }

   /**
    * Implements the <code>java.awt.Conditional</code> interface, that is package private,
    * with a JDK 1.3+ dynamic proxy.
    */
   private class Conditional implements InvocationHandler
   {
      private Task task;

      /**
       * Creates a new invocation handler for the given task.
       */
      private Conditional(Task task)
      {
         this.task = task;
      }

      /**
       * Implements method <code>java.awt.Conditional.evaluate()</code>
       */
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
      {
         String name = method.getName();
         if (!"evaluate".equals(name)) throw new Error("Unknown " + conditionalClass.getName() + " method: " + name);

         // Now let's try to find a workaround for BUG #4531693 and related
         AWTEvent nextEvent = getEventQueue().peekEvent();
         if (debug) System.out.println("[SunJDK14ConditionalEventPump] Next Event: " + nextEvent);
         if (sequencedEventClass.isInstance(nextEvent))
         {
            // Next event is a SequencedEvent: we must handle them carefully
            return canPumpSequencedEvent(nextEvent);
         }
         else
         {
            return task.isCompleted() ? Boolean.FALSE : Boolean.TRUE;
         }
      }

      private EventQueue getEventQueue()
      {
         return (EventQueue)AccessController.doPrivileged(new PrivilegedAction()
         {
            public Object run()
            {
               return Toolkit.getDefaultToolkit().getSystemEventQueue();
            }
         });
      }
   }

   /**
    * Handler for RuntimeExceptions or Errors thrown during dispatching of AWT events. <br>
    * The name of this class is used as a value of the property <code>sun.awt.exception.handler</code>,
    * and the AWT event dispatch mechanism calls it when an unexpected runtime exception or error
    * is thrown during event dispatching. If the user specifies a different exception handler,
    * this one will not be used, and the user's one is used instead.
    * Use of this class is necessary in JDK 1.4, since RuntimeExceptions and Errors are propagated to
    * be handled by the ThreadGroup (but not for modal dialogs).
    */
   public static class ThrowableHandler
   {
      /**
       * The callback method invoked by the AWT event dispatch mechanism when an unexpected
       * exception or error is thrown during event dispatching. <br>
       * It just logs the exception.
       */
      public void handle(Throwable t)
      {
         System.err.println("[SunJDK14ConditionalEventPump] Exception occurred during event dispatching:");
         t.printStackTrace();
      }
   }

   public void pumpEvents(Task task)
   {
      // A null task may be passed for initialization of this class.
      if (task == null) return;

      try
      {
         if (debug) System.out.println("[SunJDK14ConditionalEventPump] Start pumping events - Pump is " + this + " - Task is " + task);

         // Invoke java.awt.EventDispatchThread.pumpEvents(new Conditional(task));
         Object conditional = Proxy.newProxyInstance(conditionalClass.getClassLoader(), new Class[]{conditionalClass}, new Conditional(task));
         pumpEventsMethod.invoke(Thread.currentThread(), new Object[]{conditional});
      }
      catch (InvocationTargetException x)
      {
         // No exceptions should escape from java.awt.EventDispatchThread.pumpEvents(Conditional)
         // since we installed a throwable handler. But one provided by the user may fail.
         Throwable t = x.getTargetException();
         System.err.println("[SunJDK14ConditionalEventPump] Exception occurred during event dispatching:");
         t.printStackTrace();

         // Rethrow. This will exit from Worker.post with a runtime exception or an error, and
         // the original event pump will take care of it.
         if (t instanceof RuntimeException)
            throw (RuntimeException)t;
         else
            throw (Error)t;
      }
      catch (Throwable x)
      {
         // Here we have an implementation bug
         System.err.println("[SunJDK14ConditionalEventPump] PANIC: uncaught exception in Foxtrot code");
         x.printStackTrace();
      }
      finally
      {
         // We're not done. Because of bug #4531693 (see Conditional) pumpEvents() may have returned
         // immediately, but the Task is not completed. Same may happen in case of buggy exception handler.
         // Here wait until the Task is completed. Side effect is freeze of the GUI.
         waitForTask(task);

         if (debug) System.out.println("[SunJDK14ConditionalEventPump] Stop pumping events - Pump is " + this + " - Task is " + task);
      }
   }

   /**
    * There are 2 cases when a SequencedEvent 'event' is pumped by Foxtrot:
    * 1) Foxtrot was NOT called by another SequencedEvent, so 'event' is the first SequencedEvent
    *    of a series of SequencedEvents.
    * 2) Foxtrot was called by another SequencedEvent, and thus 'event' is not the first
    *    SequencedEvent of a series of SequencedEvents.
    * In the first case, Foxtrot pumps 'event' regularly: if the task ends before the last
    * SequencedEvent of the series is executed, Foxtrot will return and let the AWT mechanism
    * to dispatch the remaining SequencedEvent(s), that so are dispatched in order, as they require.
    * In the second case, the event from which Worker.post() is called (that ended up in calling
    * this method) originated from a previous SequencedEvent. In this case, the SequencedEvent
    * 'event' we will pump is not the first SequencedEvent, so 'event' will wait for the previous
    * SequencedEvent to be disposed (which cannot happen until we return from this method);
    * this blocks the EventDispatchThread and hangs the Swing application (BUG #4531693 and related).
    * We return immediately, and will make the EventDispatchThread to wait until the Task is finished.
    * Side effect is, of course, freezing of the GUI, which is however far better than a hang.
    */
   protected abstract Boolean canPumpSequencedEvent(AWTEvent event);

   private void waitForTask(Task task)
   {
      try
      {
         synchronized (task)
         {
            while (!task.isCompleted())
            {
               task.wait();
            }
         }
      }
      catch (InterruptedException x)
      {
         // Someone interrupted the Event Dispatch Thread, re-interrupt
         Thread.currentThread().interrupt();
      }
   }
}
