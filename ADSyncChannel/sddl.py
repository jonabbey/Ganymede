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
    (self.revision,
     self.type,
     owner_sid_offset,
     group_sid_offset,
     sacl_offset,
     dacl_offset) = struct.unpack("<HHIIII", data[:20])

    self.systemACL = ACL(data[sacl_offset:])
    self.discretionaryACL = ACL(data[dacl_offset:])

    self.owner = SID(data[owner_sid_offset:])
    self.group = SID(data[group_sid_offset:])

    # TODO: Break this out into a separate unittest
    # assert( self.pack() == data )


  def pack(self):
    '''
    Takes the state of this security descriptor and serializes it back into a
    stream of bytes.
    '''
    pksacl = self.systemACL.pack()
    pkdacl = self.discretionaryACL.pack()
    pkowner = self.owner.pack()
    pkgroup = self.group.pack()

    # Note: I don't know if there's a "definitive" ordering for the fields in
    # a security descriptor, nor could I locate one on MSDN. There *is* a 
    # specific ordering for the offsets, though.
    #
    # As a default, I'll just order the actual fields in the order that they
    # "usually" appear when I read a security descriptor from AD.
    
    # We'll put the sacl first, and the header block of a security descriptor
    # is 20 bytes. Thus, the offset for the sacl will always be 20
    offset_sacl = 20
    
    # Next comes the dacl, the owner sid, and the group sid
    offset_dacl = offset_sacl + len(pksacl)
    offset_owner = offset_dacl + len(pkdacl)
    offset_group = offset_owner + len(pkowner)

    s = struct.pack("<HHIIII", self.revision, \
                               self.type, \
                               offset_owner, \
                               offset_group, \
                               offset_sacl, \
                               offset_dacl)

    s += pksacl + pkdacl + pkowner + pkgroup
    return s
  

  def to_tuple(self):
    d = (("Rev", "0x%x" % self.revision),
         ("Type", "0x%x" % self.type),
         ("SACL", self.systemACL),
         ("DACL", self.discretionaryACL),
         ("Owner SID", self.owner),
         ("Group SID", self.group))
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


  def __cmp__(self, other):
    if type(other) == type(""):
      return cmp(str(self).lower(), other.lower())
    else:
      return cmp(str(self).lower(), str(other).lower())


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
    self.aceType, self.aceFlags, self.size, self.accessMask = struct.unpack("<BBHI", data[:8])

    # This will hold our current offset into the ACE's binary data
    offset = 8

    # Now here's where it gets tricky. After the Mask is read, an ACE can contain
    # some optional elements, depending on what type of ACE it actually is.
    #
    # If the ACE is of the "object" variety...
    if self.aceType in [SEC_ACE_TYPE_ACCESS_ALLOWED_OBJECT,
                        SEC_ACE_TYPE_ACCESS_DENIED_OBJECT,
                        SEC_ACE_TYPE_SYSTEM_AUDIT_OBJECT,
                        SEC_ACE_TYPE_SYSTEM_ALARM_OBJECT]:

      # ...then that means there's a 4 byte (int) flag to be read.
      self.flags = struct.unpack("<I", data[offset:offset+4])[0]
      offset += 4

      # If the flags indicate the there is an "object present", then we should
      # expect a 16-byte UUID for the object at this point in the data stream.
      if (self.flags & SEC_ACE_OBJECT_PRESENT) != 0:
        self.objectType = UUID(data[offset:offset+16])
        offset += 16

      # If the flags indicate the there is an "inherited object present", then
      # we should expect a 16-byte UUID for the inherited object at this point
      # in the data stream.
      if (self.flags & SEC_ACE_OBJECT_INHERITED_PRESENT) != 0:
        self.inheritedObjectType = UUID(data[offset:offset+16])
        offset += 16
    
      # Lastly, all ACE's of the "object" variety end with an SID of the "trustee"
      self.trustee = SID(data[offset:self.size])

    else:
      # If the ACE is NOT of the "object" variety, then it's easy: all that's left
      # to parse is the SID of the "trustee" of this ACE.
      self.trustee = SID(data[offset:self.size])

    # Ensure that when we re-encode this ACE, it matches the input we received
    # assert( self.pack() == data[:self.size] )


  def pack(self):
    '''
    Takes the state of this ACE and serializes it back into a stream of bytes.
    '''
    # We know the size of the header block is always 8, so that's what we'll
    # start with
    size = 8
    payload = ""

    # The optional object flags are 4 bytes
    if hasattr(self, "flags"):
      payload += struct.pack("<I", self.flags)
      size += 4

    if hasattr(self, "objectType"):
      payload += self.objectType.pack()
      # UUID's are 16 bytes
      size += 16

    if hasattr(self, "inheritedObjectType"):
      payload += self.inheritedObjectType.pack()
      # UUID's are 16 bytes
      size += 16

    # And we'll always have a trustee here
    packedTrustee = self.trustee.pack()
    payload += packedTrustee
    size += len(packedTrustee)

    header = struct.pack("<BBHI", self.aceType, \
                                  self.aceFlags, \
                                  size, \
                                  self.accessMask)

    return header + payload


  def to_tuple(self):
    d = [("Type", "0x%x" % self.aceType),
         ("Flags", "0x%x" % self.aceFlags),
         ("Mask", "0x%x" % self.accessMask)]

    if hasattr(self, "flags"):
      d.append(("Object flags", "0x%x" % self.flags))

    if hasattr(self, "objectType"):
      d.append(("Object Type", self.objectType))

    if hasattr(self, "inheritedObjectType"):
      d.append(("Inherited Type", self.inheritedObjectType))

    d.append(("Trustee", self.trustee))
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
    self.revision, size, num_aces = struct.unpack("<HHI", data[:8])
    self.aces = []

    offset = 8
    for i in range(num_aces):
      ace = ACE(data[offset:])
      offset += ace.size
      self.aces.append(ace)

    # Ensure that when we re-encode this ACL, it matches the input we received
    # assert( self.pack() == data[:self.size] )
  
  
  def pack(self):
    '''
    Takes the state of this ACL and serializes it back into a stream of bytes.
    '''
    # The header block is 8 bytes, so that'll be out initial size
    size = 8
    payload = ""
    
    for ace in self.aces:
      pk = ace.pack()
      payload += pk
      size += len(pk)

    header = struct.pack("<HHI", self.revision, size, len(self.aces))

    return header + payload


  def to_tuple(self):
    d = (("Rev", "0x%x" % self.revision),
         ("Number of ACEs", len(self.aces)),
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
    self.revision, self.num_subauths = struct.unpack("<BB", data[:2])

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
    s = struct.pack("<BB", self.revision, self.num_subauths)
    s += struct.pack(">Q", self.ntauth)[2:]

    for auth in self.subauths:
      s += struct.pack("<I", auth)
    
    return s


  def __repr__(self):
    s = "S-%s-%s" % (self.revision, self.ntauth)
    for sa in self.subauths:
      s += "-%s" % sa
    return s


  def __cmp__(self, other):
    if type(other) == type(""):
      return cmp(str(self).lower(), other.lower())
    else:
      return cmp(str(self).lower(), str(other).lower())

