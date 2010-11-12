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

# add this for errors!!!
use CGI::Carp qw/fatalsToBrowser/;

#####################################################################

# create the query with whatever CGI info we get from our environment

$tmpdir = "/tmp";
$query = new CGI;
$xml_path = "<#XMLPATH#>";
$xmlclient = $xml_path . "/xmlclient";

# Yes, the software is smarter than you, if you didn't include
# bin when installClient asked you for the location of the
# client utils.

if (!-f $xmlclient) {
    $xmlclient = $xml_path . "/bin/xmlclient";
}

# If this script is run from a different location from where the
# image files for the HTML result pages are located, the variable
# $web_loc must be changed to an HTTP path to the image files

$web_loc = ".";

# write out the CGI header

print $query->header;

print <<ENDSTARTHEAD;
<html>
  <head>
    <title>Ganymede Password Changer</title>
ENDSTARTHEAD

# Print Javascript basic form checker
JSVerifyForm();

print <<ENDHEAD;

  </head>
  <body bgcolor="#FFFFFF">
ENDHEAD

if (!-f $xmlclient) {
    print "<center><p>Error, can't find xmlclient</p></center>\n";
    print_tail();
    print $query->end_html;
    exit 0;
}

if ($query->param) {
    $user = $query->param('user');
    $old_pass = $query->param('old_pass');
    $new_pass = $query->param('new_pass');
    $verify = $query->param('verify');

    if ($new_pass eq $verify) {
        make_xml();
        $xml_output = `$xmlclient $filename 2>&1`;
        $xml_status = $? >> 8;
        if (($xml_status == 0)) {
            $time = `/bin/date`;
            print_success();
        } else {
            # we need to make an error message more clear
            if ($xml_output =~ /It is based on the dictionary word ([^.]*)/) {
                $word = $1;

                if ($word eq $user) {
                    $xml_output = "The new password you proposed was based on your username.  You must choose a password that can not be easily guessed from your account's name.";
                } else {
                    $xml_output = "The new password you proposed can be derived from the word '$1', so is too easily guessable by password cracking programs.\n\nPlease choose again.";
                }

                $loggedin_ok = 1;
            }

            if ($xml_output =~ /is too short/) {
                $xml_output = "The new password you proposed is too short.\nPick a password that is at least 6 characters long.";

                $loggedin_ok = 1;
            }

            if ($xml_output =~ /simplistic/) {
                $xml_output = "The new password you proposed follows too predictable a pattern.\n\nPlease try to pick a more random password.";

                $loggedin_ok = 1;
            }

            if ($xml_output =~ /DIFFERENT/) {
                $xml_output = "The new password you proposed does not contain enough different characters.\n\nPlease try to pick a more complex password.";

                $loggedin_ok = 1;
            }

            if ($xml_output =~ /used too recently/ && $xml_output =~ /last used with this account at ([^.]*)/) {
                $xml_output = "The new password you proposed was used for this account at $1.\n\nYou must choose a password that has not been used in conjunction with this account recently.";

                $loggedin_ok = 1;
            }

            if ($xml_output =~ /It is based on your username/) {
                $xml_output = "The new password you proposed was based on your username.\n\nYou must choose a password that can not be easily guessed from your account's name.";

                $loggedin_ok = 1;
            }

            if ($xml_output =~ /contains an unacceptable character \('([^'])'\)/) {
                $xml_output = "The new password you proposed contains an unacceptable character: $1\n\nPlease try again.";

                $loggedin_ok = 1;
            }

            # Couldn't login to server... the server is going down for some reason?
            # Can't login to the Ganymede server.. semaphore disabled: schema edit
            # Error, couldn't log in to server.. bad username or password?
            #
            # XML submission failed.

            if ($xml_output =~ /semaphore disabled/) {
                $xml_output = "The Ganymede server is not currently accepting logins.\nPlease try again later.";
            }

            # Couldn't login to server... bad username/password?
            # Error, couldn't log in to server.. bad username or password?
            # XML submission failed.

            if ($xml_output =~ /bad username/) {
                $xml_output = "You did not enter your current username and/or password correctly.\n\nPlease try again.";
            }

            print_failure($xml_output);
        }
    } else {
        print_nomatch();
    }

    unlink $filename;           #remove temp xml file
    print_tail();
} else {
    print_default();
}

print $query->end_html;


######################################################################
#
#                                                        make_xml
#
######################################################################

