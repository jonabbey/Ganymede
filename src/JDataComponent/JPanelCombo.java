/*

   JPanelCombo.java

   A JComboBox in a panel.

   You can mostly just use JComboBox functions on it, and it will pass them along.

   But you should extend this, or it doesn't make much sense.
   
   Created: ? April 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDataComponent;

import com.sun.java.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

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
      }
    else
      {
	combo = new JComboBox(items);

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

    add("Center", combo);
  }

  public JComboBox getCombo()
  {
    if (combo == null)
      {
	combo = new JComboBox();
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

}
