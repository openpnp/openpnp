grep -ri stopship src
if [ $? -eq 0 ]; then
	exit 1
fi
javadoc -sourcepath src/main/java -subpackages org.openpnp -d doc/javadoc
scp -r doc/javadoc/* jason@vonnieda.org:openpnp.org/htdocs/doc/javadoc
DATE=`date +%F-%H-%M-%S`
FILENAME="OpenPnP-Snapshot-$DATE.zip"
rm -f log/*
zip -x *.git* -x *.svn* -r $FILENAME *
scp $FILENAME jason@vonnieda.org:openpnp.org/htdocs/downloads/snapshots
rm $FILENAME
echo "http://openpnp.org/downloads/snapshots/$FILENAME"

