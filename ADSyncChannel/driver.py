from connection import ADFrontend
from base64 import encodestring

class MockObj: pass

def go():
  ad = ADFrontend()
  ad.connect()

  try:
    ad.delete_user("foobar")
  except:
    pass
  
  obj = MockObj()
  obj.sAMAccountName = "foobar"
  
  ad.add_user(obj)
  ad.set_user_pw("foobar", "P@$$word")
  ad.set_user_pw("foobar", "P@$$word2")

  print ad.get_user("foobar")

if __name__ == "__main__":
  go()
