from connection import ADFrontend
from base64 import encodestring
import sddl

def go():
  ad = ADFrontend()
  ad.connect()

  '''
  try:
    ad.delete_user("foobar")
  except:
    pass
  
  ad.add_user("foobar", {}, password="P@$$word", accountControlFlag=512)
  '''
  print "Dumping all security descriptors:"
  print

  r = ad.search(attrlist=["nTSecurityDescriptor"])
  for dn, attrdict in r:
    if dn is None:
      continue

    print dn
    secdescriptor = attrdict["nTSecurityDescriptor"][0]
    sddl.SecurityDescriptor(secdescriptor).pprint()
    print "----------------------------------------------------------"
    print

if __name__ == "__main__":
  go()
