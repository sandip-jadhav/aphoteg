xquery version "1.0-ml";

module namespace bing = 'http://www.bing.com/developers';

declare default function namespace "http://www.w3.org/2005/xpath-functions";

declare namespace utils = "java:photoassociation.qizx.UtilityFunctions";

declare variable $bing:errNS as xs:string := "http://www.bing.com/developers";
declare variable $bing:YS002 as xs:QName := fn:QName($bing:errNS, "err:YS002");
declare variable $bing:YS001 as xs:QName := fn:QName($bing:errNS, "err:YS001");

declare variable $bing:appid as xs:string:="C95FBF000810BE5EB21C73009C32882962C22E12";

declare function bing:_http-get ( $arg as xs:string ) {
 utils:httpGet($arg)
};

declare function bing:_http-post ( $arg as xs:string , $pars as xs:string* ) {
 utils:httpPost($arg,$pars)
};

declare function bing:query ( $query as xs:string ) {
 bing:query($query,10)
};

declare function bing:query ( $query as xs:string , $cnt as xs:integer ) {
 bing:_http-get(concat("http://api.bing.net/xml.aspx?AppId=",$bing:appid,"&amp;Sources=web+spell&amp;Web.Count=",$cnt,"&amp;Query=",fn:encode-for-uri($query)))
};

declare function bing:count-results ( $arg as xs:string ) as xs:integer {
 bing:query($arg,1)//*[name()="web:Total"]/text()
};

declare function bing:count-near-results ( $arg1 as xs:string , $arg2 as xs:string ) as xs:integer {
 bing:query(concat($arg1," near:",$arg2),1)//*[name()="web:Total"]/text()
};
