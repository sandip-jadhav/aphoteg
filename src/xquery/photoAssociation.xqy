xquery version "1.1";

module namespace photoAssociation = 'http://web.tagus.ist.utl.pt/~rui.candeias/';

import module namespace flickr="http://www.flickr.com/services/api/" at "flickr.xqy";
import module namespace geoplanet="http://developer.yahoo.com/geo/" at "geoplanet.xqy";

declare default function namespace "http://www.w3.org/2005/xpath-functions";

declare namespace x = "http://www.w3.org/1999/xhtml";
declare namespace xmls = "http://wherein.yahooapis.com/v1/schema";
declare namespace math="java:java.lang.Math";
declare namespace utils = "java:photoassociation.qizx.UtilityFunctions";

(: ************************************************************************************************
parser - Função que permite remover o "lixo" do parágrafo
*************************************************************************************************** :)
declare function photoAssociation:parserString($text){
concat(tokenize(normalize-space(replace($text,"<.*>| - |[^a-zA-Z0-9\p{L}]", " ")), " +"), " ")
};

declare function photoAssociation:parserList($text){
tokenize(normalize-space(replace($text,"<.*>| - |[^a-zA-Z0-9\p{L}]", " ")), " ")
};


(: ************************************************************************************************
euclideanDistance - 
*************************************************************************************************** :)
declare function photoAssociation:euclideanDistance($long1 as xs:double, $lat1 as xs:double, $long2 as xs:double, $lat2 as xs:double)
{
math:sqrt(math:pow(($long1 - $long2), 2) + math:pow(($lat1 - $lat2), 2))
};


(: ************************************************************************************************
haversineDistance - 
*************************************************************************************************** :)
declare function photoAssociation:haversineDistance($long1 as xs:double, $lat1 as xs:double, $long2 as xs:double, $lat2 as xs:double)
{
let $raioTerra as xs:integer := 6371
let $varLongRad := math:toRadians($long1 - $long2)
let $varLatRad := math:toRadians($lat1 - $lat2)

let $a := math:pow(math:sin($varLatRad div 2), 2 ) + 
math:cos(math:toRadians($lat2))*
math:cos(math:toRadians($lat1))*
math:pow(math:sin($varLongRad div 2), 2) 

let $b := 2 * math:atan2(math:sqrt($a),math:sqrt(xs:double(1-$a)))

return $raioTerra * $b
};



(: ************************************************************************************************
temporalDistance - 
*************************************************************************************************** :)
declare function photoAssociation:temporalDistance($tripDateTime, $photoDateTime)
{

let $dateTrip := tokenize($tripDateTime, " ")
let $datePhoto := tokenize($photoDateTime," ")

let $tripDate := xs:date($dateTrip[1])
let $dateTaken := xs:date($datePhoto[1])

let $timeTaken := xs:time(tokenize($photoDateTime," ")[2])
return if (count($dateTrip) = 1)
then(
let $date1 := dateTime($tripDate,xs:time("00:00:00"))
let $date2 := dateTime($dateTaken,$timeTaken)
return abs(get-all-seconds($date1 - $date2))
)
else(
let $tripTime := xs:time($dateTrip[2])
let $date1 := dateTime($tripDate,$tripTime)
let $date2 := dateTime($dateTaken,$timeTaken)
return xs:double(abs(get-all-seconds($date1 - $date2)))
)
};


(: ************************************************************************************************
temporalDistance - 
*************************************************************************************************** :)
declare function photoAssociation:temporalDistanceTrimester($tripDateTime as node(), $photoDateTime as xs:string)
{
let $time3Meses := abs(get-all-seconds(dateTime(xs:date("2009-12-01"),xs:time("00:00:00")) - dateTime(xs:date("2009-09-01") , xs:time("00:00:00"))))

let $dateTrip := tokenize(data($tripDateTime), " ")
let $datePhoto := tokenize($photoDateTime," ")

let $tripDate := xs:date($dateTrip[1])
let $dateTaken := xs:date($datePhoto[1])

let $timeTaken := xs:time(tokenize($photoDateTime," ")[2])
return if (count($dateTrip) = 1)
then(
let $date1 := dateTime($tripDate,xs:time("00:00:00"))
let $date2 := dateTime($dateTaken,$timeTaken)
return abs(get-all-seconds($date1 - $date2))
)
else(
let $tripTime := xs:time($dateTrip[2])
let $date1 := dateTime($tripDate,$tripTime)
let $date2 := dateTime($dateTaken,$timeTaken)
return xs:double(abs(get-all-seconds($date1 - $date2)) div $time3Meses)
)
};

