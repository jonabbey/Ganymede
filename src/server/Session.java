/*

   Session.java

   Client side remote interface.

   Client side interface definition for the Ganymede Session Object.  The
   Ganymede Session object holds the state for a Ganymede client's session
   with the Ganymede server.  The Ganymede session will also provide the
   primary interface for accessing ganymede db objects.

   Created: 1 April 1996
   Release: $Name:  $
   Version: $Revision: 1.48 $
   Last Mod Date: $Date: 2000/09/30 22:50:12 $
   Module By: Jonathan Abbey  jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                         Session

------------------------------------------------------------------------------*/

/**
 * <P>Client side interface definition for the
 * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession} class.  The Session
 * interface is provided to the client by the
 * {@link arlut.csd.ganymede.GanymedeServer GanymedeServer}'s 
 * {@link arlut.csd.ganymede.GanymedeServer#login(arlut.csd.ganymede.Client) login()}
 * method, and provides the client with an RMI reference that can be used
 * to communicate with the Ganymede server.</P>
 *
 * <P>Many of the methods in this interface, when called, will return
 * remote object references that the client can in turn interact with
 * to perform operations on the server.  These include the
 * {@link arlut.csd.ganymede.db_object db_object} reference that can
 * be returned as part of a {@link arlut.csd.ganymede.ReturnVal ReturnVal}
 * return value, and the {@link arlut.csd.ganymede.db_field db_field}
 * references that are obtained from the db_object references.</P>
 *
 * @version $Revision: 1.48 $ %D%
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 * @see arlut.csd.ganymede.DBSession
 */

public interface Session extends Remote {

  // Client/server interface operations

  /**
   * @deprecated
   */

  String      getLastError() throws RemoteException;

  /**
   *
   * This method logs a client out and closes up any resources
   * used by the client on the server. 
   *
   */

  void        logout() throws RemoteException;

  /**
   * <p>This method is used to allow a client to request that wizards
   * not be provided in response to actions by the client.  This
   * is intended to allow non-interactive or non-gui clients to
   * do work without having to go through a wizard interaction
   * sequence.</p>
   *
   * <p>Wizards are enabled by default.</p>
   *
   * @param val If true, wizards will be enabled.
   */

  void        enableWizards(boolean val) throws RemoteException;

  /**
   * <p>This method is used to tell the client where to look to access
   * the Ganymede help document tree.  The String returned is a URL
   * for the root of the Ganymede help web.</p>
   */

  String      getHelpBase() throws RemoteException;

  /**
   * <p>This method is used to allow the client to retrieve messages like
   * the motd from the server.  The client can specify that it only
   * wants to see a message if it has changed since the user last
   * logged out.  This is intended to support a message of the day
   * type functionality.  The server will not necessarily remember the
   * last log out across server restart.</p>
   *
   * @param key A string, like "motd", indicating what message to retrieve.
   * @param onlyShowIfNew If true, the message will only be returned if
   *                      it has changed since the user last logged out.
   *
   * @return A StringBuffer containing the message, if found, or null if no
   * message exists for the key, or if onlyShowIfNew was set and the message
   * was not new.
   *   
   */

  StringBuffer getMessage(String key, boolean onlyShowIfNew) throws RemoteException;

  /**
   * <p>This method is used to allow the client to retrieve messages like
   * the motd from the server.  The client can specify that it only
   * wants to see a message if it has changed since the user last
   * logged out.  This is intended to support a message of the day
   * type functionality.  The server will not necessarily remember the
   * last log out across server restart.</p>
   *
   * @param key A string, like "motd", indicating what message to retrieve.
   * @param onlyShowIfNew If true, the message will only be returned if
   *                      it has changed since the user last logged out.
   *
   * @return A StringBuffer containing the message, if found, or null if no
   * message exists for the key, or if onlyShowIfNew was set and the message
   * was not new.
   *   
   */

  StringBuffer getMessageHTML(String key, boolean onlyShowIfNew) throws RemoteException;

  /**
   * <p>This method returns the identification string that the server
   * has assigned to the user.</p>
   */

  String      getMyUserName() throws RemoteException;

  /**
   * <p>This method returns a list of personae names available
   * to the user logged in.</p>
   */

  Vector      getPersonae() throws RemoteException;

  /**
   * <p>This method is used to select an admin persona, changing the
   * permissions that the user has and the objects that are
   * accessible in the database.</p>
   */

