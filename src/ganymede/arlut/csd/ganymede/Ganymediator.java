/*

   Ganymediator.java

   This interface provides a means whereby clients can provide
   additional information to the server in response to a dialog
   request.

   The name for this class is the fault of el Webjefe Felipe Campos,
   felipe@uts.cc.utexas.edu
   
   Created: 27 January 1998
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 1999/04/07 01:14:26 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.util.Hashtable;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    Ganymediator

------------------------------------------------------------------------------*/

/**
 * <p>This interface provides a means whereby clients can provide
 * additional information to the server in response to a dialog
 * request.</p>
 *
 * <p>The name for this class is the fault of el Webjefe Felipe Campos,
 * felipe@uts.cc.utexas.edu.</p>
 *
 * @see arlut.csd.JDialog.StringDialog
 */

public interface Ganymediator extends Remote {

  /**
   * This method is used to provide feedback to the server from a client
   * in response to a specific request. 
   *
   * @param returnHash a hashtable mapping strings to values.  The strings
   * are titles of fields specified in a dialog that was provided to the
   * client.  If returnHash is null, this corresponds to the user hitting
   * cancel on such a dialog.
   *
   */

  public ReturnVal respond(Hashtable returnHash) throws RemoteException;
}
