.. _bgp-user-guide-graceful-restart-capability:

Graceful Restart Capability
===========================
The Graceful Restart Capability helps us to minimize the negative effects on routing caused by BGP restart by allowing BGP speaker to express its ability to preserve forwarding state during BGP restart.
New capability is advertised in OPEN message which contains information about Graceful Restart timer value, supported families and their forwarding state.

.. contents:: Contents
   :depth: 2
   :local:

Configuration
^^^^^^^^^^^^^
This section shows a way how to configure Graceful Restart Timer and enable Graceful Restart support for specific families.

.. note:: Graceful Restart capability is enabled by default even when no families are advertised. In that case only receiving speaker procedures apllies.

Graceful Restart Timer
''''''''''''''''''''''
Routing information for configured families are preserved for time given by Graceful Restart timer in seconds. This can be configured in *graceful-restart* section of *neighbor* or *peer-group* configuration.

**XML**

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/neighbors/neighbor/192.0.2.1/graceful-restart``

or

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/peer-groups/peer-group/external-neighbors/graceful-restart``

**Method:** ``PUT``

**Content-Type:** ``application/xml``

**Request Body:**

.. code-block:: xml
   :linenos:
   :emphasize-lines: 3

   <graceful-restart xmlns="urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions">
       <config>
           <restart-time>60</restart-time>
        </config>
    </graceful-restart>

@line 3: value of Graceful Restart timer in seconds

**JSON**

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/peer-groups/peer-group/external-neighbors/graceful-restart``

**Method:** ``PUT``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 4

   {
       "graceful-restart": {
           "config": {
               "restart-time": 60
           }
       }	
   }

@line 4: value of Graceful Restart timer in seconds

.. note:: If case that Graceful Restart timer is configured for both neighbor and peer-group, the one from peer-group is used.
   If no Graceful Restart timer is configured value of HOLD timer is used.

BGP Neighbor Families Graceful Restart Configuration
''''''''''''''''''''''''''''''''''''''''''''''''''''
Preserving specific family during Graceful Restart must be enabled in *graceful-restart* section of family configuration for *neighbor* or *peer-group*.

**XML**

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/neighbors/neighbor/192.0.2.1/afi-safis/afi-safi/openconfig-bgp-types:IPV4%2DUNICAST/graceful-restart``

or

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/peer-groups/peer-group/external-neighbors/afi-safis/afi-safi/openconfig-bgp-types:IPV4%2DUNICAST/graceful-restart``

**Method:** ``PUT``

**Content-Type:** ``application/xml``

**Request Body:**

.. code-block:: xml
   :linenos:
   :emphasize-lines: 3

   <graceful-restart xmlns="urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions">
      <config>
         <enable>true</enable>
       </config>
   </graceful-restart>

@line 3: True if we want to preserve family routing information during Graceful Restart

**JSON**

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/peer-groups/peer-group/external-neighbors/afi-safis/afi-safi/openconfig-bgp-types:IPV4%2DUNICAST/graceful-restart``

**Method:** ``PUT``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 4

   {
       "graceful-restart": {
           "config": {
               "enable": true
           }
       }	
   }

@line 4: True if we want to preserve family routing information during Graceful Restart

Usage
^^^^^
In case when we are invoking Graceful Restart we act as Restarting Speaker and we are additionally postponing path selection process until end-of-rib is received for all families or Selection Deferral timer expires, whichever happens first.
To perform Graceful Restart with peer, invoke RPC:

**XML**

**URL:** ``/restconf/operations/bgp-peer-rpc:restart-gracefully``

**Method:** ``POST``

**Content-Type:** ``application/xml``

**Request Body:**

.. code-block:: xml
   :linenos:
   :emphasize-lines: 3

   <input xmlns="urn:opendaylight:params:xml:ns:yang:bgp-peer-rpc">
       <peer-ref xmlns:rib="urn:opendaylight:params:xml:ns:yang:bgp-rib">/rib:bgp-rib/rib:rib[rib:id="bgp-example"]/rib:peer[rib:peer-id="bgp://10.25.1.9"]</peer-ref>
       <selection-deferral-time>60</selection-deferral-time>
   </input>

@line 3: Value of Selection Deferral timer in seconds

**JSON**

**URL:** ``/restconf/operations/bgp-peer-rpc:restart-gracefully``

**Method:** ``POST``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 4

   {
       "bgp-peer-rpc:input": {
           "peer-ref": "/rib:bgp-rib/rib:rib[rib:id=\"bgp-example\"]/rib:peer[rib:peer-id=\"bgp://10.25.1.9\"]",
           "selection-deferral-time": 60	
       }    	
   }

@line 4: Value of Selection Deferral timer in seconds

References
^^^^^^^^^^
* `Graceful Restart Mechanism for BGP <https://tools.ietf.org/html/rfc4724>`_
