from arlut.csd.ganymede.server import QueryParser, QueryLexer, QueryParserTokenTypes, Ganymede
from arlut.csd.ganymede.common import Query, QueryAndNode, QueryNotNode, QueryOrNode, QueryDataNode, QueryDeRefNode, FieldType
from java.lang import RuntimeException, String, Boolean
from java.io import StringReader
from antlr import ANTLRException, DumpASTVisitor
from java.text import DateFormat
import string


# Operator -> OperatorID mappings
op_scalar_mapping = {"=~": QueryDataNode.MATCHES,
                     "=~_ci" : QueryDataNode.NOCASEMATCHES,
                     "==": QueryDataNode.EQUALS,
                     "==_ci" : QueryDataNode.NOCASEEQ,
                     "<": QueryDataNode.LESS,
                     "<=": QueryDataNode.LESSEQ,
                     ">": QueryDataNode.GREAT,
                     ">=": QueryDataNode.GREATEQ,
                     "starts" : QueryDataNode.STARTSWITH,
                     "ends": QueryDataNode.ENDSWITH,
                     "defined": QueryDataNode.DEFINED }

# TODO: We need to implement len_lteq, len_gteq in QueryDataNode et all.
# TODO: We need to come up with a more flexible system of permuting array ops
# and scalar ops to be applied to a vector field
op_vector_mapping = {"len<": (QueryDataNode.NONE, QueryDataNode.LENGTHLE),
                     "len>": (QueryDataNode.NONE, QueryDataNode.LENGTHGR),
                     "len>=": (QueryDataNode.NONE, QueryDataNode.LENGTHGREQ),
                     "len<=": (QueryDataNode.NONE, QueryDataNode.LENGTHLEEQ),
                     "len==": (QueryDataNode.NONE, QueryDataNode.LENGTHEQ)}

# Valid operations for a given DBField type
validity_mapping = { FieldType.DATE: ['<','>','<=','>=','==','defined'],
                     FieldType.NUMERIC: ['<','>','<=','>=','==','defined'],
                     FieldType.FLOAT: ['<','>','<=','>=','==','defined'],
                     FieldType.BOOLEAN: ['==','defined'],
                     FieldType.IP: ['==','=~','starts','ends','defined'],
                     FieldType.STRING: ['<','>','<=','>=','=~','=~_ci','==','==_ci','starts','ends','defined'],
                     FieldType.INVID: ['<','>','<=','>=','=~','=~_ci','==','==_ci','starts','ends','defined'],
                     FieldType.PASSWORD: ['defined'],
                     FieldType.PERMISSIONMATRIX: [] }



class ParseException(RuntimeException):
  def __init__( self, msg ):
    self.msg = msg
    RuntimeException.__init__( self, msg )

  def __repr__( self ):
    return self.msg


def go( query ):
  lexer = QueryLexer( StringReader(query) )
  parser = QueryParser( lexer )

  try:
    parser.query()
  except ANTLRException, details:
    print details
    return

  ast = parser.getAST()
  if ast == None:
    raise ParseException( "I couldn't parse the query, bub. Make sure you've parenthesized everything correctly." )
  root, objectbase, fields_to_return = parse_tree(ast)

  query = Query( objectbase.getName(), root )
  
  for field in fields_to_return:
    field_id = field.getID()
    query.addField( field_id )

  return Ganymede.internalSession.dump( query )



def dequote( string ):
  # If there are surrounding quotes, remove them
  if (string[0] == '"' and string[-1] == '"') or (string[0] == "'" and string[-1] == "'"):
    string = string[1:-1]
  return string



def get_field_from_objectbase( field_name, objectbase ):
  field = objectbase.getField( dequote(field_name) )
  if field == None:
    raise ParseException( "Field " + field_name + " doesn't exist for the object base " + objectbase.getName() )
  return field



