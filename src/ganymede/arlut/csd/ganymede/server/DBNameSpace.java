/*
   GASH 2

   DBNameSpace.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Last Commit: $Format:%cd$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System

   Copyright (C) 1996-2008
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.Util.NamedStack;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.rmi.NameSpace;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     DBNameSpace

------------------------------------------------------------------------------*/

/**
 * DBNameSpaces are the objects used to manage unique value
 * tracking in {@link arlut.csd.ganymede.server.DBField DBFields} that are
 * unique value constrained.  DBNameSpace is smart enough to
 * coordinate unique value allocation and management during object
 * editing across concurrent transactions.
 *
 * In general, transactions in Ganymede are not able to affect each other
 * at all, save through the acquisition of exclusive editing locks on
 * invidivual objects, and through the atomic acquisition of values
 * for unique value constrained DBFields.  Once a transaction allocates
 * a unique value using either the {@link arlut.csd.ganymede.server.DBNameSpace#mark(arlut.csd.ganymede.server.DBEditSet,
 * java.lang.Object,arlut.csd.ganymede.server.DBField) mark()},
 * {@link arlut.csd.ganymede.server.DBNameSpace#unmark(arlut.csd.ganymede.server.DBEditSet,
 * java.lang.Object,arlut.csd.ganymede.server.DBField oldField) unmark()}, or
 * {@link arlut.csd.ganymede.server.DBNameSpace#reserve(arlut.csd.ganymede.server.DBEditSet,
 * java.lang.Object) reserve()}
 * methods, no other transaction can allocate that value, until the first transaction
 * calls the {@link arlut.csd.ganymede.server.DBNameSpace#commit(arlut.csd.ganymede.server.DBEditSet)
 * commit()}, {@link arlut.csd.ganymede.server.DBNameSpace#abort(arlut.csd.ganymede.server.DBEditSet)
 * abort()}, or {@link arlut.csd.ganymede.server.DBNameSpace#rollback(arlut.csd.ganymede.server.DBEditSet,
 * java.lang.String) rollback()}
 * methods.
 *
 * In order to perform this unique value management, DBNameSpace
 * maintains a private Hashtable, {@link
 * arlut.csd.ganymede.server.DBNameSpace#uniqueHash uniqueHash}, that
 * associates the allocated vales in the namespace with {@link
 * arlut.csd.ganymede.server.DBNameSpaceHandle DBNameSpaceHandle}
 * objects which track the transaction that is manipulating the value,
 * if any, as well as the DBField object in the database that is
 * checked in with that value.  The {@link
 * arlut.csd.ganymede.server.GanymedeSession GanymedeSession} query
 * logic takes advantage of this to do optimized, hashed look-ups of
 * values for unique value constrained fields to locate objects in the
 * database rather than having to iterate over all objects of a given
 * type to find a particular match.
 *
 * DBNameSpaces may be defined in the server's schema editor to be
 * either case sensitive or case insensitive.  The DBNameSpace class
 * uses the {@link arlut.csd.ganymede.server.GHashtable GHashtable}
 * class to handle the representational issues in the unique value
 * hash for this, as well as for things like IP address
 * representation.
 */

public final class DBNameSpace implements NameSpace {

  static final boolean debug = true;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBNameSpace");

  /**
   * The number of simultaneous transactions in progress
   * that we will size the transactions hash for initially.
   */

  static final int TRANSCOUNT = 30;

  /**
   * The initial number of slots that we will reserve in our
   * uniqueHash hashtable, if we are starting from scratch with a new
   * namespace.
   */

  static final int DEFAULTSIZE = 397; // prime

  /**
   * The number of spaces we save in our hashtable before it will need
   * to grow again.
   */

  static final int GROWTHSPACE = 250;

  // ---

  /**
   * At startup, if we are reading from a Ganymede db file of version
   * 2.14 or later, we will read the size of the hash to allocate at
   * database loading time so we don't have to do a lot of hash
   * expansion.
   */

  private int hashSize;

  /**
   * treat differently-cased Strings as the same for key?
   */

  private boolean caseInsensitive;

  /**
   * the name of this namespace
   */

  private String name;

  /**
   * Hashtable mapping values allocated (permanently, for objects
   * checked in to the database, or temporarily, for objects being
   * manipulated by active transactions) in this namespace to {@link
   * arlut.csd.ganymede.server.DBNameSpaceHandle DBNameSpaceHandle}
   * objects that track the current status of the values.
   */

  private GHashtable uniqueHash;

  /**
   * During schema editing, we keep a copy of the uniqueHash that we had
   * when schema edited started.  If fields are detached or attached to this
   * namespace during schema editing, we will make the appropriate changes
   * to the uniqueHash.  If the schema edit is committed, uniqueHash is kept
   * and saveHash is cleared.  If the schema edit is cancelled, uniqueHash
   * is set back to saveHash and the saveHash reference is cleared.  saveHash
   * will always be null except during schema editing.
   */

  private GHashtable saveHash = null;

  /**
   * Hashtable mapping {@link arlut.csd.ganymede.server.DBEditSet
   * DBEditSet's} currently active modifying values in this namespace
   * to {@link arlut.csd.ganymede.server.DBNameSpaceTransaction
   * DBNameSpaceTransaction} objects.
   */

  private Hashtable transactions;

  /* -- */

  /**
   * Constructor for new DBNameSpace for DBStore initialization.
   *
   * @param name Name for this name space
   * @param caseInsensitive If true, case is disregarded in this namespace
   */

  public DBNameSpace(String name, boolean caseInsensitive)
  {
    this.name = name;
    this.caseInsensitive = caseInsensitive;

    uniqueHash = new GHashtable(DEFAULTSIZE, caseInsensitive);
    transactions = new Hashtable(TRANSCOUNT);
  }

  /**
   * Create a new DBNameSpace object from a stream definition.
   */

  public DBNameSpace(DataInput in) throws IOException
  {
    receive(in);

    uniqueHash = new GHashtable(hashSize, caseInsensitive);
    transactions = new Hashtable(TRANSCOUNT);
  }

  /**
   * Read in a namespace definition from a DataInput stream.
   */

  private void receive(DataInput in) throws IOException
  {
    name = in.readUTF();
    caseInsensitive = in.readBoolean();

    if (Ganymede.db.isAtLeast(2, 14))
      {
	hashSize = gnu.trove.PrimeFinder.nextPrime((int)Math.ceil((in.readInt() + GROWTHSPACE) / 0.75f));
      }
    else
      {
	hashSize = DEFAULTSIZE;
      }
  }

  /**
   * Write out a namespace definition to a DataOutput stream.
   */

  public synchronized void emit(DataOutput out) throws IOException
  {
    out.writeUTF(name);
    out.writeBoolean(caseInsensitive);
    out.writeInt(uniqueHash.size());
  }

  /**
   * Write out an XML entity for this namespace.
   */

  public synchronized void emitXML(XMLDumpContext xDump) throws IOException
  {
    xDump.startElementIndent("namespace");
    xDump.attribute("name", getName());

    if (caseInsensitive)
      {
	xDump.attribute("case-sensitive", "false");
      }
    else
      {
	xDump.attribute("case-sensitive", "true");
      }

    xDump.endElement("namespace");
  }

  /**
   * Returns the name of this namespace.
   *
   * @see arlut.csd.ganymede.rmi.NameSpace
   */

  public synchronized String getName()
  {
    return name;
  }

  /**
   * Sets the name of this namespace.  Returns false
   * if the name is already taken by another namespace
   *
   * @see arlut.csd.ganymede.rmi.NameSpace
   */

