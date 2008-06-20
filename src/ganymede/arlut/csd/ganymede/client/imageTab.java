/*

   personaeTab.java

   This class manages the admin personae tab (for User objects) in the
   Ganymede client.
   
   Created: 17 November 2006
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey

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

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;

import java.awt.Image;

import java.net.URL;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.invid_field;

import arlut.csd.Util.PackageResources;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        imageTab

------------------------------------------------------------------------------*/

/**
 * This class manages the image tab (for User objects) in the Ganymede
 * client.
 */

public class imageTab extends clientTab {

  private ownerPanel owner_panel;
  private JPanel contentPane;
  private String url_string;
  private Image icon = null;
  private JLabel image = null;

  /* -- */

  public imageTab(framePanel parent, JTabbedPane pane, String tabName, String imageUrl)
  {
    super(parent, pane, tabName);
    this.url_string = imageUrl;
  }

  public JComponent getComponent()
  {
    if (contentPane == null)
      {
	contentPane = new JPanel(false);
        contentPane.setLayout(new FlowLayout());
      }

    return contentPane;
  }

  public void initialize()
  {
    loadImage();

    image = new JLabel(new ImageIcon(icon), SwingConstants.TRAILING);
    image.setOpaque(true);
    image.setBorder(BorderFactory.createEtchedBorder());

    contentPane.add(image);
  }

  public void update()
  {
  }

  public void loadImage()
  {
    if (icon != null)
      {
        return;
      }

    try
      {
        icon = PackageResources.getImageResource(contentPane, new URL(url_string));

        if (icon == null || icon.getWidth(null) <= 0)
          {
            icon = PackageResources.getImageResource(contentPane, "unknown.png", getClass());
          }
        else
          {
            icon = icon.getScaledInstance(200, -1, Image.SCALE_SMOOTH);
          }
      }
    catch (Exception ex)
      {
        // couldn't get the image for some reason
        ex.printStackTrace();
      }
  }

  public synchronized void dispose()
  {
    if (contentPane != null)
      {
	contentPane.removeAll();
	contentPane = null;
      }

    if (icon != null)
      {
        icon.flush();
        icon = null;
      }

    super.dispose();
  }
}
