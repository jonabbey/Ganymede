package Config::Properties;

use strict;
use warnings;

our $VERSION = '0.56';

use IO::Handle;
use Carp;

{
    no warnings;
    sub _t_key($) {
	my $k=shift;
	defined($k) && length($k)
	    or croak "invalid property key '$k'";
    }

    sub _t_value($) {
	my $v=shift;
	defined $v
	    or croak "undef is not a valid value for a property";
    }

    sub _t_format($) {
	my $f=shift;
	defined ($f) && $f=~/\%s.*\%s/
	    or croak "invalid format '%f'";
    }

    sub _t_validator($) {
	my $v=shift;
	defined($v) &&
	    UNIVERSAL::isa($v, 'CODE') or
		croak "invalid property validator '$v'";
    }

    sub _t_file($) {
	my $f=shift;
	defined ($f) or
	    croak "invalid file '$f'";
    }
}

#   new() - Constructor
#
#   The constructor can take one optional argument "$defaultProperties"
#   which is an instance of Config::Properties to be used as defaults
#   for this object.
sub new {
    my $proto = shift;
    my $defaultProperties = shift;

    @_ and carp "use of PerlMode flag is deprecated";
    my $perlMode = shift;

    ref($proto)
	and carp "creating new Config::Properties objects from prototypes is deprecated";

    my $class = ref($proto) || $proto;
    my $self = { 'PERL_MODE' => $perlMode ? 1 : 0,
		 'defaults' => $defaultProperties,
		 'format' => '%s=%s',
		 'properties' => {} };
    bless $self, $class;

    return $self;
}

# set property only if its going to change the property value.
#
sub changeProperty {
    my ($self, $key, $new, $default) = @_;
    _t_key $key;
    _t_value $new;
    my $old=$self->getProperty($key, $default);
    if ($old ne $new) {
	$self->setProperty($key, $new);
	return 1;
    }
    return 0;
}

sub deleteProperty {
    my ($self, $key, $recurse) = @_;
    _t_key $key;

    delete $self->{properties}{$key}
	if exists $self->{properties}{$key};

    $self->{defaults}->deleteProperty($key, 1)
	if ($recurse and $self->{defaults});
}

#	setProperty() - Set the value for a specific property
sub setProperty {
    my ($self, $key, $value)=@_;
    _t_key $key;
    _t_value $value;

    defined(wantarray) and
	carp "warning: setProperty doesn't return the old value anymore";

    $self->{properties}{$key} = $value;
}

#       properties() - return a flated hash with all the properties
sub properties {
    my $self=shift;
    if (defined ($self->{defaults})) {
	my %p=($self->{defaults}->properties, %{$self->{properties}});
	return %p;
    }
    return %{ $self->{properties} }
}

#	getProperties() - Return a hashref of all of the properties
sub getProperties { return { shift->properties }; }
# sub getProperties { my $r={ shift->properties }; return $r; }


#	getFormat() - Return the output format for the properties
sub getFormat { shift->{format} }


#	setFormat() - Set the output format for the properties
sub setFormat {
    my ($self, $format) = @_;
    defined $format or $format='%s=%s';
    _t_format $format;
    $self->{format} = $format;
}

#	format() - Alias for get/setFormat();
sub format {
    my $self = shift;
    if (@_) {
	return $self->setFormat(@_)
    }
    $self->getFormat();
}


#       setValidator(\&validator) - Set sub to be called to validate
#                property/value pairs.
#                It is called &validator($property, $value, $config)
#                being $config the Config::Properties object.
sub setValidator {
    my ($self, $validator) = @_;
    _t_validator $validator;
    $self->{validator} = $validator;
}


#       getValidator() - Return the current validator sub
sub getValidator { shift->{validator} }


#       validator() - Alias for get/setValidator();
sub validator {
    my $self=shift;
    if (@_) {
	return $self->setValidator(@_)
    }
    $self->getValidator
}


#	load() - Load the properties from a filehandle
sub load {
    my ($self, $file) = @_;
    _t_file $file;
    1 while $self->process_line($file);
}


#        escape_key(string), escape_value(string), unescape(string) -
#               subroutines to convert escaped characters to their
#               real counterparts back and forward.

my %esc = ( "\n" => 'n',
	    "\r" => 'r',
	    "\t" => 't' );
my %unesc = reverse %esc;