(: ************************************************************************************************
temporalDistance - 
*************************************************************************************************** :)
declare function photoAssociation:temporalDistanceSemester($tripDateTime as node(), $photoDateTime as xs:string)
{
let $time6Meses := abs(get-all-seconds(dateTime(xs:date("2009-12-01"),xs:time("00:00:00")) - dateTime(xs:date("2009-06-01") , xs:time("00:00:00"))))

let $dateTrip := tokenize(data($tripDateTime), " ")
let $datePhoto := tokenize($photoDateTime," ")

let $tripDate := xs:date($dateTrip[1])
let $dateTaken := xs:date($datePhoto[1])

let $timeTaken := xs:time(tokenize($photoDateTime," ")[2])
return if (count($dateTrip) = 1)
then(
let $date1 := dateTime($tripDate,xs:time("00:00:00"))
let $date2 := dateTime($dateTaken,$timeTaken)
return xs:double(abs(get-all-seconds($date1 - $date2)) div $time6Meses)
)
else(
let $tripTime := xs:time($dateTrip[2])
let $date1 := dateTime($tripDate,$tripTime)
let $date2 := dateTime($dateTaken,$timeTaken)
return xs:double(abs(get-all-seconds($date1 - $date2)) div $time6Meses)
)
};


(: ************************************************************************************************
temporalDistance - 
*************************************************************************************************** :)
declare function photoAssociation:roundedTemporalDistanceSemester($tripDateTime as node(), $photoDateTime as xs:string)
{
let $time6Meses := abs(get-all-seconds(dateTime(xs:date("2009-12-01"),xs:time("00:00:00")) - dateTime(xs:date("2009-06-01") , xs:time("00:00:00"))))

let $dateTrip := tokenize(data($tripDateTime), " ")
let $datePhoto := tokenize($photoDateTime," ")

let $tripDate := xs:date($dateTrip[1])
let $dateTaken := xs:date($datePhoto[1])

let $timeTaken := xs:time(tokenize($photoDateTime," ")[2])
return if (count($dateTrip) = 1)
then(
let $date1 := dateTime($tripDate,xs:time("00:00:00"))
let $date2 := dateTime($dateTaken,$timeTaken)
return round(xs:double(abs(get-all-seconds($date1 - $date2)) div $time6Meses))
)
else(
let $tripTime := xs:time($dateTrip[2])
let $date1 := dateTime($tripDate,$tripTime)
let $date2 := dateTime($dateTaken,$timeTaken)
return round(xs:double(abs(get-all-seconds($date1 - $date2)) div $time6Meses))
)
};

(: ************************************************************************************************
PLACEMAKER - O geocode_paragraph recebe o nomeDoFicheiro, o numero do paragrafo, e o texto do paragrafo
caso o paragrafo do ficheiro ainda não tenha sido geocodificado, é geocodificado usando o Placemaker 
*************************************************************************************************** :)


declare function photoAssociation:geocode_paragraph($nameFile as xs:string, $numP as xs:integer, $paragrafo as xs:string)
{
let $geo_par := for $place in geoplanet:placemaker($paragrafo)
					return <Placemaker>{$place}</Placemaker>
return $geo_par

};




(: ************************************************************************************************
FLICKR - Interface para a escolha do tipo de Selecção de fotos (woeid, coordenadas, texto, ou woeid & Coordenadas)
*************************************************************************************************** :)



(: ************************************************************************************************
photoCollector - Função que permite colectar fotos para um determinado local 
*************************************************************************************************** :)
declare function photoAssociation:photoCollector($place)
{
let $allPhotos := for $photo in flickr:photos.search((),(),(),(),(),$place//latitude,$place//longitude,32,(),(),(),"2000-01-01","2011-06-01",(),"interestingness-desc",(),(),(),(),(),(),"geo,tags,description,date_taken",25,1)//photo
		where exists($photo//description) and exists($photo/@title) and exists($photo/@tags)
 		and ((string-length(data($photo/@tags)) > 1) and (string-length(data($photo/@title)) > 1))
		and exists($photo/@latitude) and exists($photo/@longitude)
		and ($photo/@latitude != 0) and ($photo/@longitude != 0)
		return $photo

let $photos := for $ph in $allPhotos
		let $phInfo := flickr:photos.getInfo(string($ph/@id),())
		let $ph1 := element { xs:QName('photo')}
                  		{ attribute {xs:QName('numComments')} {$phInfo//comments/text()}, 
			$ph/@*,
                    		$ph/node() }

		let $ph2 := element { xs:QName('photo')}
                  		{ attribute {xs:QName('numFavorites')} {data(flickr:photos.getFavorites(string($ph/@id),(),())//@total)},
				$ph1/@*,
                    		$ph1/node() }

		let $ph3 := element { xs:QName('photo')}
                  		{ attribute {xs:QName('numViews')} {data($phInfo//@views)}, 
				$ph2/@*,
                    		$ph2/node() }

		let $ph4 := element { xs:QName('photo')}
                  		{ attribute {xs:QName('url')}{concat("http://www.flickr.com/photos/",$ph/@owner,"/",$ph/@id)}, 
				$ph3/@*,
                    		$ph3/node() }

		let $ph5 := element { xs:QName('photo')}
                  		{ attribute {xs:QName('photoLocation')}{$phInfo//locality/text()}, 
				$ph4/@*,
                    		$ph4/node() }

		return $ph5
return <result>{$photos}</result>
};

(: ************************************************************************************************
Selecção de fotos usando Woeid's
*************************************************************************************************** :)


(: ************************************************************************************************
Selecção de fotos usando Coordenadas
*************************************************************************************************** :)





(: ************************************************************************************************
photos_selector -  
*************************************************************************************************** :)
