import module namespace flickr="http://www.flickr.com/services/api/" at "flickr.xqy";
import module namespace geoplanet="http://developer.yahoo.com/geo/" at "geoplanet.xqy";
import module namespace photoAssociation="http://web.tagus.ist.utl.pt/~rui.candeias/" at "photoAssociation.xqy";

declare default function namespace "http://www.w3.org/2005/xpath-functions";

declare namespace x = "http://www.w3.org/1999/xhtml";
declare namespace xmls = "http://wherein.yahooapis.com/v1/schema";
declare namespace math="java:java.lang.Math";
declare namespace utils = "java:photoassociation.qizx.UtilityFunctions";

declare variable $text external;

declare variable $date external;

declare function local:text_for_tests($place)
{
let $allPhotos := for $photo in flickr:photos.search((),(),(),(),(),$place//latitude,$place//longitude,32,(),(),(),"2000-01-01","2011-06-01",(),"interestingness-desc",(),(),(),(),(),(),"geo,tags,description,date_taken",50,1)//photo
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

declare function local:photos_selector_Testes($textualParagraph, $date, $flickrInfo as node(), $placemakerInfo as node(), $vocabulary)
{
	<fotos>{
		let $cleanText := utils:removeStopWords($textualParagraph)

		for $photo in $flickrInfo//photo
		let $cleanTags := utils:removeStopWords(data($photo//@tags))
		let $cleanTitle := utils:removeStopWords(data($photo//@title))
		
		let $boundingBox := 	$placemakerInfo//extents

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
			<fotoURL>{concat("http://farm",$photo/@farm,".static.flickr.com/",$photo/@server,"/",$photo/@id,"_",$photo/@secret,"_m.jpg")}</fotoURL>
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

let $placemakerInfo := geoplanet:placemaker($text)

let $flickrInfo := <Fotos> { let $allPhotos := for $local in $placemakerInfo//place
				return local:text_for_tests($local)
			let $distPhotos := for $p in $allPhotos//photo 
				let $id := data($p//@id) group by $id return $p[1]
		return $distPhotos
		}</Fotos>		

let $allCollection := for $photo in $flickrInfo//photo
		let $desc := $photo//description
		let $ti := $photo//@title
		let $ta := $photo//@tags
		return <x>{ string-join(($desc,$ti,$ta)," ")}</x>
let $vocabulary := utils:createVoculary($allCollection)

let $featuredPhotos := <Fotos>{ 
		for $photo in local:photos_selector_Testes($text, $date, $flickrInfo, $placemakerInfo, $vocabulary)//foto
		return  <foto id="{$photo//fotoID}"  
		numWordsPhotoDesc="{$photo//numWordsPhotoDesc}"
		numWordsDoc="{$photo//numWordsDoc}"
		tf="{$photo//tf}"
		simTFIDF="{$photo//simTFIDF}"
		geograficalMin="{$photo//geospatialProximity//Min}" 
		geograficalMean="{$photo//geospatialProximity//Mean}" 
		temporal="{$photo//temporalProximity}"
		numComments="{if($photo//numComments= "") then 0 else $photo//numComments}" 
		numFavorite="{if($photo//numFavorite= "") then 0 else $photo//numFavorite}"
		numViews="{if($photo//numViews = "") then 0 else $photo//numViews}">
		{$photo/*}
		</foto>
	}
	</Fotos>


let $featuresToGet := "tf simTFIDF geograficalMin geograficalMean temporal numComments numFavorite numViews"
let $votingProtocol := "combSUM"

let $fotos := <ScoredPhotos>{for $f in utils:voting($featuredPhotos,$featuresToGet, $votingProtocol)//foto
		let $pt := $f/xs:double(@pontuacaoTotal)
		order by $pt descending
		return $f 
		}
		</ScoredPhotos>


return	<Paragrafo features="{$featuresToGet}" protocol="{$votingProtocol}">
	<paragrafo>
	{
	$text
	}
	</paragrafo>
	<Placemaker>
	{
	$placemakerInfo//place
	}
	</Placemaker>
	<Fotos>
	{
	for $x in 1 to min((5,count($fotos//foto)))
	let $photo := $fotos//foto[$x]
	return  <foto>{$photo/@*, $photo/*}<fotoLoc>{data($photo//local)}</fotoLoc></foto>
	}
	</Fotos>
	</Paragrafo>