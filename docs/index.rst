.. _bgpcep:

####################
BGPCEP Documentation
####################

This documentation provides critical information needed to help you write
code for the BGPCEP project.

.. _bgpcep-restconf-endpoint:

RESTCONF endpoint
-----------------

The examples in these guides use the default Netty RESTCONF endpoint.
Install the ``odl-restconf-nb`` Karaf feature to enable it.

The default base URL is ``http://<controller-ip>:8182/restconf``.
Replace ``<controller-ip>`` with your controller's address. Data and
RPC requests use the following URL prefixes:

* ``http://<controller-ip>:8182/restconf/data/...``
* ``http://<controller-ip>:8182/restconf/operations/...``

Relative URLs starting with ``/restconf/`` use the same controller
address and port ``8182``. If you configure a different endpoint,
adjust the examples accordingly.

The JAX-RS RESTCONF endpoint at ``http://<controller-ip>:8181/rests``
is deprecated and retained for legacy deployments. It requires the
``odl-restconf-nb-jaxrs`` feature. When using that endpoint, replace
the port and RESTCONF root in the examples with ``8181`` and
``/rests``, respectively.


Developer Guides
-----------------

.. toctree::
   :maxdepth: 1

   bgp-developer-guide
   bgp-monitoring-protocol-developer-guide
   pcep-developer-guide

User Guides
-----------

.. toctree::
   :maxdepth: 1

   bgp/index
   bmp/index
   pcep/index
   graph/index
   algo/index
