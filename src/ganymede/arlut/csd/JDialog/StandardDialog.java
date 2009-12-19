/*

   StandardDialog.java

   A dialog base class for centered, possibly modal dialogs.

   Created: 4 December 2009

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2009
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

package arlut.csd.JDialog;

import java.awt.Dialog;
import java.awt.Frame;

import java.lang.Class;
import java.lang.reflect.Method;

import javax.swing.JDialog;
import javax.swing.UIManager;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  StandardDialog

------------------------------------------------------------------------------*/

/**
 * A dialog base class for centered, possibly modal dialogs.  On the
 * Mac, document-modal StandardDialogs will appear as sheets.
 *
 * @author Jonathan Abbey
 */

public class StandardDialog extends JDialog {

  /**
   * Enum that we're using to allow subclasses to tell us what type of
   * modality, if any, they want to use.
   *
   * We in turn will use either Java 1.5 or Java 1.6 based methods to
   * tell our superclasses what modality to use.
   *
   * This is a hack so that we can operate successfully on Java 5
   * while taking advantage of the richer modality controls when
   * running on Java 6.
   */

  public static enum ModalityType {
    MODELESS,
    DOCUMENT_MODAL,
    APPLICATION_MODAL,
    TOOLKIT_MODAL
  };

  private boolean already_shown = false;
  private Frame frame;
  private ModalityType modality;

  /* -- */

  public StandardDialog(Frame frame, String title, ModalityType modality)
  {
    super(frame, title, modality!=ModalityType.MODELESS);
    setModality(modality);
    this.frame = frame;
  }

  public void setModality(ModalityType modality)
  {
    this.modality = modality;

    // this bletcherousness is to make the call to Java 6's enhanced
    // setModalityType() method in a way that won't prevent loading and
    // execution on Java 5.

    boolean success = false;

    try
      {
	Class[] classMembers = java.awt.Dialog.class.getDeclaredClasses();
	Class enumClass = null;

	for (Class cl: classMembers)
	  {
	    if ("java.awt.Dialog$ModalityType".equals(cl.getName()) && cl.isEnum())
	      {
		enumClass = cl;
	      }
	  }

	Class[] paramTypes = new Class[] {enumClass};
	Method modalityMethod = java.awt.Dialog.class.getDeclaredMethod("setModalityType", paramTypes);

	Object enumValue = Enum.valueOf(enumClass, modality.name());
	modalityMethod.invoke(this, enumValue);

	success = true;
      }
    catch (Exception ex)
      {
	// expecting ClassNotFoundException if we are running on Java
	// 1.5, but NoSuchMethodException, InvocationTargetException,
	// IllegalAccessException, SecurityExceptionare also checked
	// exceptions that reflection operations can generate
      }

    if (!success)
      {
	setModal(modality != ModalityType.MODELESS);
      }
  }

  /**
   * Returns true if this dialog is being displayed on a Mac with the
   * Mac Look and Feel
   */

  public boolean isRunningOnMac()
  {
    try
      {
	return ("Mac OS X".equals(System.getProperty("os.name")) &&
		"Mac OS X".equals(UIManager.getLookAndFeel().getName()));
      }
    catch (NullPointerException ex)
      {
	return false;
      }
  }

  /**
   * Returns true if this dialog is DOCUMENT_MODAL and running on the
   * Mac with the Mac look and feel.
   */

  public boolean isMacSheet()
  {
    if (modality == ModalityType.DOCUMENT_MODAL && isRunningOnMac())
      {
	return true;
      }

    return false;
  }

  public void setVisible(boolean state)
  {
    if (state && !already_shown)
      {
	if (isMacSheet())
	  {
	    // set it as a modal sheet on the Mac
	    //
	    // XXX nb: Any document modal StandardDialog on the Mac
	    // will need to have a button or buttons to close the
	    // sheet, as sheets on the Mac do not have close controls,
	    // etc.

	    this.setLocationRelativeTo(null);
	    getRootPane().putClientProperty("apple.awt.documentModalSheet", Boolean.TRUE);
	  }
	else
	  {
	    // center to the frame

	    this.setLocationRelativeTo(frame);
	  }

	already_shown = true;
      }

    super.setVisible(state);
  }
}
