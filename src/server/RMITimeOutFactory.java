/*

   RMITimeOutFactory.java

   This class provides an RMISocketFactory to limit/reduce connection
   timeouts when we open an RMI connection to a client.
 
   Created: 3 September 2003
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2003/09/04 00:22:02 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.rmi.server.*;
import java.net.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               RMITimeOutFactory

------------------------------------------------------------------------------*/

/**
 * <p>This class provides an RMISocketFactory to limit/reduce
 * connection timeouts when we open an RMI connection to a client.
 * The {@link arlut.csd.ganymede.GanymedeServer GanymedeServer} class
 * has to set this factory on the RMI system.</p>
 */

final public class RMITimeoutFactory extends RMISocketFactory {

  private int timeout;

  /* -- */

  /**
   * <p>Constructor for RMISocketFactory.
   *
   * @param timeout The time, in seconds, to wait before deciding that
   * we have failed to connect.
   */

  public RMITimeoutFactory(int timeout)
  {
    this.timeout = timeout;
  }

  public Socket createSocket(String host, int port) throws IOException
  {
    Socket ret = getDefaultSocketFactory().createSocket(host, port);
    ret.setSoTimeout(timeout * 1000);
    return ret;
  }

  public ServerSocket createServerSocket(int port) throws IOException
  {
    return getDefaultSocketFactory().createServerSocket(port);
  }
}
