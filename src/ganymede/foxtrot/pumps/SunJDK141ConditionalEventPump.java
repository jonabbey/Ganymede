/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.pumps;

import java.awt.AWTEvent;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * Specialized class for Sun's JDK 1.4.1
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1 $
 */
public class SunJDK141ConditionalEventPump extends SunJDK14ConditionalEventPump
{
   private static Method getFirstMethod;

   static
   {
      try
      {
         AccessController.doPrivileged(new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               getFirstMethod = sequencedEventClass.getDeclaredMethod("getFirst", new Class[0]);
               getFirstMethod.setAccessible(true);
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
	 Object first = getFirstMethod.invoke(event, ((java.lang.Object[]) null));
         if (first == event) return Boolean.TRUE;
      }
      catch (Exception ignored)
      {
      }
      return Boolean.FALSE;
   }
}
