/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

import foxtrot.workers.MultiWorkerThread;
import foxtrot.Job;

/**
 * @version $Revision: 1.2 $
 */
public class MultiWorkerThreadTest extends FoxtrotTestCase
{
   public MultiWorkerThreadTest(String s)
   {
      super(s);
   }

   public void testThreads() throws Exception
   {
      final MutableHolder thread = new MutableHolder(null);
      final MultiWorkerThread worker = new MultiWorkerThread()
      {
         public void run()
         {
            thread.set(Thread.currentThread());
            super.run();
         }
      };
      invokeTest(worker, new Runnable()
      {
         public void run()
         {
            worker.start();

            final MutableHolder runner = new MutableHolder(null);
            worker.postTask(new Job()
            {
               public Object run()
               {
                  runner.set(Thread.currentThread());
                  return null;
               }
            });

            sleep(1000);

            if (thread.get() == runner.get()) fail();
         }
      }, null);
   }

   public void testLongBeforeShort() throws Exception
   {
      final MultiWorkerThread worker = new MultiWorkerThread();
      invokeTest(worker, new Runnable()
      {
         public void run()
         {
            worker.start();

            // A long Task followed by a short one.
            final long longDelay = 5000;
            final MutableInteger longer = new MutableInteger(0);
            worker.postTask(new Job()
            {
               public Object run()
               {
                  longer.set(1);
                  sleep(longDelay);
                  longer.set(2);
                  return null;
               }
            });
            final long shortDelay = 2000;
            final MutableInteger shorter = new MutableInteger(0);
            worker.postTask(new Job()
            {
               public Object run()
               {
                  shorter.set(1);
                  sleep(shortDelay);
                  shorter.set(2);
                  return null;
               }
            });

            sleep(shortDelay / 2);
            if (shorter.get() != 1) fail();
            if (longer.get() != 1) fail();

            sleep(shortDelay);
            if (shorter.get() != 2) fail();
            if (longer.get() != 1) fail();

            sleep(longDelay);
            if (longer.get() != 2) fail();
         }
      }, null);
   }
}