  boolean     selectPersona(String persona, String password) throws RemoteException;

  /**
   * <p>This method returns a QueryResult of owner groups that the current
   * persona has access to.  This list is the transitive closure of
   * the list of owner groups in the current persona.  That is, the
   * list includes all the owner groups in the current persona along
   * with all of the owner groups those owner groups own, and so on.</p>
   */

  QueryResult      getOwnerGroups() throws RemoteException;

  /**
   * <p>This method may be used to set the owner groups of any objects
   * created hereafter.</p>
   *
   * @param ownerInvids a Vector of Invid objects pointing to
   * ownergroup objects.
   */

  ReturnVal        setDefaultOwner(Vector ownerInvids) throws RemoteException;

  /**
   * <p>This method may be used to cause the server to pre-filter any object
   * listing to only show those objects directly owned by owner groups
   * referenced in the ownerInvids list.  This filtering will not restrict
   * the ability of the client to directly view any object that the client's
   * persona would normally have access to, but will reduce clutter and allow
   * the client to present the world as would be seen by administrator personas
   * with just the listed ownerGroups accessible.</p>
   *
   * <p>This method cannot be used to grant access to objects that are accessible
   * by the client's adminPersona.</p>
   *
   * <p>Calling this method with ownerInvids set to null will turn off the filtering.</p>
   *
   * @param ownerInvids a Vector of Invid objects pointing to ownergroup objects.
   *
   */

  ReturnVal        filterQueries(Vector ownerInvids) throws RemoteException;

  //  Database operations

  /**
   * <p>List types of objects stored and manipulated through the Ganymede server.</p>
   *
   * <p>This method returns a vector of Base remote references.</p>
   *
   * @deprecated Superseded by the more efficient getBaseList()
   *
   * @see arlut.csd.ganymede.Base
   */

  Vector      getTypes() throws RemoteException;

  /**
   * <p>Returns the root of the category tree on the server</p>
   *
   * @deprecated Superseded by the more efficient getCategoryTree()
   *
   * @see arlut.csd.ganymede.Category
   */

  Category    getRootCategory() throws RemoteException;

  /**
   * <p>Returns a serialized representation of the basic category
   * and base structure on the server.  The returned CategoryTransport
   * will include only object types that are editable by the user.</p>
   *
   * @see arlut.csd.ganymede.CategoryTransport
   */

  CategoryTransport    getCategoryTree() throws RemoteException;

  /**
   * <p>Returns a serialized representation of the basic category
   * and base structure on the server.</p>
   *
   * @param hideNonEditables If true, the CategoryTransport returned
   * will only include those object types that are editable by the
   * client.
   *
   * @see arlut.csd.ganymede.CategoryTransport
   */

  CategoryTransport    getCategoryTree(boolean hideNonEditables) throws RemoteException;

  /**
   * <p>Returns a serialized representation of the object types
   * defined on the server.</p>
   *
   * @see arlut.csd.ganymede.BaseListTransport
   */

  BaseListTransport    getBaseList() throws RemoteException;

  /**
   * <p>Returns a vector of field definition templates, in display order.</p>
   *
   * <p>This vector may be cached, as it is static for this object type.</p>
   *
   * @see arlut.csd.ganymede.FieldTemplate
   */

  Vector      getFieldTemplateVector(short baseId) throws RemoteException;

  /**
   * <p>This method call initiates a transaction on the server.  This
   * call must be executed before any objects are modified (created,
   * edited, inactivated, removed).</p>
   *
   * <p>Currently each client can only have one transaction open. It
   * is an error to call openTransaction() while another transaction
   * is still open, and an exception will be thrown in this case.</p>
   */

  ReturnVal   openTransaction(String description) throws RemoteException;

  /**
   * <p>This method call initiates a transaction on the server.  This
   * call must be executed before any objects are modified (created,
   * edited, inactivated, removed).</p>
   *
   * <p>Currently each client can only have one transaction open. It
   * is an error to call openTransaction() while another transaction
   * is still open, and an exception will be thrown in this case.</p>
   *
   * <p>If interactive is false, processing of the transaction will
   * take certain shortcuts.  Invid linking will not be checkpointed..
   * any failure that cannot be safely handled without Invid link
   * checkpointing will cause the transaction to refuse to commit
   * when commitTransaction() is called.</p>
   */

