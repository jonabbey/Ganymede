#!<##PERLEXE##>
#####################################################################
#
# Ganymede Password Changer
#
# This script is used to process the form in
# 
#
# This script accepts a username, old password, new
# password and verified new password, then passes them
# to xmlclient, which attempts to change the Ganymede
# password for user from the old_pass to new_pass
#
#####################################################################
use CGI;

#####################################################################

$query = new CGI;
$xml_path = "<#XMLPATH#>";
$xmlclient = $xml_path . "/xmlclient";

# Yes, the software is smarter than you, if you didn't include
# bin when installClient asked you for the location of the
# client utils.

if (!-f $xmlclient) {
  $xmlclient = $xml_path . "/bin/xmlclient";
}

$tmpdir = "/tmp";

#If this script is run from a different location from where the
#image files for the HTML result pages are located, the variable
#$web_loc must be changed to an HTTP path to the image files

$web_loc = "."; 

if ($query->param) {

    $user = $query->param('user');
    $old_pass = $query->param('old_pass');
    $new_pass = $query->param('new_pass');
    $verify = $query->param('verify');
    

    # write out the CGI header

    print $query->header;

    if ($new_pass eq $verify) {
      &make_xml;
      $xml_output = `$xmlclient $filename >/dev/null 2>&1`;
      $xml_status = $? >> 8;
      if (($xml_status == 0)) {
	    $time = `/usr/bin/date`;
	    &print_success;
        } else {
	    &print_failure;
        }
    } else {
	&print_nomatch;
    }
    
    unlink $filename;  #remove temp xml file
    &print_tail;

    print $query->end_html;
}

else
{
 
  print $query->header;
  &print_default;

}
  

######################################################################
#
#                                                        make_xml
#
######################################################################

sub make_xml
{
  # we want a really random filename

  $randnum = int(rand 4096);

  $filename = "$tmpdir/ganypass.$randnum.$$.xml"; #give temp xml file random name
 
  $old_pass =~ s/&/&amp;/g;   #parse passwords for " and &, replace with xml equivalents
  $new_pass =~ s/&/&amp;/g;
  $old_pass =~ s/\"/&quot;/g;
  $new_pass =~ s/\"/&quot;/g;
  
  if (-f $filename) {
    die "Error, $filename already exists!";
  }

  open(XMLF, ">$filename") || die "Couldn't write to $filename";
  chmod 0600, $filename;

  select XMLF;
  print <<WRITEXML;
  <ganymede major="1" minor="0" persona="$user" password="$old_pass">
    <ganydata>
      <object type="User" id="$user">
	<Password><password plaintext="$new_pass"/>
	  </Password>
      </object>
   </ganydata>
  </ganymede>

WRITEXML

  close(XMLF);
  chmod 0600, $filename;
  select STDOUT;
}
 
 
######################################################################
#
#                                                        print_default
#
######################################################################

sub print_default {  

  print <<ENDDEFAULT;
<HTML>
  <HEAD>
    <TITLE>Ganymede Password Changer</TITLE>
  </HEAD>
  <BODY BGCOLOR="#FFFFFF">
    
    <table border=0>
      <tr>
	<td align=left>
	  <A HREF="http://www.arlut.utexas.edu/gash2/"><img src="$web_loc/ganymede_title2_sm.gif" border=0></a>
	</td>
	<td width="100%" align=center>
	  <h1>Ganymede Password Changing Utility</h1>
	</td>
	<td align=right>
	  <a href="/"><img src="$web_loc/arlbw.jpg" border=0></a>
	</td>
      </tr>

      <tr>
	<td align=center>
	  <A HREF="http://www.arlut.utexas.edu/gash2/"><small>[Ganymede Home]</small></A>
	</td>
	<td width="100%" align=center>
	  <small>[Click <a href="$web_loc/index.html" target="_top">here</a> to go directly to the Ganymede login page]</small>
	</td>
	<td align=center>
	  <a href="http://www.arlut.utexas.edu/"><small>[ARL:UT Home]</small></a>
	</td>
      </tr>
</table>

    <hr noshade> 
<center>
    <table border=0 width="60%">
<tr>

<td align="center">
<p>This form changes your user password for Ganymede and all network
services managed by Ganymede at ARL:UT.</p>

<p>All use of this form is logged, and you will receive email from Ganymede
notifying you of the success of your password change request.</p>
</td>
</tr>
    </table>
    </center>

    <center>
      <FORM METHOD="POST" ACTION="ganypass.pl"> 
	<table width="60%" bgcolor="#ccffcc" border="1" cellpadding="2">
	  <tr bgcolor="#663366">
	    <td colspan=2 align=center>
	      <big><font color=ffffcc>Ganymede Password Changer</font></big>
	    </td>
	  </tr>

	  <tr>	
	    <td align=right><b>Username?</b></td>
	    <td><INPUT TYPE="text" NAME="user"></td>
	  </tr>

	  <tr>
	    <td align=right><b>Old Password?</b></td>
	    <td><INPUT TYPE="password" NAME="old_pass"></td>
	  </tr>

	  <tr>
	    <td align=right><b>New Password?</b></td>
	    <td><INPUT TYPE="password" NAME="new_pass"></td>
	  </tr>

	  <tr>
	    <td align=right><b>Verify New Password</b></td>
	    <td><INPUT TYPE="password" NAME="verify"></td>
	  </tr>

	  <tr>
	    <td colspan=2 align=center><INPUT TYPE=submit VALUE=SUBMIT></td>
	  </tr>

	  <tr bgcolor=663366>
	    <td colspan=2>&nbsp;</td>
	  </tr>

	</table>
      </FORM>
    </center>

ENDDEFAULT

&print_tail;

}



