/*
   JchoiceList.java

   
   Created: 1 Oct 1996
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:03:57 $
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


package arlut.csd.JDataComponent;

import java.awt.*;
import java.awt.event.*;

/**********************************************************************
JchoiceList

This class is basically a java.awt.List object with some added
functionality.  When the user makes a selection, a callback is
made to send the string representation of that selection to whatever
component in attached to the JchoiceList.

**********************************************************************/
public class JchoiceList extends Panel {

  JchoiceInterface my_cint = null;

  List l;
  Button restore;
  Button apply;

  boolean changed = false;

  public JchoiceList()
  {
    setLayout(new BorderLayout());

    l = new List();
    restore = new Button("Restore");

    add("North",l);
    add("South",restore);
  }

  public JchoiceList(JchoiceInterface cint)
  {
    this();

    if (cint == null)
      throw new IllegalArgumentException("Illegal Argument: The handle to JchoiceInterface is null");

    my_cint = cint;
  }


  public void attach(JchoiceInterface cint)
  {
    if (cint == null)
      throw new IllegalArgumentException("Illegal Argument: The handle to JchoiceInterface is null");

    if (my_cint != null)
      my_cint.unAttach();

    my_cint = cint;
    clear();
  }

  public void detach()
  {
    my_cint = null;
    clear();
  }

  public void clear()
  {
    l.removeAll();
  }
  
  public void setChoices(String[] choices)
  {
    if (choices == null)
      throw new IllegalArgumentException("Illegal Argument: The array of choices is null");

    clear();

    for (int i=0;i<choices.length;i++)
	l.addItem(choices[i]);
  }

  public void actionPerformed(ActionEvent evt)
    {
      if (my_cint == null)
	return;

      if (evt.getSource() == restore)
	{
	  my_cint.restoreValue();
	  my_cint.notifyComponent();
	  changed = true;
	  return;
	}
      if (evt.getSource() == l)
	{
	  my_cint.setVal(evt.paramString());
	  my_cint.notifyComponent();
	  changed = true;
	}
    }
}





