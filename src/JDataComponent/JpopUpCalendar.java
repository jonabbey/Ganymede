/*
   JpopUpCalendar.java

   
   Created: 11 March 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JCalendar;

import arlut.csd.JDataComponent.*;
import java.awt.*;
import java.util.*;

import javax.swing.*;

/**************************************************************/      


public class JpopUpCalendar extends JFrame implements JsetValueCallback {

  static final boolean debug = false;

  // ---

  JpanelCalendar panelCal = null;
  JsetValueCallback parent;

  /* -- */

  public JpopUpCalendar(GregorianCalendar parentCalendar, JsetValueCallback callback) 
  {
    super("Please Choose a Date & Time");

    if (callback == null)
      {
	throw new IllegalArgumentException("callback parameter is null");
      }

    parent = callback;
    
    panelCal = new JpanelCalendar(this,parentCalendar,this);
    
    getContentPane().setLayout(new FlowLayout());
    getContentPane().add(panelCal);
    pack();
  }

  public boolean setValuePerformed(JValueObject vObj)
  {
    boolean b = false;

    if (debug)
      {
	System.out.println("popUp setValueperformed called");
      }

    try 
      {
	b = parent.setValuePerformed(new JValueObject(this,(Date)(vObj.getValue())));
      }
    catch (java.rmi.RemoteException e) 
      {
      }

    return b;
  }

  public void update() 
  {
    panelCal.update();
  }
}
