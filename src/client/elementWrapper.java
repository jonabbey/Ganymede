/*

   elementWrapper.java

   This class will be used as a wrapper for each of the elements in
   the vector.  It contains plus and minus buttons that will allow a
   component to be deleted or a component to be added to the vector
   being displayed.
   
   Created: 16 October 1997
   Release: $Name:  $
   Version: $Revision: 1.23 $
   Last Mod Date: $Date: 1999/01/22 18:04:13 $
   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.ganymede.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  elementWrapper

------------------------------------------------------------------------------*/

/**
 * Each object in a vector panel is wrapped in an elementWrapper.  This class
 * controls the expanding of the element, and the creation of the containerPanel
 * inside.
 */

class elementWrapper extends JPanel implements ActionListener, MouseListener {
  
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

    debug = vp.gc.debug;

    if (debug)
      {
	System.out.println("Adding new elementWrapper");
      }
    
    setLayout(new BorderLayout());
    setBorder(vp.wp.eWrapperBorder);
      
    buttonPanel = new JPanel();
    buttonPanel.setOpaque(true);
    buttonPanel.setBackground(ClientColor.vectorTitles);
    buttonPanel.setForeground(ClientColor.vectorFore);

    buttonPanel.setLayout(new BorderLayout());

    if (editable)
      {
	remove = new JButton(vp.wp.removeImageIcon);
	remove.setBorderPainted(false);
	remove.setOpaque(false);
	remove.setFocusPainted(false);
	remove.setMargin(new Insets(0,0,0,0));
	remove.setToolTipText("Delete this element");
	remove.setContentAreaFilled(false);
	remove.addActionListener(this);
      }

    if (comp instanceof containerPanel)
      {
	if (titleText != null)
	  {
	    title = new JLabel(titleText);
	  }
	else
	  {
	    title = new JLabel("Component");
	  }
	
	title.setForeground(Color.white);
	title.addMouseListener(this);

	expand = new JButton(vp.wp.closeIcon);
	expand.setPressedIcon(vp.wp.closePressedIcon);
	expand.setToolTipText("Expand this element");
	expand.setOpaque(false);
	expand.setBorderPainted(false);
	expand.setFocusPainted(false);
	expand.addActionListener(this);
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

    my_component = comp;

    add("North",buttonPanel);
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

  public void checkValidation()
  {
    if (my_component instanceof containerPanel)
      {
	//setValidated(( (containerPanel)my_component ).getObject().isValid());
      }
  }

  private void setValidated(boolean valid)
  {
    if (valid)
      {
	buttonPanel.setBackground(ClientColor.vectorTitles);
      }
    else
      {
	buttonPanel.setBackground(ClientColor.vectorTitlesInvalid);
      }
  }

  public void refreshTitle()
  {
    if (my_component instanceof containerPanel)
      {
	try
	  {
	    containerPanel cp = (containerPanel)my_component;
	    String oldTitle = title.getText();
	    title.setText(oldTitle.substring(0, oldTitle.indexOf(".")) + ". " + cp.getObject().getLabel());
	  }
	catch (java.rmi.RemoteException rx)
	  {
	    throw new RuntimeException("RemoteException getting label: " + rx);
	  }
      }
  }

  /**
   *  Expand this element wrapper.
   *
   */

  public void open()
  {
    if (my_component instanceof containerPanel)
      {
	containerPanel myContainerPanel = (containerPanel) my_component;

	if (!loaded)
	  {
	    setStatus("Loading vector element.");

	    if (!myContainerPanel.isLoaded())
	      {
		myContainerPanel.load();
	      }

	    setStatus("Finished.");
	    add("Center", my_component);
	    loaded = true;
	  }
      }

    my_component.setVisible(true);

    expand.setIcon(vp.wp.openIcon);
    expand.setPressedIcon(vp.wp.openPressedIcon);
    expand.setToolTipText("Close this element");
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
    expand.setToolTipText("Expand this element");
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
	System.out.println("toggle().");
      }

    if (expanded)
      {
	if (debug)
	  {
	    System.out.println("remove");
	  }

	close();
      }
    else
      {
	open();
      }

    if (debug)
      {
	System.out.println("Done with toggle().");
      }

    vp.wp.getgclient().setNormalCursor();
  }

  public void actionPerformed(ActionEvent evt) 
  {
    if (debug)
      {
	System.out.println("Action performed: " + evt.getActionCommand());
      }

    if (evt.getSource() == remove) 
      {
	JValueObject v = new JValueObject(this,"remove");
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

  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}

  public final void setStatus(String status)
  {
    vp.wp.getgclient().setStatus(status);
  }
}




