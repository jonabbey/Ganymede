/* 
   Resource class for use with StringDialog.java
*/

package arlut.csd.JDialog;

import java.lang.String;
import java.util.Vector;

public class choiceThing {
  
  String choiceLabel;
  Vector items;

  public choiceThing(String label, Vector Items)
    {
      this.choiceLabel = label;
      this.items = Items;
    }

  public String getLabel()
    {
      return choiceLabel;
    }

  public Vector getItems()
    {
      return items;
    }
}
