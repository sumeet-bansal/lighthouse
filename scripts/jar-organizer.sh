#!/usr/bin/env bash

cd $1
ls *.jars
for file in `ls *.jars`
do
	fabric=`echo $file | cut -d'-' -f 3`
	if [ $fabric == "stm" ]; then
		true
		fabric="storm";
	elif [ $fabric == "haz" ]; then
		true
		fabric="hazelcast";
	fi
	node=`echo $file | cut -d'-' -f 4 | cut -d'.' -f 1`
	mkdir -p $fabric/$node &> /dev/null
	mv $file $fabric/$node
done
