from arlut.csd.ganymede.server import Ganymede
import JythonURIClassLoader

# This will act as our class cache. It will be a hash that
# takes a URI for a key and returns a Jython class.
class_hash = {}

def get_jythonEditObject( uri, base, invid, editset, original ):
  '''Cached, dynamic class loading by uri
  '''
  global class_hash

  # First, we'll look for this class in the cache
  if class_hash.has_key(uri):
    Ganymede.debug( "Loaded " + uri + " from the Jython class cache." )
    return class_hash[uri]( base, invid, editset, original );
  
  # Okay, since the class isn't in the cache, we need to slurp 
  # it from the specified location
  JythonURIClassLoader.load( uri )

  # Snag the class definition from the default module that
  # JythonURIClassLoader created for us
  from ganymede_hot_code import EditObject

  # Store this class in the hash. This doesn't involve any
  # synchronization because the caller should already be
  # synchronized
  Ganymede.debug( "Adding " + uri + " to the Jython class cache." )
  class_hash[ uri ] = EditObject

  # And return an instantiation of the class
  return EditObject( base, invid, editset, original )



def unload_class( uri=None ):
  '''Remove a URI => Class mapping from the cache

  NOTE: If we aren't passed in an argument, or if the
  argument is None, we wipe out the entire cache.
  '''
  global class_hash
  
  # This doesn't involve any synchronization because the
  # caller should already be synchronized
  if not uri:
    Ganymede.debug( "Zeroing out the Jython class cache." )
    class_hash = {}
  else:
    if class_hash.has_key(uri):
      Ganymede.debug( "Removing " + uri + " from the Jython class cache." )
      del class_hash[uri]
