/*

   RMISSLServerSocketFactory.java
 
   Created: 27 August 2004
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
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

package arlut.csd.ddroid.common;

import arlut.csd.Util.PackageResources;

import java.io.*;
import java.net.*;
import java.rmi.server.*;
import java.security.KeyStore;
import javax.net.ssl.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                       RMISSLClientSocketFactory

------------------------------------------------------------------------------*/

public class RMISSLClientSocketFactory implements RMIClientSocketFactory, Serializable {

  private int _hashCode = "arlut.csd.ddroid.common.RMISSLClientSocketFactory".hashCode();

  private static String certsResource = "client.truststore";

  private transient SSLSocketFactory sf;

  private synchronized SSLSocketFactory getSF()
  {
    if (sf != null)
      {
	return sf;
      }

    try
      {
	TrustManagerFactory tmf;
	KeyStore ks;
	SSLContext ctx;

	ctx = SSLContext.getInstance("TLS");
	tmf = TrustManagerFactory.getInstance("SunX509");
	ks = KeyStore.getInstance("JKS");

	ks.load(PackageResources.getPackageResourceAsStream(certsResource, null), null);
	tmf.init(ks);
	ctx.init(null, tmf.getTrustManagers(), null);

	sf = ctx.getSocketFactory();
      }
    catch (Exception e)
      {
	e.printStackTrace();
      }

    return sf;
  }

  public Socket createSocket(String host, int port) throws IOException
  {
    return getSF().createSocket(host, port);
  }

  public boolean equals(Object object)
  {
    if (object instanceof arlut.csd.ddroid.common.RMISSLClientSocketFactory)
      {
	return true;
      }

    return false;
  }

  public int hashCode()
  {
    return _hashCode;
  }
}
