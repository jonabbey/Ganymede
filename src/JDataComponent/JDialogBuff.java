/*

   JDialogBuff.java

   Serializable resource class for use with StringDialog.java
   
   Created: 27 January 1998
   Release: $Name:  $
   Version: $Revision: 1.25 $
   Last Mod Date: $Date: 2000/11/04 03:42:41 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.JDialog;

import java.util.*;
import java.awt.Frame;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      JDialogBuff

------------------------------------------------------------------------------*/

/**
 * <P>This class is a serializable description of a dialog object that a server
 * is asking a client to present.</P>
 *
 * <P>If you don't need to send a dialog definition object across an RMI
 * link, you can just construct a {@link arlut.csd.JDialog.DialogRsrc DialogRsrc}
 * directly.</p>
 */

/*  (Note.. this is
 * semi-vestigal code, now, since we just use normal serialization to
 * have JDialogBuff transport its parameters, which could be done
 * directly with DialogRsrc just as well.  There are some things in
 * the DialogRsrc class, like the image cache, that we may not want
 * to mess with.  In any case, all of the Ganymede code expects JDialogBuff,
 * so it's going to stay for now.)
 */

public class JDialogBuff implements java.io.Serializable {

  static final boolean debug = false;

  // ---
  
  String title;
  StringBuffer text;
  String okText;
  String cancelText;
  String imageName;
  Vector resources;

  /* -- */

  // client side code

  public DialogRsrc extractDialogRsrc(Frame frame)
  { 
    DialogRsrc rsrc;

    /* -- */

    rsrc = new DialogRsrc(frame, title, text.toString(), okText, cancelText, imageName);

    rsrc.objects = resources;

    return rsrc;
  }

  // server-side constructors

  /**
   * Constructor for JDialogBuff
   *
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   *
   */

  public JDialogBuff(String Title, String Text)
  {
    this(Title, Text, "Ok", "Cancel", null);
  }

  /** 
   * Constructor with special "Ok" and "Cancel" strings
   *
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   * @param OK String for Ok button 
   * @param Cancel String for Cancel button
   */

  public JDialogBuff(String Title, String Text, String OK, String Cancel)
  {
    this(Title, Text, OK, Cancel, null);
  }

  /** 
   * Constructor with special "Ok" and "Cancel" strings
   *
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   * @param OK String for Ok button 
   * @param Cancel String for Cancel button
   * @param image Filename of image to display next to text
   */

  public JDialogBuff(String Title, String Text, String OK, String Cancel, String image)
  {
    this.title = Title;

    if (Text != null)
      {
	this.text = new StringBuffer();
	this.text.append(Text);
      }

    this.okText = OK;
    this.cancelText = Cancel;
    this.imageName = image;

    resources = new Vector();
  }

  /**
   * <p>Adds a labeled text field</p>
   *
   * @param string String to use as the label
   */

  public void addString(String string)
  {
    addString(string, (String)null);
  }

  /**
   * <p>Adds a labeled text field</p>
   *
   * @param string String to use as the label
   * @param value Initial value for string
   */

  public void addString(String string, String value)
  {
    resources.addElement(new stringThing(string, value, false));
  }

  /**
   * <p>Adds a labeled multi-line text field</p>
   *
   * @param string String to use as the label
   * @param value Initial value for string
   */

  public void addMultiString(String string, String value)
  {
    resources.addElement(new stringThing(string, value, true));
  }

  /**
   * <p>Adds a labeled check box field</p>
   *
   * @param string String to use as the label
   */
  
  public void addBoolean(String string)
  {
    addBoolean(string, false);
  }

  /**
   * <p>Adds a labeled check box field</p>
   *
   * @param string String to use as the label
   * @param value Initial value
   */
  
  public void addBoolean(String string, boolean value)
  {
    resources.addElement(new booleanThing(string, value));
  }

  /**
   * <p>Adds a labeled date field</p>
   *
   * @param label String to use as the label
   * @param currentDate Date to initialize the date field to
   * @param maxDate Latest date that the user may choose for this field.
   */
  
  public void addDate(String label, Date currentDate, Date maxDate)
  {
    resources.addElement(new dateThing(label, currentDate, maxDate));
  }

  /**
   * <p>Adds a choice field to the dialog</p>
   *
   * @param label String to use as the label
   * @param choices Vector of Strings to add to the choice 
   */
  
  public void addChoice(String label, Vector choices)
  {
    addChoice(label, choices, null);
  }

  /**
   * <p>Adds a choice field to the dialog</p>
   *
   * @param label String to use as the label
   * @param choices Vector of Strings to add to the choice 
   */
  
  public void addChoice(String label, Vector choices, String selectedValue)
  {
    resources.addElement(new choiceThing(label, choices, selectedValue));
  }

  /**
   * <p>Adds a text-hidden password string field to the dialog</p>
   *
   * @param label String to use as label
   */

  public void addPassword(String label)
  {
    addPassword(label, false);
  }

  /**
   * <p>Adds a text-hidden password string field to the dialog</p>
   *
   * @param label String to use as label
   * @param value Initial value
   */

  public void addPassword(String label, boolean isNew)
  {
    resources.addElement(new passwordThing(label, isNew));
  }

  /**
   * <p>Adds a newline and the provided text to the end of the text
   * encoded in this dialog.</p>
   */

  public void appendText(String text)
  {
    this.text.append("\n");
    this.text.append(text);
  }

  /**
   * <p>This is a convenience function for the server.</p>
   */

  public String getText()
  {
    if (text == null)
      {
	return null;
      }

    return text.toString();
  }
}
