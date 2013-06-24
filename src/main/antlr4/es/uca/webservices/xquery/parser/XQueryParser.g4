parser grammar XQueryParser;
options {
  tokenVocab=XQueryLexer;
}

// Mostly taken from http://www.w3.org/TR/xquery/#id-grammar.
//
// Notes:
// 1. In order to keep the grammar simple, the parser itself doesn't really
//    enforce ws:explicit except for some easy cases (QNames and wildcards).
//    Walkers will need to do this (and also parse wildcards a bit).
//
// 2. Attribute constructors are tricky to parse all in one go. It is easier
//    to use a separate minilexer to extract the embedded expressions and
//    then use this parser on them.
//
// 3. A walker should also collect all the little tokens in the last production
//    of dirElemContent and join them together into a single ELEMENT_CONTENT
//    token.

// MODULE HEADER ///////////////////////////////////////////////////////////////

module: versionDecl? (libraryModule | mainModule) ;

versionDecl: 'xquery' 'version' version=StringLiteral
             ('encoding' encoding=StringLiteral)?
             ';' ;

mainModule: prolog expr;

libraryModule: moduleDecl prolog;

moduleDecl: 'module' 'namespace' prefix=NCName '=' uri=StringLiteral ';' ;

// MODULE PROLOG ///////////////////////////////////////////////////////////////

prolog: ((defaultNamespaceDecl | setter | namespaceDecl | schemaImport | moduleImport) ';')*
        ((varDecl | functionDecl | optionDecl) ';')* ;

defaultNamespaceDecl: 'declare' 'default'
                      type=('element' | 'function')
                      'namespace'
                      uri=StringLiteral;

setter: 'declare' 'boundary-space' type=('preserve' | 'strip')          # boundaryDecl
      | 'declare' 'default' 'collation' StringLiteral                   # defaultCollationDecl
      | 'declare' 'base-uri' StringLiteral                              # baseURIDecl
      | 'declare' 'construction' type=('strip' | 'preserve')            # constructionDecl
      | 'declare' 'ordering' type=('ordered' | 'unordered')             # orderingModeDecl
      | 'declare' 'default' 'order' 'empty' type=('greatest' | 'least') # emptyOrderDecl
      | 'declare' 'copy-namespaces'                                     
                  preserve=('preserve' | 'no-preserve')
                  ','
                  inherit=('inherit' | 'no-inherit')                    # copyNamespacesDecl
      ;

namespaceDecl: 'declare' 'namespace' prefix=NCName '=' uri=StringLiteral ;

schemaImport: 'import' 'schema'
              ('namespace' prefix=NCName '=' | 'default' 'element' 'namespace')?
              nsURI=StringLiteral
              ('at' locations+=StringLiteral (',' locations+=StringLiteral)*)? ;

moduleImport: 'import' 'module'
              ('namespace' prefix=NCName '=')?
              nsURI=StringLiteral
              ('at' locations+=StringLiteral (',' locations+=StringLiteral)*)? ;

varDecl: 'declare' 'variable' '$' name=qName type=typeDeclaration?
         (':=' value=exprSingle | 'external') ;

functionDecl: 'declare' 'function' name=qName '(' params+=param (',' params+=param) ')'
              ('as' type=sequenceType)?
              ('{' body=expr '}' | 'external') ;

optionDecl: 'declare' 'option' name=qName value=StringLiteral ;

param: '$' name=qName type=typeDeclaration? ;

// EXPRESSIONS /////////////////////////////////////////////////////////////////

expr: exprSingle (',' exprSingle)* ;

exprSingle: flworExpr | quantifiedExpr | typeswitchExpr | ifExpr | orExpr ;

flworExpr: (forClause | letClause)+
           ('where' whereExpr=exprSingle)?
           orderByClause?
           'return' returnExpr=exprSingle ;

forClause: 'for' vars+=forVar (',' vars+=forVar)* ;

forVar: '$' name=qName type=typeDeclaration? pvar=positionalVar?
        'in' in=exprSingle ;

positionalVar: 'at' '$' name=qName ;

letClause: 'let'  vars+=letVar (',' vars+=letVar)* ;

letVar: '$' name=qName type=typeDeclaration? ':=' value=exprSingle ;

orderByClause: 'stable'? 'order' 'by' specs+=orderSpec (',' specs+=orderSpec) ;

orderSpec: value=exprSingle
           order=('ascending' | 'descending')?
           ('empty' empty=('greatest'|'latest'))?
           ('collation' collation=StringLiteral)?
         ;

