/*

   VectorUtils.java

   Convenience methods for working with Lists.. provides efficient
   Union, Intersection, and Difference methods.

   Created: 21 July 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Directory Directory Management System

   Copyright (C) 1996-2012
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     VectorUtils

------------------------------------------------------------------------------*/

/**
 * Convenience methods for working with Vectors.. provides efficient Union,
 * Intersection, and Difference methods.
 */

public class VectorUtils {

  /**
   * <p>This method returns a Vector containing the union of the objects
   * contained in vectA and vectB.  The resulting Vector will not
   * contain any duplicates, even if vectA or vectB themselves contain
   * repeated items.</p>
   *
   * <p>This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.</p>
   */

  public static <E> Vector<E> union(List<E> vectA, List<E> vectB)
  {
    int threshold = vectSize(vectA) + vectSize(vectB);

    if (threshold < 10)		// I pulled 10 out of my ass
      {
	Vector result = new Vector(threshold);

	if (vectA != null)
	  {
	    for (E obj: vectA)
	      {
		result.add(obj);
	      }
	  }

	if (vectB != null)
	  {
	    for (E obj: vectB)
	      {
		if (!result.contains(obj))
		  {
		    result.add(obj);
		  }
	      }
	  }

	result.trimToSize();

	return result;
      }
    else
      {
	// If we have a big enough set of elements to union, use a
	// temporary hashtable so that we have better scalability for
	// item lookup.

	Set<E> workSet = new HashSet<E>(vectSize(vectA) + vectSize(vectB));

	/* -- */

	if (vectA != null)
	  {
	    workSet.addAll(vectA);
	  }

	if (vectB != null)
	  {
	    workSet.addAll(vectB);
	  }

	return new Vector(workSet);
      }
  }

  /**
   * <p>This method adds obj to vect if and only if vect does not
   * already contain obj.</p>
   */

  public static <E> void unionAdd(List<E> vect, E obj)
  {
    if (obj == null)
      {
	return;
      }

    if (vect.contains(obj))
      {
	return;
      }

    vect.add(obj);
  }

  /**
   * <p>Returns true if vectA and vectB have any elements in
   * common.</p>
   */

  public static <E> boolean overlaps(List<E> vectA, List<E> vectB)
  {
    if (vectA == null || vectB == null || vectA.size() == 0 || vectB.size() == 0)
      {
	return false;
      }

    if ((vectA.size() + vectB.size()) > 20)		// ass, again
      {
	Set<E> workSet = new HashSet<E>(vectA.size());

	workSet.addAll(vectA);

	for (int i = 0; i < vectB.size(); i++)
	  {
	    if (workSet.contains(vectB.get(i)))
	      {
		return true;
	      }
	  }
      }
    else
      {
	if (vectA.size() > vectB.size())
	  {
	    for (int i = 0; i < vectA.size(); i++)
	      {
		if (vectB.contains(vectA.get(i)))
		  {
		    return true;
		  }
	      }
	  }
	else
	  {
	    for (int i = 0; i < vectB.size(); i++)
	      {
		if (vectA.contains(vectB.get(i)))
		  {
		    return true;
		  }
	      }
	  }
      }

    return false;
  }

  /**
   * <p>This method returns a Vector containing the intersection of the
   * objects contained in vectA and vectB.</p>
   *
   * <p>This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.</p>
   */

  public static <E> Vector<E> intersection(List<E> vectA, List<E> vectB)
  {
    Set<E>
      workSetA = new HashSet<E>(),
      workSetB = new HashSet<E>(),
      resultSet = new HashSet<E>();

    /* -- */

    if (vectA != null)
      {
	workSetA.addAll(vectA);
      }

    if (vectB != null)
      {
	workSetB.addAll(vectB);
      }

    for (E item: workSetA)
      {
	if (workSetB.contains(item))
	  {
	    resultSet.add(item);
	  }
      }

    for (E item: workSetB)
      {
	if (workSetA.contains(item))
	  {
	    resultSet.add(item);
	  }
      }

    return new Vector(resultSet);
  }

  /**
   * <p>This method returns a Vector containing the set of objects
   * contained in vectA that are not contained in vectB.</p>
   *
   * <p>This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.</p>
   */

  public static <E> Vector<E> difference(List<E> vectA, List<E> vectB)
  {
    Vector<E> result = new Vector<E>();

    /* -- */

    if (vectA == null)
      {
	return result;
      }

    if (vectB == null)
      {
	return new Vector<E>(vectA);
      }

    if (vectA.size() + vectB.size() < 10) // ass
      {
	for (E item: vectA)
	  {
	    if (!vectB.contains(item))
	      {
		result.add(item);
	      }
	  }
      }
    else
      {
	Set<E> workSet = new HashSet<E>(vectB);

	for (E item: vectA)
	  {
	    if (!workSet.contains(item))
	      {
		result.add(item);
	      }
	  }
      }

    return result;
  }

  /**
   * <p>This method returns true if vectA and vectB contain the same
   * elements, in whatever order.</p>
   */

  public static <E> boolean equalMembers(List<E> vectA, List<E> vectB)
  {
    if (vectA.size() != vectB.size())
      {
	return false;
      }

    return intersection(vectA, vectB).size() == vectA.size();
  }

