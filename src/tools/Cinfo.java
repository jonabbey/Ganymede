/*
   
   Cinfo.java

   Tool to generate proxy classes for new client/server 
   interaction.

   Created: 9 August 2000
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 2000/08/09 18:47:50 $
   Module By: Robbie Sternenberg, robbie@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

import java.lang.reflect.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Cinfo

------------------------------------------------------------------------------*/

public class Cinfo
{
	static Hashtable mapping = new Hashtable();
	static
	{
		mapping.put("int","Integer");
		mapping.put("float","Float");
		mapping.put("short","Short");
		mapping.put("double","Double");
		mapping.put("byte","Byte");
		mapping.put("boolean","Boolean");
		mapping.put("char","Char");
	}

	static String tName(String nm, Hashtable ht)
	{
		String yy;
		String arr;

		if (nm.charAt(0) != '[')
		{
			int i = nm.lastIndexOf(".");
			if (i == -1)
			{
				return nm; // It's a primitive type, ignore it.
			}
			else
			{
				yy = nm.substring(i+1);
			}
			if (ht != null)
			{
				ht.put(nm, yy); // note class types in the hashtable.
			}
			return yy;
		}
		arr = "[]";
		if (nm.charAt(1) == '[')
		{
			yy = tName(nm.substring(1), ht);
		}
		else
		{
			switch (nm.charAt(1))
			{
				case 'L' :
						yy = tName(nm.substring(nm.indexOf("L")+1, nm.indexOf(";")), ht);
						break;
				case 'I':
						yy = "int";
						break;
				case 'V':
						yy = "void";
						break;
				case 'C':
						yy = "char";
						break;
				case 'D':
						yy = "double";
						break;
				case 'F':
						yy = "float";
						break;
				case 'J':
						yy = "long";
						break;
				case 'S':
						yy = "short";
						break;
				case 'Z':
						yy = "boolean";
						break;
				case 'B':
						yy = "byte";
						break;
				default:
						yy = "BOGUS:"+nm;
						break;
	
				}
		}
		return yy+arr;
	}
	

