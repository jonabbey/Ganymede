/*

   Result.java

   The result class is used to return labeled invid's for
   database queries.

   Result is serializable.
   
   Created: 21 October 1996 
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import arlut.csd.ganymede.Invid;

public class Result implements java.io.Serializable {
  
  Invid  invid;
  String label;

  /* -- */

  public Result(Invid invid, String label)
  {
    this.invid = invid;
    this.label = label;
  }

  public String toString()
  {
    return label;
  }

  public boolean equals(Object object)
  {
    if (object instanceof Result)
      {
	return ((Result) object).invid.equals(invid);
      }
    else if (object instanceof Invid)
      {
	return ((Invid) object).equals(invid);
      }
    else
      {
	return false;
      }
  }

  public int hashCode()
  {
    return invid.hashCode();
  }

  public Invid Invid()
  {
    return invid;
  }
}
