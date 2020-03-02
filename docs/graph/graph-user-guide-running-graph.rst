.. _graph-user-guide-running-graph:

Running Graph
=============
This section explains how to install Graph plugin.

1. Install Graph feature - ``features-graph``.
   Also, for the sake of this sample, it is required to install RESTCONF.
   In the Karaf console, type command:

   .. code-block:: console

      feature:install features-restconf features-graph

2. The Graph plugin contains a default empty configuration, which is applied
   after the feature starts up. One instance of Graph plugin is created
   (named *graph-topology*), and its presence can be verified via REST:

   **URL:** ``restconf/config/graph:graph-topology``

   **Method:** ``GET``

   **Response Body:**

   .. code-block:: json

      {}

   It is also posible to access to the operational graph topology which is
   also empty by default via REST:

   **URL:** ``restconf/operational/graph:graph-topology``

   **Method:** ``GET``

   **Response Body:**

   .. code-block:: json

      {}
