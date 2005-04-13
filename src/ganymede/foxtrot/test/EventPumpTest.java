/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

import java.lang.reflect.Method;

import javax.swing.SwingUtilities;

import foxtrot.EventPump;
import foxtrot.Task;
import foxtrot.pumps.QueueEventPump;
import foxtrot.pumps.SunJDK14ConditionalEventPump;

/**
 * @version $Revision: 1.6 $
 */
public class EventPumpTest extends FoxtrotTestCase
{
   public EventPumpTest(String s)
   {
      super(s);
   }

   public void testJDK13QueueEventPump() throws Exception
   {
      if (!isJRE14())
      {
         QueueEventPump pump = new QueueEventPump();
         testEventPump(pump);
      }
   }

   public void testSunJDK14ConditionalEventPump() throws Exception
   {
      if (isJRE14())
      {
         SunJDK14ConditionalEventPump pump = new SunJDK14ConditionalEventPump();
         testEventPump(pump);
      }
   }

   private void testEventPump(EventPump pump) throws Exception
   {
      testPumpEventsBlocks(pump);
      testPumpEventsDequeues(pump);
      tesPumpEventsOnThrowException(pump);
      tesPumpEventsOnThrowError(pump);
   }

   /**
    * Verifies that EventPump.pumpEvents(Task) blocks until the Task is completed
    */
   private void testPumpEventsBlocks(final EventPump pump) throws Exception
   {
      invokeTest(null, new Runnable()
      {
         public void run()
         {
            final Task task = new Task()
            {
               public Object run() throws Exception
               {
                  return null;
               }
            };

            final long delay = 5000;

            // I enqueue another event to stop the task
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  sleep(delay);
                  setTaskCompleted(task);
               }
            });

            // Now I start the event pump, the events above must be dequeued.
            long start = System.currentTimeMillis();
            pump.pumpEvents(task);
            long stop = System.currentTimeMillis();

            long elapsed = stop - start;
            if (elapsed <= delay) fail("Blocking is not effective: expecting " + delay + ", blocked for only " + elapsed);
         }
      }, null);
   }

   /**
    * Verifies that AWT events are dequeued by EventPump.pumpEvents(Task)
    */
   private void testPumpEventsDequeues(final EventPump pump) throws Exception
   {
      invokeTest(null, new Runnable()
      {
         public void run()
         {
            final MutableInteger count = new MutableInteger(0);
            final int value = 13;

            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  count.set(value);
               }
            });

            final Task task = new Task()
            {
               public Object run() throws Exception
               {
                  return null;
               }
            };

            // I enqueue another event to stop the task
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  setTaskCompleted(task);
               }
            });

            // Now I start the event pump, the events above must be dequeued.
            pump.pumpEvents(task);

            if (count.get() != value) fail("Event pump does not dequeue events");
         }
      }, null);
   }

   /**
    * Verifies that EventPump.pumpEvents(Task) does not return in case of runtime exceptions
    */
   private void tesPumpEventsOnThrowException(final EventPump pump) throws Exception
   {
      invokeTest(null, new Runnable()
      {
         public void run()
         {
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  throw new RuntimeException();
               }
            });

            final Task task = new Task()
            {
               public Object run() throws Exception
               {
                  return null;
               }
            };

            final long delay = 3000;

            // I enqueue another event to stop the task
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  sleep(delay);
                  setTaskCompleted(task);
               }
            });

            try
            {
               // Now I start the event pump, the events above must be dequeued.
               long start = System.currentTimeMillis();
               pump.pumpEvents(task);
               long stop = System.currentTimeMillis();
               long elapsed = stop - start;
               if (elapsed <= delay) fail("Blocking is not effective when events throw exceptions: expecting " + delay + ", blocked for only " + elapsed);
            }
            catch (RuntimeException x)
            {
               fail("Event pump must not throw runtime exceptions thrown by events");
            }
         }
      }, null);
   }

   /**
    * Verifies that EventPump.pumpEvents(Task) does not return in case of errors
    */
   private void tesPumpEventsOnThrowError(final EventPump pump) throws Exception
   {
      invokeTest(null, new Runnable()
      {
         public void run()
         {
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  throw new Error();
               }
            });

            final Task task = new Task()
            {
               public Object run() throws Exception
               {
                  return null;
               }
            };

            final long delay = 3000;

            // I enqueue another event to stop the task
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  sleep(delay);
                  setTaskCompleted(task);
               }
            });

            try
            {
               // Now I start the event pump, the events above must be dequeued.
               long start = System.currentTimeMillis();
               pump.pumpEvents(task);
               long stop = System.currentTimeMillis();
               long elapsed = stop - start;
               if (elapsed <= delay) fail("Blocking is not effective when events throw errors: expecting " + delay + ", blocked for only " + elapsed);
            }
            catch (Error x)
            {
               fail("Event pump must not throw errors thrown by events");
            }
         }
      }, null);
   }

   private void setTaskCompleted(Task task)
   {
      try
      {
         Method completed = Task.class.getDeclaredMethod("setCompleted", new Class[] {boolean.class});
         completed.setAccessible(true);
         completed.invoke(task, new Object[]{Boolean.TRUE});

         // In case no other events are posted to the event queue, here we wake it up,
         // see AbstractWorkerThread.runTask (in the finally block)
         SwingUtilities.invokeLater(new Runnable()
         {
            public void run()
            {
            }
         });
      }
      catch (Throwable x)
      {
         x.printStackTrace();
         fail();
      }
   }
}
