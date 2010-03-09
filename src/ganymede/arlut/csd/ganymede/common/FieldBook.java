/*

   FieldBook.java

   A data structure which identifies a collection of invids and field
   identifiers which are to be used for some kind of external
   processing.

   Created: 24 February 2010

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*------------------------------------------------------------------------------
									   class
								       FieldBook

------------------------------------------------------------------------------*/

/**
 * A data structure which identifies a collection of invids and field
 * identifiers which are to be used for some kind of external
 * processing.
 */

public class FieldBook {

  /**
   * A Map structure holding the invids and fields that comprise our
   * book.
   *
   * If an Invid maps to null, this means that all fields in the
   * DBObject corresponding to the Invid are selected.  Otherwise, the
   * Invid will map to a Set which enumerates the fields to be
   * included in the book.
   */
  
  private Map<Invid, Set<Short>> map;

  /* -- */

  public FieldBook()
  {
    map = new HashMap<Invid, Set<Short>>();
  }

  public void add(Invid invid)
  {
    map.put(invid, null);	// full object
  }

  public void add(Invid invid, short fieldId)
  {
    Set<Short> set;

    if (map.containsKey(invid))
      {
	set = map.get(invid);

	if (set == null)
	  {
	    return;		// we've already got the whole object
	  }

	set.add(fieldId);
      }
    else
      {
	set = new HashSet<Short>();

	set.add(fieldId);

	map.put(invid, set);
      }
  }

  public void add(Invid invid, Collection<Short> fieldIds)
  {
    Set<Short> set;

    if (map.containsKey(invid))
      {
	set = map.get(invid);

	if (set == null)
	  {
	    return;		// we've already got the whole object
	  }

	for (Short fieldId: fieldIds)
	  {
	    set.add(fieldId);
	  }
      }
    else
      {
	set = new HashSet<Short>(fieldIds);

	map.put(invid, set);
      }
  }
  
  /**
   * Adds all DBObject and field identifiers from parameter book to this FieldBook.
   */

  public void merge(FieldBook book)
  {
    for (Invid invid: book.map.keySet())
      {
	Set<Short> set = book.map.get(invid);

	if (set == null)
	  {
	    map.put(invid, null);
	  }
	else
	  {
	    add(invid, set);
	  }
      }
  }

  /**
   * Returns a set of Invids for objects that this FieldBook contains.
   */

  public Set<Invid> objects()
  {
    return Collections.unmodifiableSet(map.keySet());
  }

  /**
   * Returns true if this FieldBook has any records for Invid invid.
   */

  public boolean has(Invid invid)
  {
    return map.containsKey(invid);
  }

  /**
   * Returns true if this FieldBook includes field fieldId in the DBObject
   * corresponding to invid.
   */

  public boolean has(Invid invid, short fieldId)
  {
    if (!map.containsKey(invid))
      {
	return false;
      }

    Set<Short> set = map.get(invid);

    if (set == null)
      {
	return true;
      }

    return set.contains(fieldId);
  }

  /**
   * Returns a set of field ids for fields for Invid invid that are
   * part of this FieldBook.
   */

  public Set<Short> fields(Invid invid)
  {
    if (!map.containsKey(invid))
      {
	throw new IllegalArgumentException("No fields for invid " + invid);
      }

    Set<Short> set = map.get(invid);

    if (set == null)
      {
	return null;
      }

    return Collections.unmodifiableSet(set);
  }
}
