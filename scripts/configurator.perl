#!/opt/bin/perl5
#
# This script is designed to walk through the ganymede package
# and make all the build scripts.  It is run by the configure
# script in the root of the ganymede distribution.
#
# $Revision: 1.58 $
# $Date: 2001/06/27 19:33:10 $
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
#                                                          write_makefile
#
# input: $dir - absolute pathname directory to write makefile into.
#
#########################################################################

sub write_makefile {

    my ($dir) = @_;

    open(MAKEFILE, ">$dir/Makefile") || die("Can't create the $dir/Makefile");

    print MAKEFILE <<ENDMAKEFILE;
#
# Ganymede source makefile
#
# Jonathan Abbey, jonabbey\@arlut.utexas.edu
#

classfiles:
	\@echo "Building Ganymede sources"
	\@cd $rootdir/src/xml; \\
	./build
	\@cd $rootdir/src/Util; \\
	./build
	\@cd $rootdir/src/Qsmtp; \\
	./build
	\@cd $rootdir/src/jcrypt; \\
	./build
	\@cd $rootdir/src/crypto; \\
	./build
	\@cd $rootdir/src/md5; \\
	./build
	\@cd $rootdir/src/JDataComponent; \\
	./build
	\@cd $rootdir/src/JTree; \\
	./build
	\@cd $rootdir/src/JTable; \\
	./build
	\@cd $rootdir/src/server; \\
	./build
	\@cd $rootdir/src/client; \\
	./build
	\@cd $rootdir/src/password; \\
	./build
	\@cd $rootdir/src
	\@echo "Built Ganymede classes"

clean:
	\@echo "Removing class files (except gnu-regexp files)"
	\@find $classdir/arlut -name \*.class -exec rm {} \\; -print
	\@find $classdir/org -name \*.class -exec rm {} \\; -print
	\@find $classdir/com/jclark -name \*.class -exec rm {} \\; -print

cleanconfig:
	\@echo "Removing config.sh files"
	\@find $rootdir/src -name config.sh -exec rm {} \\; -print
	\@echo
	\@echo "Removed all config files.. re-run $rootdir/configure to regenerate."

jars:
	\@echo "Building server, client, and admin jar files"
	\@cd $classdir; \\
	./buildJar; \\
	./buildAdminJar; \\
	./buildServerJar; \\
	cp *.jar $rootdir/jars;
	\@echo "Finished generating jars in $rootdir/jars"

ENDMAKEFILE

    close(MAKEFILE);
}

#########################################################################
#
#                                                           write_rebuild
#
# input: $dir - absolute pathname directory to write rebuild script into.
#
#########################################################################

sub write_rebuild {

    my ($dir) = @_;

    open(REBUILDIN, "<$rootdir/scripts/rebuild.in") || die ("Can't read $rootdir/scripts/rebuild.in");
    open(REBUILDOUT, ">$dir/rebuild") || die("Can't create the $dir/rebuild");

    while (<REBUILDIN>){
	s/\/opt\/bin\/perl5/$perlname/g;
	print REBUILDOUT $_;
    }

    close(REBUILDOUT);
    close(REBUILDIN);

    chmod 0755, "$dir/rebuild";
}

#########################################################################
#
#                                                          write_syncjars
#
# input: $template - filename (no path) of the template to be copied
#        $target - absolute pathname of the target
#
#########################################################################

