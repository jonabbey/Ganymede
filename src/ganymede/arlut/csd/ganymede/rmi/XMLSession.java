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
   * <p>This method is called by the XML client once the end of the
   * XML stream has been transmitted, whereupon the server will
   * attempt to finalize the XML transaction and return an overall
   * success or failure indication in the ReturnVal.</p>
   *
   * <p>xmlEnd() only returns a success / failure indication in the
   * returned ReturnVal.  In order to get all diagnostic / progress
   * messages explaining the success or failure, the client is obliged
   * to maintain a thread calling getNextErrChunk() until
   * getNextErrChunk() returns null.</p>
   */

  ReturnVal xmlEnd() throws RemoteException;

  /**
   * <p>Returns chunks of diagnostic / progress messages produced on
   * the server during the processing of XML submitted with
   * xmlSubmit().</p>
   *
   * <p>This call will block on the server until more message data is
   * available and will for at least a tenth of a second while the XML
   * is still being processed so that the client doesn't loop on
   * getNextErrChunk() too fast.</p>
   *
   * <p>Once this XMLSession has finished processing the submitted XML
   * and everything in the diagnostic / progress message stream has
   * been retrieved by calls to getNextErrChunk(), getNextErrChunk()
   * will return null.</p>
   *
   * <p>The XML client is meant to run a dedicated thread to
   * repeatedly call this method to collect diagnostic / progress data
   * until getNextErrChunk() returns null.  This thread will generally
   * last beyond the time of the XML client's xmlEnd() call.</p>
   */

  String getNextErrChunk() throws RemoteException;

  /**
   * <p>This method can be called to inform the XMLSession that
   * no more XML will be transmitted.</p>
   */

  void abort() throws RemoteException;
}
