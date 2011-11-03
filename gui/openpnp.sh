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
		java -d32 -cp lib/lti-civil.jar:lib/jVFW.jar:lib/slf4j-api-1.6.1.jar:lib/sfl4j-simple-1.6.1.jar:lib/RXTXcomm.jar:bin -Djava.library.path=lib/native/mac-universal org.openpnp.app.Main
	;;
	linux)
		echo "Not yet supported."
	;;
esac
