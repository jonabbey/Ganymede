/*

   FileTransmitter.java

   Server-side interface for a file transmitter that the client can use
   to pull down pieces of a file in order.

   Created: 16 December 2004
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                 FileTransmitter

------------------------------------------------------------------------------*/

/**
 * <p>Interface that can be used on the server to represent a transmitter
 * that can send a file across the RMI link..</p>
 */

public interface FileTransmitter extends Remote {

  /**
   * <p>This method pulls down the next sequence of bytes from the
   * FileTransmitter.  This method will block if necessary until the
   * data is ready to be transmitted.</p>
   *
   * <p>This method returns null on end of file, and will throw an excepti.</p>
   */

  public byte[] getNextChunk() throws RemoteException;

  /**
   * <p>This method is called to notify the FileTransmitter that no
   * more of the file will be pulled.</p>
   */
  
  public void end() throws RemoteException;
}