  public synchronized boolean setName(String newName)
  {
    // XXX need to make sure this new name isn't in conflict
    // with existing names XXX

    name = newName;
    return true;
  }

  /**
   * Returns true if case is to be disregarded in comparing
   * entries in namespace managed fields.
   *
   * @see arlut.csd.ganymede.rmi.NameSpace
   */

  public synchronized boolean isCaseInsensitive()
  {
    return caseInsensitive;
  }

  /**
   * Turns case sensitivity on/off.  If b is true, case will be
   * disregarded in comparing entries in namespace managed fields.
   *
   * @see arlut.csd.ganymede.rmi.NameSpace 
   */

  public synchronized void setInsensitive(boolean b)
  {
    if (b == this.caseInsensitive)
      {
	return;
      }

    // let's see if we can do this safely.. we'll throw an
    // IllegalStateException if changing the case sensitivity would
    // cause a collision, otherwise this will take care of things

    this.uniqueHash.setInSensitivity(b);

    // if we've got here, we are okay to go

    this.caseInsensitive = b;
  }

  /**
   * Returns true if this namespace has value allocated.
   */

  public synchronized boolean containsKey(Object value)
  {
    return uniqueHash.containsKey(value);
  }

  /**
   * Publicly accessible function used to record the presence of a
   * namespace value in this namespace during Ganymede database
   * loading.
   *
   * Used by the {@link arlut.csd.ganymede.server.DBObject DBObject}
   * receive() method and the {@link arlut.csd.ganymede.server.DBJournal
   * DBJournal}'s {@link arlut.csd.ganymede.server.JournalEntry JournalEntry}
   * class' process() method to build up the namespace during server
   * start-up.
   */

  public synchronized void receiveValue(Object value, DBField field)
  {
    putHandle(value, new DBNameSpaceHandle(field));
  }

  /**
   * Publicly accessible function used to clear the given
   * value from this namespace in a non-transactional fashion.
   *
   * Used by {@link arlut.csd.ganymede.server.DBJournal DBJournal}'s
   * {@link arlut.csd.ganymede.server.JournalEntry JournalEntry} class'
   * process() method to rectify the namespace during server
   * start-up.
   */
  
  public synchronized void removeHandle(Object value)
  {
    uniqueHash.remove(value);
  }

  /**
   * This method allows the namespace to be used as a unique valued 
   * search index.
   *
   * Note that this lookup is case sensitive or not according to the case
   * sensitivity of this DBNameSpace.  If this DBNameSpace is case insensitive,
   * the DBField returned may contain the value (if value is a String) with
   * different capitalization.
   *
   * As well, this method is really probably only useful in the context of
   * a DBReadLock, but we're not doing anything to enforce this requirement 
   * at this point.
   *
   * @param value The value to search for in the namespace hash.
   */

  public synchronized DBField lookupPersistent(Object value)
  {
    DBNameSpaceHandle handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (handle == null)
      {
	return null;
      }

    return handle.getPersistentField();
  }

  /**
   * If the value is attached to an object that is being created or edited in
   * a transaction, this method will return the editable DBField that contains
   * the constrained value during editing.
   *
   * Note that this lookup is case sensitive or not according to the case
   * sensitivity of this DBNameSpace.  If this DBNameSpace is case insensitive,
   * the DBField returned may contain the value (if value is a String) with
   * different capitalization.
   *
   * As well, this method is really probably useful in the context of
   * a DBReadLock, but we're not doing anything to enforce this requirement 
   * at this point.
   *
   * @param value The value to search for in the namespace hash.
   */

  public synchronized DBField lookupShadow(Object value)
  {
    DBNameSpaceHandle handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (handle == null)
      {
	return null;
      }

    return handle.getShadowField();
  }

  /**
   * This method looks to find where the given value is bound in the
   * namespace, taking into account the transactional view the calling
   * session has.  If the value is attached to an object in the
   * current transaction, this method will return a reference to the
   * editable shadow DBField containing the value.  If not, this
   * method will either return the DBField containing the read-only
   * persistent version from the DBStore, or null if the value sought
   * has been cleared from use in the objects being edited by the
   * transaction.
   *   
   * Note that this lookup is case sensitive or not according to the case
   * sensitivity of this DBNameSpace.  If this DBNameSpace is case insensitive,
   * the DBField returned may contain the value (if value is a String) with
   * different capitalization.
   *
   * As well, this method is really probably useful in the context of
   * a DBReadLock, but we're not doing anything to enforce this requirement 
   * at this point.
   *
   * @param session The GanymedeSession to use to lookup the containing object..
   * useful when a GanymedeSession is doing the looking up of value
   * @param value The value to search for in the namespace hash.
   */

  public synchronized DBField lookupMyValue(GanymedeSession session, Object value)
  {
    DBNameSpaceHandle handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (handle == null)
      {
	return null;
      }

    if (handle.isEditedByUs(session))
      {
	return handle.getShadowField();
      }

    return handle.getPersistentField(session);
  }

  /**
   * This method reserves a value so that the given editSet is
   * assured of being able to use this value at some point before the
   * transaction is commited or canceled.  reserve() is different from
   * mark() in that there is no field specified to be holding the
   * value, and that when the transaction is committed or canceled,
   * the value will be returned to the available list.  During the
   * transaction, the transaction code can mark the value at any time
   * with assurance that they will be able to do so.
   *
   * If a transaction attempts to reserve() a value that is already
   * being held by an object in the transaction, reserve() will return
   * false.
   *
   * @param editSet The transaction claiming the unique value <value>
   * @param value The unique value that transaction editset is attempting to claim
   *
   * @return true if the value could be reserved in the given editSet.  
   */

  public boolean reserve(DBEditSet editSet, Object value)
  {
    checkSchemaEditInProgress(false);

    if (editSet == null || value == null)
      {
	throw new IllegalArgumentException();
      }

    DBNameSpaceHandle handle;
    
    /* -- */

    // Is this value already taken?

    if (uniqueHash.containsKey(value))
      {
	handle = (DBNameSpaceHandle) uniqueHash.get(value);

	if (!handle.isCheckedOut())
	  {
	    return false;
	  }
	else
	  {
	    if (!handle.isEditedByUs(editSet))
	      {
		return false;
	      }

	    if (handle.getShadowField() != null)
	      {
		return false;
	      }

	    if (handle.isReserved())
	      {
		return false;
	      }
	  }

	return true;
      }

    handle = new DBNameSpaceEditingHandle(editSet, null);

    handle.setReserved(true);

    putHandle(value, handle);

    remember(editSet, value);

    return true;
  }

  /**
   * This method reserves a value so that the given editSet is assured
   * of being able to use this value at some point before the
   * transaction is commited or canceled.  reserve() is different from
   * mark() in that there is no field specified to be holding the
   * value, and that when the transaction is committed or canceled,
   * the value will be returned to the available list.  During the
   * transaction, the transaction code can mark the value at any time
   * with assurance that they will be able to do so.
   *
   * If onlyUsed is false and a transaction attempts to reserve() a
   * value that is already being held by an object in the transaction,
   * reserve() will return true, even though a subsequent mark()
   * attempt would fail unless the value is first unmarked.
   *
   * @param editSet The transaction claiming the unique value <value>
   * @param value The unique value that transaction editset is
   * attempting to claim
   * @param onlyUnused If true, reserve() will return false if the
   * value is already attached to a field connected to this namespace,
   * even if in an object attached to the editSet provided.
   *
   * @return true if the value was successfully reserved in the given
   * editSet.
   *
   * @deprecated We are no longer supporting false values of the
   * onlyUnused paramater.  We never did so properly before, anyway.
   */

