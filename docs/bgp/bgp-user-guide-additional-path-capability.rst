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

**URL:** ``/rests/data/openconfig-network-instance:network-instances/network-instance=global-bgp/protocols``

**Method:** ``POST``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 14

         <protocol xmlns="http://openconfig.net/yang/network-instance">
             <name>bgp-example</name>
             <identifier xmlns:x="http://openconfig.net/yang/policy-types">x:BGP</identifier>
             <bgp xmlns="urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions">
                 <global>
                     <config>
                         <router-id>192.0.2.2</router-id>
                         <as>65000</as>
                     </config>
                     <afi-safis>
                         <afi-safi>
                             <afi-safi-name xmlns:x="http://openconfig.net/yang/bgp-types">x:IPV4-UNICAST</afi-safi-name>
                             <receive>true</receive>
                             <send-max>2</send-max>
                         </afi-safi>
                     </afi-safis>
                 </global>
             </bgp>
         </protocol>

      @line 14: Defines path selection strategy: *send-max* > 1 -> Advertise N Paths or *send-max* = 0 -> Advertise All Paths

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 17

         {
             "protocol": [
                 {
                     "identifier": "openconfig-policy-types:BGP",
                     "name": "bgp-example",
                     "bgp-openconfig-extensions:bgp": {
                         "global": {
                             "config": {
                                 "router-id": "192.0.2.2",
                                 "as": 65000
                             },
                             "afi-safis": {
                                 "afi-safi": [
                                     {
                                         "afi-safi-name": "openconfig-bgp-types:IPV4-UNICAST",
                                         "receive": true,
                                         "send-max": 2
                                     }
                                 ]
                             }
                         }
                     }
                 }
             ]
         }

      @line 17: Defines path selection strategy: *send-max* > 1 -> Advertise N Paths or *send-max* = 0 -> Advertise All Paths

Here is an example for update a specific family with enable ADD-PATH capability

**URL:** ``/rests/data/openconfig-network-instance:network-instances/network-instance=global-bgp/openconfig-network-instance:protocols/protocol=openconfig-policy-types:BGP,bgp-example/bgp-openconfig-extensions:bgp/global/afi-safis/afi-safi=openconfig-bgp-types:IPV4%2DUNICAST``

**Method:** ``PUT``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml

         <afi-safi xmlns="urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions">
             <afi-safi-name xmlns:x="http://openconfig.net/yang/bgp-types">x:IPV4-UNICAST</afi-safi-name>
             <receive>true</receive>
             <send-max>0</send-max>
         </afi-safi>

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json

         {
             "bgp-openconfig-extensions:afi-safi": [
                 {
                     "afi-safi-name": "openconfig-bgp-types:IPV4-UNICAST",
                     "receive": true,
                     "send-max": 0
                 }
             ]
         }

BGP Peer
''''''''
Here is an example for BGP peer configuration with enabled ADD-PATH capability.

**URL:** ``/rests/data/openconfig-network-instance:network-instances/network-instance=global-bgp/openconfig-network-instance:protocols/protocol=openconfig-policy-types:BGP,bgp-example/bgp-openconfig-extensions:bgp/neighbors``

**Method:** ``POST``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml

         <neighbor xmlns="urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions">
             <neighbor-address>192.0.2.1</neighbor-address>
             <afi-safis>
                 <afi-safi>
                     <afi-safi-name xmlns:x="http://openconfig.net/yang/bgp-types">x:IPV4-LABELLED-UNICAST</afi-safi-name>
                 </afi-safi>
                 <afi-safi>
                     <afi-safi-name xmlns:x="http://openconfig.net/yang/bgp-types">x:IPV4-UNICAST</afi-safi-name>
                     <receive>true</receive>
                     <send-max>0</send-max>
                 </afi-safi>
             </afi-safis>
         </neighbor>

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json

         {
             "neighbor": [
                 {
                     "neighbor-address": "192.0.2.1",
                     "afi-safis": {
                         "afi-safi": [
                             {
                                 "afi-safi-name": "openconfig-bgp-types:IPV4-LABELLED-UNICAST"
                             },
                             {
                                 "afi-safi-name": "openconfig-bgp-types:IPV4-UNICAST",
                                 "receive": true,
                                 "send-max": 0
                             }
                         ]
                     }
                 }
             ]
         }

