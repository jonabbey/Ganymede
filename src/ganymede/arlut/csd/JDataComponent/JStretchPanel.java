/*
   JStretchPanel.java

   This class defines a JPanel that contains a single, centered, stretched
   component.
   
   Created: 20 August 2004

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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.util.HashMap;
import java.util.Iterator;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   JStretchPanel

------------------------------------------------------------------------------*/

/**
 * <p>This class defines a JPanel that contains a single, centered, stretched
 * component.</p>
 *
 * <p>All methods on a constructed JStretchPanel object should be called on the
 * GUI thread once the JStretchPanel has been added to a GUI container.</p>
 */

public class JStretchPanel extends JPanel {

  public JStretchPanel()
  {
    super();
    setup();
  }

  public JStretchPanel(boolean doubleBuffer)
  {
    super(doubleBuffer);
    setup();
  }

  private void setup()
  {
    setLayout(new BorderLayout());
  }

  public synchronized void setComponent(Component comp)
  {
    removeAll();
    add("Center", comp);
  }

  public synchronized void cleanup()
  {
    removeAll();
  }
}
