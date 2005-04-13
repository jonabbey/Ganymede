/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

import foxtrot.workers.SingleWorkerThread;
import foxtrot.Task;

/**
 * @version $Revision: 1.3 $
 */
public class SingleWorkerThreadTest extends FoxtrotTestCase
{
   public SingleWorkerThreadTest(String s)
   {
      super(s);
   }

   private class TestSingleWorkerThread extends SingleWorkerThread
   {
      public void stop()
      {
         super.stop();
      }
   }

   public void testStart() throws Exception
   {
      invokeTest(null, new Runnable()
      {
         public void run()
         {
            TestSingleWorkerThread worker = new TestSingleWorkerThread();
            if (worker.isAlive()) fail();
            worker.start();
            if (!worker.isAlive()) fail();
         }
      }, null);
   }

   public void testStop() throws Exception
   {
      invokeTest(null, new Runnable()
      {
         public void run()
         {
            TestSingleWorkerThread worker = new TestSingleWorkerThread();
            worker.start();
            worker.stop();
            if (worker.isAlive()) fail();
         }
      }, null);
   }

   public void testStartStopStart() throws Exception
   {
      invokeTest(null, new Runnable()
      {
         public void run()
         {
            TestSingleWorkerThread worker = new TestSingleWorkerThread();
            worker.start();
            worker.stop();
            worker.start();
            if (!worker.isAlive()) fail();
         }
      }, null);
   }

   public void testStopBeforeStart() throws Exception
   {
      invokeTest(null, new Runnable()
      {
         public void run()
         {
            TestSingleWorkerThread worker = new TestSingleWorkerThread();
            worker.stop();
         }
      }, null);
   }

   public void testStartStart() throws Exception
   {
      invokeTest(null, new Runnable()
      {
         public void run()
         {
            final MutableHolder thread = new MutableHolder(null);
            TestSingleWorkerThread worker = new TestSingleWorkerThread()
            {
               public void run()
               {
                  thread.set(Thread.currentThread());
                  super.run();
               }
            };
            worker.start();
            sleep(500);
            Thread foxtrot = (Thread)thread.get();
            worker.start();
            sleep(500);
            if (foxtrot != thread.get()) fail();
         }
      }, null);
   }

