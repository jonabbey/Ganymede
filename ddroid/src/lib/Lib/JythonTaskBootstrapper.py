import JythonURIClassLoader

def run( uri ):
  JythonURIClassLoader.load( uri, "buildertask" )

  # And now, invoke the "run" method on the downloaded task.
  from buildertask import Task
  task = Task()
  task.run()
