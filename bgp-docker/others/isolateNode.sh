sudo iptables -I INPUT -i $1 -j DROP
sudo iptables -I OUTPUT -o $1 -j DROP
