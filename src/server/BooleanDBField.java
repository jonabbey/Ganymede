/*
   GASH 2

   BooleanDBField.java

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
                                                                           class
                                                                  BooleanDBField

------------------------------------------------------------------------------*/

public class BooleanDBField extends DBField {

  boolean value;

  /* -- */

  BooleanDBField(DataInputStream in, DBObjectBaseField definition) throws IOException
  {
    this.definition = definition;
    receive(in);
  }

  public BooleanDBField(boolean value)
  {
    this.definition = null;
    this.value = value;
  }

  void emit(DataOutputStream out) throws IOException
  {
    out.writeBoolean(value);
  }

  void receive(DataInputStream in) throws IOException
  {
    value = in.readBoolean();
  }

  public boolean value()
  {
    return value;
  }

  public Object key()
  {
    return new Boolean(value);
  }
}

