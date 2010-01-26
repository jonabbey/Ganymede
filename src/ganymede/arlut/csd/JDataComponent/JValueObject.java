
/*
   JValueObject.java

   Base class for a new hierarchy of classes used to pass
   callback data in the arlut.csd.JDataComponent package.

   Formerly, this class was an all-in-one class that tried to encode a
   bunch of different operations, with a mess of different
   constructors and interpretations of parameters.

   Now this class will serve as a base class, and all of the actual
   data transport will be done by operation-specific subclasses.

   Created: 28 Feb 1997


   Module By: Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin.

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.JDataComponent;

import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    JValueObject

------------------------------------------------------------------------------*/

/**
 *
 * <p>A client-side message object used to pass status updates from
 * GUI components in the arlut.csd.JDataComponent package to their
 * containers.  JValueObject supports passing information about
 * scalar and vector value change operations, as well as pop-up
 * menus and error messages.</p>
 *
 * <p>Base class for a new hierarchy of classes used to pass
 * callback data in the arlut.csd.JDataComponent package.</p>
 *
 * <p>Formerly, this class was an all-in-one class that tried to encode a
 * bunch of different operations, with a mess of different
 * constructors and interpretations of parameters.</p>
 *
 * <p>Now this class will serve as a base class, and all of the actual
 * data transport will be done by operation-specific subclasses.</p>
 *
 * <p>Note that we came up with this message type before Sun introduced
 * the 1.1 AWT event model.  Great minds... ;-)</p>
 *   
 * @version $Revision$ $Date$ $Name:  $
 * @author Navin Manohar 
 */

public abstract class JValueObject {

  /**
   * Returns the arlut.csd.JDataComponent GUI component that originated this message.
   */

  public abstract Component getSource();

  /**
   * Returns an auxiliary value.  Used for passing information about pop-up menu items, but may
   * be used for different purposes if needed.
   */

  public abstract Object getParameter();

  /**
   * Returns the index of an item operated on in a vector component.
   */

  public abstract int getIndex();

  /**
   * Returns the index of an item operated on in a vector component.
   */

  public abstract int getIndex2();

  /**
   * Returns the value of the object being affected by this message.
   */

  public abstract Object getValue();
}
