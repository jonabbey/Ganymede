0#!/usr/bin/python
#
# This script runs a query against a Ganymede server to get a list of
# users and the owner groups they belong to.
#
# Jonathan Abbey
# 29 January 2010
################################################################################

import sys
import os
from subprocess import *
from xml.dom.minidom import parse, parseString

debug=False
xmlclient='/home/broccol/ganymede-client/bin/xmlclient'

def perform_query(query_string, username, password):
    args=[xmlclient, 'username='+username, '-query', query_string]
    p1=Popen(args,stderr=PIPE,stdin=PIPE,stdout=PIPE,shell=False)
    (xmloutput, xmlerror)=p1.communicate(input=password)
    try:
        dom = parseString(xmloutput)
        if debug:
            print dom.toxml()
    except:
        print xmlerror
        raise
    return dom

print 'Enter the user / persona name to query Ganymede with.\n> ',
username=sys.stdin.readline()
sys.stdout.write('') # print an empty string to make python not indent our next print

print 'Enter the password for', username, '\n> ',
password=sys.stdin.readline()
sys.stdout.write('')

dom = perform_query('\'select \\"Owner list\\", \\"Username\\" from "User"\'', username, password)

objectnodes = dom.getElementsByTagName('object')

userdict = {}

for node in objectnodes:
    userid = node.getAttribute('id')
    owners = []
    invidNodes = node.getElementsByTagName('invid')
    for inode in invidNodes:
        owners.append(inode.getAttribute('id'))
    print "User", userid, "=", ', '.join(owners).encode('us-ascii')

