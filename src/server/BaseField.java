/*

   BaseField.java

   Remote interface allowing the client access to field typing information
   from a field.
   
   Created: 17 April 1997
   Version: $Revision: 1.9 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                       BaseField

------------------------------------------------------------------------------*/

/**
 *
 * Client side interface definition for the Ganymede DBObjectBaseField
 * class.  This interface allows the client to query type information
 * remotely.
 * 
 */

public interface BaseField extends Remote {

  public boolean isEditable() throws RemoteException;

  // general

  public Base getBase() throws RemoteException;
  public String getName() throws RemoteException;
  public String getClassName() throws RemoteException;
  public String getComment() throws RemoteException;
  public byte getVisibility() throws RemoteException;
  public short getID() throws RemoteException;
  public short getDisplayOrder() throws RemoteException;

  public void setName(String name) throws RemoteException;
  public void setClassName(String name) throws RemoteException;
  public void setComment(String s) throws RemoteException;
  public void setVisibility(byte b) throws RemoteException;
  public void setID(short id) throws RemoteException;
  public void setDisplayOrder(short order) throws RemoteException;

  // type info

  public short getType() throws RemoteException;
  public boolean isBoolean() throws RemoteException;
  public boolean isNumeric() throws RemoteException;
  public boolean isDate() throws RemoteException;
  public boolean isString() throws RemoteException;
  public boolean isInvid() throws RemoteException;
  public boolean isPermMatrix() throws RemoteException;

  public void setType(short type) throws RemoteException;

  // vector info

  public boolean isArray() throws RemoteException;
  public short getMaxArraySize() throws RemoteException;

  public void setArray(boolean b) throws RemoteException;
  public void setMaxArraySize(short limit) throws RemoteException;

  // boolean

  public boolean isLabeled() throws RemoteException;
  public String getTrueLabel() throws RemoteException;
  public String getFalseLabel() throws RemoteException;

  public void setLabeled(boolean b) throws RemoteException;
  public void setTrueLabel(String label) throws RemoteException;
  public void setFalseLabel(String label) throws RemoteException;

  // string

  public short getMinLength() throws RemoteException;
  public short getMaxLength() throws RemoteException;
  public String getOKChars() throws RemoteException;
  public String getBadChars() throws RemoteException;
  public String getNameSpaceLabel() throws RemoteException;

  public void setMinLength(short val) throws RemoteException;
  public void setMaxLength(short val) throws RemoteException;
  public void setOKChars(String s) throws RemoteException;
  public void setBadChars(String s) throws RemoteException;
  public void setNameSpace(String s) throws RemoteException;
  
  // invid

  public boolean isEditInPlace() throws RemoteException;
  public void setEditInPlace(boolean b) throws RemoteException;

  public boolean isTargetRestricted() throws RemoteException;
  public boolean isSymmetric() throws RemoteException;
  public short getTargetBase() throws RemoteException;
  public short getTargetField() throws RemoteException;

  public void setTargetBase(short val) throws RemoteException;
  public void setTargetBase(String baseName) throws RemoteException;
  public void setTargetField(short val) throws RemoteException;
  public void setTargetField(String fieldName) throws RemoteException;
  
  // convenience methods

  public String getTypeDesc() throws RemoteException;
}
