/*

   VectorUtils.java

   Convenience methods for working with Vectors.. provides efficient Union,
   Intersection, and Difference methods.
   
   Created: 21 July 1998
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.Util;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     VectorUtils

------------------------------------------------------------------------------*/

/**
 *
 * Convenience methods for working with Vectors.. provides efficient Union,
 * Intersection, and Difference methods.
 *
 */

public class VectorUtils {

  /**
   *
   * This method returns a Vector containing the union of the objects
   * contained in vectA and vectB.  The resulting Vector will not
   * contain any duplicates, even if vectA or vectB themselves contain
   * repeated items.<br><br>
   *
   * This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.
   *  
   */

  public static Vector union(Vector vectA, Vector vectB)
  {
    Hashtable workSet = new Hashtable();
    Vector result = new Vector();
    Enumeration enum;
    Object item;

    /* -- */

    if (vectA != null)
      {
	enum = vectA.elements();

	while (enum.hasMoreElements())
	  {
	    item = enum.nextElement();
	    workSet.put(item, item);
	  }
      }
    
    if (vectB != null)
      {
	enum = vectB.elements();

	while (enum.hasMoreElements())
	  {
	    item = enum.nextElement();
	    workSet.put(item, item);
	  }
      }

    enum = workSet.elements();

    while (enum.hasMoreElements())
      {
	result.addElement(enum.nextElement());
      }

    return result;
  }

  /**
   *
   * This method returns a Vector containing the intersection of the
   * objects contained in vectA and vectB.<br><br>
   *
   * This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.
   *  
   */

  public static Vector intersection(Vector vectA, Vector vectB)
  {
    Hashtable 
      workSetA = new Hashtable(),
      workSetB = new Hashtable(),
      resultSet = new Hashtable();

    Vector result = new Vector();
    Enumeration enum;
    Object item;

    /* -- */

    if (vectA != null)
      {
	enum = vectA.elements();

	while (enum.hasMoreElements())
	  {
	    item = enum.nextElement();
	    workSetA.put(item, item);
	  }
      }
    
    if (vectB != null)
      {
	enum = vectB.elements();

	while (enum.hasMoreElements())
	  {
	    item = enum.nextElement();
	    workSetB.put(item, item);
	  }
      }

    enum = workSetA.elements();

    while (enum.hasMoreElements())
      {
	item = enum.nextElement();

	if (workSetB.containsKey(item))
	  {
	    resultSet.put(item, item);
	  }
      }

    enum = workSetB.elements();

    while (enum.hasMoreElements())
      {
	item = enum.nextElement();

	if (workSetA.containsKey(item))
	  {
	    resultSet.put(item, item);
	  }
      }

    enum = resultSet.elements();

    while (enum.hasMoreElements())
      {
	result.addElement(enum.nextElement());
      }

    return result;
  }

  /**
   *
   * This method returns a Vector containing the set of objects
   * contained in vectA that are not contained in vectB.<br><br>
   *
   * This method will always return a new, non-null Vector, even if
   * vectA and/or vectB are null.
   *  
   */

  public static Vector difference(Vector vectA, Vector vectB)
  {
    Hashtable 
      workSetB = new Hashtable();

    Vector result = new Vector();
    Enumeration enum;
    Object item;

    /* -- */

    if (vectA == null)
      {
	return result;
      }

    if (vectB != null)
      {
	enum = vectB.elements();

	while (enum.hasMoreElements())
	  {
	    item = enum.nextElement();
	    workSetB.put(item, item);
	  }
      }

    enum = vectA.elements();
	
    while (enum.hasMoreElements())
      {
	item = enum.nextElement();
	
	if (!workSetB.containsKey(item))
	  {
	    result.addElement(item);
	  }
      }

    return result;
  }

  /**
   *
   * This method returns a string containing all the elements in vec
   * concatenated together, comma separated.
   *
   */

  public static String vectorString(Vector vec)
  {
    StringBuffer temp = new StringBuffer();

    for (int i = 0; i < vec.size(); i++)
      {
	if (i > 0)
	  {
	    temp.append(",");
	  }

	temp.append(vec.elementAt(i));
      }

    return temp.toString();
  }

  /**
   *
   * This method takes a sepChars-separated string and converts it to
   * a vector of fields.  i.e., "gomod,jonabbey" -> a vector whose
   * elements are "gomod" and "jonabbey".<br><br>
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
   *
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

	    results.addElement(temp);
	  }
	else
	  {
	    temp = input.substring(oldindex, index);

	    // System.err.println("* " + temp + " *");

	    results.addElement(temp);

	    oldindex = index + 1;
	  }
      }
    
    return results;
  }

  /**
   *
   * findNextSep() takes a string, a starting position, and a string of
   * characters to be considered field separators, and returns the
   * first index after startDex whose char is in sepChars.
   *
   * If there are no chars in sepChars past startdex in input, findNextSep()
   * returns -1.
   *
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

  // debug rig.

  public static void main(String[] args)
  {
    // String testString = "jon, beth   ross,,,darren,anna";

    String testString = "jon, beth   ross,,,darren,anna,,,,,";
    
    Vector results = stringVector(testString, ", ");

    for (int i = 0; i < results.size(); i++)
      {
	System.out.println(i + ": " + results.elementAt(i));
      }
  }
}
