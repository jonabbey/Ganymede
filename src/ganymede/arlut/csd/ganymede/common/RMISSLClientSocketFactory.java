/*

   RMISSLServerSocketFactory.java

   Created: 27 August 2004

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT

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

package arlut.csd.ganymede.common;

import arlut.csd.Util.PackageResources;
import arlut.csd.ganymede.server.Ganymede;

import java.io.*;
import java.net.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.security.KeyStore;
import javax.net.ssl.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                       RMISSLClientSocketFactory

------------------------------------------------------------------------------*/

public class RMISSLClientSocketFactory implements RMIClientSocketFactory, Serializable {

  static final long serialVersionUID = -1503453227599275210L;

  private int _hashCode = "arlut.csd.ganymede.common.RMISSLClientSocketFactory".hashCode();

  private static String certsResource = "client_ssl_cert.jks";
  private static int counter = 0;
  private static final boolean socketDebug = false;
  private static final boolean mrShouty = false;
  private static RMISSLClientListener listener = null;

  /**
   * This variable is true if this class has been used to create an
   * SSL socket since JVM startup.  We use this as a proxy for whether
   * or not we are communicating with the Ganymede server in SSL mode.
   */

  private static boolean enabled = false;
  private transient SSLSocketFactory sf;

  /**
   * <P>This static method is used to register an optional listener on
   * the Ganymede client, to receive notification when an RMI SSL
   * socket is opened.</P>
   */

  public synchronized static void setSSLClientListener(RMISSLClientListener listener)
  {
    RMISSLClientSocketFactory.listener = listener;
  }

  /**
   * Returns true if this RMISSLClientSocketFactory has been invoked
   * to create a socket since JVM startup.
   */

  public synchronized static boolean isSSLEnabled()
  {
    return RMISSLClientSocketFactory.enabled;
  }

  /* -- */

  public RMISSLClientSocketFactory()
  {
  }

  public Socket createSocket(String host, int port) throws IOException
  {
    synchronized (arlut.csd.ganymede.common.RMISSLClientSocketFactory.class)
      {
        RMISSLClientSocketFactory.enabled = true;
      }

    if (socketDebug)
      {
        synchronized (arlut.csd.ganymede.common.RMISSLClientSocketFactory.class)
          {
            System.err.println("Creating client socket # " + counter + " to host " + host + " on port " + port);
            counter++;
          }

        RMISSLClientSocketFactory.printCallStack();
      }

    SSLSocket sock = (SSLSocket) getSF().createSocket(host, port);

    SSLSession session = sock.getSession();
    String cipherSuite = session.getCipherSuite();

    if (mrShouty)
      {
        System.err.println("RMISSLClientSocketFactory: created SSL socket to host " + host +
                           " on port " + port + ", using " + cipherSuite);
      }

    synchronized (RMISSLClientSocketFactory.class)
      {
        if (RMISSLClientSocketFactory.listener != null)
          {
            RMISSLClientSocketFactory.listener.notifySSLClient(host, port, cipherSuite);
          }
      }

    return sock;
  }

  public boolean equals(Object object)
  {
    if (object instanceof arlut.csd.ganymede.common.RMISSLClientSocketFactory)
      {
        return true;
      }

    return false;
  }

  public int hashCode()
  {
    return _hashCode;
  }

  private synchronized SSLSocketFactory getSF()
  {
    if (sf != null)
      {
        if (socketDebug)
          {
            System.err.println("getSF() returning a pre-existing SocketFactory");
          }

        return sf;
      }

    if (socketDebug)
      {
        System.err.println("Creating client socket factory");
        RMISSLClientSocketFactory.printCallStack();
      }

    try
      {
        TrustManagerFactory tmf;
        KeyStore ks;
        SSLContext ctx;

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
        tmf = TrustManagerFactory.getInstance(factoryID);
        ks = KeyStore.getInstance("JKS");

        InputStream x = PackageResources.getPackageResourceAsStream(certsResource, this.getClass());

        if (x == null)
          {
            throw new RuntimeException("Hey, couldn't load " + certsResource);
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

                System.err.println("Read " + count + " bytes from " + certsResource);
              }

            x.close();
          }

        ks.load(PackageResources.getPackageResourceAsStream(certsResource, this.getClass()), null);
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

  /**
   * This is a convenience method used by the server to generate a
   * stack trace print from a server code.
   */

  private static void printCallStack()
  {
    try
      {
        throw new RuntimeException("TRACE");
      }
    catch (RuntimeException ex)
      {
        ex.printStackTrace();
      }
  }
}
