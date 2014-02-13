/*

   FileTransmitter.java

   Server-side interface for a file transmitter that the client can use
   to pull down pieces of a file in order.

   Created: 16 December 2004

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2014
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                 FileTransmitter

------------------------------------------------------------------------------*/

/**
 * Interface that can be used on the server to represent a transmitter
 * that can send a file across the RMI link..
 */

public interface FileTransmitter extends Remote {

  /**
   * <p>This method pulls down the next sequence of bytes from the
   * FileTransmitter.  This method will block if necessary until the
   * data is ready to be transmitted.</p>
   *
   * <p>This method returns null on end of file, and will throw an
   * exception if it is called again after null is returned.</p>
   */

  public byte[] getNextChunk() throws RemoteException;

  /**
   * Consumes everything sent through the XMLTransmitter until the
   * writer side has finished.  This method will block if necessary
   * until the data is ready to be consumed..
   */

  public void drain() throws RemoteException;

  /**
   * This method is called to notify the FileTransmitter that no
   * more of the file will be pulled.
   */

  public void end() throws RemoteException;
}
