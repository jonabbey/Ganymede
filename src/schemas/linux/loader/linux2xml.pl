#!/opt/bin/perl5

#=====================================================================
#                                linux2xml
#
# Takes Linux /etc/passwd, /etc/shadow, and /etc/group files and
# converts them into an xml representation for loading into
# Ganymede.
#
# Amy Bush & Jonathan Abbey
# 11/1999-9/2000
#
# Released under GPL as part of the Ganymede network directory
# management system, http://www.arlut.utexas.edu/gash2
#
# This script requires that the XML:Writer Perl 5 module be installed
# in order to operate.  You can find this module in CPAN, or
# at http://www.megginson.com/Software/index.html.
#
# Release: $Name:  $
# Version: $Revision: 1.3 $
# Date: $Date: 2000/09/15 05:44:02 $
#
#=====================================================================

use XML::Writer;
use IO;

#=====================================================================
# Globals  
#=====================================================================

# Ganymede XML format major and minor numbers

$major_ver = 1;
$minor_ver = 0;

# name of directory containing the files we are going to load from

$dir = ".";

# name of file containing user records

$passwdfile = "$dir/passwd";

# name of file containing group records

$groupfile = "$dir/group";

# name of shadow file, if in use

$shadowfile = "$dir/shadow";

#=====================================================================
# Main  
#=====================================================================

# Loop through passwd file creating a hash entry (username as key)
# for each passwd entry.

open (INPUT, "<$passwdfile") || die "Error!  Couldn't open $passwdfile!";

while (<INPUT>) {

  # in the context of a while (<FILEHANDLE>)
  # loop, $_ is set to each line of the text,
  # including trailing newline

  chop $_;  # cut off trailing newline from $_

  # Then apply a regexp to the line in $_.. check to
  # see if it matches so we skip blank lines and so we can
  # report any errors we detect

  if (/([^:]+):([^:]*):([^:]*):([^:]*):([^:]*):([^:]*):(.*)/) {
    $username = $1;
    $passwd = $2;
    $uid = $3;
    $gid = $4;
    $gecos = $5;
    $homedir = $6;
    $shell = $7;

  # Actually fill the hash with the information gathered

    $UsernameToUser{$username} = {
      name     => $username,
      password => $passwd,
      uid      => $uid,
      gid      => $gid,
      gecos    => $gecos,
      homedir  => $homedir,
      shell    => $shell,
    };
  } else {
    print STDERR "Error, passwd line\n$_\ndidn't match regexp for passwd entry";
  }
}

close (INPUT);

# Only access the $shadowfile if it has been declared to exist

if (defined $shadowfile) {

  # If $UsernameToUser{username}{password} is x (or one character), pull the 
  # actual password from the shadow/master.passwd file and place the
  # value in $UsernameToUser{username}{password}

  open (INPUT, "<$shadowfile") || die "Error!  Couldn't open $shadowfile";

  while (<INPUT>) {

    chop $_;  # cut off trailing newline from $_

    if (/([^:]+):([^:]*):([^:]*):([^:]*):([^:]*):([^:]*):([^:]*):([^:]*):(.*)/) {
      $username = $1;
      $passwd = $2;

      # if a username appears in the shadow file but not in the passwd
      # file, it is skipped and not added or accounted for

      if ($UsernameToUser{$username}) {
        if (length($UsernameToUser{$username}{password}) < 3) {
          $UsernameToUser{$username}{password} = $passwd;
        } else {
          print STDERR "$username may already have a valid password - skipping shadow password for this user\n";
        }
      } else {
        print STDERR "$username was found in the shadow file but not the passwd file - skipping\n";
      }
    } else {
      print STDERR "Error, shadow line\n$_\ndidn't match regexp for shadow entry";
    }
  }
  
  close (INPUT);
}

# Parse group file line by line and place the fields in the 
# GroupnameToGroup hash. At the same time, place gid (key) and
# groupname (value) in the GidToName hash for future lookups.

