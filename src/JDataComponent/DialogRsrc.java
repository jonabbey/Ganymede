/* 
   Resource class for use with StringDialog.java
*/

package arlut.csd.Dialog;

import java.lang.String;
import java.util.*;
import java.awt.*;

import gjt.*;

public class DialogRsrc {
  
  Frame frame;
  
  Vector objects;

  String title;
  String text;
  public String OKText;
  public String CancelText;

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
      this(frame, Title, Text, "Ok", "Cancel");
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
      
      this.frame = frame;
      
      this.title = Title;
      this.text = Text;
      this.OKText = OK;
      this.CancelText = Cancel;
      
      objects = new Vector();
      
    }
  
  public void addString(String string)
    {
      objects.addElement(new stringThing(string));
    }
  
  public void addBoolean(String string)
    {
      objects.addElement(new booleanThing(string));
    }
  
  public void addChoice(String label, Vector choices)
    {
      System.out.println("adding choice, but not really");
      objects.addElement(new choiceThing(label, choices));
    }

  public void addSeparator()
    {
      System.out.println("Adding separator");
      objects.addElement(new Separator());
    }

  public Vector getObjects()
    {
      return objects;
    }

  public String getText()
    {
      return text;
    }
  
  
  
}//DialogRsrc


