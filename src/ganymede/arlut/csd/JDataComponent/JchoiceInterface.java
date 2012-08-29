
/*
   JchoiceInterface.java

   Created: 1 Oct 1996

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

package arlut.csd.JDataComponent;


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



