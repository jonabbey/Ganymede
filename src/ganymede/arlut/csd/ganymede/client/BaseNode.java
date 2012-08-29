/*
   BaseNode.java

   Tree node subclass used by gclient.java

   Created: 15 January 1999


   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2009
   The University of Texas at Austin

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

package arlut.csd.ganymede.client;

import java.rmi.RemoteException;

import arlut.csd.JTree.treeMenu;
import arlut.csd.JTree.treeNode;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.rmi.Base;

/*------------------------------------------------------------------------------
                                                                           Class
                                                                        BaseNode

------------------------------------------------------------------------------*/

/**
 * Subclass of {@link arlut.csd.JTree.treeNode treeNode} used in the
 * client.
 */

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
    catch (Exception rx)
      {
        gclient.client.processExceptionRethrow(rx);
      }
  }

  public Short getTypeID()
  {
    if (type == null)
      {
        try
          {
            type = Short.valueOf(base.getTypeID());
          }
        catch (Exception rx)
          {
            gclient.client.processExceptionRethrow(rx);
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
