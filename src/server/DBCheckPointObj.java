/*
   GASH 2

   DBCheckPointObj.java

   The GANYMEDE object storage system.

   Created: 15 January 1999
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 2000/06/17 00:23:53 $
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
                                                                 DBCheckPointObj

------------------------------------------------------------------------------*/

/**
 * <p>DBCheckPointObj holds a snapshot of an object's state at a moment
 * in time.  It is used by the {@link arlut.csd.ganymede.DBCheckPoint DBCheckPoint}
 * class to record the state of the fields in an object.</p>
 */

class DBCheckPointObj {

  Invid invid;
  byte status;

  /**
   * <p>This field actually holds the object value state.</p>
   */

  Hashtable fields;

  /* -- */

  DBCheckPointObj(DBEditObject obj)
  {
    this.invid = obj.getInvid();
    this.status = obj.status;
    this.fields = obj.checkpoint();
  }
}
