import struct
from sddl_constants import *

class SecurityDescriptor:
  '''
  Represents a Windows NT/2KX security descriptor field. These descriptors
  are typically found as the "nTSecurityDescriptor" attribute of various
  Windows objects, and they define permissions/ACL's for the object in 
  question.
  '''

  def __init__(self, data):
    '''
    Parses a list of bytes/chars into the constituent pieces of a security
    descriptor.

    Some resources that clue us dolts in about the exact structure of a
    security descriptor:
    http://linux-ntfs.sourceforge.net/ntfs/attributes/security_descriptor.html
    http://msdn.microsoft.com/library/default.asp?url=/library/en-us/secauthz/security/security_descriptor_string_format.asp
    http://websvn.samba.org/cgi-bin/viewcvs.cgi/trunk/source/rpc_parse/parse_sec.c?view=markup
    http://websvn.samba.org/cgi-bin/viewcvs.cgi/trunk/source/include/rpc_secdes.h?view=markup
    '''
    self.data = data

    # The header block:
    # Revision: 2 bytes
    # Type: 2 bytes
    # 4 Offset markers: 4 bytes each
    (self.rev,
     self.stype,
     self.owner_sid_offset,
     self.group_sid_offset,
     self.sacl_offset,
     self.dacl_offset) = struct.unpack("<HHIIII", data[:20])

    self.sacl = ACL(data[self.sacl_offset:])
    self.dacl = ACL(data[self.dacl_offset:])

    self.owner_sid = SID(data[self.owner_sid_offset:])
    self.group_sid = SID(data[self.group_sid_offset:])

    # TODO: Break this out into a separate unittest
    # assert( self.pack() == data )


  def pack(self):
    '''
    Takes the state of this security descriptor and serializes it back into a
    stream of bytes.
    '''
    s = struct.pack("<HHIIII", self.rev, \
                               self.stype, \
                               self.owner_sid_offset, \
                               self.group_sid_offset, \
                               self.sacl_offset, \
                               self.dacl_offset)
    s += self.sacl.pack()
    s += self.dacl.pack()
    s += self.owner_sid.pack()
    s += self.group_sid.pack()
    return s
  

  def to_tuple(self):
    d = (("Rev", "0x%x" % self.rev),
         ("Type", "0x%x" % self.stype),
         ("SACL", self.sacl),
         ("DACL", self.dacl),
         ("Owner SID", self.owner_sid),
         ("Group SID", self.group_sid))
    return d


  def __repr__(self):
    return str(self.to_tuple())


  def pprint(self):
    '''
    A pretty printing function. Gives a decent, indented representation of the
    security descriptor. Though keep in mind that depending on the number of
    ACE's contained within, this output can be pretty long.
    '''
    for propname, propvalue in self.to_tuple():
      if propname not in ["SACL", "DACL"]:
        print propname, "->", propvalue

      else:
        # We're printing out an ACL
        print propname, "->"

        for aclpropname, aclpropvalue in propvalue.to_tuple():
          if aclpropname != "ACES":
            print "\t", aclpropname, "->", aclpropvalue
          
          else:
            # We're printing out an ACE
            print "\t", aclpropname, "->"

            for i in range(len(aclpropvalue)):
              print "\t\t", i+1, "->"
            
              for acepropname, acepropvalue in aclpropvalue[i].to_tuple():
                print "\t\t\t", acepropname, "->", acepropvalue



