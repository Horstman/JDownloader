#! /usr/bin/perl -w

# This script tries to find translatable strings in the source code
# It does not detect all strings!
# Must be called from main SVN directory or path needs to be passed as argument.

use File::Find;
use strict;
use utf8;

my $debug = 1; # set this to get verbose output.
my $path = $ARGV[1] || "src/jd";
my $rpath = $ARGV[1] || "ressourcen/jd/languages/";

main();

sub loadres
{
  my %res;
  foreach my $l ("en", "de")
  {
    my $file = "$rpath$l.loc";
    open FILE,"<",$file or die "Can't open $file\n";
    while(my $line = <FILE>)
    {
      if($line =~ /^(.*?)\s*=\s*(.*$)$/)
      {
        print STDERR "Mismatch for lang $l string $1: '$2' != '$res{$1}{$l}'\n" if($res{$1}{$l});
        $res{$1}{$l} = $2;
      }
      elsif($debug)
      {
        print STDERR "Error loading lang $l line: $line";
      }
    }
    close FILE;
  }
  return %res;
}

sub findfiles
{
  my $dir = $_[0] ? $_[0] : 'src';
  my @findres = ();
  File::Find::find({follow => 1, no_chdir => 1, wanted => sub
  {
    push(@findres, $_) if(-f _ && $_ =~ /\.java/ && !($_ =~/\.svn/));
  }}, $dir);
  return sort @findres;
}

sub main
{
  my %res;
  foreach my $file (findfiles($path))
  {
    my $prefix;
    my $lastline = "";
    open FILE,"<",$file or die;
    my $name = $file; $name =~ s/.*?src\///; $name =~ s/\//./g; $name =~ s/\.java$//;
    my $sname = $name; $sname =~ s/^.*\.//;
    print STDERR "Parsing $file ($name, $sname)\n" if $debug;
    while(my $line = <FILE>)
    {
      $line = "$line$lastline"; $lastline = "";
      $line =~ s/^\s*\/\/.*$//; # remove comments
      $line =~ s/^\s*\*.*$//; # remove comments
      $line =~ s/this\.getClass\(\)\.getName\(\) \+ "/"$name/g;
      $line =~ s/this\.getClass\(\)\.getSimpleName\(\)/"$sname"/g;
      $prefix = $1 if($line =~ s/[A-Z]+_PREFIX\s*=\s*"(.*?)"//);

      while($line =~ s/JDL\.LF?\("(.*?)",\s*"(.*?)"//)
      {
        print STDERR "Mismatch for $1: '$2' != '$res{$1}'\n" if($res{$1} && $res{$1} ne $2);
        $res{$1} = $2;
      }
      while($prefix && $line =~ s/JDL\.LF?\([A-Z]+_PREFIX\s*\+\s*"(.*?)",\s*"(.*?)"//)
      {
        my $k = "$prefix$1";
        print STDERR "Mismatch for $k: '$2' != '$res{$k}'\n" if($res{$k} && $res{$k} ne $2);
        $res{$k} = $2;
      }
      if($line =~ /JDL\.L/)
      {
        if($line =~ /,\w*$/)
        {
          $lastline = $line;
        }
        elsif($debug)
        {
          print STDERR "Can't parse following line in $file: $line";
        }
      }
    }
    close FILE;
  }
  my %ref = loadres();
  foreach my $k (sort keys %res)
  {
    my $refen = $ref{$k}{en};
    my $refde = $ref{$k}{de};
    if($refen && $res{$k} eq $refen)
    {
    }
    elsif($refde && $res{$k} eq $refde)
    {
      print STDERR "German reference: $k = $refde\n";
    }
    elsif($refen)
    {
      print STDERR "Mismatch in langfile for $k: '$refen' != '$res{$k}'\n";
    }
    else
    {
      print STDERR "Missing string in langfile $k: '$res{$k}'\n" if $debug;
    }
  }
  foreach my $k (sort keys %res)
  {
    print "$k = $res{$k}\n";
  }
}
