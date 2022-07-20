.. _graph-user-guide-graph-model:

Graph Model Overview
====================
This section provides a high-level overview of the Graph Model.

.. contents:: Contents
   :depth: 2
   :local:

Graph Theory and Path Computation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The primary goal of Graph and Algorithms features, is to be able to compute
constrainted path among a given IP/MPLS network. Such path could be used to
enforce connectivity by means of RSVP-TE or Segment Routing TE through PCEP
protocol or simply provide a solution to answer a path request (coming from
RESTCONF API or a PcRequest message).

In IP/MPLS networks, these path computation algorithms need to access to a
representation of the network topology which is, in general, denoted as
*Traffic Engineering Database (TED)* in reference to information convey by
the IGP Traffic Engineering routing protocols such as OSPF-TE or IS-IS-TE.
There is many way to store this TED and the IETF is in the process of
defining the corresponding yang model (draft-ietf-teas-yang-te-topo-22.txt).
But, most of these path computation algorithms have been designed with a
particular representation of network model for perfomrance efficiency: a Graph.
This latter is denoted in literature as *G(V, E)* where V, the Vertices,
represent the nodes (e.g. routers) and E, the Edges, represent the link between
nodes. There is numerous graph type, but for path computation, a Directed and
Connected Graph is recommended, again for performance efficiency.

A Connected Graph is a particular graph type where each pair of vertices forms
the endpoints of a path. It is used because path computation algorithms need to
progress smoothly in the graph without searching for the next hops in the whole
graph (mostly for performance issue) and also because some algorithms need to
mark Vertices as visited once processed to avoid loop. Connected Graph is a
particular graph type where each pair of vertices forms the endpoints of a
path.

In general, for modern networks (i.e. with optical links), links are considered
as full-duplex. Thus, from a resources (mostly bandwidth) point of view, going
from node *A to B* will consumes resources (mostly bandwidth) on the link from
A to B while keeping intact resources on the link from B to A. So, the graph
model needs to reflect this behaviour. The graph that represents the network
will used Directed Edges for that purposes. So, the recommended graph is a
Directed Graph (sometimes also named Oriented Graph). As a consequence, it is
necessary to create 2 edges between 2 vertices: *A to B* and *B to A*.

For more information, reader could refer to Graph Theory
e.g. https://en.wikipedia.org/wiki/Graph_theory and Path Computation algorithms
e.g. Shortest Path First (SPF) https://en.wikipedia.org/wiki/Shortest_path_problem
and Constrainted Shortest Path First (CSPF) https://en.wikipedia.org/wiki/Constrained_Shortest_Path_First.

Yang Model for Graph
^^^^^^^^^^^^^^^^^^^^

In order to manipulate the Graph within the OpenDaylight Data Store, the given
yang model has been provided. It defines Edges and Vertices. Edges are enhanced
by Edges Attributes which are define all link attributes we could collect from
real network (e.g. through BGP Link State). Vertices are enhanced by Prefixes
in order to find quickly where End Points of a path are attached in the Graph.
It also serves to store Segment Routing information when computing path for
Segment Routing TE setup.

A new yang model is provided as an alternative to the IETF yang-te-topo model
and IETF RFC8345 for several reasons:

* Some link and node parameters (e.g. Segment Routing, TE Metric extensions)
  which are available in IP/MPLS networks, through the IGP-TE or BGP-LS
  protocols (see Linkstate Routes in BGP RIB) are not present in the IETF
  ted model
* Node and link identifiers are represented as strings in the IETF ted model,
  which it is not efficient when looking into a HashMap() to find a node or
  a link by its identifier
* Even if LinkstateTopologyBuilder() provided mechanism to fulfil IETF ted
  model in the datastore, the NodeHolder an TpHolder classes have been
  defined as private, and thus could not be used outside the
  LinkstateTopologyBuilder() class

Graph and Algorithm have been also designed to be used by other projects
(e.g. openflow) which not control IP/MPLS network. Thus, even if the graph
model addresses in first case the IP/MPLS network, its genericity allows
it to be suitable for other network types.

The yand model for the Graph is as follow:

.. code-block:: console

        module: graph
        +--rw graph-topology
            +--rw graph* [name]
                +--rw name            string
                +--rw domain-scope?   enumeration
                +--rw asn?            uint32
                +--rw vertex* [vertex-id]
                |  +--rw vertex-id      uint64
                |  +--rw name?          string
                |  +--rw router-id?     inet:ipv4-address
                |  +--rw router-id6?    inet:ipv6-address
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
                |     +--rw admin-group?               uint32
                |     +--rw local-address?             inet:ipv4-address
                |     +--rw remote-address?            inet:ipv4-address
                |     +--rw local-address6?            inet:ipv6-address
                |     +--rw remote-address6?           inet:ipv6-address
                |     +--rw local-identifier?          uint32
                |     +--rw remote-identifier?         uint32
                |     +--rw max-link-bandwidth?        decimal-bandwidth
                |     +--rw max-resv-link-bandwidth?   decimal-bandwidth
                |     +--rw unreserved-bandwidth* [class-type]
                |     |  +--rw class-type    uint8
                |     |  +--rw bandwidth?    decimal-bandwidth
                |     +--rw delay?                     delay
                |     +--rw min-max-delay
                |     |  +--rw min-delay?   delay
                |     |  +--rw max-delay?   delay
                |     +--rw jitter?                    delay
                |     +--rw loss?                      loss
                |     +--rw residual-bandwidth?        decimal-bandwidth
                |     +--rw available-bandwidth?       decimal-bandwidth
                |     +--rw utilized-bandwidth?        decimal-bandwidth
                |     +--rw adj-sid?                   uint32
                |     +--rw backup-adj-sid?            uint32
                |     +--rw adj-sid6?                  uint32
                |     +--rw backup-adj-sid6?           uint32
                |     +--rw srlgs*                     uint32
                +--rw prefix* [prefix]
                +--rw prefix        inet:ip-prefix
                +--rw prefix-sid?   uint32
                +--rw node-sid?     boolean
                +--rw vertex-id?    uint64

Java Class for Connected Graph
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Yang model represents data as a Flat Tree hierarchy. However, this particular
graph representation (without a specific storage engine capabilities) is not
very useful for Path Computation due to lower performance compared to other
Graph types. Of course path computation algorithms could play with a such
Graph, but at the cost of performance issue as algorithms need to search the
neighbours of a vertices at each step when progressing in the graph. This will
decrease the performance by a factor of *N* to *N²* depending of the
algorithms. For large scale network, say 1000+ nodes, it is too high.

Yang syntax authorizes reference to other grouping or leaf with 'leafref'.
This could allows from a Vertex to access to Edges. However, it is not possible
to achieve a cross reference between Vertex and Edge. In Connected Graph,
both Vertex and Edge must reference each together: from Vertex it is needed to
access directly at the list of Edges connected to this Vertex, and from Edge,
it is need to access directly at the source and destination Vertex.

So, to overcome this limitation, the implemented Graph is composed of two
pieces:

* A standard Graph modeled in yang and stored in the Data Store
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

Note that the Unique Key identifier for Connected Edge and Connected Vertex
must not be equal to zero (and as a consequence the Edge and Vertex key).
This restriction is due to some algorithms that used the value 0 as a
special indication during the path computation.

