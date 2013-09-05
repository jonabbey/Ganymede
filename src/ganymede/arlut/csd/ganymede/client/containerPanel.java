/*

   containerPanel.java

   This is the container for all the information in a field.  Used in window Panels.

   Created:  11 August 1997

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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

package arlut.csd.ganymede.client;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import arlut.csd.JDataComponent.JAddValueObject;
import arlut.csd.JDataComponent.JAddVectorValueObject;
import arlut.csd.JDataComponent.JDeleteValueObject;
import arlut.csd.JDataComponent.JDeleteVectorValueObject;
import arlut.csd.JDataComponent.JErrorValueObject;
import arlut.csd.JDataComponent.JIPField;
import arlut.csd.JDataComponent.JLabelPanel;
import arlut.csd.JDataComponent.JParameterValueObject;
import arlut.csd.JDataComponent.JResetDateObject;
import arlut.csd.JDataComponent.JStretchPanel;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JdateField;
import arlut.csd.JDataComponent.JfloatField;
import arlut.csd.JDataComponent.JnumberField;
import arlut.csd.JDataComponent.JpassField;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.JstringArea;
import arlut.csd.JDataComponent.JstringField;
import arlut.csd.JDataComponent.StringSelector;
import arlut.csd.JDataComponent.TimedKeySelectionManager;
import arlut.csd.JDataComponent.listHandle;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VecSortInsert;
import arlut.csd.ganymede.common.FieldInfo;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.IPAddress;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.date_field;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.rmi.db_object;
import arlut.csd.ganymede.rmi.invid_field;
import arlut.csd.ganymede.rmi.ip_field;
import arlut.csd.ganymede.rmi.pass_field;
import arlut.csd.ganymede.rmi.perm_field;
import arlut.csd.ganymede.rmi.string_field;
import arlut.csd.ganymede.rmi.field_option_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  containerPanel

------------------------------------------------------------------------------*/

/**
 * <p>One of the basic building blocks of the ganymede client, a
 * containerPanel is a GUI panel which allows the user to view and/or
 * edit all the custom fields for an object in the Ganymede database.</p>
 *
 * <p>Each containerPanel displays a single {@link
 * arlut.csd.ganymede.rmi.db_object db_object}, and allows the user to
 * edit or view each {@link arlut.csd.ganymede.rmi.db_field db_field}
 * in the object.  On loading, containerPanel loops through the fields
 * of the object, adding the appropriate type of input for each field.
 * This includes text fields, number fields, boolean fields, and
 * string selector fields(fields that can have multiple values).</p>
 *
 * <p>containerPanel handles the connection between GUI components and
 * server fields, translating GUI activity to attempted changes to
 * server fields.  Any attempted change that the server refuses will
 * cause a dialog to be popped up via gclient's {@link
 * arlut.csd.ganymede.client.gclient#handleReturnVal(arlut.csd.ganymede.common.ReturnVal)
 * handleReturnVal()} method, and the GUI component that caused the
 * change will be reverted to its pre-change status.</p>
 *
 * <p>The gclient's handleReturnVal() method also supports extracting a
 * list of objects and fields that need to be refreshed when one
 * change on the server is reflected across more than one object.
 * This is handled by containerPanel's {@link
 * arlut.csd.ganymede.client.containerPanel#update(java.util.Vector)
 * update()} method.</p>
 *
 * @author Mike Mulvaney
 */

public class containerPanel extends JStretchPanel implements ActionListener, JsetValueCallback, ItemListener {

  static final boolean debug = false;
  static final boolean debug_persona = false;

  /**
   * Number of columns to size our string fields to, one of our
   * primary references for our layout.
   */

  static final int FIELDWIDTH = 35;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.containerPanel");

  static final String edit_action = ts.l("global.edit_action"); // "Edit Object"
  static final String view_action = ts.l("global.view_action"); // "View Object"

  // ---

  /**
   * Reference to the client's main class, used for some utility functions.
   */

  gclient gc;

  /**
   * Remote reference to the server-side object we are viewing or editing.
   */

  private db_object object;

  /**
   * Database id for the object we are viewing or editing
   */

  private Invid invid;

  /**
   * Reference to the desktop pane containing the client's internal windows.  Used to access
   * some GUI resources.
   */

  windowPanel winP;

  /**
   * The window we are contained in, may be null if we are embedded in a
   * {@link arlut.csd.ganymede.client.vectorPanel vectorPanel}.
   */

  protected framePanel frame;

  /**
   * All of the components in this containerPanel are placed in this
   * {@link arlut.csd.JDataComponent.JLabelPanel JLabelPanel}, which
   * automatically takes care of the layout and management of labeled
   * fields in this panel.
   */

  private JLabelPanel contentsPanel;

  /**
   * Vector of Short field id's used to track fields for which we
   * receive update requests while we are still loading.  After we
   * finish loading this panel, we'll go back and refresh any fields
   * whose field id's are listed in this vector.
   */

  private Vector<Short> updatesWhileLoading = new Vector();

  /**
   * Vector used to list vectorPanels embedded in this object window.
   * This variable is used by {@link
   * arlut.csd.ganymede.client.vectorPanel#expandAllLevels()
   * vectorPanel.expandAllLevels()} to do recursive expansion of
   * embedded objects.
   */

  Vector<vectorPanel> vectorPanelList = new Vector();

  /**
   * To help avoid recursive problems, we keep track of any
   * arlut.csd.JDataComponent GUI components that are currently having
   * their change notification messages handled, and refuse to try to
   * refresh them re-entrantly.
   */

  private JComponent currentlyChangingComponent = null;

  /**
   * Hashtable mapping GUI components to their associated
   * {@link arlut.csd.ganymede.rmi.db_field db_field}'s.
   */

  private Hashtable<Component, db_field> objectHash = new Hashtable<Component, db_field>();

  /**
   * Hashtable mapping GUI components to their associated
   * FieldTemplate objects.
   */

  private Hashtable<JComponent, FieldTemplate> objectTemplateHash = new Hashtable<JComponent, FieldTemplate>();

  /**
   * Hashtable mapping Short Field ID numbers to their associated
   * GUI components.
   */

  private Hashtable<Short, Component> idHash = new Hashtable<Short, Component>();

  /**
   * <p>Hashtable mapping the combo boxes contained within
   * {@link arlut.csd.ganymede.client.JInvidChooser JInvidChooser}
   * GUI components to their associated
   * {@link arlut.csd.ganymede.rmi.invid_field invid_field}'s.</p>
   *
   * <p>This is required because while we want to hide or reveal the JInvidChooser
   * as a whole, we'll get itemStateChanged() calls from the combo
   * box within the JInvidChooser.</p>
   */

  private Hashtable<JComboBox, invid_field> invidChooserHash = new Hashtable<JComboBox, invid_field>();

  /**
   * Vector of {@link arlut.csd.ganymede.common.FieldInfo FieldInfo} objects
   * holding the values for fields in this object.  Used during loading
   * and update.
   */

  private Vector<FieldInfo> infoVector = null;

  /**
   * Vector of {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate}
   * objects holding the constant field type information for fields in
   * this object.
   */

  private Vector<FieldTemplate> templates = null;

  /**
   * The name of the tab this containerPanel is limited to showing
   * when the load() method is called.  If this variable is null,
   * load() will load and display all custom field definitions from
   * the server.
   */

  private String tabName = null;

  boolean
    isCreating,
    editable;

  boolean
    isEmbedded,
    loading = false,
    loaded = false;

  /**
   * If progressBar is not null, the load() method for containerPanel will
   * update this progressBar as the panel is loaded from the server.
   */

  private JProgressBar progressBar;

  int
    vectorElementsAdded = 0;

  /**
   * Object type id for this object.. should be equal to invid.getType().
   */

  short type;

  /**
   * <p>If true, this containerPanel is being displayed in a persona
   * pane in a frame panel, and we'll hide the associated user field,
   * which is implicit when we embedded a persona panel in a
   * framePanel showing a user object.</p>
   *
   * <p>This is a dirty hack to make the client a little extra smart
   * about one particular kind of mandatory Ganymede server
   * object.</p>
   */

  boolean isPersonaPanel = false;

  /* -- */

  /**
   * Constructor with default values for progressBar set to false, loadNow
   * set to true, and isCreating set to false.
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param gc   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param frame    framePanel holding this containerPanel(although this cp is not necessarily in the "General" tab)
   * @param context An object that can be provided to identify the context in
   * which this containerPanel is being created.
   */

  public containerPanel(db_object    object,
                        Invid        invid,
                        boolean      editable,
                        gclient      gc,
                        windowPanel  window,
                        framePanel   frame,
                        Object context)
  {
    this(object, invid, editable, gc, window, frame, null, true, context);
  }

  /**
   * <p>Constructor with default values for loadNow set to true, and
   * isCreating set to false.</p>
   *
   * <p>The &lt;progressBar&gt; parameter is used so that
   * containerpanel can increment an external JProgressBar as
   * information on the fields for this object are loaded from the
   * server.  progressBar should be null if this containerPanel is not
   * serving as the main panel for a framePanel.</p>a
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param gc       Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param frame    framePanel holding this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   * @param context An object that can be provided to identify the context in
   * which this containerPanel is being created.
   */

  public containerPanel(db_object object,
                        Invid invid,
                        boolean editable,
                        gclient gc,
                        windowPanel window,
                        framePanel frame,
                        JProgressBar progressBar,
                        Object context)
  {
    this(object, invid, editable, gc, window, frame, progressBar, true, context);
  }

  /**
   * <p>Constructor with default value for isCreating set to
   * false.</p>
   *
   * <p>The &lt;progressBar&gt; parameter is used so that
   * containerpanel can increment an external JProgressBar as
   * information on the fields for this object are loaded from
   * the server.</p>
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param gc       Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   * @param loadNow  If true, container panel will be loaded immediately
   * @param context An object that can be provided to identify the context in
   * which this containerPanel is being created.
   */

  public containerPanel(db_object object,
                        Invid invid,
                        boolean editable,
                        gclient gc,
                        windowPanel window,
                        framePanel frame,
                        JProgressBar progressBar,
                        boolean loadNow,
                        Object context)
  {
    this(object, invid, editable, gc, window, frame, progressBar, loadNow, false, context);
  }

