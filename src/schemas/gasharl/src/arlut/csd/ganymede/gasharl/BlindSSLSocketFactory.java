/*

   BlindSSLSocketFactory.java

   An SSL Socket Factory class that does not refuse connection if we
   don't have a certificate available to validate the server.

   Used during development of our gasharl ExchangeStoreTask.

   This was copied from http://blog.platinumsolutions.com/files/BlindSSLSocketFactory.java.txt

*/

package arlut.csd.ganymede.gasharl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Class to create an SSL Socket without checking validity of certificate signer.
 */

public class BlindSSLSocketFactory extends SocketFactory
{
  private static SocketFactory blindFactory = null;

  /**
   * Builds an all trusting "blind" ssl socket factory.
   */

  static
  {
    // create a trust manager that will purposefully fall down on the
    // job

    TrustManager[] blindTrustMan = new TrustManager[] {
      new X509TrustManager()
      {
	public X509Certificate[] getAcceptedIssuers()
	{
	  return null;
	}

	public void checkClientTrusted(X509Certificate[] c, String a)
	{
	}

	public void checkServerTrusted(X509Certificate[] c, String a)
	{
	}
      }
    };

    // create our "blind" ssl socket factory with our lazy trust manager

    try
      {
	SSLContext sc = SSLContext.getInstance("SSL");
	sc.init(null, blindTrustMan, new java.security.SecureRandom());
	blindFactory = sc.getSocketFactory();
      }
    catch (GeneralSecurityException e)
      {
	e.printStackTrace();
      }
  }

  /**
   * @see javax.net.SocketFactory#getDefault()
   */

  public static SocketFactory getDefault()
  {
    return new BlindSSLSocketFactory();
  }

  /**
   * @see javax.net.SocketFactory#createSocket(java.lang.String, int)
   */

  public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
  {
    return blindFactory.createSocket(arg0, arg1);
  }

  /**
   * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int)
   */

  public Socket createSocket(InetAddress arg0, int arg1) throws IOException
  {
    return blindFactory.createSocket(arg0, arg1);
  }

  /**
   * @see javax.net.SocketFactory#createSocket(java.lang.String, int,
   * java.net.InetAddress, int)
   */

  public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException
  {
    return blindFactory.createSocket(arg0, arg1, arg2, arg3);
  }

  /**
   * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int,
   * java.net.InetAddress, int)
   */

  public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
  {
    return blindFactory.createSocket(arg0, arg1, arg2, arg3);
  }
}
