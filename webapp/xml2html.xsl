<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
								xmlns:xmls="http://wherein.yahooapis.com/v1/schema">
	<xsl:template match="/">
		<html>
			<head>
			<script src="http://openlayers.org/api/OpenLayers.js"></script>
			<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=ABQIAAAAa4sh-0NOoAXTCNy7UZzEIBQ0iDZq0h3flG5ZV1k_bg2YcsZEDRSVFQBX2tyTm929hEyRmnEr5aHzYQ"></script>

			<script type="text/javascript" >
				var centerLat = 0.00;
				var centerLon = 2.00;
				var initialZoomLevel = 2;
				var size = new OpenLayers.Size(20,27);
				var offset = new OpenLayers.Pixel(-(size.w/2), -size.h);

				
				function createMap(){
					var map = new OpenLayers.Map('map');
					var gphy = new OpenLayers.Layer.Google(
						"Google Physical",
						{type: G_PHYSICAL_MAP}
					);
					var gmap = new OpenLayers.Layer.Google(
						"Google Streets");
			
					var ghyb = new OpenLayers.Layer.Google(
						"Google Hybrid",
						{type: G_HYBRID_MAP}
					);
					var gsat = new OpenLayers.Layer.Google(
						"Google Satellite",
						{type: G_SATELLITE_MAP}
					);

					map.addLayer(gphy);
					map.addLayer(gmap);
					map.addLayer(ghyb);
					map.addLayer(gsat);
					
					var markers = new OpenLayers.Layer.Markers("Markers");
					map.addLayer(markers);
					
					var imageMarkers = new OpenLayers.Layer.Markers("ImageMarkers");
					map.addLayer(imageMarkers);
					
					map.setCenter(new OpenLayers.LonLat(centerLon, centerLat), initialZoomLevel);
					map.addControl(new OpenLayers.Control.LayerSwitcher());
					map.addControl( new OpenLayers.Control.MouseDefaults());
					return map;
				}
				
			</script>
			</head>
			<body>
				<table border="1" >
					<xsl:for-each select="//Paragrafo">
						<tr>
							<th align="center">Paragrafo</th>
							<th align="center">Placemaker</th>
							<th align="center">Score</th>
							<th align="center">Imagens</th>
						</tr>
						<tr>
							<td>
								<xsl:apply-templates select="./paragrafo"/>
								<br/>
								<br/>								
								<xsl:element name="div" >
									<xsl:attribute name="id">map<xsl:value-of select="./id"/></xsl:attribute>
									<xsl:attribute name="style">width: 700px; height: 500px;</xsl:attribute>
								</xsl:element>
								<script  type="text/javascript">
								var m = createMap();
								</script>
								<xsl:for-each select=".//place">
									<xsl:variable name="lat" select="./centroid//latitude/text()" />
									<xsl:variable name="lon" select="./centroid//longitude/text()" />
									<script   type="text/javaScript">
									
									var icon = new OpenLayers.Icon('http://www.openlayers.org/api/img/marker.png',size,offset);
									var popup;
									var currentPopup;
									
									var localMarkers = m.layers[4];
									var lat =  <xsl:value-of select="$lat"/>;
									var long = <xsl:value-of select="$lon"/>;
									
									var longLat = new OpenLayers.LonLat(long,lat);
									var feature = new OpenLayers.Feature(localMarkers, longLat); 
									
									feature.id = 'f_'+long+ '_'+lat;
									feature.popupClass = OpenLayers.Class(OpenLayers.Popup.Anchored, {'autoSize': true});
									feature.data.popupContentHTML = "<xsl:value-of select="./name/text()"/>";
									feature.closeBox = false;
									feature.data.overflow =  "auto";
									
									var marker = feature.createMarker();
									
									var markerClick = function(evt) {
										if (this.popup == null) {
											this.popup = this.createPopup(true);
											this.marker.map.addPopup(this.popup);
											this.popup.show();
										} else {
											this.popup.toggle();
									}
									
									currentPopup = this.popup;
									OpenLayers.Event.stop(evt);
									};
									
									marker.events.register("mousedown", feature,markerClick);
									
									localMarkers.addMarker(marker);
									
								</script>
							</xsl:for-each>
							<xsl:for-each select="./Fotos/foto">
								<xsl:variable name="Imglat" select=".//fotoCoords/lat/text()" />
								<xsl:variable name="Imglon" select=".//fotoCoords/long/text()" />
								<xsl:variable name="image" select=".//fotoURL"/>
								<script   type="text/javaScript">
									var imageIcon = new OpenLayers.Icon('http://chart.apis.google.com/chart?chst=d_map_pin_letter&amp;chld=F|00FFFF|000000',size,offset);
									var popup;
									var currentPopup;
									var imageMarkers = m.layers[5];
									var Ilat = <xsl:value-of select="$Imglat"/>;
									var Ilong = <xsl:value-of select="$Imglon"/>;
																		
									var IlongLat = new OpenLayers.LonLat(Ilong,Ilat);
									var feature = new OpenLayers.Feature(imageMarkers, IlongLat); 
									
									feature.popupClass = OpenLayers.Class(OpenLayers.Popup.Anchored, {'autoSize': true});
									feature.data.popupContentHTML = '<xsl:element name="img" ><xsl:attribute name="src"><xsl:value-of select="./fotoURL"/></xsl:attribute></xsl:element>';
									feature.closeBox = false;
									feature.data.overflow = "auto";
									feature.data.icon = imageIcon.clone();
									
									var marker = feature.createMarker();
									
									var markerClick = function(evt) {
										if (this.popup == null) {
											this.popup = this.createPopup(true);
											this.marker.map.addPopup(this.popup);
											this.popup.show();
										} else {
											this.popup.toggle();
										}
									currentPopup = this.popup;
									OpenLayers.Event.stop(evt);
									};
									
									marker.events.register("mousedown", feature,markerClick);
									
									imageMarkers.addMarker(marker);
								</script>
							</xsl:for-each>
							</td>
							
							<td>
								<xsl:for-each select=".//Placemaker//place">
									<xsl:value-of select=".//name/text()"/>
									<br/>
									<br/>
								</xsl:for-each>
							</td>
							<td>
								<xsl:for-each select="./Fotos/foto">
									<xsl:value-of select=".//@simTFIDF"/> <br/>
									<xsl:value-of select=".//@geograficalMin"/> <br/>
									<xsl:value-of select=".//@temporal"/> <br/>
									<br/>
									<br/>
									<br/>
								</xsl:for-each>
							</td>
							<td>
								<xsl:for-each select="./Fotos/foto">
									<xsl:variable name="link" select=".//fotoURL" />
									<img src="{$link}"/>
									<br/>
									<xsl:value-of select=".//fotoCoords//local/text()"/>
									<br/>
								</xsl:for-each>
							</td>
						</tr>
					</xsl:for-each>
				</table>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="//B">
		<b><xsl:value-of select="." /></b>
	</xsl:template>
</xsl:stylesheet>
