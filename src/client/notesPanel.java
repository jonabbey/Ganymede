  /*

   notesPanel.java

   The frame containing the notes panel
   
   Created: 4 September 1997
   Version: $Revision: 1.14 $
   Last Mod Date: $Date: 1999/01/22 18:04:16 $
   Release: $Name:  $

   Module By: Michael Mulvaney

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

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;
import java.text.SimpleDateFormat;

import javax.swing.*;
import javax.swing.border.*;

import arlut.csd.Util.PackageResources;
import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      notesPanel

------------------------------------------------------------------------------*/

public class notesPanel extends JPanel implements KeyListener{

  boolean debug = false;
  
  JTextArea
    notesArea;

  framePanel 
    fp;

  string_field
    notes_field;

  public notesPanel(string_field    notes_field,
		    boolean         editable, 
		    framePanel      fp)
    {
      debug = fp.debug;

      if (debug)
	{
	  System.out.println("Creating notes panel");
	}
      
      this.fp = fp;
      this.notes_field = notes_field;

      setBorder(fp.wp.emptyBorder5);

      setLayout(new BorderLayout());
      
      notesArea = new JTextArea();
      EmptyBorder eb = fp.wp.emptyBorder5;
      TitledBorder tb = new TitledBorder("Notes");
      notesArea.setBorder(new CompoundBorder(tb,eb));
      notesArea.setEditable(editable);
      notesArea.addKeyListener(this);

      JScrollPane notesScroll = new JScrollPane(notesArea);
      add(BorderLayout.CENTER, notesScroll);

      if (notes_field != null)
	{
	  try
	    {
	      String s = (String)notes_field.getValue();
	      if (s != null)
		{
		  
		  notesArea.append(s);
		}
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not get note text: " + rx);
	    }
	}

    }

  public void updateNotes()
    {
      try
	{
	  if (notes_field != null)
	    {
	      if (debug)
		{
		  System.out.println("Updating notes: " + notesArea.getText().trim());
		}

	      notes_field.setValue(notesArea.getText().trim());
	    }
	  else if (debug)
	    {
	      System.out.println("notes_field is null, not updating.");
	    }
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not set notes field: " + rx);
	}
    }


  public void keyPressed(KeyEvent e)
  {
    fp.gc.somethingChanged();
    notesArea.removeKeyListener(this);
  }
  
  public void keyReleased(KeyEvent e) {}
  public void keyTyped(KeyEvent e) {}


}
