
/*

   InvidPool.java

   This class is intended to serve as an efficient, garbage-collecting
   object cache for Invids on the Ganymede server.

   Created: 7 January 2005

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2011
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

import java.lang.ref.SoftReference;
import java.lang.ref.ReferenceQueue;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       InvidPool

------------------------------------------------------------------------------*/

/**
 * <p>This InvidPool class is used by the Invid class to provide an
 * Invid when given a short type number and an int object number.  By
 * using an InvidPool, the server will be able to reuse previously
 * created Invids, much as the JVM can reuse interned strings.</p>
 *
 * <p>InvidPool uses SoftReferences to permit automatic clean-up of the
 * pool when pooled Invids fall out of usage in the rest of the
 * Ganymede heap.</p>
 */

public class InvidPool implements InvidAllocator {

  /**
   * The load factor used when none specified in constructor.
   */

  private static final float DEFAULT_LOAD_FACTOR = 0.75f;

  private InvidSlot[] table;

  private int floor;
  private int size;
  private int threshold;
  private final float loadFactor;

  /**
   * <p>InvidSlot items are added to this queue when the garbage
   * collector detects that the InvidSlot is no longer referenced by
   * hard references in the Ganymede heap.</p>
   */

  private final ReferenceQueue queue = new ReferenceQueue();

  /* -- */

  public InvidPool(int initialCapacity, float loadFactor)
  {
    if (initialCapacity < 0)
      {
        throw new IllegalArgumentException("Illegal Initial Capacity: " +
                                           initialCapacity);
      }

    if (loadFactor <= 0 || Float.isNaN(loadFactor))
      {
        throw new IllegalArgumentException("Illegal Load factor: " +
                                           loadFactor);
      }

    table = new InvidSlot[initialCapacity];

    this.loadFactor = loadFactor;
    this.floor = initialCapacity;
    threshold = (int) (initialCapacity * loadFactor);
  }

  public InvidPool(int initialCapacity)
  {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
  }

  public InvidPool()
  {
    this(25301, DEFAULT_LOAD_FACTOR); // 25301 is a nice, biggish prime
  }

  /**
   * <p>This method takes the identifying elements of an Invid to be
   * found, and searches to find a suitable Invid object to return.
   * If one cannot found, null should be returned, in which case
   * {@link arlut.csd.ganymede.common.Invid#createInvid(short,int)
   * createInvid} will synthesize and return a new one.</p>
   */

  public synchronized Invid findInvid(Invid newInvid)
  {
    expungeStaleEntries();
    InvidSlot s = table[(newInvid.hashCode() & 0x7FFFFFFF) % table.length];

    while (s != null)
      {
        Invid storedInvid = (Invid) s.get();

        if (newInvid.equals(storedInvid))
          {
            return storedInvid;
          }

        s = s.next;
      }

    return null;
  }

  /**
   * <p>This method takes the invid given and places in whatever storage
   * mechanism is appropriate, if any, for findInvid() to later draw
   * upon.</p>
   */

  public synchronized void storeInvid(Invid newInvid)
  {
    expungeStaleEntries();
    int i = (newInvid.hashCode() & 0x7FFFFFFF) % table.length;
    InvidSlot s = table[i];

    while (s != null)
      {
        Invid storedInvid = (Invid) s.get();

        if (newInvid.equals(storedInvid))
          {
            return;             // already stored
          }

        s = s.next;
      }

    table[i] = new InvidSlot(newInvid, queue, table[i]);

    if (++size >= threshold)
      {
        resize(table.length * 2 + 1);
      }
  }

  public synchronized int size()
  {
    if (size == 0)
      {
        return 0;
      }

    expungeStaleEntries();
    return size;
  }

  /**
   * <p>This method iterates polls through the ReferenceQueue,
   * identifying InvidSlots that reference Invids which are no longer
   * hard-referenced in the Ganymede heap and removing them from the
   * hash table, thus freeing space in our pool.</p>
   */

  private void expungeStaleEntries()
  {
    InvidSlot s;

    /* -- */

    while ((s = (InvidSlot) queue.poll()) != null)
      {
        int i = (s.hashCode() & 0x7FFFFFFF) % table.length;

        if (table[i] == s)
          {
            table[i] = s.next;
          }
        else
          {
            InvidSlot prev = table[i];
            InvidSlot p = table[i].next;

            while (p != null)
              {
                if (p == s)
                  {
                    prev.next = p.next;
                    p.next = null;
                    size--;
                    break;
                  }

                prev = p;
                p = p.next;
              }
          }
      }

    // if we've drastically shrunk, we'll collapse our table down

    if (floor < size && size < (table.length / 4))
      {
        resize(size * 2 + 1);
      }
  }

  private void resize(int newCapacity)
  {
    threshold = (int)(newCapacity * loadFactor);
    InvidSlot newMap[] = new InvidSlot[newCapacity];

    for (int i = 0; i < table.length; i++)
      {
        InvidSlot s = table[i];

        while (s != null)
          {
            InvidSlot next = s.next;

            int index = (s.hashCode() & 0x7FFFFFFF) % newCapacity;
            s.next = newMap[index];
            newMap[index] = s;

            s = next;
          }
      }

    this.table = newMap;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       InvidSlot

------------------------------------------------------------------------------*/

/**
 * <p>This (non-public) class is used in the {@link
 * arlut.csd.ganymede.common.InvidPool} to softly reference Invids
 * that we are storing for purposes of interning.</p>
 */

class InvidSlot extends SoftReference {

  /**
   * For linking collision buckets.
   */

  InvidSlot next;

  /**
   * We have to remember the hash code for the referent Invid so that
   * we can properly find the hash slot this InvidSlot would have been
   * contained in before the reference was released.
   */

  int save_hash;

  /* -- */

  InvidSlot(Invid item, ReferenceQueue queue, InvidSlot next)
  {
    super(item, queue);
    this.save_hash = item.hashCode();
    this.next = next;
  }

  public boolean equals(Object o)
  {
    if (o == null)
      {
        return false;
      }

    return (o.equals(get()));
  }

  public int hashCode()
  {
    return save_hash;
  }
}
