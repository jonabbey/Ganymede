/*

   WordWrap.java

   This class provides a static word wrap method.
   
   Created: 12 September 1997
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 1999/06/18 22:43:12 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        WordWrap

------------------------------------------------------------------------------*/

/**
 * <P>Handy word wrap module.  This class provides the static
 * {@link arlut.csd.Util.WordWrap#wrap(java.lang.String, int, java.lang.String) wrap()}
 * method for word-wrapping text strings.</P>
 *
 * <P>Used in the Ganymede client and server for utilitarian word-wrapping.</P>
 *
 * @version $Revision: 1.5 $ $Date: 1999/06/18 22:43:12 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public class WordWrap {

  static final boolean debug = false;

  /* -- */

  /**
   *
   * This method takes a string and wraps it to a line length of no more than
   * wrap_length.  If prepend is not null, each resulting line will be prefixed
   * with the prepend string.  In that case, resultant line length will be no
   * more than wrap_length + prepend.length()
   *
   */

  public static String wrap(String inString, int wrap_length, String prepend)
  {
    char[] 
      charAry;

    int 
      p,
      p2,
      offset = 0,
      marker;

    StringBuffer
      result = new StringBuffer();

    /* -- */

    if (inString == null)
      {
	return null;
      }

    if (wrap_length < 0)
      {
	throw new IllegalArgumentException("bad params");
      }

    if (prepend != null)
      {
	result.append(prepend);
      }

    if (debug)
      {
	System.err.println("String size = " + inString.length());
      }

    charAry = inString.toCharArray();

    p = marker = 0;

    // each time through the loop, p starts out pointing to the same char as marker
    
    while (marker < charAry.length)
      {
	while (p < charAry.length && (charAry[p] != '\n') && ((p - marker) < wrap_length))
	  {
	    p++;
	  }
	
	if (p == charAry.length)
	  {
	    if (debug)
	      {
		System.err.println("At completion..");
	      }

	    result.append(inString.substring(marker, p));
	    return result.toString();
	  }

	if (debug)
	  {
	    System.err.println("Step 1: p = " + p + ", marker = " + marker);
	  }

	if (charAry[p] == '\n')
	  {
	    /* We've got a newline.  This newline is bound to have
	       terminated the while loop above.  Step p back one
	       character so that the isspace(*p) check below will detect
	       that it hit the \n, and will do the right thing. */


	    result.append(inString.substring(marker, p+1));

	    if (prepend != null)
	      {
		result.append(prepend);
	      }

	    if (debug)
	      {
		System.err.println("found natural newline.. current result = " + result.toString());
	      }

	    p = marker = p+1;

	    continue;
	  }

	if (debug)
	  {
	    System.err.println("Step 2: hit wrap length, back searching for newline");
	  }

	p2 = p-1;
      
	/* We've either hit the end of the string, or we've
	   gotten past the wrap_length.  Back p2 up to the last space
	   before the wrap_length, if there is such a space.

	   Note that if the next character in the string (the character
	   immediately after the break point) is a space, we don't need
	   to back up at all.  We'll just print up to our current
	   location, do the newline, and skip to the next line. */
	
	if (p < charAry.length)
	  {
	    if (isspace(charAry[p]))
	      {
		offset = 1;	/* the next character is white space.  We'll
				   want to skip that. */
	      }
	    else
	      {
		/* back p2 up to the last white space before the break point */

		while ((p2 > marker) && !isspace(charAry[p2]))
		  {
		    p2--;
		  }

		offset = 0;
	      }
	  }

	/* If the line was completely filled (no place to break),
	   we'll just copy the whole line out and force a break. */
	
	if (p2 == marker)
	  {
	    p2 = p-1;

	    if (debug)
	      {
		System.err.println("Step 3: no opportunity for break, forcing..");
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("Step 3: found break at column " + p2);
	      }
	  }

	if (!isspace(charAry[p2]))
	  {
	    /* If weren't were able to back up to a space, copy
	       out the whole line, including the break character 
	       (in this case, we'll be making the string one
	       character longer by inserting a newline). */
	    
	    result.append(inString.substring(marker, p2+1));
	  }
	else
	  {
	    /* The break character is whitespace.  We'll
	       copy out the characters up to but not
	       including the break character, which
	       we will effectively replace with a
	       newline. */

	    result.append(inString.substring(marker, p2));
	  }

	/* If we have not reached the end of the string, newline */

	if (p < charAry.length) 
	  {
	    result.append("\n");

	    if (prepend != null)
	      {
		result.append(prepend);
	      }
	  }

	p = marker = p2 + 1 + offset;
      }

    return result.toString();
  }

  public static String wrap(String inString, int wrap_length)
  {
    return wrap(inString, wrap_length, null);
  }

  public static boolean isspace(char c)
  {
    return (c == '\n' || c == ' ' || c == '\t');
  }

  public static void main (String args[])
  {
    System.err.println("Word Wrap 20 chars: ");
    System.err.println(wrap("Mynameisinigomontoyayoukilledmyfatherpreparetodieok?", 20));
  }
}
