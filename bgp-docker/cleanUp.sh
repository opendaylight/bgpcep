for i in 1 2 3
do
   docker stop node$i
done
for i in 1 2 3
do
   docker rm node$i
done
./utils/removeNetwork.sh &
