/*
   GASH 2

   PasswordDBField.java

   The GANYMEDE object storage system.

   Created: 21 July 1997

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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.solinger.cracklib.CrackLib;

import org.mindrot.BCrypt;

import arlut.csd.Util.TranslationService;

import arlut.csd.crypto.MD5Crypt;
import arlut.csd.crypto.Sha256Crypt;
import arlut.csd.crypto.Sha512Crypt;
import arlut.csd.crypto.SSHA;
import arlut.csd.crypto.jcrypt;
import arlut.csd.crypto.smbencrypt;

import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.pass_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PasswordDBField

------------------------------------------------------------------------------*/

/**
 * <p>PasswordDBField is a subclass of {@link
 * arlut.csd.ganymede.server.DBField DBField} for the storage and
 * handling of password fields in the {@link
 * arlut.csd.ganymede.server.DBStore DBStore} on the Ganymede server.</p>
 *
 * <p>The Ganymede client talks to PasswordDBFields through the {@link
 * arlut.csd.ganymede.rmi.pass_field pass_field} RMI interface.</p>
 *
 * <p>This class differs a bit from most subclasses of {@link
 * arlut.csd.ganymede.server.DBField DBField} in that the normal
 * setValue()/getValue() methods are non-functional.  Instead, there
 * are special methods used to set or access password information in
 * hashed and non-hashed forms.</p>
 *
 * <p>PasswordDBField supports a significant variety of password hash
 * formats, to allow Ganymede to (optionally) avoid storing passwords
 * in plain text, while still retaining the ability to emit password
 * information to a variety of information systems.</p>
 *
 * <p>Here are the hash algorithms supported by PasswordDBField:</p>
 *
 * <ul>
 * <li>Traditional DES-based Unix Crypt()</li>
 * <li>OpenBSD-style md5Crypt ($1$ prefix)</li>
 * <li>OpenBSD-style md5Crypt, as modified for use with Apache ($apr1$ prefix)</li>
 * <li>OpenBSD-style BCrypt ($2a$ prefix)</li>
 * <li>Traditional LAN Manager hash</li>
 * <li>Windows NT Unicode Hash algorithm</li>
 * <li>SSHA, Salted SHA-1 hash, as used in OpenLDAP</li>
 * <li>SHA Crypt, SHA256 and SHA512 based scalable hash
 * algorithm, supported in Linux starting with glibc version 2.7. ($5$ and $6$ prefixes)</li>
 * </ul>
 *
 * <p>There are no methods provided to allow remote access to password
 * information..  server-side code must locally call methods to get
 * access to stored password information.  Even in that case, only
 * hashed password information will generally be available, though the
 * schema can be configured to have password fields maintain plaintext
 * (necessary for sync'ing to Kerberos based systems like Active
 * Directory).</p>
 *
 * <p> If this password field is configured to store only hashed
 * passwords by way of its {@link
 * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField},
 * this password field will never emit() the plaintext to disk.</p>
 *
 * <p>In such cases, only the hash text password information will be
 * retained on disk for user authentication.  The plaintext of the
 * password can be retained in memory for the duration of a run of the
 * Ganymede server process, but as the plaintext is not stored in the
 * Ganymede server's ganymede.db file, the plaintext is lost when the
 * server is stopped.</p>
 *
 * <p>This transient retention of plaintext password information can
 * still be useful in the context of the Ganymede 2.0 {@link
 * arlut.csd.ganymede.server.SyncRunner Sync Channel} mechanism,
 * however.  If a user changes his password, a Sync Channel can be
 * configured to write the plaintext password change information out,
 * even though the server is prone to forget the plaintext if it is
 * stopped and restarted.</p>
 *
 * <p>At ARL, we use this transient plaintext retention to allow us to
 * synchronize passwords to Active Directory without having the risk
 * of long term plaintext storage in the ganymede.db file.</p>
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
   * <p>The complex md5crypt()'ed password, as in OpenBSD, FreeBSD,
   * Linux PAM, etc.  Good for validating indefinite length
   * strings.</p>
   *
   * <p>Algorithm by Paul-Hennig Kamp.  Uses a $1$ prefix.</p>
   */

  private String md5CryptPass;

  /**
   * <p>The complex bCrypted password, as in OpenBSD. Very high,
   * scalable security.</p>
   *
   * <p>This hash format can have a very large digest size, a large
   * salt, and a very significant computational cost, which makes this
   * hash (along with Ulrich Drepper's SHA-CRYPT) one of the
   * most resistant to brute force attacks.</p>
   *
   * <p>Algorithm by Neils Provos and the Blowfish creators.  Uses a
   * $2a$ prefix.</p>
   */

  private String bCryptPass;

  /**
   * <p>The complex md5crypt()'ed password, with the magic string used
   * by Apache for their htpasswd file format.  Good for validating
   * indefinite length strings.</p>
   *
   * <p>Variant of Paul-Hennig Kamps's algorithm.  Uses a $apr1$
   * prefix.</p>
   */

  private String apacheMd5CryptPass;

  /**
   * <p>Plaintext password.. will never be saved to disk if we have
   * another hash format available to validate, unless this field has
   * been specifically configured to always save plaintext to disk in
   * the schema editor.  See {@link
   * arlut.csd.ganymede.server.DBObjectBaseField#isPlainText()} for more
   * detail.</p>
   */

  private String uncryptedPass;

  /**
   * <p>Samba LANMAN hash, for Win95 clients.  Only good for
   * validating the first 14 characters of a plaintext.  This hash is
   * actually incredibly, mind-crushingly weak.. weaker than
   * traditional Unix crypt, even.  If you're basing your password
   * security on this hash still, you're in trouble.</p>
   */

  private String lanHash;

  /**
   * <p>Samba md4 Unicode hash, for WinNT/2k clients. Good for
   * validating up to 2^64 bits of plaintext.. effectively indefinite
   * in extent</p>
   */

  private String ntHash;

  /**
   * <p>Salted SHA-1 hash, for OpenLDAP.  Good for validating up to 2^64
   * bits of plaintext.. effectively indefinite in extent.  A very
   * strong hash format in terms of the difficulty of finding
   * collisions in the hash range, but it's very quick to evaluate, so
   * a dictionary attack against this hash can proceed rapidly.</p>
   *
   * <p>Note that we keep the sshaHash string here in the same form
   * as would be used in an LDAP store.</p>
   *
   * <p>This is Netscape's salted variant of the FIPS SHA-1 standard.
   * SHA-1 is described at <a href="http://en.wikipedia.org/wiki/SHA-1">http://en.wikipedia.org/wiki/SHA-1</a>,
   * while SSHA is described at
   * <a href="http://www.openldap.org/faq/data/cache/347.html">http://www.openldap.org/faq/data/cache/347.html</a>.</p>
   */

  private String sshaHash;

  /**
   * <p>Password hashed using the SHA Unix Crypt algorithm published
   * by Ulrich Drepper at</p>
   *
   * <p><a href="http://people.redhat.com/drepper/sha-crypt.html">http://people.redhat.com/drepper/sha-crypt.html</a></p>
   *
   * <p>The hash text in shaUnixCrypt can be generated using either the
   * Sha256Crypt or Sha512Crypt variants of the SHA Unix Crypt
   * algorithm described at the above URL.</p>
   *
   * <p>This hash format can have a very large digest size, a large
   * salt, and a very significant computational cost, which makes this
   * hash (along with Niels Provos's bCrypt) one of the most resistant
   * to brute force attacks.</p>
   */

  private String shaUnixCrypt;

  /**
   * <p>History archive of previous password hashes and the dates that
   * the previous passwords were committed into the database.</p>
   *
   * <p>This variable will only be non-null if the DBObjectBaseField
   * definition for this field has history_check set.</p>
   */

  private passwordHistoryArchive history = null;

  /* -- */

  /**
   * Receive constructor.  Used to create a PasswordDBField from a DBStore/DBJournal
   * DataInput stream.
   */

  PasswordDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    super(owner, definition.getID());

    this.value = null;
    receive(in, definition);
  }

  /**
   * <p>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} definition
   * indicates that a given field may be present, but for which no
   * value has been stored in the {@link arlut.csd.ganymede.server.DBStore
   * DBStore}.</p>
   *
   * <p>Used to provide the client a template for 'creating' this
   * field if so desired.</p>
   */

  PasswordDBField(DBObject owner, DBObjectBaseField definition)
  {
    super(owner, definition.getID());

    if (definition.isHistoryChecked())
      {
        this.history = new passwordHistoryArchive(definition.getHistoryDepth());
      }

    value = null;
  }

  /**
   * <p>Copy constructor, used when checking edit objects in and out.</p>
   *
   * <p>As a field copy constructor, this method must not throw an
   * exception, or else commits can be seriously broken.</p>
   */

  public PasswordDBField(DBObject owner, PasswordDBField field)
  {
    super(owner, field.getID());

    cryptedPass = field.cryptedPass;
    md5CryptPass = field.md5CryptPass;
    apacheMd5CryptPass = field.apacheMd5CryptPass;
    uncryptedPass = field.uncryptedPass;
    lanHash = field.lanHash;
    ntHash = field.ntHash;
    sshaHash = field.sshaHash;
    shaUnixCrypt = field.shaUnixCrypt;
    bCryptPass = field.bCryptPass;
    history = field.history;

    try
      {
        // If we're keeping history and we're copying from an editable
        // object to a non-editable object and the field we're copying
        // from changed during the transaction we're consolidating, we
        // need to remember the password that is being set with this
        // commit.

        if (getFieldDef().isHistoryChecked())
          {
            if (history == null)
              {
                history = new passwordHistoryArchive(getFieldDef().getHistoryDepth());
              }
            else if (history.getPoolSize() != getFieldDef().getHistoryDepth())
              {
                history.setPoolSize(getFieldDef().getHistoryDepth());
              }

            if (this.uncryptedPass != null &&
                !(this.owner instanceof DBEditObject) &&
                field.hasChanged())
              {
                history.add(uncryptedPass, new Date());
              }
          }
        else
          {
            history = null;
          }
      }
    catch (Throwable ex)
      {
        // we're *not* going to allow an exception to be thrown here
        // for the sake of the password history tracking

        ex.printStackTrace();
      }
  }

  /**
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  @Override public boolean isDefined()
  {
    return (cryptedPass != null || md5CryptPass != null ||
            apacheMd5CryptPass != null || uncryptedPass != null || lanHash != null
            || ntHash != null || bCryptPass != null | sshaHash != null || shaUnixCrypt != null);
  }

  /**
   * <p>This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of {@link
   * arlut.csd.ganymede.server.DBField DBField} may implement this in
   * different ways, if simply setting the field's value member to
   * null is not appropriate.  Any namespace values claimed by the
   * field will be released, and when the transaction is committed,
   * this field will be released.</p>
   *
   * <p>Note that this method is really only intended for those fields
   * which have some significant internal structure to them, such as
   * permission matrix, field option matrix, and password fields.</p>
   *
   * <p>NOTE: There is, at present, no defined DBEditObject callback
   * method that tracks generic field nullification.  This means that
   * if your code uses setUndefined on a PermissionMatrixDBField,
   * FieldOptionDBField, or PasswordDBField, the plugin code is not
   * currently given the opportunity to review and refuse that
   * operation.  Caveat Coder.</p>
   */

  @Override public synchronized ReturnVal setUndefined(boolean local)
  {
    if (!isEditable(local))
      {
        // "Permissions Error"
        // "You do not have permission to clear the "{0}" password field in object "{1}"."
        return Ganymede.createErrorDialog(ts.l("setUndefined.perm_error_subj"),
                                          ts.l("setUndefined.perm_error_text", this.getName(),
                                               this.owner.getLabel()));
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
    bCryptPass = null;
    uncryptedPass = null;
    ntHash = null;
    lanHash = null;
    sshaHash = null;
    shaUnixCrypt = null;

    // NB: Don't clear history.. this password field might be set to a
    // defined value later on and we don't want to forget previous
    // values
  }

  /**
   * private helper to clear stored unnecessary stored password
   * information in this field
   */

  private synchronized final void clear_unused_stored()
  {
    if (!getFieldDef().isCrypted())
      {
        cryptedPass = null;
      }

    if (!getFieldDef().isMD5Crypted())
      {
        md5CryptPass = null;
      }

    if (!getFieldDef().isApacheMD5Crypted())
      {
        apacheMd5CryptPass = null;
      }

    if (!getFieldDef().isWinHashed())
      {
        lanHash = null;
        ntHash = null;
      }

    if (!getFieldDef().isBCrypted())
      {
        bCryptPass = null;
      }

    if (!getFieldDef().isSSHAHashed())
      {
        sshaHash = null;
      }

    if (!getFieldDef().isShaUnixCrypted())
      {
        shaUnixCrypt = null;    // force new hash
      }
  }

  /**
   * We don't expect these fields to ever be stored in a hash.
   */

  @Override public int hashCode()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * <p>Returns true if obj is a field with the same value(s) as
   * this one.</p>
   *
   * <p>This method is ok to be synchronized because it does not
   * call synchronized methods on any other object.</p>
   */

  @Override public synchronized boolean equals(Object obj)
  {
    if (obj == null)
      {
        return false;
      }

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
            streq(bCryptPass, origP.bCryptPass) &&
            streq(sshaHash, origP.sshaHash) &&
            streq(shaUnixCrypt, origP.shaUnixCrypt));
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
   * <p>This method copies the current value of this DBField
   * to target.  The target DBField must be contained within a
   * checked-out DBEditObject in order to be updated.  Any actions
   * that would normally occur from a user manually setting a value
   * into the field will occur.</p>
   *
   * <p>NOTE: this method is mainly used in cloning objects, and
   * {@link
   * arlut.csd.ganymede.server.DBEditObject#cloneFromObject(arlut.csd.ganymede.server.DBSession,
   * arlut.csd.ganymede.server.DBObject, boolean) cloneFromObject}
   * doesn't allow cloning of password fields by default.</p>
   *
   * @param target The DBField to copy this field's contents to.
   * @param local If true, permissions checking is skipped.
   */

  @Override public synchronized ReturnVal copyFieldTo(DBField target, boolean local)
  {
    PasswordDBField targetField = (PasswordDBField) target;

    if (!local)
      {
        if (!verifyReadPermission())
          {
            // "Error Copying Password Field"
            // "Can''t copy field "{0}" in object "{1}", no read privileges on source."
            return Ganymede.createErrorDialog(ts.l("copyFieldTo.error_subj"),
                                              ts.l("copyFieldTo.no_read", this.getName(),
                                                   this.owner.getLabel()));
          }
      }

    if (!targetField.isEditable(local))
      {
        // "Error Copying Password Field"
        // "Can''t copy field "{0}" in object "{1}", no write privileges on target."
        return Ganymede.createErrorDialog(ts.l("copyFieldTo.error_subj"),
                                          ts.l("copyFieldTo.no_write", this.getName(),
                                               this.owner.getLabel()));
      }

    targetField.cryptedPass = cryptedPass;
    targetField.md5CryptPass = md5CryptPass;
    targetField.apacheMd5CryptPass = apacheMd5CryptPass;
    targetField.lanHash = lanHash;
    targetField.ntHash = ntHash;
    targetField.uncryptedPass = uncryptedPass;
    targetField.bCryptPass = bCryptPass;
    targetField.sshaHash = sshaHash;
    targetField.shaUnixCrypt = shaUnixCrypt;

    targetField.history = history;

    return null;                // simple success value
  }

  /**
   * Object value of DBField.  Used to represent value in value hashes.
   * Subclasses need to override this method in subclass.
   */

  @Override public Object key()
  {
    throw new IllegalArgumentException("PasswordDBFields may not be tracked in namespaces");
  }

  @Override public Object clone() throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }

  /**
   * <p>This method is responsible for writing out the contents of
   * this field to an binary output stream.  It is used in writing
   * fields to the ganymede.db file and to the journal file.</p>
   *
   * <p>This method only writes out the value contents of this field.
   * The {@link arlut.csd.ganymede.server.DBObject DBObject}
   * {@link arlut.csd.ganymede.server.DBObject#emit(java.io.DataOutput) emit()}
   * method is responsible for writing out the field identifier information
   * ahead of the field's contents.</p>
   */

  @Override void emit(DataOutput out) throws IOException
  {
    boolean need_to_write_all_hashes = writeOutAllStoredValues();
    boolean wrote_hash = false;

    /* -- */

    // at 2.1 we write out all hashes all the time, and the
    // plaintext if we are told to, or if we don't have any
    // hashed form of it to use

    if (getFieldDef().isCrypted() || (cryptedPass != null && need_to_write_all_hashes))
      {
        cryptedPass = getUNIXCryptText();
        wrote_hash = emitHelper(out, cryptedPass, wrote_hash);
      }
    else
      {
        out.writeUTF("");
      }

    if (getFieldDef().isMD5Crypted() || (md5CryptPass != null && need_to_write_all_hashes))
      {
        md5CryptPass = getMD5CryptText();
        wrote_hash = emitHelper(out, md5CryptPass, wrote_hash);
      }
    else
      {
        out.writeUTF("");
      }

    if (getFieldDef().isApacheMD5Crypted() || (apacheMd5CryptPass != null && need_to_write_all_hashes))
      {
        apacheMd5CryptPass = getApacheMD5CryptText();
        wrote_hash = emitHelper(out, apacheMd5CryptPass, wrote_hash);
      }
    else
      {
        out.writeUTF("");
      }

    if (getFieldDef().isWinHashed() || ((lanHash != null || ntHash != null) && need_to_write_all_hashes))
      {
        lanHash = getLANMANCryptText();
        wrote_hash = emitHelper(out, lanHash, wrote_hash);

        ntHash = getNTUNICODECryptText();
        wrote_hash = emitHelper(out, ntHash, wrote_hash);
      }
    else
      {
        out.writeUTF("");
        out.writeUTF("");
      }

    if (getFieldDef().isSSHAHashed() || (sshaHash != null && need_to_write_all_hashes))
      {
        sshaHash = getSSHAHashText();
        wrote_hash = emitHelper(out, sshaHash, wrote_hash);
      }
    else
      {
        out.writeUTF("");
      }

    // starting at file version 2.13
    //
    // (see DBStore.major_version and DBStore.minor_version)

    if (getFieldDef().isShaUnixCrypted() || (shaUnixCrypt != null && need_to_write_all_hashes))
      {
        shaUnixCrypt = getShaUnixCryptText();
        wrote_hash = emitHelper(out, shaUnixCrypt, wrote_hash);
      }
    else
      {
        out.writeUTF("");
      }

    // starting at file version 2.21
    //
    // (see DBStore.major_version and DBStore.minor_version)

    if (getFieldDef().isBCrypted() || (bCryptPass != null && need_to_write_all_hashes))
      {
        bCryptPass = getBCryptText();
        wrote_hash = emitHelper(out, bCryptPass, wrote_hash);
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
        emitHelper(out, uncryptedPass, true);
      }
    else
      {
        out.writeUTF("");
      }

    // starting at 2.19, we store a history archive, if defined

    if (getFieldDef().isHistoryChecked())
      {
        if (history != null)
          {
            // if this field's pool size has been changed since the last
            // time the history archive was adjusted, tweak it as a side
            // effect while we're writing out.

            if (getFieldDef().getHistoryDepth() != history.getPoolSize())
              {
                history.setPoolSize(getFieldDef().getHistoryDepth());
              }

            history.emit(out);
          }
        else
          {
            out.writeInt(0);
          }
      }
  }

  /**
   * Helper method for the emit() method, which takes care of encoding
   * null strings as empty strings on disk.
   *
   * @param out The DataOutput that we are emitting to
   * @param val The String to write out
   * @param wrote_hash If we write out a non-null value, we'll return
   * true.  Otherwise, we'll just return the value of this boolean.
   */

  private boolean emitHelper(DataOutput out, String val, boolean wrote_hash) throws IOException
  {
    if (val == null)
      {
        out.writeUTF("");
        return wrote_hash;
      }
    else
      {
        out.writeUTF(val);
        return true;
      }
  }

  /**
   * <p>This method helps calculate what we should do if the
   * administrator has reconfigured the hash requirements for this
   * field.</p>
   *
   * <p>Effectively, we're looking to see if we have any validly
   * constructed hash text that suits any hash algorithms left enabled
   * in the schema editor.  If we find anything that we have been told
   * to use (i.e., getFieldDef().isCrypted(), etc.) and for which we
   * have a non-null text available, we won't need to take any special
   * action, and we'll return false here.</p>
   *
   * <p>If we get through the plaintext and all the supported hash forms,
   * and we determine that we have no requested hash information for
   * the user at all, we'll return true, which will cause the emit()
   * routine above to write out any hash text which it still has
   * possession of, even if this field is no longer configured to
   * cause that hash to be generated for further usage.</p>
   *
   * <p>Thanks to our password capture logic, if a user validates his
   * password against this password field, we'll generate the new hash
   * format when the user validates, and from then on, we won't need
   * to save the old type of hash text.</p>
   */

  private boolean writeOutAllStoredValues()
  {
    DBObjectBaseField def = getFieldDef();

    /* -- */

    // If we know the plaintext, we're guaranteed to write out
    // something we can use during emit, either through the tracking
    // the wrote_hash variable in emit() does for us, or through
    // generation of new hash text on demand at emit time.
    //
    // In either case, we don't need to make a point of writing out
    // old hash text which we've kept around.

    if (uncryptedPass != null)
      {
        return false;
      }

    if (def.isCrypted() && cryptedPass != null)
      {
        return false;
      }

    if (def.isMD5Crypted() && md5CryptPass != null)
      {
        return false;
      }

    if (def.isApacheMD5Crypted() && apacheMd5CryptPass != null)
      {
        return false;
      }

    if (def.isWinHashed() && (lanHash != null || ntHash != null))
      {
        return false;
      }

    if (def.isSSHAHashed() && sshaHash != null)
      {
        return false;
      }

    if (def.isBCrypted() && bCryptPass != null)
      {
        return false;
      }

    if (def.isShaUnixCrypted() && shaUnixCrypt != null)
      {
        return false;
      }

    return true;
  }

  /**
   * <p>This method is responsible for reading in the contents of
   * this field from an binary input stream.  It is used in reading
   * fields from the ganymede.db file and from the journal file.</p>
   *
   * <p>The code that calls receive() on this field is responsible for
   * having read enough of the binary input stream's context to
   * place the read cursor at the point in the file immediately after
   * the field's id and type information has been read.</p>
   */

  @Override void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    clear_stored();

    // we radically simplified PasswordDBField's on-disk format at
    // file version 2.1

    if (Ganymede.db.isAtLeast(2,1))
      {
        cryptedPass = readUTF(in);
        md5CryptPass = readUTF(in);

        if (Ganymede.db.isAtLeast(2,4))
          {
            apacheMd5CryptPass = readUTF(in);
          }

        lanHash = readUTF(in);
        ntHash = readUTF(in);

        if (Ganymede.db.isAtLeast(2,5))
          {
            sshaHash = readUTF(in);
          }

        if (Ganymede.db.isAtLeast(2,13))
          {
            shaUnixCrypt = readUTF(in);
          }

        if (Ganymede.db.isAtLeast(2,21))
          {
            bCryptPass = readUTF(in);
          }

        uncryptedPass = readUTF(in);

        // we added passwordHistoryArchive at 2.19

        if (Ganymede.db.isAtLeast(2,19))
          {
            // At 2.19, I had things quite broken, so I need some
            // special logic for reading the history pool during
            // journal loading when treating that db version.
            //
            // At 2.20, we only write out an archive (including the
            // count) if the field is configured for history
            // archiving/checking, and things are stable and as we
            // like it.

            if ((Ganymede.db.isAtRev(2,19) && Ganymede.db.journalLoading) || definition.isHistoryChecked())
              {
                int count = in.readInt();

                history = new passwordHistoryArchive(definition.getHistoryDepth(), count, in);
              }
          }
        else
          {
            if (getFieldDef().isHistoryChecked())
              {
                history = new passwordHistoryArchive(definition.getHistoryDepth());
              }
            else
              {
                history = null;
              }
          }

        return;
      }

    // From here on down we do things the old, hard way

    // at file format 1.10, we were keeping both crypted and unecrypted
    // passwords on disk.  Since then, we have decided to only write
    // out encrypted passwords if we are using them.

    if (Ganymede.db.isAtRev(1,10))
      {
        cryptedPass = readUTF(in);
        uncryptedPass = readUTF(in);

        return;
      }

    // if we're not looking at file version 1.10, the crypted password is
    // the first thing we'll see, if the field definition specifies the
    // use of it

    if (definition.isCrypted())
      {
        cryptedPass = readUTF(in);

        if (Ganymede.db.isBetweenRevs(1,13,1,16))
          {
            in.readUTF();       // skip old-style (buggy) md5 pass
          }
      }

    // now we see if we expect to see an MD5Crypt()'ed  password

    // note that even though we test for >= 1.16, we won't get to this point
    // if we are using the >= 2.1 logic

    if (Ganymede.db.isAtLeast(1,16))
      {
        if (definition.isMD5Crypted())
          {
            md5CryptPass = readUTF(in);
          }
      }

    if (!definition.isCrypted() && !definition.isMD5Crypted())
      {
        uncryptedPass = readUTF(in);
      }
  }

  /**
   * This helper method reads a UTF string from in, decoding an empty
   * String as null.
   */

  private String readUTF(DataInput in) throws IOException
  {
    String val = in.readUTF();

    if (val.equals(""))
      {
        return null;
      }
    else
      {
        return val;
      }
  }

  /**
   * This method is used when the database is being dumped, to write
   * out this field to disk.
   */

  @Override void emitXML(XMLDumpContext dump) throws IOException
  {
    this.emitXML(dump, true);
  }

  /**
   * This method is used when the database is being dumped, to write
   * out this field to disk.
   */

  synchronized void emitXML(XMLDumpContext dump, boolean writeSurroundContext) throws IOException
  {
    if (writeSurroundContext)
      {
        dump.indent();
        dump.startElement(this.getXMLName());
      }

    dump.startElement("password");

    if (!dump.doDumpPasswords())
      {
        dump.endElement("password");

        if (writeSurroundContext)
          {
            dump.endElement(this.getXMLName());
          }

        return;
      }

    if (uncryptedPass != null &&
        (dump.doDumpPlaintext() ||
         (cryptedPass == null &&
          md5CryptPass == null &&
          apacheMd5CryptPass == null &&
          lanHash == null &&
          ntHash == null &&
          sshaHash == null &&
          shaUnixCrypt == null &&
          bCryptPass == null)))
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

    if (shaUnixCrypt != null)
      {
        dump.attribute("shaUnixCrypt", shaUnixCrypt);
      }

    if (bCryptPass != null)
      {
        dump.attribute("bCrypt", bCryptPass);
      }

    dump.endElement("password");

    if (writeSurroundContext)
      {
        dump.endElement(this.getXMLName());
      }
  }

  /**
   * <p>Standard {@link arlut.csd.ganymede.rmi.db_field db_field}
   * method to retrieve the value of this field.  Because we are
   * holding sensitive password information, this method always throws
   * an IllegalAccessException.. we don't want to make password values
   * available to a remote client under any circumstances.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  @Override public Object getValue()
  {
    return null;
  }

  /**
   * <po>Returns an Object carrying the value held in this field.</p>
   *
   * <p>This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.</p>
   *
   * <p>Note that this method will always return null, as you need to use
   * the special Password-specific value accessors to get access to the
   * password information in crypted or non-crypted form.</p>
   */

  @Override public Object getValueLocal()
  {
    return null;
  }

  // ****
  //
  // type specific value accessors
  //
  // ****

  /**
   * <p>Returns a descriptive text value for this PasswordDBField
   * without checking permissions.  The returned string will include
   * information about what hash formats are present in this field,
   * but will not include any other information about the contents of
   * this password field.</p>
   *
   * <p>This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the {@link
   * arlut.csd.ganymede.server.DBObject#getLabel() getLabel()} logic
   * for {@link arlut.csd.ganymede.server.DBObject DBObject}.</p>
   *
   * <p>If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.</p>
   */

  @Override public synchronized String getValueString()
  {
    if (this.isDefined())
      {
        StringBuilder result = new StringBuilder();

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

        if (shaUnixCrypt != null)
          {
            result.append("shaUnixCrypt ");
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

  @Override public String getEncodingString()
  {
    return getValueString();
  }

  /**
   * <p>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</p>
   *
   * <p>In the case of the PasswordDBField, the string returned simply
   * indicates that the password has changed.</p>
   *
   * <p>If there is no change in the field, null will be returned.</p>
   */

  @Override public String getDiffString(DBField orig)
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
   * <p>Returns true if the password stored in this field is hash-crypted.</p>
   */

  public boolean crypted()
  {
    return (getFieldDef().isCrypted());
  }

  /**
   * <p>Authenticates a provided plaintext password against the stored
   * contents of this password field.</p>
   *
   * <p>The password field may have stored the password in plaintext,
   * or in any of a variety of cryptographic hash formats.
   * matchPlainText() will perform whatever operation on the provided
   * plaintext as is required to determine whether or not it matches
   * with the stored password data.</p>
   *
   * <p>If this field is configured to create and retain other hash
   * formats, this method will create the missing hash formats as
   * needed if the input text is successfully matched against the
   * password data held in this field.</p>
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

    // Test against our hashes in decreasing order of hashing
    // fidelity.  note that we check here to see if we are in
    // possession of a piece of hash text, not whether we are
    // currently configured to generate that form of hash.
    //
    // This is to allow us to transition from one requested hash text
    // to the other.  See the emit() and the writeOutAllStoredValues()
    // methods, above for more details.

    if (uncryptedPass != null)
      {
        success = uncryptedPass.equals(plaintext); // most accurate
      }
    else if (sshaHash != null)  // 2^64 bits, but fast
      {
        success = SSHA.matchSHAHash(sshaHash, plaintext);
      }
    else if (shaUnixCrypt != null) // large precision, but potentially quite slow
      {
        if (shaUnixCrypt.startsWith("$5$"))
          {
            success = Sha256Crypt.verifyPassword(plaintext, shaUnixCrypt);
          }
        else if (shaUnixCrypt.startsWith("$6$"))
          {
            success = Sha512Crypt.verifyPassword(plaintext, shaUnixCrypt);
          }
      }
    else if (bCryptPass != null) // ditto
      {
        success = BCrypt.checkpw(plaintext, bCryptPass);
      }
    else if (md5CryptPass != null) // indefinite
      {
        success = md5CryptPass.equals(MD5Crypt.crypt(plaintext, getMD5Salt()));
      }
    else if (apacheMd5CryptPass != null) // indefinite
      {
        success = apacheMd5CryptPass.equals(MD5Crypt.apacheCrypt(plaintext, getApacheMD5Salt()));
      }
    else if (ntHash != null)    // 2^64 bits
      {
        success = ntHash.equals(smbencrypt.NTUNICODEHash(plaintext));
      }
    else if (lanHash != null)   // 14 chars
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
    //
    // we call this 'passive password capture'.

    if (success && uncryptedPass == null)
      {
        int precision = getHashPrecision();

        if (precision == -1 ||
            (precision > 0 && precision >= plaintext.length()))
          {
            uncryptedPass = plaintext;
            clear_unused_stored();
            setHashes(plaintext, false);
          }
      }

    return success;
  }

  /**
   * <p>This server-side only method returns the UNIX-encrypted
   * password text.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
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
   * <p>This server-side only method returns the md5crypt()-encrypted
   * hashed password text.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
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
   * <p>This server-side only method returns the Apache md5crypt()-encrypted
   * hashed password text.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
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
   * <p>This server-side only method returns the LANMAN-compatible
   * password hash of the password data held in this field.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
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
   * <p>This server-side only method returns the Windows NT 4
   * SP3-compatible md4/Unicode password hash of the password data
   * held in this field.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
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
   * <p>This server-side only method returns the Netscape SSHA (salted
   * SHA) LDAP hash of the password data held in this field.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
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
   * <p>This server-side only method returns the OpenBSD BCrypt hash text
   * of the password data held in this field, generating the hash text
   * from scratch if it is not contained in the local bCryptPass
   * variable.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
   */

  public String getBCryptText()
  {
    if (bCryptPass != null)
      {
        return bCryptPass;
      }
    else
      {
        if (uncryptedPass == null)
          {
            return null;
          }

        bCryptPass = BCrypt.hashpw(uncryptedPass, BCrypt.gensalt(getFieldDef().getBCryptRounds()));

        return bCryptPass;
      }
  }

  /**
   * <p>This server-side only method returns the Sha Unix Crypt hash text
   * of the password data held in this field, generating the hash text
   * from scratch if it is not contained in the local shaUnixCrypt
   * variable.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
   *
   * <p>The hashText returned by this method will match one of the
   * following four forms:</p>
   *
   * <pre>
   * $5$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   * $5$rounds=&lt;round-count&gt;$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   *
   * $6$&lt;saltstring&gt;$&lt;64 bytes of hash text, base 64 encoded&gt;
   * $6$rounds=&lt;round-count&gt;$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   * </pre>
   *
   * <p>If the round count is specified using the '$rounds=n' syntax, the
   * higher the round count, the more computational work will be
   * required to verify passwords against this hash text.</p>
   *
   * <p>See <a href="http://people.redhat.com/drepper/sha-crypt.html">http://people.redhat.com/drepper/sha-crypt.html</a>
   * for full details of the hash format this method is expecting.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
   */

  public String getShaUnixCryptText()
  {
    if (shaUnixCrypt != null)
      {
        return shaUnixCrypt;
      }
    else
      {
        if (uncryptedPass == null)
          {
            return null;
          }

        if (getFieldDef().isShaUnixCrypted512())
          {
            shaUnixCrypt = Sha512Crypt.Sha512_crypt(uncryptedPass, null, getFieldDef().getShaUnixCryptRounds());
          }
        else
          {
            shaUnixCrypt = Sha256Crypt.Sha256_crypt(uncryptedPass, null, getFieldDef().getShaUnixCryptRounds());
          }

        return shaUnixCrypt;
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
   * <p>Method to obtain the SALT for a stored (traditional Unix)
   * crypted password.  If the client is going to submit a pre-crypted
   * password for comparison via matchCryptText(), it must be salted
   * by the salt returned by this method.</p>
   *
   * <p>If the password is not stored in crypt() form, null will be
   * returned.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
   */

  private String getSalt()
  {
    if (cryptedPass != null)
      {
        return cryptedPass.substring(0,2);
      }
    else
      {
        return null;
      }
  }

  /**
   * <p>Method to obtain the SALT for a stored OpenBSD-style
   * md5crypt()'ed password.</p>
   *
   * <p>If the password is not stored in md5crypt() form,
   * null will be returned.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
   */

  private String getMD5Salt()
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
   * <p>Method to obtain the SALT for a stored Apache-style
   * md5crypt()'ed password.</p>
   *
   * <p>If the password is not stored in apacheMd5crypt() form,
   * null will be returned.</p>
   *
   * <p>This method is never meant to be available remotely.</p>
   */

  private String getApacheMD5Salt()
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
   * <p>Not supported for PasswordDBField.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  @Override public ReturnVal setValue(Object value, boolean local, boolean noWizards)
  {
    // "The setValue() method is not supported on the PasswordDBField."
    throw new IllegalArgumentException(ts.l("setValue.invalid_call"));
  }

  /**
   * <p>This method is used to set the password for this field,
   * crypting it in various ways if this password field is stored
   * crypted.</p>
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public synchronized ReturnVal setPlainTextPass(String plaintext)
  {
    return setPlainTextPass(plaintext, false, false);
  }

  /**
   * <p>Set the plain text password for this field.</p>
   *
   * <p>If this field is configured to create and retain other hash
   * formats, it will do so as needed during the call to this
   * method.</p>
   *
   * <p>Not exported for access by remote clients.</p>
   *
   * @param plaintext The crypt text to load into this PasswordDBField
   * @param local If true, permission checking is skipped
   * @param noWizards If true, the wizardHook() call on the containing DBEditObject will be inhibited.
   */

  public synchronized ReturnVal setPlainTextPass(String plaintext, boolean local, boolean noWizards)
  {
    ReturnVal retVal;
    DBEditObject eObj;

    /* -- */

    retVal = verifyNewValue(plaintext);

    if (!ReturnVal.didSucceed(retVal))
      {
        return retVal;
      }

    eObj = (DBEditObject) this.owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = ReturnVal.merge(retVal, eObj.wizardHook(this,
                                                         DBEditObject.SETPASSPLAIN,
                                                         plaintext,
                                                         null));

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // call finalizeSetValue to allow for chained reactions

    // we'll still retain our first retVal so that we can return
    // advisory-only messages that the wizardHook generated, even if
    // the finalizeSetValue() doesn't generate any.

    retVal = ReturnVal.merge(retVal, ((DBEditObject) this.owner).finalizeSetValue(this, null));

    if (!ReturnVal.didSucceed(retVal))
      {
        return retVal;
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
   * <p>This method is used to set a pre-hashed password for this field,
   * using the traditional (weak) Unix Crypt algorithm.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store Crypt hashed password text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setCryptPass(String text)
  {
    return setCryptPass(text, false, false);
  }

  /**
   * <p>This server-side method is used to set a pre-crypted password
   * for this field.</p>
   *
   * <p>This method will return an error dialog if this field does not store
   * passwords in UNIX crypted format.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * <p>Not exported for access by remote clients.</p>
   *
   * @param text The crypt text to load into this PasswordDBField
   * @param local If true, permission checking is skipped
   * @param noWizards If true, the wizardHook() call on the containing DBEditObject will be inhibited.
   */

  public ReturnVal setCryptPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal = null;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
        // "Password Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("global.error_subj"),
                                          ts.l("global.perm_error_text", this.getName(),
                                               this.owner.getLabel()));
      }

    if (!getFieldDef().isCrypted())
      {
        // "Server: Error in PasswordDBField.setCryptTextPass()"
        // "Password field not configured to support traditional Unix crypt hashing."
        return Ganymede.createErrorDialog(ts.l("setCryptPass.error_title"),
                                          ts.l("setCryptPass.error_text"));
      }

    eObj = (DBEditObject) this.owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = eObj.wizardHook(this, DBEditObject.SETPASSCRYPT, text, null);

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ReturnVal.merge(retVal, ((DBEditObject) this.owner).finalizeSetValue(this, null));

    if (ReturnVal.didSucceed(retVal))
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
   * <p>This method is used to set a pre-crypted FreeBSD-style MD5Crypt
   * password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store MD5Crypt hashed password text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setMD5CryptedPass(String text)
  {
    return setMD5CryptedPass(text, false, false);
  }

  /**
   * <p>This method is used to set a pre-crypted FreeBSD-style MD5Crypt
   * password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store MD5Crypt hashed password text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * <p>Not exported for access by remote clients.</p>
   *
   * @param text The crypt text to load into this PasswordDBField
   * @param local If true, permission checking is skipped
   * @param noWizards If true, the wizardHook() call on the containing DBEditObject will be inhibited.
   */

  public ReturnVal setMD5CryptedPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal = null;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
        // "Password Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("global.error_subj"),
                                          ts.l("global.perm_error_text", this.getName(),
                                               this.owner.getLabel()));
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

    eObj = (DBEditObject) this.owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = eObj.wizardHook(this, DBEditObject.SETPASSMD5, text, null);

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ReturnVal.merge(retVal, ((DBEditObject) this.owner).finalizeSetValue(this, null));

    if (ReturnVal.didSucceed(retVal))
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
   * <p>This method is used to set a pre-crypted Apache-style MD5Crypt
   * password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store Apache-style MD5Crypt hashed password
   * text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setApacheMD5CryptedPass(String text)
  {
    return setApacheMD5CryptedPass(text, false, false);
  }

  /**
   * <p>This method is used to set a pre-crypted Apache-style MD5Crypt
   * password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store Apache-style MD5Crypt hashed password
   * text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * <p>Not exported for access by remote clients.</p>
   *
   * @param text The crypt text to load into this PasswordDBField
   * @param local If true, permission checking is skipped
   * @param noWizards If true, the wizardHook() call on the containing DBEditObject will be inhibited.
   */

  public ReturnVal setApacheMD5CryptedPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal = null;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
        // "Password Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("global.error_subj"),
                                          ts.l("global.perm_error_text", this.getName(),
                                               this.owner.getLabel()));
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

    eObj = (DBEditObject) this.owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = eObj.wizardHook(this, DBEditObject.SETPASSAPACHEMD5, text, null);

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ReturnVal.merge(retVal, ((DBEditObject) this.owner).finalizeSetValue(this, null));

    if (ReturnVal.didSucceed(retVal))
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
   * <p>This method is used to set pre-crypted Windows-style password
   * hashes for this field.  These strings are formatted as used in
   * Samba's encrypted password files.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to accept Windows-hashed password strings.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setWinCryptedPass(String LANMAN, String NTUnicodeMD4)
  {
    return setWinCryptedPass(LANMAN, NTUnicodeMD4, false, false);
  }

  /**
   * <p>This method is used to set pre-crypted Windows-style password
   * hashes for this field.  These strings are formatted as used in
   * Samba's encrypted password files.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to accept Windows-hashed password strings.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * <p>Not exported for access by remote clients.</p>
   *
   * @param LANMAN  The LANMAN hash text to load into this PasswordDBField
   * @param NTUnicodeMD4  The NTUnicodeMD4 hash text to load into this PasswordDBField
   * @param local If true, permission checking is skipped
   * @param noWizards If true, the wizardHook() call on the containing DBEditObject will be inhibited.
   */

  public ReturnVal setWinCryptedPass(String LANMAN, String NTUnicodeMD4, boolean local, boolean noWizards)
  {
    ReturnVal retVal = null;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
        // "Password Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("global.error_subj"),
                                          ts.l("global.perm_error_text", this.getName(),
                                               this.owner.getLabel()));
      }

    if (!getFieldDef().isWinHashed())
      {
        // "Server: Error in PasswordDBField.setWinCryptedPass()"
        // "Password field is not configured to accept Samba hashed password strings."
        return Ganymede.createErrorDialog(ts.l("setWinCryptedPass.error_title"),
                                          ts.l("setWinCryptedPass.error_text"));
      }

    eObj = (DBEditObject) this.owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = eObj.wizardHook(this, DBEditObject.SETPASSWINHASHES, LANMAN, NTUnicodeMD4);

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ReturnVal.merge(retVal, ((DBEditObject) this.owner).finalizeSetValue(this, null));

    if (ReturnVal.didSucceed(retVal))
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
   * <p>This method is used to set a pre-crypted OpenLDAP/Netscape
   * Directory Server Salted SHA (SSHA) password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store SSHA hashed password text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setSSHAPass(String text)
  {
    return this.setSSHAPass(text, false, false);
  }

  /**
   * <p>Sets a pre-crypted OpenLDAP/Netscape Directory Server Salted
   * SHA (SSHA) password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store SSHA hashed password text.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * <p>Not exported for access by remote clients.</p>
   *
   * @param text The crypt text to load into this PasswordDBField
   * @param local If true, permission checking is skipped
   * @param noWizards If true, the wizardHook() call on the containing DBEditObject will be inhibited.
   */

  public ReturnVal setSSHAPass(String text, boolean local, boolean noWizards)
  {
    ReturnVal retVal = null;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
        // "Password Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("global.error_subj"),
                                          ts.l("global.perm_error_text", this.getName(),
                                               this.owner.getLabel()));
      }

    if (!getFieldDef().isSSHAHashed())
      {
        // "Server: Error in PasswordDBField.setSSHAPass()"
        // "Password field is not configured to accept SSHA-1 hashed password strings."
        return Ganymede.createErrorDialog(ts.l("setSSHAPass.error_title"),
                                          ts.l("setSSHAPass.error_text"));
      }

    if (text != null && !text.equals("") && !text.startsWith("{SSHA}"))
      {
        // "Server: Error in PasswordDBField.setSSHAPass()"
        // "The hash text passed to setSSHAPass(), "{0}", is not a well-formed, OpenLDAP-encoded SSHA-1 hash text."
        return Ganymede.createErrorDialog(ts.l("setSSHAPass.error_title"),
                                          ts.l("setSSHAPass.format_error", this.getName()));
      }

    eObj = (DBEditObject) this.owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = eObj.wizardHook(this, DBEditObject.SETPASSSSHA, text, null);

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ReturnVal.merge(retVal, ((DBEditObject) this.owner).finalizeSetValue(this, null));

    if (ReturnVal.didSucceed(retVal))
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
   * <p>This method is used to set a pre-crypted Sha256Crypt or
   * Sha512Crypt password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store ShaCrypt hashed password text.</p>
   *
   * <p>The hashText submitted to this method must match one of the
   * following four forms:</p>
   *
   * <pre>
   * $5$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   * $5$rounds=&lt;round-count&gt;$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   *
   * $6$&lt;saltstring&gt;$&lt;64 bytes of hash text, base 64 encoded&gt;
   * $6$rounds=&lt;round-count&gt;$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   * </pre>
   *
   * <p>If the round count is specified using the '$rounds=n' syntax, the
   * higher the round count, the more computational work will be
   * required to verify passwords against this hash text.</p>
   *
   * <p>See <a href="http://people.redhat.com/drepper/sha-crypt.html">http://people.redhat.com/drepper/sha-crypt.html</a>
   * for full details of the hash format this method is expecting.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setShaUnixCryptPass(String hashText)
  {
    return this.setShaUnixCryptPass(hashText, false, false);
  }

  /**
   * <p>This method is used to set a pre-crypted Sha256Crypt or
   * Sha512Crypt password for this field.</p>
   *
   * <p>This method will return an error code if this password field is
   * not configured to store ShaCrypt hashed password text.</p>
   *
   * <p>The hashText submitted to this method must match one of the
   * following four forms:</p>
   *
   * <pre>
   * $5$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   * $5$rounds=&lt;round-count&gt;$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   *
   * $6$&lt;saltstring&gt;$&lt;64 bytes of hash text, base 64 encoded&gt;
   * $6$rounds=&lt;round-count&gt;$&lt;saltstring&gt;$&lt;32 bytes of hash text, base 64 encoded&gt;
   * </pre>
   *
   * <p>If the round count is specified using the '$rounds=n' syntax, the
   * higher the round count, the more computational work will be
   * required to verify passwords against this hash text.</p>
   *
   * <p>See <a href="http://people.redhat.com/drepper/sha-crypt.html">http://people.redhat.com/drepper/sha-crypt.html</a>
   * for full details of the hash format this method is expecting.</p>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * <p>Not exported for access by remote clients.</p>
   *
   * @param hashText The crypt text to load into this PasswordDBField
   * @param local If true, permission checking is skipped
   * @param noWizards If true, the wizardHook() call on the containing DBEditObject will be inhibited.
   */

  public ReturnVal setShaUnixCryptPass(String hashText, boolean local, boolean noWizards)
  {
    ReturnVal retVal = null;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
        // "Password Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("global.error_subj"),
                                          ts.l("global.perm_error_text", this.getName(),
                                               this.owner.getLabel()));
      }

    if (!getFieldDef().isShaUnixCrypted())
      {
        // "Server: Error in PasswordDBField.setShaUnixCryptPass()"
        // "Password field not configured to accept SHA Unix Crypt hashed password strings."
        return Ganymede.createErrorDialog(ts.l("setShaUnixCryptPass.error_title"),
                                          ts.l("setShaUnixCryptPass.error_text"));
      }

    if (!Sha256Crypt.verifyHashTextFormat(hashText) && !Sha512Crypt.verifyHashTextFormat(hashText))
      {
        // "Server: Error in PasswordDBField.setShaUnixCryptPass()"
        // "The hash text passed to setShaUnixCryptPass(), "{0}", is
        // not a well-formed, SHA Unix Crypt hash text"
        return Ganymede.createErrorDialog(ts.l("setShaUnixCryptPass.error_title"),
                                          ts.l("setShaUnixCryptPass.format_error", this.getName()));
      }

    eObj = (DBEditObject) this.owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = eObj.wizardHook(this, DBEditObject.SETPASS_SHAUNIXCRYPT, hashText, null);

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ReturnVal.merge(retVal, ((DBEditObject) this.owner).finalizeSetValue(this, null));

    if (ReturnVal.didSucceed(retVal))
      {
        // whenever the ShaUnixCrypt password is directly set, we lose
        // plaintext and alternate hashes

        clear_stored();

        if ((hashText == null) || (hashText.equals("")))
          {
            shaUnixCrypt = null;
          }
        else
          {
            shaUnixCrypt = hashText;
          }
      }

    return retVal;
  }

  /**
   * <p>This method is used to set a pre-crypted BCrypt password for
   * this field.</p>
   *
   * <p>This method will return an error code if this password field
   * is not configured to store BCrypt hashed password text.</p>
   *
   * <p>The hashText submitted to this method must match one of the
   * following two formats:</p>
   *
   * <pre>
   * $2$&lt;2 digit cost parameter&gt;$&lt;22 characters of salt followed immediately by 31 characters of hash text, encoded in non-standard base 64&gt;
   * $2a$&lt;2 digit cost parameter&gt;$&lt;22 characters of salt followed immediately by 31 characters of hash text, encoded in non-standard base 64&gt;
   * </pre>
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.pass_field
   */

  public ReturnVal setBCryptPass(String hashText)
  {
    return setBCryptPass(hashText, false, false);
  }

  /**
   * <p>This method is used to set a pre-crypted BCrypt password for
   * this field.</p>
   *
   * <p>This method will return an error code if this password field
   * is not configured to store BCrypt hashed password text.</p>
   *
   * <p>The hashText submitted to this method must match one of the
   * following two formats:</p>
   *
   * <pre>
   * $2$&lt;2 digit cost parameter&gt;$&lt;22 characters of salt followed immediately by 31 characters of hash text, encoded in non-standard base 64&gt;
   * $2a$&lt;2 digit cost parameter&gt;$&lt;22 characters of salt followed immediately by 31 characters of hash text, encoded in non-standard base 64&gt;
   * </pre>
   *
   * <p>Note
   *
   * <p>When this method is called, all other data for this password
   * field are cleared.  Any plaintext held by the field is erased,
   * and any other stored hash formats are deleted.  If the field is
   * configured to create and retain other hash formats, it will do so
   * opportunistically if the user successfully logs into Ganymede
   * using the password stored in this field.</p>
   *
   * <p>Not exported for access by remote clients.</p>
   *
   * @param hashText The crypt text to load into this PasswordDBField
   * @param local If true, permission checking is skipped
   * @param noWizards If true, the wizardHook() call on the containing DBEditObject will be inhibited.
   */

  public ReturnVal setBCryptPass(String hashText, boolean local, boolean noWizards)
  {
    ReturnVal retVal = null;
    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
        // "Password Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("global.error_subj"),
                                          ts.l("global.perm_error_text", this.getName(),
                                               this.owner.getLabel()));
      }

    if (!getFieldDef().isBCrypted())
      {
        // "Server: Error in PasswordDBField.setBCryptPass()"
        // "Password field not configured to accept bCrypt hashed password strings."
        return Ganymede.createErrorDialog(ts.l("setBCryptPass.error_title"),
                                          ts.l("setBCryptPass.error_text"));
      }

    if (!BCrypt.verifyHashTextFormat(hashText))
      {
        // "Server: Error in PasswordDBField.setBCryptPass()"
        // "The hash text passed to setBCryptPass(), "{0}", is
        // not a well-formed, bCrypt hash text"
        return Ganymede.createErrorDialog(ts.l("setBCryptPass.error_title"),
                                          ts.l("setBCryptPass.format_error", this.getName()));
      }

    eObj = (DBEditObject) this.owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        retVal = eObj.wizardHook(this, DBEditObject.SETPASS_BCRYPT, hashText, null);

        // if a wizard intercedes, we are going to let it take the ball.

        if (ReturnVal.wizardHandled(retVal))
          {
            return retVal;
          }
      }

    // call finalizeSetValue to allow for chained reactions

    retVal = ReturnVal.merge(retVal, ((DBEditObject) this.owner).finalizeSetValue(this, null));

    if (ReturnVal.didSucceed(retVal))
      {
        // whenever the BCrypt password is directly set, we lose
        // plaintext and alternate hashes

        clear_stored();

        if ((hashText == null) || (hashText.equals("")))
          {
            bCryptPass = null;
          }
        else
          {
            bCryptPass = hashText;
          }
      }

    return retVal;
  }

  /**
   * <p>This method is used to force all known hashes into this password
   * field.  Ganymede does no verifications to insure that all of these
   * hashes really match the same password, so caveat emptor.  If any of
   * these hashes are null or empty string, those hashes will be cleared.</p>
   *
   * </p>Calling this method will clear the password's stored plaintext,
   * if any.</p>
   *
   * <p>This method is intended to be called via the {@link
   * arlut.csd.ganymede.server.GanymedeXMLSession GanymedeXMLSession}
   * in support of the xmlclient, and is specifically designed to
   * allow all types of hash text to be loaded, without regard for
   * which hash formats this password field is configured to
   * generate.</p>
   *
   * <p>We're deliberately permissive in accepting hash text from the
   * xmlclient so that we can take hash text in for the purpose of
   * authenticating users logging to Ganymede itself, even if the
   * adopter doesn't necessarily intend to use that hash text format
   * going forward.  This is mainly for the purpose of bootstrapping a
   * new Ganymede server with pre-hashed password data from a
   * pre-existing authentication system.</p>
   *
   * <p>Not exported for access by remote clients.</p>
   *
   * @param local If true, permission checking is skipped
   * @param noWizards If true, the wizardHook() call on the containing DBEditObject will be inhibited.
   */

  public ReturnVal setAllHashes(String crypt,
                                String md5crypt,
                                String apacheMd5Crypt,
                                String LANMAN,
                                String NTUnicodeMD4,
                                String SSHAText,
                                String ShaUnixCryptText,
                                String bCryptText,
                                boolean local,
                                boolean noWizards)
  {
    ReturnVal retVal = null;
    DBEditObject eObj;
    boolean settingCrypt, settingMD5, settingApacheMD5, settingWin, settingSSHA, settingShaUnixCrypt, settingBCrypt;

    /* -- */

    if (!isEditable(local))
      {
        // "Password Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("global.error_subj"),
                                          ts.l("global.perm_error_text", this.getName(),
                                               this.owner.getLabel()));
      }

    settingCrypt = (crypt != null && !crypt.equals(""));
    settingMD5 = (md5crypt != null && !md5crypt.equals(""));
    settingApacheMD5 = (apacheMd5Crypt != null && !apacheMd5Crypt.equals(""));
    settingWin = (LANMAN != null && !LANMAN.equals("")) || (NTUnicodeMD4 != null && !NTUnicodeMD4.equals(""));
    settingSSHA = (SSHAText != null && !SSHAText.equals(""));
    settingShaUnixCrypt = (ShaUnixCryptText != null && !ShaUnixCryptText.equals(""));
    settingBCrypt = (bCryptText != null && !bCryptText.equals(""));

    if (!settingCrypt && !settingWin && !settingMD5 && !settingApacheMD5 && !settingSSHA && !settingShaUnixCrypt && !settingBCrypt)
      {
        // clear it!

        return setPlainTextPass(null);
      }

    // nope, we're setting something.. let's find out what

    if (settingSSHA)
      {
        if (!SSHAText.startsWith("{SSHA}"))
          {
            // "Server: Error in PasswordDBField.setAllHashes()"
            // "The SSHA hash text passed to setAllHashes() is not a well-formed, OpenLDAP-encoded SSHA-1 hash text."
            return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
                                              ts.l("setAllHashes.ssha_format_error"));
          }
      }

    if (settingShaUnixCrypt)
      {
        if (!Sha256Crypt.verifyHashTextFormat(ShaUnixCryptText) && !Sha512Crypt.verifyHashTextFormat(ShaUnixCryptText))
          {
            // "Server: Error in PasswordDBField.setAllHashes()"
            // "The hash text passed to setShaUnixCryptPass(), "{0}", is not a well-formed, SHA Unix Crypt hash text"
            return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
                                              ts.l("setShaUnixCryptPass.format_error", ShaUnixCryptText));
          }
      }

    if (settingMD5)
      {
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
        if (!apacheMd5Crypt.startsWith("$apr1$") || (md5crypt.indexOf('$', 6) == -1))
          {
            // "Server: Error in PasswordDBField.setAllHashes()"
            // "The Apache MD5Crypt hash text passed to setAllHashes(), "{0}", is not well-formed."
            return Ganymede.createErrorDialog(ts.l("setAllHashes.error_title"),
                                              ts.l("setAllHashes.apache_format_error", apacheMd5Crypt));
          }
      }

    eObj = (DBEditObject) this.owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
        // Wizard check

        if (settingWin)
          {
            retVal = eObj.wizardHook(this, DBEditObject.SETPASSWINHASHES, LANMAN, NTUnicodeMD4);

            // if a wizard intercedes, we are going to let it take the ball.

            if (ReturnVal.wizardHandled(retVal))
              {
                return retVal;
              }
          }

        if (settingMD5)
          {
            retVal = ReturnVal.merge(retVal, eObj.wizardHook(this,
                                                             DBEditObject.SETPASSMD5,
                                                             md5crypt,
                                                             null));

            // if a wizard intercedes, we are going to let it take the ball.

            if (ReturnVal.wizardHandled(retVal))
              {
                return retVal;
              }
          }

        if (settingApacheMD5)
          {
            retVal = ReturnVal.merge(retVal, eObj.wizardHook(this,
                                                             DBEditObject.SETPASSAPACHEMD5,
                                                             apacheMd5Crypt,
                                                             null));

            // if a wizard intercedes, we are going to let it take the ball.

            if (ReturnVal.wizardHandled(retVal))
              {
                return retVal;
              }
          }

        if (settingCrypt)
          {
            retVal = ReturnVal.merge(retVal, eObj.wizardHook(this,
                                                             DBEditObject.SETPASSCRYPT,
                                                             crypt,
                                                             null));

            // if a wizard intercedes, we are going to let it take the ball.

            if (ReturnVal.wizardHandled(retVal))
              {
                return retVal;
              }
          }

        if (settingSSHA)
          {
            retVal = ReturnVal.merge(retVal, eObj.wizardHook(this,
                                                             DBEditObject.SETPASSSSHA,
                                                             sshaHash,
                                                             null));

            // if a wizard intercedes, we are going to let it take the ball.

            if (ReturnVal.wizardHandled(retVal))
              {
                return retVal;
              }
          }

        if (settingShaUnixCrypt)
          {
            retVal = ReturnVal.merge(retVal, eObj.wizardHook(this,
                                                             DBEditObject.SETPASS_SHAUNIXCRYPT,
                                                             ShaUnixCryptText,
                                                             null));

            // if a wizard intercedes, we are going to let it take the ball.

            if (ReturnVal.wizardHandled(retVal))
              {
                return retVal;
              }
          }

        if (settingBCrypt)
          {
            retVal = ReturnVal.merge(retVal, eObj.wizardHook(this,
                                                             DBEditObject.SETPASS_BCRYPT,
                                                             bCryptText,
                                                             null));

            // if a wizard intercedes, we are going to let it take the ball.

            if (ReturnVal.wizardHandled(retVal))
              {
                return retVal;
              }
          }
      }

    // call finalizeSetValue to allow for chained reactions.  note
    // that we don't actually pass any password data to the
    // finalizeSetValue method.. this is just to allow for a generic
    // veto on all changes

    retVal = ReturnVal.merge(retVal, ((DBEditObject) this.owner).finalizeSetValue(this, null));

    if (ReturnVal.didSucceed(retVal))
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

        if (settingShaUnixCrypt)
          {
            shaUnixCrypt = ShaUnixCryptText;
          }

        if (settingBCrypt)
          {
            bCryptPass = bCryptText;
          }
      }

    return retVal;
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  @Override public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof String));
  }

  /**
   * Generally only for when we get a plaintext submission..
   */

  @Override public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;
    String s;

    /* -- */

    if (!isEditable(true))
      {
        // "Password Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("global.error_subj"),
                                          ts.l("global.perm_error_text", this.getName(),
                                               this.owner.getLabel()));
      }

    eObj = (DBEditObject) this.owner;

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
                                          ts.l("verifyNewValue.too_long", Integer.valueOf(this.maxSize())));
      }

    if (s.length() < minSize())
      {
        // "Password Field Error"
        // "The submitted password is too short.  The minimum plaintext password length accepted is {0,number,#} characters."
        return Ganymede.createErrorDialog(ts.l("global.error_subj"),
                                          ts.l("verifyNewValue.too_short", Integer.valueOf(this.minSize())));
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
                                                       Character.valueOf(s.charAt(i))));
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
                                                       Character.valueOf(s.charAt(i))));
              }
          }
      }

    ReturnVal returnValInProgress = ReturnVal.success();

    if (getFieldDef().isCracklibChecked() && Ganymede.crackLibPacker != null)
      {
        try
          {
            String cracklibCheck = CrackLib.fascistLook(Ganymede.crackLibPacker, s, this.owner.getLabel());

            if (cracklibCheck != null)
              {
                if (getFieldDef().hasCracklibCheckException() && getGSession().getPermManager().isSuperGash())
                  {
                    // "Password Quality Problem"
                    // "The password fails quality checking.\nThe checker reported the following problem:\n{0}"
                    returnValInProgress = Ganymede.createInfoDialog(ts.l("verifyNewValue.cracklib_failure_title"),
                                                                    ts.l("verifyNewValue.cracklib_failure_error", cracklibCheck));
                  }
                else
                  {
                    // "Password Quality Problem"
                    // "The password fails quality checking.\nThe checker reported the following problem:\n{0}"
                    return Ganymede.createErrorDialog(ts.l("verifyNewValue.cracklib_failure_title"),
                                                      ts.l("verifyNewValue.cracklib_failure_error", cracklibCheck));
                  }
              }
          }
        catch (IOException ex)
          {
            ex.printStackTrace();
          }
      }

    if (getFieldDef().isHistoryChecked())
      {
        if (history != null)
          {
            Date previousDate = history.contains(s);

            if (previousDate != null)
              {
                if (getFieldDef().hasHistoryCheckException() && getGSession().getPermManager().isSuperGash())
                  {
                    // "Password Used Before"
                    // "This password has been used too recently with this account.\n\nIt was last used with this account at {0, time} on {0, date, full}."
                    returnValInProgress = ReturnVal.merge(returnValInProgress,
                                                          Ganymede.createInfoDialog(ts.l("verifyNewValue.history_reuse_title"),
                                                                                    ts.l("verifyNewValue.history_reuse_error",
                                                                                         previousDate)));
                  }
                else
                  {
                    // "Password Used Before"
                    // "This password has been used too recently with this account.\n\nIt was last used with this account at {0, time} on {0, date, full}."
                    return Ganymede.createErrorDialog(ts.l("verifyNewValue.history_reuse_title"),
                                                      ts.l("verifyNewValue.history_reuse_error",
                                                           previousDate));
                  }
              }
          }
      }

    // have our parent make the final ok on the value

    return ReturnVal.merge(returnValInProgress,
                           eObj.verifyNewValue(this, s));
  }

  /**
   * <p>This method returns an int indicating to what precision the
   * password in this PasswordDBField is known.  Certain cryptographic
   * hashes have limits on how many characters of the input text are
   * taken into account in the hash.</p>
   *
   * <p>This method returns -1 if the password is known with no limits
   * on its precision (plaintext, or md5crypt, or ssha which is
   * precise to 2^64 bits.. close enough), 0 if the password is not
   * known, or a positive integer indicating the number of characters
   * of precision that we believe we can recognize from our hash
   * authenticators.</p>
   */

  private int getHashPrecision()
  {
    if (uncryptedPass != null || md5CryptPass != null ||
        apacheMd5CryptPass != null || sshaHash != null || ntHash != null ||
        shaUnixCrypt != null || bCryptPass != null)
      {
        return -1;              // full precision
      }

    if (lanHash != null)
      {
        return 14;              // Old-school Windows hashes are good
                                // for 14 chars
      }

    if (cryptedPass != null)
      {
        return 8;               // Old-school UNIX sux0rs.. we should
                                // only be using this for importing
                                // users from old /etc/passwd-style
                                // files
      }

    return 0;                   // i got nothing, boss
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

    if (getFieldDef().isShaUnixCrypted() && (forceChange || shaUnixCrypt == null))
      {
        shaUnixCrypt = null;    // force new hash

        shaUnixCrypt = getShaUnixCryptText();
      }

    if (getFieldDef().isBCrypted() && (forceChange || bCryptPass == null))
      {
        bCryptPass = null;      // force new hash

        bCryptPass = getBCryptText();
      }
  }

  /*----------------------------------------------------------------------------
                                                                    nested class
                                                          passwordHistoryArchive

  ----------------------------------------------------------------------------*/

  /**
   * <p>Holds previous passwords that have been associated with this
   * password field.</p>
   *
   * <p>This archive is stored on disk in the ganymede.db and journal
   * files as part of the on-disk storage of the PasswordDBField.</p>
   *
   * <p>All passwords in the archive are stored in an unsalted SSHA
   * hash format, LDAP style.</p>
   */

  static class passwordHistoryArchive {

    /**
     * Queue of password history entries.  New elements are added to
     * the beginning of the list, and old elements are removed from
     * the end after the pool is filled.
     */

    private List<passwordHistoryEntry> pool;
    private int poolSize;

    /* -- */

    public passwordHistoryArchive(int poolSize)
    {
      this.poolSize = poolSize;
      this.pool = null;
    }

    /**
     * Receive constructor
     */

    public passwordHistoryArchive(int poolSize, int count, DataInput in) throws IOException
    {
      setPoolSize(poolSize);
      this.receive(count, in);
    }

    public synchronized void receive(int count, DataInput in) throws IOException
    {
      pool = new ArrayList<passwordHistoryEntry>(count);

      for (int i = 0; i < count; i++)
        {
          pool.add(new passwordHistoryEntry(in));
        }
    }

    public synchronized void emit(DataOutput out) throws IOException
    {
      if (pool != null)
        {
          out.writeInt(pool.size());

          for (passwordHistoryEntry entry: pool)
            {
              entry.emit(out);
            }
        }
      else
        {
          out.writeInt(0);
        }
    }

    /**
     * Returns the size of the pool.
     */

    public synchronized int getPoolSize()
    {
      return poolSize;
    }

    /**
     * <p>This method changes the size of this archive.  If poolSize is
     * less than the current size of this archive, older passwords are
     * trimmed from this archive.</p>
     *
     * <p>If poolSize is greater than the current size of this archive,
     * space is added, but no values are put in the newly available
     * slots until add() is called.</p>
     */

    public synchronized void setPoolSize(int poolSize)
    {
      this.poolSize = poolSize;

      if (pool != null)
        {
          while (pool.size() > poolSize)
            {
              pool.remove(pool.size() - 1);
            }
        }
    }

    /**
     * Add a new password to the password archive.
     *
     * @param password The plaintext of the password
     * @param date The date the password was committed into the
     * database.
     */

    public synchronized void add(String password, Date date)
    {
      if (pool == null)
        {
          pool = new ArrayList<passwordHistoryEntry>();
        }

      // remove a password if it's already in the pool, so we can add
      // it back to the start of the queue with our new date.

      for (passwordHistoryEntry entry: pool)
        {
          if (entry.matches(password))
            {
              pool.remove(entry);
              break;
            }
        }

      pool.add(0, new passwordHistoryEntry(password, date));

      if (pool.size() > poolSize)
        {
          pool.remove(pool.size() - 1);
        }
    }

    /**
     * <p>This method checks to see if the plaintext password was
     * previously associated with this passwordHistoryArchive.</p>
     *
     * <p>If a previous instance of this password is found in this
     * archive, the Date of the previous use is returned.</p>
     *
     * <p>If not, this method returns null.</p>
     */

    public synchronized Date contains(String password)
    {
      if (pool == null)
        {
          return null;
        }

      for (passwordHistoryEntry entry: pool)
        {
          if (entry.matches(password))
            {
              return entry.getDate();
            }
        }

      return null;
    }
  }

  /*----------------------------------------------------------------------------
                                                                    nested class
                                                            passwordHistoryEntry

  ----------------------------------------------------------------------------*/

  /**
   * This nested class holds a previous password hash along with the
   * date that it was committed into this field.
   */

  static class passwordHistoryEntry {

    /**
     * The date this password history entry was committed to the
     * Ganymede database.
     */

    private Date date;

    /**
     * An unsalted, fast-evalated SSHA hash, LDAP style.
     */

    private String hash;

    /* -- */

    public passwordHistoryEntry(String plaintext, Date date)
    {
      this.hash = SSHA.getLDAPSSHAHash(plaintext, null);
      this.date = date;
    }

    public passwordHistoryEntry(DataInput in) throws IOException
    {
      this.hash = in.readUTF();
      this.date = new Date(in.readLong());
    }

    public void emit(DataOutput out) throws IOException
    {
      out.writeUTF(hash);
      out.writeLong(date.getTime());
    }

    public Date getDate()
    {
      return this.date;
    }

    public String getHash()
    {
      return this.hash;
    }

    public boolean matches(String plaintext)
    {
      return SSHA.matchSHAHash(hash, plaintext);
    }
  }
}

