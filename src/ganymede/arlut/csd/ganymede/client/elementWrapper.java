/*

   elementWrapper.java

   This class will be used as a wrapper for each of the elements in an
   edit-in-place vector.  It contains plus and minus buttons that will
   allow a component to be deleted or a component to be added to the
   vector being displayed.

   Created: 16 October 1997

   Module By: Michael Mulvaney

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
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.rmi.RemoteException;

import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import arlut.csd.JDataComponent.JIPField;
import arlut.csd.JDataComponent.JSetValueObject;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.Invid;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  elementWrapper

------------------------------------------------------------------------------*/

/**
 * This GUI class is used in the Ganymede client to wrap embedded
 * objects in an expandable panel for inclusion in a presentation in a
 * {@link arlut.csd.ganymede.client.vectorPanel vectorPanel}.
 *
 * Typically, elementWrapper objects contain {@link
 * arlut.csd.ganymede.client.containerPanel containerPanels}, which
 * themselves contain GUI elements from embedded objects.
 */

class elementWrapper extends JPanel implements ActionListener, MouseListener, FocusListener {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.elementWrapper");

  boolean debug = false;

  // class variables

  private Component my_component = null;

  JPanel
    buttonPanel;

  JLabel
    title;

  String
    titleText;

  JButton
    expand,
    remove;

  vectorPanel
    vp;

  boolean
    editable,
    expanded = false,
    loaded = false;

  int index;

  long
    lastClick = 0;  // Used to determine double clicks

  /* -- */

  // class methods

  public elementWrapper(String titleText, Component comp, vectorPanel parent, boolean editable)
  {
    if (comp == null)
      {
        throw new IllegalArgumentException("Error: Component parameter is null");
      }

    this.titleText = titleText;
    this.vp = parent;
    this.editable = editable;
    this.my_component = comp;

    debug = vp.gc.debug;

    if (debug)
      {
        System.err.println("Adding new elementWrapper");
      }

    setLayout(new BorderLayout());

    buttonPanel = new JPanel();
    buttonPanel.setOpaque(true);

    /*
      // set the border/button panel color based on the
      // object's validity

      checkValidation();

      */

    buttonPanel.setBackground(ClientColor.vectorTitles);
    setBorder(vp.wp.eWrapperBorder);

    buttonPanel.setForeground(ClientColor.vectorFore);
    buttonPanel.setLayout(new BorderLayout());

    if (editable)
      {
        remove = new JButton(vp.wp.removeImageIcon);
        remove.setOpaque(false);
        remove.setMargin(new Insets(0,0,0,0));
        remove.setBorder(vp.wp.eWrapperButtonBorder);
        remove.setBorderPainted(false);

        // "Delete this element"
        remove.setToolTipText(ts.l("init.remove_tooltip"));
        remove.setContentAreaFilled(false);
        remove.addActionListener(this);
        remove.addFocusListener(this);
      }

    if (comp instanceof containerPanel)
      {
        if (titleText != null)
          {
            title = new JLabel(titleText);
          }
        else
          {
            // "Component"
            title = new JLabel(ts.l("init.default_label"));
          }

        title.setForeground(Color.white);
        title.addMouseListener(this);

        expand = new JButton(vp.wp.closeIcon);
        expand.setPressedIcon(vp.wp.closePressedIcon);
        expand.setMargin(new Insets(0,0,0,0));
        expand.setBorder(vp.wp.eWrapperButtonBorder);
        expand.setBorderPainted(false);

        // "Expand this element"
        expand.setToolTipText(ts.l("global.expand_tooltip"));
        expand.setOpaque(false);
        expand.addActionListener(this);
        expand.addFocusListener(this);
        expand.setContentAreaFilled(false);

        buttonPanel.add("West", expand);
        buttonPanel.add("Center", title);

        if (editable)
          {
            buttonPanel.add("East",remove);
          }
      }
    else
      {
        if (titleText != null)
          {
            buttonPanel.add("West", new JLabel(titleText));
          }

        buttonPanel.add("Center", comp);

        if (editable)
          {
            buttonPanel.add("East", remove);
          }
      }

    add("North",buttonPanel);
  }

  public void setIndex(int index)
  {
    this.index = index;

    // "{0,number,#}. {1}"
    title.setText(ts.l("setIndex.element_pattern", Integer.valueOf(index+1), titleText));
  }

  public Component getComponent()
  {
    return my_component;
  }

  public Invid getObjectInvid()
  {
    if (!(my_component instanceof containerPanel))
      {
        return null;
      }

    return ((containerPanel)my_component).getObjectInvid();
  }

  public Object getValue()
  {
    if (my_component instanceof containerPanel)
      {
        return getObjectInvid();
      }

    if (my_component instanceof JIPField)
      {
        return ((JIPField) my_component).getValue();
      }

    throw new RuntimeException("Unrecognized value wrapped in elementWrapper");
  }

