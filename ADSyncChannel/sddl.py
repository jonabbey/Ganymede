import struct

class SecurityDescriptor:
  def __init__(self, data):
    self.data = data

    (self.rev,
     self.stype,
     self.owner_sid_offset,
     self.group_sid_offset,
     self.sacl_offset,
     self.dacl_offset) = struct.unpack("<HHIIII", data[:20])

    self.owner_sid = SID(data[self.owner_sid_offset:])
    self.group_sid = SID(data[self.group_sid_offset:])


class SID:
  def __init__(self, data):
    self.rev, self.num_subauths = struct.unpack("<BB", data[:2])

    # The NT Authority is stored in 6-bytes, big endian. We'll pad
    # it with 2 leading empty bytes so it will fit in a python bignum
    self.ntauth = struct.unpack(">Q", "\x00\x00" + data[2:8])[0]

    # There are a variable number of NT sub-authorities, each one
    # is 4 bytes. 'subauths' is a list of them all.
    bytes_left = 4 * self.num_subauths
    self.subauths = struct.unpack("<" + ("I" * self.num_subauths), data[8:8+bytes_left])


  def pack(self):
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


def parse(data):
  sd = SecurityDescriptor(data)

  import pdb
  pdb.set_trace()



if __name__ == "__main__":
  parse(open('ntsecurity.bin', 'r').read())
