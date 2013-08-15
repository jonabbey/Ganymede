/*

   PermEntry.java

   This class holds the basic per-object / per-field access control bits.

   Created: 27 June 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       PermEntry

------------------------------------------------------------------------------*/

/**
 * <P>Serializable and immutable permissions entry object, used to
 * store and transmit permissions for a specific {@link
 * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} and {@link
 * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}.</P>
 *
 * <P>Used in conjunction with
 * {@link arlut.csd.ganymede.server.PermissionMatrixDBField PermissionMatrixDBField}
 * and {@link arlut.csd.ganymede.common.PermMatrix PermMatrix} to handle Permissions
 * in a Role object in the Ganymede server.</P>
 */

public class PermEntry implements java.io.Serializable {

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.common.PermEntry");

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

  static final long serialVersionUID = 1867526089374473743L;

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

  // transient fields are initialized to 0 or false when objects
  // are deserialized, so we can use indexSet to differentiate
  // between index being zero because we have no permissions and
  // index being zero because of deserialization

  private transient byte index;
  private transient boolean indexSet;

  /* -- */

  public PermEntry(boolean visible, boolean editable, boolean create, boolean delete)
  {
    this.visible = visible;
    this.editable = editable;
    this.create = create;
    this.delete = delete;

    calcIndex();
    indexSet = true;
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

    calcIndex();
    indexSet = true;
  }

  public int hashCode()
  {
    return (visible ? 1 : 0) + (editable ? 2 : 0) + (create ? 4: 0) + (delete ? 8 : 0);
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

  public void emit(DataOutput out) throws IOException
  {
    out.writeShort(4);
    out.writeBoolean(visible);
    out.writeBoolean(editable);
    out.writeBoolean(create);
    out.writeBoolean(delete);
  }

  /**
   * <p>Private so only static method on PermEntry can call this to
   * modify the PermEntry's internal state.</p>
   */

  private void receive(DataInput in) throws IOException
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

    calcIndex();
    indexSet = true;
  }

  /**
   * <p>This method returns true if the this entry in a
   * {@link arlut.csd.ganymede.common.PermMatrix PermMatrix} is granted
   * visibility privilege.</p>
   */

  public boolean isVisible()
  {
    return visible;
  }

  /**
   * <p>This method returns true if the this entry in a
   * {@link arlut.csd.ganymede.common.PermMatrix PermMatrix} is granted
   * editing privilege.</p>
   */

  public boolean isEditable()
  {
    return editable;
  }

  /**
   * <p>This method returns true if the this entry in a
   * {@link arlut.csd.ganymede.common.PermMatrix PermMatrix} is granted
   * creation privilege.</p>
   */

  public boolean isCreatable()
  {
    return create;
  }

  /**
   * <p>This method returns true if the this entry in a
   * {@link arlut.csd.ganymede.common.PermMatrix PermMatrix} is granted
   * deletion privilege.</p>
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
    if (!indexSet)
      {
        calcIndex();
        indexSet = true;
      }

    return index;
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
   * <p>This method returns a textual description of the changes
   * between this PermEntry and &lt;p&gt;</p>
   */

  public final String difference(PermEntry p)
  {
    StringBuilder result = new StringBuilder();

    if (visible && (p == null || !p.visible))
      {
        // "+visible"
        addString(result, ts.l("difference.addVisible"));
      }

    if (p != null && p.visible && !visible)
      {
        // "-visible"
        addString(result, ts.l("difference.remVisible"));
      }

    if (editable && (p == null || !p.editable))
      {
        // "+editable"
        addString(result, ts.l("difference.addEditable"));
      }

    if (p != null && p.editable && !editable)
      {
        // "-editable"
        addString(result, ts.l("difference.remEditable"));
      }

    if (create && (p == null || !p.create))
      {
        // "+create"
        addString(result, ts.l("difference.addCreate"));
      }

    if (p != null && p.create && !create)
      {
        // "-create"
        addString(result, ts.l("difference.remCreate"));
      }

    if (delete && (p == null || !p.delete))
      {
        // "+delete"
        addString(result, ts.l("difference.addDelete"));
      }

    if (p != null && p.delete && !delete)
      {
        // "-delete"
        addString(result, ts.l("difference.remDelete"));
      }

    return result.toString();
  }

  private void addString(StringBuilder x, String y)
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
    StringBuilder result = new StringBuilder();

    if (visible)
      {
        // "visible"
        result.append(ts.l("toString.visible"));
        result.append(" ");
      }

    if (editable)
      {
        // "editable"
        result.append(ts.l("toString.editable"));
        result.append(" ");
      }

    if (create)
      {
        // "create"
        result.append(ts.l("toString.create"));
        result.append(" ");
      }

    if (delete)
      {
        // "delete"
        result.append(ts.l("toString.delete"));
      }

    return result.toString().trim();
  }

  public String getXMLCode()
  {
    StringBuilder result = new StringBuilder();

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

  private synchronized void calcIndex()
  {
    index = 0;

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
  }
}
