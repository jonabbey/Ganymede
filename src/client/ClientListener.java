/*

   ClientListener.java

   An interface to complement the ClientBase class.  This interface
   must be implemented by any code that creates uses ClientBase to
   talk to the Ganymede server.
   
   Created: 31 March 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin
*/

package arlut.csd.ganymede.client;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                  ClientListener

------------------------------------------------------------------------------*/

/**
 *
 * An interface to complement the ClientBase class.  This interface
 * must be implemented by any code that creates uses ClientBase to
 * talk to the Ganymede server.
 *
 */

public interface ClientListener {

  /**
   *
   * Called when the server forces a disconnect.<br><br>
   *
   * Call getMessage() on the ClientEvent to get the
   * reason for the disconnect.
   *
   * @see arlut.csd.ganymede.client.ClientEvent
   *
   */

  public void disconnected(ClientEvent e);


  /**
   *
   * Called when the ClientBase needs to report something
   * to the client.  The client is expected to then put
   * up a dialog or do whatever else is appropriate.<br><br>
   *
   * Call getMessage() on the ClientEvent to get the
   * message.
   *
   * @see arlut.csd.ganymede.client.ClientEvent
   *
   */

  public void messageReceived(ClientEvent e);
}
