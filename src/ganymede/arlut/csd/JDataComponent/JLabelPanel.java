/*
   JLabelPanel.java

   This class defines a JPanel that contains stacked, labeled items.
   
   Created: 19 August 2004

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2006
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
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.util.HashMap;
import java.util.Iterator;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JLabelPanel

------------------------------------------------------------------------------*/

/**
 * This panel contains labeled GUI components in a vertical stack orientation.
 * Each GUI component may be made visible or invisible at will.
 *
 * All methods on a constructed JLabelPanel object should be called on the
 * GUI thread once the JLabelPanel has been added to a GUI container.
 *
 * @version $Id$
 * @author Jonathan Abbey
 */

public class JLabelPanel extends JPanel {

  private JPanel bPanel = null;
  private JPanel gPanel = null;
  private GridBagLayout gbl;
  private GridBagConstraints gbc;
  private int row = 0;
  private HashMap rowHash;
  private Font font = null;
  private float size = 0.0F;
  private int style = 0;

  /**
   * Insets for the left column in our JLabelPanel
   */

  private Insets leftInsets = new java.awt.Insets(0,0,0,0);

  /**
   * Insets for the right column in our JLabelPanel
   */

  private Insets rightInsets = new java.awt.Insets(0,0,0,0);

  /**
   * If true, we'll put in a JSpacer to keep the label cells
   * at a fixed size, even if certain rows are made invisible.
   */

  private boolean enforceFixedSize = false;
  private int maximumLabelWidth = 0;
  private JSpacer spacer = null;

  /* -- */

  public JLabelPanel()
  {
    super();
  }

  public JLabelPanel(boolean doubleBuffer)
  {
    super(doubleBuffer);
  }

  private void setup()
  {
    // we want to be packed in the north west

    setLayout(new BorderLayout());

    bPanel = new JPanel();
    bPanel.setLayout(new BorderLayout());

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    gPanel = new JPanel();
    gPanel.setLayout(gbl);

    bPanel.add("West", gPanel);
    
    add("North", bPanel);

    row = 0;
    rowHash = new HashMap();
  }

  /**
   * If setFixedSizeLabelCells() is called with a true value, the
   * JLabelPanel will be configured so that it keeps the label column
   * wide enough to encompass all labels, whether they are on visible
   * rows or not.  If it is called with a false value (or not called
   * at all.. the default is off), the hiding or revealing of rows may
   * cause shifting of the horizontal position of the fields on the
   * right column.
   */

  public synchronized void setFixedSizeLabelCells(boolean tf)
  {
    if (gbl == null)
      {
	setup();
      }

    if (tf && !enforceFixedSize)
      {
	gbc.gridwidth = 1;
	gbc.gridy = row;
	gbc.weightx = 0.0;
	gbc.gridx = 0;
	spacer = new JSpacer(maximumLabelWidth, 0);
	gbl.setConstraints(spacer,gbc);
	gPanel.add(spacer);

	row = row + 1;
      }

    enforceFixedSize = tf;
    
    if (!enforceFixedSize)
      {
	gPanel.remove(spacer);
	spacer = null;
      }
  }

  /**
   * Sets standardized insets around each label and component to be
   * added to this JLabelPanel.
   */

  public synchronized void setInsets(int top, int left, int bottom, int right)
  {
    this.leftInsets = new java.awt.Insets(top,left,bottom,right);
    this.rightInsets = this.leftInsets;
  }


  /**
   * Sets standardized insets around each label and component to be
   * added to this JLabelPanel.
   */

  public synchronized void setInsets(java.awt.Insets insets)
  {
    if (insets == null)
      {
	this.leftInsets = new java.awt.Insets(0,0,0,0);
      }
    else
      {
	this.leftInsets = insets;
      }

    this.rightInsets = this.leftInsets;
  }

  /**
   * Sets standardized insets around each label to be added to this
   * JLabelPanel.
   */

  public synchronized void setLeftInsets(int top, int left, int bottom, int right)
  {
    this.leftInsets = new java.awt.Insets(top,left,bottom,right);
  }


  /**
   * Sets standardized insets around each label to be added to this
   * JLabelPanel.
   */

  public synchronized void setLeftInsets(java.awt.Insets insets)
  {
    if (insets == null)
      {
	this.leftInsets = new java.awt.Insets(0,0,0,0);
      }
    else
      {
	this.leftInsets = insets;
      }
  }

  /**
   * Sets standardized insets around each component to be added to
   * this JLabelPanel.
   */