quantifiedExpr: quantifier=('some' | 'every') vars+=forVar (',' vars+=forVar)*
                'satisfies' value=exprSingle ;

typeswitchExpr: 'typeswitch' '(' switchExpr=expr ')'
                clauses=caseClause+
                'default' ('$' var=qName) returnExpr=exprSingle ;

caseClause: 'case' ('$' var=qName 'as')? type=sequenceType 'return'
            returnExpr=exprSingle ;

ifExpr: 'if' '(' conditionExpr=expr ')'
        'then' thenExpr=exprSingle
        'else' elseExpr=exprSingle ;

// Here we use a bit of ANTLR4's new capabilities to simplify the grammar
orExpr:
        ('-'|'+') orExpr                                   # unary
      | orExpr 'cast' 'as' singleType                      # cast
      | orExpr 'castable' 'as' sequenceType                # castable
      | orExpr 'treat' 'as' sequenceType                   # treat
      | orExpr 'instance' 'of' sequenceType                # instanceOf
      | orExpr op=('intersect' | 'except') orExpr          # intersect
      | orExpr (KW_UNION | '|') orExpr                     # union
      | orExpr op=('*' | 'div' | 'idiv' | 'mod') orExpr    # mult
      | orExpr op=('+' | '-') orExpr                       # add
      | orExpr 'to' orExpr                                 # range
      | orExpr (valueComp | generalComp | nodeComp) orExpr # comp
      | orExpr 'and' orExpr                                # and
      | orExpr 'or' orExpr                                 # or
      | 'validate' ('lax' | 'strict') '{' expr '}'         # validate
      | PRAGMA '{' expr? '}'                               # extension
      | '/' relativePathExpr?                              # rooted
      | '//' relativePathExpr                              # allDesc
      | relativePathExpr                                   # relative
      ;

valueComp: 'eq' | 'ne' | 'lt' | 'le' | 'gt' | 'ge' ;

generalComp: '=' | '!=' | '<' | '<=' | '>' | '>=' ;

nodeComp: 'is' | '<<' | '>>' ;

primaryExpr: IntegerLiteral # integer
           | DecimalLiteral # decimal
           | DoubleLiteral  # double
           | StringLiteral  # string
           | '$' qName      # var
           | '(' expr? ')'  # paren
           | '.'            # current
           | qName '(' (args+=exprSingle (',' args+=exprSingle)*)? ')' # funcall
           | 'ordered' '{' expr '}'   # ordered
           | 'unordered' '{' expr '}' # unordered
           | constructor              # ctor
           ;

// PATHS ///////////////////////////////////////////////////////////////////////

relativePathExpr: stepExpr (('/'|'//') stepExpr)* ;

stepExpr: axisStep | filterExpr ;

axisStep: (reverseStep | forwardStep) predicateList ;

forwardStep: forwardAxis nodeTest | abbrevForwardStep ;

forwardAxis: ( 'child'
             | 'descendant'
             | 'attribute'
             | 'self'
             | 'descendant-or-self'
             | 'following-sibling'
             | 'following' ) '::' ;

abbrevForwardStep: '@'? nodeTest ;

reverseStep: reverseAxis nodeTest | abbrevReverseStep ;

reverseAxis: ( 'parent'
             | 'ancestor'
             | 'preceding-sibling'
             | 'preceding'
             | 'ancestor-or-self' ) '::';

abbrevReverseStep: '..' ;

nodeTest: nameTest | kindTest ;

nameTest: qName          # exactMatch
        | '*'            # allNames
        | NCNameWithLocalWildcard  # allWithNS    // walkers must strip out the trailing :*
        | NCNameWithPrefixWildcard # allWithLocal // walkers must strip out the leading *:
        ;

filterExpr: primaryExpr predicateList ;

predicateList: ('[' predicates+=expr ']')*;

// CONSTRUCTORS ////////////////////////////////////////////////////////////////

constructor: directConstructor | computedConstructor ;

directConstructor: dirElemConstructor
                 | (COMMENT | PI)
                 ;

// [96]: we don't check that the closing tag is the same here: it should be
// done elsewhere, if we really want to know. We've also simplified the rule
// by removing the S? bits, which has to do with handling whitespace and is
// beyond our scope of a basic parser. Tree walkers could handle this quite
// well.
dirElemConstructor: '<'
                    qName dirAttributeList
                    ( '/' '>'
                    | '>' dirElemContent* '<' '/' qName '>')
                  ;

