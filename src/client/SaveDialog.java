/*

   SaveDialog.java

   Dialog for saving or mailing a table from dialog.
   
   Created: ??
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 1999/01/20 18:08:16 $
   Release: $Name:  $

   Module By: Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.ganymede.client;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import arlut.csd.JCalendar.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.JCenterDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      SaveDialog

------------------------------------------------------------------------------*/

public class SaveDialog extends JCenterDialog implements ActionListener {

  private final boolean debug = false;

  Date startDate;
  boolean
    addedFormatChoice = false,
    returnValue = false;
  
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints gbc = new GridBagConstraints();

  JButton 
    ok,
    cancel;

  JTextField
    subject = new JTextField(20),
    recipients = new JTextField(20);

  JComboBox
    formats = null;

  // This is the panel that holds everything, layed out by gbl
  JPanel 
    panel;

  formatButtonPanel
    formatPanel;

  historyInfoPanel
    historyInfo;

  Image
    saveImage = arlut.csd.Util.PackageResources.getImageResource(this, "SaveDialog.gif", getClass());

  SaveDialog(Frame owner, boolean forMail)
  {
    this(owner, forMail, true);
  }

  /**
   * Main Constructor.
   *
   * @param owner Parent frame
   * @param forMail If true, the dialog will show the recipients field and the ok button will say "mail".  Otherwise, it says "save".
   * @param showHistoryChoices If true, the dialog will show a checkbox for the history and a date field for choosing the starting date.
   */

  SaveDialog(Frame owner, boolean forMail, boolean showHistoryChoices)
  {
    super(owner, forMail ? "Send Mail" : "Save format", true);

    panel = new JPanel(gbl);

    gbc.insets = new Insets(6,6,6,6);
    
    if (showHistoryChoices)
      {

	historyInfo = new historyInfoPanel();
	gbc.gridx = 0;
	gbc.gridy = 0;
	gbc.gridwidth = 2;
	gbc.fill = GridBagConstraints.BOTH;
	gbl.setConstraints(historyInfo, gbc);
	panel.add(historyInfo);

	gbc.fill = GridBagConstraints.NONE;
      }

    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;

    if (forMail)
      {
	gbc.anchor = GridBagConstraints.WEST;

	gbc.gridy = 0;
	gbc.gridx = 0;
	gbc.gridheight = 2;
	JLabel image = new JLabel(new ImageIcon(saveImage));
	gbl.setConstraints(image, gbc);
	panel.add(image);

	gbc.gridheight = 1;
	gbc.gridx = 1;
	gbc.gridy = 0;
	gbc.fill = GridBagConstraints.NONE;
	JLabel rec = new JLabel("To:");
	gbl.setConstraints(rec, gbc);
	panel.add(rec);

	gbc.gridx = 2;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.weightx = 1;
	gbl.setConstraints(recipients, gbc);
	panel.add(recipients);


	gbc.gridy = 1;
	gbc.gridx = 1;
	gbc.weightx = 0;
	gbc.fill = GridBagConstraints.NONE;
	JLabel sub = new JLabel("Subject:");
	gbl.setConstraints(sub, gbc);
	panel.add(sub);

	gbc.gridx = 2;
	gbc.weightx = 1;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	subject.setText("Query report");
	gbl.setConstraints(subject, gbc);
	panel.add(subject);

      }

    // Row 3 is for the format choices

    arlut.csd.JDataComponent.JSeparator sep = new arlut.csd.JDataComponent.JSeparator();
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridy = 4;
    gbc.gridx = 0;
    gbl.setConstraints(sep, gbc);
    panel.add(sep);

    JPanel buttonPanel = new JPanel();
    ok = new JButton(forMail ? "Mail" : "Save");
    ok.addActionListener(this);

    cancel = new JButton("Cancel");
    cancel.addActionListener(this);

    buttonPanel.add(ok);
    buttonPanel.add(cancel);

    gbc.gridx = 0;
    gbc.gridy = 5;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(buttonPanel, gbc);
    panel.add(buttonPanel);

    setContentPane(panel);

    pack();
  }

  /**
   * Show the dialog.
   *
   * @return True if user pressed "Ok".
   *
   * <p>Use this instead of calling setVisible(true) yourself.  You need to get the boolean
   * return from this method, in order to know if the user pressed "Ok" or "Cancel".
   */

  public boolean showDialog()
  {
    setVisible(true);
    if (debug)
      {
	System.out.println("Returning " + returnValue);
      }

    return returnValue;
  }

  /**
   * True if "Show History" was chosen.
   */

  public boolean isShowHistory()
  {
    return historyInfo.isShowHistory();
  }

  /**
   * The start date for the history.  Makes sense only if isShowHistory() returns true.
   *
   */

