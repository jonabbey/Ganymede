/*

   SchemaEdit.java

   Client side interface for schema editing
   
   Created: 17 April 1997
   Version: $Revision: 1.7 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                      SchemaEdit

------------------------------------------------------------------------------*/

/**
 *
 * Client side interface definition for the the Ganymede Schema Editing class.
 *
 */

public interface SchemaEdit extends Remote {

  /**
   *
   * Returns true if the schema editor is allowing
   * the 'constant' fields to be edited.  This is
   * provided solely so the Ganymede developers can
   * make incompatible changes to the 'constant' schema
   * items during development.
   *
   */

  public boolean isDevelopMode() throws RemoteException;

  /**
   *
   * When the server is in develop mode, it is possible to create new
   * built-in fields, or fields that can be relied on by the server
   * code to exist in every non-embedded object type defined.
   * 
   */

  public BaseField createNewBuiltIn() throws RemoteException;

  //
  // From here on are the normal schema editing methods
  //

  public Category getRootCategory() throws RemoteException;

  public Base[] getBases(boolean embedded) throws RemoteException;
  public Base[] getBases() throws RemoteException;

  public Base getBase(short id) throws RemoteException;
  public Base getBase(String baseName) throws RemoteException;

  public Base createNewBase(Category category, boolean embedded) throws RemoteException;
  public void deleteBase(Base b) throws RemoteException;

  public NameSpace[] getNameSpaces() throws RemoteException;
  public NameSpace getNameSpace(String spaceName) throws RemoteException;

  public NameSpace createNewNameSpace(String name, boolean caseInsensitive) throws RemoteException;
  public boolean deleteNameSpace(String name) throws RemoteException;

  public void commit() throws RemoteException;
  public void release() throws RemoteException;
}
