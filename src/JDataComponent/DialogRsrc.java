/* 
   Resource class for use with StringDialog.java
*/

package arlut.csd.JDialog;

import java.lang.String;
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

  private final boolean debug = false;

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
	    image = jdj.PackageResources.getImageResource(frame, imageName, frame.getClass());

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
    objects.addElement(new stringThing(label, value));
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
   * Adds a one line separator to the dialog
   *
   */
  public void addSeparator()
  {
    objects.addElement(new JSeparator());
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


