/*

   JPanelCombo.java

   A JComboBox in a panel.

   You can mostly just use JComboBox functions on it, and it will pass them along.

   But you should extend this, or it doesn't make much sense.
   
   Created: ? April 1998

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2004
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.JDataComponent;

import java.awt.BorderLayout;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JPanelCombo

------------------------------------------------------------------------------*/

public class JPanelCombo extends JPanel {

  //  myCellRenderer
  //renderer = new myCellRenderer();

  JComboBox 
    combo = null;

  public JPanelCombo()
  {
    this(null);
  }
  
  public JPanelCombo(Vector items)
  {
    setLayout(new BorderLayout());

    if (items == null)
      {
	combo = new JComboBox();
	combo.setKeySelectionManager(new TimedKeySelectionManager());
      }
    else
      {
	combo = new JComboBox(items);
	combo.setKeySelectionManager(new TimedKeySelectionManager());

	// the following try/catch is to workaround
	// a bug in Swing 1.0.2.

	try
	  {
	    combo.setSelectedIndex(0);
	  }
	catch (IllegalArgumentException ex)
	  {
	  }
      }

    add("West", combo);
  }

  public JComboBox getCombo()
  {
    if (combo == null)
      {
	combo = new JComboBox();
	combo.setKeySelectionManager(new TimedKeySelectionManager());
      }

    return combo;
  }

  // We need to pass just about everything on to the combo box

  public void addItem(Object item)
  {
    combo.addItem(item);
  }

  public void addItemListener(ItemListener l)
  {
    combo.addItemListener(l);
  }

  public Object getItemAt(int i)
  {
    return combo.getItemAt(i);
  }

  public int getItemCount()
  {
    return combo.getItemCount();
  }

  public int getMaximumRowCount() {
    return combo.getMaximumRowCount();
  }

  public ComboBoxModel getModel() {
    return combo.getModel();
  }

  public int getSelectedIndex() {
    return combo.getSelectedIndex();
  }

  public Object getSelectedItem() {
    return combo.getSelectedItem();
  }

  public Object[] getSelectedObjects() {
    return combo.getSelectedObjects();
  }

  public void insertItemAt(Object item, int index) {
    combo.insertItemAt(item, index);
  }

  public boolean isEditable() {
    return combo.isEditable();
  }

  public void removeAllItems() {
    combo.removeAllItems();
  }

  public void removeItemAt(int index) {
    combo.removeItemAt(index);
  }

  public void removeItemListener(ItemListener l) {
    combo.removeItemListener(l);
  }

  public void setEditable(boolean edit) {
    combo.setEditable(edit);
  }

  public void setEnabled(boolean enabled) {
    combo.setEditable(enabled);
  }

  public void setMaximumRowCount(int count) {
    combo.setMaximumRowCount(count);
  }

  public void setSelectedIndex(int index) {
    combo.setSelectedIndex(index);
  }

  public void setSelectedItem(Object o) {
    combo.setSelectedItem(o);
  }

  public void setVectorContents(Vector vect, Object selected)
  {
    combo.setModel(new DefaultComboBoxModel(vect));
    setSelectedItem(selected);
  }
}

