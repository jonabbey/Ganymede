/*
   GASH 2

   DBNameSpaceHandle.java

   The GANYMEDE object storage system.

   Created: 15 January 1999
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:05:35 $
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
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBNameSpaceHandle

------------------------------------------------------------------------------*/

/**
 *
 * <p>This class is intended to be the targets of elements of a name
 * space's unique value hash.  The fields in this class are used to
 * keep track of who currently 'owns' a given value, and whether or not
 * there is actually any field in the namespace that really contains
 * that value.</p>
 *
 *
 * <p>This class will be manipulated by the DBNameSpace class and by the
 * DBEditObject class.</p>
 *
 */

class DBNameSpaceHandle {

  /**
   * if this value is currently being shuffled
   * by a transaction, this is the transaction
   */

  DBEditSet owner;

  /**
   * remember if the value was in use at the
   * start of the transaction
   */

  boolean original;

  /**
   * is the value currently in use?
   */

  boolean inuse;

  /**
   * so the namespace hash can be used as an index
   * field always points to the field that contained
   * field always points to the field that contained
   * this value at the time this field was last
   * committed in a transaction
   */

  DBField field;

  /**
   * if this handle is currently being edited
   * by an editset, shadowField points to the
   * new field
   */

  DBField shadowField;

  /* -- */

  public DBNameSpaceHandle(DBEditSet owner, boolean originalValue)
  {
    this.owner = owner;
    this.original = this.inuse = originalValue;
  }

  public DBNameSpaceHandle(DBEditSet owner, boolean originalValue, DBField field)
  {
    this.owner = owner;
    this.original = this.inuse = originalValue;
    this.field = field;
  }

  public boolean matches(DBEditSet set)
  {
    return (this.owner == set);
  }
}
