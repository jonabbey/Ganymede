/*

   dateThing.java

   Resource class for use with StringDialog.java
   
   Created: 13 October 1998
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDialog;

import java.util.Date;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       dateThing

------------------------------------------------------------------------------*/

public class dateThing implements java.io.Serializable {

  String stringLabel;
  Date currentDate;
  Date maxDate;

  /* -- */

  public dateThing(String label, Date currentDate, Date maxDate)
  {
    this.stringLabel = label;
    this.currentDate = currentDate;
    this.maxDate = maxDate;
  }

  public String getLabel()
  {
    return stringLabel;
  }

  public Date getDate()
  {
    return currentDate;
  }

  public Date getMaxDate()
  {
    return maxDate;
  }

}
