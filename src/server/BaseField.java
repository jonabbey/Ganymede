/*

   BaseField.java

   Remote interface allowing the client access to field typing information
   from a field.
   
   Created: 17 April 1997
   Version: $Revision: 1.17 $ %D%
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
  public boolean isRemovable() throws RemoteException;
  public boolean isBuiltIn() throws RemoteException;

  // general

  public Base getBase() throws RemoteException;
  public String getName() throws RemoteException;
  public String getClassName() throws RemoteException;
  public String getComment() throws RemoteException;
  public short getID() throws RemoteException;
  public short getDisplayOrder() throws RemoteException;

  // all of the setter methods below can only be called when a SchemaEdit
  // is in progress.

  public void setName(String name) throws RemoteException;
  public void setClassName(String name) throws RemoteException;
  public void setComment(String s) throws RemoteException;
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
  public boolean isPassword() throws RemoteException;
  public boolean isIP() throws RemoteException;

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
  public boolean isMultiLine() throws RemoteException;

  public void setMinLength(short val) throws RemoteException;
  public void setMaxLength(short val) throws RemoteException;
  public void setOKChars(String s) throws RemoteException;
  public void setBadChars(String s) throws RemoteException;
  public void setMultiLine(boolean b) throws RemoteException;

  // these two apply to strings, numbers, and IP addresses

  public String getNameSpaceLabel() throws RemoteException;

  /**
   * Note that this is intended to be called from the Schema Editor,
   * and won't take effect until the next time the system is stopped
   * and reloaded.
   */

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

  // password

  public boolean isCrypted() throws RemoteException;
  public void setCrypted(boolean b) throws RemoteException;
  
  // convenience methods

  /**
   *
   * This method is intended to produce a human readable
   * representation of this field definition's type attributes.  This
   * method should not be used programatically to determine this
   * field's type information.
   *
   * This method is only for human elucidation, and the precise
   * results returned are subject to change at any time.
   *
   */

  public String getTypeDesc() throws RemoteException;
}
