from arlut.csd.ddroid.server import Ganymede
import JythonURIClassLoader

def get_jythonEditObject( uri, base, invid, editset, original ):
  JythonURIClassLoader.load( uri )

  from ddroid_hot_code import EditObject
  obj = EditObject( base, invid, editset, original )

  return obj
