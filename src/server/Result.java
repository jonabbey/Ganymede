/*

   Result.java

   The result class is used to return labeled invid's for
   database queries.

   Result is serializable.
   
   Created: 21 October 1996 
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;

public class Result implements java.io.Serializable {
  
  Invid invid;	// remote reference to an object on the server
  String label = null;

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

  public Invid getInvid()
  {
    return invid;
  }

  // and hashCode

  public int hashCode()
  {
    return invid.hashCode();
  }
}