def parse_argument( operator, argument, argument_type, field ):
  # Handle the degenerate cases where we don't know what type of field we're
  # looking at (this happens, say, in the context of a deref operation). We'll
  # just try our best to convert the argument based on what the parser tells us
  # to do.
  
  if field == None and argument_type == QueryParserTokenTypes.INT_VALUE:
    return int(argument)

  if field == None and argument_type == QueryParserTokenTypes.DECIMAL_VALUE:
    return float(argument)

  if field == None and argument_type == QueryParserTokenTypes.STRING_VALUE:
    return dequote(argument)

  # Now we'll handle the normal cases where we know what kind of DBField we're
  # dealing with. This gives much more precise results.
  
  field_type = field.getType()

  # Length operations get special treatment, since they take integer arguments
  # regardless of the type of the underlying DBField
  if operator in ["len<", "len<=", "len>", "len>=", "len=="] and \
     field.isArray() and \
     argument_type == QueryParserTokenTypes.INT_VALUE:
    return int(argument)

  if field_type == FieldType.NUMERIC and \
     argument_type == QueryParserTokenTypes.INT_VALUE:
    return int(argument)

  if field_type == FieldType.FLOAT and \
      argument_type == QueryParserTokenTypes.DECIMAL_VALUE:
    return float(argument)

  if field_type == FieldType.BOOLEAN:
    if argument_type == QueryParserTokenTypes.BOOLEAN_VALUE:
      if argument == "true":
        return Boolean.TRUE
      else:
        return Boolean.FALSE

    if argument_type == QueryParserTokenTypes.STRING_VALUE:
      return (int(dequote(argument)) != 0)
    if argument_type == QueryParserTokenTypes.INT_VALUE:
      return (int(argument) != 0)

  if field_type in [FieldType.STRING, FieldType.INVID, FieldType.IP] and \
     argument_type == QueryParserTokenTypes.STRING_VALUE:
    return dequote(argument)

  if field_type == FieldType.DATE and \
     argument_type == QueryParserTokenTypes.STRING_VALUE:
    return DateFormat.parse(dequote(argument))

  raise ParseException( "The field " + field.getName() + " takes arguments of type " + field.getTypeDesc() )
  



def parse_select_tree( ast, objectbase ):
  select_fields = []

  # The select subtree has a root of "select" and then a set of leaf nodes
  # underneath, with each leaf node representing a DBField to return
  select_node = ast.getFirstChild()

  while select_node != None:
    field_name = select_node.getText()
    field = get_field_from_objectbase( field_name, objectbase )
    select_fields.append( field )
    select_node = select_node.getNextSibling()

  return select_fields



def parse_from_tree( ast ):
  # The from subtree has a root of "from" and then one child node that 
  # represents the DBObjectBase to query against
  from_objectbase = dequote( ast.getFirstChild().getText() )
  objectbase = Ganymede.db[ from_objectbase ]

  if objectbase == None:
    raise ParseException( "The objectbase " + from_objectbase + " doesn't exist, bub." )

  return objectbase



def parse_tree( ast ):
  # Handle the from clause. We handle this first since we need a DBObjectBase
  # to information about fields in the select clause. The from clause is the
  # next sibling in the ast tree...the select clause lexically precedes it.
  objectbase = parse_from_tree( ast.getNextSibling() )

  # The select clause is the first element of this tree
  select_fields = parse_select_tree( ast, objectbase )
  
  # Handle the where clause. The where subtree has a root of "where" and then
  # a complicated binary tree of AND, OR, NOT, etc QueryNodes.
  where_node = ast.getNextSibling().getNextSibling().getFirstChild()
  where_tree = parse_where_clause( where_node, objectbase )

  return where_tree, objectbase, select_fields
  
  

