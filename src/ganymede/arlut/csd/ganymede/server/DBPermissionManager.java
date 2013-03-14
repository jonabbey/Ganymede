/*

   DBPermissionManager.java

   Contains the permissions management logic for the Ganymede Server.

   Created: 18 April 2012

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

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

import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.BaseListTransport;
import arlut.csd.ganymede.common.CategoryTransport;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectHandle;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.PermMatrix;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                             DBPermissionManager

------------------------------------------------------------------------------*/

/**
 * <p>Permissions manager for the Ganymede Server.</p>
 *
 * <p>Each GanymedeSession logged into the Ganymede Server will have
 * its own DBPermissionManager attached, which does permission
 * management for it.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public final class DBPermissionManager {

  static final boolean debug = false;
  static final boolean permsdebug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBPermissionManager");

  // ---

  /**
   * The GanymedeSession that this DBPermissionManager is connected to.
   */

  private GanymedeSession gSession = null;

  /**
   * The DBSession that lays under gSession.
   */

  private DBSession dbSession = null;

  /**
   * A flag indicating whether the client has supergash priviliges.  We
   * keep track of this to speed internal operations.
   */

  private boolean supergashMode = false;

  /**
   * GanymedeSessions created for internal operations always operate
   * with supergash privileges.  We'll set this flag to true to avoid
   * having to do persona membership checks on initial set-up.
   */

  private boolean beforeversupergash = false; // Be Forever Yamamoto

  /**
   * <p>The name of the user logged in.  If the person logged in is
   * using supergash, username will be supergash, even though
   * supergash isn't technically a user.</p>
   *
   * <p>May be null if the containing GanymedeSession was created by
   * an internal Ganymede task or process.</p>
   */

  private String username;

  /**
   * <p>The name that the session is given.  Must be non-null and
   * unique among logged in sessions on the server.</p>
   */

  private String sessionName;

  /**
   * <p>The object reference identifier for the logged in user, if
   * any.  If the client logged in directly to a persona account, this
   * will be null.  See personaInvid in that case.</p>
   */

  private Invid userInvid;

  /**
   * <p>The name of the current persona, of the form
   * '&lt;username&gt;:&lt;description&gt;', for example,
   * 'broccol:GASH Admin'.  If the user is logged in with just
   * end-user privileges, personaName will be null.</p>
   */

  private String personaName = null;

  /**
   * <p>The object reference identifier for the current persona, if any.</p>
   */

  private Invid personaInvid;

  /**
   * <p>A reference to our current persona object.  We save this so we
   * can look up owner groups and what not more quickly.  An end-user
   * logged in without any extra privileges will have a null
   * personaObj value.</p>
   */

  private DBObject personaObj = null;

  /**
   * <p>When did we last check our persona permissions?</p>
   */

  private Date personaTimeStamp = null;

  /**
   * <p>When did we last update our local cache/summary of permissions records?</p>
   */

  private Date permTimeStamp;

  /**
   * <p>This variable stores the permission bits that are applicable
   * to objects that the current persona has ownership privilege over.
   * This matrix is always a permissive superset of {@link
   * arlut.csd.ganymede.server.DBPermissionManager#defaultPerms
   * defaultPerms}.</p>
   */

  private PermMatrix personaPerms;

  /**
   * <p>This variable stores the permission bits that are applicable
   * to generic objects not specifically owned by this persona.</p>
   *
   * <p>Each permission object in the Ganymede database includes
   * permissions as apply to objects owned by the persona and as apply
   * to objects not owned by the persona.</p>
   *
   * <p>This variable holds the union of the 'as apply to objects not
   * owned by the persona' matrices across all permissions objects
   * that apply to the current persona.</p>
   */

  private PermMatrix defaultPerms;

  /**
   * <p>This variable stores the permission bits that are applicable
   * to objects that the current persona has ownership privilege over
   * and which the current admin has permission to delegate to
   * subordinate roles.  This matrix is always a permissive superset
   * of {@link
   * arlut.csd.ganymede.server.DBPermissionManager#delegatableDefaultPerms
   * delegatableDefaultPerms}.</p>
   */

  private PermMatrix delegatablePersonaPerms;

  /**
   * <p>This variable stores the permission bits that are applicable to
   * generic objects not specifically owned by this persona and which
   * the current admin has permission to delegate to subordinate
   * roles.</p>
   *
   * <p>Each permission object in the Ganymede database includes
   * permissions as apply to objects owned by the persona and as apply
   * to objects not owned by the persona.</p>
   *
   * <p>This variable holds the union of the 'as apply to objects not
   * owned by the persona' matrices across all permissions objects
   * that apply to the current persona.</p>
   */

  private PermMatrix delegatableDefaultPerms;

  /**
   * <p>A reference to the Ganymede {@link
   * arlut.csd.ganymede.server.DBObject DBObject} storing our default
   * permissions, or the permissions that applies when we are not in
   * supergash mode and we do not have any ownership over the object
   * in question.</p>
   */

  private DBObject defaultObj;

  /**
   * <p>This variable is a vector of object references
   * ({@link arlut.csd.ganymede.common.Invid Invid}'s) to the owner groups
   * that the client has requested newly created objects be placed in.  While
   * this vector is not-null, any new objects created will be owned by the list
   * of ownergroups held here.</p>
   */

  private Vector<Invid> newObjectOwnerInvids = null;

  /**
   * <p>This variable is a vector of object references (Invid's) to the
   * owner groups that the client has requested the listing of objects
   * be restricted to.  That is, the client has requested that the
   * results of Queries and Dumps only include those objects owned by
   * owner groups in this list.  This feature is used primarily for
   * when a client is logged in with supergash privileges, but the
   * user wants to restrict the visibility of objects for convenience.</p>
   */

  private Vector<Invid> visibilityFilterInvids = null;

  /**
   * <p>Boolean semaphore to control whether we have already been
   * configured.</p>
   */

  private booleanSemaphore configured = new booleanSemaphore(false);

  /* -- */

  /**
   * Constructor
   */

  public DBPermissionManager(GanymedeSession gSession)
  {
    this.gSession = gSession;
    this.dbSession = gSession.getDBSession();
  }

  /**
   * Configures this DBPermissionManager for a privileged internal session.
   *
   * @param sessionName The name of this session, used for identifying
   * the task or server component that is using our GanymedeSession to
   * perform work in the server.  Must be unique among logged-in
   * sessions on the server and may not be null.
   */

  public DBPermissionManager configureInternalSession(String sessionName)
  {
    if (sessionName == null)
      {
        throw new IllegalArgumentException("sessionName may not be null");
      }

    if (configured.set(true))
      {
        throw new IllegalStateException("Reconfiguring a DBPermissionManager is not allowed.");
      }

    this.sessionName = sessionName;

    username = null;
    userInvid = null;

    supergashMode = true;
    beforeversupergash = true;

    updatePerms(true);

    return this;
  }

  /**
   * Configures this DBPermissionManager for a remote user session.
   *
   * @param userObject A DBObject describing the user logged in, or
   * null if the user is logging in with a non-user-linked persona
   * object (supergash, monitor, etc.)
   *
   * @param personaObject A DBObject describing the Admin Persona
   * logged in.  May be null if the user is logged in only with his
   * unprivileged end-user account.
   *
   * @param sessionName The name of this session, used for
   * identifying the task or server component that is using our
   * GanymedeSession to perform work in the server.  Must be unique
   * among logged-in sessions in the server and may not be null.
   */

  public DBPermissionManager configureClientSession(DBObject userObject, DBObject personaObject, String sessionName)
  {
    if (sessionName == null)
      {
        throw new IllegalArgumentException("sessionLabel may not be null");
      }

    if (configured.set(true))
      {
        throw new IllegalStateException("Reconfiguring a DBPermissionManager is not allowed.");
      }

    this.sessionName = sessionName;

    if (userObject != null)
      {
        userInvid = userObject.getInvid();
        username = userObject.getLabel();
      }
    else
      {
        userInvid = null;
      }

    if (personaObject != null)
      {
        personaInvid = personaObject.getInvid();
        personaName = personaObject.getLabel();

        if (username == null)
          {
            username = personaName; // for supergash, monitor
          }
      }
    else
      {
        personaInvid = null;    // shouldn't happen
      }

    updatePerms(true);

    return this;
  }

  /**
   * Returns true if the session is operating with unrestricted 'root'
   * level privileges.
   */

  public boolean isSuperGash()
  {
    return supergashMode;
  }

  /**
   * Returns true if the session has any kind of privileges beyond the
   * default end-user privileges.
   */

  public boolean isPrivileged()
  {
    return personaName != null || isSuperGash();
  }

  /**
   * Returns true if the session is a standard user with currently
   * elevated privileges.
   */

  public boolean isElevated()
  {
    return userInvid != null;
  }

  /**
   * <p>This method returns the name of the user that is logged into
   * this session, or null if this session was created by a Ganymede
   * server task or other internal process.</p>
   *
   * <p>If supergash is using this session, this method will return
   * supergash as well, even though technically supergash isn't a
   * user.</p>
   */

  public String getUserName()
  {
    return username;
  }

  /**
   * <p>Convenience method to get access to this session's user invid.</p>
   *
   * <p>May be null if supergash, monitor, or Ganymede server task or
   * internal process is running the session.</p>
   */

  public Invid getUserInvid()
  {
    return userInvid;
  }

  /**
   * <p>Convenience method to get access to this session's UserBase
   * instance.</p>
   *
   * <p>May be null if supergash, monitor, or Ganymede server task or
   * internal process is running the session.</p>
   */

  public DBObject getUser()
  {
    if (userInvid != null)
      {
        // okay to use session.viewDBObject() here, because getUser()
        // is only used for internal purposes, and we don't need or
        // want to do permissions checking

        return dbSession.viewDBObject(userInvid);
      }

    return null;
  }

  /**
   * <p>This method returns the name of the persona who is active.
   * May be null or empty if we have an end-user who is logged in with
   * no elevated persona privileges.</p>
   */

  public String getPersonaName()
  {
    return personaName;
  }

  /**
   * <p>Convenience method to get access to this session's persona invid.</p>
   */

  public Invid getPersonaInvid()
  {
    return personaInvid;
  }

  /**
   * <p>This method gives access to the DBObject for the administrator's
   * persona record, if any.  This is used by
   * {@link arlut.csd.ganymede.server.DBSession DBSession} to get the
   * label for the administrator for record keeping.</p>
   */

  public DBObject getPersona()
  {
    return personaObj;
  }

  /**
   * <p>Returns the session name assigned to the GanymedeSession that
   * owns us.  Must be unique among all logged in sessions.</p>
   *
   * <p>getSessionName() will never return a null value.</p>
   */

  public String getSessionName()
  {
    return sessionName;
  }

  /**
   * <p>This method returns the name of the user who is active
   * (including supergash or monitor for the non-user-linked
   * personas), or the name of the internal Ganymede task or process
   * that is running the session if no user is attached to this
   * session.</p>
   */

  public String getBaseIdentity()
  {
    if (username != null && !username.equals(""))
      {
        return username;
      }
    else
      {
        return sessionName;
      }
  }

  /**
   * <p>This method returns the name of the persona who is active, the
   * raw user name if no persona privileges have been assumed, or the
   * name of the internal Ganymede task or process that is running the
   * session if no user is attached to this session.</p>
   */

  public String getIdentity()
  {
    if (personaName == null || personaName.equals(""))
      {
        if (username != null && !username.equals(""))
          {
            return username;
          }
        else
          {
            return sessionName;
          }
      }
    else
      {
        return personaName;
      }
  }

  /**
   * <p>This method returns the Invid of the user who logged in, or
   * the non-user-linked persona (supergash, monitor) if there was no
   * underlying user attached to the persona.</p>
   *
   * <p>May return null if this session is being run by a Ganymede
   * server task or internal process.</p>
   */

  public Invid getIdentityInvid()
  {
    if (userInvid != null)
      {
        return userInvid;
      }

    return personaInvid;
  }

  /**
   * <p>Returns a Vector of Invids containing user and persona Invids
   * for the GanymedeSession that this DBPermissionManager is attached
   * to.</p>
   *
   * <p>May return an empty Vector if this session is being run by a
   * Ganymede server task or internal process.</p>
   */

  public Vector<Invid> getIdentityInvids()
  {
    Vector<Invid> ids = new Vector<Invid>();

    if (userInvid != null)
      {
        ids.add(userInvid);
      }

    if (personaInvid != null)
      {
        ids.add(personaInvid);
      }

    return ids;
  }

  /**
   * <p>Returns the email address that should be used in the 'From:'
   * field of mail sent by the GanymedeSession which owns this
   * DBPermissionManager.
   */

  public String getIdentityReturnAddress()
  {
    String returnAddr;

    // do we have a real user name, or a persona name?

    if (username == null || userInvid == null)
      {
        // local server process or supergash/monitor

        returnAddr = Ganymede.returnaddrProperty;
      }
    else
      {
        if (username.indexOf(':') == -1)
          {
            // real username, save it as is

            returnAddr = username;
          }
        else
          {
            // persona, extract the user's name out of it
            returnAddr = username.substring(0, username.indexOf(':'));
          }

        String mailsuffix = System.getProperty("ganymede.defaultmailsuffix");

        if (mailsuffix != null)
          {
            returnAddr += mailsuffix;
          }
      }

    return returnAddr;
  }

  /**
   * <p>Returns the Invid of the admin persona (or user, if running
   * with unelevated privileges) who is responsible for actions taken
   * by the containing GanymedeSession.</p>
   */

  public Invid getResponsibleInvid()
  {
    if (personaInvid != null)
      {
        return personaInvid;
      }

    return userInvid;
  }

  /**
   * <p>This method returns a list of personae names available to the
   * user logged in.</p>
   */

  public Vector<String> getAvailablePersonae()
  {
    DBObject user;
    Vector<String> results;
    InvidDBField inv;

    /* -- */

    user = getUser();

    if (user == null)
      {
        return null;
      }

    results = new Vector<String>();

    inv = (InvidDBField) user.getField(SchemaConstants.UserAdminPersonae);

    if (inv != null)
      {
        // it's okay to loop on this field since we should be looking
        // at a DBObject and not a DBEditObject

        for (int i = 0; i < inv.size(); i++)
          {
            Invid invid = (Invid) inv.getElementLocal(i);

            try
              {
                results.add(dbSession.viewDBObject(invid).getLabel());
              }
            catch (NullPointerException ex)
              {
              }
          }
      }

    results.add(user.getLabel()); // add their 'end-user' persona

    return results;
  }

  public PermMatrix getPersonaPerms()
  {
    return personaPerms;
  }

  public PermMatrix getDefaultPerms()
  {
    return defaultPerms;
  }

  public PermMatrix getDelegatablePersonaPerms()
  {
    return delegatablePersonaPerms;
  }

  public PermMatrix getDelegatableDefaultPerms()
  {
    return delegatableDefaultPerms;
  }

  /**
   * <p>This method is used to select an admin persona, changing the
   * permissions that the user has and the objects that are accessible
   * in the database.</p>
   */

  public boolean selectPersona(String newPersona, String password)
  {
    DBObject
      user,
      personaObject = null;

    InvidDBField inv;
    Invid invid;
    PasswordDBField pdbf;

    /* -- */

    user = getUser();

    if (user == null)
      {
        // they may be the special supergash account, but they can't
        // change persona

        return false;
      }

    // if they are selecting their base username, go ahead and clear
    // out the persona privs and return true

    if (user.getLabel().equals(newPersona))
      {
        if (password != null)
          {
            pdbf = (PasswordDBField) user.getField(SchemaConstants.UserPassword);

            if (pdbf == null || !pdbf.matchPlainText(password))
              {
                return false;
              }
          }

        // the GUI client will close transactions first, but since we
        // might not be working on behalf of the GUI client, let's
        // make sure

        if (dbSession.editSet != null)
          {
            String description = dbSession.editSet.description;
            boolean interactive = dbSession.editSet.isInteractive();

            // close the existing transaction

            try
              {
                gSession.abortTransaction();

                // open a new one with the same description and
                // interactivity

                gSession.openTransaction(description, interactive);
              }
            catch (NotLoggedInException ex)
              {
                throw new RuntimeException(ex);
              }
          }

        personaObject = null;
        this.personaInvid = null;
        this.personaName = null;
        this.visibilityFilterInvids = null;
        this.username = user.getLabel(); // in case they logged in directly as an admin account

        updatePerms(true);

        gSession.resetAdminEntry(); // null our admin console cache
        gSession.setLastEvent("selectPersona: " + newPersona);

        return true;
      }

    // ok, we need to find out persona they are trying to switch to

    inv = (InvidDBField) user.getField(SchemaConstants.UserAdminPersonae);

    // it's okay to loop on this field since we should be looking at a
    // DBObject and not a DBEditObject

    for (int i = 0; i < inv.size(); i++)
      {
        invid = (Invid) inv.getElementLocal(i);

        // it's okay to use the faster viewDBObject() here, because we
        // are always going to be doing this for internal purposes

        personaObject = dbSession.viewDBObject(invid);

        if (!personaObject.getLabel().equals(newPersona))
          {
            personaObject = null;
          }
        else
          {
            break;
          }
      }

    if (personaObject == null)
      {
        // "Couldn''t find persona {0} for user: {1}"
        Ganymede.debug(ts.l("selectPersona.no_persona", newPersona, this.username));
        return false;
      }

    pdbf = (PasswordDBField) personaObject.getField(SchemaConstants.PersonaPasswordField);

    if (pdbf != null && pdbf.matchPlainText(password))
      {
        // "User {0} switched to persona {1}."
        Ganymede.debug(ts.l("selectPersona.switched", this.username, newPersona));

        this.personaName = personaObject.getLabel();

        // the GUI client will close transactions first, but since we
        // might not be working on behalf of the GUI client, let's
        // make sure

        if (dbSession.editSet != null)
          {
            String description = dbSession.editSet.description;
            boolean interactive = dbSession.editSet.isInteractive();

            try
              {
                // close the existing transaction

                gSession.abortTransaction();

                // open a new one with the same description and
                // interactivity

                gSession.openTransaction(description, interactive);
              }
            catch (NotLoggedInException ex)
              {
                throw new RuntimeException(ex);
              }
          }

        this.personaInvid = personaObject.getInvid();
        this.username = user.getLabel(); // in case they logged in directly as an admin account
        this.visibilityFilterInvids = null;

        updatePerms(true);

        gSession.resetAdminEntry(); // null our admin console cache
        gSession.setLastEvent("selectPersona: " + newPersona);
        return true;
      }

    return false;
  }

  /**
   * <p>This method returns a QueryResult of owner groups that the
   * current persona has access to.  This list is the transitive
   * closure of the list of owner groups in the current persona.  That
   * is, the list includes all the owner groups in the current persona
   * along with all of the owner groups those owner groups own, and so
   * on.</p>
   */

  public QueryResult getAvailableOwnerGroups()
  {
    Query q;
    QueryResult result = new QueryResult();
    QueryResult fullOwnerList;
    Vector<Invid> alreadySeen = new Vector<Invid>();
    Invid inv;

    /* -- */

    if (!isPrivileged())
      {
        return result;          // End users don't have any owner group access
      }

    q = new Query(SchemaConstants.OwnerBase);
    q.setFiltered(false);

    try
      {
        fullOwnerList = gSession.query(q);
      }
    catch (NotLoggedInException ex)
      {
        throw new RuntimeException(ex);
      }

    // if we're in supergash mode, return a complete list of owner groups

    if (isSuperGash())
      {
        return fullOwnerList;
      }

    // otherwise, we've got to do a very little bit of legwork

    for (int i = 0; i < fullOwnerList.size(); i++)
      {
        alreadySeen.clear();

        inv = fullOwnerList.getInvid(i);

        if (recursePersonaMatch(inv, alreadySeen))
          {
            result.addRow(inv, dbSession.viewDBObject(inv).getLabel(), false);
          }
      }

    return result;
  }

  /**
   * <p>This method may be used to set the owner groups of any objects
   * created hereafter.</p>
   *
   * @param ownerInvids a Vector of Invid objects pointing to
   * ownergroup objects.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal setDefaultOwner(Vector<Invid> ownerInvids)
  {
    Vector<Invid> tmpInvids;

    /* -- */

    if (ownerInvids == null)
      {
        newObjectOwnerInvids = null;
        return null;
      }

    tmpInvids = new Vector<Invid>();

    for (Invid ownerInvidItem: ownerInvids)
      {
        // this check is actually redundant, as the InvidDBField link
        // logic would catch such for us, but it makes a nice couplet
        // with the getNum() check below, so I'll leave it here.

        if (ownerInvidItem.getType() != SchemaConstants.OwnerBase)
          {
            return Ganymede.createErrorDialog(ts.l("setDefaultOwner.error_title"),
                                              ts.l("setDefaultOwner.error_text"));
          }

        // we don't want to explicitly place the object in
        // supergash.. all objects are implicitly availble to
        // supergash, no sense in making a big deal of it.

        // this is also redundant, since DBSession.createDBObject()
        // will filter this out as well.  Err.. I probably should
        // have faith in DBSession.createDBObject() and take this
        // whole loop out, but I'm gonna leave it for now.

        if (ownerInvidItem.getNum() == SchemaConstants.OwnerSupergash)
          {
            continue;
          }

        tmpInvids.add(ownerInvidItem);
      }

    if (!supergashMode && !isMemberAll(tmpInvids))
      {
        return Ganymede.createErrorDialog(ts.l("setDefaultOwner.error_title"),
                                          ts.l("setDefaultOwner.error_text2"));
      }
    else
      {
        newObjectOwnerInvids = tmpInvids;
        gSession.setLastEvent("setDefaultOwner");
        return null;
      }
  }

  /**
   * <p>Returns a Vector of Invids of the owner groups that should be
   * made owners of a newly created object by the GanymedeSession
   * owned by this DBPermissionManager.</p>
   *
   * <p>If an admin has authority over more than one owner group and
   * they have not previously specified the collection of owner groups
   * that they want to assign to new objects, we'll just pick the
   * first one in the list.</p>
   */

  public Vector<Invid> getNewOwnerInvids()
  {
    if (newObjectOwnerInvids != null)
      {
        return newObjectOwnerInvids;
      }
    else
      {
        Vector<Invid> ownerInvids = new Vector<Invid>();

        // supergash is allowed to create objects with no owners,
        // so we won't worry about what supergash might try to do.

        if (!isSuperGash())
          {
            QueryResult ownerList = getAvailableOwnerGroups();

            if (ownerList.size() > 0)
              {
                // If we're interactive, the client really should hav
                // helped us out by prompting the user for their
                // preferred default owner list, but if we are talking
                // to a custom client, this might not be the case, in
                // which case we'll just pick the first owner group we
                // can put it into and put it there.
                //
                // The client can always manually set the owner group
                // in a created object after we return it, of course.

                ownerInvids.add(ownerList.getInvid(0));
              }
          }

        return ownerInvids;
      }
  }

  /**
   * <p>This method may be used to cause the server to pre-filter any object
   * listing to only show those objects directly owned by owner groups
   * referenced in the ownerInvids list.  This filtering will not restrict
   * the ability of the client to directly view any object that the client's
   * persona would normally have access to, but will reduce clutter and allow
   * the client to present the world as would be seen by administrator personas
   * with just the listed ownerGroups accessible.</p>
   *
   * <p>This method cannot be used to grant access to objects that are
   * not accessible by the client's adminPersona.</p>
   *
   * <p>Calling this method with ownerInvids set to null will turn off the filtering.</p>
   *
   * @param ownerInvids a Vector of Invid objects pointing to ownergroup objects.
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public synchronized ReturnVal filterQueries(Vector<Invid> ownerInvids)
  {
    if (ownerInvids == null || ownerInvids.size() == 0)
      {
        visibilityFilterInvids = null;
        return null;
      }

    if (!supergashMode && !isMemberAll(ownerInvids))
      {
        return Ganymede.createErrorDialog(ts.l("filterQueries.error"),
                                          ts.l("setDefaultOwner.error_text2"));
      }
    else
      {
        visibilityFilterInvids = ownerInvids;
        gSession.setLastEvent("filterQueries");
        return null;
      }
  }

  //  Database operations

  /**
   * <p>Returns a serialized representation of the basic category
   * and base structure on the server.</p>
   *
   * <p>This method is synchronized to avoid any possible deadlock
   * between DBStore and GanymedeSession, as the CategoryTransport
   * constructor calls other synchronized methods on GanymedeSession.</p>
   *
   * @param hideNonEditables If true, the CategoryTransport returned
   * will only include those object types that are editable by the
   * client.
   *
   * @see arlut.csd.ganymede.rmi.Category
   */

  public synchronized CategoryTransport getCategoryTree(boolean hideNonEditables)
  {
    if (supergashMode)
      {
        // XXX CACHE WARNING! XXX

        // All sessions with supergash privileges can use the cached
        // full category tree transport object.. we'll build it here
        // if we need to.

        if (Ganymede.catTransport == null)
          {
            // pass Ganymede.internalSession so that the master
            // CategoryTransport object will correctly grant
            // object creation privs for all object types

            Ganymede.catTransport = Ganymede.db.rootCategory.getTransport(Ganymede.internalSession, true);
          }

        if (debug)
          {
            System.err.println("getCategoryTree(): returning system's complete category tree");
          }

        return Ganymede.catTransport;
      }
    else
      {
        // not in supergash mode.. download a subset of the category tree to the user

        CategoryTransport transport = Ganymede.db.rootCategory.getTransport(gSession, hideNonEditables);

        if (debug)
          {
            System.err.println("getCategoryTree(): generated custom category tree");
          }

        if (false)
          {
            System.err.println("%%% Printing PersonaPerms");
            PermissionMatrixDBField.debugdump(personaPerms);

            System.err.println("%%% Printing DefaultPerms");
            PermissionMatrixDBField.debugdump(defaultPerms);
          }

        return transport;
      }
  }

  /**
   * <p>Returns a serialized representation of the object types
   * defined on the server.  This BaseListTransport object
   * will not include field information.  The client is
   * obliged to call getFieldTemplateVector() on any
   * bases that it needs field information for.</p>
   *
   * <p>This method is synchronized to avoid any possible deadlock
   * between DBStore and GanymedeSession, as the BaseListTransport
   * constructor calls other synchronized methods on GanymedeSession.</p>
   *
   * @see arlut.csd.ganymede.common.BaseListTransport
   */

  public synchronized BaseListTransport getBaseList()
  {
    if (supergashMode && Ganymede.baseTransport != null)
      {
        return Ganymede.baseTransport;
      }

    BaseListTransport transport = new BaseListTransport();

    // *sync* on DBStore, this GanymedeSession

    // we sync on Ganymede.db to make sure that no one adds or deletes
    // any object bases while we're creating our BaseListTransport.
    // We could use the loginSemaphore, but that would be a bit heavy
    // for our purposes here.

    synchronized (Ganymede.db)
      {
        for (DBObjectBase base: Ganymede.db.bases())
          {
            base.addBaseToTransport(transport, null);
          }
      }

    if (supergashMode)
      {
        Ganymede.baseTransport = transport;
      }

    return transport;
  }

  /**
   * <p>This method applies this GanymedeSession's current owner filter
   * to the given QueryResult &lt;qr&gt; and returns a QueryResult
   * with any object handles that are not matched by the filter
   * stripped.</p>
   *
   * <p>If the submitted QueryResult &lt;qr&gt; is null, filterQueryResult()
   * will itself return null.</p>
   *
   * <p>NB: This method requires no external synchronization</p>
   */

  public QueryResult filterQueryResult(QueryResult qr)
  {
    if (qr == null)
      {
        return null;
      }

    QueryResult result = new QueryResult(qr.isForTransport());

    /* -- */

    Vector<ObjectHandle> handles = qr.getHandles();

    for (ObjectHandle handle: handles)
      {
        Invid invid = handle.getInvid();

        if (invid != null)
          {
            DBObject obj = dbSession.viewDBObject(invid);

            if (filterMatch(obj))
              {
                result.addRow(handle);
              }
          }
      }

    return result;
  }

  // **
  // the following are the non-exported permissions management
  // **

  /**
   * <p>This method finds the ultimate owner of an embedded object</p>
   */

  DBObject getContainingObj(DBObject object)
  {
    return dbSession.getContainingObj(object);
  }

  /**
   * <p>This method takes the administrator's current
   * persona, considers the owner groups the administrator
   * is a member of, checks to see if the object is owned
   * by that group, and determines the appropriate permission
   * bits for the object.  getPerm() will OR any proprietary
   * ownership bits with the default permissions to give
   * an appropriate result.</p>
   */

  public PermEntry getPerm(DBObject object)
  {
    boolean doDebug = permsdebug && object.getInvid().getType() == 267;
    boolean useSelfPerm = false;
    PermEntry result;

    /* -- */

    if (object == null)
      {
        return null;
      }

    if (supergashMode)
      {
        return PermEntry.fullPerms;
      }

    // find the top-level object if we were passed an embedded object

    if (doDebug)
      {
        System.err.println("DBPermissionManager.getPerm(" + object + ")");
      }

    object = getContainingObj(object);

    // does this object type have an override?

    result = object.getBase().getObjectHook().permOverride(gSession, object);

    if (result != null)
      {
        if (doDebug)
          {
            System.err.println("getPerm(): found an object override, returning " + result);
          }

        return result;
      }

    // no override.. do we have an expansion?

    result = object.getBase().getObjectHook().permExpand(gSession, object);

    if (result == null)
      {
        result = PermEntry.noPerms;
      }

    // make sure we have personaPerms up to date

    updatePerms(false);         // *sync*

    // if we are operating on behalf of an end user and the object in
    // question happens to be that user's record, we'll act as if we
    // own ourself.  We'll then wind up with the default permission
    // object's objects owned privs.

    useSelfPerm = (userInvid != null) && userInvid.equals(object.getInvid());

    // If we aren't editing ourselves, go ahead and check to see
    // whether the custom logic for this object type wants to grant
    // ownership of this object.

    if (!useSelfPerm && object.getBase().getObjectHook().grantOwnership(gSession, object))
      {
        if (doDebug)
          {
            System.err.println("getPerm(): grantOwnership() returned true");
          }

        useSelfPerm = true;
      }

    // If the current persona owns the object, look to our
    // personaPerms to get the permissions applicable, else
    // look at the default perms

    if (useSelfPerm || personaMatch(object))
      {
        if (doDebug)
          {
            System.err.println("getPerm(): personaMatch or useSelfPerm returned true");
          }

        PermEntry temp = personaPerms.getPerm(object.getTypeID());

        if (doDebug)
          {
            System.err.println("getPerm(): personaPerms.getPerm(" + object + ") returned " + temp);

            System.err.println("%%% Printing PersonaPerms");
            PermissionMatrixDBField.debugdump(personaPerms);
          }

        PermEntry val = result.union(temp);

        if (doDebug)
          {
            System.err.println("getPerm(): returning " + val);
          }

        return val;
      }
    else
      {
        if (doDebug)
          {
            System.err.println("getPerm(): personaMatch and useSelfPerm returned false");
          }

        PermEntry temp = defaultPerms.getPerm(object.getTypeID());

        if (doDebug)
          {
            System.err.println("getPerm(): defaultPerms.getPerm(" + object + ") returned " + temp);

            System.err.println("%%% Printing DefaultPerms");
            PermissionMatrixDBField.debugdump(defaultPerms);
          }

        PermEntry val = result.union(temp);

        if (doDebug)
          {
            System.err.println("getPerm(): returning " + val);
          }

        return val;
      }
  }

  /**
   * <p>This method takes the administrator's current
   * persona, considers the owner groups the administrator
   * is a member of, checks to see if the object is owned
   * by that group, and determines the appropriate permission
   * bits for the field in the object.</p>
   *
   * <p>This method duplicates the logic of {@link
   * arlut.csd.ganymede.server.DBPermissionManager#getPerm(arlut.csd.ganymede.server.DBObject)
   * getPerm(object)} internally for efficiency.  This method is
   * called <B>quite</B> a lot in the server, and has been tuned
   * to use the pre-calculated DBPermissionManager
   * {@link arlut.csd.ganymede.server.DBPermissionManager#defaultPerms defaultPerms}
   * and {@link arlut.csd.ganymede.server.DBPermissionManager#personaPerms personaPerms}
   * objects which cache the effective permissions for fields in the
   * Ganymede {@link arlut.csd.ganymede.server.DBStore DBStore} for the current
   * persona.</p>
   */

  public PermEntry getPerm(DBObject object, short fieldId)
  {
    // if this is true, the object was considered to be owned.

    boolean objectIsOwned = false;

    // reference to which PermMatrix we use to look up permissions..
    // that for objects we own, or that for objects we don't.

    PermMatrix applicablePerms = null;

    // reference to custom pseudostatic DBEditObject handler
    // registered with the object's type, if any

    DBEditObject objectHook;

    // object permissions resulting from DBEditObject subclass
    // customization

    PermEntry overrideObjPerm = null;
    PermEntry expandObjPerm = null;

    // field permissions resulting from DBEditObject subclass
    // customization

    PermEntry overrideFieldPerm = null;
    PermEntry expandFieldPerm = null;

    // and our results

    PermEntry objectPerm = null;
    PermEntry fieldPerm = null;

    /* -- */

    if (permsdebug)
      {
        System.err.println("Entering DBPermissionManager.getPerm(" + object + "," + fieldId + ")");
      }

    if (supergashMode)
      {
        return PermEntry.fullPerms;
      }

    objectHook = object.getBase().getObjectHook();

    // check for permissions overrides or expansions from the object's
    // custom plug-in class.. all of these objectHook calls will
    // return null if there is no customization

    overrideFieldPerm = objectHook.permOverride(gSession, object, fieldId);

    if (overrideFieldPerm == null)
      {
        expandFieldPerm = objectHook.permExpand(gSession, object, fieldId);
      }

    overrideObjPerm = objectHook.permOverride(gSession, object);

    if (overrideObjPerm == null)
      {
        expandObjPerm = objectHook.permExpand(gSession, object);
      }

    // make sure we have personaPerms up to date

    updatePerms(false);         // *sync*

    // embedded object ownership is determined by the top-level object

    DBObject containingObj = getContainingObj(object);

    if ((userInvid != null && userInvid.equals(containingObj.getInvid())) ||
        objectHook.grantOwnership(gSession, object) ||
        objectHook.grantOwnership(gSession, containingObj) ||
        personaMatch(containingObj))
      {
        if (permsdebug)
          {
            System.err.println("DBPermissionManager.getPerm(" + object + "," + fieldId + ") choosing persona perms");
          }

        objectIsOwned = true;

        applicablePerms = personaPerms; // superset of defaultPerms
      }
    else
      {
        if (permsdebug)
          {
            System.err.println("DBPermissionManager.getPerm(" + object + "," + fieldId + ") choosing default perms");
          }

        applicablePerms = defaultPerms;
      }

    if (overrideObjPerm != null)
      {
        objectPerm = overrideObjPerm;
      }
    else
      {
        objectPerm = applicablePerms.getPerm(object.getTypeID());

        if (objectPerm == null)
          {
            if (permsdebug)
              {
                System.err.println("DBPermissionManager.getPerm(" + object + "," + fieldId + ") found no object perm");
              }

            objectPerm = PermEntry.noPerms;
          }

        objectPerm = objectPerm.union(expandObjPerm);
      }

    if (overrideFieldPerm != null)
      {
        if (permsdebug)
          {
            System.err.println("DBPermissionManager.getPerm(" + object + "," + fieldId + ") returning override perm");
          }

        // allow field create perm even if they don't have object create perm

        PermEntry temp = overrideFieldPerm.intersection(objectPerm);

        // add back the create bit if the field is creatable

        if (overrideFieldPerm.isCreatable())
          {
            temp = temp.union(PermEntry.getPermEntry(false, false, true, false));
          }

        return temp;
      }

    fieldPerm = applicablePerms.getPerm(object.getTypeID(), fieldId);

    // if we don't have an explicit permissions entry for the field,
    // return the effective one for the object.

    if (fieldPerm == null)
      {
        if (permsdebug)
          {
            System.err.println("DBPermissionManager.getPerm(" + object + "," + fieldId + ") returning object perms");
          }

        // if we are returning permissions for the owner list field
        // and the object in question has not been granted owner ship
        // privileges, make sure that we don't allow editing of the
        // owner list field, which could be used to make the object
        // owned, and thus gain privileges

        // likewise, we don't want to allow non-privileged end users
        // to edit the owner list field at all.

        if (fieldId == SchemaConstants.OwnerListField &&
            (!objectIsOwned || personaObj == null))
          {
            return objectPerm.intersection(PermEntry.viewPerms);
          }

        // nor do we want anyone to be able to modify the historical
        // fields

        if (fieldId == SchemaConstants.CreationDateField ||
            fieldId == SchemaConstants.CreatorField ||
            fieldId == SchemaConstants.ModificationDateField ||
            fieldId == SchemaConstants.ModifierField)
          {
            return objectPerm.intersection(PermEntry.viewPerms);
          }

        return objectPerm;
      }

    // we want to return the more restrictive permissions of the
    // object's permissions and the field's permissions.. we can never
    // look at a field in an object we can't look at.

    if (permsdebug)
      {
        System.err.println("DBPermissionManager.getPerm(" + object + "," + fieldId + ") returning field perms");

        System.err.println("fieldPerm = " + fieldPerm);
        System.err.println("objectPerm = " + objectPerm);
        System.err.println("expandFieldPerm = " + expandFieldPerm);
      }

    // we never want to allow users who don't own an object to edit
    // the object ownership list, nor do we ever want to allow
    // non-privileged end users to edit the ownership list.

    // nor do we allow editing the historical fields

    if ((fieldId == SchemaConstants.OwnerListField &&
        (!objectIsOwned || personaObj == null)) ||
        (fieldId == SchemaConstants.CreationDateField ||
         fieldId == SchemaConstants.CreatorField ||
         fieldId == SchemaConstants.ModificationDateField ||
         fieldId == SchemaConstants.ModifierField))
      {
        return fieldPerm.union(expandFieldPerm).intersection(objectPerm).intersection(PermEntry.viewPerms);
      }
    else
      {
        // allow field create perm even if they don't have object create perm

        PermEntry temp = fieldPerm.union(expandFieldPerm);
        PermEntry temp2 = temp.intersection(objectPerm);

        // add back the create bit if the field is creatable

        if (temp.isCreatable())
          {
            temp = temp2.union(PermEntry.getPermEntry(false, false, true, false));
          }

        return temp;
      }
  }

  /**
   * <p>This method returns the generic permissions for a object type.
   * This is currently used primarily to check to see whether a user
   * has privileges to create an object of a specific type.</p>
   *
   * <p>This method takes the administrator's current persona's set of
   * appropriate permission matrices, does a binary OR'ing of the
   * permission bits for the given base, and returns the effective
   * permission entry.</p>
   *
   * @param includeOwnedPerms If true, this method will return the
   * permission that the current persona would have for an object that
   * was owned by the current persona.  If false, this method will
   * return the default permissions that apply to objects not owned by
   * the persona.
   */

  PermEntry getPerm(short baseID, boolean includeOwnedPerms)
  {
    PermEntry result;

    /* -- */

    if (supergashMode)
      {
        return PermEntry.fullPerms;
      }

    updatePerms(false); // *sync* make sure we have personaPerms up to date

    // note that we can use personaPerms, since the persona's
    // base type privileges apply generically to objects of the
    // given type

    if (includeOwnedPerms)
      {
        result = personaPerms.getPerm(baseID);
      }
    else
      {
        result = defaultPerms.getPerm(baseID);
      }

    if (result == null)
      {
        return PermEntry.noPerms;
      }
    else
      {
        return result;
      }
  }

  /**
   * <p>This method returns the current persona's default permissions for
   * a base and field.  This permission applies generically to objects
   * that are not owned by this persona and to objects that are
   * owned.</p>
   *
   * <p>This is used by the {@link
   * arlut.csd.ganymede.server.GanymedeSession#dump(arlut.csd.ganymede.common.Query)
   * dump()} code to determine whether a field should be added to the
   * set of possible fields to be returned at the time that the dump
   * results are being prepared.</p>
   *
   * @param includeOwnedPerms If true, this method will return the permission
   * that the current persona would have for an object that was owned
   * by the current persona.  If false, this method will return the default
   * permissions that apply to objects not owned by the persona.
   */

  PermEntry getPerm(short baseID, short fieldID, boolean includeOwnedPerms)
  {
    PermEntry
      result = null;

    /* -- */

    if (supergashMode)
      {
        return PermEntry.fullPerms;
      }

    // make sure we have defaultPerms and personaPerms up to date

    updatePerms(false);         // *sync*

    // remember that personaPerms is a permissive superset of
    // defaultPerms

    if (includeOwnedPerms)
      {
        if (personaPerms != null)
          {
            result = personaPerms.getPerm(baseID, fieldID);

            // if we don't have a specific permissions entry for
            // this field, inherit the one for the base

            if (result == null)
              {
                result = personaPerms.getPerm(baseID);
              }
          }
      }
    else
      {
        result = defaultPerms.getPerm(baseID, fieldID);

        // if we don't have a specific permissions entry for
        // this field, inherit the one for the base

        if (result == null)
          {
            result = defaultPerms.getPerm(baseID);
          }
      }

    if (result == null)
      {
        return PermEntry.noPerms;
      }
    else
      {
        return result;
      }
  }

  /**
   * <p>This convenience method resets defaultPerms from the default
   * permission object in the Ganymede database.</p>
   */

  private void resetDefaultPerms()
  {
    PermissionMatrixDBField pField;

    /* -- */

    pField = (PermissionMatrixDBField) defaultObj.getField(SchemaConstants.RoleDefaultMatrix);

    if (pField == null)
      {
        defaultPerms = new PermMatrix();
        delegatableDefaultPerms = new PermMatrix();
      }
    else
      {
        defaultPerms = pField.getMatrix();

        // default permissions are implicitly delegatable

        delegatableDefaultPerms = pField.getMatrix();
      }
  }

  /**
   * <p>This non-exported (server-side only) method is used to
   * generate a comprehensive permissions matrix that applies to all
   * objects owned by the active persona for this user.</p>
   *
   * <p>This method is synchronized, and a whole lot of operations in the server
   * need to pass through here to ensure that the effective permissions for this
   * session haven't changed.  This method is designed to return very quickly
   * if permissions have not changed and forceUpdate is false.</p>
   *
   * @param forceUpdate If false, updatePerms() will do nothing if the Ganymede
   * permissions database has not been changed since updatePerms() was last
   * called in this DBPermissionManager.
   */

  private synchronized void updatePerms(boolean forceUpdate)
  {
    PermissionMatrixDBField permField;

    /* -- */

    if (forceUpdate)
      {
        // clear our time stamp to force an update further on

        personaTimeStamp = null;

        if (permsdebug)
          {
            System.err.println("updatePerms(true)");
          }
      }
    else
      {
        if (permsdebug)
          {
            System.err.println("updatePerms(false)");
          }
      }

    // first, make sure we have a copy of our default role
    // DBObject.. permTimeStamp is used to track this.

    if (permTimeStamp == null || !permTimeStamp.before(Ganymede.db.getObjectBase(SchemaConstants.RoleBase).getTimeStamp()))
      {
        defaultObj = dbSession.viewDBObject(SchemaConstants.RoleBase,
                                            SchemaConstants.RoleDefaultObj);

        if (defaultObj == null)
          {
            if (!Ganymede.firstrun)
              {
                Ganymede.debug(ts.l("updatePerms.no_default_perms"));
                throw new RuntimeException(ts.l("updatePerms.no_default_perms"));
              }
            else
              {
                // we're loading the database with a bulk-loader
                // linked to the server code.  Don't bother with
                // permissions

                supergashMode = true;
                return;
              }
          }

        // remember we update this so we don't need to do it again

        if (permTimeStamp == null)
          {
            permTimeStamp = new Date();
          }
        else
          {
            permTimeStamp.setTime(System.currentTimeMillis());
          }
      }

    // here's where we break out if nothing needs to be updated.. note
    // that we are testing personaTimeStamp here, not permTimeStamp.

    if (personaTimeStamp != null && personaTimeStamp.after(Ganymede.db.getObjectBase(SchemaConstants.PersonaBase).getTimeStamp()))
      {
        return;
      }

    if (permsdebug)
      {
        System.err.println("DBPermissionManager.updatePerms(): doing full permissions recalc for " +
                           (personaName == null ? username : personaName));
      }

    // persona invid may well have changed since we last loaded
    // personaInvid.. thus, we have to set it here.  Setting
    // personaObj is one of the primary reasons that other parts of
    // DBPermissionManager call updatePerms(), so don't mess with this. I
    // tried it, believe me, it didn't work.

    if (personaInvid != null)
      {
        personaObj = dbSession.viewDBObject(personaInvid);

        // if this session is editing the personaObj at the moment,
        // let's make a point of getting the version that isn't
        // checked out for editing so we don't risk inter-thread
        // interactions below

        if (personaObj instanceof DBEditObject)
          {
            personaObj = ((DBEditObject) personaObj).getOriginal();
          }
      }
    else
      {
        personaObj = null;
      }

    // if we're not locked into supergash mode (for internal sessions,
    // etc.), lets find out whether we're in supergash mode currently

    if (!beforeversupergash)
      {
        supergashMode = false;

        // ok, we're not supergash.. or at least, we might not be.  If
        // we are not currently active as a persona, personaPerms will
        // just be our defaultPerms

        if (personaObj == null)
          {
            // ok, we're not only not supergash, but we're also not
            // even a privileged persona.  Load defaultPerms and
            // personaPerms with the two permission matrices from the
            // default permission object.

            PermMatrix selfPerm = null;

            /* -- */

            resetDefaultPerms();

            permField = (PermissionMatrixDBField) defaultObj.getField(SchemaConstants.RoleMatrix);

            if (permField == null)
              {
                selfPerm = new PermMatrix();
              }
            else
              {
                // selfPerm is the permissions that the default
                // permission object has for objects owned.

                selfPerm = permField.getMatrix();

                if (selfPerm == null)
                  {
                    System.err.println(ts.l("updatePerms.null_selfperm"));
                  }
              }

            // personaPerms starts off as the union of permissions
            // applicable to all objects and all objects owned, from
            // the default permissions object.

            personaPerms = new PermMatrix(defaultPerms).union(selfPerm);
            delegatablePersonaPerms = new PermMatrix(defaultPerms).union(selfPerm);

            if (permsdebug)
              {
                System.err.println("DBPermissionManager.updatePerms(): returning.. no persona obj for " +
                                   (personaName == null ? username : personaName));
              }

            // remember the last time we pulled personaPerms / defaultPerms

            if (personaTimeStamp == null)
              {
                personaTimeStamp = new Date();
              }
            else
              {
                personaTimeStamp.setTime(System.currentTimeMillis());
              }

            return;
          }
        else
          {
            if (permsdebug)
              {
                System.err.println("updatePerms(): calculating new personaPerms");;
              }

            InvidDBField idbf = (InvidDBField) personaObj.getField(SchemaConstants.PersonaGroupsField);

            if (idbf != null)
              {
                Vector<Invid> vals = (Vector<Invid>) idbf.getValuesLocal();

                // *** Caution!  getValuesLocal() does not clone the
                // field's contents, and neither do we, for speed
                // reasons.  Since we get personaObj using
                // viewDBObject(), and we made sure that we got a
                // non-editable view, there's no chance that this loop
                // will get trashed by another thread messing with
                // vals.

                // DO NOT modify vals here!

                // loop over the owner groups this persona is a member
                // of, see if it includes the supergash owner group

                // it's okay to loop on this field since we should be looking
                // at a DBObject and not a DBEditObject

                for (Invid inv: vals)
                  {
                    if (inv.getNum() == SchemaConstants.OwnerSupergash)
                      {
                        supergashMode = true;
                        break;
                      }
                  }
              }

            if (!supergashMode)
              {
                // since we're not in supergash mode, we need to take
                // into account the operational privileges granted us
                // by the default permission matrix and all the
                // permission matrices associated with this persona.
                // Calculate the union of all of the applicable
                // permission matrices.

                // make sure that defaultPerms is reset to the
                // baseline, and initialize personaPerms from it.

                resetDefaultPerms();

                // Personas do not get the default 'objects-owned'
                // privileges for the wider range of objects under
                // their ownership.  Any special privileges granted to
                // admins over objects owned by them must be derived
                // from a non-default role.

                // they do get the default permissions that all users have
                // for non-owned objects, though.

                personaPerms = new PermMatrix(defaultPerms);

                // default permissions on non-owned are implicitly delegatable.

                delegatablePersonaPerms = new PermMatrix(defaultPerms);

                // now we loop over all permissions objects referenced
                // by our persona, or'ing in both the objects owned
                // permissions and default permissions to augment defaultPerms
                // and personaPerms.

                idbf = (InvidDBField) personaObj.getField(SchemaConstants.PersonaPrivs);

                if (idbf != null)
                  {
                    Vector<Invid> vals = (Vector<Invid>) idbf.getValuesLocal();

                    // *** Caution!  getValuesLocal() does not clone the field's contents..
                    //
                    // DO NOT modify vals here!

                    PermissionMatrixDBField pmdbf, pmdbf2;
                    Hashtable<String, PermEntry> pmdbfMatrix1 = null, pmdbfMatrix2 = null;
                    DBObject pObj;

                    /* -- */

                    // it's okay to loop on this field since we should be looking
                    // at a DBObject and not a DBEditObject

                    for (Invid inv: vals)
                      {
                        pObj = dbSession.viewDBObject(inv);

                        if (pObj != null)
                          {
                            if (permsdebug)
                              {
                                System.err.println("updatePerms(): unioning " + pObj + " into personaPerms and defaultPerms");

                                System.err.println("personaPerms is currently:");

                                PermissionMatrixDBField.debugdump(personaPerms);
                              }

                            // The default permissions for this
                            // administrator consists of the union of
                            // all default perms fields in all
                            // permission matrices for this admin
                            // persona.

                            // personaPerms is the union of all
                            // permissions applicable to objects that
                            // are owned by this persona

                            pmdbf = (PermissionMatrixDBField) pObj.getField(SchemaConstants.RoleMatrix);

                            if (pmdbf != null)
                              {
                                pmdbfMatrix1 = pmdbf.getInnerMatrix();
                              }

                            pmdbf2 = (PermissionMatrixDBField) pObj.getField(SchemaConstants.RoleDefaultMatrix);

                            if (pmdbf2 != null)
                              {
                                pmdbfMatrix2 = pmdbf2.getInnerMatrix();
                              }

                            if (permsdebug)
                              {
                                PermMatrix pm = new PermMatrix(pmdbfMatrix1);

                                System.err.println("updatePerms(): RoleMatrix for " + pObj + ":");

                                PermissionMatrixDBField.debugdump(pm);

                                pm = new PermMatrix(pmdbfMatrix2);

                                System.err.println("updatePerms(): RoleDefaultMatrix for " + pObj + ":");

                                PermissionMatrixDBField.debugdump(pm);
                              }

                            personaPerms = personaPerms.union(pmdbfMatrix1);

                            if (permsdebug)
                              {
                                System.err.println("updatePerms(): personaPerms after unioning with RoleMatrix is");

                                PermissionMatrixDBField.debugdump(personaPerms);
                              }

                            personaPerms = personaPerms.union(pmdbfMatrix2);

                            if (permsdebug)
                              {
                                System.err.println("updatePerms(): personaPerms after unioning with RoleDefaultMatrix is");

                                PermissionMatrixDBField.debugdump(personaPerms);
                              }

                            defaultPerms = defaultPerms.union(pmdbfMatrix2);

                            // we want to maintain our notion of
                            // delegatable permissions separately..

                            Boolean delegatable = (Boolean) pObj.getFieldValueLocal(SchemaConstants.RoleDelegatable);

                            if (delegatable != null && delegatable.booleanValue())
                              {
                                delegatablePersonaPerms = delegatablePersonaPerms.union(pmdbfMatrix1).union(pmdbfMatrix2);
                                delegatableDefaultPerms = delegatableDefaultPerms.union(pmdbfMatrix2);
                              }
                          }
                      }
                  }
              }
          }
      }

    // remember the last time we pulled personaPerms / defaultPerms

    if (personaTimeStamp == null)
      {
        personaTimeStamp = new Date();
      }
    else
      {
        personaTimeStamp.setTime(System.currentTimeMillis());
      }

    if (permsdebug)
      {
        System.err.println("DBPermissionManager.updatePerms(): finished full permissions recalc for " +
                           (personaName == null ? username : personaName));

        System.err.println("personaPerms = \n\n" + personaPerms);
        System.err.println("\n\ndefaultPerms = \n\n" + defaultPerms);
      }

    return;
  }

  /**
   * <p>Recursive helper method for personaMatch.. this method does a
   * depth first search up the owner tree for each Invid contained in
   * the invids Vector to see if the gSession's personaInvid is a
   * member of any of the containing owner groups.</p>
   *
   * @param owners A vector of invids pointing to OwnerBase objects
   * @param alreadySeen A vector of owner group Invid's that have
   * already been checked.  (For infinite loop avoidance).
   *
   * @return true if a match is found
   */

  public boolean recursePersonasMatch(Vector<Invid> owners, Vector<Invid> alreadySeen)
  {
    // *** It is critical that this method not modify the owners parameter passed
    // *** in, as it may be 'live' in a DBField.

    if (owners == null)
      {
        return false;
      }

    for (Invid owner: owners)
      {
        if (recursePersonaMatch(owner, alreadySeen))
          {
            return true;
          }
      }

    return false;
  }

  /**
   * <p>Recursive helper method for personaMatch.. this method does a
   * depth first search up the owner tree for the owner Invid to see
   * if the gSession's personaInvid is a member of any of the
   * containing owner groups.</p>
   *
   * @param owner An Invid pointing to an OwnerBase object
   * @param alreadySeen A vector of owner group Invid's that have
   * already been checked.  (For infinite loop avoidance).
   *
   * @return true if a match is found
   */

  public boolean recursePersonaMatch(Invid owner, Vector<Invid> alreadySeen)
  {
    DBObject ownerObj;
    InvidDBField inf;

    /* -- */

    if (owner == null)
      {
        throw new IllegalArgumentException("Null owner passed to recursePersonaMatch");
      }

    if (alreadySeen.contains(owner))
      {
        return false;
      }
    else
      {
        alreadySeen.add(owner);
      }

    ownerObj = dbSession.viewDBObject(owner);

    inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerMembersField);

    if (inf != null)
      {
        if (inf.getValuesLocal().contains(getPersonaInvid()))
          {
            return true;
          }
      }

    // didn't find, recurse up

    inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerListField);

    if (inf != null)
      {
        if (recursePersonasMatch((Vector<Invid>) inf.getValuesLocal(), alreadySeen))
          {
            return true;
          }
      }

    return false;
  }

  /**
   * <p>Returns true if the active persona has some sort of
   * owner/access relationship with the object in question through
   * its list of owner groups.</p>
   */

  public boolean personaMatch(DBObject obj)
  {
    Vector<Invid> owners;
    InvidDBField inf;
    boolean showit = false;

    /* -- */

    if (obj == null || personaInvid == null)
      {
        //      Ganymede.debug("Null obj/personaInvid");
        return false;
      }

    inf = (InvidDBField) obj.getField(SchemaConstants.OwnerListField); // owner or container

    if (inf == null)
      {
        owners = new Vector<Invid>();
      }
    else
      {
        // we have to clone the value returned to us by
        // getValuesLocal() because getValuesLocal() returns the
        // actual vector held in the field, and if we were to change
        // that, bad things would happen.

        owners = (Vector<Invid>) inf.getValuesLocal().clone();
      }

    // All owner group objects are considered to be self-owning.

    if (obj.getTypeID() == SchemaConstants.OwnerBase)
      {
        if (permsdebug)
          {
            System.err.println("** Augmenting owner group " + obj.getLabel() + " with self-ownership");
            showit = true;
          }

        if (!owners.contains(obj.getInvid()))
          {
            owners.add(obj.getInvid());
          }
      }

    // All admin personae are considered to be owned by the owner groups
    // that they are members of

    if (obj.getTypeID() == SchemaConstants.PersonaBase)
      {
        if (permsdebug)
          {
            System.err.print("** Augmenting admin persona " + obj.getLabel() + " ");
            showit = true;
          }

        InvidDBField inf2 = (InvidDBField) obj.getField(SchemaConstants.PersonaGroupsField);

        if (inf2 != null)
          {
            if (permsdebug)
              {
                Vector<Invid> values = (Vector<Invid>) inf2.getValuesLocal();

                // *** Caution!  getValuesLocal() does not clone the field's contents..
                //
                // DO NOT modify values here!

                // it's okay to loop on this field since we should be
                // looking at a DBObject and not a DBEditObject

                for (int i = 0; i < values.size(); i++)
                  {
                    if (i > 0)
                      {
                        System.err.print(", ");
                      }

                    System.err.print(values.get(i));
                  }

                System.err.println();
              }

            // we don't have to clone inf2.getValuesLocal() since union() will copy
            // the elements rather than just setting owners to the vector returned
            // by inf2.getValuesLocal() if owners is currently null.

            owners = arlut.csd.Util.VectorUtils.union(owners, (Vector<Invid>) inf2.getValuesLocal());
          }
        else
          {
            if (permsdebug)
              {
                System.err.println("<no owner groups in this persona>");
              }
          }
      }

    boolean result = recursePersonasMatch(owners, new Vector<Invid>());

    if (showit)
      {
        System.err.println("++ Result = " + result);
      }

    return result;
  }

  /**
   * <p>This helper method iterates through the owners vector and
   * checks to see if the current personaInvid is a member of all of
   * the groups through either direct membership or through membership
   * of an owning group.  This method depends on
   * recursePersonasMatch().</p>
   */

  private boolean isMemberAll(Vector<Invid> owners)
  {
    DBObject ownerObj;
    InvidDBField inf;
    boolean found;

    /* -- */

    if (owners == null)
      {
        return false;           // shouldn't happen in context
      }

    // loop over all the owner groups in the vector, make sure
    // that we are a valid member of each of these groups, either
    // directly or through being a member of a group that owns
    // one of these groups.

    for (Invid owner: owners)
      {
        found = false;  // yes, but what have you done for me _lately_?

        if (owner.getType() != SchemaConstants.OwnerBase)
          {
            Ganymede.debug("DBPermissionManager.isMemberAll(): bad invid passed " + owner.toString());
            return false;
          }

        ownerObj = dbSession.viewDBObject(owner);

        inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerMembersField);

        if (inf != null && inf.getValuesLocal().contains(personaInvid))
          {
            found = true;
          }
        else
          {
            // didn't find, recurse up

            inf = (InvidDBField) ownerObj.getField(SchemaConstants.OwnerListField);

            if (inf != null)
              {
                // using getValuesLocal() here is safe only because
                // recursePersonasMatch() never tries to modify the
                // owners value passed in.  Otherwise, we'd have to
                // clone the results from getValuesLocal().

                if (recursePersonasMatch(inf.getValuesLocal(), new Vector<Invid>()))
                  {
                    found = true;
                  }
              }
          }

        if (!found)
          {
            return false;
          }
      }

    return true;
  }

  /**
   * <p>This method returns true if the visibility filter vector
   * allows visibility of the object in question.  The visibility
   * vector works by direct ownership identity (i.e., no recursing
   * up), so it's a simple loop-di-loop.</p>
   */

  public boolean filterMatch(DBObject obj)
  {
    Vector<Invid> owners;
    InvidDBField inf;

    /* -- */

    if (obj == null)
      {
        return false;
      }

    if (visibilityFilterInvids == null || visibilityFilterInvids.size() == 0)
      {
        return true;            // no visibility restriction, go for it
      }

    inf = (InvidDBField) obj.getField(SchemaConstants.OwnerListField);

    if (inf == null)
      {
        return false;   // we have a restriction, but the object is only owned by supergash.. nope.
      }

    owners = (Vector<Invid>) inf.getValuesLocal();

    // *** Caution!  getValuesLocal() does not clone the field's contents..
    //
    // DO NOT modify owners here!

    if (owners == null)
      {
        return false;   // we shouldn't get here, but we don't really care either
      }

    // we've got the owners for this object.. now, is there any match between our
    // visibilityFilterInvids and the owners of this object?

    for (Invid tmpInvid: visibilityFilterInvids)
      {
        for (Invid secondInvid: owners)
          {
            if (tmpInvid.equals(secondInvid))
              {
                return true;
              }
          }
      }

    return false;
  }
}
