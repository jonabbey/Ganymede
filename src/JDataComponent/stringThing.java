/* 
   Resource class for use with StringDialog.java
*/

package arlut.csd.JDialog;

import java.lang.String;

public class stringThing {

  String stringLabel;
  String value;
  boolean multiline;

  /* -- */

  public stringThing(String label, String value, boolean multiline)
  {
    this.value = value;
    this.stringLabel = label;
    this.multiline = multiline;
  }

  public String getLabel()
  {
    return stringLabel;
  }

  public String getValue()
  {
    return value;
  }

  public boolean isMultiline()
  {
    return multiline;
  }

}
