/*

   brian_editor.java

   Description.
   
   Created: 18 November 1998
   Version: $Revision: 1.7 $ %D%
   Module By: Brian O'Mara omara@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.rmi.RemoteException;

import jdj.PackageResources; 

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.JSeparator;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                     perm_editor

------------------------------------------------------------------------------*/

class brian_editor extends JDialog implements ActionListener, Runnable {

  static final int MAXBOXES = 4;
  static final int CREATABLE = 0;
  static final int VISIBLE = 1;
  static final int EDITABLE = 2;
  static final int DELETABLE = 3;
  boolean debug = false;

  // --

  boolean enabled;
  Session session;
  perm_field permField;
  PermMatrix matrix, templateMatrix;
  Vector rowVector;

  gclient gc;

  JButton OkButton = new JButton ("Ok");
  JButton CancelButton = new JButton("Cancel");
 
  boolean keepLoading = true;

  JProgressBar progressBar;
  JDialog progressDialog;
  JButton cancelLoadingButton;
  JScrollPane edit_pane;
  // * Layout Stuff *

  JPanel 
    Choice_Buttons,
    waitPanel;
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints gbc = new GridBagConstraints();

  /* -- */

  /**
   *
   * Constructor
   *
   * @param permField Who do we talk to to get the permissions?
   * @param enabled If false, it will not be possible to edit permissions, will just display
   * @param gc The gclient that connects us to the client-side schema caches
   * @param parent The frame we are attaching this dialog to
   * @param DialogTitle The title for this dialog box
   * @param justShowUser If true, this permissions editor will only show the user object.
   * This is used when editing the 'self permissions' object in the database.
   *
   */

  public brian_editor (perm_field permField, 
		       boolean enabled, gclient gc,
		       Frame parent, String DialogTitle,
		       boolean justShowUser) // need to get rid of justShowUser
  {
    super(parent, DialogTitle, true); // the boolean value is to make the dialog modal
    

    // Main constructor for the perm_editor window      

    this.session = session;
    this.permField = permField;
    this.enabled = enabled;
    this.gc = gc;
    
    if (!debug)
      {
	this.debug = gc.debug;
      }


    // Set up progress bar stuff

    progressBar = new JProgressBar();
    progressBar.setBorder(gc.emptyBorder10);
    progressBar.setMinimum(0);
    progressBar.setMaximum(29);
    progressBar.setValue(0);

    JPanel progressBarPanel = new JPanel();
    progressBarPanel.add(progressBar);

    waitPanel = new JPanel(new BorderLayout(5, 5));
    waitPanel.add("Center", progressBarPanel);
    waitPanel.add("South", new JSeparator());

    progressDialog = new JDialog(gc, "Loading permission editor", false);    
    progressDialog.getContentPane().setLayout(new BorderLayout(5, 5));
    progressDialog.getContentPane().add("Center", waitPanel);
    
    cancelLoadingButton = new JButton("Cancel");
    cancelLoadingButton.addActionListener(new ActionListener()
					  {
					    public void actionPerformed(ActionEvent e)
					      {
						keepLoading = false;
					      }
					  });

    JPanel cancelButtonPanel = new JPanel();
    cancelButtonPanel.add(cancelLoadingButton);
    progressDialog.getContentPane().add("South", cancelButtonPanel);

    JLabel loadingLabel = new JLabel("Loading permissions editor", SwingConstants.CENTER);
    loadingLabel.setBorder(gc.emptyBorder10);
    progressDialog.getContentPane().add("North", loadingLabel);

    Rectangle b = gc.getBounds();
    progressDialog.setLocation(b.width/2 + b.x - 75, b.height/2 + b.y - 50);
    progressDialog.pack();

    progressDialog.setVisible(true);

    Thread t = new Thread(this);
    t.start();
  }

