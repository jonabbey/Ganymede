/*

   JsetValueCallback.java

   Created: 18 June 1996

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

/*------------------------------------------------------------------------------
                                                                       interface
                                                               JsetValueCallback

------------------------------------------------------------------------------*/

/** 
 * Client-side interface used to allow callback to be done from GUI
 * components in the arlut.csd.JDataComponent package to the container
 * which contains them.
 *
 * @version $Id$
 * @author Navin Manohar 
 */

public interface JsetValueCallback
{
  /**
   *
   * Accept a status update from a GUI component in the arlut.csd.JDataComponent
   * package.  This method throws an RMI remote exception to allow the client
   * to call the server for value verification from within a setValuePerformed()
   * method.  GUI components that call setValuePerformed() should treate an RMI
   * RemoteException as a failure and not display the proposed value change.
   *
   * @return true if the callback accepted the change and the GUI component should
   * go ahead and display the change made by the user.
   *
   */

  public boolean setValuePerformed(JValueObject v) throws java.rmi.RemoteException;
}
