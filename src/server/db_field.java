/*

   db_field.java

   A db_field is an item in a db_object.  A db_field can be a vector
   or a scalar.  

   Created: 10 April 1996
   Version: $Revision: 1.11 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;
import java.util.*;

public interface db_field extends java.rmi.Remote {

  FieldInfo getFieldInfo() throws RemoteException;

  String getName() throws RemoteException;
  short getID() throws RemoteException;
  String getComment() throws RemoteException;
  String getTypeDesc() throws RemoteException;
  short getType() throws RemoteException;
  short getDisplayOrder() throws RemoteException;
  String getValueString() throws RemoteException;
  String getEncodingString() throws RemoteException;

  boolean isDefined() throws RemoteException;
  boolean isVector() throws RemoteException;
  boolean isEditable() throws RemoteException;
  boolean isVisible() throws RemoteException;
  boolean isBuiltIn() throws RemoteException;
  boolean isEditInPlace() throws RemoteException;

  // for scalars

  Object getValue() throws RemoteException;
  boolean setValue(Object value) throws RemoteException;

  // for vectors

  int size() throws RemoteException;

  Vector getValues() throws RemoteException;
  Object getElement(int index) throws RemoteException;
  boolean setElement(int index, Object value) throws RemoteException;
  boolean addElement(Object value) throws RemoteException;
  boolean deleteElement(int index) throws RemoteException;
  boolean deleteElement(Object value) throws RemoteException;
}
