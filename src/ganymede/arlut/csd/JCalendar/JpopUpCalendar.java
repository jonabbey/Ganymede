/*
   JpopUpCalendar.java

   
   Created: 11 March 1997
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 2002/01/29 09:55:13 $
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

package arlut.csd.JCalendar;

import arlut.csd.JDataComponent.*;
import java.awt.*;
import java.util.*;

import javax.swing.*;

/**************************************************************/      


public class JpopUpCalendar extends JFrame implements JsetValueCallback {

  static final boolean debug = false;

  // ---

  JpanelCalendar panelCal = null;
  JsetValueCallback parent;

  /* -- */

  public JpopUpCalendar(GregorianCalendar parentCalendar, JsetValueCallback callback, boolean editable) 
  {
    super(editable? "Please Choose a Date & Time": "Selected Date");

    if (callback == null)
      {
	throw new IllegalArgumentException("callback parameter is null");
      }

    parent = callback;
    
    panelCal = new JpanelCalendar(this,parentCalendar,this, editable);
    
    getContentPane().setLayout(new FlowLayout());
    getContentPane().add(panelCal);
    pack();
  }

  public boolean setValuePerformed(JValueObject vObj)
  {
    boolean b = false;

    if (debug)
      {
	System.out.println("popUp setValueperformed called");
      }

    try 
      {
	b = parent.setValuePerformed(new JValueObject(this,(Date)(vObj.getValue())));
      }
    catch (java.rmi.RemoteException e) 
      {
      }

    return b;
  }

  public void update() 
  {
    panelCal.update();
  }
}
