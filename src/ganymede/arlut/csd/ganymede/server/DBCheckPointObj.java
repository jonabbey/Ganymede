/*
   GASH 2

   DBCheckPointObj.java

   The GANYMEDE object storage system.

   Created: 15 January 1999
   Version: $Revision$
   Last Mod Date: $Date$
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
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

package arlut.csd.ganymede.server;

import java.util.Hashtable;

import arlut.csd.ganymede.common.Invid;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 DBCheckPointObj

------------------------------------------------------------------------------*/

/**
 * <p>DBCheckPointObj holds a snapshot of an object's state at a moment
 * in time.  It is used by the {@link arlut.csd.ganymede.server.DBCheckPoint DBCheckPoint}
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