  @Deprecated
  public boolean reserve(DBEditSet editSet, Object value, boolean onlyUnused)
  {
    if (onlyUnused != true)
      {
	throw new UnsupportedOperationException("reserve() no longer accepts a false onlyUnused parameter");
      }

    return this.reserve(editSet, value);
  }

  /**
   * This method tests to see whether a value in the namespace can
   * be marked as in use.  Such a marking is done to allow an editset
   * to have the ability to juggle values in namespace associated
   * fields without allowing another editset to acquire a value
   * needed if the editset transaction is aborted.
   *
   * For array db fields, all elements in the array should be
   * testmark'ed in the context of a synchronized block on the
   * namespace before going back and marking each value (while still
   * in the synchronized block on the namespace).. this ensures
   * that we won't mark several values in an array before discovering
   * that one of the values in a DBArrayField is already taken.
   *
   * The success of testmark() is no guarantee of a future successful
   * mark() operation, of course, unless the testmark and mark
   * operations are done within a synchronization block on the
   * namespace.
   *
   * @param editSet The transaction testing permission to claim value.
   * @param value The unique value desired by editSet.
   */

  public synchronized boolean testmark(DBEditSet editSet, Object value)
  {
    checkSchemaEditInProgress(false);

    if (editSet == null || value == null)
      {
	throw new IllegalArgumentException();
      }

    DBNameSpaceHandle handle;

    /* -- */

    if (!uniqueHash.containsKey(value))
      {
	return true;
      }

    handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (handle.isEditedByOtherTransaction(editSet))
      {
	return false;
      }

    if (handle.getShadowField() == null ||
	(!editSet.isInteractive() && handle.getShadowFieldB() == null))
      {
	return true;
      }

    return false;
  }

  /**
   * This method marks a value as being used in this namespace.
   * Marked values are held in editSet's, which are free to shuffle
   * these reserved values around during processing.  The persistent
   * fieldInvid and fieldId fields in the namespace handle object are
   * used during unique value searches to do direct hash lookups
   * without getting confused by any shuffling being performed by a
   * current thread.
   *
   * @param editSet The transaction claiming the unique value &lt;value&gt;
   * @param value The unique value that transaction editset is attempting to claim
   * @param field The DBField which will take the unique value.
   */

  public synchronized boolean mark(DBEditSet editSet, Object value, DBField field)
  {
    checkSchemaEditInProgress(false);

    if (editSet == null || value == null || field == null)
      {
	throw new IllegalArgumentException();
      }

    DBNameSpaceHandle handle;
    
    /* -- */

    if (!uniqueHash.containsKey(value))
      {
	handle = new DBNameSpaceEditingHandle(editSet, null);
	handle.setShadowField(field);

	putHandle(value, handle);

	remember(editSet, value);

	return true;
      }

    handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (handle.isEditedByOtherTransaction(editSet))
      {
	return false;
      }

    if (editSet.isInteractive())
      {
	if (!handle.isCheckedOut() || handle.getShadowField() != null)
	  {
	    return false;
	  }

	handle.setShadowField(field);

	return true;
      }
    else
      {
	if (!handle.isCheckedOut())
	  {
	    handle = handle.checkout(editSet);

	    putHandle(value, handle);
	    remember(editSet, value);

	    handle.setShadowField(handle.getPersistentField(editSet));
	    handle.setShadowFieldB(field);

	    return true;
	  }
	else if (handle.getShadowField() == null)
	  {
	    handle.setShadowField(field);

	    return true;
	  }
	else if (!field.matches(handle.getShadowField()) &&
		 (handle.getShadowFieldB() == null ||
		  field.matches(handle.getShadowFieldB())))
	  {
	    handle.setShadowFieldB(field);

	    return true;
	  }
      }

    return false;
  }

  /**
   * This method tests to see whether a value in the namespace can
   * be marked as not in use.  Such a marking is done to allow an
   * editset to have the ability to juggle values in namespace
   * associated fields without allowing another editset to acquire a
   * value needed if the editset transaction is aborted.
   *
   * For array db fields, all elements in the array should be
   * testunmark'ed in the context of a synchronized block on the
   * namespace before actually unmarking all the values.  See the
   * comments in testmark() for the logic here.  Note that
   * testunmark() is less useful than testmark() because we really
   * aren't expecting anything to prevent us from unmarking()
   * something.
   *
   * @param editSet The transaction that is determining whether value can be freed.
   * @param value The unique value being tested.
   */

  public synchronized boolean testunmark(DBEditSet editSet, Object value, DBField oldField)
  {
    checkSchemaEditInProgress(false);

    if (oldField == null || editSet == null || value == null)
      {
	throw new IllegalArgumentException();
      }

    if (!uniqueHash.containsKey(value))
      {
	throw new RuntimeException("ASSERT: testunmark called on value '" + GHashtable.keyString(value) +
				   "' not in namespace: " + this.getName());
      }

    DBNameSpaceHandle handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (handle.isEditedByOtherTransaction(editSet))
      {
	throw new RuntimeException("ASSERT: testunmark called on value '" + GHashtable.keyString(value) +
				   "' that namespace: " + this.getName() +
				   " believes is being edited by another transaction.  Field is " + oldField);
      }

    if (!handle.matchesAnySlot(oldField))
      {
	throw new RuntimeException("ASSERT: testunmark called on value '" + GHashtable.keyString(value) +
				   "' that namespace: " + this.getName() + " believes is not in field " + oldField);
      }

    return true;
  }

  /**
   * Used to mark a value as not used in the namespace.  Unmarked
   * values are not available for other threads / editset's until
   * commit or abort is called on this namespace on behalf of this
   * editset.
   *
   * @param editSet The transaction that is freeing value.
   * @param value The unique value being tentatively marked as unused.
   * @param oldField The old DBField that the namespace value is being
   * unmarked for
   */

  public synchronized boolean unmark(DBEditSet editSet, Object value, DBField oldField)
  {
    checkSchemaEditInProgress(false);

    if (editSet == null || value == null || oldField == null)
      {
	throw new IllegalArgumentException();
      }

    if (!uniqueHash.containsKey(value))
      {
	throw new RuntimeException("ASSERT: unmark called on value '" + GHashtable.keyString(value) +
				   "' not in namespace: " + this.getName());
      }

    DBNameSpaceHandle handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (handle.isEditedByOtherTransaction(editSet))
      {
	throw new RuntimeException("ASSERT: unmark called on value '" + GHashtable.keyString(value) +
				   "' that namespace: " + this.getName() +
				   " believes is being edited by another transaction.  Field is " + oldField);
      }

    if (!handle.matchesAnySlot(oldField))
      {
	throw new RuntimeException("ASSERT: unmark called on value '" + GHashtable.keyString(value) +
				   "' that namespace: " + this.getName() + " believes is not in field " + oldField);
      }

    if (!handle.isCheckedOut())
      {
	handle = handle.checkout(editSet);

	putHandle(value, handle);
        remember(editSet, value);

	return true;
      }

    if (!editSet.isInteractive())
      {
	if (!oldField.matches(handle.getShadowFieldB()) &&
	    !oldField.matches(handle.getShadowField()))
	  {
	    throw new RuntimeException("ASSERT: mismatched field in non-interactive unmark");
	  }

	if (oldField.matches(handle.getShadowFieldB()))
	  {
	    // I really don't expect getShadowFieldB() to be equal to
	    // the oldField, given how the non-interactive xmlclient
	    // works, but we'll handle that case in the event we do
	    // have some very weird non-interactive client talking to
	    // us which decided to set a prospective mark and then
	    // clear it

	    handle.setShadowFieldB(null);

	    return true;
	  }

	if (handle.getShadowFieldB() != null)
	  {
	    // promote B to A

	    handle.setShadowField(handle.getShadowFieldB());
	    handle.setShadowFieldB(null);

	    return true;
	  }
      }

    if (oldField.matches(handle.getShadowField()))
      {
	handle.setShadowField(null);

	return true;
      }

    return false;
  }

