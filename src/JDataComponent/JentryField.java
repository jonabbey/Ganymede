/*
   JentryField.java
   
   Created: 12 Jul 1996
   Version: 1.2 97/07/22
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.JDataComponent;

import java.awt.*;
import java.awt.event.*;

import com.sun.java.swing.*;
import com.sun.java.swing.text.*;

//---------------------------------------------------------------------
//                                                 class JentryField
//---------------------------------------------------------------------

/**
 *  JentryField serves as an abstract base class for all Fields that use textfields.
 *  The subclasses of this class should be used.
 */

abstract public class JentryField extends JTextField implements FocusListener{
  final boolean debug = false;

  public boolean allowCallback = false;
  protected boolean changed = false; 

  protected JsetValueCallback my_parent = null;

  protected JcomponentAttr valueAttr = null;

  //////////////////
  // Constructors //
  //////////////////

  public JentryField(int columns) 
  {
    super(columns);
    enableEvents(AWTEvent.KEY_EVENT_MASK);
    setEnabled(true);
    setEditable(true);

    addFocusListener(this);

    // disable tab insertion

    //    Keymap binding = getKeymap();
    //    binding.addActionForKeyStroke(KeyStroke.getKeyStroke('\t'),
    //				  new ("null action"));
  }

  ///////////////////
  // Class Methods //
  ///////////////////

  /**
   *
   * Hack to get a border around us.
   *
   */

  //  public void paint(Graphics g)
  //  {
  //    super.paint(g);
  //    g.setColor(Color.black);
  //    g.drawRect(0, 0, getBounds().width-1, getBounds().height-1);
  //  }

  /**
   *  returns true if the value in the JentryField has 
   *  been modified.
   */

  public boolean getChanged()
  {
    return changed;
  }

  /**
   *  returns a JcomponentAttr object for the JentryField
   */

  public JcomponentAttr getValueAttr()
  {
    return this.valueAttr;
  }
  
  /**
   *  sets the parent of this component for callback purposes
   *
   */

  public void setCallback(JsetValueCallback parent)
  {
    if (parent == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: parent cannot be null");
      }
    
    my_parent = parent;

    allowCallback = true;
  }

  /**
   * sendCallback is called when focus is lost.
   */
  public abstract void sendCallback();
    

  /**
   *  Stub function that is overriden is subclasses of JentryField
   *
   */

  private boolean isAllowed(char ch)
  {
    return true;
  }
  
  /**
   * sets the background color for the JentryField
   * and forces a repaint
   *
   * @param color the color which will be used
   */

  public void setBackground(Color color)
  {
    //setValueBackColor(color,true);
    super.setBackground(color);
  }

  
  /**
   * sets the background color for the JentryField
   *
   * @param color the color which will be used
   * @param repaint true if the value component needs to be repainted
   */

  public void setValueBackColor(Color color,boolean repaint)
  {
    if (valueAttr == null)
      {
	return;
      }
    
    valueAttr.setBackground(color);
    
    setValueAttr(valueAttr,repaint);
  }
  
  
  /**
   * sets the attributes for the JentryField
   *
   * @param attrib the attributes which will be used
   * @param repaint true if the label component needs to be repainted
   */

  public void setValueAttr(JcomponentAttr attributes,boolean repaint)
  {
    this.valueAttr = attributes;

    super.setFont(attributes.font);
    super.setForeground(attributes.fg);
    super.setBackground(attributes.bg);

    if (repaint)
      {
	this.repaint();
      }
  }

  /**
   *  sets the font for the JentryField and
   *  forces a repaint
   *
   * @param f the font which will be used
   */

  public void setFont(Font f)
  {
    //setValueFont(f,true);
    super.setFont(f);
  }
  
  /**
   *  sets the font for the JentryField
   *
   * @param f the font which will be used
   * @param repaint true if the value component needs to be repainted
   */

  public void setValueFont(Font f,boolean repaint)
  {
    if (valueAttr == null)
      {
	return;
      }
    
    valueAttr.setFont(f);

    setValueAttr(valueAttr,repaint);
  }

  /**
   * sets the foreground color for the JentryField
   * and forces a repaint.
   *
   * @param color the color which will be used
   */

  public void setForeground(Color color)
  {
    //setValueForeColor(color,true);    
    super.setForeground(color);
  }

  /**
   * sets the foreground color for the JentryField
   *
   * @param color the color which will be used
   * @param repaint true if the value component needs to be repainted
   */

  public void setValueForeColor(Color color,boolean repaint)
  {
    if (valueAttr == null)
      {
	return;
      }
    
    valueAttr.setForeground(color);

    setValueAttr(valueAttr,repaint);
  } 

  public void focusLost(FocusEvent e)
  {
    sendCallback();
  }

  public void focusGained(FocusEvent e) {}
}






