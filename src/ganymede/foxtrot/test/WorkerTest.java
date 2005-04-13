/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import foxtrot.Job;
import foxtrot.Task;
import foxtrot.Worker;

/**
 * @version $Revision: 1.4 $
 */
public class WorkerTest extends FoxtrotTestCase
{
   public WorkerTest(String s)
   {
      super(s);
   }

   public void testThreads() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            Worker.post(new Job()
            {
               public Object run()
               {
                  // Check that I'm NOT in the AWT Event Dispatch Thread
                  if (SwingUtilities.isEventDispatchThread()) fail("Must not be in the Event Dispatch Thread");

                  // Check that I'm really in the Foxtrot Worker Thread
                  if (Thread.currentThread().getName().indexOf("Foxtrot") < 0) fail("Must be in the Foxtrot Worker Thread");

                  return null;
               }
            });
         }
      }, null);
   }

   public void testBlocking() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            final long sleep = 1000;

            long start = System.currentTimeMillis();
            Worker.post(new Job()
            {
               public Object run()
               {
                  sleep(sleep);
                  return null;
               }
            });
            long end = System.currentTimeMillis();

            long elapsed = end - start;
            System.out.println("Sleep time is: " + sleep + ", Worker.post() blocked for " + elapsed);

            if (elapsed < sleep) fail("Worker.post() does not block");
         }
      }, null);
   }

   public void testDequeuing() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            final MutableInteger check = new MutableInteger(0);
            final long sleep = 1000;

            // This event will be dequeued only after Worker.post()
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  check.set(1);
               }
            });

            sleep(2 * sleep);

            // Check that the value is still the original one
            if (check.get() != 0) fail();

            Worker.post(new Job()
            {
               public Object run()
               {
                  sleep(sleep);
                  return null;
               }
            });

            // Check that the event posted with invokeLater has been dequeued
            if (check.get() != 1) fail("Event has not been dequeued");
         }
      }, null);
   }

   public void testTaskException() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            try
            {
               Worker.post(new Task()
               {
                  public Object run() throws NumberFormatException
                  {
                     return new NumberFormatException();
                  }
               });
            }
            catch (NumberFormatException ignored)
            {
            }
            catch (Throwable x)
            {
               fail();
            }
         }
      }, null);
   }

   public void testTaskError() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            try
            {
               Worker.post(new Job()
               {
                  public Object run()
                  {
                     return new NoClassDefFoundError();
                  }
               });
            }
            catch (NoClassDefFoundError ignored)
            {
            }
            catch (Throwable x)
            {
               fail();
            }
         }
      }, null);
   }

   public void testAWTException() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
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

            final long sleep = 1000;
            long start = System.currentTimeMillis();
            Worker.post(new Job()
            {
               public Object run()
               {
                  sleep(sleep);
                  return null;
               }
            });
            long end = System.currentTimeMillis();

            // Must check that really elapsed all the time
            long elapsed = end - start;
            if (elapsed < sleep) fail("Worker.post() does not block in case of AWT exception: expected " + sleep + ", waited " + elapsed);
         }
      }, null);
   }

   public void testAWTError() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
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

            final long sleep = 1000;
            long start = System.currentTimeMillis();
            Worker.post(new Job()
            {
               public Object run()
               {
                  sleep(sleep);
                  return null;
               }
            });
            long end = System.currentTimeMillis();

            // Must check that really elapsed all the time
            long elapsed = end - start;
            if (elapsed < sleep) fail("Worker.post() does not block in case of AWT error: expected " + sleep + ", waited " + elapsed);
         }
      }, null);
   }

   public void testPostFromTask() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            final MutableInteger counter = new MutableInteger(0);

            Worker.post(new Job()
            {
               public Object run()
               {
                  counter.set(counter.get() + 1);

                  // Nested Worker.post()
                  Worker.post(new Job()
                  {
                     public Object run()
                     {
                        if (counter.get() != 1) fail();

                        counter.set(counter.get() + 1);
                        return null;
                     }
                  });

                  if (counter.get() != 2) fail("Nested Task is not executed immediately");

                  counter.set(counter.get() + 1);

                  return null;
               }
            });

            if (counter.get() != 3) fail();
         }
      }, null);
   }

   public void testTaskReuse() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            final MutableInteger count = new MutableInteger(0);

            Job job = new Job()
            {
               public Object run()
               {
                  count.set(count.get() + 1);
                  return null;
               }
            };

            int times = 2;
            for (int i = 0; i < times; ++i)
            {
               Worker.post(job);
            }

            if (count.get() != times) fail("Task is not reused");
         }
      }, null);
   }

   public void testPostFromInvokeLater() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            int max = 5;
            MutableInteger counter = new MutableInteger(0);

            long start = System.currentTimeMillis();

            postFromInvokeLater(counter, max);

            long end = System.currentTimeMillis();

            // We used the default WorkerThread, be sure task times were summed
            long sum = 0;
            for (int i = 0; i < max; ++i) sum += i + 1;
            sum *= 1000;

            long epsilon = 100;
            long elapsed = end - start;
            if (elapsed > sum + epsilon) fail("Elapsed time is: " + elapsed + ", expected time is: " + sum);
            if (elapsed < sum - epsilon) fail("Elapsed time is: " + elapsed + ", expected time is: " + sum);
         }
      }, null);
   }

   private void postFromInvokeLater(final MutableInteger counter, final int maxDeep)
   {
      final int deep = counter.get() + 1;

      Job job = new Job()
      {
         public Object run()
         {
            // Here I recurse on calling Worker.post(), that is: I am in event0, that calls
            // Worker.post(task1) that dequeues event1 that calls Worker.post(task2) that dequeues event2
            // that calls Worker.post(task3) and so on.
            // Since Worker.post() calls are synchronous, the Worker.post(task1) call returns
            // only when the task1 is finished AND event1 is finished; but event1 is finished
            // only when Worker.post(task2) returns; Worker.post(task2) returns only when task2
            // is finished AND event2 is finished; but event2 is finished only when Worker.post(task3)
            // returns; and so on.
            // The total execution time is dependent on the implementation of the WorkerThread:
            // if it enqueues tasks (like the default implementation) we have (roughly) that:
            // time(task1) = time(task3) + time(task2)
            // even if to execute only task1 taskes a very short time.
            // If the worker implementation uses parallel threads to execute tasks, then (roughly):
            // time(task1) = max(time(task3), time(task2)).
            // In general, it is a bad idea to use Foxtrot this way: you probably need an asynchronous
            // solution.
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  counter.set(deep);
                  if (deep < maxDeep) postFromInvokeLater(counter, maxDeep);
               }
            });

            sleep(1000 * deep);

            return null;
         }
      };

      // job1 sleeps 1 s, but Worker.post(job1) returns after event1 is finished.
      // event1 runs Worker.post(job2); job2 sleeps 2 s, but Worker.post(job2) returns after event2 is finished.
      // event2 runs Worker.post(job3); job3 sleeps 3 s, but Worker.post(job3) returns after event3 is finished.
      // event3 runs Worker.post(job4); job4 sleeps 4 s, but Worker.post(job4) returns after event4 is finished.
      // event4 runs Worker.post(job5); job5 sleeps 5 s.
      // Worker.post(job1) returns after 5+4+3+2+1 s since the default implementation enqueues tasks.
      Worker.post(job);
   }

   public void testTaskQueueing() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            final int count = 10;
            final MutableInteger counter = new MutableInteger(0);

            // From Worker.post() I post some event on the Event Queue using invokeLater.
            // The events posted are dequeued and they call again Worker.post(),
            // that is not yet returned, so that Tasks are queued in the worker thread,
            // and not yet executed. When the first Worker.post() returns, the enqueued
            // Tasks get a chance to be executed.
            Worker.post(new Job()
            {
               public Object run()
               {
                  for (int i = 0; i < 10; ++i)
                  {
                     SwingUtilities.invokeLater(new Runnable()
                     {
                        public void run()
                        {
                           Worker.post(new Job()
                           {
                              public Object run()
                              {
                                 counter.set(counter.get() + 1);
                                 return null;
                              }
                           });
                        }
                     });
                  }

                  // Wait for all tasks to be queued in the worker thread.
                  sleep(1000);

                  return null;
               }
            });

            // Wait for all the enqueued Tasks in the worker thread to be executed.
            sleep(1000);

            assertEquals(count, counter.get());
         }
      }, null);
   }

   public void testPerformance() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            JButton button = new JButton();
            int count = 100;
            final long sleep = 100;

            ActionListener listener = new ActionListener()
            {
               public void actionPerformed(ActionEvent e)
               {
                  Worker.post(new Job()
                  {
                     public Object run()
                     {
                        sleep(sleep);
                        return null;
                     }
                  });
               }
            };
            button.addActionListener(listener);

            long start = System.currentTimeMillis();
            for (int i = 0; i < count; ++i) button.doClick();
            long end = System.currentTimeMillis();
            long workerElapsed = end - start;
            System.out.println("Worker.post(Job) performance: " + count + " calls in " + workerElapsed + " ms");

            button.removeActionListener(listener);

            listener = new ActionListener()
            {
               public void actionPerformed(ActionEvent e)
               {
                  sleep(sleep);
               }
            };
            button.addActionListener(listener);

            start = System.currentTimeMillis();
            for (int i = 0; i < count; ++i)
            {
               button.doClick();
            }
            end = System.currentTimeMillis();
            long plainElapsed = end - start;
            System.out.println("Plain Listener performance: " + count + " calls in " + plainElapsed + " ms");

            int perthousand = 1;
            if ((workerElapsed - plainElapsed) * 1000 > plainElapsed * perthousand) fail();
         }
      }, null);
   }

   public void testPumpSequencedEvents() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            final JDialog dialog = new JDialog((JFrame)null, true);

            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  dialog.setVisible(false);
               }
            });

            dialog.setVisible(true);

            final MutableInteger pumped = new MutableInteger(0);
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  pumped.set(pumped.get() + 1);
               }
            });

            Worker.post(new Job()
            {
               public Object run()
               {
                  sleep(1000);
                  return null;
               }
            });

            // Verify that the event has been pumped
            if (pumped.get() != 1) fail();
         }
      }, null);
   }

   public void testMemoryLeaks() throws Exception
   {
      invokeTest(Worker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            ArrayList list = new ArrayList();

            int times = 1024;
            for (int i = 0; i < times; ++i)
            {
               try
               {
                  Job job = new FatJob();
                  list.add(job);
                  Worker.post(job);
               }
               catch (OutOfMemoryError x)
               {
                  list.clear();
                  break;
               }
            }

            // Try again, without mantaining jobs alive
            int j = 0;
            for (; j < times; ++j)
            {
               Job job = new FatJob();
               Worker.post(job);
            }

            if (j < times) fail();
         }
      }, null);
   }

   private static class FatJob extends Job
   {
      // A heavy data member to explode the heap
      private byte[] fatty = new byte[1024 * 1024];

      public Object run()
      {
         Arrays.fill(fatty, (byte)31);
         return null;
      }
   }
}
