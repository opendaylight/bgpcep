.. _pcep-user-guide-path-computation:

Path Computation Server
=======================

This section describes how to use Path Computation Server bundle in
conjunction with the Path Computation Algorithm and Graph plugins. This Server
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

The Path Computation Server uses the Path Computation Algorithm plugin which
needs a graph to be able to compute constrained paths. Thus, a valid graph must
be provided manually or automatically.

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

Usage
^^^^^

There is two ways to use the Path Computation Server: through PcRequest and
with PcInitiate.

With PcRequest, just create a new tunnel on the router with an external PCE
for path computation. Once PcRequest received, the Path Computation Server
launches path computation algorithm with requested parameters and in turn sends
back to the PCC a PcResponse message with the computed path in the ERO.
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

Known limitations
^^^^^^^^^^^^^^^^^

As the Path Computation Server is in its initial release, there are some
limitations mention hereinafter:

* Following PCEP Objects that may be present in the PcRequest message are not
  yet supported, and right now, ignored:

  * Include Route Object (IRO)
  * Exclude Route Object (XRO)
  * Objective Function (OF)

* LSP-Update Rest API with an empty ERO will not trigger Path Computation
  Algorithm. Use Path Computation Algorithm Rest API to get a new path, and
  then use the LSP-Update Rest API as usual with the computed ERO.

* For Segment Routing, ERO is provided with Node SID for NAI and SID index.

* Due to the integration with BGP-LS, the graph name must start with *ted://*
  tag in order to be automatically used by the pcep plugin.

* For Segment Routing, as network resources are not updated due to the lack
  of signaling, the resources consumed by the new segment path are not updated
  in the graph.

All these limitations will be solved in future releases.

Known Bug
^^^^^^^^^

When using BGP-LS for automatic Graph topology acquisition, for an undetermined
reason, karaf is unable to start properly the *bgp-topology-provider* bundle.
This is due to karaf that doesn't properly manage blueprint dependencies. Thus,
BGP Topology Provider class is initialized with a wrong reference to the Graph
Topology Service: a null pointer is provided instead. However, it is easy to
overcome this issue by simply restarting the *bgp-topology-provider* bundle.

First identify the bundle number of *bgp-topology-provider* and check the
status.

.. code-block:: console

    opendaylight-user@karaf>bundle:list | grep bgp-topology-provider
    232 │ Failure  │  80 │ 0.14.0          │ bgp-topology-provider


Then restart the bundle if status is *Failure*

.. code-block:: console

    opendaylight-user@karaf>bundle:restart 232

And finaly, verify that the bundle is active

.. code-block:: console

    opendaylight-user@root>bundle:list 232
    START LEVEL 100 , List Threshold: 50
     ID │ State  │ Lvl │ Version         │ Name
    ────┼────────┼─────┼─────────────────┼───────────────────────
    232 │ Active │  80 │ 0.14.0          │ bgp-topology-provider


Looking to the log, you will normally see that a new Graph has been created and
fulfil with your network topology element. Using Graph Rest API *Get Operational
Graph* will also validate that all is running correctly.

