/*
   GASH 2

   DBNameSpaceHandle.java

   The GANYMEDE object storage system.

   Created: 15 January 1999
   Version: $Revision$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.Invid;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBNameSpaceHandle

------------------------------------------------------------------------------*/

/**
 * <p>This class is intended to be the targets of elements of a name
 * space's unique value hash.  The fields in this class are used to
 * keep track of who currently 'owns' a given value, and whether or not
 * there is actually any field in the namespace that really contains
 * that value.</p>
 *
 * <p>This class will be manipulated by the DBNameSpace class and by the
 * DBEditObject class.</p>
 */

class DBNameSpaceHandle implements Cloneable {

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBNameSpaceHandle");

  /**
   * if this value is currently being shuffled
   * by a transaction, this is the transaction
   */

  DBEditSet owner;

  /**
   * remember if the value was in use at the
   * start of the transaction
   */

  boolean original;

  /**
   * is the value currently in use?
   */

  private boolean inuse;

  /**
   * <P>So that the namespace hash can be used as an index,
   * persistentFieldInvid always points to the object that contained
   * the field that contained this value at the time this field was
   * last committed in a transaction.</P>
   *
   * <P>persistentFieldInvid will be null if the value pointing to
   * this handle has not been committed into the database outside of
   * an active transaction.</P>
   */

  private Invid persistentFieldInvid;

  /**
   * <P>If this handle is associated with a value that has been
   * checked into the database, persistentFieldId will be the field
   * number for the field that holds that value in the database,
   * within the object referenced by persistentFieldInvid.</P>
   */

  private short persistentFieldId;

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
   * <P>Non-interactive transactions need to be able to shuffle
   * namespace values between two fields in the data store, even if
   * the operation to mark the unique value for association with a
   * second field is done before the operation to unlink the unique
   * value from the persistently stored field is done.</P>
   *
   * <P>To support this, we have both shadowField and shadowFieldB,
   * along the lines of the A-B-C values used in swapping values
   * between two memory locations with the aid of a third.</P>
   *
   * <P>This is the 'B' shadowField because it is not a firm
   * association, and cannot be one unless and until the original
   * persistent field that contains the constrained value is made to
   * release the value.  At the time the constrained value is released
   * from the earlier field, shadowField will be set to shadowFieldB,
   * and shadowFieldB will be cleared.</P>
   */

  private DBField shadowFieldB;

  /* -- */

  public DBNameSpaceHandle(DBEditSet owner, boolean originalValue)
  {
    this.owner = owner;
    this.original = this.inuse = originalValue;
  }

  public DBNameSpaceHandle(DBEditSet owner, boolean originalValue, DBField field)
  {
    this.owner = owner;
    this.original = this.inuse = originalValue;

    setPersistentField(field);
  }

  public boolean matches(DBEditSet set)
  {
    return (this.owner == set);
  }

  /**
   * <p>This method returns true if the namespace-managed value that
   * this handle is associated with is held in a committed object in the
   * Ganymede data store.</p>
   *
   * <p>If this method returns false, that means that this handle must
   * be associated with a field in an active DBEditSet's transaction
   * set, or else we wouldn't have a handle for it.</p>
   */

  public boolean isPersisted()
  {
    return persistentFieldInvid != null;
  }

  /**
   * <p>This method associates this value with a DBField that is
   * persisted (or will be persisted?) in the Ganymede persistent
   * store.</p>
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
   * <p>If the value that this handle is associated with is stored in
   * the Ganymede server's persistent data store (i.e., that this
   * handle is associated with a field in an already-committed
   * object), this method will return a pointer to the DBField that
   * contains this handle's value in the committed data store.</p>
   *
   * <p>Note that if the GanymedeSession passed in is currently
   * editing the object which is identified by persistentFieldInvid,
   * the DBField returned will be the editable version of the field
   * from the DBEditObject the session is working with.  This may be
   * something of a surprise, as the field returned may not actually
   * contain the value sought.</p>
   */

