import ldap
import config
import utils
from flags import *


class ADLDAPConnection:

  def __init__(self):
    # The actual ldap connection to the server
    self.conn = None
  
  def connect(self):
    try:
      ldap.set_option(ldap.OPT_X_TLS_REQUIRE_CERT, ldap.OPT_X_TLS_NEVER)
      ldap.set_option(ldap.OPT_DEBUG_LEVEL, 0)

      self.conn = ldap.initialize(config.LDAP_URI)
      self.conn.protocol_version = ldap.VERSION3

      self.conn.simple_bind_s(config.LDAP_USERNAME, config.LDAP_PASSWORD)
    except ldap.LDAPError, e:
      # TODO: do something with this error. Catastrophic failure?
      raise


  def disconnect(self):
    del self.conn


  def delete(self, dn):
    return self.conn.delete(dn)
  delete = utils.require_connection(delete)



  def search(self, base=config.LDAP_BASEDN, filter="objectClass=*", scope=ldap.SCOPE_SUBTREE, attrlist=None):
    return self.conn.search_s(base, scope, filter, attrlist)
  search = utils.require_connection(search)



  def add(self, dn, attrs):
    return self.conn.add_s(dn, attrs)
  add = utils.require_connection(add)





class ADFrontend(ADLDAPConnection):

  def get_user(self, username):
    base = "cn=users," + config.LDAP_BASEDN
    filter = "cn=" + username

    result = self.search(base=base, filter=filter)
    if len(result) == 0:
      return None
    else:
      return result[0]


  def get_dn_for_user(self, username):
    return "cn=" + username + ",cn=users," + config.LDAP_BASEDN


  def user_exists(self, username):
    user = self.get_user(username)
    return (user is not None)


  def add_user(self, username, attrdict, password=None, accountControlFlag=None):
    assert not self.user_exists(username), \
           username + " can't be added since he already exists."

    assert "unicodePwd" not in attrdict.keys(), \
           "You can't set a password attribute at the time a user is created. " + \
           "Pass the password in as an additional, separate argument."

    assert "userAccountControl" not in attrdict.keys(), \
           "You can't set an account level attribute at the time a user is created. " + \
           "Pass the account control flag in as an additional, separate argument."

    dn = self.get_dn_for_user(username)

    uac = NORMAL_ACCOUNT + ACCOUNTDISABLE + PASSWD_NOTREQD + PASSWORD_EXPIRED
    attrs = [("objectclass", "user"), ("userAccountControl", str(uac)), ("sAMAccountName", username)]
    for key, value in attrdict.iteritems():
      attrs.append((key, value))

    self.add(dn, attrs)

    if password is not None:
      self.set_user_pw(username, password)

    if accountControlFlag is not None:
      self.set_user_account_control_flag(username, accountControlFlag)


  def set_user_account_control_flag(self, username, flag):
    assert self.user_exists(username), \
           username + "'s account flag can't be set since he doesn't exist."

    if type(flag) != type(""):
      flag = str(flag)

    self.set_user_attributes(username, {"userAccountControl": flag}, True)
    

  def set_user_pw(self, username, password):
    assert self.user_exists(username), \
           username + "'s password can't be set since he doesn't exist."

    dn = self.get_dn_for_user(username)

    pw = unicode("\"" + password + "\"", "iso-8859-1")
    pw = pw.encode("utf-16-le")

    self.set_user_attributes(username, {"unicodePwd": pw}, True)


  def set_user_attributes(self, username, attrdict, clobber=False):
    assert self.user_exists(username), \
           username + "'s attributes can't be set since he doesn't exist."

    dn = self.get_dn_for_user(username)

    # TODO: This is slow, since we're doing an ldap-modify for each
    # individual attribute instead of doing them all at once. What's
    # the elegant way to do this?
    for key, value in attrdict.iteritems():
      print "Setting", username+"'s", key, "to", value
      try:
        self.conn.modify_s(dn, [(ldap.MOD_ADD, key, value)])
      except ldap.TYPE_OR_VALUE_EXISTS:
        if clobber:
          self.conn.modify_s(dn, [(ldap.MOD_REPLACE, key, value)])
        else:
          raise


  def delete_user(self, username):
    assert self.user_exists(username), \
           username + " can't be deleted since he doesn't exist."

    dn = self.get_dn_for_user(username)
    self.conn.delete_s(dn)