  ReturnVal   openTransaction(String description, boolean interactive) throws RemoteException;

  /**
   * <p>This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients.</p>
   *
   * <p>If the transaction cannot be committed for some reason,
   * commitTransaction() will abort the transaction if abortOnFail is
   * true.  In any case, commitTransaction() will return a ReturnVal
   * indicating whether or not the transaction could be committed, and
   * whether or not the transaction remains open for further attempts
   * at commit.  If ReturnVal.doNormalProcessing is set to true, the
   * transaction remains open and it is up to the client to decide
   * whether to abort the transaction by calling abortTransaction(),
   * or to attempt to fix the reported problem and try another call
   * to commitTransaction().</p>
   *
   * @param abortOnFail If true, the transaction will be aborted if it
   * could not be committed successfully.
   *
   * @return a ReturnVal object if the transaction could not be committed,
   *         or null if there were no problems.  If the transaction was
   *         forcibly terminated due to a major error, the 
   *         doNormalProcessing flag in the returned ReturnVal will be
   *         set to false.
   */

  ReturnVal  commitTransaction(boolean abortOnFail) throws RemoteException;

  /**
   * <p>This method causes all changes made by the client to be 'locked in'
   * to the database.  When commitTransaction() is called, the changes
   * made by the client during this transaction is logged to a journal
   * file on the server, and the changes will become visible to other
   * clients.</p>
   *
   * <p>commitTransaction() will return a ReturnVal indicating whether or
   * not the transaction could be committed, and whether or not the
   * transaction remains open for further attempts at commit.  If
   * ReturnVal.doNormalProcessing is set to true, the transaction
   * remains open and it is up to the client to decide whether to
   * abort the transaction by calling abortTransaction(), or to
   * attempt to fix the reported problem and try another call to
   * commitTransaction().</p>
   *
   * @return a ReturnVal object if the transaction could not be committed,
   *         or null if there were no problems.  If the transaction was
   *         forcibly terminated due to a major error, the 
   *         doNormalProcessing flag in the returned ReturnVal will be
   *         set to false.
   */

  ReturnVal  commitTransaction() throws RemoteException;

  /**
   * <p>This method causes all changes made by the client to be thrown out
   * by the database, and the transaction is closed.</p>
   *
   * @return null if the transaction was cleared successfully,
   *         a non-null ReturnVal if there was some kind of abnormal condition.
   */
  
  ReturnVal     abortTransaction() throws RemoteException;

  /**
   * <p>This method allows clients to cause mail to be sent from the
   * Ganymede server when they can't do it themselves.  The mail
   * will have a From: header indicating the identity of the
   * sender.</p>
   *
   * <p>body is a StringBuffer instead of a String because RMI has a 64k
   * serialization limit on the String class.</p>
   *
   * @param address The addresses to mail to, may have more than one
   * address separated by commas or spaces.
   * @param subject The subject of this mail, will have 'Ganymede:' prepended
   * by the server.
   * @param body The content of the message.
   */

  void sendMail(String address, String subject, StringBuffer body) throws RemoteException;

  /**
   * <p>This method allows clients to cause mail to be sent from the
   * Ganymede server when they can't do it themselves.  The mail
   * will have a From: header indicating the identity of the
   * sender.</p>
   *
   * <p>body and HTMLbody are StringBuffer's instead of Strings because RMI
   * has a 64k serialization limit on the String class.</p>
   *
   * @param address The addresses to mail to, may have more than one
   * address separated by commas or spaces.
   * @param subject The subject of this mail, will have 'Ganymede:' prepended
   * by the server.
   * @param body The plain-ASCII content of the message, or null if none.
   * @param HTMLbody The HTML content of the message, or null if none.
   */

  void sendHTMLMail(String address, String subject, StringBuffer body, StringBuffer HTMLbody) throws RemoteException;

  /**
   * <p>This method provides the hook for doing a
   * fast database dump to a string form.  The 
   * {@link arlut.csd.ganymede.DumpResult DumpResult}
   * returned comprises a formatted dump of all visible
   * fields and objects that match the given query.</p>
   *
   * @see arlut.csd.ganymede.Query
   */

  DumpResult dump(Query query) throws RemoteException;

  /**
   * <p>This method allows the client to get a status update on a
   * specific list of invids.</p>
   *
   * <p>If any of the invids are not currently defined in the server, or
   * if the client doesn't have permission to view any of the invids,
   * those invids' status will not be included in the returned
   * QueryResult.</p>
   *
   * @param invidVector Vector of Invid's to get the status for.
   */

