/*

   FieldType.java

   Hackified enumeration of defined field types
   
   Created: 15 April 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.common;

/**
 * <p>Hackified enumeration of defined types for database fields in
 * the Ganymede {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
 *
 * <p>Ganymede classes can implement this interface to have access to
 * these constants.</p>
 */

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
  static final short FLOAT = 8;
  static final short LASTFIELD = 8;
}
