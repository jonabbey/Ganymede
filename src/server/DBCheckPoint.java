/*
   GASH 2

   DBCheckPoint.java

   The GANYMEDE object storage system.

   Created: 15 January 1999
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 1999/11/16 08:00:56 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBCheckPoint

------------------------------------------------------------------------------*/

/**
 * <p>DBCheckPoint is a class designed to allow server-side code that
 * needs to attempt a multi-step operation that might not successfully
 * complete to be able to undo all changes made without having to
 * abort the entire transaction.</p>
 * 
 * <p>In other words, a DBCheckPoint is basically a transaction within 
 * a transaction.</p>
 */

class DBCheckPoint {

  static final boolean debug = false;

  // ---

  String 
    name;

  Vector
    objects = null,
    logEvents = null;

  Hashtable noDeleteLocks;

  /* -- */

  DBCheckPoint(String name, DBEditSet transaction)
  {
    DBEditObject obj;

    /* -- */

    this.name = name;

    // assume that log events are not going to change once recorded,
    // so we can make do with a shallow copy.

    logEvents = (Vector) transaction.logEvents.clone();

    objects = new Vector();

    for (int i = 0; i < transaction.objects.size(); i++)
      {
	obj = (DBEditObject) transaction.objects.elementAt(i);

	if (debug)
	  {
	    System.err.println("DBCheckPoint: add " + obj.getLabel() + 
			       " (" + obj.getInvid().toString() + ")");
	  }

	objects.addElement(new DBCheckPointObj(obj));
      }

    noDeleteLocks = (Hashtable) transaction.noDeleteLocks.clone();
  }
}