open (INPUT, "<$groupfile") || die "Error!  Couldn't open $groupfile!";

while (<INPUT>) {
  
  chop $_;  # cut off trailing newline from $_
  
  if (/([^:]+):([^:]*):([^:]*):(.*)/) {
    $groupname = $1;
    $password = $2;
    $gid = $3;
    $userlist = $4;
    
    $GidToName{$gid} = $groupname;
    
    # Check user array member-by-member -- if there is a user in a group who
    # is not in the passwd file, the user is skipped

    @userstemp = split /,/, $userlist;
    @users = ();

    foreach $user (@userstemp) {
      if ($UsernameToUser{$user}) { 
        push @users, $user;
      } else {
        print STDERR "$user is in a group but not in the passwd file - skipping\n";
      }
    }
    
    $GroupnameToGroup{$groupname} = {
				     groupname => $groupname,
				     password  => $password,
				     gid       => $gid,
				     users     => [@users],
				    };
    
  } else {
    print STDERR "Error, group line\n$_\ndidn't match regexp for group entry";
  }
}

#&printUsers;
#&printGroups;
&printXML();

exit;

#=====================================================================
# Subroutines  
#=====================================================================

#---------------------------------------------------------------------
#                                                             printXML
#
# Subroutine printXML uses the information in the %UsernameToUser, 
# %GroupnameToGroup and %GidToName hashes to produce an XML document
# formatted in such a way that Ganymede can import it
#
#---------------------------------------------------------------------

sub printXML {

  $output = new IO::File(">$Output_File");
  $writer = new XML::Writer(OUTPUT => $output);

  # set up the indentation level

  $indentlevel = 0;

  # write out the ganymede document element

  $writer->startTag("ganymede", 'major' => $major_ver, 'minor' => $minor_ver);
  $indentlevel++;

  # we could write out the <ganyschema> section here if we had a reason
  # to.  instead, we proceed directly to write out the <ganydata> element

  &xmlIndent();
  $writer->startTag("ganydata", 'username' => "supergash");
  $indentlevel++;

  #
  # write out all the <object type="user"> elements, for users from the
  # passwd file
  #

  foreach $user (keys %UsernameToUser) {
    &xmlIndent();
    $writer->startTag("object", 'type' => 'User', 'id', => $UsernameToUser{$user}{name});

    $indentlevel++;

    &xmlIndent();
    $writer->startTag("Username");
    $writer->characters($UsernameToUser{$user}{name});
    $writer->endTag("Username");

    &xmlIndent();
    $writer->startTag("UID");
    $writer->emptyTag("int", 'val' => $UsernameToUser{$user}{uid});
    $writer->endTag("UID");

    &xmlIndent();
    $writer->startTag("Password");
    $writer->emptyTag("password", 'crypt' => $UsernameToUser{$user}{password});
    $writer->endTag("Password");

    # We are only showing the user's home group in the <Groups> field, even
    # if the user is actually in multiple groups.. the Ganymede server will
    # take care of linking ther Groups into this user at load time if need
    # be.

    &xmlIndent();
    $writer->startTag("Groups");
    $writer->emptyTag("invid", 'type' => 'Group', 'id' => $GidToName{$UsernameToUser{$user}{gid}});
    $writer->endTag("Groups");

    # This field is actually called "Home Group", note that we have to
    # substitute underscores for spaces in field names when doing XML
    # loading

    &xmlIndent();
    $writer->startTag("Home_Group");
    $writer->emptyTag("invid", 'type' => 'Group', 'id' => $GidToName{$UsernameToUser{$user}{gid}});
    $writer->endTag("Home_Group");

    &xmlIndent();
    $writer->startTag("Login_Shell");
    
    if (length($UsernameToUser{$user}{shell}) == 0) {
      $writer->characters("/bin/false");
    } else {
      $writer->characters($UsernameToUser{$user}{shell});
    }

    $writer->endTag("Login_Shell");

    &xmlIndent();
    $writer->startTag("Home_Directory");
    $writer->characters($UsernameToUser{$user}{homedir});
    $writer->endTag("Home_Directory");

    $indentlevel--;

    &xmlIndent();
    $writer->endTag("object");
  }

  #
  # write out all the <object type="group"> elements, for groups from the
  # group file
  #

  foreach $group (keys %GroupnameToGroup) {
    &xmlIndent();
    $writer->startTag("object", 'type' => 'Group', 'id' => $GroupnameToGroup{$group}{groupname});
    $indentlevel++;

    &xmlIndent();
    $writer->startTag("Groupname");
    $writer->characters($GroupnameToGroup{$group}{groupname});
    $writer->endTag("Groupname");

    &xmlIndent();
    $writer->startTag("GID");
    $writer->emptyTag("int", 'val' => $GroupnameToGroup{$group}{gid});
    $writer->endTag("GID");

    # we're only going to write out users that were explicitly listed
    # in the group file.. we don't write out implicit membership due
    # to the gid field in the passwd file

    @usermembers = @{$GroupnameToGroup{$group}{users}};

    if ($#usermembers > 0) {
      &xmlIndent();
      $writer->startTag("Users");

      $indentlevel++;

      foreach $groupuser (@usermembers) {
	&xmlIndent();
	$writer->emptyTag("invid", 'type' => 'User', 'id' => $groupuser);
      }
      
      $indentlevel--;

      &xmlIndent();
      $writer->endTag("Users");
    }

    $indentlevel--;

    &xmlIndent();
    $writer->endTag("object");
  }

  # close the <ganydata> section

  $indentlevel--;
  &xmlIndent();
  $writer->endTag("ganydata");

  # and finish up by closing the <ganymede> document element

  $indentlevel--;
  &xmlIndent();
  $writer->endTag("ganymede");

  $writer->end();
} 

