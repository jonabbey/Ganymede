/*

   VectorUtils.java

   Convenience methods for working with Vectors.. provides efficient Union,
   Intersection, and Difference methods.
   
   Created: 21 July 1998
   Version: $Revision: 1.2 $ %D%
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

}
