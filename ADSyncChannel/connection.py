import ldap
import config
import utils


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


  def user_exists(self, username):
    user = self.get_user(username)
    return (user is not None)


  def add_user(self, userobj):
    # TODO: Don't take a direct user object, take a username an a dictionary
    # of  attributes
    assert not self.user_exists(userobj.sAMAccountName), \
           userobj.sAMAccountName + " can't be added since he already exists."

    dn = "cn=" + userobj.sAMAccountName + ",cn=users," + config.LDAP_BASEDN

    # 512=Normal account, 32=Password not required
    attrs = [("objectclass", "user"), ("userAccountControl", "514")]
    for attr in userobj.__dict__.iterkeys():
      if attr[:2] != "__" and attr not in ["unicodePwd", "userAccountControl"]:
        attrs.append((attr, getattr(userobj, attr)))

    self.add(dn, attrs)
    

  def set_user_pw(self, username, password):
    assert self.user_exists(username), \
           username + "'s password can't be set since he doesn't exist."

    dn = "cn=" + username + ",cn=users," + config.LDAP_BASEDN
    pw = unicode("\"" + password + "\"", "iso-8859-1")
    pw = pw.encode("utf-16-le")
    self.conn.modify_s(dn, [(ldap.MOD_ADD, "unicodePwd", pw)])
    self.conn.modify_s(dn, [(ldap.MOD_REPLACE, "userAccountControl", "576")])


  def delete_user(self, username):
    assert self.user_exists(username), \
           username + " can't be deleted since he doesn't exist."

    dn = "cn=" + username + ",cn=users," + config.LDAP_BASEDN

    self.conn.delete_s(dn)
