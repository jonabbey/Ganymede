  /*

   notesPanel.java

   The frame containing the notes panel
   
   Created: 4 September 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.rmi.*;
import java.util.*;
import java.text.SimpleDateFormat;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;
//import com.sun.java.swing.event.*;

import tablelayout.*;

import jdj.PackageResources;

import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;


public class notesPanel extends JPanel{

  final static boolean debug = true;

  int 
    row = 0;

  JTextArea
    notesArea;

  framePanel 
    parent;

  string_field
    notes_field;

  JTextField
    createdBy,
    modifiedBy,
    createdOn,
    modifiedOn;

  public notesPanel(string_field notes_field, string_field creator_field, 
		    date_field creation_date_field, string_field modifier_field,
		    date_field modification_date_field, boolean editable, framePanel parent)
    {
      if (debug)
	{
	  System.out.println("Creating notes panel");
	}
      
      this.parent = parent;
      this.notes_field = notes_field;

      //setLayout(new BorderLayout(5,5));
      //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      setLayout(new GridLayout(2,1));

      JPanel topPane = new JPanel(false);
      //topPane.setLayout(new BoxLayout(topPane, BoxLayout.Y_AXIS));
      topPane.setLayout(new GridLayout(2,1));
      JPanel createdPane = new JPanel(false);
      //createdPane.setLayout(new BoxLayout(createdPane, BoxLayout.Y_AXIS));
      createdPane.setLayout(new TableLayout());
      createdPane.setBorder(new TitledBorder("Creation Information"));
      JPanel modPane = new JPanel(false);
      //modPane.setLayout(new BoxLayout(modPane, BoxLayout.Y_AXIS));
      modPane.setLayout(new TableLayout());
      modPane.setBorder(new TitledBorder("Modification Information"));

      topPane.add(createdPane);
      //topPane.add(Box.createVerticalStrut(3));
      topPane.add(modPane);

      String creator = null;
      Date creation_date = null;
      String modifier = null;
      Date mod_date = null;

      SimpleDateFormat dateformat = new SimpleDateFormat("MMM dd, yyyy",Locale.getDefault());

      try
	{
	  creator = (String)creator_field.getValue();
	  creation_date = (Date)creation_date_field.getValue();
	  modifier = (String)modifier_field.getValue();
	  mod_date = (Date)modification_date_field.getValue();
	}
      catch ( RemoteException rx)
	{
	  throw new RuntimeException("Could not get creation info: " + rx);
	}

      createdBy = new JTextField(20);
      if (creator == null)
	{
	  createdBy.setText("No creator set for this object.");
	}
      else
	{
	  createdBy.setText(creator);
	}

      createdOn = new JTextField(20);
      if (creation_date == null)
	{
	  createdOn.setText("No creation date has been set for this object.");
	}
      else
	{
	  createdOn.setText(dateformat.format(creation_date));
	}

      addRow(createdPane, "Created on:", createdBy, 0);
      
      addRow(createdPane, "Created by:", createdOn, 1);

      modifiedBy = new JTextField(20);
      if (modifier == null)
	{
	  modifiedBy.setText("No information about the last modifier.");
	}
      else
	{
	  modifiedBy.setText(modifier);
	}

      modifiedOn = new JTextField(20);
      if (mod_date == null)
	{
	  modifiedOn.setText("No last modification date");
	}
      else
	{
	  modifiedOn.setText(dateformat.format(mod_date));
	}

      addRow(modPane, "Modified by:", modifiedBy, 0);

      addRow(modPane, "Modified on:", modifiedOn, 1);

      //add("North", topPane);
      add(topPane);
      
      //notesArea = new JTextArea(null, 30,15, JTextArea.SCROLLBARS_NONE);
      //notesArea = new JTextArea(30,15);
      notesArea = new JTextArea();
      notesArea.setBorder(new TitledBorder("Notes"));

      if (debug)
	{
	  System.out.println("Columns= " + notesArea.getColumns());
	  System.out.println("Rows =   " + notesArea.getRows());
	}

      add(notesArea);
      //add("Center", bottomPane);

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
	  notes_field.setValue(notesArea.getText().trim());
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not set notes field: " + rx);
	}
    }

  void addRow(Container cont, String label, JComponent comp, int row)
  {
    JLabel l = new JLabel(label);
    l.setOpaque(true);
    l.setBackground(ClientColor.ComponentBG);
    comp.setBackground(ClientColor.ComponentBG);
    cont.add("0 " + row + " lthwHW", l);
    cont.add("1 " + row + " lthwHW", comp);
  }


}