  /**
   * <p>Primary constructor for containerPanel</p>
   *
   * <p>The &lt;progressBar&gt; parameter is used so that
   * containerpanel can increment an external JProgressBar as
   * information on the fields for this object are loaded from the
   * server.  progressBar should be null if this containerPanel is not
   * serving as the main panel for a framePanel.</p>
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param gc       Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   * @param loadNow  If true, container panel will be loaded immediately
   * @param isCreating
   * @param context An object that can be provided to identify the context in
   * which this containerPanel is being created.
   */

  public containerPanel(db_object object,
                        Invid invid,
                        boolean editable,
                        gclient gc,
                        windowPanel window,
                        framePanel frame,
                        JProgressBar progressBar,
                        boolean loadNow,
                        boolean isCreating,
                        Object context)
  {
    super(false);

    /* -- */

    this.gc = gc;

    if (object == null)
      {
        throw new NullPointerException("null object passed to containerPanel constructor");
      }

    this.winP = window;
    this.object = object;
    this.invid = invid;
    this.editable = editable;
    this.frame = frame;
    this.progressBar = progressBar;
    this.isCreating = isCreating;

    if (context != null && (context instanceof personaContainer))
      {
        this.isPersonaPanel = true;
      }

    frame.addContainerPanel(this);

    // initialize layout

    contentsPanel = new JLabelPanel();
    contentsPanel.setLeftInsets(4,4,4,4);
    contentsPanel.setRightInsets(4,4,4,0);  // no right margin
    contentsPanel.setFixedSizeLabelCells(true);

    setComponent(contentsPanel);

    if (loadNow)
      {
        load();
      }
  }

  /**
   * <p>This method is used to set the name of the tab that this
   * containerPanel is limited to showing.</p>
   *
   * <p>If the tabName is null (or if setTabName() has not been
   * called), the load() method in this class will simply display all
   * fields.</p>
   *
   * <p>This method will not have any effect if it is called after
   * load()</p>
   */

  public void setTabName(String tabName)
  {
    this.tabName = tabName;
  }

  /**
   * This method is used to pre-load a Vector of {@link
   * arlut.csd.ganymede.common.FieldInfo} objects into this
   * containerPanel.  The goal is to allow multiple tabs to share a
   * FieldInfo Vector that only needs to be downloaded once.
   */

  public void setInfoVector(Vector<FieldInfo> infoVector)
  {
    this.infoVector = infoVector;
  }

  /**
   * Downloads all necessary information from the server
   * about the object being viewed or edited.  Typically this is called
   * when the containerPanel is initialized by the containerPanel
   * constructor, but we defer loading when we are placed in a vector
   * panel hierarchy.
   */

  public void load()
  {
    loading = true;

    int infoSize;

    FieldInfo
      fieldInfo = null;

    FieldTemplate
      fieldTemplate = null;

    short ID;

    /* -- */

    if (loaded)
      {
        printErr("Container panel is already loaded!");
        return;
      }

    if (debug)
      {
        println("Loading container panel");
      }

    try
      {
        // if we are a top-level container panel in a general pane
        // or persona pane, we'll have a progress bar.. we'll want
        // to update it as we go along loading field information.

        if (progressBar != null)
          {
            progressBar.setMinimum(0);
            progressBar.setMaximum(20);
            progressBar.setValue(0);
          }

        // Get the list of fields

        if (debug)
          {
            println("Getting list of fields");
          }

        type = invid.getType();

        setProgressBar(1);

        // pull static field type information from the client's caches

        templates = gc.getTemplateVector(type);

        if (templates == null || templates.size() == 0)
          {
            printErr("No fields defined for this object type.. ??");

            if (templates == null)
              {
                printErr("templates is *null*");
              }
            else
              {
                printErr("templates is empty");
              }

            return;
          }

        setProgressBar(2);

        //
        // ok, got the list of field definitions.  Now we need to get
        // the current values and visibility information for the fields
        // in this object.
        //
        // Note that if we have previously been pre-loaded with a
        // FieldInfo Vector, we won't bother calling the server to get
        // one.
        //

        if (infoVector == null)
          {
            try
              {
                infoVector = object.getFieldInfoVector();
              }
            catch (Exception rx)
              {
                gc.processExceptionRethrow(rx);
              }
          }

        if (infoVector.size() == 0)
          {
            printErr("No field info in getFieldInfoVector()");
          }

        // keep a copy of the infoVector size so we don't have to
        // continously call the synchronized infoVector.size()
        // method during our loops

        infoSize = infoVector.size();

        // now we know how many fields are actually present in this
        // object, we can set the max size of the progress bar (plus
        // how many elements in each vector panel.)

        if (progressBar != null)
          {
            int totalSize = 0;

            for (FieldInfo info: infoVector)
              {
                FieldTemplate template = findtemplate(info.getID());

                if (this.tabName == null)
                  {
                    totalSize++;
                  }
                else if (this.tabName.equals(template.getTabName()))
                  {
                    totalSize++;
                  }

                if (template.isArray())
                  {
                    if ((template.getType() == FieldType.INVID) && template.isEditInPlace())
                      {
                        totalSize += ((Vector)info.getValue()).size();
                      }
                  }
              }

            progressBar.setMaximum(totalSize);
            progressBar.setValue(3);
          }

        if (debug)
          {
            println("Entering big loop");
          }

        for (int i = 0; i < infoSize; i++)
          {
            // let the gclient interrupt us

            if (!keepLoading())
              {
                break;
              }

            setProgressBar(i + 3 + vectorElementsAdded);

            try
              {
                fieldInfo = infoVector.get(i);
                ID = fieldInfo.getID();
                fieldTemplate = findtemplate(ID);

                if (fieldTemplate == null)
                  {
                    throw new RuntimeException("Could not find the template for this field: " +
                                               fieldInfo.getField());
                  }

                // Skip some fields.  custom panels hold the built ins, and a few others.

                // If we are a persona panel, hide the associated user field.

                if ((ID == SchemaConstants.BackLinksField)
                    || ((type == SchemaConstants.UserBase) &&
                        (ID == SchemaConstants.UserAdminPersonae))
                    || ((ID == SchemaConstants.ContainerField) &&
                        object.isEmbedded())
                    || (isPersonaPanel &&
                        (type == SchemaConstants.PersonaBase) &&
                        (ID == SchemaConstants.PersonaAssocUser)))
                  {
                    if (debug)
                      {
                        println("Skipping a special field: " + fieldTemplate.getName());
                      }

                    continue;
                  }

                // skip fields for other tabs

                if (this.tabName != null && !this.tabName.equals(fieldTemplate.getTabName()))
                  {
                    continue;
                  }

                // and do the work.  If we're read only, we don't want
                // to bother showing fields that are undefined.  In
                // fact, we should only see undefined fields if we are
                // viewing an object that has been checked out for
                // editing by the current transaction.  Normally, when
                // we view an object from the server and a read-only
                // copy is created for us, fields without values
                // simply aren't part of the object.

                if (editable || fieldInfo.isDefined())
                  {
                    addFieldComponent(fieldInfo.getField(), fieldInfo, fieldTemplate);
                  }
              }
            catch (Exception ex)
              {
                gc.processExceptionRethrow(ex);
              }
          }

        if (debug)
          {
            println("Done with loop");
          }
      }
    finally
      {
        loaded = true;
        loading = false;

        if (!keepLoading())
          {
            return;
          }

        // If update(Vector) was called during the load, then any
        // fields to be updated were added to the updatesWhileLoading
        // vector.  So call update with that vector now, if it has any
        // size.

        synchronized (updatesWhileLoading)
          {
            if (updatesWhileLoading.size() > 0)
              {
                if (debug)
                  {
                    println("Calling update with the updatesWhileLoading vector.");
                  }

                update(updatesWhileLoading);
              }
          }
      }
  }

  /**
   * Helper method to keep the load() method clean.
   */

  private final void setProgressBar(int count)
  {
    if (progressBar != null)
      {
        progressBar.setValue(count);
      }
  }

  /**
   * Helper method to keep the load() method clean.
   */

  private final FieldTemplate findtemplate(short type)
  {
    for (FieldTemplate template: templates)
      {
        if (template.getID() == type)
          {
            return template;
          }
      }

    return null;
  }

  /**
   * This is a convenience method for other client classes to access
   * our gclient reference.
   */

  public final gclient getgclient()
  {
    return gc;
  }

  /**
   * Use this to print stuff out, so we know it is from the containerPanel
   */

  private final void println(String s)
  {
    System.out.println("containerPanel: " + s);
  }

  private final void printErr(String s)
  {
    System.err.println("containerPanel err: " + s);
  }

  /**
   * Get the object contained in this containerPanel.
   */

  public  db_object getObject()
  {
    return object;
  }

  /**
   * Get the invid for the object in this containerPanel.
   */

  public Invid getObjectInvid()
  {
    if (invid == null)
      {
        try
          {
            invid = object.getInvid().intern();
          }
        catch (Exception rx)
          {
            gc.processExceptionRethrow(rx);
          }
      }

    return invid;
  }

  /**
   * This method returns true if this containerPanel has already
   * been loaded.
   */

  public boolean isLoaded()
  {
    return loaded;
  }

  /**
   * This method returns false when the containerPanel loading has
   * been interupted.  The vectorPanel checks this.
   */

  public boolean keepLoading()
  {
    return !frame.isStopped();
  }

  /**
   * <p>Goes through all the components and checks to see if they
   * should be visible, and updates their contents.</p>
   *
   * <p>If this containerPanel is attached to an object that is not
   * being edited, this method will return without doing anything.</p>
   */

  public void updateAll()
  {
    // View windows can't be updated.

    if (!editable)
      {
        return;
      }

    if (debug)
      {
        println("Updating container panel");
      }

    gc.setWaitCursor();

    try
      {
        for (Component comp: objectHash.keySet())
          {
            updateComponent(comp);
          }

        invalidate();
        frame.validate();
      }
    finally
      {
        if (debug)
          {
            println("Done updating container panel");
          }

        gc.setNormalCursor();
      }
  }

  /**
   * <p>Goes through all the components and checks to see if any of
   * them are Invid fields that contain a reference to invid.</p>
   *
   * <p>If so, we'll refresh the label for invid.</p>
   */

  public void updateInvidLabels(Invid invid, String newLabel)
  {
    if (debug)
      {
        println("Updating container panel");
      }

    gc.setWaitCursor();

    try
      {
        for (Component element: objectHash.keySet())
          {
            db_field field = objectHash.get(element);

            if (field instanceof invid_field)
              {
                relabelInvidComponent((invid_field)field, element, invid, newLabel);
              }
          }
      }
    finally
      {
        if (debug)
          {
            println("Done updating container panel");
          }

        gc.setNormalCursor();
      }
  }

