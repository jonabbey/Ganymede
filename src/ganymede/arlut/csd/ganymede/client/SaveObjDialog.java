/*

   SaveObjDialog.java

   Dialog for saving or mailing information about a DBObject from the
   Ganymede client, including optional history dump.

   Created: October 19, 1999

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
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

package arlut.csd.ganymede.client;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JdateField;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDialog.StandardDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.TranslationService;

import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.db_object;
import arlut.csd.ganymede.rmi.date_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   SaveObjDialog

------------------------------------------------------------------------------*/

/**
 * <p>Client dialog for saving or mailing an objects's XML
 * representation and/or a history dump.</p>
 */

public class SaveObjDialog extends StandardDialog implements ActionListener, JsetValueCallback {

  private static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.SaveObjDialog");

  private Date startDate;

  private boolean
    returnValue = false;

  private GridBagLayout gbl = new GridBagLayout();
  private GridBagConstraints gbc = new GridBagConstraints();

  private JCheckBox showTransactions;

  private JdateField date;

  private db_object server_object;

  private JButton
    ok,
    cancel;

  private JTextField
    subject = new JTextField(20),
    recipients = new JTextField(20);

  private JComboBox
    formats = null;

  // This is the panel that holds everything, layed out by gbl

  private JPanel
    panel;

  private Image
    saveImage = arlut.csd.Util.PackageResources.getImageResource(this, "SaveDialog.gif", getClass());

  /* -- */

  /**
   * Main Constructor.
   *
   * @param owner Parent frame
   * @param title The title for this dialog
   * @param historySave If true, we're saving the history for an
   * object.  If false, we're saving the XML for an object, and don't
   * need to ask about the full transaction, etc.
   * @param forMail If true, the dialog will show the recipients field
   * and the ok button will say "mail".  Otherwise, it says "save".
   * @param mailSubj The default subject, if we are mailing
   * @param object The server-side object we're saving data for.
   */

  SaveObjDialog(Frame owner, String title, boolean historySave, boolean forMail, String mailSubj, db_object object)
  {
    super(owner, title, StandardDialog.ModalityType.DOCUMENT_MODAL);

    this.server_object = object;

    panel = new JPanel(gbl);

    gbc.insets = new Insets(6,6,6,6);

    // on top the mail info

    if (forMail)
      {
        JPanel mailPanel = makeMailPanel();
        subject.setText(mailSubj);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(mailPanel, gbc);
        panel.add(mailPanel);

        // and a separator

        JSeparator sep = new JSeparator();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbl.setConstraints(sep, gbc);
        panel.add(sep);
      }

    // next the history info, if we will have it.

    if (historySave)
      {
        JPanel historyPanel = makeHistoryPanel();

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(historyPanel, gbc);
        panel.add(historyPanel);
      }

    // and finally the ok/cancel buttons

    JPanel buttonPanel = new JPanel();

    // "Mail"
    // "Save"
    ok = new JButton(forMail ? ts.l("init.mail_button") : ts.l("init.save_button"));
    ok.addActionListener(this);

    cancel = new JButton(StringDialog.getDefaultCancel());
    cancel.addActionListener(this);

    buttonPanel.add(ok);
    buttonPanel.add(cancel);

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(buttonPanel, gbc);
    panel.add(buttonPanel);

    // okay, we've got panel set up.. make it our content pane

    setContentPane(panel);

    pack();
  }

  /**
   * <p>Show the dialog.</p>
   *
   * <p>Use this instead of calling setVisible(true) yourself.  You
   * need to get the boolean return from this method, in order to know
   * if the user pressed "Ok" or "Cancel".</p>
   *
   * @return True if user pressed "Ok".
   */

  public boolean showDialog()
  {
    this.setLocationRelativeTo(getOwner());

    setVisible(true);

    return returnValue;
  }

  /**
   * True if we want to show complete transactions (including changes
   * made to objects other than this one) in the history dump we
   * include in our output.
   */

  public boolean isShowTransactions()
  {
    return showTransactions != null && showTransactions.isSelected();
  }

  /**
   * <p>The start date for the history.  Makes sense only if
   * isShowHistory() returns true.</p>
   */

  public Date getStartDate()
  {
    return startDate;
  }

  /**
   * <p>String of recipients for the mail.</p>
   *
   * <p>This is not formatted in any way, so you get whatever the user
   * typed in.</p>
   */

  public String getRecipients()
  {
    return recipients.getText();
  }

  /**
   * Returns the text for the subject of the mail.
   */

  public String getSubject()
  {
    return subject.getText();
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == ok)
      {
        returnValue = true;
        setVisible(false);
      }
    else if (e.getSource() == cancel)
      {
        returnValue = false;
        setVisible(false);
      }
  }

  public boolean setValuePerformed(JValueObject v)
  {
    this.startDate = (Date) v.getValue();

    return true;
  }

  private JPanel makeMailPanel()
  {
    JPanel panel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    /* -- */

    panel.setLayout(gbl);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridwidth = 1;

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridheight = 2;

    JLabel image = new JLabel(new ImageIcon(saveImage));
    gbl.setConstraints(image, gbc);
    panel.add(image);

    // add the recipients field

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridheight = 1;

    // "To:"
    JLabel rec = new JLabel(ts.l("makeMailPanel.to_label"));
    gbl.setConstraints(rec, gbc);
    panel.add(rec);

    gbc.gridx = 2;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;

    gbl.setConstraints(recipients, gbc);
    panel.add(recipients);

    // add the subject field

    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0;

    // "Subject:"
    JLabel sub = new JLabel(ts.l("makeMailPanel.subject_label"));
    gbl.setConstraints(sub, gbc);
    panel.add(sub);

    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;

    gbl.setConstraints(subject, gbc);
    panel.add(subject);

    return panel;
  }

  private JPanel makeHistoryPanel()
  {
    JPanel panel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    /* -- */

    // "Historical Information"
    panel.setBorder(BorderFactory.createTitledBorder(ts.l("makeHistoryPanel.border_title")));
    panel.setLayout(gbl);

    gbc.insets = new Insets(4,4,4,4);

    gbc.anchor = GridBagConstraints.WEST;

    // "Starting Date"
    JLabel startDateL = new JLabel(ts.l("makeHistoryPanel.start_date"));
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbl.setConstraints(startDateL, gbc);
    panel.add(startDateL);

    Date creation_date = null;

    try
      {
        date_field creation_date_field = server_object.getDateField(SchemaConstants.CreationDateField);

        if (creation_date_field != null)
          {
            creation_date = (Date) creation_date_field.getValue();
          }
      }
    catch (RemoteException ex)
      {
      }

    date = new JdateField(creation_date, true, true, false, creation_date, new Date(), this);
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbl.setConstraints(date, gbc);
    panel.add(date);

    // "Include Complete Transactions"
    showTransactions = new JCheckBox(ts.l("makeHistoryPanel.transaction_button"));
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbl.setConstraints(showTransactions, gbc);
    panel.add(showTransactions);

    return panel;
  }
}