  public synchronized void setRightInsets(int top, int left, int bottom, int right)
  {
    this.rightInsets = new java.awt.Insets(top,left,bottom,right);
  }

  /**
   * Sets standardized insets around each component to be added to
   * this JLabelPanel.
   */

  public synchronized void setRightInsets(java.awt.Insets insets)
  {
    if (insets == null)
      {
	this.rightInsets = new java.awt.Insets(0,0,0,0);
      }
    else
      {
	this.rightInsets = insets;
      }
  }


  /**
   * This method sets the style of all the labels in this label
   * panel.  The styles are taken from the {@link java.awt.Font
   * java.awt.Font} class's static int members, and include Font.BOLD,
   * Font.ITALIC, Font.PLAIN, or the sum of FONT.BOLD and
   * FONT.ITALIC.
   *
   * These are the old-school Java 1.1 styles, not the fancier Java
   * 1.2 stuff.
   */

  public void setFontStyle(int style)
  {
    setFontStyleSize(style, size);
  }

  /**
   * This method sets the size of all the labels in this label
   * panel.  If size is equal to 0.0, the default label font size
   * will be used instead.
   */

  public void setFontSize(float size)
  {
    setFontStyleSize(style, size);
  }

  /**
   * This method sets the size of all the labels in this label
   * panel.  If size is equal to 0.0, the default label font size
   * will be used instead.
   */

  public synchronized void setFontStyleSize(int style, float size)
  {
    this.size = size;
    this.style = style;

    JLabel newLabel = new JLabel();
    Font labelFont = newLabel.getFont();

    if (size == 0.0F)
      {
	this.font = labelFont;
      }
    else
      {
	this.font = labelFont.deriveFont(size);
      }

    this.font = this.font.deriveFont(style);
    
    setFont(this.font);
  }

  /**
   * Returns the point size of the labels in this JLabelPanel.
   */

  public synchronized float getFontSize()
  {
    if (this.size != 0.0F)
      {
	return this.size;
      }

    JLabel newLabel = new JLabel();
    Font labelFont = newLabel.getFont();
    return labelFont.getSize2D();
  }

  /**
   * Sets the font for all labels in this JLabelPanel.
   */

  public synchronized void setFont(Font font)
  {
    this.font = font;
    this.size = 0.0F;

    if (rowHash == null)
      {
	return;
      }

    Iterator it = rowHash.values().iterator();

    int maxSize = 0;

    while (it.hasNext())
      {
	JLabel label = (JLabel) it.next();
	label.setFont(font);
	label.invalidate();

	if (label.getPreferredSize().width > maxSize)
	  {
	    maxSize = label.getPreferredSize().width;
	  }
      }

    gPanel.invalidate();
    bPanel.invalidate();
    this.maximumLabelWidth = maxSize;

    if (spacer != null)
      {
	spacer.setSpacerSize(this.maximumLabelWidth, 0);
      }

    validate();
    repaint();
  }

  /**
   * For adding a labeled item.
   *
   * Each row that is added is placed below all rows above it.
   *
   * @param label The text to put in a label on the leftmost column for this row.  May be null.
   * @param comp The component to add in the right column, after the label, if any.
   */

  public void addRow(String label, Component comp)
  {
    addRow(label, comp, false, false, 1);
  }

  /**
   * For adding a labeled item that is to stretch horizontally
   * to fill the entire component column.
   *
   * Each row that is added is placed below all rows above it.
   *
   * @param label The text to put in a label on the leftmost column for this row.  May be null.
   * @param comp The component to add in the right column, after the label, if any.
   */

  public void addFillRow(String label, Component comp)
  {
    addRow(label, comp, true, false, 1);
  }

  /**
   * For adding a labeled item that is to stretch horizontally
   * to fill the entire panel, and more besides.
   *
   * Each row that is added is placed below all rows above it.
   *
   * @param label The text to put in a label on the leftmost column for this row.  May be null.
   * @param comp The component to add in the right column, after the label, if any.
   * @param colsWidth The number of columns the component comp will
   * be stretched out to cover.  Values greater than 1 may be used to
   * put component into its own right-edge alignment point, distinct
   * from other components added with differing colsWidth values
   */

  public void addFillRow(String label, Component comp, int colsWidth)
  {
    addRow(label, comp, true, false, colsWidth);
  }

  /**
   * For adding a component that spans the label and item columns.
   *
   * Each row that is added is placed below all rows above it.
   * @param comp The component to add.
   */

  public void addWideComponent(Component comp)
  {
    addRow(null, comp, false, true, 1);
  }

