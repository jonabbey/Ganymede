/*

  tableCanvas.java

  A JDK 1.1 table AWT component.

  Copyright (C) 1997, 1998, 1999  The University of Texas at Austin.

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
  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

  Created: 15 May 1999
  Version: $Revision: 1.1 $
  Last Mod Date: $Date: 1999/01/16 01:38:24 $
  Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JTable;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     tableCanvas

------------------------------------------------------------------------------*/

/**
 *
 * This class is the actual pane that is rendered on to create the table.  The
 * tableCanvas is double buffered, and optimized for speed.
 *
 */

class tableCanvas extends JComponent implements MouseListener, MouseMotionListener {

  static final boolean debug = false;
  static final int colgrab = 4;
  static final int mincolwidth = 20;

  /* - */

  baseTable rt;
  Image backing;
  Rectangle backing_rect;
  Graphics bg;

  int 
    hbar_old = 0, 
    vbar_old = 0,
    colDrag = 0,
    dragRowSave = -1,
    dragRowSaveY = 0,
    colXOR = -1,
    v_offset = 0,		// y value of the topmost displayed pixel
    h_offset = 0,		// x value of the leftmost displayed pixel
    oldClickCol = -1,
    oldClickRow = -1; 

  long
    lastClick = 0;

  boolean dragCursor = false;

  /* -- */

  // -------------------- Constructors --------------------

  public tableCanvas(baseTable rt)
  {
    this.rt = rt;
    addMouseListener(this);
    addMouseMotionListener(this);

    setDoubleBuffered(false);		// we do the buffering ourselves currently.
  }

  // -------------------- Access Methods --------------------

  /**
   *
   * Copy the backing image into the canvas
   *
   */

  public synchronized void paint(Graphics g) 
  {
    if (debug)
      {
	System.err.println("tableCanvas: paint called");
      }

    if ((backing == null) ||
	(backing_rect.width != getBounds().width) ||
	(backing_rect.height != getBounds().height) ||
	(hbar_old != rt.hbar.getValue()) ||
	(vbar_old != rt.vbar.getValue()))
      {
	render();
      }

    if (debug)
      { 
	System.err.println("tableCanvas: copying image");
      }

    g.drawImage(backing, 0, 0, this);

    if (debug)
      {
	System.err.println("tableCanvas: image copied");
      }
  }

  /**
   *
   * Scheduled for us by repaint()
   *
   */

  public void update(Graphics g)
  {
    if (debug)
      {
	System.err.println("update called");
      }

    // don't blank out the background, just overwrite it

    if (backing != null)
      {
	paint(g);
      }
  }

  /* ----------------------------------------------------------------------

     Ok, we need to draw our table into the backing store.

     The rendering algorithm uses a single large backing store image, with
     each cell drawn directly onto the backing store using the 1.1 setClip()
     routine.
     
     For now we're just going to center our strings within each
     header.  Later on maybe we'll make this modifiable.

     ---------------------------------------------------------------------- */

