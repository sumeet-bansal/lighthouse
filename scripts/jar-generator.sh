#!/usr/bin/env bash

ROOT=/
JARS=()

NAME=`uname -n`

if [[ $NAME == *"conf"* ]]; then
		ROOT=/apps/zookeeper-3.4.6
		JARS+=(lib)
elif [[ $NAME == *"stm"* ]]; then
		ROOT=/apps/storm-1.0.2
		JARS+=(lib)
elif [[ $NAME == *"haz"* ]]; then
		ROOT=/apps/alcatraz_cache-1.0
		JARS+=(lib)
		JARS+=(apclib)
elif [[ $NAME == *"karaf"* ]]; then
		ROOT=/apps/karaf
		JARS+=(lib)
fi

function generateJARS {

	cd $ROOT/$1
	
	curr=0
	file=${NAME}.$1.jars

	rm $file &> /dev/null
	touch ~/$file
	for property in `ls -l *.jar | awk '{print $9, $5}'`
	do
		if [ `expr $(($curr)) % 2` -eq 0 ]; then
			echo -e -n "$property=" >> ~/$file
			curr=1
		else
			echo -e "$property" >> ~/$file
			curr=0
		fi
	done
}

for jarloc in ${JARS[@]}
do
	generateJARS $jarloc
done
