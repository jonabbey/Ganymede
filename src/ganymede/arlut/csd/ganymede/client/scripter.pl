#!/usr/bin/perl

for $file (@ARGV) {
    print $file, "\n";

    open(IN, "<$file") || die "Couldn't open $file";
    open(OUT, ">$file.tmp") || die "Couldn't open $file.tmp";

    while(<IN>) {
	if (/^   Last Mod Date: \$Date\$/ ||
	    /^   Last Revision Changed: \$Rev\$/ ||
	    /^   Last Changed By: \$Author\$/ ||
	    /^   Last Commit: \$Format:%cd\$/ ||
	    /^   SVN URL: \$HeadURL\$/) {
	    next;
	} elsif (/You should have received a copy of the GNU General Public License/) {
	    inner:
	    while ($line = <IN>) {
		if ($line =~ /^\s*$/) {
		    last inner;
		}
	    }
	    
	    print OUT "   You should have received a copy of the GNU General Public License\n";
	    print OUT "   along with this program.  If not, see <http://www.gnu.org/licenses/>.\n\n";
	} elsif (/^\s+Copyright \(C\) 1996-20/) {
	    print OUT "   Copyright (C) 1996-2010\n";
	} else {
	    print OUT $_;
	}
    }

    close(OUT);
    close(IN);

    rename("$file.tmp", "$file");
}
