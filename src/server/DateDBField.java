/*
   GASH 2

   DateDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     DateDBField

------------------------------------------------------------------------------*/

public class DateDBField extends DBField {

  Date value;

  /* -- */

  DateDBField(DataInput in, DBObjectBaseField definition) throws IOException
  {
    this.definition = definition;
    receive(in);
  }

  public DateDBField(Date value)
  {
    this.definition = null;
    this.value = value;
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeLong(value.getTime());
  }

  void receive(DataInput in) throws IOException
  {
    value = new Date(in.readLong());
  }

  public Date value()
  {
    return value;
  }

  public Object key()
  {
    return value;
  }
}
