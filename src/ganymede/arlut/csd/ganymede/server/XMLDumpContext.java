/*
   GASH 2

   XMLDumpContext.java

   The GANYMEDE object storage system.

   Created: 24 March 2000
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.Util.XMLUtils;
import com.jclark.xml.output.XMLWriter;
import java.io.IOException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  XMLDumpContext

------------------------------------------------------------------------------*/

/**
 * <P>This class is used on the server to group parameters that will guide
 * the server in doing an XML dump.</P>
 */

public class XMLDumpContext {

  /**
   * <P>If true, the Ganymede server thread using this
   * XMLDumpContext will include plaintext passwords
   * to the emitted XML file whenever possible</P>
   */

  boolean dumpPlaintextPasswords;

  /**
   * <P>If true, the Ganymede server thread using this
   * XMLDumpContext will include the four standard
   * history fields for each object in the emitted XML
   * file.</P>
   */

  boolean dumpCreatorModifierInfo;

  /**
   * <p>If non-null, this SyncRunner will be consulted to answer the
   * mayInclude and shouldInclude questions.</p>
   */

  private SyncRunner syncConstraints;

  /**
   * <p>The actual writer, from James Clark's XML package.</p>
   */

  XMLWriter xmlOut;

  int indentLevel = 0;

  /* -- */

  /**
   * <P>Main constructor</P>
   *
   * @param xmlOut The XMLWriter to write to
   * @param passwords Include plaintext passwords in password field dumps,
   * even if the password field includes UNIX crypt() or md5Crypt() passwords
   * that could be depended on for authentication without having to reveal
   * the plaintext.
   * @param historyInfo If true, all objects dumped out using this XMLDumpContext
   * will include creator and modification information.
   * @param syncConstraints If non-null, this XMLDumpContext will
   * carry along with it enough information to answer questions about
   * whether a given object or field should be emitted to the Sync Channel this XMLDumpContext
   * is writing to.  If null, the mayInclude and shouldInclude methods will always
   * return true.
   */
  
  public XMLDumpContext(XMLWriter xmlOut, boolean passwords, boolean historyInfo, SyncRunner syncConstraints)
  {
    this.xmlOut = xmlOut;
    dumpPlaintextPasswords = passwords;
    dumpCreatorModifierInfo = historyInfo;
    this.syncConstraints = syncConstraints;
  }

  public XMLWriter getWriter()
  {
    return xmlOut;
  }

  public XMLWriter writer()
  {
    return xmlOut;
  }

  /**
   * <p>Returns true if the DBObject passed in needs to be synced to
   * this channel.  This version of shouldInclude() assumes that the
   * object passed in is a read-only DBObject.  In this case, the
   * shouldInclude() test will just determine whether an object of
   * this type should ever be written to the sync channel we are
   * writing to, if indeed we are writing to one.</p>
   *
   * <p>If we're not writing to a sync channel, this method always
   * returns true.</p>
   */

  public boolean mayInclude(DBObject object)
  {
    return syncConstraints == null || syncConstraints.mayInclude(object);
  }

  /**
   * <p>Returns true if the DBEditObject passed in needs to be synced
   * to the sync channel we're writing to.  Because we're passed in a
   * DBEditObject, we can assume that we are being asked about whether
   * we should write out this object in the course of a sync
   * operation.  When DBStore is writing out an XML dump, all the
   * objects should be read only copies, so we will always use the
   * DBObject version of shouldInclude().</p>
   *
   * <p>If we're not writing to a sync channel, this method always
   * returns true.</p>
   */

  public boolean shouldInclude(DBEditObject object, DBEditSet transaction)
  {
    return syncConstraints == null || syncConstraints.shouldInclude(object, transaction);
  }

  /**
   * <p>Returns true if the given field needs to be sent to this sync
   * channel.  This method is responsible for doing the determination
   * only if both field and origField are not null and isDefined().</p>
   *
   * <p>The transaction DBEditObject is used to determine whether an
   * edit-in-place InvidDBField contains embedded objects that were
   * changed enough to need to be sent to this sync channel.</p>
   *
   * <p>If we're not writing to a sync channel, this method always
   * returns true.</p>
   */

  public boolean shouldInclude(DBField newField, DBField oldField, DBEditSet transaction)
  {
    return syncConstraints == null || syncConstraints.shouldInclude(newField, oldField, transaction);
  }