   public void testNoStartAllowsPost() throws Exception
   {
      final TestSingleWorkerThread worker = new TestSingleWorkerThread();
      invokeTest(worker, new Runnable()
      {
         public void run()
         {
            final MutableInteger pass = new MutableInteger(0);
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  pass.set(1);
                  return null;
               }
            });
            sleep(500);
            if (pass.get() != 1) fail();
         }
      }, null);
   }

   public void testPost() throws Exception
   {
      final TestSingleWorkerThread worker = new TestSingleWorkerThread();
      invokeTest(worker, new Runnable()
      {
         public void run()
         {
            worker.start();
            final MutableInteger pass = new MutableInteger(0);
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  pass.set(1);
                  return null;
               }
            });
            sleep(500);
            if (pass.get() != 1) fail();
         }
      }, null);
   }

   public void testManyPosts() throws Exception
   {
      final TestSingleWorkerThread worker = new TestSingleWorkerThread();
      invokeTest(worker, new Runnable()
      {
         public void run()
         {
            worker.start();
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  sleep(500);
                  return null;
               }
            });
            final MutableInteger pass = new MutableInteger(0);
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  pass.set(pass.get() + 1);
                  return null;
               }
            });
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  pass.set(pass.get() + 1);
                  return null;
               }
            });
            sleep(1000);
            if (pass.get() != 2) fail();
         }
      }, null);
   }

   public void testTaskIsExecutedAfterIgnoredInterruptInTask() throws Exception
   {
      final TestSingleWorkerThread worker = new TestSingleWorkerThread();
      invokeTest(worker, new Runnable()
      {
         public void run()
         {
            final MutableHolder thread = new MutableHolder(null);
            final MutableInteger pass = new MutableInteger(0);
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  thread.set(Thread.currentThread());

                  try
                  {
                     Thread.sleep(1000);
                  }
                  catch (InterruptedException x)
                  {
                     // Swallow the exception and don't restore the interrupted status of the thread
                     // Be sure we pass here
                     pass.set(1);
                  }

                  return null;
               }
            });
            sleep(500);

            // Interrupt the WorkerThread.
            // The task will not restore the status of the thread, thereby allowing it to continue
            Thread foxtrot = (Thread)thread.get();
            foxtrot.interrupt();
            sleep(1000);
            if (pass.get() != 1) fail();

            // Be sure another Task can be posted and executed on the same worker thread as above
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  thread.set(Thread.currentThread());
                  pass.set(2);
                  return null;
               }
            });
            sleep(500);
            if (thread.get() != foxtrot) fail();
            if (pass.get() != 2) fail();
         }
      }, null);
   }

   public void testTaskIsExecutedAfterNotIgnoredInterruptInTask() throws Exception
   {
      final TestSingleWorkerThread worker = new TestSingleWorkerThread();
      invokeTest(worker, new Runnable()
      {
         public void run()
         {
            final MutableHolder thread = new MutableHolder(null);
            final MutableInteger pass = new MutableInteger(0);
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  thread.set(Thread.currentThread());

                  try
                  {
                     Thread.sleep(1000);
                  }
                  catch (InterruptedException x)
                  {
                     // Restore the interrupted status of the thread
                     Thread.currentThread().interrupt();
                     // Be sure we pass here
                     pass.set(1);
                  }

                  return null;
               }
            });
            sleep(500);

            // Interrupt the WorkerThread.
            // The task will not restore the status of the thread, thereby allowing it to continue
            Thread foxtrot = (Thread)thread.get();
            foxtrot.interrupt();
            sleep(1000);
            if (pass.get() != 1) fail();

            // Be sure another Task can be posted and executed on a different worker thread
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  thread.set(Thread.currentThread());
                  pass.set(2);
                  return null;
               }
            });
            sleep(500);
            if (thread.get() == foxtrot) fail();
            if (pass.get() != 2) fail();
         }
      }, null);
   }

   public void testTaskIsExecutedAfterInterruptInTask() throws Exception
   {
      final TestSingleWorkerThread worker = new TestSingleWorkerThread();
      invokeTest(worker, new Runnable()
      {
         public void run()
         {
            final MutableHolder thread = new MutableHolder(null);
            final MutableInteger pass = new MutableInteger(0);
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  thread.set(Thread.currentThread());

                  // Allow InterruptedException to unwind the stack frames
                  Thread.sleep(1000);

                  return null;
               }
            });
            sleep(500);

            Thread foxtrot = (Thread)thread.get();
            foxtrot.interrupt();
            sleep(1000);

            // Be sure another Task can be posted and executed on a different worker thread
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  thread.set(Thread.currentThread());
                  pass.set(1);
                  return null;
               }
            });
            sleep(500);
            if (thread.get() == foxtrot) fail();
            if (pass.get() != 1) fail();
         }
      }, null);
   }

   public void testPendingTasksAreExecutedAfterRestart() throws Exception
   {
      final TestSingleWorkerThread worker = new TestSingleWorkerThread();
      invokeTest(worker, new Runnable()
      {
         public void run()
         {
            worker.start();
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  Thread.sleep(500);
                  return null;
               }
            });
            final MutableInteger pass = new MutableInteger(0);
            worker.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  pass.set(1);
                  return null;
               }
            });
            sleep(250);
            worker.stop();
            sleep(1000);
            // Be sure 2nd Task not yet executed
            if (pass.get() != 0) fail();
            worker.start();
            sleep(500);
            if (pass.get() != 1) fail();
         }
      }, null);
   }
}
