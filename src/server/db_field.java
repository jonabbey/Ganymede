/*

   db_field.java

   A db_field is an item in a db_object.  A db_field can be a vector
   or a scalar.  

   Created: 10 April 1996
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;

public interface db_field extends java.rmi.Remote {

  String getName() throws RemoteException;
  short getId() throws RemoteException;
  String getComment() throws RemoteException;
  String getTypeDesc() throws RemoteException;
  short getType() throws RemoteException;
  boolean isVector() throws RemoteException;
  boolean isEditable() throws RemoteException;
  boolean isVisible() throws RemoteException;

  // for scalars

  Object getValue() throws RemoteException;
  boolean setValue(Object value) throws RemoteException;

  // for vectors

  int length() throws RemoteException;

  Object getElement(int index) throws RemoteException;
  boolean setElement(int index, Object value) throws RemoteException;
  boolean addElement(Object value) throws RemoteException;
  boolean deleteElement(int index) throws RemoteException;
}
