xquery version "1.1";

module namespace photoAssociation = 'http://web.tagus.ist.utl.pt/~rui.candeias/';

import module namespace flickr="http://www.flickr.com/services/api/" at "file:///c:/qizx/bin/flickr.xqy";
import module namespace geoplanet="http://developer.yahoo.com/geo/" at "file:///c:/qizx/bin/geoplanet.xqy";

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

*************************************************************************************************** :)
declare function photoAssociation:save_to_library($nameFile as xs:string, $paragrafos as node())
{
for $p at $pos in $paragrafos//paragrafo
return
try {
xlib:write-document( concat("/",$nameFile,"-paragrafo-",$pos),  $p ), 
xlib:commit()
} catch($err) { element error { $err }}
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

declare function photoAssociation:geocode_paragraph($nameFile as xs:string, $paragraph as xs:string)
{
let $paragrafo := $paragraph
return 
try {
	collection(concat("/",$nameFile,"-Placemaker"))
}
catch($err){if (contains($err,"no such collection")) 
			then (
				let $geo_par := for $place in geoplanet:placemaker($paragrafo)
					return <Placemaker>{$place}</Placemaker>
				let $commit := (
				try{
				xlib:write-document( concat("/",$nameFile,"-Placemaker"), $geo_par ), 
				xlib:commit()
				}
				catch($error){element error { $err }})
				return $geo_par
			)
			else ()
}
};

declare function photoAssociation:geocodeFocused_paragraph($nameFile as xs:string, $paragraph as xs:string, $params as xs:string?)
{
let $paragrafo := $paragraph
return 
try {
	collection(concat("/",$nameFile,"-Placemaker"))
}
catch($err){if (contains($err,"no such collection")) 
			then (
				let $geo_par := for $place in geoplanet:placemakerFocused($paragrafo, $params)
					return <Placemaker>{$place}</Placemaker>
				let $commit := (
				try{
				xlib:write-document( concat("/",$nameFile,"-Placemaker"), $geo_par ), 
				xlib:commit()
				}
				catch($error){element error { $err }})
				return $geo_par
			)
			else ()
}
};

(:
declare function photoAssociation:geocode_paragraph($nameFile as xs:string, $numP as xs:integer, $paragrafo as xs:string)
{
let $geo_par := for $place in geoplanet:placemaker($paragrafo)
					return <Placemaker>{$place}</Placemaker>
return $geo_par

};
:)




(: ************************************************************************************************
FLICKR - Interface para a escolha do tipo de Selecção de fotos (woeid, coordenadas, texto, ou woeid & Coordenadas)
*************************************************************************************************** :)
declare function photoAssociation:photos_from_geocode($type as xs:string, $nameFile as xs:string, $numP as xs:integer,$placemaker as node())
{
let $a := $placemaker
return
switch ($type)
case "woeid" return photoAssociation:photos_from_woeid($type, $nameFile, $numP, $placemaker)
case "coordinates" return photoAssociation:photos_from_coordinates($type, $nameFile, $numP, $placemaker)
case "texto" return photoAssociation:photos_from_texto($type, $nameFile, $numP, $placemaker)
case "woeidCoordinates" return photoAssociation:photos_from_woeidCoordinates($placemaker)
default return "()"
};




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

declare function photoAssociation:photos_from_woeid($type as xs:string, $nameFile as xs:string ,$numP as xs:integer, $placemaker as node())
{
let $a := "type"
return 
try {
	collection(concat("/",$nameFile,"-paragrafo-",$numP,"-woeid-Flickr"))
}
catch($err){if (contains($err,"no such collection")) 
			then (
				let $flickrResult := <Flickr>
								{
								for $place in $placemaker//place
								let $woeid := $place/woeId/text()
								return 
									<photos place="{$place/name/text()}" woeid="{$woeid}">
									{
								(:$user_id,$tags,$tag_mode,$text,$woeid,$lat,$lon,$radius,$in_gallery,$min_upload,$max_upload,$min_taken,$max_take,
								$license,$sort,$privacy_filter,$bbox,$accuracy,$machine_tags,$machine_tag_mode,$group_id,$extras,$per_page,$page:)
									for $photo in flickr:photos.search((),(),(),(),$woeid,(),(),(),true(),(),(),(),(),(),(),(),(),(),(),(),(),"geo,tags,description,date_taken",(),())//photo
									where exists($photo/@woeid) and exists($photo/@latitude) and exists($photo/@longitude)
										and ($photo/@latitude != 0) and ($photo/@longitude != 0)
									return 	$photo
									}
									</photos>
								}
								</Flickr>
				let $commit := (
				try{
				xlib:write-document( concat("/",$nameFile,"-paragrafo-",$numP,"-woeid-Flickr"), $flickrResult ), 
				xlib:commit()
				}
				catch($error){element error { $err }}
				)
				return $flickrResult
			)
			else ()
}
};

(:
declare function photoAssociation:photos_from_woeid($type as xs:string, $nameFile as xs:string ,$numP as xs:integer, $placemaker as node())
{
let $a := "type"
let $flickrResult := <Flickr>
					{
					for $place in $placemaker//place
					let $woeid := $place/woeId/text()
					return 
						<photos place="{$place/name/text()}" woeid="{$woeid}">
						{
					(:$user_id,$tags,$tag_mode,$text,$woeid,$lat,$lon,$radius,$in_gallery,$min_upload,$max_upload,$min_taken,$max_take,
					$license,$sort,$privacy_filter,$bbox,$accuracy,$machine_tags,$machine_tag_mode,$group_id,$extras,$per_page,$page:)
						for $photo in flickr:photos.search((),(),(),(),$woeid,(),(),(),true(),(),(),(),(),(),(),(),(),(),(),(),(),"geo,tags,description,date_taken",(),())//photo
						where exists($photo/@woeid) and exists($photo/@latitude) and exists($photo/@longitude)
							and ($photo/@latitude != 0) and ($photo/@longitude != 0)
						return 	$photo
						}
						</photos>
					}
					</Flickr>
return $flickrResult
};
:)

(: ************************************************************************************************
Selecção de fotos usando Coordenadas
*************************************************************************************************** :)
declare function photoAssociation:photos_from_coordinates($type as xs:string, $nameFile as xs:string , $numP as xs:integer, $placemaker as node())
{
let $hash := utils:generateHash($placemaker,"MD5")
return 
try {
	collection(concat("/",$nameFile,"-paragrafo-",$numP,"-coordinates-Flickr"))
}
catch($err){if (contains($err,"no such collection")) 
			then (
				let $flickrResult := <Flickr>
					{
					for $place in $placemaker//place
					let $lat := $place//latitude/text()
					let $long := $place//longitude/text()
					let $raio := 10
					return 
						<photos place="{$place/name/text()}" lat="{$lat}" long="{$long}" raio="{$raio}">
						{
					(:$user_id,$tags,$tag_mode,$text,$woeid,$lat,$lon,$radius,$in_gallery,$min_upload,$max_upload,$min_taken,$max_take,
					$license,$sort,$privacy_filter,$bbox,$accuracy,$machine_tags,$machine_tag_mode,$group_id,$extras,$per_page,$page:)
						for $photo in flickr:photos.search((),(),(),(),(),$lat,$long,$raio,true(),(),(),(),(),(),(),(),(),(),(),(),(),"geo,tags,description,date_taken",(),())//photo
						where exists($photo/@woeid) and exists($photo/@latitude) and exists($photo/@longitude)
							and ($photo/@latitude != 0) and ($photo/@longitude != 0)
						return 	$photo
						}
						</photos>
					}
					</Flickr>
				let $commit := (
				try{
				xlib:write-document( concat("/",$nameFile,"-paragrafo-",$numP,"-coordinates-Flickr"), $flickrResult ), 
				xlib:commit()
				}
				catch($error){element error { $err }}
				)
				return $flickrResult
			)
			else ()
}
};

(: ************************************************************************************************
Selecção de fotos usando Texto
*************************************************************************************************** :)
declare function photoAssociation:photos_from_texto($type as xs:string, $nameFile as xs:string ,$numP as xs:integer, $placemaker as node())
{
let $hash := utils:generateHash($placemaker,"MD5")
return 
try {
	collection(concat("/",$nameFile,"-paragrafo-",$numP,"-text-Flickr"))
}
catch($err){if (contains($err,"no such collection")) 
			
			then (
				let $flickrResult :=	<Flickr> 
								{
								for $place in $placemaker//place
								let $name := tokenize($place/name/text(),',')[1]
								return 
									<photos place="{$place/name/text()}">
									{
								(:$user_id,$tags,$tag_mode,$text,$woeid,$lat,$lon,$radius,$in_gallery,$min_upload,$max_upload,$min_taken,$max_take,
								$license,$sort,$privacy_filter,$bbox,$accuracy,$machine_tags,$machine_tag_mode,$group_id,$extras,$per_page,$page:)
									for $photo in flickr:photos.search((),(),(),$name,(),(),(),(),true(),(),(),(),(),(),(),(),(),(),(),(),(),"geo,tags,description,date_taken",(),())//photo
									return 	$photo
									}
									</photos>
								}
								</Flickr>			
				let $commit := (
				try{
				xlib:write-document( concat("/",$nameFile,"-paragrafo-",$numP,"-text-Flickr"), $flickrResult ),  
				xlib:commit()
				}
				catch($error){element error { $err }}
				)
				return $flickrResult
			)
			else ()
}
};




(: ************************************************************************************************
photos_selector -  
*************************************************************************************************** :)

declare function photoAssociation:photos_selector($textualParagraph, $date, $flickrInfo as node(), $placemakerInfo as node(), $vocabulary)
{
	<fotos>{
		let $cleanText := utils:removeStopWords($textualParagraph)

		for $photo in $flickrInfo//photo
		let $cleanTags := utils:removeStopWords(data($photo//@tags))
		let $cleanTitle := utils:removeStopWords(data($photo//@title))
		
		let $boundingBox := $placemakerInfo//extents

		let $photoQuery := string-join(($cleanTitle,$cleanTags,$cleanTags), " ")
		
		let $textSim := 	<x>{let $vect := for $term in tokenize($photoQuery," ")
						let $tf := utils:getTF($cleanText,$term)
						return <all tf="{$tf}"/>
				return <res>
				<tf>{sum($vect//@tf)}</tf>
				<cleanText>{$cleanText}</cleanText><photoQuery>{$photoQuery}</photoQuery>
				<simTFIDF>{utils:cossineSimilarity($vocabulary, $cleanText ,$photoQuery)}</simTFIDF></res>
			}
			</x>

		return	<foto>
			<fotoInfo>{$photo/@*}</fotoInfo>
			<fotoID>{string($photo/@id)}</fotoID>
~			<fotoURL>{concat("http://farm",$photo/@farm,".static.flickr.com/",$photo/@server,"/",$photo/@id,"_",$photo/@secret,"_m.jpg")}</fotoURL>
			<fotoCoords>
				<placeId>{string($photo//@place_id)}</placeId>
				<woeid>{string($photo//@woeid)}</woeid>
				<lat>{string($photo//@latitude)}</lat> 
				<long>{string($photo//@longitude)}</long>		
				<local>{string($photo//@photoLocation)}</local>
			</fotoCoords>
			<features>
			<textualSimilarity>
				<numWordsPhotoDesc>{let $photoDesc := string-join(($cleanTitle,$cleanTags), " ") return count(tokenize($photoDesc," "))}</numWordsPhotoDesc>
				<numWordsDoc>{count(tokenize($cleanText, " "))}</numWordsDoc>
				<textSim>{$textSim}</textSim>
			</textualSimilarity>
			<geospatialProximity>
				{
				let $photoLat := xs:double($photo//@latitude)
				let $photoLong := xs:double($photo//@longitude)
				let $distances := for $place in $placemakerInfo//place
						let $placeLat := xs:double($place//latitude/text())
						let $placeLong := xs:double($place//longitude/text())
						let $val := utils:getVincentysDistance($placeLat, $placeLong, $photoLat, $photoLong)
						order by $val
						return <distance toPlace="{$place/name}">{$val}</distance>
				let $min := $distances[position() = 1]
				return <x><Min toPlace="{$min/@toPlace}">{$min}</Min>
				<Mean>{sum($distances) div count($distances)}</Mean>{$distances}</x>
				}
			</geospatialProximity>
			<temporalProximity datePhoto="{data($photo//@datetaken)}" dateTrip="{$date}">{
				photoAssociation:roundedTemporalDistanceSemester($date,data($photo//@datetaken))
			}</temporalProximity>
			<photoInterestingness>{
				let $bbNordesteLat := xs:double($boundingBox//northEast//latitude)
				let $bbNordesteLong := xs:double($boundingBox//northEast//longitude)
				let $bbSudoesteLat := xs:double($boundingBox//southWest//latitude)
				let $bbSudoesteLong := xs:double($boundingBox//southWest//longitude)

				let $photoLat := xs:double(string($photo//@latitude))
				let $photoLong := xs:double(string($photo//@longitude))

				return if(($bbSudoesteLong < $photoLong) and ($bbNordesteLong > $photoLong) 
					and ($bbSudoesteLat < $photoLat) and ($bbNordesteLat > $photoLat))
					then(let $numFavorites := string($photo//@numFavorites)
					let $numComments := string($photo//@numComments)
					let $numViews := string($photo//@numViews)
					return <t type="{"inside"}"><numComments>{$numComments}</numComments>
						<numFavorite>{$numFavorites}</numFavorite>
						<numViews>{$numViews}</numViews></t>
					)
				else( <t type="{"outside"}"><numComments>{-1}</numComments>
				<numFavorite>{-1}</numFavorite>
				<numViews>{-1}</numViews></t>)
			}</photoInterestingness>
			</features>
			</foto>
		}
	</fotos>
};