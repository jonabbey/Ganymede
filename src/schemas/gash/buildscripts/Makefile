#***********************************************************************
#
#             ARL:UT Group Administration Shell Package
#                         September 13, 1994
#
#  Copyright 1994. The University of Texas at Austin (UTA).  All rights
#  reserved.  By using this software the USER indicates that he or she
#  has read, understood, and will comply with the following:
#
#  -UTA hereby grants USER nonexclusive permission to use, copy, and/or
#  modify this software for internal purposes only.  Any non-internal 
#  distribution, including commercial sale or license, of this software,
#  copies of the software, its associated documentation, and/or
#  modifications of either is strictly prohibited without the prior
#  consent of UTA.  Title to copyright to this software and its 
#  associated documentation shall at all times remain with UTA. 
#  Appropriate copyright notice shall be placed on all software copies,
#  and a complete copy of this notice shall be included in all copies
#  of the associated documentation.  No right is granted to use in 
#  advertising, publicity, or otherwise any trademark, service mark,
#  or the name of UTA.
#
#  -This software and any associated documentation is provided "as is",
#  and UTA MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED,
#  INCLUDING THOSE OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR
#  PURPOSE, OR THAT USE OF THE SOFTWARE, MODIFICATIONS, OR ASSOCIATED
#  DOCUMENTATION WILL NOT INFRINGE ANY PATENTS, COPYRIGHTS, TRADEMARKS 
#  OR OTHER INTELLECTUAL PROPERTY RIGHTS OF A THIRD PARTY.  UTA, the
#  University of Texas System, its Regents, officers, and employees 
#  shall not be liable under any circumstances for any direct, indirect,
#  special, incidental, or consequential damages with respect to any
#  claim by USER or any third party on account of or arising from the
#  use, or inability to use, this software or its associated 
#  documentation, even if UTA has been advised of the possibility of
#  those damages.
#
#***********************************************************************

#
# Welcome to the wonderful world of GASH makefiles. This will will have a
# list of variables so that they can easily be moved. Following this list
# will be the actual dependences and how to make the correct maps from the
# original information files.
#
# Last modified by Pug on 9/14/94.
#
# The B variable tells NIS weither to use DNS over NIS or not.
# B=-b means user DNS over NIS.
B=-b
#B=
#
# DIR is the location of the files such as services, networks, etc. 
# (Note, netmasks is located in SRC to get around multiple-interface netmasks.)
#
# SRC is the location of the GASH maintained files. (ie. user_info, hosts_info)
# DNSDIR is the location of the DNS master maps. (ie. named.*)
#
DIR         = /etc
SRC         = /var/yp/gash/files
DNSDIR      = /var/named
#
# The NIS domainname. It is usually returned by the `domainname` command.
#
DOM         = `domainname`
#
# NOPUSH = "1" means don't push NIS maps.
#
NOPUSH      = "1"
#NOPUSH      = ""
#
# See notes under hosts.time for YPALT.
#
YPALT       = /var/yp/etc
#
# The location of the NIS programs. The first entry is SunOS 5.x and the
# second is SunOS 4.x.
#
YPDIR       = /usr/lib/netsvc/yp
#YPDIR       = /usr/etc/yp
#
# The location of the GASH database creation scripts.
#
YPDBSRC     = /home/dean/ansi_gash/gash-test/scripts
#
# The directory where NIS maps are to be put. It is actually
# $(YPDBDIR)/$(DOM)
#
YPDBDIR     = /home/dean/ansi_gash/gash-test
#
#
YPPUSH      = $(YPDIR)/yppush
MAKEDBM     = $(YPDIR)/makedbm
REVNETGROUP = $(YPDIR)/revnetgroup
STDETHERS   = $(YPDIR)/stdethers
STDHOSTS    = $(YPDIR)/stdhosts
MKNETID     = $(YPDIR)/mknetid
MKALIAS     = $(YPDIR)/mkalias

CHKPIPE     = || ( echo "GASH make terminated:" $@ 1>&2; kill -TERM 0 )

k:	move
	@if [ ! $(NOPUSH) ]; then $(MAKE) $(MFLAGS) -k all; \
	else $(MAKE) $(MFLAGS) -k all NOPUSH=$(NOPUSH);fi
#
# This is the list of informational files.
#
all:	passwd hosts aliases netgroups groups auto.vol \
	networks rpc services protocols bootparams publickey netmasks 
#timezone

