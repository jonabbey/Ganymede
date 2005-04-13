/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

import java.awt.AWTEvent;

import javax.swing.SwingUtilities;

import foxtrot.EventPump;
import foxtrot.Job;
import foxtrot.Worker;
import foxtrot.pumps.EventFilter;
import foxtrot.pumps.EventFilterable;

/**
 * @version $Revision: 1.5 $
 */
public class EventFilterableTest extends FoxtrotTestCase
{
   public EventFilterableTest(String s)
   {
      super(s);
   }

   public void testEventNotFiltered() throws Exception
   {
       invokeTest(Worker.getWorkerThread(), new Runnable()
       {
          public void run()
          {
             EventPump pump = Worker.getEventPump();

             final MutableHolder check = new MutableHolder(null);
             EventFilterable filterable = null;
             EventFilter oldFilter = null;
             try
             {
                SwingUtilities.invokeLater(new Runnable()
                {
                   public void run()
                   {
                      if (check.get() == Boolean.FALSE) check.set(Boolean.TRUE);
                   }
                });

                if (pump instanceof EventFilterable)
                {
                   filterable = (EventFilterable)pump;
                   oldFilter = filterable.getEventFilter();
                   filterable.setEventFilter(new EventFilter()
                   {
                      public boolean accept(AWTEvent event)
                      {
                         if (check.get() == null) check.set(Boolean.FALSE);
                         return true;
                      }
                   });
                }

                Worker.post(new Job()
                {
                   public Object run()
                   {
                      sleep(5000);
                      return null;
                   }
                });
             }
             finally
             {
                if (filterable != null) filterable.setEventFilter(oldFilter);
             }

             // Ensure that we've passed from the filter and dispatched the event
             if (check.get() != Boolean.TRUE) fail();

             check.set(null);

             // Be sure that after everything is again ok
             Worker.post(new Job()
             {
                public Object run()
                {
                   sleep(5000);
                   return null;
                }
             });

             // Should not have been called again
             if (check.get() != null) fail();
          }
       }, null);
   }

   public void testEventFiltered() throws Exception
   {
       invokeTest(Worker.getWorkerThread(), new Runnable()
       {
          public void run()
          {
             EventPump pump = Worker.getEventPump();

             final MutableHolder check = new MutableHolder(null);
             EventFilterable filterable = null;
             EventFilter oldFilter = null;
             try
             {
                SwingUtilities.invokeLater(new Runnable()
                {
                   public void run()
                   {
                      if (check.get() == Boolean.FALSE) check.set(Boolean.TRUE);
                   }
                });

                if (pump instanceof EventFilterable)
                {
                   filterable = (EventFilterable)pump;
                   oldFilter = filterable.getEventFilter();
                   filterable.setEventFilter(new EventFilter()
                   {
                      public boolean accept(AWTEvent event)
                      {
                         boolean result = check.get() != null;
                         check.set(Boolean.FALSE);
                         return result;
                      }
                   });
                }

                Worker.post(new Job()
                {
                   public Object run()
                   {
                      sleep(5000);
                      return null;
                   }
                });
             }
             finally
             {
                if (filterable != null) filterable.setEventFilter(oldFilter);
             }

             // Ensure that we've passed from the filter and not dispatched the event
             if (check.get() != Boolean.FALSE) fail();

             check.set(null);

             // Be sure that after everything is again ok
             Worker.post(new Job()
             {
                public Object run()
                {
                   sleep(5000);
                   return null;
                }
             });

             // Should not have been called again
             if (check.get() != null) fail();
          }
       }, null);
   }
}
