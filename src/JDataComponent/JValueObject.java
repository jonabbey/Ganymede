
/*
   JValueObject.java


   This class is primary a holder object that is used to
   pass scalar and vector values and vector operation
   instructions using a callback method to the Object designated
   as an appropriate callback handler.

   
   Created: 28 Feb 1997
   Version: 1.2 97/08/26
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDataComponent;

import java.awt.*;

public class JValueObject {


  public static final int ADD = -1000;
  public static final int INSERT = -2000;
  public static final int DELETE = -3000;
  public static final int SET = -4000;
  public static final int NONE = -5000;

  private Component source;
  private Object value;
  private int index;
  private int operationValue = NONE;

  private boolean Operation = false;  // true if the value object is asking for a field to be added
                                      // to a vectorContainer.


  public JValueObject(Component source,
		     Object value)
    {
      this.source = source;
      this.value = value;
      operationValue = SET;
    }
  
  public JValueObject(Component source,
		     int index,
		     int operation)
    {

      this.source = source;
      this.index = index;

      if (operation != ADD && operation != INSERT && operation != DELETE)
	throw new IllegalArgumentException("Illegal Argument: operation has invalid value");

      this.operationValue = operation;

      Operation = true;
    }


  public JValueObject(Component source,
		     int index,
		     int operation,
		     Object value)
    {

      this.source = source;
      this.index = index;

     
      if (operation != ADD && operation != INSERT && operation != DELETE)
	throw new IllegalArgumentException("Illegal Argument: operation has invalid value");
 

      this.operationValue = operation;

      Operation = true;

      this.value = value;
    }


  public Component getSource() {

    return source;
  }


  public boolean isOperation() {

    return Operation;
  }

  public int getIndex() {

    return index;
  }

  public Object getValue() {
    
    return value;
  }

  public int operationType() {

    return operationValue;
  }

  public int getOperationType() {
    return operationValue;
  }

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
