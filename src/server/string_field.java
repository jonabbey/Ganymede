/*

   string_field.java

   Remote interface definition.

   Created: 14 November 1996
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 1999/05/07 05:21:38 $
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

import java.rmi.RemoteException;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    string_field

------------------------------------------------------------------------------*/

public interface string_field extends db_field {

  int maxSize() throws RemoteException;
  int minSize() throws RemoteException;

  boolean showEcho() throws RemoteException;
  boolean canChoose() throws RemoteException;
  boolean mustChoose() throws RemoteException;

  String allowedChars() throws RemoteException;
  String disallowedChars() throws RemoteException;
  boolean allowed(char c) throws RemoteException;

  /**
   * <p>This method returns true if this invid field should not
   * show any choices that are currently selected in field
   * x, where x is another field in this db_object.</p>
   */

  boolean excludeSelected(db_field x) throws RemoteException;

  /**
   * <p>Returns a QueryResult encoding a list of valid string choices
   * for this field.  Consult canChoose() and mustChoose() to determine
   * how the results of this method should be treated.</p>
   */

  QueryResult choices() throws RemoteException;

  /**
   * <p>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</p>
   */

  Object choicesKey() throws RemoteException;
}
