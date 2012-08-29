/*

   NameSpace.java

   Remote interface for NameSpace viewing/editing
   
   Created: 21 April 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2004
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

package arlut.csd.ganymede.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                       NameSpace

------------------------------------------------------------------------------*/

/**
 * <P>Client side interface for the Ganymede
 * {@link arlut.csd.ganymede.server.DBNameSpace DBNameSpace} class.  This interface
 * allows the admin console to be able to view the name of the namespace and
 * view/set the case sensitivity of this namespace.</P>
 */

public interface NameSpace extends Remote {
  public String getName() throws RemoteException;
  public boolean isCaseInsensitive() throws RemoteException;
  public boolean setName(String name) throws RemoteException;
  public void setInsensitive(boolean b) throws RemoteException;
}