  /**
   * <p>This method returns a Vector of items that appeared in the
   * vector parameter more than once.</p>
   *
   * <p>If no duplicates are found or if vector is null, this method
   * returns null.</p>
   */

  public static <E> Vector<E> duplicates(List<E> vector)
  {
    if (vector == null)
      {
	return null;
      }

    Vector result = null;
    Set found = new HashSet();

    for (Object item: vector)
      {
	if (found.contains(item))
	  {
	    if (result == null)
	      {
		result = new Vector();
	      }

	    unionAdd(result, item);
	  }

	found.add(item);
      }

    return result;
  }

  /**
   * <p>This method returns a Vector containing the elements of vectA minus
   * the elements of vectB.  If vectA has an element in the Vector 5 times
   * and vectB has it 3 times, the result will have it two times.</p>
   *
   * <p>This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.</p>
   */

  public static <E> Vector<E> minus(List<E> vectA, List<E> vectB)
  {
    if (vectA == null)
      {
	return new Vector<E>();	// empty
      }

    Vector<E> result = new Vector<E>(vectA);

    if (vectB != null)
      {
	for (E item: vectB)
	  {
	    result.remove(item);
	  }
      }

    return result;
  }

  /**
   * <p>This method returns a string containing all the elements in vec
   * concatenated together, comma separated.</p>
   */

  public static String vectorString(Collection vec)
  {
    return VectorUtils.vectorString(vec, ",");
  }

  /**
   * <p>This method returns a string containing all the elements in vec
   * concatenated together, comma separated.</p>
   */

  public static String vectorString(Collection vec, String separator)
  {
    if (vec == null)
      {
	return "";
      }

    StringBuilder temp = new StringBuilder();

    boolean first = true;

    for (Object elem: vec)
      {
	if (!first)
	  {
	    temp.append(separator);
	  }

	temp.append(elem);

	first = false;
      }

    return temp.toString();
  }

  /**
   * <p>This method takes a sepChars-separated string and converts it to
   * a vector of fields.  i.e., "gomod,jonabbey" -&gt; a vector whose
   * elements are "gomod" and "jonabbey".</p>
   *
   * <p>NOTE: this method will omit 'degenerate' fields from the output
   * vector.  That is, if input is "gomod,,,  jonabbey" and sepChars
   * is ", ", then the result vector will still only have "gomod"
   * and "jonabbey" as elements, even though one might wish to
   * explicitly know about the blanks between commas.  This method
   * is intended mostly for creating email list vectors, rather than
   * general file-parsing vectors.</p>
   *
   * @param input the sepChars-separated string to test.
   *
   * @param sepChars a string containing a list of characters which
   * may occur as field separators.  Any two fields in the input may
   * be separated by one or many of the characters present in sepChars.
   */

  public static Vector stringVector(String input, String sepChars)
  {
    Vector results = new Vector();
    int index = 0;
    int oldindex = 0;
    String temp;
    char inputAry[] = input.toCharArray();

    /* -- */

    while (index != -1)
      {
	// skip any leading field-separator chars

	for (; oldindex < input.length(); oldindex++)
	  {
	    if (sepChars.indexOf(inputAry[oldindex]) == -1)
	      {
		break;
	      }
	  }

	if (oldindex == input.length())
	  {
	    break;
	  }

	index = findNextSep(input, oldindex, sepChars);

	if (index == -1)
	  {
	    temp = input.substring(oldindex);

	    // System.err.println("+ " + temp + " +");

	    results.add(temp);
	  }
	else
	  {
	    temp = input.substring(oldindex, index);

	    // System.err.println("* " + temp + " *");

	    results.add(temp);

	    oldindex = index + 1;
	  }
      }

    return results;
  }

  /**
   * <p>findNextSep() takes a string, a starting position, and a string of
   * characters to be considered field separators, and returns the
   * first index after startDex whose char is in sepChars.</p>
   *
   * <p>If there are no chars in sepChars past startdex in input, findNextSep()
   * returns -1.</p>
   */

  private static int findNextSep(String input, int startDex, String sepChars)
  {
    int currentIndex = input.length();
    char sepAry[] = sepChars.toCharArray();
    boolean foundSep = false;

    /* -- */

    // find the next separator

    for (int i = 0; i < sepAry.length; i++)
      {
	int tempdex = input.indexOf(sepAry[i], startDex);

	if (tempdex > -1 && tempdex <= currentIndex)
	  {
	    currentIndex = tempdex;
	    foundSep = true;
	  }
      }

    if (foundSep)
      {
	return currentIndex;
      }
    else
      {
	return -1;
      }
  }

  private static int vectSize(List x)
  {
    if (x == null)
      {
	return 0;
      }
    else
      {
	return x.size();
      }
  }

  // debug rig.

  public static void main(String[] args)
  {
    // String testString = "jon, beth   ross,,,darren,anna";

    String testString = "jon, beth   ross,,,darren,anna,,,,,";

    Vector results = stringVector(testString, ", ");

    for (int i = 0; i < results.size(); i++)
      {
	System.out.println(i + ": " + results.get(i));
      }
  }
}
