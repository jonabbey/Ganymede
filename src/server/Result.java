/*

   Result.java

   The result class is used to return labeled invid's for
   database queries.

   Result is serializable.
   
   Created: 21 October 1996 
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;

public class Result implements java.io.Serializable {
  
  db_object object;	// remote reference to an object on the server

  /* -- */

  public Result(db_object object)
  {
    this.object = object;
  }

  // toString() can't throw RemoteException because it is
  // defined in class Object without so doing

  public String toString()
  {
    try 
      {
	return object.getLabel();
      } 
    catch (RemoteException ex)
      {
	return "";
      }
  }

  // ditto equals

  public boolean equals(Object object)
  {
    try
      {
	if (object instanceof Result)
	  {
	    return ((Result) object).object.equals(object);
	  }
	else if (object instanceof db_object)
	  {
	    return ((db_object) object).equals(object);
	  }
	else
	  {
	    return false;
	  }
      }
    catch (Exception ex)
      {
	return false;
      }
  }

  // and hashCode

  public int hashCode()
  {
    try
      {
	return object.getInvid().hashCode();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught a remote exception: " + ex);
      }
  }

  public Invid getInvid() throws RemoteException
  {
    return object.getInvid();
  }
}
