/*

   JstringListBox.java

   An implementation of JListBox used to display strings.

   Created: 21 Aug 1997

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;
import java.util.Comparator;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import arlut.csd.Util.VecQuickSort;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  JstringListBox

------------------------------------------------------------------------------*/

/**
 * <p>A sorted listbox that handles {@link arlut.csd.JDataComponent.listHandle listHandle}'s.
 * JstringListBox supports pop-up menus and uses the
 * @link arlut.csd.JDataComponent.JsetValueCallback JsetValueCallback}
 * interface to report selection and pop-up menu activity to the registered
 * callback.</p>
 *
 * <p>listHandles are wrappers that can hold both a String and (optionally) a related
 * object, such as an Invid.  The JstringListBox uses them to allow the client to
 * manipulate labeled object pointers.</p>
 *
 * <p>The {@link arlut.csd.JDataComponent.StringSelector StringSelector} class uses
 * JstringListBoxes to support adding or removing Strings and Objects from String
 * and Invid vector fields.</p>
 *
 * @see arlut.csd.ganymede.common.Invid
 * @see arlut.csd.JDataComponent.listHandle
 * @see arlut.csd.JDataComponent.StringSelector
 * @see arlut.csd.JDataComponent.JsetValueCallback
 * @author Mike Mulvaney
 */

