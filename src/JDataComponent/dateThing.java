/*

   dateThing.java

   Resource class for use with StringDialog.java
   
   Created: 13 October 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDialog;

import java.util.Date;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       dateThing

------------------------------------------------------------------------------*/

public class dateThing {

  String stringLabel;
  Date maxDate;

  /* -- */

  public dateThing(String label, Date maxDate)
  {
    this.stringLabel = label;
    this.maxDate = maxDate;
  }

  public String getLabel()
  {
    return stringLabel;
  }

  public Date getMaxDate()
  {
    return maxDate;
  }

}