clean:
	@rm -f *.errors *.warns *.time named.1* named.2* named.hosts \
	  hosts ethers aliases generics* xaliases

clean_auto:	clean
		@rm -f /var/yp/$(DOM)/auto*

pristine: 	clean
		@rm -f /var/yp/$(DOM)/* gashA*

passwd:		passwd.time
hosts:		hosts.time
aliases:	aliases.time
netgroups:	netgroups.time
groups:		groups.time
auto.vol:	auto.vol.time
networks:	networks.time
rpc:		rpc.time
services:	services.time
protocols:	protocols.time
bootparams:	bootparams.time
publickey:	publickey.time
# See note below for netmasks.time.
netmasks:	netmasks.time
# Commented out since in our setup we didn't use the timezone NIS map.
#timezone:	timezone.time
#$(DIR)/timezone:


#
# Move all the old files into the subdir.
#
move:
	-@/usr/bin/rm -f *.temp *.backup *.new;
	-@/bin/sh $(YPDBSRC)/gashmove;
	-@find $(SRC)/logs -mtime +5 -exec rm {} \;
	-@compress -f $(SRC)/logs/old/* > /dev/null 2>&1;

#
# Make the passwd map.
#
passwd.time:	$(SRC)/user_info
	@echo "sorting user_info file";
	@sort -o $(SRC)/user_info $(SRC)/user_info;
	@$(YPDBSRC)/gash2passwd $(SRC)/user_info;
	@if [ -f passwd.errors -a ! -s passwd.errors ]; then \
	  rm -f passwd.errors; fi
	@if [ -f passwd.warns -a ! -s passwd.warns ]; then \
	  rm -f passwd.warns; fi
	@echo "step 1 of passwd";
	@(awk 'BEGIN { FS=":"; OFS="\t"; } /^[a-zA-Z0-9_]/ \
	  { print $$1, $$0 }' passwd $(CHKPIPE)) | $(MAKEDBM) - \
	  $(YPDBDIR)/$(DOM)/passwd.byname;
	@echo "step 2 of passwd";
	@(awk 'BEGIN { FS=":"; OFS="\t"; } /^[a-zA-Z0-9_]/ \
	  { printf("%-10d ", $$3); print  $$0 }' passwd $(CHKPIPE)) \
	  | $(MAKEDBM) - $(YPDBDIR)/$(DOM)/passwd.byuid;
	@echo "step 3 of passwd";
	@rm -f passwd;
	@touch passwd.time;
	@echo "updated passwd";
	@if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) passwd.byname &); fi
	@if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) passwd.byuid &); fi
	@if [ ! $(NOPUSH) ]; then echo "pushed passwd"; fi

#
# This will make the hosts, ethers, and DNS records.
#
# Note that it actually makes the hosts file from a cut down hosts file.
# This is due to the fact that we actually want to do all nameservice
# through DNS. Remove references of $(YPALT)/ if you want to actually use
# the GASH hosts file that is created.
# 
hosts.time:	$(SRC)/hosts_info
	@echo "sorting hosts_info file";
	@$(YPDBSRC)/sort_hosts;	
	@$(YPDBSRC)/gash2dns $(SRC)/hosts_info $(DNSDIR);
	@if [ -f host.errors -a ! -s host.errors ]; then \
	  rm -f host.errors; fi;
	@echo "step 1 of hosts";
	@(sed -e "/^#/d" -e s/#.*$$// $(SRC)/hosts $(CHKPIPE)) | \
	  ($(STDHOSTS) $(CHKPIPE)) | \
	  (awk '{for (i = 2; i <= NF; i++) print $$i, $$0 }' $(CHKPIPE)) \
	  | $(MAKEDBM) $(B) -l - $(YPDBDIR)/$(DOM)/hosts.byname;
	@echo "step 2 of hosts";
	@($(STDHOSTS) $(SRC)/hosts $(CHKPIPE)) | \
	  (awk 'BEGIN { OFS="\t"; } $$1 !~ /^#/ { print $$1, $$0 }' \
	  $(CHKPIPE)) | $(MAKEDBM) $(B) - $(YPDBDIR)/$(DOM)/hosts.byaddr;
	@echo "step 3 of hosts";
	@($(STDETHERS) ethers $(CHKPIPE)) |\
	  (awk '{ print $$1, $$0; for (i=3;i<=NF;i++) print $$i, $$0 }' \
	  $(CHKPIPE)) | $(MAKEDBM) - $(YPDBDIR)/$(DOM)/ethers.byaddr;
	@echo "step 4 of hosts";
	@(awk 'BEGIN { OFS="\t"; } $$1 !~ /^#/ { print $$2, $$0 }' ethers \
	  $(CHKPIPE)) | $(MAKEDBM) -l - $(YPDBDIR)/$(DOM)/ethers.byname;
	@@echo "step 5 of hosts";
	-@kill -HUP `cat /etc/named.pid`;
	@touch hosts.time;
	@echo "updated hosts";
	@if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) hosts.byname &); fi
	@if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) hosts.byaddr &); fi
	@if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) ethers.byname &); fi
	@if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) ethers.byaddr &); fi
	@if [ ! $(NOPUSH) ]; then echo "pushed hosts"; fi

#
# This makes the mail aliases, generics files and the like.
#
aliases.time: $(SRC)/aliases_info
	@echo "sorting aliases_info file";
	@$(YPDBSRC)/sort_aliases;
	@if [ -f mail_renaming ]; then \
	  cat mail_renaming | \
	  awk '{ print "if [-f /var/mail/" $$1, "; \
	  mv -f /var/mail/" $$1, "/var/mail/" $$2, "; fi" '} | \
	  /bin/sh; \
	  rm -f mail_renaming; \
	fi
	@$(YPDBSRC)/gash2alias $(SRC)/aliases_info
	@if [ -f aliases.errors -a ! -s aliases.errors ]; then \
	  rm -f aliases.errors; fi
	@if [ -f aliases.warns -a ! -s aliases.warns ]; then \
	  rm -f aliases.warns; fi
	@mv -f $(SRC)/aliases $(YPDBDIR)/$(DOM)/mail.aliases;
	@/usr/lib/sendmail -bi -oA$(YPDBDIR)/$(DOM)/mail.aliases;
	@rm -f $(YPDBDIR)/$(DOM)/mail.aliases;
	@rm -f $(SRC)/userdb;
	@touch aliases.time
	@echo "updated aliases";
	@if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) mail.aliases &); fi
	@if [ ! $(NOPUSH) ]; then echo "pushed aliases"; fi
	@echo "Please ignore any errors about ypclear if any appeared above.";
#
# This will make the netgroups map.
#
netgroups.time:	$(SRC)/netgroup
	-@if [ -f $(SRC)/netgroup ]; then \
	  echo "sorting netgroup file"; \
	  sort -o netgroup netgroup; \
	  $(MAKEDBM) $(SRC)/netgroup $(YPDBDIR)/$(DOM)/netgroup; \
	  echo "step 1 of netgroups"; \
	  ($(REVNETGROUP) < $(SRC)/netgroup -u $(CHKPIPE)) | \
		$(MAKEDBM) - $(YPDBDIR)/$(DOM)/netgroup.byuser; \
	  echo "step 2 of netgroups"; \
	  ($(REVNETGROUP) < $(SRC)/netgroup -h $(CHKPIPE)) | \
		$(MAKEDBM) - $(YPDBDIR)/$(DOM)/netgroup.byhost; \
	  echo "step 3 of netgroups"; \
	  touch netgroups.time; \
	  echo "updated netgroup"; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) netgroup &); fi; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) \
		netgroup.byuser &); fi; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) \
		netgroup.byhost &); fi; \
	  if [ ! $(NOPUSH) ]; then echo "pushed netgroup"; fi; \
	else \
	  echo "could not find $(SRC)/netgroup"; \
	fi

#
# This will make the groups map from the group_info file.
#
groups.time: $(SRC)/group_info
	@echo "sorting group_info file";
	@sort -o group_info group_info;
	@$(YPDBSRC)/gash2group $(SRC)/group_info;
	@if [ -f group.errors -a ! -s group.errors ]; then \
	  rm -f group.errors; fi
	@if [ -f group.warns -a ! -s group.warns ]; then \
	  rm -f group.warns; fi
	@echo "step 1 of groups";
	@(awk 'BEGIN { FS=":"; OFS="\t"; } { print $$1, $$0 }' \
	  $(SRC)/group $(CHKPIPE)) | \
	  $(MAKEDBM) - $(YPDBDIR)/$(DOM)/group.byname;
	@echo "step 2 of groups";
	@(awk 'BEGIN { FS=":"; OFS="\t"; } { printf("%-10d ", $$3); \
	  print $$0 }' $(SRC)/group $(CHKPIPE)) | \
	  $(MAKEDBM) - $(YPDBDIR)/$(DOM)/group.bygid;
	@echo "step 3 of groups";
	@rm -f group;
	@touch groups.time;
	@echo "updated group";
	@if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) group.byname &); fi
	@if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) group.bygid &); fi
	@if [ ! $(NOPUSH) ]; then echo "pushed group"; fi

#
# This little script will make the automount.volume map as well as the
# auto.home.* maps for all the individualized maps. The rm of $(YPDBIR)/
# $(DOM)/auto.home.* is so that once a map has been removed it will not
# show up in future NIS maps. There needs to be a way of deleting old
# maps from NIS slaves.
#
auto.vol.time:	$(SRC)/auto.vol $(SRC)/auto.home.*
	@sed -e "/^#/d" -e s/#.*$$// $(SRC)/auto.vol \
	  | $(MAKEDBM) - $(YPDBDIR)/$(DOM)/auto.vol;
	@echo "step 1 of auto.vol";
	@if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM)  auto.vol &); fi
	@if [ ! $(NOPUSH) ]; then echo "pushed auto.vol"; fi
	-@rm -f $(YPDBDIR)/$(DOM)/auto.home.[!.];
	@for name in $(SRC)/auto.home.*; do \
	  name2=`echo $$name | sed -e s\#$(SRC)/## -e s/auto.home/gash.home/`;\
	  cp -p $$name $$name2; \
	  $(YPDBSRC)/gash2auto $$name2; \
	  echo "iteration 1 of $$name in auto.vol"; \
	done;
	@echo "step 2 of auto.vol"; 
	@for name in auto.home.*; do \
	  sed -e "/^#/d" -e s/#.*$$// $$name \
		| $(MAKEDBM) - $(YPDBDIR)/$(DOM)/$$name; \
	  echo "iteration 2 of $$name in auto.vol"; \
	  name2=`echo $$name | sed -e s/auto.home/gash.home/`; \
	  mv -f $$name2 $$name; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) $$name &); fi; \
	  if [ ! $(NOPUSH) ]; then echo "pushed $$name"; fi; \
          echo sorting $$name; \
	  $(YPDBSRC)/sort_automap $$name; \
	done;
	@echo "step 3 of auto.vol";
	@echo "sorting auto.vol";
	@sort -o auto.vol auto.vol;
	@touch auto.vol.time;
	@echo "updated auto.vol";

#
# The remaining maps are out of the regular files and need no modifications
# for use in NIS besides being ran through their normal filters and makedbm.
#

networks.time: $(DIR)/networks
	@if [ -f $(DIR)/networks ]; then \
	  (sed -e "/^#/d" -e s/#.*$$// $(DIR)/networks $(CHKPIPE)) \
		| (awk '{print $$1, $$0; for (i = 3;i <= NF;i++) print \
		$$i,$$0}' $(CHKPIPE)) | $(MAKEDBM) - \
		$(YPDBDIR)/$(DOM)/networks.byname; \
	  (awk 'BEGIN { OFS="\t"; } $$1 !~ /^#/ { print $$2, $$0 }' \
		$(DIR)/networks $(CHKPIPE)) | $(MAKEDBM) - \
		$(YPDBDIR)/$(DOM)/networks.byaddr; \
	  touch networks.time; \
	  echo "updated networks"; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) \
		networks.byname &); fi; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) \
	  	networks.byaddr &); fi; \
	  if [ ! $(NOPUSH) ]; then echo "pushed networks"; fi; \
	else \
	  echo "couldn't find $(DIR)/networks"; \
	fi

services.time: $(DIR)/services
	@if [ -f $(DIR)/services ]; then \
	  (awk 'BEGIN { OFS="\t"; } $$1 !~ /^#/ { print $$2, $$0 }' \
		$(DIR)/services $(CHKPIPE))| $(MAKEDBM) - \
		$(YPDBDIR)/$(DOM)/services.byname; \
	  touch services.time; \
	  echo "updated services"; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) \
	  	services.byname &); fi; \
	  if [ ! $(NOPUSH) ]; then echo "pushed services"; fi; \
	else \
	  echo "couldn't fine $(DIR)/services"; \
	fi

rpc.time: $(DIR)/rpc
	@if [ -f $(DIR)/rpc ]; then \
	  (awk 'BEGIN { OFS="\t"; } $$1 !~ /^#/ { print $$2, $$0 }' \
		$(DIR)/rpc $(CHKPIPE))| $(MAKEDBM) - \
		$(YPDBDIR)/$(DOM)/rpc.bynumber; \
	  touch rpc.time; \
	  echo "updated rpc"; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) rpc.bynumber &); fi; \
	  if [ ! $(NOPUSH) ]; then echo "pushed rpc"; fi; \
	else \
	  echo "couldn't find $(DIR)/rpc"; \
	fi

protocols.time: $(DIR)/protocols
	@if [ -f $(DIR)/protocols ]; then \
	  (awk 'BEGIN { OFS="\t"; } $$1 !~ /^#/ { print $$2, $$0 }' \
		$(DIR)/protocols $(CHKPIPE)) | $(MAKEDBM) - \
		$(YPDBDIR)/$(DOM)/protocols.bynumber; \
	  (sed -e "/^#/d" -e s/#.*$$// $(DIR)/protocols $(CHKPIPE)) | \
		(awk '{print $$1,$$0; for (i = 3;i <= NF;i++) print $$i, \
		$$0}' $(CHKPIPE)) | $(MAKEDBM) - \
		$(YPDBDIR)/$(DOM)/protocols.byname; \
	  touch protocols.time; \
	  echo "updated protocols"; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) \
		protocols.byname &); fi; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) \
		protocols.bynumber &); fi; \
	  if [ ! $(NOPUSH) ]; then echo "pushed protocols"; fi; \
	else \
	  echo "couldn't find $(DIR)/protocols"; \
	fi

bootparams.time: $(DIR)/bootparams
	@if [ -f $(DIR)/bootparams ]; then \
	  (sed -e '/^#/d' -e s/#.*$$// -e 's/[    ][      ]*$$//' \
		-e '/\\$$/s/\\$$/ /' $(DIR)/bootparams $(CHKPIPE)) \
		|( awk '/ $$/ {printf "%s", $$0} !/ $$/ {print}' $(CHKPIPE)) \
		|( sed -e 's/[   ][      ]*/ /g' $(CHKPIPE)) \
		| $(MAKEDBM) - $(YPDBDIR)/$(DOM)/bootparams; \
	  touch bootparams.time; \
	  echo "updated bootparams"; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) bootparams &); fi; \
	  if [ ! $(NOPUSH) ]; then echo "pushed bootparams"; fi; \
	else \
	  echo "couldn't find $(DIR)/bootparams"; \
	fi

