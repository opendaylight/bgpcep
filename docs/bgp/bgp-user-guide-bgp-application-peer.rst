.. _bgp-user-guide-bgp-application-peer:

BGP Application Peer and programmable RIB
=========================================
The OpenDaylight BGP implementation also supports routes injection via *Application Peer*.
Such peer has its own programmable RIB, which can be modified by user.
This concept allows user to originate new routes and advertise them to all connected peers.

Application Peer configuration
''''''''''''''''''''''''''''''
Following configuration sample show a way to configure the *Application Peer*:

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/neighbors``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 3,5

    {
        "bgp-openconfig-extensions:neighbor": {
            "neighbor-address": "10.25.1.9",
            "config": {
                "peer-group": "application-peers"
            }
        }
    }

@line 3: IP address is uniquely identifying *Application Peer* and its programmable RIB. Address is also used in local BGP speaker decision process.

@line 5: Indicates that peer is associated with *application-peers* group. It serves to distinguish *Application Peer's* from regular neighbors.

-----

The *Application Peer* presence can be verified via REST:

**URL:** ``/restconf/operational/bgp-rib:bgp-rib/rib/bgp-example/peer/bgp:%2F%2F10.25.1.9``

**Method:** ``GET``

**Response Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 4,9

    {
        "openconfig-bgp-rib:peer": {
            "peer-id": "bgp://10.25.1.9",
            "peer-role": "internal",
            "adj-rib-in": {
                "tables": {
                    "afi": "openconfig-bgp-types:ipv4-address-family",
                    "safi": "openconfig-bgp-types:unicast-subsequent-address-family",
                    "attributes": {
                        "uptodate": "false"
                    }
                }
            },
            "effective-rib-in": {
                "tables": {
                    "afi": "openconfig-bgp-types:ipv4-address-family",
                    "safi": "openconfig-bgp-types:unicast-subsequent-address-family"
                }
            }
        }
    }

@line 4: Peer role for *Application Peer* is *internal*.

.. note:: Adj-RIB-In is empty, as no routes were originated yet.

.. note:: There is no Adj-RIB-Out for *Application Peer*.

Programmable RIB
''''''''''''''''
Next example shows how to inject a route into the programmable RIB.

**URL:** ``/restconf/config/bgp-rib:application-rib/10.25.1.9/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json

    {
        "openconfig-bgp-inet:ipv4-route": {
            "path-id": "0",
            "prefix": "10.0.0.11/32",
            "attributes": {
                "origin": {
                    "value": "igp"
                },
                "local-pref": {
                    "pref": "100"
                },
                "ipv4-next-hop": {
                    "global": "10.11.1.1"
                }
            }
        }
    }

-----

Now the injected route appears in *Application Peer's* RIBs and in local speaker's Loc-RIB:

**URL:** ``/restconf/operational/bgp-rib:bgp-rib/rib/bgp-example/peer/bgp:%2F%2F10.25.1.9``

**Method:** ``GET``

**Response Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 10

    {
        "openconfig-bgp-rib:peer": {
            "peer-id": "bgp://10.25.1.9",
            "peer-role": "internal",
            "adj-rib-in": {
                "tables": {
                    "afi": "openconfig-bgp-types:ipv4-address-family",
                    "safi": "openconfig-bgp-types:unicast-subsequent-address-family",
                    "openconfig-bgp-inet:ipv4-routes": {
                        "ipv4-route": {
                            "path-id": "0",
                            "prefix": "10.0.0.11/32",
                            "attributes": {
                                "origin": {
                                    "value": "igp"
                                },
                                "local-pref": {
                                    "pref": "100"
                                },
                                "ipv4-next-hop": {
                                    "global": "10.11.1.1"
                                }
                            }
                        }
                    },
                    "attributes": {
                        "uptodate": "false"
                    }
                }
            },
            "effective-rib-in": {
                "tables": {
                    "afi": "openconfig-bgp-types:ipv4-address-family",
                    "safi": "openconfig-bgp-types:unicast-subsequent-address-family",
                    "openconfig-bgp-inet:ipv4-routes": {
                        "ipv4-route": {
                            "path-id": "0",
                            "prefix": "10.0.0.11/32",
                            "attributes": {
                                "origin": {
                                    "value": "igp"
                                },
                                "local-pref": {
                                    "pref": "100"
                                },
                                "ipv4-next-hop": {
                                    "global": "10.11.1.1"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@line 10: Injected route is present in *Application Peer's* Adj-RIB-In and Effective-RIB-In.

-----

**URL:** ``/restconf/operational/bgp-rib:bgp-rib/rib/bgp-example/loc-rib/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/ipv4-routes``

**Method:** ``GET``

**Response Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 3

    {
        "openconfig-bgp-inet:ipv4-routes": {
            "ipv4-route": [{
                "path-id": "0",
                "prefix": "10.0.0.10/32",
                "attributes": {
                    "origin": {
                        "value": "igp"
                    },
                    "local-pref": {
                        "pref": "100"
                    },
                    "ipv4-next-hop": {
                        "global": "10.11.1.1"
                    }
                }
            },
            {
                "path-id": "0",
                "prefix": "10.0.0.10/32",
                "attributes": {
                    "origin": {
                        "value": "igp"
                    },
                    "local-pref": {
                        "pref": "100"
                    },
                    "ipv4-next-hop": {
                        "global": "10.10.1.1"
                    }
                }
            }]
        }
    }

@line 3: The injected route is now present in Loc-RIB along with a route (destination *10.0.0.10/32*) advertised by remote peer.

-----

This route is also advertised to the remote peer (*192.0.2.1*), hence route appears in its Adj-RIB-Out:

**URL:** ``/restconf/operational/bgp-rib:bgp-rib/rib/bgp-example/peer/bgp:%2F%2F192.0.2.1/adj-rib-out/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes``

**Method:** ``GET``

**Response Body:**

.. code-block:: json

    {
        "openconfig-bgp-inet:ipv4-route": {
            "path-id": "0",
            "prefix": "10.0.0.11/32",
            "attributes": {
                "origin": {
                    "value": "igp"
                },
                "local-pref": {
                    "pref": "100"
                },
                "ipv4-next-hop": {
                    "global": "10.11.1.1"
                }
            }
        }
    }

-----

The injected route can be modified (i.e. different path attribute):

**URL:** ``/restconf/config/bgp-rib:application-rib/10.25.1.9/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes/ipv4-route/10.0.0.11%2F32/0``

**Method:** ``PUT``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json

    {
        "openconfig-bgp-inet:ipv4-route": {
            "path-id": "0",
            "prefix": "10.0.0.11/32",
            "attributes": {
                "origin": {
                    "value": "igp"
                },
                "local-pref": {
                    "pref": "50"
                },
                "ipv4-next-hop": {
                    "global": "10.11.1.2"
                }
            }
        }
    }

-----

The route can be removed from programmable RIB in a following way:

**URL:** ``/restconf/config/bgp-rib:application-rib/10.25.1.9/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes/ipv4-route/10.0.0.11%2F32/0``

**Method:** ``DELETE``

-----

Also it is possible to remove all routes from a particular table at once:

**URL:** ``/restconf/config/bgp-rib:application-rib/10.25.1.9/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes/``

**Method:** ``DELETE``

-----

Consequently, route disappears from programmable RIB, *Application Peer's* RIBs, Loc-RIB and peer's Adj-RIB-Out (UPDATE message with prefix withdrawal is send).

.. note:: Routes stored in programmable RIB are persisted on OpendDaylight shutdown and restored after the re-start.
