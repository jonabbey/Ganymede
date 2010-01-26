/*
   JpopUpCalendar.java

   Created: 11 March 1997

   Module By: Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.JCalendar;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JDialog;

import arlut.csd.JDataComponent.JResetDateObject;
import arlut.csd.JDataComponent.JSetValueObject;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;

import arlut.csd.Util.TranslationService;

/**************************************************************/      


public class JpopUpCalendar extends JDialog implements JsetValueCallback {

  static final boolean debug = false;

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede system.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JCalendar.JpopUpCalendar");

  // ---

  JpanelCalendar panelCal = null;
  JsetValueCallback parent;

  /**
   * We use this to inhibit re-rendering of the panel calendar upon
   * the initial setVisible() call.
   */

  private boolean firstAppearance = true;
  
  /* -- */

  public JpopUpCalendar(Frame parentFrame, GregorianCalendar parentCalendar, JsetValueCallback callback, boolean editable) 
  {
    // "Please Choose A Date And Time"
    // "Selected Date"
    super(parentFrame, editable ? ts.l("init.chooseTitle") : ts.l("init.displayTitle"), false);

    if (callback == null)
      {
	throw new IllegalArgumentException("callback parameter is null");
      }

    parent = callback;
    
    panelCal = new JpanelCalendar(this,parentCalendar,this, editable);

    getContentPane().add(panelCal);
    pack();

    this.setLocationRelativeTo(parentFrame);
  }

  public boolean setValuePerformed(JValueObject vObj)
  {
    boolean b = false;

    if (debug)
      {
	System.out.println("popUp setValueperformed called");
      }

    if (vObj instanceof JSetValueObject)
      {
        JSetValueObject jsvobj = (JSetValueObject) vObj;

        jsvobj.setSource(this);

        try 
          {
            b = parent.setValuePerformed(jsvobj);
          }
        catch (java.rmi.RemoteException e) 
          {
          }
      }
    else if (vObj instanceof JResetDateObject)
      {
        JResetDateObject jrdobj = (JResetDateObject) vObj;

        jrdobj.setSource(this);

        try 
          {
            b = parent.setValuePerformed(jrdobj);
          }
        catch (java.rmi.RemoteException e) 
          {
          }
      }

    return b;
  }
  
  public void setVisible(boolean visibility)
  {
    if (visibility)
      {
	if (!firstAppearance)
	  {
	    panelCal.displaySelectedPage();
	  }
	else
	  {
	    firstAppearance = false;
	  }
      }

    super.setVisible(visibility);
  }

  public void update() 
  {
    panelCal.update();
  }
}
