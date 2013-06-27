#!/bin/bash

XQTS_URL="http://dev.w3.org/2006/xquery-test-suite/PublicPagesStagingArea/XQTS_1_0_3.zip"

# Grab the W3C test suite and place the catalog and
#  XPath queries into src/test/resource/xqts
rm -rf src/test/resources/xqts
mkdir -p src/test/resources/xqts

wget -q -c -O xqts.zip "$XQTS_URL"
mkdir -p xqts
pushd xqts
unzip -q ../xqts.zip
mv *.xml cat Queries/XQuery/* ../src/test/resources/xqts
popd
rm -rf xqts
