#!/usr/bin/env bash
# assumes LTS build of Ubuntu

# updates JRE to compatible version (1.8+)
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer

# installs and runs MongoDB
release=`lsb_release -a 2> /dev/null`
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 0C49F3730359A14518585931BC711F9BA15703C6
if [ $(echo $release | grep "12.04" | wc -l) -gt 0 ]; then
	echo "deb [ arch=amd64,arm64 ] http://repo.mongodb.org/apt/ubuntu xenial/mongodb-org/3.4 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.4.listo "deb [ arch=amd64 ] http://repo.mongodb.org/apt/ubuntu precise/mongodb-org/3.4 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.4.list
elif [ $(echo $release | grep "14.04" | wc -l) -gt 0 ]; then
	echo "deb [ arch=amd64 ] http://repo.mongodb.org/apt/ubuntu trusty/mongodb-org/3.4 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.4.list
elif [ $(echo $release | grep "16.04" | wc -l) -gt 0 ]; then
	echo "deb [ arch=amd64,arm64 ] http://repo.mongodb.org/apt/ubuntu xenial/mongodb-org/3.4 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.4.list
fi
sudo apt-get update
sudo apt-get install -y mongodb-org
sudo service mongod start

# installs Maven
sudo apt install maven
