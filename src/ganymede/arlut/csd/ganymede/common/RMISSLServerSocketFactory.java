/*

   RMISSLServerSocketFactory.java
 
   Created: 27 August 2004

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

import arlut.csd.Util.PackageResources;
import arlut.csd.ganymede.server.Ganymede;

import java.io.*;
import java.net.*;
import java.rmi.server.*;
import javax.net.ssl.*;
import java.security.KeyStore;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

/*------------------------------------------------------------------------------
                                                                           class
                                                       RMISSLServerSocketFactory

------------------------------------------------------------------------------*/

public class RMISSLServerSocketFactory implements RMIServerSocketFactory, Serializable {

  static final long serialVersionUID = -7421176607557939283L;

  private int _hashCode = "arlut.csd.ganymede.common.RMISSLServerSocketFactory".hashCode();

  private static String passphrase = "ganypassphrase";
  private static String keysResource = "GanymedeSSLServerKeys";
  private static final boolean socketDebug = false;

  private static int counter = 0;

  private transient SSLServerSocketFactory ssf;

  /* -- */

  public RMISSLServerSocketFactory()
  {
  }

  public ServerSocket createServerSocket(int port) throws IOException
  {
    if (socketDebug)
      {
        synchronized (arlut.csd.ganymede.common.RMISSLServerSocketFactory.class)
          {
            System.err.println("Creating server socket # " + counter + " on port " + port);
            counter++;
          }

        Ganymede.printCallStack();
      }

    return getSSF().createServerSocket(port);
  }

  public boolean equals(Object object)
  {
    if (object instanceof arlut.csd.ganymede.common.RMISSLServerSocketFactory)
      {
        return true;
      }

    return false;
  }

  public int hashCode()
  {
    return _hashCode;
  }

  private synchronized SSLServerSocketFactory getSSF()
  {
    if (ssf != null)
      {
        return ssf;
      }

    if (socketDebug)
      {
        System.err.println("Creating server socket factory");
        Ganymede.printCallStack();
      }

    try
      {
        // set up key manager to do server authentication
        SSLContext ctx;
        KeyManagerFactory kmf;
        KeyStore ks;
        char[] pass = passphrase.toCharArray();

        String factoryID = null;

        if (System.getProperty("java.vm.vendor").indexOf("IBM") != -1)
          {
            factoryID = "IbmX509"; // for IBM JVMs
          }
        else
          {
            factoryID = "SunX509";
          }

        ctx = SSLContext.getInstance("TLS");
        kmf = KeyManagerFactory.getInstance(factoryID);
        ks = KeyStore.getInstance("JKS");

        InputStream x = PackageResources.getPackageResourceAsStream(keysResource, this.getClass());

        if (x == null)
          {
            throw new RuntimeException("Hey, couldn't load " + keysResource);
          }
        else
          {
            if (socketDebug)
              {
                int count = 0;

                try
                  {
                    int i = x.read();

                    while (i >= 0)
                      {
                        count++;
                        i = x.read();
                      }
                  }
                catch (IOException ex)
                  {
                    ex.printStackTrace();
                  }
                
                System.err.println("Read " + count + " bytes from " + keysResource);
              }

            x.close();
          }
      
        ks.load(PackageResources.getPackageResourceAsStream(keysResource, this.getClass()), pass);
        kmf.init(ks, pass);
        ctx.init(kmf.getKeyManagers(), null, null);
        
        ssf = ctx.getServerSocketFactory();
      }
    catch (Exception e)
      {
        e.printStackTrace();
      }

    return ssf;
  }
}
