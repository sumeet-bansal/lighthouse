#!/usr/bin/env bash

ROOT=/
JARS=

NAME=`uname -n`

if [[ $NAME == *"stm"* ]]; then
	ROOT=/apps/storm-1.0.2
	JAR=lib
fi

cd $ROOT/$JAR

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