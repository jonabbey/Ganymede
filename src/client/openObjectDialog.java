/* 

   openObjectDialog.java

   A Dialog to open an object from the database for a variety of operations.

   */


package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import com.sun.java.swing.*;
import com.sun.java.swing.border.TitledBorder;
import java.util.Vector;
import java.util.Hashtable;

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.Util.VecQuickSort;

//public class openObjectDialog extends JDialog implements ActionListener, MouseListener, JsetValueCallback{
public class openObjectDialog extends JDialog implements ActionListener, MouseListener{
  private final static boolean debug = true;

  long
    lastClick = 0;

  Invid 
    invid = null;

  GridBagLayout
    gbl;
  
  GridBagConstraints
    gbc;

  gclient
    client;

  JPanel 
    middle;

  JList
    list = null;

  JScrollPane
    pane = null;

  JComboBox
    type;

  JButton
    ok;

  JTextField 
    text;

  listHandle
    lastObject = null,
    currentObject = null;

  JLabel
    titleL;

  String lastValue = null;

  /* -- */

  public openObjectDialog(gclient client)
  {
    super(client, "Open object", true);

    this.client = client;
    
    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    
    getContentPane().setLayout(new BorderLayout());
    middle = new JPanel(gbl);
    getContentPane().add(middle, BorderLayout.CENTER);
    gbc.insets = new Insets(4,4,4,4);
    
    titleL = new JLabel("Choose invid:", SwingConstants.CENTER);
    titleL.setFont(new Font("Helvetica", Font.BOLD, 14));
    titleL.setOpaque(true);
    titleL.setBorder(client.emptyBorder5);
    
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 4;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(titleL, gbc);
    middle.add(titleL);
    
    gbc.fill = GridBagConstraints.NONE;
    
    type = new JComboBox();
    Vector bases = client.getBaseList();
    Hashtable baseNames = client.getBaseNames();

    Base thisBase = null;

    try
      {
	for (int i = 0; i < bases.size(); i++)
	  {
	    thisBase = (Base)bases.elementAt(i);
	    String name = (String)baseNames.get(thisBase);
	    
	    if (name.startsWith("Embedded:"))
	      {
		if (debug)
		  {
		    System.out.println("Skipping embedded field: " + name);
		  }
	      }
	    else
	      {
		listHandle lh = new listHandle(name, new Short(thisBase.getTypeID()));
		type.addItem(lh);
	      }
	  }
      }
    catch (java.rmi.RemoteException rx)
      {
	throw new RuntimeException("Could not get type id: " + rx);
      }
    
    
    gbc.gridx = 0;
    gbc.gridy = 1;

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 2;

    JLabel oType = new JLabel("Object Type:"); 

    gbl.setConstraints(oType, gbc);
    middle.add(oType);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 2;
    gbl.setConstraints(type, gbc);
    middle.add(type);
	
    text = new JTextField(20);
    text.addActionListener(this);
    JLabel editTextL = new JLabel("Object Name:");
    
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbl.setConstraints(editTextL, gbc);
    middle.add(editTextL);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;    
    gbc.gridx = 2;
    gbl.setConstraints(text, gbc);
    middle.add(text);
    
    JPanel buttonP = new JPanel();
    
    ok = new JButton("Ok");
    ok.setActionCommand("Find Object with this name");
    ok.addActionListener(this);
    buttonP.add(ok);


    JButton neverMind = new JButton("Cancel");
    neverMind.setActionCommand("Nevermind finding this object");
    neverMind.addActionListener(this);
    buttonP.add(neverMind);
	
    getContentPane().add(buttonP, BorderLayout.SOUTH);	
        
    setBounds(150,100, 200,100);
  }

  public Invid chooseInvid()
  {
    pack();
    type.requestFocus();
		    
    setVisible(true);

    return invid;
  }

  public void setText(String text)
  {
    titleL.setText(text);
  }

  public String getTypeString()
  {
    listHandle lh = (listHandle)type.getSelectedItem();
    return lh.getLabel();
  }

  public void close(boolean foundOne)
  {
    // Make sure we return null if we didn't find one

    if (!foundOne)
      {
	invid = null;
      }

    setVisible(false);
    
    text.setText("");

    if (list != null)
      {
	if (debug)
	  {
	    System.out.println("Removing the list");
	  }
	pane.remove(list);
      }

    if (pane != null)
      {
	if (debug)
	  {
	    System.out.println("Removing pane");
	  }
	middle.remove(pane);
      }

    if (debug)
      {
	System.out.println("Nulling the pane and list");
      }

    pane = null;
    list = null;
  }

