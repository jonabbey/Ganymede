/* 
   Resource class for use with StringDialog.java
*/

package arlut.csd.JDialog;

import java.lang.String;

public class stringThing {
  
  String stringLabel;
  String value;

  public stringThing(String label, String value)
    {
      this.value = value;
      this.stringLabel = label;
    }

  public String getLabel()
    {
      return stringLabel;
    }

  public String getValue()
    {
      return value;
    }

}