sub make_xml {
    # we want a really random filename

    $randnum = int(rand 4096);

    $filename = "$tmpdir/ganypass.$randnum.$$.xml"; #give temp xml file random name

    $old_pass =~ s/&/&amp;/g; #parse passwords for " and &, replace with xml equivalents
    $new_pass =~ s/&/&amp;/g;
    $old_pass =~ s/\"/&quot;/g;
    $new_pass =~ s/\"/&quot;/g;
    $old_pass =~ s/</&lt;/g;
    $new_pass =~ s/</&lt;/g;
    $old_pass =~ s/>/&gt;/g;
    $new_pass =~ s/>/&gt;/g;

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
      <Password><password plaintext="$new_pass"/></Password>
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
    <table border="0">
      <tr>
        <td align="left">
          <a href="http://www.arlut.utexas.edu/gash2/"><img src="$web_loc/ganymede_title2_sm.gif" border="0"></a>
        </td>
        <td width="100%" align="center">
          <h1>Ganymede Password Changing Utility</h1>
        </td>
<!--

If you want a link to your home page, uncomment this and tweak
accordingly.

        <td align="right">
          <a href="/"><img src="$web_loc/arlbw.jpg" border="0"></a>
        </td>
-->

      </tr>

      <tr>
        <td align="center">
          <a href="http://www.arlut.utexas.edu/gash2/"><small>[Ganymede Home]</small></a>
        </td>
        <td width="100%" align="center">
          <small>[Click <a href="$web_loc/index.html" target="_top">here</a>
                 to go directly to the Ganymede login page]</small>
        </td>

<!--

If you want a link to your home page, uncomment this and tweak
accordingly.

        <td align="center">
          <a href="http://www.arlut.utexas.edu/"><small>[ARL:UT Home]</small></a>
        </td>
-->

      </tr>
</table>

    <hr noshade />
<center>
    <table border="0" width="60%">
<tr>

<td align="center">

<!--

You need to change this as well, of course

-->

<p>This form changes your user password for Ganymede and all network
services managed by Ganymede at ARL:UT.</p>

<p>All use of this form is logged, and you will receive email from Ganymede
notifying you of the success of your password change request.</p>
</td>
</tr>
    </table>
    </center>

    <center>
      <form method="post" action="ganypass.pl">
        <table width="60%" bgcolor="#ccffcc" border="1" cellpadding="2">
          <tr bgcolor="#663366">
            <td colspan="2" align="center">
              <big><font color="#ffffcc">Ganymede Password Changer</font></big>
            </td>
          </tr>

          <tr>
            <td align="right"><b>Username</b></td>
            <td><input type="text" name="user"></td>
          </tr>

          <tr>
            <td align="right"><b>Old Password</b></td>
            <td><input type="password" name="old_pass"></td>
          </tr>

          <tr>
            <td align="right"><b>New Password</b></td>
            <td><input type="password" name="new_pass"></td>
          </tr>

          <tr>
            <td align="right"><b>Verify New Password</b></td>
            <td><input type="password" name="verify"></td>
          </tr>

          <tr>
            <td colspan="2" align="center"><input type="submit" value="submit"></td>
          </tr>

          <tr bgcolor="#663366">
            <td colspan="2">&nbsp;</td>
          </tr>

        </table>
      </form>
    </center>

ENDDEFAULT

print_tail();
}

######################################################################
#
#                                                        print_success
#
######################################################################

