/*

   FileReceiverBase.java

   Standard remotely accessible FileReceiver object for use in Ganymede
   clients.  By using a single class that implements the FileReceiver
   interface, we only need to have a single stub in the server's jar
   file.

   Created: 17 September 2000
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2000/09/17 10:04:27 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.ReturnVal;
import arlut.csd.ganymede.FileReceiver;
import java.rmi.*;
import java.rmi.server.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                FileReceiverBase

------------------------------------------------------------------------------*/

/**
 * <p>Standard remotely accessible {@link
 * arlut.csd.ganymede.FileReceiver FileReceiver} object for use in
 * Ganymede clients.  By using a single class that implements the
 * FileReceiver interface, we only need to have a single stub in the
 * server's jar file.</p> 
 */

public class FileReceiverBase extends UnicastRemoteObject implements FileReceiver, Unreferenced {

  private FileReceiver localReceiver;

  /* -- */
  
  public FileReceiverBase(FileReceiver localReceiver) throws RemoteException
  {
    super();	// UnicastRemoteObject can throw RemoteException 

    this.localReceiver = localReceiver;
  }
  
  /**
   * <p>This method is used to send chunks of a file, in order, to the
   * FileReceiver.  The FileReceiver can return a non-successful ReturnVal
   * if it doesn't want to stop receiving the file.  A null return value
   * indicates success, keep sending.</p>
   */

  public ReturnVal sendBytes(byte[] bytes) throws RemoteException
  {
    return localReceiver.sendBytes(bytes);
  }

  /**
   * <p>This method is used to send chunks of a file, in order, to the
   * FileReceiver.  The FileReceiver can return a non-successful ReturnVal
   * if it doesn't want to stop receiving the file.  A null return value
   * indicates success, keep sending.</p>
   */
  
  public ReturnVal sendBytes(byte[] bytes, int offset, int len) throws RemoteException
  {
    return localReceiver.sendBytes(bytes, offset, len);
  }
  
  /**
   * <p>This method is called to notify the FileReceiver that no more
   * of the file will be transmitted.  The boolean parameter will
   * be true if the file was completely sent, or false if the transmission
   * is being aborted by the sender for some reason.</p>
   */
  
  public ReturnVal end(boolean completed) throws RemoteException
  {
    return localReceiver.end(completed);
  }

  /**
   * <p>This method implements the Unreferenced interface.  The RMI system
   * on the client will call this method if the server drops the reference
   * to the FileReceiverBase.  This will allow the client to realize
   * that the file transmission has timed out if the server dies.</p>
   */

  public void unreferenced()
  {
    System.err.println("FileReceiverBase: unreferenced()");

    try
      {
	localReceiver.end(false);
      }
    catch (RemoteException ex)
      {
      }
  }
}
