/*

   DialogRsrc.java

   Non-serializable Resource class for use with StringDialog.java
   
   Created: 16 June 1997
   Version: $Revision: 1.17 $
   Last Mod Date: $Date: 1999/01/22 18:03:54 $
   Release: $Name:  $

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

package arlut.csd.JDialog;

import java.util.*;
import java.awt.*;

import javax.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DialogRsrc

------------------------------------------------------------------------------*/

/**
 * This class is used to create a customized StringDialog.  
 *
 * <p>Use the various addXXX methods on this class to insert
 * the desired type of inputs, and then pass it to a StringDialog
 * constructor.  The order in which the addXXX methods are called
 * determines the layout order in the StringDialog.</p>
 *
 * Example:
 *
 * <code><blockquote><pre>
 * DialogRsrc r = new DialogRsrc(frame, "Simple dialog", "Give us some information:");
 * r.addString("Name:");
 * r.addBoolean("Married:");
 * 
 * StringDialog d = new StringDialog(r);
 * Hashtable result = d.DialogShow();
 * if (result == null) {
 *     // cancel was clicked...
 * } else {
 *     //process hashtable...
 * }
 * </pre></blockquote></code>
 *  @see StringDialog
 */

public class DialogRsrc {

  private final static boolean debug = false;

  /**
   * A static (persistent, shared) image cache used to translate
   * image names to Image objects.
   */

  static Hashtable imageCache = new Hashtable();

  // ---
  
  Frame frame;
  
  Vector objects;

  Image image;

  String 
    title,
    text;

  public String
    OKText,
    CancelText;

  /* -- */

  /**
   * Constructor for DialogRsrc
   *
   * @param frame Parent frame.
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   *
   */
  public DialogRsrc(Frame frame, String Title, String Text)
  {
    this(frame, Title, Text, "Ok", "Cancel", (Image) null);
  }

  /** 
   * Constructor with special "Ok" and "Cancel" strings
   *
   * @param frame Parent frame.
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   * @param OK String for Ok button 
   * @param Cancel String for Cancel button
   */

  public DialogRsrc(Frame frame, String Title, String Text, String OK, String Cancel)
  {
    this(frame, Title, Text, OK, Cancel, (Image) null);
  }

  /** 
   * Constructor with special "Ok" and "Cancel" strings
   *
   * @param frame Parent frame.
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   * @param OK String for Ok button 
   * @param Cancel String for Cancel button
   * @param image Image to display next to text
   */

  public DialogRsrc(Frame frame, String Title, String Text, String OK, String Cancel, Image image)
  {
    this.frame = frame;
    
    this.title = Title;
    this.text = Text;
    this.OKText = OK;
    this.CancelText = Cancel;
    this.image = image;
      
    objects = new Vector();
  }

  /** 
   * Constructor with special "Ok" and "Cancel" strings
   *
   * @param frame Parent frame.
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   * @param OK String for Ok button 
   * @param Cancel String for Cancel button
   * @param imageName Image to display next to text
   */

  public DialogRsrc(Frame frame, String Title, String Text, String OK, String Cancel, String imageName)
  {
    this.frame = frame;
    
    this.title = Title;
    this.text = Text;
    this.OKText = OK;
    this.CancelText = Cancel;

    if (imageName != null)
      {
	if (imageCache.containsKey(imageName))
	  {
	    image = (Image) imageCache.get(imageName);
	  }
	else
	  {
	    image = arlut.csd.Util.PackageResources.getImageResource(frame, imageName, frame.getClass());

	    imageCache.put(imageName, image);
	  }
      }
    else
      {
	this.image = null;
      }

    objects = new Vector();
  }

  /**
   *
   * Adds a labeled text field
   *
   * @param label String to use as the label
   * @param value Initial value of text field
   */

  public void addString(String label, String value)
  {
    objects.addElement(new stringThing(label, value, false));
  }

  /**
   *
   * Adds a labeled text field
   *
   * @param string String to use as the label
   */

  public void addString(String label)
  {
    addString(label, null);
  }

  /**
   *
   * Adds a labeled multi-line text field
   *
   * @param label String to use as the label
   * @param value Initial value of text field
   */

  public void addMultiString(String label, String value)
  {
    objects.addElement(new stringThing(label, value, true));
  }

  /**
   *
   * Adds a labeled multi-line text field
   *
   * @param string String to use as the label
   */

  public void addMultiString(String label)
  {
    addMultiString(label, null);
  }

  /**
   *
   * Adds a labeled date field
   *
   * @param string String to use as the label
   * @param currentDate What should the date be set to initially?
   * @param maxDate What is the latest the date can be set to?
   *
   */
  
  public void addDate(String label, Date currentDate, Date maxDate)
  {
    dateThing dt = new dateThing(label, currentDate, maxDate);
    objects.addElement(dt);
  }

  /**
   *
   * Adds a labeled date field
   *
   * @param string String to use as the label
   */

  public void addDate(String label)
  {
    addDate(label, null, null);
  }

  /**
   * 
   * Adds a labeled check box field
   *
   * @param string String to use as the label
   */
  
  public void addBoolean(String label)
  {
    addBoolean(label, false);
  }

  /**
   * 
   * Adds a labeled check box field
   *
   * @param string String to use as the label
   * @param value Initial value of field
   */
  
  public void addBoolean(String label, boolean value)
  {
    objects.addElement(new booleanThing(label, value));
  }

  /**
   *
   * Adds a choice field to the dialog
   *
   * @param label String to use as the label
   * @param choices Vector of Strings to add to the choice 
   * @param selectedItem Initially selected item
   */
  
  public void addChoice(String label, Vector choices, Object selectedItem)
  {
    objects.addElement(new choiceThing(label, choices, selectedItem));
  }

  /**
   *
   * Adds a choice field to the dialog
   *
   * @param label String to use as the label
   * @param choices Vector of Strings to add to the choice 
   */
  
  public void addChoice(String label, Vector choices)
  {
    addChoice(label, choices, null);
  }

  /**
   *
   * Adds a text-hidden password string field to the dialog
   *
   * @param label String to use as label
   */
  public void addPassword(String label)
  {
    addPassword(label, false);
  }

  /**
   *
   * Adds a text-hidden password string field to the dialog
   *
   * @param label String to use as label
   * @param isNew If true, password will have two fields for verification
   */
  public void addPassword(String label, boolean isNew)
  {
    if (debug)
      {
	System.out.println("Adding password field: "  + isNew);
      }

    objects.addElement(new passwordThing(label, isNew));
  }

  public Vector getObjects()
  {
    return objects;
  }

  public String getText()
  {
    return text;
  }
  
  public Image getImage()
  {
    return image;
  }

  /**
   *
   * Set the image to be displayed in upper left corner.
   *
   * @param newImage Image to display
   */

  public void setImage(Image newImage)
  {
    image = newImage;
  }

}//DialogRsrc


