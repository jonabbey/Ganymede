/*

   GHashtable.java

   A subclass of Hashtable that supports case-insensitive
   hashing/retrieval.
   
   Created: 10 April 1997
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 2001/05/21 08:26:42 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      GHashtable

------------------------------------------------------------------------------*/

/**
 * <p>GHashtable is a Hashtable subclass that can map uppercase/lowercase keys
 * of the same string to identity.  It does this by basically mapping all
 * strings to the lowercase version internally.  The case sensitivity of
 * the hashtable is specified at hash creation time, and may not change
 * thereafter.</p>
 *
 * <p>This hashtable also has special support for handling arrays of Bytes as
 * keys in the hash, using the {@link arlut.csd.ganymede.IPwrap IPwrap}
 * class for I.P. address representation.</p>
 */

public class GHashtable extends Hashtable {

  private boolean caseInsensitive; // we don't allow this to change after creation

  /* -- */

  /**
   *
   * Fully specified constructor.
   *
   * @param initialCapacity as for Hashtable
   * @param loadFactor as for Hashtable
   * @param caseInsensitive if true, lowercase and uppercase string keys will be mapped together
   *
   * @see java.util.Hashtable
   *
   */

  public GHashtable(int initialCapacity, float loadFactor, boolean caseInsensitive)
  {
    super(initialCapacity, loadFactor);
    this.caseInsensitive = caseInsensitive;
  }

  /**
   *
   * Medium specified constructor.
   *
   * @param initialCapacity as for Hashtable
   * @param caseInsensitive if true, lowercase and uppercase string keys will be mapped together
   *
   * @see java.util.Hashtable
   *
   */

  public GHashtable(int initialCapacity, boolean caseInsensitive)
  {
    super(initialCapacity);
    this.caseInsensitive = caseInsensitive;
  }

  /**
   *
   * Least specified constructor.
   *
   * @param caseInsensitive if true, lowercase and uppercase string keys will be mapped together
   *
   * @see java.util.Hashtable
   *
   */

  public GHashtable(boolean caseInsensitive)
  {
    super();
    this.caseInsensitive = caseInsensitive;
  }

  public synchronized Enumeration keys()
  {
    if (caseInsensitive)
      {
	return new GEnum(super.keys());
      }
    else
      {
	return super.keys();
      }
  }

  public synchronized boolean containsKey(Object key)
  {
    if (key instanceof Byte[])
      {
	key = new IPwrap((Byte[]) key);
      }

    if (caseInsensitive)
      {
	return super.containsKey(new GKey(key));
      }
    else
      {
	return super.containsKey(key);
      }
  }

  public synchronized Object get(Object key)
  {
    if (key instanceof Byte[])
      {
	key = new IPwrap((Byte[]) key);
      }

    if (caseInsensitive)
      {
	return super.get(new GKey(key));
      }
    else
      {
	return super.get(key);
      }
  }

  public synchronized Object put(Object key, Object value)
  {
    Object result;

    /* -- */

    if (key instanceof Byte[])
      {
	key = new IPwrap((Byte[]) key);
      }

    if (caseInsensitive)
      {
	result = super.put(new GKey(key), value);
      }
    else
      {
	result = super.put(key, value);
      }

    return result;
  }

  public synchronized Object remove(Object key)
  {
    if (key instanceof Byte[])
      {
	key = new IPwrap((Byte[]) key);
      }

    if (caseInsensitive)
      {
	return super.remove(new GKey(key));
      }
    else
      {
	return super.remove(key);
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                            GKey

This class provides a mapping to allow keys of differing capitalization to be
treated as identical in a hashtable, while allowing the capitalization-preserved
key value to be retrieved on demand, in support of the Hashtable.keys() method.

------------------------------------------------------------------------------*/

class GKey {

  Object
    key, 
    orig;

  /* -- */

  GKey(Object key)
  {
    if (key == null)
      {
	throw new NullPointerException("Null key value");
      }

    if (key instanceof String)
      {
	orig = key;
	this.key = ((String) key).toLowerCase();
      }
    else
      {
	this.key = orig = key;
      }
  }

  public int hashCode()
  {
    return key.hashCode();
  }

  public boolean equals(Object obj)
  {
    if (obj instanceof GKey)
      {
	return key.equals(((GKey) obj).key);
      }
    else
      {
	return key.equals(obj);
      }
  }

  public Object origValue()
  {
    return orig;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                           GEnum

This class is in support of the Hashtable keys() method, to provide an 
enumeration which will 'unwrap' GKey objects to provide access to the original
key submitted to the GHashtable, with capitalization preserved.

------------------------------------------------------------------------------*/

class GEnum implements Enumeration {

  Enumeration source;
  Object t;

  /* -- */

  GEnum(Enumeration enum)
  {
    source = enum;
  }

  public boolean hasMoreElements()
  {
    return source.hasMoreElements();
  }

  public Object nextElement()
  {
    t = source.nextElement();
    
    if (t instanceof GKey)
      {
	return ((GKey) t).origValue();
      }
    else
      {
	return t;
      }
  }
}
