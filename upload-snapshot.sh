echo "Checking for STOPSHIP in source."
grep -ri stopship src
if [ $? -eq 0 ]; then
	echo "Files found with STOPSHIP. Aborting release."
	exit 1
fi
echo "Checking for files without a license header."
grep -Lr "OpenPnP is free software: you can redistribute it and/or modify" --include='*.java' .
if [ $? -eq 0 ]; then
	echo "Files missing license. Aborting release."
	exit 1
fi
rm -rf doc/javadoc
javadoc -sourcepath src/main/java -subpackages org.openpnp -d doc/javadoc
scp -r doc/javadoc/* jason@vonnieda.org:openpnp.org/htdocs/doc/javadoc
DATE=`date +%F-%H-%M-%S`
FILENAME="OpenPnP-Snapshot-$DATE.zip"
rm -f log/*
zip -x *.git* -x *.svn* -x installers/* -r $FILENAME *
scp $FILENAME jason@vonnieda.org:openpnp.org/htdocs/downloads/snapshots
rm $FILENAME
echo "http://openpnp.org/downloads/snapshots/$FILENAME"

