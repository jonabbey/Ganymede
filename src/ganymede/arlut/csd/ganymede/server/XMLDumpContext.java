/*
   GASH 2

   XMLDumpContext.java

   The GANYMEDE object storage system.

   Created: 24 March 2000

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.FieldBook;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.Util.XMLUtils;
import com.jclark.xml.output.XMLWriter;
import java.io.FileOutputStream;
import java.io.IOException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  XMLDumpContext

------------------------------------------------------------------------------*/

/**
 * This class is used on the server to group parameters that will guide
 * the server in doing an XML dump.
 */

public final class XMLDumpContext implements java.io.Closeable {

  /**
   * <p>If true, the Ganymede server thread using this XMLDumpContext
   * will include password hash information to the emitted XML
   * file.</p>
   *
   * <p>If false, no password hash data will be emitted at all.</p>
   */

  private boolean dumpPasswordHashes;

  /**
   * If true, the Ganymede server thread using this
   * XMLDumpContext will include plaintext passwords
   * to the emitted XML file whenever possible
   */

  private boolean dumpPlaintextPasswords;

  /**
   * If true, the Ganymede server thread using this
   * XMLDumpContext will include the four standard
   * history fields for each object in the emitted XML
   * file.
   */

  private boolean dumpCreatorModifierInfo;

  /**
   * <p>If non-null, this SyncRunner will be consulted to answer the
   * mayInclude and shouldInclude questions.</p>
   *
   * <p>syncConstraints will be null if we are doing a non-sync channel
   * filtered dump from the DBStore using the xmlclient.</p>
   *
   * <p>If we're using this XMLDumpContext to do a sync, or we're using
   * xmlclient with a channel specified, syncConstraints will help us
   * figure out what specific data we need to dump.</p>
   */

  private SyncRunner syncConstraints;

  /**
   * <p>If we're doing an incremental sync, we'll use a FieldBook to
   * denote what objects and fields we need to write to this
   * XMLDumpContext.</p>
   *
   * <p>If book is non-null, this XMLDumpContext is being used to
   * transmit a transaction delta, in which case we'll want to let
   * callers know so that they can decide to handle certain things
   * (such as embedded objects) differently.</p>
   */

  private FieldBook book = null;

  /**
   * Reference to a DBSesssion that is used to provide a DBSession
   * context when emitting XML from InvidDBFields from DBObjects that
   * were not edited by a DBSesssion.
   */

  private DBSession session = null;

  /**
   * <p>If true, this XMLDumpContext is currently writing out the
   * before state of a transaction incremental dump.  We'll keep hold
   * of this information so server-side code can decide whether to
   * write information from the before or after state of a transaction
   * in progress.</p>
   *
   * <p>If we're not dumping out a transactional incremental dump, this
   * variable should always be false.</p>
   */

  private boolean beforeState = false;

  /**
   * If true, this XMLDumpContext will write out oid attributes for
   * dumped objects.
   */

  private boolean includeOid = false;

  /**
   * If non-null, this XMLDumpContext is being used to present the
   * results of a Ganymede {@link arlut.csd.ganymede.common.Query}
   * operation to the xmlclient.  When {@link
   * arlut.csd.ganymede.server.DBObject DBObjects} are dumped to this
   * XMLDumpContext in such a case, the fields included in the dump
   * will be filtered against the fields requested in this Query
   * object.
   */

  private Query query = null;

  /**
   * The actual writer, from James Clark's XML package.
   */

  XMLWriter xmlOut;

  /**
   * <p>The underlying FileOutputStream, if we have been given it in
   * addition to the XMLWriter.</p>
   *
   * <p>We store this reference to allow the caller to sync() our
   * output to disc.</p>
   */

  FileOutputStream out;

  int indentLevel = 0;

  /* -- */

  /**
   * Constructor for use with dumping schema to XML
   *
   * @param xmlOut The XMLWriter to write to
   */

  public XMLDumpContext(XMLWriter xmlOut)
  {
    this.xmlOut = xmlOut;
  }

  /**
   * Constructor for use with dumping query results to XML
   *
   * @param xmlOut The XMLWriter to write to
   * @param query The {@link arlut.csd.ganymede.common.Query} object
   * defining the fields to include
   */

  public XMLDumpContext(XMLWriter xmlOut, Query query)
  {
    this.xmlOut = xmlOut;
    this.query = query;
  }

  /**
   * Main constructor
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
   * @param includeOid If true, the objects written out to the xml
   * stream will include an "oid" attribute which contains the precise
   * Invid of the object.
   */

