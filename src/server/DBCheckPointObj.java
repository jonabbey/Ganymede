/*
   GASH 2

   DBCheckPointObj.java

   The GANYMEDE object storage system.

   Created: 15 January 1999
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 1999/01/16 01:48:34 $
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 DBCheckPointObj

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

class DBCheckPointObj {

  Invid invid;
  Hashtable fields;
  byte status;

  /* -- */

  DBCheckPointObj(DBEditObject obj)
  {
    this.invid = obj.getInvid();
    this.status = obj.status;
    this.fields = obj.checkpoint();
  }
}
