xquery version "1.0-ml";

module namespace yahoo = 'http://developer.yahoo.com/geo/';

declare default function namespace "http://www.w3.org/2005/xpath-functions";

declare namespace utils = "java:photoassociation.qizx.UtilityFunctions";

declare variable $yahoo:errNS as xs:string := "http://developer.yahoo.com/geo/";
declare variable $yahoo:YS002 as xs:QName := fn:QName($yahoo:errNS, "err:YS002");
declare variable $yahoo:YS001 as xs:QName := fn:QName($yahoo:errNS, "err:YS001");

declare variable $yahoo:appid as xs:string:="UycaMUTV34EdWHmbojYbqY1Pq888ZRlP3tePPp.1G0G2g2LhkW9PU7HWnY.JJh7L.XA-";

declare function yahoo:_http-get ( $arg as xs:string ) {
 utils:httpGet($arg)
};

declare function yahoo:_http-post ( $arg as xs:string , $pars as xs:string* ) {
 utils:httpPost($arg,$pars)
};

declare function yahoo:normalization($params as element()+, $divide as xs:string, $option as xs:string?,$comma as xs:string) as xs:string
{
  fn:string-join(
          for $param in $params//param
          let $k := $param/@key/data(.)
          let $v := $param/@value/data(.)
          return concat(fn:encode-for-uri($k), $divide,encode-for-uri($v), $option),$comma)
};

declare function yahoo:woeid-xml($location as xs:string)
{
  let $x := fn:replace($location, " ", "%20")
  return yahoo:_http-get(concat("http://where.yahooapis.com/v1/places.q(&apos;",$x,"&apos;)?appid=[",$yahoo:appid,"]"))
};

declare function yahoo:woeid($location as xs:string) as xs:string?
{
  let $result := yahoo:woeid-xml($location)//*:woeid
  return
    if(empty($result))
    then error($yahoo:YS002, concat("Location not found: ", $location))
    else $result/data(.)
};

declare function yahoo:longitude-latitude($location as xs:string) as xs:string?
{
  let $centroid := yahoo:woeid-xml($location)//*:centroid
  let $longitude := $centroid/*:longitude/data(.)
  let $latitude := $centroid/*:latitude/data(.)
  return 
    if(empty($centroid))
    then error($yahoo:YS002, concat("Location not found: ", $location))
    else concat($longitude, ", ", $latitude)
};

declare function yahoo:location-xml($location as xs:string, $flags as xs:string?)
{
  let $response := yahoo:_http-get(concat("http://where.yahooapis.com/geocode?location=",fn:encode-for-uri($location),"&amp;flags=",$flags))
  return
    if($response//*:Found/data(.) eq "0")
    then error($yahoo:YS002, concat("Location not found: ", $location))
    else $response
};

declare function yahoo:location-xml($params as element()+)
{
  let $parameters := yahoo:normalization($params,'=',"&amp;","")
  let $href := concat("http://where.yahooapis.com/geocode?",$parameters)
  return yahoo:_http-get($href)
};

declare function yahoo:geocode-xml($woeid as xs:string, $flags as xs:string?)
{
  let $response := yahoo:_http-get(concat("http://where.yahooapis.com/geocode?woeid=",fn:encode-for-uri($woeid),"&amp;flags=",$flags))
  return
    if($response//*:Found/data(.) eq "0")
    then error($yahoo:YS002, concat("Location not found for woeid: ", $woeid))
    else $response
};

declare function yahoo:placemaker($input as xs:string) {
  yahoo:_http-post("http://wherein.yahooapis.com/v1/document", 
                  ( "appid", $yahoo:appid, "documentType" ,"text/plain" , "documentContent" ,$input ) )
};