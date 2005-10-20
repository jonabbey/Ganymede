/*

   JPropertyPanel.java

   A javax.swing.JTable-derived property sheet GUI component, intended
   for editing a map mapping strings to ordered collections of
   strings.

   This class is incomplete, and under development.

   Created: 14 October 2004

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  JPropertyPanel

------------------------------------------------------------------------------*/

/**
 * A javax.swing.JTable-derived property sheet GUI component, intended
 * for editing a map mapping strings to ordered collections of
 * strings.
 *
 * This class is incomplete, and under development.
 */

public class JPropertyPanel extends JPanel implements ActionListener {

  final static boolean debug = true;
  
  private Container parent;
  private boolean editable;
  private JTable table;
  private JPropertyPanelTM model;
  private JPropertyPanelTML listener;
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

    model = new JPropertyPanelTM(editable);

    listener = new JPropertyPanelTML();
    model.addTableModelListener(listener);

    table = new JTable(model);

    table.getSelectionModel().addListSelectionListener(new JPropertyPanelLSL(this));

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

    if (e.getActionCommand().equals("AddNewString"))
      {
	model.addRow(custom.getText());
	custom.setText("");
	table.repaint();
      }

    if (e.getActionCommand().equals("Remove"))
      {
	int[] selected = table.getSelectedRows();

	for (int i = selected.length - 1; i >= 0; i--)
	  {
	    model.deleteRow(selected[i]);
	  }
      }
  }

  public void setSelection(boolean selectionActive)
  {
    remove.setEnabled(selectionActive);

    if (selectionActive)
      {
	int[] selected = table.getSelectedRows();

	for (int i = 0; i < selected.length; i++)
	  {
	    System.out.println("Selected: " + selected[i]);
	  }
      }
  }

  /**
   * debug rig
   */

  public static void main(String[] args) {
    System.out.println("main");

    JFrame frame = new JFrame("SwingApplication");

    Vector v1 = new Vector();
    Vector v2 = new Vector();
    for ( int i=0; i < 10; i++ )
      {
	v1.addElement( Integer.toString( i ) );
	v2.addElement( Integer.toString( 20-i ) );
      }

    JPropertyPanel jpp = new JPropertyPanel(frame, true);

    frame.getContentPane().add(jpp, BorderLayout.CENTER);

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
 * TableModel for the JPropertyPanel
 */

class JPropertyPanelTM extends AbstractTableModel {

  final static boolean debug = false;
  private boolean editable;

  ArrayList rows = new ArrayList();

  /* -- */

  public JPropertyPanelTM(boolean editable)
  {
    this.editable = editable;
  }
  
  public String getColumnName(int col)
  {
    switch (col)
      {
	case 0: return "Property";
	case 1: return "Value";
      }

    return "Huh?";
  }

  public void setValueAt(Object value, int row, int col)
  {
    if (debug)
      {
	System.out.println("setValueAt(" + value + ", " + row + ", " + col + ")");
      }

    ArrayList x = (ArrayList) rows.get(row);

    if (col > x.size()-1)
      {
	x.add(value);
      }
    else
      {
	x.set(col, value);
      }

    fireTableCellUpdated(row, col);
  }

  public Object getValueAt(int row, int col)
  {
    if (debug)
      {
	System.out.println("getValueAt(" + row + ", " + col + ")");
      }

    ArrayList x = (ArrayList) rows.get(row);

    if (col > x.size()-1)
      {
	return null;
      }

    return x.get(col);
  }

  public int getRowCount()
  {
    return rows.size();
  }

  public int getColumnCount()
  {
    return 2;
  }

  public Class getColumnClass(int col)
  {
    return java.lang.String.class;
  }

  public boolean isCellEditable(int row, int col)
  {
    return editable;
  }

  /**
   * <p>This method is a custom addition, used to create a new
   * property row with property name 'key'.  The new row will be
   * created at the bottom of the JPropertyPanel, and the table will
   * be notified to update itself.</p>
   */

  public void addRow(String key)
  {
    if (debug)
      {
	System.out.println("addRow(" + key + ")");
      }

    ArrayList x = new ArrayList();
    x.add(key);
    rows.add(x);

    int row = rows.size()-1;
    fireTableRowsInserted(row, row);
  }

  /**
   * <p>This method is a custom addition, used to delete a 
   * property row by row index.  The table will be notified to
   * update itself after deleting the row.</p>
   */

  public void deleteRow(int row)
  {
    rows.remove(row);
    fireTableRowsDeleted(row, row);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                               JPropertyPanelTML

------------------------------------------------------------------------------*/

/**
 * TableModelListener for the JPropertyPanel
 */

class JPropertyPanelTML implements TableModelListener {

  public void tableChanged(TableModelEvent event)
  {
    int row = event.getFirstRow();
    int col = event.getColumn();
    JPropertyPanelTM model = (JPropertyPanelTM) event.getSource();
    String columnName = model.getColumnName(col);

    if (col >= 0)
      {
	Object data = model.getValueAt(row, col);

	System.out.println("Change event: " + row + ", " + col + ": " + String.valueOf(data));
      }
    else
      {
	System.out.println("Row event: " + row);
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                               JPropertyPanelLSL

------------------------------------------------------------------------------*/

/**
 * ListSelectionListener for the JPropertyPanel
 */

class JPropertyPanelLSL implements ListSelectionListener {

  JPropertyPanel parent;

  public JPropertyPanelLSL(JPropertyPanel parent)
  {
    this.parent = parent;
  }

  public void valueChanged(ListSelectionEvent event)
  {
    if (event.getValueIsAdjusting())
      {
	return;
      }

    System.err.println("Selection change: " + event.toString());
    ListSelectionModel model = (ListSelectionModel) event.getSource();
    parent.setSelection(!model.isSelectionEmpty());
  }
}
