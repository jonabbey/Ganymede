/*
   GASH 2

   DBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import csd.DBStore.*;
import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                  abstract class
                                                                         DBField

------------------------------------------------------------------------------*/
public abstract class DBField implements Cloneable {
  
  // -- abstract parent

  DBObjectBaseField definition;		    // used for fields registered in the
					    // object bases

  Object key();			// key used to represent value in a hash of the
				// values in a given set of fields

  boolean unmark(DBEditSet editset, DBNameSpace namespace)
  {
    return namespace.unmark(editset, this.key());
  }

  boolean mark(DBEditSet editset, DBNameSpace namespace)
  {
    return namespace.mark(editset, this.key(), this);
  }
}
