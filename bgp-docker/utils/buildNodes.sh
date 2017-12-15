echo "## Build nodes"
for i in 1 2 3
do
   docker run --cap-add=NET_ADMIN --cap-add=NET_RAW --rm -d --name node$i -it bgp-docker 
   ./utils/findIp.sh node$i
done
