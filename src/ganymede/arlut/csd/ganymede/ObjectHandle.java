/*

   ObjectHandle.java

   This class is used to group information about objects.  It is
   used in the QueryResult class to keep things organized, and
   on the client to keep track of the status of objects on the
   server.
   
   Created: 6 February 1998
   Release: $Name:  $
   Version: $Revision: 1.10 $
   Last Mod Date: $Date: 1999/03/25 08:19:52 $
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

import arlut.csd.JDataComponent.listHandle;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    ObjectHandle

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to group information about objects.  It is
 * used in the {@link arlut.csd.ganymede.QueryResult QueryResult}
 * class to keep things organized, and on the client to keep
 * track of the status of objects on the server.</p>
 *
 * @version $Revision: 1.10 $ $Date: 1999/03/25 08:19:52 $ $Name:  $
 * @author Jonathan Abbey
 */

public class ObjectHandle implements Cloneable {

  String label;
  Invid invid;
  boolean editable = false;
  boolean inactive, expirationSet, removalSet;

  listHandle lHandle = null;

  /* -- */

  public ObjectHandle(String label, Invid invid,
		      boolean inactive,
		      boolean expirationSet,
		      boolean removalSet,
		      boolean editable)
  {
    this.label = label;
    this.invid = invid;
    this.inactive = inactive;
    this.expirationSet = expirationSet;
    this.removalSet = removalSet;
    this.editable = editable;
  }

  public Object clone()
  {
    try
      {
	return super.clone();
      }
    catch (CloneNotSupportedException ex)
      {
      }

    return null;		// if it didn't work.. not a prob.
  }

  public final String getLabel()
  {
    return label;
  }

  public final void setLabel(String label)
  {
    this.label = label;

    if (lHandle != null)
      {
	lHandle.setLabel(label);
      }
  }

  public final Invid getInvid()
  {
    return invid;
  }

  /**
   *
   * Various GUI components use listHandles.
   *
   */

  public final listHandle getListHandle()
  {
    if (lHandle == null)
      {
	lHandle = new listHandle(label, invid);
      }

    return lHandle;
  }

  public final boolean isInactive()
  {
    return inactive;
  }

  public final boolean isExpirationSet()
  {
    return expirationSet;
  }

  public final boolean isRemovalSet()
  {
    return removalSet;
  }

  public final boolean isEditable()
  {
    return editable;
  }

  public final void setEditable(boolean editable)
  {
    this.editable = editable;
  }

  public void setExpirationSet(boolean expirationSet)
  {
    this.expirationSet = expirationSet;
  }

  public void setInactive(boolean isInactive)
  {
    this.inactive = isInactive;
  }

  public void setRemovalSet(boolean removalSet)
  {
    this.removalSet = removalSet;
  }

  /**
   *
   * toString() is not finalized, in case we get
   * wacky with subclassing.
   *
   */

  public String toString()
  {
    return label;
  }

  public String debugDump()
  {
    StringBuffer tmpBuf = new StringBuffer();

    tmpBuf.append(label);
    tmpBuf.append(": ");
    
    if (editable)
      {
	tmpBuf.append("editable :");
      }

    if (inactive)
      {
	tmpBuf.append("inactive :");
      }
    
    if (expirationSet)
      {
	tmpBuf.append("expiration set :");
      }

    if (removalSet)
      {
	tmpBuf.append("removal set :");
      }

    return tmpBuf.toString();
  }
}
