/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

/**
 * @version $Revision: 1.2 $
 */
public class MutableHolder
{
   private Object held;

   public MutableHolder(Object held)
   {
      this.held = held;
   }

   public Object get()
   {
      return held;
   }

   public void set(Object held)
   {
      this.held = held;
   }

   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (!(o instanceof MutableHolder)) return false;

      final MutableHolder holder = (MutableHolder)o;

      if (held != null ? !held.equals(holder.held) : holder.held != null) return false;

      return true;
   }

   public int hashCode()
   {
      return (held != null ? held.hashCode() : 0);
   }
}
