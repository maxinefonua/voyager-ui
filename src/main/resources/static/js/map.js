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

function updateLookupMap(resultButtonElem) {
    if (airportMarker) airportMarker.remove();
    if (lookupMarker) lookupMarker.remove();
    if (resultButtonElem && resultButtonElem.getAttribute("aria-expanded") == "true") {
        const bounds = resultButtonElem.dataset.bounds
            .split(',')
            .map(Number.parseFloat)
            .filter(n => !isNaN(n));
        map.fitBounds(bounds);
        lookupMarker = addLookupMarkerFromSearch(resultButtonElem);
    }
};

function getLocationElemByName(locationElementName) {
    const allLocations = document.getElementById('all-locations');
    if (allLocations && allLocations.options && allLocations.options.namedItem(locationElementName)) {
        return allLocations.options.namedItem(locationElementName);
    }
    return null;
}

function mapToLocationByElemName(locationElementName) {
    const allLocations = document.getElementById('all-locations');
    if (allLocations && allLocations.options && allLocations.options.namedItem(locationElementName)) {
        const match = allLocations.options.namedItem(locationElementName);
        fitMapToElementBounds(match);
    }
}

function resetTripMap(isStart) {
    if (isStart) {
        clearStartMarkers();
        recenterMap();
    } else {
        clearEndMarkers();
        recenterMap();
    }
}

function clearStartMarkers() {
    if (startMarker) {
        startMarker.remove();
        startMarker = null;
    }
    if (airportStartMarker) {
        airportStartMarker.remove();
        airportStartMarker = null;
    }
}

function clearEndMarkers() {
    if (endMarker) {
        endMarker.remove();
        endMarker = null;
    }
    if (airportEndMarker) {
        airportEndMarker.remove();
        airportEndMarker = null;
    }
}

function zoomToLocationMap(locationName,locationId,clickedButtonElem) {
    if (airportMarker) airportMarker.remove();
    if (lookupMarker) lookupMarker.remove();
    if (locationName && locationId && clickedButtonElem
            && clickedButtonElem.getAttribute("aria-expanded") == "true") {
        const elemName = locationName + '-' + locationId;
        const allLocations = document.getElementById('all-locations');
        if (allLocations && allLocations.options && allLocations.options.namedItem(elemName)) {
            const match = allLocations.options.namedItem(elemName);
            const bounds = match.dataset.bounds
                .split(',')
                .map(Number.parseFloat)
                .filter(n => !isNaN(n));
            map.fitBounds(bounds);
            lookupMarker = addLookupMarker(match,true);
        }
    }
};

function addTripLocationToMapNew(locationElementName,isStart) {
    if (isStart) {
        clearStartMarkers();
        const locationElem = getLocationElemByName(locationElementName);
        if (locationElem) {
            startMarker = addLookupMarker(locationElem,true);
            if (endMarker) mapFitBothMarkers(startMarker,endMarker);
            else fitMapToElementBounds(locationElem);
        }
    } else {
        clearEndMarkers();
        const locationElem = getLocationElemByName(locationElementName);
        if (locationElem) {
            endMarker = addLookupMarker(locationElem,true);
            if (startMarker) mapFitBothMarkers(startMarker,endMarker);
            else fitMapToElementBounds(locationElem);
        }
    }
};

function addTripLocationToMap(optionSelected,isStart) {
    const longitude = optionSelected.dataset.longitude;
    const latitude = optionSelected.dataset.latitude;
    if (isStart) {
        clearStartMarkers();
        startMarker = new mapboxgl.Marker().setLngLat([longitude,latitude]).addTo(map);

        if (endMarker) mapFitBothMarkers(startMarker,endMarker);
        else fitMapToElemWithBounds(optionSelected);

        const airportSelectElement = document.getElementById('airport-filter-select-start');
        airportSelectElement.dispatchEvent(new Event('change'));
    } else {
        clearEndMarkers();
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

function fitMapToElementBounds(elementWithBounds) {
    const bounds = elementWithBounds.dataset.bounds
                .split(',')
                .map(Number.parseFloat)
                .filter(n => !isNaN(n));
    map.fitBounds(bounds);
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

function mapToFirstLocation(fromSearch) {
    if (lookupMarker) lookupMarker.remove();
    if (airportMarker) airportMarker.remove();
    if (fromSearch) {
        document.getElementById('searchButton').innerHTML = 'Search';
        document.getElementById('searchButton').disabled = false;
        const firstElemButton = document.getElementById('searchResultButtonHeader-0');
        if (firstElemButton) updateLookupMap(firstElemButton);
    } else {
        const firstElemButton = document.getElementById('location-display-0');
        if (firstElemButton) {
            const locationName = firstElemButton.dataset.name;
            const locationId = firstElemButton.dataset.locationId;
            zoomToLocationMap(locationName,locationId,firstElemButton);
        }
    }
}

function resetMapAndOptionsPostFilter(selection) {
    clearStartMarkers();
    if (endMarker == null && airportEndMarker == null) recenterMap();
    if (selection == 'LOCATION') {
        selectLocationElem = document.getElementById('select-start-location-options');
        if (selectLocationElem) selectLocationElem.selectedIndex = 0;
    } else if (selection == 'AIRPORT') {
        airportInputElem = document.getElementById('select-start-airport-input');
        if (airportInputElem) airportInputElem.value = '';
    }
}