/*

   PermEntry.java

   This class holds the basic per-object / per-field access control bits.
   
   Created: 27 June 1997
   Release: $Name:  $
   Version: $Revision: 1.25 $
   Last Mod Date: $Date: 2001/05/07 05:57:53 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
   The University of Texas at Austin.

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

/*------------------------------------------------------------------------------
                                                                           class
                                                                       PermEntry

------------------------------------------------------------------------------*/

/**
 * <P>Serializable and immutable permissions entry object, used to
 * store and transmit permissions for a specific {@link
 * arlut.csd.ganymede.DBObjectBase DBObjectBase} and {@link
 * arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField}.</P>
 *
 * <P>Used in conjunction with
 * {@link arlut.csd.ganymede.PermissionMatrixDBField PermissionMatrixDBField}
 * and {@link arlut.csd.ganymede.PermMatrix PermMatrix} to handle Permissions
 * in a Role object in the Ganymede server.</P>
 */

public class PermEntry implements java.io.Serializable {

  static private PermEntry[] permObs;
  static public final PermEntry fullPerms;
  static public final PermEntry noPerms;
  static public final PermEntry viewPerms;

  static
    {
      permObs = new PermEntry[16];

      permObs[0] = new PermEntry(false, false, false, false);
      permObs[1] = new PermEntry(true, false, false, false);
      permObs[2] = new PermEntry(false, true, false, false);
      permObs[3] = new PermEntry(true, true, false, false);
      permObs[4] = new PermEntry(false, false, true, false);
      permObs[5] = new PermEntry(true, false, true, false);
      permObs[6] = new PermEntry(false, true, true, false);
      permObs[7] = new PermEntry(true, true, true, false);
      permObs[8] = new PermEntry(false, false, false, true);
      permObs[9] = new PermEntry(true, false, false, true);
      permObs[10] = new PermEntry(false, true, false, true);
      permObs[11] = new PermEntry(true, true, false, true);
      permObs[12] = new PermEntry(false, false, true, true);
      permObs[13] = new PermEntry(true, false, true, true);
      permObs[14] = new PermEntry(false, true, true, true);
      permObs[15] = new PermEntry(true, true, true, true);

      fullPerms = permObs[15];
      noPerms = permObs[0];
      viewPerms = permObs[1];
    }

  static final long serialVersionUID = 1595596943430809895L;

  public static void main(String argv[])
  {
    PermEntry x = getPermEntry(true, false, false, false);

    System.err.println(x.union(x));
  }

  /**
   * <p>This static method returns a reference to an immutable PermEntry
   * object with the requested privilege bits set.</p>
   */

  public static PermEntry getPermEntry(boolean visible, boolean editable, boolean create, boolean delete)
  {
    byte result = 0;

    /* -- */

    if (visible)
      {
	result++;
      }

    if (editable)
      {
	result += 2;
      }

    if (create)
      {
	result += 4;
      }

    if (delete)
      {
	result += 8;
      }

    return permObs[result];
  }

  /**
   * <p>This static method returns a reference to an immutable PermEntry
   * object with the requested privilege bits set.</p>
   */

  public static PermEntry getPermEntry(byte index)
  {
    return permObs[index];
  }

  /**
   * <p>This static method reads a PermEntry object from the given DataInput
   * stream and returns an immutable PermEntry with the appropriate bits
   * set.</p>
   */

  public static PermEntry getPermEntry(DataInput in) throws IOException
  {
    boolean visible, editable, create, delete;
    short entrySize;

    /* -- */

    entrySize = in.readShort();
    
    // we'll only worry about entrySize if we add perm bools later

    visible = in.readBoolean();
    editable = in.readBoolean();
    create = in.readBoolean();

    if (entrySize >= 4)
      {
	delete = in.readBoolean();
      }
    else
      {
	delete = false;
      }

    return getPermEntry(visible, editable, create, delete);
  }
  
  // ---

  private boolean visible;
  private boolean editable;
  private boolean create;
  private boolean delete;

  /* -- */

  public PermEntry(boolean visible, boolean editable, boolean create, boolean delete)
  {
    this.visible = visible;
    this.editable = editable;
    this.create = create;
    this.delete = delete;
  }

  public PermEntry(DataInput in) throws IOException
  {
    receive(in);
  }

  public PermEntry(PermEntry orig)
  {
    this.visible = orig.visible;
    this.editable = orig.editable;
    this.create = orig.create;
    this.delete = orig.delete;
  }

