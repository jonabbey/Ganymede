/*

   DBQueryHandler.java

   This is the query processing engine for the Ganymede database.
   
   Created: 10 July 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  DBQueryHandler

------------------------------------------------------------------------------*/

public class DBQueryHandler {

  public static boolean matches(Query q, DBObject obj)
  {
    if ((q == null) || (obj == null))
      {
	throw new NullPointerException("null param to DBQueryHandler");
      }

    if (q.root == null)
      {
	return true;		// need to add check for editability here
      }
    else
      {
	return nodeMatch(q.root, obj);
      }
  }

  public static boolean nodeMatch(QueryNode qN, DBObject obj)
  {
    if (qN == null)
      {
	return false;
      }

    if (qN instanceof QueryNotNode)
      {
	return (!nodeMatch(((QueryNotNode)qN).child, obj));
      }

    if (qN instanceof QueryAndNode)
      {
	return (nodeMatch(((QueryAndNode)qN).child1, obj) && 
		nodeMatch(((QueryAndNode)qN).child2, obj));
      }

    if (qN instanceof QueryOrNode)
      {
	return (nodeMatch(((QueryOrNode)qN).child1, obj) ||
		nodeMatch(((QueryOrNode)qN).child2, obj));
      }

    if (qN instanceof QueryDataNode)
      {
	DBField field;
	QueryDataNode n;

	/* - */

	n = (QueryDataNode) qN;

	if (n.fieldname != null)
	  {
	    field = (DBField) obj.getField(n.fieldname);
	  }
	else if (n.fieldId != -1)
	  {
	    field = (DBField) obj.getField(n.fieldId);
	  }
	else
	  {
	    throw new RuntimeException("invalid field id in QueryNode");
	  }

	if (n.comparator == n.UNDEFINED)
	  {
	    return ((field == null) || (!field.defined));
	  }

	if ((field == null) || (!field.defined))
	  {
	    return false;
	  }

	// okay.  Now we check each field type

	if (field instanceof StringDBField)
	  {
	    StringDBField string = (StringDBField) field;

	    /* -- */

	    if (n.comparator == n.EQUALS)
	      {
		return (string.getValue().equals(n.value));
	      }
	    else if (n.comparator == n.NOCASEEQ)
	      {
		try
		  {
		    return (((String) string.getValue()).equalsIgnoreCase((String) n.value));
		  }
		catch (ClassCastException ex)
		  {
		    return false;
		  }
	      }
	    else
	      {
		return false;	// invalid comparator
	      }
	  }

	//

	if (field instanceof BooleanDBField)
	  {
	    BooleanDBField bool = (BooleanDBField) field;

	    /* -- */

	    if (n.comparator == n.EQUALS)
	      {
		return (bool.getValue().equals(n.value));
	      }
	    else
	      {
		return false;	// invalid comparator
	      }
	  }

	//

	if (field instanceof DateDBField)
	  {
	    DateDBField date = (DateDBField) field;
	    long time1, time2;

	    /* -- */

	    try
	      {
		time1 = ((Date) date.getValue()).getTime();
		time2 = ((Date) n.value).getTime();
	      }
	    catch (ClassCastException ex)
	      {
		return false;
	      }

	    if (n.comparator == n.EQUALS)
	      {
		return (time1 == time2);
	      }
	    else if (n.comparator == n.LESS)
	      {
		return (time1 < time2);
	      }
	    else if (n.comparator == n.LESSEQ)
	      {
		return (time1 <= time2);
	      }
	    else if (n.comparator == n.GREAT)
	      {
		return (time1 > time2);
	      }
	    else if (n.comparator == n.GREATEQ)
	      {
		return (time1 >= time2);
	      }
	    else
	      {
		return false;	// invalid comparator
	      }
	  }

	//

	if (field instanceof NumericDBField)
	  {
	    NumericDBField num = (NumericDBField) field;
	    int val1, val2;

	    /* -- */

	    try
	      {
		val1 = ((Integer) num.getValue()).intValue();
		val2 = ((Integer) n.value).intValue();
	      }
	    catch (ClassCastException ex)
	      {
		return false;
	      }

	    if (n.comparator == n.EQUALS)
	      {
		return (val1 == val2);
	      }
	    else if (n.comparator == n.LESS)
	      {
		return (val1 < val2);
	      }
	    else if (n.comparator == n.LESSEQ)
	      {
		return (val1 <= val2);
	      }
	    else if (n.comparator == n.GREAT)
	      {
		return (val1 > val2);
	      }
	    else if (n.comparator == n.GREATEQ)
	      {
		return (val1 >= val2);
	      }
	    else
	      {
		return false;	// invalid comparator
	      }
	  }

	//

	if (field instanceof InvidDBField)
	  {
	    InvidDBField invid = (InvidDBField) field;
	    Invid i1, i2;

	    /* -- */

	    try
	      {
		i1 = (Invid) invid.getValue();
		i2 = (Invid) n.value;
	      }
	    catch (ClassCastException ex)
	      {
		return false;
	      }

	    if (n.comparator == n.EQUALS)
	      {
		return i1.equals(i2);
	      }
	    else
	      {
		return false;	// invalid comparator
	      }
	  }
      }

    return false;		// wtf?
  }
}
