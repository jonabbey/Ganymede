/*

   PermEntry.java

   This class holds the basic per-object / per-field access control bits.
   
   Created: 27 June 1997
   Version: $Revision: 1.15 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       PermEntry

------------------------------------------------------------------------------*/

public class PermEntry implements java.io.Serializable {

  static final long serialVersionUID = 1595596943430809895L;

  static public final PermEntry fullPerms = new PermEntry(true, true, true, true);
  static public final PermEntry noPerms = new PermEntry(false, false, false, false);

  // ---

  boolean visible;
  boolean editable;
  boolean create;
  boolean delete;

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
   * This method returns true if the this entry in a PermMatrix is granted
   * visibility privilege.
   *
   */ 

  public boolean isVisible()
  {
    return visible;
  }

  /**
   *
   * This method returns true if the this entry in a PermMatrix is granted
   * editing privilege.
   *
   */ 

  public boolean isEditable()
  {
    return editable;
  }

  /**
   *
   * This method returns true if the this entry in a PermMatrix is granted
   * creation privilege.
   *
   */ 

  public boolean isCreatable()
  {
    return create;
  }

  /**
   *
   * This method returns true if the this entry in a PermMatrix is granted
   * deletion privilege.
   *
   */ 

  public boolean isDeletable()
  {
    return delete;
  }

  public final PermEntry union(PermEntry p)
  {
    if (p == null)
      {
	return this;
      }
    else
      {
	return new PermEntry(p.visible || visible,
			     p.editable || editable,
			     p.create || create,
			     p.delete || delete);
      }
  }

  public final PermEntry intersection(PermEntry p)
  {
    if (p == null)
      {
	return PermEntry.noPerms;
      }

    return new PermEntry(p.visible && visible,
			 p.editable && editable,
			 p.create && create,
			 p.delete && delete);
  }

  /**
   *
   * Diagnostic aid.
   *
   */

  public String toString()
  {
    String result;

    result = "visible : " + (visible ? "true" : "false");
    result += " editable : " + (editable ? "true" : "false");
    result += " create : " + (create ? "true" : "false");
    result += " deletable : " + (delete ? "true" : "false");

    return result;
  }
}