sub escape_key {
    $_[0]=~s{([\t\n\r\\"' =:])}{
	"\\".($esc{$1}||$1) }ge;
    $_[0]=~s{([^\x20-\x7e])}{sprintf "\\u%04x", ord $1}ge;
    $_[0]=~s/^ /\\ /;
    $_[0]=~s/^([#!])/\\$1/;
    $_[0]=~s/(?<!\\)((?:\\\\)*) $/$1\\ /;
}

sub escape_value {
    $_[0]=~s{([\t\n\r\\])}{
	"\\".($esc{$1}||$1) }ge;
    $_[0]=~s{([^\x20-\x7e])}{sprintf "\\u%04x", ord $1}ge;
    $_[0]=~s/^ /\\ /;
}

sub unescape {
    $_[0]=~s/\\([tnr\\"' =:#!])|\\u([\da-fA-F]{4})/
	defined $1 ? $unesc{$1}||$1 : chr hex $2 /ge;
}


#	process_line() - read and parse a line from the properties file.
sub process_line {
    my ($self, $file) = @_;
    my $line=<$file>;

    defined $line or return undef;
    $line =~ /^\s*(\#|\!|$)/ and return 1;
    $self->{line_number}=$file->input_line_number;
    $line =~ s/\x0D*\x0A$//;

    # handle continuation lines
    my @lines;
    while ($line =~ /(\\+)$/ and length($1) & 1) {
	$line =~ s/\\$//;
	push @lines, $line;
	$line = <$file>;
	$line =~ s/\x0D*\x0A$//;
	$line =~ s/^\s+//;
    }
    $line=join('', @lines, $line) if @lines;

    my ($key, $value) = $line =~ /^
				  \s*
				  ((?:[^\s:=\\]|\\.)+)
				  \s*
				  [:=\s]
				  \s*
				  (.*)
				  $
				  /x
       or $self->fail("invalid property line '$line'");
	
    unescape $key;
    unescape $value;
    $self->{properties}{$key} =
	$self->validate($key, $value);

    return 1;
}

#       validate(key, value) - check if the property is valid.
#               calls the validator if it has been set.
sub validate {
    my ($self, $key, $value)=@_;
    my $validator = $self->validator;
    if ($validator) {
	return &{$validator}($key, $value, $self)
    }
    $value;
}


#       line_number() - number for the last line read from the configuration file
sub line_number { shift->{line_number} }


#       fail(error) - report errors in the configuration file while reading.
sub fail {
    my ($self, $error) = @_;
    die "$error at line ".$self->line_number()."\n";
}

#	_save() - Utility function that performs the actual saving of
#		the properties file to a filehandle.
sub _save {
    my ($self, $file) = @_;
    _t_file $file;

    my $wrap;
    eval {
	require Text::Wrap;
	$wrap=($Text::Wrap::VERSION >= 2001.0929);
    };
    unless ($wrap) {
	carp "Text::Wrap module is to old, version 2001.0929 or newer required: long lines will not be wrapped"
    }

    local($Text::Wrap::separator)=" \\\n";
    local($Text::Wrap::unexpand)=undef;
    local($Text::Wrap::huge)='overflow';
    local($Text::Wrap::break)=qr/(?<!\\) (?! )/;

    foreach (sort keys %{$self->{properties}}) {
	my $key=$_;
	my $value=$self->{properties}{$key};
	escape_key $key;
	escape_value $value;

	if ($wrap) {
	    $file->print( Text::Wrap::wrap( "",
					    "    ",
					    sprintf( $self->{'format'},
						     $key, $value ) ),
			  "\n" );
	}
	else {
	    $file->print(sprintf( $self->{'format'}, $key, $value ), "\n")
	}
    }
}


#	save() - Save the properties to a filehandle with the given header.
sub save {
    my ($self, $file, $header)=@_;
    _t_file($file);

    if (defined $header) {
	$header=~s/\n/# \n/sg;
	print $file "# $header\n#\n";
    }
    print $file '# ' . localtime() . "\n\n";
    $self->_save( $file );
}


#	store() - Synonym for save()
sub store { shift->save(@_) }


#	getProperty() - Return the value of a property key. Returns the default
#		for that key (if there is one) if no value exists for that key.
sub getProperty {
    my ($self, $key, $default)=@_;
    _t_key $key;

    if (exists $self->{properties}{$key}) {
	return $self->{properties}{$key}
    }
    elsif (defined $self->{defaults}) {
	return $self->{defaults}->getProperty($key, $default);
    }
    $default;
}


#	propertyName() - Returns an array of the keys of the Properties
sub propertyNames {
    my %p=shift->properties;
    keys %p;
}


#	list() - Same as store() except that it doesn't include a header.
#		Meant for debugging use.
sub list {
    my ($self, $file) = @_;
    _t_file $file;

    print $file "# -- listing properties --";
    $self->_save( $file );
}

#	setPerlMode() - Sets the value (true/false) of the PERL_MODE parameter.
sub setPerlMode {
    my ($self, $mode) = @_;
    carp "use of PerlMode flag is deprecated";
    return $self->{'PERL_MODE'} = (defined($mode) && $mode) ? 1 : 0;
}

#	perlMode() - Returns the current PERL_MODE setting (Default is false)
sub perlMode {
    my $self = shift;
    carp "use of PerlMode flag is deprecated";
    return $self->{'PERL_MODE'};
}

1;
__END__

=head1 NAME

Config::Properties - Read and write property files

=head1 SYNOPSIS

  use Config::Properties;

  # reading...

  open PROPS, "< my_config.props"
    or die "unable to open configuration file";

  my $properties = new Config::Properties();
  $properties->load(*PROPS);

  $value = $properties->getProperty( $key );


  # saving...

  open PROPS, "> my_config.props"
    or die "unable to open configuration file for writing";

  $properties->setProperty( $key, $value );

  $properties->format( '%s => %s' );
  $properties->store(*PROPS, $header );


=head1 DESCRIPTION

Config::Properties is a near implementation of the
java.util.Properties API.  It is designed to allow easy reading,
writing and manipulation of Java-style property files.

The format of a Java-style property file is that of a key-value pair
seperated by either whitespace, the colon (:) character, or the equals
(=) character.  Whitespace before the key and on either side of the
seperator is ignored.

Lines that begin with either a hash (#) or a bang (!) are considered
comment lines and ignored.

A backslash (\) at the end of a line signifies a continuation and the
next line is counted as part of the current line (minus the backslash,
any whitespace after the backslash, the line break, and any whitespace
at the beginning of the next line).

The official references used to determine this format can be found in
the Java API docs for java.util.Properties at
L<http://java.sun.com/j2se/1.3/docs/api/index.html>.

When a property file is saved it is in the format "key=value" for each
line. This can be changed by setting the format attribute using either
$object->format( $format_string ) or $object->setFormat(
$format_string ) (they do the same thing). The format string is fed to
printf and must contain exactly two %s format characters. The first
will be replaced with the key of the property and the second with the
value. The string can contain no other printf control characters, but
can be anything else. A newline will be automatically added to the end
of the string. You an get the current format string either by using
$object->format() (with no arguments) or $object->getFormat().

=over 4

*** DEPRECATED!!! ***

If a true third parameter is passed to the constructor, the
Config::Properties object be created in PERL_MODE. This can be set at
any time by passing a true or false value into the setPerlMode()
instance method. If in PERL_MODE, the behavior of the object may be
expanded, enhanced and/or just plain different than the Java API spec.

The following is a list of the current behavior changed under PERL_MODE:

* Ummm... nothing yet.

The current (true/false) value of PERL_MODE can be retrieved with the
perlMode instance variable.

--- As PERL_MODE has not ever done anything its usage has been
deprecated ---

*** DEPRECATED!!! ***

=back

=head1 METHODS

C<Config::Property> objects have this set of methods available:

=over 4

=item Config::Properties-E<gt>new()

=item Config::Properties-E<gt>new($defaults)

creates a new Config::Properties object. The optional C<$defaults>
parameter can be used to pass another Config::Properties object
holding default property values.

=item $p-E<gt>getProperty($k, $default)

return property C<$k> or C<$default> if property C<$k> is not
defined.

=item $p-E<gt>setProperty($k, $v)

set property C<$k> value to C<$v>.

=item $p-E<gt>changeProperty($k, $v)

=item $p-E<gt>changeProperty($k, $v, $default)

method similar to C<setPropery> but that does nothing when the new
value is equal to the one returned by C<getProperty>.

An example shows why it is useful:

  my $defaults=Config::Properties->new();
  $defaults->setProperty(foo => 'bar');

  my $p1=Config::Properties->new($defaults);
  $p2->setProperty(foo => 'bar');
  $p2->store(FILE1); foo gets saved on the file

  my $p2=Config::Properties->new($defaults);
  $p2->changeProperty(foo => 'bar'); # does nothing!
  $p2->store(FILE2); # foo doesn't get saved on the file

=item $p-E<gt>deleteProperty($k)

=item $p-E<gt>deleteProperty($k, $recurse)

deletes property $k from the object.

If C<$recurse> is true, it also deletes any C<$k> property from the
default properties object.

=item $p-E<gt>properties

returns a flatten hash with all the property key/value pairs, i.e.:

  my %props=$p->properties;

=item $p-E<gt>getProperties

returns a hash reference with all the properties.

=item $p-E<gt>propertyNames;

returns the names of all the properties.

=item $p-E<gt>load($file)

loads its properties from the open file C<$file>.

=item $p-E<gt>save($file)

=item $p-E<gt>save($file, $header)

=item $p-E<gt>store($file)

=item $p-E<gt>store($file, $header)

Save to the open file C<$file> the properties on the object. Default
properties are not saved.

=item $p-E<gt>getFormat()

=item $p-E<gt>setFormat($f)

get/set the format string used when saving the object to a file.

=back

=head1 SEE ALSO

Java docs for C<java.util.Properties> at
L<http://java.sun.com/j2se/1.3/docs/api/index.html>.

L<Config::Properties::Simple> for a simpler alternative interface to
L<Config::Properties>.

=head1 AUTHORS

C<Config::Properties> was originally developed by Randy Jay Yarger. It
was mantained for some time by Craig Manley and recently it has passed
hands to Salvador Fandiño <sfandino@yahoo.com>.

=cut
