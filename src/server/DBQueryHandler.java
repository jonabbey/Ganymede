/*

   DBQueryHandler.java

   This is the query processing engine for the Ganymede database.
   
   Created: 10 July 1997
   Version: $Revision: 1.4 $ %D%
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

  public static final boolean matches(Query q, DBObject obj)
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

  public static final boolean nodeMatch(QueryNode qN, DBObject obj)
  {
    Object value;
    
    /* -- */

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
	DBField field = null;
	QueryDataNode n;

	/* - */

	n = (QueryDataNode) qN;

	if (n.fieldId == -2)
	  {
	    // compare the Invid

	    if (n.comparator == n.EQUALS)
	      {
		return obj.getInvid().equals(n.value);
	      }
	    else
	      {
		return false;
	      }
	  }

	if (n.fieldname != null)
	  {
	    field = (DBField) obj.getField(n.fieldname);

	    if ((field != null) && (field.defined))
	      {
		value = field.getValue();
	      }
	    else
	      {
		return false;
	      }
	  }
	else if (n.fieldId != -1)
	  {
	    field = (DBField) obj.getField(n.fieldId);

	    if ((field != null) && (field.defined))
	      {
		value = field.getValue();
	      }
	    else
	      {
		return false;
	      }
	  }
	else
	  {
	    value = obj.getLabel();
	  }

	if (n.comparator == n.UNDEFINED)
	  {
	    return ((field == null) || (!field.defined));
	  }

	// okay.  Now we check each field type

	if (value instanceof String)
	  {
	    if (n.comparator == n.EQUALS)
	      {
		return (value.equals(n.value));
	      }
	    else if (n.comparator == n.NOCASEEQ)
	      {
		try
		  {
		    return (((String) value).equalsIgnoreCase((String) n.value));
		  }
		catch (ClassCastException ex)
		  {
		    return false;
		  }
	      }
	    else if (n.comparator == n.STARTSWITH)
	      {
		try
		  {
		    return (((String) value).startsWith((String) n.value));
		  }
		catch (ClassCastException ex)
		  {
		    return false;
		  }
	      }
	    else if (n.comparator == n.ENDSWITH)
	      {
		try
		  {
		    return (((String) value).endsWith((String) n.value));
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

	if (value instanceof Boolean)
	  {
	    if (n.comparator == n.EQUALS)
	      {
		return ((Boolean) value).equals(n.value);
	      }
	    else
	      {
		return false;	// invalid comparator
	      }
	  }

	//

	if (value instanceof Date)
	  {
	    long time1, time2;

	    /* -- */

	    try
	      {
		time1 = ((Date) value).getTime();
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

	if (value instanceof Integer)
	  {
	    int val1, val2;

	    /* -- */

	    try
	      {
		val1 = ((Integer) value).intValue();
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

	if (value instanceof Invid)
	  {
	    Invid i1, i2;

	    /* -- */

	    try
	      {
		i1 = (Invid) value;
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
