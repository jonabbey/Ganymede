/*

   JInvidChooser.java

   A fancy custom JComboBox thing for Scalar Invid fields.

   Created: 26 October 1999

   Module By: Michael Mulvaney, Jonathan Abbey

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2011
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.MutableComboBoxModel;

import arlut.csd.JDataComponent.JPanelCombo;
import arlut.csd.JDataComponent.listHandle;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.Util.TranslationService;


/*------------------------------------------------------------------------------
                                                                           class
                                                                   JInvidChooser

------------------------------------------------------------------------------*/

/**
 * A GUI component for choosing an Invid for a scalar invid_field.
 *
 * @author Jonathan Abbey
 */

public class JInvidChooser extends JPanelCombo implements ActionListener, ItemListener {

  private final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.JInvidChooser");

  // ---

  JButton
    view;

  containerPanel
    cp;

  private short
    type;

  private boolean
    removedNone = false,
    allowNone = true;

  private listHandle
    noneHandle = new listHandle(ts.l("global.none"), null); // "<none>"

  JInvidChooserFieldEditor editor;

  /* -- */

  /**
   * @param parent The general or embedded object panel that contains us
   * @param objectType object type number, used to support creating a new
   * object by the use of the 'new' button if enabled.
   */

  public JInvidChooser(containerPanel parent, short objectType)
  {
    this(null, parent, objectType);
  }

  /**
   * @param objects A vector of {@link arlut.csd.JDataComponent.listHandle listHandle}
   * objects representing labeled Invid choices for the user to choose among.
   * @param parent The general or embedded object panel that contains us
   * @param objectType object type number, used to support creating a new
   * object by the use of the 'new' button if enabled.
   */

  public JInvidChooser(Vector objects, containerPanel parent, short objectType)
  {
    super(objects);

    cp = parent;
    type = objectType;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BorderLayout());

    // "View"
    view = new JButton(ts.l("global.view_button"));
    view.addActionListener(this);

    editor = new JInvidChooserFieldEditor(this);
    getCombo().setEditor(editor);
    getCombo().setEditable(true);

    getCombo().addItemListener(this);

    if (getSelectedInvid() == null)
      {
        view.setEnabled(false);
      }

    buttonPanel.add("West", view);

    // JPanelCombo already added the combo to the west.

