
/*
   JchoiceInterface.java

   
   Created: 1 Oct 1996
   Version: 1.1 97/07/16
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.JDataComponent;

import java.awt.*;

/**
 *  
 *  This interface is used to allow a callback to be done from the
 *  ChoiceList to set the component which is attached to that JchoiceList.
 *  The component that is attached to the JchoiceList needs to provide
 *  implementations for the methods defined in this interface.
 */
public interface JchoiceInterface {

  // Variables

  // Interface methods
  
  public void setVal(String choice_str);
  public void notifyComponent();
  public void unAttach();
  public void restoreValue();
}



