from BaseJythonEditObject import BaseJythonEditObject
from java.lang import String
from arlut.csd.ganymede.server import Ganymede
from arlut.csd.ganymede.common import SchemaConstants, Query, QueryNotNode, QueryDataNode, Invid

class EditObject(BaseJythonEditObject, SchemaConstants):

  LISTNAME_FIELD_ID = 256
  MEMBERS_FIELD_ID = 257
  EXTERNALTARGETS_FIELD_ID = 258

  

  def __init__( self, base, invid, editset, original ):
    BaseJythonEditObject.__init__( self, base, invid, editset, original )
    self.membersChoice = None



  def lookupLabel( self, obj ):
    '''This method is used to provide a hook to allow different objects to
    generate different labels for a given object based on their perspective.
    This is used to sort of hackishly simulate a relational-type capability for
    the purposes of viewing backlinks.
   
    See the automounter map and NFS volume DBEditObject subclasses for how this
    is to be used, if you have them.
    '''
    if obj.getTypeID() == SchemaConstants.UserBase:
      fullNameField = obj['Full_Name']
      nameField = obj['Username']

      if fullNameField and nameField:
        return nameField.val + " (" + fullNameField.val + ")"

    # Mark email lists
    if obj.getTypeID() == 274:
      return BaseJythonEditObject.lookupLabel(self, obj) + " (email list)"

    # Mark external email records
    if obj.getTypeID() == 275 and obj['Targets'] != None:
      addresses = [ str(addr) for addr in obj['Targets'].val ]
      return BaseJythonEditObject.lookupLabel( self, obj ) + " (" + ", ".join(addresses) + ")"

    return BaseJythonEditObject.lookupLabel( self, obj )



  def obtainChoicesKey( self, field ):
    '''This method returns a key that can be used by the client to cache the
    value returned by choices().  If the client already has the key cached on
    the client side, it can provide the choice list from its cache rather than
    calling choices() on this object again.
   
    If there is no caching key, this method will return null.
    '''
    if field.getID() == self.MEMBERS_FIELD_ID:
      return None
    else:
      return BaseJythonEditObject.obtainChoicesKey( self, field )



  def obtainChoiceList( self, field ):
    '''This method provides a hook that can be used to generate choice lists
    for invid and string fields that provide such.  String and Invid DBFields
    will call their owner's obtainChoiceList() method to get a list of valid
    choices.
   
    This method will provide a reasonable default for targetted invid fields.
    '''
    if field.getID() != self.MEMBERS_FIELD_ID:
      return BaseJythonEditObject.obtainChoiceList( self, field )

    if self.membersChoice is None:
      # we want to present a list of all users, mail groups besides this one,
      # and external mail aliases (email addresses that have local aliases in
      # ARL's mail system) as valid choices for the MEMBERS field.
      
      # List all users
      query1 = Query( SchemaConstants.UserBase, None, 0 )

      # List all external email targets
      query2 = Query( 275, None, 0 )

      # List all other email groups, but not ourselves
      root3 = QueryNotNode( QueryDataNode(-2, QueryDataNode.EQUALS, self.getInvid()) )
      query3 = Query( 274, root3, 0 )

      # We need a handle to the GSession to execute queries
      gsession = self.editset.getSession().getGSession()

      result = gsession.query(query1, self)
      result.append( gsession.query(query2, self) )
      result.append( gsession.query(query3, self) )

      self.membersChoice = result

    return self.membersChoice



  def anonymousLinkOK( self, targetObject, targetFieldID, sourceObject,
      sourceFieldID, gsession ):
    '''
    This method is used to control whether or not it is acceptable to make a
    link to the given field in this {@link arlut.csd.ganymede.server.DBObject
    DBObject} type when the user only has editing access for the source {@link
    arlut.csd.ganymede.server.InvidDBField InvidDBField} and not the target.
   
    This version of anonymousLinkOK takes additional parameters to allow an
    object type to decide that it does or does not want to allow a link based
    on what field of what object wants to link to it.
   
    By default, the 3 variants of the DBEditObject anonymousLinkOK() method are
    chained together, so that the customizer can choose which level of detail
    he is interested in.  {@link arlut.csd.ganymede.server.InvidDBField
    InvidDBField}'s {@link arlut.csd.ganymede.server.InvidDBField#bind(
    arlut.csd.ganymede.common.Invid,arlut.csd.ganymede.common.Invid,boolean)
    bind()} method calls this version.  This version calls the three parameter
    version, which calls the two parameter version, which returns false by
    default.  Customizers can implement any of the three versions, but unless
    you maintain the version chaining yourself, there's no point to
    implementing more than one of them.  
    '''
    # If someone tries to put this list in another email list, let them.
    # Type 274 is Email List, and field 257 is Members.
    if targetFieldID == SchemaConstants.BackLinksField and \
        sourceObject.getTypeID() == 274  and \
        sourceFieldID == self.MEMBERS_FIELD_ID:
      return true

    # The default anonymousLinkOK method returns false
    return BaseJythonEditObject.anonymousLinkOK( self, targetObject,
        targetFieldID, sourceObject, sourceFieldID, gsession )

    

  def fieldRequired( self, obj, fieldid ):
    '''
    Customization method to control whether a specified field is required to be
    defined at commit time for a given object.
   
    To be overridden in DBEditObject subclasses.
   
    Note that this method will not be called if the controlling
    GanymedeSession's enableOversight is turned off, as in bulk loading.
    '''
    # The email list name is required
    if fieldid == self.LISTNAME_FIELD_ID:
      return 1
    
    return BaseJythonEditObject.fieldRequired( self, obj, fieldid )

    

  def finalizeAddElement( self, field, value ):
    '''
    This method allows the DBEditObject to have executive approval of any
    vector-vector add operation, and to take any special actions in reaction to
    the add.. if this method returns null or a success code in its ReturnVal,
    the DBField that called us is guaranteed to proceed to make the change to
    its vector.  If this method returns a non-success code in its ReturnVal,
    the DBField that called us will not make the change, and the field will be
    left unchanged.
   
    The <field> parameter identifies the field that is requesting approval for
    item deletion, and the <submittedValues> parameter carries the values to be
    added.
   
    The DBField that called us will take care of all standard checks on the
    operation (including vector bounds, etc.) before calling this method.
    Under normal circumstances, we won't need to do anything here.
    '''
    if field.getID() not in [self.MEMBERS_FIELD_ID, self.EXTERNALTARGETS_FIELD_ID]:
      return None

    newItemVect = [value]
    if not self.fitsInNIS(newItemVect):
      return Ganymede.createErrorDialog("Overflow error", \
		"The item that you are attempting to add to the " + self.getTypeName() + \
		" email list cannot fit.  No NIS email list in the laboratory's " + \
		"network can be longer than 1024 characters when converted to an " + \
		"NIS email alias definition.\n\n" + \
		"If you need this list to be expanded, you should create a new sublist " + \
		"for the overflow items, move some items from this list to the new sublist, " + \
		" and then add the new sublist to this list.")

    return None



  def finalizeAddElements( self, field, submittedValues ):
    '''
    This method allows the DBEditObject to have executive approval of any
    vector-vector add operation, and to take any special actions in reaction to
    the add.. if this method returns null or a success code in its ReturnVal,
    the DBField that called us is guaranteed to proceed to make the change to
    its vector.  If this method returns a non-success code in its ReturnVal,
    the DBField that called us will not make the change, and the field will be
    left unchanged.
   
    The <field> parameter identifies the field that is requesting approval for
    item deletion, and the <submittedValues> parameter carries the values to be
    added.
   
    The DBField that called us will take care of all standard checks on the
    operation (including vector bounds, etc.) before calling this method.
    Under normal circumstances, we won't need to do anything here.
    '''
    if field.getID() not in [self.MEMBERS_FIELD_ID, self.EXTERNALTARGETS_FIELD_ID]:
      return None

    if not self.fitsInNIS(submittedValues):
      return Ganymede.createErrorDialog("Overflow error", \
		"The " + str(len(submittedValues)) + \
                " items that you are attempting to add to the " + self.getTypeName() + \
		" email list cannot fit.  No NIS email list in the laboratory's " + \
		"network can be longer than 1024 characters when converted to an " + \
		"NIS email alias definition.\n\n" + \
		"If you need this list to be expanded, you should create a new sublist " + \
		"for the overflow items, move some items from this list to the new sublist, " + \
		" and then add the new sublist to this list.")

    return None


  
  def fitsInNIS( self, newItemVect ):
    '''
    This method takes a vector of new items and returns true if the new items
    should be able to fit in the NIS line built from this emailList object.
    '''
    externalTargets = self.getField( self.EXTERNALTARGETS_FIELD_ID )
    members = self.getField( self.MEMBERS_FIELD_ID )
    totalLength = len(externalTargets.valueString) + len(members.valueString)

    for value in newItemVect:
      if isinstance(value, Invid):
        gsession = self.getGSession()
        if gsession:
          objectRef = gsession.getSession().viewDBObject(value)
          if objectRef:
            label = objectRef.label
            if label:
              totalLength += len(label) + 2   # The extra 2 are for comma and space

      elif isinstance(value, String):
        totalLength += len(value) + 2   # The extra 2 are for comma and space
        
      if totalLength >= 1024:
        return 0

    return 1
