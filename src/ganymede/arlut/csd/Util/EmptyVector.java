/*

   EmptyVector.java

   An immutable Vector subclass with no contents.

   Created: 22 April 2013

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Directory Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     EmptyVector

------------------------------------------------------------------------------*/

/**
 * <p>An immutable, empty subclass of java.util.Vector.</p>
 */

final public class EmptyVector<E> extends Vector<E>
{
  public EmptyVector()
  {
  }

  /**
   * If code clones this EmptyVector, go ahead and give them back a
   * mutable Vector they can play with.
   */

  @Override public Object clone()
  {
    return new Vector();
  }

  @Override public synchronized void trimToSize()
  {
    return;
  }

  @Override public synchronized void ensureCapacity(int minCapacity)
  {
    return;
  }

  @Override public synchronized void setSize(int newSize)
  {
    return;
  }

  @Override public synchronized void setElementAt(E obj, int index)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized void removeElementAt(int index)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized void insertElementAt(E obj, int index)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized void addElement(E obj)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized boolean removeElement(Object obj)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized void removeAllElements()
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized E set(int index, E element)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized boolean add(E obj)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized boolean remove(Object obj)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized void add(int index, E obj)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized E remove(int index)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized boolean addAll(Collection<? extends E> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized boolean removeAll(Collection<?> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized boolean retainAll(Collection<?> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized boolean addAll(int index, Collection<? extends E> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override public synchronized List<E> subList(int fromIndex, int toIndex)
  {
    return Collections.EMPTY_LIST.subList(fromIndex, toIndex);
  }

  @Override public synchronized ListIterator<E> listIterator(int index)
  {
    return Collections.EMPTY_LIST.listIterator(index);
  }

  @Override public synchronized ListIterator<E> listIterator()
  {
    return Collections.EMPTY_LIST.listIterator();
  }

  @Override public synchronized Iterator<E> iterator()
  {
    return Collections.EMPTY_LIST.iterator();
  }
}