  /**
   * For adding a component that spans the label and item columns, and
   * that is to stretch horizontally to fill the entire panel.
   *
   * Each row that is added is placed below all rows above it.
   *
   * @param comp The component to add.
   */

  public void addWideFillComponent(Component comp)
  {
    addRow(null, comp, true, true, 1);
  }

  /**
   * For adding a component that spans the label and item columns, and
   * that is to stretch horizontally to fill the entire panel.
   *
   * Each row that is added is placed below all rows above it.
   *
   * @param comp The component to add.
   * @param colsWidth The number of columns the component comp will
   * be stretched out to cover.  Values greater than 1 may be used to
   * put component into its own right-edge alignment point, distinct
   * from other components added with differing colsWidth values
   */

  public void addWideFillComponent(Component comp, int colsWidth)
  {
    addRow(null, comp, true, true, colsWidth);
  }

  /**
   * Private worker method for adding a possibly labeled component
   * to this JLabelPanel.
   *
   * Each row that is added is placed below all rows above it.
   *
   * @param label The text to put in a label on the leftmost column for this row.  May be null.
   * @param comp The component to add in the right column, after the label, if any.
   * @param fill If true, the component will be allowed to stretch to
   * fill the remaining horizontal space in this panel.
   * @param wideComponent If true and if label is null, the comp will
   * be horizontally positioned starting in the label column rather
   * than the field column.
   */

  private synchronized void addRow(String label, Component comp, boolean fill, boolean wideComponent, int colsWidth)
  {
    if (rowHash == null || gPanel == null)
      {
	this.setup();
      }

    gbc.gridy = row;
    gbc.insets = this.leftInsets;

    if (label != null)
      {
	JLabel l = new JLabel(label);

	if (this.font != null)
	  {
	    l.setFont(font);
	  }

	rowHash.put(comp, l);

	gbc.anchor = GridBagConstraints.NORTHWEST;
	gbc.fill = GridBagConstraints.NONE;
	gbc.weightx = 0.0;
	gbc.gridx = 0;
	gbc.gridwidth = 1;
	gbl.setConstraints(l, gbc);
	gPanel.add(l);

	if (l.getPreferredSize().width > this.maximumLabelWidth)
	  {
	    this.maximumLabelWidth = l.getPreferredSize().width;

	    if (spacer != null)
	      {
		spacer.setSpacerSize(this.maximumLabelWidth, 0);
	      }
	  }
      }

    gbc.anchor = GridBagConstraints.WEST;

    if (fill)
      {
	gbc.fill = GridBagConstraints.HORIZONTAL;
      }
    else
      {
	gbc.fill = GridBagConstraints.NONE;
      }

    if (label == null && wideComponent)
      {
	gbc.gridx = 0;
      }
    else
      {
	gbc.gridx = 1;
      }

    gbc.insets = this.rightInsets;
    gbc.weightx = 0.0;
    gbc.gridwidth = colsWidth;

    gbl.setConstraints(comp, gbc);
    gPanel.add(comp);

    row = row + 1;
  }

  /**
   * For making a given row visible or invisible.  If b is set to
   * true, the given row will be made visible, if it is set to false,
   * the given row (and its label) will be made invisible, and the
   * JLabelPanel will pull any rows beneath it up to fill in the
   * space.
   *
   * Note that if a setFixedSizeLabelCells(true) call has been made
   * on this JLabelPanel, making a row invisible will not cause the
   * horizontal positioning of the second, field column to shift.
   */

  public synchronized void setRowVisible(Component comp, boolean b)
  {
    if (rowHash == null)
      {
	this.setup();
      }

    Component label = (Component) rowHash.get(comp);

    comp.setVisible(b);

    if (label != null)
      {
	label.setVisible(b);
      }
  }

  /**
   * Removes the given component and its label from this
   * JLabelPanel.
   */

  public synchronized void removeRow(Component comp)
  {
    if (rowHash == null)
      {
	return;			// nothing added
      }

    gPanel.remove(comp);

    Component label = (Component) rowHash.get(comp);

    if (label != null)
      {
	gPanel.remove(label);
      }
  }

  /**
   * Does dissolution of this JLabelPanel.  Useful to make sure we
   * don't keep hold of any lingering references to things.
   */

  public synchronized void cleanup()
  {
    if (rowHash != null)
      {
	rowHash.clear();
	rowHash = null;
      }

    removeAll();

    if (gPanel != null)
      {
	gPanel.removeAll();
	gPanel = null;
      }

    if (bPanel != null)
      {
	bPanel.removeAll();
	bPanel = null;
      }

    gbl = null;
    gbc = null;
    
    spacer = null;
  }
}