  private void relabelInvidComponent(invid_field field, Component element, Invid invid, String newLabel)
  {
    if (element instanceof StringSelector)
      {
        ((StringSelector) element).relabelObject(invid, newLabel);
      }
    else if (element instanceof JInvidChooser)
      {
        ((JInvidChooser) element).relabelObject(invid, newLabel);
      }
    else if (element instanceof JButton)
      {
        try
          {
            if (field.getValue().equals(invid))
              {
                ((JButton) element).setText(newLabel);
              }
          }
        catch (RemoteException ex)
          {
            gc.processExceptionRethrow(ex);
          }
      }
  }

  /**
   * Updates a subset of the fields in this containerPanel.
   *
   * @param fields Vector of Shorts, field ID's
   */

  public void update(Vector<Short> fields)
  {
    if (!editable)
      {
        return;
      }

    if (fields == null)
      {
        return;
      }

    Component c;

    /* -- */

    if (debug)
      {
        println("Updating a few fields...");
      }

    // If the containerPanel is not loaded, then we need to keep track
    // of all the fields that need to be updated, and call update on
    // them after the load is finished.

    synchronized (updatesWhileLoading)
      {
        if (!loaded)
          {
            // If we are not loading yet, then we don't need to worry
            // about keeping track of the fields.  They will current when
            // they are first loaded.

            if (loading)
              {
                for (Short key: fields)
                  {
                    updatesWhileLoading.add(key);
                  }
              }

            return;
          }
      }

    gc.setWaitCursor();

    try
      {
        for (Short fieldID: fields)
          {
            if (!keepLoading())
              {
                return;
              }

            // if we're not an embedded container panel, check to see if
            // we should update either the expiration field or the removal
            // field.

            if (frame != null)
              {
                if (fieldID.shortValue() == SchemaConstants.ExpirationField)
                  {
                    frame.refresh_expiration_date_panel();
                    continue;
                  }
                else if (fieldID.shortValue() == SchemaConstants.RemovalField)
                  {
                    frame.refresh_removal_date_panel();
                    continue;
                  }
              }

            c = idHash.get(fieldID);

            if (c == null)
              {
                if (debug)
                  {
                    println("Could not find this component: ID = " + fieldID);
                  }
              }
            else
              {
                updateComponent(c);
              }
          }

        invalidate();
        frame.validate();

        if (debug)
          {
            println("Done updating container panel");
          }
      }
    finally
      {
        gc.setNormalCursor();
      }
  }

  /**
   * Updates the contents and visibility status of
   * a component in this containerPanel.
   *
   * @param comp An AWT/Swing component that we need to refresh
   */

  private void updateComponent(Component comp)
  {
    if (debug)
      {
        printErr("containerPanel.updateComponent(" + comp + ")");
      }

    try
      {
        db_field field = objectHash.get(comp);

        // by getting a FieldInfo, we'll save a call to the server by
        // not having to repeatedly probe the field for elements of
        // its state

        FieldInfo currentInfo = field.getFieldInfo();

        if (debug)
          {
            println("Updating " + field.getName() + " " + comp);
          }

        // if the field is not visible, just hide it and
        // return.. otherwise, set it visible and update
        // the value and choices for the field

        if (!currentInfo.isVisible())
          {
            contentsPanel.setRowVisible(comp, false);
            return;
          }

        contentsPanel.setRowVisible(comp, true);

        if (comp instanceof JstringField)
          {
            if (comp.equals(currentlyChangingComponent))
              {
                // the server apparently triggered a refresh of the
                // field that we are processing a callback from.  the
                // JstringField has specific support for this, to
                // allow the server to canonicalize strings entered by
                // the user.  We'll call the appropriate method on the
                // JstringField so that it will redraw itself with the
                // canonicalized value when our callstack unwinds to
                // the callback origination in this JstringField.

                ((JstringField)comp).substituteValueByCallBack(this, (String)currentInfo.getValue());
              }
            else
              {
                ((JstringField)comp).setText((String)currentInfo.getValue());
              }
          }
        else if (comp instanceof JstringArea)
          {
            // JstringArea handles re-entrant refresh okay, no need to
            // worry about self-refresh

            ((JstringArea)comp).setText((String)currentInfo.getValue());
          }
        else if (comp instanceof JdateField)
          {
            // JdateField handles re-entrant refresh okay as well

            date_field datef = (date_field) field;

            ((JdateField)comp).setDate((Date)currentInfo.getValue());

            if (currentInfo.isEditable())
              {
                if (datef.limited())
                  {
                    ((JdateField)comp).setLimits(datef.minDate(), datef.maxDate());
                  }
                else
                  {
                    ((JdateField)comp).setLimits(null, null);
                  }
              }
          }
        else if (comp instanceof JnumberField)
          {
            Integer value = (Integer)currentInfo.getValue();

            if (comp.equals(currentlyChangingComponent))
              {
                // the server apparently triggered a refresh of the field
                // that we are processing a callback from.  the
                // JnumberField has specific support for this, etc.

                ((JnumberField)comp).substituteValueByCallBack(this, value);
              }
            else
              {
                ((JnumberField)comp).setValue(value);
              }
          }
        else if (comp instanceof JfloatField)
          {
            Double value = (Double)currentInfo.getValue();

            if (comp.equals(currentlyChangingComponent))
              {
                // the server apparently triggered a refresh of the field
                // that we are processing a callback from.  the
                // JfloatField has specific support for this, etc.

                ((JfloatField)comp).substituteValueByCallBack(this, value);
              }
            else
              {
                ((JfloatField)comp).setValue(value);
              }
          }
        else if (comp instanceof JCheckBox)
          {
            Boolean value = (Boolean)currentInfo.getValue();
            JCheckBox cb = (JCheckBox) comp;

            // make sure we don't trigger a callback here

            cb.removeActionListener(this);
            cb.setSelected((value == null) ? false : value.booleanValue());
            cb.addActionListener(this);
          }
        else if (comp instanceof JComboBox)
          {
            JComboBox cb = (JComboBox) comp;
            string_field sf = (string_field) field;

            /* -- */

            // remove this as an item listener so we don't get tricked
            // into thinking this update came from the user

            cb.removeItemListener(this);

            if (debug)
              {
                println("Updating the combo box.");
              }

            // First we need to rebuild the list of choices

            Vector<String> labels = null;
            Object key = sf.choicesKey();

            // if our choices key is null, we're not going to use a cached copy..
            // pull down a new list of choices for this field.

            if (key == null)
              {
                QueryResult qr = sf.choices();

                if (qr != null)
                  {
                    labels = qr.getLabels();
                  }
              }
            else
              {
                if (debug)
                  {
                    println("key = " + key);
                  }

                if (gc.cachedLists.containsList(key))
                  {
                    if (debug)
                      {
                        println("key in there, using cached list");
                      }

                    labels = gc.cachedLists.getLabels(key, false);
                  }
                else
                  {
                    if (debug)
                      {
                        println("JComboBox contents not cached, downloading stringfield choices.");
                      }

                    QueryResult choicesV = sf.choices();

                    // if we got a null result, assume we have no choices,
                    // otherwise we're going to cache this result

                    if (choicesV == null)
                      {
                        labels = new Vector<String>();
                      }
                    else
                      {
                        gc.cachedLists.putList(key, choicesV);
                        labels = choicesV.getLabels();
                      }
                  }
              }

            // reset the combo box.

            boolean mustChoose = sf.mustChoose();

            String currentValue = (String) sf.getValue();

            if (!mustChoose || currentValue == null)
              {
                // "<none>"
                labels.add(ts.l("global.none"));
              }

            if (currentValue == null)
              {
                // "<none>"
                currentValue = ts.l("global.none");
              }

            // create a new model to avoid O(n^2) order time hassles when
            // we add items one-by-one to an extant JComboBox

            cb.setModel(new DefaultComboBoxModel(labels));
            cb.setSelectedItem(currentValue);

            if (debug)
              {
                printErr("setting currentvalue in JComboBox to " + currentValue);
              }

            // put us back on as an item listener so we are live for updates
            // from the user again

            cb.repaint();
            cb.addItemListener(this);
          }
        else if (comp instanceof JInvidChooser)
          {
            JInvidChooser chooser = (JInvidChooser) comp;
            invid_field invf = (invid_field) field;

            // "<none">
            listHandle noneHandle = new listHandle(ts.l("global.none"), null);
            boolean mustChoose;

            /* -- */

            // remove this as an item listener so we don't get tricked
            // into thinking this update came from the user

            chooser.removeItemListener(this);

            if (debug)
              {
                println("Updating the InvidChooser.");
              }

            // First we need to rebuild the list of choices

            Vector<listHandle> choiceHandles = null;
            Object key = invf.choicesKey();

            // if our choices key is null, we're not going to use a cached copy..
            // pull down a new list of choices for this field.

            if (key == null)
              {
                if (debug)
                  {
                    println("key is null, getting new copy, not caching.");
                  }

                QueryResult qr = invf.choices();

                if (qr == null)
                  {
                    choiceHandles = new Vector<listHandle>();  // empty
                  }
                else
                  {
                    choiceHandles = qr.getListHandles(); // pre-sorted
                  }
              }
            else
              {
                if (debug)
                  {
                    println("key = " + key);
                  }

                if (gc.cachedLists.containsList(key))
                  {
                    if (debug)
                      {
                        println("key in there, using cached list");
                      }

                    choiceHandles = gc.cachedLists.getListHandles(key, false); // pre-sorted
                  }
                else
                  {
                    if (debug)
                      {
                        println("JInvidChooser contents not cached, downloading invid field choices.");
                      }

                    QueryResult choicesV = invf.choices();

                    // if we got a null result, assume we have no choices
                    // otherwise, we're going to cache this result

                    if (choicesV == null)
                      {
                        choiceHandles = new Vector<listHandle>();
                      }
                    else
                      {
                        gc.cachedLists.putList(key, choicesV);
                        choiceHandles = choicesV.getListHandles(); // sorted
                      }
                  }
              }

            // reset the combo box.

            Invid currentValue = (Invid) invf.getValue();
            String currentLabel = gc.getSession().viewObjectLabel(currentValue);
            listHandle currentValueHandle = new listHandle(currentLabel, currentValue);
            listHandle currentHandle = null;

            if (debug)
              {
                printErr("containerPanel.updateComponent(): updating invid chooser combo box");
                printErr("containerPanel.updateComponent(): searching for value " + currentValue);
              }

            for (int i = 0; i < choiceHandles.size(); i++)
              {
                currentHandle = (listHandle) choiceHandles.elementAt(i);

                if (currentHandle.getObject().equals(currentValue))
                  {
                    break;
                  }
                else
                  {
                    currentHandle = null;
                  }
              }

            // in many cases, the list of choices may not include the
            // current value held in an invid field.  In this case,
            // we need to synthesize a handle for the current value

            if (currentHandle == null && currentValue != null)
              {
                VecSortInsert inserter = new VecSortInsert(new Comparator()
                                                           {
                                                             public int compare(Object o_a, Object o_b)
                                                               {
                                                                 listHandle a, b;

                                                                 a = (listHandle) o_a;
                                                                 b = (listHandle) o_b;
                                                                 int compResult = 0;

                                                                 compResult = a.toString().compareToIgnoreCase(b.toString());

                                                                 if (compResult < 0)
                                                                   {
                                                                     return -1;
                                                                   }
                                                                 else if (compResult > 0)
                                                                   {
                                                                     return 1;
                                                                   }
                                                                 else
                                                                   {
                                                                     return 0;
                                                                   }
                                                               }
                                                           });

                inserter.insert(choiceHandles, currentValueHandle);
                currentHandle = currentValueHandle;
              }

            // now we need to decide whether we should allow the user
            // to set this field back to the <none> selection.

            mustChoose = invf.mustChoose();

            if (!mustChoose || (currentHandle == null))
              {
                choiceHandles.insertElementAt(noneHandle, 0);
              }

            if (debug)
              {
                printErr("containerPanel.updateComponent(): got handles, setting model");
              }

            // aaaand resort

            //      choiceHandles = gc.sortListHandleVector(choiceHandles);

            if (currentHandle == null)
              {
                chooser.setVectorContents(choiceHandles, noneHandle);
              }
            else
              {
                chooser.setVectorContents(choiceHandles, currentHandle);
              }

            if (debug)
              {
                printErr("setting currentvalue in JInvidChooser to " + currentHandle);
              }

            // put us back on as an item listener so we are live for updates
            // from the user again

            chooser.repaint();
            chooser.addItemListener(this);
          }
        else if (comp instanceof JLabel)
          {
            ((JLabel)comp).setText((String)currentInfo.getValue());
          }
        else if (comp instanceof JButton)
          {
            // This is an invid field, non-editable.
            Invid inv = (Invid)currentInfo.getValue();
            ((JButton)comp).setText(gc.getSession().viewObjectLabel(inv));
          }
        else if (comp instanceof JpassField)
          {
            if (debug)
              {
                println("Passfield, ingnoring");
              }
          }
        else if (comp instanceof StringSelector)
          {
            if (field instanceof invid_field)
              {
                updateInvidStringSelector((StringSelector)comp, (invid_field)field);
              }
            else // must be a string_field
              {
                updateStringStringSelector((StringSelector)comp, (string_field)field,
                                           currentInfo);
              }

            // In the case of self-refresh on server command, prevent
            // the StringSelector from trying to finish up its
            // graphical state changing with the old data.

            if (comp.equals(currentlyChangingComponent))
              {
                ((StringSelector) comp).substituteValueByCallBack(this);
              }
          }
        else if (comp instanceof vectorPanel)
          {
            ((vectorPanel)comp).refresh();
          }
        else if (comp instanceof JIPField)
          {
            if (debug)
              {
                println("Updating JIPField.");
              }

            IPAddress address = (IPAddress) currentInfo.getValue();

            if (comp.equals(currentlyChangingComponent))
              {
                // the server apparently triggered a refresh of the field
                // that we are processing a callback from.  the
                // JIPField has specific support for this, etc.

                ((JIPField)comp).substituteValueByCallBack(this, address);
              }
            else
              {
                ((JIPField)comp).setValue(address);
              }
          }
        else
          {
            printErr("field of unknown type: " + comp);
          }
      }
    catch (Exception rx)
      {
        gc.processExceptionRethrow(rx);
      }
  }

