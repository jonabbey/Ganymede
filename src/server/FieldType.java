/*

   FieldType.java

   Hackified enumeration of defined field types
   
   Created: 15 April 1997
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

public interface FieldType {
  static final short FIRSTFIELD = 0;
  static final short BOOLEAN = 0;
  static final short NUMERIC = 1;
  static final short DATE = 2;
  static final short STRING = 3;
  static final short INVID = 4;
  static final short PERMISSIONMATRIX = 5;
  static final short PASSWORD = 6;
  static final short IP = 7;
  static final short LASTFIELD = 7;
}
