/*
   GASH 2

   PasswordDBField.java

   The GANYMEDE object storage system.

   Created: 21 July 1997

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2006
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jcrypt.jcrypt;
import md5.MD5Crypt;
import arlut.csd.Util.TranslationService;
import arlut.csd.crypto.SSHA;
import arlut.csd.crypto.smbencrypt;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.pass_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PasswordDBField

------------------------------------------------------------------------------*/

/**
 * PasswordDBField is a subclass of {@link
 * arlut.csd.ganymede.server.DBField DBField} for the storage and
 * handling of password fields in the {@link
 * arlut.csd.ganymede.server.DBStore DBStore} on the Ganymede server.
 *
 * The Ganymede client talks to PasswordDBFields through the {@link
 * arlut.csd.ganymede.rmi.pass_field pass_field} RMI interface.
 *
 * This class differs a bit from most subclasses of {@link
 * arlut.csd.ganymede.server.DBField DBField} in that the normal
 * setValue()/getValue() methods are non-functional.  Instead, there
 * are special methods used to set or access password information in
 * crypted and non-crypted forms.
 *
 * Crypted passwords are stored in the UNIX crypt() format.  See the
 * {@link jcrypt jcrypt} class for details on the crypt hashing.
 *
 * There are no methods provided to allow remote access to password
 * information..  server-side code must locally access the {@link
 * arlut.csd.ganymede.server.PasswordDBField#getUNIXCryptText()
 * getUNIXCryptText()} and {@link
 * arlut.csd.ganymede.server.PasswordDBField#getPlainText()
 * getPlainText()} methods to get access to the password information.
 * Generally, even in that case, only crypted password information
 * will be available.  If this password field was configured to store
 * encrypted passwords by way of its {@link
 * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField},
 * this password field will never emit() the plaintext to disk.
 * Instead, the crypt()'ed password information will be retained on
 * disk for user authentication.  The plaintext of the password
 * <b>may</b> be retained in memory for the purpose of replicating to
 * systems that do not use the UNIX crypt() format for password
 * hashing, but only on a temporary basis, for those passwords whose
 * plaintext was provided to the server during its operation.
 * Basically, it's for custom builder tasks that need to be able to
 * provide the plaintext of a stored password for replication to a
 * system with an incompatible hash format.
 *
 * The new {@link arlut.csd.ganymede.server.SyncRunner} and {@link
 * arlut.csd.ganymede.server.XMLDumpContext} classes are also capable
 * of conspiring to dump plain text passwords into properly configured
 * sync channels.
 *
 * @see arlut.csd.ganymede.rmi.BaseField#setCrypted(boolean)
 * @see arlut.csd.ganymede.rmi.BaseField#setPlainText(boolean)
 */

public class PasswordDBField extends DBField implements pass_field {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.PasswordDBField");

  // ---

  /**
   * Traditional Unix crypt()'ed pass.  Only good for validating the
   * first 8 characters of a plaintext.
   */

  private String cryptedPass;

  /**
   * The complex md5crypt()'ed password, as in OpenBSD, FreeBSD,
   * Linux PAM, etc.  Good for validating indefinite length
   * strings.
   */

  private String md5CryptPass;

  /**
   * The complex md5crypt()'ed password, with the magic string used
   * by Apache for their htpasswd file format.  Good for validating
   * indefinite length strings.
   */

  private String apacheMd5CryptPass;

  /**
   * Plaintext password.. will never be saved to disk if we have
   * another hash format available to validate, unless this field has
   * been specifically configured to always save plaintext to disk in
   * the schema editor.  See {@link
   * arlut.csd.ganymede.server.DBObjectBaseField#isPlainText()} for more
   * detail.
   */

  private String uncryptedPass;

  /**
   * Samba LANMAN hash, for Win95 clients.  Only good for
   * validating the first 14 characters of a plaintext.  This hash is
   * actually incredibly, mind-crushingly weak.. weaker than
   * traditional Unix crypt, even.  If you're basing your password
   * security on this hash still, you're in trouble.
   *
   * At the time this comment was written (26 October 2004), the
   * following URL had a really good discussion of a great number of
   * password authenticator hash algorithms:
   *
   * http://www.harper.no/valery/default,date,2004-08-27.aspx
   */

  private String lanHash;

  /**
   * Samba md4 Unicode hash, for WinNT/2k clients. Good for
   * validating up to 2^64 bits of plaintext.. effectively indefinite
   * in extent 
   */

  private String ntHash;

  /**
   * SSHA hash, for LDAP.  Good for validating up to 2^64 bits of
   * plaintext.. effectively indefinite in extent.  Probably the
   * strongest hash here in terms of difficulty of finding collisions,
   * but it's very quick to evaluate, so a dictionary attack against
   * this hash can proceed rapidly.
   *
   * Note that we keep the sshaHash string here in the same form
   * as would be used in an LDAP store.
   *
   * This is Netscape's salted variant of the FIPS SHA-1 standard.
   * SHA-1 is described at http://en.wikipedia.org/wiki/SHA-1, while
   * SSHA is described at
   * http://www.openldap.org/faq/data/cache/347.html.
   */

  private String sshaHash;

  /* -- */

  /**
   * Receive constructor.  Used to create a PasswordDBField from a DBStore/DBJournal
   * DataInput stream.
   */

  PasswordDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = null;
    this.owner = owner;
    this.fieldcode = definition.getID();
    receive(in, definition);
  }

  /** 
   * No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} definition
   * indicates that a given field may be present, but for which no
   * value has been stored in the {@link arlut.csd.ganymede.server.DBStore
   * DBStore}.
   *
   * Used to provide the client a template for 'creating' this
   * field if so desired. 
   */

  PasswordDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();
    
    value = null;
  }

  /**
   * Copy constructor.
   */

  public PasswordDBField(DBObject owner, PasswordDBField field)
  {
    this.owner = owner;
    this.fieldcode = field.getID();

    cryptedPass = field.cryptedPass;
    md5CryptPass = field.md5CryptPass;
    apacheMd5CryptPass = field.apacheMd5CryptPass;
    uncryptedPass = field.uncryptedPass;
    lanHash = field.lanHash;
    ntHash = field.ntHash;
    sshaHash = field.sshaHash;
  }

  /**
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public boolean isDefined()
  {
    return (cryptedPass != null || md5CryptPass != null ||
	    apacheMd5CryptPass != null || uncryptedPass != null || lanHash != null
	    || ntHash != null || sshaHash != null);
  }

  /**
   * This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of {@link
   * arlut.csd.ganymede.server.DBField DBField} may implement this in
   * different ways, if simply setting the field's value member to
   * null is not appropriate.  Any namespace values claimed by the
   * field will be released, and when the transaction is committed,
   * this field will be released.
   *
   * Note that this method is really only intended for those fields
   * which have some significant internal structure to them, such as
   * permission matrix, field option matrix, and password fields.
   *
   * NOTE: There is, at present, no defined DBEditObject callback
   * method that tracks generic field nullification.  This means that
   * if your code uses setUndefined on a PermissionMatrixDBField,
   * FieldOptionDBField, or PasswordDBField, the plugin code is not
   * currently given the opportunity to review and refuse that
   * operation.  Caveat Coder.
   */

  public synchronized ReturnVal setUndefined(boolean local)
  {
    if (!isEditable(local))
      {
	// "Permissions Error"
	// "You do not have permission to clear the "{0}" password field in object "{1}"."
	return Ganymede.createErrorDialog(ts.l("setUndefined.perm_error_subj"),
					  ts.l("setUndefined.perm_error_text", this.getName(), owner.getLabel()));
      }

    clear_stored();
    return null;
  }

  /**
   * private helper to clear all stored password information in this field
   */

  private synchronized final void clear_stored()
  {
    cryptedPass = null;
    md5CryptPass = null;
    apacheMd5CryptPass = null;
    uncryptedPass = null;
    ntHash = null;
    lanHash = null;
    sshaHash = null;
  }

  /**
   * Returns true if obj is a field with the same value(s) as
   * this one.
   *
   * This method is ok to be synchronized because it does not
   * call synchronized methods on any other object.
   */

  public synchronized boolean equals(Object obj)
  {
    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    PasswordDBField origP = (PasswordDBField) obj;

    return (streq(cryptedPass, origP.cryptedPass) &&
	    streq(md5CryptPass, origP.md5CryptPass) &&
	    streq(apacheMd5CryptPass, origP.apacheMd5CryptPass) &&
	    streq(uncryptedPass, origP.uncryptedPass) && 
	    streq(lanHash, origP.lanHash) && 
	    streq(ntHash, origP.ntHash) &&
	    streq(sshaHash, origP.sshaHash));
  }

  /**
   * Convenience null-friendly string comparison helper.
   */

  private final boolean streq(String str1, String str2)
  {
    if (str1 == null && str2 == null)
      {
	return true;
      }

    if (str1 == null || str2 == null)
      {
	return false;
      }

    return str1.equals(str2);
  }

  /**
   * This method copies the current value of this DBField
   * to target.  The target DBField must be contained within a
   * checked-out DBEditObject in order to be updated.  Any actions
   * that would normally occur from a user manually setting a value
   * into the field will occur.
   *
   * NOTE: this method is mainly used in cloning objects, and
   * {@link arlut.csd.ganymede.server.DBEditObject#cloneFromObject(arlut.csd.ganymede.server.DBSession, arlut.csd.ganymede.server.DBObject, boolean) cloneFromObject}
   * doesn't allow cloning of password fields by default.
   *
   * @param target The DBField to copy this field's contents to.
   * @param local If true, permissions checking is skipped.
   */

  public synchronized ReturnVal copyFieldTo(PasswordDBField target, boolean local)
  {
    if (!local)
      {
	if (!verifyReadPermission())
	  {
	    // "Error Copying Password Field"
	    // "Can''t copy field "{0}" in object "{1}", no read privileges on source."
	    return Ganymede.createErrorDialog(ts.l("copyFieldTo.error_subj"),
					      ts.l("copyFieldTo.no_read", this.getName(), owner.getLabel()));
	  }
      }
	
    if (!target.isEditable(local))
      {
	// "Error Copying Password Field"
	// "Can''t copy field "{0}" in object "{1}", no write privileges on target."
	return Ganymede.createErrorDialog(ts.l("copyFieldTo.error_subj"),
					  ts.l("copyFieldTo.no_write", this.getName(), owner.getLabel()));
      }

    target.cryptedPass = cryptedPass;
    target.md5CryptPass = md5CryptPass;
    target.apacheMd5CryptPass = apacheMd5CryptPass;
    target.lanHash = lanHash;
    target.ntHash = ntHash;
    target.uncryptedPass = uncryptedPass;
    target.sshaHash = sshaHash;

    return null;		// simple success value
  }

  /**
   * Object value of DBField.  Used to represent value in value hashes.
   * Subclasses need to override this method in subclass.
   */

  public Object key()
  {
    throw new IllegalArgumentException("PasswordDBFields may not be tracked in namespaces");
  }

  public Object clone()
  {
    return new PasswordDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    boolean wrote_hash = false;

    /* -- */

    // at 2.1 we write out all hashes all the time, and the
    // plaintext if we are told to, or if we don't have any
    // hashed form of it to use

    if (getFieldDef().isCrypted())
      {
	cryptedPass = getUNIXCryptText();

	if (cryptedPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(cryptedPass);
	    wrote_hash = true;
	  }
      }
    else
      {
	out.writeUTF("");
      }

    if (getFieldDef().isMD5Crypted())
      {
	md5CryptPass = getMD5CryptText();

	if (md5CryptPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(md5CryptPass);
	    wrote_hash = true;
	  }
      }
    else
      {
	out.writeUTF("");
      }

    if (getFieldDef().isApacheMD5Crypted())
      {
	apacheMd5CryptPass = getApacheMD5CryptText();

	if (apacheMd5CryptPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(apacheMd5CryptPass);
	    wrote_hash = true;
	  }
      }
    else
      {
	out.writeUTF("");
      }

    if (getFieldDef().isWinHashed())
      {
	lanHash = getLANMANCryptText();

	if (lanHash == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(lanHash);
	    wrote_hash = true;
	  }

	ntHash = getNTUNICODECryptText();

	if (ntHash == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(ntHash);
	    wrote_hash = true;
	  }
      } 
    else
      {
	out.writeUTF("");
	out.writeUTF("");
      }

    if (getFieldDef().isSSHAHashed())
      {
	sshaHash = getSSHAHashText();

	if (sshaHash == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(sshaHash);
	    wrote_hash = true;
	  }
      }
    else
      {
	out.writeUTF("");
      }

    // at file version 2.1, we write out plaintext if the field
    // definition requires it, or if we were not able to write
    // out any crypttext

    if (getFieldDef().isPlainText() || !wrote_hash)
      {
	if (uncryptedPass == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(uncryptedPass);
	  }
      }
    else
      {
	out.writeUTF("");
      }
  }

  void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    // we radically simplified PasswordDBField's on-disk format at
    // file version 2.1

    if (Ganymede.db.isAtLeast(2,1))
      {
	cryptedPass = in.readUTF();

	if (cryptedPass.equals(""))
	  {
	    cryptedPass = null;
	  }

	md5CryptPass = in.readUTF();

	if (md5CryptPass.equals(""))
	  {
	    md5CryptPass = null;
	  }

	// at file format 2.4 we added the Apache-hashed password format

	if (Ganymede.db.isAtLeast(2,4))
	  {
	    apacheMd5CryptPass = in.readUTF();
	    
	    if (apacheMd5CryptPass.equals(""))
	      {
		apacheMd5CryptPass = null;
	      }
	  }

	lanHash = in.readUTF();

	if (lanHash.equals(""))
	  {
	    lanHash = null;
	  }

	ntHash = in.readUTF();

	if (ntHash.equals(""))
	  {
	    ntHash = null;
	  }

	// we added SSHA Hash at file format 2.5

	if (Ganymede.db.isAtLeast(2,5))
	  {
	    sshaHash = in.readUTF();
	    
	    if (sshaHash.equals(""))
	      {
		sshaHash = null;
	      }
	  }

	uncryptedPass = in.readUTF();

	if (uncryptedPass.equals(""))
	  {
	    uncryptedPass = null;
	  }

	return;
      }

    // From here on down we do things the old, hard way

    // at file format 1.10, we were keeping both crypted and unecrypted
    // passwords on disk.  Since then, we have decided to only write
    // out encrypted passwords if we are using them.

    if (Ganymede.db.isAtRev(1,10))
      {
	cryptedPass = in.readUTF();

	if (cryptedPass.equals(""))
	  {
	    cryptedPass = null;
	  }
	
	uncryptedPass = in.readUTF();

	if (uncryptedPass.equals(""))
	  {
	    uncryptedPass = null;
	  }

	return;
      }

    // if we're not looking at file version 1.10, the crypted password is
    // the first thing we'll see, if the field definition specifies the
    // use of it

    if (definition.isCrypted())
      {
	cryptedPass = in.readUTF();

	if (cryptedPass.equals(""))
	  {
	    cryptedPass = null;
	  }

	if (Ganymede.db.isBetweenRevs(1,13,1,16))
	  {
	    in.readUTF();	// skip old-style (buggy) md5 pass
	  }
      }

    // now we see if we expect to see an MD5Crypt()'ed  password

    // note that even though we test for >= 1.16, we won't get to this point
    // if we are using the >= 2.1 logic
	
    if (Ganymede.db.isAtLeast(1,16))
      {
	if (definition.isMD5Crypted())
	  {
	    md5CryptPass = in.readUTF();
		
	    if (md5CryptPass.equals(""))
	      {
		md5CryptPass = null;
	      }
	  }
	else
	  {
	    md5CryptPass = null;
	  }
      }

    if (definition.isCrypted() || definition.isMD5Crypted())
      {
	uncryptedPass = null;
      }
    else
      {
	uncryptedPass = in.readUTF();
	
	if (uncryptedPass.equals(""))
	  {
	    uncryptedPass = null;
	  }
	
	cryptedPass = null;
	md5CryptPass = null;
      }
  }

  /**
   * This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().
   */

  void emitXML(XMLDumpContext dump) throws IOException
  {
    this.emitXML(dump, true);
  }

  /**
   * This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().
   */

  synchronized void emitXML(XMLDumpContext dump, boolean writeSurroundContext) throws IOException
  {
    if (writeSurroundContext)
      {
	dump.indent();
	dump.startElement(this.getXMLName());
      }

    dump.startElement("password");
    
    if (uncryptedPass != null && 
	(dump.doDumpPlaintext() || (md5CryptPass == null && cryptedPass == null)))
      {
	dump.attribute("plaintext", uncryptedPass);
      }

    if (cryptedPass != null)
      {
	dump.attribute("crypt", cryptedPass);
      }
	
    if (md5CryptPass != null)
      {
	dump.attribute("md5crypt", md5CryptPass);
      }

    if (apacheMd5CryptPass != null)
      {
	dump.attribute("apachemd5crypt", apacheMd5CryptPass);
      }

    if (lanHash != null)
      {
	dump.attribute("lanman", lanHash);
      }

    if (ntHash != null)
      {
	dump.attribute("ntmd4", ntHash);
      }

    if (sshaHash != null)
      {
	dump.attribute("ssha", sshaHash);
      }

    dump.endElement("password");

    if (writeSurroundContext)
      {
	dump.endElement(this.getXMLName());
      }
  }

  /**
   * Standard {@link arlut.csd.ganymede.rmi.db_field db_field} method
   * to retrieve the value of this field.  Because we are holding sensitive
   * password information, this method always returns null.. we don't want
   * to make password values available to the client under any circumstances.
   */

  public Object getValue()
  {
    return null;
  }

  /** 
   * Returns an Object carrying the value held in this field.
   *
   * This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.
   *
   * Note that this method will always return null, as you need to use
   * the special Password-specific value accessors to get access to the
   * password information in crypted or non-crypted form.
   */

  public Object getValueLocal()
  {
    return null;
  }

  // ****
  //
  // type specific value accessors
  //
  // ****

  /**
   * This method returns a text encoded value for this DBField
   * without checking permissions.
   *
   * This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the 
   * {@link arlut.csd.ganymede.server.DBObject#getLabel() getLabel()}
   * logic for {@link arlut.csd.ganymede.server.DBObject DBObject}, 
   * which is invoked from {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.server.GanymedeSession#getPerm(arlut.csd.ganymede.server.DBObject) getPerm()} 
   * method.
   *
   * If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.
   */

  public synchronized String getValueString()
  {
    if (this.isDefined())
      {
	StringBuffer result = new StringBuffer();

	result.append("< ");

	if (cryptedPass != null)
	  {
	    result.append("crypt ");
	  }

	if (md5CryptPass != null)
	  {
	    result.append("md5crypt ");
	  }
	
	if (apacheMd5CryptPass != null)
	  {
	    result.append("apachemd5crypt ");
	  }
	
	if (lanHash != null)
	  {
	    result.append("lanman ");
	  }

	if (ntHash != null)
	  {
	    result.append("ntmd4 ");
	  }

	if (sshaHash != null)
	  {
	    result.append("ssha ");
	  }

	if (uncryptedPass != null)
	  {
	    result.append("text ");
	  }

	result.append(">");

	return result.toString();
      }
    else
      {
	return null;
      }
  }

  /**
   * The default getValueString() encoding is acceptable.
   */

  public String getEncodingString()
  {
    return getValueString();
  }

  /**
   * Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.
   *
   * If there is no change in the field, null will be returned.
   */

  public String getDiffString(DBField orig)
  {
    PasswordDBField origP;

    /* -- */

    if (!(orig instanceof PasswordDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origP = (PasswordDBField) orig;

    if (!this.equals(origP))
      {
	// "\tPassword changed\n"
	return ts.l("getDiffString.changed");
      }
    else
      {
	return null;
      }
  }

  // ****
  //
  // pass_field methods 
  //
  // ****

  /**
   * Returns the maximum acceptable string length
   * for this field.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public int maxSize()
  {
    return getFieldDef().getMaxLength();
  }

  /**
   * Returns the minimum acceptable string length
   * for this field.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public int minSize()
  {
    return getFieldDef().getMinLength();
  }

  /**
   * Returns a string containing the list of acceptable characters.
   * If the string is null, it should be interpreted as meaning all
   * characters not listed in disallowedChars() are allowable by
   * default.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public String allowedChars()
  {
    return getFieldDef().getOKChars();
  }

  /**
   * Returns a string containing the list of forbidden
   * characters for this field.  If the string is null,
   * it should be interpreted as meaning that no characters
   * are specifically disallowed.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public String disallowedChars()
  {
    return getFieldDef().getBadChars();
  }

  /**
   * Convenience method to identify if a particular
   * character is acceptable in this field.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public boolean allowed(char c)
  {
    if (allowedChars() != null && (allowedChars().indexOf(c) == -1))
      {
	return false;
      }

    if (disallowedChars() != null && (disallowedChars().indexOf(c) != -1))
      {
	return false;
      }
    
    return true;
  }

  /**
   * Returns true if the password stored in this field is hash-crypted.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public boolean crypted()
  {
    return (getFieldDef().isCrypted());
  }

  /**
   * This method is used for authenticating a provided plaintext
   * password against the stored contents of this password field.  The
   * password field may have stored the password in plaintext, or in
   * any of a variety of cryptographic hash formats.  matchPlainText()
   * will perform whatever operation on the provided plaintext as is
   * required to determine whether or not it matches with the stored
   * password data.
   *
   * @return true if the given plaintext matches the stored password
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public synchronized boolean matchPlainText(String plaintext)
  {
    boolean success = false;

    /* -- */
    
    if (plaintext == null || !this.isDefined())
      {
	return false;
      }

    // test against our hashes in decreasing order of hashing fidelity

    if (uncryptedPass != null)
      {
	success = uncryptedPass.equals(plaintext); // most accurate
      }
    else if (sshaHash != null)	// 2^64 bits, but fast
      {
	success = SSHA.matchSHAHash(sshaHash, plaintext);
      }
    else if (md5CryptPass != null) // indefinite
      {
	success = md5CryptPass.equals(MD5Crypt.crypt(plaintext, getMD5Salt()));
      }
    else if (apacheMd5CryptPass != null) // indefinite
      {
	success = apacheMd5CryptPass.equals(MD5Crypt.apacheCrypt(plaintext, getApacheMD5Salt()));
      }
    else if (ntHash != null)	// 2^64 bits
      {
	success = ntHash.equals(smbencrypt.NTUNICODEHash(plaintext));
      }
    else if (lanHash != null)	// 14 chars
      {
	success = lanHash.equals(smbencrypt.LANMANHash(plaintext));
      }
    else if (cryptedPass != null) // 8 chars
      {
	success = cryptedPass.equals(jcrypt.crypt(getSalt(), plaintext));
      }

    // if we matched against a stored hash that has sufficient
    // representational capacity to verify the full plaintext (at
    // least to the limits of chance collision), go ahead and take the
    // opportunity to capture the plaintext and set up any hashes that
    // we want to use but don't have initialized at this point.

    if (success && uncryptedPass == null)
      {
	int precision = getHashPrecision();

	if (precision == -1 ||
	    (precision > 0 && precision >= plaintext.length()))
	  {
	    uncryptedPass = plaintext;
	    setHashes(plaintext, false);
	  }
      }

    return success;
  }

  /**
   * This server-side only method returns the UNIX-encrypted password text.
   *
   * This method is never meant to be available remotely.
   */

  public String getUNIXCryptText()
  {
    if (cryptedPass != null)
      {
	return cryptedPass;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return jcrypt.crypt(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * This server-side only method returns the md5crypt()-encrypted
   * hashed password text.
   *
   * This method is never meant to be available remotely. 
   */

  public String getMD5CryptText()
  {
    if (md5CryptPass != null)
      {
	return md5CryptPass;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return MD5Crypt.crypt(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * This server-side only method returns the Apache md5crypt()-encrypted
   * hashed password text.
   *
   * This method is never meant to be available remotely. 
   */

  public String getApacheMD5CryptText()
  {
    if (apacheMd5CryptPass != null)
      {
	return apacheMd5CryptPass;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return MD5Crypt.apacheCrypt(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * This server-side only method returns the LANMAN-compatible
   * password hash of the password data held in this field.
   *
   * This method is never meant to be available remotely. 
   */

  public String getLANMANCryptText()
  {
    if (lanHash != null)
      {
	return lanHash;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return smbencrypt.LANMANHash(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * This server-side only method returns the Windows NT 4
   * SP3-compatible md4/Unicode password hash of the password data
   * held in this field.
   *
   * This method is never meant to be available remotely.
   */

  public String getNTUNICODECryptText()
  {
    if (ntHash != null)
      {
	return ntHash;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return smbencrypt.NTUNICODEHash(uncryptedPass);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * This server-side only method returns the Netscape SSHA (salted
   * SHA) LDAP hash of the password data held in this field.
   *
   * This method is never meant to be available remotely.
   */

  public String getSSHAHashText()
  {
    if (sshaHash != null)
      {
	return sshaHash;
      }
    else
      {
	if (uncryptedPass != null)
	  {
	    return SSHA.getLDAPSSHAHash(uncryptedPass, null);
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /**
   * This server-side only method returns the plaintext password text,
   * if available.
   */

  public String getPlainText()
  {
    return uncryptedPass;
  }

  /** 
   * Method to obtain the SALT for a stored crypted password.  If
   * the client is going to submit a pre-crypted password for
   * comparison via matchCryptText(), it must be salted by the salt
   * returned by this method.
   * 
   * If the password is not stored in crypt() form, null will be
   * returned. 
   */

  public String getSalt()
  {
    if (getFieldDef().isCrypted() && cryptedPass != null)
      {
	return cryptedPass.substring(0,2);
      }
    else
      {
	return null;
      }
  }

  /** 
   * Method to obtain the SALT for a stored OpenBSD-style
   * md5crypt()'ed password.  If the client is going to submit a
   * pre-crypted password for comparison via matchMD5CryptText(), it
   * must be salted by the salt returned by this method.
   *
   * If the password is not stored in md5crypt() form,
   * null will be returned.
   */

  public String getMD5Salt()
  {
    if (getFieldDef().isMD5Crypted() && md5CryptPass != null)
      {
	String salt = md5CryptPass;
	String magic = "$1$";

	if (salt.startsWith(magic))
	  {
	    salt = salt.substring(magic.length());
	  }
	
	/* It stops at the first '$', max 8 chars */
	
	if (salt.indexOf('$') != -1)
	  {
	    salt = salt.substring(0, salt.indexOf('$'));
	  }

	if (salt.length() > 8)
	  {
	    salt = salt.substring(0, 8);
	  }

	return salt;
      }
    else
      {
	return null;
      }
  }

  /** 
   * Method to obtain the SALT for a stored Apache-style
   * md5crypt()'ed password.  If the client is going to submit a
   * pre-crypted Apache password for comparison via
   * matchMD5CryptText(), it must be salted by the salt returned by
   * this method.
   *
   * If the password is not stored in apacheMd5crypt() form,
   * null will be returned.
   */

  public String getApacheMD5Salt()
  {
    if (getFieldDef().isApacheMD5Crypted() && apacheMd5CryptPass != null)
      {
	String salt = apacheMd5CryptPass;
	String magic = "$apr1$";

	if (salt.startsWith(magic))
	  {
	    salt = salt.substring(magic.length());
	  }
	
	/* It stops at the first '$', max 8 chars */
	
	if (salt.indexOf('$') != -1)
	  {
	    salt = salt.substring(0, salt.indexOf('$'));
	  }

	if (salt.length() > 8)
	  {
	    salt = salt.substring(0, 8);
	  }

	return salt;
      }
    else
      {
	return null;
      }
  }

  /**
   * Sets the value of this field, if a scalar.
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. 
   */

  public ReturnVal setValue(Object value, boolean local, boolean noWizards)
  {
    // "The setValue() method is not supported on the PasswordDBField."
    throw new IllegalArgumentException(ts.l("setValue.invalid_call"));
  }

  /** 
   * This method is used to set the password for this field,
   * crypting it in various ways if this password field is stored
   * crypted.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public synchronized ReturnVal setPlainTextPass(String plaintext)
  {
    return setPlainTextPass(plaintext, false, false);
  }

  /** 
   * This method is used to set the password for this field,
   * crypting it in various ways if this password field is stored
   * crypted.
   */

  public synchronized ReturnVal setPlainTextPass(String plaintext, boolean local, boolean noWizards)
  {
    ReturnVal retVal, retVal2;
    DBEditObject eObj;

    /* -- */

    retVal = verifyNewValue(plaintext);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETPASSPLAIN, plaintext, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    // we'll still retain our first retVal so that we can return
    // advisory-only messages that the wizardHook generated, even if
    // the finalizeSetValue() doesn't generate any.

    retVal2 = ((DBEditObject) owner).finalizeSetValue(this, null);

    if (retVal2 != null && !retVal2.didSucceed())
      {
	return retVal2;
      }

    // reset all hashes to start things off

    clear_stored();

    // if we've got an empty string, clear the plaintext, too

    if (plaintext == null || plaintext.equals(""))
      {
	uncryptedPass = null;

	return retVal;
      }

    // else, go ahead and set everything

    uncryptedPass = plaintext;
    setHashes(plaintext, true);

    return retVal;
  }

  /**
   * This method is used to set a pre-crypted password for this field.
   *
   * This method will return an error dialog if this field does not store
   * passwords in UNIX crypted format.
   *
   * Because the UNIX crypt() hashing is not reversible, any MD5 and plain text
   * password information stored in this field will be lost.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setCryptPass(String text)
  {
    return setCryptPass(text, false, false);
  }

  /**
   * This method is used to set a pre-crypted password for this field.
   *
   * This method will return an error dialog if this field does not store
   * passwords in UNIX crypted format.
   *
   * Because the UNIX crypt() hashing is not reversible, any MD5 and plain text
   * password information stored in this field will be lost.
   */

  public ReturnVal setCryptPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
	// "Password Field Error"
	// "Don''t have permission to edit field {0} in object {1}."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("global.perm_error_text", this.getName(), owner.getLabel()));
      }

    if (!getFieldDef().isCrypted())
      {
	// "Server: Error in PasswordDBField.setCryptTextPass()"
	// "Password field not configured to support traditional Unix crypt hashing."
	return Ganymede.createErrorDialog(ts.l("setCryptPass.error_title"),
					  ts.l("setCryptPass.error_text"));
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETPASSCRYPT, text, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the crypt password is directly set, we lose 
	// plaintext and alternate hashes

	clear_stored();

	if ((text == null) || (text.equals("")))
	  {
	    cryptedPass = null;
	  }
	else
	  {
	    cryptedPass = text;
	  }
      }

    return retVal;
  }

  /**
   * This method is used to set a pre-crypted OpenBSD-style
   * MD5Crypt password for this field.  This method will return
   * an error code if this password field is not stored crypted.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setMD5CryptedPass(String text)
  {
    return setMD5CryptedPass(text, false, false);
  }

  /**
   * This method is used to set a pre-crypted OpenBSD-style
   * MD5Crypt password for this field.  This method will return
   * an error code if this password field is not stored crypted.
   */

  public ReturnVal setMD5CryptedPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
	// "Password Field Error"
	// "Don''t have permission to edit field {0} in object {1}."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("global.perm_error_text", this.getName(), owner.getLabel()));
      }

    if (!getFieldDef().isMD5Crypted())
      {
	// "Server: Error in PasswordDBField.setMD5CryptPass()"
	// "Password field not configured to support MD5Crypt hashing."
	return Ganymede.createErrorDialog(ts.l("setMD5CryptPass.error_title"),
					  ts.l("setMD5CryptPass.error_text"));
      }

    if (text != null && !text.equals("") && (!text.startsWith("$1$") || (text.indexOf('$', 3) == -1)))
      {
	// "Password Field Error"
	// "The hash text passed to setMD5CryptPass(), "{0}", is not a well-formed MD5Crypt hash text."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("setMD5CryptPass.format_error", text));
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETPASSMD5, text, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the md5CryptPass password is directly set, we lose 
	// plaintext and alternate hashes

	clear_stored();

	if ((text == null) || (text.equals("")))
	  {
	    md5CryptPass = null;
	  }
	else
	  {
	    md5CryptPass = text;
	  }
      }

    return retVal;
  }

  /**
   * This method is used to set a pre-crypted Apache-style
   * MD5Crypt password for this field.  This method will return
   * an error code if this password field is not stored crypted.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setApacheMD5CryptedPass(String text)
  {
    return setApacheMD5CryptedPass(text, false, false);
  }

  /**
   * This method is used to set a pre-crypted Apache-style
   * MD5Crypt password for this field.  This method will return
   * an error code if this password field is not stored crypted.
   */

  public ReturnVal setApacheMD5CryptedPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
	// "Password Field Error"
	// "Don''t have permission to edit field {0} in object {1}."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("global.perm_error_text", this.getName(), owner.getLabel()));
      }

    if (!getFieldDef().isApacheMD5Crypted())
      {
	// "Server: Error in PasswordDBField.setApacheMD5CryptTextPass()"
	// "Password field not configured to support ApacheMD5Crypt hashing."
	return Ganymede.createErrorDialog(ts.l("setApacheMD5CryptPass.error_title"),
					  ts.l("setApacheMD5CryptPass.error_text"));
      }

    if (text != null && !text.equals("") && (!text.startsWith("$apr1$") || (text.indexOf('$', 6) == -1)))
      {
	// "Password Field Error"
	// "The hash text passed to setMD5CryptPass(), "{0}", is not a well-formed ApacheMD5Crypt hash text."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("setApacheMD5CryptPass.format_error", text));
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETPASSAPACHEMD5, text, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the apacheMd5CryptPass password is directly set, we lose 
	// plaintext and alternate hashes

	clear_stored();

	if ((text == null) || (text.equals("")))
	  {
	    apacheMd5CryptPass = null;
	  }
	else
	  {
	    apacheMd5CryptPass = text;
	  }
      }

    return retVal;
  }

  /**
   * This method is used to set pre-crypted Windows-style password
   * hashes for this field.  These strings are formatted as used in Samba's
   * encrypted password files.  This method will return
   * an error code if this password field is not configured to accept
   * Windows-hashed password strings.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setWinCryptedPass(String LANMAN, String NTUnicodeMD4)
  {
    return setWinCryptedPass(LANMAN, NTUnicodeMD4, false, false);
  }

  /**
   * This method is used to set pre-crypted Windows-style password
   * hashes for this field.  These strings are formatted as used in Samba's
   * encrypted password files.  This method will return
   * an error code if this password field is not configured to accept
   * Windows-hashed password strings.
   */

  public ReturnVal setWinCryptedPass(String LANMAN, String NTUnicodeMD4, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
	// "Password Field Error"
	// "Don''t have permission to edit field {0} in object {1}."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("global.perm_error_text", this.getName(), owner.getLabel()));
      }

    if (!getFieldDef().isWinHashed())
      {
	// "Server: Error in PasswordDBField.setWinCryptedPass()"
	// "Password field is not configured to accept Samba hashed password strings."
	return Ganymede.createErrorDialog(ts.l("setWinCryptedPass.error_title"),
					  ts.l("setWinCryptedPass.error_text"));
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.SETPASSWINHASHES, LANMAN, NTUnicodeMD4);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the windows hashes are set directly, we lose 
	// plaintext and alternate hashes

	clear_stored();

	if ((LANMAN == null) || (LANMAN.equals("")))
	  {
	    lanHash = null;
	  }
	else
	  {
	    lanHash = LANMAN;
	  }

	if ((NTUnicodeMD4 == null) || (NTUnicodeMD4.equals("")))
	  {
	    ntHash = null;
	  }
	else
	  {
	    ntHash = NTUnicodeMD4;
	  }
      }

    return retVal;
  }

  /**
   * This method is used to set a pre-crypted password for this field.
   *
   * This method will return an error dialog if this field does not store
   * passwords in SSHA format.
   *
   * Because the SSHA hashing is not reversible, any other password
   * information stored in this field will be lost.
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setSSHAPass(String text)
  {
    return this.setSSHAPass(text, false, false);
  }

  /**
   * This method is used to set a pre-crypted password for this field.
   *
   * This method will return an error dialog if this field does not store
   * passwords in SSHA format.
   *
   * Because the SSHA hashing is not reversible, any other password
   * information stored in this field will be lost.
   */

  public ReturnVal setSSHAPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
	// "Password Field Error"
	// "Don''t have permission to edit field {0} in object {1}."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("global.perm_error_text", this.getName(), owner.getLabel()));
      }

    if (!getFieldDef().isSSHAHashed())
      {
	// "Server: Error in PasswordDBField.setSSHAPass()"
	// "Password field is not configured to accept SSHA-1 hashed password strings."
	return Ganymede.createErrorDialog(ts.l("setSSHAPass.error_title"),
					  ts.l("setSSHAPass.error_text"));
      }

    if (!text.startsWith("{SSHA}"))
      {
	// "Server: Error in PasswordDBField.setSSHAPass()"
	// "The hash text passed to setSSHAPass(), "{0}", is not a well-formed, OpenLDAP-encoded SSHA-1 hash text."
	return Ganymede.createErrorDialog(ts.l("setSSHAPass.error_title"),
					  ts.l("setSSHAPass.format_error", this.getName()));
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETPASSSSHA, text, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the SSHA password is directly set, we lose
	// plaintext and alternate hashes

	clear_stored();

	if ((text == null) || (text.equals("")))
	  {
	    sshaHash = null;
	  }
	else
	  {
	    sshaHash = text;
	  }
      }

    return retVal;
  }

  /**
   * This method is used to force all known hashes into this password
   * field.  Ganymede does no verifications to insure that all of these
   * hashes really match the same password, so caveat emptor.  If any of
   * these hashes are null or empty string, those hashes will be cleared.
   *
   * Calling this method will clear the password's stored plaintext,
   * if any.
   *
   * If this password field is not configured to support any of the
   * various hash formats in the Ganymede schema, an error will be returned.
   */

  public ReturnVal setAllHashes(String crypt,
				String md5crypt,
				String apacheMd5Crypt,
				String LANMAN,
				String NTUnicodeMD4,
				String SSHAText,
				boolean local, 
				boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;
    boolean settingCrypt, settingMD5, settingApacheMD5, settingWin, settingSSHA;

    /* -- */

    if (!isEditable(local))
      {
	// "Password Field Error"
	// "Don''t have permission to edit field {0} in object {1}."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("global.perm_error_text", this.getName(), owner.getLabel()));
      }

    settingCrypt = (crypt != null && !crypt.equals(""));
    settingMD5 = (md5crypt != null && !md5crypt.equals(""));
    settingApacheMD5 = (apacheMd5Crypt != null && !apacheMd5Crypt.equals(""));
    settingWin = (LANMAN != null && !LANMAN.equals("")) || (NTUnicodeMD4 != null && !NTUnicodeMD4.equals(""));
    settingSSHA = (SSHAText != null && !SSHAText.equals(""));

    if (!settingCrypt && !settingWin && !settingMD5 && !settingApacheMD5 && !settingSSHA)
      {
	// clear it!

	return setPlainTextPass(null);
      }

    // nope, we're setting something.. let's find out what

    if (settingSSHA)
      {
	if (!getFieldDef().isSSHAHashed())
	  {
	    // "Server: Error in PasswordDBField.setAllHashes()"
	    // "Password field not configured to accept SSHA-1 hashed password strings."
	    return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
					      ts.l("setSSHAPass.error_text"));
	  }

	if (!SSHAText.startsWith("{SSHA}"))
	  {
	    // "Server: Error in PasswordDBField.setAllHashes()"
	    // "The SSHA hash text passed to setAllHashes() is not a well-formed, OpenLDAP-encoded SSHA-1 hash text."
	    return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
					      ts.l("setAllHashes.ssha_format_error"));
	  }
      }

    if (settingWin && !getFieldDef().isWinHashed())
      {
	// "Server: Error in PasswordDBField.setAllHashes()"
	// "Password field not configured to accept Samba hashed password strings."
	return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
					  ts.l("setWinCryptedPass.error_text"));
      }

    if (settingMD5)
      {
	if (!getFieldDef().isMD5Crypted())
	  {
	    // "Server: Error in PasswordDBField.setAllHashes()"
	    // "Password field not configured to support MD5Crypt hashing."
	    return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
					      ts.l("setMD5CryptPass.error_text"));
	  }

	if (!md5crypt.startsWith("$1$") || (md5crypt.indexOf('$', 3) == -1))
	  {
	    // "Server: Error in PasswordDBField.setAllHashes()"
	    // "The MD5Crypt hash text passed to setAllHashes(), "{0}", is not well-formed."
	    return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
					      ts.l("setAllHashes.md5_format_error", md5crypt));
	  }
      }

    if (settingApacheMD5)
      {
	if (!getFieldDef().isApacheMD5Crypted())
	  {
	    // "Server: Error in PasswordDBField.setAllHashes()"
	    // "Password field not configured to support ApacheMD5Crypt hashing."
	    return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
					      ts.l("setApacheMD5CryptPass.error_text"));

	  }

	if (!apacheMd5Crypt.startsWith("$apr1$") || (md5crypt.indexOf('$', 6) == -1))
	  {
	    // "Server: Error in PasswordDBField.setAllHashes()"
	    // "The Apache MD5Crypt hash text passed to setAllHashes(), "{0}", is not well-formed."
	    return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
					      ts.l("setAllHashes.apache_format_error", apacheMd5Crypt));
	  }
      }

    if (settingCrypt && !getFieldDef().isCrypted())
      {
	// "Server: Error in PasswordDBField.setAllHashes()"
	// "Password field not configured to support traditional Unix crypt hashing."
	return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
					  ts.l("setCryptPass.error_text"));
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	if (settingWin)
	  {
	    retVal = eObj.wizardHook(this, DBEditObject.SETPASSWINHASHES, LANMAN, NTUnicodeMD4);

	    // if a wizard intercedes, we are going to let it take the ball.
	    
	    if (retVal != null && !retVal.doNormalProcessing)
	      {
		return retVal;
	      }
	  }

	if (settingMD5)
	  {
	    retVal = eObj.wizardHook(this, DBEditObject.SETPASSMD5, md5crypt, null);

	    // if a wizard intercedes, we are going to let it take the ball.
	    
	    if (retVal != null && !retVal.doNormalProcessing)
	      {
		return retVal;
	      }
	  }

	if (settingApacheMD5)
	  {
	    retVal = eObj.wizardHook(this, DBEditObject.SETPASSAPACHEMD5, apacheMd5Crypt, null);

	    // if a wizard intercedes, we are going to let it take the ball.
	    
	    if (retVal != null && !retVal.doNormalProcessing)
	      {
		return retVal;
	      }
	  }

	if (settingCrypt)
	  {
	    retVal = eObj.wizardHook(this, DBEditObject.SETPASSCRYPT, crypt, null);

	    // if a wizard intercedes, we are going to let it take the ball.
	    
	    if (retVal != null && !retVal.doNormalProcessing)
	      {
		return retVal;
	      }
	  }

	if (settingSSHA)
	  {
	    retVal = eObj.wizardHook(this, DBEditObject.SETPASSSSHA, sshaHash, null);

	    // if a wizard intercedes, we are going to let it take the ball.
	    
	    if (retVal != null && !retVal.doNormalProcessing)
	      {
		return retVal;
	      }
	  }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ((DBEditObject)owner).finalizeSetValue(this, null);

    if (retVal == null || retVal.didSucceed())
      {
	// whenever the hashes are set directly, we lose 
	// plaintext and alternate hashes

	clear_stored();

	if ((LANMAN == null) || (LANMAN.equals("")))
	  {
	    lanHash = null;
	  }
	else
	  {
	    lanHash = LANMAN;
	  }

	if ((NTUnicodeMD4 == null) || (NTUnicodeMD4.equals("")))
	  {
	    ntHash = null;
	  }
	else
	  {
	    ntHash = NTUnicodeMD4;
	  }

	if (settingCrypt)
	  {
	    cryptedPass = crypt;
	  }

	if (settingMD5)
	  {
	    md5CryptPass = md5crypt;
	  }

	if (settingApacheMD5)
	  {
	    apacheMd5CryptPass = apacheMd5Crypt;
	  }

	if (settingSSHA)
	  {
	    sshaHash = SSHAText;
	  }
      }

    return retVal;    
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof String));
  }

  /**
   * Generally only for when we get a plaintext submission..
   */

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;
    String s;
    
    /* -- */

    if (!isEditable(true))
      {
	// "Password Field Error"
	// "Don''t have permission to edit field {0} in object {1}."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("global.perm_error_text", this.getName(), owner.getLabel()));
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	// "Password Field Error"
	// "Submitted value "{0}" is not a String object!  Major client error while trying to edit password field."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("verifyNewValue.type_error", o));
      }

    if (o == null)
      {
	return null; // assume we can null out this field
      }

    s = (String) o;

    if (s.length() > maxSize())
      {
	// string too long

	// "Password Field Error"
	// "The submitted password is too long.  The maximum plaintext password length accepted is {0,number,#} characters."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("verifyNewValue.too_long", new Integer(this.maxSize())));
      }

    if (s.length() < minSize())
      {
	// "Password Field Error"
	// "The submitted password is too short.  The minimum plaintext password length accepted is {0,number,#} characters."
	return Ganymede.createErrorDialog(ts.l("global.error_subj"),
					  ts.l("verifyNewValue.too_short", new Integer(this.minSize())));
      }
    
    if (allowedChars() != null)
      {
	String okChars = allowedChars();
	
	for (int i = 0; i < s.length(); i++)
	  {
	    if (okChars.indexOf(s.charAt(i)) == -1)
	      {
		// "Password Field Error"
		// "Submitted password contains an unacceptable character ('{0}')."
		return Ganymede.createErrorDialog(ts.l("global.error_subj"),
						  ts.l("verifyNewValue.bad_char",
						       new Character(s.charAt(i))));
	      }
	  }
      }
    
    if (disallowedChars() != null)
      {
	String badChars = disallowedChars();
	
	for (int i = 0; i < s.length(); i++)
	  {
	    if (badChars.indexOf(s.charAt(i)) != -1)
	      {
		// "Password Field Error"
		// "Submitted password contains an unacceptable character ('{0}')."
		return Ganymede.createErrorDialog(ts.l("global.error_subj"),
						  ts.l("verifyNewValue.bad_char",
						       new Character(s.charAt(i))));
	      }
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, s);
  }

  /**
   * This method returns an int indicating to what precision the
   * password in this PasswordDBField is known.  Certain cryptographic
   * hashes have limits on how many characters of the input text are
   * taken into account in the hash.
   *
   * This method returns -1 if the password is known with no limits
   * on its precision (plaintext, or md5crypt, or ssha which is
   * precise to 2^64 bits.. close enough), 0 if the password is not
   * know, or a positive integer indicating the number of characters
   * of precision that we believe we can recognize from our hash
   * authenticators.
   */

  private int getHashPrecision()
  {
    if (uncryptedPass != null || md5CryptPass != null ||
	apacheMd5CryptPass != null || sshaHash != null || ntHash != null)
      {
	return -1;		// full precision
      }

    if (lanHash != null)
      {
	return 14;		// Old-school Windows hashes are good
				// for 14 chars
      }

    if (cryptedPass != null)
      {
	return 8;		// Old-school UNIX sux0rs.. we should
				// only be using this for importing
				// users from old /etc/passwd-style
				// files
      }

    return 0;			// i got nothing, boss
  }

  /**
   * This method does the work of storing the given plaintext into
   * whatever hashes we are configured to retain.  If forceChange is
   * true, this calculation and storage is non-optional.  If
   * forceChange is false, we will only store the plaintext into a
   * hash if we are configured to use it but for some reason do not
   * have a hash value of that kind stored.
   */

  private void setHashes(String plaintext, boolean forceChange)
  {
    if (getFieldDef().isCrypted() && (forceChange || cryptedPass == null))
      {
	cryptedPass = jcrypt.crypt(plaintext);
      }

    if (getFieldDef().isMD5Crypted() && (forceChange || md5CryptPass == null))
      {
	md5CryptPass = MD5Crypt.crypt(plaintext);
      }

    if (getFieldDef().isApacheMD5Crypted() && (forceChange || apacheMd5CryptPass == null))
      {
	apacheMd5CryptPass = MD5Crypt.apacheCrypt(plaintext);
      }

    if (getFieldDef().isWinHashed())
      {
	if (forceChange || lanHash == null)
	  {
	    lanHash = smbencrypt.LANMANHash(plaintext);
	  }

	if (forceChange || ntHash == null)
	  {
	    ntHash = smbencrypt.NTUNICODEHash(plaintext);
	  }
      }

    if (getFieldDef().isSSHAHashed() && (forceChange || sshaHash == null))
      {
	sshaHash = SSHA.getLDAPSSHAHash(plaintext, null);
      }
  }
}