  /**
   * Method to checkpoint namespace changes made by a specific transaction
   * so that state can be rolled back if necessary at a later time.
   *
   * @param editSet The transaction that needs to be checkpointed.
   * @param name The name of the checkpoint to be marked.
   */

  public synchronized void checkpoint(DBEditSet editSet, String name)
  {
    checkSchemaEditInProgress(false);

    getTransactionRecord(editSet).pushCheckpoint(name, new DBNameSpaceCkPoint(this, editSet));
  }

  /**
   * Method to remove a checkpoint from this namespace's
   * DBNameSpaceCkPoint hash.  This is to be done when the calling
   * code knows that it will no longer need to be able to rollback to
   * the named checkpoint.
   *
   * This method really isn't very important, because when the
   * transaction is committed or aborted, the checkpoints hashtable
   * will be cleared of editSet anyway.
   *
   * @param editSet The transaction that is requesting the checkpoint pop.
   * @param name The name of the checkpoint to be popped.
   */

  public synchronized void popCheckpoint(DBEditSet editSet, String name)
  {
    checkSchemaEditInProgress(false);

    getTransactionRecord(editSet).popCheckpoint(name);
  }

  /**
   * Method to rollback namespace changes made by a specific
   * transaction to a checkpoint.
   *
   * @param editSet The transaction that needs to be checkpointed.
   * @param name The name of the checkpoint to be rolled back to.
   *
   * @return false if the checkpoint could not be found.
   */

  public synchronized boolean rollback(DBEditSet editSet, String name)
  {
    checkSchemaEditInProgress(false);

    Object value;
    Vector elementsToRemove = new Vector();
    DBNameSpaceTransaction tRecord; 
    DBNameSpaceCkPoint point;
    DBNameSpaceEditingHandle handle;
    Enumeration en;

    /* -- */

    tRecord = getTransactionRecord(editSet);

    // try to pop off the named checkpoint we're looking for

    point = tRecord.popCheckpoint(name);

    if (point == null)
      {
	throw new RuntimeException("ASSERT: couldn't find checkpoint for " + name + "\n" +
				   "In transaction " + editSet.description + "\n\n" +
				   "Currently registered checkpoints:\n" +
				   tRecord.getCheckpointStack());
      }

    en = tRecord.getReservedEnum();

    while (en.hasMoreElements())
      {
	value = en.nextElement();
	handle = (DBNameSpaceEditingHandle) uniqueHash.get(value);

	if (!point.containsValue(value))
	  {
	    DBNameSpaceHandle oldHandle = handle.getOriginal();

	    if (oldHandle == null)
	      {
		removeHandle(value);
	      }
	    else
	      {
		putHandle(value, oldHandle);
	      }

	    elementsToRemove.addElement(value);
	  }
	else
	  {
	    putHandle(value, point.getValueHandle(value));
	  }
      }

    for (Object element: elementsToRemove)
      {
	tRecord.forget(element);
      }

    return true;
  }

  /**
   * This method returns null if the given transaction doesn't have
   * any shadowFieldB's outstanding.  This is the desired case.  If
   * the given transaction has some values with shadowFieldB's set,
   * that means that those unique values are left "bound" to more than
   * one field, which is an error that non-interactive clients (the
   * xmlclient) can run into.
   *
   * If we have such conflicts, we return a vector of namespace values
   * that are in conflict at transaction commit time.
   */

  public synchronized Vector verify_noninteractive(DBEditSet editSet)
  {
    DBNameSpaceTransaction tRecord;
    Enumeration en;
    Object value;
    DBNameSpaceEditingHandle handle;
    Vector results = null;

    /* -- */

    tRecord = getTransactionRecord(editSet);

    en = tRecord.getReservedEnum();
    
    while (en.hasMoreElements())
      {
	value = en.nextElement();
	handle = (DBNameSpaceEditingHandle) getHandle(value);

	if (handle.getShadowFieldB() == null)
	  {
	    continue;
	  }

	// we've got a shadowFieldB, which we ought not to have.

	if (results == null)
	  {
	    results = new Vector();
	  }

	if (handle.getShadowField() != null)
	  {
	    // "{0}, value in conflict = {1}"
	    results.addElement(ts.l("verify_noninteractive.template",
				    handle.getConflictString(), GHashtable.keyString(value)));
	  }
	else
	  {
	    // we've got a conflict between a checked-in object in
	    // the datastore and an object we're creating or
	    // editing in the xml session.  shadowFieldB() points
	    // to the in-xml session object.

	    String editingRefStr = String.valueOf(handle.getShadowFieldB());
	    DBField conflictField = handle.getPersistentField(editSet);

	    // "{0} conflicts with checked-in {1}"
	    String checkedInConflictStr = ts.l("verify_noninteractive.persistent_conflict",
					       String.valueOf(handle.getShadowFieldB()),
					       String.valueOf(conflictField));

	    // "{0}, value in conflict = {1}"
	    results.addElement(ts.l("verify_noninteractive.template",
				    checkedInConflictStr, GHashtable.keyString(value)));
	  }
      }

    return results;
  }

  /**
   * Method to put the editSet's current namespace modifications into
   * final effect and to make any abandoned values available for other
   * namespaces.
   *
   * Note that a NameSpace should really never fail here.  We assume that
   * all NameSpace management code up to this point has functioned properly..
   * at this point, the EditSet has already committed changes to the DBStore
   * and to any external processes, we're just doing paperwork at this point.
   *
   * @param editSet The transaction being committed.
   */

  public synchronized void commit(DBEditSet editSet)
  {
    checkSchemaEditInProgress(false);

    DBNameSpaceTransaction tRecord;
    Enumeration en;
    Object value;
    DBNameSpaceEditingHandle handle;

    /* -- */

    tRecord = getTransactionRecord(editSet);

    en = tRecord.getReservedEnum();

    // loop over the values in the namespace that were changed or
    // affected by this editset, and commit them into the database,
    // copying the shadowField DBField into the handle's permanent
    // field pointer, or getting rid of them entirely if they are not
    // in use anymore

    while (en.hasMoreElements())
      {
	value = en.nextElement();
	handle = (DBNameSpaceEditingHandle) getHandle(value);

	if (!handle.isEditedByUs(editSet))
	  {
	    if (debug)
	      {
		Ganymede.debug("DBNameSpace.commit(): trying to commit handle for value " + GHashtable.keyString(value) +
			       " that is being edited by another transaction.");
	      }

	    continue;
	  }

	if (handle.getShadowFieldB() != null)
	  {
	    throw new RuntimeException("ASSERT: lingering shadowFieldB at commit time for transaction " +
				       editSet.session.key);
	  }

	DBNameSpaceHandle newHandle = handle.getNewHandle();

	if (newHandle == null)
	  {
	    removeHandle(value);
	  }
	else
	  {
	    putHandle(value, newHandle);
	  }
      }

    tRecord.cleanup();
    transactions.remove(editSet);
  }  

  /**
   * Method to revert an editSet's namespace modifications to its
   * original state.  Used when a transaction is rolled back.
   *
   * @param editSet The transaction whose claimed values in this
   * namespace need to be freed.
   */

