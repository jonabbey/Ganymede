/*
   GASH 2

   XMLCharData.java

   The GANYMEDE object storage system.

   Created: 9 March 2000
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 2000/03/15 03:36:51 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     XMLCharData

------------------------------------------------------------------------------*/

/**
 * <P>Character Data class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

public class XMLCharData extends XMLItem {

  String data;
  Boolean nonEmpty = null;

  /* -- */

  XMLCharData(char ch[], int start, int length)
  {
    data = new String(ch, start, length);
  }

  /**
   * <P>This method returns the character data for this XMLItem.</P>
   */

  public String getString()
  {
    return data;
  }

  /**
   * <P>This method returns the character data for this XMLItem with
   * leading and trailing whitespace filtered out.</P>
   */

  public String getCleanString()
  {
    return data.trim();
  }

  /**
   * <P>This method returns true if this char data contains any non-whitespace
   * data.</P>
   */

  public boolean containsNonWhitespace()
  {
    // we use the java.lang.Boolean nonEmpty to
    // cache this test, since the trim operation
    // can be time intensive

    if (nonEmpty == null)
      {
	if (data.trim().length() != 0)
	  {
	    nonEmpty = new Boolean(true);
	  }
	else
	  {
	    nonEmpty = new Boolean(false);
	  }
      }

    return nonEmpty.booleanValue();
  }

  public String toString()
  {
    return "XML Chardata: " + getCleanString();
  }
}
