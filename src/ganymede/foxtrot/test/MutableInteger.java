/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

/**
 * @version $Revision: 1.3 $
 */
public class MutableInteger implements Comparable
{
   private int value;

   public MutableInteger(int value)
   {
      this.value = value;
   }

   public int get()
   {
      return value;
   }

   public int hashCode()
   {
      return value;
   }

   public boolean equals(Object obj)
   {
      if (obj == null) return false;
      if (obj == this) return true;
      try
      {
         return get() == ((MutableInteger)obj).get();
      }
      catch (ClassCastException x)
      {
      }
      return false;
   }

   public int compareTo(Object obj)
   {
      if (obj == null) return 1;
      if (obj == this) return 0;

      int thisValue = get();
      int otherValue = ((MutableInteger)obj).get();
      if (thisValue > otherValue) return 1;
      if (thisValue < otherValue) return -1;
      return 0;
   }

   public void set(int value)
   {
      this.value = value;
   }

   public String toString()
   {
      return String.valueOf(get());
   }
}
