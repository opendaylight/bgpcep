.. _bgp-user-guide-long-lived-graceful-restart-capability:

Long-Lived Graceful Restart Capability
======================================
The Long-Lived Graceful Restart Capability is extension to Graceful Restart that provides tools to retain stale routes for longer time upon session failure.
New capability is advertised in OPEN message in conjunction with Graceful Restart Capability, which contains list of supported families and their stale timer.
After session failure and Graceful Restart timer expire routing information is retained for value of Long-Lived Stale Timer.

.. contents:: Contents
   :depth: 2
   :local:

Configuration
^^^^^^^^^^^^^
Long-Live Graceful Restart is enabled and configured per family in *ll-graceful-restart* section of *neighbor* or *peer-group* family configuration.

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/neighbors/neighbor/192.0.2.1/afi-safis/afi-safi/openconfig-bgp-types:IPV4%2DUNICAST/graceful-restart``

or

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/peer-groups/peer-group/external-neighbors/afi-safis/afi-safi/openconfig-bgp-types:IPV4%2DUNICAST/graceful-restart``

**Method:** ``PUT``

**Content-Type:** ``application/xml``

**Request Body:**

.. code-block:: xml
   :linenos:
   :emphasize-lines: 3,6

   <graceful-restart xmlns="urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions">
      <config>
         <enable>true</enable>
         <ll-graceful-restart xmlns="urn:opendaylight:params:xml:ns:yang:bgp:ll-graceful-restart">
             <config>
                 <long-live-stale-time>180</long-live-stale-time>
             </config>
         </ll-graceful-restart>
       </config>
   </graceful-restart>

@line 3: if Graceful Restart support is disabled for family only procedures for Long-Lived Graceful Restart applies

@line 6: value of Long-Lived Stale timer in seconds

References
^^^^^^^^^^
* `Support for Long-lived BGP Graceful Restart <https://tools.ietf.org/html/draft-uttaro-idr-bgp-persistence-04>`_
