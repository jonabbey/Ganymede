/*
   JLabelPanel.java

   This class defines a JPanel that contains stacked, labeled items.
   preset.
   
   Created: 19 August 2004

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.util.HashMap;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JLabelPanel

------------------------------------------------------------------------------*/

/**
 * <p>This panel contains labeled GUI components in a vertical stack orientation.
 * Each GUI component may be made visible or invisible at will.</p>
 *
 * <p>All methods on a constructed JLabelPanel object should be called on the
 * GUI thread once the JLabelPanel has been added to a GUI container.</p>
 */

public class JLabelPanel extends JPanel {

  private GridBagLayout gbl;
  private GridBagConstraints gbc;
  private int row = 0;
  private HashMap rowHash;

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
    gbl = new GridBagLayout();
    setLayout(gbl);
    gbc = new GridBagConstraints();
    row = 0;
    rowHash = new HashMap();
  }

  public synchronized void addRow(String label, Component comp)
  {
    if (rowHash == null)
      {
	this.setup();
      }

    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridy = row;

    if (label != null)
      {
	JLabel l = new JLabel(label);

	rowHash.put(comp, l);

	gbc.fill = GridBagConstraints.NONE;
	gbc.weightx = 0.0;
	gbc.gridx = 0;
	gbl.setConstraints(l, gbc);
	add(l);
      }

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx = 1;

    gbl.setConstraints(comp, gbc);
    add(comp);

    row = row + 1;
  }

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

  public synchronized void cleanup()
  {
    rowHash.clear();
    rowHash = null;
    removeAll();
    gbl = null;
    gbc = null;
  }
}