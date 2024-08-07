.. _bgp-user-guide-bgp-application-peer:

BGP Application Peer and programmable RIB
=========================================
The OpenDaylight BGP implementation also supports routes injection via *Application Peer*.
Such peer has its own programmable RIB, which can be modified by user.
This concept allows user to originate new routes and advertise them to all connected peers.

Application Peer configuration
''''''''''''''''''''''''''''''
Following configuration sample show a way to configure the *Application Peer*:

**URL:** ``/rests/data/openconfig-network-instance:network-instances/network-instance=global-bgp/openconfig-network-instance:protocols/protocol=openconfig-policy-types:BGP,bgp-example/bgp-openconfig-extensions:bgp/neighbors``

**Method:** ``POST``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 2,4

         <neighbor xmlns="urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions">
             <neighbor-address>10.25.1.9</neighbor-address>
             <config>
                 <peer-group>application-peers</peer-group>
             </config>
         </neighbor>

      @line 2: IP address is uniquely identifying *Application Peer* and its programmable RIB. Address is also used in local BGP speaker decision process.

      @line 4: Indicates that peer is associated with *application-peers* group. It serves to distinguish *Application Peer's* from regular neighbors.

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,6

         {
             "neighbor": [
                 {
                     "neighbor-address": "10.25.1.9",
                     "config": {
                         "peer-group": "application-peers"
                     }
                 }
             ]
         }

      @line 4: IP address is uniquely identifying *Application Peer* and its programmable RIB. Address is also used in local BGP speaker decision process.

      @line 6: Indicates that peer is associated with *application-peers* group. It serves to distinguish *Application Peer's* from regular neighbors.

-----

The *Application Peer* presence can be verified via REST:

**URL:** ``/rests/data/bgp-rib:bgp-rib/rib=bgp-example/peer=bgp%3A%2F%2F10.25.1.9?content=nonconfig``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Response Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,8

         <peer xmlns="urn:opendaylight:params:xml:ns:yang:bgp-rib">
             <peer-id>bgp://10.25.1.9</peer-id>
             <peer-role>internal</peer-role>
             <adj-rib-in>
                 <tables>
                     <afi xmlns:x="urn:opendaylight:params:xml:ns:yang:bgp-types">x:ipv4-address-family</afi>
                     <safi xmlns:x="urn:opendaylight:params:xml:ns:yang:bgp-types">x:unicast-subsequent-address-family</safi>
                     <ipv4-routes xmlns="urn:opendaylight:params:xml:ns:yang:bgp-inet"></ipv4-routes>
                     <attributes>
                         <uptodate>false</uptodate>
                     </attributes>
                 </tables>
             </adj-rib-in>
             <effective-rib-in>
                 <tables>
                     <afi xmlns:x="urn:opendaylight:params:xml:ns:yang:bgp-types">x:ipv4-address-family</afi>
                     <safi xmlns:x="urn:opendaylight:params:xml:ns:yang:bgp-types">x:unicast-subsequent-address-family</safi>
                     <ipv4-routes xmlns="urn:opendaylight:params:xml:ns:yang:bgp-inet"></ipv4-routes>
                     <attributes></attributes>
                 </tables>
             </effective-rib-in>
         </peer>

      @line 3: Peer role for *Application Peer* is *internal*.

      @line 8: Adj-RIB-In is empty, as no routes were originated yet.

   .. tab:: JSON

      **Response Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 5,12

         {
             "peer": [
                 {
                     "peer-id": "bgp://10.25.1.9",
                     "peer-role": "internal",
                     "adj-rib-in": {
                         "tables": [
                             {
                                 "afi": "bgp-types:ipv4-address-family",
                                 "safi": "bgp-types:unicast-subsequent-address-family",
                                 "attributes": {
                                     "uptodate": false
                                 }
                             }
                         ]
                     },
                     "effective-rib-in": {
                         "tables": [
                             {
                                 "afi": "bgp-types:ipv4-address-family",
                                 "safi": "bgp-types:unicast-subsequent-address-family"
                             }
                         ]
                     }
                 }
             ]
         }

      @line 5: Peer role for *Application Peer* is *internal*.

      @line 12: Adj-RIB-In is empty, as no routes were originated yet.

.. note:: There is no Adj-RIB-Out for *Application Peer*.