  synchronized void render()
  {
    int strwidth;
    int just;
    int xpos;
    int leftedge;
    int ypos;
    int ypos2;
    int bottomedge;

    /**
     * The lower bound of the columns visible on the screen.. i.e., index of
     * the first column at least partially visible.
     */

    int first_col;

    /**
     * The upper bound of the columns visible on the screen.. i.e., index of
     * the last column at least partially visible.
     */

    int last_col;

    /**
     * The lower bound of the rows visible on the screen.. i.e., index of
     * the first row at least partially visible.
     */

    int first_row;

    /**
     * The last row visible on the screen, whether rendered or not (if
     * vertFill).
     */

    int last_row;

    /**
     * The upper bound of the rows visible on the screen.. i.e., index of
     * the last row at least partially visible.
     */

    int last_visible_row;

    tableCell
      cell = null;		// make compiler happy

    String
      cellText = null;

    Rectangle
      cellRect = new Rectangle();

    tableRow
      tr = null;

    int
      tempI;

    /* -- */
    
    /* ------------------------------------------------------------------------

       General algorithm: reallocate/initialize our backing store if our size
       has changed, draw cells, draw headers, draw lines.  Note that we don't
       perform an explicit backing store erase step.  We depend on the cell
       rendering algorithm to clear the table.

       ------------------------------------------------------------------------ */
       
    if (debug)
      {
	System.err.println("render called");
      }

    // prep our backing image

    if (backing == null) 
      {
	if (debug)
	  {
	    System.err.println("creating backing image");

	    System.err.println("width = " + getBounds().width);
	    System.err.println("height = " + getBounds().height);
	  }

	backing_rect = new Rectangle(0, 0, getBounds().width, 
				     getBounds().height);

	int width = getBounds().width;
	int height = getBounds().height;

	if (debug)
	  {
	    System.err.println("Trying to create image of size " + width + " x " + height);
	  }

	// In Swing 1.0.2, we get an exception thrown here when we are
	// called prematurely.. before, we could handle this by just
	// checking for a null backing, but in 1.1.6/1.0.2 we get an
	// exception.  Thus, the catch here.

	try
	  {
	    backing = createImage(width, height);
	  }
	catch (IllegalArgumentException ex)
	  {
	    // do nothing
	  }

	if (backing == null)
	  {
	    if (debug)
	      {
		System.err.println("baseTable.render(): couldn't create backing store, returning");
	      }
	    return;
	  }
	bg = backing.getGraphics();
      }
    else if ((backing_rect.width != getBounds().width) ||
	     (backing_rect.height != getBounds().height))
      {
	// need to get a new backing image

	if (debug)
	  {
	    System.err.println("creating new backing image");
	  }

	backing_rect = new Rectangle(0, 0, getBounds().width, getBounds().height);

	backing.flush();	// free old image resources
	backing = createImage(getBounds().width, getBounds().height);
	bg = backing.getGraphics();
      }
    else if ((hbar_old != rt.hbar.getValue()) ||
	     (vbar_old != rt.vbar.getValue()))
      {
	if (debug)
	  {
	    System.err.println("rendering new scroll pos");
	  }

	// paint() uses hbar_old and vbar_old to decide
	// whether it needs to call render

	hbar_old = rt.hbar.getValue();
	vbar_old = rt.vbar.getValue();
      }

    /* Calculate horizontal offset, rendering parameters */

    if (rt.hbar_visible)
      {
	h_offset = rt.hbar.getValue(); // this may not work on win32?

	if (debug)
	  {
	    System.err.println("h_offset = " + h_offset);
	    System.err.println("maximum = " + rt.hbar.getMaximum());
	    System.err.println("calculated right edge = " + (h_offset + getBounds().width));
	    System.err.println("canvas width = " + getBounds().width);
	    System.err.println("position of right pole = " + rt.colPos.elementAt(rt.cols.size()));
	  }

	/* calculate first col visible */

	first_col = 0;	
	xpos =  rt.vLineThickness + ((tableCol) rt.cols.elementAt(first_col)).width;

	while (xpos < h_offset)
	  {
	    xpos += rt.vLineThickness + ((tableCol) rt.cols.elementAt(++first_col)).width;
	  }

	/* calculate last col visible */

	last_col = first_col;
	leftedge = getBounds().width + h_offset;
	
	while ((xpos < leftedge) && (last_col < rt.cols.size() - 1))
	  {
	    xpos += rt.vLineThickness + ((tableCol) rt.cols.elementAt(++last_col)).width;
	  }
      }
    else
      {
	h_offset = 0;
	first_col = 0;
	last_col = rt.cols.size() -1;
      }

    /* Calculate vertical offset, rendering parameters */

    if (rt.vbar_visible)
      {
	// we've got a vertical scrollbar, so we must be filled with
	// visible rows.

	v_offset = rt.vbar.getValue();

	if (debug)
	  {
	    System.err.println("v_offset = " + v_offset);
	    System.err.println("maximum = " + rt.vbar.getMaximum());
	    System.err.println("calculated bottom edge = " + (v_offset + getBounds().height));
	    System.err.println("canvas height = " + getBounds().height);
	  }

	/* what is the first row that we can see?  that is, the first
	   row whose last line is > v_offset.

	   v_offset is the first line of the display area that we will
	   see.. that is, if v_offset = rt.row_height +
	   rt.hRowLineThickness + 1, we will see the second line of the
	   second row as the first line in our scrolling area */

	first_row = 0;
	ypos = 0;

	tr = (tableRow) rt.rows.elementAt(first_row);

	while (tr.getBottomEdge() < v_offset && ++first_row < rt.rows.size())
	  {
	    tr = (tableRow) rt.rows.elementAt(first_row);
	  }

	if (debug)
	  {
	    System.err.println("Calculated first_row as " + first_row);
	  }

	/* what is the last row we can see?  that is, the last row
	   whose first line is < getBounds().height  */

	last_row = first_row;

	bottomedge = v_offset + getBounds().height - 1 - rt.displayRegionFirstLine();

	while (last_row < (rt.rows.size() - 1) &&
	       ((tableRow) rt.rows.elementAt(last_row)).getTopEdge() < bottomedge)
	  {
	    last_row++;
	  }

	last_visible_row = last_row;

	if (debug)
	  {
	    System.err.println("Calculated last_row as " + last_row);
	  }
      }
    else
      {
	// no scroll bar, so everything must be visible

	v_offset = 0;
	first_row = 0;

	last_visible_row = rt.rows.size() - 1;

	if (!rt.vertFill)
	  {
	    last_row = last_visible_row;
	  }
	else
	  {
	    // we'll have varying row sizes for the loaded rows,
	    // followed by a series of rows of single row_height 

	    last_row = rt.rows.size() + 
	      (getBounds().height - rt.displayRegionFirstLine() - rt.calcVSize()) /
	      (rt.row_height + rt.hRowLineThickness);

	    if (debug)
	      {
		System.err.println("Precalc: last_row = " + last_row);
	      }
	  }
      }

    /* ------------------- okay, we've got our general parameters.
                           we can start doing our drawing. ------------------ */
    if (debug)
      {
	System.err.println("Rendering cols: first_col = " + first_col + ", last_col = " + last_col);
      }

    tableCol column = null;
    tableRow row = null;
    int topLine = rt.displayRegionFirstLine();
    int leftEdge;

    for (int j = first_col; j <= last_col; j++)
      {
	column = (tableCol) rt.cols.elementAt(j);

	/* render the column 

	   note that this loop can go past the last row in the
	   rt.rows vector (if rt.vertFill is true).. we do this
	   so that we can extend our columns to the bottom of the
	   display, even if the rows are undefined */

	if (debug)
	  {
	    System.err.println("Rendering rows: first_row = " + first_row + ", last_row = " + last_row);
	  }

	leftEdge = ((Integer) rt.colPos.elementAt(j)).intValue() - h_offset + rt.vLineThickness;

	int i;
	
	if (!rt.vbar_visible)
	  {
	    // we have the table partially filled.. handle it

	    for (i = first_row; i <= last_visible_row; i++)
	      {
		row = (tableRow) rt.rows.elementAt(i);

		topLine = row.getTopEdge() - v_offset;

		cellRect.setBounds(leftEdge, topLine, column.width, row.getHeight() + 1);
		bg.setClip(cellRect);

		renderBlitCell(cellRect, bg, j, i, column);
	      }

	    // we need to blank out the rest of this column.. this will
	    // only occur if we have fewer rows than we have room to
	    // display, and if we have vertical filling turned on.

	    if (debug)
	      {
		System.err.println("Blanking bottom of column " + j);
	      }

	    int blankTop;

	    if (row == null)
	      {
		blankTop = topLine;
	      }
	    else
	      {
		blankTop = topLine + row.getHeight() + 1;
	      }

	    if (debug)
	      {
		System.err.println("Blanking from " + blankTop +
				   " to the bottom of the canvas");
	      }

	    if (column != null && column.attr != null && column.attr.bg != null)
	      {
		bg.setColor(column.attr.bg);
	      }
	    else
	      {
		bg.setColor(rt.tableAttrib.bg);
	      }
	    
	    // open up the clip region enough for us to blank the rest of
	    // this column
	    
	    bg.setClip(leftEdge, blankTop,
		       column.width, getBounds().height - blankTop);
	    
	    // and do it
	    
	    bg.fillRect(leftEdge, blankTop,
			column.width, getBounds().height - blankTop);
	  }
	else
	  {
	    // we have the table fully filled.. render it

	    for (i = first_row; i <= last_row; i++)
	      {
		row = (tableRow) rt.rows.elementAt(i);
		
		topLine = row.getTopEdge() - v_offset;
		
		cellRect.setBounds(leftEdge, topLine, column.width, row.getHeight() + 1);
		bg.setClip(cellRect);
		
		renderBlitCell(cellRect, bg, j, i, column);
	      }
	  }

	// and now render the header for this column.  We do this last so that
	// we will overwrite whatever portion of the first row extends above
	// the displayRegionFirstLine() demarc.
	
	cellRect.setBounds(leftEdge, 0, column.width, rt.headerAttrib.height + 1);
	bg.setClip(cellRect);

	bg.setFont(rt.headerAttrib.font);

	bg.setColor(rt.headerAttrib.bg);
	bg.fillRect(cellRect.x, cellRect.y, cellRect.width, cellRect.height);

	if (column.header != null)
	  {
	    bg.setColor(rt.headerAttrib.fg);
	    
	    strwidth = rt.headerAttrib.fontMetric.stringWidth(column.header);
	    bg.drawString(column.header, cellRect.x + cellRect.width / 2 - (strwidth/2), 
			  cellRect.y + rt.headerAttrib.baseline);
	  }
      }
	
    // Draw lines ------------------------------------------------------------

    // max out our clip so we can draw the lines

    bg.setClip(0,0, getBounds().width, getBounds().height);

    // draw horizontal lines

    bg.setColor(rt.hHeadLineColor);

    bg.drawLine(0, 0, getBounds().width-1, 0); // top line

    bg.setColor(rt.hRowLineColor);

    bg.drawLine(0,
		rt.headerAttrib.height + rt.hHeadLineThickness,
		getBounds().width-1, 
		rt.headerAttrib.height + rt.hHeadLineThickness); // line between header and table

    // draw a line across the bottom of the table

    if (!rt.vbar_visible)
      {
	// very bottom of the canvas

    	ypos = getBounds().height - 1;

	drawHorizLine(ypos);
      }
    else
      {
	// bottom of the last row defined (this makes a difference in fill mode)

	ypos = ((tableRow) rt.rows.lastElement()).getBottomEdge() - v_offset;

	if (ypos <= getBounds().height - 1)
	  {
	    drawHorizLine(ypos);
	  }
      }
    
    // if rt.horizLines is true, draw the horizontal lines
    // in the body of the table

    // if rt.horizLines is false, rt.hRowLineThickness should be
    // 0, and we have no lines to draw

    if (rt.horizLines)
      {
	topLine = rt.displayRegionFirstLine();

	int horizlinePos = topLine;

	for (int i = first_row; i <= last_row; i++)
	  {
	    if (i >= rt.rows.size())
	      {
		if (rt.hVertFill)
		  {
		    horizlinePos += rt.row_height + rt.hRowLineThickness;

		    if (horizlinePos > topLine)
		      {
			drawHorizLine(horizlinePos);
		      }
		  }
	      }
	    else
	      {
		row = (tableRow) rt.rows.elementAt(i);

		horizlinePos = row.getBottomEdge() - v_offset;

		if (horizlinePos > topLine)
		  {
		    drawHorizLine(horizlinePos);
		  }
	      }
	  }
      }

    // draw vertical lines

    if (rt.vertLines)
      {
	for (int j = first_col; j <= last_col + 1; j++)
	  {
	    xpos = ((Integer) rt.colPos.elementAt(j)).intValue() - h_offset;
	    
	    bg.setColor(rt.vHeadLineColor);
	    bg.drawLine(xpos, 0, xpos, rt.displayRegionFirstLine() - 1);
	    bg.setColor(rt.vRowLineColor);
	    bg.drawLine(xpos, rt.displayRegionFirstLine(), xpos, getBounds().height - 1);
	  }
      }

    if (debug)
      {
	System.err.println("Exiting render");
      }
  }

