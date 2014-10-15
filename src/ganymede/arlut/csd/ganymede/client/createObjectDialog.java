/*

   createObjectDialog.java

   This class is the dialog that is presented to the user when they go to
   create a new object in the client.

   Created: 17 September 1998

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import arlut.csd.JDataComponent.TimedKeySelectionManager;
import arlut.csd.JDataComponent.listHandle;
import arlut.csd.JDialog.StandardDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.rmi.Base;

/*------------------------------------------------------------------------------
                                                                           class
                                                              createObjectDialog

------------------------------------------------------------------------------*/

/**
 * <p>Dialog used to create new objects from the client's toolbar or
 * menu.  The dialog shows the user a list of object types and prompts
 * the user to pick an object type to create.  If the user selects one
 * and clicks ok, we'll try to create a new object for the user and
 * put up a window for the user to edit the new object with if we
 * succeed.</p>
 *
 * <p>This dialog is modal, and will block on the GUI thread until it
 * is closed.</p>
 *
 * @author Mike Mulvaney
 */

public class createObjectDialog extends StandardDialog implements ActionListener {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.createObjectDialog");

  static final String DEFAULT_CREATE = "creation_default_type";

  private boolean debug = false;

  private gclient
    gc;

  JComboBox
    types;

  JPanel
    middle;

  GridBagLayout
    gbl;

  GridBagConstraints
    gbc;

  ImageIcon
    icon;

  JLabel
    titleL,
    iconL;

  JButton
    ok,
    cancel;

  /* -- */

  public createObjectDialog(gclient client)
  {
    // "Create Object"
    super(client, ts.l("init.dialog_title"), StandardDialog.ModalityType.DOCUMENT_MODAL);
    this.gc = client;


    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    getContentPane().setLayout(new BorderLayout());
    middle = new JPanel(gbl);
    getContentPane().add(middle, BorderLayout.CENTER);
    gbc.insets = new Insets(4,4,4,4);

    icon = new ImageIcon(gc.createDialogImage);
    iconL = new JLabel(icon);
    iconL.setBorder(new EmptyBorder(10,15,10,15));

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbl.setConstraints(iconL, gbc);

    middle.add(iconL);

    // "Create Object"
    titleL = new JLabel(ts.l("init.dialog_text"), SwingConstants.CENTER);
    titleL.setFont(new Font("Helvetica", Font.BOLD, 14));
    titleL.setOpaque(true);
    titleL.setBorder(client.emptyBorder5);

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(titleL, gbc);
    middle.add(titleL);

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;

    // get list of types from gclient

    Vector<Base> bases = client.getBaseList();
    Hashtable<Base, Short> baseToShort = client.getBaseToShort();
    Hashtable<Base, String> baseNames = client.getBaseNames();

    Vector<listHandle> listHandles = new Vector<listHandle>();

    try
      {
        for (Base thisBase: bases)
          {
            String name = baseNames.get(thisBase);

            if (name == null)
              {
                name = thisBase.getName();
              }

            if (thisBase.isEmbedded())
              {
                continue;
              }

            if (thisBase.canCreate(null))
              {
                listHandle lh = new listHandle(name, (Short)baseToShort.get(thisBase));
                listHandles.add(lh);
              }
          }
      }
    catch (java.rmi.RemoteException rx)
      {
        throw new RuntimeException("Could not check to see if the base was creatable: " + rx);
      }

    listHandles = gc.sortListHandleVector(listHandles);

    types = new JComboBox(listHandles);
    types.setKeySelectionManager(new TimedKeySelectionManager());

    // see if we remember a type of object to create

    if (gclient.prefs != null)
      {
        String defaultType = gclient.prefs.get(DEFAULT_CREATE, null);

        if (defaultType != null)
          {
            for (listHandle handle: listHandles)
              {
                if (defaultType.equals(handle.getLabel()))
                  {
                    types.setSelectedItem(handle);
                    break;
                  }
              }
          }
      }

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 2;

    // "Object Type:"
    JLabel l = new JLabel(ts.l("init.type_label"));

    gbl.setConstraints(l, gbc);
    middle.add(l);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 2;
    gbl.setConstraints(types, gbc);
    middle.add(types);

    ok = new JButton(StringDialog.getDefaultOk());
    ok.addActionListener(this);

    cancel = new JButton(StringDialog.getDefaultCancel());
    cancel.addActionListener(this);

    if (isRunningOnMac())
      {
        JPanel macPanel = new JPanel();
        macPanel.setLayout(new BorderLayout());

        JPanel buttonP = new JPanel();

        buttonP.add(cancel);
        buttonP.add(ok);

        macPanel.add(buttonP, BorderLayout.EAST);
        getContentPane().add(macPanel, BorderLayout.SOUTH);
      }
    else
      {
        JPanel buttonP = new JPanel();

        buttonP.add(ok);
        buttonP.add(cancel);

        getContentPane().add(buttonP, BorderLayout.SOUTH);
      }

    setBounds(150,100, 200,100);

    pack();

    setVisible(true);
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == ok)
      {
        listHandle choice = (listHandle)types.getSelectedItem();

        if (gclient.prefs != null)
          {
            gclient.prefs.put(DEFAULT_CREATE, choice.getLabel());
          }

        Short type = (Short)choice.getObject();

        setVisible(false);

        if (type.shortValue() >= 0)
          {
            gc.createObject(type.shortValue());
          }
      }
    else if (e.getSource() == cancel)
      {
        setVisible(false);
      }
  }
}
