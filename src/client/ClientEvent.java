/*

   ClientEvent.java

   An event object to pass information from the ClientBase class to
   users thereof. Currently, this only has support for a message
   String, but it is here in case we need some other types in there.
   
   Created: 31 March 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     ClientEvent

------------------------------------------------------------------------------*/

/**
 *
 * An event object to pass information from the ClientBase class to
 * users thereof. Currently, this only has support for a message
 * String, but it is here in case we need some other types in there.
 *
 */

public class ClientEvent {

  private String message;

  /* -- */

  public ClientEvent(String message)
  {
    this.message = message;
  }

  /**
   *
   * Returns the message for this event.
   *
   */

  public String getMessage()
  {
    return message;
  }
}
