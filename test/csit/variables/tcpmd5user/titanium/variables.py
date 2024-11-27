"""
Variables file for tcpmd5user suite.

Expected JSON templates are fairly long,
therefore there are moved out of testcase file.
Also, it is needed to generate base64 encoded tunnel name
from Mininet IP (which is not known beforehand),
so it is easier to employ Python here,
than do manipulation in Robot file.
"""
# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import base64
from string import Template


__author__ = "Vratko Polak"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "Eclipse Public License v1.0"
__email__ = "vrpolak@cisco.com"


# FIXME: Migrate values shared by other suites to separate Python module.


def get_variables(mininet_ip):
    """Return dict of variables for the given IPv4 address of Mininet VM."""
    # TODO: Document in 'V' fashion, as in pcepuser/variables.py using more systematic local variable names.
    # Dict of variables to return, starts empty and grows as function proceeds.
    variables = {}
    # Given mininet_ip, this will be the sympolic name uf tunnel under test.
    tunnelname = "pcc_" + mininet_ip + "_tunnel_1"
    # Base64 code for the symbolic name, as that is present in datastore.
    tunnelname_bytes = tunnelname.encode("ascii")
    pathcode_encoded = base64.b64encode(tunnelname_bytes)
    pathcode = pathcode_encoded.decode("ascii")
    variables["pcc_name"] = tunnelname
    variables["pcc_name_code"] = pathcode
    # JSON response when pcep-topology is ready but no PCC is connected.
    variables[
        "offjson"
    ] = """{
 "topology": [
  {
   "topology-id": "pcep-topology",
   "topology-types": {
    "network-topology-pcep:topology-pcep": {}
   }
  }
 ]
}"""
    # Template of JSON response with pcep-topology seeing 1 PCC 1 LSP.
    onjsontempl = Template(
        """{
 "topology": [
  {
   "node": [
    {
     "network-topology-pcep:path-computation-client": {
      "ip-address": "$IP",
      "reported-lsp": [
       {
        "name": "$NAME",
        "path": [
         {
          "ero": {
           "ignore": false,
           "processing-rule": false,
           "subobject": [
            {
             "ip-prefix": {
              "ip-prefix": "1.1.1.1/32"
             },
             "loose": false
            }
           ]
          },
          "lsp-id": 1,
          "odl-pcep-ietf-stateful:lsp": {
           "administrative": true,
           "delegate": true,
           "ignore": false,
           "odl-pcep-ietf-initiated:create": false,
           "operational": "up",
           "plsp-id": 1,
           "processing-rule": false,
           "remove": false,
           "sync": true,
           "tlvs": {
            "lsp-identifiers": {
             "ipv4": {
              "ipv4-extended-tunnel-id": "$IP",
              "ipv4-tunnel-endpoint-address": "1.1.1.1",
              "ipv4-tunnel-sender-address": "$IP"
             },
             "lsp-id": 1,
             "tunnel-id": 1
            },
            "symbolic-path-name": {
             "path-name": "$CODE"
            }
           }
          }
         }
        ]
       }
      ],
      "state-sync": "synchronized",
      "stateful-tlv": {
       "odl-pcep-ietf-stateful:stateful": {
        "lsp-update-capability": true,
        "odl-pcep-ietf-initiated:initiation": true
       }
      }
     },
     "node-id": "pcc://$IP"
    }
   ],
   "topology-id": "pcep-topology",
   "topology-types": {
    "network-topology-pcep:topology-pcep": {}
   }
  }
 ]
}"""
    )
    # Dictionly which tells values for placeholders.
    repl_dict = {"IP": mininet_ip, "NAME": tunnelname, "CODE": pathcode}
    # The finalized JSON.
    variables["onjson"] = onjsontempl.substitute(repl_dict)
    # The following strings are XML data.
    # See https://wiki.opendaylight.org/view/BGP_LS_PCEP:TCP_MD5_Guide#RESTCONF_Configuration
    # For curl, string is suitable to became -d argument only after
    # replacing ' -> '"'"' and enclosing in single quotes.
    variables[
        "key_access_module"
    ] = """<module xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">
 <type xmlns:x="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:jni:cfg">x:native-key-access-factory</type>
 <name>global-key-access-factory</name>
</module>"""
    variables[
        "key_access_service"
    ] = """<service xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">
 <type xmlns:x="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:cfg">x:key-access-factory</type>
 <instance>
  <name>global-key-access-factory</name>
  <provider>/modules/module[type='native-key-access-factory'][name='global-key-access-factory']</provider>
 </instance>
</service>"""
    variables[
        "client_channel_module"
    ] = """<module xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">
 <type xmlns:x="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:netty:cfg">x:md5-client-channel-factory</type>
 <name>md5-client-channel-factory</name>
 <key-access-factory xmlns="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:netty:cfg">
  <type xmlns:x="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:cfg">x:key-access-factory</type>
  <name>global-key-access-factory</name>
 </key-access-factory>
</module>"""
    variables[
        "client_channel_service"
    ] = """<service xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">
 <type xmlns:x="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:netty:cfg">x:md5-channel-factory</type>
 <instance>
  <name>md5-client-channel-factory</name>
  <provider>/modules/module[type='md5-client-channel-factory'][name='md5-client-channel-factory']</provider>
 </instance>
</service>"""
    variables[
        "server_channel_module"
    ] = """<module xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">
 <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:netty:cfg">"""
    # What is your favourite way to concatenate strings without resembling tuple?
    variables[
        "server_channel_module"
    ] += """prefix:md5-server-channel-factory-impl</type>
 <name>md5-server-channel-factory</name>
 <server-key-access-factory xmlns="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:netty:cfg">
  <type xmlns:x="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:cfg">x:key-access-factory</type>
  <name>global-key-access-factory</name>
 </server-key-access-factory>
</module>"""
    variables[
        "server_channel_service"
    ] = """<service xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">
 <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:netty:cfg">"""
    variables[
        "server_channel_service"
    ] += """prefix:md5-server-channel-factory</type>
 <instance>
  <name>md5-server-channel-factory</name>
  <provider>/modules/module[type='md5-server-channel-factory-impl'][name='md5-server-channel-factory']</provider>
 </instance>
</service>"""
    variables[
        "pcep_dispatcher_module"
    ] = """<module xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">
 <type xmlns:x="urn:opendaylight:params:xml:ns:yang:controller:pcep:impl">x:pcep-dispatcher-impl</type>
 <name>global-pcep-dispatcher</name>
 <md5-channel-factory xmlns="urn:opendaylight:params:xml:ns:yang:controller:pcep:impl">
  <type xmlns:x="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:netty:cfg">x:md5-channel-factory</type>
  <name>md5-client-channel-factory</name>
 </md5-channel-factory>
 <md5-server-channel-factory xmlns="urn:opendaylight:params:xml:ns:yang:controller:pcep:impl">
  <type xmlns:x="urn:opendaylight:params:xml:ns:yang:controller:tcpmd5:netty:cfg">x:md5-server-channel-factory</type>
  <name>md5-server-channel-factory</name>
 </md5-server-channel-factory>
</module>"""
    # Template to set password.
    passwd_templ = Template(
        """<module xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">
 <type xmlns:x="urn:opendaylight:params:xml:ns:yang:controller:pcep:topology:provider">x:pcep-topology-provider</type>
 <name>pcep-topology</name>
 <client xmlns="urn:opendaylight:params:xml:ns:yang:controller:pcep:topology:provider">
  <address xmlns="urn:opendaylight:params:xml:ns:yang:controller:pcep:topology:provider">$IP</address>
$PASSWD </client>
</module>"""
    )
    # We use three template instantiations. No password:
    repl_dict = {"IP": mininet_ip, "PASSWD": ""}
    variables["no_passwd_module"] = passwd_templ.substitute(repl_dict)
    changeme = """  <password>changeme</password>
"""
    # wrong password
    repl_dict = {"IP": mininet_ip, "PASSWD": changeme}
    variables["passwd_changeme_module"] = passwd_templ.substitute(repl_dict)
    # and correct password.
    topsecret = """  <password>topsecret</password>
"""
    repl_dict = {"IP": mininet_ip, "PASSWD": topsecret}
    variables["passwd_topsecret_module"] = passwd_templ.substitute(repl_dict)
    # All variables set, return dict to Robot.
    return variables
