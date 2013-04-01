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

import java.awt.Dialog;
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

import arlut.csd.JDataComponent.JMultiLineLabel;
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

  GridBagLayout
    gbl = new GridBagLayout();

  GridBagConstraints
    gbc = new GridBagConstraints();

  JButton
    ok,
    cancel;

  /* -- */

  public createObjectDialog(gclient client)
  {
    // "Create Object"
    super(client, ts.l("init.dialog_title"), StandardDialog.ModalityType.DOCUMENT_MODAL);

    this.gc = client;

    JPanel p = new JPanel(gbl);

    gbc.insets = new Insets(3,3,3,3);

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 2;
    JLabel image = new JLabel(new ImageIcon(gc.createDialogImage));
    gbl.setConstraints(image, gbc);
    p.add(image);

    gbc.gridheight = 1;
    gbc.ipadx = 8;
    gbc.ipady = 8;

    // "Choose the type of object\nyou wish to create"
    JMultiLineLabel text = new JMultiLineLabel(ts.l("init.dialog_text"));
    gbc.gridx = 1;
    gbc.gridwidth = 2;
    gbl.setConstraints(text, gbc);
    p.add(text);

    gbc.gridwidth = 1;
    gbc.ipadx = gbc.ipady = 0;

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

    types.setKeySelectionManager(new TimedKeySelectionManager());

    // Ideally, we'd really like for our JComboBox's pop-ups to be
    // able to go beyond the borders of our dialog.  Unfortunately,
    // the Swing library, up to and including Swing 1.1 beta 3, is
    // hideously broken when it comes to handling pop-ups in dialogs.

    // By leaving it lightweight, our pop-up will get truncated to the
    // dialog's edges, but at least it will be fully displayed, with a
    // scrollable menu region that fits within our dialog.

    // **  types.setLightWeightPopupEnabled(false);

    // "Type of object:"
    JLabel l = new JLabel(ts.l("init.type_label"));

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbl.setConstraints(l, gbc);
    p.add(l);

    gbc.gridx = 2;
    gbl.setConstraints(types, gbc);
    p.add(types);

    JPanel buttonP = new JPanel();
    ok = new JButton(StringDialog.getDefaultOk());
    ok.addActionListener(this);
    cancel = new JButton(StringDialog.getDefaultCancel());
    cancel.addActionListener(this);

    if (glogin.isRunningOnMac())
      {
        buttonP.add(cancel);
        buttonP.add(ok);
      }
    else
      {
        buttonP.add(ok);
        buttonP.add(cancel);
      }

    gbc.insets = new Insets(4,4,4,4);

    gbc.gridy = 3;
    gbc.gridx = 0;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.BOTH;
    javax.swing.JSeparator sep = new javax.swing.JSeparator();
    gbl.setConstraints(sep, gbc);
    p.add(sep);

    gbc.gridy = 4;
    gbc.gridx = 0;

    if (glogin.isRunningOnMac())
      {
        gbc.gridx = 2;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.EAST;
      }

    gbl.setConstraints(buttonP, gbc);
    p.add(buttonP);

    this.setContentPane(p);

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
