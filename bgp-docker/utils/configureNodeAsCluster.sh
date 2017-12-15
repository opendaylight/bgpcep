for i in 1 2 3
do
   array[$i]=$( ./utils/findIp.sh node$i )
done

for i in 1 2 3
do
   echo "## Configure "node$i" as clustering"
   (
      echo "bin/configure_cluster.sh "$i" "${array[1]}" "${array[2]}" "${array[3]} 
   ) | docker exec -i node$i bash
done

for i in 1 2 3
do
   echo "## Configure "node$i" as clustering"
   (
      echo "sed -i 's/wrap/wrap, odl-restconf, odl-bgpcep-"$1"/' ./etc/org.apache.karaf.features.cfg" 
   ) | docker exec -i node$i bash
done

for i in 1 2 3
do
   echo "## start Karaf in node "node$i
   (
      echo "bin/start" 
   ) | docker exec -i node$i bash
done

