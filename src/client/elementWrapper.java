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

class elementWrapper extends JPanel implements ActionListener, MouseListener {

  final static boolean debug = true;

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
    if (debug)
      {
	System.out.println("Adding new elementWrapper");
      }

    if (comp == null) 
      {
	throw new IllegalArgumentException("Error: Component parameter is null");
      }

    this.titleText = titleText;
    this.vp = parent;
    this.editable = editable;
    
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
	System.out.println("Adding mouse listener.");
	title.addMouseListener(this);

	expand = new JButton(vp.wp.closeIcon);
	expand.setToolTipText("Expand this element");
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

  
  public void expand()
  {
    System.out.println("expand().");
    if (expanded)
      {
	System.out.println("remove");
	//remove(my_component);
	my_component.setVisible(false);
	System.out.println("setIcon");
	expand.setIcon(vp.wp.closeIcon);
	System.out.println("setToolTipText");
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

    System.out.println("Done with expand().");
  }

  /*
  public void validate()
  {
    System.out.println("--Validate: element Wrapper: " + this);
    super.validate();
  }

  public void invalidate()
  {
    System.out.println("--inValidate: elementWrapper: " + this);
    super.invalidate();
  }

  public void doLayout()
  {
    System.out.println("//doLayout in elementW: " + this);
    super.doLayout();
    System.out.println("\\doLayout over in ew");
  }
  */

  /**
   *
   * This method does causes the hierarchy of containers above
   * us to be recalculated from the bottom (us) on up.  Normally
   * the validate process works from the top-most container down,
   * which isn't what we want at all in this context.
   *
   */

  public void invalidateRight()
  {
    Component c;

    //c = my_component;
    c = this;

    while ((c!= null) && !(c instanceof JViewport))
      {
	System.out.println("doLayout on: " + c);
	c.doLayout();
	c = c.getParent();

      }
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




