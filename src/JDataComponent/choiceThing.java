/* 
   Resource class for use with StringDialog.java
*/

package arlut.csd.JDialog;

import java.lang.String;
import java.util.Vector;

public class choiceThing {
  
  String choiceLabel;
  Vector items;
  Object selected;

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