  private final void drawHorizLine(int y)
  {
    bg.drawLine(0, y, getBounds().width - 1, y);
  }

  private void setCellForeColor(Graphics g, tableCell cell, tableCol element)
  {
    if (cell != null)
      {
	if ((cell.attr != null) && (cell.attr.fg != null) && (cell.attr.bg != null))
	  {
	    if (cell.selected)
	      {
		g.setColor(cell.attr.bg);
	      }
	    else
	      {
		g.setColor(cell.attr.fg);
	      }
	  }
	else if ((element.attr != null) &&
		 (element.attr.fg != null) &&
		 (element.attr.bg != null))
	  {
	    if (cell.selected)
	      {
		g.setColor(element.attr.bg);
	      }
	    else
	      {
		g.setColor(element.attr.fg);
	      }
	  }
	else
	  {
	    if (cell.selected)
	      {
		g.setColor(rt.tableAttrib.bg);
	      }
	    else
	      {
		g.setColor(rt.tableAttrib.fg);
	      }
	  }
      }
    else
      {
	if ((element != null) &&
	    (element.attr != null) &&
	    (element.attr.bg != null) &&
	    (element.attr.fg != null))
	  {
	    g.setColor(element.attr.fg);
	  }
	else
	  {
	    g.setColor(rt.tableAttrib.fg);
	  }
      }
  }

