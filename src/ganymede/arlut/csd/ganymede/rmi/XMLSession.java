/*
   GASH 2

   XMLSession.java

   The GANYMEDE object storage system.

   Created: 1 August 2000

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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

import java.rmi.RemoteException;

import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      XMLSession

------------------------------------------------------------------------------*/

public interface XMLSession extends java.rmi.Remote {

  /**
   * <p>This method returns a remote reference to the underlying
   * GanymedeSession in use on the server.</p>
   */

  Session getSession() throws RemoteException;

  /**
   * <p>This method is called repeatedly by the XML client in order to
   * send the next packet of XML data to the server.  If the server
   * has detected any errors in the already-received XML stream,
   * xmlSubmit() may return a non-null ReturnVal with a description of
   * the failure.  Otherwise, the xmlSubmit() method will enqueue the
   * XML data for the server's continued processing and immediately
   * return a null value, indicating success.  The xmlSubmit() method
   * will only block if the server has filled up its internal buffers
   * and must wait to digest more of the already submitted XML.</p>
   */

  ReturnVal xmlSubmit(byte[] bytes) throws RemoteException;

  /**
   * <p>This method is called by the XML client once the end of the XML
   * stream has been transmitted, whereupon the server will attempt
   * to finalize the XML transaction and return an overall success or
   * failure message in the ReturnVal.</p>
   */

  ReturnVal xmlEnd() throws RemoteException;

  /**
   * <p>This method is called by the XML client on a dedicated thread
   * to pull stderr messages from the server.</p>
   *
   * <p>This call will block on the server until err stream data is
   * available, but will always block for at least a tenth of a second
   * so that the client doesn't loop on getNextErrChunk() too
   * fast.</p>
   *
   * <p>This method will return null after the server closes its error
   * stream.</p>
   */

  String getNextErrChunk() throws RemoteException;

  /**
   * <p>This method can be called to inform the XMLSession that
   * no more XML will be transmitted.</p>
   */

  void abort() throws RemoteException;
}
