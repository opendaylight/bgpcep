.. _graph-user-guide-graph-model:

Graph Model Overview
====================
This section provides a high-level overview of the Graph Model.

.. contents:: Contents
   :depth: 2
   :local:

Graph Theodry and Path Computation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The primary goal of Graph and Algorithms features, is to be able to compute
constraints path amog a given network topology. Such path could be used to
enforce connectivity by means of RSVP-TE or Segment Routing TE through PCEP
protocol or simply provide a solution to answer a path request (coming from
RestConf API or PCRequest message).

Path computation is generally performed by Constraints Shortest Path First
algoritms. These algorithms need to have a representation of the network
topology. A common way to represent a network topology, is to used Graph
denoted *G(V, E)* where V the Vertices represent the nodes (e.g. routers)
and Edges represent the lnk between nodes. There is numerous graph type, but
for our objectives, we need to dispose of a Directed and Connected Graph.

Connected Graph is a graph where each pair of vertices forms the endpoints of
a path. This corresponds a real network where links that forms a path are
connected to a pair of nodes. Path computation algorithms need also such
connected graph in order to progress smoothly in the graph without the need to
search for the next hops in the whole graph (mostly for performance issue) and
also because some algorithms need to mark Vertices as visited once processed
to avoid loop.

Of course in real network, link are mostly bidirectional. But, going from node
*A to B* will consumes resources (mostly bandwidth) on the link from A to B
while keeping intact resources on the link from B to A. Thus, the network model
needs to reflect this behaviour. The graph that represents the network will
used Directed Edges for that purposes. So, the Graph Model is a Directed Graph
(sometimes also named Oriented Graph). As a consequence, it is necessary to
create 2 edges between 2 vertices: *A to B* and *B to A*.

Reader could refer to Graph Theory for more information (e.g. https://en.wikipedia.org/wiki/Graph_theory)
and Path computation algorithms (e.g. https://en.wikipedia.org/wiki/Shortest_path_problem)

Yang Model for Graph
^^^^^^^^^^^^^^^^^^^^

In order to manipulate the Graph within the OpenDayLight Data Store, the given
yang model has been provided. It defines Edges and Vertices. Edges are enhanced
by Edges Attributes which are define all link attributes we could collect from
real network (e.g. through BGP Link State). Vertices are enhanced by Prefixes
in order to find quickly where End Points of a path are attached in the Graph.
It also serves to store Segment Routing information when computing path for
Segment Routing TE setup.

The yand model for the Graph is as follow:

.. code-block:: console

        module: graph
        +--rw graph-topology
        +--rw graph* [name]
                +--rw name          string
                +--rw graph-type?   enumeration
                +--rw asn?          uint32
                +--rw vertex* [vertex-id]
                |  +--rw vertex-id      uint64
                |  +--rw name?          string
                |  +--rw router-id?     inet:ip-address
                |  +--rw vertex-type?   enumeration
                |  +--rw srgb
                |  |  +--rw lower-bound?   uint32
                |  |  +--rw range-size?    uint32
                |  +--rw asn?           uint32
                +--rw edge* [edge-id]
                |  +--rw edge-id             uint64
                |  +--rw local-vertex-id?    uint64
                |  +--rw remote-vertex-id?   uint64
                |  +--rw name?               string
                |  +--rw edge-attributes
                |     +--rw metric?                    uint32
                |     +--rw te-metric?                 uint32
                |     +--rw color?                     uint32
                |     +--rw local-address?             inet:ip-address
                |     +--rw remote-address?            inet:ip-address
                |     +--rw local-identifier?          uint32
                |     +--rw remote-identifier?         uint32
                |     +--rw max-link-bandwidth?        decimal64
                |     +--rw max-resv-link-bandwidth?   decimal64
                |     +--rw unreserved-bandwidth*      decimal64
                |     +--rw delay?                     delay
                |     +--rw min-max-delay
                |     |  +--rw min-delay?   delay
                |     |  +--rw max-delay?   delay
                |     +--rw jitter?                    delay
                |     +--rw loss?                      loss
                |     +--rw residual-bandwidth?        decimal64
                |     +--rw available-bandwidth?       decimal64
                |     +--rw utilized-bandwidth?        decimal64
                |     +--rw adj-sid?                   uint32
                |     +--rw backup-adj-sid?            uint32
                |     +--rw srlgs*                     uint32
                +--rw prefix* [prefix]
                +--rw prefix        inet:ip-prefix
                +--rw prefix-sid?   uint32
                +--rw vertex-id?    uint64

Java Class for Connected Graph
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If it is possible to model Directed Edges, thus Directed Graph, in yang, it is
impossible to model Connected Graph. Indeed, yang allows to model object that
are stored in a flat tree hierarchy. Even if such flat tree is a particular
Graph, it is not suitable for shortest path algorithms. Of course path
computation algorithms could play with a such Graph, but at the cost of
performance issue as algorithms need to search the neighbours of a vertices at
each step when progressing in the graph. This will decrease the performance by
a factor of *N* to *N²* depending of the algorithms. For large scale network,
say 1000+ nodes, it is too high.

So, to overcomes this limitation, the implemented Graph is composed of two
pieces:

 * A standard Graph modeled in yang and stored in the Data stored
 * A Connected Graph version based on the yang model but stored in memory only


The connected version of Vertex is composed of:

.. code-block:: java

    /* Reference to input and output Connected Edge within the Connected Graph */
    private ArrayList<ConnectedEdgeImpl> input = new ArrayList<>();
    private ArrayList<ConnectedEdgeImpl> output = new ArrayList<>();

    /* List of Prefixes announced by this Vertex */
    private ArrayList<Prefix> prefixes = new ArrayList<>();

    /* Reference to the Vertex of the standard Graph associated to the Connected Graph */
    private Vertex vertex = null;

Where distinction is made between input and output Edges in order to respect the Directed Graph
behviour.

The connected version of Edges is composed of:

.. code-block:: java

    /* Reference to Source and Destination Connected Vertex within the Connected Graph */
    private ConnectedVertexImpl source;
    private ConnectedVertexImpl destination;

    /* Reference to the Edge within the Graph associated to the Connected Graph */
    private Edge edge;

Where source and destination Vertices also ease to implement the Directed Graph.

And finally, the connected version of Graph is composed of:

.. code-block:: java

    /* List of Connected Vertics that composed this Connected Graph */
    private final HashMap<Long, ConnectedVertexImpl> vertices = new HashMap<>();

    /* List of Connected Edges that composed this Connected Graph */
    private final HashMap<Long, ConnectedEdgeImpl> edges = new HashMap<>();

    /* List of IP prefix attached to Vertices */
    private final HashMap<IpPrefix, Prefix> prefixes = new HashMap<>();

    /* Reference to the non connected Graph stored in DataStore */
    private Graph graph;

Where Vertices, Edges and Prefixes are stored in *HashMap* to speed up the
access of a given element of the Graph.



