/*

   brian_editor.java

   Description.
   
   Created: 18 November 1998
   Version: $Revision: 1.5 $ %D%
   Module By: Brian O'Mara omara@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.rmi.RemoteException;

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
  Hashtable myHash, changeHash;
  Vector rowVector;

  gclient gc;

  // changeHash will store the references to the bases and fields that have been 
  // clicked as the keys to the hash, along with the value "happy" 

  JButton OkButton = new JButton ("Ok");
  JButton CancelButton = new JButton("Cancel");
 
  int row = 0;

  boolean justShowUser = false;

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
		      boolean justShowUser)
  {
    super(parent, DialogTitle, true); // the boolean value is to make the dialog modal
    

    /* -- */
      
    // Main constructor for the perm_editor window
      
    this.session = session;
    this.permField = permField;
    this.enabled = enabled;
    this.gc = gc;
    this.justShowUser = justShowUser;
    
    if (!debug)
      {
	this.debug = gc.debug;
      }

    waitPanel = new JPanel(new BorderLayout(5, 5));
    progressBar = new JProgressBar();
    progressBar.setBorder(gc.emptyBorder10);
    progressBar.setMinimum(0);
    progressBar.setMaximum(29);
    progressBar.setValue(0);
    JPanel progressBarPanel = new JPanel();
    progressBarPanel.add(progressBar);

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
    
    setBackground(Color.white); 

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

    progressBar.setValue(1);



    progressBar.setValue(2);
    
    try 
      {
	//	myHash = initHash();	// construct components
	rowVector = initHash();

	if (rowVector == null)
	  {
	    this.dispose();
	    return;
	  }
	
	changeHash = new Hashtable();
	
	System.out.println("Hash initialized");

	if (debug)
	  {
	    System.err.println("got it, it " + (myHash == null ? "is " : "isn't ") + "equal to null");
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Caught RemoteException" + ex);
      }
    
    getContentPane().remove(waitPanel);
    getContentPane().setLayout(new BorderLayout());
    
    BPermTableModel myModel = new BPermTableModel(rowVector);
    JTable table = new JTable(myModel);
    table.setShowHorizontalLines(false);
    //table.setShowGrid(false);
    table.setDefaultRenderer(String.class,
			     new StringRenderer(rowVector));
    table.setDefaultRenderer(Boolean.class,
			     new BoolRenderer(rowVector));
    TableColumn column = null;
    for (int i = 0; i < 5; i++) {
      column = table.getColumnModel().getColumn(i);
      if (i == 0) {
	column.setPreferredWidth(180); 
      } else {
	column.setPreferredWidth(15);
      }
    }

    edit_pane = new JScrollPane(table);
    
    edit_pane.setBackground(Color.lightGray);
    getContentPane().add("Center", edit_pane);
    getContentPane().add("South", Choice_Buttons);
    gc.setWaitCursor();
    
    progressDialog.setVisible(false);
    
    progressDialog.dispose();
    
    this.myshow(true);
    gc.setNormalCursor();
  }
  
  /**
   * This method will create the hash table that will be used
   * to store the permissions values for the base and basefields
   * and their respective locations on the panel
   */
  
  //private Hashtable initHash() throws RemoteException 
  private Vector initHash() throws RemoteException 
  {
    PermEntry entry, templateEntry;
    BaseDump base;
    FieldTemplate template;
    Hashtable results = new Hashtable(); 
    Component[] myAry;
    PermRow myPermRow;
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
	    //return (Hashtable)null;
	    return (Vector)null;
	  }

	progressBar.setValue(i++);

	base = (BaseDump) enum.nextElement();

	id = base.getTypeID();
	name = base.getName();

	if (justShowUser)
	  {
	    if (id != SchemaConstants.UserBase)
	      {
		continue;
	      }
	  }

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

	myPermRow = new PermRow(base, enabled);
	myPermRow.viewOK = viewOK;
	myPermRow.createOK = createOK;
	myPermRow.editOK = editOK;
	myPermRow.deleteOK = deleteOK;
	myPermRow.visible = new Boolean(baseView);
	myPermRow.creatable = new Boolean(baseCreate);
	myPermRow.editable = new Boolean(baseEdit);
	myPermRow.deletable = new Boolean(baseDelete);

	rows.addElement(myPermRow);

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


	    myPermRow = new PermRow(template, visibleField);
	    myPermRow.viewOK = viewOK;
	    myPermRow.createOK = createOK;
	    myPermRow.editOK = editOK;
	    myPermRow.deleteOK = deleteOK;
	    myPermRow.visible = new Boolean(view);
	    myPermRow.creatable = new Boolean(create);
	    myPermRow.editable = new Boolean(edit);
	    myPermRow.deletable = new Boolean(delete);
	    
	    rows.addElement(myPermRow);
	
	  }
      }
    
    //    return results;
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


class BPermTableModel extends AbstractTableModel {
  Vector rows;

  public BPermTableModel(Vector rowVector) {
    this.rows = rowVector;
  } 

  
  String[] columnNames = {"Name", 
			  "Visible",
			  "Creatable",
			  "Editable",
			  "Deletable"};
  
  
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
  
    case 0:

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
  
  /*
   * Don't need to implement this method unless your table's
   * editable.
   */
  public boolean isCellEditable(int row, int col) {
    //Note that the data/cell address is constant,
    //no matter where the cell appears onscreen.
    if (col < 1) { 
      return false;
    } else {
     PermRow myRow = (PermRow)rows.elementAt(row);
     if (myRow.enabled) {
     return true;
     } else {
       return false;
     }
    }
  }
  
  /*
   * Don't need to implement this method unless your table's
   * data can change.
   */
  public void setValueAt(Object value, int row, int col) {
    if (col > 0) {
      PermRow myRow = (PermRow)rows.elementAt(row);
      switch(col) {
	
      case 1:
	myRow.visible = (Boolean)value;
	break;
	
      case 2:
	myRow.creatable = (Boolean)value;
	break;

      case 3:
	myRow.editable = (Boolean)value;
	break;
	
      case 4:
	myRow.deletable = (Boolean)value;
	break;
	
      default:
	// do nothing;
      }
    }
  }  
}

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

class BoolRenderer extends JCheckBox
    implements TableCellRenderer {
  
  Vector rows;
  
  public BoolRenderer(Vector rows) {
    super();
    this.rows = rows;
    setOpaque(true); //MUST do this for background to show up.
  }
  
  public Component getTableCellRendererComponent(
						 JTable table, Object value, 
						 boolean isSelected, boolean hasFocus,
						 int row, int column) {
    setHorizontalAlignment(CENTER);   
    
    PermRow myRow = (PermRow) rows.elementAt(row);
    boolean enabled = myRow.enabled;
    Object ref = myRow.reference; 
    if (ref instanceof BaseDump) {
      setBackground(Color.white);
    }
    else {
      setBackground(new Color(224,224,224));
    }
    
    switch(column) {
    
    case 1:
      if (!myRow.viewOK) {
	setBackground(Color.blue);
      }
      break;
      
    case 2:
      if (!myRow.createOK) {
	setBackground(Color.blue);
      }
      break;
      
    case 3:
      if (!myRow.editOK) {
	setBackground(Color.blue);
      }
      break;
      
    case 4:
      if (!myRow.deleteOK) {
	setBackground(Color.blue);
      }
      break;
      
    }
    setEnabled(enabled);
    Boolean sel = (Boolean)table.getValueAt(row,column);
    setSelected(sel.booleanValue());
    
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

  public PermRow(Object reference, boolean enabled) {
    this.reference = reference;
    this.enabled = enabled;
  }
}
