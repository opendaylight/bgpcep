"""Variables file for pcepuser suite.

Expected JSON templates are fairly long,
therefore they are moved out of the testcase file.
Also, it is needed to generate base64 encoded tunnel name
from Mininet IP (which is not known beforehand),
so it is easier to employ Python here,
than do manipulation in Robot file."""
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


def get_variables(mininet_ip):
    """Return dict of variables for the given IP address of Mininet VM."""
    variables = {}
    # 'V' style of explanation.
    # Comments analyze from high level, to low level, then code builds from low level back to high level.
    # ### Pcep-topology JSON responses.
    # Some testcases see only the tunnel created by pcc-mock start: "delegated tunnel" (ID 1).
    # Other testcases see also tunnel created on ODL demand: "instantiated tunnel" (ID 2).
    # Both tunnels can have two states. "Default" upon creation with single hop "1.1.1.1",
    # and "updated" after update-lsp, which prepends another hop "2.2.2.2".
    # Variable naming always specifies delegated state first, and ends with _json to distinguish from operation data.
    # The whole list: default_json, updated_json, updated_default_json, updated_updated_json.
    # Oh, and the state without mock-pcc connected is off_json.
    # off_json has '{}' substring and no variable data, so here it is as a special case:
    variables[
        "off_json"
    ] = """{
 "network-topology:topology": [
  {
   "topology-id": "pcep-topology",
   "topology-types": {
    "network-topology-pcep:topology-pcep": {}
   }
  }
 ]
}"""
    # Ok, other _json strings will have more regular structure and some variable data,
    # so we will be using templates heavily.
    # First off, there is a segment describing PCC which contains IP address but is otherwise constant.
    # So the top-level template will look like this:
    json_templ = Template(
        """{
 "network-topology-pcep:path-computation-client": {
  "ip-address": "$IP",
  "reported-lsp": [$LSPS
  ],
  "state-sync": "synchronized",
  "stateful-tlv": {
   "odl-pcep-ietf-stateful:stateful": {
    "lsp-update-capability": true,
    "odl-pcep-ietf-initiated:initiation": true
   }
  }
 }
}"""
    )
    # The _json variables will differ only in $LSPS, but $IP will be present inside.
    # Thus, the $IP substitution will come last, and any auxiliary substitutions before this final one
    # will have to use safe_substitute().
    # As you see, $LSPS is in json_templ without preceding newline.
    # As a rule, a segment will always start with endline and end without endline,
    # so that we can add commas where needed.
    # Discussion about delegated and instantiated implies that $LSPS is either a single delegated LSP
    # or a pair of delegated and instantiated (separated by comma) LSPS, in appropriate state.
    # Of course, one LSP always follows a structure, for which here is the template:
    lsp_templ = Template(
        """
   {
    "name": "$NAME",
    "path": [
     {
      "ero": {
       "ignore": false,
       "processing-rule": false,
       "subobject": [$HOPS
       ]
      },
      "lsp-id": $ID,
      "odl-pcep-ietf-stateful:lsp": {
       "administrative": true,
       "delegate": true,
       "ignore": false,
       "odl-pcep-ietf-initiated:create": $CREATED,
       "operational": "up",
       "plsp-id": $ID,
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
         "lsp-id": $ID,
         "tunnel-id": $ID
        },
        "symbolic-path-name": {
         "path-name": "$CODE"
        }
       }
      }
     }
    ]
   }"""
    )
    # IDs were already talked about, IP will be set last. Now, $NAME.
    # Pcc-mock uses a fixed naming scheme for delegated tunnels, so one more template can be written,
    # but it is so simple we can write just the one-line code instead:
    delegated_name = "pcc_" + mininet_ip + "_tunnel_1"  # 1 == ID
    # For the instantiated tunnel, user is free to specify anything, even characters such as \u0000 work.
    # But as we need to plug the name to XML, let us try something more friendly:
    instantiated_name = "Instantiated tunnel"
    # What is CODE? The NAME in base64 encoding (without endline):
    delegated_name_bytes = delegated_name.encode("ascii")
    delegated_code_encoded = base64.b64encode(delegated_name_bytes)
    delegated_code = delegated_code_encoded.decode("ascii")
    instantiated_name_bytes = instantiated_name.encode("ascii")
    instantiated_code_encoded = base64.b64encode(instantiated_name_bytes)
    instantiated_code = instantiated_code_encoded.decode("ascii")

    # The remaining segment is HOPS, and that is the place where default and updated states differ.
    # Once again, there is a template for a single hop:
    hop_templ = Template(
        """
       {
         "ip-prefix": {
          "ip-prefix": "$HOPIP/32"
         },
         "loose": false
       }"""
    )
    # The low-to-high part of V comes now, it is just substituting and concatenating.
    # Hops:
    final_hop = hop_templ.substitute({"HOPIP": "1.1.1.1"})
    update_hop = hop_templ.substitute({"HOPIP": "2.2.2.2"})
    both_hops = update_hop + "," + final_hop
    # Lsps:
    default_lsp_templ = Template(lsp_templ.safe_substitute({"HOPS": final_hop}))
    updated_lsp_templ = Template(lsp_templ.safe_substitute({"HOPS": both_hops}))
    repl_dict = {
        "NAME": delegated_name,
        "ID": "1",
        "CODE": delegated_code,
        "CREATED": "false",
    }
    delegated_default_lsp = default_lsp_templ.safe_substitute(repl_dict)
    delegated_updated_lsp = updated_lsp_templ.safe_substitute(repl_dict)
    repl_dict = {
        "NAME": instantiated_name,
        "ID": "2",
        "CODE": instantiated_code,
        "CREATED": "true",
    }
    instantiated_default_lsp = default_lsp_templ.safe_substitute(repl_dict)
    instantiated_updated_lsp = updated_lsp_templ.safe_substitute(repl_dict)
    # Json templates (without IP set).
    repl_dict = {"LSPS": delegated_default_lsp}
    default_json_templ = Template(json_templ.safe_substitute(repl_dict))
    repl_dict = {"LSPS": delegated_updated_lsp}
    updated_json_templ = Template(json_templ.safe_substitute(repl_dict))
    repl_dict = {"LSPS": delegated_updated_lsp + "," + instantiated_default_lsp}
    updated_default_json_templ = Template(json_templ.safe_substitute(repl_dict))
    repl_dict = {"LSPS": delegated_updated_lsp + "," + instantiated_updated_lsp}
    updated_updated_json_templ = Template(json_templ.safe_substitute(repl_dict))
    # Final json variables.
    repl_dict = {"IP": mininet_ip}
    variables["default_json"] = default_json_templ.substitute(repl_dict)
    variables["updated_json"] = updated_json_templ.substitute(repl_dict)
    variables["updated_default_json"] = updated_default_json_templ.substitute(repl_dict)
    variables["updated_updated_json"] = updated_updated_json_templ.substitute(repl_dict)
    # ### Pcep operations XML data.
    # There are three operations, so let us just write templates from information at
    # https://wiki.opendaylight.org/view/BGP_LS_PCEP:Programmer_Guide#Tunnel_Management_for_draft-ietf-pce-stateful-pce-07_and_draft-ietf-pce-pce-initiated-lsp-00
    # _xml describes content type and also distinguishes from similarly named _json strings.
    add_xml_templ = Template(
        '<input xmlns="urn:opendaylight:params:xml:ns:yang:topology:pcep">\n'
        " <node>pcc://$IP</node>\n"
        " <name>$NAME</name>\n"
        ' <network-topology-ref xmlns:topo="urn:TBD:params:xml:ns:yang:network-topology">'
        '/topo:network-topology/topo:topology[topo:topology-id="pcep-topology"]'
        "</network-topology-ref>\n"
        " <arguments>\n"
        '  <lsp xmlns="urn:opendaylight:params:xml:ns:yang:pcep:ietf:stateful">\n'
        "   <delegate>true</delegate>\n"
        "   <administrative>true</administrative>\n"
        "  </lsp>\n"
        "  <endpoints-obj>\n"
        "   <ipv4>\n"
        "    <source-ipv4-address>$IP</source-ipv4-address>\n"
        "    <destination-ipv4-address>1.1.1.1</destination-ipv4-address>\n"
        "   </ipv4>\n"
        "  </endpoints-obj>\n"
        "  <ero>\n"
        "   <subobject>\n"
        "    <loose>false</loose>\n"
        "    <ip-prefix><ip-prefix>1.1.1.1/32</ip-prefix></ip-prefix>\n"
        "   </subobject>\n"
        "  </ero>\n"
        " </arguments>\n"
        "</input>\n"
    )
    update_xml_templ = Template(
        '<input xmlns="urn:opendaylight:params:xml:ns:yang:topology:pcep">\n'
        " <node>pcc://$IP</node>\n"
        " <name>$NAME</name>\n"
        ' <network-topology-ref xmlns:topo="urn:TBD:params:xml:ns:yang:network-topology">'
        '/topo:network-topology/topo:topology[topo:topology-id="pcep-topology"]'
        "</network-topology-ref>\n"
        " <arguments>\n"
        '  <lsp xmlns="urn:opendaylight:params:xml:ns:yang:pcep:ietf:stateful">\n'
        "   <delegate>true</delegate>\n"
        "   <administrative>true</administrative>\n"
        "  </lsp>\n"
        "  <ero>\n"
        "   <subobject>\n"
        "    <loose>false</loose>\n"
        "    <ip-prefix><ip-prefix>2.2.2.2/32</ip-prefix></ip-prefix>\n"
        "   </subobject>\n"
        "   <subobject>\n"
        "    <loose>false</loose>\n"
        "    <ip-prefix><ip-prefix>1.1.1.1/32</ip-prefix></ip-prefix>\n"
        "   </subobject>\n"
        "  </ero>\n"
        " </arguments>\n"
        "</input>\n"
    )
    remove_xml_templ = Template(
        '<input xmlns="urn:opendaylight:params:xml:ns:yang:topology:pcep">\n'
        " <node>pcc://$IP</node>\n"
        " <name>$NAME</name>\n"
        ' <network-topology-ref xmlns:topo="urn:TBD:params:xml:ns:yang:network-topology">'
        '/topo:network-topology/topo:topology[topo:topology-id="pcep-topology"]'
        "</network-topology-ref>\n"
        "</input>\n"
    )
    # The operations can be applied to either delegated or instantiated tunnel, NAME is the only distinguishing value.
    # Also, the final IP substitution can be done here.
    repl_dict = {"IP": mininet_ip}
    repl_dict["NAME"] = delegated_name
    variables["update_delegated_xml"] = update_xml_templ.substitute(repl_dict)
    variables["remove_delegated_xml"] = remove_xml_templ.substitute(repl_dict)
    repl_dict["NAME"] = instantiated_name
    variables["add_instantiated_xml"] = add_xml_templ.substitute(repl_dict)
    variables["update_instantiated_xml"] = update_xml_templ.substitute(repl_dict)
    variables["remove_instantiated_xml"] = remove_xml_templ.substitute(repl_dict)
    # All variables ready.
    return variables