  public void run() 
  {
    System.out.println("Starting thread");


    // Get a quick dump of the permission field's state

    try
      {
	this.matrix = permField.getMatrix();
	this.templateMatrix = permField.getTemplateMatrix();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't get permission matrix\n" + ex.getMessage());
      }
    
    //    setBackground(Color.white); 

    if (debug)
      {
	System.out.println("Starting Permissions Editor Initialization");
      }
    
    OkButton.addActionListener(this);
    CancelButton.addActionListener(this);
    OkButton.setBackground(Color.lightGray);
    CancelButton.setBackground(Color.lightGray);

    Choice_Buttons = new JPanel(); 
    Choice_Buttons.setLayout(new FlowLayout ());
    Choice_Buttons.add(OkButton);
    Choice_Buttons.add(CancelButton);

    progressBar.setValue(2);
    
    try 
      {

	rowVector = initRowVector();

	if (rowVector == null)
	  {
	    this.dispose();
	    return;
	  }
	
	System.out.println("rowVector initialized");

	if (debug)
	  {
	    System.err.println("got it, it " + (rowVector == null ? "is " : "isn't ") + "equal to null");
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Caught RemoteException" + ex);
      }
    
    getContentPane().remove(waitPanel);
    getContentPane().setLayout(new BorderLayout());

    /* Initialize table w/ custom table model 
       and custom cell renderers, 
    */
    
    BPermTableModel permModel = new BPermTableModel(rowVector);
    JTable table = new JTable(permModel);

    // Set default column widths
 
    TableColumn column = null;
    for (int i = 0; i < 5; i++) {
      column = table.getColumnModel().getColumn(i);
      if (i == 0) {
	column.setPreferredWidth(180); 
      } else {
	column.setPreferredWidth(15);
      }
    }

    // Uses custom renderers for Strings and Booleans

    table.setDefaultRenderer(String.class,
			     new StringRenderer(rowVector));
    table.setDefaultRenderer(Boolean.class,
			     new BoolRenderer(rowVector));

    table.setShowHorizontalLines(false);


    edit_pane = new JScrollPane(table);
    
    //    edit_pane.setBackground(Color.lightGray);
    getContentPane().add("Center", edit_pane);
    getContentPane().add("South", Choice_Buttons);
    gc.setWaitCursor();
    
    progressDialog.setVisible(false);    
    progressDialog.dispose();
    
    this.myshow(true);
    gc.setNormalCursor();
  }
  
  /**
   * This method will create a vector of row info that will be used
   * to store the permissions values for the base and basefields
   */
  
  private Vector initRowVector() throws RemoteException 
  {
    PermEntry entry, templateEntry;
    BaseDump base;
    FieldTemplate template;
    //    Hashtable results = new Hashtable(); 
    //    Component[] myAry;
    //    PermRow myPermRow;
    Vector rows = new Vector();
    boolean create, view, edit, delete;
    boolean createOK, viewOK, editOK, deleteOK;
    boolean baseCreate, baseView, baseEdit, baseDelete;
    Vector fields;
    boolean visibleField;
    Enumeration enum;
    short id;
    String name;

    /* -- */

    // get a list of base types from the gclient.. we really just care
    // about their names and id's.

    enum = gc.getBaseList().elements();

    progressBar.setMaximum(gc.getBaseList().size() + 3);
    progressBar.setValue(3);

    int i = 3;

    while (enum.hasMoreElements())
      {
	if (! keepLoading)
	  {
	    // Cancel was pushed.
	    stopLoading();
	    return (Vector)null;
	  }

	progressBar.setValue(i++);

	base = (BaseDump) enum.nextElement();

	id = base.getTypeID();
	name = base.getName();

	if (debug)
	  {
	    System.err.println("init_hash: processing " + name);
	  }


	// retrieve the current permissions for this object type 
	
	entry = matrix.getPerm(id);
	
	if (entry == null)
	  {
	    baseCreate = baseView = baseEdit = baseDelete = false;
	  }
	else
	  {
	    baseCreate = entry.isCreatable();
	    baseView = entry.isVisible();
	    baseEdit = entry.isEditable();
	    baseDelete = entry.isDeletable();
	  }

	if (templateMatrix != null)
	  {
	    templateEntry = templateMatrix.getPerm(id);

	    // if permissions already exist on an entry, we'll allow
	    // it to be reduced.. it's conceivable that an admin might
	    // have authority to edit a role that he or she wouldn't
	    // normally have the authority to delegate.. of course,
	    // this wouldn't make a lot of sense since in theory the
	    // admin could just click on the delegatable checkbox and
	    // give him/herself the ability to delegate these perms,
	    // but we might conceivably give an admin permission to
	    // edit this perm field but not the delegatable checkbox
	    // in this object.

	    if (templateEntry == null)
	      {
		createOK = baseCreate;
		viewOK = baseView;
		editOK = baseEdit;
		deleteOK = baseDelete;
	      }
	    else
	      {
		createOK = templateEntry.isCreatable() || baseCreate;
		viewOK = templateEntry.isVisible() || baseView;
		editOK = templateEntry.isEditable() || baseEdit;
		deleteOK = templateEntry.isDeletable() || baseDelete;
	      }
	  }
	else
	  {
	    createOK = viewOK = editOK = deleteOK = true;
	  }


	// Initialize a PermRow object for this base and
	// add it to the rows vector 

	PermRow	basePermRow = new PermRow(base, null, enabled);
	basePermRow.viewOK = viewOK;
	basePermRow.createOK = createOK;
	basePermRow.editOK = editOK;
	basePermRow.deleteOK = deleteOK;
	basePermRow.visible = new Boolean(baseView);
	basePermRow.creatable = new Boolean(baseCreate);
	basePermRow.editable = new Boolean(baseEdit);
	basePermRow.deletable = new Boolean(baseDelete);

	rows.addElement(basePermRow);


	// Now go through the fields

	visibleField = baseView;

	fields = (Vector) gc.getTemplateVector(id);

	for (int j=0; fields != null && (j < fields.size()); j++) 
	  {
	    template = (FieldTemplate) fields.elementAt(j);

	    // we don't want to show built-in fields

	    if (template.isBuiltIn())
	      {
		continue;
	      }

	    // get the permission set for this field

	    entry = matrix.getPerm(id, template.getID());

	    if (entry == null)
	      {
		// if no permission is explicitly recorded for this
		// field, use the record for the base

		create = baseCreate;
		view = baseView;
		edit = baseEdit;
		delete = baseDelete;
	      }
	    else 
	      {
		create = entry.isCreatable();
		view = entry.isVisible();
		edit = entry.isEditable();
		delete = entry.isDeletable();
	      }

	    if (templateMatrix != null)
	      {
		templateEntry = templateMatrix.getPerm(id, template.getID());

		// if permissions already exist on an entry, we'll allow
		// it to be reduced.. it's conceivable that an admin might
		// have authority to edit a role that he or she wouldn't
		// normally have the authority to delegate.. of course,
		// this wouldn't make a lot of sense since in theory the
		// admin could just click on the delegatable checkbox and
		// give him/herself the ability to delegate these perms,
		// but we might conceivably give an admin permission to
		// edit this perm field but not the delegatable checkbox
		// in this object.
		
		if (templateEntry == null)
		  {
		    createOK = create;
		    viewOK = view;
		    editOK = edit;
		    deleteOK = delete;
		  }
		else
		  {
		    createOK = templateEntry.isCreatable() || create;
		    viewOK = templateEntry.isVisible() || view;
		    editOK = templateEntry.isEditable() || edit;
		    deleteOK = templateEntry.isDeletable() || delete;
		  }
	      }
	    else
	      {
		createOK = viewOK = editOK = deleteOK = true;
	      }


	// Initialize a PermRow object for this field and
	// add it to the rows vector 

	    PermRow templatePermRow = new PermRow(base, template, visibleField);
	    templatePermRow.viewOK = viewOK;
	    templatePermRow.createOK = createOK;
	    templatePermRow.editOK = editOK;
	    templatePermRow.deleteOK = deleteOK;
	    templatePermRow.visible = new Boolean(view);
	    templatePermRow.creatable = new Boolean(create);
	    templatePermRow.editable = new Boolean(edit);
	    templatePermRow.deletable = new Boolean(delete);
	    
	    rows.addElement(templatePermRow);
	  }
      }
    
    return rows;
  }

  private void stopLoading()
  {
    if (progressDialog != null)
      {
	progressDialog.setVisible(false);
	progressDialog.dispose();
      }

    this.dispose();
  }

 
 
 
  /**
   *
   * Method to pop-up/pop-down the perm_editor
   *
   */
  
  public void myshow(boolean truth_value)
  {
    if (truth_value)
      {
	setSize(550,550);
      }

    setVisible(truth_value);
  }

  
  public void actionPerformed(ActionEvent e)
  {
    Object ref;
    short baseid;
    BaseDump bd;
    FieldTemplate template;
    boolean view, edit, create, delete;
   
    /* -- */

    /* This method will either simply exit the perm_editor [if "ok" is 
     * the source], or will retore the original permission values [if the
     * "cancel" button is pushed]
     */
    
    if (e.getSource() == OkButton) 
      {
	if (debug)
	  {
	    System.out.println("Ok was pushed");
	  }
            
	myshow(false);
	return;
      } 
    else 
      {
	if (debug)
	  {
	    System.out.println("Cancel was pushed");
	  }

	myshow(false);
	return;
      }    
  }
   
}

/*
  BPermTableModel

  This class describes and implements the table model for the permissions editor.
*/

class BPermTableModel extends AbstractTableModel {

