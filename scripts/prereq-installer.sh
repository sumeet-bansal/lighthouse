#!/usr/bin/env bash
# assumes LTS build of Ubuntu

# updates JRE to compatible version (1.8+)
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer

# installs Maven
sudo apt install maven
