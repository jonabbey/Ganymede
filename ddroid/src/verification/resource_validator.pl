#!/usr/bin/perl
#
# resource_validator.pl
#
# Last Revision Changed: $Rev$
# Last Changed By: $Author$
# Last Mod Date: $Date$
#
# This perl script is designed to scan through the Directory Droid
# source files in search of localization string retrievals, and to
# match them against the property file resources in the Directory
# Droid resources tree.
#
# This code does not attempt to be comprehensive in its operation..
# there are a number of significant simplifying assumptions:
#
# To wit, that all localization retrievals will be pulled from
# a resource bundle whose name is identical to that of the fully qualified
# class name of the containing file, and that all retrievals will
# be made using a TranslationService variable named 'ts', with a
# localize method named 'l'.
#
# If these assumptions hold, then resource_validator.pl will check all
# source files under this directory against the appropriate
# localization property definitions to insure that all localization keys
# exist, and that the ts.l() call provides the proper number of parameters
# to match the resource definition in the property file.
#
######################################################################

$scan_root = "/home/broccol/ganymede/ddroid/src/ddroid/arlut/csd/ddroid";
$properties_root = "/home/broccol/ganymede/ddroid/src/resources/arlut/csd/ddroid";

#########################################################################
#
#                                                              find_javas
# input: $dir - The directory we're scanning
#
# uses: global @javafiles array
#
# output: Adds to @javafiles all of the files ending in .java that are
#         found under $dir, by way of a recursive process
#
#########################################################################

sub find_javas {
  my ($dir) = @_;

  my (@elements, @filtered, $item);

  opendir (DIR, "$dir") || die "Couldn't open source tree $dir";
  @elements = readdir(DIR);
  closedir(DIR);

  @filtered = grep (!/^\./, @elements);

  foreach $item (@filtered) {
    if (-f "$dir/$item" && $item =~ /\.java$/) {
      push @javafiles, "$dir/$item";
    }

    if (-d "$dir/$item") {
      find_javas("$dir/$item");
    }
  }
}

#########################################################################
#
#                                                            examine_java
# input: $jfile - the java file to examine
#
# output: returns true if the java file makes no reference to any
#         TranslationService ts.l localization calls, or if it does
#         and the resources were all successfully found.
#
#########################################################################

sub examine_java {
  my ($jfile) = @_;

  $jfile =~ /\/([^\/]*)\.java$/;

  $classname = $1;

  open (IN, "<$jfile") || die "Couldn't open $jfile";
  while (<IN>) {
    if (/^\s*package\s*([^;]*);/) {
      $package = $1;
    }

    if (/TranslationService\.getTranslationService\(\"([^\"]*)\"\)/) {
      $propname = $1;

      print "\nFound use of property bundle $propname in Class $package.$classname\n";

      if ($propname ne "$package.$classname") {
	print "***  Error, property/classname mismatch ***\n";
      }
    }

    if (/ts\.l\(\"([^\"]*)\"/) {
      $key = $1;

      print "Found ts.l() call for key $key\n";
    }
  }
  close(IN);
}

find_javas($scan_root);

foreach $file (@javafiles) {
  examine_java($file);
#  $file =~ /^$scan_root\/?(.*)/;
#  $relative_file = $1;
#  $property = $relative_file;
#  $property =~ s/\.java$/\.properties/;
#  print "$relative_file -- $property\n";
}

# foreach $file (@javafiles) {
#   print $file . "\n";
# }

# print "\nWe found " . scalar(@javafiles) . " java files\n";
