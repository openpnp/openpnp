#!/bin/bash

platform='unknown'
unamestr=`uname`
case "$unamestr" in
	Linux)
		platform='linux'
		rootdir="$(dirname $(readlink -f $0))"
	;;
	Darwin)
		platform='mac'
		rootdir="$(cd $(dirname $0); pwd -P)"
	;;
esac

case "$platform" in
	mac)
		java -Xdock:name=OpenPnP -jar $rootdir/target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
	;;
	linux)
		java -jar $rootdir/target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
	;;
esac
