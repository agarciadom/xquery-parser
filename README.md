xquery-parser
=============

This project implements an [ANTLR4](http://www.antlr.org/)-based [XQuery 1.0](http://www.w3.org/TR/xquery/) parser that strives to comply with the requirements of the [W3C XQuery Test Suite 1.0](http://dev.w3.org/2006/xquery-test-suite/PublicPagesStagingArea/). Namely, it should accept all the XQuery modules that XQTS deems valid, and reject all the XQuery modules that should produce parsing errors. Runtime errors are not covered by the parser, though some extra-grammatical constraints may be honored (such as ws:explicit or xgc:lone-leading-slash).

The parser does not use *any* semantic predicates or actions that may clutter the grammar or tie it into a specific target language. Therefore, this parser should also work for any other target language in ANTLR4, but I haven't had time to test it.

The parser is currently limited to XQuery 1.0, and does not support XQuery 1.1, XQuery 3.0, XQuery with Full Text or any other extensions that may exist. Contributions are welcome, though!

How to build
------------

Install Maven 3 and wget and run these commands from a Bash shell:

    ./bootstrap.sh
    mvn test

The first command will take longer the first time you run it, as it needs to download a ~30MB file with the contents of the W3C XQuery 1.0 Test Suite and then unpack it. Later invocations will reuse the downloaded file.

The second command runs the test. It may take a while, though, as the XQTS has over 16000 different examples :-).

License
-------

Copyright (c) 2013 Antonio García-Domínguez

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
