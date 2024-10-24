.. _bgp-user-guide-linkstate-family:

Link-State Family
=================
The BGP Link-State (BGP-LS) Multiprotocol extension allows to distribute Link-State and Traffic Engineering (TE) information.
This information is typically distributed by IGP routing protocols with in the network, limiting LSDB or TED visibility to the IGP area.
The BGP-LS-enabled routers are capable to collect such information from networks (multiple IGP areas, inter-AS) and share with external components (i.e. OpenDaylight BGP).
The information is applicable in ALTO servers and PCEs, as both need to gather information about topologies.
In addition, link-state information is extended to carry segment information (Spring).

.. contents:: Contents
   :depth: 2
   :local:

Configuration
^^^^^^^^^^^^^
This section shows a way to enable IPv4 and IPv6 Labeled Unicast family in BGP speaker and peer configuration.

BGP Speaker
'''''''''''
To enable BGP-LS support in BGP plugin, first configure BGP speaker instance:

**URL:** ``/rests/data/openconfig-network-instance:network-instances/network-instance=global-bgp/protocols``

**Method:** ``POST``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml

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
                             <afi-safi-name>LINKSTATE</afi-safi-name>
                         </afi-safi>
                     </afi-safis>
                 </global>
             </bgp>
         </protocol>

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json

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
                                         "afi-safi-name": "LINKSTATE"
                                     }
                                 ]
                             }
                         }
                     }
                 }
             ]
         }

Linkstate path attribute
''''''''''''''''''''''''
The BGP-LS specification has seen early field deployments before the code point assignments have been
properly allocated. RFC7752 specifies this attribute to be TYPE 29, while earlier software is using
TYPE 99.

OpenDaylight defaults to using the RFC7752 allocation, but can be reconfigured to recognize the legacy
code point allocation. This can be achieved through Karaf shell in a running instance:

.. code-block:: console

   opendaylight-user@root>config:edit org.opendaylight.bgp.extensions.linkstate
   opendaylight-user@root>config:property-set ianaAttributeType false
   opendaylight-user@root>config:update

Alternatively, the same effect can be achieved by placing the line ``ianaAttributeType = false`` into
``etc/org.opendaylight.bgp.extensions.linkstate.cfg`` in the installation directory.

BGP Peer
''''''''
Here is an example for BGP peer configuration with enabled BGP-LS family.

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
                     <afi-safi-name>LINKSTATE</afi-safi-name>
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
                                 "afi-safi-name": "LINKSTATE"
                             }
                         ]
                     }
                 }
             ]
         }

Link-State Route API
^^^^^^^^^^^^^^^^^^^^
Following tree illustrate the BGP Link-State route structure.

.. code-block:: console

   :(linkstate-routes-case)
      +--ro linkstate-routes
         +--ro linkstate-route* [route-key path-id]
            +--ro route-key                       string
            +--ro path-id                         path-id
            +--ro protocol-id                     protocol-id
            +--ro identifier                      identifier
            +--ro (object-type)?
            |  +--:(node-case)
            |  |  +--ro node-descriptors
            |  |     +--ro as-number?         inet:as-number
            |  |     +--ro area-id?           area-identifier
            |  |     +--ro domain-id?         domain-identifier
            |  |     +--ro (c-router-identifier)?
            |  |        +--:(isis-node-case)
            |  |        |  +--ro isis-node
            |  |        |     +--ro iso-system-id    netc:iso-system-identifier
            |  |        +--:(isis-pseudonode-case)
            |  |        |  +--ro isis-pseudonode
            |  |        |     +--ro is-is-router-identifier
            |  |        |     |  +--ro iso-system-id    netc:iso-system-identifier
            |  |        |     +--ro psn                        uint8
            |  |        +--:(ospf-node-case)
            |  |        |  +--ro ospf-node
            |  |        |     +--ro ospf-router-id    uint32
            |  |        +--:(ospf-pseudonode-case)
            |  |           +--ro ospf-pseudonode
            |  |              +--ro ospf-router-id    uint32
            |  |              +--ro lan-interface     ospf-interface-identifier
            |  +--:(link-case)
            |  |  +--ro local-node-descriptors
            |  |  |  +--ro as-number?         inet:as-number
            |  |  |  +--ro area-id?           area-identifier
            |  |  |  +--ro domain-id?         domain-identifier
            |  |  |  +--ro (c-router-identifier)?
            |  |  |  |  +--:(isis-node-case)
            |  |  |  |  |  +--ro isis-node
            |  |  |  |  |     +--ro iso-system-id    netc:iso-system-identifier
            |  |  |  |  +--:(isis-pseudonode-case)
            |  |  |  |  |  +--ro isis-pseudonode
            |  |  |  |  |     +--ro is-is-router-identifier
            |  |  |  |  |     |  +--ro iso-system-id    netc:iso-system-identifier
            |  |  |  |  |     +--ro psn                        uint8
            |  |  |  |  +--:(ospf-node-case)
            |  |  |  |  |  +--ro ospf-node
            |  |  |  |  |     +--ro ospf-router-id    uint32
            |  |  |  |  +--:(ospf-pseudonode-case)
            |  |  |  |     +--ro ospf-pseudonode
            |  |  |  |        +--ro ospf-router-id    uint32
            |  |  |  |        +--ro lan-interface     ospf-interface-identifier
            |  |  |  +--ro bgp-router-id?     inet:ipv4-address
            |  |  |  +--ro member-asn?        inet:as-number
            |  |  +--ro remote-node-descriptors
            |  |  |  +--ro as-number?         inet:as-number
            |  |  |  +--ro area-id?           area-identifier
            |  |  |  +--ro domain-id?         domain-identifier
            |  |  |  +--ro (c-router-identifier)?
            |  |  |  |  +--:(isis-node-case)
            |  |  |  |  |  +--ro isis-node
            |  |  |  |  |     +--ro iso-system-id    netc:iso-system-identifier
            |  |  |  |  +--:(isis-pseudonode-case)
            |  |  |  |  |  +--ro isis-pseudonode
            |  |  |  |  |     +--ro is-is-router-identifier
            |  |  |  |  |     |  +--ro iso-system-id    netc:iso-system-identifier
            |  |  |  |  |     +--ro psn                        uint8
            |  |  |  |  +--:(ospf-node-case)
            |  |  |  |  |  +--ro ospf-node
            |  |  |  |  |     +--ro ospf-router-id    uint32
            |  |  |  |  +--:(ospf-pseudonode-case)
            |  |  |  |     +--ro ospf-pseudonode
            |  |  |  |        +--ro ospf-router-id    uint32
            |  |  |  |        +--ro lan-interface     ospf-interface-identifier
            |  |  |  +--ro bgp-router-id?     inet:ipv4-address
            |  |  |  +--ro member-asn?        inet:as-number
            |  |  +--ro link-descriptors
            |  |     +--ro link-local-identifier?    uint32
            |  |     +--ro link-remote-identifier?   uint32
            |  |     +--ro ipv4-interface-address?   ipv4-interface-identifier
            |  |     +--ro ipv6-interface-address?   ipv6-interface-identifier
            |  |     +--ro ipv4-neighbor-address?    ipv4-interface-identifier
            |  |     +--ro ipv6-neighbor-address?    ipv6-interface-identifier
            |  |     +--ro multi-topology-id?        topology-identifier
            |  +--:(prefix-case)
            |  |  +--ro advertising-node-descriptors
            |  |  |  +--ro as-number?         inet:as-number
            |  |  |  +--ro area-id?           area-identifier
            |  |  |  +--ro domain-id?         domain-identifier
            |  |  |  +--ro (c-router-identifier)?
            |  |  |     +--:(isis-node-case)
            |  |  |     |  +--ro isis-node
            |  |  |     |     +--ro iso-system-id    netc:iso-system-identifier
            |  |  |     +--:(isis-pseudonode-case)
            |  |  |     |  +--ro isis-pseudonode
            |  |  |     |     +--ro is-is-router-identifier
            |  |  |     |     |  +--ro iso-system-id    netc:iso-system-identifier
            |  |  |     |     +--ro psn                        uint8
            |  |  |     +--:(ospf-node-case)
            |  |  |     |  +--ro ospf-node
            |  |  |     |     +--ro ospf-router-id    uint32
            |  |  |     +--:(ospf-pseudonode-case)
            |  |  |        +--ro ospf-pseudonode
            |  |  |           +--ro ospf-router-id    uint32
            |  |  |           +--ro lan-interface     ospf-interface-identifier
            |  |  +--ro prefix-descriptors
            |  |     +--ro multi-topology-id?             topology-identifier
            |  |     +--ro ospf-route-type?               ospf-route-type
            |  |     +--ro ip-reachability-information?   inet:ip-prefix
            |  +--:(te-lsp-case)
            |     +--ro (address-family)?
            |     |  +--:(ipv4-case)
            |     |  |  +--ro ipv4-tunnel-sender-address      inet:ipv4-address
            |     |  |  +--ro ipv4-tunnel-endpoint-address    inet:ipv4-address
            |     |  +--:(ipv6-case)
            |     |     +--ro ipv6-tunnel-sender-address      inet:ipv6-address
            |     |     +--ro ipv6-tunnel-endpoint-address    inet:ipv6-address
            |     +--ro tunnel-id?                      rsvp:tunnel-id
            |     +--ro lsp-id?                         rsvp:lsp-id
            +--ro attributes
               +--ro (link-state-attribute)?
                  +--:(node-attributes-case)
                  |  +--ro node-attributes
                  |     +--ro topology-identifier*   topology-identifier
                  |     +--ro node-flags?            node-flag-bits
                  |     +--ro isis-area-id*          isis-area-identifier
                  |     +--ro dynamic-hostname?      string
                  |     +--ro ipv4-router-id?        ipv4-router-identifier
                  |     +--ro ipv6-router-id?        ipv6-router-identifier
                  |     +--ro sr-capabilities
                  |     |  +--ro mpls-ipv4?      boolean
                  |     |  +--ro mpls-ipv6?      boolean
                  |     |  +--ro sr-ipv6?        boolean
                  |     |  +--ro range-size?     uint32
                  |     |  +--ro (sid-label-index)?
                  |     |     +--:(local-label-case)
                  |     |     |  +--ro local-label?    netc:mpls-label
                  |     |     +--:(ipv6-address-case)
                  |     |     |  +--ro ipv6-address?   inet:ipv6-address
                  |     |     +--:(sid-case)
                  |     |        +--ro sid?            uint32
                  |     +--ro sr-algorithm
                  |        +--ro algorithms*   algorithm
                  +--:(link-attributes-case)
                  |  +--ro link-attributes
                  |     +--ro local-ipv4-router-id?       ipv4-router-identifier
                  |     +--ro local-ipv6-router-id?       ipv6-router-identifier
                  |     +--ro remote-ipv4-router-id?      ipv4-router-identifier
                  |     +--ro remote-ipv6-router-id?      ipv6-router-identifier
                  |     +--ro mpls-protocol?              mpls-protocol-mask
                  |     +--ro te-metric?                  netc:te-metric
                  |     +--ro metric?                     netc:metric
                  |     +--ro shared-risk-link-groups*    rsvp:srlg-id
                  |     +--ro link-name?                  string
                  |     +--ro max-link-bandwidth?         netc:bandwidth
                  |     +--ro max-reservable-bandwidth?   netc:bandwidth
                  |     +--ro unreserved-bandwidth* [priority]
                  |     |  +--ro priority     uint8
                  |     |  +--ro bandwidth?   netc:bandwidth
                  |     +--ro link-protection?            link-protection-type
                  |     +--ro admin-group?                administrative-group
                  |     +--ro sr-adj-ids*
                  |     |  +--ro (flags)?
                  |     |  |  +--:(ospf-adj-flags-case)
                  |     |  |  |  +--ro backup?           boolean
                  |     |  |  |  +--ro set?              boolean
                  |     |  |  +--:(isis-adj-flags-case)
                  |     |  |     +--ro backup?           boolean
                  |     |  |     +--ro set?              boolean
                  |     |  |     +--ro address-family?   boolean
                  |     |  +--ro weight?           weight
                  |     |  +--ro (sid-label-index)?
                  |     |     +--:(local-label-case)
                  |     |     |  +--ro local-label?      netc:mpls-label
                  |     |     +--:(ipv6-address-case)
                  |     |     |  +--ro ipv6-address?     inet:ipv6-address
                  |     |     +--:(sid-case)
                  |     |        +--ro sid?              uint32
                  |     +--ro sr-lan-adj-ids*
                  |     |  +--ro (flags)?
                  |     |  |  +--:(ospf-adj-flags-case)
                  |     |  |  |  +--ro backup?           boolean
                  |     |  |  |  +--ro set?              boolean
                  |     |  |  +--:(isis-adj-flags-case)
                  |     |  |     +--ro backup?           boolean
                  |     |  |     +--ro set?              boolean
                  |     |  |     +--ro address-family?   boolean
                  |     |  +--ro weight?           weight
                  |     |  +--ro iso-system-id?    netc:iso-system-identifier
                  |     |  +--ro neighbor-id?      inet:ipv4-address
                  |     |  +--ro (sid-label-index)?
                  |     |     +--:(local-label-case)
                  |     |     |  +--ro local-label?      netc:mpls-label
                  |     |     +--:(ipv6-address-case)
                  |     |     |  +--ro ipv6-address?     inet:ipv6-address
                  |     |     +--:(sid-case)
                  |     |        +--ro sid?              uint32
                  |     +--ro peer-node-sid
                  |     |  +--ro weight?         weight
                  |     |  +--ro (sid-label-index)?
                  |     |     +--:(local-label-case)
                  |     |     |  +--ro local-label?    netc:mpls-label
                  |     |     +--:(ipv6-address-case)
                  |     |     |  +--ro ipv6-address?   inet:ipv6-address
                  |     |     +--:(sid-case)
                  |     |        +--ro sid?            uint32
                  |     +--ro peer-adj-sid
                  |     |  +--ro weight?         weight
                  |     |  +--ro (sid-label-index)?
                  |     |     +--:(local-label-case)
                  |     |     |  +--ro local-label?    netc:mpls-label
                  |     |     +--:(ipv6-address-case)
                  |     |     |  +--ro ipv6-address?   inet:ipv6-address
                  |     |     +--:(sid-case)
                  |     |        +--ro sid?            uint32
                  |     +--ro peer-set-sids*
                  |        +--ro weight?         weight
                  |        +--ro (sid-label-index)?
                  |           +--:(local-label-case)
                  |           |  +--ro local-label?    netc:mpls-label
                  |           +--:(ipv6-address-case)
                  |           |  +--ro ipv6-address?   inet:ipv6-address
                  |           +--:(sid-case)
                  |              +--ro sid?            uint32
                  +--:(prefix-attributes-case)
                  |  +--ro prefix-attributes
                  |     +--ro igp-bits
                  |     |  x--ro up-down?               bits
                  |     |  +--ro is-is-up-down?         boolean
                  |     |  +--ro ospf-no-unicast?       boolean
                  |     |  +--ro ospf-local-address?    boolean
                  |     |  +--ro ospf-propagate-nssa?   boolean
                  |     +--ro route-tags*                route-tag
                  |     +--ro extended-tags*             extended-route-tag
                  |     +--ro prefix-metric?             netc:igp-metric
                  |     +--ro ospf-forwarding-address?   inet:ip-address
                  |     +--ro sr-prefix
                  |     |  +--ro (flags)?
                  |     |  |  +--:(isis-prefix-flags-case)
                  |     |  |  |  +--ro no-php?            boolean
                  |     |  |  |  +--ro explicit-null?     boolean
                  |     |  |  |  +--ro readvertisement?   boolean
                  |     |  |  |  +--ro node-sid?          boolean
                  |     |  |  +--:(ospf-prefix-flags-case)
                  |     |  |     +--ro no-php?            boolean
                  |     |  |     +--ro explicit-null?     boolean
                  |     |  |     +--ro mapping-server?    boolean
                  |     |  +--ro algorithm?         algorithm
                  |     |  +--ro (sid-label-index)?
                  |     |     +--:(local-label-case)
                  |     |     |  +--ro local-label?       netc:mpls-label
                  |     |     +--:(ipv6-address-case)
                  |     |     |  +--ro ipv6-address?      inet:ipv6-address
                  |     |     +--:(sid-case)
                  |     |        +--ro sid?               uint32
                  |     +--ro ipv6-sr-prefix
                  |     |  +--ro algorithm?   algorithm
                  |     +--ro sr-range
                  |     |  +--ro inter-area?   boolean
                  |     |  +--ro range-size?   uint16
                  |     |  +--ro sub-tlvs*
                  |     |     +--ro (range-sub-tlv)?
                  |     |        +--:(binding-sid-tlv-case)
                  |     |        |  +--ro weight?                weight
                  |     |        |  +--ro (flags)?
                  |     |        |  |  +--:(isis-binding-flags-case)
                  |     |        |  |  |  +--ro address-family?        boolean
                  |     |        |  |  |  +--ro mirror-context?        boolean
                  |     |        |  |  |  +--ro spread-tlv?            boolean
                  |     |        |  |  |  +--ro leaked-from-level-2?   boolean
                  |     |        |  |  |  +--ro attached-flag?         boolean
                  |     |        |  |  +--:(ospf-binding-flags-case)
                  |     |        |  |     +--ro mirroring?             boolean
                  |     |        |  +--ro binding-sub-tlvs*
                  |     |        |     +--ro (binding-sub-tlv)?
                  |     |        |        +--:(prefix-sid-case)
                  |     |        |        |  +--ro (flags)?
                  |     |        |        |  |  +--:(isis-prefix-flags-case)
                  |     |        |        |  |  |  +--ro no-php?            boolean
                  |     |        |        |  |  |  +--ro explicit-null?     boolean
                  |     |        |        |  |  |  +--ro readvertisement?   boolean
                  |     |        |        |  |  |  +--ro node-sid?          boolean
                  |     |        |        |  |  +--:(ospf-prefix-flags-case)
                  |     |        |        |  |     +--ro no-php?            boolean
                  |     |        |        |  |     +--ro explicit-null?     boolean
                  |     |        |        |  |     +--ro mapping-server?    boolean
                  |     |        |        |  +--ro algorithm?         algorithm
                  |     |        |        |  +--ro (sid-label-index)?
                  |     |        |        |     +--:(local-label-case)
                  |     |        |        |     |  +--ro local-label?       netc:mpls-label
                  |     |        |        |     +--:(ipv6-address-case)
                  |     |        |        |     |  +--ro ipv6-address?      inet:ipv6-address
                  |     |        |        |     +--:(sid-case)
                  |     |        |        |        +--ro sid?               uint32
                  |     |        |        +--:(ipv6-prefix-sid-case)
                  |     |        |        |  +--ro algorithm?         algorithm
                  |     |        |        +--:(sid-label-case)
                  |     |        |        |  +--ro (sid-label-index)?
                  |     |        |        |     +--:(local-label-case)
                  |     |        |        |     |  +--ro local-label?       netc:mpls-label
                  |     |        |        |     +--:(ipv6-address-case)
                  |     |        |        |     |  +--ro ipv6-address?      inet:ipv6-address
                  |     |        |        |     +--:(sid-case)
                  |     |        |        |        +--ro sid?               uint32
                  |     |        |        +--:(ero-metric-case)
                  |     |        |        |  +--ro ero-metric?        netc:te-metric
                  |     |        |        +--:(ipv4-ero-case)
                  |     |        |        |  +--ro loose?             boolean
                  |     |        |        |  +--ro address            inet:ipv4-address
                  |     |        |        +--:(ipv6-ero-case)
                  |     |        |        |  +--ro loose?             boolean
                  |     |        |        |  +--ro address            inet:ipv6-address
                  |     |        |        +--:(unnumbered-interface-id-ero-case)
                  |     |        |        |  +--ro loose?             boolean
                  |     |        |        |  +--ro router-id?         uint32
                  |     |        |        |  +--ro interface-id?      uint32
                  |     |        |        +--:(ipv4-ero-backup-case)
                  |     |        |        |  +--ro loose?             boolean
                  |     |        |        |  +--ro address            inet:ipv4-address
                  |     |        |        +--:(ipv6-ero-backup-case)
                  |     |        |        |  +--ro loose?             boolean
                  |     |        |        |  +--ro address            inet:ipv6-address
                  |     |        |        +--:(unnumbered-interface-id-backup-ero-case)
                  |     |        |           +--ro loose?             boolean
                  |     |        |           +--ro router-id?         uint32
                  |     |        |           +--ro interface-id?      uint32
                  |     |        +--:(prefix-sid-tlv-case)
                  |     |        |  +--ro (flags)?
                  |     |        |  |  +--:(isis-prefix-flags-case)
                  |     |        |  |  |  +--ro no-php?                boolean
                  |     |        |  |  |  +--ro explicit-null?         boolean
                  |     |        |  |  |  +--ro readvertisement?       boolean
                  |     |        |  |  |  +--ro node-sid?              boolean
                  |     |        |  |  +--:(ospf-prefix-flags-case)
                  |     |        |  |     +--ro no-php?                boolean
                  |     |        |  |     +--ro explicit-null?         boolean
                  |     |        |  |     +--ro mapping-server?        boolean
                  |     |        |  +--ro algorithm?             algorithm
                  |     |        |  +--ro (sid-label-index)?
                  |     |        |     +--:(local-label-case)
                  |     |        |     |  +--ro local-label?           netc:mpls-label
                  |     |        |     +--:(ipv6-address-case)
                  |     |        |     |  +--ro ipv6-address?          inet:ipv6-address
                  |     |        |     +--:(sid-case)
                  |     |        |        +--ro sid?                   uint32
                  |     |        +--:(ipv6-prefix-sid-tlv-case)
                  |     |        |  +--ro algorithm?             algorithm
                  |     |        +--:(sid-label-tlv-case)
                  |     |           +--ro (sid-label-index)?
                  |     |              +--:(local-label-case)
                  |     |              |  +--ro local-label?           netc:mpls-label
                  |     |              +--:(ipv6-address-case)
                  |     |              |  +--ro ipv6-address?          inet:ipv6-address
                  |     |              +--:(sid-case)
                  |     |                 +--ro sid?                   uint32
                  |     +--ro sr-binding-sid-labels*
                  |        +--ro weight?                weight
                  |        +--ro (flags)?
                  |        |  +--:(isis-binding-flags-case)
                  |        |  |  +--ro address-family?        boolean
                  |        |  |  +--ro mirror-context?        boolean
                  |        |  |  +--ro spread-tlv?            boolean
                  |        |  |  +--ro leaked-from-level-2?   boolean
                  |        |  |  +--ro attached-flag?         boolean
                  |        |  +--:(ospf-binding-flags-case)
                  |        |     +--ro mirroring?             boolean
                  |        +--ro binding-sub-tlvs*
                  |           +--ro (binding-sub-tlv)?
                  |              +--:(prefix-sid-case)
                  |              |  +--ro (flags)?
                  |              |  |  +--:(isis-prefix-flags-case)
                  |              |  |  |  +--ro no-php?            boolean
                  |              |  |  |  +--ro explicit-null?     boolean
                  |              |  |  |  +--ro readvertisement?   boolean
                  |              |  |  |  +--ro node-sid?          boolean
                  |              |  |  +--:(ospf-prefix-flags-case)
                  |              |  |     +--ro no-php?            boolean
                  |              |  |     +--ro explicit-null?     boolean
                  |              |  |     +--ro mapping-server?    boolean
                  |              |  +--ro algorithm?         algorithm
                  |              |  +--ro (sid-label-index)?
                  |              |     +--:(local-label-case)
                  |              |     |  +--ro local-label?       netc:mpls-label
                  |              |     +--:(ipv6-address-case)
                  |              |     |  +--ro ipv6-address?      inet:ipv6-address
                  |              |     +--:(sid-case)
                  |              |        +--ro sid?               uint32
                  |              +--:(ipv6-prefix-sid-case)
                  |              |  +--ro algorithm?         algorithm
                  |              +--:(sid-label-case)
                  |              |  +--ro (sid-label-index)?
                  |              |     +--:(local-label-case)
                  |              |     |  +--ro local-label?       netc:mpls-label
                  |              |     +--:(ipv6-address-case)
                  |              |     |  +--ro ipv6-address?      inet:ipv6-address
                  |              |     +--:(sid-case)
                  |              |        +--ro sid?               uint32
                  |              +--:(ero-metric-case)
                  |              |  +--ro ero-metric?        netc:te-metric
                  |              +--:(ipv4-ero-case)
                  |              |  +--ro loose?             boolean
                  |              |  +--ro address            inet:ipv4-address
                  |              +--:(ipv6-ero-case)
                  |              |  +--ro loose?             boolean
                  |              |  +--ro address            inet:ipv6-address
                  |              +--:(unnumbered-interface-id-ero-case)
                  |              |  +--ro loose?             boolean
                  |              |  +--ro router-id?         uint32
                  |              |  +--ro interface-id?      uint32
                  |              +--:(ipv4-ero-backup-case)
                  |              |  +--ro loose?             boolean
                  |              |  +--ro address            inet:ipv4-address
                  |              +--:(ipv6-ero-backup-case)
                  |              |  +--ro loose?             boolean
                  |              |  +--ro address            inet:ipv6-address
                  |              +--:(unnumbered-interface-id-backup-ero-case)
                  |                 +--ro loose?             boolean
                  |                 +--ro router-id?         uint32
                  |                 +--ro interface-id?      uint32
                  x--:(te-lsp-attributes-case)
                     +--ro te-lsp-attributes


Usage
^^^^^
The Link-State table in a instance of the speaker's Loc-RIB can be verified via REST:

**URL:** ``/rests/data/bgp-rib:bgp-rib/rib=bgp-example/loc-rib/tables=bgp-linkstate:linkstate-address-family,bgp-linkstate:linkstate-subsequent-address-family/bgp-linkstate:linkstate-routes?content=nonconfig``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Response Body:**

      .. code-block:: xml

         <linkstate-routes xmlns="urn:opendaylight:params:xml:ns:yang:bgp-linkstate">
            ...
         </linkstate-routes>

   .. tab:: JSON

      **Response Body:**

      .. code-block:: json

         {
             "bgp-linkstate:linkstate-routes": "..."
         }

.. note:: Link-State routes mapping to topology links/nodes/prefixes is supported by BGP Topology Provider.

BGP-LS supported TLVs
^^^^^^^^^^^^^^^^^^^^^

Here it is the list of the supported TLVs and subTLVs by the BGP Link State implementation.
TLVs in bold are newly supported since latest stable release scandium.

* [x] 256 - 257: Node Descriptors (rfc9552 section 5.2.1)
* [x] 258 - 263: Link Descriptors (rfc9552 section 5.2.2)
* [x] 263 - 265: Prefix Descriptors (rfc9552 section 5.2.3)
* [x] **266**: Node MSD (rfc8814)
* [x] **267**: Link MSD (rfc8814)
* [x] 512 - 517: Local Node Decriptors (rfc9552 section 5.2.1) 
* [x] **518**: SRv6 SId Information (rfc9514)
* [ ] 550 - 557: tunel, LSP, MPLS ID (drafts bgp-ls-te-path)
* [ ] 554: SR Policy (draft-ietf-idr-bgp-ls-sr-policy-02)
* [x] 1024 - 1029: Node Attributes (rfc9552 section 5.3.1)
* [x] 1028 - 1031: Link Attributes (rfc9552 section 5.3.2)
* [ ] 1032 - S-BFD Discriminators (rfc9247)
* [x] **1034 - 1037**: SR (rfc9085 section 2.1)
* [x] **1038**: SRv6 (rfc9514)
* [x] **1039 - 1046**: Flex-Algo (rfc 9351)
* [x] 1088 - 1098: Link Attributes (rfc9552 section 5.3.2)
* [x] 1099 - **1100**: Adjacency & LAN Adjacency SID (rfc9085 / 2.2.1 & 2.2.2)
* [x] 1101 - 1103: Peer SIDs (rfc9086)
* [ ] 1105: RTM Capability (rfc8169)
* [x] **1106 - 1108**: SRv6 (rfc9514)
* [x] 1114 - 1120: Extended TE metric (rfc8571)
* [ ] 1121: Gracefull restart (8379)
* [x] **1122**: ASLA (rfc9294)
* [x] 1152 - 1157: Prefix Attributes (rfc9552 section 5.3.3)
* [x] 1158: Prefix-SID (rfc9085)
* [x] 1159: Range (rfc9085)
* [ ] 1160: IS-IS Flood reflection (draft bgp-ls-isis-flood-reflection)
* [x] 1161: SID/Label (rfc9085 section 2.1.1)
* [x] **1162**: SRv6 locator (rfc9514)
* [x] 1170 - **1172**: Prefix attributes (rfc9085 section 2.3.2 & 2.3.3)
* [x] **1173**: Extended Administrative Group (rfc9104)
* [x] **1174**: Source OSPF Router-ID (rfc9085 / 2.3.4)
* [ ] 1180 - 1184: SPF (draft lsvr-bgp-spf)
* [ ] 1200 - 1217: SR Policy (draft bgp-ls-sr-policy)
* [ ] 1220: NRP (draft bgp-ls-sr-policy-nrp)
* [x] **1250 - 1252**: SRv6 (rfc9514)

References
^^^^^^^^^^
* `Distribution of Link-State and Traffic Engineering (TE) Information Using BGP <https://tools.ietf.org/html/rfc9552>`_
* `Border Gateway Protocol - Link-State (BGP-LS) Extensions for Segment Routing <https://tools.ietf.org/html/rfc9085>`_
* `Border Gateway Protocol - Link-State (BGP-LS) Extensions for Segment Routing BGP Egress Peer Engineering <https://tools.ietf.org/html/rfc9086>`_
* `Border Gateway Protocol - Link-State (BGP-LS) Extensions for Flexible Algorithm Advertisement <https://tools.ietf.org/html/rfc9351>`_
* `Border Gateway Protocol - Link-State (BGP-LS) Extensions for Segment Routing over IPv6 (SRv6) <https://tools.ietf.org/html/rfc9514>`_
* `Signaling Maximum SID Depth (MSD) Using the Border Gateway Protocol - Link State <https://tools.ietf.org/html/rfc8814>`_
* `BGP - Link State (BGP-LS) Advertisement of IGP Traffic Engineering Performance Metric Extensions <https://tools.ietf.org/html/rfc8571>`_
* `Application-Specific Link Attributes Advertisement Using the Border Gateway Protocol - Link State (BGP‑LS) <https://tools.ietf.org/html/rfc9294>`_
* `BGP Link-State Information Distribution Implementation Report <https://tools.ietf.org/html/draft-ietf-idr-ls-distribution-impl-04>`_
 
