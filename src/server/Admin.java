/*

   Admin.java

   Server-side interface for the Admin callbacks.

   The methods in the Admin interface are implemented by
   the Admin console, and are called by the server as
   appropriate.

   Created: 28 May 1996
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 2001/02/08 22:52:11 $
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
import java.util.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                           Admin

------------------------------------------------------------------------------*/

/**
 * <P>RMI interface that must be implemented by clients that connect to the
 * {@link arlut.csd.ganymede.GanymedeServer GanymedeServer} through the 
 * {@link arlut.csd.ganymede.GanymedeServer#admin(arlut.csd.ganymede.Admin) admin()}
 * method.  Basically, the admin console and other programs that can act like the
 * admin console have to implement this interface someplace.</P>
 *
 * <P>The server uses this interface to call methods on the admin console to
 * update the console's status displays.  The server also uses this interface
 * to authenticate the console with the getName() and getPassword() methods.</P> 
 */

public interface Admin extends Remote {
  String getName() throws RemoteException;
  String getPassword() throws RemoteException;
  void forceDisconnect(String reason) throws RemoteException;

  void setServerStart(Date date) throws RemoteException;
  void setLastDumpTime(Date date) throws RemoteException;
  void setTransactionsInJournal(int trans) throws RemoteException;
  void setObjectsCheckedOut(int objs) throws RemoteException;
  void setLocksHeld(int locks) throws RemoteException;
  void setMemoryState(long freeMemory, long totalMemory) throws RemoteException;

  void changeState(String state) throws RemoteException;
  void changeStatus(String status) throws RemoteException;
  void changeAdmins(String adminStatus) throws RemoteException;
  void changeUsers(Vector entries) throws RemoteException;
  void changeTasks(Vector tasks) throws RemoteException;
}
