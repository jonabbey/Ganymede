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
  String OKText;
  String CancelText;

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
      System.out.println("adding " + string);
      objects.addElement(new stringThing(string));
      
    }
  
  public void addBoolean(String string)
    {
      System.out.println("adding " + string);
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


