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
            closeTripMarkerPopups();
            startMarker = addLookupMarker(locationElem,true);
            if (endMarker) mapFitBothMarkers(startMarker,endMarker);
            else fitMapToElementBounds(locationElem);
        } else clearStartMarkers();
    } else {
        clearEndMarkers();
        const locationElem = getLocationElemByName(locationElementName);
        if (locationElem) {
            closeTripMarkerPopups();
            endMarker = addLookupMarker(locationElem,true);
            if (startMarker) mapFitBothMarkers(startMarker,endMarker);
            else fitMapToElementBounds(locationElem);
        } else clearStartMarkers();
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

function zoomToTripLocation(isStart,targetElemId) {
    event.preventDefault(); // prevents url mod
    closeTripMarkerPopups();
    if (isStart) {
        if (startMarker && airportStartMarker && nonDeltaStartMarker) {
            mapFitThreeMarkers(startMarker,airportStartMarker,nonDeltaStartMarker);
            startMarker.togglePopup();
        } else if (airportStartMarker && nonDeltaStartMarker) {
            mapFitBothMarkers(airportStartMarker,nonDeltaStartMarker);
            nonDeltaStartMarker.togglePopup();
        } else if (startMarker && airportStartMarker) {
            mapFitBothMarkers(airportStartMarker,startMarker);
            startMarker.togglePopup();
        } else if (nonDeltaStartMarker && airportStartMarker) {
            mapFitBothMarkers(nonDeltaStartMarker,startMarker);
            nonDeltaStartMarker.togglePopup();
        } else if (startMarker) {
            centerMapOnMarker(startMarker);
            startMarker.togglePopup();
        } else if (airportStartMarker) {
            centerMapOnMarker(airportStartMarker);
            airportStartMarker.togglePopup();
        } else if (nonDeltaStartMarker) {
            centerMapOnMarker(nonDeltaStartMarker);
            nonDeltaStartMarker.togglePopup();
        } else recenterMap();
    } else {
        if (endMarker && airportEndMarker && nonDeltaEndMarker) {
            mapFitThreeMarkers(endMarker,airportEndMarker,nonDeltaEndMarker);
            endMarker.togglePopup();
        } else if (airportEndMarker && nonDeltaEndMarker) {
            mapFitBothMarkers(airportEndMarker,nonDeltaEndMarker);
            nonDeltaEndMarker.togglePopup();
        } else if (endMarker && airportEndMarker) {
            mapFitBothMarkers(airportEndMarker,endMarker);
            endMarker.togglePopup();
        } else if (nonDeltaEndMarker && airportEndMarker) {
            mapFitBothMarkers(nonDeltaEndMarker,airportEndMarker);
            nonDeltaEndMarker.togglePopup();
        } else if (endMarker) {
            centerMapOnMarker(endMarker);
            endMarker.togglePopup();
        } else if (airportEndMarker) {
            centerMapOnMarker(airportEndMarker);
            airportEndMarker.togglePopup();
        } else if (nonDeltaEndMarker) {
            centerMapOnMarker(nonDeltaEndMarker);
            nonDeltaEndMarker.togglePopup();
        } else recenterMap();
    }
    scrollElemToTarget(targetElemId);
}

function scrollElemToTarget(targetElemId) {
    const target = document.querySelector(targetElemId);
    const contentContainer = document.getElementById('scrollable-content');

    if (target && contentContainer) {
        const offset = target.offsetTop - contentContainer.offsetTop;
        contentContainer.scrollTo({
            top: offset,
            behavior: 'smooth'
        });
    }
}

function mapFitToTrip(targetElemId) {
    event.preventDefault();
    closeTripMarkerPopups();
    var marker1 = null;
    var marker2 = null;

    if (startMarker) marker1 = startMarker;
    else if (nonDeltaStartMarker) marker1 = nonDeltaStartMarker;
    else if (airportStartMarker) marker1 = airportStartMarker;

    if (endMarker) marker2 = endMarker;
    else if (nonDeltaEndMarker) marker2 = nonDeltaEndMarker;
    else if (airportEndMarker) marker2 = airportEndMarker;

    if (marker1 && marker2) mapFitBothMarkers(marker1,marker2);
    else recenterMap();
    scrollElemToTarget(targetElemId);
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

function addSelectedLocationToMap(selectLocationElemId,isStart) {
    const selectLocationElem = document.getElementById(selectLocationElemId);
    if (selectLocationElem && selectLocationElem.options && selectLocationElem.options[0]
        && selectLocationElem.options[0].dataset.elementName) {
        addTripLocationToMapNew(selectLocationElem.options[0].dataset.elementName,isStart)
    }
}

function resetMapAndAirportInput(airportInputElemId,isStart) {
    const airportInputElem = document.getElementById(airportInputElemId);
    if (airportInputElem) airportInputElem.value = '';
    if (isStart) {
        if (airportStartMarker) airportStartMarker.remove();
        if (nonDeltaStartMarker) nonDeltaStartMarker.remove();
    } else {
        if (airportEndMarker) airportEndMarker.remove();
        if (nonDeltaEndMarker) nonDeltaEndMarker.remove();
    }
    recenterMap();
}

function locationSelectSwapped(selectLocationElem,isStart) {
    if (selectLocationElem && selectLocationElem.options && selectLocationElem.options[0]
        && selectLocationElem.options[0].dataset.elementName) {
        addTripLocationToMapNew(selectLocationElem.options[0].dataset.elementName,isStart);
        selectLocationElem.dispatchEvent(new Event('change'));
    }
}