/*

   JIPField.java

   An IPv4/IPv6 data display / entry widget for Ganymede

   Created: 13 October 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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

package arlut.csd.JDataComponent;

import java.rmi.RemoteException;
import java.util.Vector;

import arlut.csd.ganymede.common.IPAddress;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        JIPField

------------------------------------------------------------------------------*/

/**
 * <p>This class is an IPv4/IPv6 data display/entry widget for
 * Ganymede.  Its purpose is to allow the viewing and editing of
 * either 4 or 16 byte Internet addresses and subnet masks.</p>
 */

public class JIPField extends JentryField {

  public static final boolean debug = false;

  public static int DEFAULT_COLS = 20;

  private static String IPv4allowedChars = "1234567890.";
  private static String IPv6allowedChars = "1234567890.abcdefABCDEF:";

  // --

  private String storedValue;
  private boolean allowV6;
  private boolean processingCallback = false;

  private boolean replacingValue = false;
  private IPAddress replacementAddr = null;


  /**  Constructors ***/

 /**
   * Base constructor for JIPField
   *
   * @param is_editable true if this JIPField is editable
   * @param allowV6 true is IPv6 format is allowed
   */

  public JIPField(boolean is_editable, boolean allowV6)
  {
    super(DEFAULT_COLS);

    this.allowV6 = allowV6;

    setEditable(is_editable);
  }

  /**
   * Constructor which uses default fonts,no parent,
   * default column size, and default foregound/background
   * colors.
   */

  public JIPField(boolean allowV6)
  {
    this(true, allowV6);
  }

  /**
   * Constructor that allows for the creation of a JIPField
   * that knows about its parent.
   *
   * @param callback An interface for the container within which this
   * JIPField is typically contained.  The JIPField will call this
   * interface to pass change notifications.
   */

  public JIPField(boolean is_editable,
                  JsetValueCallback callback,
                  boolean allowV6)
  {
    this(is_editable, allowV6);

    setCallback(callback);
  }

  /**
   * Returns the character located at position n in the JIPField value
   *
   * @param n position in the JIPField value from which to retrieve character
   */

  public char getCharAt(int n)
  {
    return this.getText().charAt(n);
  }

  /**
   * Sets the IP value held in this JIPField, without triggering a
   * callback update.
   */

  public void setValue(IPAddress address)
  {
    if (address == null)
      {
        setText("");
        storedValue = null;

        return;
      }

    setText(address.toString());
    storedValue = getText();

    return;
  }

  /**
   * Returns the current IP value held in this JIPField.
   */

  public IPAddress getValue()
  {
    String str;

    /* -- */

    str = getText();

    if (str == null || str.equals(""))
      {
        return null;
      }

    return new IPAddress(str);
  }

  /**
   * <p>When the JIPField loses focus, any changes made to the value
   * in the JIPField need to be propogated to the server.  This method
   * will handle that functionality.</p>
   *
   * <p>sendCallback is called when focus is lost, or when we are
   * otherwise triggered.</p>
   *
   * @return -1 on change rejected, 0 on no change required, 1 on change approved
   */

  public int sendCallback()
  {
    String str;
    IPAddress address;

    /* -- */

    synchronized (this)
      {
        if (processingCallback)
          {
            return -1;
          }

        processingCallback = true;
      }

    try
      {
        // if nothing in the JIPField has changed,
        // we don't need to worry about this event.

        str = getText();

        if ((storedValue != null && storedValue.equals(str)) ||
            (storedValue == null && (str == null || str.equals(""))))
          {
            return 0;
          }

        try
          {
            address = new IPAddress(str);

            // canonicalize the address entered

            setText(address.toString());
          }
        catch (IllegalArgumentException ex)
          {
            reportError(ex.getMessage());
            return -1;
          }

        try
          {
            if (debug)
              {
                System.err.println("JIPField.processFocusEvent: making callback");
              }

            if (!allowCallback || my_parent.setValuePerformed(new JSetValueObject(this, address)))
              {
                // handle modifications that were applied to us if
                // setValuePerformed() canonicalized otherwise
                // approved-but-modified the bytes that we suggested

                if (replacingValue)
                  {
                    address = replacementAddr;
                  }

                if (address == null)
                  {
                    storedValue = "";
                    setText(storedValue);
                  }
                else
                  {
                    storedValue = address.toString();
                    setText(storedValue);
                  }

                return 1;
              }
            else
              {
                if (storedValue == null)
                  {
                    setText("");
                  }
                else
                  {
                    setText(storedValue);
                  }

                return -1;
              }
          }
        catch (RemoteException re)
          {
            throw new RuntimeException("failure in callback dispatch: " + re);
          }
      }
    finally
      {
        processingCallback = false;
        replacingValue = false;
        replacementAddr = null;
      }
  }

  /**
   * This private method is used to report an error condition to the user.
   */

  private void reportError(String error)
  {
    if (allowCallback)
      {
        try
          {
            if (debug)
              {
                System.err.println("JIPField.processFocusEvent: making callback");
              }

            my_parent.setValuePerformed(new JErrorValueObject(this, error));
          }
        catch (RemoteException ex)
          {
            throw new RuntimeException("failure in error report: " + ex);
          }
      }
  }

  @Override public boolean isAllowed(char ch)
  {
    if (this.allowV6)
      {
        return IPv6allowedChars.indexOf(ch) != -1;
      }
    else
      {
        return IPv4allowedChars.indexOf(ch) != -1;
      }
  }

  /**
   * <p>This method is intended to be called if the
   * setValuePerformed() callback that we call out to decides that it
   * wants to substitute a replacement value for the value that we
   * asked to have validated.</p>
   *
   * <p>This is used to allow the server to reformat/canonicalize data
   * that we passed to it.</p>
   */

  public void substituteValueByCallBack(JsetValueCallback callback, IPAddress replacementValue)
  {
    if (callback != this.my_parent)
      {
        throw new IllegalStateException();
      }

    this.replacingValue = true;
    this.replacementAddr = replacementValue;
  }
}
