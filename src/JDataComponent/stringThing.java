/*

   stringThing.java

   Resource class for use with StringDialog.java
   
   Created: 16 June 1997
   Version: $Revision: 1.5 $ %D%
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     stringThing

------------------------------------------------------------------------------*/

public class stringThing implements java.io.Serializable {

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