  private void setCellBackColor(Graphics g, tableCell cell, tableCol element)
  {
    if (cell != null)
      {
	if ((cell.attr != null) && (cell.attr.fg != null) && (cell.attr.bg != null))
	  {
	    if (cell.selected)
	      {
		g.setColor(cell.attr.fg);
	      }
	    else
	      {
		g.setColor(cell.attr.bg);
	      }
	  }
	else if ((element.attr != null) &&
		 (element.attr.fg != null) &&
		 (element.attr.bg != null))
	  {
	    if (cell.selected)
	      {
		g.setColor(element.attr.fg);
	      }
	    else
	      {
		g.setColor(element.attr.bg);
	      }
	  }
	else
	  {
	    if (cell.selected)
	      {
		g.setColor(rt.tableAttrib.fg);
	      }
	    else
	      {
		g.setColor(rt.tableAttrib.bg);
	      }
	  }
      }
    else
      {
	if ((element != null) &&
	    (element.attr != null) &&
	    (element.attr.bg != null) &&
	    (element.attr.fg != null))
	  {
	    g.setColor(element.attr.bg);
	  }
	else
	  {
	    g.setColor(rt.tableAttrib.bg);
	  }
      }
  }

  /**
   *
   * This method handles rendering into the blit template for
   * column <col>, row <row>, portion <spanSubset>.  That is,
   * if a particular row is more than one standard row_height
   * tall, spanSubset indicates what portion of the row
   * should be rendered into the blitCell.
   *
   *
   */

