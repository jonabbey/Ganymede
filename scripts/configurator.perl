#!/opt/bin/perl5
#
# This script is designed to walk through the ganymede package
# and make all the build scripts.  It is run by the configure
# script in the root of the ganymede distribution.
#
# $Revision: 1.61 $
# $Date: 2004/03/20 02:36:34 $
# $Name:  $
#
# Jonathan Abbey
# jonabbey@arlut.utexas.edu
#
############################################################

die "We require Perl 5.003 or greater to install Ganymede." if $] < 5.003;

use File::Copy;
use Cwd;
use English;

#########################################################################
#
#                                                                 resolve
#
# input: $dir - absolute pathname of current directory
#        $link - string containing the readlink() results for a
#                symbolic link in $dir to be processed
#
# returns: absolute pathname of the target of the symbolic link
#
#########################################################################
sub resolve{
    my($dir, $link) = @_;
    my(@alinkp, $d, $alinkp);

    # make array representations of
    # the current directory and symbolic link

    # if we have a leading / in our $dir or $link,
    # we'll need to shift to get rid of the leading
    # empty array element

    @dirp=split(/\//, $dir);
    shift(@dirp) if (!($dirp[0]));
    @linkp=split(/\//, $link);
    shift(@linkp) if (!($linkp[0]));

    # @alinkp is an array that we will build to contain the absolute link
    # target pathname.  If the link does not begin with a /, it is a relative link,
    # and we need to place our current directory into the @alinkp array.

    if ($link !~ /^\//) {
	@alinkp=@dirp;
    }

    # modify the @alinkp array according
    # to each path component of the @linkp array
    # (an array representation of the symbolic link
    # given to us), to arrive at the ultimate absolute
    # pathname of the symbolic link

    $d = shift(@linkp);

    while ($d) {
	if ($d eq "..") {
	    pop(@alinkp);
	}
	elsif ($d ne "."){
	    push(@alinkp, $d);
	}
	$d=shift(@linkp);
    }

    $"='/';

    # perl functions return the value of the last expression
    # in the subroutine

    $alinkp="/@alinkp";
}

#########################################################################
#
#                                                         removelastslash
#
# input: a pathname to test
#
# this function will remove a trailing slash from the directory name
# input
#
#########################################################################
sub removelastslash{
  if ($_[0] =~ /\/$/) {
    chop $_[0];
  }

  return $_[0];
}

#########################################################################
#
#                                                           write_install
#
# input: $template - filename (no path) of the template to be copied
#        $target - filename (no path) of the install script
#
#########################################################################

sub write_install {

    my ($template, $target) = @_;

    open(INSTIN, "<$rootdir/scripts/$template") || die ("Can't read $rootdir/scripts/$template");
    open(INSTOUT, ">$rootdir/$target") || die("Can't create the $target");

    while (<INSTIN>){
	s/\/opt\/bin\/perl5/$perlname/g;
	s/\<\#JAVADIR\#\>/$javadir/g;
	s/\<\#SWINGDIR\#\>/$swingdir/g;
	s/\<\#PERLEXE\#\>/$perlname/g;
	print INSTOUT $_;
    }

    close(INSTOUT);
    close(INSTIN);

    chmod 0755, "$target";
}

#########################################################################
#
#                                                                 makedir
#
# input: 1) a directory to make
#        2) octal chmod bits
#
#########################################################################
sub makedir{
  my ($dirpath, $chmod) = @_;

  if (!-e $dirpath) {
    mkdir ($dirpath, $chmod) or die("*Couldn't make $dirpath*");
  }
}
#########################################################################
#
#                                                                 copydir
#
# input: 1) a directory to copy from
#        2) directory target
#
#########################################################################
sub copydir{
  my ($source, $target) = @_;
  my (@dirs, $file);

  &removelastslash($source);
  &removelastslash($target);

  if (!-e $target) {
    &makedir($target, 0750);
  }

  opendir SOURCE, $source || die "Failure in copydir";
  @dirs = readdir SOURCE;
  closedir SOURCE;

  foreach $file (@dirs) {
    if (($file eq ".") || ($file eq "..") || ($file eq "CVS")) {
      next;
    }

    if (-d "$source/$file") {
      &copydir("$source/$file", "$target/$file"); #recurse
    } else {
      if (!-e "$target/$file") {
	@stats = stat "$source/$file";

	# Get permissions info
	$mymode = $stats[2] & 0777;

	copy("$source/$file", "$target/$file");
	chmod($mymode, "$target/$file");
      }
    }
  }
}

###
### Let's do it, then.
###

$perlname = $ENV{GPERL};
$rootdir = &resolve(cwd(), $ENV{GROOTDIR});
$javadir = $ENV{GJAVA};
$swingdir = $ENV{GSWING};

# if we have a swingall.jar file, set the $swingjar
# string which will be appended into the config files
# built

if (-f "$swingdir/swingall.jar") {
  $swingjar = ":$swingdir/swingall.jar";
} else {
  $swingjar = "";
}

removelastslash($javadir);
removelastslash($swingdir);

print "Generating install scripts\n";

write_install("installClient.in", "installClient");
write_install("installWeb.in", "installWeb");
write_install("installServer.in", "installServer");

if (-f "$rootdir/jars/ganymedeServer.jar") {
print <<ENDCODA;
Done.

Ganymede is now configured for installation.  You can now run the
installServer, installClient, and installWeb scripts to install the
Ganymede software.

ENDCODA
} else {
print <<ENDCODA2;
Done.

The Ganymede distribution directory is now configured for compilation.
You can now cd to the src directory and run 'ant' followed by 'ant
jars' to compile Ganymede.

ENDCODA2
}
exit;
