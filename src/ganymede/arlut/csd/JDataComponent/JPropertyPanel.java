/*

   JPropertyPanel.java

   A javax.swing.JTable-derived property sheet GUI component, intended
   for editing a map mapping strings to ordered collections of
   strings.

   Created: 14 October 2004

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003, 2004
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.JDataComponent;

import arlut.csd.Util.Compare;

import java.awt.event.*;
import java.awt.*;

import java.util.*;
import java.rmi.*;
import java.net.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import javax.swing.table.AbstractTableModel;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  JPropertyPanel


------------------------------------------------------------------------------*/

/**
 * A javax.swing.JTable-derived property sheet GUI component, intended
 * for editing a map mapping strings to ordered collections of
 * strings.
 */

public class JPropertyPanel extends JPanel implements ActionListener {

  final static boolean debug = true;
  
  private Container parent;
  private boolean editable;
  private JTable table;
  private JPropertyPanelTM model;
  private JPanel tablePanel;
  private JButton addCustom, remove;
  private JstringField custom;

  /**
   *
   * Fully specified Constructor for JPropertyPanel
   *
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   *
   */

  public JPropertyPanel(Container parent, boolean editable)
  {
    if (debug)
      {
	System.out.println("-Adding new JPropertyPanel-");
      }
    
    setBorder(new javax.swing.border.EtchedBorder());

    this.parent = parent;
    this.editable = editable;
    
    setLayout(new BorderLayout());

    // lists holds the outPanel and inPanel.

    GridBagLayout
      gbl = new GridBagLayout();

    GridBagConstraints
      gbc = new GridBagConstraints();

    JPanel lists = new JPanel();
    lists.setLayout(gbl);

    model = new JPropertyPanelTM();
    table = new JTable(model);

    BevelBorder
      bborder = new BevelBorder(BevelBorder.RAISED);

    tablePanel = new JPanel();
    tablePanel.setBorder(bborder);
    tablePanel.setLayout(new BorderLayout());

    tablePanel.add("Center", new JScrollPane(table));

    if (editable)
      {
	remove = new JButton("remove");

	remove.setEnabled(false);
	remove.setOpaque(true);
	remove.setActionCommand("Remove");
	remove.addActionListener(this);
	tablePanel.add("South", remove);
      }

    gbc.fill = gbc.BOTH;
    gbc.gridwidth = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbl.setConstraints(tablePanel, gbc);

    lists.add(tablePanel);

    add("Center", lists);

    if (editable)
      {
	custom = new JstringField();
	custom.setBorder(new EmptyBorder(new Insets(0,0,0,4)));
	custom.addActionListener(new ActionListener() 
				 {
				   public void actionPerformed(ActionEvent e)
				     {
				       addCustom.doClick();
				     }
				 });

	JPanel customP = new JPanel();
	customP.setLayout(new BorderLayout());
	customP.add("Center", custom);

	addCustom = new JButton("Add");
	addCustom.setEnabled(false);
	addCustom.setActionCommand("AddNewString");
	addCustom.addActionListener(this);
	customP.add("West", addCustom);

	// we only want this add button to be active when the user
	// has entered something in the text field.  Some users
	// have been confused by the add button just sitting there
	// active.

	custom.getDocument().addDocumentListener(new DocumentListener()
	  {
	    public void changedUpdate(DocumentEvent x) {}
	    public void insertUpdate(DocumentEvent x) 
	    {
	      if (x.getDocument().getLength() > 0)
		{
		  addCustom.setEnabled(true);
		}
	    }
	    
	    public void removeUpdate(DocumentEvent x) 
	    {
	      if (x.getDocument().getLength() == 0)
		{
		  addCustom.setEnabled(false);
		}
	    }
	  });

	add("South", customP);
      }

    invalidate();
    parent.validate();

    if (debug)
      {
	System.out.println("Done creating JPP");
      }
  }

  // ActionListener methods -------------------------------------------------

  /**
   *
   * This method handles events from the Add and Remove
   * buttons, and from hitting enter/loss of focus in the
   * custom JstringField.
   *
   */

  public void actionPerformed(ActionEvent e)
  {
    if (!editable)
      {
	return;
      }
  }

  /**
   * debug rig
   */

  public static void main(String[] args) {
    /*try {
      UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName() );
    } 
    catch (Exception e) { }*/

    System.out.println("main");

    JFrame frame = new JFrame("SwingApplication");

    Vector v1 = new Vector();
    Vector v2 = new Vector();
    for ( int i=0; i < 10; i++ )
      {
	v1.addElement( Integer.toString( i ) );
	v2.addElement( Integer.toString( 20-i ) );
      }

    JPropertyPanel ss = new JPropertyPanel(frame, false);

    //    ss.update(v1, true, null, v2, true, null);
	
    frame.getContentPane().add(ss, BorderLayout.CENTER);

    frame.addWindowListener(new WindowAdapter() 
    {
      public void windowClosing(WindowEvent e) 
      {
        System.exit(0);
      }
    });
    
    frame.pack();
    frame.setVisible(true);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                JPropertyPanelTM

------------------------------------------------------------------------------*/

/**
 * TreeModel for the JPropertyPanel
 */

class JPropertyPanelTM extends AbstractTableModel {

  String[][] values = {{"syncRole", "IRIS"},
		       {"syncRol", "WinAD"},
		       {"svc_class", "unix"}};
  
  public String getColumnName(int col)
  {
    switch (col)
      {
	case 0: return "Property";
	case 1: return "Value";
      }

    return "Huh?";
  }

  public Object getValueAt(int row, int col)
  {
    return values[row][col];
  }

  public int getRowCount()
  {
    return 3;
  }

  public int getColumnCount()
  {
    return 2;
  }
}
