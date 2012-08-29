/*
   JButtonPanel.java

   This class defines a JPanel that contains a row of buttons, with
   controllable justification within the panel.
   
   Created: 19 August 2004


   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.JDataComponent;

import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.Iterator;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    JButtonPanel

------------------------------------------------------------------------------*/

/**
 * <p>This panel contains a row of buttons, with controllable
 * justification within the panel.</p>
 *
 * <p>All methods on a constructed JButtonPanel object should be
 * called on the GUI thread once the JButtonPanel has been added to a
 * GUI container.</p>
 */

public class JButtonPanel extends JPanel {

  public static final int LEFT = -1;
  public static final int CENTER = 0;
  public static final int RIGHT = 1;

  // ---

  private JPanel gPanel = null;
  private GridBagLayout gbl;
  private GridBagConstraints gbc;
  private int justification = RIGHT;
  private int col = 0;
  private ArrayList list = null;

  /* -- */

  public JButtonPanel()
  {
    super();
  }

  public JButtonPanel(boolean doubleBuffer)
  {
    super(doubleBuffer);
  }

  public JButtonPanel(int justification)
  {
    super();

    setJustification(justification);
  }

  public JButtonPanel(int justification, boolean doubleBuffer)
  {
    super(doubleBuffer);

    setJustification(justification);
  }

  private void setJustification(int just)
  {
    if (just < LEFT || just > RIGHT)
      {
        throw new IllegalArgumentException();
      }

    this.justification = just;
  }

  private void setup()
  {
    setLayout(new BorderLayout());

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    col = 0;

    gPanel = new JPanel();
    gPanel.setLayout(gbl);

    switch (justification)
      {
      case LEFT:
        add("West", gPanel);
        break;

      case CENTER:
        add("Center", gPanel);
        break;

      case RIGHT:
        add("East", gPanel);
        break;
      }

    list = new ArrayList();
  }

  public synchronized void addButton(Component comp)
  {
    if (gPanel == null)
      {
        this.setup();
      }

    gbc.gridwidth = 1;

    switch (justification)
      {
      case LEFT:
        gbc.anchor = GridBagConstraints.WEST;
        break;

      case CENTER:
        gbc.anchor = GridBagConstraints.CENTER;
        break;

      case RIGHT:
        gbc.anchor = GridBagConstraints.EAST;
        break;
      }

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridx = col;
    gbc.gridy = 0;

    gbl.setConstraints(comp, gbc);

    gPanel.add(comp);

    col = col + 1;

    list.add(comp);
  }

  /**
   * <p>If everything that is in this panel are buttons, calling this
   * method will set an ActionListener on all buttons in the
   * panel.</p>
   */

  public synchronized void addListeners(ActionListener listener)
  {
    if (list == null)
      {
        return;
      }

    Iterator it = list.iterator();

    while (it.hasNext())
      {
        JButton b = (JButton) it.next();
        b.addActionListener(listener);
      }
  }

  public synchronized void cleanup()
  {
    removeAll();
    gbl = null;
    gbc = null;
    gPanel = null;
    list.clear();
    list = null;
  }
}
