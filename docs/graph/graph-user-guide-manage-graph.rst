.. _graph-user-guide-manage-graph:

Manage Graph
============
This section explains how to manipulate the Graph through REST API.

.. contents:: Contents
   :depth: 2
   :local:

Concept
^^^^^^^

The connected Graph is not accessible through the REST API as it is not
posssible to represent connected Edges and Vertices with yang model (see
Graph Overview). Thus, Graph feature provides a new service named
ConnectedGraphProvider published in Karaf. This service maintains an
up-to-date list of Connected Graph stored in memory and allows to:

  * Get Connected Graph by name or GraphKey
  * Create Connected Graph
  * Add existing graph to create associated Connected Graph
  * Delete existing Graph identify by its GraphKey

Then, Connected Graph provides method to manage Vertices, Edges and Prefix.
The ConnectedGraphProvider is also in charge to maintain up to date the Graph
associated to the Connected Graph in the OpenDaylight operational Data Store.

In fact, two graphs are stored in the Data Store:

 * Operational Graph in ``restconf/operational`` which is the graph
   associated with the Connected Graph stored in memory
 * Configuration Graph in ``restconf/config`` which is the graph that
   could be create / modify / delete in order to produce the Connected
   Graph and thus, the associated Graph stored in operational Data Store

It is also possible to add / delete Vertices, Edges and Prefix on an existing
Graph through the REST API.


JAVA API
^^^^^^^^

Developper will refer to the Java Doc to manage Connected and standard Graph.
The main principle is as follow:

1. First Create a Connected Graph through the ConnectedGraphProvider which is a
new service provided by the Graph feature through Karaf:

.. code-block:: java

  ConnectedGraph cgraph = graphProvider.createConnectedGraph("example", GraphType.IntraDomain);

Or by adding an initial graph:

.. code-block:: java

  Graph graph = new GraphBuilder().setName("example").setGraphType(GraphType.IntraDomain).build();
  ConnectedGraph cgraph = graphProvider.addGraph(graph);

Where graphProvider is obtained from blueprint.

2. Once created, the Connected Graph offers various method to manage Vertices
and Edges. For example:

.. code-block:: java

  /* Add a Vertex */
  Vertex vertex = new VertexBuilder().setVertexId(Uint64.ValueOf(1)).build();
  cgraph.addVertex(vertex);

  /* Add an Edge */
  Edge edge = new EdgeBuilder().setEdgeId(Uint64.ValueOf(1)).build();
  cgraph.addEdge(edge);

  ...

REST API
^^^^^^^^

This section provides the list of REST method that could be used to manage
Graph.

Get Graph
'''''''''

Graphs are stored in operation and config Data Store. Thus there are accessible
through the ``graph:graph-topology`` namespace as follow:

-----

**URL:** ``restconf/operational/graph:graph-topology``

**Method:** ``GET``

**Response Body:**

.. code-block:: json

  {
      "graph-topology": {
          "graph": [
              {
                  "name": "example",
                  "vertex": [
                      {
                          "vertex-id": 2,
                          "name": "r2",
                          "vertex-type": "standard"
                      },
                      {
                          "vertex-id": 1,
                          "name": "r1",
                          "vertex-type": "standard"
                      }
                  ],
                  "graph-type": "intra-domain"
              }
          ]
      }
  }

Graphs publish in the configuration Data Store are also accessible through REST
API with the same namespace as follow:

-----

**URL:** ``restconf/config/graph:graph-topology``

**Method:** ``GET``

**Response Body:**

.. code-block:: json

  {
      "graph-topology": {
          "graph": [
              {
                  "name": "example",
                  "vertex": [
                      {
                          "vertex-id": 2,
                          "name": "r2",
                          "vertex-type": "standard"
                      },
                      {
                          "vertex-id": 1,
                          "name": "r1",
                          "vertex-type": "standard"
                      }
                  ],
                  "graph-type": "intra-domain"
              }
          ]
      }
  }

Create Graph
''''''''''''

Graphs could be created with PUT method. In this case, all previously
configured graphs are removed from both the configuration and operational
Data Store. This includes all modification and associated Connected Graphs.

-----

**URL:** ``restconf/config/graph:graph-topology``

**Method:** ``PUT``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json

  {
      "graph-topology": {
          "graph": [
              {
                  "name": "example",
                  "graph-type": "intra-domain",
                  "vertex": [
                      {
                          "vertex-id": 1,
                          "name": "r1"
                      },
                      {
                          "vertex-id": 2,
                          "name": "r2"
                      }
                  ],
                  "edge": [
                      {
                          "edge-id": 1,
                          "name": "r1 - r2",
                          "local-vertex-id": 1,
                          "remote-vertex-id": 2
                      },
                      {
                          "edge-id": 2,
                          "name": "r2 - r1",
                          "local-vertex-id": 2,
                          "remote-vertex-id": 1
                      }
                  ]
              }
          ]
      }
  }

@line 5: **name** The Graph identifier. Must be unique.

@line 6: **graph-type** The type of the Graph: intra-domain or inter-domain.

@line 7: **vertex** - List of Vertices. Each Vertex ID must be unique.

@line 17: **edges** - List of Edges. Each Edge ID must be unique.

@line 21: **local-vertex-id** - Vertex ID where the Edge is connected from.
The vertex ID must correspond to vertex that is present in the vertex list,
otherwise, the connection will not be estabished in the Connected Graph.

@line 22: **remote-vertex-id** - Vertex ID where the Edge is connected to.
The vertex ID must correspond to vertex that is present in the vertex list,
otherwise, the connection will not be estabished in the Connected Graph.

Add Graph
'''''''''

