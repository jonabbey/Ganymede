/*

   sortTest.java

   Test rig for arlut.csd.Util sort routines
   
   Created: 24 April 1997

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996 - 2004
   The University of Texas at Austin

   Contact information

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

public class sortTest implements Compare {

  static Integer ints[];

  public static void main(String[] argv)
  {
    ints = new Integer[argv.length];

    for (int i = 0; i < argv.length; i++)
      {
	ints[i] = new Integer(argv[i]);
      }
    
    QuickSort qs = new QuickSort(ints, new sortTest());
    qs.sort();

    for (int i = 0; i < argv.length; i++)
      {
	System.out.print(ints[i]);
	if (i+1 < argv.length)
	  {
	    System.out.print(" ");
	  }
	else
	  {
	    System.out.println();
	  }
      }
  }

  public sortTest()
  {
  }

  public int compare(Object a, Object b)
  {
    int i, j;

    i = ((Integer) a).intValue();
    j = ((Integer) b).intValue();

    if (i < j)
      {
	return -1;
      }
    else if (i > j)
      {
	return 1;
      }
    else
      {
	return 0;
      }
  }

}