  /**
   * Updates the contents of a vector {@link arlut.csd.ganymede.rmi.string_field string_field}
   * value selector against the current contents of the field on the server.
   *
   * @param ss The StringSelector GUI component being updated
   * @param field The server-side string_field attached to the StringSelector to be updated
   * @param currentInfo A download of the string_field's current value
   */

  public void updateStringStringSelector(StringSelector ss, string_field field,
                                         FieldInfo currentInfo) throws RemoteException
  {
    Vector available = null;
    Vector chosen = null;
    Object key = null;

    /* -- */

    // If the field is not editable, there will be no available vector

    if (ss.isEditable())
      {
        key = field.choicesKey();

        if (key == null)
          {
            QueryResult qr = field.choices();

            if (qr != null)
              {
                available = qr.getListHandles();
              }
          }
        else
          {
            if (gc.cachedLists.containsList(key))
              {
                if (debug)
                  {
                    println("key in there, using cached list");
                  }

                available = gc.cachedLists.getListHandles(key, false);
              }
            else
              {
                if (debug)
                  {
                    println("list for updateStringStringSelector() not loaded, downloading a new one.");
                  }

                QueryResult choicesV = field.choices();

                // if we got a null result, assume we have no choices
                // otherwise, we're going to cache this result

                if (choicesV == null)
                  {
                    available = new Vector();
                  }
                else
                  {
                    gc.cachedLists.putList(key, choicesV);
                    available = choicesV.getListHandles();
                  }
              }
          }
      }

    // now find the chosen vector

    chosen = (Vector) currentInfo.getValue();

    ss.update(available, true, null, chosen, false, null);
  }

  /**
   * Updates the contents of a vector {@link arlut.csd.ganymede.rmi.invid_field invid_field}
   * value selector against the current contents of the field on the server.
   *
   * @param ss The StringSelector GUI component being updated
   * @param field The server-side invid_field attached to the StringSelector to be updated
   */

  public void updateInvidStringSelector(StringSelector ss, invid_field field) throws RemoteException
  {
    Vector available = null;
    Vector chosen = null;
    Object key = null;

    /* -- */

    // Only editable fields have available vectors

    if (ss.isEditable())
      {
        key = field.choicesKey();

        if (key == null)
          {
            QueryResult qr = field.choices();

            if (qr != null)
              {
                available = qr.getListHandles();
              }
          }
        else
          {
            if (gc.cachedLists.containsList(key))
              {
                if (debug)
                  {
                    println("key in there, using cached list");
                  }

                available = gc.cachedLists.getListHandles(key, false);
              }
            else
              {
                if (debug)
                  {
                    println("list for updateInvidStringSelector() not loaded, downloading a new one.");
                  }

                QueryResult choicesV = field.choices();

                // if we got a null result, assume we have no choices
                // otherwise, we're going to cache this result

                if (choicesV == null)
                  {
                    available = new Vector();
                  }
                else
                  {
                    gc.cachedLists.putList(key, choicesV);
                    available = choicesV.getListHandles();
                  }
              }
          }
      }

    QueryResult res = field.encodedValues();

    if (res != null)
      {
        chosen = res.getListHandles();
      }

    try
      {
        ss.update(available, true, null, chosen, false, null);
      }
    catch (Exception e)
      {
        println("Caught exception updating StringSelector: " + e);
      }
  }

  /**
   * <p>This method comprises the JsetValueCallback interface, and is
   * how the customized data-carrying components in this
   * containerPanel notify us when something changes.</p>
   *
   * <p>Note that we don't use this method for checkboxes, or
   * comboboxes.</p>
   *
   * @see arlut.csd.JDataComponent.JsetValueCallback
   * @see arlut.csd.JDataComponent.JValueObject
   *
   * @return false if the JDataComponent that is calling us should
   * reject the value change operation and revert back to the prior
   * value.
   */

