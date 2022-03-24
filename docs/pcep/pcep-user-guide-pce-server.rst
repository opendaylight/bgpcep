.. _pcep-user-guide-pce-server:

Path Computation Element Server
===============================

This section describes how to use Path Computation Element (PCE) Server bundle
in conjunction with the Path Computation Algorithm and Graph plugins. This
provides a full PCE component that fully supports RFC5440 including PcRequest
PcResponse messages exchanges from a PCC requesting a valid path to the PCE.

.. contents:: Contents
   :depth: 2
   :local:

Installation
^^^^^^^^^^^^

Check that the feature ``features-algo`` is installed. Normally it will be
installed with the pcep feature ``features-pcep``. Otherwise, install it
with the following command:

.. code-block:: console

    feature:install features-algo

Graph Setup
^^^^^^^^^^^

The PCE Server uses the Path Computation Algorithm plugin which needs a graph
to be able to compute constrained paths. Thus, a valid graph must be provided
manually or automatically.

Manual activation
'''''''''''''''''

Create a new graph with the Rest API ``Create Graph``:

.. code-block:: console

    PUT: restconf/config/graph:graph-topology

Refer to Graph documentation for details.

There is a restriction on the name of the graph due to the Path Computation
Algorithm integration. It must be started by **"ted://"** string in order to
be learn automatically by the Path Computation Server bundle.

Note that this kind of graph remains static. Thus, resources, mostly bandwidth,
are not updated after deploying some RSVP-TE tunnels which consume bandwidth.

BGP-LS activation
'''''''''''''''''

To achieve better experience, notably in conjunction with RSVP-TE and in order
to work on an up-to-date graph, an integration is provided with the BGP Link
State protocol. This allows to automatically fulfil a graph with network
traffic engineering information conveyed by the BGP-LS protocol. The resources,
mostly bandwidth, are automatically updated in the graph after deploying
an RSVP-TE tunnel. Note that this is not the case with Segment Routing.

For that purpose, just setup a BGP peering with a router that is BGP-LS
speaker and report traffic engineering network topology from IS-IS-TE or
OSPF-TE routing protocol. Refer to BGP documentation for the detail about
how to setup a BGP peering with Link-State family.

Once done, verify that the graph is correctly fulfil with the Rest API:

.. code-block:: console

    GET: restconf/operational/graph:graph-topology

Basic Usage
^^^^^^^^^^^

There is two ways to use the PCE Server: through PcRequest and with PcInitiate.

With PcRequest, just create a new tunnel on the router with an external PCE
for path computation. Once PcRequest received, the PCE Server launches the path
computation algorithm with requested parameters and in turn sends back to the
PCC a PcResponse message with the computed path in the ERO.
A NO-PATH object is returned in case of failure with the reason (e.g. source
or destination unknown, constraints not met ...). Check on the router that
the tunnel is up and running. Wireshark capture will help to determine
if the exchanges between the PCC and the PCE went well. Setting log debug for
algo and pcep plugins and looking to the log will also ease debugging.

With PcInitiate message, just use the PCEP Rest API to setup an LSP

.. code-block:: console

    POST: /restconf/operations/network-topology-pcep:add-lsp

by omitting the ERO Object. Indeed, an automatic call to the Path Computation
Algorithm will be triggered when the ERO is absent or empty with the given
end-points and metrics objects as input paramters. Address family is
automatically deduced from the IP address family of the end-points object.
The same behaviour applies for Segment Routing: just add the *PST=1* indication
in the json or xml payload will force the address family of path computation
to Segment Routing.

To verify the result, just check the LSP-Database. The new LSP must have an
ERO automatically computed as well as an RRO. Again, setting log debug for algo
and pcep plugins and looking to the log will also help to verify that all is
conform as expected.

Advance Usage
^^^^^^^^^^^^^

A new Path Manager service has been added withing the PCE Server. This Path
Manager allows:

* The management of LSPs, in particular to update them without the need to
  manually compute a path
* The possibility to provide an ERO to reported LSPs without a valid path
* The Persistency of Initiated and Updated LSPs accross PCC and or PCE reboot
* The update of reported LSP from PCC with an empty ERO. For such reported LSP,
  a path computation based on the LSP constraints is automatically triggered.
  If a path is found, it is automatically enforced through a PcUpdate message.

In order to be able to manage tunnels (RSVP-TE or Segment Routing) a new
yang model has been added within the pcep configuration with the following
schema:

