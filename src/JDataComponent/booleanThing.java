/*

   choiceThing.java

   Resource class for use with StringDialog.java
   
   Created: 16 June 1997
   Version: $Revision: 1.5 $ %D%
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     choiceThing

------------------------------------------------------------------------------*/

public class booleanThing implements java.io.Serializable {

  String booleanLabel;
  boolean Default;

  /* -- */

  public booleanThing(String label)
  {
    this(label, false);
  }

  public booleanThing(String label, boolean Default)
  {
    this.booleanLabel = label;
    this.Default = Default;
  }

  public String getLabel()
  {
    return booleanLabel;
  }

  public Boolean getDefault()
  {
    Boolean answer = new Boolean(Default);
    return answer;
  }

  public boolean getValue()
  {
    return Default;
  }
}
