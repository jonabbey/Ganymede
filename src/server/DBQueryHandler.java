/*

   DBQueryHandler.java

   This is the query processing engine for the Ganymede database.
   
   Created: 10 July 1997
   Version: $Revision: 1.6 $ %D%
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

  static final boolean debug = true;

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
    Object value = null;
    Vector values = null;
    int intval;
    
    /* -- */

    try				// wheee, sloppy
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
		    if (field.isVector())
		      {
			values = field.getValues();
		      }
		    else
		      {
			value = field.getValue();
		      }
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
		    if (field.isVector())
		      {
			values = field.getValues();
		      }
		    else
		      {
			value = field.getValue();
		      }
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

	    switch (n.arrayOp)
	      {
	      case n.NONE:
	      case n.CONTAINSANY:
	      case n.CONTAINSALL:
	      case n.CONTAINSNONE:
		break;

	      case n.LENGTHEQ:
		intval = ((Integer) value).intValue();
		return (intval == values.size());

	      case n.LENGTHGR:
		intval = ((Integer) value).intValue();
		return (intval > values.size());

	      case n.LENGTHLE:
		intval = ((Integer) value).intValue();
		return (intval < values.size());
	      }

	    // okay.  Now we check each field type

	    if (n.value instanceof String)
	      {
		if (n.comparator == n.EQUALS)
		  {
		    if (n.arrayOp == n.NONE)
		      {
			return (value.equals(n.value));
		      }
		    else
		      {
			switch (n.arrayOp)
			  {
			  case n.CONTAINSANY:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (n.value.equals(values.elementAt(i)))
				  {
				    return true;
				  }
			      }

			    return false;

			  case n.CONTAINSALL:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (!n.value.equals(values.elementAt(i)))
				  {
				    return false;
				  }
			      }

			    return true;

			  case n.CONTAINSNONE:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (n.value.equals(values.elementAt(i)))
				  {
				    return false;
				  }
			      }

			    return true;

			  default:

			    return false;
			  }
		      }
		  }
		else if (n.comparator == n.NOCASEEQ)
		  {
		    if (n.arrayOp == n.NONE)
		      {
			return (((String) value).equalsIgnoreCase((String) n.value));
		      }
		    else
		      {
			String tmpValString = (String) n.value;

			/* -- */

			switch (n.arrayOp)
			  {
			  case n.CONTAINSANY:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (tmpValString.equalsIgnoreCase((String) values.elementAt(i)))
				  {
				    return true;
				  }
			      }

			    return false;

			  case n.CONTAINSALL:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (!tmpValString.equalsIgnoreCase((String) values.elementAt(i)))
				  {
				    return false;
				  }
			      }

			    return true;

			  case n.CONTAINSNONE:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (tmpValString.equalsIgnoreCase((String) values.elementAt(i)))
				  {
				    return false;
				  }
			      }

			    return true;

			  default:

			    return false;
			  }
		      }
		  }
		else if (n.comparator == n.STARTSWITH)
		  {
		    if (n.arrayOp == n.NONE)
		      {
			return (((String) value).startsWith((String) n.value));
		      }
		    else
		      {
			String tmpValString = (String) n.value;

			/* -- */

			switch (n.arrayOp)
			  {
			  case n.CONTAINSANY:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (tmpValString.startsWith((String) values.elementAt(i)))
				  {
				    return true;
				  }
			      }

			    return false;

			  case n.CONTAINSALL:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (!tmpValString.startsWith((String) values.elementAt(i)))
				  {
				    return false;
				  }
			      }

			    return true;

			  case n.CONTAINSNONE:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (tmpValString.startsWith((String) values.elementAt(i)))
				  {
				    return false;
				  }
			      }

			    return true;

			  default:
			    
			    return false;
			  }
		      }
		  }
		else if (n.comparator == n.ENDSWITH)
		  {
		    if (n.arrayOp == n.NONE)
		      {
			return (((String) value).endsWith((String) n.value));
		      }
		    else
		      {
			String tmpValString = (String) n.value;

			/* -- */

			switch (n.arrayOp)
			  {
			  case n.CONTAINSANY:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (tmpValString.endsWith((String) values.elementAt(i)))
				  {
				    return true;
				  }
			      }

			    return false;

			  case n.CONTAINSALL:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (!tmpValString.endsWith((String) values.elementAt(i)))
				  {
				    return false;
				  }
			      }

			    return true;

			  case n.CONTAINSNONE:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (tmpValString.endsWith((String) values.elementAt(i)))
				  {
				    return false;
				  }
			      }

			    return true;

			  default:

			    return false;
			  }
		      }
		  }
		else
		  {
		    return false;	// invalid comparator
		  }
	      }

	    // invids can be arrays

	    if (n.value instanceof Invid)
	      {
		Invid 
		  i1 = null, 
		  i2 = null;

		/* -- */

		if (n.arrayOp == n.NONE)
		  {
		    i1 = (Invid) n.value;
		    i2 = (Invid) value;
		  }

		if (n.comparator == n.EQUALS)
		  {
		    if (n.arrayOp == n.NONE)
		      {
			return i1.equals(i2);
		      }
		    else
		      {
			i1 = (Invid) n.value;

			/* -- */

			switch (n.arrayOp)
			  {
			  case n.CONTAINSANY:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (i1.equals((Invid) values.elementAt(i)))
				  {
				    return true;
				  }
			      }

			    return false;

			  case n.CONTAINSALL:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (!i1.equals((Invid) values.elementAt(i)))
				  {
				    return false;
				  }
			      }

			    return true;

			  case n.CONTAINSNONE:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (i1.equals((Invid) values.elementAt(i)))
				  {
				    return false;
				  }
			      }

			    return true;

			  default:

			    return false;
			  }
		      }
		  }
		else
		  {
		    return false;	// invalid comparator
		  }
	      }

	    // i.p. address can be arrays

	    if (n.value instanceof Byte[])
	      {
		Byte[] fBytes;
		Byte[] oBytes;

		/* -- */

		if (n.comparator == n.EQUALS)
		  {
		    if (n.arrayOp == n.NONE)
		      {
			fBytes = (Byte[]) value;
			oBytes = (Byte[]) n.value;
		    
			return compareIPs(fBytes, oBytes);
		      }
		    else
		      {
			oBytes = (Byte[]) n.value;

			switch (n.arrayOp)
			  {
			  case n.CONTAINSANY:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (compareIPs(oBytes, ((Byte[]) values.elementAt(i))))
				  {
				    return true;
				  }
			      }

			    return false;

			  case n.CONTAINSALL:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (!compareIPs(oBytes, ((Byte[]) values.elementAt(i))))
				  {
				    return false;
				  }
			      }

			    return true;

			  case n.CONTAINSNONE:
			
			    for (int i = 0; i < values.size(); i++)
			      {
				if (compareIPs(oBytes, ((Byte[]) values.elementAt(i))))
				  {
				    return false;
				  }
			      }

			    return true;

			  default:

			    return false;
			  }
		      }
		  }
		else
		  {
		    return false;	// invalid comparator
		  }
	      }

	    // ---------------------------------------- End possible array fields --------------------

	    if (n.arrayOp != n.NONE)
	      {
		return false;
	      }

	    // ---------------------------------------- Check remaining scalar types --------------------

	    // booleans can't be arrays

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

	    // dates can't be arrays

	    if (value instanceof Date)
	      {
		long time1, time2;

		/* -- */

		time1 = ((Date) value).getTime();
		time2 = ((Date) n.value).getTime();

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

	    // integers can't be arrays

	    if (value instanceof Integer)
	      {
		int val1, val2;

		/* -- */

		val1 = ((Integer) value).intValue();
		val2 = ((Integer) n.value).intValue();

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
	  }

	return false;		// wtf?
      }
    catch (ClassCastException ex)
      {
	return false;
      }
  }

  // helpers

  private static boolean compareIPs(Byte[] param1, Byte[] param2)
  {
    if (param1.length != param2.length)
      {
	return false;
      }
    else
      {
	for (int i = 0; i < param1.length; i++)
	  {
	    if (param1[i].byteValue() != param2[i].byteValue())
	      {
		return false;
	      }
	  }
	
	return true;
      }
  }
}