  Vector rows; 
  String[] columnNames = {"Name", 
			    "Visible",
			    "Creatable",
			    "Editable",
			    "Deletable"};

  public BPermTableModel(Vector rowVector) {
    this.rows = rowVector;
  } 
  
  public int getColumnCount() {
    return columnNames.length;
  }
  
  public int getRowCount() {
    return rows.size();
  }
  
  public String getColumnName(int col) {
    return columnNames[col];
  }
  
  public Object getValueAt(int row, int col) {
    PermRow myRow = (PermRow)rows.elementAt(row);
    Object ref = myRow.reference;
    switch(col) {
      
    case 0: // This one is just for indent effect   
      if (ref instanceof BaseDump) {
	BaseDump bd= (BaseDump) ref;
	return " "+bd.getName();
      } 
      else if (ref instanceof FieldTemplate) {
	FieldTemplate ft= (FieldTemplate) ref;
	return "   "+ft.getName();
      }
      
    case 1:
      return myRow.visible;
      
    case 2:
      return myRow.creatable;
      
    case 3:
      return myRow.editable;
      
    case 4:
      return myRow.deletable;
      
    default:
      return new Integer(col);
    }
  }


  public Class getColumnClass(int c) {
    return getValueAt(0, c).getClass();
  }
  
  public boolean isCellEditable(int row, int col) {

      PermRow myRow = (PermRow)rows.elementAt(row);
      if ((!myRow.enabled) || (col < 1)) {
	return false;
      } else {
	
	switch(col) {
	  
	case 1:
	  if (!myRow.viewOK) 
	    return false;
	  else 
	    return true;

	case 2:
	  if (!myRow.createOK) 
	    return false;
	  else 
	    return true;

	  
	case 3:
	  if (!myRow.editOK) 
	    return false;
	  else 
	    return true;

	  
	case 4:
	  if (!myRow.deleteOK) 
	    return false;
	  else 
	    return true;
 
	}
	return true;
      }
  }
  
