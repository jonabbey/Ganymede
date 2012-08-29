/*

   RMISSLClientListener.java

   This interface is used to receive notifications when an RMI SSL
   connection is formed on the Ganymede client.
 
   Created: 13 January 2005
   Last Changed By: $Author: broccol $

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.common;

/*------------------------------------------------------------------------------
                                                                           class
                                                            RMISSLClientListener

------------------------------------------------------------------------------*/

/**
 * <p>This interface is used to receive notifications when an RMI SSL
 * connection is formed on the Ganymede client.  Used in conjunction with
 * {@link arlut.csd.ganymede.common.RMISSLClientSocketFactory}.</p>
 */

public interface RMISSLClientListener {

  /**
   * <p>This method is called when an RMI SSL client socket is created on the
   * Ganymede client.</p>
   */

  public void notifySSLClient(String host, int port, String cipherSuite);
}
