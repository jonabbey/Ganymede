from arlut.csd.ganymede.server import DBEditObject, Ganymede, GanymedeManagementException

class BaseJythonEditObject(DBEditObject):

  def __init__( self, base, invid, editset, original ):
    # Now we'll run a "constructor" based on what arguments we've received
    if base and invid and editset:
      # We should call the new object constructor
      DBEditObject.__init__( self, base, invid, editset )

    elif base:
      # We should call the customization constructor
      DBEditObject.__init__( self, base )

    elif editset and original:
      # We should call the check-out constructor
      DBEditObject.__init__( self, original, editset )

    else:
      raise GanymedeManagementException("Couldn't determine which constructor to call based on args.")
