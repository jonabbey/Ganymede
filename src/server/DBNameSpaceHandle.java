/*
   GASH 2

   DBNameSpace.java

   The GANYMEDE object storage system.

   Created: 15 January 1999
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 1999/01/16 01:27:40 $
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBNameSpaceHandle

------------------------------------------------------------------------------*/

/**
 *
 * <p>This class is intended to be the targets of elements of a name
 * space's unique value hash.  The fields in this class are used to
 * keep track of who currently 'owns' a given value, and whether or not
 * there is actually any field in the namespace that really contains
 * that value.</p>
 *
 *
 * <p>This class will be manipulated by the DBNameSpace class and by the
 * DBEditObject class.</p>
 *
 */

class DBNameSpaceHandle {

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

  boolean inuse;

  /**
   * so the namespace hash can be used as an index
   * field always points to the field that contained
   * field always points to the field that contained
   * this value at the time this field was last
   * committed in a transaction
   */

  DBField field;

  /**
   * if this handle is currently being edited
   * by an editset, shadowField points to the
   * new field
   */

  DBField shadowField;

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
    this.field = field;
  }

  public boolean matches(DBEditSet set)
  {
    return (this.owner == set);
  }
}
