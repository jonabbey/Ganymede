/*

   choiceThing.java

   Resource class for use with StringDialog.java
   
   Created: 16 June 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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

package arlut.csd.JDialog;

import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     choiceThing

------------------------------------------------------------------------------*/

/**
 * <P>Serializable object to describe a string chooser field for passing to the
 * client as part of a {@link arlut.csd.JDialog.JDialogBuff JDialogBuff}
 * or {@link arlut.csd.JDialog.StringDialog StringDialog}.</P> 
 */

public class choiceThing implements java.io.Serializable {
  
  String choiceLabel;
  Vector items;
  Object selected;

  /* -- */

  public choiceThing(String label, Vector Items)
  {
    this(label, Items, null);
  }

  public choiceThing(String label, Vector Items, Object selectedObject)
  {
    this.choiceLabel = label;
    this.items = Items;
    this.selected = selectedObject;
  }

  public String getLabel()
  {
    return choiceLabel;
  }

  public Vector getItems()
  {
    return items;
  }

  public Object getSelectedItem()
  {
    return selected;
  }
}