  public void actionPerformed(ActionEvent e)
  {
    if (debug)
      {
	System.out.println("Action performed: " + e.getActionCommand());
      }
    

    if (e.getSource() == text)
      {
	ok.doClick();
      }
    else if (e.getActionCommand().equals("Find Object with this name"))
      {
	String string = text.getText();

	if ((string == null) || (string.equals("")))
	  {
	    client.setStatus("You are going to have to do better than that.");
	    return;
	  }

	if ((currentObject != null) && (string.equals(currentObject.getLabel())))
	  {
	    //This was set from the listbox, and hasn't been changed.
	    
	    invid = (Invid)currentObject.getObject();
	    close(true);
	  }
	else
	  {
	    if (list == null)
	      {
		list = new JList();
		list.setBorder(client.lineBorder);
		list.setModel(new DefaultListModel());
		list.addMouseListener(this);
	      }
	    else
	      {
		((DefaultListModel)list.getModel()).clear();
	      }

	    if (pane == null)
	      {
		pane = new JScrollPane(list);
		pane.setBorder(new TitledBorder("Matching objects"));
	      }

	    client.setStatus("Searching for objects begining with " + string);
	    
	    if (type == null)
	      {
		System.out.println("edit object type = null");
	      }
	    
	    listHandle lh = (listHandle)type.getSelectedItem();
	    Short baseID = (Short)lh.getObject();
	    
	    if (debug)
	      {
		System.out.println("BaseID = " + baseID + ", string = " + string);
	      }
	    
	    QueryDataNode node = new QueryDataNode(QueryDataNode.STARTSWITH, string);  
	    QueryResult edit_query = null;

	    try
	      {
		edit_query = client.session.query(new Query(baseID.shortValue(), node, true));
		
		
		Vector edit_invids = edit_query.getListHandles();
		
		if (edit_invids.size() == 1)
		  {
		    // I don't know if this is enough.  It needs to be added to the changedHash
		    // hash, but i don't know how to find the node right now.  Maybe that node
		    // hasn't even been added yet.  Maybe there should be another hash, of created
		    // nodes, or maybe there already is.
		    //client.wp.addWindow(client.session.edit_db_object( (Invid)((listHandle)edit_invids.elementAt(0)).getObject()) , true);
		    invid = (Invid)((listHandle)edit_invids.elementAt(0)).getObject();
		    close(true);
		  }
		else if (edit_invids.size() == 0)
		  {
		    client.showErrorMessage("Error finding object",
						      "No object starts with that string.");
		    return;
		  }
		else
		  {
		    (new VecQuickSort(edit_invids, 
				      new arlut.csd.Util.Compare() {
					public int compare(Object a, Object b) 
					  {
					    listHandle aF, bF;
					    
					    aF = (listHandle) a;
					    bF = (listHandle) b;
					    int comp = 0;
					    
					    comp =  aF.toString().compareTo(bF.toString());
					    
					    if (comp < 0)
					      {
						return -1;
					      }
					    else if (comp > 0)
					      { 
						return 1;
					      } 
					    else
					      { 
						return 0;
					      }
					  }
		    })).sort();

		       
		    DefaultListModel model = (DefaultListModel)list.getModel();
		    for (int i = 0; i < edit_invids.size(); i++)
		      {
			model.addElement(edit_invids.elementAt(i));
		      }
		    
		    
		    gbc.gridx = 0;
		    gbc.gridy = 3;
		    gbc.gridwidth = GridBagConstraints.REMAINDER;
		    gbc.fill = GridBagConstraints.HORIZONTAL;
		    gbl.setConstraints(pane, gbc);
		    
		    middle.add(pane);
		    pack();
		  }
	      }
	    catch (java.rmi.RemoteException rx)
	      {
		throw new RuntimeException("Could not get query: " + rx);
	      }
	  }
      }
    else if (e.getActionCommand().equals("Nevermind finding this object"))
      {
	close(false);
      }

  }

  public void mouseClicked(MouseEvent e)
  {
    
    currentObject = (listHandle)list.getSelectedValue();
    
    if ((e.getWhen() - lastClick < 500)  && (currentObject == lastObject))
      {
	//client.wp.addWindow(client.session.edit_db_object( (Invid)((listHandle)currentObject).getObject()) , true);
	invid = (Invid)((listHandle)currentObject).getObject();
	close(true);

      }
    else
      {
	text.setText(currentObject.toString());
      }

    lastClick = e.getWhen();
    lastObject = currentObject;

  }

  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}

}
