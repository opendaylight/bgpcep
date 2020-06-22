.. _bgp-user-guide-running-bgp:

Running BGP
===========
This section explains how to install BGP plugin.

1. Install BGP feature - ``odl-bgpcep-bgp``.
   Also, for sake of this sample, it is required to install RESTCONF.
   In the Karaf console, type command:

   .. code-block:: console

      feature:install odl-restconf odl-bgpcep-bgp

2. The BGP plugin contains a default configuration, which is applied after the feature starts up.
   One instance of BGP plugin is created (named *example-bgp-rib*), and its presence can be verified via REST:

   **XML**

   **URL:** ``/restconf/operational/bgp-rib:bgp-rib``
 
   **RFC8040 URL:** ``/rests/data/bgp-rib:bgp-rib?content=non-config``

   **Method:** ``GET``

   **Response Body:**

   .. code-block:: xml

      <bgp-rib xmlns="urn:opendaylight:params:xml:ns:yang:bgp-rib">
         <rib>
             <id>example-bgp-rib</id>
             <loc-rib>
             ....
             </loc-rib>
         </rib>
      </bgp-rib>

   **JSON** 

   **URL:** ``/restconf/operational/bgp-rib:bgp-rib``

   **RFC8040 URL:** ``/rests/data/bgp-rib:bgp-rib?content=non-config``

   **Method:** ``GET``

   **Response Body:**

   .. code-block:: json

      {
          "bgp-rib": {
              "rib": {
                  "id": "example-bgp-rib",
                  "loc-rib": "...."
              }
          }
      }