  public boolean equals(Object obj)
  {
    PermEntry pe;

    /* -- */

    if (obj == null)
      {
	return false;
      }

    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    pe = (PermEntry) obj;

    return ((visible == pe.visible) && 
	    (editable == pe.editable) && 
	    (create == pe.create) &&
	    (delete == pe.delete));
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeShort(4);
    out.writeBoolean(visible);
    out.writeBoolean(editable);
    out.writeBoolean(create);
    out.writeBoolean(delete);
  }

  void receive(DataInput in) throws IOException
  {
    short entrySize;

    /* -- */

    entrySize = in.readShort();
    
    // we'll only worry about entrySize if we add perm bools later

    visible = in.readBoolean();
    editable = in.readBoolean();
    create = in.readBoolean();

    if (entrySize >= 4)
      {
	delete = in.readBoolean();
      }
    else
      {
	delete = false;
      }
  }

  /**
   *
   * This method returns true if the this entry in a
   * {@link arlut.csd.ganymede.PermMatrix PermMatrix} is granted
   * visibility privilege.
   *
   */ 

  public boolean isVisible()
  {
    return visible;
  }

  /**
   *
   * This method returns true if the this entry in a
   * {@link arlut.csd.ganymede.PermMatrix PermMatrix} is granted
   * editing privilege.
   *
   */ 

  public boolean isEditable()
  {
    return editable;
  }

  /**
   *
   * This method returns true if the this entry in a 
   * {@link arlut.csd.ganymede.PermMatrix PermMatrix} is granted
   * creation privilege.
   *
   */ 

  public boolean isCreatable()
  {
    return create;
  }

  /**
   *
   * This method returns true if the this entry in a
   * {@link arlut.csd.ganymede.PermMatrix PermMatrix} is granted
   * deletion privilege.
   *
   */ 

  public boolean isDeletable()
  {
    return delete;
  }

  /**
   * <p>This method returns a bit coded byte value representing the
   * permission bits set in this PermEntry object.  This byte may
   * be used to access a pre-allocated PermEntry object using the
   * static getPermEntry() method.</p>
   */

  public byte indexNum()
  {
    return calcIndex();
  }

  /**
   * <p>This method returns an immutable PermEntry that allows all
   * permissions allowed by the logical union of this PermEntry and
   * p.</p>
   */

  public final PermEntry union(PermEntry p)
  {
    if (p == null)
      {
	return this;
      }

    byte pVal = p.indexNum();
    byte myVal = indexNum();

    return permObs[pVal | myVal];
  }

  /**
   * <p>This method returns an immutable PermEntry that allows all
   * permissions allowed by the logical intersection of this PermEntry and
   * p.</p>
   */

  public final PermEntry intersection(PermEntry p)
  {
    if (p == null)
      {
	return PermEntry.noPerms;
      }

    byte pVal = p.indexNum();
    byte myVal = indexNum();

    return permObs[pVal & myVal];
  }

  /**
   * <P>This method returns a textual description
   * of the changes between this PermEntry and &lt;p&gt;</P> */

  public final String difference(PermEntry p)
  {
    StringBuffer result = new StringBuffer();

    if (visible && (p == null || !p.visible))
      {
	addString(result, "+ visible");
      }

    if (p != null && p.visible && !visible)
      {
	addString(result, "- visible");
      }

    if (editable && (p == null || !p.editable))
      {
	addString(result, "+ editable");
      }

    if (p != null && p.editable && !editable)
      {
	addString(result, "- editable");
      }

    if (create && (p == null || !p.create))
      {
	addString(result, "+ create");
      }

    if (p != null && p.create && !create)
      {
	addString(result, "- create");
      }

    if (delete && (p == null || !p.delete))
      {
	addString(result, "+ delete");
      }

    if (p != null && p.delete && !delete)
      {
	addString(result, "- delete");
      }

    return result.toString();
  }

  private void addString(StringBuffer x, String y)
  {
    if (x.length() > 0)
      {
	x.append(", ");
      }

    x.append(y);
  }

  /**
   *
   * Diagnostic aid.
   *
   */

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    if (visible)
      {
	result.append("visible ");
      }

    if (editable)
      {
	result.append("editable ");
      }

    if (create)
      {
	result.append("create ");
      }

    if (delete)
      {
	result.append("delete ");
      }

    return result.toString();
  }

  public String getXMLCode()
  {
    StringBuffer result = new StringBuffer();

    if (visible)
      {
	result.append("V");
      }

    if (editable)
      {
	result.append("E");
      }

    if (create)
      {
	result.append("C");
      }

    if (delete)
      {
	result.append("D");
      }

    return result.toString();
  }

  private byte calcIndex()
  {
    byte index = 0;
    
    if (visible)
      {
	index++;
      }

    if (editable)
      {
	index += 2;
      }

    if (create)
      {
	index += 4;
      }

    if (delete)
      {
	index += 8;
      }

    return index;
  }
}
