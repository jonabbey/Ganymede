/*

   Ganymede.java

   Server main module

   Created: 17 January 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package gash;

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class Ganymede {
  
  public static RegistryImpl registry;
  
  public static void main(String argv[]) 
  {

    System.setSecurityManager(new StubSecurityManager());

    try
      {
	registry = new RegistryImpl(7211);
      }
    catch (RemoteException ex)
      {
	System.err.println("Couldn't establish registry: " + ex);
	return;
      }

    // Create a Server object

    try
      {
	GanymedeServer server = new GanymedeServer(10);
	registry.bind("ganymede.server", server);
      }
    catch (Exception ex)
      {
	System.err.println("Couldn't establish server binding: " + ex);
	return;
      }

    System.err.println("Setup and bound server object OK");
  }
}