  public DBField getPersistentField(GanymedeSession session)
  {
    if (persistentFieldInvid == null)
      {
	return null;
      }

    if (session != null)
      {
	DBObject _obj = session.session.viewDBObject(persistentFieldInvid);

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
   * Returns true if this DBNameSpaceHandle is referring to a value
   * that is in use, either in the persistent datastore, or in the
   * transaction referred to by the owner field.
   *
   * This method will return false if a transaction has checked out an
   * object for editing and then cleared this value from a field.  If
   * this transaction is aborted, then inuse will be set to true again
   * as part of the abort process.  If the transaction is committed,
   * all namespace handles owned by the owner transaction whose inuse
   * flags are false will be removed from the namespace.
   *
   * This method may also return false if a transaction has reserved
   * this value without setting it into a field.
   */

  public boolean isInUse()
  {
    return this.inuse;
  }

  /**
   * Changes the value of the inuse flag.  See {@link
   * arlut.csd.ganymede.server.DBNameSpaceHandle#isInUse()} for
   * interpretation.
   */

  public void setInUse(boolean val)
  {
    this.inuse = val;
  }

  /**
   * <p>This method returns true if the namespace-constrained value
   * controlled by this handle is being edited by the GanymedeSession
   * provided.</p>
   */

  public boolean isEditedByUs(GanymedeSession session)
  {
    return (owner != null && session.getSession().getEditSet() == owner);
  }

  /**
   * <p>This method returns true if the namespace-constrained value
   * controlled by this handle is being edited by the transaction
   * provided.</p>
   */

  public boolean isEditedByUs(DBEditSet editSet)
  {
    return (owner != null && editSet == owner);
  }

  /**
   * <p>If this namespace-managed value is being edited in an active
   * Ganymede transaction, this method may be used to set a pointer to
   * the editable DBField which contains the constrained value in the
   * active transaction.</p>
   */

  public void setShadowField(DBField newShadow)
  {
    shadowField = newShadow;
  }

  /**
   * <p>If this namespace-managed value is being edited in an active
   * Ganymede transaction, this method will return a pointer to the
   * editable DBField which contains the constrained value in the active
   * transaction.</p>
   */

  public DBField getShadowField()
  {
    return shadowField;
  }

  /**
   * <p>If this namespace-managed value is being edited in an active,
   * non-interactive Ganymede transaction, this method may be used to
   * set a pointer to the editable DBField which aspires to contain
   * the constrained value in the active transaction.</p>
   *
   * <p>This is the 'B' shadowField because it is not a firm
   * association, and cannot be one unless and until the original
   * persistent field that contains the constrained value is made to
   * release the value.  At the time the constrained value is released
   * from the earlier field, shadowField will be set to shadowFieldB,
   * and shadowFieldB will be cleared.</p>
   */

  public void setShadowFieldB(DBField newShadow)
  {
    shadowFieldB = newShadow;
  }

  /**
   * <p>If this namespace-managed value is being edited in an active,
   * non-interactive Ganymede transaction, this method will return a
   * pointer to the editable DBField which is proposed to contain the
   * constrained value once the existing use of the value is cleared.</p>
   */

  public DBField getShadowFieldB()
  {
    return shadowFieldB;
  }


  public Object clone()
  {
    // we should be clonable

    try
      {
	return super.clone();
      }
    catch (CloneNotSupportedException ex)
      {
	throw new RuntimeException(ex.getMessage());
      }
  }

  /**
   * <p>This method is used to verify that this handle points to the same
   * field as the one specified by the parameter list.</p>
   */

  public boolean matches(DBField field)
  {
    return (this.matches(field.getOwner().getInvid(), field.getID()));
  }

  /**
   * <p>This method is used to verify that this handle points to the same
   * field as the one specified by the parameter list.</p>
   */

  public boolean matches(Invid persistentFieldInvid, short persistentFieldId)
  {
    return (this.persistentFieldInvid == persistentFieldInvid) && (this.persistentFieldId == persistentFieldId);
  }

  /**
   * <p>This method is used to verify that this handle points to the same
   * kind of field as the one specified by the parameter list.</p>
   */

  public boolean matches (short objectType, short persistentFieldId)
  {
    return (this.persistentFieldInvid.getType() == objectType) &&
      (this.persistentFieldId == persistentFieldId);
  }

  /**
   * Affirmative dissolution for gc.
   */

  public void cleanup()
  {
    owner = null;
    persistentFieldInvid = null;
    shadowField = null;
    shadowFieldB = null;
  }

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    if (owner != null)
      {
	result.append("owner == " + owner.toString());
      }

    if (result.length() != 0)
      {
	result.append(", ");
      }

    if (original)
      {
	result.append("original");
      }
    else
      {
	result.append("!original");
      }

    if (inuse)
      {
	result.append(", inuse");
      }
    else
      {
	result.append(", !inuse");
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
}