  public synchronized void abort(DBEditSet editSet)
  {
    checkSchemaEditInProgress(false);

    DBNameSpaceTransaction tRecord;
    Enumeration en;
    Object value;
    DBNameSpaceEditingHandle handle;

    /* -- */

    tRecord = getTransactionRecord(editSet);

    en = tRecord.getReservedEnum();

    // loop over the values in the namespace that were changed or affected
    // by this editset, and revert them to their checked-in status, or
    // get rid of them entirely if they weren't allocated in this namespace
    // before this transaction allocated them

    while (en.hasMoreElements())
      {
	value = en.nextElement();
	handle = (DBNameSpaceEditingHandle) getHandle(value);

	if (!handle.isEditedByUs(editSet))
	  {
	    if (debug)
	      {
		Ganymede.debug("DBNameSpace.abort(): trying to abort handle for value" + GHashtable.keyString(value) +
			       " that is being edited by another transaction.");
	      }

	    continue;
	  }

	DBNameSpaceHandle oldHandle = handle.getOriginal();

	if (oldHandle == null)
	  {
	    removeHandle(value);
	  }
	else
	  {
	    putHandle(value, oldHandle);
	  }
      }

    tRecord.cleanup();
    transactions.remove(editSet);
  }

  public String toString()
  {
    return name;
  }

  /**
   * This method prepares this DBNameSpace for changes to be made
   * during schema editing.  A back-up copy of the uniqueHash is made,
   * to allow reversion in case the schema edit is aborted.  Between
   * schemaEditCheckout() and either schemaEditCommit() or
   * schemaEditAbort(), the schemaEditRegister() and
   * schemaEditUnregister() methods may be called to handle
   * attaching/detaching fields to the namespace.
   */

  public synchronized void schemaEditCheckout()
  {
    checkSchemaEditInProgress(false);

    this.saveHash = this.uniqueHash;

    uniqueHash = new GHashtable(saveHash.size(), caseInsensitive);

    Enumeration en = saveHash.keys();

    while (en.hasMoreElements())
      {
	Object key = en.nextElement();

	DBNameSpaceHandle handle = (DBNameSpaceHandle) saveHash.get(key);

	DBNameSpaceHandle handleCopy = (DBNameSpaceHandle) handle.clone();

	if (!handleCopy.isCheckedOut())
	  {
	    // "Error, non-null handle owner found during copy of namespace {0} for key: {1}."
	    throw new RuntimeException(ts.l("schemaEditCheckout.non_null_owner",
					    this.toString(),
					    key));
	  }

	if (handleCopy.getShadowField() != null)
	  {
	    // "Error, non-null handle shadowField found during copy of namespace {0} for key {1}."
	    throw new RuntimeException(ts.l("schemaEditCheckout.non_null_shadowField",
					    this.toString(),
					    key));
	  }

	putHandle(key, handleCopy);
      }
  }

  /**
   * Returns true if this namespace has already been checked out for schema editing.
   */

  public synchronized boolean isSchemaEditInProgress()
  {
    return (this.saveHash != null);
  }

  public synchronized void checkSchemaEditInProgress(boolean expecting)
  {
    if (expecting)
      {
	if (this.saveHash == null)
	  {
	    // "Can''t perform, not in schema edit."
	    throw new RuntimeException(ts.l("global.not_editing"));
	  }
      }
    else
      {
	if (this.saveHash != null)
	  {
	    // "Can''t perform, still in schema edit."
	    throw new RuntimeException(ts.l("global.editing"));
	  }
      }
  }

  /**
   * This method locks in any changes made after schema editing is complete.
   */

  public synchronized void schemaEditCommit()
  {
    if (this.saveHash == null)
      {
	return;
      }

    this.saveHash = null;
  }

  /**
   * This method aborts any changes made during schema editing.
   */

  public synchronized void schemaEditAbort()
  {
    if (this.saveHash == null)
      {
	return;
      }

    this.uniqueHash = this.saveHash;
    this.saveHash = null;
  }

  /**
   * This method links the given value to the specified field.  If the
   * value has already been allocated, schemaEditRegister will return false
   * and the value won't be attached to the field in question.
   *
   * @return true if the value could be registered with the specified field,
   * false otherwise
   */

  public synchronized boolean schemaEditRegister(Object value, DBField field)
  {
    checkSchemaEditInProgress(true);

    if (uniqueHash.containsKey(value))
      {
	return false;
      }

    putHandle(value, new DBNameSpaceHandle(field));

    return true;
  }

  /**
   * This method unlinks a single item from this namespace index, if and only
   * if the value is associated with the object and field id specified.
   *
   * @return true if the value was associated with the object and field specified,
   * false otherwise.  If false is returned, no value was removed from this
   * namespace.
   */

  public synchronized boolean schemaEditUnregister(Object value, Invid objid, short field)
  {
    checkSchemaEditInProgress(true);

    DBNameSpaceHandle handle = (DBNameSpaceHandle) uniqueHash.get(value);

    if (handle == null)
      {
	return false;
      }

    if (!handle.matches(objid, field))
      {
	return false;
      }

    removeHandle(value);

    return true;
  }

  /**
   * This method unlinks all values that are associated with the specified
   * object type and field id from this namespace.
   */

  public synchronized void schemaEditUnregister(short objectType, short fieldId)
  {
    checkSchemaEditInProgress(true);

    Vector elementsToRemove = new Vector();
    Enumeration en = this.uniqueHash.keys();

    while (en.hasMoreElements())
      {
	Object value = en.nextElement();
	DBNameSpaceHandle handle = (DBNameSpaceHandle) this.uniqueHash.get(value);

	if (handle.matchesFieldType(objectType, fieldId))
	  {
	    elementsToRemove.addElement(value);
	  }
      }

    for (Object element: elementsToRemove)
      {
	removeHandle(element);
      }
  }

  /**
   * This method is designed for doing conflict detection between this
   * DBNameSpace and another DBNameSpace that we might be looking at
   * merging into this namespace.  This is usually necessary when a
   * Ganymede adopter is collapsing two namespaces into one.
   *
   * In addition to returning true if there are values in conflict
   * between the two namespaces, or false if conflicts exist, a report
   * of the conflicting values will be written to the Ganymede admin
   * console and the Ganymede server's stdout.
   */

  public synchronized boolean findConflicts(DBNameSpace otherSpace)
  {
    boolean success = true;
    Enumeration en = this.uniqueHash.keys();

    while (en.hasMoreElements())
      {
        Object value = en.nextElement();

        if (!otherSpace.containsKey(value))
          {
            continue;
          }

        success = false;

        DBField thisField = this.lookupPersistent(value);
        DBField otherField = otherSpace.lookupPersistent(value);

        if (thisField == null || otherField == null)
          {
            // oops, the conflict isn't really between persistent
            // registrations across the two name spaces.. never mind.

            continue;
          }

        DBObject thisObject = thisField.getOwner();
        DBObject otherObject = otherField.getOwner();

        Ganymede.debug("Namespace " + this.getName() + " has a conflict for value " + value.toString() +
                       " in " + thisObject.getTypeName() + " " + thisObject.getLabel() + "'s " +
                       thisField.getName() + " field, and in " + otherObject.getTypeName() + " " +
                       otherObject.getLabel() + "'s " + otherField.getName() + " field.");
      }

    return success;
  }

  /**
   * Performs some assertinon checking, then places the given
   * DBNameSpaceHandle in the uniqueHash using value as they key.
   */

  private void putHandle(Object value, DBNameSpaceHandle handle)
  {
    assert (handle instanceof DBNameSpaceEditingHandle) || handle.isPersisted();

    uniqueHash.put(value, handle);
  }

