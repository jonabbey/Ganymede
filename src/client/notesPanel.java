  /*

   notesPanel.java

   The frame containing the notes panel
   
   Created: 4 September 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.rmi.*;
import java.util.*;
import java.text.SimpleDateFormat;

import com.sun.java.swing.*;
import com.sun.java.swing.event.*;

import jdj.PackageResources;

import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;


public class notesPanel extends JBufferedPane implements DocumentListener{

  final static boolean debug = true;

  JTextArea
    notesArea;

  framePanel 
    parent;

  string_field
    notes_field;

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
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      setInsets(new Insets(5,5,5,5));

      JBufferedPane topPane = new JBufferedPane(false);
      topPane.setLayout(new BoxLayout(topPane, BoxLayout.Y_AXIS));
      JBufferedPane leftPane = new JBufferedPane(false);
      leftPane.setLayout(new BoxLayout(leftPane, BoxLayout.Y_AXIS));
      JBufferedPane rightPane = new JBufferedPane(false);
      rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));

      JBorderedPane creationPane = new JBorderedPane();
      creationPane.setBorder(BorderFactory.createTitledBorder(creationPane, "Creation Information"));
      creationPane.setLayout(new BorderLayout());
      creationPane.add("Center", leftPane);

      JBorderedPane modificationPane = new JBorderedPane();
      modificationPane.setBorder(BorderFactory.createTitledBorder(modificationPane, "Last Modification"));
      modificationPane.setLayout(new BorderLayout());
      modificationPane.add("Center", rightPane);

      topPane.add(creationPane);
      topPane.add(Box.createVerticalStrut(3));
      topPane.add(modificationPane);
      topPane.add(Box.createVerticalStrut(3));

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

      JLabel createdBy = new JLabel();
      if (creator == null)
	{
	  createdBy.setText("No creator set for this object.");
	}
      else
	{
	  createdBy.setText("Created by " + creator);
	}

      JLabel createdOn = new JLabel();
      if (creation_date == null)
	{
	  createdOn.setText("No creation date has been set for this object.");
	}
      else
	{
	  createdOn.setText("Created on " + dateformat.format(creation_date));
	}

      leftPane.add(createdBy);
      leftPane.add(Box.createVerticalStrut(3));
      leftPane.add(createdOn);
      leftPane.add(Box.createVerticalStrut(3));

      JLabel modifiedBy = new JLabel();
      if (modifier == null)
	{
	  modifiedBy.setText("No information about the last modifier.");
	}
      else
	{
	  modifiedBy.setText("Modified by " + modifier);
	}
      JLabel modifiedOn = new JLabel();
      if (mod_date == null)
	{
	  modifiedOn.setText("No last modification date");
	}
      else
	{
	  modifiedOn.setText("Modified on " + dateformat.format(mod_date));
	}

      rightPane.add(modifiedBy);
      rightPane.add(Box.createVerticalStrut(3));
      rightPane.add(modifiedOn);
      rightPane.add(Box.createVerticalStrut(3));

      //add("North", topPane);
      add(topPane);
      
      JBorderedPane bottomPane = new JBorderedPane();
      bottomPane.setBorder(BorderFactory.createTitledBorder(bottomPane, "Notes"));
      bottomPane.setLayout(new BorderLayout());

      notesArea = new JTextArea(null, 30,15, JTextArea.SCROLLBARS_NONE);

      if (debug)
	{
	  System.out.println("Columns= " + notesArea.getColumns());
	  System.out.println("Rows =   " + notesArea.getRows());
	}

      bottomPane.add("Center", notesArea);
      add(bottomPane);
      //add("Center", bottomPane);
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
      
      //notesArea.getDocument().addDocumentListener(this);

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

  // DocumentListener methods
  // Get rid of this soon, don't think we need the listeners
  public void insertUpdate(DocumentEvent e)
    {
      System.out.println("insert update");
    }
  public void changedUpdate(DocumentEvent e)
    {
      System.out.println("changed update");
    }

  public void removeUpdate(DocumentEvent e)
    {
      System.out.println("remove update");
    }

}
