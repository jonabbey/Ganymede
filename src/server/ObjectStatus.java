/*

   ObjectStatus.java

   Hackishly enumerated type for DBEditObject status
   
   Created: 15 April 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

public interface ObjectStatus {

  /**
   * Status code for an object in the DBStore that has been checked out for editing.
   */
  static final byte EDITING = 1;

  /**
   * Status code for a newly created object.
   */
  static final byte CREATING = 2;

  /**
   * Status code for a previously existing object that is to be deleted
   */
  static final byte DELETING = 3;

  /**
   * Status code for a newly created object that is to be dropped
   */
  static final byte DROPPING = 4;

}