  public QueryResult queryInvids(Vector invidVector) throws RemoteException;

  /**
   * <p>Returns an Invid for an object of a specified type and name, or
   * null if no such object could be found.</p>
   *
   * <p>If the user does not have permission to view the object, null will
   * be returned even if an object by that name does exist.</p>
   *
   * @param name Label for an object
   * @param type Object type id number
   */

  public Invid findLabeledObject(String name, short type) throws RemoteException;

  /**
   * <p>List objects in the database meeting the given query criteria.</p>
   *
   * <p>The database will be read-locked during the query, assuring
   * a transaction-consistent view of the database.  The QueryResult
   * returned comprises a formatted dump of the invid's and
   * labels of the viewable objects matching the provided query.</p>
   */

  QueryResult    query(Query query) throws RemoteException;

  /**
   * <p>This method returns the label for a specific invid.</p>
   */

  String    viewObjectLabel(Invid invid) throws RemoteException;

  /**
   * <p>This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to &lt;invid&gt;, since time &lt;since&gt;.</p>
   *
   * @param invid The invid identifier for the object whose history is sought
   * @param since Report events since this date, or all events if this is null.
   *
   * @return A StringBuffer containing a record of events for the Invid in question,
   * or null if permissions are denied to view the history.
   */

  StringBuffer    viewObjectHistory(Invid invid, Date since) throws RemoteException;

  /**
   * <p>This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to &lt;invid&gt;, since time &lt;since&gt;.</p>
   *
   * @param invid The invid identifier for the object whose history is sought
   * @param since Report events since this date, or all events if this is null.
   * @param fullTransactions If false, only events directly involving the requested
   * object will be included in the result buffer.
   *
   * @return A StringBuffer containing a record of events for the Invid in question,
   * or null if permissions are denied to view the history.
   */

  StringBuffer    viewObjectHistory(Invid invid, Date since, boolean fullTransactions) throws RemoteException;

  /**
   * <p>This method returns a multi-line string containing excerpts from
   * the Ganymede log relating to &lt;invid&gt;, since time &lt;since&gt;.</p>
   *
   * @param invid The invid identifier for the admin Persona whose history is sought
   * @param since Report events since this date, or all events if this is null.
   *
   * @return A StringBuffer containing a record of events for the Invid in question,
   * or null if permissions are denied to view the history.
   */

  StringBuffer    viewAdminHistory(Invid invid, Date since) throws RemoteException;

  /**
   * <p>View an object from the database.  The ReturnVal returned will
   * carry a {@link arlut.csd.ganymede.db_object db_object} reference,
   * which can be obtained by the client
   * calling {@link arlut.csd.ganymede.ReturnVal#getObject() ReturnVal.getObject()}.
   * If the object could not be
   * viewed for some reason, the ReturnVal will carry an encoded error
   * dialog for the client to display.</p>
   *
   * <p>view_db_object() can be done at any time, outside of the bounds of
   * any transaction.  view_db_object() returns a snapshot of the object's
   * state at the time the view_db_object() call is processed, and will
   * be transaction-consistent internally.</p>
   *
   * <p>If view_db_object() is called during a transaction, the object
   * will be returned as it stands during the transaction.. that is,
   * if the object has been changed during the transaction, that
   * changed object will be returned, even if the transaction has
   * not yet been committed, and other clients would not be able to
   * see that version of the object.</p>
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   */

  ReturnVal   view_db_object(Invid invid) throws RemoteException;

  /**
   * <p>Check an object out from the database for editing.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling
   * {@link arlut.csd.ganymede.ReturnVal#getObject() ReturnVal.getObject()}.
   * If the object could not be checked out for editing for some
   * reason, the ReturnVal will carry an encoded error dialog for the
   * client to display.</p>
   *
   * <p>Keep in mind that only one Session can have a particular
   * {@link arlut.csd.ganymede.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once checked out, the object will be unavailable
   * to any other sessions until this session calls 
   * {@link arlut.csd.ganymede.Session#commitTransaction() commitTransaction()}
   * or {@link arlut.csd.ganymede.Session#abortTransaction() abortTransaction()}.</p>
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   */

  ReturnVal   edit_db_object(Invid invid) throws RemoteException;

