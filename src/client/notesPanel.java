  /*

   notesPanel.java

   The frame containing the notes panel
   
   Created: 4 September 1997
   Version: $Revision: 1.1 $ %D%
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


public class notesPanel extends JBufferedPane {

  final static boolean debug = true;

  JTextArea
    notesArea;

  public notesPanel(string_field notes_field, string_field creator_field, date_field creation_date_field, string_field modifier_field, date_field modification_date_field, boolean editable)
    {
      if (debug)
	{
	  System.out.println("Creating notes panel");
	}
      
      setLayout(new BorderLayout(5,5));
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

      JLabel createdBy = new JLabel("Created by " + creator);
      JLabel createdOn = new JLabel("Created on " + dateformat.format(creation_date));
      
      leftPane.add(createdBy);
      leftPane.add(Box.createVerticalStrut(3));
      leftPane.add(createdOn);
      leftPane.add(Box.createVerticalStrut(3));

      JLabel modifiedBy = new JLabel("Modified by " + modifier);
      JLabel modifiedOn = new JLabel("Modified on " + dateformat.format(mod_date));

      rightPane.add(modifiedBy);
      rightPane.add(Box.createVerticalStrut(3));
      rightPane.add(modifiedOn);
      rightPane.add(Box.createVerticalStrut(3));

      add("North", topPane);

      JBorderedPane bottomPane = new JBorderedPane();
      bottomPane.setBorder(BorderFactory.createTitledBorder(bottomPane, "Notes"));
      bottomPane.setLayout(new BorderLayout());

      notesArea = new JTextArea();
      bottomPane.add("Center", notesArea);

      add("Center", bottomPane);
      try
	{
	  String s = (String)notes_field.getValue();
	  if (s != null)
	    {
	      notesArea.setText(s);
	    }
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get note text: " + rx);
	}
    }
}