  /**
   * <p>Returns true if the kind of DBField passed in needs to be
   * synced to the sync channel attached to this XMLDumpContext.  This
   * version of shouldInclude() treats the field as always changed,
   * and is intended for doing full dumps, rather than delta
   * dumps.</p>
   *
   * <p>This differs from shouldInclude on DBField in that this method
   * leaves it to the caller to decide whether the field has changed.</p>
   *
   * <p>If we're not writing to a sync channel, this method always
   * returns true.</p>
   */

  public boolean mayInclude(DBField field)
  {
    if ((field.getID() == SchemaConstants.CreationDateField ||
	 field.getID() == SchemaConstants.CreatorField ||
	 field.getID() == SchemaConstants.ModificationDateField ||
	 field.getID() == SchemaConstants.ModifierField) &&
	!doDumpHistoryInfo())
      {
	return false;
      }

    return syncConstraints == null || syncConstraints.mayInclude(field, true);
  }

  /**
   * <p>Returns true if the kind of DBField passed in needs to be
   * synced to the sync channel attached to this XMLDumpContext. The
   * hasChanged parameter should be set to true if the field being
   * tested was changed in the current transaction, or false if it
   * remains unchanged.</p>
   *
   * <p>This differs from shouldInclude on DBField in that this method
   * leaves it to the caller to decide whether the field has changed.</p>
   *
   * <p>If we're not writing to a sync channel, this method always
   * returns true.</p>
   */

  public boolean mayInclude(DBField field, boolean hasChanged)
  {
    if ((field.getID() == SchemaConstants.CreationDateField ||
	 field.getID() == SchemaConstants.CreatorField ||
	 field.getID() == SchemaConstants.ModificationDateField ||
	 field.getID() == SchemaConstants.ModifierField) &&
	!doDumpHistoryInfo())
      {
	return false;
      }

    return syncConstraints == null || syncConstraints.mayInclude(field, hasChanged);
  }

  /**
   * <P>Increase the indent level one step.  Successive indent(),
   * startElementIndent(), and endElementIndent() method calls
   * will indent one level further.</P>
   *
   * <P>This method itself produces no output.</P>
   */

  public void indentOut()
  {
    indentLevel++;
  }

  /**
   * <P>Decreases the indent level one step.  Successive indent(),
   * startElementIndent(), and endElementIndent() method calls
   * will decrease one level fiewer.</P>
   *
   * <P>This method itself produces no output.</P>
   */

  public void indentIn()
  {
    indentLevel--;
  }

  /**
   * <P>This method directly sets the indention level for subsequent
   * indent(), startElementIndent(), and endElementIndent() calls.</P>
   */

  public void setIndentLevel(int x)
  {
    indentLevel = x;
  }

  /**
   * <P>Returns the current indentation level.</P>
   */

  public int getIndent()
  {
    return indentLevel;
  }

  /**
   * <P>This helper method writes a newline and the appropriate amount
   * of indentation to this XMLDumpContext's stream.</p>
   */

  public void indent() throws IOException
  {
    XMLUtils.indent(xmlOut, indentLevel);
  }

  public void skipLine() throws IOException
  {
    XMLUtils.indent(xmlOut, 0);
  }

  /**
   * <P>Returns true if this XMLDumpContext is configured to dump
   * plaintext password information to disk when a password field
   * has enough information in crypt() or md5Crypt() form that the
   * Ganymede server would be able to load and authenticate against
   * a non-plaintext version of the password.</P>
   */

  public boolean doDumpPlaintext()
  {
    return dumpPlaintextPasswords;
  }

  /**
   * <P>Returns true if this XMLDumpContext is configured to dump
   * creation and modification information when writing out object
   * records.</P>
   */

  public boolean doDumpHistoryInfo()
  {
    return dumpCreatorModifierInfo;
  }

  /**
   * Starts an element, preceded by a newline and indented according
   * to the current indent level.  This may be followed by zero or
   * more calls to <code>attribute</code>.  The start-tag will be
   * closed by the first following call to any method other than
   * <code>attribute</code>.
   */

  public void startElementIndent(String name) throws IOException
  {
    indent();
    startElement(name);
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
   * Ends an element, indenting before emitting the end tag.  If
   * the element comprised no content between the startElement
   * and endElement calls, aside from attributes, then endElement
   * should be used.  endElementIndent() will always put space
   * between a start and end tag.
   * This may output an end-tag or close the current start-tag as an
   * empty element.
   */

  public void endElementIndent(String name) throws IOException
  {
    indent();
    endElement(name);
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

  public void flush() throws IOException
  {
    xmlOut.flush();
  }

  public void close() throws IOException
  {
    xmlOut.close();
  }
}
