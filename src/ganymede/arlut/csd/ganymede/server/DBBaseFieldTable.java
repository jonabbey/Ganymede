/*

   DBBaseFieldTable.java

   A customized variant of the java.util.Hashtable class that is
   tuned for use as Ganymede's base field hashes.
   
   Created: 9 June 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2009
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

import java.lang.Iterable;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                DBBaseFieldTable

------------------------------------------------------------------------------*/

/**
 * <P>A customized variant of the java.util.Hashtable class that is
 * tuned for use in managing
 * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}s
 * in a Ganymede {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}.</P>
 * 
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBBaseFieldTable implements Iterable<DBObjectBaseField> {

  /**
   * Array of DBObjectBaseField objects, sorted in id order.
   */

  private transient DBObjectBaseField[] table;

  /**
   * The total number of entries in the hash table.
   */

  private transient int count;

  /**
   * Constructs a new, empty DBBaseFieldTable.
   */

  public DBBaseFieldTable()
  {
    table = new DBObjectBaseField[0];
  }
  
  /**
   * This method sets the contents of the DBBaseFieldTable.  The
   * fieldAry array must be sorted in increasing field id order.
   */
  
  public void replaceContents(DBObjectBaseField fieldAry[])
  {
    this.table = fieldAry;
  }

  /**
   * Returns the number of objects in this DBBaseFieldTable.
   *
   * @return  the number of objects in this DBBaseFieldTable.
   */

  public int size() 
  {
    return count;
  }

  /**
   * Tests if this DBBaseFieldTable contains no objects.
   *
   * @return  <code>true</code> if this DBBaseFieldTable contains no values;
   *          <code>false</code> otherwise.
   */

  public boolean isEmpty() 
  {
    return count == 0;
  }

  /**
   * Returns an Iterator of the objects in this DBBaseFieldTable.
   *
   * Use the Iterator methods on the returned object to fetch the
   * elements sequentially.
   *
   * This method allows DBBaseFieldTable to support the Java 5 foreach
   * loop construct.
   *
   * @return  an Iterator of the objects in this DBObjectTable.
   * @see     java.util.Iterator
   */

  public synchronized Iterator<DBObjectBaseField> iterator()
  {
    return new DBBaseFieldTableIterator(table);
  }

  /**
   * Returns an Iterator of the built-in DBObjectBaseField objects in
   * this DBBaseFieldTable.
   *
   * Use the Iterator methods on the returned object to fetch the
   * elements sequentially.
   */

  public synchronized Iterator<DBObjectBaseField> builtInIterator()
  {
    return new DBBaseFieldTableBuiltInIterator(table);
  }

  /**
   * Returns an enumeration of the objects in this DBBaseFieldTable.
   * Use the Enumeration methods on the returned object to fetch the elements
   * sequentially.
   *
   * @return  an enumeration of the objects in this DBBaseFieldTable.
   * @see     java.util.Enumeration
   *
   */

  public synchronized Enumeration elements()
  {
    return new DBBaseFieldTableEnumerator(table);
  }

  /**
   * Tests if the DBObjectBaseField value is contained in this DBBaseFieldTable.
   *
   * @param      value   a DBObjectBaseField to search for.
   * @exception  NullPointerException  if the value is <code>null</code>.
   *
   */

  public boolean contains(DBObjectBaseField value) 
  {
    int index = java.util.Arrays.binarySearch(table, value);

    if (index < 0)
      {
	return false;
      }

    return table[index].equals(value);
  }

  /**
   * Tests if a DBObjectBaseField with the specified object id is in this DBBaseFieldTable.
   * 
   * @param   key   possible object id.
   *
   */

  public boolean containsKey(Short key) 
  {
    return java.util.Arrays.binarySearch(table, key) >= 0;
  }

  /**
   * Tests if a DBObjectBaseField with the specified object id is in this DBBaseFieldTable.
   * 
   * @param   key   possible object id.
   */

  public synchronized boolean containsKey(short key) 
  {
    return java.util.Arrays.binarySearch(table, key) >= 0;
  }

  public synchronized DBObjectBaseField[] getIDSortedArray()
  {
    return java.util.Arrays.copyOf(table, table.length);
  }

  /**
   *
   * Returns the DBObjectBaseField with the specified key from this DBBaseFieldTable, or
   * null if no object with that id is in this table.
   *
   */

  public DBObjectBaseField getNoSync(short key) 
  {
    int index = java.util.Arrays.binarySearch(table, key);

    if (index < 0)
      {
	return null;
      }

    return table[index];
  }

  /**
   *
   * Returns the DBObjectBaseField with the specified key from this DBBaseFieldTable, or
   * null if no object with that id is in this table.
   *
   */

  public synchronized DBObjectBaseField get(short key) 
  {
    int index = java.util.Arrays.binarySearch(table, key);

    if (index < 0)
      {
	return null;
      }

    return table[index];
  }

  /**
   *
   * Returns the DBObjectBaseField with the specified name from this
   * DBBaseFieldTable, or null if no object with that name is in this
   * table.
   *
   * This method is unprotected by synchronization, so you must be
   * sure to use higher level synchronization to use this safely.
   */

  public DBObjectBaseField getNoSync(String name) 
  {
    return this.findByName(name);
  }

  /**
   * Returns the DBObjectBaseField with the specified name from this
   * DBBaseFieldTable, or null if no object with that name is in this
   * table.
   *
   * The comparisons done in this method are case insensitive.
   */

  public synchronized DBObjectBaseField get(String name) 
  {
    return this.findByName(name);
  }

  /**
   *
   * Inserts a DBObjectBaseField into this DBBaseFieldTable.
   *
   * This put is not sync'ed, and should only be used with
   * higher level sync provisions.
   *
   */

  public void putNoSync(DBObjectBaseField value) 
  {
    // Make sure the value is not null

    if (value == null) 
      {
	throw new NullPointerException();
      }

    int index = java.util.Arrays.binarySearch(table, value);

    if (index < 0)
      {
	// we'll need to expand to make room

	DBObjectBaseField[] newTable = new DBObjectBaseField[table.length + 1];

	if (table.length == 0)
	  {
	    newTable[0] = value;
	  }
	else
	  {
	    boolean found = false;

	    int j = 0;
	    int i = 0;

	    while (j < newTable.length)
	      {
		if (i < table.length)
		  {
		    DBObjectBaseField field = table[i];

		    if (!found && value.getID() < field.getID())
		      {
			// insert into the beginning or middle

			newTable[j++] = value;
			found = true;
		      }
		    else
		      {
			newTable[j++] = table[i++];
		      }
		  }
		else
		  {
		    // append to the end

		    newTable[j++] = value;
		  }
	      }
	  }

	table = newTable;

	return;
      }

    // else, we're replacing

    table[index] = value;
  }

  /**
   *
   * Inserts a DBObjectBaseField into this DBBaseFieldTable
   *
   */

  public synchronized void put(DBObjectBaseField value) 
  {
    putNoSync(value);
  }

  /**
   *
   * Inserts a DBObjectBaseField into this DBBaseFieldTable.
   *
   * This put is not sync'ed, and should only be used with
   * higher level sync provisions.
   *
   */

  public void putNoSyncNoRemove(DBObjectBaseField value) 
  {
    putNoSync(value);
  }

  /**
   *
   * Removes the DBObjectBaseField with the given id from this DBBaseFieldTable.
   *
   */

  public void removeNoSync(short key) 
  {
    int index = java.util.Arrays.binarySearch(table, key);

    if (index < 0)
      {
	return;
      }

    // we'll need to shrink by one

    DBObjectBaseField[] newTable = new DBObjectBaseField[table.length - 1];

    for (int j = 0, i = 0; i < table.length; i++)
      {
	DBObjectBaseField field = table[i];

	if (i != index)
	  {
	    newTable[j++] = table[i++];
	  }
	else
	  {
	    i++;
	  }
      }

    table = newTable;
  }

  /**
   *
   * Removes the DBObjectBaseField with the given id from this DBBaseFieldTable.
   *
   */

  public synchronized void remove(short key) 
  {
    removeNoSync(key);
  }

  /**
   *
   * Clears this DBBaseFieldTable.
   *
   */

  public synchronized void clear() 
  {
    table = new DBObjectBaseField[0];
  }

  /**
   * This unsynchronized private helper method looks up
   * DBObjectBaseFields by name, using a case-insensitive comparison.
   */

  private final DBObjectBaseField findByName(String name)
  {
    for (int i = 0; i < table.length; i++)
      {
	if (table[i].getName().equalsIgnoreCase(name))
	  {
	    return table[i];
	  }
      }

    return null;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                      DBBaseFieldTableEnumerator

------------------------------------------------------------------------------*/

/**
 * A DBBaseFieldTable enumerator class.  This class should remain opaque 
 * to the client. It will use the Enumeration interface. 
 */

class DBBaseFieldTableEnumerator implements Enumeration {

  short index;
  DBObjectBaseField table[];

  /* -- */

  DBBaseFieldTableEnumerator(DBObjectBaseField table[]) 
  {
    this.table = table;
    this.index = 0;
  }
	
  public boolean hasMoreElements() 
  {
    return index < table.length;
  }

  public Object nextElement() 
  {
    return table[index++];
  }
}


/*------------------------------------------------------------------------------
                                                                           class
                                                        DBBaseFieldTableIterator

------------------------------------------------------------------------------*/

/**
 * A DBBaseFieldTable Iterator class.  This class should remain opaque 
 * to the client. It will use the Iterator interface. 
 */

class DBBaseFieldTableIterator implements Iterator<DBObjectBaseField> {

  short index;
  DBObjectBaseField table[];

  /* -- */

  DBBaseFieldTableIterator(DBObjectBaseField table[]) 
  {
    this.table = table;
    this.index = 0;
  }
	
  public boolean hasNext() 
  {
    return index < table.length;
  }

  public DBObjectBaseField next() 
  {
    return table[index++];
  }

  public void remove()
  {
    throw new UnsupportedOperationException();
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                 DBBaseFieldTableBuiltInIterator

------------------------------------------------------------------------------*/

/**
 * A DBBaseFieldTable Iterator class that iterates over the built-in
 * field definitions in a DBObjectBase, in ascending field id order.
 *
 * This class should remain opaque to the client.
 */

class DBBaseFieldTableBuiltInIterator implements Iterator<DBObjectBaseField> {

  short index;
  DBObjectBaseField table[];

  /* -- */

  DBBaseFieldTableBuiltInIterator(DBObjectBaseField table[]) 
  {
    this.table = table;
    this.index = 0;
  }
	
  public boolean hasNext() 
  {
    return index < table.length && table[index].isBuiltIn();
  }

  public DBObjectBaseField next() 
  {
    return table[index++];
  }

  public void remove()
  {
    throw new UnsupportedOperationException();
  }
}