  public void clearElement()
  {
    this.my_component = null;
  }

  /**
   * Checks to see if the object on the server wrapped by this elementWrapper
   * is complete and correct, and sets the color of the elementWrapper based
   * on this.
   *
   * THIS IS CURRENTLY NOT BEING USED.. NOT SURE IF THIS IS A DESIRABLE UI
   * FEATURE IN ANY CASE.
   */

  public void checkValidation()
  {
    if (my_component instanceof containerPanel)
      {
        try
          {
            setValidated(((containerPanel)my_component).getObject().isValid());
          }
        catch (Exception ex)
          {
            gclient.client.processExceptionRethrow(ex);
          }
      }
  }

  private void setValidated(boolean valid)
  {
    if (valid)
      {
        buttonPanel.setBackground(ClientColor.vectorTitles);
        setBorder(vp.wp.eWrapperBorder);
      }
    else
      {
        buttonPanel.setBackground(ClientColor.vectorTitlesInvalid);
        setBorder(vp.wp.eWrapperBorderInvalid);
      }
  }

  public void refreshTitle()
  {
    if (my_component instanceof containerPanel)
      {
        try
          {
            containerPanel cp = (containerPanel) my_component;
            titleText = cp.getObject().getEmbeddedObjectDisplayLabel();
            title.setText((index + 1) + ". " + titleText);
          }
        catch (Exception rx)
          {
            gclient.client.processExceptionRethrow(rx);
          }
      }
  }

  /**
   *  Expand this element wrapper.
   */

  public void open()
  {
    if (my_component instanceof containerPanel)
      {
        containerPanel myContainerPanel = (containerPanel) my_component;

        if (!loaded)
          {
            if (!myContainerPanel.isLoaded())
              {
                myContainerPanel.load();
              }

            add("Center", my_component);
            loaded = true;
          }
      }

    my_component.setVisible(true);

    expand.setIcon(vp.wp.openIcon);
    expand.setPressedIcon(vp.wp.openPressedIcon);

    // "Close this element"
    expand.setToolTipText(ts.l("global.shrink_tooltip"));
    expanded = true;
  }

  /**
   * Close this element wrapper.
   */

  public void close()
  {
    my_component.setVisible(false);
    expand.setIcon(vp.wp.closeIcon);
    expand.setPressedIcon(vp.wp.closePressedIcon);

    // "Expand this element"
    expand.setToolTipText(ts.l("global.expand_tooltip"));
    expanded = false;
  }

  /**
   * Toggle the elementWrapper open/closed.
   *
   * If the elementWrapper is open, this will close it.  If it is closed, this will open it.
   */

  public void toggle()
  {
    vp.wp.getgclient().setWaitCursor();

    if (debug)
      {
        System.err.println("toggle().");
      }

    if (expanded)
      {
        if (debug)
          {
            System.err.println("remove");
          }

        close();
      }
    else
      {
        open();
      }

    if (debug)
      {
        System.err.println("Done with toggle().");
      }

    vp.wp.getgclient().setNormalCursor();
  }

  // ActionListener methods ------------------------------------------------------

  public void actionPerformed(ActionEvent evt)
  {
    if (debug)
      {
        System.err.println("Action performed: " + evt.getActionCommand());
      }

    if (evt.getSource() == remove)
      {
        JValueObject v = new JSetValueObject(this,"remove");
        vp.setValuePerformed(v);
      }
    else if (evt.getSource() == expand)
      {
        toggle();
        invalidate();
        vp.container.frame.validate();
      }
    else
      {
        throw new RuntimeException("actionPerformed invoked by ActionEvent from invalid source");
      }
  }

  // MouseListener methods ------------------------------------------------------

  public void mouseClicked(MouseEvent e)
  {
    if (e.getWhen() - lastClick < 500)
      {
        toggle();
        invalidate();
        vp.container.frame.validate();
      }

    lastClick = e.getWhen();
  }

  public void mouseEntered(MouseEvent e)
  {
  }

  public void mouseExited(MouseEvent e)
  {
  }

  public void mousePressed(MouseEvent e)
  {
  }

  public void mouseReleased(MouseEvent e)
  {
  }

  // FocusListener methods ------------------------------------------------------

  public void focusLost(FocusEvent e)
  {
    JButton trigger = null;

    try
      {
        trigger = (JButton) e.getSource();
      }
    catch (ClassCastException ex)
      {
        return;
      }

    trigger.setBorderPainted(false);
    trigger.invalidate();
    repaint();
  }

  public void focusGained(FocusEvent e)
  {
    JButton trigger = null;

    try
      {
        trigger = (JButton) e.getSource();
      }
    catch (ClassCastException ex)
      {
        return;
      }

    trigger.setBorderPainted(true);
    trigger.invalidate();
    repaint();

    if (expanded)
      {
        scrollRectToVisible(trigger.getBounds());
      }
    else
      {
        scrollRectToVisible(this.getBounds());
      }
  }
}