######################################################################
#
#                                                        print_success
#
######################################################################

sub print_success {
    print <<ENDSUCCESS;
 <HTML>
  <HEAD>
    <TITLE>Ganymede Password Changed Successfully</TITLE>
  </HEAD>
  <BODY BGCOLOR="#FFFFFF">
    <table border=0>
      <tr>
	<td align=left>
	  <A HREF="http://www.arlut.utexas.edu/gash2/"><img src="$web_loc/ganymede_title2_sm.gif" border=0></a>
	</td>
	<td width="100%" align=center>
	  <h1>Password Changed Successfully</h1>
	</td>
	<td align=right>
	  <a href="/"><img src="/graphics/arlbw.jpg" border=0></a>
	</td>
      </tr>

      <tr>
	<td align=center>
	  <A HREF="http://www.arlut.utexas.edu/gash2/"><small>[Ganymede Home]</small></A>
	</td>
	<td width="100%" align=center>
	  <small>[Click <a href="$web_loc/index.html" target="_top">here</a> to go directly to the Ganymede login page]</small>
	</td>
	<td align=center>
	  <a href="/"><small>[ARL:UT Home]</small></a>
	</td>
      </tr>
</table>

    <hr noshade> 
<center>
    <table border=0 width="60%">
<tr>
<td align="center"> <p>Ganymede has accepted your password change
request, and is currently working to propagate your changed password
information into the network.  It may take a few minutes for your new
password to take effect everywhere.</p>

<p>As additional confirmation, in a few moments you will receive a
mail message from Ganymede describing the change to your
account.</p></td></tr>

    </table>
    </center>

<br>

    <center>
	<table width="60%" bgcolor="#ccffcc" border="1" cellpadding="2">
	  <tr bgcolor="#663366">
	    <td colspan=2 align=center>
	      <big><font color=ffffcc>Ganymede Password Changer</font></big>
	    </td>
	  </tr>

          <tr><td colspan=2><br></td></tr>

	  <tr><td colspan=2 align=center>Time: $time</td></tr>

          <tr><td colspan=2><br></td></tr>

	  <tr><td colspan=2 align=center>Password change request processed for user $user</td></tr>

          <tr><td colspan=2><br></td></tr>

	  <tr bgcolor=663366>
	    <td colspan=2>&nbsp;</td>
	  </tr>

	</table>
    </center>

ENDSUCCESS
}

######################################################################
#
#                                                        print_failure
#
######################################################################