    add("East", buttonPanel);
  }

  public Invid getSelectedInvid()
  {
    listHandle lh = (listHandle) getSelectedItem();

    if (lh == null)
      {
        return null;
      }

    if (editor != null && editor.theField != null)
      {
        if (!lh.toString().equals(editor.theField.getText()))
          {
            System.err.println("JInvidChooser: " + lh.toString() + " does not equal " +
                               editor.theField.getText());
            return null;
          }
      }

    return (Invid) lh.getObject();
  }

  /**
   * <p>Set the allowNone bit.</p>
   *
   * <p>If allowNone is true, then &lt;none&gt; will remain as a
   * choice in the chooser.  If it is false, &lt;none&gt; will only be
   * included in the beginning if nothing is set; it will be removed
   * as soon as anything is chosen.</p>
   */

  public void setAllowNone(boolean allow)
  {
    if (debug)
      {
        System.out.println("JInvidChooser: setAllowNone(" + allow +")");
      }

    // If we used to allow, but now we don't, we need to take out the
    // noneHandle if it is not selected.

    if (allowNone && (!allow) && (!removedNone))
      {
        Object item = getCombo().getSelectedItem();

        if ((item != null) && (!item.equals(noneHandle)))
          {
            if (debug)
              {
                System.out.println("taking out <none>");
              }

            try
              {
                getCombo().removeItem(noneHandle);
                removedNone = true;

                if (debug)
                  {
                    System.out.println("+setting removedNone to true");
                  }
              }
            catch (IllegalArgumentException ia)
              {
                // none handle wasn't in there...
                removedNone = false;
              }
          }
        else if (debug)
          {
            System.out.println("<none> is selected, I will wait.");
          }
      }

    // Now if we are allowing none, but we weren't before, and we took
    // the none handle out, we have to put it back in

    if (removedNone && allow && !allowNone)
      {
        boolean found = false;

        for (int i = 0; i < getCombo().getItemCount(); i++)
          {
            if (getCombo().getItemAt(i).equals(noneHandle))
              {
                found = true;
                break;
              }
          }

        if (!found)
          {
            if (debug)
              {
                System.out.println("Putting none back in.");
              }

            getCombo().addItem(noneHandle);
          }

        removedNone = false;

        if (debug)
          {
            System.out.println("+setting removedNone to false");
          }
      }

    allowNone = allow;
  }

  /**
   * <p>Get the allowNone bit.</p>
   *
   * <p>If allowNone is true, then &lt;none&gt; will remain as a
   * choice in the chooser.  If it is false, &lt;none&gt; will only be
   * included in the beginning if nothing is set; it will be removed
   * as soon as anything is chosen.</p>
   */

  public boolean isAllowNone()
  {
    return allowNone;
  }

  /**
   * This method is used to change the dynamically label of an object in this
   * JInvidChooser.
   */

  public void relabelObject(Invid invid, String newLabel)
  {
    MutableComboBoxModel model = (MutableComboBoxModel) getModel();

    synchronized (model)
      {
        for (int i = 0; i < model.getSize(); i++)
          {
            listHandle lh = (listHandle) model.getElementAt(i);

            if (lh != null && lh.getObject() != null && lh.getObject().equals(invid))
              {
                model.removeElementAt(i);
                lh.setLabel(newLabel);
                model.insertElementAt(lh, i);
                repaint();
                break;
              }
          }
      }
  }

  public void setSelectedItem(Object o)
  {
    if (o == null && isAllowNone())
      {
        getCombo().setSelectedItem(noneHandle);
      }
    else
      {
        getCombo().setSelectedItem(o);
      }
  }

  /**
   * ItemListener method
   *
   * @see java.awt.event.ItemListener
   */

  public void itemStateChanged(ItemEvent e)
  {
    // keep non selection events to ourselves

    if (e.getStateChange() != ItemEvent.SELECTED)
      {
        return;
      }

    if (debug)
      {
        System.err.println("JInvidChooser.itemStateChanged(" + e.toString() + ")");
      }

    view.setEnabled(getSelectedInvid() != null);
  }

  /**
   * ActionListener method
   *
   * @see java.awt.event.ActionListener
   */

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == view)
      {
        listHandle lh = (listHandle) getSelectedItem();

        if (lh != null)
          {
            Invid invid = (Invid) lh.getObject();

            if (invid == null)
              {
                /* XXX I don't think this can ever occur.. */

                // "You don''t have permission to view {0}."
                showErrorMessage(ts.l("actionPerformed.permissions_error", lh));
              }
            else
              {
                cp.gc.viewObject(invid);
              }
          }
      }
  }

  private final void  showErrorMessage(String message) {
    cp.getgclient().showErrorMessage(message);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                        JInvidChooserFieldEditor

------------------------------------------------------------------------------*/

/**
 * A combobox editor class to provide intelligent keyboard handling for
 * the {@link arlut.csd.ganymede.client.JInvidChooser JInvidChooser} scalar
 * invid field gui component.
 */

class JInvidChooserFieldEditor extends KeyAdapter implements ComboBoxEditor, ActionListener {

  static final boolean debug = false;

  // ---

  JTextField theField;
  Object curItem;
  JComboBox box;
  JInvidChooser chooser;
  Vector actionListeners = new Vector();
  String lastGoodString = null;
  boolean lastGoodMatched = false;
  int lastGoodIndex = -1;

  /* -- */

  public JInvidChooserFieldEditor(JInvidChooser chooser)
  {
    this.chooser = chooser;
    this.box = chooser.getCombo();
    theField = new JTextField();
    theField.addKeyListener(this);
    theField.addActionListener(this);
  }

  public void setItem(Object anObject)
  {
    curItem = anObject;

    if (curItem != null)
      {
        String str;

        str = curItem.toString();
        theField.setText(str);
        lastGoodString = str;
        lastGoodMatched = true;
      }
    else
      {
        theField.setText("");
        lastGoodString = "";
        lastGoodMatched = false;
      }
  }

  public Component getEditorComponent()
  {
    return theField;
  }

  public Object getItem()
  {
    return box.getSelectedItem();
  }

  public void selectAll()
  {
    theField.selectAll();
  }

  public void addActionListener(ActionListener l)
  {
    actionListeners.addElement(l);
  }

  public void removeActionListener(ActionListener l)
  {
    actionListeners.removeElement(l);
  }

  /**
   * <p>Tap into the text field's key release to see if
   * we can complete the user's selection.  Note that
   * we are doing this without synchronizing on the text
   * field's own user interface.. to do this properly, we might
   * ought to be doing this with a document model on the text
   * field, but this works ok.  Since we're keying on key release,
   * we can expect to be called after the text field has processed
   * the key press.</p>
   */

  public void keyReleased(KeyEvent ke)
  {
    int curLen;
    String curVal;
    JButton viewButton = chooser.view;

    /* -- */

    curVal = theField.getText();

    if (curVal != null)
      {
        curLen = curVal.length();
      }
    else
      {
        curLen = 0;
      }

    // ignore arrow keys, delete, shift

    int keyCode = ke.getKeyCode();

    switch (keyCode)
      {
      case KeyEvent.VK_UP:
      case KeyEvent.VK_DOWN:
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_SHIFT:
        return;

      case KeyEvent.VK_DELETE:
      case KeyEvent.VK_BACK_SPACE:
        viewButton.setEnabled(false);
        return;
      }

    if (curLen > 0)
      {
        String item;
        int max = box.getItemCount();

        int matching = 0;
        String matchingItem = null;
        int matchingIndex = -1;

        for (int i = 0; i < max; i++)
          {
            item = box.getItemAt(i).toString();

            if (item.equals(curVal))
              {
                // found it

                lastGoodString = curVal;
                lastGoodMatched = true;
                lastGoodIndex = i;

                chooser.cp.gc.setWaitCursor();

                try
                  {
                    box.setSelectedIndex(lastGoodIndex);
                  }
                finally
                  {
                    chooser.cp.gc.setNormalCursor();
                  }

                viewButton.setEnabled(true);

                return;
              }
            else if (item.startsWith(curVal))
              {
                matching++;
                matchingItem = item;
                matchingIndex = i;
                lastGoodString = curVal;

                lastGoodIndex = -1;    // nothing definitively selected
              }
          }

        if (matching == 1)
          {
            lastGoodIndex = matchingIndex;
            lastGoodMatched = true;

            setItem(matchingItem); // will set lastGoodString

            chooser.cp.gc.setWaitCursor();

            try
              {
                box.setSelectedIndex(lastGoodIndex);
              }
            finally
              {
                chooser.cp.gc.setNormalCursor();
              }

            theField.select(curLen, matchingItem.length());

            viewButton.setEnabled(true);

            return;
          }
        else if (matching == 0)                // no match, don't let them have that char
          {
            // this is really kind of weak, since we're not actually
            // rejecting this with a document model, but it seems to
            // work..

            Toolkit.getDefaultToolkit().beep();

            if (lastGoodMatched)
              {
                chooser.cp.gc.setWaitCursor();

                try
                  {
                    box.setSelectedIndex(lastGoodIndex);
                  }
                finally
                  {
                    chooser.cp.gc.setNormalCursor();
                  }

                setItem(box.getSelectedItem());
                viewButton.setEnabled(true);
              }
            else
              {
                theField.setText(lastGoodString);

                viewButton.setEnabled(false);
              }
          }
        else
          {
            // too many matching, we don't yet have a unique prefix

            lastGoodMatched = false;
            lastGoodIndex = -1;
          }
      }
  }

  /**
   * Handle the user hitting return in the editable area.. if they hit return
   * without a reasonable value, revert the combo.
   */

  public void actionPerformed(ActionEvent e)
  {
    String value = theField.getText();
    String item;

    int max = box.getItemCount();

    boolean found = false;

    for (int i = 0; !found && i < max; i++)
      {
        item = box.getItemAt(i).toString();

        if (item.equals(value))
          {
            found = true;
            lastGoodIndex = i;

            chooser.cp.gc.setWaitCursor();

            try
              {
                box.setSelectedIndex(i);   // this will cause the combo box to send an update
              }
            finally
              {
                chooser.cp.gc.setNormalCursor();
              }
          }
      }

    if (!found)
      {
        if (lastGoodMatched)
          {
            chooser.cp.gc.setWaitCursor();

            try
              {
                box.setSelectedIndex(lastGoodIndex);
              }
            finally
              {
                chooser.cp.gc.setNormalCursor();
              }

            setItem(box.getSelectedItem());
            chooser.view.setEnabled(true);
          }
        else
          {
            theField.setText(lastGoodString);

            chooser.view.setEnabled(false);
          }
      }
  }
}