  private void renderBlitCell(Rectangle cellRect, Graphics g, 
			      int col, int row,
			      tableCol element)
  {
    tableCell cell;

    int 
      baseLine,
      strwidth,
      just;

    String
      renderString;

    /* -- */

    if (debug)
      {
	System.err.println("renderBlitCell: (" + col + "," + row +")");
      }

    if (row < rt.rows.size())
      {
	cell = rt.getCell(col, row);
      }
    else
      {
	cell = null;	// if we are vertically filling
      }
    
    // fill in our background

    setCellBackColor(g, cell, element);	// note this is the canvas' setCBC, not the tables
	    
    g.fillRect(cellRect.x, cellRect.y, cellRect.width, cellRect.height);

    if (cell == null)
      {
	return;
      }

    // render the contents of our cell if it is not empty

    // set our font

    g.setFont(cell.getFont());
    strwidth = cell.getCurrentWidth();
	    
    // set our color

    setCellForeColor(g, cell, element);

    for (int i = 0; i < cell.getRowSpan(); i++)
      {
	renderString = cell.getText(i);
	baseLine = cellRect.y + rt.row_baseline + ((rt.row_height + rt.hRowLineThickness) * i);

	if (renderString != null)
	  {
	    // and draw

	    just = cell.getJust();
		    
	    switch (just)
	      {
	      case tableAttr.JUST_LEFT:
		g.drawString(renderString, cellRect.x + 2, baseLine);
		break;
			
	      case tableAttr.JUST_RIGHT:
		g.drawString(renderString, cellRect.x + cellRect.width - strwidth - 2, baseLine);
		break;
			
	      case tableAttr.JUST_CENTER:
		g.drawString(renderString, cellRect.x + cellRect.width / 2 - (strwidth/2), baseLine);
		break;
	      }
	  }
      }
  }

  //
  // Our MouseListener Support
  //

  public void mouseClicked(MouseEvent e)
  {
  }

  public void mouseEntered(MouseEvent e)
  {
  }

  public void mouseExited(MouseEvent e)
  {
  }

  /**
   *
   * This method takes a y coordinate in virtual table space
   * (i.e., after vertical scrollbar transform) and returns
   * the index for the row containing that point.
   *
   */
  
  int mapClickToRow(int vy)
  {
    int 
      base,
      row,
      rowHeight;

    tableRow
      tr;

    /* -- */
    
    base = rt.displayRegionFirstLine();
    
    row = 0;

    for (row = 0; row < rt.rows.size() && base < vy; row++)
      {
	tr = (tableRow) rt.rows.elementAt(row);
	rowHeight = tr.getRowSpan() * (rt.row_height + rt.hRowLineThickness);

	base += rowHeight;
      }

    if (base >= vy)
      {
	return row - 1;
      }
    else
      {
	// we're out of range.. return something silly

	return -1;
      }
  }

  // initiate column dragging and/or select row

