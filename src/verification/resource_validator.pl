#!/usr/bin/env perl
#
# resource_validator.pl
#
# Last Revision Changed: $Rev$
# Last Changed By: $Author$
# Last Mod Date: $Date$
#
# This perl script is designed to scan through the Ganymede
# source files in search of localization string retrievals, and to
# match them against the property file resources in the Ganymede
# resources tree.
#
# This code does not attempt to be comprehensive in its operation..
# there are a number of significant simplifying assumptions:
#
# To wit, that all localization retrievals will be pulled from a
# resource bundle whose name is identical to that of the fully
# qualified class name of the containing file, and that all retrievals
# will be made using a class-local static or member TranslationService
# variable named 'ts', with a localize method named 'l'.
#
# If these assumptions hold, then resource_validator.pl will check all
# source files under this directory against the appropriate
# localization property definitions to insure that all localization keys
# exist, and that the ts.l() call provides the proper number of parameters
# to match the resource definition in the property file.
#
# Jonathan Abbey
# Applied Research Laboratories, The University of Texas at Austin
#
######################################################################

use FindBin;
use lib "$FindBin::Bin/Modules";

use Config::Properties;

$| = 1;				# don't buffer stdout, to assist debug

if (defined $ARGV[0]) {
  $scan_root = "$ARGV[0]/ganymede/arlut/csd/ganymede";
  $properties_root = "$ARGV[0]/resources";
} else {
  $scan_root = "/home/broccol/ganymede/ganymede/src/ganymede/arlut/csd/ganymede";
  $properties_root = "/home/broccol/ganymede/ganymede/src/resources";
}

$showall_props = 0;
$showallfiles = 0;

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
#                                                             countparams
# input: $string - The property template
#
# output: Returns a count (between 0 and 9) of the number of template
# parameters present in the template pattern.
#
#########################################################################

