from arlut.csd.ganymede.server import Ganymede
from arlut.csd.ganymede.server import GanymedeBuilderTask
from arlut.csd.ganymede.server import PasswordDBField
from arlut.csd.Util import PathComplete
from arlut.csd.ganymede.common import SchemaConstants

from java.lang import System, RuntimeException

import re
import base64
import os


class Task(GanymedeBuilderTask):
  output_path = None
  output_file = None
  dnsdomain = None
  tryKerberos = 0

  AppleOptions = '''<?xml version=\"1.0\" encoding=\"UTF-8\"?> <!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\"><plist version=\"1.0\"><dict><key>simultaneous_login_enabled</key><true/></dict></plist>'''

  binary_regexp = re.compile("[\000-\037]|[\301-\377]")




  def __init__( self ):
    self.db = Ganymede.db
    GanymedeBuilderTask.__init__( self )




  def builderPhase1( self ):
    Ganymede.debug( "LDAPBuilderTask builderPhase1 running" )

    self.dnsdomain = System.getProperty("ganymede.gash.dnsdomain")
    if not self.dnsdomain:
      raise RuntimeException("LDAPBuilder not able to determine dns domain name")
    
    self.output_path = System.getProperty("ganymede.builder.output")
    if not self.output_path:
      raise RuntimeException("LDAPBuilder not able to determine output directory")
    else:
      self.output_path = PathComplete.completePath( self.output_path )

    self.build_users_and_groups()
    self.build_netgroups()

    Ganymede.debug( "LDAPBuilderTask builderPhase1 complete" )
    return 1




  def builderPhase2( self ):
    Ganymede.debug( "LDAPBuilderTask builderPhase2 running" )

    build_script = System.getProperty( "ganymede.biulder.scriptlocation" )
    if not build_script:
      raise RuntimeException( "Unable to determine builder script location" )
    else:
      build_script = PathComplete.completePath( build_script ) + "ldapbuilder"

    if not os.path.is_file( build_script ):
      raise RuntimeException( build_script + " doesn't exist, not running external LDAP build script" )

    os.system( build_script )

    Ganymede.debug( "LDAPBuilderTask builderPhase2 complete" )
    return 1




  def build_users_and_groups( self ):
    # Only build the output if the users/groups have changed
    if not self.super__baseChanged( SchemaConstants.UserBase ) and \
       not self.super__baseChanged( 257 ):
      return
      
    Ganymede.debug( "Need to build LDAP users and groups output" )

    Ganymede.debug( "Writing out users" )
    self.output_file = open( self.output_path + "users.ldif", "w" )
    
    for user in self.db['User'].keys():
      self.user_to_LDIF( self.db['User'][user] )
      self.output_file.write( "\n" )
    
    self.output_file.flush()
    self.output_file.close()

    Ganymede.debug( "Writing out groups" )
    self.output_file = open( self.output_path + "groups.ldif", "w" )

    for group in self.db['Group'].keys():
      self.group_to_LDIF( self.db['Group'][group] )
      self.output_file.write( "\n" )

    self.output_file.flush()
    self.output_file.close()




  def build_netgroups( self ):
    # Only build the output if its constituent objects have changed:
    # Users, groups, user netgroups, systems, and system netgroups
    if not self.super__baseChanged( SchemaConstants.UserBase ) and \
       not self.super__baseChanged( 257 ) and \
       not self.super__baseChanged( 270 ) and \
       not self.super__baseChanged( 263 ) and \
       not self.super__baseChanged( 271 ):
      return

    Ganymede.debug( "Writing out netgroups" )
    self.output_file = open( self.output_path + "netgroups.ldif", "w" )

    for netgroup_type in ['User Netgroup', 'System Netgroups']:
      for netgroup in self.db[netgroup_type].keys():
        self.netgroup_to_LDIF( self.db[netgroup_type][netgroup] )
        self.output_file.write( "\n" )

    self.output_file.flush()
    self.output_file.close()
    self.output_file = None




  def user_to_LDIF( self, user ):
    self.write_LDIF( "dn", "uid=" + user['Username'].val + ",cn=users,dc=xserve" )
    self.write_LDIF( "apple-generateduid", user['Global UID'].val )
    self.write_LDIF( "sn", user['Username'].val )
    self.write_LDIF( "loginShell", user['Login Shell'].val )
    self.write_LDIF( "uidNumber", user['UID'].val )
    self.write_LDIF( "gidNumber", user['Home Group'].val['GID'].val )
    
    if not self.tryKerberos:
      # now write out the password.  If the user was inactivated, there
      # won't be a password.. to make sure that ldapdiff does the right
      # thing, we just won't emit a userPassword field in that case.

      pw_field = user['Password']
      if pw_field:
        # Try and snag the SSHA password, which is preformatted fot LDAP
        pw_text = pw_field.getSSHAHashText()
        
        if not pw_text:
          # If we didn't find one, the use the UNIX Crypt password
          pw_text = "{CRYPT}" + pw_field.getUNIXCryptText()
        
        self.write_LDIF( "userPassword", pw_text )

    self.write_LDIF( "objectClass", "inetOrgPerson" )
    self.write_LDIF( "objectClass", "posixAccount" )
    self.write_LDIF( "objectClass", "shadowAccount" )
    self.write_LDIF( "objectClass", "apple-user" )
    self.write_LDIF( "objectClass", "extensibleObject" )
    self.write_LDIF( "objectClass", "organizationalPerson" )
    self.write_LDIF( "objectClass", "top" )
    self.write_LDIF( "objectClass", "person" )
    self.write_LDIF( "uid", user['Username'].val )

    if self.tryKerberos:
      self.write_LDIF( "authAuthority", "1.0;Kerberos:ARLUT.UTEXAS.EDU;" )
    else:
      self.write_LDIF( "authAuthority", ";basic;" )
    
    if user.has_key('Full Name'):
      self.write_LDIF( "cn", user['Full Name'].val )
    else:
      self.write_LDIF( "cn", user['Username'].val )

    if user.has_key('Home Directory'):
      self.write_LDIF( "homeDirectory", user['Home Directory'].val )

    self.write_LDIF( "apple-mcxflags", self.AppleOptions )




  def group_to_LDIF( self, group ):
    self.write_LDIF( "dn",  "cn=" + group['GroupName'].val + ",cn=groups,dc=xserve" )
    self.write_LDIF( "gidNumber", group['GID'].val )
    
    if group.has_key('Users'):
      for user in group['Users'].val:
        self.write_LDIF( "memberUid", user['Username'].val )

    self.write_LDIF( "objectClass", "posixgroup" )
    self.write_LDIF( "objectClass", "apple-group" )
    self.write_LDIF( "objectClass", "extensibleObject" )

    if group.has_key('Users'):
      for user in group['Users'].val:
        self.write_LDIF( "uniqueMember", "uid=" + user['Username'].val + ",cn=users,dc=xserve" )

    self.write_LDIF( "cn", group['GroupName'].val )




  def netgroup_to_LDIF( self, netgroup ):
    self.write_LDIF( "dn", "cn=" + netgroup['Netgroup Name'].val + ",cn=netgroups,dc=xserve" )
    self.write_LDIF( "objectClass", "nisNetgroup" )
    self.write_LDIF( "cn", netgroup['Netgroup Name'].val )

    if netgroup.has_key("Users"):
      for user in netgroup['Users'].val:
        self.write_LDIF( "nisNetgroupTriple", "(," + user['Username'].val + ",)" )
    
    if netgroup.has_key("Systems"):
      for system in netgroup['Systems'].val:
        self.write_LDIF( "nisNetgroupTriple", "(" + system['System Name'].val + "." + self.dnsdomain + ",,)" )

    if netgroup.has_key("Member Netgroups"):
      for member_netgroup in netgroup['Member Netgroups'].val:
        self.write_LDIF( "memberNisNetgroup", member_netgroup['Netgroup Name'].val )
  
    


  def write_LDIF( self, attr, value ):
    v = str(value)
    if self.binary_regexp.search( v ):
      self.output_file.write( attr + ":: " + base64.encodestring( v ) + "\n" )
    else:
      self.output_file.write( attr + ": " + v + "\n" )