  /**
   * Returns the DBNameSpaceHandle associated with the value
   * in this namespace, or null if the value is not allocated in
   * this namespace.
   */

  private DBNameSpaceHandle getHandle(Object value)
  {
    return (DBNameSpaceHandle) uniqueHash.get(value);
  }

  /**
   * This method returns the {@link
   * arlut.csd.ganymede.server.DBNameSpaceTransaction
   * DBNameSpaceTransaction} associated with the given transaction,
   * creating one if one was not previously so associated.
   *
   * This method will always return a valid DBNameSpaceTransaction
   * record.
   */

  private synchronized DBNameSpaceTransaction getTransactionRecord(DBEditSet transaction)
  {
    DBNameSpaceTransaction transRecord = (DBNameSpaceTransaction) transactions.get(transaction);

    if (transRecord == null)
      {
	transRecord = new DBNameSpaceTransaction(transaction, caseInsensitive);
	transactions.put(transaction, transRecord);
      }

    return transRecord;
  }

  /**
   * Remember that this editSet has changed the location/status of
   * this value.
   *
   * This is a private convenience method.
   *
   * This method associates the value with the given editset in a
   * stored transaction record, so that we can rollback the namespace
   * to a fixed state later.
   */

  private void remember(DBEditSet editSet, Object value)
  {
    getTransactionRecord(editSet).remember(value);
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                          DBNameSpaceTransaction

  ----------------------------------------------------------------------------*/

  /**
   * This inner class holds information associated with an active
   * transaction (a {@link arlut.csd.ganymede.server.DBEditSet
   * DBEditSet}) in care of a {@link
   * arlut.csd.ganymede.server.DBNameSpace DBNameSpace}.
   */

  class DBNameSpaceTransaction {

    private NamedStack checkpointStack;
    private GHashtable reservedValues;
    private DBEditSet transaction;

    /* -- */

    DBNameSpaceTransaction(DBEditSet transaction, boolean caseInsensitive)
    {
      this.transaction = transaction;
      this.reservedValues = new GHashtable(caseInsensitive);
      this.checkpointStack = new NamedStack();
    }

    public synchronized void remember(Object value)
    {
      if (reservedValues.containsKey(value))
	{
	  Ganymede.logAssert("ASSERT: DBNameSpaceTransaction.remember(): transaction " + transaction +
			     " already contains value " + GHashtable.keyString(value));

	  return;
	}

      reservedValues.put(value, value);
    }

    public synchronized void forget(Object value)
    {
      if (!reservedValues.containsKey(value))
	{
	  Ganymede.logAssert("ASSERT: DBNameSpaceTransaction.forget(): transaction " + transaction +
			     " does not contain value " + GHashtable.keyString(value));

	  return;
	}
    
      reservedValues.remove(value);
    }

    /**
     * This method dissolves everything referenced by this DBNameSpaceTransaction,
     * in order to facilitate speedy garbage collection.
     */

    public synchronized void cleanup()
    {
      if (checkpointStack != null)
	{
	  while (checkpointStack.size() != 0)
	    {
	      DBNameSpaceCkPoint ckpoint = (DBNameSpaceCkPoint) checkpointStack.pop();
	      ckpoint.cleanup();
	    }

	  checkpointStack = null;
	}

      if (reservedValues != null)
	{
	  reservedValues.clear();
	  reservedValues = null;
	}

      transaction = null;
    }

    public Enumeration getReservedEnum()
    {
      return reservedValues.elements();
    }

    public GHashtable getReservedHash()
    {
      return reservedValues;
    }

    public DBEditSet getDBEditSet()
    {
      return transaction;
    }

    public void pushCheckpoint(String name, DBNameSpaceCkPoint cPoint)
    {
      checkpointStack.push(name, cPoint);
    }

    public DBNameSpaceCkPoint popCheckpoint(String name)
    {
      DBNameSpaceCkPoint point = (DBNameSpaceCkPoint) checkpointStack.pop(name);

      if (point == null)
	{
	  Ganymede.logAssert("ASSERT: DBNameSpaceTransaction.popCheckpoint(): transaction " + transaction +
			     " does not contain a checkpoint named " + name);
	}

      return point;
    }
    
    public NamedStack getCheckpointStack()
    {
      return checkpointStack;
    }
  }

  /*----------------------------------------------------------------------------
                                                                     inner class
                                                              DBNameSpaceCkPoint

  ----------------------------------------------------------------------------*/

  /**
   * This inner class holds checkpoint information associated with
   * an active transaction (a {@link
   * arlut.csd.ganymede.server.DBEditSet DBEditSet}) in care of a
   * {@link arlut.csd.ganymede.server.DBNameSpace DBNameSpace}.
   */

  class DBNameSpaceCkPoint {

    GHashtable reserved;
    Hashtable uniqueHash;

    /* -- */

    DBNameSpaceCkPoint(DBNameSpace space, DBEditSet transaction)
    {
      DBNameSpaceTransaction tRecord = space.getTransactionRecord(transaction);

      reserved = tRecord.getReservedHash();

      if (reserved == null)
	{
	  return;
	}

      // clone the hash to avoid sync problems with other threads

      reserved = (GHashtable) reserved.clone();

      if (reserved.size() > 0)
	{
	  uniqueHash = new Hashtable(reserved.size(), 1.0f);
	}
      else
	{
	  uniqueHash = new Hashtable(10);
	}

      // now copy our hash to preserve the namespace handles

      for (Object value: reserved.values())
	{
	  DBNameSpaceHandle handle = space.getHandle(value);

	  handle = (DBNameSpaceHandle) handle.clone();

	  uniqueHash.put(value, handle);
	}
    }

    public boolean containsValue(Object value)
    {
      return reserved.containsKey(value);
    }

    public DBNameSpaceHandle getValueHandle(Object value)
    {
      return (DBNameSpaceHandle) uniqueHash.get(value);
    }

    /**
     * This method dissolves everything referenced by this DBNameSpaceCkPoint
     * in order to facilitate speedy garbage collection.
     */

    public synchronized void cleanup()
    {
      if (reserved != null)
	{
	  reserved.clear();
	  reserved = null;
	}

      if (uniqueHash != null)
	{
	  uniqueHash.clear();
	  uniqueHash = null;
	}
    }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBNameSpaceHandle

------------------------------------------------------------------------------*/

/**
 * This class is intended to be the targets of elements of a name
 * space's {@link arlut.csd.ganymede.server.DBNameSpace#uniqueHash
 * uniqueHash}.  The fields in this class are used to keep track of
 * who currently 'owns' a given value, and whether or not there is
 * actually any field in the namespace that really contains that
 * value.
 */

class DBNameSpaceHandle implements Cloneable {

  /**
   * So that the namespace hash can be used as an index,
   * persistentFieldInvid always points to the object that contained
   * the field that contained this value at the time this field was
   * last committed in a transaction.
   *
   * persistentFieldInvid will be null if the value pointing to
   * this handle has not been committed into the database outside of
   * an active transaction.
   */

  private Invid persistentFieldInvid = null;

  /**
   * If this handle is associated with a value that has been
   * checked into the database, persistentFieldId will be the field
   * number for the field that holds that value in the database,
   * within the object referenced by persistentFieldInvid.
   */

  private short persistentFieldId = -1;

  /* -- */

  /**
   * Constructor used by the system to originate a value when reading
   * from the database.
   */

  public DBNameSpaceHandle(DBField field)
  {
    setPersistentField(field);
  }

  /**
   * This method creates a new editable DBNameSpaceEditingHandle that
   * will revert to this handle if the DBEditSet is aborted.
   *
   * The checked out handle is returned with a null shadowField and
   * shadowFieldB, as would be appropriate if a user was unmarking a
   * value in the namespace uniqueHash.
   *
   * If the handle is being checked out by a non-interactive xmlclient
   * that needs to address the shadowFieldB member, it will need to be
   * sure to set the shadowField() to point to the appropriate field
   * at check out time.
   */

  public DBNameSpaceEditingHandle checkout(DBEditSet editSet)
  {
    DBNameSpaceHandle newHandle = new DBNameSpaceEditingHandle(editSet, this);

    newHandle.persistentFieldInvid = persistentFieldInvid;
    newHandle.persistentFieldId = persistentFieldId;

    return (DBNameSpaceEditingHandle) newHandle;
  }

  /**
   * This method returns true if this handle has been checked out of
   * the DBNameSpace.uniqueHash by a transaction, or false otherwise.
   */

  public boolean isCheckedOut()
  {
    return false;
  }

  /**
   * This method returns true if the namespace-managed value that
   * this handle is associated with is held in a committed object in the
   * Ganymede data store.
   *
   * If this method returns false, that means that this handle must
   * be associated with a field in an active DBEditSet's transaction
   * set, or else we wouldn't have a handle for it.
   */

  public boolean isPersisted()
  {
    return persistentFieldInvid != null;
  }

  /**
   * This method associates this value with a DBField that is
   * persisted (or will be persisted?) in the Ganymede persistent
   * store.
   */

  public void setPersistentField(DBField field)
  {
    if (field != null)
      {
	persistentFieldInvid = field.getOwner().getInvid();
	persistentFieldId = field.getID();
      }
    else
      {
	persistentFieldInvid = null;
	persistentFieldId = -1;
      }
  }

  /**
   * If the value that this handle is associated with is stored in
   * the Ganymede server's persistent data store (i.e., that this
   * handle is associated with a field in an already-committed
   * object), this method will return a pointer to the DBField that
   * contains this handle's value in the committed data store.
   */

  public DBField getPersistentField()
  {
    return getPersistentField((DBSession) null);
  }

  /**
   * If the value that this handle is associated with is stored in
   * the Ganymede server's persistent data store (i.e., that this
   * handle is associated with a field in an already-committed
   * object), this method will return a pointer to the DBField that
   * contains this handle's value in the committed data store.
   *
   * Note that if the DBEditSet passed in is currently editing the
   * object which is identified by persistentFieldInvid, the DBField
   * returned will be the editable version of the field from the
   * DBEditObject the session is working with.  This may be something
   * of a surprise, as the field returned may not actually contain the
   * value sought.
   */

  public DBField getPersistentField(DBEditSet editSet)
  {
    return getPersistentField(editSet.session);
  }

  /**
   * If the value that this handle is associated with is stored in
   * the Ganymede server's persistent data store (i.e., that this
   * handle is associated with a field in an already-committed
   * object), this method will return a pointer to the DBField that
   * contains this handle's value in the committed data store.
   *
   * Note that if the GanymedeSession passed in is currently
   * editing the object which is identified by persistentFieldInvid,
   * the DBField returned will be the editable version of the field
   * from the DBEditObject the session is working with.  This may be
   * something of a surprise, as the field returned may not actually
   * contain the value sought.
   */

  public DBField getPersistentField(GanymedeSession gsession)
  {
    return getPersistentField(gsession.session);
  }

  /**
   * If the value that this handle is associated with is stored in
   * the Ganymede server's persistent data store (i.e., that this
   * handle is associated with a field in an already-committed
   * object), this method will return a pointer to the DBField that
   * contains this handle's value in the committed data store.
   *
   * Note that if the DBSession passed in is currently
   * editing the object which is identified by persistentFieldInvid,
   * the DBField returned will be the editable version of the field
   * from the DBEditObject the session is working with.  This may be
   * something of a surprise, as the field returned may not actually
   * contain the value sought.
   */

  public DBField getPersistentField(DBSession session)
  {
    if (persistentFieldInvid == null)
      {
	return null;
      }

    if (session != null)
      {
	DBObject _obj = session.viewDBObject(persistentFieldInvid);

	if (_obj == null)
	  {
	    return null;
	  }
	
	return (DBField) _obj.getField(persistentFieldId);
      }
    else
      {
	// during start-up, before we have a session available

	DBObject _obj = Ganymede.db.viewDBObject(persistentFieldInvid);

	if (_obj == null)
	  {
	    return null;
	  }

	return (DBField) _obj.getField(persistentFieldId);
      }
  }

  /**
   * This method returns true if the namespace-constrained value
   * controlled by this handle is being edited by the GanymedeSession
   * provided.
   */

  public boolean isEditedByUs(GanymedeSession session)
  {
    return false;
  }

  /**
   * This method returns true if the namespace-constrained value
   * controlled by this handle is being edited by the transaction
   * provided.
   */

  public boolean isEditedByUs(DBEditSet editSet)
  {
    return false;
  }

  /**
   * This method returns true if the namespace-constrained value
   * controlled by this handle is being edited by some active
   * transaction other than the editSet passed in.
   */

  public boolean isEditedByOtherTransaction(DBEditSet editSet)
  {
    return false;
  }

  /**
   * If this namespace-managed value is being edited in an active
   * Ganymede transaction, this method may be used to set a pointer to
   * the editable DBField which contains the constrained value in the
   * active transaction.
   */

  public void setShadowField(DBField newShadow)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * If this namespace-managed value is being edited in an active
   * Ganymede transaction, this method will return a pointer to the
   * editable DBField which contains the constrained value in the active
   * transaction.
   */

  public DBField getShadowField()
  {
    return null;
  }

  /**
   * If this namespace-managed value is being edited in an active,
   * non-interactive Ganymede transaction, this method may be used to
   * set a pointer to the editable DBField which aspires to contain
   * the constrained value in the active transaction.
   *
   * This is the 'B' shadowField because it is not a firm association,
   * and cannot be one unless and until the original persistent field
   * that contains the constrained value is made to release the value.
   * At the time the constrained value is released from the earlier
   * field, shadowField will be set to shadowFieldB, and shadowFieldB
   * will be cleared.
   */

  public void setShadowFieldB(DBField newShadow)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * If this namespace-managed value is being edited in an active,
   * non-interactive Ganymede transaction, this method will return a
   * pointer to the editable DBField which is proposed to contain the
   * constrained value once the existing use of the value is cleared.
   */

  public DBField getShadowFieldB()
  {
    return null;
  }

  /**
   * This method is used to verify that this handle points to the same
   * field as the one specified by the parameter list.
   */

  public boolean matches(DBField field)
  {
    return (this.matches(field.getOwner().getInvid(), field.getID()));
  }

  /**
   * This method is used to verify that this handle points to the same
   * field as the one specified by the parameter list.
   */

  public boolean matches(Invid persistentFieldInvid, short persistentFieldId)
  {
    return (this.persistentFieldInvid == persistentFieldInvid) &&
      (this.persistentFieldId == persistentFieldId);
  }

  /**
   * This method is used to verify that this handle points to the same
   * kind of field as the one specified by the parameter list.
   */

  public boolean matchesFieldType(short objectType, short persistentFieldId)
  {
    return (this.persistentFieldInvid.getType() == objectType) &&
      (this.persistentFieldId == persistentFieldId);
  }

  /**
   * Returns true if the given field is associated with this handle in
   * any of the persistent, transaction-local, or xml transaction
   * secondary field slots.
   */

  public boolean matchesAnySlot(DBField field)
  {
    return this.matches(field);
  }

  /**
   * Returns true if this handle has been reserved during an editing
   * transaction.
   */

  public boolean isReserved()
  {
    return false;
  }

  /**
   * Used to mark this handle as being reserved by the editing
   * transaction.
   */

  public void setReserved(boolean reserved)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * We want to allow cloning.
   */

  public Object clone()
  {
    try
      {
	return super.clone();
      }
    catch (CloneNotSupportedException ex)
      {
	throw new RuntimeException(ex.getMessage());
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                        DBNameSpaceEditingHandle

------------------------------------------------------------------------------*/

class DBNameSpaceEditingHandle extends DBNameSpaceHandle {

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts =
    TranslationService.getTranslationService("arlut.csd.ganymede.server.DBNameSpaceEditingHandle");

  // ---

  /**
   * The DBNameSpaceHandle that this handle is replacing during value
   * manipulation by a transaction.  If the transaction's namespace
   * manipulations are cancelled or rolled back, this handle will be
   * take this editing handle's place in the namespace's uniqueHash.
   */

  private DBNameSpaceHandle original;

  /**
   * If editingTransaction is null, that means that the field
   * containing the value tracked by this handle is 'checked in' to
   * the datastore.
   *
   * If editingTransaction is not null, a transaction has checked the
   * value tracked by this handle out for manipulation, and no other
   * transaction may mess with that value's binding until the first
   * transaction has released this handle.
   */

  private DBEditSet editingTransaction;

  /**
   * if this handle is currently being edited by an editset,
   * shadowField points to the field in the transaction that contains
   * this value.  If the transaction is committed, the DBField pointer
   * in shadowField will be transferred to persistentFieldInvid and
   * persistentFieldId.  If this value is not being manipulated by a
   * transaction, shadowField will be equal to null.
   */

  private DBField shadowField;

  /**
   * Non-interactive transactions need to be able to shuffle
   * namespace values between two fields in the data store, even if
   * the operation to mark the unique value for association with a
   * second field is done before the operation to unlink the unique
   * value from the persistently stored field is done.
   *
   * To support this, we have both shadowField and shadowFieldB,
   * along the lines of the A-B-C values used in swapping values
   * between two memory locations with the aid of a third.
   *
   * This is the 'B' shadowField because it is not a firm
   * association, and cannot be one unless and until the original
   * persistent field that contains the constrained value is made to
   * release the value.  At the time the constrained value is released
   * from the earlier field, shadowField will be set to shadowFieldB,
   * and shadowFieldB will be cleared.
   */

  private DBField shadowFieldB;

  /**
   * If true, this value has been reserved.
   */

  private boolean reserved;

  /* -- */

  /**
   * Constructor used by a transaction marking a value that is new to
   * the namespace.
   */

  public DBNameSpaceEditingHandle(DBEditSet owner, DBNameSpaceHandle original)
  {
    super(null);

    this.editingTransaction = owner;
    this.original = original;
  }

  /**
   * This method returns true if this handle has been checked out of
   * the DBNameSpace.uniqueHash by a transaction, or false otherwise.
   */

  public boolean isCheckedOut()
  {
    return true;
  }

  /**
   * This method creates a new editable DBNameSpaceEditingHandle that
   * will revert to this handle if the DBEditSet is aborted.
   *
   * The checked out handle is returned with a null shadowField and
   * shadowFieldB, as would be appropriate if a user was unmarking a
   * value in the namespace uniqueHash.
   *
   * If the handle is being checked out by a non-interactive xmlclient
   * that needs to address the shadowFieldB member, it will need to be
   * sure to set the shadowField() to point to the appropriate field
   * at check out time.
   */

  public DBNameSpaceEditingHandle checkout(DBEditSet editSet)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * This method returns the DBNameSpaceHandle that should be used to
   * replace this DBNameSpaceHandle if this transaction is committed.
   */

  public DBNameSpaceHandle getNewHandle()
  {
    if (getShadowField() == null)
      {
	return null;
      }
    else
      {
	return new DBNameSpaceHandle(getShadowField());
      }
  }

  /**
   * This method returns the DBNameSpaceHandle that should be used to
   * replace this DBNameSpaceHandle if this transaction is aborted.
   */

  public DBNameSpaceHandle getOriginal()
  {
    return original;
  }

  /**
   * This method returns true if the namespace-constrained value
   * controlled by this handle is being edited by the GanymedeSession
   * provided.
   */

  public boolean isEditedByUs(GanymedeSession session)
  {
    return session.getSession().getEditSet() == editingTransaction;
  }

  /**
   * This method returns true if the namespace-constrained value
   * controlled by this handle is being edited by the transaction
   * provided.
   */

  public boolean isEditedByUs(DBEditSet editSet)
  {
    return editSet == editingTransaction;
  }

  /**
   * This method returns true if the namespace-constrained value
   * controlled by this handle is being edited by some active
   * transaction other than the editSet passed in.
   */

  public boolean isEditedByOtherTransaction(DBEditSet editSet)
  {
    return editSet != editingTransaction;
  }

  /**
   * Returns true if this handle has been reserved during an editing
   * transaction.
   */

  public boolean isReserved()
  {
    return this.reserved;
  }

  /**
   * Used to mark this handle as being reserved by the editing
   * transaction.
   */

  public void setReserved(boolean reserved)
  {
    this.reserved = reserved;
  }

  /**
   * If this namespace-managed value is being edited in an active
   * Ganymede transaction, this method may be used to set a pointer to
   * the editable DBField which contains the constrained value in the
   * active transaction.
   */

  public void setShadowField(DBField newShadow)
  {
    shadowField = newShadow;
  }

  /**
   * If this namespace-managed value is being edited in an active
   * Ganymede transaction, this method will return a pointer to the
   * editable DBField which contains the constrained value in the active
   * transaction.
   */

  public DBField getShadowField()
  {
    return shadowField;
  }

  /**
   * If this namespace-managed value is being edited in an active,
   * non-interactive Ganymede transaction, this method may be used to
   * set a pointer to the editable DBField which aspires to contain
   * the constrained value in the active transaction.
   *
   * This is the 'B' shadowField because it is not a firm association,
   * and cannot be one unless and until the original persistent field
   * that contains the constrained value is made to release the value.
   * At the time the constrained value is released from the earlier
   * field, shadowField will be set to shadowFieldB, and shadowFieldB
   * will be cleared.
   */

  public void setShadowFieldB(DBField newShadow)
  {
    shadowFieldB = newShadow;
  }

  /**
   * If this namespace-managed value is being edited in an active,
   * non-interactive Ganymede transaction, this method will return a
   * pointer to the editable DBField which is proposed to contain the
   * constrained value once the existing use of the value is cleared.
   */

  public DBField getShadowFieldB()
  {
    return shadowFieldB;
  }

  /**
   * This helper method is intended to report when a non-interactive
   * xml session has two objects checked out for editing with a
   * conflicting value at transaction commit time.
   */

  public String getConflictString()
  {
    // "Conflict: {0} and {1}"
    return ts.l("getConflictString.template",
                String.valueOf(shadowField), String.valueOf(shadowFieldB));
  }

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    if (editingTransaction != null)
      {
	result.append("editingTransaction == " + editingTransaction.toString());
      }

    if (result.length() != 0)
      {
	result.append(", ");
      }

    if (shadowField != null)
      {
	result.append(", shadowField == " + shadowField.toString());
      }

    if (shadowFieldB != null)
      {
	result.append(", shadowFieldB == " + shadowFieldB.toString());
      }

    return result.toString();
  }
}
