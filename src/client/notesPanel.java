  /*

   notesPanel.java

   The frame containing the notes panel
   
   Created: 4 September 1997
   Version: $Revision: 1.12 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;
import java.text.SimpleDateFormat;

import javax.swing.*;
import javax.swing.border.*;
//import javax.swing.event.*;

import jdj.PackageResources;

import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;


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