class UUID:
  '''
  A python representation of a Windows UUID (aka, GUID)
  '''

  def __init__(self, data):
    '''
    Parses a list of bytes/chars into the constituent pieces of a UUID.

    The format of a UUID can be seen here:
    http://www.opengroup.org/onlinepubs/9629399/apdxa.htm
    '''
    self.data = data
    
    # Anatomy of a UUID:
    # Low field of timestamp: 4 byte int
    # Mid field of timestamp: 2 byte short
    # High field of timestamp, multiplexed with the version: 2 byte short
    # Clock sequence (hi and low): each is a single bytes
    # Node identifier (spatially unique): 6 characters
    (self.time_low,
     self.time_mid,
     self.time_hi_and_version) = struct.unpack("<IHH", data[:8])
    
    self.clock_seq = struct.unpack("<2B", data[8:10])
    self.node = struct.unpack("<6B", data[10:16])

    # Ensure that when we re-encode this uuid, it matches the input we received
    # assert( self.pack() == data )
 

  def pack(self):
    '''
    Takes the state of this UUID and serializes it back into a stream of bytes.
    '''
    s = struct.pack("<IHH", self.time_low, \
                            self.time_mid, \
                            self.time_hi_and_version)

    s += struct.pack("<2B", self.clock_seq[0], self.clock_seq[1])

    s += struct.pack("<6B", self.node[0], \
                            self.node[1],
                            self.node[2],
                            self.node[3],
                            self.node[4],
                            self.node[5])
    return s
  

  def __repr__(self):
    return "%x" % (self.time_low) + "-" + \
           "%x" % (self.time_mid) + "-" + \
           "%x" % (self.time_hi_and_version) + "-" + \
           "".join( ["%x" % c for c in self.clock_seq] ) + "-" + \
           "".join( ["%x" % c for c in self.node] )



class ACE:
  '''
  Represents a Windows ACE (Access Control Element?). An ACE is always
  contained within an ACL object, so refer to the ACL class for more details.
  '''
  def __init__(self, data):
    '''
    Parses a list of bytes/chars into the constituent pieces of an ACE object.

    The format of an ACE:
    http://linux-ntfs.sourceforge.net/ntfs/attributes/security_descriptor.html
    http://msdn.microsoft.com/library/default.asp?url=/library/en-us/secauthz/security/ace_strings.asp
    http://websvn.samba.org/cgi-bin/viewcvs.cgi/trunk/source/rpc_parse/parse_sec.c?view=markup
    http://websvn.samba.org/cgi-bin/viewcvs.cgi/trunk/source/include/rpc_secdes.h?view=markup
    '''
    self.data = data

    # Anatomy of an ACE:
    # Type: 1 byte
    # Flags: 1 byte
    # Size: 2 bytes
    # Access mask: 4 byte int
    self.stype, self.flags, self.size, self.mask = struct.unpack("<BBHI", data[:8])

    # This will hold our current offset into the ACE's binary data
    offset = 8

    # Now here's where it gets tricky. After the Mask is read, an ACE can contain
    # some optional elements, depending on what type of ACE it actually is.
    #
    # If the ACE is of the "object" variety...
    if self.stype in [SEC_ACE_TYPE_ACCESS_ALLOWED_OBJECT,
                      SEC_ACE_TYPE_ACCESS_DENIED_OBJECT,
                      SEC_ACE_TYPE_SYSTEM_AUDIT_OBJECT,
                      SEC_ACE_TYPE_SYSTEM_ALARM_OBJECT]:

      # ...then that means there's a 4 byte (int) flag to be read.
      self.objflags = struct.unpack("<I", data[offset:offset+4])[0]
      offset += 4

      # If the flags indicate the there is an "object present", then we should
      # expect a 16-byte UUID for the object at this point in the data stream.
      if (self.objflags & SEC_ACE_OBJECT_PRESENT) != 0:
        self.obj_guid = UUID(data[offset:offset+16])
        offset += 16

      # If the flags indicate the there is an "inherited object present", then
      # we should expect a 16-byte UUID for the inhereited object at this point
      # in the data stream.
      if (self.objflags & SEC_ACE_OBJECT_INHERITED_PRESENT) != 0:
        self.inh_guid = UUID(data[offset:offset+16])
        offset += 16
    
      # Lastly, all ACE's of the "object" variety end with an SID of the "trustee"
      self.sid = SID(data[offset:self.size])

    else:
      # If the ACE is NOT of the "object" variety, then it's easy: all that's left
      # to parse is the SID of the "trustee" of this ACE.
      self.sid = SID(data[offset:self.size])

    # Ensure that when we re-encode this ACE, it matches the input we received
    # assert( self.pack() == data[:self.size] )


  def pack(self):
    '''
    Takes the state of this ACE and serializes it back into a stream of bytes.
    '''
    s = struct.pack("<BBHI", self.stype, \
                             self.flags, \
                             self.size, \
                             self.mask)

    if hasattr(self, "objflags"):
      s += struct.pack("<I", self.objflags)

    if hasattr(self, "obj_guid"):
      s += self.obj_guid.pack()

    if hasattr(self, "inh_guid"):
      s += self.inh_guid.pack()

    s += self.sid.pack()
    return s


  def to_tuple(self):
    d = [("Type", "0x%x" % self.stype),
         ("Flags", "0x%x" % self.flags),
         ("Size", self.size),
         ("Mask", "0x%x" % self.mask)]

    if hasattr(self, "objflags"):
      d.append(("Object flags", "0x%x" % self.objflags))

    if hasattr(self, "obj_guid"):
      d.append(("Object GUID", self.obj_guid))

    if hasattr(self, "inh_guid"):
      d.append(("Inherited GUID", self.inh_guid))

    d.append(("SID", self.sid))
    return d


  def __repr__(self):
    return str(self.to_tuple())