.. note:: The path selection strategy is not configurable on per peer basis. The send-max presence indicates a willingness to send ADD-PATH NLRIs to the neighbor.

Here is an example for update specific family BGP peer configuration with enabled ADD-PATH capability.

**URL:** ``/rests/data/openconfig-network-instance:network-instances/network-instance=global-bgp/openconfig-network-instance:protocols/protocol=openconfig-policy-types:BGP,bgp-example/bgp-openconfig-extensions:bgp/neighbors/neighbor=192.0.2.1/afi-safis/afi-safi=openconfig-bgp-types:IPV4%2DUNICAST``

**Method:** ``PUT``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml

         <afi-safi xmlns="urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions">
            <afi-safi-name xmlns:x="http://openconfig.net/yang/bgp-types">x:IPV4-UNICAST</afi-safi-name>
            <receive>true</receive>
            <send-max>0</send-max>
         </afi-safi>

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json

         {
             "bgp-openconfig-extensions:afi-safi": [
                 {
                     "afi-safi-name": "openconfig-bgp-types:IPV4-UNICAST",
                     "receive": true,
                     "send-max": 0
                 }
             ]
         }

Usage
^^^^^
The IPv4 Unicast table with enabled ADD-PATH capability in an instance of the speaker's Loc-RIB can be verified via REST:

**URL:** ``/rests/data/bgp-rib:bgp-rib/rib=bgp-example/loc-rib/tables=bgp-types:ipv4-address-family,bgp-types:unicast-subsequent-address-family/bgp-inet:ipv4-routes?content=nonconfig``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Response Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3

         <ipv4-routes xmlns="urn:opendaylight:params:xml:ns:yang:bgp-inet">
             <ipv4-route>
                 <path-id>1</path-id>
                 <prefix>193.0.2.1/32</prefix>
                 <attributes>
                     <as-path></as-path>
                     <origin>
                         <value>igp</value>
                     </origin>
                     <local-pref>
                         <pref>100</pref>
                     </local-pref>
                     <ipv4-next-hop>
                         <global>10.0.0.1</global>
                     </ipv4-next-hop>
                 </attributes>
             </ipv4-route>
             <ipv4-route>
                 <path-id>2</path-id>
                 <prefix>193.0.2.1/32</prefix>
                 <attributes>
                     <as-path></as-path>
                     <origin>
                         <value>igp</value>
                     </origin>
                     <local-pref>
                         <pref>100</pref>
                     </local-pref>
                     <ipv4-next-hop>
                         <global>10.0.0.2</global>
                     </ipv4-next-hop>
                 </attributes>
             </ipv4-route>
         </ipv4-routes>

      @line 3: The routes with the same destination are distinguished by *path-id* attribute.

   .. tab:: JSON

      **Response Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 5

         {
             "bgp-inet:ipv4-routes":{
                 "ipv4-route": [
                     {
                         "path-id": 1,
                         "prefix": "193.0.2.1/32",
                         "attributes": {
                             "origin": {
                                 "value": "igp"
                             },
                             "local-pref": {
                                 "pref": 100
                             },
                             "ipv4-next-hop": {
                                "global": "10.0.0.1"
                             }
                         }
                     },
                     {
                         "path-id": 2,
                         "prefix": "193.0.2.1/32",
                         "attributes": {
                             "origin": {
                                 "value": "igp"
                             },
                             "local-pref": {
                                 "pref": 100
                             },
                             "ipv4-next-hop": {
                                 "global": "10.0.0.2"
                             }
                         }
                     }
                 ]
             }
         }

      @line 5: The routes with the same destination are distinguished by *path-id* attribute.

References
^^^^^^^^^^
* `Advertisement of Multiple Paths in BGP <https://tools.ietf.org/html/rfc7911>`_
* `Best Practices for Advertisement of Multiple Paths in IBGP <https://tools.ietf.org/html/draft-ietf-idr-add-paths-guidelines-08>`_
