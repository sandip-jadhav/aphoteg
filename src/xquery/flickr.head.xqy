xquery version "1.0-ml";

module namespace flickr="http://www.flickr.com/services/api/";

declare default function namespace "http://www.w3.org/2005/xpath-functions";

declare namespace utils = "java:photoassociation.qizx.UtilityFunctions";

declare variable $flickr:key    := "bf5e51500f8f17b9bd8fded4fb8c526d";
declare variable $flickr:secret := "96400c714f738028";
declare variable $flickr:token  := "YOUR AUTH_TOKEN";

declare function flickr:_md5 ( $arg as xs:string ) {
 utils:generateHash($arg,"MD5")
};

declare function flickr:_http-get ( $arg as xs:string ) {
 utils:httpGet($arg)
};

declare function flickr:_flickr(
    $method as element(flickr:method))
{
  flickr:_flickr-pages($method, ())
};

declare function flickr:_flickr-pages(
    $method as element(flickr:method),
    $result as element()?)
{
  let $args := for $arg in ($method/flickr:arg,
                            <flickr:arg name="api_key">{$flickr:key}</flickr:arg>,
                            if ($method/@auth="true")
                            then
                              <flickr:arg name="auth_token">{$flickr:token}</flickr:arg>
                            else
                              (),
                            <flickr:arg name="method">{string($method/@name)}</flickr:arg>)
               order by $arg/@name ascending
               return $arg
  let $sign := string-join(($flickr:secret, for $arg in $args return concat($arg/@name,$arg)), "")
  let $md5  := flickr:_md5($sign)
  let $uri  := concat("http://api.flickr.com/services/rest/?method=", $method/@name,
                      "&amp;api_key=", $flickr:key, "&amp;",
                      string-join(for $arg in $method/flickr:arg
                                  return concat($arg/@name, "=", string($arg)), "&amp;"),
                      if ($method/@auth="true")
                      then concat("&amp;auth_token=", $flickr:token)
                      else "",
                      "&amp;api_sig=", $md5)
  let $rsp  := flickr:_http-get($uri)
  return
    if ($rsp[2]/*/@stat = "ok")
    then
      let $body  := $rsp[2]/*/*
      let $page  := if ($body/@page and $body/@page castable as xs:integer)
                    then xs:integer($body/@page)
                    else 0
      let $pages := if ($body/@pages and $body/@pages castable as xs:integer)
                    then xs:integer($body/@pages)
                    else 0
      let $new   := if (empty($result))
                    then $body
                    else element { node-name($result) } { $body/@*, $result/*, $body/* }
      return
        if ($page < $pages and (not($method/flickr:arg[@name="page"]) or not(empty($result))))
        then
          let $newmethod := element { node-name($method) }
                                    { $method/@*, $method/*[not(@name='page')],
                                      <flickr:arg name="page">{$page + 1 }</flickr:arg>
                                    }
          return
            flickr:_flickr-pages($newmethod, $new)
        else
          flickr:_fixns($new)
    else
      <flickr:error>{$rsp}</flickr:error>
};

declare function flickr:_fixns($nodes as node()*) as node()* {
  for $x in $nodes
  return
    typeswitch ($x)
      case element()
        return
          if (namespace-uri($x) = "")
          then
            element { QName("http://www.flickr.com/services/api/", local-name($x)) }
                    { $x/@*, flickr:_fixns($x/node()) }
          else
            element { node-name($x) }
                    { $x/@*, flickr:_fixns($x/node()) }
      default
        return $x
};

(: ====================================================================== :)

