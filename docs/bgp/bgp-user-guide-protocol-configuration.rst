.. _bgp-user-guide-protocol-configuration:

Protocol Configuration
======================
As a first step, a new protocol instance needs to be configured.
It is a very basic configuration conforming with RFC4271.

.. note:: RIB policy must already be configured and present before configuring the protocol.

**XML**

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols``

**RFC8040 URL:** ``/rests/data/openconfig-network-instance:network-instances/network-instance=global-bgp/protocols``

**Method:** ``POST``

**Content-Type:** ``application/xml``

**Request Body:**

.. code-block:: xml
   :linenos:
   :emphasize-lines: 2,7,8

   <protocol xmlns="http://openconfig.net/yang/network-instance">
       <name>bgp-example</name>
       <identifier xmlns:x="http://openconfig.net/yang/policy-types">x:BGP</identifier>
       <bgp xmlns="urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions">
           <global>
               <config>
                   <router-id>192.0.2.2</router-id>
                   <as>65000</as>
               </config>
                <apply-policy>
                    <config>
                        <default-export-policy>REJECT-ROUTE</default-export-policy>
                        <default-import-policy>REJECT-ROUTE</default-import-policy>
                        <import-policy>default-odl-import-policy</import-policy>
                        <export-policy>default-odl-export-policy</export-policy>
                    </config>
                </apply-policy>
           </global>
       </bgp>
   </protocol>

@line 2: The unique protocol instance identifier.

@line 7: BGP Identifier of the speaker.

@line 8: Local autonomous system number of the speaker. Note that, OpenDaylight BGP implementation supports four-octet AS numbers only.

@line 14: Default ODL Import Policy.

@line 15: Default ODL Export Policy.

**JSON**

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols``

**RFC8040 URL:** ``/rests/data/openconfig-network-instance:network-instances/network-instance=global-bgp/protocols``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 3,8,9

    {
        "protocols": {
            "protocol": [
                {
                    "identifier": "openconfig-policy-types:BGP",
                    "name": "bgp-example",
                    "bgp-openconfig-extensions:bgp": {
                        "global": {
                            "config": {
                                "router-id": "192.0.2.2",
                                "as": 65000
                            },
                            "apply-policy": {
                                "config": {
                                    "export-policy": [
                                        "default-odl-export-policy"
                                    ],
                                    "import-policy": [
                                        "default-odl-import-policy"
                                    ],
                                    "default-export-policy": "REJECT-ROUTE",
                                    "default-import-policy": "REJECT-ROUTE"
                                }
                            }
                        }
                    }
                }
            ]
        }
    }

@line 3: The unique protocol instance identifier.

@line 8: BGP Identifier of the speaker.

@line 9: Local autonomous system number of the speaker. Note that, OpenDaylight BGP implementation supports four-octet AS numbers only.

@line 15: Default ODL Import Policy.

@line 16: Default ODL Export Policy.

-----

The new instance presence can be verified via REST:

**XML**

**URL:** ``/restconf/operational/bgp-rib:bgp-rib/rib/bgp-example``

**RFC8040 URL:** ``/rests/data/bgp-rib:bgp-rib/rib=bgp-example?content=nonconfig``

**Method:** ``GET``

**Response Body:**

.. code-block:: xml
   :linenos:
   :emphasize-lines: 3,4

   <rib xmlns="urn:opendaylight:params:xml:ns:yang:bgp-rib">
       <id>bgp-example</id>
       <loc-rib>
           <tables>
               <afi xmlns:x="urn:opendaylight:params:xml:ns:yang:bgp-types">x:ipv4-address-family</afi>
               <safi xmlns:x="urn:opendaylight:params:xml:ns:yang:bgp-types">x:unicast-subsequent-address-family</safi>
               <ipv4-routes xmlns="urn:opendaylight:params:xml:ns:yang:bgp-inet"></ipv4-routes>
               <attributes>
                   <uptodate>true</uptodate>
               </attributes>
           </tables>
       </loc-rib>
   </rib>

@line 3: Loc-RIB - Per-protocol instance RIB, which contains the routes that have been selected by local BGP speaker's decision process.

@line 4: The BGP-4 supports carrying IPv4 prefixes, such routes are stored in *ipv4-address-family*/*unicast-subsequent-address-family* table.

**JSON**

**URL:** ``/restconf/operational/bgp-rib:bgp-rib/rib/bgp-example``

**RFC8040 URL:** ``/rests/data/bgp-rib:bgp-rib/rib=bgp-example?content=nonconfig``

**Method:** ``GET``

**Response Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 4,5

    {
        "rib": [
            {
                "id": "bgp-example",
                "loc-rib": {
                    "tables": [
                        {
                            "afi": "bgp-types:ipv4-address-family",
                            "safi": "bgp-types:unicast-subsequent-address-family",
                            "attributes": {
                                "uptodate": true
                            }
                        }
                    ]
                }
            }
        ]
    }

@line 4: Loc-RIB - Per-protocol instance RIB, which contains the routes that have been selected by local BGP speaker's decision process.

@line 5: The BGP-4 supports carrying IPv4 prefixes, such routes are stored in *ipv4-address-family*/*unicast-subsequent-address-family* table.
