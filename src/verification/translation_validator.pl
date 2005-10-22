#!/usr/bin/env perl
#
# translation_validator.pl
#
# Last Revision Changed: $Rev$
# Last Changed By: $Author$
# Last Mod Date: $Date$
#
# This script is a partner to resource_validator.pl.  Where
# resource_validator.pl validates the Java sources in the Ganymede
# tree against the default internationalization/localization
# properties files, translation_validator.pl cross-checks specific
# language translation files against the default property files.
#
# The output of this script is a report listing translation languages
# that have been found in the Ganymede resource tree, with checks
# to make sure that translated resource strings match the default resource
# strings in terms of arguments.
#
# A translation percentage report is also generated for each language.
#
# Jonathan Abbey
# Applied Research Laboratories, The University of Texas at Austin
#
######################################################################

use File::Find;
use FindBin;
use lib "$FindBin::Bin/Modules";

use Config::Properties;

$| = 1;				# don't buffer stdout, to assist debug

if (defined $ARGV[0]) {
  $properties_root = "$ARGV[0]/resources";
} else {
  $properties_root = "/home/broccol/ganymede/ganymede/src/resources";
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
#                                                          find_languages
#
# output: Fills the global %langs hash with all of the translation
# extension ISO codes that we have found property file localizations
# for.
#
# Fills the global @properties array with all of the non-translated
# localization files under the resource tree.
#
#########################################################################

sub find_languages {
  find sub {if (/_(.._?.?.?)\.properties$/) {$langs{$1} = 1;}}, $properties_root;
  find sub {if (/\.properties$/ && !/_(.?.?)_?.?.?\.properties$/) {push @properties, $File::Find::name}}, $properties_root;
}

#########################################################################
#
#                                               generate_justified_string
#
# input: ($line_length, $left, $right)
#
# output: returns a string of a fixed length that left-justifies $left
# and right-justifies $right.
#
#########################################################################

sub generate_justified_string {
  my ($line_length, $left, $right) = @_;
  my ($fill, $str);

  $fill = $line_length - length($left) - length($right);
  $str = $left . " "x$fill . $right;
  return $str;
}

#########################################################################
#
#                                              generate_percentage_string
#
# input: ($numerator, $denominator)
#
# output: returns a four digit percentage string
#
#########################################################################

sub generate_percentage_string {
  my ($numerator, $denominator) = @_;
  my $str;

  $str = sprintf ("%2.2f", (100 * $numerator / $denominator));

  if ($str =~ /^.\./) {
    $str = "0$str";
  }

  return $str . "%";
}

#########################################################################
#
#                                                      scan_for_languages
#
# output: Fills the global %langs hash with all of the translation
# extension ISO codes that we have found property file localizations
# for.
#
#########################################################################

sub scan_for_languages {

  my ($default_count, $translated_count);
  my ($property_name);
  my ($default_name, $translated_name, $default_properties, $translated_properties);
  my ($default_hashref, $translated_hashref);
  my ($translated_key);
  my (@local);

  foreach $default_name (@properties) {

    $default_name =~ /$properties_root\/(.*)\.properties$/;
    $property_name = $1;
    $property_name =~ s/\//\./g;

    if (!open(PROPS, "< $default_name")) {
      print "*** Error, couldn't load properties file $default_name ***\n";
    } else {
      $default_properties = new Config::Properties();
      $default_properties->load(*PROPS);
      $default_hashref = $default_properties->getProperties();
      $default_count = scalar(keys(%$default_hashref));
      $total_default_count = $total_default_count + $default_count;
      close(PROPS);
    }

    foreach $language (keys(%langs)) {
      $translated_name = $default_name;
      $translated_name =~ s/\.properties$/_$language.properties/;
      @local = ();

      if (!open(PROPS, "< $translated_name")) {
        push (@{$results{$language}}, generate_justified_string(90, $property_name, "0/$default_count      0%"));
      } else {
	$translated_properties = new Config::Properties();
	$translated_properties->load(*PROPS);
	$translated_hashref = $translated_properties->getProperties();
	$translated_count = scalar(keys(%$translated_hashref));
        $total_translated_count{$language} = $total_translated_count{$language} + 1;

	close(PROPS);

        foreach $translated_key (keys(%$translated_hashref)) {
	  $translated_string = $translated_properties->getProperty($translated_key);
	  $default_string = $default_properties->getProperty($translated_key);

	  if ($default_string eq "") {
            push (@local, generate_justified_string(60, "  $translated_key", "unrecognized"));
	    $translated_count = $translated_count - 1;
	  } elsif (countparams($default_string) != countparams($translated_string)) {
	    push (@local, generate_justified_string(60, "  $translated_key", "bad arguments"));
	    $translated_count = $translated_count - 1;
	  }
	}

        $total_translated_property_count{$language} = $total_translated_property_count{$language} + $translated_count;
        push(@{$results{$language}}, generate_justified_string(90, $property_name, "$translated_count/$default_count  " . generate_percentage_string($translated_count, $default_count)));
	push(@{$results{$language}}, @local);
      }
    }
  }
}

# print "Scanning for languages:\n\n";

find_languages();

# print "Languages found:\n\n";
#
# foreach $key (keys %langs) {
#   print "\t$key\n";
#   $results{$key} = [];
# }

scan_for_languages();

print "\nLanguage totals:\n";

foreach $key (keys %langs) {

  print "\n------------------------------------------------------------\n";
  print "$key:\n\n";

  foreach $item (@{$results{$key}}) {
    print "$item\n";
  }

  $percent_string = generate_percentage_string($total_translated_property_count{$key},
					       $total_default_count);


  print "\n  $total_translated_count{$key} out of ". scalar(@properties) . " total property files translated to $key\n";
  print "  $percent_string total property strings translated to $key (" . $total_translated_property_count{$key} . "/" . $total_default_count . ")\n";
  print "\n------------------------------------------------------------\n";
}
