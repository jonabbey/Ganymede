/* 
   Resource class for use with StringDialog.java
*/

package arlut.csd.JDialog;

import java.lang.String;

public class passwordThing implements java.io.Serializable {
  
  String PWLabel;
  boolean isNew;

  public passwordThing(String label)
    {
      this(label, false);
    }

  /**
   * Constructor.
   *
   * @param label Label for this field
   * @param isNew If true, password will prompt for the password twice.
   */

  public passwordThing(String label, boolean isNew)
    {
      this.PWLabel = label;
      this.isNew = isNew;
    }

  public String getLabel()
    {
      return PWLabel;
    }

  public boolean isNew()
    {
      return isNew;
    }

}
