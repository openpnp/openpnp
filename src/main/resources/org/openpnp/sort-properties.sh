#!/bin/bash
cp translations.properties translations.properties.bak \
&& LC_ALL=C sort -t$'=' -k1,1 translations.properties > translations.properties.new \
&& mv translations.properties.new translations.properties