sub print_success {
    print <<ENDSUCCESS;
    <table border="0">
      <tr>
        <td align="left">
          <a href="http://www.arlut.utexas.edu/gash2/"><img src="$web_loc/ganymede_title2_sm.gif" border="0"></a>
        </td>
        <td width="100%" align="center">
          <h1>Password Changed Successfully</h1>
        </td>
        <td align="right">
          <a href="/"><img src="/graphics/arlbw.jpg" border="0"></a>
        </td>
      </tr>

      <tr>
        <td align="center">
          <a href="http://www.arlut.utexas.edu/gash2/"><small>[Ganymede Home]</small></a>
        </td>
        <td width="100%" align="center">
          <small>[Click <a href="$web_loc/index.html" target="_top">here</a> to go directly to the Ganymede login page]</small>
        </td>
        <td align="center">
          <a href="/"><small>[ARL:UT Home]</small></a>
        </td>
      </tr>
    </table>

    <hr noshade="noshade"/>

    <center>
      <table border="0" width="60%">
        <tr>
          <td align="center">
            <p>Ganymede has accepted your password change
            request, and is currently working to propagate your changed password
            information into the network.  It may take a few minutes for your new
            password to take effect everywhere.</p>

            <p>As additional confirmation, in a few moments you will receive a
            mail message from Ganymede describing the change to your
            account.</p>
          </td>
        </tr>
      </table>
    </center>

    <br/>

    <center>
      <table width="60%" bgcolor="#ccffcc" border="1" cellpadding="2">
        <tr bgcolor="#663366">
          <td colspan="2" align="center">
            <big><font color="ffffcc">Ganymede Password Changer</font></big>
          </td>
        </tr>

        <tr><td colspan="2"><br/></td></tr>

        <tr><td colspan="2" align="center">Time: $time</td></tr>

        <tr><td colspan="2"><br/></td></tr>

        <tr><td colspan="2" align="center">Password change request processed for user $user</td></tr>

        <tr><td colspan="2"><br/></td></tr>

        <tr bgcolor="663366">
          <td colspan="2">&nbsp;</td>
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
my ($failure) = @_;

$failure =~ s/</&lt;/g;
$failure =~ s/>/&gt;/g;

    print <<ENDFAILURE;
    <table border="0">
      <tr>
        <td align="left">
          <a href="http://www.arlut.utexas.edu/gash2/"><img src="$web_loc/ganymede_title2_sm.gif" border="0"/></a>
        </td>
        <td width="100%" align="center">
          <h1>Password not changed<br/>Check username and password</h1>
        </td>
        <td align="right">
          <a href="/"><img src="$web_loc/graphics/arlbw.jpg" border="0"></a>
        </td>
      </tr>

      <tr>
        <td align="center">
          <a href="http://www.arlut.utexas.edu/gash2/"><small>[Ganymede Home]</small></a>
        </td>
        <td width="100%" align="center">
          <small>[Click <a href="$web_loc/index.html" target="_top">here</a> to go directly to the Ganymede login page]</small>
        </td>
        <td align="center">
          <a href="/"><small>[ARL:UT Home]</small></a>
        </td>
      </tr>
    </table>

    <hr noshade="noshade"/>

    <center>

      <table border="0" width="60%">
        <tr>
          <td align="center">
            <p>Ganymede was not able to accept your
            password change request.  The following error message was
            reported:</p>

            <pre>
$failure
            </pre>

            <p>If your current password was not entered properly, you will receive
            mail from Ganymede reporting a failure to login.  This is normal, and
            is simply letting you know that someone unsuccessfully attempted to
            make a change in Ganymede on your behalf.</p>
          </td>
        </tr>
      </table>

      <form method="POST" action="ganypass.pl">
        <table width="60%" bgcolor="#ccffcc" border="1" cellpadding="2">
          <tr bgcolor="#663366">
            <td colspan="2" align="center">
              <big><font color="ffffcc">Ganymede Password Changer</font></big>
            </td>
          </tr>

          <tr>
            <td align="right"><b>Username?</b></td>
            <td><input type="text" name="user" value="$user"></td>
          </tr>

          <tr>
            <td align="right"><b>Old Password?</b></td>
            <td><input type="password" name="old_pass"></td>
          </tr>

          <tr>
            <td align="right"><b>New Password?</b></td>
            <td><input type="password" name="new_pass"></td>
          </tr>

          <tr>
            <td align="right"><b>Verify New Password</b></td>
            <td><input type="password" name="verify"></td>
          </tr>

          <tr>
            <td colspan="2" align="center"><input type="submit" value="SUBMIT"></td>
          </tr>

          <tr bgcolor="663366">
            <td colspan="2">&nbsp;</td>
          </tr>
        </table>
      </form>
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
    <table border="0">
      <tr>
        <td align="left">
          <a href="http://www.arlut.utexas.edu/gash2/"><img src="$web_loc/ganymede_title2_sm.gif" border="0"/></a>
        </td>
        <td width="100%" align="center">
          <h1>Password not changed<br/>password verification failed</h1>
        </td>
        <td align="right">
          <a href="/"><img src="$web_loc/arlbw.jpg" border="0"></a>
        </td>
      </tr>

      <tr>
        <td align="center">
          <a href="http://www.arlut.utexas.edu/gash2/"><small>[Ganymede Home]</small></A>
        </td>
        <td width="100%" align="center">
          <small>[Click <a href="$web_loc/index.html" target="_top">here</a> to go directly to the Ganymede login page]</small>
        </td>
        <td align="center">
          <a href="/"><small>[ARL:UT Home]</small></a>
        </td>
      </tr>
    </table>

    <hr noshade="noshade"/>

    <center>
      <table border="0" width="60%">
        <tr>
          <td align="center">
            <p>Ganymede was not able to accept your
            password change request.  You did not enter your new password
            consistently.  Please try again.</p>
          </td>
        </tr>
      </table>

      <form method="POST" action="ganypass.pl">
        <table width="60%" bgcolor="#ccffcc" border="1" cellpadding="2">
          <tr bgcolor="#663366">
            <td colspan="2" align="center">
              <big><font color="ffffcc">Ganymede Password Changer</font></big>
            </td>
          </tr>

          <tr>
            <td align="right"><b>Username?</b></td>
            <td><input type="text" name="user" value="$user"></td>
          </tr>

          <tr>
            <td align="right"><b>Old Password?</b></td>
            <td><input type="password" name="old_pass"></td>
          </tr>

          <tr>
            <td align="right"><b>New Password?</b></td>
            <td><input type="password" name="new_pass"></td>
          </tr>

          <tr>
            <td align="right"><b>Verify New Password</b></td>
            <td><input type="password" name="verify"></td>
          </tr>

          <tr>
            <td colspan="2" align="center"><input type="submit" value="SUBMIT"></td>
          </tr>

          <tr bgcolor="663366">
            <td colspan="2">&nbsp;</td>
          </tr>

        </table>
      </form>
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
  </body>
</html>
END
    return;
}
