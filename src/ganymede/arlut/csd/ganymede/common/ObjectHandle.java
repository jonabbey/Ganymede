/*

   ObjectHandle.java

   This class is used to group information about objects.  It is
   used in the QueryResult class to keep things organized, and
   on the client to keep track of the status of objects on the
   server.

   Created: 6 February 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.common;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import arlut.csd.JDataComponent.listHandle;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    ObjectHandle

------------------------------------------------------------------------------*/

/**
 * <p>This immutable class is used to hold serializable label, Invid
 * and object status information.</p>
 *
 * <p>ObjectHandles are collected in {@link
 * arlut.csd.ganymede.common.QueryResult QueryResults}, which are
 * returned by certain query operations in {@link
 * arlut.csd.ganymede.server.DBQueryEngine} and by the choices()
 * method in the {@link arlut.csd.ganymede.server.StringDBField} and
 * {@link arlut.csd.ganymede.server.InvidDBField} classes.</p>
 *
 * <p>Because QueryResult is also used to return string choices from
 * the StringDBField.choices() method, ObjectHandles may have null
 * Invids, in which case they are just used for transporting
 * Strings.</p>
 */

public class ObjectHandle implements Cloneable, Externalizable {

  private boolean externalizing;
  private String label;
  private Invid invid;
  private boolean editable = false;
  private boolean inactive = false;
  private boolean expirationSet = false;
  private boolean removalSet = false;

  /* -- */

  /**
   * Default no-arg constructor for Externalization
   *
   * When this constructor is called, readExternal() can be called to
   * load state into this object.  After readExternal() returns, no
   * further modifications to this object can be made.
   */

  public ObjectHandle()
  {
    this.externalizing = true;
  }

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
    this.externalizing = false;
  }

  /**
   * Relabel copy constructor
   */

  public ObjectHandle(ObjectHandle original,
                      String newLabel)
  {
    this.label = newLabel;
    this.invid = original.invid;
    this.inactive = original.inactive;
    this.expirationSet = original.expirationSet;
    this.removalSet = original.removalSet;
    this.editable = original.editable;
    this.externalizing = false;
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

    return null;                // if it didn't work.. not a prob.
  }

  public final String getLabel()
  {
    return label;
  }

  public final Invid getInvid()
  {
    return invid;
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

  /**
   * Various GUI components use listHandles.
   */

  public final listHandle getListHandle()
  {
    return new listHandle(label, invid);
  }

  // externalization methods

  public void writeExternal(ObjectOutput out) throws IOException
  {
    byte status = 0;

    if (this.inactive)
      {
        status += 1;
      }

    if (this.expirationSet)
      {
        status += 2;
      }

    if (this.removalSet)
      {
        status += 4;
      }

    if (this.editable)
      {
        status += 8;
      }

    if (this.invid == null)
      {
        status += 16;
      }

    out.writeByte(status);
    out.writeUTF(this.label);

    if (this.invid != null)
      {
        this.invid.writeExternal(out);
      }
  }

  public void readExternal(ObjectInput in) throws IOException
  {
    if (!this.externalizing)
      {
        throw new RuntimeException("Invalid double de-externalization");
      }

    byte status = in.readByte();

    this.inactive = (status & 1) != 0;
    this.expirationSet = (status & 2) != 0;
    this.removalSet = (status & 4) != 0;
    this.editable = (status & 8) != 0;

    this.label = in.readUTF();

    if ((status & 16) != 0)
      {
        this.invid = null;
        return;
      }

    Invid anInvid = new Invid();
    anInvid.readExternal(in);
    this.invid = anInvid.intern();

    this.externalizing = false;
  }

  /**
   * toString() is not finalized, in case we get
   * wacky with subclassing.
   */

  public String toString()
  {
    return label;
  }

  public String debugDump()
  {
    StringBuilder tmpBuf = new StringBuilder();

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