.. code-block:: console

  module: pcep-server

  augment /nt:network-topology/nt:topology/nt:node/topo:path-computation-client:
    +--ro configured-lsp* [name]
       +--ro name             string
       +--ro path-status?     path-status
       +--ro intended-path
       |  +--ro source?           inet:ip-address
       |  +--ro destination?      inet:ip-address
       |  +--ro constraints
       |     +--ro metric?           uint32
       |     +--ro te-metric?        uint32
       |     +--ro delay?            gr:delay
       |     +--ro jitter?           gr:delay
       |     +--ro loss?             gr:loss
       |     +--ro admin-group?      uint32
       |     +--ro address-family?   enumeration
       |     +--ro class-type?       uint8
       |     +--ro bandwidth?        gr:decimal-bandwidth
       |     +--ro include-route* []
       |     |  +--ro ipv4?   inet:ipv4-address
       |     |  +--ro ipv6?   inet:ipv6-address
       |     +--ro exclude-route* []
       |        +--ro ipv4?   inet:ipv4-address
       |        +--ro ipv6?   inet:ipv6-address
       +--ro computed-path
          +--ro path-description* []
          |  +--ro ipv4?          inet:ipv4-address
          |  +--ro ipv6?          inet:ipv6-address
          |  +--ro sid?           uint32
          |  +--ro local-ipv4?    inet:ipv4-address
          |  +--ro remote-ipv4?   inet:ipv4-address
          |  +--ro local-ipv6?    inet:ipv6-address
          |  +--ro remote-ipv6?   inet:ipv6-address
          +--ro computation-status?   algo:computation-status
  augment /nt:network-topology/nt:topology/nt:node:
    +--rw configured-lsp* [name]
       +--rw name             string
       +--ro path-status?     path-status
       +--rw intended-path
       |  +--rw source?           inet:ip-address
       |  +--rw destination?      inet:ip-address
       |  +--rw routing-method?   routing-type
       |  +--rw constraints
       |     +--rw metric?           uint32
       |     +--rw te-metric?        uint32
       |     +--rw delay?            gr:delay
       |     +--rw jitter?           gr:delay
       |     +--rw loss?             gr:loss
       |     +--rw admin-group?      uint32
       |     +--rw address-family?   enumeration
       |     +--rw class-type?       uint8
       |     +--rw bandwidth?        gr:decimal-bandwidth
       |     +--rw include-route* []
       |     |  +--rw ipv4?   inet:ipv4-address
       |     |  +--rw ipv6?   inet:ipv6-address
       |     +--rw exclude-route* []
       |        +--rw ipv4?   inet:ipv4-address
       |        +--rw ipv6?   inet:ipv6-address
       +--ro computed-path
          +--ro path-description* []
          |  +--ro ipv4?          inet:ipv4-address
          |  +--ro ipv6?          inet:ipv6-address
          |  +--ro sid?           uint32
          |  +--ro local-ipv4?    inet:ipv4-address
          |  +--ro remote-ipv4?   inet:ipv4-address
          |  +--ro local-ipv6?    inet:ipv6-address
          |  +--ro remote-ipv6?   inet:ipv6-address
          +--ro computation-status?   algo:computation-status

Usual REST API could be used against the pcep network topology config schema
of the Data Store to create, update and remove new tunnels.

REST API
^^^^^^^^