  public boolean setValuePerformed(JValueObject v)
  {
    ReturnVal returnValue = null;
    FieldTemplate fieldTemplate = null;

    /* -- */

    if (v instanceof JErrorValueObject)
      {
        gc.showErrorMessage((String)v.getValue());
        return true;
      }

    currentlyChangingComponent = (JComponent)v.getSource();

    try
      {
        // ok, now we have to connect the field change report coming
        // from the JDataComponent to the appropriate field object
        // on the Ganymede server.  First we'll try the simplest,
        // generic case.

        if ((v.getSource() instanceof JstringField) ||
            (v.getSource() instanceof JnumberField) ||
            (v.getSource() instanceof JfloatField) ||
            (v.getSource() instanceof JIPField) ||
            (v.getSource() instanceof JdateField) ||
            (v.getSource() instanceof JstringArea))
          {
            db_field field = objectHash.get(v.getSource());

            /* -- */

            try
              {
                if (debug)
                  {
                    println(field.getTypeDesc() + " trying to set to " + v.getValue());
                  }

                returnValue = field.setValue(v.getValue());
              }
            catch (Exception rx)
              {
                gc.processException(rx);

                return false;
              }
          }
        else if (v.getSource() instanceof JpassField)
          {
            pass_field field = (pass_field) objectHash.get(v.getSource());

            /* -- */

            try
              {
                if (debug)
                  {
                    println(field.getTypeDesc() + " trying to set to " + v.getValue());
                  }

                returnValue = field.setPlainTextPass((String)v.getValue());
              }
            catch (Exception rx)
              {
                gc.processException(rx);

                return false;
              }
          }
        else if (v.getSource() instanceof vectorPanel)
          {
            // no vectorPanel should really ever call this
            // method, so wtf?

            if (debug)
              {
                println("Something happened in the vector panel");
              }
          }
        else if (v.getSource() instanceof StringSelector)
          {
            StringSelector sourceComponent = (StringSelector) v.getSource();

            /* -- */

            if (debug)
              {
                println("value performed from StringSelector");
              }

            // a StringSelector data component could be feeding us any of a
            // number of conditions, that we need to check.

            // First, are we being given a menu operation from StringSelector?

            if (v instanceof JParameterValueObject)
              {
                if (debug)
                  {
                    println("MenuItem selected in a StringSelector");
                  }

                String command = (String) v.getParameter();

                if (command.equals(edit_action))
                  {
                    if (debug)
                      {
                        println("Edit object: " + v.getValue());
                      }

                    Invid invid = (Invid) v.getValue();

                    gc.editObject(invid);

                    return true;
                  }
                else if (command.equals(view_action))
                  {
                    if (debug)
                      {
                        println("View object: " + v.getValue());
                      }

                    Invid invid = (Invid) v.getValue();

                    gc.viewObject(invid);

                    return true;
                  }
                else
                  {
                    println("Unknown action command from popup: " + command);
                  }
              }
            else if (objectHash.get(sourceComponent) instanceof invid_field)
              {
                invid_field field = (invid_field) objectHash.get(sourceComponent);

                /* -- */

                if (field == null)
                  {
                    throw new RuntimeException("Could not find field in objectHash");
                  }

                try
                  {
                    if (v instanceof JAddValueObject)
                      {
                        if (debug)
                          {
                            println("Adding new value to string selector");
                          }

                        returnValue = field.addElement(v.getValue());
                      }
                    else if (v instanceof JAddVectorValueObject)
                      {
                        if (debug)
                          {
                            println("Adding new value vector to string selector");
                          }

                        returnValue = field.addElements((Vector) v.getValue());
                      }
                    else if (v instanceof JDeleteValueObject)
                      {
                        if (debug)
                          {
                            println("Removing value from field(string selector)");
                          }

                        returnValue = field.deleteElement(v.getValue());
                      }
                    else if (v instanceof JDeleteVectorValueObject)
                      {
                        if (debug)
                          {
                            println("Removing value vector from field(string selector)");
                          }

                        returnValue = field.deleteElements((Vector) v.getValue());
                      }
                  }
                catch (Exception rx)
                  {
                    gc.processExceptionRethrow(rx, "Could not change add/delete invid from field");
                  }
              }
            else if (objectHash.get(v.getSource()) instanceof string_field)
              {
                string_field field = (string_field) objectHash.get(v.getSource());

                /* -- */

                if (field == null)
                  {
                    throw new RuntimeException("Could not find field in objectHash");
                  }

                try
                  {
                    if (v instanceof JAddValueObject)
                      {
                        returnValue = field.addElement(v.getValue());
                      }
                    else if (v instanceof JAddVectorValueObject)
                      {
                        returnValue = field.addElements((Vector) v.getValue());
                      }
                    else if (v instanceof JDeleteValueObject)
                      {
                        returnValue = field.deleteElement(v.getValue());
                      }
                    else if (v instanceof JDeleteVectorValueObject)
                      {
                        returnValue = field.deleteElements((Vector) v.getValue());
                      }
                  }
                catch (Exception rx)
                  {
                    gc.processExceptionRethrow(rx, "Could not add/remove string from string_field: ");
                  }
              }
            else
              {
                println("Not an Invid in string selector.");
              }
          }
        else
          {
            println("Value performed from unknown source");
          }

        // Handle any wizards, error dialogs, or rescan commands

        returnValue = gc.handleReturnVal(returnValue);

        if (returnValue == null)  // Success, no need to do anything else
          {
            if (debug)
              {
                println("retVal is null: returning true");
              }

            gc.somethingChanged();
            return true;
          }

        if (returnValue.didSucceed())
          {
            if (debug)
              {
                println("didSucceed: Returning true.");
              }

            gc.somethingChanged();
            return true;
          }
        else
          {
            if (debug)
              {
                println("didSucceed: Returning false.");
              }

            return false;
          }
      }
    catch (NullPointerException ne)
      {
        ne.printStackTrace();
        return false;
      }
    catch (IllegalArgumentException e)
      {
        e.printStackTrace();
        return false;
      }
    catch (RuntimeException e)
      {
        e.printStackTrace();
        return false;
      }
    finally
      {
        currentlyChangingComponent = null;
      }
  }

  /**
   * Some of our components, most notably the checkboxes, don't
   * go through JDataComponent.setValuePerformed(), but instead
   * give us direct feedback.  Those we take care of here.
   *
   * @see java.awt.event.ActionListener
   */

  public void actionPerformed(ActionEvent e)
  {
    ReturnVal returnValue = null;
    db_field field = null;
    FieldTemplate fieldTemplate = null;
    boolean newValue;

    // we are only acting as an action listener for checkboxes..
    // we'll just throw a ClassCastException if this changes
    // and we haven't fixed this code to match.

    JCheckBox cb = (JCheckBox) e.getSource();

    /* -- */

    field = objectHash.get(cb);

    if (field == null)
      {
        throw new RuntimeException("Whoa, null field for a JCheckBox: " + e);
      }

    try
      {
        newValue = cb.isSelected();

        try
          {
            returnValue = field.setValue(Boolean.valueOf(newValue));
          }
        catch (Exception rx)
          {
            gc.processExceptionRethrow(rx, "Could not set field value: ");
          }

        // Handle any wizards or error dialogs resulting from the
        // field.setValue()

        returnValue = gc.handleReturnVal(returnValue);

        if (returnValue == null)
          {
            gc.somethingChanged();
          }
        else if (returnValue.didSucceed())
          {
            gc.somethingChanged();
          }
        else
          {
            // we need to undo things

            // We need to turn off ourselves as an action listener
            // while we flip this back, so we don't go through this
            // method again.

            cb.removeActionListener(this);

            cb.setSelected(!newValue);

            // and we re-enable event notification

            cb.addActionListener(this);
          }
      }
    catch (Exception ex)
      {
        // An exception was thrown, most likely from the server.  We need to revert the check box.

        printErr("Exception occured in containerPanel.actionPerformed: " + ex);

        try
          {
            Boolean b = (Boolean)field.getValue();
            cb.setSelected((b == null) ? false : b.booleanValue());
          }
        catch (Exception rx)
          {
            gc.processExceptionRethrow(rx);
          }

        gc.processExceptionRethrow(ex);
      }
  }

  /**
   * Some of our components, most notably the JComboBoxes, don't
   * go through JDataComponent.setValuePerformed(), but instead
   * give us direct feedback.  Those we take care of here.
   *
   * @see java.awt.event.ItemListener
   */

  public void itemStateChanged(ItemEvent e)
  {
    ReturnVal returnValue = null;

    // we are only acting as an action listener for comboboxes..
    // we'll just throw a ClassCastException if this changes
    // and we haven't fixed this code to match.

    JComboBox cb = (JComboBox) e.getSource();

    /* -- */

    // We don't care about deselect reports

    if (e.getStateChange() != ItemEvent.SELECTED)
      {
        return;
      }

    if (debug)
      {
        println("containerPanel.itemStateChanged(): Item selected: " + e.getItem());
      }

    // Find the field that is associated with this combo box.  Some
    // combo boxes are all by themselves, and they will be in the
    // objectHash.  Other comboBoxes are part of JInvidChoosers, and
    // they will be in the invidChooserHash

    db_field field = objectHash.get(cb);

    if (field == null)
      {
        field = invidChooserHash.get(cb);

        if (field == null)
          {
            throw new RuntimeException("Whoa, null field for a JComboBox: " + e);
          }
      }

    try
      {
        Object newValue = e.getItem();
        Object oldValue = field.getValue();

        if (newValue.equals(oldValue))
          {
            return;             // what else is new?
          }

        if (newValue instanceof String)
          {
            returnValue = field.setValue(newValue);
          }
        else if (newValue instanceof listHandle)
          {
            listHandle lh = (listHandle) newValue;

            if (debug)
              {
                if (field == null)
                  {
                    println("Field is null.");
                  }
              }

            returnValue = field.setValue(lh.getObject());
          }
        else
          {
            throw new RuntimeException("Unknown type from JComboBox: " + newValue);
          }

        // handle any wizards and/or error dialogs

        returnValue = gc.handleReturnVal(returnValue);

        if (returnValue == null)
          {
            gc.somethingChanged();

            if (debug)
              {
                println("field setValue returned true");
              }
          }
        else if (returnValue.didSucceed())
          {
            if (debug)
              {
                println("field setValue returned true!!");
              }

            gc.somethingChanged();
          }
        else
          {
            // Failure.. need to revert the combobox

            // turn off callbacks

            cb.removeItemListener(this);

            if (oldValue == null)
              {
                cb.setSelectedItem(null);
              }
            else if (newValue instanceof String)
              {
                cb.setSelectedItem(oldValue);
              }
            else if (newValue instanceof listHandle)
              {
                listHandle lh = new listHandle(gc.getSession().viewObjectLabel((Invid) oldValue), oldValue);
                cb.setSelectedItem(lh);
              }

            // turn callbacks back on

            cb.addItemListener(this);
          }
      }
    catch (Exception rx)
      {
        gc.processExceptionRethrow(rx);
      }
  }

  /**
   * Helper method to add a component during constructor operation.  This
   * is the top-level field component adding method.
   */

  private void addFieldComponent(db_field field,
                                 FieldInfo fieldInfo,
                                 FieldTemplate fieldTemplate) throws RemoteException
  {
    short fieldType;
    boolean isVector;

    /* -- */

    if (!keepLoading())
      {
        return;
      }

    if (field == null)
      {
        throw new IllegalArgumentException("null field");
      }

    fieldType = fieldTemplate.getType();
    isVector = fieldTemplate.isArray();

    if (debug)
      {
        println(" Name: " + fieldTemplate.getName() + " Field type desc: " + fieldType);
      }

    if (isVector)
      {
        if (fieldType == FieldType.STRING)
          {
            addStringVector((string_field) field, fieldInfo, fieldTemplate);
          }
        else if (fieldType == FieldType.INVID && !fieldTemplate.isEditInPlace())
          {
            addInvidVector((invid_field) field, fieldInfo, fieldTemplate);
          }
        else                    // generic vector
          {
            addVectorPanel(field, fieldInfo, fieldTemplate);
          }
      }
    else
      {
        // plain old component

        switch (fieldType)
          {
          case -1:
            printErr("**** Could not get field information");
            break;

          case FieldType.STRING:
            addStringField((string_field) field, fieldInfo, fieldTemplate);
            break;

          case FieldType.PASSWORD:
            addPasswordField((pass_field) field, fieldInfo, fieldTemplate);
            break;

          case FieldType.NUMERIC:
            addNumericField(field, fieldInfo, fieldTemplate);
            break;

          case FieldType.FLOAT:
            addFloatField(field, fieldInfo, fieldTemplate);
            break;

          case FieldType.DATE:
            addDateField(field, fieldInfo, fieldTemplate);
            break;

          case FieldType.BOOLEAN:
            addBooleanField(field, fieldInfo, fieldTemplate);
            break;

          case FieldType.PERMISSIONMATRIX:
            addPermissionField(field, fieldInfo, fieldTemplate);
            break;

          case FieldType.FIELDOPTIONS:
            addFieldOptionsField(field, fieldInfo, fieldTemplate);
            break;

          case FieldType.INVID:
            addInvidField((invid_field)field, fieldInfo, fieldTemplate);
            break;

          case FieldType.IP:
            addIPField((ip_field) field, fieldInfo, fieldTemplate);
            break;

          default:
            JLabel label = new JLabel("(Unknown)Field type ID = " + fieldType);
            contentsPanel.addRow(fieldTemplate.getName(), label);
          }
      }
  }

