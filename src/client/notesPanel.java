  /*

   notesPanel.java

   The frame containing the notes panel
   
   Created: 4 September 1997
   Version: $Revision: 1.6 $ %D%
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

import jdj.PackageResources;

import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;


public class notesPanel extends JPanel{

  final static boolean debug = false;

  int 
    row = 0;

  JPanel
    center;
  
  JTextArea
    notesArea;

  framePanel 
    fp;

  string_field
    notes_field;

  JTextField
    createdBy,
    modifiedBy,
    createdOn,
    modifiedOn;

  GridBagLayout
    gbl;

  GridBagConstraints
    gbc;

  public notesPanel(string_field notes_field, string_field creator_field, 
		    date_field creation_date_field, string_field modifier_field,
		    date_field modification_date_field, boolean editable, framePanel fp)
    {
      if (debug)
	{
	  System.out.println("Creating notes panel");
	}
      
      this.fp = fp;
      this.notes_field = notes_field;

      setBorder(fp.wp.emptyBorder5);

      center = new JPanel(false);
      setLayout(new BorderLayout());
      add("West", center);

      gbc = new GridBagConstraints();
      gbl = new GridBagLayout();
      center.setLayout(gbl);

      gbc.anchor = GridBagConstraints.NORTHWEST;
      gbc.insets = new Insets(6,6,6,6);

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

      createdBy = new JTextField(30);

      if (creator == null)
	{
	  createdBy.setText("No creator set for this object.");
	}
      else
	{
	  createdBy.setText(creator);
	}

      addRow(createdBy, "Created By:");

      createdOn = new JTextField(30);

      if (creation_date == null)
	{
	  createdOn.setText("No creation date has been set for this object.");
	}
      else
	{
	  createdOn.setText(dateformat.format(creation_date));
	}

      addRow(createdOn, "Created On:");

      modifiedBy = new JTextField(30);
      if (modifier == null)
	{
	  modifiedBy.setText("No information about the last modifier.");
	}
      else
	{
	  modifiedBy.setText(modifier);
	}

      addRow(modifiedBy, "Modified By:");

      modifiedOn = new JTextField(30);
      if (mod_date == null)
	{
	  modifiedOn.setText("No last modification date");
	}
      else
	{
	  modifiedOn.setText(dateformat.format(mod_date));
	}

      addRow(modifiedOn, "Modified on:");

      
      notesArea = new JTextArea();
      EmptyBorder eb = fp.wp.emptyBorder5;
      TitledBorder tb = new TitledBorder("Notes");
      notesArea.setBorder(new CompoundBorder(tb,eb));

      gbc.weighty = 1.0;
      gbc.weightx = 0.0;

      gbc.gridx = 0;
      gbc.gridy = row;
      //gbc.gridwidth = 2;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.fill = GridBagConstraints.BOTH;
      JScrollPane notesScroll = new JScrollPane(notesArea);
      gbl.setConstraints(notesScroll, gbc);
      center.add(notesScroll);

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

  void addRow(JComponent comp, String title)
  {

    JLabel l = new JLabel(title);
    
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;

    gbc.gridwidth = 1;
    gbc.gridx = 0;
    gbc.gridy = row;
    gbl.setConstraints(l, gbc);
    center.add(l);

    gbc.gridx = 1;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(comp, gbc);
    center.add(comp);

    row++;

  }


}
