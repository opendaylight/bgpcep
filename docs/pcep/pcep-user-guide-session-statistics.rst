.. _pcep-user-guide-session-statistics:

Session statistics
==================
The PCEP statistics provides information about PCE <-> PCC session and its stateful listener (topology-provider).

Usage
'''''

**URL:** ``/restconf/operational/network-topology:network-topology/topology/pcep-topology/node/pcc:%2F%2F43.43.43.43/pcep-session-state``

**RFC8040 URL:** ``/rests/data/network-topology:network-topology/topology=pcep-topology/node=pcc%3A%2F%2F43.43.43.43/pcep-session-state?content=nonconfig``

**Method:** ``GET``

.. tabs::

   .. tab:: XML

      **Response Body:**

      .. code-block:: xml
         :linenos:
         :emphasize-lines: 3,4,5,6,7,8,9,10,12,13,14,15,24,25,26,27,30,31,32,33,36,37,38,40,41,42

         <pcep-session-state xmlns="urn:opendaylight:params:xml:ns:yang:topology:pcep:stats">
            <messages>
               <last-received-rpt-msg-timestamp xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">1512640592</last-received-rpt-msg-timestamp>
               <sent-upd-msg-count xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">0</sent-upd-msg-count>
               <received-rpt-msg-count xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">2</received-rpt-msg-count>
               <sent-init-msg-count xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">0</sent-init-msg-count>
               <sent-msg-count>0</sent-msg-count>
               <last-sent-msg-timestamp>0</last-sent-msg-timestamp>
               <unknown-msg-received>0</unknown-msg-received>
               <received-msg-count>2</received-msg-count>
               <error-messages>
                  <last-sent-error></last-sent-error>
                  <received-error-msg-count>0</received-error-msg-count>
                  <sent-error-msg-count>0</sent-error-msg-count>
                  <last-received-error></last-received-error>
               </error-messages>
               <reply-time>
                  <average-time>0</average-time>
                  <min-time>0</min-time>
                  <max-time>0</max-time>
               </reply-time>
            </messages>
            <peer-pref>
               <keepalive>30</keepalive>
               <deadtimer>120</deadtimer>
               <ip-address>127.0.0.1</ip-address>
               <session-id>0</session-id>
            </peer-pref>
            <local-pref>
               <keepalive>30</keepalive>
               <deadtimer>120</deadtimer>
               <ip-address>127.0.0.1</ip-address>
               <session-id>0</session-id>
            </local-pref>
            <peer-capabilities>
               <stateful xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">true</stateful>
               <instantiation xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">true</instantiation>
               <active xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">true</active>
            </peer-capabilities>
            <session-duration>0:00:00:18</session-duration>
            <delegated-lsps-count>1</delegated-lsps-count>
            <synchronized>true</synchronized>
         </pcep-session-state>

      @line 3: **last-received-rpt-msg-timestamp** - The timestamp of last received PCRpt message.

      @line 4: **sent-upd-msg-count** - The number of sent PCUpd messages.

      @line 5: **received-rpt-msg-count** - The number of received PcRpt messages.

      @line 6: **sent-init-msg-count** - The number of sent PCInitiate messages.

      @line 7: **sent-msg-count** - Total number of sent PCEP messages.

      @line 8: **last-sent-msg-timestamp** - The timestamp of last sent message.

      @line 9: **unknown-msg-received** - The number of received unknown messages.

      @line 10: **received-msg-count** - Total number of received PCEP messages.

      @line 12: **last-sent-error** - Type/value tuple of last sent error.

      @line 13: **received-error-msg-count** - Total number of received PCErr messages.

      @line 14: **sent-error-msg-count** - Total number of sent PCErr messages.

      @line 15: **last-received-error** - Type/value tuple of last sent error.

      @line 24: **keepalive** - Advertised keep-alive value.

      @line 25: **deadtimer** - Advertised deadtimer value.

      @line 26: **ip-address** - Peer's IP address.

      @line 27: **session-id** - Peer's session identifier.

      @line 30: **keepalive** - Advertised keep-alive value.

      @line 31: **deadtimer** - Advertised deadtimer value.

      @line 32: **ip-address** - Peer's IP address.

      @line 33: **session-id** - Peer's session identifier.

      @line 35: **stateful** - Represents peer's stateful/stateless capability.

      @line 36: **instantiation** - Represents peer's instantiation capability.

      @line 37: **active** - Represents peer's LSP update capability.

      @line 40: **session-duration** - Elapsed time (in d:H:m:s) from session-up until last statistic update.

      @line 41: **delegated-lsps-count** - The number of delegated LSPs (tunnels) from PCC.

      @line 42: **synchronized** - Represents synchronization status.

   .. tab:: JSON

      **Response Body:**

      .. code-block:: json
         :linenos:
         :emphasize-lines: 4,5,6,7,8,9,10,11,13,14,15,16,25,26,27,28,31,32,33,34,37,38,39,41,42,43

         {
             "pcep-session-state": {
                 "messages": {
                     "last-received-rpt-msg-timestamp": 1512640592,
                     "sent-upd-msg-count": 0,
                     "received-rpt-msg-count": 2,
                     "sent-init-msg-count": 0,
                     "sent-msg-count": 0,
                     "last-sent-msg-timestamp": 0,
                     "unknown-msg-received": 0,
                     "received-msg-count": 2,
                     "error-messages": {
                         "last-sent-error": null,
                         "received-error-msg-count": 0,
                         "sent-error-msg-count": 0,
                         "last-received-error": null
                     },
                     "reply-time": {
                         "average-time": 0,
                         "min-time": 0,
                         "max-time": 0
                     }
                 },
                 "peer-pref": {
                     "keepalive": 30,
                     "deadtimer": 120,
                     "ip-address": "127.0.0.1",
                     "session-id": 0
                 },
                 "local-pref": {
                     "keepalive": 30,
                     "deadtimer": 120,
                     "ip-address": "127.0.0.1",
                     "session-id": 0
                 },
                 "peer-capabilities": {
                     "stateful": true,
                     "instantiation": true,
                     "active": true
                 },
                 "session-duration": "0:00:00:18",
                 "delegated-lsps-count": 1,
                 "synchronized": true
             }
         }

      @line 4: **last-received-rpt-msg-timestamp** - The timestamp of last received PCRpt message.

      @line 5: **sent-upd-msg-count** - The number of sent PCUpd messages.

      @line 6: **received-rpt-msg-count** - The number of received PcRpt messages.

      @line 7: **sent-init-msg-count** - The number of sent PCInitiate messages.

      @line 8: **sent-msg-count** - Total number of sent PCEP messages.

      @line 9: **last-sent-msg-timestamp** - The timestamp of last sent message.

      @line 10: **unknown-msg-received** - The number of received unknown messages.

      @line 11: **received-msg-count** - Total number of received PCEP messages.

      @line 13: **last-sent-error** - Type/value tuple of last sent error.

      @line 14: **received-error-msg-count** - Total number of received PCErr messages.

      @line 15: **sent-error-msg-count** - Total number of sent PCErr messages.

      @line 16: **last-received-error** - Type/value tuple of last sent error.

      @line 25: **keepalive** - Advertised keep-alive value.

      @line 26: **deadtimer** - Advertised deadtimer value.

      @line 27: **ip-address** - Peer's IP address.

      @line 28: **session-id** - Peer's session identifier.

      @line 31: **keepalive** - Advertised keep-alive value.

      @line 32: **deadtimer** - Advertised deadtimer value.

      @line 33: **ip-address** - Peer's IP address.

      @line 34: **session-id** - Peer's session identifier.

      @line 37: **stateful** - Represents peer's stateful/stateless capability.

      @line 38: **instantiation** - Represents peer's instantiation capability.

      @line 39: **active** - Represents peer's LSP update capability.

      @line 41: **session-duration** - Elapsed time (in d:H:m:s) from session-up until last statistic update.

      @line 42: **delegated-lsps-count** - The number of delegated LSPs (tunnels) from PCC.

      @line 43: **synchronized** - Represents synchronization status.

Following RPC can be used to fetch PCEP session statistics. If PCEP topology and/or PCC node is not specified in input,
statistics for all PCEP sessions under the context are returned.

Usage
'''''

