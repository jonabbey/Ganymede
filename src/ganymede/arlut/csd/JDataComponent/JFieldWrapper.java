/*
   JFieldWrapper.java
   
   Created: 1 Oct 1996
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 1999/01/22 18:03:55 $
   Module By: Navin Manohar

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

/**
 *
 *  This file contains classes that will provide a decorator of sorts for each field component
 *  that will be added to the containerPanel. There will be two types of
 *  wrappers. (maybe?)  One will contain a label and a space for the field component.
 *  The other one will contain a space for a field component and a delete ("X") button.
 *  The latter is designed to wrap the field components contained within a vectorPanel.


 ****NOTE:  Add functionality to display a field comment string as part of a pop up dialog box
            The right mouse button will activate the dialog box.

 */

package arlut.csd.JDataComponent;

import javax.swing.*;

import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   JFieldWrapper

------------------------------------------------------------------------------*/

public class JFieldWrapper extends JPanel {

  JComponent my_field = null;

  /* -- */

  public JFieldWrapper(String fieldname)
  {
    if (fieldname==null)
      {
	throw new IllegalArgumentException("Error: handle to the name of the field is null");
      }

    setLayout(new BorderLayout());

    JLabel l = new JLabel(fieldname);

    add("West",l);
  }

  public JFieldWrapper(String fieldname,JComponent field)
  {
    this(fieldname);

    my_field = field;

    add("East",my_field);
  }

  public void highLight()
  {
    // Set the background color to red and foreground color to black
  }

}