	public static void main(String args[])
	{
		Constructor   cn[];
		Class         cc[];
		Method        mm[];
		Field         ff[];
		Class         c = null;
		Class         supClass;
		String      x, y, s1, s2, s3;
		Hashtable classRef = new Hashtable();
		if (args.length == 0)
		{
			System.out.println("Please specify a class name on the command line.");
			System.exit(1);
		}
		try
		{
			c = Class.forName(args[0]);
		}catch (ClassNotFoundException ee){
		System.out.println("Couldn't find class '"+args[0]+"'");
		System.exit(1);
		}

        /*
         * Step 0: If our name contains dots we're in a package so put
         * that out first.
         */
		x = c.getName();
		if (x.lastIndexOf(".") != -1)
		{
			y = x.substring(0, x.lastIndexOf("."));
			System.out.println("package "+y+";\n\r");
		}

        /*
         * Let's use the Reflection API to sift through what is
         * inside this class.
         *
         * Step 1: Collect referenced classes
         * This step is used so that I can regenerate the import statements.
         * It isn't strictly required of course, Java works just fine with
         * fully qualified object class names, but it looks better when you
         * use 'String' rather than 'java.lang.String' as the return type.
         */

		ff = c.getDeclaredFields();
		for (int i = 0; i < ff.length; i++)
		{
			x = tName(ff[i].getType().getName(), classRef);
		}

		cn = c.getDeclaredConstructors();
		for (int i = 0; i < cn.length; i++)
		{
			Class cx[] = cn[i].getParameterTypes();
			if (cx.length > 0)
			{
				for (int j = 0; j < cx.length; j++)
				{
					x = tName(cx[j].getName(), classRef);
				}
			}
		}

		mm = c.getDeclaredMethods();
		for (int i = 0; i < mm.length; i++)
		{
			x = tName(mm[i].getReturnType().getName(), classRef);
			Class cx[] = mm[i].getParameterTypes();
			if (cx.length > 0)
			{
				for (int j = 0; j < cx.length; j++)
				{
					x = tName(cx[j].getName(), classRef);
				}
			}
		}

        // Don't import ourselves ...
		classRef.remove(c.getName());

        /*
         * Step 2: Start class description generation, start by printing
         *  out the import statements.
         *
         * This is the line that goes 'public SomeClass extends Foo {'
         */
		for (Enumeration e = classRef.keys(); e.hasMoreElements(); )
		{
			System.out.println("import "+e.nextElement()+";");
		}
		System.out.println();

        /*
         * Step 3: Print the class or interface introducer. We use
         * a convienience method in Modifer to print the whole string.
         */
		int mod = c.getModifiers();

		System.out.print("public class ");

		System.out.print(tName(c.getName(), null) + "Remote implements " + c.getName() + ", java.io.Serializable");

		supClass = c.getSuperclass();
		if (supClass != null)
		{
			System.out.print(" extends "+tName(supClass.getName(), classRef));
		}
		System.out.println("\n{");

        /*
         * Step 4: Print out the fields (internal class members) that are declared
         * by this class.
         *
         * Fields are of the form [Modifiers] [Type] [Name] ;
         */

		System.out.println("\n/*\n * Field Definitions.\n */");
		for (int i = 0; i < ff.length; i++)
		{
			Class ctmp = ff[i].getType();
			int md = ff[i].getModifiers();
			System.out.println("\t"+Modifier.toString(md)+" "+
                    tName(ff[i].getType().getName(), null) +" "+
                    ff[i].getName()+";");
		}
		System.out.println("\t public Invid objID;");
		System.out.println("\t public Session ses;");
		if(c.getName().endsWith("field"))
		{
			System.out.println("\t public short fieldID;");
		}

        /*
         * Step 5: Print out the constructor declarations.
         *
         * We note the name of the class which is the 'name' for all
         * constructors. Also there is no type, so the definition is
         * simplye [Modifiers] ClassName ( [ Parameters ] ) { }
         *
         */
		System.out.println("\n/*\n * Declared Constructors. \n */");
		if(c.getName().endsWith("field"))
		{
			System.out.print("\tpublic " + tName(c.getName(), null) + "Remote (Invid objID, short fieldID, Session ses)\n\t{");
			System.out.print("\n\t\tthis.objID = objID;\n\t\tthis.fieldID = fieldID;\n\t\tthis.ses = ses;\n\t}");
		}
		else
		{
			System.out.print("\tpublic " + tName(c.getName(), null) + "Remote (Invid objID, Session ses)\n\t{");
			System.out.print("\n\t\tthis.objID = objID;\n\t\tthis.ses = ses;\n\t}");
		}	
        /*
         * Step 6: Print out the method declarations.
         *
         * Now methods have a name, a return type, and an optional
         * set of parameters so they are :
         *  [modifiers] [type] [name] ( [optional parameters] ) { }
         */
		System.out.println("\n/*\n * Declared Methods.\n */");
		for (int i = 0; i < mm.length; i++)
		{
			int md = mm[i].getModifiers();
			if (Modifier.isAbstract(md))
			{
				md -= Modifier.ABSTRACT;
			}
			System.out.print("\t"+Modifier.toString(md)+" "+
                    tName(mm[i].getReturnType().getName(), null)+" "+
                    mm[i].getName());
			Class cx[] = mm[i].getParameterTypes();
			System.out.print("( ");
			if (cx.length > 0)
			{
				for (int j = 0; j < cx.length; j++)
				{
					System.out.print(tName(cx[j].getName(), classRef) + " p" + j);
					if (j < (cx.length - 1))
					{
						System.out.print(", ");
					}
				}
			}
			System.out.print(" ) throws RemoteException");
			System.out.println("\n\t{");
			if (cx.length > 0)
			{
				System.out.println("\t\tObject[] params = new Object[" + cx.length + "];");
				for (int j = 0; j < cx.length; j++)
				{
					if(mapping.containsKey(tName(cx[j].getName(), classRef)))
					{
						System.out.println("\t\tparams[" + j + "] = new " + mapping.get(tName(cx[j].getName(), classRef)) + "(p" + j + ");");
					}
					else
					{	
						System.out.println("\t\tparams[" + j + "] = p" + j + ";");
					}
				}
			}
			if(!tName(mm[i].getReturnType().getName(), null).equals("void"))
			{
				if(c.getName().endsWith("field"))
				{
					System.out.print("\t\tObject result = ses.doFieldCall(objID, fieldID, \"" + mm[i].getName() + "\", ");
				}
				else
				{
					System.out.print("\t\tObject result = ses.doObjectCall(objID, \"" + mm[i].getName() + "\", ");
				}
				if(cx.length > 0)
				{
					System.out.println("params);");
				}
				else
				{
					System.out.println("null);");
				}
				if(mapping.containsKey(tName(mm[i].getReturnType().getName(), null)))
				{
					System.out.println("\t\treturn ((" + mapping.get(tName(mm[i].getReturnType().getName(), null)) + ")result)." + tName(mm[i].getReturnType().getName(), null) + "Value();");
				}
				else
				{
					System.out.println("\t\treturn (" + tName(mm[i].getReturnType().getName(), null) + ")result;");
				}
			}
			System.out.println("\t}");
		}
        /*
         * Step 7: Print out the closing brace and we're done!
         */
		System.out.println("}");
	}
}