**URL:** ``/restconf/operations/pcep-topology-stats-rpc:get-stats``

**RFC8040 URL:** ``/rests/operations/pcep-topology-stats-rpc:get-stats``

**Method:** ``POST``

.. tabs::

   .. tab:: XML

      **Content-Type:** ``application/xml``

      **Request Body:**

      .. code-block:: xml

         <input xmlns="urn:opendaylight:params:xml:ns:yang:pcep:topology:stats:rpc">
            <topology>
               <topology-id>pcep-topology</topology-id>
               <node>
                  <node-id>pcc://43.43.43.43</node-id>
               </node>
            </topology>
         </input>

      **Response Body:**

      .. code-block:: xml

         <output xmlns="urn:opendaylight:params:xml:ns:yang:pcep:topology:stats:rpc">
            <topology>
               <topology-id>pcep-topology</topology-id>
               <node>
                  <node-id>pcc://43.43.43.43</node-id>
                  <pcep-session-state>
                     <synchronized>true</synchronized>
                     <peer-capabilities>
                        <stateful xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">true</stateful>
                        <instantiation xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">true</instantiation>
                        <active xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">true</active>
                     </peer-capabilities>
                     <local-pref>
                        <keepalive>30</keepalive>
                        <deadtimer>120</deadtimer>
                        <session-id>1</session-id>
                        <ip-address>127.0.0.1</ip-address>
                     </local-pref>
                     <session-duration>4:01:59:46</session-duration>
                     <messages>
                        <unknown-msg-received>0</unknown-msg-received>
                        <received-msg-count>11752</received-msg-count>
                        <error-messages>
                           <last-sent-error>
                              <error-type>0</error-type>
                              <error-value>0</error-value>
                           </last-sent-error>
                           <received-error-msg-count>0</received-error-msg-count>
                           <last-received-error>
                              <error-type>0</error-type>
                              <error-value>0</error-value>
                           </last-received-error>
                           <sent-error-msg-count>0</sent-error-msg-count>
                        </error-messages>
                        <sent-msg-count>11759</sent-msg-count>
                        <last-sent-msg-timestamp>1553547804</last-sent-msg-timestamp>
                        <reply-time>
                           <average-time>0</average-time>
                           <min-time>0</min-time>
                           <max-time>0</max-time>
                        </reply-time>
                        <received-rpt-msg-count xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">1</received-rpt-msg-count>
                        <sent-init-msg-count xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">0</sent-init-msg-count>
                        <last-received-rpt-msg-timestamp xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">1553195032</last-received-rpt-msg-timestamp>
                        <sent-upd-msg-count xmlns="urn:opendaylight:params:xml:ns:yang:pcep:stateful:stats">0</sent-upd-msg-count>
                     </messages>
                     <peer-pref>
                        <keepalive>30</keepalive>
                        <deadtimer>120</deadtimer>
                        <session-id>8</session-id>
                        <ip-address>127.0.0.1</ip-address>
                     </peer-pref>
                     <delegated-lsps-count>0</delegated-lsps-count>
                  </pcep-session-state>
               </node>
            </topology>
         </output>

   .. tab:: JSON

      **Content-Type:** ``application/json``

      **Request Body:**

      .. code-block:: json

         {
             "input": {
                 "topology": [
                     {
                       "topology-id": "pcep-topology",
                         "node": [
                             {
                                 "node-id": "pcc://43.43.43.43"
                             }
                         ]
                     }
                 ]
             }
         }

      **Response Body:**

      .. code-block:: json

         {
             "output": {
                 "topology": {
                     "topology-id": "pcep-topology",
                     "node": {
                         "node-id": "pcc://43.43.43.43",
                         "pcep-session-state": {
                             "synchronized": true,
                             "peer-capabilities": {
                                 "stateful": true,
                                 "instantiation": true,
                                 "active": true
                             },
                             "local-pref": {
                                 "keepalive": 30,
                                 "deadtimer": 120,
                                 "session-id": 1,
                                 "ip-address": "127.0.0.1"
                             },
                             "session-duration": "4:01:59:46",
                             "messages": {
                                 "unknown-msg-received": 0,
                                 "received-msg-count": 11752,
                                 "error-messages": {
                                     "last-sent-error": {
                                     "error-type": 0,
                                     "error-value": 0
                                 },
                                 "received-error-msg-count": 0,
                                     "last-received-error": {
                                     "error-type": 0,
                                     "error-value": 0
                                 },
                                 "sent-error-msg-count": 0
                             },
                             "sent-msg-count": 11759,
                             "last-sent-msg-timestamp": 1553547804,
                             "reply-time": {
                                 "average-time": 0,
                                 "min-time": 0,
                                 "max-time": 0
                             },
                             "received-rpt-msg-count": 1,
                             "sent-init-msg-count": 0,
                             "last-received-rpt-msg-timestamp": 1553195032,
                             "sent-upd-msg-count": 0
                         },
                         "peer-pref": {
                             "keepalive": 30,
                             "deadtimer": 120,
                             "session-id": 8,
                             "ip-address": "127.0.0.1"
                         },
                         "delegated-lsps-count": 0
                     }
                 }
             }
         }
