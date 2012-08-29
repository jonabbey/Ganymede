/*
   JentryField.java
   
   Created: 12 Jul 1996

   Module By: Navin Manohar

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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

package arlut.csd.JDataComponent;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JstringArea

------------------------------------------------------------------------------*/

/**
 * This is a multi-line, scrollable text component with support for
 * the {@link arlut.csd.JDataComponent.JsetValueCallback} listener
 * interface used for the arlut.csd.JDataComponent graphical
 * components.
 *
 * This component consists of a JScrollPane with an embedded {@link
 * arlut.csd.JDataComponent.myTextArea}.  The myTextArea object
 * handles keystroke filtering to prevent input of characters that do
 * not meet the acceptable characters constraints.
 *
 * You can control how arlut.csd.JDataComponent.JstringArea handles
 * scrolling by calling the {@link
 * javax.swing.JScrollPane#setHorizontalScrollBarPolicy(int)} and
 * {@link javax.swing.JScrollPane#setVerticalScrollBarPolicy(int)}
 * methods on JScrollPane, which we inherit from.
 */

public class JstringArea extends JScrollPane implements FocusListener {

  final static boolean debug = false;

  // ---

  public boolean allowCallback = false;

  protected boolean changed = false; 

  protected JsetValueCallback my_parent = null;

  private boolean processingCallback = false;

  private myTextArea textArea = null;

  String
    value = null,               // last known value, used in comparisons to see if we need to do full callback
    allowedChars = null,
    disallowedChars = null;

  /* -- */

  public JstringArea() 
  {
    this(0,0);  // Call to main contructor function
  }

  public JstringArea(int rows, int columns) 
  {
    // create myTextArea to put inside the JScrollPane, JScrollPane
    // fits to area size

    textArea = new myTextArea(this,rows,columns);

    if (debug)
      {
        System.out.println("Constructing pane with textarea, adding to JScrollPane: ");
      }

    // Add textArea to scrollPane viewport
    setViewportView(textArea);
    textArea.setVisible(true);
  } // JstringArea

  ///////////////////
  // Class Methods //
  ///////////////////

  /**
   *  returns true if the value in the JentryField has 
   *  been modified.
   */

  public boolean getChanged()
  {
    return changed;
  }

  public void setText(String s)
  {
    value = s;
    textArea.setText(s);
  }

  public String getText()
  {
    return textArea.getText();
  }

  public void setCaretPosition(int pos)
  {
    textArea.setCaretPosition(pos);
  }

  public void setAllowedChars(String s)
  {
    allowedChars = s;
  }

  public void setDisallowedChars(String s)
  {
    disallowedChars = s;
  }

  public void setEditable(boolean val)
  {
    if (my_parent != null)
      {
        allowCallback = val;
      }

    textArea.setEditable(val);
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

    allowCallback = textArea.isEditable();
  }

  /**
   * sendCallback is called when focus is lost.
   */

  public void sendCallback()
  {
    String str;
    
    /* -- */

    synchronized (this)
      {
        if (processingCallback)
          {
            return;
          }
        
        processingCallback = true;
      }

    try
      {
        // if nothing in the JstringArea has changed,
        // we don't need to worry about this event.
    
        str = getText();
    
        if (value != null)
          {
            if (debug)
              {
                System.err.println("JstringArea.sendCallback: old value != null");
              }
        
            changed = !value.equals(str);
          }
        else
          {
            if (debug)
              {
                System.err.println("JstringArea.sendCallback: old value == null");
              }

            if (str == null || str.equals(""))
              {
                changed = false;
              }
            else
              {
                changed = true;
              }
          }

        if (debug)
          {
            System.err.println("JstringArea.sendCallback(): str == '" + str + "', value == '" + value + "'");
          }
    
        if (!changed)
          {
            if (debug)
              {
                System.err.println("JstringArea.sendCallback: no change, ignoring");
              }
        
            return;
          }

        // if we don't need to handle callbacks, just accept the new
        // string value from the user and return
    
        if (!allowCallback) 
          {
            value = str;
            return;
          }

        boolean b = false;
          
        try 
          {
            if (debug)
              {
                System.err.println("JstringArea.sendCallback: making callback");
              }

            if (str.equals(""))
              {
                b = my_parent.setValuePerformed(new JSetValueObject(this, null));
              }
            else
              {
                b = my_parent.setValuePerformed(new JSetValueObject(this, str));
              }
          }
        catch (RemoteException re)
          {
          }

        // If the setValuePerformed callback failed, we'll revert the value to our last
        // approved value
    
        if (!b) 
          {
            if (debug)
              {
                System.err.println("JstringArea.sendCallback: setValue rejected");
                
                if (value == null)
                  {
                    System.err.println("JstringArea.sendCallback: resetting to empty string");
                  }
                else
                  {
                    System.err.println("JstringArea.sendCallback: resetting to " + value);
                  }
              }
            
            if (value == null)
              {
                textArea.setText("");
              }
            else
              {
                textArea.setText(value);
              }
            
            changed = false;
          }
        else 
          {
            if (debug)
              {
                System.err.println("JstringArea.sendCallback: setValue('" + str + "') accepted");
              }

            if (str.equals(""))
              {
                value = null;
              }
            else
              {
                value = str;
              }
                
            changed = false;
          }
      }
    finally
      {
        processingCallback = false;
      }
  }

