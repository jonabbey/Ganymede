/*
   GASH 2

   DBCheckPoint.java

   The GANYMEDE object storage system.

   Created: 15 January 1999
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/16 01:48:33 $
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
 * DBCheckPoint is a class designed to allow server-side code that
 * needs to attempt a multi-step operation that might not successfully
 * complete to be able to undo all changes made without having to
 * abort the entire transaction.
 * 
 * In other words, a DBCheckPoint is basically a transaction within a transaction.
 *
 */

class DBCheckPoint {

  static final boolean debug = false;

  // ---

  String 
    name;

  Vector
    objects = null,
    logEvents = null;

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
  }
}

