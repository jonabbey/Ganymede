from arlut.csd.ganymede.server import Ganymede
import urllib
import imp
import sys

def load( uri, name="ganymede_hot_code" ):
  # Grab the Jython code from the desired location
  Ganymede.debug( "Loading code from: " + uri )
  f = urllib.urlopen( uri )
  code = f.read()
  f.close()

  # Create a new module that will hold the new code
  module = imp.new_module( name )

  # Compile the new code into Jython bytecode
  bytecode = compile( code, name, "exec" )
  
  Ganymede.debug( "Inserting hot code into module: " + name )
  
  # Execute the code in the context of the newly created module.
  # This will place the new code in the module's scope.
  exec bytecode in module.__dict__, module.__dict__

  # Add the newly created module to the list of modules available
  # to this interpreter instance
  sys.modules[ name ] = module
