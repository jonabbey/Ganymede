/*
   GASH 2

   StringDBField.java

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
                                                                   StringDBField

------------------------------------------------------------------------------*/

public class StringDBField extends DBField {

  String value;

  /* -- */

  StringDBField(DataInput in, DBObjectBaseField definition) throws IOException
  {
    this.definition = definition;
    receive(in);
  }

  public StringDBField(String value)
  {
    this.definition = null;
    this.value = value;
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeUTF(value);
  }

  void receive(DataInput in) throws IOException
  {
    value = in.readUTF();
  }

  public String value()
  {
    return value;
  }

  public Object key()
  {
    return value;
  }
}