  /**
   * private helper method to instantiate a string vector in this
   * container panel
   */

  private void addStringVector(string_field field,
                               FieldInfo fieldInfo,
                               FieldTemplate fieldTemplate) throws RemoteException
  {
    objectList list = null;

    /* -- */

    if (debug)
      {
        println("Adding StringSelector, its a vector of strings!");
      }

    if (field == null)
      {
        throw new NullPointerException();
      }

    if (editable && fieldInfo.isEditable())
      {
        QueryResult qr = null;

        if (debug)
          {
            println("Getting choicesKey()");
          }

        Object id = field.choicesKey();

        if (id == null)
          {
            if (debug)
              {
                println("Key is null, Getting choices");
              }

            qr = field.choices();

            if (qr != null)
              {
                list = new objectList(qr);
              }
          }
        else
          {
            if (gc.cachedLists.containsList(id))
              {
                list = gc.cachedLists.getList(id);
              }
            else
              {
                if (debug)
                  {
                    println("Getting QueryResult now");
                  }

                qr = field.choices();

                if (qr != null)
                  {
                    gc.cachedLists.putList(id, qr);
                    list = gc.cachedLists.getList(id);
                  }
              }
          }

        if (!keepLoading())
          {
            if (debug)
              {
                println("Stopping containerPanel in the midst of loading a StringSelector");
              }

            return;
          }

        if (list == null)
          {
            StringSelector ss = new StringSelector(this,
                                                   true, // editable
                                                   false, // canChoose
                                                   false); // mustChoose

            ss.setCellWidth(300);
            ss.setMinimumRowCount(3);
            ss.setMaximumRowCount(8);

            ss.update(null, true, null, (Vector) fieldInfo.getValue(), true, null);

            registerComponent(ss, field, fieldTemplate);

            ss.setCallback(this);

            String comment = fieldTemplate.getComment();

            if (comment != null && !comment.equals(""))
              {
                ss.setToolTipText(comment);
              }

            associateFieldId(fieldInfo, ss);

            contentsPanel.addFillRow(fieldTemplate.getName(), ss);
            contentsPanel.setRowVisible(ss, fieldInfo.isVisible());
          }
        else
          {
            Vector available = list.getLabels(false);
            StringSelector ss = new StringSelector(this,
                                                   true, // editable
                                                   true,   // canChoose
                                                   false);  // mustChoose
            ss.setCellWidth(150);
            ss.setMinimumRowCount(3);
            ss.setMaximumRowCount(8);
            ss.update(available, true, null, (Vector) fieldInfo.getValue(), true, null);

            registerComponent(ss, field, fieldTemplate);

            ss.setCallback(this);

            String comment = fieldTemplate.getComment();

            if (comment != null && !comment.equals(""))
              {
                ss.setToolTipText(comment);
              }

            associateFieldId(fieldInfo, ss);

            contentsPanel.addFillRow(fieldTemplate.getName(), ss, 2);
            contentsPanel.setRowVisible(ss, fieldInfo.isVisible());
          }
      }
    else  //not editable, don't need whole list of things
      {
        StringSelector ss = new StringSelector(this,
                                               false, // not editable
                                               false,   // canChoose
                                               false);  // mustChoose
        ss.setCellWidth(300);
        ss.setMinimumRowCount(3);
        ss.setMaximumRowCount(8);

        ss.update(null, false, null, (Vector) fieldInfo.getValue(), true, null);

        registerComponent(ss, field, fieldTemplate);

        String comment = fieldTemplate.getComment();

        if (comment != null && !comment.equals(""))
          {
            ss.setToolTipText(comment);
          }

        associateFieldId(fieldInfo, ss);

        contentsPanel.addFillRow(fieldTemplate.getName(), ss, 1);
        contentsPanel.setRowVisible(ss, fieldInfo.isVisible());
      }
  }

  /**
   * private helper method to instantiate an invid vector in this
   * container panel
   */

  private void addInvidVector(invid_field field,
                              FieldInfo fieldInfo,
                              FieldTemplate fieldTemplate) throws RemoteException
  {
    Vector
      valueHandles = null,
      choiceHandles = null;

    objectList
      list = null;

    /* -- */

    if (debug)
      {
        println("Adding StringSelector, it's a vector of invids!");
      }

    QueryResult qres = field.encodedValues();

    if (qres != null)
      {
        valueHandles = qres.getListHandles();
      }

    if (!keepLoading())
      {
        if (debug)
          {
            println("Stopping containerPanel in the midst of loading a StringSelector");
          }

        return;
      }

    if (editable && fieldInfo.isEditable())
      {
        Object key = field.choicesKey();

        if (key == null)
          {
            if (debug)
              {
                println("key is null, downloading new copy");
              }

            QueryResult choices = field.choices();

            if (choices != null)
              {
                // the server may not automatically restrict inactive
                // objects from the list, so we want to exclude inactive
                // objects.  We don't want to exclude non-editable objects,
                // however, because the server may include nominally
                // non-editable objects that we are granted permission
                // to link to by the DBEditObject.anonymousLinkOK()
                // method.

                choiceHandles = choices.getListHandles(false, true);
              }
            else
              {
                if (debug)
                  {
                    println("choices is null");
                  }

                choiceHandles = null;
              }
          }
        else
          {
            if (debug)
              {
                println("key= " + key);
              }

            if (gc.cachedLists.containsList(key))
              {
                if (debug)
                  {
                    println("It's in there, using cached list");
                  }

                // when we are drawing a list of choices from the cache,
                // we know that the server didn't filter that list for
                // us so that the 'non-editables' are actually valid
                // choices in this context, so we don't want either
                // inactive nor non-editable choices.

                choiceHandles = gc.cachedLists.getListHandles(key, false, false);
              }
            else
              {
                if (debug)
                  {
                    println("Choice list for addInvidVector not cached, downloading choices.");
                  }

                QueryResult qr = field.choices();

                if (qr == null)
                  {
                    choiceHandles = null;
                  }
                else
                  {
                    gc.cachedLists.putList(key, qr);
                    list = gc.cachedLists.getList(key);

                    // when the server gives us a choice key, we know
                    // that the server didn't filter that list for us
                    // so that the 'non-editables' are actually valid
                    // choices in this context, so we don't want
                    // either inactive nor non-editable choices.

                    choiceHandles = list.getListHandles(false, false);
                  }

                // debuging stuff

                if (debug_persona)
                  {
                    System.out.println();

                    for (int i = 0; i < choiceHandles.size(); i++)
                      {
                        println(" choices: " + (listHandle)choiceHandles.elementAt(i));
                      }

                    System.out.println();
                  }
              }
          }
      }
    else
      {
        if (debug)
          {
            println("Not editable, not downloading choices");
          }
      }

    // ss is canChoose, mustChoose
    JPopupMenu invidTablePopup = new JPopupMenu();
    JMenuItem viewO = new JMenuItem(view_action);
    JMenuItem editO = new JMenuItem(edit_action);
    invidTablePopup.add(viewO);
    invidTablePopup.add(editO);

    JPopupMenu invidTablePopup2 = new JPopupMenu();
    JMenuItem viewO2 = new JMenuItem(view_action);
    JMenuItem editO2 = new JMenuItem(edit_action);
    invidTablePopup2.add(viewO2);
    invidTablePopup2.add(editO2);

    if (debug)
      {
        println("Creating StringSelector");
      }

    StringSelector ss = new StringSelector(this,
                                           editable && fieldInfo.isEditable(),
                                           true, true);

    ss.setMinimumRowCount(3);
    ss.setMaximumRowCount(8);

    ss.setCellWidth(editable && fieldInfo.isEditable() ? 150: 300);
    ss.update(choiceHandles, true, null, valueHandles, true, null);
    ss.setPopups(invidTablePopup, invidTablePopup2);

    registerComponent(ss, field, fieldTemplate);

    ss.setCallback(this);

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
        ss.setToolTipText(comment);
      }

    associateFieldId(fieldInfo, ss);

