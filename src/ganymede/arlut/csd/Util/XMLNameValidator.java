/*
   GASH 2

   XMLNameValidator

   Created: 10 December 2009

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2009
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                XMLNameValidator

------------------------------------------------------------------------------*/

/**
 * This is a dirt-simple XML Name validator for Ganymede, used to
 * verify that proposed field names are acceptable for Ganymede's use.
 *
 * It is intended to validate whether a given string is usable as an
 * XML identifier, but it doesn't fully do the job correctly, as it is
 * using the Java Unicode methods, which differ slightly from those of
 * XML.
 *
 * However, I think you'd have to go to some effort to find cases
 * where they don't overlap, and for our current purposes this will
 * have to be good enough.  Certainly far better than limiting
 * Ganymede field and object type names to the American alphabet.
 *
 * Sun actually uses the Xerces code from Apache to do this, but that
 * code is packaged under com.sun.org.apache.xerces, and we are meant
 * not to rely on that as it is not part of the official JDK API.
 *
 * We can't use the Xerces code ourselves, because it is not
 * compatible with GPL 2.0, but Red Hat's IcedTea distribution
 * considers it okay because it is licensed under GPL+Linking
 * exception.
 *
 * Since Ganymede's use of the GPL does not extend to the licensing of
 * the JDK, we'll use reflection to use the Xerces XName validation if
 * we can, otherwise we'll back down to using the
 * java.lang.Character.isUnicodeIdentifer* methods.
 */

public class XMLNameValidator {

  public static boolean isValidXMLName(String text)
  {
    return true;
  }

  public static boolean isValidGanymedeFieldName(String text)
  {
    return true;
  }
}