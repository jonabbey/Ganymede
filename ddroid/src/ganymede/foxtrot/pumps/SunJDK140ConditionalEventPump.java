/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.pumps;

import java.awt.AWTEvent;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.LinkedList;

/**
 * Specialized class for Sun's JDK 1.4.0
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1 $
 */
public class SunJDK140ConditionalEventPump extends SunJDK14ConditionalEventPump
{
   private static Field listField;

   static
   {
      try
      {
         AccessController.doPrivileged(new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               listField = sequencedEventClass.getDeclaredField("list");
               listField.setAccessible(true);
               return null;
            }
         });
      }
      catch (Throwable x)
      {
         throw new Error(x.toString());
      }
   }

   protected Boolean canPumpSequencedEvent(AWTEvent event)
   {
      try
      {
         LinkedList list = (LinkedList)listField.get(event);
         // In JDK 1.4.1 SequencedEvent.getFirst() is static synchronized, which is
         // a small bug: it will synchronized on an eventual subclass, while elsewhere
         // in SequencedEvent there is explicit synchronization on SequencedEvent.class.
         // Here I keep consistency with JDK 1.4.1, will see in 1.4.2 what happens.
         synchronized (sequencedEventClass)
         {
            if (list.getFirst() == event) return Boolean.TRUE;
         }
      }
      catch (Exception x)
      {
      }
      return Boolean.FALSE;
   }
}