sub write_syncjars {

    my ($template, $target) = @_;

    open(SYNCIN, "<$rootdir/scripts/$template") || die ("Can't read $rootdir/scripts/$template");
    open(SYNCOUT, ">$target") || die("Can't create the $target");

    while (<SYNCIN>){
	s/\/opt\/bin\/perl5/$perlname/g;
	s/\<\#CLASSDIR\#\>/$classdir/g;
	print SYNCOUT $_;
    }

    close(SYNCOUT);
    close(SYNCIN);

    chmod 0755, "$target";
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
#                                                            write_config
#
# input: $dir - absolute pathname directory to write config.sh into.
#        $name - name of component being configured
#        $targetdir - where is the class directory for this package?
#
#########################################################################

sub write_config {

    my($dir, $name, $targetdir) = @_;

    open(CONFIGFILE, ">$dir/config.sh") || die("Can't create the $dir/config.sh");

    print CONFIGFILE <<ENDCONFIG;
#
# $name config.sh
#
# Auto-generated by $rootdir/configure
#

# Name of component package being built

COMPNAME="$name"

# Location of java compiler

JAVAC=$javadir/javac

# Location of directory containing java executables

JAVADIR=$javadir

# Master location for ganymede classes.. the java compiler
# will look here to find pre-compiled classes needed to compile
# code in this directory

CLASSDIR=$classdir$swingjar

# Target location for classes built from sources in this directory.
# This is just the root of the tree.. the javac compiler will
# place classes in a subdirectory of this directory according to
# the package name

TARGETDIR=$targetdir

ENDCONFIG

    close(CONFIGFILE);
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

# See if there's a user-defined target location
# for the classes. Otherwise, use default.
$classdir = $ENV{GCLASSDIR};

if ($classdir eq "") {
  $classdir = "$rootdir/src/classes";
} else {
  # If there is a user-set classes path, make sure it has 
  # the necessary directory structure and support files.
  &copydir("$rootdir/src/classes","$classdir");
}

removelastslash($javadir);
removelastslash($swingdir);

# First we need to put out all the config.sh files that the build and
# rebuild scripts depend on.  See the header for write_config() to
# identify the three pieces.

@configs=("$rootdir/src/xml/sax", "OMG XML SAX Parser Classes", "$classdir",
	  "$rootdir/src/xml/xp/util", "James Clark XP XML Parser Utility Classes", "$classdir",
	  "$rootdir/src/xml/xp/xml", "James Clark XP XML Parser", "$classdir",
	  "$rootdir/src/xml/xp/xml/parse", "James Clark XP XML Parser Classes", "$classdir",
	  "$rootdir/src/xml/xp/xml/parse/base", "James Clark XP XML Parser Base Classes", "$classdir",
	  "$rootdir/src/xml/xp/xml/parse/io", "James Clark XP XML Parser IO Classes", "$classdir",
	  "$rootdir/src/xml/xp/xml/parse/awt", "James Clark XP XML Parser AWT Classes", "$classdir",
	  "$rootdir/src/xml/xp/xml/output", "James Clark XP XML Writer Classes", "$classdir",
	  "$rootdir/src/xml/xp/xml/tok", "James Clark XP XML Parser Tokenizer Classes", "$classdir",
	  "$rootdir/src/xml/xp/xml/sax", "James Clark XP XML Parser SAX Driver Classes", "$classdir",
	  "$rootdir/src/Qsmtp", "Qsmtp Mail Class", "$classdir",
	  "$rootdir/src/jcrypt", "jcrypt Class", "$classdir",
	  "$rootdir/src/crypto", "SMB Cryptographic Hash Classes", "$classdir",
	  "$rootdir/src/md5", "md5 Classes", "$classdir",
	  "$rootdir/src/Util", "Ganymede Utility Classes", "$classdir",
	  "$rootdir/src/JTable", "Ganymede Table Classes", "$classdir",
	  "$rootdir/src/JTree", "Ganymede Tree Classes", "$classdir",
	  "$rootdir/src/JDataComponent", "Ganymede GUI Component Classes", "$classdir",
	  "$rootdir/src/server", "Ganymede Server Classes", "$classdir",
	  "$rootdir/src/client", "Ganymede Client Classes", "$classdir",
	  "$classdir", "Ganymede Jars", "$classdir",
	  "$rootdir/src/password", "Ganymede Sample Password Client", "$rootdir/src/password/classes",
	  "$rootdir/doc", "Javadoc", "$classdir");

print "\nGenerating config.sh files in source directories.\n";

while ($#configs >= 0) {
    write_config(shift @configs, shift @configs, shift @configs);
}

# Now we need to write out the rebuild scripts.  The only reason we're
# doing it here is that rebuild is written in perl, and we want to
# rewrite the header to properly specify the location of perl on this
# system.

@rebuilds=("$rootdir/src/xml/sax",
	   "$rootdir/src/xml/xp/util",
	   "$rootdir/src/xml/xp/xml",
	   "$rootdir/src/xml/xp/xml/parse/base",
	   "$rootdir/src/xml/xp/xml/parse/io",
	   "$rootdir/src/xml/xp/xml/parse/awt",
	   "$rootdir/src/xml/xp/xml/parse",
	   "$rootdir/src/xml/xp/xml/output",
	   "$rootdir/src/xml/xp/xml/tok",
	   "$rootdir/src/xml/xp/xml/sax",
	   "$rootdir/src/Util",
	   "$rootdir/src/JTable",
	   "$rootdir/src/JTree",
	   "$rootdir/src/server",
	   "$rootdir/src/client",
	   "$rootdir/src/JDataComponent");

print "Generating rebuild files in source directories.\n";

while ($#rebuilds >= 0) {
    write_rebuild(shift @rebuilds);
}

@sync=("sync_tree.admin.in",
       "$classdir/admin_classes/sync_tree",
       "sync_tree.client.in",
       "$classdir/client_classes/sync_tree",
       "sync_tree.server.in",
       "$classdir/server_classes/sync_tree",
       "sync_tree.gnu.server.in",
       "$classdir/server_classes/sync_tree.gnu");

print "Generating jar generation scripts.\n\n";

while ($#sync >= 0) {
    write_syncjars(shift @sync, shift @sync);
}

print "Generating $rootdir/src/Makefile\n";

write_makefile("$rootdir/src");

print "Generating install scripts\n";

write_install("installClient.in", "installClient");
write_install("installWeb.in", "installWeb");
write_install("installServer.in", "installServer");

print <<ENDCODA;
Done.

Ganymede is now configured for installation.  You can now run the
installServer, installClient, and installWeb scripts to install the
Ganymede software.

ENDCODA
exit;
