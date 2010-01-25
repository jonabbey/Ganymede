/*

   ClientMessage.java

   Message types used for client messages.
   
   Created: 1 March 2000

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.common;

/**
 * Old-skool 'enumeration' interface for server-to-client messages.
 */

public interface ClientMessage {
  static final int FIRST = 0;
  static final int ERROR = 0;
  static final int BUILDSTATUS = 1;
  static final int SOFTTIMEOUT = 2;
  static final int LOGIN = 3;
  static final int LOGOUT = 4;
  static final int LOGINCOUNT = 5;
  static final int COMMITNOTIFY = 6;
  static final int ABORTNOTIFY = 7;
  static final int LAST = 7;
}
