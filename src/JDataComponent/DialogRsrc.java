/* 
   Resource class for use with StringDialog.java
*/

package arlut.csd.JDialog;

import java.lang.String;
import java.util.*;
import java.awt.*;

import com.sun.java.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DialogRsrc

------------------------------------------------------------------------------*/

public class DialogRsrc {

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
   * @param string String to use as the label
   */

  public void addString(String string)
  {
    objects.addElement(new stringThing(string));
  }

  /**
   * 
   * Adds a labeled check box field
   *
   * @param string String to use as the label
   */
  
  public void addBoolean(String string)
  {
    objects.addElement(new booleanThing(string));
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
    System.out.println("adding choice, but not really");
    objects.addElement(new choiceThing(label, choices));
  }

  /**
   *
   * Adds a one line separator to the dialog
   *
   */
  public void addSeparator()
  {
    System.out.println("NOT Adding separator.. not using gjt");
    //    objects.addElement(new Separator());
  }

  /**
   *
   * Adds a text-hidden password string field to the dialog
   *
   * @param label String to use as label
   */
  public void addPassword(String label)
  {
    System.out.println("Adding password field");
    objects.addElement(new passwordThing(label));
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