  /**
   * <p>Create a new object of the given type.  The ReturnVal
   * returned will carry a db_object reference, which can be obtained
   * by the client calling ReturnVal.getObject().  If the object
   * could not be checked out for editing for some reason, the ReturnVal
   * will carry an encoded error dialog for the client to display.</p>
   *
   * <p>Keep in mind that only one Session can have a particular
   * {@link arlut.csd.ganymede.DBEditObject DBEditObject} checked out for
   * editing at a time.  Once created, the object will be unavailable
   * to any other sessions until this session calls 
   * {@link arlut.csd.ganymede.Session#commitTransaction() commitTransaction()}.</p>
   *
   * @param type The kind of object to create.
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   */

  ReturnVal   create_db_object(short type) throws RemoteException;

  /**
   * <p>Clone a new object from object &lt;invid&gt;.  The ReturnVal returned
   * will carry a db_object reference, which can be obtained by the
   * client calling ReturnVal.getObject().  If the object could not
   * be checked out for editing for some reason, the ReturnVal will
   * carry an encoded error dialog for the client to display.</p>
   *
   * <p>This method must be called within a transactional context.</p>
   *
   * <p>Typically, only certain values will be cloned.  What values are
   * retained is up to the specific code module provided for the
   * invid type of object.</p>
   *
   * @return A ReturnVal carrying an object reference and/or error dialog
   *    
   * @see arlut.csd.ganymede.Session
   */

  ReturnVal   clone_db_object(Invid invid) throws RemoteException;

  /**
   * <p>Inactivate an object in the database</p>
   *
   * <p>This method must be called within a transactional context.  The object's
   * change in status will not be visible to other sessions until this session calls 
   * {@link arlut.csd.ganymede.Session#commitTransaction() commitTransaction()}.</p>
   *
   * <p>Objects inactivated will typically be altered to reflect their inactive
   * status, but the object itself might not be purged from the Ganymede
   * server for a defined period of time, to allow other network systems
   * to have time to do accounting, clean up, etc., before a user id or
   * network address is re-used.</p>
   *
   * @return a ReturnVal object if the object could not be inactivated,
   *         or null if there were no problems
   */

  ReturnVal     inactivate_db_object(Invid invid) throws RemoteException;

  /**
   * <p>Reactivates an inactivated object in the database</p>
   *
   * <p>This method is only applicable to inactivated objects.  For such,
   * the object will be reactivated if possible, and the removal date
   * will be cleared.  The object may retain an expiration date,
   * however.</p>
   *
   * <p>The client should check the returned ReturnVal's
   * {@link arlut.csd.ganymede.ReturnVal#getObjectStatus() getObjectStatus()}
   * method to see whether the re-activated object has an expiration date set.</p>
   *
   * <p>This method must be called within a transactional context.  The object's
   * change in status will not be visible to other sessions until this session calls 
   * {@link arlut.csd.ganymede.Session#commitTransaction() commitTransaction()}.</p>
   *
   * @see arlut.csd.ganymede.Session
   */

  ReturnVal     reactivate_db_object(Invid invid) throws RemoteException;

  /**
   * <p>Remove an object from the database</p>
   *
   * <p>This method must be called within a transactional context.</p>
   *
   * <p>Certain objects cannot be inactivated, but must instead be
   * simply removed on demand.  The active permissions for the client
   * may determine whether a particular type of object may be removed.
   * Any problems with permissions to remove this object will result
   * in a dialog being returned in the ReturnVal.</p>
   *
   * <p>This method must be called within a transactional context.  The object's
   * removal will not be visible to other sessions until this session calls 
   * {@link arlut.csd.ganymede.Session#commitTransaction() commitTransaction()}.</p>
   *
   * @return a ReturnVal object if the object could not be inactivated,
   *         or null if there were no problems
   */

  ReturnVal     remove_db_object(Invid invid) throws RemoteException;

  /**
   * <p>This method is called by the XML client to initiate a dump
   * of the server's schema definition in XML format.  The
   * FileReceiver referenced passed as a parameter to this method
   * will be used to send the data to the client.</p>
   *
   * <p>This method will not return until the complete schema
   * definition in XML form has been sent to the receiver, or until
   * an exception is caught from the receiver.  The returned ReturnVal
   * indicates the success of the file transmission.</p>
   */

  ReturnVal getSchemaXML(FileReceiver receiver) throws RemoteException;
}
