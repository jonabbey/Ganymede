
/*
   JValueObject.java


   This class is primary a holder object that is used to
   pass scalar and vector values and vector operation
   instructions using a callback method to the Object designated
   as an appropriate callback handler.

   Created: 28 Feb 1997
   Version: $Revision: 1.8 $ %D%
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDataComponent;

import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    JValueObject

------------------------------------------------------------------------------*/

/**
 *
 *  JValueObject.java<br><br>
 *
 *  This class is primary a holder object that is used to
 *  pass scalar and vector values and vector operation
 *  instructions using a callback method to the Object designated
 *  as an appropriate callback handler.<br><br>
 *   
 *  Created: 28 Feb 1997<br>
 *  Version: $Revision: 1.8 $ %D%<br>
 *  Module By: Navin Manohar<br>
 *  Applied Research Laboratories, The University of Texas at Austin
 *
 */

public class JValueObject {

  public static final int FIRST = -1009;
  public static final int ADD = -1001;
  public static final int INSERT = -1002;
  public static final int DELETE = -1003;
  public static final int SET = -1004;
  public static final int NONE = -1005;
  public static final int ERROR = -1006;
  public static final int SPECIAL = -1007;  //Use this for those hacks
  public static final int PARAMETER = -1008;
  public static final int LAST = -1000;

  private Component source;
  private Object value;
  private Object parameter;  // for JValueObjects with PARAMETER
  private int index;
  private int operationValue = NONE;

  private boolean Operation = false;  // true if the value object is asking for a field to be added
                                      // to a vectorContainer.
  /* -- */

  public JValueObject(Component source, Object value)
  {
    this.source = source;
    this.value = value;
    operationValue = SET;
  }

  public JValueObject(Component source, Object value, int operation)
  {
    this.source = source;
    this.value = value;
    operationValue = operation;
  }
  
  public JValueObject(Component source, int index, int operation)
  {
    this.source = source;
    this.index = index;

    if ((operation > LAST) || (operation < FIRST))
      {
	throw new IllegalArgumentException("Illegal Argument: operation has invalid value: " + operation);
      }
    
    this.operationValue = operation;
    
    Operation = true;
  }

  public JValueObject(Component source, int index, int operation, Object value)
  {
    this(source, index, operation, value, null);
  }

  public JValueObject(Component source, int index, int operation, Object value, Object parameter)
  {
    this.source = source;
    this.index = index;
    this.parameter = parameter;
    
    if ((operation < FIRST) || (operation > LAST))
      {
	throw new IllegalArgumentException("Illegal Argument: operation has invalid value: " + operation);
      }
    
    this.operationValue = operation;
    
    Operation = true;
    
    this.value = value;
  }

  public Component getSource()
  {
    return source;
  }

  public Object getParameter() 
  {
    return parameter;
  }

  public boolean isOperation() 
  {
    return Operation;
  }

  public int getIndex() 
  {
    return index;
  }

  public Object getValue() 
  {
    return value;
  }

  public int operationType() 
  {
    return operationValue;
  }

  public int getOperationType() 
  {
    return operationValue;
  }

  /**
   *
   * Method to get a human-readable description of the event carried
   * by this object
   * 
   */

  public String toString()
  {
    String result;

    /* -- */

    result = source.toString();

    switch (operationValue)
      {
      case ADD:
	
	result += " add element " + index;
	
	break;

      case INSERT:

	result += " insert element " + index;

	break;

      case DELETE:

	result += " delete element " + index;

	break;

      case SET:

	result += " set value ";

	break;

      case NONE:

	result += " none";

	break;
      }

    result += value;

    return result;
  }
}
