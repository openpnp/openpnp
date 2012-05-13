#!/bin/bash

platform='unknown'
unamestr=`uname`
case "$unamestr" in
	Linux)
		platform='linux'
	;;
	Darwin)
		platform='mac'
	;;
esac

case "$platform" in
	mac)
		java -d32 -Djava.library.path=lib/native/mac-universal -jar target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
	;;
	linux)
		java -jar target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
	;;
esac
