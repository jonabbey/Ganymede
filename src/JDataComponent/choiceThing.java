/*

   choiceThing.java

   Resource class for use with StringDialog.java
   
   Created: 16 June 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDialog;

import java.lang.String;
import java.util.Vector;


/*------------------------------------------------------------------------------
                                                                           class
                                                                     choiceThing

------------------------------------------------------------------------------*/


public class choiceThing implements java.io.Serializable {
  
  String choiceLabel;
  Vector items;
  Object selected;

  /* -- */

  public choiceThing(String label, Vector Items)
    {
      this(label, Items, null);
    }

  public choiceThing(String label, Vector Items, Object selectedObject)
    {
      this.choiceLabel = label;
      this.items = Items;
      this.selected = selectedObject;
    }

  public String getLabel()
    {
      return choiceLabel;
    }

  public Vector getItems()
    {
      return items;
    }

  public Object getSelectedItem()
    {
      return selected;
    }
}
