/*

   Result.java

   The result class is used to return labeled invid's for
   database queries.

   Result is serializable.
   
   Created: 21 October 1996 
   Version: $Revision: 1.7 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          Result

------------------------------------------------------------------------------*/

/**
 * The result class is used to return labeled invid's for
 * database queries.
 */

public class Result implements java.io.Serializable {

  static final long serialVersionUID = -8417751229367613063L;

  // ---
  
  Invid invid;	// remote reference to an object on the server
  String label = null;

  /* -- */

  public Result(Invid invid, String label)
  {
    this.invid = invid;
    this.label = label;
  }

  public String toString()
  {
    return label;
  }

  public Invid getInvid()
  {
    return invid;
  }

  public String resultDump()
  {
    StringBuffer buffer = new StringBuffer();
    char[] chars;

    /* -- */

    chars = invid.toString().toCharArray();
    
    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '|')
	  {
	    buffer.append("\\|");
	  }
	else if (chars[j] == '\n')
	  {
	    buffer.append("\\\n");
	  }
	else if (chars[j] == '\\')
	  {
	    buffer.append("\\\\");
	  }
	else
	  {
	    buffer.append(chars[j]);
	  }
      }

    buffer.append("|");

    chars = label.toCharArray();
    
    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '|')
	  {
	    buffer.append("\\|");
	  }
	else if (chars[j] == '\n')
	  {
	    buffer.append("\\\n");
	  }
	else if (chars[j] == '\\')
	  {
	    buffer.append("\\\\");
	  }
	else
	  {
	    buffer.append(chars[j]);
	  }
      }

    buffer.append("\n");

    return buffer.toString();
  }

  // and hashCode

  public int hashCode()
  {
    return invid.hashCode();
  }
}
