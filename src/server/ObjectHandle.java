/*

   ObjectHandle.java

   This class is used to group information about objects.  It is
   used in the QueryResult class to keep things organized, and
   on the client to keep track of the status of objects on the
   server.
   
   Created: 6 February 1998
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.JDataComponent.listHandle;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    ObjectHandle

------------------------------------------------------------------------------*/

public class ObjectHandle {

  String label;
  Invid invid;
  boolean inactive, expirationSet, removalSet;
  boolean creating, editing, deleting;

  listHandle lHandle = null;

  /* -- */

  public ObjectHandle(String label, Invid invid,
			   boolean inactive,
			   boolean expirationSet,
			   boolean removalSet)
  {
    this.label = label;
    this.invid = invid;
    this.inactive = inactive;
    this.expirationSet = expirationSet;
    this.removalSet = removalSet;

    creating = editing = deleting = false;
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

  public final boolean isCreating()
  {
    return creating;
  }

  public final boolean isEditing()
  {
    return editing;
  }

  public final boolean isDeleting()
  {
    return deleting;
  }

  public final void setCreating(boolean b)
  {
    creating = b;
  }

  public final void setEditing(boolean b)
  {
    editing = b;
  }

  public final void setDeleting(boolean b)
  {
    deleting = b;
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
}
