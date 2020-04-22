.. _bgp-user-guide-additional-path-capability:

Additional Path Capability
==========================
The ADD-PATH capability allows to advertise multiple paths for the same address prefix.
It can help with optimal routing and routing convergence in a network by providing potential alternate or backup paths.

.. contents:: Contents
   :depth: 2
   :local:

Configuration
^^^^^^^^^^^^^
This section shows a way to enable ADD-PATH capability in BGP speaker and peer configuration.

.. note:: The capability is applicable for IP Unicast, IP Labeled Unicast and Flow Specification address families.

BGP Speaker
'''''''''''
To enable ADD-PATH capability in BGP plugin, first configure BGP speaker instance:

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 15

    {
        "protocol": {
            "name": "bgp-example",
            "identifier": "openconfig-policy-types:BGP",
            "bgp": {
                "global": {
                    "config": {
                        "router-id": "192.0.2.2",
                        "as": "65000"
                    },
                    "afi-safis": {
                        "afi-safi": {
                            "afi-safe-name": "openconfig-bgp-types:IPV4-UNICAST",
                            "receive": "true",
                            "send-max": "2"
                        }
                    }
                }
            }
        }
    }

@line 15: Defines path selection strategy: *send-max* > 1 -> Advertise N Paths or *send-max* = 0 -> Advertise All Paths

Here is an example for update a specific family with enable ADD-PATH capability

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/global/afi-safis/afi-safi/openconfig-bgp-types:IPV4%2DUNICAST``

**Method:** ``PUT``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json

    {
        "afi-safi": {
            "afi-safi-name": "openconfig-bgp-types:IPV4-UNICAST",
            "receive": "true",
            "send-max": "0"
        }
    }
    
BGP Peer
''''''''
Here is an example for BGP peer configuration with enabled ADD-PATH capability.

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/neighbors``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json

    {
        "neighbor": {
            "neighbor-address": "192.0.2.1",
            "afi-safis": {
                "afi-safi": {
                    "afi-safi-name": ["openconfig-bgp-types:IPV4-LABELLED-UNICAST","openconfig-bgp-types:IPV4-UNICAST"],
                    "receive": "true",
                    "send-max": "0"
                }
            }
        }
    }

.. note:: The path selection strategy is not configurable on per peer basis. The send-max presence indicates a willingness to send ADD-PATH NLRIs to the neighbor.

Here is an example for update specific family BGP peer configuration with enabled ADD-PATH capability.

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/neighbors/neighbor/192.0.2.1/afi-safis/afi-safi/openconfig-bgp-types:IPV4%2DUNICAST``

**Method:** ``PUT``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json

    {
        "afi-safi": {
            "afi-safi-name": "openconfig-bgp-types:IPV4-UNICAST",
            "receive": "true",
            "send-max": "0"
        }
    }

Usage
^^^^^
The IPv4 Unicast table with enabled ADD-PATH capability in an instance of the speaker's Loc-RIB can be verified via REST:

**URL:** ``/restconf/operational/bgp-rib:bgp-rib/rib/bgp-example/loc-rib/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/ipv4-routes``

**Method:** ``GET``

**Response Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 5

    {
        "ipv4-routes": {
            "_xmlns": "urn:opendaylight:params:xml:ns:yang:bgp-inet",
            "ipv4-route:1": {
                "path-id": "1",
                "prefix": "193.0.2.1/32",
                "attributes" : {
                    "as-path": null,
                    "origin": {
                        "value": "igp"
                    },
                    "local-pref": {
                        "pref": "100"
                    },
                    "ipv4-next-hop": {
                        "global": "10.0.0.1"
                    }
                }
            },
            "ipv4-route:2": {
                "path-id": "2",
                "prefix": "193.0.2.1/32",
                "attributes" : {
                    "as-path": null,
                    "origin": {
                        "value": "igp"
                    },
                    "local-pref": {
                        "pref": "100"
                    },
                    "ipv4-next-hop": {
                        "global": "10.0.0.2"
                    }
                }
            }
        }
    }

@line 5: The routes with the same destination are distinguished by *path-id* attribute.

References
^^^^^^^^^^
* `Advertisement of Multiple Paths in BGP <https://tools.ietf.org/html/rfc7911>`_
* `Best Practices for Advertisement of Multiple Paths in IBGP <https://tools.ietf.org/html/draft-ietf-idr-add-paths-guidelines-08>`_
