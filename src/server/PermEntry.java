/*

   PermEntry.java

   This class holds the basic per-object / per-field access control bits.
   
   Created: 27 June 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       PermEntry

------------------------------------------------------------------------------*/

private boolean visible;
private boolean editable;

/* -- */

public class PermEntry implements java.io.Serializable {

  public PermEntry(boolean visible, boolean editable)
  {
    this.visible = visible;
    this.editable = editable;
  }

  public PermEntry(DataInput in) throws IOException
  {
    receive(in);
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeShort(2);
    out.writeBoolean(visible);
    out.writeBoolean(editable);
  }

  void receive(DataInput in) throws IOException
  {
    short entrySize;

    /* -- */

    entrySize = in.readShort();
    
    // we'll only worry about entrySize if we add perm bools later

    visible = in.readBoolean();
    editable = in.readBoolean();
  }

  /**
   * This method returns true if the this entry in a PermMatrix is granted
   * visibility privilege.
   *
   * @see arlut.csd.ganymede.PermEntry
   */ 

  public boolean isVisible()
  {
    return visible;
  }

  /**
   * This method returns true if the this entry in a PermMatrix is granted
   * editing privilege.
   *
   * @see arlut.csd.ganymede.PermEntry
   */ 

  public boolean isEditable()
  {
    return editable;
  }
}
