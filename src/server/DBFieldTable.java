/*

   DBFieldTable.java

   A customized variant of the java.util.Hashtable class that is
   tuned for use in managing fields in a Ganymede DBObject.
   
   Created: 9 June 1998
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 1999/01/22 18:05:32 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBFieldTable

------------------------------------------------------------------------------*/

/**
 *
 * A customized variant of the java.util.Hashtable class that is
 * tuned for use in managing fields in a Ganymede DBObject.
 * 
 * @version $Revision: 1.4 $ %D%, Created: 9 June 1998
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 *
 */

public class DBFieldTable {

  /**
   * The hash table data.
   */

  private transient DBField table[];

  /**
   * The total number of entries in the hash table.
   */

  private transient int count;

  /**
   * Rehashes the table when count exceeds this threshold.
   */

  private int threshold;

  /**
   * The load factor for the hashtable.
   */

  private float loadFactor;

  /**
   * Constructs a new, empty DBFieldTable with the specified initial 
   * capacity and the specified load factor. 
   *
   * @param      initialCapacity   the initial capacity of the hashtable.
   * @param      loadFactor        a number between 0.0 and 1.0.
   * @exception  IllegalArgumentException  if the initial capacity is less
   *               than or equal to zero, or if the load factor is less than
   *               or equal to zero.
   */

  public DBFieldTable(int initialCapacity, float loadFactor) 
  {
    if ((initialCapacity <= 0) || (loadFactor <= 0.0) || (loadFactor > 1.0)) 
      {
	throw new IllegalArgumentException();
      }

    this.loadFactor = loadFactor;
    table = new DBField[initialCapacity];
    threshold = (int)(initialCapacity * loadFactor);
  }

  /**
   * Constructs a new, empty DBFieldTable with the specified initial capacity
   * and default load factor.
   *
   * @param   initialCapacity   the initial capacity of the hashtable.
   */

  public DBFieldTable(int initialCapacity) 
  {
    this(initialCapacity, 0.75f);
  }

  /**
   * Constructs a new, empty DBFieldTable with a default capacity and load
   * factor. 
   *
   */

  public DBFieldTable() 
  {
    this(101, 0.75f);
  }

  /**
   * Returns the number of objects in this DBFieldTable.
   *
   * @return  the number of objects in this DBFieldTable.
   *
   */

  public int size() 
  {
    return count;
  }

  /**
   * Tests if this DBFieldTable contains no objects.
   *
   * @return  <code>true</code> if this DBFieldTable contains no values;
   *          <code>false</code> otherwise.
   *
   */

  public boolean isEmpty() 
  {
    return count == 0;
  }

  /**
   * Returns an enumeration of the objects in this DBFieldTable.
   * Use the Enumeration methods on the returned object to fetch the elements
   * sequentially.
   *
   * @return  an enumeration of the objects in this DBFieldTable.
   * @see     java.util.Enumeration
   *
   */

  public synchronized Enumeration elements()
  {
    return new DBFieldTableEnumerator(table);
  }

  /**
   * Tests if the DBField value is contained in this DBFieldTable.
   *
   * @param      value   a DBField to search for.
   * @exception  NullPointerException  if the value is <code>null</code>.
   *
   */

  public boolean contains(DBField value) 
  {
    if (value == null) 
      {
	throw new NullPointerException();
      }

    return containsKey(value.getID());
  }

  /**
   * Tests if a DBField with the specified object id is in this DBFieldTable.
   * 
   * @param   key   possible object id.
   *
   */

  public boolean containsKey(Short key) 
  {
    return containsKey(key.shortValue());
  }

  /**
   * Tests if a DBField with the specified object id is in this DBFieldTable.
   * 
   * @param   key   possible object id.
   *
   */

  public synchronized boolean containsKey(short key) 
  {
    DBField tab[] = table;

    short index = (short) ((key & 0x7FFF) % tab.length);

    for (DBField e = tab[index] ; e != null ; e = e.next) 
      {
	if (e.getID() == key)
	  {
	    return true;
	  }
      }
    
    return false;
  }

  /**
   *
   * Returns the DBField with the specified key from this DBFieldTable, or
   * null if no object with that id is in this table.
   *
   */

  public DBField getNoSync(short key) 
  {
    DBField tab[] = table;

    short index = (short) ((key & 0x7FFF) % tab.length);

    for (DBField e = tab[index] ; e != null ; e = e.next) 
      {
	if (e.getID() == key)
	  {
	    return e;
	  }
      }

    return null;
  }

  /**
   *
   * Returns the DBField with the specified key from this DBFieldTable, or
   * null if no object with that id is in this table.
   *
   */

  public synchronized DBField get(short key) 
  {
    DBField tab[] = table;

    short index = (short) ((key & 0x7FFF) % tab.length);

    for (DBField e = tab[index] ; e != null ; e = e.next) 
      {
	if (e.getID() == key)
	  {
	    return e;
	  }
      }

    return null;
  }

  /**
   *
   * Rehashes the contents of the DBFieldTable into a DBFieldTable
   * with a larger capacity. This method is called automatically when
   * the number of keys in the hashtable exceeds this DBFieldTable's
   * capacity and load factor.
   * 
   */

