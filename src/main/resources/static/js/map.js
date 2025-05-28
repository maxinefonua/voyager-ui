// TODO: implement loading data into the DOM https://docs.mapbox.com/help/tutorials/custom-markers-gl-js/?step=4

var map = new mapboxgl.Map({
    container: 'map',
    style: {
        version: 8,
            sources: {
                osm: {
                    type: 'raster',
                    tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
                    tileSize: 256
                }
            },
            layers: [{
                id: 'osm',
                type: 'raster',
                source: 'osm',
            }],
        },
    projection: 'globe',
    attributionControl: false
}).addControl(new mapboxgl.AttributionControl({
    customAttribution: [
        '&copy; <a href="https://www.mapbox.com/about/maps">Mapbox</a>',
        '&copy; <a href="https://osm.org/copyright" target=”_blank" rel="noopener noreferrer nofollow">OpenStreetMap</a>'],
    compact: false
}));

const nav = new mapboxgl.NavigationControl({
  showCompass: false,
  visualizePitch: false
});

map.addControl(nav);

function updateLookupMap(iterIndex,longitude,latitude,bounds) {
    if (airportMarker) airportMarker.remove();
    if (lookupMarker) lookupMarker.remove();
    const clickedButton = document.getElementById("searchResultButtonHeader-"+iterIndex);
    if (clickedButton && clickedButton.getAttribute("aria-expanded") == "true") {
        map.fitBounds(bounds);
        lookupMarker = new mapboxgl.Marker().setLngLat([longitude,latitude]).addTo(map);
    }
};

function addTripLocationToMap(optionSelected,isStart) {
    const longitude = optionSelected.dataset.longitude;
    const latitude = optionSelected.dataset.latitude;
    if (isStart) {
        if (startMarker) startMarker.remove();
        if (airportStartMarker) {
            airportStartMarker.remove();
            airportStartMarker = null;
        }
        startMarker = new mapboxgl.Marker().setLngLat([longitude,latitude]).addTo(map);

        if (endMarker) mapFitBothMarkers(startMarker,endMarker);
        else fitMapToElemWithBounds(optionSelected);

        const airportSelectElement = document.getElementById('airport-filter-select-start');
        airportSelectElement.dispatchEvent(new Event('change'));
    } else {
        if (endMarker) endMarker.remove();
        if (airportEndMarker) {
            airportEndMarker.remove();
            airportEndMarker = null;
        }
        endMarker = new mapboxgl.Marker().setLngLat([longitude,latitude]).addTo(map);
        if (startMarker) mapFitBothMarkers(startMarker,endMarker);
        else fitMapToElemWithBounds(optionSelected);

        const airportSelectElement = document.getElementById('airport-filter-select-end');
        airportSelectElement.dispatchEvent(new Event('change'));
    }
};

function mapFitToTrip() {
    if (startMarker && endMarker) mapFitBothMarkers(startMarker,endMarker);
    else if (airportStartMarker && airportEndMarker) mapFitBothMarkers(airportStartMarker,airportEndMarker);
    else recenterMap();
}

function fitMapToElemWithBounds(elem) {
    const bounds = [elem.dataset.boundsWest,elem.dataset.boundsSouth,elem.dataset.boundsEast,elem.dataset.boundsNorth];
    map.fitBounds(bounds);
}

function zoomToTripLocation(isStart) {
    const startSelect = document.getElementById('select-start-location');
    const endSelect = document.getElementById('select-end-location');
    if (isStart && startSelect && startSelect.selectedIndex != 0) {
        fitMapToElemWithBounds(startSelect[startSelect.selectedIndex]);
    } else if (!isStart && endSelect && endSelect.selectedIndex !=0) {
        fitMapToElemWithBounds(endSelect[endSelect.selectedIndex]);
    } else if (isStart && airportStartMarker) {
        centerMapOnMarker(airportStartMarker);
    } else if (!isStart && airportEndMarker) {
        centerMapOnMarker(airportEndMarker);
    }
}

function recenterMap() {
    map.flyTo({
        center: [0, 0],
        zoom: 0
    });
}

function centerMapOnMarker(marker) {
    map.flyTo({
        center: marker.getLngLat(),
        zoom: 9
    });
}

function resetMapOnSearch() {
    recenterMap();
    document.getElementById('searchButton').disabled = true;
    document.getElementById('searchButton').innerHTML = 'Searching';
}

function mapToFirst(elementId,fromSearch) {
    if (lookupMarker) lookupMarker.remove();
    if (airportMarker) airportMarker.remove();
    const firstElemButton = document.getElementById(elementId);
    if (firstElemButton) {
        const longitude = firstElemButton.dataset.lng;
        const latitude = firstElemButton.dataset.lat;
        const bounds = [firstElemButton.dataset.boundsWest,firstElemButton.dataset.boundsSouth,firstElemButton.dataset.boundsEast,firstElemButton.dataset.boundsNorth]
        if (fromSearch) updateLookupMap(0,longitude,latitude,bounds);
        else {
            const locationName = firstElemButton.dataset.name;
            const locationId = firstElemButton.dataset.locationId;
            zoomToLocationMap(locationName,locationId,firstElemButton);
        }
    }
}

function setMapToFirstSearch() {
    mapToFirst("searchResultButtonHeader-0",true);
    document.getElementById('searchButton').innerHTML = 'Search';
    document.getElementById('searchButton').disabled = false;
}