class ACL:
  '''
  Represents a Windows ACL (access control list).
  '''

  def __init__(self, data):
    '''
    Parses a list of bytes/chars into the constituent pieces of an ACL.

    The format of an ACL can be found here:
    http://linux-ntfs.sourceforge.net/ntfs/attributes/security_descriptor.html
    http://websvn.samba.org/cgi-bin/viewcvs.cgi/trunk/source/rpc_parse/parse_sec.c?view=markup
    http://websvn.samba.org/cgi-bin/viewcvs.cgi/trunk/source/include/rpc_secdes.h?view=markup
    '''
    self.data = data

    # Anatomy of an ACL:
    # Revision: 2 byte short
    # Size: 2 byte short
    # Number of ACEs to follow: 4 byte int
    self.rev, self.size, self.num_aces = struct.unpack("<HHI", data[:8])
    self.aces = []

    offset = 8
    for i in range(self.num_aces):
      ace = ACE(data[offset:])
      offset += ace.size
      self.aces.append(ace)

    # Ensure that when we re-encode this ACL, it matches the input we received
    # assert( self.pack() == data[:self.size] )
  
  
  def pack(self):
    '''
    Takes the state of this ACL and serializes it back into a stream of bytes.
    '''
    s = struct.pack("<HHI", self.rev, self.size, self.num_aces)
    for ace in self.aces:
      s += ace.pack()
    return s


  def to_tuple(self):
    d = (("Rev", "0x%x" % self.rev),
         ("Size", self.size),
         ("Number of ACEs", self.num_aces),
         ("ACES", [ace for ace in self.aces]))
    return d

  def __repr__(self):
    return str(self.to_tuple())


class SID:
  '''
  Represents a Windows SID (security identifier)
  '''

  def __init__(self, data):
    '''
    Parses a list of bytes/chars into the constituent pieces of an ACL.

    The format of an SID can be found here:
    http://linux-ntfs.sourceforge.net/ntfs/attributes/security_descriptor.html
    http://websvn.samba.org/cgi-bin/viewcvs.cgi/trunk/source/rpc_parse/parse_sec.c?view=markup
    http://websvn.samba.org/cgi-bin/viewcvs.cgi/trunk/source/include/rpc_secdes.h?view=markup

    Some canonical SIDs:
    http://linux-ntfs.sourceforge.net/ntfs/concepts/sid.html
    '''
    self.data = data
    self.rev, self.num_subauths = struct.unpack("<BB", data[:2])

    # The NT Authority is stored in 6-bytes, big endian. We'll pad
    # it with 2 leading empty bytes so it will fit in a python bignum
    self.ntauth = struct.unpack(">Q", "\x00\x00" + data[2:8])[0]

    # There are a variable number of NT sub-authorities, each one
    # is 4 bytes. 'subauths' is a list of them all.
    bytes_left = 4 * self.num_subauths
    self.subauths = struct.unpack("<" + ("I" * self.num_subauths), data[8:8+bytes_left])

    # Ensure that when we re-encode this sid, it matches the input we received
    # assert(self.pack() == data[:8+bytes_left])


  def pack(self):
    '''
    Takes the state of this ACL and serializes it back into a stream of bytes.
    '''
    s = struct.pack("<BB", self.rev, self.num_subauths)
    s += struct.pack(">Q", self.ntauth)[2:]

    for auth in self.subauths:
      s += struct.pack("<I", auth)
    
    return s


  def __repr__(self):
    s = "S-%s-%s" % (self.rev, self.ntauth)
    for sa in self.subauths:
      s += "-%s" % sa
    return s

