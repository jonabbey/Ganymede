/*
   GASH 2

   XMLNameValidator

   Created: 10 December 2009

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2010
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

/*------------------------------------------------------------------------------
                                                                           class
                                                                XMLNameValidator

------------------------------------------------------------------------------*/

/**
 * This is a dirt-simple XML Name validator for Ganymede, used to
 * verify that proposed field names are acceptable for Ganymede's use.
 *
 * From the XML 1.1 (and XML 1.0 Version 5) standards:
 *
 * NameStartChar ::= ":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] |
 * [#xD8-#xF6] | [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF] |
 * [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] |
 * [#x3001-#xD7FF] | [#xF900-#xFDCF] | [#xFDF0-#xFFFD] |
 * [#x10000-#xEFFFF]
 *
 * NameChar ::= NameStartChar | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
 *
 */

public class XMLNameValidator {

  /**
   * Returns true if codePoint is a valid character to start a name
   * (element name or attribute name) in XML 1.1 or XML 1.0 (revision
   * 5).
   */

  public static boolean isValidNameStartChar(int codePoint)
  {
    return (codePoint == ':' || codePoint == '_' ||
	    (codePoint >= 'A' && codePoint <= 'Z') ||
	    (codePoint >= 'a' && codePoint <= 'z') ||
	    (codePoint >= 0xC0 && codePoint <= 0xD6) ||
	    (codePoint >= 0xD8 && codePoint <= 0xF6) ||
	    (codePoint >= 0xF8 && codePoint <= 0x2FF) ||
	    (codePoint >= 0x370 && codePoint <= 0x37D) ||
	    (codePoint >= 0x37F && codePoint <= 0x1FFF) ||
	    (codePoint >= 0x200C && codePoint <= 0x200D) ||
	    (codePoint >= 0x2070 && codePoint <= 0x218F) ||
	    (codePoint >= 0x2C00 && codePoint <= 0x2FEF) ||
	    (codePoint >= 0x3001 && codePoint <= 0xD7FF) ||
	    (codePoint >= 0xF900 && codePoint <= 0xFDCF) ||
	    (codePoint >= 0xFDF0 && codePoint <= 0xFFFD) ||
	    (codePoint >= 0x10000 && codePoint <= 0xEFFFF));
  }

  /**
   * Returns true if codePoint is a valid character for a character
   * after the first in a name (element name or attribute name) in XML
   * 1.1 or XML 1.0 (revision 5).
   */

  public static boolean isValidNameChar(int codePoint)
  {
    return (isValidNameStartChar(codePoint) ||
	    codePoint == '-' || codePoint == '.' ||
	    (codePoint >= '0' && codePoint <= '9') ||
	    codePoint == 0xB7 ||
	    (codePoint >= 0x300 && codePoint <= 0x36F) ||
	    (codePoint >= 0x203F && codePoint <= 0x2040));
  }

  /**
   * Returns true if text is a valid name for element name or
   * attribute name in XML 1.1 or XML 1.0 (revision 5)
   */

  public static boolean isValidXMLName(String text)
  {
    if (text == null || text.isEmpty())
      {
	return false;
      }

    for (int offset = 0; offset < text.length();)
      {
	int codePoint = text.codePointAt(offset);

	if (offset == 0)
	  {
	    if (!XMLNameValidator.isValidNameStartChar(codePoint))
	      {
		return false;
	      }
	  }
	else
	  {
	    if (!XMLNameValidator.isValidNameChar(codePoint))
	      {
		return false;
	      }
	  }

	offset += Character.charCount(codePoint);
      }

    return true;
  }

  /**
   * Returns true if text can be used as a Ganymede object type or
   * field name.
   *
   * Ganymede names differ from XML names in that underscores are not
   * allowed (because they are used as the representation of spaces),
   * and spaces are, after the first character in the name.
   *
   * We're also not going to permit colons to avoid confusion about
   * XML namespaces.
   */

  public static boolean isValidGanymedeName(String text)
  {
    if (text == null || text.isEmpty())
      {
	return false;
      }

    for (int offset = 0; offset < text.length();)
      {
	int codePoint = text.codePointAt(offset);

	if (offset == 0)
	  {
	    if (codePoint == ' ' || codePoint == '_' || codePoint == ':')
	      {
		return false;
	      }

	    if (!XMLNameValidator.isValidNameStartChar(codePoint))
	      {
		return false;
	      }
	  }
	else
	  {
	    if (codePoint == '_' || codePoint == ':')
	      {
		return false;
	      }

	    if (codePoint != ' ' && !XMLNameValidator.isValidNameChar(codePoint))
	      {
		return false;
	      }
	  }

	offset += Character.charCount(codePoint);
      }

    // XML names that begin with xml are reserved, so we don't want to
    // allow Ganymede names to start that way.

    if (text.startsWith("xml"))
      {
	return false;
      }

    return true;
  }
}