sub countparams {
  my ($string) = @_;

  # Because template substitution points may be reused in a
  # localization string, we need to actually examine the digits in the
  # {0}, {1}, {2 Date} style substitution points to find the index of
  # the highest substitution point.

  my $count = 0;

  while ($string =~ /[^\\]\{(\d)/ || $string =~ /^\{(\d)/){
    if ($1 + 1 > $count) {
      $count = $1 + 1;
    }
    $string =~ s/\{[^\}]+\}/<>/;
  }

  return $count;
}


#########################################################################
#
#                                                        inchoverjavaargs
#
# This subroutine is designed to help us validate the number of
# arguments that come after the property key in a ts.l() method call
# in Ganymede java code.
#
# We pass in two parameters: the first is a string containing the remaining
# characters on the same line as the ts.l() call, after the closing double
# quote character of the property key.  The second is a filehandle that
# we can pull more lines from if necessary, until we hit the closing
# parenthesis.
#
# As you can guess, this subroutine is extremely specialized.. it's
# really a context sensitive baby parser, and should be viewed as an
# integral part of the examine_java() subroutine.
#
# input: $string, $filehandle
#
# output: Returns an array containing first, the count of the number
# of distinct arguments that we find before we hit the closing paren
# for the ts.l() method call, and second, the last string we inched
# over, back to examine_java() for it to continue chewing on.
#
#
#########################################################################

sub inchoverjavaargs {
  my ($string, $fh, $line_number) = @_;

  my ($done, @mode, $arg_count);

  # @mode is actually a mode stack.. the following mode states are
  # defined:
  #
  # 0 - neutral, what we're in after the closing quotation
  # mark.. implies that we're not in a quoted string, not in a
  # comment, not in an interior paren'd section
  #
  # 1 - paren.. means we've hit an open paren, and we're going to wait
  # until we're back in mode 1 and we see a close paren before we pop
  # this state off the stack
  #
  # 2 - single quote.. means we've hit a single quote mark and that we
  # have to wait until we're in mode 2 and see another single quote mark
  # to pop this state off the stack
  #
  # 3 - double quote.. means we've hit a double quote mark and that we
  # have to wait until we're in mode 3 and see another double quote mark
  # to pop this state off the stack
  #
  # 4 - / mode.. we've hit a slash in mode 0
  #
  # 5 - /* comment.. we've entered a traditional C-style java comment
  # section and we'll continue to be until we've seen a */ to pop this
  # mode
  #
  # 6 - // comment.. we've hit a pair of forward slashes, indicating
  # that we're in a comment until we hit the end of line, whereupon we'll
  # pop this mode
  #
  # 7 - * mode.. we've hit a * in mode 5
  #
  # 8 - \ mode.. we hit a backslash in mode 0,1,2,3,4, so we're doing a backslash
  # escape, and the next character won't actually count for anything
  #
  # We'll count the arguments in the source code by counting commas
  # that we hit in mode 0.

  push(@mode, 0);

  $done = 0;
  $arg_count = 0;

  while (!$done) {
    foreach $char (split //, $string) {
      if ($done) {
	next;
      }

      $topmode = $mode[$#mode];

      if ($topmode == 0) {
	if ($char eq ",") {
	  $arg_count = $arg_count + 1;
	  next;
	}

	if ($char eq ")") {
	  $done = 1;
	  next;
	}

	if ($char eq "(") {
	  push(@mode, 1);
	  next;
	}

	if ($char eq "'") {
	  push(@mode, 2);
	  next;
	}

	if ($char eq "\"") {
	  push(@mode, 3);
	  next;
	}

	if ($char eq "/") {
	  push(@mode, 4);
	  next;
	}

	if ($char eq "\\") {
	  push(@mode, 8);
	  next;
	}
      } elsif ($topmode == 1) {
	if ($char eq ")") {
	  pop(@mode);
	  next;
	}

	if ($char eq "(") {
	  push(@mode, 1);
	  next;
	}

	if ($char eq "'") {
	  push(@mode, 2);
	  next;
	}

	if ($char eq "\"") {
	  push(@mode, 3);
	  next;
	}

	if ($char eq "/") {
	  push(@mode, 4);
	  next;
	}

	if ($char eq "\\") {
	  push(@mode, 8);
	  next;
	}
      } elsif ($topmode == 2) {
	if ($char eq ("'")) {
	  pop(@mode);
	  next;
	}

	if ($char eq "\\") {
	  push(@mode, 8);
	  next;
	}
      } elsif ($topmode == 3) {
	if ($char eq ("\"")) {
	  pop(@mode);
	  next;
	}

	if ($char eq "\\") {
	  push(@mode, 8);
	  next;
	}
      } elsif ($topmode == 4) {
	if ($char eq "/") {
	  pop(@mode);
	  push(@mode, 6);
	  next;
	} elsif ($char eq "*") {
	  pop(@mode);
	  push(@mode, 5);
	  next;
	}

	pop(@mode);		# guess we're not going into a comment mode after all..

	if ($char eq "\\") {
	  push(@mode, 8);
	  next;
	}
      } elsif ($topmode == 5) {
	if ($char eq "*") {
	  push(@mode, 7);
	  next;
	}
      } elsif ($topmode == 6) {
	next;			# skip everything until end of line
      } elsif ($topmode == 7) {
	if ($char eq "/") {
	  pop(@mode);		# take off * mode
	  pop(@mode);		# take off /* comment mode
	} else {
	  pop(@mode);		# take off * mode
	}
      } elsif ($topmode == 8) {
	pop(@mode);		# we've escaped whatever single char, so we can drop again
      }
    }

    if (!$done) {
      $string = <$fh>;
      $line_number = $line_number + 1;

      if ($topmode == 6) {
	pop(@mode);		# no longer in // mode
      }
    }
  }

  return ($arg_count, $string, $line_number);
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

  my ($properties, $sourcename, $mode, $package, $propname, $proppath, $working, $key);
  my ($value, $prop_param_count, $rest, $arg_count);
  my (%seen, $hashref, $prop);

  my $result = 0;
  my $prop_loaded = 0;

  my $line_number = 0;

  $jfile =~ /\/([^\/]*)\.java$/;

  $sourcename = $1;

  $mode = 0;

  if ($showallfiles)
    {
      print "FILE: $jfile\n";
    }

  open (IN, "<$jfile") || die "Couldn't open $jfile";
  while (<IN>) {
    $line_number = $line_number + 1;

    if (/^\s*package\s*([^;]*);/) {
      $package = $1;
    }

    if (/TranslationService\.getTranslationService\(\"([^\"]*)\"\)/) {
      $propname = $1;

      if ($propname ne "$package.$sourcename") {
	print "$propname in $sourcename\n";
      } else {
	print "$propname\n";
      }

      $proppath = $propname;

      $proppath =~ s/\./\//g;

      if (!open(PROPS, "< $properties_root/$proppath.properties")) {
	print "*** Error, couldn't load properties file $properties_root/$proppath.properties ***\n";
	return;
      }

      $prop_loaded = 1;
      $properties = new Config::Properties();
      $properties->load(*PROPS);
      close(PROPS);
    }

    # Now let's do a regexp to see if we can find a ts.l() call and
    # its first parameter (the prop key) in double quotes.  We'll
    # worry about additional parameters thereafter.

    if (/ts\.l\(\"([^\"]*)\"/) {
      $working = $_;
      $key = $1;

      $seen{$key} = 1;

      $value = $properties->getProperty($key);

      # make sure we don't have any non-doubled single quotes in this property

      if ($value =~ /[^']'[^']/) {
	print "*** Warning, non-escaped single quote in property $key\n";
      }

      $prop_param_count = countparams($value);

      # okay, now we need to count the parameters provided.

      # first, get the remnants of the line after the ts.l method
      # call and the key parameter..

      $working =~ /ts\.l\(\"[^\"]*\"(.*)/;

      $rest = $1;

      # now we need to do a mode-sensitive scan loop looking for
      # commas at the top level.. the inchoverjavaargs() subroutine
      # takes care of that for us.. if it has to slurp in successive lines
      # to make it to the end of the ts.l() call, we'll take the last string
      # that it processed and redo our loop looking at that as our next line
      # to search for another ts.l() call.

      if ($value eq "") {
	print "*** Error, couldn't find property for key $key on line $line_number!!! ***\n";
      } else {

	($arg_count, $new_string, $line_number) = inchoverjavaargs($rest, *IN, $line_number);

        if ($showall_props) {
	  print "Found ts.l($key) call, property takes $prop_param_count params, is \"$value\"\n";
	}

	if ($arg_count < $prop_param_count) {
	  print "Error, ts.l($key) call takes $prop_param_count params in properties, but has $arg_count args in java code on line $line_number\n";
	  $result = 1;
	} else if ($arg_count > $prop_param_count) {
	  print "Warning, ts.l($key) call has more parameters ($arg_count) than are needed for the property ($prop_param_count).  Line number $line_number\n";
	}

	if (defined $new_string) {
	  $_ = $new_string;
	  $line_number = $line_number - 1; # since we're redoing this line in the loop
	  redo;
	}
      }
    }
  }
  close(IN);

  # we've finished scanning this java file.. now, did we get any
  # properties in the property file that weren't used in the source
  # code?

  if ($prop_loaded) {
    $hashref = $properties->getProperties();

    foreach $prop (keys %$hashref) {
      if (!defined $seen{$prop}) {
	print "  Warning, property $prop not used\n";
      }
    }
  }

  return $result;
}

$errors_found = 0;

find_javas($scan_root);

foreach $file (@javafiles) {
  if (examine_java($file)) {
    $errors_found = 1;
  }
}

if (!$errors_found) {
  print "No problems found\n";
  exit 0;
} else {
  exit 1;
}
