#!/opt/bin/perl5 -w
#
# This will compile all .java files in the current directory
# that have newer dates than the corresponding .class files.
#

$class_location = "/home/broccol/gash2/classes/arlut/csd/ganymede/client";
$destination_dir = "/home/broccol/gash2/classes";
$javac = "/opt/bin/javac -deprecation";

umask 0113;

#if (-e ".lastbuild") {
#  $lastbuild = `cat .lastbuild`;
#  chomp($lastbuild);
#  if ($lastbuild eq "optimized") {
#    die "The last build was optimized.  If you want to do incremental builds, you must do a full build first.\n\n";
#  }
#} else {
#  print "Can't find last build information, assuming it was not optimized.\n";
#}


opendir CURRENT, "." or die "Can't open current directory, that's no good.\n\n";
@files = readdir(CURRENT);
closedir CURRENT;

$file_list = "";

@javafiles = grep /\.java$/, @files;

foreach $file (@javafiles) {

  if ($file eq "template.java")
    {
      print "Skipping: $file\n";
      next;
    }


  $mtime = (stat ($file))[9];

  @file = split /\./, $file;
  $class_file = "$file[0].class";

  $class_mtime = (stat ("$class_location/$class_file"))[9];

  if ($mtime - $class_mtime >= 0)
    {
      $file_list = $file_list." $file";
    }

}

if ($file_list eq "") {
  print "There is nothing to compile.\n";
} else {
  print "Compiling $file_list\n";
  system "$javac -d $destination_dir $file_list\n" || print "Could not finish compiling.\n";
  print "Done.\n";
}