  protected void rehash() 
  {
    int oldCapacity = table.length;
    DBField oldTable[] = table;

    int newCapacity = oldCapacity * 2 + 1;
    DBField newTable[] = new DBField[newCapacity];

    threshold = (int) (newCapacity * loadFactor);
    table = newTable;

    //System.out.println("rehash old=" + oldCapacity + ", new=" +
    //newCapacity + ", thresh=" + threshold + ", count=" + count);

    for (int i = oldCapacity ; i-- > 0 ;) 
      {
	for (DBField old = oldTable[i] ; old != null ; ) 
	  {
	    DBField e = old;
	    old = old.next;
	    
	    short index = (short) ((e.getID() & 0x7FFF) % newCapacity);
	    e.next = newTable[index];
	    newTable[index] = e;
	  }
      }
  }

  /**
   *
   * Inserts a DBField into this DBFieldTable.
   *
   * This put is not sync'ed, and should only be used with
   * higher level sync provisions.
   *
   */

  public void putNoSync(DBField value) 
  {
    // Make sure the value is not null

    if (value == null) 
      {
	throw new NullPointerException();
      }

    // Makes sure the object is not already in the hashtable.
    
    removeNoSync(value.getID());
    
    DBField tab[] = table;
    short hash = value.getID();
    short index = (short) ((hash & 0x7FFF) % tab.length);

    if (count > threshold) 
      {
	rehash();
	putNoSync(value);

	return;
      } 

    // Insert the new entry.

    value.next = tab[index];
    tab[index] = value;
    count++;
    return;
  }

  /**
   *
   * Inserts a DBField into this DBFieldTable
   *
   */

  public synchronized void put(DBField value) 
  {
    // Make sure the value is not null

    if (value == null) 
      {
	throw new NullPointerException();
      }

    // Makes sure the object is not already in the hashtable.
    // Note that we are sync'ed, so we can use the non-sync'ed
    // removeNoSync().
    
    removeNoSync(value.getID());

    if (count > threshold) 
      {
	rehash();
      }

    DBField tab[] = table;
    short hash = value.getID();
    short index = (short) ((hash & 0x7FFF) % tab.length);

    // Insert the new entry.
    
    value.next = tab[index];
    tab[index] = value;
    count++;
    
    return;
  }

  /**
   *
   * Inserts a DBField into this DBFieldTable.
   *
   * This put is not sync'ed, and should only be used with
   * higher level sync provisions.
   *
   */

  public void putNoSyncNoRemove(DBField value) 
  {
    // Make sure the value is not null

    if (value == null) 
      {
	throw new NullPointerException();
      }

    DBField tab[] = table;
    short hash = value.getID();
    short index = (short) ((hash & 0x7FFF) % tab.length);

    if (count > threshold) 
      {
	rehash();
	putNoSync(value);

	return;
      } 

    // Insert the new entry.

    value.next = tab[index];
    tab[index] = value;
    count++;
    return;
  }

  /**
   *
   * Removes the DBField with the given id from this DBFieldTable.
   *
   */

  public void removeNoSync(short key) 
  {
    DBField tab[] = table;
    short index = (short) ((key & 0x7FFF) % tab.length);

    for (DBField e = tab[index], prev = null ; e != null ; prev = e, e = e.next) 
      {
	if (e.getID() == key)
	  {
	    if (prev != null) 
	      {
		prev.next = e.next;
	      } 
	    else
	      {
		tab[index] = e.next;
	      }

	    count--;

	    return;
	  }
      }

    return;
  }

  /**
   *
   * Removes the DBField with the given id from this DBFieldTable.
   *
   */

  public synchronized void remove(short key) 
  {
    DBField tab[] = table;
    short index = (short) ((key & 0x7FFF) % tab.length);

    for (DBField e = tab[index], prev = null ; e != null ; prev = e, e = e.next) 
      {
	if (e.getID() == key)
	  {
	    if (prev != null) 
	      {
		prev.next = e.next;
	      } 
	    else
	      {
		tab[index] = e.next;
	      }

	    count--;

	    return;
	  }
      }

    return;
  }

  /**
   *
   * Clears this DBFieldTable.
   *
   */

  public synchronized void clear() 
  {
    DBField tab[] = table;

    /* -- */

    for (int index = tab.length; --index >= 0; )
      {
	tab[index] = null;
      }

    count = 0;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                         DBFieldTableEnumerator

------------------------------------------------------------------------------*/

/**
 * A DBFieldTable enumerator class.  This class should remain opaque 
 * to the client. It will use the Enumeration interface. 
 */

class DBFieldTableEnumerator implements Enumeration {

  short index;
  DBField table[];
  DBField entry;

  /* -- */

  DBFieldTableEnumerator(DBField table[]) 
  {
    this.table = table;
    this.index = (short) table.length;
  }
	
  public boolean hasMoreElements() 
  {
    if (entry != null) 
      {
	return true;
      }

    while (index-- > 0) 
      {
	if ((entry = table[index]) != null) 
	  {
	    return true;
	  }
      }

    return false;
  }

  public Object nextElement() 
  {
    if (entry == null) 
      {
	while ((index-- > 0) && ((entry = table[index]) == null));
      }

    if (entry != null) 
      {
	DBField e = entry;
	entry = e.next;
	return e;
      }

    throw new NoSuchElementException("HashtableEnumerator");
  }
}