Programmable RIB
''''''''''''''''
Next example shows how to inject a route into the programmable RIB.

**URL:** ``/rests/data/bgp-rib:application-rib/10.25.1.9/tables=bgp-types:ipv4-address-family,bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes``

**Method:** ``POST``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml

         <ipv4-route xmlns="urn:opendaylight:params:xml:ns:yang:bgp-inet">
             <path-id>0</path-id>
             <prefix>10.0.0.11/32</prefix>
             <attributes>
                 <origin>
                     <value>igp</value>
                 </origin>
                 <local-pref>
                     <pref>100</pref>
                 </local-pref>
                 <ipv4-next-hop>
                     <global>10.11.1.1</global>
                 </ipv4-next-hop>
             </attributes>
         </ipv4-route>

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json

         {
             "bgp-inet:ipv4-route": [
                 {
                     "path-id": 0,
                     "prefix": "10.0.0.11/32",
                     "attributes": {
                         "origin": {
                             "value": "igp"
                         },
                         "local-pref": {
                             "pref": 100
                         },
                         "ipv4-next-hop": {
                             "global": "10.11.1.1"
                         }
                     }
                 }
             ]
         }

-----

Now the injected route appears in *Application Peer's* RIBs and in local speaker's Loc-RIB:

**URL:** ``/rests/data/bgp-rib:bgp-rib/rib=bgp-example/peer=bgp%3A%2F%2F10.25.1.9?content=nonconfig``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Response Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 9

         <peer xmlns="urn:opendaylight:params:xml:ns:yang:bgp-rib">
             <peer-id>bgp://10.25.1.9</peer-id>
             <peer-role>internal</peer-role>
             <adj-rib-in>
                 <tables>
                     <afi xmlns:x="urn:opendaylight:params:xml:ns:yang:bgp-types">x:ipv4-address-family</afi>
                     <safi xmlns:x="urn:opendaylight:params:xml:ns:yang:bgp-types">x:unicast-subsequent-address-family</safi>
                     <ipv4-routes xmlns="urn:opendaylight:params:xml:ns:yang:bgp-inet">
                         <ipv4-route>
                             <path-id>0</path-id>
                             <prefix>10.0.0.11/32</prefix>
                             <attributes>
                                 <origin>
                                     <value>igp</value>
                                 </origin>
                                 <local-pref>
                                     <pref>100</pref>
                                 </local-pref>
                                 <ipv4-next-hop>
                                     <global>10.11.1.1</global>
                                 </ipv4-next-hop>
                             </attributes>
                         </ipv4-route>
                     </ipv4-routes>
                     <attributes>
                         <uptodate>false</uptodate>
                     </attributes>
                 </tables>
             </adj-rib-in>
             <effective-rib-in>
                 <tables>
                     <afi xmlns:x="urn:opendaylight:params:xml:ns:yang:bgp-types">x:ipv4-address-family</afi>
                     <safi xmlns:x="urn:opendaylight:params:xml:ns:yang:bgp-types">x:unicast-subsequent-address-family</safi>
                     <ipv4-routes xmlns="urn:opendaylight:params:xml:ns:yang:bgp-inet">
                         <ipv4-route>
                             <path-id>0</path-id>
                             <prefix>10.0.0.11/32</prefix>
                             <attributes>
                                 <origin>
                                     <value>igp</value>
                                 </origin>
                                 <local-pref>
                                     <pref>100</pref>
                                 </local-pref>
                                 <ipv4-next-hop>
                                     <global>10.11.1.1</global>
                                 </ipv4-next-hop>
                             </attributes>
                         </ipv4-route>
                     </ipv4-routes>
                     <attributes></attributes>
                 </tables>
             </effective-rib-in>
         </peer>

      @line 9: Injected route is present in *Application Peer's* Adj-RIB-In and Effective-RIB-In.

   .. tab:: JSON

      **Response Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 12

         {
             "peer": [
                 {
                     "peer-id": "bgp://10.25.1.9",
                     "peer-role": "internal",
                     "adj-rib-in": {
                         "tables": [
                             {
                                 "afi": "bgp-types:ipv4-address-family",
                                 "safi": "bgp-types:unicast-subsequent-address-family",
                                 "bgp-inet:ipv4-routes":{
                                     "ipv4-route": [
                                         {
                                             "path-id": 0,
                                             "prefix": "10.0.0.11/32",
                                             "attributes": {
                                                 "origin": {
                                                     "value": "igp"
                                                 },
                                                 "local-pref": {
                                                     "pref": 100
                                                 },
                                                 "ipv4-next-hop": {
                                                     "global": "10.11.1.1"
                                                 }
                                             }
                                         }
                                     ]
                                 },
                                 "attributes": {
                                     "uptodate": false
                                 }
                             }
                         ]
                     },
                     "effective-rib-in": {
                         "tables": [
                             {
                                 "afi": "bgp-types:ipv4-address-family",
                                 "safi": "bgp-types:unicast-subsequent-address-family",
                                 "bgp-inet:ipv4-routes":{
                                     "ipv4-route": [
                                         {
                                             "path-id": 0,
                                             "prefix": "10.0.0.11/32",
                                             "attributes": {
                                                 "origin": {
                                                     "value": "igp"
                                                 },
                                                 "local-pref": {
                                                     "pref": 100
                                                 },
                                                 "ipv4-next-hop": {
                                                     "global": "10.11.1.1"
                                                 }
                                             }
                                         }
                                     ]
                                 }
                             }
                         ]
                     }
                 }
             ]
         }

      @line 12: Injected route is present in *Application Peer's* Adj-RIB-In and Effective-RIB-In.

-----

**URL:** ``/rests/data/bgp-rib:bgp-rib/rib=bgp-example/loc-rib/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/ipv4-routes?content=nonconfig``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Response Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 2

         <ipv4-routes xmlns="urn:opendaylight:params:xml:ns:yang:bgp-inet">
             <ipv4-route>
                 <path-id>0</path-id>
                 <prefix>10.0.0.10/32</prefix>
                 <attributes>
                     <origin>
                         <value>igp</value>
                     </origin>
                     <local-pref>
                         <pref>100</pref>
                     </local-pref>
                     <ipv4-next-hop>
                         <global>10.11.1.1</global>
                     </ipv4-next-hop>
                 </attributes>
             </ipv4-route>
             <ipv4-route>
                 <path-id>0</path-id>
                 <prefix>10.0.0.10/32</prefix>
                 <attributes>
                     <origin>
                         <value>igp</value>
                     </origin>
                     <local-pref>
                         <pref>100</pref>
                     </local-pref>
                     <ipv4-next-hop>
                         <global>10.10.1.1</global>
                     </ipv4-next-hop>
                 </attributes>
             </ipv4-route>
         </ipv4-routes>

      @line 2: The injected route is now present in Loc-RIB along with a route (destination *10.0.0.10/32*) advertised by remote peer.

   .. tab:: JSON

      **Response Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 3

         {
             "bgp-inet:ipv4-routes":{
                 "ipv4-route": [
                     {
                         "path-id": 0,
                         "prefix": "10.0.0.10/32",
                         "attributes": {
                             "origin": {
                                 "value": "igp"
                             },
                             "local-pref": {
                                   "pref": 100
                             },
                             "ipv4-next-hop": {
                                "global": "10.11.1.1"
                             }
                         }
                     },
                     {
                         "path-id": 0,
                         "prefix": "10.0.0.10/32",
                         "attributes": {
                             "origin": {
                                "value": "igp"
                             },
                             "local-pref": {
                                "pref": 100
                             },
                             "ipv4-next-hop": {
                                 "global": "10.11.1.1"
                             }
                         }
                     }
                 ]
             }
         }

      @line 3: The injected route is now present in Loc-RIB along with a route (destination *10.0.0.10/32*) advertised by remote peer.

-----

This route is also advertised to the remote peer (*192.0.2.1*), hence route appears in its Adj-RIB-Out:

**URL:** ``/rests/data/bgp-rib:bgp-rib/rib=bgp-example/peer/bgp:%2F%2F192.0.2.1/adj-rib-out/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes?content=nonconfig``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Response Body:**

      .. code-block:: xml

         <ipv4-route xmlns="urn:opendaylight:params:xml:ns:yang:bgp-inet">
             <path-id>0</path-id>
             <prefix>10.0.0.11/32</prefix>
             <attributes>
                 <origin>
                     <value>igp</value>
                 </origin>
                 <local-pref>
                     <pref>100</pref>
                 </local-pref>
                 <ipv4-next-hop>
                     <global>10.11.1.1</global>
                 </ipv4-next-hop>
             </attributes>
         </ipv4-route>

   .. tab:: JSON

      **Response Body:**

      .. code-block:: json

         {
             "bgp-inet:ipv4-route": [
                 {
                     "path-id": 0,
                     "prefix": "10.0.0.11/32",
                     "attributes": {
                         "origin": {
                            "value": "igp"
                         },
                         "local-pref": {
                             "pref": 100
                         },
                         "ipv4-next-hop": {
                             "global": "10.11.1.1"
                         }
                     }
                 }
             ]
         }

-----

The injected route can be modified (i.e. different path attribute):

**URL:** ``/rests/data/bgp-rib:application-rib/10.25.1.9/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes/ipv4-route/10.0.0.11%2F32/0``

**Method:** ``PUT``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml

         <ipv4-route xmlns="urn:opendaylight:params:xml:ns:yang:bgp-inet">
             <path-id>0</path-id>
             <prefix>10.0.0.11/32</prefix>
             <attributes>
                 <origin>
                     <value>igp</value>
                 </origin>
                 <local-pref>
                     <pref>50</pref>
                 </local-pref>
                 <ipv4-next-hop>
                     <global>10.11.1.2</global>
                 </ipv4-next-hop>
             </attributes>
         </ipv4-route>

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json

         {
             "bgp-inet:ipv4-route": [
                 {
                     "path-id": 0,
                     "prefix": "10.0.0.11/32",
                     "attributes": {
                         "origin": {
                             "value": "igp"
                         },
                         "local-pref": {
                             "pref": 50
                         },
                         "ipv4-next-hop": {
                             "global": "10.11.1.1"
                         }
                     }
                 }
             ]
         }

-----

The route can be removed from programmable RIB in a following way:

**URL:** ``/rests/data/bgp-rib:application-rib/10.25.1.9/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes/ipv4-route/10.0.0.11%2F32/0``

**Method:** ``DELETE``

-----

Also it is possible to remove all routes from a particular table at once:

**URL:** ``/rests/data/bgp-rib:application-rib/10.25.1.9/tables/bgp-types:ipv4-address-family/bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes``

**Method:** ``DELETE``

-----

Consequently, route disappears from programmable RIB, *Application Peer's* RIBs, Loc-RIB and peer's Adj-RIB-Out (UPDATE message with prefix withdrawal is send).

.. note:: Routes stored in programmable RIB are persisted on OpendDaylight shutdown and restored after the re-start.