  public void setValueAt(Object value, int row, int col) {
    if (col > 0) {
      PermRow myRow = (PermRow)rows.elementAt(row);

      switch(col) {
	
      case 1:
	myRow.visible = (Boolean)value;

	// If making a base selection
	// update children too 

	if (myRow.field == null) {
	  setBaseChildren(row, col, value);
	  
	  // Take care of visibility of creatable
	  // and editable children too

	  if (myRow.creatable.booleanValue()) {
	    setBaseChildren(row, col+1, value);
	  }
	  
	  if (myRow.editable.booleanValue()) {
	    setBaseChildren(row, col+2, value);
	  }
	  
	}
	break;
	
      case 2:
	myRow.creatable = (Boolean)value;

	// If base, update children too

	if ((myRow.field == null) && (myRow.visible.booleanValue())) {
	  setBaseChildren(row, col, value);
	}
	break;
	
      case 3:
	myRow.editable = (Boolean)value;

	// If base, update children too

	if ((myRow.field == null) && (myRow.visible.booleanValue())) {
	  setBaseChildren(row, col, value);
	}
	break;
	
      case 4:
	myRow.deletable = (Boolean)value;

	// No update of children for deletable
	break;
	
      default:
	// do nothing;
      }
      fireTableDataChanged();
    }
  }


  /* Programmatically updates status of children when a 
     base is toggled 
  */

