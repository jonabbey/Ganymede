/*

   ArrayUtils.java

   Convenience methods for working with Arrays
   
   Created: 2 February 2008

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Directory Management System
 
   Copyright (C) 1996 - 2008
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.Util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      ArrayUtils

------------------------------------------------------------------------------*/

/**
 * Convenience methods for working with Arrays
 */

public class ArrayUtils {

  public static Object[] concat(Object[] ary1, Object[] ary2)
  {
    Object[] results = new Object[ary1.length + ary2.length];

    System.arraycopy(ary1, 0, results, 0, ary1.length);
    System.arraycopy(ary2, 0, results, results.length, ary2.length);
  }

  public static Object[] concat(Object[] ary1, Object[] ary2, Object[] ary3)
  {
    Object[] results = new Object[ary1.length + ary2.length + ary3.length];

    System.arraycopy(ary1, 0, results, 0, ary1.length);
    System.arraycopy(ary2, 0, results, results.length, ary2.length);
    System.arraycopy(ary3, 0, results, results.length, ary3.length);
  }

  public static Object[] concat(Object[] ary1, Object[] ary2, Object[] ary3, Object[] ary4)
  {
    Object[] results = new Object[ary1.length + ary2.length + ary3.length + ary4.length];

    System.arraycopy(ary1, 0, results, 0, ary1.length);
    System.arraycopy(ary2, 0, results, results.length, ary2.length);
    System.arraycopy(ary3, 0, results, results.length, ary3.length);
    System.arraycopy(ary4, 0, results, results.length, ary4.length);
  }
}