  public XMLDumpContext(XMLWriter xmlOut, boolean passwords, boolean historyInfo, SyncRunner syncConstraints, boolean includeOid)
  {
    this.xmlOut = xmlOut;
    dumpPlaintextPasswords = passwords;
    dumpCreatorModifierInfo = historyInfo;
    this.syncConstraints = syncConstraints;
    this.includeOid = includeOid;
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
   * Returns true if this XMLDumpContext was created to write to a
   * sync channel.
   */

  public boolean isSyncing()
  {
    return this.syncConstraints != null;
  }

  /**
   * <p>Associates a FieldBook with this XMLDumpContext.</p>
   *
   * <p>If the book param is non-null, we will use it to decide what
   * objects and fields need to be written out.</p>
   *
   * <p>A non-null book parameter will configure this XMLDumpContext for
   * delta syncing, and isDeltaSyncing() will return true thereafter.</p>
   */

  public void setDeltaFieldBook(FieldBook book)
  {
    this.book = book;
  }

  public void setDBSession(DBSession session)
  {
    this.session = session;
  }

  public DBSession getDBSession()
  {
    return this.session;
  }

  /**
   * Returns true if this XMLDumpContext was created to write to a
   * delta sync channel.
   */

  public boolean isDeltaSyncing()
  {
    return this.book != null;
  }

  /**
   * Sets whether or not this XMLDumpContext is in the middle of writing
   * out the before state of an incremental dump.
   */

  public void setBeforeStateDumping(boolean param)
  {
    this.beforeState = param;
  }

  /**
   * Returns true if this XMLDumpContext is writing out the before
   * state of an incremental dump, or false if we're either writing
   * the after state or are not doing an incremental dump.
   */

  public boolean isBeforeStateDumping()
  {
    return this.beforeState;
  }

  /**
   * This method returns true if this XMLDumpContext wants to include
   * oid attributes in dumped objects.  This will ultimately be true
   * when the -includeOid command line flag is set on the xmlclient,
   * or when the server is dumping to a sync channel.
   */

  public boolean isDumpingOid()
  {
    return includeOid;
  }

  /**
   * Returns the name of the Sync Channel we're writing to, if we
   * are writing to one.
   */

  public String getSyncChannelName()
  {
    if (this.syncConstraints == null)
      {
        return null;
      }

    return this.syncConstraints.getName();
  }

  /**
   * Returns true if the DBObject passed in needs to be synced to
   * this channel.  This version of shouldInclude() assumes that the
   * object passed in is a read-only DBObject.  In this case, the
   * shouldInclude() test will just determine whether an object of
   * this type should ever be written to the sync channel we are
   * writing to, if indeed we are writing to one.
   *
   * If we're not writing to a sync channel, this method always
   * returns true.
   */

  public boolean mayInclude(DBObject object)
  {
    if (book != null)
      {
        return book.has(object.getInvid());
      }

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

  public boolean shouldInclude(DBEditObject object)
  {
    if (book != null)
      {
        return book.has(object.getInvid());
      }

    return syncConstraints == null || syncConstraints.shouldInclude(object);
  }

  /**
   * <p>Returns true if the given field needs to be sent to this sync
   * channel.  This method is responsible for doing the determination
   * only if both field and origField are not null and isDefined().</p>
   *
   * <p>If we're not writing to a sync channel, this method always
   * returns true.</p>
   */

  public boolean shouldInclude(DBField newField, DBField oldField)
  {
    // never write out the container field for embedded objects

    if (newField.getOwner().isEmbedded() && newField.getID() == SchemaConstants.ContainerField)
      {
        return false;
      }

    if (syncConstraints == null)
      {
        return true;
      }

    return syncConstraints.shouldInclude(newField, oldField, book);
  }

  /**
   * <p>Returns true if the kind of DBField passed in needs to be
   * synced in the context of a query or sync channel attached to this
   * XMLDumpContext.  This version of mayInclude() treats the field as
   * always changed, and is intended for doing queries and full state
   * syncs / dumps, and not delta dumps.</p>
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

    if (book != null)
      {
        return book.has(field.getOwner().getInvid(), field.getID());
      }

    if (query != null)
      {
        return query.returnField(field.getID());
      }
    else
      {
        return syncConstraints == null || syncConstraints.mayInclude(field, true);
      }
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

    if (book != null)
      {
        return book.has(field.getOwner().getInvid(), field.getID());
      }

    return syncConstraints == null || syncConstraints.mayInclude(field, hasChanged);
  }

  /**
   * <p>Increase the indent level one step.  Successive indent(),
   * startElementIndent(), and endElementIndent() method calls
   * will indent one level further.</p>
   *
   * <p>This method itself produces no output.</p>
   */

  public void indentOut()
  {
    indentLevel++;
  }

  /**
   * <p>Decreases the indent level one step.  Successive indent(),
   * startElementIndent(), and endElementIndent() method calls
   * will decrease one level fiewer.</p>
   *
   * <p>This method itself produces no output.</p>
   */

  public void indentIn()
  {
    indentLevel--;
  }

  /**
   * This method directly sets the indention level for subsequent
   * indent(), startElementIndent(), and endElementIndent() calls.
   */

  public void setIndentLevel(int x)
  {
    indentLevel = x;
  }

  /**
   * Returns the current indentation level.
   */

  public int getIndent()
  {
    return indentLevel;
  }

  /**
   * This helper method writes a newline and the appropriate amount
   * of indentation to this XMLDumpContext's stream.
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
   * <p>Sets the FileOutputStream associated with this XMLDumpContext,
   * if any.</p>
   */

  public void setFileOutputStream(FileOutputStream outStream)
  {
    this.out = outStream;
  }

  /**
   * <p>Gets the FileOutputStream associated with this XMLDumpContext,
   * if any.</p>
   */

  public FileOutputStream getFileOutputStream()
  {
    return this.out;
  }

  /**
   * <p>Sets whether an XML Query being processed by this XMLDumpContext
   * should allow password field hashes to be transmitted.</p>
   *
   * <p>Will be set to false by the XMLTransmitter constructor if the
   * controlling GanymedeSession is not supergash.</p>
   */

  public void setDumpPasswords(boolean allowHashes)
  {
    dumpPasswordHashes = allowHashes;
  }

  /**
   * Returns true if this XMLDumpContext is configured to dump
   * password information of any kind (i.e., password hashes)
   */

  public boolean doDumpPasswords()
  {
    return dumpPasswordHashes;
  }

  /**
   * Returns true if this XMLDumpContext is configured to dump
   * plaintext password information to disk when a password field
   * has enough information in crypt() or md5Crypt() form that the
   * Ganymede server would be able to load and authenticate against
   * a non-plaintext version of the password.
   */

  public boolean doDumpPlaintext()
  {
    return dumpPlaintextPasswords;
  }

  /**
   * Returns true if this XMLDumpContext is configured to dump
   * creation and modification information when writing out object
   * records.
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
   * <p>Starts an element.</p>
   *
   * <p>This may be followed by zero or more calls to
   * <code>attribute</code>.  The start-tag will be closed by the
   * first following call to any method other than
   * <code>attribute</code>.</p>
   */

  public void startElement(String name) throws IOException
  {
    xmlOut.startElement(name);
  }

  /**
   * <p>Writes an attribute.</p>
   *
   * <p>This is not legal if there have been calls to methods other
   * than <code>attribute</code> since the last call to
   * <code>startElement</code>, unless inside a
   * <code>startAttribute</code>, <code>endAttribute</code> pair.</p>
   */

  public void attribute(String name, String value) throws IOException
  {
    xmlOut.attribute(name, value);
  }

  /**
   * <p>Starts an attribute.</p>
   *
   * <p>This writes the attribute name, <code>=</code> and the opening
   * quote.
   * This provides an alternative to <code>attribute</code>
   * that allows markup to be included in the attribute value.
   * The value of the attribute is written using the normal
   * <code>write</code> methods;
   * <code>endAttribute</code> must be called at the end
   * of the attribute value.
   * Entity and character references can be written using
   * <code>entityReference</code> and <code>characterReference</code>.</p>
   */

  public void startAttribute(String name) throws IOException
  {
    xmlOut.startAttribute(name);
  }

  /**
   * <p>Ends an attribute.</p>
   *
   * <p>This writes the closing quote of the attribute value.</p>
   */

  public void endAttribute() throws IOException
  {
    xmlOut.endAttribute();
  }

  /**
   * <p>Ends an element, indenting before emitting the end tag.</p>
   *
   * <p>If the element comprised no content between the startElement and
   * endElement calls, aside from attributes, then endElement should
   * be used.  endElementIndent() will always put space between a
   * start and end tag.  This may output an end-tag or close the
   * current start-tag as an empty element.</p>
   */

  public void endElementIndent(String name) throws IOException
  {
    indent();
    endElement(name);
  }

  /**
   * <p>Ends an element.</p>
   *
   * <p>This may output an end-tag or close the current start-tag as an
   * empty element.</p>
   */

  public void endElement(String name) throws IOException
  {
    xmlOut.endElement(name);
  }

  /**
   * <p>Writes a processing instruction.</p>
   *
   * <p>If <code>data</code> is non-empty a space will be inserted automatically
   * to separate it from the <code>target</code>.</p>
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
   * <p>Starts the replacement text for an internal entity.</p>
   *
   * <p>The replacement text must be ended with
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
   * would be constructed.</p>
   *
   * <p>This call does not itself cause anything to be written.</p>
   */

  public void startReplacementText() throws IOException
  {
    xmlOut.startReplacementText();
  }

  /**
   * <p>Ends the replacement text for an internal entity.</p>
   *
   * <p>This disables the extra level of escaping enabled by
   * <code>startReplacementText</code>.</p>
   *
   * <p>This call does not itself cause anything to be written.</p>
   */

  public void endReplacementText() throws IOException
  {
    xmlOut.endReplacementText();
  }

  /**
   * <p>Writes markup.</p>
   *
   * <p>The characters in the string will be written as is
   * without being escaped (except for any escaping enabled by
   * <code>startReplacementText</code>).</p>
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

  /**
   * Closes the output stream and clears any DBSession, FieldBook,
   * SyncRunner, or Query linked to this XMLDumpContext.
   */

  public void close() throws IOException
  {
    session = null;
    book = null;
    syncConstraints = null;
    query = null;

    xmlOut.close();
  }
}