#---------------------------------------------------------------------
#                                                             xmlIndent
#
# xmlIndent writes out a newline and the current amount of leading
# indentation, as counted by the global $indentlevel, to the
# global $writer XML::Writer object.
#
#---------------------------------------------------------------------

sub xmlIndent() {
  my $indentation = "\n";
  my $tab = " " x 2;   # leading spaces           

  # This newline bit is just so closing tags can be printed on the 
  # same line as the opening tag if there is no data contained 
  # between them: <tag></tag> instead of messier <tag>
  #                                              </tag>

  $indentation .= $tab x $indentlevel;

  # Indentation 

  $writer->characters($indentation);
}

#---------------------------------------------------------------------
#                                                           printUsers
# printUsers - for testing
# Subroutine to print out the contents of the $UsernameToUser hash -
# prints the groupname instead of the gid
#---------------------------------------------------------------------

sub printUsers {
  foreach $key (keys %UsernameToUser) {
    foreach $key2 (keys %{$UsernameToUser{$key}}) {
      if ($key2 eq "gid") {
        print "group\t";
        print "$GidToName{$UsernameToUser{$key}{$key2}}\n";
      } else {
	print "$key2\t";
	print "$UsernameToUser{$key}{$key2}\n";
      }
    }
    print "\n";
  }
}

#---------------------------------------------------------------------
#                                                          printGroups
#
# printGroups - for testing
# Subroutine to print the contents of the $GroupnameToGroup hash
#
#---------------------------------------------------------------------

sub printGroups {
  foreach $key (keys %GroupnameToGroup) {
    foreach $key2 (keys %{$GroupnameToGroup{$key}}) {
      print "$key2\t";
      if ($key2 eq "users") {
	foreach $user ($GroupnameToGroup{$key}{users}) {
	  print "@{$user}\n";
	}
      } else {
        print "$GroupnameToGroup{$key}{$key2}\n";
      }
    }
    print "\n";
  }
}
