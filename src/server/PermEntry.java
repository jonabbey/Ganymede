/*

   PermEntry.java

   This class holds the basic per-object / per-field access control bits.
   
   Created: 27 June 1997
   Version: $Revision: 1.5 $ %D%
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

  private boolean visible;
  private boolean editable;
  private boolean create;

  /* -- */

  public PermEntry(boolean visible, boolean editable, boolean create)
  {
    this.visible = visible;
    this.editable = editable;
    this.create = create;
  }

  public PermEntry(DataInput in) throws IOException
  {
    receive(in);
  }

  public boolean equals(Object obj)
  {
    PermEntry pe;

    /* -- */

    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    pe = (PermEntry) obj;

    return ((visible == pe.visible) && (editable == pe.editable) && (create == pe.create));
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeShort(3);
    out.writeBoolean(visible);
    out.writeBoolean(editable);
    out.writeBoolean(create);
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
  }

  /**
   * This method returns true if the this entry in a PermMatrix is granted
   * visibility privilege.
   *
   */ 

  public boolean isVisible()
  {
    return visible;
  }

  /**
   * This method returns true if the this entry in a PermMatrix is granted
   * editing privilege.
   *
   */ 

  public boolean isEditable()
  {
    return editable;
  }

  /**
   * This method returns true if the this entry in a PermMatrix is granted
   * creation privilege.
   *
   */ 

  public boolean isCreatable()
  {
    return create;
  }
}
