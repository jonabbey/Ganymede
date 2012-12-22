/*

   adminAsyncMessage.java

   Created: 4 September 2003

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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


/*------------------------------------------------------------------------------
                                                                           class
                                                               adminAsyncMessage

------------------------------------------------------------------------------*/

/**
 * <p>The adminAsyncMessage class is used by the Ganymede server to
 * pass asynchronous messages to the Ganymede admin console.  The
 * Ganymede console repeatedly calls {@link
 * arlut.csd.ganymede.server.serverClientAsyncResponder#getNextMsg()
 * arlut.csd.ganymede.server.serverClientAsyncResponder#getNextMsgs()
 * } to receive asynchonous notifications from the server.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class adminAsyncMessage implements java.io.Serializable {

  static final long serialVersionUID = -2607353339584658316L;

  static public final int FIRST = 0;
  static public final int SETSERVERSTART = 0;
  static public final int SETLASTDUMPTIME = 1;
  static public final int SETTRANSACTIONS = 2;
  static public final int SETOBJSCHECKOUT = 3;
  static public final int SETLOCKSHELD = 4;
  static public final int CHANGESTATE = 5;
  static public final int LOGAPPEND = 6;
  static public final int CHANGEADMINS = 7;
  static public final int CHANGEUSERS = 8;
  static public final int CHANGETASKS = 9;
  static public final int SETMEMORYSTATE = 10;
  static public final int FORCEDISCONNECT = 11;
  static public final int LAST = 11;

  /* --- */

  /**
   * <p>Identifies what RMI call is going to need to be made to the
   * remote Client.</p>
   */

  int method;

  /**
   * Generic RMI call parameters to be sent to the remote client.
   */

  Object params[];

  /* -- */

  public adminAsyncMessage(int method, Object param)
  {
    if (method < FIRST || method > LAST)
      {
        throw new IllegalArgumentException("bad method code: " + method);
      }

    this.method = method;
    params = new Object[1];
    params[0] = param;
  }

  public adminAsyncMessage(int method, int param)
  {
    if (method < FIRST || method > LAST)
      {
        throw new IllegalArgumentException("bad method code: " + method);
      }

    this.method = method;
    params = new Object[1];
    params[0] = Integer.valueOf(param);
  }

  public adminAsyncMessage(int method, Object param[])
  {
    if (method < FIRST || method > LAST)
      {
        throw new IllegalArgumentException("bad method code: " + method);
      }

    this.method = method;
    params = param;
  }

  public String toString()
  {
    StringBuilder result = new StringBuilder();

    switch (method)
      {
      case SETSERVERSTART:
        result.append("setServerStart");
        break;

      case SETLASTDUMPTIME:
        result.append("setLastDumpTime");
        break;

      case SETTRANSACTIONS:
        result.append("setTransactionsInJournal");
        break;

      case SETOBJSCHECKOUT:
        result.append("setObjectsCheckedOut");
        break;

      case SETLOCKSHELD:
        result.append("setLocksHeld");
        break;

      case CHANGESTATE:
        result.append("changeState");
        break;

      case LOGAPPEND:
        result.append("logAppend");
        break;

      case CHANGEADMINS:
        result.append("changeAdmins");
        break;

      case CHANGEUSERS:
        result.append("changeUsers");
        break;

      case CHANGETASKS:
        result.append("changeTasks");
        break;

      case SETMEMORYSTATE:
        result.append("setMemoryState");
        break;

      default:
        result.append("<" + method + ">??");
      }

    result.append("(");

    for (int i = 0; i < params.length; i++)
      {
        if (i > 0)
          {
            result.append(", ");
          }

        result.append(params[i]);
      }

    result.append(")");

    return result.toString();
  }

  public int getMethod()
  {
    return method;
  }

  public int getParamCount()
  {
    return params.length;
  }

  public Object getParam(int index)
  {
    return params[index];
  }

  public Object[] getParams()
  {
    return params;
  }

  public String getString(int index)
  {
    return (String) params[index];
  }

  public int getInt(int index)
  {
    return ((Integer) params[index]).intValue();
  }

  public long getLong(int index)
  {
    return ((Long) params[index]).longValue();
  }
}