def parse_where_clause( ast, objectbase ):
  root_type = ast.getType()

  if root_type == QueryParserTokenTypes.NOT:
    return QueryNotNode( parse_where_clause(ast.getFirstChild(), objectbase) )

  elif root_type == QueryParserTokenTypes.AND:
    child1 = parse_where_clause( ast.getFirstChild(), objectbase )
    child2 = parse_where_clause( ast.getFirstChild().getNextSibling(), objectbase )
    return QueryAndNode( child1, child2 )

  elif root_type == QueryParserTokenTypes.OR:
    child1 = parse_where_clause( ast.getFirstChild(), objectbase )
    child2 = parse_where_clause( ast.getFirstChild().getNextSibling(), objectbase )
    return QueryOrNode( child1, child2 )

  elif root_type == QueryParserTokenTypes.DEREF:
    field_name = ast.getFirstChild().getText()

    if objectbase != None:
      field = get_field_from_objectbase( field_name, objectbase )

      # Check to make sure that this is an InvidDBField
      if field.getType() != FieldType.INVID:
        raise ParseException( "You can only dereference an Invid field, which " + field_name + " isn't." )
      
      # Now try and figure out what kind of object this field points to
      target_objectbase_id = field.getTargetBase()
      if target_objectbase_id >= 0:
        target_objectbase = Ganymede.db.getObjectBase( target_objectbase_id )
      else:
        target_objectbase = None
        
    else:
      target_objectbase = None

    child1 = dequote( field_name )

    # We don't pass in an object base since in a deref operation, we have no idea
    # in advance what kind of object the dereferenced field will point to
    child2 = parse_where_clause( ast.getFirstChild().getNextSibling(), target_objectbase )

    return QueryDeRefNode( child1, child2 )
  
  elif root_type == QueryParserTokenTypes.BINARY_OPERATOR or \
       root_type == QueryParserTokenTypes.UNARY_OPERATOR:

    op = ast.getText()
    
    field_node = ast.getFirstChild()
    field_name = dequote(field_node.getText())

    if objectbase == None:
      field = None
    else:
      field = get_field_from_objectbase( field_name, objectbase )
      field_type = field.getType()
    
    if root_type == QueryParserTokenTypes.BINARY_OPERATOR:
      argument_node = field_node.getNextSibling()
      argument_type = argument_node.getType()
      argument = argument_node.getText()
      argument = parse_argument( op, argument, argument_type, field )
    else:
      argument = None

    scalar_operator, vector_operator = get_operator_value( op, field_name, objectbase )

    # Check that the scalar operation we want is valid for the field type. We only do this
    # check if there is a scalar operation involved!
    if objectbase != None and \
       scalar_operator != QueryDataNode.NONE and \
       op not in validity_mapping[ field_type ]:
      raise ParseException( "The " + str(scalar_operator) + " operation is not valid on a " + \
                            objectbase.getName() + "'s " + field_name + " field, which is of type " + \
                            field.getTypeDesc() )

    return QueryDataNode( field_name, scalar_operator, vector_operator, argument )

  raise ParseException( "I couldn't process node type: " + str(root_type) )



def get_operator_value( op, field_name, objectbase ):
  # If we don't know the objectbase, then we have to think. We can't check
  # and see if the field we're querying is a scalar or a vector field. Thus,
  # we'll have to trust that the user knows what he wants. If he's wrong
  # about the vector/scalar type of the field he's querying, then the query
  # engine will try its best to figure out what the user really meant.
  if objectbase == None:
    if op in op_vector_mapping.keys():
      return op_vector_mapping[op]
    elif op in op_scalar_mapping.keys():
      return (op_scalar_mapping[op], QueryDataNode.NONE)
    else:
      raise ParseException( op + " is not an operation that I understand." )

  field = get_field_from_objectbase( field_name, objectbase )

  if field.isArray():
    # If the field is a vector field and we're attempting a vector operation,
    # then we don't have to do anything special
    if op in op_vector_mapping.keys():
      return op_vector_mapping[op]

    # If the field is a vector field and we're attempting a scalar operation,
    # then we'll assume that the user wants us to perform the scalar comparison
    # on each item in the vector. Hence, we'll use an implicit "CONTAINS"
    # operation.
    elif op in op_scalar_mapping.keys():
      return (op_scalar_mapping[op], QueryDataNode.CONTAINS)

    else:
      raise ParseException( op + " is not an operation that I understand." )
    
  # The field is a scalar field, and we're attempting a scalar operation
  elif op in op_scalar_mapping.keys():
    return (op_scalar_mapping[op], QueryDataNode.NONE)

  elif op in op_vector_mapping.keys():
    raise ParseException( op + " is a vector operation, but " + field_name + " is a scalar field." )

  else:
    raise ParseException( op + " is not an operation that I understand." )