  public synchronized void mousePressed(MouseEvent e)
  {
    int
      x, y,
      vx, vy,
      clickRow,
      clickCol,
      col;

    /* -- */

    x = e.getX();
    y = e.getY();

    if (rt.hbar_visible)
      {
	vx = x + h_offset;	// adjust for horizontal scrolling
      }
    else
      {
	vx = x;
      }

    if (rt.vbar_visible)
      {
	vy = y + v_offset;	// adjust for vertical scrolling
      }
    else
      {
	vy = y;
      }

    if (debug)
      {
	System.err.println("tableCanvas: mouseDown x = " + x + ", y = " + y);
      }

    colDrag = 0;
    clickCol = -1;

    // We don't want a popupTrigger event to initialize a column drag

    if (!e.isPopupTrigger())
      {
	// mouse down near column line?

	for (col = 1; col < rt.cols.size(); col++)
	  {
	    int colLoc = ((Integer) rt.colPos.elementAt(col)).intValue();

	    // nice wide grab range
	
	    if ((vx > colLoc - colgrab) &&
		(vx < colLoc + colgrab))
	      {
		if (debug)
		  {
		    System.err.println("column " + col);
		  }

		// we've clicked close to an interior
		// pole.. we'll drag it

		colDrag = col;
		dragRowSave = mapClickToRow(vy);

		if (dragRowSave != -1)
		  {
		    dragRowSaveY = ((tableRow) rt.rows.elementAt(dragRowSave)).getTopEdge() - v_offset;

		    if (debug)
		      {
			System.err.println("Remembering drag row.. row: " + dragRowSave +
					   ", topEdge " + dragRowSaveY);
		      }
		  }
	      }
	    else if ((vx >= (colLoc + colgrab)) &&
		     (vx <= (((Integer) rt.colPos.elementAt(col+1)).intValue() - colgrab)))
	      {
		clickCol = col;
	      }
	  }

	// note that the above loop is mostly intended to handle the
	// column adjustment.. since the far left column is not adjustable,
	// the above loop misses it.  Check for it here.

	if (clickCol == -1)
	  {
	    if ((vx >= ((Integer) rt.colPos.elementAt(0)).intValue()) &&
		(vx <= (((Integer) rt.colPos.elementAt(1)).intValue() - colgrab)))
	      {
		clickCol = 0;
	      }
	  }
      }

    if (clickCol != -1)
      {
	// not a column drag.. row chosen?

	if (y > rt.displayRegionFirstLine())
	  {
	    clickRow = mapClickToRow(vy);

	    // if the user clicked below the last defined row, unselect
	    // anything selected and return.

	    if (clickRow == -1)
	      {
		rt.unSelectAll();
		return;
	      }

	    if ((clickRow == oldClickRow) && (clickCol == oldClickCol))
	      {
		if (e.getWhen() - lastClick < 500)
		  {
		    rt.doubleClickInCell(clickCol, clickRow);
		  }
		else
		  {
		    rt.clickInCell(clickCol, clickRow);
		  }
	      }
	    else
	      {
		rt.clickInCell(clickCol, clickRow);
	      }

	    oldClickRow = clickRow;
	    oldClickCol = clickCol;
	    lastClick = e.getWhen();
	  }
      }
    else
      {
	setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR));
	dragCursor = true;
      }

    if (e.isPopupTrigger())
      {
	popupHandler(e);
      }
  }

  // finish column dragging

  public synchronized void mouseReleased(MouseEvent e)
  {
    int
      x, y;

    float 
      x1;

    /* -- */

    x = e.getX();
    y = e.getY();

    if (debug)
      {
	System.err.println("tableCanvas: mouseUp x = " + x + ", y = " + y);
      }

    if (rt.hbar_visible)
      {
	x = x + h_offset;
      }

    // for Win32

    if (e.isPopupTrigger())
      {
	popupHandler(e);
	return;
      }

    if (colDrag != 0)
      {
	if (dragCursor)
	  {
	    setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
	    dragCursor = false;
	  }

	if (debug)
	  {
	    System.err.println("placing column " + colDrag);
	    
	    System.err.println("x = " + x);
	    System.err.println("colPos["+(colDrag-1)+"] = " + rt.colPos.elementAt(colDrag-1));
	    System.err.println("colPos["+(colDrag)+"] = " + rt.colPos.elementAt(colDrag));
	  }

	// if we have gone past the edge of our valid column adjust region,
	// restrict it to a valid range

	int prior = ((Integer) rt.colPos.elementAt(colDrag-1)).intValue();
	int next = ((Integer) rt.colPos.elementAt(colDrag+1)).intValue();

	if (x <= prior + mincolwidth)
	  {
	    x = prior + mincolwidth + 1;
	  }

	if (x >= next - mincolwidth)
	  {
	    x = next - mincolwidth - 1;
	  }

	if (debug)
	  {
	    System.err.println("Adjusting column " + colDrag);
	  }

	rt.colPos.setElementAt(new Integer(x), colDrag);

	if (rt.hbar_visible)
	  {
	    // we are not scaled

	    tableCol priorCol, thisCol;

	    priorCol = (tableCol) rt.cols.elementAt(colDrag-1);
	    thisCol = (tableCol) rt.cols.elementAt(colDrag);

	    if (debug)
	      {
		System.err.println("Adjusting..OLD cols["+(colDrag-1)+"].origWidth = " +
				   priorCol.origWidth + ", cols["+colDrag+"].origWidth = " +
				   thisCol.origWidth);
	      }

	    priorCol.origWidth = x - prior - rt.vLineThickness;
	    thisCol.origWidth = next - x - rt.vLineThickness;

	    if (debug)
	      {
		System.err.println("Adjusting..NEW cols["+(colDrag-1)+"].origWidth = " +
				   priorCol.origWidth + ", cols["+colDrag+"].origWidth = " +
				   thisCol.origWidth);
	      }
	  }
	else
	  {
	    // we are probably scaled

	    tableCol priorCol, thisCol;

	    priorCol = (tableCol) rt.cols.elementAt(colDrag-1);
	    thisCol = (tableCol) rt.cols.elementAt(colDrag);

	    if (debug)
	      {
		System.err.println("Scaling and adjusting..OLD cols["+(colDrag-1)+"].origWidth = " +
				   priorCol.origWidth + ", cols["+colDrag+"].origWidth = " +
				   thisCol.origWidth);
		System.err.println("scalefact = " + rt.scalefact);
	      }

	    x1 = priorCol.origWidth;
	    priorCol.origWidth = ((x - prior - rt.vLineThickness) / rt.scalefact);
	    thisCol.origWidth += x1 - priorCol.origWidth;

	    if (debug)
	      {
		System.err.println("Scaling and adjusting..NEW cols["+(colDrag-1)+"].origWidth = " +
				   priorCol.origWidth + ", cols["+colDrag+"].origWidth = " +
				   thisCol.origWidth);		
	      }
	  }

	rt.calcCols();	// update colPos[] based on origColWidths[]

	// we want to wrap the columns fore and aft.

	tableRow tr;
	tableCell cl;
	tableCol 
	  priorCol = (tableCol) rt.cols.elementAt(colDrag-1),
	  thisCol = (tableCol) rt.cols.elementAt(colDrag);

	for (int i = 0; i < rt.rows.size(); i++)
	  {
	    tr = (tableRow) rt.rows.elementAt(i);
	    cl = (tableCell) tr.elementAt(colDrag - 1);
	    cl.wrap(priorCol.width);
	    cl = (tableCell) tr.elementAt(colDrag);
	    cl.wrap(thisCol.width);
	  }

	rt.reCalcRowPos(0);

	// now scroll the table so that the row the user
	// grabbed the bar in will have it's top edge
	// positioned correctly.

	if (dragRowSave != -1)
	  {
	    rt.scrollRowTo(dragRowSave, dragRowSaveY);
	  }

	render();
	repaint();
      }
    else
      {
	// even if we couldn't recalc the column due to a minimum
	// column width violation, we still need to erase our
	// XOR line

	if (colXOR != -1)
	  {
	    bg.setXORMode(Color.red); // needs to be settable
	    bg.drawLine(colXOR, 0, colXOR, getBounds().height-1);
	    bg.setPaintMode();
	    update(this.getGraphics());
	  }
      }

    colDrag = 0;
    colXOR = -1;
    
    return;
  }

  // private popupmenu handler

  private void popupHandler(MouseEvent e)
  {
    int
      x, y,
      vx, vy,
      clickRow,
      clickCol;

    tableCell
      cell;

    tableCol
      col;

    /* -- */

    if (dragCursor)
      {
	setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
	dragCursor = false;
      }

    x = e.getX();
    y = e.getY();

    if (rt.hbar_visible)
      {
	vx = x + h_offset;	// adjust for horizontal scrolling
      }
    else
      {
	vx = x;
      }

    if (rt.vbar_visible)
      {
	vy = y + v_offset;	// adjust for vertical scrolling
      }
    else
      {
	vy = y;
      }

    if (debug)
      {
	System.err.println("baseTable: popupHandler(" + x + "," + y + ")");
      }

    // What column were we triggered on?

    clickCol = -1;

    for (int i = 0; i < rt.cols.size(); i++)
      {
	// nice wide grab range

	if ((vx >= (((Integer) rt.colPos.elementAt(i)).intValue())) &&
	    (vx <= (((Integer) rt.colPos.elementAt(i+1)).intValue())))
	  {
	    clickCol = i;
	  }
      }

    if (clickCol == -1)
      {
	return;
      }

    if (debug)
      {
	System.err.println("baseTable: popupHandler(): column = " + clickCol);
      }

    // What row were we triggered on?

    if (y > rt.displayRegionFirstLine())
      {
	clickRow = mapClickToRow(vy);
	
	// if the user clicked below the last defined row, ignore it
	
	if (clickRow == -1)
	  {
	    return;
	  }
      }
    else
      {
	// we've got a header column..

	if (debug)
	  {
	    System.err.println("baseTable: popupHandler(): header row");
	  }

	clickRow = -1;
	rt.menuRow = -1;
	rt.menuCol = clickCol;

	if (rt.headerMenu != null)
	  {
	    rt.headerMenu.show(this,x,y);
	    return;
	  }
	else
	  {
	    return;
	  }

      }

    // remember what row and column we launched the popup from so 
    // rowTable, etc., can report the row/col in its callback

    rt.menuRow = clickRow;
    rt.menuCol = clickCol;

    rt.clickInCell(clickCol, clickRow);

    if (debug)
      {
	System.err.println("Base table: menuRow = " + rt.menuRow + ", menuCol = " + rt.menuCol);
      }

    cell = ((tableRow) rt.rows.elementAt(clickRow)).elementAt(clickCol);

    col = (tableCol) rt.cols.elementAt(clickCol);

    if (col.menu != null)
      {
	if (debug)
	  {
	    System.err.println("Showing column menu");
	  }
	col.menu.show(this, x, y);
	return;
      }

    if (rt.menu != null)
      {
	if (debug)
	  {
	    System.err.println("Showing topLevel menu");
	  }
	rt.menu.show(this,x,y);
	return;
      }
    
  }

  // 
  // MouseMotionListener methods
  //

  public void mouseMoved(MouseEvent e)
  {
    int
      x,vx,
      column;

    // set the cursor?

    x = e.getX();

    if (rt.hbar_visible)
      {
	vx = x + h_offset;	// adjust for horizontal scrolling
      }
    else
      {
	vx = x;
      }

    column = -1;

    // mouse down near column line?

    for (int col = 1; column == -1 && col < rt.cols.size(); col++)
      {
	int colLoc = ((Integer) rt.colPos.elementAt(col)).intValue();

	// nice wide grab range
	
	if ((vx > colLoc - colgrab) &&
	    (vx < colLoc + colgrab))
	  {
	    column = col;
	  }
      }

    if (column == -1)
      {	
	// we'll only reset the cursor if we're not dragging and the
	// cursor is not already reset.

	if (colDrag == 0 && dragCursor)
	  {
	    setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
	    dragCursor = false;
	  }
      }
    else
      {
	if (!dragCursor)
	  {
	    setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR));
	    dragCursor = true;
	  }
      }
  }

  // perform column dragging

  public synchronized void mouseDragged(MouseEvent e)
  {
    int 
      x, y;

    /* -- */

    x = e.getX();
    y = e.getY();
    
    if (rt.hbar_visible)
      {
	x = x + h_offset;		// adjust for horizontal scrolling
      }

    if (colDrag != 0)
      {
	if (debug)
	  {
	    System.err.println("colDragging column " + colDrag);
	    System.err.println("x = " + x);
	    System.err.println("colPos["+(colDrag-1)+"] = " + 
			       ((Integer) rt.colPos.elementAt(colDrag-1)).intValue());

	    System.err.println("colPos["+(colDrag)+"] = " + 
			       ((Integer) rt.colPos.elementAt(colDrag)).intValue());
	  }

	if ((x > (((Integer)rt.colPos.elementAt(colDrag-1)).intValue() + mincolwidth + 1)) && 
	    (x < ((Integer)rt.colPos.elementAt(colDrag+1)).intValue() - mincolwidth - 1))
	  {
	    bg.setXORMode(Color.red); // needs to be settable

	    // erase the old line if we had one

	    if (colXOR != -1)
	      {
		bg.drawLine(colXOR, 0, colXOR, getBounds().height-1);
	      }

	    // XOR in our new line, remember where it is with colXOR
	    // so that we can undraw it later.

	    colXOR = x - h_offset;

	    bg.drawLine(colXOR, 0, colXOR, getBounds().height-1);

	    bg.setPaintMode();

	    update(this.getGraphics());
	  }
      }
  }

  // component overrides

  public Dimension getPreferredSize()
  {
    if (rt.rows.size() < 5)
      {
	return new Dimension(rt.origTotalWidth + (rt.cols.size() + 1) * rt.vLineThickness,
			     rt.displayRegionFirstLine() +
			     5 * (rt.row_height + rt.hRowLineThickness));
      }
    else if (rt.rowsToShow != -1)
      {
	return new Dimension(rt.origTotalWidth + (rt.cols.size() + 1) * rt.vLineThickness,
			     rt.displayRegionFirstLine() +
			     rt.rowsToShow * (rt.row_height + rt.hRowLineThickness));
      }
    else
      {
	return new Dimension(rt.origTotalWidth + (rt.cols.size() + 1) * rt.vLineThickness,
			     rt.displayRegionFirstLine() +
			     rt.rows.size() * (rt.row_height + rt.hRowLineThickness));
      }
  }

  public Dimension getMinimumSize()
  {
    return new Dimension(rt.origTotalWidth,
			 rt.displayRegionFirstLine() +
			 2 * (rt.row_height + rt.hRowLineThickness));
  }
}
