/*

   VectorUtils.java

   Convenience methods for working with Lists.. provides efficient
   Union, Intersection, and Difference methods.
   
   Created: 21 July 1998


   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

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
   * This method returns a Vector containing the union of the objects
   * contained in vectA and vectB.  The resulting Vector will not
   * contain any duplicates, even if vectA or vectB themselves contain
   * repeated items.
   *
   * This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.
   */

  public static Vector union(List vectA, List vectB)
  {
    int threshold = vectSize(vectA) + vectSize(vectB);

    if (threshold < 10)		// I pulled 10 out of my ass
      {
	Vector result = new Vector(threshold);

	if (vectA != null)
	  {
	    for (Object obj: vectA)
	      {
		result.add(obj);
	      }
	  }

	if (vectB != null)
	  {
	    for (Object obj: vectB)
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

	Set workSet = new HashSet(vectSize(vectA) + vectSize(vectB));

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
   * This method adds obj to vect if and only if vect does not
   * already contain obj.
   */

  public static void unionAdd(List vect, Object obj)
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
   * Returns true if vectA and vectB have any elements in
   * common. 
   */

  public static boolean overlaps(List vectA, List vectB)
  {
    if (vectA == null || vectB == null || vectA.size() == 0 || vectB.size() == 0)
      {
	return false;
      }

    if ((vectA.size() + vectB.size()) > 20)		// ass, again
      {
	Set workSet = new HashSet(vectA.size());

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
   * This method returns a Vector containing the intersection of the
   * objects contained in vectA and vectB.
   *
   * This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.
   */

  public static Vector intersection(List vectA, List vectB)
  {
    Set
      workSetA = new HashSet(),
      workSetB = new HashSet(),
      resultSet = new HashSet();

    /* -- */

    if (vectA != null)
      {
	workSetA.addAll(vectA);
      }
    
    if (vectB != null)
      {
	workSetB.addAll(vectB);
      }

    for (Object item: workSetA)
      {
	if (workSetB.contains(item))
	  {
	    resultSet.add(item);
	  }
      }

    for (Object item: workSetB)
      {
	if (workSetA.contains(item))
	  {
	    resultSet.add(item);
	  }
      }

    return new Vector(resultSet);
  }

  /**
   * This method returns a Vector containing the set of objects
   * contained in vectA that are not contained in vectB.
   *
   * This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.
   */

  public static Vector difference(List vectA, List vectB)
  {
    Vector result = new Vector();

    /* -- */

    if (vectA == null)
      {
	return result;
      }

    if (vectB == null)
      {
	return new Vector(vectA);
      }

    if (vectA.size() + vectB.size() < 10) // ass
      {
	for (Object item: vectA)
	  {
	    if (!vectB.contains(item))
	      {
		result.add(item);
	      }
	  }
      }
    else
      {
	Set workSet = new HashSet(vectB);

	for (Object item: vectA)
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
   * This method returns a Vector of items that appeared in the 
   * vector parameter more than once.
   *
   * If no duplicates are found or if vector is null, this method
   * returns null.
   */

  public static Vector duplicates(List vector)
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
   * This method returns a Vector containing the elements of vectA minus
   * the elements of vectB.  If vectA has an element in the Vector 5 times
   * and vectB has it 3 times, the result will have it two times.
   *
   * This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.
   */

  public static Vector minus(List vectA, List vectB)
  {
    if (vectA == null)
      {
	return new Vector();	// empty
      }

    Vector result = new Vector(vectA);

    if (vectB != null)
      {
	for (Object item: vectB)
	  {
	    result.remove(item);
	  }
      }

    return result;
  }

  /**
   * This method returns a string containing all the elements in vec
   * concatenated together, comma separated.
   */

  public static String vectorString(Collection vec)
  {
    return VectorUtils.vectorString(vec, ",");
  }

  /**
   * This method returns a string containing all the elements in vec
   * concatenated together, comma separated.
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
   * This method takes a sepChars-separated string and converts it to
   * a vector of fields.  i.e., "gomod,jonabbey" -> a vector whose
   * elements are "gomod" and "jonabbey".
   *
   * NOTE: this method will omit 'degenerate' fields from the output
   * vector.  That is, if input is "gomod,,,  jonabbey" and sepChars
   * is ", ", then the result vector will still only have "gomod"
   * and "jonabbey" as elements, even though one might wish to
   * explicitly know about the blanks between commas.  This method
   * is intended mostly for creating email list vectors, rather than
   * general file-parsing vectors.
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
   * findNextSep() takes a string, a starting position, and a string of
   * characters to be considered field separators, and returns the
   * first index after startDex whose char is in sepChars.
   *
   * If there are no chars in sepChars past startdex in input, findNextSep()
   * returns -1.
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