  /**
   *  Stub function that is overriden in subclasses of JentryField
   *
   */

  boolean isAllowed(char ch)
  {
    if (disallowedChars != null)
      {
        if (disallowedChars.indexOf(ch) != -1)
          {
            if (debug)
              {
                System.err.println("JstringArea.isAllowed() rejecting char " + ch + " as disallowed");
              }

            return false;
          }
      }
    
    if (allowedChars != null)
      {
        if (allowedChars.indexOf(ch) == -1)
          {
            if (debug)
              {
                System.err.println("JstringArea.isAllowed() rejecting char " + ch + " as not allowed");
              }

            return false;
          }
      }
    
    return true;
  }
  
  public void focusLost(FocusEvent e)
  {
    if (debug)
      {
        System.out.println("JstringArea: focusLost");
      }

    sendCallback();
  }

  public void focusGained(FocusEvent e)
  {
    if (debug)
      {
        System.out.println("focusGained");
      }
  }

  /**
   *
   * Debug rig
   *
   */

  public static final void main(String argv[]) 
  {
    JFrame frame = new JFrame();

    JstringArea area = new JstringArea(5, 10);
    area.setText("Hello world");

    area.setDisallowedChars("asdf");
    frame.getContentPane().add(area);

    area.setCallback(new JsetValueCallback() {
      public boolean setValuePerformed(JValueObject o) {
        System.out.println("I got an o: " + o.getValue());
        return true;
      }
    });

    frame.pack();
    frame.setSize(200,200);
    frame.setVisible(true);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      myTextArea

------------------------------------------------------------------------------*/

/**
 * This is a custom subclass of {@link javax.swing.JTextArea} that
 * provides the inner gui component substrate for the optionally
 * scrollable {@link arlut.csd.JDataComponent.JstringArea}.
 *
 * By embedding this subclass in JstringArea, we can intercept event
 * processing from our superclass while still allowing JstringArea to
 * be an all-in-one component suitable for use in the Ganymede {@link
 * arlut.csd.ganymede.client.containerPanel}.
 */

class myTextArea extends JTextArea
{
  private JstringArea my_parent;

  /* -- */

  public myTextArea(JstringArea x, int rows, int cols)
  {
    super(rows,cols);
    this.my_parent = x;
    enableEvents(AWTEvent.KEY_EVENT_MASK);
    addFocusListener(x);
  }
  
  /**
   * We only want certain keystrokes to be registered by the field.
   *
   * This method overrides the processKeyEvent() method in JComponent and
   * gives us a way of intercepting characters that we don't want in our
   * string field.
   */

  protected void processKeyEvent(KeyEvent e)
  {
    // always pass through useful editing keystrokes.. this seems to be
    // necessary because backspace and delete apparently have defined
    // Unicode representations, so they don't match CHAR_UNDEFINED below

    switch (e.getKeyCode())
      {
      case KeyEvent.VK_BACK_SPACE:
      case KeyEvent.VK_DELETE:
      case KeyEvent.VK_END:
      case KeyEvent.VK_HOME:
        super.processKeyEvent(e);
        return; 
      }

    // We check against KeyEvent.CHAR_UNDEFINED so that we pass
    // through things like arrow keys, etc. that don't result in
    // character insertion

    if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED)
      {
        super.processKeyEvent(e);
        return;
      }

    // now check with the JstringArea to adjudicate this character
    // insertion

    if (my_parent.isAllowed(e.getKeyChar()))
      {
        super.processKeyEvent(e);
        return;
      }

    // otherwise, we ignore it

    if (JstringArea.debug)
      {
        System.err.println("JstringArea: skipping key event " + e);
      }
  } // processKeyEvent

} // mytextarea
