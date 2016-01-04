/*
 * Copyright (c) 2014 Tata Consultancy Services.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.sdniwrapper;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.Authenticator;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdniWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(SdniWrapper.class);


    private final String QOS_URL = "http://localhost:8181/restconf/operations/opendaylight-sdni-qos-msg:get-all-node-connectors-statistics";
    private final String CONTROLLER_URL = "http://localhost:8181/restconf/operations/opendaylight-sdni-topology-msg:get-Topology";
    public static Map peer_information = new HashMap();
    private static int peer_count = 0;
    private static int qos_peer_count = 0;
    private static final String JDBC_DRIVER = "org.sqlite.JDBC";
    private static final String DB_URL = "jdbc:sqlite:/home/tcs/sdni/database/CONTROLLER_TOPOLOGY_DATABASE";
    private static final String QOS_DB_URL = "jdbc:sqlite:/home/tcs/sdni/database/CONTROLLER_QOS_DATABASE";



    /**
     * Finds IPv4 address of the local VM TODO: This method is
     * non-deterministic. There may be more than one IPv4 address. Cant say
     * which address will be returned. Read IP from a property file or enhance
     * the code to make it deterministic.
     *
     * @return String
     */
    public String findIpAddress() {
        Enumeration e = null;
        try {
            e = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e1) {
            LOG.warn("Failed to get list of interfaces {0}", e1);
            return null;
        }
        while (e.hasMoreElements()) {

            NetworkInterface n = (NetworkInterface) e.nextElement();

            Enumeration ee = n.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress i = (InetAddress) ee.nextElement();
                if ((i instanceof Inet4Address) && (!i.isLoopbackAddress())) {
                    String hostAddress = i.getHostAddress();
                    return hostAddress;
                }
            }
        }
        LOG.debug("Failed to find a suitable host address");
        return null;
    }

    /*public void printPeerInformation() {
        LOG.info("inside printPeerInformation");
        Iterator<Map.Entry<Integer, Integer>> entries = peer_information
                .entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Integer, Integer> entry = entries.next();
            LOG.info("Key = " + entry.getKey() + ", Value = "
                    + entry.getValue());
        }
    }*/
    public ByteBuf getSDNIMessage() {
        LOG.trace("Sdniwrapper : getSDNIMessage - Start");
        byte[] defaultBytes = null;
        String topologyDetails = null;
        ByteBuf sdniBytes = null;

        // Read the message/topology details from the controller's Rest API
        topologyDetails = callRestAPI(CONTROLLER_URL);

        // Convert the message from string to byte array
        defaultBytes = topologyDetails.getBytes();
        sdniBytes = Unpooled.copiedBuffer(defaultBytes);
        LOG.trace("TOPO: Convert sdni message into ByteBuf: {}", sdniBytes);

        //Parse rest api content and populate TOPOLOGY_DATABASE data in sqlite
        parseSDNIMessage(topologyDetails);
        LOG.trace("Sdniwrapper : getSDNIMessage - End");
        return sdniBytes;
    }


    public String parseSDNIMessage(ByteBuf msg) {
        LOG.trace("Sdniwrapper : parseSDNIMessage - Start");
        String result = "";
        byte[] bytes = new byte[msg.readableBytes()];
        int readerIndex = msg.readerIndex();
        msg.getBytes(readerIndex, bytes);
        String sdniMsg = new String(bytes);

        LOG.trace("TOPO: Before parsing sdni message from ByteBuf to String: {}", sdniMsg);
        result = parseSDNIMessage(sdniMsg);
        LOG.trace("Sdniwrapper : parseSDNIMessage - End");
        return result;

    }

    @SuppressWarnings("unchecked")
    public String parseSDNIMessage(String sdnimsg) {
        LOG.trace("Sdniwrapper : parseSDNIMessage (sdni msg)- Start");
        final List<NetworkCapabilities> listTopo = new ArrayList<NetworkCapabilities>();

        try {

            String message = sdnimsg.replace('"', '\"');
            JSONObject json=new JSONObject(message);

            for(int i=0;i<json.getJSONObject("output").getJSONObject("network-topology").getJSONArray("topology").length();i++)
            {
                JSONObject topologyObj = (JSONObject)json.getJSONObject("output").getJSONObject("network-topology").getJSONArray("topology").get(i);
                List<String> node_id_list = new ArrayList<String>();
                List<String> host_id_list = new ArrayList<String>();
                List<String> controller_ip_list = new ArrayList<String>();
                List<String> link_list = new ArrayList<String>();

                JSONArray nodeArray = topologyObj.getJSONArray("node");
                JSONArray linkArray = topologyObj.getJSONArray("link");
                controller_ip_list.add(topologyObj.getString("controller-ip"));

                NetworkCapabilities networkData = new NetworkCapabilities();

                for(int j=0;j<nodeArray.length();j++)
                {

                    if(!((JSONObject)nodeArray.get(j)).getString("node-id").contains("host"))
                        node_id_list.add(((JSONObject)nodeArray.get(j)).getString("node-id").replace("openflow:",""));
                    else
                        host_id_list.add(((JSONObject)nodeArray.get(j)).getString("node-id").replace("host:",""));
                }

                for(int j=0;j<linkArray.length();j++)
                {
                    String a = ((JSONObject)((JSONObject)linkArray.get(j)).get("source")).get("source-tp")+"->"+
                        ((JSONObject)((JSONObject)linkArray.get(j)).get("destination")).get("dest-tp");
                    link_list.add(a.replace("openflow:", "").replace("host:", ""));
                }


                networkData.setHost(host_id_list);
                networkData.setLink(link_list);
                networkData.setNode(node_id_list);
                networkData.setController(controller_ip_list);
                listTopo.add(networkData);
            }


        } catch (Exception e) {
            LOG.trace("Exception: {0}", e);
            return "IOException";
        }

        for(int i=0;i<listTopo.size();i++)
        {

            NetworkCapabilities networkData = listTopo.get(i);
            boolean peer = false;
            String controllerIP = networkData.getController().get(0);
            if (controllerIP.equals(findIpAddress())) {
                peer = true;
            }
            if (peer) {
                LOG.trace("TOPO: Before calling updatePeerTable");
                updatePeerTable(networkData);
            } else {
                updateControllerTable(networkData);
            }

        }
        LOG.trace("Sdniwrapper : parseSDNIMessage (sdni msg)- END");
        return "success";
    }


    public void updateControllerTable(NetworkCapabilities networkData){
        LOG.trace("Sdniwrapper : updateControllerTable- Start");

        Connection conn = null;
        Statement stmt = null;
        int peer_count = 0;

        LOG.trace("TOPO: inside updateControllerTable PeerCount:0");

        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL);

            LOG.trace("TOPO: sql connection established");

            stmt = conn.createStatement();
            String sql = "drop table if exists TOPOLOGY_DATABASE ";
            stmt.executeUpdate(sql);
            LOG.trace("TOPO: SQL query to delete controller table: {}", sql);

            sql = "create table TOPOLOGY_DATABASE (controller varchar(20), links varchar(30));";
            stmt.executeUpdate(sql);
            LOG.trace("TOPO: SQL query to create controller table: {}", sql);

            String insertQueries = formInsertQuery(networkData, peer_count);
            String[] insertQuery = insertQueries.split("--");
            for(int j = 0; j< insertQuery.length;j++){
                LOG.trace("insertQuery: {}", insertQuery[j]);
                stmt.executeUpdate(insertQuery[j]);
            }
        } catch (SQLException se) {
            LOG.trace("SQLException: {0}",se);
            return;
        } catch (Exception e) {
            LOG.trace("Exception: {0}",e);
            return;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException se2) {
                LOG.trace("SQLException2: {0}",se2);
                return;
            }

            try {
                if (conn != null)
                        conn.close();
            } catch (SQLException se) {
                LOG.trace("SQLException3: {0}",se);
                return;
            }
        }
        LOG.trace("Sdniwrapper : updateControllerTable- End");
    }

    public void updatePeerTable(NetworkCapabilities networkData){
        LOG.trace("Sdniwrapper : updatePeerTable- Start");
        String ipAddress = networkData.getController().toString().replace("[","").replace("]","");
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        boolean tableExist = false;
        LOG.trace("TOPO:Inside updatePeerTable PeerCount: {}", peer_count);

        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL);
            stmt = conn.createStatement();

            LOG.trace("sql connection established");

            LOG.trace("TOPO:in update Peer Table ipAddress: {} findIpAddress(): {}", ipAddress, findIpAddress());

            //check for the self controller ip
            if (ipAddress != findIpAddress()) {
                //if not, get the peercount and loop it
                for(int i=1;i<=peer_count;i++){
                    //check for the exist of peer controller 1 table
                    String tableName = "TOPOLOGY_DATABASE_PEER_"+i;
                    String sql = "SELECT controller FROM "+tableName +" LIMIT 1";
                    rs = stmt.executeQuery(sql);
                    long peerIP = 0L;
                    while (rs.next()) {
                        peerIP = rs.getLong("controller");
                    }
                    String peerIPAddress =ntoa(peerIP);
                    LOG.trace("TOPO: Inside for loop, ipAddress: {} peerIPAdrress:{}",ipAddress,peerIPAddress);
                    if (ipAddress.equals(peerIPAddress)) {

                        sql = "drop table TOPOLOGY_DATABASE_PEER_"+i;
                        stmt.executeUpdate(sql);
                        LOG.trace("TOPO: inside if, SQL query to delete topology peer table: {}", sql);

                        sql = "create table TOPOLOGY_DATABASE_PEER_"+i+" (controller varchar(20), links varchar(30));";
                        LOG.trace("TOPO: inside if SQL query to create topology peer table: {}", sql);
                        stmt.executeUpdate(sql);

                        String insertQueries = formInsertQuery(networkData, i);
                        String[] insertQuery = insertQueries.split("--");
                        for(int j = 0; j< insertQuery.length;j++){
                            LOG.trace("insertQUery: {0}", insertQuery[j]);
                            stmt.executeUpdate(insertQuery[j]);
                        }
                        tableExist = true;
                    }
                }

                if (!tableExist) {
                    peer_count++;
                    LOG.trace("TOPO: now peerCount: {}", peer_count);
                    //create a new table TOPOLOGY_DATABASE_PEER + count
                    String sql = "create table TOPOLOGY_DATABASE_PEER_"+peer_count+" (controller bigint(20), links int(11), nodes int(11), hosts int(11), link_bandwidths bigint(20) , latencies int(11), macAddressList bigint(20), ipAddressList bigint(20));";
                    LOG.trace("TOPO:SQL query to create topology peer table for first time: {}", sql);
                    stmt = conn.createStatement();
                    stmt.executeUpdate(sql);
                    String insertQueries = formInsertQuery(networkData, peer_count);
                    String[] insertQuery = insertQueries.split("--");
                    for(int i = 0; i< insertQuery.length-1;i++){
                        LOG.trace("insertQUery: {0}", insertQuery[i]);
                        stmt.executeUpdate(insertQuery[i]);
                    }
                }
            } else {
                //update self controller table
                LOG.trace("inside update self controller table ie topology_database");
            }
            LOG.trace("TOPO:Peer count at the end of try in topology updatetable {}",peer_count);
        } catch (SQLException se) {
            LOG.trace("SQLException: {0}", se);
            return;
        } catch (Exception e) {
            LOG.trace("Exception: {0}", e);
            return;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException se2) {
                LOG.trace("SQLException2: {0}", se2);
                return;
            }

            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se) {
                LOG.trace("SQLException3: {0}", se);
                return;
            }
        }
        LOG.trace("Sdniwrapper : updatePeerTable- End");
    }

    public String formInsertQuery(NetworkCapabilities networkData, int peer_count){
        LOG.trace("QOS: Inside formQOSInsertQuery peercount {}",peer_count);
        String insertQuery = "";

        String controller = networkData.getController().get(0);
        List<String> link_id_list = networkData.getLink();
        long controllerIP = ipToLong(controller);

        for(int i=0;i<link_id_list.size();i++)
        {
            String link = link_id_list.get(i);
            if (peer_count == 0) {
                insertQuery +=  "insert into TOPOLOGY_DATABASE values (\"" + controllerIP + "\",\"" + link + "\"); -- ";
            } else {
                insertQuery +=  "insert into TOPOLOGY_DATABASE_PEER_"+peer_count+" values( \"" + controllerIP + "\",\"" + link + "\"); -- ";
            }

        }
        LOG.trace("QOS: At the end of formQOSInsertQuery() method insertQuery:{}",insertQuery);
        return insertQuery;
    }

    public ByteBuf getSDNIQOSMessage() {
        byte[] defaultBytes = null;
        String qosDetails = null;
        ByteBuf sdniQOSBytes = null;

        // Read the message/topology details from the controller's Rest API
        qosDetails = callRestAPI(QOS_URL);

        // Convert the message from string to byte array
        defaultBytes = qosDetails.getBytes();
        sdniQOSBytes = Unpooled.copiedBuffer(defaultBytes);
        LOG.trace("QOS: Convert sdni qos message into ByteBuf");

        //Parse rest api content and populate TOPOLOGY_DATABASE data in sqlite
        parseSDNIQOSMessage(qosDetails);

        return sdniQOSBytes;
    }

    public String parseSDNIQOSMessage(ByteBuf msg) {
        String result = "";
        byte[] bytes = new byte[msg.readableBytes()];
        int readerIndex = msg.readerIndex();
        msg.getBytes(readerIndex, bytes);
        String sdniQOSMsg = new String(bytes);

        LOG.trace("QOS: After parsing sdni qos message from ByteBuf to String: {}", sdniQOSMsg);
        result = parseSDNIQOSMessage(sdniQOSMsg);
        return result;

    }
    @SuppressWarnings("unchecked")
    public String parseSDNIQOSMessage(String sdniQOSMsg) {
        final List<NetworkCapabilitiesQOS> list_QoS = new ArrayList();
        boolean peer = true;
        String controller = null;
        try {

            String message = sdniQOSMsg.replace('"', '\"');

            LOG.trace("QOS: Started parsing sdni message to NetworkCapabilitiesQOS {}", sdniQOSMsg);

            JSONObject json=new JSONObject(message);

            for(int i=0;i<json.getJSONObject("output").getJSONArray("node-list").length();i++)
            {
                JSONObject jObj = (JSONObject)json.getJSONObject("output").getJSONArray("node-list").get(i);
                for(int j=0; j<jObj.getJSONArray("port-list").length();j++) {
                    JSONObject jSubObj = (JSONObject)jObj.getJSONArray("port-list").get(j);

                    NetworkCapabilitiesQOS qosData = new NetworkCapabilitiesQOS();
                    qosData.setController(json.getJSONObject("output").getString("controller-ip"));
                    qosData.setNode(jObj.getString("node-id"));
                    qosData.setPort(jSubObj.getString("port-id"));
                    JSONObject jSubSubObj = (JSONObject)jSubObj.getJSONArray("port-params").get(0);
                    qosData.setReceiveCrcError(jSubSubObj.get("receive-crc-error").toString());
                    qosData.setReceiveFrameError(jSubSubObj.get("receive-frame-error").toString());
                    qosData.setReceiveOverRunError(jSubSubObj.get("receive-over-run-error").toString());
                    qosData.setCollisionCount(jSubSubObj.get("collision-count").toString());
                    qosData.setTransmitPackets(jSubSubObj.getJSONObject("packets").get("transmitted").toString());
                    qosData.setReceivePackets(jSubSubObj.getJSONObject("packets").get("received").toString());

                    list_QoS.add(qosData);
                }
            }
            if(controller==null || controller =="")
            {
                controller = findIpAddress();
            }

        }
        catch (Exception e) {
            LOG.trace("Exception: {0}", e);
            return "Exception";
        }

        //Update peer/controller table in database
        if (controller.equals(findIpAddress())) {
            peer = false;
        }
        if (peer) {
            LOG.trace("Before calling updatePeerQOSTable");
            updatePeerQOSTable(list_QoS);
        } else {
            LOG.trace("Before calling updateControllerQOSTable");
            updateControllerQOSTable(list_QoS);
        }
        return "success";
    }

    public void updateControllerQOSTable(List<NetworkCapabilitiesQOS> list){

        Connection conn = null;
        Statement stmt = null;
        int qos_peer_count = 0;

        LOG.trace("QOS: Inside updateControllerQOSTable PeerCount:0");

        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(QOS_DB_URL);

            LOG.trace("sql connection established");

            stmt = conn.createStatement();
            String sql = "drop table if exists QOS_DATABASE ";
            stmt.executeUpdate(sql);
            LOG.trace("QOS: SQL query to delete Controller QOS table: {}", sql);

            sql = "create table QOS_DATABASE (controller varchar(20), node varchar(20), port varchar(20), receiveFrameError varchar(10) , receiveOverRunError varchar(10), receiveCrcError varchar(10), collisionCount varchar(10), receivePackets varchar(10), transmitPackets varchar(10));";
            stmt.executeUpdate(sql);
            LOG.trace("QOS: SQL query to create Controller QOS table: {}", sql);
            String insertQueries = formQOSInsertQuery(list, qos_peer_count);
            String[] insertQuery = insertQueries.split("--");

            for(int j = 0; j< insertQuery.length;j++){
                String query = insertQuery[j].replace("LOCAL", "0");
                stmt.executeUpdate(query);
            }
            LOG.trace("QOS: InsertQuery after executing:");

        } catch (SQLException se) {
            LOG.trace("SQLException: {0}",se);
            return;
        } catch (Exception e) {
            LOG.trace("Exception: {0}",e);
            return;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException se2) {
                LOG.trace("SQLException2: {0}",se2);
                return;
            }

            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se) {
                LOG.trace("SQLException3: {0}",se);
                return;
            }
        }
    }

    public void updatePeerQOSTable(List<NetworkCapabilitiesQOS> list){
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        boolean tableExist = false;
        String ipAddress = null;
        for(NetworkCapabilitiesQOS qosData : list){
            ipAddress = qosData.getController();
        }
        LOG.info("QOS: Inside updatePeerQOSTable PeerCount: {}", qos_peer_count);
        try {

            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(QOS_DB_URL);
            stmt = conn.createStatement();
            LOG.trace("sql connection established");

            LOG.trace("QOS: ipAddress: {} findIpAddress(): {}", ipAddress, findIpAddress());
            String sql;
            //check for the self controller ip
            if (ipAddress != findIpAddress()) {
            //if not, get the peercount and loop it

                for(int i=1;i<=qos_peer_count;i++){
                    LOG.trace("QOS: Inside trp block in updatePeerTable");
                //check for the exist of peer controller 1 table
                    String tableName = "QOS_DATABASE_PEER_"+i;
                    sql = "SELECT controller FROM "+tableName +" LIMIT 1";
                    rs = stmt.executeQuery(sql);
                    long peerIP = 0L;
                    while (rs.next()) {
                        peerIP = rs.getLong("controller");
                    }
                    String peerIPAddress =ntoa(peerIP);
                    LOG.trace("QOS: Before if block, IPaddress: {} peerIPaddress: {}", ipAddress, peerIPAddress);
                    if (ipAddress.equals(peerIPAddress)) {

                        sql = "drop table QOS_DATABASE_PEER_"+i;
                        stmt.executeUpdate(sql);
                        sql = "create table QOS_DATABASE_PEER_"+i+" (controller varchar(20), node varchar(30), port varchar(30), receiveFrameError varchar(10) , receiveOverRunError varchar(10), receiveCrcError varchar(10), collisionCount varchar(10), receivePackets varchar(10), transmitPackets varchar(10));";
                        LOG.trace("QOS: SQL query to create QOS peer table: {}", sql);
                        stmt.executeUpdate(sql);

                        String insertQueries = formQOSInsertQuery(list, i);
                        String[] insertQuery = insertQueries.split("--");
                        for(int j = 0; j< insertQuery.length;j++){
                            //LOG.info("insertQuery: {}", insertQuery[j]);
                            String query = insertQuery[j].replace("LOCAL", "0");
                            stmt.executeUpdate(query);
                        }
                        tableExist = true;
                    }
                }

                if (!tableExist) {
                    qos_peer_count++;
                    LOG.info("QOS: now peerCount: {}", qos_peer_count);
                    //create a new table QOS_DATABASE_PEER + count
                    sql = "create table QOS_DATABASE_PEER_"+qos_peer_count+"(controller varchar(20), node varchar(30), port varchar(30), receiveFrameError varchar(10) , receiveOverRunError varchar(10), receiveCrcError varchar(10), collisionCount varchar(10), receivePackets varchar(10), transmitPackets varchar(10));";
                    stmt = conn.createStatement();
                    stmt.executeUpdate(sql);
                    String insertQueries = formQOSInsertQuery(list, qos_peer_count);
                    String[] insertQuery = insertQueries.split("--");
                    for(int i = 0; i< insertQuery.length-1;i++){
                        //LOG.info("insertQuery: {}", insertQuery[i]);
                        String query = insertQuery[i].replace("LOCAL", "0");
                        stmt.executeUpdate(query);
                    }

                    LOG.trace("QOS: Peer table creation for first time");
                }
            }
            LOG.trace("QOS: Peer count at the end of try in updatetable {}",qos_peer_count);

        } catch (SQLException se) {
            LOG.trace("SQLException: {0}", se);
            return;
        } catch (Exception e) {
            LOG.trace("Exception: {0}", e);
            return;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException se2) {
                LOG.trace("SQLException2: {0}", se2);
                return;
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException se) {
                LOG.trace("SQLException3: {0}", se);
                return;
            }
        }
    }

    public String formQOSInsertQuery(List<NetworkCapabilitiesQOS> list, int qos_peer_count){
        LOG.trace("QOS: Inside formQOSInsertQuery peercount {}", qos_peer_count);
        String insertQuery = "";
        int i=0;

        for (NetworkCapabilitiesQOS qosData : list) {
            String controller = qosData.getController();
            String node = qosData.getNode();
            String port= qosData.getPort();
            String receiveFrameError = qosData.getReceiveFrameError();
            String receiveOverRunError = qosData.getReceiveOverRunError();
            String receiveCrcError = qosData.getReceiveCrcError();
            String collisionCount = qosData.getCollisionCount();
            String receivePackets = qosData.getReceivePackets();
            String transmitPackets = qosData.getTransmitPackets();
            LOG.trace("QOS: Contents in list: Controller {} Node {} port {} ReceiveFrameError {} ReceiveOverRunError {} ReceiveCrcError {} CollisionCount {} ReceivePackets {} TransmitPackets {}", controller,node,port,receiveFrameError,receiveOverRunError,receiveCrcError,collisionCount,receivePackets,transmitPackets);
            long controllerIP = ipToLong(controller);

            if (node == null || port == null ) {
                if (qos_peer_count == 0) {
                    insertQuery +=  "insert into QOS_DATABASE (controller) values (" + controllerIP + "); -- ";
                } else {
                    insertQuery +=  "insert into QOS_DATABASE_PEER_"+qos_peer_count+" (controller) values( " + controllerIP + "); -- ";
                }
            }
            else {
                if (qos_peer_count == 0) {
                    insertQuery +=  "insert into QOS_DATABASE values (" + controllerIP + "," + node.substring(node.lastIndexOf(":")+1) + "," + port.substring(port.lastIndexOf(":")+1) + "," + receiveFrameError + "," + receiveOverRunError + "," +  receiveCrcError  + "," + collisionCount + "," + receivePackets + "," + transmitPackets + "); -- ";
                } else {
                    insertQuery +=  "insert into QOS_DATABASE_PEER_"+qos_peer_count + " values (" + controllerIP + "," + node.substring(node.lastIndexOf(":")+1) + "," + port.substring(port.lastIndexOf(":")+1) + "," + receiveFrameError + "," + receiveOverRunError + "," +  receiveCrcError  + "," + collisionCount + "," + receivePackets + "," + transmitPackets + "); -- ";
                }
            }
        }

        LOG.trace("QOS: At the end of formQOSInsertQuery() method insertQuery:{}",insertQuery);
        return insertQuery;
    }

    public static String ntoa(long raw) {
        byte[] b = new byte[] {(byte)(raw >> 24), (byte)(raw >> 16), (byte)(raw >> 8), (byte)raw};
        try {
            return InetAddress.getByAddress(b).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }
    public static long ipToLong(String ipAddress) {
        long result = 0;
        String[] atoms = ipAddress.split("\\.");

        for (int i = 3; i >= 0; i--) {
            result |= (Long.parseLong(atoms[3 - i]) << (i * 8));
        }

        return result & 0xFFFFFFFF;
    }


    public String callRestAPI(String url) {
        final String admin = "admin";
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(admin, admin.toCharArray());
            }
        });
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        WebResource service = client.resource(UriBuilder.fromUri(url).build());
        String data = service.accept(MediaType.APPLICATION_JSON).get(
                String.class);
        LOG.trace("Read sdni message from rest api: {}", data);
        return data;
    }
}