public class JstringListBox extends JList implements ActionListener, FocusListener,
                                                     ListSelectionListener,
                                                     MouseListener, MouseMotionListener,
                                                     Comparator {

  static final boolean debug = false;

  // ---

  int
    width,
    popUpIndex = -1;

  DefaultListModel
    model = new DefaultListModel();

  /**
   * <p>If true, this JstringListBox will allow nodes to be dragged up
   * and down in the list.</p>
   */

  private boolean dragOk = false;

  /**
   * <p>If true, the JstringListBox will sort items.  This variable is
   * set by the value of the sort parameter in the most recent {@link
   * arlut.csd.JDataComponent.JstringListBox#load(java.util.Vector
   * items, int width, boolean sort, java.util.Comparator comparator)
   * load()} call.</p>
   */

  private boolean doSort = false;

  /**
   * <p>The callback we'l use to report user activities.</p>
   */

  JsetValueCallback
    callback;

  /**
   * <p>The popup menu to be displayed on right-click.</p>
   */

  JPopupMenu
    popup = null;

  private int
    startDragIndex = -1,
    dragNode = -1;

  /**
   * <p>The comparator to use for putting items in sort order if the
   * JstringListBox was most recently with sorting request.</p>
   */

  Comparator comparator;

  /**
   * <p>The default maximum width string</p>
   */

  String maxWidthString = "this is the minimum!";

  /* -- */

  /**
   *
   * Default Constructor
   *
   */

  public JstringListBox()
  {
    addMouseListener(this);
    addMouseMotionListener(this);
    setSelectionMode(DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    getSelectionModel().addListSelectionListener(this);
  }

  /**
   * <p>This method associates a node-linked popup menu to this
   * listbox.</p>
   */

  public void registerPopupMenu(JPopupMenu popup)
  {
    this.popup = popup;

    if (popup == null)
      {
        return;
      }

    Component[] c = popup.getComponents();

    for (int i = 0; i < c.length; i++)
      {
        if (c[i] instanceof JMenuItem)
          {
            JMenuItem pm = (JMenuItem)c[i];
            pm.addActionListener(this);
          }
        else
          {
            throw new IllegalArgumentException("Hey, you are supposed to use JMenuItems in JPopupMenus, buddy.");
          }
      }
  }

  /**
   * <p>This method loads a Vector of items into this list box.
   * Elements of the items Vector may be Strings or {@link
   * arlut.csd.JDataComponent.listHandle listHandle} objects.</p>
   *
   * <p>If sort is true, the items will be sorted in display order
   * before being loaded into this list box.</p>
   *
   * <p>Any values previously in the list box will be removed.</p>
   *
   * @param items A Vector of Strings or listHandles
   * @param width If less than zero, the listbox's cell width will
   * be left unchanged.  If set to zero, the listbox's cell width will
   * be calculated based on the longest string in the items Vector.  If
   * greater than zero, the cell width will be set to &lt;width&gt; columns
   * wide.
   * @param sort  If true, the items Vector will be sorted in place before
   * being set into the listbox.
   * @param comparator Typically an instance of an inner class that implements
   * the Comparator interface, used to guide the sort process.  If this
   * is null, the sort will be performed using a normal string ordering sort.
   */

  public synchronized void load(Vector items, int width, boolean sort, Comparator comparator)
  {
    this.maxWidthString = "this is the minimum!";

    model = new DefaultListModel();

    try
      {
        doSort = sort;

        if (comparator == null)
          {
            this.comparator = this;
          }
        else
          {
            this.comparator = comparator;
          }

        if (items == null || items.size() == 0)
          {
            return;
          }

        if (items.elementAt(0) instanceof listHandle)
          {
            if (doSort)
              {
                new VecQuickSort(items, comparator).sort();
              }

            for (int i = 0; i < items.size(); i++)
              {
                listHandle handle = (listHandle) items.elementAt(i);

                insertHandleAt(handle, i);

                if (handle.toString().length() > maxWidthString.length())
                  {
                    maxWidthString = handle.toString();
                  }
              }
          }
        else  //It must be a string, or it will throw a ClassCastException
          {
            Vector convertedVect = new Vector(items.size());

            for (int i = 0; i < items.size(); i++)
              {
                String s = (String)items.elementAt(i);

                if (s.length() > maxWidthString.length())
                  {
                    maxWidthString = s;
                  }

                convertedVect.addElement(new listHandle(s,s));
              }

            if (doSort)
              {
                new VecQuickSort(convertedVect, comparator).sort();
              }

            for (int i = 0; i < convertedVect.size(); i++)
              {
                listHandle handle = (listHandle) convertedVect.elementAt(i);

                insertHandleAt(handle, i);
              }
          }
      }
    finally
      {
        setModel(model);
        repaint();
      }
  }

  /**
   * This method is used to change the dynamically label of an object in this
   * StringSelector.
   */

  public synchronized void relabelObject(Object object, String newLabel)
  {
    // only bother if we have _objects_

    ListModel model = getModel();

    if ((model.getSize() == 0) || !(model.getElementAt(0) instanceof listHandle))
      {
        return;
      }

    for (int i = 0; i < model.getSize(); i++)
      {
        listHandle handle = (listHandle) model.getElementAt(i);

        if (handle.getObject() != null && handle.getObject().equals(object))
          {
            handle.setLabel(newLabel);
            break;
          }
      }

    if (doSort)
      {
        resort();
      }

    repaint();
  }

  /**
   * This method resorts the elements in this JstringListBox.
   */

  public void resort()
  {
    ListModel model = getModel();

    if (model.getSize() < 2)
      {
        return;
      }

    Vector sortedList = new Vector();

    for (int i = 0; i < model.getSize(); i++)
      {
        sortedList.addElement(model.getElementAt(i));
      }

    if (this.comparator == null)
      {
        this.comparator = this;
      }

    new VecQuickSort(sortedList, this.comparator).sort();

    setListData(sortedList);
  }

  /**
   * <p>This method sets the default cell width for this list.</p>
   */

  public void setCellWidth(int width)
  {
    this.width = width;

    if (width > 0)
      {
        if (debug)
          {
            System.err.println("JstringListBox: setting fixed cell width to " + width);
          }

        setFixedCellWidth(width);
      }
    else if (width == 0)
      {
        setPrototypeCellValue(maxWidthString);
      }
  }

  /**
   * <p>This method sets the default cell width for this list.</p>
   */

  public void setCellWidth(String template)
  {
    setPrototypeCellValue(template);
  }

  /**
   * <p>This method enables and disables item dragging in this list.</p>
   */

  public void setDragEnabled(boolean dragOk)
  {
    this.dragOk = dragOk;

    if (!dragOk)
      {
        dragNode = -1;
        startDragIndex = -1;
      }
  }

  /**
   * Convenience method to set the size on the model.
   */

  public void setSize(int size)
  {
    model.setSize(size);
  }

  /**
   * <p>Connects this JstringListBox to an implementaton of the {@link
   * arlut.csd.JDataComponent.JsetValueCallback JsetValueCallback}
   * interface in order to provide live notification of changes
   * performed by the user.  The JsetValueCallback implementation is
   * given the opportunity to approve any change made by the user
   * before the GUI is updated to show the change.  The
   * JsetValueCallback interface is also used to pass pop-up menu
   * commands to the client.</p>
   *
   * <p>JstringListBox uses the following subclasses of {@link
   * arlut.csd.JDataComponent.JValueObject JValueObject} to pass
   * status updates to the callback.
   *
   * <ul>
   * <li>{@link arlut.csd.JDataComponent.JParameterValueObject} Action
   * from a PopupMenu.  The Parameter is the ActionCommand string for
   * the pop-up menu item selected, and the value is the object (or
   * string if no object defined) associated with the item selected
   * when the pop-up menu was fired.</li>
   * <li>{@link arlut.csd.JDataComponent.JAddValueObject} Object has
   * been selected.  Value is the object (or string) added.</li>
   * <li>{@link arlut.csd.JDataComponent.JInsertValueObject} Object
   * has been double-clicked.  Value is the object (or string)
   * double-clicked.</li>
   * <li>{@link arlut.csd.JDataComponent.JMoveValueObject} Object has
   * been dragged up or down.  Index holds the index the object has
   * been moved to.</li>
   * <li>{@link arlut.csd.JDataComponent.JErrorValueObject} Something
   * went wrong.  Value is the error message to be displayed to the
   * user in whatever fashion is appropriate.</li>
   * </ul>
   *
   * @see JsetValueCallback
   * @see JValueObject
   */

  public void setCallback(JsetValueCallback callback)
  {
    if (debug)
      {
        System.out.println("Setting callback in JstringListBox");
      }

    this.callback = callback;
  }

  /**
   * Add an item to the list box.
   *
   * @param o Can be a String or listHandle.
   */

  public void addItem(Object o)
  {
    if (debug)
      {
        System.err.println("JstringListBox.addItem(" + String.valueOf(o) + ")");
      }

    listHandle lh = null;

    if (o instanceof String)
      {
        lh = new listHandle((String)o, (String)o);
      }
    else                        // we'll throw a ClassCastException if we need to
      {
        lh = (listHandle)o;
      }

    if (doSort)
      {
        int i = 0;
        int total = model.getSize();

        // find the insertion point

        while ((i < total) && (comparator.compare(lh, model.getElementAt(i))>0))
          {
            i++;
          }

        insertHandleAt(lh, i);

        addSelectionInterval(i, i);

        if (debug)
          {
            System.err.println("Adding sorted");
          }

        ensureIndexIsVisible(i);
      }
    else
      {
        int topIndex = model.getSize();

        model.addElement(lh);
        addSelectionInterval(topIndex, topIndex);

        if (debug)
          {
            System.err.println("Adding non-sorted");
          }

        ensureIndexIsVisible(topIndex);
      }

    invalidate();
    getParent().validate();
  }

  /**
   * <p>This method moves an item around in the list.</p>
   */

  public void moveItem(int sourceRow, int targetRow)
  {
    if (sourceRow == targetRow)
      {
        return;
      }

    listHandle h = (listHandle) model.remove(sourceRow);

    if (h == null)
      {
        return;
      }

    model.insertElementAt(h, targetRow);

    ensureIndexIsVisible(targetRow);

    invalidate();
    getParent().validate();
  }

  /**
   * Use this one to skip the sorting.  Called by all the add methods.
   */

  public void insertHandleAt(listHandle handle, int row)
  {
    model.insertElementAt(handle, row);
  }

  /**
   * Remove an item from list.
   *
   * @param o can be listHandle or String
   */

  public void removeItem(Object o)
  {
    if (o instanceof listHandle)
      {
        model.removeElement((listHandle)o);
      }
    else if (o instanceof String)
      {
        if (debug)
          {
            System.out.println("I am guessing you want me to remove a label...");
          }

        removeLabel((String) o);
      }
    else
      {
        if (debug)
          {
            System.out.println("Ok, i will look for this object in the listHnaldes.");
          }

        for (int i = 0; i < model.getSize(); i++)
          {
            if ((((listHandle)model.elementAt(i)).getObject()).equals(o))
              {
                model.removeElementAt(i);
                break;
              }
          }
      }

    repaint();
  }

  /**
   * Remove an object by label
   *
   * @param s Label of object to remove.
   */

  public void removeLabel(String s)
  {
    for (int i = 0; i < model.getSize(); i++)
      {
        if ((((listHandle)model.elementAt(i)).getLabel()).equals(s))
          {
            model.removeElementAt(i);
            break; //Assume there is only one?
          }
      }
  }

  public int getSizeOfList()
  {
    return model.getSize();
  }

  /**
   * Returns true if the list contains an object with the specified label.
   */

  public boolean containsLabel(String string)
  {
    return containsString(string);
  }

  /**
   * <p>Returns true if the list contains an object with the specified
   * label.</p>
   *
   * <p>Since everything is a listHandle internally, this is the same
   * as containsLabel</p>
   */

  public boolean containsString(String string)
  {
    for (int i = 0; i < model.getSize(); i++)
      {
        if ((((listHandle)model.elementAt(i)).getLabel()).equals(string))
          {
            return true;
          }
      }

    return false;
  }

  /**
   * Returns true if the item is in the list
   *
   * @param o Can be a String(label) or listHandle
   */

  public boolean containsItem(Object o)
  {
    if (o instanceof String)
      {
        return containsLabel((String)o);
      }
    else if (o instanceof listHandle)
      {
        return model.contains(o);
      }
    else
      {
        for (int i = 0; i < model.getSize(); i++)
          {
            if (((listHandle)model.elementAt(i)).getObject().equals(o))
              {
                return true;
              }
          }
      }

    return false;
  }

  /**
   * This selects the item with the given label.
   */

  public void setSelectedLabel(String s)
  {
    this.setSelectedLabel(s, false);
  }

  /**
   * This selects the item with the given label.
   */

  public void setSelectedLabel(String s, boolean ensureVisible)
  {
    int size = model.getSize();
    listHandle lh = null;

    for (int i = 0; i < size; i++)
      {
        lh = (listHandle)model.getElementAt(i);

        if (lh.getLabel().equals(s))
          {
            setSelected(lh);

            if (ensureVisible)
              {
                ensureIndexIsVisible(i);
              }

            break;
          }
      }
  }

  /**
   * Sets the selected item.
   *
   * @param o Can be listHandle or String
   */

  public void setSelected(Object o)
  {
    if (o instanceof listHandle)
      {
        setSelectedValue(o, true); // use the JList.setSelectedValue(Object, boolean shouldScroll)
      }
    else                        // we'll throw ClassCastException if we need to
      {
        setSelectedLabel((String)o);
      }
  }

  /**
   * This returns just the label.
   */

  public String getSelectedLabel()
  {
    return ((listHandle)model.elementAt(getSelectedIndex())).getLabel();
  }

  /**
   * This returns the whole listHandle.
   */

  public listHandle getSelectedHandle()
  {
    return (listHandle)model.elementAt(getSelectedIndex());
  }

  /**
   * Returns all the selected handles.
   */

  @SuppressWarnings("deprecation")

  public Vector getSelectedHandles()
  {
    Vector v = new Vector();
    Object[] values = getSelectedValues();

    for (int i =0; i < values.length; i++)
      {
        v.addElement(values[i]);
      }

    return v;
  }

  /**
   * Returns all handles
   */

  public Vector getHandles()
  {
    Vector v = new Vector();

    for (int i =0; i < getModel().getSize(); i++)
      {
        v.addElement(getModel().getElementAt(i));
      }

    return v;
  }

  /**
   * This returns the object, without the label.
   */

  public Object getSelectedItem()
  {
    try
      {
        listHandle lh = (listHandle) model.elementAt(getSelectedIndex());
        return lh.getObject();
      }
    catch (Exception e)
      {
        return null;
      }
  }

  /**
   * For the ListSelectionListener
   */

  public void valueChanged(ListSelectionEvent e)
  {
    int selectedIndex = getSelectedIndex();

    if (selectedIndex == -1 || dragNode != -1)
      {
        return;                 // don't notify our container on deselect
      }

    if (callback != null)
      {
        boolean ok = false;

        try
          {
            ok = callback.setValuePerformed(new JAddValueObject(this, selectedIndex));
          }
        catch (java.rmi.RemoteException rx)
          {
            throw new RuntimeException("Could not setValuePerformed: " + rx);
          }

        if (ok)
          {
            //do something
          }
        else
          {
            //put it back
          }
      }
  }

  /**
   * For the MouseListener interface
   */

  public void mouseClicked(MouseEvent e)
  {
    if (callback != null)
      {
        if (SwingUtilities.isLeftMouseButton(e))
          {
            // we only want to respond to a double-click.  mouseDown with no modifier
            // will be treated as a drag initiation

            if (e.getClickCount() == 2)
              {
                boolean ok = false;

                int index = locationToIndex(e.getPoint());

                if (debug)
                  {
                    System.out.println("Double clicked on Item " + index);
                  }

                try
                  {
                    ok = callback.setValuePerformed(new JInsertValueObject(this, index));
                  }
                catch (java.rmi.RemoteException rx)
                  {
                    throw new RuntimeException("Double click produced: " + rx);
                  }

                if (debug)
                  {
                    System.out.println("setValue from JstringListBox=" + ok);
                  }
              }
          }
        else if (SwingUtilities.isRightMouseButton(e))
          {
            if (debug)
              {
                System.out.println("Its a popup trigger!");
              }

            popUpIndex = locationToIndex(e.getPoint());

            boolean found = false;

            try
              {
                if (model.elementAt(popUpIndex) != null)
                  {
                    found = true;
                  }
              }
            catch (ArrayIndexOutOfBoundsException ex)
              {
              }

            if (found)
              {
                clearSelection();
                setSelectedIndex(popUpIndex);

                if (popup != null)
                  {
                    popup.setVisible(false);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                  }
              }
          }
      }
  }

  /**
   * For the MouseListener interface
   */

  public void mouseEntered(MouseEvent e)
  {
  }

  /**
   * For the MouseListener interface
   */

  public void mouseExited(MouseEvent e)
  {
  }

  /**
   * For the mouseListener interface
   */

  public void mousePressed(MouseEvent e)
  {
    if (e.isShiftDown() || e.isControlDown() || !dragOk)
      {
        dragNode = -1;
        startDragIndex = -1;
        return;
      }

    this.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    dragNode = locationToIndex(e.getPoint());
    startDragIndex = dragNode;
  }

  /**
   * For the MouseListener interface
   */

  public void mouseReleased(MouseEvent e)
  {
    if (startDragIndex != -1)
      {
        if (callback != null)
          {
            boolean ok = false;

            try
              {
                ok = callback.setValuePerformed(new JMoveValueObject(this,
                                                                     startDragIndex,
                                                                     dragNode));
              }
            catch (java.rmi.RemoteException rx)
              {
                throw new RuntimeException("Could not setValuePerformed: " + rx);
              }

            if (!ok)
              {
                moveItem(dragNode, startDragIndex);
              }
          }
      }

    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    dragNode = -1;
    startDragIndex = -1;
  }

  /**
   * For the MouseMotionListener interface
   */

  public void mouseDragged(MouseEvent e)
  {
    if (dragNode == -1)
      {
        return;
      }

    int overIndex = locationToIndex(e.getPoint());

    if (overIndex >= (model.getSize() - 1))
      {
        overIndex = model.getSize() - 1;
      }

    if (overIndex != -1 && overIndex != dragNode)
      {
        moveItem(dragNode, overIndex);

        dragNode = overIndex;
        setSelectedIndex(overIndex);
      }
  }

  /**
   * For the MouseMotionListener interface
   */

  public void mouseMoved(MouseEvent e)
  {
  }

  /**
   * For the pop up menu callback.  We use the popUpIndex variable to
   * identify the item in the list that the popup menu was issued on.
   */

  public void actionPerformed(ActionEvent e)
  {
    if (callback != null)
      {
        if (e.getSource() instanceof JMenuItem)
          {
            String string = ((JMenuItem)e.getSource()).getActionCommand();
            Object popSelectedItem = null;

            try
              {
                popSelectedItem = ((listHandle) model.elementAt(popUpIndex)).getObject();
              }
            catch (ArrayIndexOutOfBoundsException ex)
              {
                popSelectedItem = null;
              }

            if (debug)
              {
                System.err.println("Forwarding selected item.. (" + popUpIndex + ": " +
                                   popSelectedItem + ")");
              }

            try
              {
                callback.setValuePerformed(new JParameterValueObject(this,
                                                                     popUpIndex,
                                                                     popSelectedItem,
                                                                     string));
              }
            catch (java.rmi.RemoteException rx)
              {
                throw new RuntimeException("Could not set value from JstringListBox: " + rx);
              }
          }
      }
  }

  // FocusListener methods ------------------------------------------------------

  public void focusLost(FocusEvent e)
  {
    if (debug)
      {
        System.out.println("JstringListBox: focusLost");
      }
  }

  public void focusGained(FocusEvent e)
  {
    if (debug)
      {
        System.out.println("focusGained");
      }

    JComponent parent = (JComponent) this.getParent();

    if (parent != null)
      {
        parent.scrollRectToVisible(this.getBounds());
      }
  }

  /**
   * <p>Default comparator, does a string comparison on the
   * toString() output of the objects for ordering.</p>
   */

  public int compare(Object a, Object b)
  {
    return a.toString().compareTo(b.toString());
  }
}