  public void setBaseChildren(int row, int col, Object value) {
    PermRow baseRow = (PermRow) rows.elementAt(row);
    
    for (int i = row+1; i<=rows.size(); i++) {
      PermRow myRow = (PermRow) rows.elementAt(i);
      if (myRow.field == null) { // stop updating when we run 
	                         // out of children
	break;

      } else {
	
	switch(col) {
	case 1:
	  if (myRow.viewOK) {
	    myRow.visible = (Boolean)value;
	    
	    myRow.enabled = ((Boolean)value).booleanValue();
	    if (!myRow.enabled) {
	      myRow.visible = new Boolean(false);
	      myRow.creatable = new Boolean(false);
	      myRow.editable = new Boolean(false);
	      myRow.deletable = new Boolean(false);
	    }
	  }
	  break;

	case 2:
	  if (myRow.createOK) {
	    myRow.creatable = (Boolean)value;
	  }
	  break;

	case 3:
	  if (myRow.editOK) {
	    myRow.editable = (Boolean)value;
	  }
	  break;

	case 4:
	  if (myRow.deleteOK){
	    myRow.deletable = (Boolean)value;
	  }
	  break;
	  
	default:
	  // do nothing
	}
      }
    }
  }
}




/* StringRenderer 

   Provides custom renderer for String class.
   Really only used to provide different colored backgrounds
   for bases and fields. 
*/

class StringRenderer extends JLabel
  implements TableCellRenderer {
  
  Vector rows;
  
  public StringRenderer(Vector rows) {
    super();
    this.rows = rows;
    setOpaque(true); //MUST do this for background to show up.
  }
  
  public Component getTableCellRendererComponent(
						 JTable table, Object value, 
						 boolean isSelected, boolean hasFocus,
						 int row, int column) {
    
    PermRow myRow = (PermRow) rows.elementAt(row);
    Object ref = myRow.reference; 

    if (ref instanceof BaseDump) {
      setBackground(Color.white);
    }
    else {
      setBackground(Color.lightGray);
    }
    
    setText((String)table.getValueAt(row,column));
    
    return this;
  }
}


/* BoolRenderer

 Provides custom renderer for Boolean class. 
*/

class BoolRenderer extends JCheckBox
    implements TableCellRenderer {
  
  Vector rows;
  Color lightGray2 = new Color(224,224,224); 
  ImageIcon noAccess = new ImageIcon(PackageResources.getImageResource(this, "noaccess.gif", getClass()));

  public BoolRenderer(Vector rows) {
    super();
    this.rows = rows;
    setOpaque(true); //MUST do this for background to show up.
  }
  
  public Component getTableCellRendererComponent(
						 JTable table, Object value, 
						 boolean isSelected, boolean hasFocus,
						 int row, int column) {
        
    PermRow myRow = (PermRow) rows.elementAt(row);
    Boolean selected = (Boolean)table.getValueAt(row,column);
    boolean enabled = myRow.enabled;
    Object ref = myRow.reference; 


    // Take care of background colors

    if (ref instanceof BaseDump) {
      setBackground(Color.white);
    }
    else {
      setBackground(lightGray2);
    }

    // Check if viewOK, etc. If not, put
    // noAccess icon ("X") instead of checkbox
    
    switch(column) {
    
    case 1:
      if (myRow.viewOK) {
	setIcon(null);
	setEnabled(enabled);
	setSelected(selected.booleanValue());

      } else {
	setIcon(noAccess);
	setEnabled(true);
      }
      break;
      
    case 2:
      if (myRow.createOK) {
	setIcon(null);
	setEnabled(enabled);
	setSelected(selected.booleanValue());

      } else {
	setIcon(noAccess);
	setEnabled(true);
      }
      break;
      
    case 3:
      if (myRow.editOK) {
	setIcon(null);
	setEnabled(enabled);
	setSelected(selected.booleanValue());
	
      } else {
	setIcon(noAccess);
	setEnabled(true);
      }
      break;

    case 4:
      if (myRow.deleteOK) {
	setIcon(null);
	setEnabled(enabled);
	setSelected(selected.booleanValue());
	
      } else {
	setIcon(noAccess);
	setEnabled(true);
      }
      break;
      
    }

    

    // Center checkbox

    setHorizontalAlignment(CENTER);   

    return this;
  }
}


class PermRow {
  Object reference;
  BaseDump base;
  FieldTemplate field;
  Boolean creatable, visible, editable, deletable;
  boolean changed;
  boolean createOK, viewOK, editOK, deleteOK;
  boolean enabled;

  public PermRow(BaseDump base, FieldTemplate field, boolean enabled) {
    if (field == null) {   
      this.reference = base;
    } else {
      this.reference = field;
    }
    this.enabled = enabled;
    this.base = base;
    this.field = field;
  }
}