// [97]: again, ws:explicit is better handled through the walker. Parsing the
// value of the attribute all in one parser+lexer can be quite hard, so it
// might be better to use a miniparser to strip out the embedded expressions
// and then use the main parser again on them.
//
// A straight translation of dirAttributeValue ([98]) would be something like:
//
// dirAttributeValue:
//      '"'  ('""'   | QuotAttrContentChar | commonContent)* '"'
//    | '\'' ('\'\'' | AposAttrContentChar | commonContent)* '\''
//    ;
dirAttributeList: ((qName '=' StringLiteral)?)* ;

// This rule captures all the possible content that an element may have. Again,
// a walker could build a TokenStreamRewriter to join all the little tokens into
// a single virtual ELEMENT_CONTENT token.
dirElemContent: directConstructor
              | commonContent
              | ELEMENT_CONTENT
              | text=(CDATA
                     | IntegerLiteral
                     | DecimalLiteral
                     | DoubleLiteral
                     | StringLiteral
                     | EscapeQuot
                     | EscapeApos
                     | Quot
                     | Apos
                     | PRAGMA
                     | EQUAL
                     | NOT_EQUAL
                     // OPERATORS AND DELIMITERS
                     | LPAREN
                     | RPAREN
                     | LBRACKET
                     | RBRACKET
                     | LBRACE
                     | RBRACE
                     | STAR
                     | PLUS
                     | MINUS
                     | COMMA
                     | DOT
                     | DDOT
                     | COLON
                     | DCOLON
                     | COLON
                     | SEMICOLON
                     | SLASH
                     | DSLASH
                     | VBAR
                     | LANGLE
                     | DLANGLE
                     | RANGLE
                     | DRANGLE
                     | LANGLE
                     | RANGLE
                     | QUESTION
                     | AT
                     | DOLLAR
                     // KEYWORDS
                     | KW_ANCESTOR
                     | KW_ANCESTOR_OR_SELF
                     | KW_AND
                     | KW_AS
                     | KW_ASCENDING
                     | KW_AT
                     | KW_ATTRIBUTE
                     | KW_BASE_URI
                     | KW_BOUNDARY_SPACE
                     | KW_BY
                     | KW_CASE
                     | KW_CAST
                     | KW_CASTABLE
                     | KW_CHILD
                     | KW_COLLATION
                     | KW_COMMENT
                     | KW_CONSTRUCTION
                     | KW_COPY_NS
                     | KW_DECLARE
                     | KW_DEFAULT
                     | KW_DESCENDANT
                     | KW_DESCENDANT_OR_SELF
                     | KW_DESCENDING
                     | KW_DIV
                     | KW_DOCUMENT
                     | KW_DOCUMENT_NODE
                     | KW_ELEMENT
                     | KW_ELSE
                     | KW_EMPTY
                     | KW_EMPTY_SEQUENCE
                     | KW_ENCODING
                     | KW_EQ
                     | KW_EVERY
                     | KW_EXCEPT
                     | KW_EXTERNAL
                     | KW_FOLLOWING
                     | KW_FOLLOWING_SIBLING
                     | KW_FOR
                     | KW_FUNCTION
                     | KW_GE
                     | KW_GREATEST
                     | KW_GT
                     | KW_IDIV
                     | KW_IF
                     | KW_IMPORT
                     | KW_IN
                     | KW_INHERIT
                     | KW_INSTANCE
                     | KW_INTERSECT
                     | KW_IS
                     | KW_ITEM
                     | KW_LATEST
                     | KW_LAX
                     | KW_LE
                     | KW_LEAST
                     | KW_LET
                     | KW_LT
                     | KW_MOD
                     | KW_MODULE
                     | KW_NAMESPACE
                     | KW_NE
                     | KW_NO_INHERIT
                     | KW_NO_PRESERVE
                     | KW_NODE
                     | KW_OF
                     | KW_OPTION
                     | KW_OR
                     | KW_ORDER
                     | KW_ORDERED
                     | KW_ORDERING
                     | KW_PARENT
                     | KW_PRECEDING
                     | KW_PRECEDING_SIBLING
                     | KW_PRESERVE
                     | KW_PI
                     | KW_RETURN
                     | KW_SATISFIES
                     | KW_SCHEMA
                     | KW_SCHEMA_ATTR
                     | KW_SCHEMA_ELEM
                     | KW_SELF
                     | KW_SOME
                     | KW_STABLE
                     | KW_STRICT
                     | KW_STRIP
                     | KW_TEXT
                     | KW_THEN
                     | KW_TO
                     | KW_TREAT
                     | KW_TYPESWITCH
                     | KW_UNION
                     | KW_UNORDERED
                     | KW_VALIDATE
                     | KW_VARIABLE
                     | KW_VERSION
                     | KW_WHERE
                     | KW_XQUERY
                     // NAMES
                     | FullQName
                     | NCNameWithLocalWildcard
                     | NCNameWithPrefixWildcard
                     | NCName
                     | ElementContentChar
                     )+
              ;