#
# A netmasks files must exist in the in the SRC directory to make sure a
# machine with multiple interfaces pushes out the correct netmasks for
# the other machines.
#
netmasks.time: $(SRC)/netmasks
	@if [ -f $(SRC)/netmasks ]; then \
	  $(MAKEDBM) $(SRC)/netmasks $(YPDBDIR)/$(DOM)/netmasks.byaddr; \
	  touch netmasks.time; \
	  echo "updated netmasks"; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) \
			netmasks.byaddr &); fi; \
	  if [ ! $(NOPUSH) ]; then echo "pushed netmasks"; fi; \
	else \
	  echo "couldn't find $(SRC)/netmasks"; \
	fi

publickey.time: $(DIR)/publickey
	@if [ -f $(DIR)/publickey ]; then \
	  (sed "/^#/d" < $(DIR)/publickey $(CHKPIPE)) \
		| $(MAKEDBM) - $(YPDBDIR)/$(DOM)/publickey.byname; \
	  touch publickey.time; \
	  echo "updated publickey"; \
	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) \
		publickey.byname &); fi; \
	  if [ ! $(NOPUSH) ]; then echo "pushed publickey"; fi; \
	else \
	  echo "couldn't find $(DIR)/publickey"; \
	fi

# Commented out since we don't use it.
#
#timezone.time:  $(DIR)/timezone
#	-@if [ -f $(DIR)/timezone ]; then \
#	  sed -e "/^#/d" -e s/#.*$$// $(DIR)/timezone \
#		| awk '{for (i = 2; i<=NF; i++) print $$i, $$0}' \
#		| $(MAKEDBM) - $(YPDBDIR)/$(DOM)/timezone.byname; \
#	  touch timezone.time; \
#	  echo "updated timezone"; \
#	  if [ ! $(NOPUSH) ]; then ($(YPPUSH) -d $(DOM) \
#		timezone.byname &); fi; \
#	  if [ ! $(NOPUSH) ]; then echo "pushed timezone"; fi; \
#	else \
#	  echo "couldn't find $(DIR)/timezone"; \
#	fi
