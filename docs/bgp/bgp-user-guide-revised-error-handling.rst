.. _bgp-user-guide-revised-error-handling:

Revised Error Handling for BGP UPDATE Messages
==============================================

According to `RFC4271 <https://tools.ietf.org/html/rfc4271>`_ a BGP speaker that receives an UPDATE message containing a malformed attribute
is required to reset the session over which the offending attribute was received. Revised Error Handling procedures defined in `RFC7606 <https://tools.ietf.org/html/rfc7606>`_
is introducing a ways to avoid negative effects of session restart. This document provides guide to configure specific approach called *treat-as-withdraw*
which is treating malformed UPDATE messages as their withdrawal equivalent.

Configuration
^^^^^^^^^^^^^
*Treat-as-withdraw* procedures are disabled by default. There are two ways to enable it. One via *peer-group* to affect all neighbors in that group and one via *neighbor*.

For *neighbor* configuration:

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/neighbors/neighbor/192.0.2.1/error-handling``

For *peer-group* configuration:

**XML**

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/peer-groups/peer-group/external-neighbor/error-handling``

**Method:** ``PUT``

**Content-Type:** ``application/xml``

**Request Body:**

.. code-block:: xml
   :linenos:
   :emphasize-lines: 3

   <error-handling xmlns="urn:opendaylight:params:xml:ns:yang:bgp:openconfig-extensions">
        <config>
            <treat-as-withdraw>true</treat-as-withdraw>
        </config>
    </error-handling>

@line 3: *True* to enable *treat-as-withdraw* procedures, *False* to disabled it

**JSON**

**URL:** ``/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/bgp-example/bgp/peer-groups/peer-group/external-neighbor/error-handling``

**Method:** ``PUT``

**Content-Type:** ``application/json``

**Request Body:**

.. code-block:: json
   :linenos:
   :emphasize-lines: 4

   {
       "bgp-openconfig-extensions:error-handling": {
           "config": {
               "treat-as-withdraw": true
           }
       }
   }

@line 4: *True* to enable *treat-as-withdraw* procedures, *False* to disabled it

.. note:: If neighbor Error handling configuration in *neighbor* ALWAYS supersed *peer-group* configuration. That means if *peer-group* have error handling enabled and *neighbor* disabled, result is disabled error handling.

References
^^^^^^^^^^
* `Revised Error Handling for BGP UPDATE Messages <https://tools.ietf.org/html/rfc7606>`_