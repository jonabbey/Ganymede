/*

   db_field.java

   A db_field is an item in a db_object.  A db_field can be a vector
   or a scalar.  

   Created: 10 April 1996
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 1999/01/22 18:05:57 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.rmi.RemoteException;
import java.util.*;

public interface db_field extends java.rmi.Remote {

  FieldTemplate getFieldTemplate() throws RemoteException;
  FieldInfo getFieldInfo() throws RemoteException;

  String getName() throws RemoteException;
  short getID() throws RemoteException;
  String getComment() throws RemoteException;
  String getTypeDesc() throws RemoteException;
  short getType() throws RemoteException;
  short getDisplayOrder() throws RemoteException;
  String getValueString() throws RemoteException;
  String getEncodingString() throws RemoteException;

  boolean isDefined() throws RemoteException;
  boolean isVector() throws RemoteException;
  boolean isEditable() throws RemoteException;
  boolean isVisible() throws RemoteException;
  boolean isBuiltIn() throws RemoteException;
  boolean isEditInPlace() throws RemoteException;

  // for scalars

  Object getValue() throws RemoteException;
  ReturnVal setValue(Object value) throws RemoteException;

  // for vectors

  int size() throws RemoteException;

  Vector getValues() throws RemoteException;
  Object getElement(int index) throws RemoteException;
  ReturnVal setElement(int index, Object value) throws RemoteException;
  ReturnVal addElement(Object value) throws RemoteException;
  ReturnVal deleteElement(int index) throws RemoteException;
  ReturnVal deleteElement(Object value) throws RemoteException;
  boolean containsElement(Object value) throws RemoteException;
}
