/*

   adminSession.java

   Client side remote interface.

   Client side interface definition for the Ganymede adminSession Object.  The
   Ganymede adminSession object holds the state for the Ganymede Admin console.

   Created: 28 May 1996
   Release: $Name:  $
   Version: $Revision: 1.10 $
   Last Mod Date: $Date: 1999/02/10 05:33:42 $
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
import java.util.Date;

public interface adminSession extends Remote {

  // Client/server interface operations

  void        logout() throws RemoteException;

  /**
   *
   * This method lets the admin console explicitly request
   * a refresh.
   *
   */

  void        refreshMe() throws RemoteException;
  boolean     kill(String user) throws RemoteException;
  boolean     killAll() throws RemoteException;
  String      getInfo(String user) throws RemoteException;
  boolean     shutdown() throws RemoteException;
  boolean     dumpDB() throws RemoteException;
  boolean     dumpSchema() throws RemoteException;
  boolean     reloadCustomClasses() throws RemoteException;
  void        runInvidTest() throws RemoteException;
  void        runInvidSweep() throws RemoteException;

  boolean     runTaskNow(String name) throws RemoteException;
  boolean     stopTask(String name) throws RemoteException;
  boolean     disableTask(String name) throws RemoteException;
  boolean     enableTask(String name) throws RemoteException;

  SchemaEdit  editSchema() throws RemoteException;
}
