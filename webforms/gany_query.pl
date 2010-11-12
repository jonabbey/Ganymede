#!<##PERLEXE##>
#####################################################################
#
# Ganymede Query
#
# This script is used to make a query on the Ganymede server.
#
#####################################################################
use IPC::Open2;
use CGI;
use CGI::Carp qw/fatalsToBrowser/;
#####################################################################

# create the query with whatever CGI info we get from our environment
$query = new CGI;

$tmpdir = "/tmp";
$xml_path = "<#XMLPATH#>";
$xmlclient = $xml_path . "/xmlclient";

# Yes, the software is smarter than you, if you didn't include bin
# when installWeb asked you for the location of the client utils.

if (!-f $xmlclient) {
  $xmlclient = $xml_path . "/bin/xmlclient";
}

$query_label = "Submit Query";
$schema_label = "Retrieve Schema";

# write out the CGI header

if (!$query->param) {
    print_headers();
    print_default();
    print_footers();
    print $query->end_html;
    exit 0;
} else {
    if (!-f $xmlclient) {
	print_headers();
	print "<center><p>Error, can't find xmlclient</p></center>\n";
	print_footers();
	print $query->end_html;
	exit 0;
    }

    $user = $query->param('user');
    $password = $query->param('password');
    $querystr = $query->param('querystr');
    $submit = $query->param('submit');

    if ($submit eq "") {
	$submit = $query_label;
    }

    if ($submit eq $query_label) {
	make_queryfile();
    }

    # we know that we can trust $filename, because we created that,
    # and we know that the $password is safe, because we're passing
    # that in via STDIN, but we need to be careful to make sure that
    # they haven't slipped us a mickey on the username $user.
    #
    # we do have to allow colons and whitespace thereafter for admin
    # persona names, though.
    #
    # absolutely no single quotes or back slash characters allowed,
    # thankyouverymuch!

    if ($user !~ /^[a-z]([a-z]|[0-9])*(:([a-z]|[0-9]|\s)+)?$/i) {
	$user = "invalid_web_query_user";
    }

    # if they have any spaces in their name, convert to url escape

    $user =~ s/ /%20/;

    if ($submit eq $schema_label) {
	$program = "$xmlclient 'username=$user' -dumpSchema 2>&1";
    } elsif ($submit eq $query_label) {
	$program = "$xmlclient 'username=$user' -queryfile $filename 2>&1";
    }

    eval {
	open2(*README, *WRITEME, $program);
    };
    
    if ($@) {
	if ($@ =~ /^open2/) {
	    $xml_output = "open2 failed: $!\n$@\n";
	}

	$xml_status = 1;	# fail
    } else {
	print WRITEME "$password\n";
	close(WRITEME);

	$xml_output = "";

	while (<README>) {
	    $xml_output .= $_;
	}

	close(README);

	$xml_status = $? >> 8;

	unlink($filename);

	# cut out the interactive password prompt from xmlclient

	$xml_output =~ s/^Password for.*\n//m;
    }

    if (($xml_status == 0)) {
	print_success($xml_output);
    } else {

	print_headers();

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
	
	print <<ERRORS;
<pre>
$xml_output
</pre>
ERRORS

        print_footers();
	print $query->end_html;
    }
}

######################################################################
#
#                                                       make_queryfile
#
######################################################################

sub make_queryfile
{
  # we want a really random filename

  $randnum = int(rand 4096);

  $filename = "$tmpdir/gany_query.$randnum.$$.txt"; #give temp query file random name

  if (-f $filename) {
    die "Error, $filename already exists!";
  }

  open(XMLF, ">$filename") || die "Couldn't write to $filename";
  chmod 0600, $filename;

  select XMLF;
  print $querystr;
  close(XMLF);

  chmod 0600, $filename;
  select STDOUT;
}


######################################################################
#
#                                                        print_headers
#
######################################################################

sub print_headers {
print $query->header;

print <<ENDSTARTHEAD;
<html>
  <head>
    <title>Ganymede Query</title>

    <!-- Insert your CSS here -->
  </head>
  <body bgcolor="#FFFFFF">
ENDSTARTHEAD

print <<CUSTOMHEAD;
  <!-- Insert your site-specific header here -->
CUSTOMHEAD
}

######################################################################
#
#                                                        print_footers
#
######################################################################

sub print_footers {
print <<FOOTER;
  <!-- your custom footer goes here -->
FOOTER
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
	  <a href="http://www.arlut.utexas.edu/gash2/"><img src="/images/ganymede_title2_sm.gif" border="0"></a>
	</td>
	<td width="100%" align="center">
	  <h1>Ganymede Query Utility</h1>
	</td>
      </tr>

      <tr>
	<td align="center">
	  <a href="http://www.arlut.utexas.edu/gash2/"><small>[Ganymede Home]</small></a>
	</td>
	<td width="100%" align="center">
	  <small><a href="http://tools.arlut.utexas.edu/gash2/doc/querylanguage/">Ganymede Query Language Guide</a></small>
	</td>
      </tr>
    </table>

    <hr noshade/>
<center>

    <center>
      <form method="post" action="gany_query.pl" name="former">
	<table width="60%" bgcolor="#ccffcc" border="1" cellpadding="2">
	  <tr bgcolor="#663366">
	    <td colspan="2" align="center">
	      <big><font color="ffffcc">Ganymede Query Utility</font></big>
	    </td>
	  </tr>

	  <tr>
	  <td align="right"><b>Query:</b></td>
	  <td><input type="text" size="120" name="querystr"></td>
	  </tr>

	  <tr>
	    <td align="right"><b>Username</b></td>
	    <td><input type="text" name="user"></td>
	  </tr>

	  <tr>
	    <td align=right><b>Password</b></td>
	    <td><input type="password" name="password"></td>
	  </tr>

	  <tr>
	    <td colspan="2" align="right"><input type="submit" name="submit" value="$query_label"><input type="submit" name="submit" value="$schema_label"></td>
	  </tr>

	  <tr bgcolor="#663366">
	    <td colspan="2">&nbsp;</td>
	  </tr>

	</table>
      </form>
    </center>

ENDDEFAULT
}

######################################################################
#
#                                                        print_success
#
# Outputs the $xml_output to the user with an appropriate MIME header
#
######################################################################

sub print_success {
    print $query->header("-type"=>"application/xml",
			 "-attachment"=>"ganymede_results.xml");
    print $xml_output;
}
