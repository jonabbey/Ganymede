  /////////////////////////////////////////////////////////////////////////

  /**
   *  This class will be used as a wrapper for each of the elements in the
   *  vector.  It contains plus and minus buttons that will allow a
   *  component to be deleted or a component to be added to the vector being
   *  displayed.
   *
   */ 

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

import arlut.csd.JDataComponent.*;

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
    lastClick = 0;  //Used to determine double clicks

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
	expand.setToolTipText("Expand this element");
	expand.setOpaque(false);
	expand.setBorderPainted(false);
	expand.setFocusPainted(false);
	expand.addActionListener(this);
	
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

      
    //add("Center",my_component);
    add("North",buttonPanel);
  }

  public Component getComponent() 
  {
    return my_component;
  }

  /**
   *  Expand this element wrapper.
   *
   * If makeOpen is true, expand(boolean) will open it or leave it open.
   * expand() is a misnomer; it is really a toggle.  Use expand(boolean) to
   * force open or closedness.
   */
  public void expand(boolean makeOpen)
  {
    expanded = !makeOpen;
    expand();
  }

  public void expand()
  {
    vp.wp.getgclient().setWaitCursor();;
    if (debug)
      {
	System.out.println("expand().");
      }

    if (expanded)
      {
	if (debug)
	  {
	    System.out.println("remove");
	  }

	//remove(my_component);
	my_component.setVisible(false);	
	expand.setIcon(vp.wp.closeIcon);
	expand.setToolTipText("Collapse this element");
	expanded = false;
      }
    else
      {
	if (! loaded)
	  {
	    setStatus("Loading container panel, you are just gonna have to wait.");
	    ((containerPanel)my_component).load();
	    add("Center", my_component);
	    loaded = true;
	  }
	else
	  {
	    my_component.setVisible(true);
	  }

	expand.setIcon(vp.wp.openIcon);
	expand.setToolTipText("Expand this element");
	expanded = true;
      }

    if(debug)
      {
	System.out.println("Done with expand().");
      }

    vp.wp.getgclient().setNormalCursor();;    
  }

  public void actionPerformed(ActionEvent evt) 
  {
    if (debug)
      {
	System.out.println("Action performed: " + evt.getActionCommand());
      }
    if (evt.getSource() == remove) 
      {
	JValueObject v = new JValueObject(getComponent(),"remove");
	vp.setValuePerformed(v);
      }
    else if (evt.getSource() == expand)
      {
	expand();
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
	expand();
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




