/*

   ObjectHandle.java

   This class is used to group information about objects.  It is
   used in the QueryResult class to keep things organized, and
   on the client to keep track of the status of objects on the
   server.
   
   Created: 6 February 1998
   Version: $Revision: 1.8 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.JDataComponent.listHandle;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    ObjectHandle

------------------------------------------------------------------------------*/

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
