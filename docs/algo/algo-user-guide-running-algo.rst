.. _algo-user-guide-running-algo:

Running Path Computation
========================

Installation
^^^^^^^^^^^^

This section explains how to install Path Computation Algorithm plugin.

Install feature - ``features-algo``. Also, for sake of this sample, it is
required to install RESTCONF in order to use the Path Computation service.

In the Karaf console, type command:

.. code-block:: console

    feature:install features-restconf features-algo


Yang Model
^^^^^^^^^^

Path Computation algorithm used Graph plugin, thus graph API and yang models.
A new yang model has been introduced in order to define constrained path model
as well as a new RPC service to call Path Computation Algorithm plugin.
The model is given below:

.. code-block:: console

      module: path-computation
      +--rw constrained-path
         +--rw metric?             uint32
         +--rw te-metric?          uint32
         +--rw delay?              gr:delay
         +--rw jitter?             gr:delay
         +--rw loss?               gr:loss
         +--rw admin-group?        uint32
         +--rw address-family?     enumeration
         +--rw class-type?         uint8
         +--rw bandwidth?          gr:decimal-bandwidth
         +--rw source?             uint64
         +--rw destination?        uint64
         +--rw path-description* []
         |  +--rw ipv4?    inet:ipv4-address
         |  +--rw ipv6?    inet:ipv6-address
         |  +--rw label?   netc:mpls-label
         +--rw status?             computation-status

      rpcs:
         +---x get-constrained-path
            +---w input
            |  +---w graph-name     string
            |  +---w source?        uint64
            |  +---w destination?   uint64
            |  +---w constraints
            |  |  +---w metric?           uint32
            |  |  +---w te-metric?        uint32
            |  |  +---w delay?            gr:delay
            |  |  +---w jitter?           gr:delay
            |  |  +---w loss?             gr:loss
            |  |  +---w admin-group?      uint32
            |  |  +---w address-family?   enumeration
            |  |  +---w class-type?       uint8
            |  |  +---w bandwidth?        gr:decimal-bandwidth
            |  +---w algorithm?     algorithm-type
            +--ro output
               +--ro path-description* []
               |  +--ro ipv4?    inet:ipv4-address
               |  +--ro ipv6?    inet:ipv6-address
               |  +--ro label?   netc:mpls-label
               +--ro status?               computation-status
               +--ro computed-metric?      uint32
               +--ro computed-te-metric?   uint32
               +--ro computed-delay?       gr:delay


REST API
^^^^^^^^

This section provides how to use the Path Computation Service RPC that could be
used to request a path computation from a source to a destination with given
constraints over a given graph.

Get Constrained Path
''''''''''''''''''''

Path Computation algorithms are accessible through the RPC described below:

-----

**URL:** ``restconf/operations/path-computation:get-constrained-path``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json

   {
      "input": {
         "graph-name": "example",
         "source": 9,
         "destination": 4,
         "constraints": {
            "address-family": "ipv4",
            "te-metric": 250,
            "bandwidth": 100000000,
            "class-type": 0
         },
         "algorithm": "cspf"
      }
   }

@line 3: **graph-name** The name of the graph that must exist.

@line 4: **source** The source as vertex ID in the graph.

@line 5: **destination** - The destination as vertex ID in the graph.

@line 6: **constraints** - List of Constraints. Possible values are:
  * address-family (ipv4, ipv6, sr-ipv4 and sr-ipv6) - default ipv4
  * te-metric as integer value
  * bandwidth (byte/sec) as integer value
  * class-type for the bandwidth - default 0
  * delay (micro-second) as integer value

@line 12: **algorithm** - Type of Path Computation Algorithm. Valid options
are ``spf``, ``cspf`` and ``samcra`` - default ``spf``.

**Response Body:**

.. code-block:: json

   {
      "output": {
         "computed-metric": 210,
         "status": "completed",
         "path-description": [
               {
                  "ipv4": "10.194.77.143"
               },
               {
                  "ipv4": "10.194.77.155"
               },
               {
                  "ipv4": "10.194.77.161"
               }
         ]
      }
   }


Troubleshooting
^^^^^^^^^^^^^^^

Debug message could be activate with:

.. code-block:: console

    log:set DEBUG org.opendaylight.algo

Then check log with ``log:tail`` command.

In particular, if answer is ``failed`` check that source and destination
vertices are known in the graph and that constraints are not too huge.
A good advice is to start first by relaxing some constraints to see if
algorithm could find a valid path or not, and then re-enable constraints
one by one to find which one could not be met. Log will also provide
information about constraints that are not met during the path computation.
