/*
   JFocusRootPanel.java
   
   Created: 9 October 2000
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/10/10 02:18:44 $
   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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

package arlut.csd.JDataComponent;

import javax.swing.JPanel;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 JFocusRootPanel

------------------------------------------------------------------------------*/

public class JFocusRootPanel extends JPanel {

  public JFocusRootPanel()
  {
  }

  public boolean isFocusCycleRoot()
  {
    return true;
  }
}
