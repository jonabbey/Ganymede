/*

   db_object.java

   Base class for GANYMEDE client-visible objects. 
   
   Created: 11 April 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin */

package arlut.csd.ganymede;
import java.rmi.RemoteException;

public interface db_object extends java.rmi.Remote {

  public Invid getInvid() throws RemoteException;
  public db_field getField(String fieldname) throws RemoteException;
  public db_field[] listFields() throws RemoteException;
}
