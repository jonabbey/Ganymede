
/*
   JFieldWrapper.java

   
   Created: 1 Oct 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin
*/

/**
 *
 *  This file contains classes that will provide a decorator of sorts for each field component
 *  that will be added to the containerPanel. There will be two types of
 *  wrappers. (maybe?)  One will contain a label and a space for the field component.
 *  The other one will contain a space for a field component and a delete ("X") button.
 *  The latter is designed to wrap the field components contained within a vectorPanel.


 ****NOTE:  Add functionality to display a field comment string as part of a pop up dialog box
            The right mouse button will activate the dialog box.

 */

package arlut.csd.JDataComponent;

import com.sun.java.swing.*;

import java.awt.*;
import gjt.Box;

public class JFieldWrapper extends JPanel {

  Component my_field = null;

  public JFieldWrapper(String fieldname)
  {
    if (fieldname==null)
      throw new IllegalArgumentException("Error: handle to the name of the field is null");

    setLayout(new BorderLayout());

    JLabel l = new JLabel(fieldname);

    add("West",l);
  }

  public JFieldWrapper(String fieldname,Component field)
  {
    this(fieldname);

    my_field = field;

    add("East",my_field);

  }

  public void highLight()
  {

    // Set the background color to red and foreground color to black

  }

}