commonContent: (PredefinedEntityRef | CharRef) | '{' '{' | '}' '}' | '{' expr '}' ;

computedConstructor: 'document' '{' expr '}'   # docConstructor
                   | 'element'
                     (elementName=qName | '{' elementExpr=expr '}')
                     '{' contentExpr=expr? '}' # elementConstructor
                   | 'attribute'
                     (attrName=qName | ('{' attrExpr=expr '}'))
                     '{' contentExpr=expr? '}' # attrConstructor
                   | 'text' '{' expr '}'       # textConstructor 
                   | 'comment' '{' expr '}'    # commentConstructor
                   | 'processing-instruction'
                     (piName=NCName | '{' piExpr=expr '}')
                     '{' contentExpr=expr? '}' # piConstructor
                   ;

// TYPES AND TYPE TESTS ////////////////////////////////////////////////////////

singleType: qName '?' ;

typeDeclaration: 'as' sequenceType ;

sequenceType: 'empty-sequence' '(' ')' | itemType occurrence=('?'|'*'|'+')? ;

itemType: kindTest | 'item' '(' ')' | qName ;

kindTest: documentTest | elementTest | attributeTest | schemaElementTest
        | schemaAttributeTest | piTest | commentTest | textTest
        | anyKindTest
        ;

documentTest: 'document-node' '(' (elementTest | schemaElementTest)? ')' ;

elementTest: 'element' '(' (
                (name=qName | wildcard='*')
                (',' type=qName optional='?'?)?
             )? ')' ;

attributeTest: 'attribute' '(' (
                (name=qName | wildcard='*')
                (',' type=qName)?
               )? ')' ;

schemaElementTest: 'schema-element' '(' qName ')' ;

schemaAttributeTest: 'schema-attribute' '(' qName ')' ;

piTest: 'processing-instruction' '(' (NCName | StringLiteral)? ')' ;

commentTest: 'comment' '(' ')' ;

textTest: 'text' '(' ')' ;

anyKindTest: 'node' '(' ')' ;

// QNAMES //////////////////////////////////////////////////////////////////////

qName: full=FullQName   // walkers need to split into prefix+localpart by the ':'
       | local=(NCName
       | KW_ANCESTOR
       | KW_AND
       | KW_AS
       | KW_ASCENDING
       | KW_AT
       | KW_ATTRIBUTE
       | KW_BY
       | KW_CASE
       | KW_CAST
       | KW_CASTABLE
       | KW_CHILD
       | KW_COLLATION
       | KW_COMMENT
       | KW_CONSTRUCTION
       | KW_DECLARE
       | KW_DEFAULT
       | KW_DESCENDANT
       | KW_DESCENDING
       | KW_DIV
       | KW_DOCUMENT
       | KW_ELEMENT
       | KW_ELSE
       | KW_EMPTY
       | KW_ENCODING
       | KW_EQ
       | KW_EVERY
       | KW_EXCEPT
       | KW_EXTERNAL
       | KW_FOLLOWING
       | KW_FOR
       | KW_FUNCTION
       | KW_GE
       | KW_GREATEST
       | KW_GT
       | KW_IDIV
       | KW_IF
       | KW_IMPORT
       | KW_IN
       | KW_INHERIT
       | KW_INSTANCE
       | KW_INTERSECT
       | KW_IS
       | KW_ITEM
       | KW_LATEST
       | KW_LAX
       | KW_LE
       | KW_LEAST
       | KW_LET
       | KW_LT
       | KW_MOD
       | KW_MODULE
       | KW_NAMESPACE
       | KW_NE
       | KW_NODE
       | KW_OF
       | KW_OPTION
       | KW_OR
       | KW_ORDER
       | KW_ORDERED
       | KW_ORDERING
       | KW_PARENT
       | KW_PRECEDING
       | KW_PRESERVE
       | KW_RETURN
       | KW_SATISFIES
       | KW_SCHEMA
       | KW_SELF
       | KW_SOME
       | KW_STABLE
       | KW_STRICT
       | KW_STRIP
       | KW_TEXT
       | KW_THEN
       | KW_TO
       | KW_TREAT
       | KW_TYPESWITCH
       | KW_UNION
       | KW_UNORDERED
       | KW_VALIDATE
       | KW_VARIABLE
       | KW_VERSION
       | KW_WHERE
       | KW_XQUERY)
       ;
