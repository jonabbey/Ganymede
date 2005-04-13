/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

import javax.swing.SwingUtilities;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;
import foxtrot.Task;
import foxtrot.WorkerThread;

/**
 * @version $Revision: 1.2 $
 */
public class AsyncWorkerTest extends FoxtrotTestCase
{
   public AsyncWorkerTest(String s)
   {
      super(s);
   }

   public void testPostAndForget() throws Exception
   {
      final MutableHolder result = new MutableHolder(null);
      final WorkerThread workerThread = AsyncWorker.getWorkerThread();
      invokeTest(workerThread, new Runnable()
      {
         public void run()
         {
            // This avoids to use AsyncTask and implement finish()
            workerThread.postTask(new Task()
            {
               public Object run() throws Exception
               {
                  sleep(1000);
                  result.set(Boolean.TRUE);
                  return null;
               }
            });

            sleep(500);
         }
      }, new Runnable()
      {
         public void run()
         {
            if (result.get() != Boolean.TRUE) fail("AsyncTask is not executed");
         }
      });
   }

   public void testUsage() throws Exception
   {
      final MutableHolder result = new MutableHolder(null);
      invokeTest(AsyncWorker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            AsyncWorker.post(new AsyncTask()
            {
               private static final String VALUE = "1000";

               public Object run() throws Exception
               {
                  Thread.sleep(1000);
                  return VALUE;
               }

               public void finish()
               {
                  try
                  {
                     String value = (String)getResultOrThrow();
                     if (!VALUE.equals(value)) result.set("AsyncTask.run() does not return the result");
                  }
                  catch (Exception x)
                  {
                     result.set(x.toString());
                  }
               }
            });

            sleep(500);
         }
      }, new Runnable()
      {
         public void run()
         {
            if (result.get() != null) fail((String)result.get());
         }
      });
   }

   public void testThreads() throws Exception
   {
      final MutableHolder result = new MutableHolder(null);
      invokeTest(AsyncWorker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            AsyncWorker.post(new AsyncTask()
            {
               public Object run() throws Exception
               {
                  // Check that I'm NOT in the AWT Event Dispatch Thread
                  if (SwingUtilities.isEventDispatchThread())
                  {
                     return "Must not be in the Event Dispatch Thread";
                  }
                  else
                  {
                     // Check that I'm really in the Foxtrot Worker Thread
                     if (Thread.currentThread().getName().indexOf("Foxtrot") < 0) return "Must be in the Foxtrot Worker Thread";
                  }
                  return null;
               }

               public void finish()
               {
                  try
                  {
                     String failure = (String)getResultOrThrow();
                     if (failure == null)
                     {
                        // Check that I'm in the AWT Event Dispatch Thread
                        if (!SwingUtilities.isEventDispatchThread()) result.set("Must be in the Event Dispatch Thread");
                     }
                     else
                     {
                        result.set(failure);
                     }
                  }
                  catch (Exception x)
                  {
                     result.set(x.toString());
                  }
               }
            });

            sleep(500);
         }
      }, new Runnable()
      {
         public void run()
         {
            if (result.get() != null) fail((String)result.get());
         }
      });
   }

   public void testTaskException() throws Exception
   {
      final MutableHolder result = new MutableHolder(null);
      invokeTest(AsyncWorker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            final IndexOutOfBoundsException ex = new IndexOutOfBoundsException();
            AsyncWorker.post(new AsyncTask()
            {
               public Object run() throws Exception
               {
                  throw ex;
               }

               public void finish()
               {
                  try
                  {
                     getResultOrThrow();
                     result.set("Expected exception");
                  }
                  catch (IndexOutOfBoundsException x)
                  {
                     if (x != ex) result.set("Expected same exception");
                  }
                  catch (Exception x)
                  {
                     result.set("Did not expect checked exception");
                  }
               }
            });

            sleep(500);
         }
      }, new Runnable()
      {
         public void run()
         {
            if (result.get() != null) fail((String)result.get());
         }
      });
   }

   public void testTaskError() throws Exception
   {
      final MutableHolder result = new MutableHolder(null);
      invokeTest(AsyncWorker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            final Error ex = new Error();
            AsyncWorker.post(new AsyncTask()
            {
               public Object run() throws Exception
               {
                  throw ex;
               }

               public void finish()
               {
                  try
                  {
                     getResultOrThrow();
                     result.set("Expected error");
                  }
                  catch (Error x)
                  {
                     if (x != ex) result.set("Expected same error");
                  }
                  catch (Exception x)
                  {
                     result.set("Did not expect exception");
                  }
               }
            });

            sleep(500);
         }
      }, new Runnable()
      {
         public void run()
         {
            if (result.get() != null) fail((String)result.get());
         }
      });
   }

   public void testPostFromTask() throws Exception
   {
      final MutableHolder result = new MutableHolder(null);
      invokeTest(AsyncWorker.getWorkerThread(), new Runnable()
      {
         public void run()
         {
            AsyncWorker.post(new AsyncTask()
            {
               public Object run()
               {
                  // Nested AsyncWorker.post(): this is invalid as
                  // it has the same effect of 2 consecutive AsyncWorker.post()
                  // calls, so there is no need to nest
                  AsyncWorker.post(new AsyncTask()
                  {
                     public Object run()
                     {
                        return null;
                     }

                     public void finish()
                     {
                     }
                  });
                  return null;
               }

               public void finish()
               {
                  try
                  {
                     getResultOrThrow();
                     result.set("Expected exception");
                  }
                  catch (IllegalStateException x)
                  {
                  }
                  catch (Exception x)
                  {
                     result.set("Did not expect exception");
                  }
               }
            });
         }
      }, new Runnable()
      {
         public void run()
         {
            if (result.get() != null) fail((String)result.get());
         }
      });
   }
}
