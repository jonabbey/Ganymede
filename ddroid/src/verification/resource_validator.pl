#!/opt/bin/perl
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

use FindBin;
use lib "$FindBin::Bin/Modules";

use Config::Properties;

if (defined $ARGV[0]) {
  $scan_root = "$ARGV[0]/ddroid/arlut/csd/ddroid";
  $properties_root = "$ARGV[0]/resources";
} else {
  $scan_root = "/home/broccol/ganymede/ddroid/src/ddroid/arlut/csd/ddroid";
  $properties_root = "/home/broccol/ganymede/ddroid/src/resources";
}

$showall_props = 0;

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

  # okay, this is super lame.. just going to look for {0}, {1}, etc.,
  # keeping a counter as we substitute the templatization points out
  # for <>.  I'd do this better if I didn't have such an allergy to
  # doing manual character loops in perl. TMTOWTDI.

  my $count = 0;

  while ($string =~ /[^\\]\{\d/ || $string =~ /^\{\d/){
    $count = $count + 1;
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
# in Directory Droid java code.
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
  my ($string, $fh) = @_;

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

      if ($topmode == 6) {
	pop(@mode);		# no longer in // mode
      }
    }
  }

  return ($arg_count, $string);
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

  my ($properties, $classname, $mode, $package, $propname, $proppath, $working, $key);
  my ($value, $prop_param_count, $rest, $arg_count);

  my $result = 0;

  $jfile =~ /\/([^\/]*)\.java$/;

  $classname = $1;

  $mode = 0;

  open (IN, "<$jfile") || die "Couldn't open $jfile";
  while (<IN>) {
    if (/^\s*package\s*([^;]*);/) {
      $package = $1;
    }

    if (/TranslationService\.getTranslationService\(\"([^\"]*)\"\)/) {
      $propname = $1;

      if ($showall_props) {
	print "\nFound use of property bundle $propname in Class $package.$classname\n\n";
      } else {
	print "$package.$classname\n";
      }

      if ($propname ne "$package.$classname") {
	print "***  Error, property/classname mismatch ***\n";
	return;
      }

      $proppath = $propname;

      $proppath =~ s/\./\//g;

      if (!open(PROPS, "< $properties_root/$proppath.properties")) {
	print "***  Error, couldn't load properties file $properties_root/$proppath.properties ***\n";
	return;
      }

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

      $value = $properties->getProperty($key);
      $prop_param_count = countparams($value);

      # okay, now we need to count the parameters provided.. this will
      # be more than a bit tricky.  we'll need to find the end of the
      # key string, look for a comma, then keep reading characters
      # until we get to the closing parenthesis, taking into account
      # backslash escapes, comment sections, quotation marks of both
      # kinds, and other mode-changing thingies.  this will be
      # especially tricky because we might have to jump across one or
      # more newlines before we find the closing paren.  We might make
      # the simplifying assumption that the additional lines we slurp
      # in won't have a ts.l() call on them, if we've already
      # determined that we have to cross the newline boundary, or we
      # could use the perl redo function to jump back to the interior
      # of our while body and continue regexp processing anew after
      # we've found the end of one call.

      # first, get the remnants of the line after the ts.l method
      # call and the key parameter..

      $working =~ /ts\.l\(\"[^\"]*\"(.*)/;

      $rest = $1;

      # now we need to do a mode-sensitive scan loop looking for
      # commas at the top level.. things that will make us jump into
      # another mode includes open parens, // comment introducers,
      # single quotes, and double quotes.. when we hit one of them,
      # we'll push that mode char onto a stack and keep reading char
      # by char until we find the matching exit from that mode

      if ($value eq "") {
	print "***  Error, couldn't find property for key $key!!! ***\n";
      } else {

	($arg_count, $new_string) = inchoverjavaargs($rest, *IN);

        if ($showall_props) {
	  print "Found ts.l($key) call, property takes $prop_param_count params, is \"$value\"\n";
	}

	if ($arg_count != $prop_param_count) {
	  print "Error, ts.l($key) call takes $prop_param_count params in properties, but has $arg_count args in java code\n";
	  $result = 1;
	}

	if (defined $new_string) {
	  $_ = $new_string;
	  redo;
	}
      }
    }
  }
  close(IN);
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