It is also possible to add a Graph to the existing list. POST method will
be used instead of PUT. Body and URL remains the same.

Delete Graph
''''''''''''

Removing a graph used the DELETE method as follow:

-----

**URL:** ``restconf/config/graph:graph-topology/graph/example``

**Method:** ``DELETE``

The name of the graph i.e. the Graph Key to be deleted must be provide
within the URL.

Add Vertices
''''''''''''

One or more vertex could be added to a Graph. If the graph doesn't exist,
it will be automatically created. Only POST method must be used.

-----

**URL:** ``restconf/config/graph:graph-topology/graph/example``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json

  {
      "vertex": [
          {
              "vertex-id": 100,
              "name": "r100",
              "router-id": "192.168.1.100"
          }
      ]
  }

Delete Vertex
'''''''''''''

Removing a vertex used the DELETE method as follow:

-----

**URL:** ``restconf/config/graph:graph-topology/graph/example/vertex/10``

**Method:** ``DELETE``

The Vertex to be deleted is identified by its Vertex Id and must be provide
within the URL.

Add Edges
'''''''''

One or more edges could be added to a Graph. If the graph doesn't exist,
it will be automatically created. Only POST method must be used.

-----

**URL:** ``restconf/config/graph:graph-topology/graph/example``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json

  {
      "edge": [
          {
              "edge-id": 10,
              "name": "r1 - r2",
              "local-vertex-id": 1,
              "remote-vertex-id": 2
          },
          {
              "edge-id": 20,
              "name": "r2 - r1",
              "local-vertex-id": 2,
              "remote-vertex-id": 1
          }
      ]
  }

Delete Edge
'''''''''''

Removing an edge used the DELETE method as follow:

-----

**URL:** ``restconf/config/graph:graph-topology/graph/example/edge/10``

**Method:** ``DELETE``

The Edge to be deleted is identified by its Edge Id and must be provide
within the URL.

Add Prefixes
''''''''''''

One or more prefixe could be added to a Graph. If the graph doesn't exist,
it will be automatically created. Only POST method must be used.

-----

**URL:** ``restconf/config/graph:graph-topology/graph/example``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json

  {
      "prefix": [
          {
              "prefix": "192.168.1.0/24",
              "vertex-id": 1
          }
      ]
  }

Delete Prefix
'''''''''''''

Removing a prefix used the DELETE method as follow:

-----

**URL:** ``restconf/config/graph:graph-topology/graph/example/prefix/192%2e168%2e1%2e0%2f24``

**Method:** ``DELETE``

The Prefix to be deleted is identified by its Prefix Id and must be provide
within the URL. As the prefix identifier is the ip prefix, '.' and '/' must
be replace by their respective ASCII representation i.e. '%2e' for dot and
'%2f' for slash.