sub print_failure {
    print <<ENDFAILURE;
 <HTML>
  <HEAD>
    <TITLE>Ganymede Password Changer</TITLE>
  </HEAD>
  <BODY BGCOLOR="#FFFFFF">
    
    <table border=0>
      <tr>
	<td align=left>
	  <A HREF="http://www.arlut.utexas.edu/gash2/"><img src="$web_loc/ganymede_title2_sm.gif" border=0></a>
	</td>
	<td width="100%" align=center>
	  <h1>Password not changed<br>Check username and password</h1>
	</td>
	<td align=right>
	  <a href="/"><img src="$web_loc/graphics/arlbw.jpg" border=0></a>
	</td>
      </tr>

      <tr>
	<td align=center>
	  <A HREF="http://www.arlut.utexas.edu/gash2/"><small>[Ganymede Home]</small></A>
	</td>
	<td width="100%" align=center>
	  <small>[Click <a href="$web_loc/index.html" target="_top">here</a> to go directly to the Ganymede login page]</small>
	</td>
	<td align=center>
	  <a href="/"><small>[ARL:UT Home]</small></a>
	</td>
      </tr>
</table>

    <hr noshade> 
<center>

    <table border=0 width="60%">

<tr> <td align="center"> <p>Ganymede was not able to accept your
password change request.  Either your username or your current
password were not valid.</p>

<p>If your current password was not entered properly, you will receive
mail from Ganymede reporting a failure to login.  This is normal, and
is simply letting you know that someone unsuccessfully attempted to
make a change in Ganymede on your behalf.</p></tr>
    </table>

    </table>
    </center>

    <center>
      <FORM METHOD="POST" ACTION="ganypass.pl"> 
	<table width="60%" bgcolor="#ccffcc" border="1" cellpadding="2">
	  <tr bgcolor="#663366">
	    <td colspan=2 align=center>
	      <big><font color=ffffcc>Ganymede Password Changer</font></big>
	    </td>
	  </tr>

	  <tr>	
	    <td align=right><b>Username?</b></td>
	    <td><INPUT TYPE="text" NAME="user" VALUE="$user"></td>
	  </tr>

	  <tr>
	    <td align=right><b>Old Password?</b></td>
	    <td><INPUT TYPE="password" NAME="old_pass"></td>
	  </tr>

	  <tr>
	    <td align=right><b>New Password?</b></td>
	    <td><INPUT TYPE="password" NAME="new_pass"></td>
	  </tr>

	  <tr>
	    <td align=right><b>Verify New Password</b></td>
	    <td><INPUT TYPE="password" NAME="verify"></td>
	  </tr>

	  <tr>
	    <td colspan=2 align=center><INPUT TYPE=submit VALUE=SUBMIT></td>
	  </tr>

	  <tr bgcolor=663366>
	    <td colspan=2>&nbsp;</td>
	  </tr>

	</table>
      </FORM>
    </center>

ENDFAILURE
}

######################################################################
#
#                                                        print_nomatch
#
######################################################################

sub print_nomatch {
    print <<ENDNOMATCH;
 <HTML>
  <HEAD>
    <TITLE>Ganymede Password Changer</TITLE>
  </HEAD>
  <BODY BGCOLOR="#FFFFFF">
    
    <table border=0>
      <tr>
	<td align=left>
	  <A HREF="http://www.arlut.utexas.edu/gash2/"><img src="$web_loc/ganymede_title2_sm.gif" border=0></a>
	</td>
	<td width="100%" align=center>
	  <h1>Password not changed<br>password verification failed</h1>
	</td>
	<td align=right>
	  <a href="/"><img src="$web_loc/arlbw.jpg" border=0></a>
	</td>
      </tr>

      <tr>
	<td align=center>
	  <A HREF="http://www.arlut.utexas.edu/gash2/"><small>[Ganymede Home]</small></A>
	</td>
	<td width="100%" align=center>
	  <small>[Click <a href="$web_loc/index.html" target="_top">here</a> to go directly to the Ganymede login page]</small>
	</td>
	<td align=center>
	  <a href="/"><small>[ARL:UT Home]</small></a>
	</td>
      </tr>
</table>

    <hr noshade> 
<center>
    <table border=0 width="60%">

<tr> <td align="center"> <p>Ganymede was not able to accept your
password change request.  You did not enter your new password
consistently.  Please try again.</tr>

    </table>
    </center>


    <center>
      <FORM METHOD="POST" ACTION="ganypass.pl"> 
	<table width="60%" bgcolor="#ccffcc" border="1" cellpadding="2">
	  <tr bgcolor="#663366">
	    <td colspan=2 align=center>
	      <big><font color=ffffcc>Ganymede Password Changer</font></big>
	    </td>
	  </tr>

	  <tr>	
	    <td align=right><b>Username?</b></td>
	    <td><INPUT TYPE="text" NAME="user" VALUE="$user"></td>
	  </tr>

	  <tr>
	    <td align=right><b>Old Password?</b></td>
	    <td><INPUT TYPE="password" NAME="old_pass"></td>
	  </tr>

	  <tr>
	    <td align=right><b>New Password?</b></td>
	    <td><INPUT TYPE="password" NAME="new_pass"></td>
	  </tr>

	  <tr>
	    <td align=right><b>Verify New Password</b></td>
	    <td><INPUT TYPE="password" NAME="verify"></td>
	  </tr>

	  <tr>
	    <td colspan=2 align=center><INPUT TYPE=submit VALUE=SUBMIT></td>
	  </tr>

	  <tr bgcolor=663366>
	    <td colspan=2>&nbsp;</td>
	  </tr>

	</table>
      </FORM>
    </center>
ENDNOMATCH
}

######################################################################
#
#                                                           print_tail
#
######################################################################

sub print_tail {
    print <<END;
<hr noshade>
<a href="mailto:webmaster\@arlut.utexas.edu">webmaster\@arlut.utexas.edu</a><P>
  </BODY>
</HTML>
END
    return;
} 
