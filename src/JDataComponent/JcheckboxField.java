
/*
   JcheckboxField.java

   
   Created: 12 Jul 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

*/


package arlut.csd.JDataComponent;

import com.sun.java.swing.*;

import java.awt.event.*;
import java.awt.*;

/*******************************************************************
                                                      JcheckboxField()

 This class defines a JcheckboxField.

*******************************************************************/
public class JcheckboxField extends JCheckbox implements ItemListener {

  private boolean allowCallback = false;
  private boolean changed = false; 

  private boolean isEditable = true;

  private JsetValueCallback callback = null;

  private JcomponentAttr valueAttr = null;

  private boolean value;
  private boolean oldvalue;
  
  private String label;

  private boolean notifyOnFocus = false;

  ///////////////////
  //  Constructors //
  ///////////////////

  /**
   * Constructor that can create a JcheckboxField with a label, a particular state,
   * and with the specified font/foreground/background value
   *
   * @param label the label to use for this JcheckboxField
   * @param state the state to which this JcheckboxField is to be set
   * @param valueAttr JcomponentAttr object used to specify font/foreground/background values
   */
  public JcheckboxField(String label,boolean state,JcomponentAttr valueAttr,boolean editable)
  {
    super(label);

    if (valueAttr == null)
      {
	valueAttr = new JcomponentAttr(null,new Font("Helvetica",Font.PLAIN,12),Color.black,Color.gray);
      }
    
    setValueAttr(valueAttr,true);

    value = state;
    oldvalue = state;
    
    setSelected(state);

    isEditable = editable;

    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    enableEvents(AWTEvent.FOCUS_EVENT_MASK); 

    addItemListener(this);
  }

  /** Constructor that creates a basic checkbox with default foreground and background
   *
   */
  public JcheckboxField()
  {
    super();

    this.valueAttr = new JcomponentAttr(null,getFont(),getForeground(),getBackground());
    
    value = false;
    oldvalue = false;
    
    setSelected(false);

    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    enableEvents(AWTEvent.FOCUS_EVENT_MASK);

    addItemListener(this);
  }
  
  /**
   *
   * @param label the label to use for this JcheckboxField
   * @param state the state to which this JcheckboxField is to be set
   * @param valueAttr JcomponentAttr object used to specify font/foreground/background values
   * @param parent the component which can use the value of this JcheckboxField
   *
   */
  public JcheckboxField(String label,boolean state,JcomponentAttr valueAttr,boolean editable,JsetValueCallback callback)
    {
      this(label,state,valueAttr,editable);

      setCallback(callback);
      
      this.value = state;
	
      this.oldvalue = state;

      setSelected(value);
    }

  ///////////////////
  // Class Methods //
  ///////////////////

  /**
   *  sets the parent of this component for callback purposes
   *
   */
  public void setCallback(JsetValueCallback callback)
  {
    if (callback == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: callback cannot be null");
      }
    
    this.callback = callback;

    allowCallback = true;
  }


  /**
   * sets the value back to what it was before it was
   * changed 
   */
  public void resetValue()
  {
    setValue(oldvalue);
  }

  /**
   * sets the value of this JcheckboxField to the boolean
   * value of state.
   * 
   */
  
  public void setValue(boolean state)
  {
    setSelected(state);
  }

  /**
   * returns the value represented by this JcheckboxField
   */
  public boolean getValue()
  {
    return value;
  }

  /**
   * returns true if the value of this JcheckboxField has changed
   * since it was initiallly created.
   */
  
  public boolean getChanged()
  {
    return changed;
  }

  /**
   * returns the JcomponentAttr object associated with this JcheckboxField
   *
   */
  public JcomponentAttr getValueAttr()
  {
    return this.valueAttr;
  }

 
  /**
   * sets the background color for the JentryField
   * and forces a repaint
   *
   * @param color the color which will be used
   */
  public void setBackground(Color color)
  {
    setValueBackColor(color,true);
  }

  
  /**
   * sets the background color for the JentryField
   *
   * @param color the color which will be used
   * @param repaint true if the value component needs to be repainted
   */
  public void setValueBackColor(Color color,boolean repaint)
  {
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
    if (attributes == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: attributes is null");
      }

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
   * sets the label of this JcheckboxField
   *
   * deprecated
   *
   */

  public void setLabel(String label)
  {
    this.label = label;

    super.setText(label);
  }

  /**
   * sets the label of this JcheckboxField
   *
   */

  public void setText(String label)
  {
    this.label = label;

    super.setText(label);
  }



  /**
   * sets the state of this JcheckboxField
   */
  public void setSelected(boolean state)
  {
    if (value != state)
      {
	changed = true;
      }

    this.value = state;
    super.setSelected(state);
  }

  /**
   *  sets the font for the JentryField and
   *  forces a repaint
   *
   * @param f the font which will be used
   */
  public void setFont(Font f)
  {
    setValueFont(f,true);
  }
  
  /**
   *  sets the font for the JentryField
   *
   * @param f the font which will be used
   * @param repaint true if the value component needs to be repainted
   */
  public void setValueFont(Font f,boolean repaint)
  {
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
    setValueForeColor(color,true);    
  }

  /**
   * sets the foreground color for the JentryField
   *
   * @param color the color which will be used
   * @param repaint true if the value component needs to be repainted
   */
  public void setValueForeColor(Color color,boolean repaint)
  {
    valueAttr.setForeground(color);

    setValueAttr(valueAttr,repaint);
  } 

  /**
   * processes any mouse events generated by this component
   *
   * @param e the MouseEvent that needs to be processed
   */
  protected void processMouseEvent(MouseEvent e)
  {
    // If this JcheckboxField is not editable, the event gets
    // consumed here, otherwise we let the parent handle it.

    if (!isEditable) 
      {
	e.consume();
	return;  
      }

    super.processMouseEvent(e);
  }

  /**
   *  Process any focus events that may be generated in this component
   *  
   * @param FocusEvent e the FocusEvent that needs to be processed
   */
  protected void processFocusEvent(FocusEvent e)
  {
    if (!notifyOnFocus)
      {
	return;
      }
    
    switch (e.getID()) 
      {
      
      case FocusEvent.FOCUS_LOST:
	{
	  if (!changed) 
	    {
	      break;
	    }

	  value = isSelected();
	  notify(value);

	  break;
	}

      case FocusEvent.FOCUS_GAINED:
	{
	}
    }

    super.processFocusEvent(e);
  }

  /**
   * @see java.awt.event.itemListener
   */  

  public void itemStateChanged(ItemEvent e)
  {
    notify(e.getStateChange() == ItemEvent.SELECTED);
  }

  private void notify(boolean value)
  {
    Boolean bval = new Boolean(value);

    if (allowCallback) 
      {
	// do a callback to talk to the server

	boolean b = false;

	try 
	  {
	    b = callback.setValuePerformed(new JValueObject(this,bval));
	  }
	catch (java.rmi.RemoteException ex) 
	  {
	    throw new RuntimeException("notify caught remote exception: " + ex);
	  }
	
	if (b==false)
	  {
	    resetValue();
	  }
	else
	  {
	    oldvalue = value;
	  }
      }
  }
}
/************************************************************/ 