  public Date getStartDate()
  {
    return historyInfo.getStartDate();
  }

  /**
   * String of recipients for the mail.
   *
   * This is not formatted in any way, so you get whatever the user typed in.
   */

  public String getRecipients()
  {
    return recipients.getText();
  }

  /**
   * Returns the text for the subject of the mail.
   *
   */
  public String getSubject()
  {
    return subject.getText();
  }

  /**
   * Set the choices for the format choices.
   *
   * @param choices Vector of Strings of the different choices.
   *
   * <P>Usually we send in a Vector of strings like "HTML", "Plain text", etc.</p>
   */
  public void setFormatChoices(Vector choices)
  {
    //formats = new JComboBox(choices);

    if (!addedFormatChoice)
      {
	addFormatChoiceButtons(choices);
      }
  }

  /**
   * Returns the choice of format.
   *
   * This will be one of the Strings in setFormatChoicse(), unless
   * something went horribly awry.
   */
  public String getFormat()
  {
    //return (String)formats.getSelectedItem();
    return formatPanel.getSelectedFormat();
  }

  // This one adds the format choice.
  private void addFormatChoice()
  {
    gbc.gridy = 3;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;

    JLabel l = new JLabel("Format:");
    gbl.setConstraints(l, gbc);
    panel.add(l);

    gbc.gridx = 1;

    gbl.setConstraints(formats, gbc);
    panel.add(formats);

    addedFormatChoice = true;
    pack();
  }

  // This really just adds in a new formatButtonPanel
  private void addFormatChoiceButtons(Vector choices)
  {
    gbc.gridy = 3;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 3;

    formatPanel  = new formatButtonPanel(choices);
    gbl.setConstraints(formatPanel, gbc);
    panel.add(formatPanel);

    addedFormatChoice = true;
    pack();
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == ok)
      {
	if (debug)
	  {
	    System.out.println("ok");
	  }


	returnValue = true;
	setVisible(false);
      }
    else if (e.getSource() == cancel)
      {
	if (debug)
	  {
	    System.out.println("cancel");
	  }

	returnValue = false;
	setVisible(false);
      }
  }

}

class historyInfoPanel extends JPanel implements ActionListener, JsetValueCallback{

  JCheckBox showHistory;
  JdateField date;
  
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints gbc = new GridBagConstraints();

  Date
    startDate = null;

  public historyInfoPanel()
  {
    setBorder(BorderFactory.createTitledBorder("History information"));
    setLayout(gbl);
    
    gbc.insets = new Insets(4,4,4,4);

    gbc.anchor = GridBagConstraints.WEST;

    showHistory = new JCheckBox();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbl.setConstraints(showHistory, gbc);
    add(showHistory);
    showHistory.addActionListener(this);
    
    JLabel showDateL = new JLabel("Show History");
    gbc.gridx = 0;
    gbl.setConstraints(showDateL, gbc);
    add(showDateL);
    
    JLabel startDateL = new JLabel("Starting Date");
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbl.setConstraints(startDateL, gbc);
    add(startDateL);
    
    date = new JdateField();
    date.setEnabled(false);
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbl.setConstraints(date, gbc);
    add(date);
  }

  public Date getStartDate()
  {
    return startDate;
  }

  public boolean isShowHistory()
  {
    return showHistory.isSelected();
  }

  public void actionPerformed(ActionEvent e)
  {

    date.setEnabled(showHistory.isSelected());
  }

  public boolean setValuePerformed(JValueObject o)
  {
    if (o.getValue() instanceof Date)
      {
	startDate = (Date)o.getValue();
	return true;
      }
    else
      {
	System.out.println("What is this: " + o);
      }

    return false;

  }


}

/*
 * This is the panel that holds the format radio buttons.
 */

class formatButtonPanel extends JPanel {

  Hashtable modelToLabel = new Hashtable();

  ButtonGroup
    group = new ButtonGroup();

  GridBagLayout
    layout = new GridBagLayout();

  GridBagConstraints
    constraints = new GridBagConstraints();

  formatButtonPanel(Vector choices)
  {
    setLayout(layout);

    String s;
    
    setBorder(BorderFactory.createTitledBorder("Format"));
    constraints.gridx = 0;
    constraints.anchor = GridBagConstraints.WEST;

    for (int i = 0; i < choices.size(); i++)
      {
	s = (String)choices.elementAt(i);

	JRadioButton b = new JRadioButton(s);

	if (i == 0)
	  {
	    b.setSelected(true);
	  }

	modelToLabel.put(b.getModel(), s);
	group.add(b);

	constraints.gridy = i;
	layout.setConstraints(b, constraints);
	add(b);
      }

    
  }


  public String getSelectedFormat()
  {
    return (String)modelToLabel.get(group.getSelection());
  }
}