    contentsPanel.addFillRow(fieldTemplate.getName(), ss, choiceHandles == null ? 1 : 2);
    contentsPanel.setRowVisible(ss, fieldInfo.isVisible());
  }

  /**
   * private helper method to instantiate a vector panel in this
   * container panel
   */

  private void addVectorPanel(db_field field,
                              FieldInfo fieldInfo,
                              FieldTemplate fieldTemplate) throws RemoteException
  {
    boolean isEditInPlace = fieldTemplate.isEditInPlace();

    /* -- */

    if (debug)
      {
        if (isEditInPlace)
          {
            println("Adding editInPlace vector panel");
          }
        else
          {
            println("Adding normal vector panel");
          }
      }

    vectorPanel vp = new vectorPanel(field, fieldTemplate, winP, editable && fieldInfo.isEditable(),
                                     isEditInPlace, this, isCreating);
    vectorPanelList.add(vp);

    registerComponent(vp, field, fieldTemplate);

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
        vp.setToolTipText(comment);
      }

    associateFieldId(fieldInfo, vp);

    contentsPanel.addFillRow(fieldTemplate.getName(), vp, 3);
    contentsPanel.setRowVisible(vp, fieldInfo.isVisible());
  }

  /**
   * If we contain any {@link arlut.csd.ganymede.client.vectorPanel vectorPanel}s,
   * they will call this method during loading to let us update our progress bar if
   * we have it still up.  This is used to let us include the time it will take
   * to get vector panels loaded in the progress bar time estimate.
   */

  public void vectorElementAdded()
  {
    if (progressBar != null)
      {
        progressBar.setValue(progressBar.getValue() + 1);
      }

    ++vectorElementsAdded;
  }

  /**
   * private helper method to instantiate a scalar string field in this
   * container panel
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addStringField(string_field field, FieldInfo fieldInfo,
                              FieldTemplate fieldTemplate) throws RemoteException
  {
    objectList
      list;

    JstringField
      sf;

    boolean
      mustChoose;

    /* -- */

    if (this.editable && fieldInfo.isEditable() && field.canChoose())
      {
        if (debug)
          {
            println("You can choose");
          }

        Vector choices = null;

        Object key = field.choicesKey();

        if (key == null)
          {
            if (debug)
              {
                println("key is null, getting new copy.");
              }

            choices = field.choices().getLabels();
          }
        else
          {
            if (debug)
              {
                println("key = " + key);
              }

            if (gc.cachedLists.containsList(key))
              {
                if (debug)
                  {
                    println("key in there, using cached list");
                  }

                list = gc.cachedLists.getList(key);
              }
            else
              {
                if (debug)
                  {
                    println("Choice list for addStringField not cached, downloading a new one.");
                  }

                gc.cachedLists.putList(key, field.choices());
                list = gc.cachedLists.getList(key);
              }

            choices = list.getLabels(false);
          }

        String currentChoice = (String) fieldInfo.getValue();

        final JComboBox combo = new JComboBox(choices);
        combo.setKeySelectionManager(new TimedKeySelectionManager());

        combo.setMaximumRowCount(8);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));

        try
          {
            mustChoose = field.mustChoose();
          }
        catch (Exception rx)
          {
            gc.processException(rx);
            throw new RuntimeException(rx);
          }

        combo.setEditable(!mustChoose);

        if (currentChoice == null)
          {
            if (debug)
              {
                println("Setting current value to <none>, because the current choice is null. " + currentChoice);
              }

            combo.addItem("<none>");
            combo.setSelectedItem("<none>");
          }
        else
          {
            if (debug)
              {
                println("Setting current value: " + currentChoice);
              }

            try
              {
                combo.setSelectedItem(currentChoice);
              }
            catch (IllegalArgumentException e)
              {
                println("IllegalArgumentException: current choice is not in the string selection combobox.  Adding it now.");
                combo.addItem(currentChoice);
                combo.setSelectedItem(currentChoice);
              }
          }

        if (editable && fieldInfo.isEditable())
          {
            combo.addItemListener(this); // register callback
          }

        combo.addFocusListener(new FocusListener()
          {
            public void focusLost(FocusEvent e)
            {
            }

            public void focusGained(FocusEvent e)
            {
              scrollRectToVisible(combo.getBounds());
            }

          });

        Component editor = combo.getEditor().getEditorComponent();

        editor.addFocusListener(new FocusListener()
          {
            public void focusLost(FocusEvent e)
            {
            }

            public void focusGained(FocusEvent e)
            {
              scrollRectToVisible(combo.getBounds());
            }

          });

        registerComponent(combo, field, fieldTemplate);

        String comment = fieldTemplate.getComment();

        if (comment != null && !comment.equals(""))
          {
            combo.setToolTipText(comment);
          }

        associateFieldId(fieldInfo, combo);

        contentsPanel.addFillRow(fieldTemplate.getName(), combo, 1);
        contentsPanel.setRowVisible(combo, fieldInfo.isVisible());
      }
    else if (fieldTemplate.isMultiLine())
      {
        JstringArea sa = new JstringArea(6, FIELDWIDTH);

        registerComponent(sa, field, fieldTemplate);

        sa.setAllowedChars(fieldTemplate.getOKChars());
        sa.setDisallowedChars(fieldTemplate.getBadChars());
        sa.setText((String)fieldInfo.getValue());

        if (editable && fieldInfo.isEditable())
          {
            sa.setCallback(this);
          }
        else
          {
            // make sure we show the start of the non-editable string

            sa.setCaretPosition(0);
          }

        sa.setEditable(editable && fieldInfo.isEditable());

        String comment = fieldTemplate.getComment();

        if (comment != null && !comment.equals(""))
          {
            sa.setToolTipText(comment);
          }

        associateFieldId(fieldInfo, sa);

        contentsPanel.addFillRow(fieldTemplate.getName(), sa, 1);
        contentsPanel.setRowVisible(sa, fieldInfo.isVisible());
      }
    else
      {
        // It's not a choice

        int maxLength = fieldTemplate.getMaxLength();

        sf = new JstringField(FIELDWIDTH > maxLength ? maxLength + 1 : FIELDWIDTH,
                              maxLength,
                              editable && fieldInfo.isEditable(),
                              false,
                              fieldTemplate.getOKChars(),
                              fieldTemplate.getBadChars(),
                              this);

        registerComponent(sf, field, fieldTemplate);

        sf.setText((String)fieldInfo.getValue());

        if (editable && fieldInfo.isEditable())
          {
            sf.setCallback(this);
          }

        sf.setEditable(editable && fieldInfo.isEditable());

        String comment = fieldTemplate.getComment();

        if (comment != null && !comment.equals(""))
          {
            sf.setToolTipText(comment);
          }

        associateFieldId(fieldInfo, sf);

        contentsPanel.addFillRow(fieldTemplate.getName(), sf, 1);
        contentsPanel.setRowVisible(sf, fieldInfo.isVisible());
      }
  }

  /**
   * <p>Private helper method to instantiate a password field in this
   * container panel.</p>
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addPasswordField(pass_field field,
                                FieldInfo fieldInfo,
                                FieldTemplate fieldTemplate) throws RemoteException
  {
    JstringField sf;

    /* -- */

    if (editable && fieldInfo.isEditable())
      {
        JpassField pf = new JpassField(gc, 10, 8, editable && fieldInfo.isEditable());

        registerComponent(pf, field, fieldTemplate);

        if (editable && fieldInfo.isEditable())
          {
            pf.setCallback(this);
          }

        String comment = fieldTemplate.getComment();

        if (comment != null && !comment.equals(""))
          {
            pf.setToolTipText(comment);
          }

        associateFieldId(fieldInfo, pf);

        contentsPanel.addRow(fieldTemplate.getName(), pf);
        contentsPanel.setRowVisible(pf, fieldInfo.isVisible());
      }
    else
      {
        int maxLength = fieldTemplate.getMaxLength();
        sf = new JstringField(FIELDWIDTH > maxLength ? maxLength + 1 : FIELDWIDTH,
                              maxLength,
                              true,
                              false,
                              null,
                              null);

        registerComponent(sf, field, fieldTemplate);

        // the server won't give us an unencrypted password, we're clear here

        sf.setText((String)fieldInfo.getValue());

        sf.setEditable(false);

        String comment = fieldTemplate.getComment();

        if (comment != null && !comment.equals(""))
          {
            sf.setToolTipText(comment);
          }

        associateFieldId(fieldInfo, sf);

        contentsPanel.addFillRow(fieldTemplate.getName(), sf, 1);
        contentsPanel.setRowVisible(sf, fieldInfo.isVisible());
      }
  }

  /**
   * private helper method to instantiate a numeric field in this
   * container panel
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addNumericField(db_field field,
                               FieldInfo fieldInfo,
                               FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
        println("Adding numeric field");
      }

    JnumberField nf = new JnumberField();

    registerComponent(nf, field, fieldTemplate);

    Integer value = (Integer)fieldInfo.getValue();

    if (value != null)
      {
        nf.setValue(value.intValue());
      }

    if (editable && fieldInfo.isEditable())
      {
        nf.setCallback(this);
      }

    nf.setEditable(editable && fieldInfo.isEditable());
    nf.setColumns(FIELDWIDTH);

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
        nf.setToolTipText(comment);
      }

    associateFieldId(fieldInfo, nf);

    contentsPanel.addFillRow(fieldTemplate.getName(), nf, 1);
    contentsPanel.setRowVisible(nf, fieldInfo.isVisible());
  }

   /**
    * private helper method to instantiate a numeric field in this
    * container panel
    *
    * @param field Remote reference to database field to be associated with a gui component
    * @param fieldInfo Downloaded value and status information for this field
    * @param fieldTemplate Downloaded static field type information for this field
    */

  private void addFloatField(db_field field,
                             FieldInfo fieldInfo,
                             FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
        println("Adding float field");
      }

    JfloatField nf = new JfloatField();

    registerComponent(nf, field, fieldTemplate);

    Double value = (Double)fieldInfo.getValue();

    if (value != null)
      {
        nf.setValue(value.doubleValue());
      }

    if (editable && fieldInfo.isEditable())
      {
        nf.setCallback(this);
      }

    nf.setEditable(editable && fieldInfo.isEditable());
    nf.setColumns(FIELDWIDTH);

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
        nf.setToolTipText(comment);
      }

    associateFieldId(fieldInfo, nf);

    contentsPanel.addFillRow(fieldTemplate.getName(), nf, 1);
    contentsPanel.setRowVisible(nf, fieldInfo.isVisible());
  }

  /**
   * private helper method to instantiate a date field in this
   * container panel
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addDateField(db_field field,
                            FieldInfo fieldInfo,
                            FieldTemplate fieldTemplate) throws RemoteException
  {
    boolean enabled = editable && fieldInfo.isEditable();
    Date date = (Date) fieldInfo.getValue();
    JsetValueCallback callback = null;

    if (enabled)
      {
        callback = this;
      }

    date_field datef = (date_field) field;

    JdateField df = null;

    if (fieldInfo.isEditable())
      {
        df = new JdateField(date, enabled, datef.limited(), true, datef.minDate(), datef.maxDate(), callback);
      }
    else
      {
        df = new JdateField(date, enabled, false, true, null, null, callback);
      }

    registerComponent(df, field, fieldTemplate);

    if (debug)
      {
        println("Editable: " + editable  + " isEditable: " + fieldInfo.isEditable());
      }

    associateFieldId(fieldInfo, df);

    contentsPanel.addRow(fieldTemplate.getName(), df);
    contentsPanel.setRowVisible(df, fieldInfo.isVisible());
  }

  /**
   * private helper method to instantiate a boolean field in this
   * container panel
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addBooleanField(db_field field, FieldInfo fieldInfo,
                               FieldTemplate fieldTemplate) throws RemoteException
  {
    JCheckBox cb = new JCheckBox();

    /* -- */

    registerComponent(cb, field, fieldTemplate);

    cb.setEnabled(editable && fieldInfo.isEditable());

    if (editable && fieldInfo.isEditable())
      {
        cb.addActionListener(this);     // register callback
      }

    try
      {
        cb.setSelected(((Boolean)fieldInfo.getValue()).booleanValue());
      }
    catch (NullPointerException ex)
      {
        if (debug)
          {
            println("Null pointer setting selected choice: " + ex);
          }
      }

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
        cb.setToolTipText(comment);
      }

    associateFieldId(fieldInfo, cb);

    contentsPanel.addRow(fieldTemplate.getName(), cb);
    contentsPanel.setRowVisible(cb, fieldInfo.isVisible());
  }

  /**
   * private helper method to instantiate a field options field in this
   * container panel
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addFieldOptionsField(db_field field, FieldInfo fieldInfo,
                                    FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
        println("Adding field options matrix");
      }

    // note that the field options editor does its own callbacks to
    // the server, albeit using our transaction / session.

    fieldoption_button fob = new fieldoption_button((field_option_field) field,
                                                    this.object,
                                                    editable && fieldInfo.isEditable(),
                                                    gc,
                                                    fieldTemplate.getName());

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
        fob.setToolTipText(comment);
      }

    associateFieldId(fieldInfo, fob);

    contentsPanel.addRow(fieldTemplate.getName(), fob);
    contentsPanel.setRowVisible(fob, fieldInfo.isVisible());
  }


  /**
   * private helper method to instantiate a permission matrix field in this
   * container panel
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addPermissionField(db_field field, FieldInfo fieldInfo,
                                  FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
        println("Adding perm matrix");
      }

    // note that the permissions editor does its own callbacks to
    // the server, albeit using our transaction / session.

    perm_button pb = new perm_button((perm_field) field,
                                     editable && fieldInfo.isEditable(),
                                     gc,
                                     false,
                                     fieldTemplate.getName());

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
        pb.setToolTipText(comment);
      }

    associateFieldId(fieldInfo, pb);

    contentsPanel.addRow(fieldTemplate.getName(), pb);
    contentsPanel.setRowVisible(pb, fieldInfo.isVisible());
  }

  /**
   * private helper method to instantiate a scalar invid field in this
   * container panel
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addInvidField(invid_field field,
                             FieldInfo fieldInfo,
                             FieldTemplate fieldTemplate) throws RemoteException
  {
    objectList list;

    /* -- */

    if (debug)
      {
        println("addInvidField(" + fieldTemplate.getName() + ")");
      }

    if (fieldTemplate.isEditInPlace())
      {
        // this should never happen

        if (debug)
          {
            println("Hey, " + fieldTemplate.getName() +
                    " is edit in place but not a vector, what gives?");
          }

        JLabel errorLabel = new JLabel("edit in place non-vector");

        contentsPanel.addFillRow(fieldTemplate.getName(), errorLabel);
        contentsPanel.setRowVisible(errorLabel, fieldInfo.isVisible());

        return;
      }

    if (!editable || !fieldInfo.isEditable())
      {
        if (fieldInfo.getValue() == null)
          {
            // "Null Pointer"
            JLabel errorLabel = new JLabel(ts.l("addInvidField.null_invid"));

            contentsPanel.addFillRow(fieldTemplate.getName(), errorLabel);
            contentsPanel.setRowVisible(errorLabel, fieldInfo.isVisible());

            return;
          }

        final Invid thisInvid = (Invid) fieldInfo.getValue();

        String label = (String) gc.getSession().viewObjectLabel(thisInvid);

        if (label == null)
          {
            if (debug)
              {
                println("-you don't have permission to view this object.");
              }

            // "Can''t Show Target"
            label = ts.l("addInvidField.no_view_perm");
          }

        JButton b = new JButton(label);

        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
              getgclient().viewObject(thisInvid);
            }
          });

        String comment = fieldTemplate.getComment();

        if (comment != null && !comment.equals(""))
          {
            b.setToolTipText(comment);
          }

        // add the button to our objectHash so that we can change
        // the label if the user edits the label field of the
        // object we point to.

        registerComponent(b, field, fieldTemplate);

        contentsPanel.addRow(fieldTemplate.getName(), b);
        contentsPanel.setRowVisible(b, fieldInfo.isVisible());

        return;
      }

    if (!keepLoading())
      {
        return;         // we were told to cancel
      }

    // okay, we've got an editable field, we need to construct and install
    // a JInvidChooser.

    // get the list of choices for this invid field

    Object key = field.choicesKey();

    if (key != null)
      {
        if (!gc.cachedLists.containsList(key))
          {
            gc.cachedLists.putList(key, field.choices());
          }

        list = gc.cachedLists.getList(key);
      }
    else
      {
        list = new objectList(field.choices());
      }

    // we have to include non-editables, because the server will
    // include some that are non-editable, but for which
    // DBEditObject.anonymousLinkOK() nonetheless give us rights
    // to link.

    Vector<listHandle> choices = list.getListHandles(false, true);

    Invid currentChoice = (Invid) fieldInfo.getValue();
    String currentChoiceLabel = null;

    if (debug)
      {
        currentChoiceLabel = gc.getSession().viewObjectLabel(currentChoice);
        println("Current choice is : " + currentChoice + ", " + currentChoiceLabel);
      }

    listHandle currentListHandle = null;
    listHandle noneHandle = new listHandle(ts.l("global.none"), null); // "<none>"
    boolean found = false;
    JInvidChooser combo;
    boolean mustChoose = false;

    try
      {
        mustChoose = field.mustChoose();
      }
    catch (Exception rx)
      {
        gc.processExceptionRethrow(rx);
      }

    // Find currentListHandle

    // Make sure the current choice is in the chooser, if there is
    // a current choice.

    if (currentChoice != null)
      {
        for (listHandle thisChoice: choices)
          {
            if (thisChoice.getObject() == null)
              {
                println("Current object " + thisChoice + " is null.");
              }

            if (currentChoice.equals(thisChoice.getObject()))
              {
                if (debug)
                  {
                    println("Found the current object in the list!");
                  }

                currentListHandle = thisChoice;
                found = true;
                break;
              }
          }

        if (!found)
          {
            currentListHandle = new listHandle(gc.getSession().viewObjectLabel(currentChoice), currentChoice);
            choices.add(currentListHandle);
          }
      }

    if (!mustChoose || (currentChoice == null))
      {
        if (debug)
          {
            println("inserting null handle");
          }

        choices.insertElementAt(noneHandle, 0);
      }

    if (debug)
      {
        println("creating JInvidChooser");
      }

    combo = new JInvidChooser(choices, this, fieldTemplate.getTargetBase());

    combo.setMaximumRowCount(12);
    combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
    combo.setVisible(true);

    if (currentChoice != null)
      {
        if (debug)
          {
            println("setting current choice: " + currentChoiceLabel);
          }

        try
          {
            combo.setSelectedItem(currentListHandle);
          }
        catch (IllegalArgumentException e)
          {
            println("IllegalArgumentException: current handle not in the list, adding it now.");
            combo.addItem(currentListHandle);
            combo.setSelectedItem(currentListHandle);
          }
      }
    else
      {
        if (debug)
          {
            println("currentChoice is null");
          }

        combo.setSelectedItem(noneHandle);
      }

    if (editable && fieldInfo.isEditable())
      {
        combo.addItemListener(this); // register callback
      }

    combo.setAllowNone(!mustChoose);

    // We get the itemStateChanged straight from the JComboBox in the
    // JInvidChooser, so we need to save an association between the
    // combobox and the field

    invidChooserHash.put(combo.getCombo(), field);

    // The update method still need to be able to find the field from
    // the JInvidChooser, so we save it in objectHash, too.

    registerComponent(combo, field, fieldTemplate);

    if (debug)
      {
        println("Adding to panel");
      }

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
        combo.setToolTipText(comment);
      }

    associateFieldId(fieldInfo, combo);

    contentsPanel.addRow(fieldTemplate.getName(), combo);
    contentsPanel.setRowVisible(combo, fieldInfo.isVisible());
  }

  /**
   * private helper method to instantiate an ip address field in this
   * container panel
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addIPField(ip_field field,
                          FieldInfo fieldInfo,
                          FieldTemplate fieldTemplate) throws RemoteException
  {
    JIPField
      ipf;

    IPAddress address;

    /* -- */

    if (debug)
      {
        println("Adding IP field");
      }

    try
      {
        ipf = new JIPField(editable && fieldInfo.isEditable(),
                           (editable && fieldInfo.isEditable()) ? field.v6Allowed() : field.isIPV6());
      }
    catch (Exception rx)
      {
        gc.processException(rx, "Could not determine if v6 Allowed for ip field: ");
        throw new RuntimeException(rx);
      }

    registerComponent(ipf, field, fieldTemplate);

    address = (IPAddress) fieldInfo.getValue();

    if (address != null)
      {
        ipf.setValue(address);
      }

    ipf.setCallback(this);

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
        ipf.setToolTipText(comment);
      }

    associateFieldId(fieldInfo, ipf);

    contentsPanel.addFillRow(fieldTemplate.getName(), ipf, 1);
    contentsPanel.setRowVisible(ipf, fieldInfo.isVisible());
  }

  private void associateFieldId(FieldInfo fieldInfo, Component comp)
  {
    idHash.put(fieldInfo.getIDObj(), comp);
  }

  /**
   * <p>This method provides a handy way to null out data structures
   * held in relationship to this containerPanel, particularly network
   * reference resources.</p>
   *
   * <p>It is essential that this method be called from the client's
   * GUI thread.</p>
   */

  public synchronized final void cleanup()
  {
    if (debug)
      {
        printErr("containerPanel cleanUp()");
      }

    contentsPanel.cleanup();

    gc = null;
    invid = null;
    winP = null;
    frame = null;

    if (updatesWhileLoading != null)
      {
        updatesWhileLoading.setSize(0);
        updatesWhileLoading = null;
      }

    if (vectorPanelList != null)
      {
        vectorPanelList.setSize(0);
        vectorPanelList = null;
      }

    currentlyChangingComponent = null;

    /**
     * The critical ones.. this will release our references to fields
     * on the server
     */

    object = null;

    if (objectHash != null)
      {
        objectHash.clear();
        objectHash = null;
      }

    if (objectTemplateHash != null)
      {
        objectTemplateHash.clear();
        objectTemplateHash = null;
      }

    if (invidChooserHash != null)
      {
        invidChooserHash.clear();
        invidChooserHash = null;
      }

    if (infoVector != null)
      {
        infoVector.setSize(0);
        infoVector = null;
      }

    if (templates != null)
      {
        // it is critical that we don't setSize(0) on templates,
        // as it is belongs to Loader.  we can drop our reference
        // to it, though.

        templates = null;
      }

    progressBar = null;
  }

  /**
   * This private helper method records associations between the
   * guiComponent and the db_field and field id connected to that gui
   * component, so that we can rapidly look up the information as
   * needed during runtime.
   */

  private void registerComponent(JComponent guiComponent, db_field field, FieldTemplate fieldTemplate)
  {
    objectHash.put(guiComponent, field);
    objectTemplateHash.put(guiComponent, fieldTemplate);
  }
}
