/*
   GASH 2

   DBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                  abstract class
                                                                         DBField

------------------------------------------------------------------------------*/
public abstract class DBField implements Cloneable {
  
  DBObject owner;
  DBObjectBaseField definition;		    // used for fields registered in the
					    // object bases

  /* -- */

  abstract Object key();	// key used to represent value in a hash of the
				// values in a given set of fields

  abstract void emit(DataOutput out) throws IOException;

  public boolean equals(Object obj)
  {
    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    DBField f = (DBField) obj;

    return f.key().equals(this.key());
  }

  boolean unmark(DBEditSet editset, DBNameSpace namespace)
  {
    return namespace.unmark(editset, this.key());
  }

  boolean mark(DBEditSet editset, DBNameSpace namespace)
  {
    return namespace.mark(editset, this.key(), this);
  }
}
