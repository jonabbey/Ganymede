/*

   PasswordChecker.java

   This class provides password quality strength estimation for
   Ganymede.

   The code in this class was modeled after that from Password Geek,
   licensed under the GPL at http://sourceforge.net/projects/passwordgeek/.

   Created: 15 November 2009

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2009
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

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 PasswordChecker

------------------------------------------------------------------------------*/

/**
 * This class provides password quality strength estimation for Ganymede.
 *
 * The code in this class was modeled after that from Password Geek, licensed
 * under the GPL at http://sourceforge.net/projects/passwordgeek/.
 */

public class PasswordChecker {

  private int n_char;
  private int n_upper;
  private int n_num;
  private int n_sym;
  private int n_mid;
  private int letter_only;
  private int num_only;
  private int rep_char;
  private int consecutive_upper;
  private int consecutive_lower;
  private int consecutive_num;
  private int seq_char;
  private int seq_num;

  /**
   * Word separating characters
   */

  private char[] seps = {' ', ',', '-', ';', '\'', '\"', '?', '=', '!', '/', ':', '&', '*', '%', '(',
			'@', ')', '#', '_', '^', '`', '[', ']', '\\', '<', '>', '~',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

  /**
   * Constructor for a PasswordChecker object.
   */

  public PasswordChecker()
  {
  }

  private void init()
  {
    n_char = 0;
    n_upper = 0;
    n_num = 0;
    n_sym = 0;
    n_mid = 0;
    letter_only = 0;
    num_only = 0;
    rep_char = 0;
    consecutive_upper = 0;
    consecutive_lower = 0;
    consecutive_num = 0;
    seq_char = 0;
    seq_num = 0;
  }

  private void evaluate(String newpassword)
  {
    init();

    char[] sorted = newpassword.toCharArray();
    Arrays.sort(sorted);

    for (int i = 1; i < sorted.length(); i++)
      {
	if (sorted[i-1] == sorted[i])
	  {
	    if (rep_char == 0)
	      {
		rep_char += 2;
	      }
	    else
	      {
		rep_char++;
	      }
	  }
      }

    char[] chars = newpassword.toCharArray();

    for (int i = 0; i < chars.length; i++)
      {
	char c = chars[i];

	if (Character.isUpperCase(c))
	  {
	    n_char++;
	    n_upper++;

	    if (i > 0)
	      {
		if (Character.isUpperCase(chars[i-1]))
		  {
		    if (consecutive_upper == 0)
		      {
			consecutive_upper += 2;
		      }
		    else
		      {
			consecutive_upper++;
		      }
		  }
	      }
	  }
	else if (Character.isLowerCase(c))
	  {
	    n_char++;
	    n_lower++;

	    if (i > 0)
	      {
		if (Character.isLowerCase(chars[i-1]))
		  {
		    if (consecutive_lower == 0)
		      {
			consecutive_lower += 2;
		      }
		    else
		      {
			consecutive_lower++;
		      }
		  }
	      }
	  }
	else if (Character.isDigit(c))
	  {
	    n_num++;

	    if (i > 0)
	      {
		if (Character.isDigit(chars[i-1]))
		  {
		    if (consecutive_num == 0)
		      {
			consecutive_num += 2;
		      }
		    else
		      {
			consecutive_num++;
		      }

		    if (chars[i-1] + 1 == c || chars[i-1] - 1 == c)
		      {
			seq_num++;
		      }
		  }
	      }
	  }
	else
	  {
	    n_sym++;
	  }
      }

    if (n_char = newpassword.length)
      {
	letter_only = 1;
      }
    else if (n_num = newpassword.length)
      {
	num_only = 1;
      }
  }

  /**
   * Calculates and returns the percentage strength.
   */

  private float calculator()
  {
    int sum = 0, sub = 0;

    sum += n_char * 4;
    sum += (n - n_upper) * 2;
    sum += (n - n_lower) * 2;
    sum += n_num * 4;
    sum += n_sym * 6;
    sum += n_mid * 2;

    sub += letter_only;
    sub += num_only;
    sub += rep_char * (rep_char - 1);
    sub += consecutive_lower * 2;
    sub += consecutive_upper * 2;
    sub += consecutive_num * 2;
    sub += seq_char * 2 + seq_num * 2;

    float p = sum - sub;
  }
}