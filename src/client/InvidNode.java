/*
   InvidNode.java

   Tree node subclass used by gclient.java and openObjectDialog.java

   Created: 15 January 1999
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:04:09 $
   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;
import arlut.csd.JTree.*;

/*---------------------------------------------------------------------
                                                                  class 
                                                              InvidNode

---------------------------------------------------------------------*/

class InvidNode extends arlut.csd.JTree.treeNode {

  final static boolean debug = true;

  private Invid invid;

  private String typeText;

  private ObjectHandle handle;

  public InvidNode(treeNode parent, String text, Invid invid, treeNode insertAfter,
		   boolean expandable, int openImage, int closedImage, treeMenu menu,
		   ObjectHandle handle)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);

    this.invid = invid;
    this.typeText = parent.getText();
    this.handle = handle;

    if (debug)
      {
	if (invid == null)
	  {
	    System.out.println(" null invid in InvidNode: " + text);
	  }
      }
  }

  public Invid getInvid()
  {
    return invid;
  }

  // Can't think of why you would ever want this

  public void setInvid(Invid invid)
  {
    this.invid = invid;
  }

  public String getTypeText()
  {
    return typeText;
  }  

  public ObjectHandle getHandle()
  {
    return handle;
  }

  public void setHandle(ObjectHandle handle)
  {
    this.handle = handle;
  }
}
