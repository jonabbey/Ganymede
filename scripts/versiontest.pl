# this script is used to validate that we have a new enough version of perl
die if $] < 5.003;
exit 0;
