/*

   ownerPanel.java

   The individual frames in the windowPanel.
   
   Created: 9 September 1997
   Version: $Revision: 1.5 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.rmi.*;
import java.util.*;

import com.sun.java.swing.*;
import tablelayout.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

public class ownerPanel extends JBufferedPane implements JsetValueCallback {

  private final static boolean debug = false;

  boolean
    editable;

  invid_field
    field;

  framePanel
   parent;

  public ownerPanel(invid_field field, boolean editable, framePanel parent)
    {
      if (debug)
	{
	  System.out.println("=-=-=-=-=- Adding ownerPanel");
	}

      if (field == null)
	{
	  throw new RuntimeException("null field passed to ownerPanel constructor");
	}
      
      try
	{
	  if (field.getType() == SchemaConstants.OwnerListField)
	    {
	      throw new RuntimeException("Non-ownerListField passed to ownerPanel constructor");
	    }
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get type in ownerPanel constuctor: " + rx);
	}

      this.editable = editable;
      this.field = field;
      this.parent = parent;

      setInsets(new Insets(5,5,5,5));
      
      try
	{
	  stringSelector ownerList = createInvidSelector(field);
	  ownerList.setBorderStyle(0);
	  add(ownerList);
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not addDateField: " + rx);
	}
    }

  private stringSelector createInvidSelector(invid_field field) throws RemoteException
  {
    QueryResult
      results,
      choiceResults = null;

    Vector
      valueHandles = null,
      choiceHandles = null;

    /* -- */

    System.out.println("Adding StringSelector, its a vector of invids!");
    
    results = field.encodedValues();
    valueHandles = new Vector();

    if (editable)
      {
	choiceResults = field.choices();
	choiceHandles = new Vector();
      }

    for (int i = 0; i < results.size(); i++)
      {
	valueHandles.addElement(new listHandle(results.getLabel(i), results.getInvid(i)));
      }

    if (editable)
      {
	for (int i = 0; i < choiceResults.size(); i++)
	  {
	    choiceHandles.addElement(new listHandle(choiceResults.getLabel(i), choiceResults.getInvid(i)));
	  }
      }

    stringSelector ss = new stringSelector(choiceHandles, valueHandles, this, editable);
    ss.setCallback(this);
    return ss;
  }

  public boolean setValuePerformed(JValueObject o)
    {
      boolean returnValue = false;
      if (o.getSource() instanceof stringSelector)
	{
	  if (o.getValue() instanceof Invid)
	    {
	      Invid invid = (Invid)o.getValue();
	      int index = o.getIndex();
	      try
		{
		  if (o.getOperationType() == JValueObject.ADD)
		    {
		      returnValue = field.addElement(invid);
		    }
		  else if (o.getOperationType() == JValueObject.DELETE)
		    {
		      returnValue = field.deleteElement(index);
		    }
		}
	      catch (RemoteException rx)
		{
		  throw new RuntimeException("Could not change owner field: " + rx);
		}
	    }
	}
      else
	{
	  System.out.println("Where did this setValuePerformed come from?");
	}

      if (returnValue)
	{
	  parent.parent.getgclient().somethingChanged = true;
	}

      return returnValue;
    }

}
