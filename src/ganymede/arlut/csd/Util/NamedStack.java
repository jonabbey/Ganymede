/*

   NamedStack.java

   A Stack object in which items on the stack are given names, to
   allow items on the stack at or after the occurrence of a given name
   to be popped in one operation.

   Created: 2 October 2000

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

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

import java.util.Stack;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      NamedStack

------------------------------------------------------------------------------*/

/**
 * <p>NamedStack is a data structure that allows named items to be placed on a
 * stack.  When it comes time to pop data off the stack, the pop operation takes
 * the name of the item to be popped, and all items pushed on the stack after
 * the named item will also be popped.</p>
 *
 * <p>That is, NamedStack is basically a Stack that supports rollback to a named
 * state.  It is used in the Ganymede server to support checkpoint operations.</p>
 *
 * <p>NamedStack does not require that names of pushed items be unique.  A pop
 * operation will simply pop up to the last item matching the given name off
 * the stack.</p>
 */

final public class NamedStack<E> {

  private Stack<NamedStackHandle<E>> stack;

  /* -- */

  public NamedStack()
  {
    stack = new Stack<NamedStackHandle<E>>();
  }

  /**
   * <p>push pushes a named item onto the stack.</p>
   */

  public synchronized E push(String name, E item)
  {
    stack.push(new NamedStackHandle<E>(name, item));

    return item;
  }

  /**
   * <p>pop rolls the state of the stack back to the time just before
   * the top-most named object matching name was pushed.</p>
   *
   * @return The top-most named object matching name.
   */

  public synchronized E pop(String name)
  {
    // okay, i'm a bad person.  i micro-optimized this to avoid having
    // to do a few name equality tests twice.  i figure everything's
    // nice, private, and synchronized, so this is a safe enough thing
    // to do.  but you can spank me if you want to.

    int i = findName(name);

    if (i == -1)
      {
        return null;
      }

    for (int j = stack.size()-1; j > i; j--)
      {
        stack.pop();
      }

    return stack.pop().getData();
  }

  /**
   * <p>This is just plain old pop, it will return the top element
   * without regard to its name.</p>
   */

  public synchronized E pop()
  {
    if (stack.size() == 0)
      {
        return null;
      }

    return stack.pop().getData();
  }

  public E elementAt(int index)
  {
    NamedStackHandle<E> handle = stack.elementAt(index);

    if (handle == null)
      {
        return null;
      }

    return handle.getData();
  }

  public String nameAt(int index)
  {
    NamedStackHandle handle = stack.elementAt(index);

    if (handle == null)
      {
        return null;
      }

    return handle.getName();
  }

  public int size()
  {
    return stack.size();
  }

  public void clear()
  {
    this.removeAllElements();
  }

  public synchronized void removeAllElements()
  {
    stack.removeAllElements();
  }

  /**
   * <p>This method returns the name associated with the last item pushed
   * onto this name stack, without altering the stack.</p>
   */

  public synchronized String getTopName()
  {
    if (stack.size() == 0)
      {
        return null;
      }

    return stack.elementAt(stack.size()-1).getName();
  }

  /**
   * <p>This method returns the object associated with the last item pushed
   * onto this name stack, without altering the stack.</p>
   */

  public synchronized E getTopObject()
  {
    if (stack.size() == 0)
      {
        return null;
      }

    return stack.elementAt(stack.size()-1).getData();
  }

  /**
   * <p>findName() returns the top-most index at which the name is
   * present in the named object stack, or -1 if the name was not found.</p>
   */

  public synchronized int findName(String name)
  {
    for (int i = stack.size()-1; i >= 0; i--)
      {
        if (stack.elementAt(i).getName().equals(name))
          {
            return i;
          }
      }

    return -1;
  }

  public boolean empty()
  {
    return stack.empty();
  }

  public synchronized String toString()
  {
    StringBuilder result = new StringBuilder();

    /* -- */

    for (int i = stack.size()-1; i >= 0; i--)
      {
        NamedStackHandle handle = stack.elementAt(i);

        result.append(i);
        result.append(" : ");
        result.append(handle.getName());
        result.append(" -- ");
        result.append(handle.getData().toString());
        result.append("\n");
      }

    return result.toString();
  }
}

/**
 * <p>This class is used to associate a name String with an object for use with the
 * {@link arlut.csd.Util.NamedStack NamedStack} data structure.</p>
 */

final class NamedStackHandle<E> {

  private String name;
  private E data;

  /* -- */

  public NamedStackHandle(String name, E data)
  {
    if (name == null)
      {
        throw new IllegalArgumentException("NamedStackHandle: null name");
      }

    if (data == null)
      {
        throw new IllegalArgumentException("NamedStackHandle: null data");
      }

    this.name = name;
    this.data = data;
  }

  public String getName()
  {
    return name;
  }

  public E getData()
  {
    return data;
  }
}