Get PCE tunnels
'''''''''''''''

Tunnels are stored in configuration Data Store and are accesible through the
``network-topology:network-topology/topology=pcep-topology`` namespace in both
operational (with ``?content=nonconfig``) and onfiguration (with
``?content=config``) as follow:

-----

**RFC8040:** ``restconf/data/network-topology:network-topology/topology=pcep-topology``

**Method:** ``GET``

**Response Body:**

.. code-block:: json
   :linenos:

    {
        "network-topology:topology": [
            {
                "node": [
                    {
                       "node-id": "10.1.1.1",
                        "pcep-server:configured-lsp": [
                            {
                                "name": "test-sr",
                                "intended-path": {
                                    "destination": "10.2.2.2",
                                    "source": "10.1.1.1",
                                    "constraints": {
                                        "bandwidth": "100000",
                                        "class-type": 1,
                                        "metric": 500,
                                        "address-family": "sr-ipv4"
                                    }
                                }
                            }
                        ]
                    }
                ]
            }
        ]
    }

Once Tunnels enforced on a PCC, there are available in the operational Data
Store under the same namespace within the ``pcep-server:configuredi-lsp`` table
for each PCC.

When getting the tunnel from the operational Data Store, state and computed
path are also reported:

.. code-block:: json
   :linenos:

    {
        "network-topology:topology": [
            {
                "node": [
                    {
                        "node-id": "10.1.1.1",
                        "pcep-server:configured-lsp": [
                            {
                                "name": "test-sr",
                                "intended-path": {
                                    "destination": "10.1.1.1",
                                    "source": "10.2.2.2",
                                    "constraints": {
                                        "bandwidth": "100000",
                                        "class-type": 1,
                                        "metric": 500,
                                        "address-family": "sr-ipv4"
                                    }
                                },
                                "computed-path": {
                                    "path-description": [
                                        {
                                            "remote-ipv4": "10.0.1.3",
                                            "local-ipv4": "10.0.1.1",
                                            "sid": 113
                                        },
                                        {
                                            "remote-ipv4": "10.0.2.2",
                                            "local-ipv4": "10.0.3.2",
                                            "sid": 112
                                        }
                                    ],
                                    "computation-status": "completed"
                                },
                                "path-status": "sync"
                            }
                        ]
                    }
                ]
            }
        ]
    }

The ``path-status`` indicate if the status of the configured tunnel, in
particular if it is in failure, or correctly configured (sync).

Note that tunnels that are only reported by a PCC and for which
no particular configuration has been setup are not provided the model
``pcep-server:configured-lsp`` within the node-id schema.

Create a tunnel:
''''''''''''''''

To add a tunnel or a set of tunnels on a given PCC, just create new entry in
the configuration as follow:

-----

**RFC8040:** ``restconf/data/network-topology:network-topology/topology=pcep-topology/node=10.1.1.1``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 4,8,11,12

    {
        "pcep-server:configured-lsp": [
            {
                "name": "test",
                "intended-path": {
                    "destination": "10.2.2.2",
                    "source": "10.1.1.1",
                    "constraints": {
                        "bandwidth": "100000",
                        "class-type": 1,
                        "metric": 500,
                        "address-family": "ipv4"
                    }
                }
            }
        ]
    }

@line 4: **name** The tunnel identifier. Must be unique.

@line 8: **constraints** Constraints that the path compputation algorithm
should respect to determine the path of the tunnel. Note that if no path
is found, the tunnel is not enforced in the PCC and ``computation-status``
within the ``computed-path`` is set to failed.

@line 11: Specify which type of metric is used to compute the path:
``metric`` (standard IGP metric), ``te-metric`` (TE metric) or ``delay``

@line 12: **address-family** Indicate the IP family of the tunnel: ``ipv4`` or
``ipv6`` for IPv4 respectively IPv6 RSVP-TE tunnel, ``sr-ipv4`` or ``sr-ipv6``
for IPv4 respectively IPv6 Segment Routing tunnel.

Update a tunnel
'''''''''''''''

The procedure is the same as for the creation. Just used the ``PUT`` method
instead of the ``POST`` mest for the REST API. The json body follows the same
yang model. Note that it is not allowed to change end points of the tunnel i.e.
the source and destination. If such modification is required, you must first
remove the tunnel and then create a new one with the new end points.

Remove a tunnel
'''''''''''''''

This simply done by removing the corresponding entry in the configuration by
using the ``DELETE`` method as follow:

**URL:** ``restconf/data/network-topology:network-topology/topology=pcep-topology/node=10.1.1.1/pcep-server:configured-lsp=test``

**Method:** ``DELETE``

Close Loop
^^^^^^^^^^

Each Managed TE Path automatically registers its current path within the
Connected Graph whih serves to compute the route. In case of failure (Link or
Node removal) or Link or Node attributes modifications in the Graph, registered
Managed TE Path are trigger against those modifications. This feature allows
the Path Manager to automatically detects problems in the underlying network
topology and made appropriate action (i.e. mostly path re-computation and new
computed path enforcement) in order to ensure that the constraints of the
Managed TE Path are always guaranteed.

Known limitations
^^^^^^^^^^^^^^^^^

As the PCE Server is in its initial release, there are some limitations
mentioned hereinafter:

* Following PCEP Objects that may be present in the PcRequest message are not
  yet supported, and right now, ignored:

  * Objective Function (OF)

* For Segment Routing, ERO is only provided with Adjacency NAI type and Adjacency SID.

* Due to the integration with BGP-LS, the graph name must start with *ted://*
  tag in order to be automatically used by the pcep plugin.

All these limitations will be solved in future releases.

