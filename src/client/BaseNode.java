/*
   BaseNode.java

   Tree node subclass used by gclient.java

   Created: 15 January 1999
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/16 02:11:44 $
   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;
import arlut.csd.JTree.*;

import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           Class
                                                                        BaseNode

------------------------------------------------------------------------------*/

class BaseNode extends arlut.csd.JTree.treeNode {

  private Base base;

  private Query 
    editableQuery = null,
    allQuery = null;

  private boolean loaded = false;
  private boolean canBeInactivated = false;
  private boolean showAll = false;
  private boolean canCreate;
  private Short type = null;

  /* -- */

  BaseNode(treeNode parent, String text, Base base, treeNode insertAfter,
	   boolean expandable, int openImage, int closedImage, treeMenu menu, boolean canCreate)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);
    this.base = base;
    this.canCreate = canCreate;
    
    try
      {
	canBeInactivated = base.canInactivate();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check inactivate.");
      }
  }

  public Short getTypeID()
  {
    if (type == null)
      {
	try
	  {
	    type = new Short(base.getTypeID());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get the base type in BaseNode: " + rx);
	  }
      }

    return type;
  }

  public Base getBase()
  {
    return base;
  }

  public boolean canInactivate()
  {
    return canBeInactivated;
  }

  public boolean canCreate()
  {
    return canCreate;
  }

  public boolean isShowAll()
  {
    return showAll;
  }

  public void showAll(boolean showAll)
  {
    this.showAll = showAll;
  }

  public void setBase(Base base)
  {
    this.base = base;
  }

  public void setEditableQuery(Query query)
  {
    this.editableQuery = query;
  }

  public void setAllQuery(Query query)
  {
    this.allQuery = query;
  }

  public Query getEditableQuery()
  {
    if (editableQuery == null)
      {	
	editableQuery = new Query(getTypeID().shortValue(), 
				  null, true);// include all, even non-editables
      }

    return editableQuery;
  }

  public Query getAllQuery()
  {
    if (allQuery == null)
      {
        allQuery = new Query(getTypeID().shortValue(), null, false);
      }

    return allQuery;
  }

  public boolean isLoaded()
  {
    return loaded;
  }

  public void markLoaded()
  {
    loaded = true;
  }

  public void markUnLoaded()
  {
    loaded = false;
  }
}
