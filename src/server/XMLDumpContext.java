/*
   GASH 2

   XMLDumpContext.java

   The GANYMEDE object storage system.

   Created: 24 March 2000
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/03/25 05:36:48 $
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

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.text.*;
import java.rmi.*;

import com.jclark.xml.output.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  XMLDumpContext

------------------------------------------------------------------------------*/

/**
 * <P>This class is used on the server to group parameters that will guide
 * the server in doing an XML dump.</P>
 */

public class XMLDumpContext {

  boolean dumpPlaintextPasswords;
  boolean dumpCreatorModifierInfo;
  boolean dumpObjectNumbers;
  XMLWriter xmlOut;
  int indentLevel = 0;

  /* -- */
  
  public XMLDumpContext(XMLWriter xmlOut, boolean passwords, boolean historyInfo, boolean objectNumbers)
  {
    dumpPlaintextPasswords = passwords;
    dumpCreatorModifierInfo = historyInfo;
    dumpObjectNumbers = objectNumbers;
  }

  public XMLWriter getWriter()
  {
    return xmlOut;
  }

  public XMLWriter writer()
  {
    return xmlOut;
  }
  
  public void bumpIndentLevel()
  {
    indentLevel++;
  }

  public void dumpIndentLevel()
  {
    indentLevel--;
  }

  public void setIndentLevel(int x)
  {
    indentLevel = x;
  }

  public int getIndent()
  {
    return indentLevel;
  }

  public void indent() throws IOException
  {
    XMLUtils.indent(xmlOut, indentLevel);
  }

  public void skipLine() throws IOException
  {
    XMLUtils.indent(xmlOut, 0);
  }

  public boolean doDumpPlaintext()
  {
    return dumpPlaintextPasswords;
  }

  public boolean doDumpHistoryInfo()
  {
    return dumpCreatorModifierInfo;
  }

  public boolean doDumpObjectNumbers()
  {
    return dumpObjectNumbers;
  }

  /**
   * Starts an element.
   * This may be followed by zero or more calls to <code>attribute</code>.
   * The start-tag will be closed by the first following call to any method
   * other than <code>attribute</code>.
   */
  public void startElement(String name) throws IOException
  {
    xmlOut.startElement(name);
  }

  /**
   * Writes an attribute.
   * This is not legal if there have been calls to methods other than
   * <code>attribute</code> since the last call to <code>startElement</code>,
   * unless inside a <code>startAttribute</code>, <code>endAttribute</code>
   * pair.
   */
  public void attribute(String name, String value) throws IOException
  {
    xmlOut.attribute(name, value);
  }

  /**
   * Starts an attribute.
   * This writes the attribute name, <code>=</code> and the opening
   * quote.
   * This provides an alternative to <code>attribute</code>
   * that allows markup to be included in the attribute value.
   * The value of the attribute is written using the normal
   * <code>write</code> methods;
   * <code>endAttribute</code> must be called at the end
   * of the attribute value.
   * Entity and character references can be written using
   * <code>entityReference</code> and <code>characterReference</code>.
   */
  public void startAttribute(String name) throws IOException
  {
    xmlOut.startAttribute(name);
  }

  /**
   * Ends an attribute.
   * This writes the closing quote of the attribute value.
   */
  public void endAttribute() throws IOException
  {
    xmlOut.endAttribute();
  }

  /**
   * Ends an element.
   * This may output an end-tag or close the current start-tag as an
   * empty element.
   */
  public void endElement(String name) throws IOException
  {
    xmlOut.endElement(name);
  }

  /**
   * Writes a processing instruction.
   * If <code>data</code> is non-empty a space will be inserted automatically
   * to separate it from the <code>target</code>.
   */
  public void processingInstruction(String target, String data) throws IOException
  {
    xmlOut.processingInstruction(target, data);
  }

  /**
   * Writes a comment.
   */
  public void comment(String body) throws IOException
  {
    xmlOut.comment(body);
  }

  /**
   * Writes an entity reference.
   */
  public void entityReference(boolean isParam, String name) throws IOException
  {
    xmlOut.entityReference(isParam, name);
  }

  /**
   * Writes a character reference.
   */
  public void characterReference(int n) throws IOException
  {
    xmlOut.characterReference(n);
  }

  /**
   * Writes a CDATA section.
   */
  public void cdataSection(String content) throws IOException
  {
    xmlOut.cdataSection(content);
  }

  /**
   * Starts the replacement text for an internal entity.
   * The replacement text must be ended with
   * <code>endReplacementText</code>.
   * This enables an extra level of escaping that protects
   * against the process of constructing an entity's replacement
   * text from the literal entity value.
   * See Section 4.5 of the XML Recommendation.
   * Between a call to <code>startReplacementText</code>
   * and <code>endReplacementText</code>, the argument to
   * <code>markup</code> would specify entity replacement text;
   * these would be escaped so that when processed as
   * a literal entity value, the specified entity replacement text
   * would be constructed.
   * This call does not itself cause anything to be written.
   */
  public void startReplacementText() throws IOException
  {
    xmlOut.startReplacementText();
  }

  /**
   * Ends the replacement text for an internal entity.
   * This disables the extra level of escaping enabled by
   * <code>startReplacementText</code>.
   * This call does not itself cause anything to be written.
   */
  public void endReplacementText() throws IOException
  {
    xmlOut.endReplacementText();
  }

  /**
   * Writes markup.  The characters in the string will be written as is
   * without being escaped (except for any escaping enabled by
   * <code>startReplacementText</code>).
   */
  public void markup(String str) throws IOException
  {
    xmlOut.markup(str);
  }

  public void write(String str) throws IOException
  {
    xmlOut.write(str);
  }

  public void close() throws IOException
  {
    xmlOut.close();
  }
}
