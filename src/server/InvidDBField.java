/*
   GASH 2

   InvidDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;
import java.util.*;


/*------------------------------------------------------------------------------
                                                                           class
                                                                    InvidDBField

------------------------------------------------------------------------------*/

public class InvidDBField extends DBField {

  Invid value;

  /* -- */

  InvidDBField(DataInputStream in, DBObjectBaseField definition) throws IOException
  {
    this.definition = definition;
    receive(in);
  }

  public InvidDBField(Invid value)
  {
    this.definition = null;
    this.value = value;
  }

  void emit(DataOutputStream out) throws IOException
  {
    out.writeShort(value.getType());
    out.writeInt(value.getNum());
  }

  void receive(DataInputStream in) throws IOException
  {
    value = new Invid(in.readShort(), in.readInt());
  }

  public Invid value()
  {
    return value;
  }

  public Object key()
  {
    return value;
  }
}
