.. _bgp-user-guide-topology-provider:

Topology Provider
=================
This section provides an overview of the BGP topology provider service.
It shows how to configure and use all available BGP topology providers.
Providers are building topology view of BGP routes stored in local BGP speaker's Loc-RIB.
Output topologies are rendered in a form of standardised IETF network topology model.

.. contents:: Contents
   :depth: 2
   :local:

Inet Reachability Topology
^^^^^^^^^^^^^^^^^^^^^^^^^^
Inet reachability topology exporter offers a mapping service from IPv4/6 routes to network topology nodes.

Configuration
'''''''''''''
Following example shows how to create a new instance of IPv4 BGP topology exporter:

**URL:** ``/restconf/config/network-topology:network-topology``

**RFC8040 URL:** ``/rests/data/network-topology:network-topology``

**Method:** ``POST``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 2,4,6

         <topology xmlns="urn:TBD:params:xml:ns:yang:network-topology">
             <topology-id>bgp-example-ipv4-topology</topology-id>
             <topology-types>
                 <bgp-ipv4-reachability-topology xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-types"></bgp-ipv4-reachability-topology>
             </topology-types>
             <rib-id xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-config">bgp-example</rib-id>
         </topology>

      @line 2: An identifier for a topology.

      @line 4: Used to identify type of the topology. In this case BGP IPv4 reachability topology.

      @line 6: A name of the local BGP speaker instance.

   .. tab:: JSON`

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,6,8

         {
             "topology": [
                 {
                     "topology-id": "bgp-example-ipv4-topology",
                     "topology-types": {
                         "odl-bgp-topology-types:bgp-ipv4-reachability-topology": {}
                     },
                     "odl-bgp-topology-config:rib-id": "bgp-example"
                 }
             ]
         }

      @line 4: An identifier for a topology.

      @line 6: Used to identify type of the topology. In this case BGP IPv4 reachability topology.

      @line 8: A name of the local BGP speaker instance.

-----

The topology exporter instance can be removed in a following way:

**URL:** ``/restconf/config/network-topology:network-topology/topology/bgp-example-ipv4-topology``

**RFC8040 URL:** ``/rests/data/network-topology:network-topology/topology=bgp-example-ipv4-topology``

**Method:** ``DELETE``

-----

Following example shows how to create a new instance of IPv6 BGP topology exporter:

**URL:** ``/restconf/config/network-topology:network-topology``

**RFC8040 URL:** ``/rests/data/network-topology:network-topology``

**Method:** ``POST``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml

         <topology xmlns="urn:TBD:params:xml:ns:yang:network-topology">
             <topology-id>bgp-example-ipv6-topology</topology-id>
             <topology-types>
                 <bgp-ipv6-reachability-topology xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-types"></bgp-ipv6-reachability-topology>
             </topology-types>
             <rib-id xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-config">bgp-example</rib-id>
         </topology>

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json

         {
             "topology": [
                 {
                     "topology-id": "bgp-example-ipv6-topology",
                     "odl-bgp-topology-config:rib-id": "bgp-example",
                     "topology-types": {
                         "odl-bgp-topology-types:bgp-ipv6-reachability-topology": {}
                     }
                 }
             ]
         }

Usage
'''''
Operational state of the topology can be verified via REST:

**URL:** ``/restconf/operational/network-topology:network-topology/topology/bgp-example-ipv4-topology``

**RFC8040 URL:**: ``/rests/data/network-topology:network-topology/topology=bgp-example-ipv4-topology?content=nonconfig``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Response Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 8,11

         <topology xmlns="urn:TBD:params:xml:ns:yang:network-topology">
             <topology-id>bgp-example-ipv4-topology</topology-id>
             <server-provided>true</server-provided>
             <topology-types>
                 <bgp-ipv4-reachability-topology xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-types"></bgp-ipv4-reachability-topology>
             </topology-types>
             <node>
                 <node-id>10.10.1.1</node-id>
                 <igp-node-attributes xmlns="urn:TBD:params:xml:ns:yang:nt:l3-unicast-igp-topology">
                     <prefix>
                         <prefix>10.0.0.10/32</prefix>
                     </prefix>
                 </igp-node-attributes>
             </node>
         </topology>

      @line 8: The identifier of a node in a topology. Its value is mapped from route's NEXT_HOP attribute.

      @line 11: The IP prefix attribute of the node. Its value is mapped from routes's destination IP prefix.

   .. tab:: JSON

      **Response Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 11,15

         {
             "topology": [
                 {
                     "topology-id": "bgp-example-ipv4-topology",
                     "server-provided": true,
                     "topology-types": {
                         "odl-bgp-topology-types:bgp-ipv4-reachability-topology": {}
                     },
                     "node": [
                         {
                             "node-id": "10.11.1.1",
                             "l3-unicast-igp-topology:igp-node-attributes": {
                                 "prefix": [
                                     {
                                         "prefix": "10.0.0.11/32"
                                     }
                                 ]
                             }
                         }
                     ]
                 }
             ]
         }

      @line 11: The identifier of a node in a topology. Its value is mapped from route's NEXT_HOP attribute.

      @line 15: The IP prefix attribute of the node. Its value is mapped from routes's destination IP prefix.

BGP Linkstate Topology
^^^^^^^^^^^^^^^^^^^^^^
BGP linkstate topology exporter offers a mapping service from BGP-LS routes to network topology nodes and links.

Configuration
'''''''''''''
Following example shows how to create a new instance of linkstate BGP topology exporter:

**URL:** ``/restconf/config/network-topology:network-topology``

**RFC8040 URL:** ``/rests/data/network-topology:network-topology``

**Method:** ``POST``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml

         <topology  xmlns="urn:TBD:params:xml:ns:yang:network-topology">
             <topology-id>bgp-example-linkstate-topology</topology-id>
             <topology-types>
                 <bgp-linkstate-topology xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-types"></bgp-linkstate-topology>
             </topology-types>
             <rib-id xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-config">bgp-example</rib-id>
         </topology>

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json

         {
             "topology": [
                 {
                     "topology-id": "bgp-example-linkstate-topology",
                     "odl-bgp-topology-config:rib-id": "bgp-example",
                     "topology-types": {
                         "odl-bgp-topology-types:bgp-linkstate-topology": {}
                     }
                 }
             ]
         }

Usage
'''''
Operational state of the topology can be verified via REST.
A sample output below represents a two node topology with two unidirectional links interconnecting those nodes.

**URL:** ``/restconf/operational/network-topology:network-topology/topology/bgp-example-linkstate-topology``

**RFC8040 URL:**: ``/rests/data/network-topology:network-topology/topology=bgp-example-linkstate-topology?content=nonconfig``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Response Body:**

      .. code-block:: xml

         <topology xmlns="urn:TBD:params:xml:ns:yang:network-topology">
             <topology-id>bgp-example-linkstate-topology</topology-id>
             <server-provided>true</server-provided>
             <topology-types>
                 <bgp-linkstate-topology xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-types"></bgp-linkstate-topology>
             </topology-types>
             <node>
                 <node-id>bgpls://IsisLevel2:1/type=node&amp;as=65000&amp;domain=673720360&amp;router=0000.0000.0040</node-id>
                 <termination-point>
                     <tp-id>bgpls://IsisLevel2:1/type=tp&amp;ipv4=203.20.160.40</tp-id>
                     <igp-termination-point-attributes xmlns="urn:TBD:params:xml:ns:yang:nt:l3-unicast-igp-topology"/>
                 </termination-point>
                 <igp-node-attributes xmlns="urn:TBD:params:xml:ns:yang:nt:l3-unicast-igp-topology">
                     <prefix>
                         <prefix>40.40.40.40/32</prefix>
                         <metric>10</metric>
                     </prefix>
                     <prefix>
                         <prefix>203.20.160.0/24</prefix>
                         <metric>10</metric>
                     </prefix>
                     <name>node1</name>
                     <router-id>40.40.40.40</router-id>
                     <isis-node-attributes xmlns="urn:TBD:params:xml:ns:yang:network:isis-topology">
                         <ted>
                             <te-router-id-ipv4>40.40.40.40</te-router-id-ipv4>
                         </ted>
                         <iso>
                             <iso-system-id>MDAwMDAwMDAwMDY0</iso-system-id>
                         </iso>
                     </isis-node-attributes>
                 </igp-node-attributes>
             </node>
             <node>
                 <node-id>bgpls://IsisLevel2:1/type=node&amp;as=65000&amp;domain=673720360&amp;router=0000.0000.0039</node-id>
                 <termination-point>
                     <tp-id>bgpls://IsisLevel2:1/type=tp&amp;ipv4=203.20.160.39</tp-id>
                     <igp-termination-point-attributes xmlns="urn:TBD:params:xml:ns:yang:nt:l3-unicast-igp-topology"/>
                 </termination-point>
                 <igp-node-attributes xmlns="urn:TBD:params:xml:ns:yang:nt:l3-unicast-igp-topology">
                     <prefix>
                         <prefix>39.39.39.39/32</prefix>
                         <metric>10</metric>
                     </prefix>
                     <prefix>
                         <prefix>203.20.160.0/24</prefix>
                         <metric>10</metric>
                     </prefix>
                     <name>node2</name>
                     <router-id>39.39.39.39</router-id>
                     <isis-node-attributes xmlns="urn:TBD:params:xml:ns:yang:network:isis-topology">
                         <ted>
                             <te-router-id-ipv4>39.39.39.39</te-router-id-ipv4>
                         </ted>
                         <iso>
                             <iso-system-id>MDAwMDAwMDAwMDg3</iso-system-id>
                         </iso>
                     </isis-node-attributes>
                 </igp-node-attributes>
             </node>
             <link>
                 <destination>
                     <dest-node>bgpls://IsisLevel2:1/type=node&amp;as=65000&amp;domain=673720360&amp;router=0000.0000.0039</dest-node>
                     <dest-tp>bgpls://IsisLevel2:1/type=tp&amp;ipv4=203.20.160.39</dest-tp>
                 </destination>
                 <link-id>bgpls://IsisLevel2:1/type=link&amp;local-as=65000&amp;local-domain=673720360&amp;local-router=0000.0000.0040&amp;remote-as=65000&amp;remote-domain=673720360&amp;remote-router=0000.0000.0039&amp;ipv4-iface=203.20.160.40&amp;ipv4-neigh=203.20.160.39</link-id>
                 <source>
                     <source-node>bgpls://IsisLevel2:1/type=node&amp;as=65000&amp;domain=673720360&amp;router=0000.0000.0040</source-node>
                     <source-tp>bgpls://IsisLevel2:1/type=tp&amp;ipv4=203.20.160.40</source-tp>
                 </source>
                 <igp-link-attributes xmlns="urn:TBD:params:xml:ns:yang:nt:l3-unicast-igp-topology">
                     <metric>10</metric>
                     <isis-link-attributes xmlns="urn:TBD:params:xml:ns:yang:network:isis-topology">
                         <ted>
                             <color>0</color>
                             <max-link-bandwidth>1250000.0</max-link-bandwidth>
                             <max-resv-link-bandwidth>12500.0</max-resv-link-bandwidth>
                             <te-default-metric>0</te-default-metric>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>0</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>1</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>2</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>3</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>4</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>5</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>6</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>7</priority>
                             </unreserved-bandwidth>
                         </ted>
                     </isis-link-attributes>
                 </igp-link-attributes>
             </link>
             <link>
                 <destination>
                     <dest-node>bgpls://IsisLevel2:1/type=node&amp;as=65000&amp;domain=673720360&amp;router=0000.0000.0040</dest-node>
                     <dest-tp>bgpls://IsisLevel2:1/type=tp&amp;ipv4=203.20.160.40</dest-tp>
                 </destination>
                 <link-id>bgpls://IsisLevel2:1/type=link&amp;local-as=65000&amp;local-domain=673720360&amp;local-router=0000.0000.0039&amp;remote-as=65000&amp;remote-domain=673720360&amp;remote-router=0000.0000.0040&amp;ipv4-iface=203.20.160.39&amp;ipv4-neigh=203.20.160.40</link-id>
                 <source>
                     <source-node>bgpls://IsisLevel2:1/type=node&amp;as=65000&amp;domain=673720360&amp;router=0000.0000.0039</source-node>
                     <source-tp>bgpls://IsisLevel2:1/type=tp&amp;ipv4=203.20.160.39</source-tp>
                 </source>
                 <igp-link-attributes xmlns="urn:TBD:params:xml:ns:yang:nt:l3-unicast-igp-topology">
                     <metric>10</metric>
                     <isis-link-attributes xmlns="urn:TBD:params:xml:ns:yang:network:isis-topology">
                         <ted>
                             <color>0</color>
                             <max-link-bandwidth>1250000.0</max-link-bandwidth>
                             <max-resv-link-bandwidth>12500.0</max-resv-link-bandwidth>
                             <te-default-metric>0</te-default-metric>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>0</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>1</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>2</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>3</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>4</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>5</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>6</priority>
                             </unreserved-bandwidth>
                             <unreserved-bandwidth>
                                 <bandwidth>12500.0</bandwidth>
                                 <priority>7</priority>
                             </unreserved-bandwidth>
                         </ted>
                     </isis-link-attributes>
                 </igp-link-attributes>
             </link>
         </topology>

   .. tab:: JSON

      **Response Body:**

      .. code-block:: json

         {
             "topology": {
                 "topology-id": "bgp-example-linkstate-topology",
                 "server-provided": "true",
                 "topology-types": {
                     "bgp-linkstate-topology": null
                 },
                 "node": [
                     {
                         "node-id": "bgpls://IsisLevel2:1/type=node&as=65000&domain=673720360&router=0000.0000.0040",
                         "termination-point": {
                             "tp-id": "bgpls://IsisLevel2:1/type=tp&ipv4=203.20.160.40",
                             "igp-termination-point-attributes": null
                         },
                         "igp-node-attributes": {
                             "prefix": [
                                 {
                                     "prefix": "40.40.40.40/32",
                                     "metric": "10"
                                 },
                                 {
                                     "prefix": "203.20.160.0/24",
                                     "metric": "10"
                                 }
                             ],
                             "name": "node1",
                             "router-id": "40.40.40.40",
                             "isis-node-attributes": {
                                 "ted": {
                                     "te-router-id-ipv4": "40.40.40.40"
                                 },
                                 "iso": {
                                     "iso-system-id": "MDAwMDAwMDAwMDY0"
                                 }
                             }
                         }
                     },
                     {
                         "node-id": "bgpls://IsisLevel2:1/type=node&as=65000&domain=673720360&router=0000.0000.0039",
                         "termination-point": {
                             "tp-id": "bgpls://IsisLevel2:1/type=tp&ipv4=203.20.160.39",
                             "igp-termination-point-attributes": null
                         },
                         "igp-node-attributes": {
                             "prefix": [
                                 {
                                     "prefix": "39.39.39.39/32",
                                     "metric": "10"
                                 },
                                 {
                                     "prefix": "203.20.160.0/24",
                                     "metric": "10"
                                 }
                             ],
                             "name": "node2",
                             "router-id": "39.39.39.39",
                             "isis-node-attributes": {
                                 "ted": {
                                     "te-router-id-ipv4": "39.39.39.39"
                                 },
                                 "iso": {
                                     "iso-system-id": "MDAwMDAwMDAwMDg3"
                                 }
                             }
                         }
                     }
                 ],
                 "link": [
                     {
                         "destination": {
                             "dest-node": "bgpls://IsisLevel2:1/type=node&as=65000&domain=673720360&router=0000.0000.0039",
                             "dest-tp": "bgpls://IsisLevel2:1/type=tp&ipv4=203.20.160.39"
                         },
                         "link-id": "bgpls://IsisLevel2:1/type=link&local-as=65000&local-domain=673720360&local-router=0000.0000.0040&remote-as=65000&remote-domain=673720360&remote-router=0000.0000.0039&ipv4- iface=203.20.160.40&ipv4-neigh=203.20.160.39",
                         "source": {
                             "source-node": "bgpls://IsisLevel2:1/type=node&as=65000&domain=673720360&router=0000.0000.0040",
                             "source-tp": "bgpls://IsisLevel2:1/type=tp&ipv4=203.20.160.40"
                         },
                         "igp-link-attributes": {
                             "metric": "10",
                             "isis-link-attributes": {
                                 "ted": {
                                     "color": "0",
                                     "max-link-bandwidth": "1250000.0",
                                     "max-resv-link-bandwidth": "12500.0",
                                     "te-default-metric": "0",
                                     "unreserved-bandwidth": [
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "0"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "1"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "2"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "3"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "4"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "5"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "6"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "7"
                                         }
                                     ]
                                 }
                             }
                         }
                     },
                     {
                         "destination": {
                             "dest-node": "bgpls://IsisLevel2:1/type=node&as=65000&domain=673720360&router=0000.0000.0040",
                             "dest-tp": "bgpls://IsisLevel2:1/type=tp&ipv4=203.20.160.40"
                         },
                         "link-id": "bgpls://IsisLevel2:1/type=link&local-as=65000&local-domain=673720360&local-router=0000.0000.0039&remote-as=65000&remote-domain=673720360&remote-router=0000.0000.0040&ipv4-iface=203.20.160.39&ipv4-neigh=203.20.160.40",
                         "source": {
                             "source-node": "bgpls://IsisLevel2:1/type=node&as=65000&domain=673720360&router=0000.0000.0039",
                             "source-tp": "bgpls://IsisLevel2:1/type=tp&ipv4=203.20.160.39"
                         },
                         "igp-link-attributes": {
                             "metric": "10",
                             "isis-link-attributes": {
                                 "ted": {
                                     "color": "0",
                                     "max-link-bandwidth": "1250000.0",
                                     "max-resv-link-bandwidth": "12500.0",
                                     "te-default-metric": "0",
                                     "unreserved-bandwidth": [
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "0"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "1"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "2"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "3"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "4"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "5"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "6"
                                         },
                                         {
                                             "bandwidth": "12500.0",
                                             "priority": "7"
                                         }
                                     ]
                                 }
                             }
                         }
                     }
                 ]
             }
         }

BGP Network Topology Configuration Loader
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

BGP Network Topology Configuration Loader allows user to define static initial
configuration for a BGP protocol instance.
This service will detect the creation of new configuration files following the
pattern ``network-topology-*.xml`` under the path ``etc/opendaylight/bgpcep``.
Once the file is processed, the defined configuration will be available from
the configuration Data Store.

.. note:: If the BGP topology instance is already present, no update or configuration will be applied.

**PATH:** ``etc/opendaylight/bgpcep/network-topology-config.xml``

.. tabs::

   .. tab:: XML

      .. code-block:: xml

         <network-topology xmlns="urn:TBD:params:xml:ns:yang:network-topology">
             <topology>
                 <topology-id>example-ipv4-topology</topology-id>
                 <topology-types>
                     <bgp-ipv4-reachability-topology xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-types"/>
                 </topology-types>
                 <rib-id xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-config">example-bgp-rib</rib-id>
             </topology>
             <topology>
                 <topology-id>example-ipv6-topology</topology-id>
                 <topology-types>
                    <bgp-ipv6-reachability-topology xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-types"/>
                 </topology-types>
                 <rib-id xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-config">example-bgp-rib</rib-id>
             </topology>
             <topology>
                 <topology-id>example-linkstate-topology</topology-id>
                 <topology-types>
                     <bgp-linkstate-topology xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-types"/>
                 </topology-types>
                 <rib-id xmlns="urn:opendaylight:params:xml:ns:yang:odl-bgp-topology-config">example-bgp-rib</rib-id>
             </topology>
         </network-topology>

   .. tab:: JSON

      .. code-block:: json

         {
             "network-topology" : {
                 "topology": [
                     {
                         "topology-id": "example-ipv4-topology",
                         "topology-types": {
                         },
                         "rib-id": "example-bgp-rib"
                     },
                     {
                         "topology-id": "example-ipv6-topology",
                         "topology-types": {
                         },
                         "rib-id": "example-bgp-rib"
                     },
                     {
                         "topology-id": "example-linkstate-topology",
                         "topology-types": {
                         },
                         "rib-id": "example-bgp-rib"
                     }
                 ]
             }
         }
