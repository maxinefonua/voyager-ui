// TODO: implement loading data into the DOM https://docs.mapbox.com/help/tutorials/custom-markers-gl-js/?step=4

var lookupMarker = null;
var airportMarker = null;
var startMarker = null;
var airportStartMarker = null;
var nonDeltaStartMarker = null;
var endMarker = null;
var airportEndMarker = null;
var nonDeltaEndMarker = null;

function setAirportPlaceHolder(iterIndex) {
    if (airportMarker) airportMarker.remove();
    const airportInput = document.getElementById("airport-code-"+iterIndex);
    const airportList = document.getElementById("filtered-list-"+iterIndex);
    if (airportInput && airportList && airportList.options && airportList.options[0]) {
        airportInput.placeholder = airportList.options[0].value;
        airportInput.value = '';
    }
}

function mapFitBothMarkers(marker1,marker2) {
    const container = map.getContainer();
    const prevTabIndex = container.getAttribute('tabindex');
    container.setAttribute('tabindex', '-1');
    if (marker1 == null) centerMapOnMarker(marker2);
    else if (marker2 == null) centerMapOnMarker(marker1);
    else {
        const lngLat1 = marker1.getLngLat();
        const lngLat2 = marker2.getLngLat();
        const south = lngLat1.lat < lngLat2.lat ? lngLat1.lat : lngLat2.lat;
        const north = lngLat1.lat > lngLat2.lat ? lngLat1.lat : lngLat2.lat;
        const west = lngLat1.lng < lngLat2.lng ? lngLat1.lng : lngLat2.lng;
        const east = lngLat1.lng > lngLat2.lng ? lngLat1.lng : lngLat2.lng;
        const bounds = [west,south,east,north];
        map.fitBounds(bounds, {padding: {top: 60, bottom: 30, left: 80, right: 80}});
    }
    container.removeAttribute('tabindex');

}

function mapFitThreeMarkers(marker1,marker2,marker3) {
    const lngLat1 = marker1.getLngLat();
    const lngLat2 = marker2.getLngLat();
    const lngLat3 = marker3.getLngLat();

    var south = lngLat1.lat < lngLat2.lat ? lngLat1.lat : lngLat2.lat;
    if (lngLat3.lat < south) south = lngLat3.lat;

    var north = lngLat1.lat > lngLat2.lat ? lngLat1.lat : lngLat2.lat;
    if (lngLat3.lat > north) north = lngLat3.lat;

    var west = lngLat1.lng < lngLat2.lng ? lngLat1.lng : lngLat2.lng;
    if (lngLat3.lng < west) west = lngLat3.lng;

    var east = lngLat1.lng > lngLat2.lng ? lngLat1.lng : lngLat2.lng;
    if (lngLat3.lng > east) east = lngLat3.lng;

    const bounds = [west,south,east,north];
    map.fitBounds(bounds, {padding: {top: 60, bottom: 30, left: 80, right: 80}});
}

function addAirportToMap(airportInputElem,isStart) {
    if (airportInputElem && airportInputElem.value && regexIata.test(airportInputElem.value)) {
        const allAirports = document.getElementById('all-airports');
        const deltaAirports = document.getElementById('delta-airports');
        if (allAirports && allAirports.options && allAirports.options.namedItem(airportInputElem.value.toUpperCase())) {
            airportInputElem.classList.remove('is-invalid');
            const airportMatch = allAirports.options.namedItem(airportInputElem.value.toUpperCase());
            const deltaMatch = deltaAirports.options.namedItem(airportInputElem.value.toUpperCase());
            const isDelta = (deltaMatch != null);
            if (isStart) {
                if (isDelta) {
                    if (airportStartMarker) airportStartMarker.remove();
                    airportStartMarker = addAirportMarker(airportMatch,isDelta);
                    if (startMarker) mapFitBothMarkers(startMarker,airportStartMarker);
                    else if (nonDeltaStartMarker) mapFitBothMarkers(nonDeltaStartMarker,airportStartMarker);
                    else centerMapOnMarker(airportStartMarker);
                } else {
                    if (nonDeltaStartMarker) nonDeltaStartMarker.remove();
                    nonDeltaStartMarker = addAirportMarker(airportMatch,isDelta);
                    if (startMarker) mapFitBothMarkers(startMarker,nonDeltaStartMarker);
                    else if (airportStartMarker) mapFitBothMarkers(nonDeltaStartMarker,airportStartMarker);
                    else centerMapOnMarker(nonDeltaStartMarker);
                }
            } else {
                if (isDelta) {
                    if (airportEndMarker) airportEndMarker.remove();
                    airportEndMarker = addAirportMarker(airportMatch,isDelta);
                    if (endMarker) mapFitBothMarkers(endMarker,airportEndMarker);
                    else if (nonDeltaEndMarker) mapFitBothMarkers(nonDeltaEndMarker,airportEndMarker);
                    else centerMapOnMarker(airportEndMarker);
                } else {
                    if (nonDeltaEndMarker) nonDeltaEndMarker.remove();
                    nonDeltaEndMarker = addAirportMarker(airportMatch,isDelta);
                    if (endMarker) mapFitBothMarkers(endMarker,nonDeltaEndMarker);
                    else if (airportEndMarker) mapFitBothMarkers(nonDeltaEndMarker,airportEndMarker);
                    else centerMapOnMarker(nonDeltaEndMarker);
                }
            }
        }
    } else {
        airportInputElem.classList.add('is-invalid');
        if (isStart && airportStartMarker) {
            airportStartMarker.remove();
            airportStartMarker = null;
            nonDeltaStartMarker.remove();
            nonDeltaStartMarker = null;
        } else if (!isStart && airportEndMarker) {
            airportEndMarker.remove();
            airportEndMarker = null;
            nonDeltaEndMarker.remove();
            nonDeltaEndMarker = null;
        }
    }
};

function addAirportMarker(airportOptionElem,isDelta) {
    var airportPopup =  new mapboxgl.Popup({ offset: 16 }) // add popups
              .setHTML(`<strong data-name='${airportOptionElem.value}'>${airportOptionElem.value}</strong> <i>${airportOptionElem.dataset.airport}</i>
                in ${airportOptionElem.dataset.city}, ${airportOptionElem.dataset.subdivision}
                 of ${airportOptionElem.dataset.country}`);

    const el = document.createElement('div');
    if (isDelta) {
        el.className = 'airport-marker';
    }
    else {
        el.innerHTML = `<svg display="block" height="41" width="27" viewBox="0 0 27 41" xmlns="http://www.w3.org/2000/svg">
                             <!-- Shadow effect -->
                             <g transform="translate(3.0, 29.0)" fill="#000000">
                                 <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="10.5" ry="5.25"/>
                                 <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="9.5" ry="4.77"/>
                                 <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="8.5" ry="4.3"/>
                                 <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="7.5" ry="3.82"/>
                                 <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="6.5" ry="3.34"/>
                                 <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="5.5" ry="2.86"/>
                                 <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="4.5" ry="2.39"/>
                             </g>
                             <!-- Main marker body -->
                             <path fill="#3FB1CE" d="M27,13.5C27,19.07 20.25,27 14.75,34.5C14.02,35.5 12.98,35.5 12.25,34.5C6.75,27 0,19.22 0,13.5C0,6.04 6.04,0 13.5,0C20.96,0 27,6.04 27,13.5Z"/>
                             <!-- Marker border -->
                             <path opacity="0.25" fill="#000000" d="M13.5,0C6.04,0 0,6.04 0,13.5C0,19.22 6.75,27 12.25,34.5C13,35.52 14.02,35.5 14.75,34.5C20.25,27 27,19.07 27,13.5C27,6.04 20.96,0 13.5,0ZM13.5,1C20.42,1 26,6.58 26,13.5C26,15.9 24.5,19.18 22.22,22.74C19.95,26.3 16.71,30.14 13.94,33.91C13.74,34.18 13.61,34.32 13.5,34.44C13.39,34.32 13.26,34.18 13.06,33.91C10.28,30.13 7.41,26.31 5.02,22.77C2.62,19.23 1,15.95 1,13.5C1,6.58 6.58,1 13.5,1Z"/>
                             <!-- Center dot -->
                             <g transform="translate(5.5, 5.6) scale(1.5)">
                                 <circle fill="#FFFFFF" cx="5.5" cy="5.5" r="5.5"/>
                             </g>
                             <!-- plane dot -->
                             <path fill="#3FB1CE"
                                   transform="scale(0.15) translate(93,35) rotate(45)"
                                   d="m34.228 12.148c1.368-15.768 10.112-16.112 11.312 0v16.428l34.36 17.136v5.78l-34.368-6.156v17.672l11.596 11.224v5.772l-17.12-5.708-17.12 5.732v-5.756l11.424-11.424v-17.512L.004 51.408v-5.692l34.228-17.048V12.176" />
                         </svg>`;
    }
    var airportMarker = new mapboxgl.Marker(el)
        .setLngLat([airportOptionElem.dataset.longitude,airportOptionElem.dataset.latitude])
        .setPopup(airportPopup)
        .addTo(map);
    airportMarker.togglePopup();
    return airportMarker;
}

function addLookupMarker(locationOptionElem,isSaved) {
    const nameAttribute = 'name';
    var locationPopup =  new mapboxgl.Popup({ offset:
            {
                'top': [0, 10],    // When popup appears below marker (anchor: 'top')
                'top-left': [0, 10],
                'top-right': [0, 10],
                'bottom': [0, -42],  // When popup appears above marker (anchor: 'bottom')
                'bottom-left': [0, -42],
                'bottom-right': [0, -42],
                'left': [13, 0],    // When popup appears to the right of marker (anchor: 'left')
                'right': [-13, 0],    // When popup appears to the left of marker (anchor: 'right')
                // Add other anchor positions as needed
              }
        }) // add popups
              .setHTML(`<strong data-name='${locationOptionElem.getAttribute(nameAttribute)}'>${locationOptionElem.dataset.location}</strong>,
               <i>${locationOptionElem.dataset.subdivision}</i>
               of ${locationOptionElem.dataset.country}`);

    const el = document.createElement('div');
    if (isSaved) el.className = 'lookup-marker';
    else el.className = 'lookup-marker-from-results';
    var locationMarker = new mapboxgl.Marker()
        .setLngLat([locationOptionElem.dataset.longitude,locationOptionElem.dataset.latitude])
        .setPopup(locationPopup)
        .addTo(map);
    locationMarker.togglePopup();
    return locationMarker;
}

function addLookupMarkerFromSearch(searchButtonElem) {
    var locationPopup =  new mapboxgl.Popup({ offset:
            {
                'top': [0, 10],    // When popup appears below marker (anchor: 'top')
                'top-left': [0, 10],
                'top-right': [0, 10],
                'bottom': [0, -42],  // When popup appears above marker (anchor: 'bottom')
                'bottom-left': [0, -42],
                'bottom-right': [0, -42],
                'left': [13, 0],    // When popup appears to the right of marker (anchor: 'left')
                'right': [-13, 0],    // When popup appears to the left of marker (anchor: 'right')
                // Add other anchor positions as needed
              }
        }) // add popups
              .setHTML(`<strong data-name='${searchButtonElem.id}'>${searchButtonElem.dataset.name}</strong>,
               <i>${searchButtonElem.dataset.subdivision}</i> |
                of ${searchButtonElem.dataset.country}`);
    const el = document.createElement('div');
    var locationMarker = new mapboxgl.Marker()
        .setLngLat([searchButtonElem.dataset.lng,searchButtonElem.dataset.lat])
        .setPopup(locationPopup)
        .addTo(map);
    locationMarker.togglePopup();
    return locationMarker;
}

function clearMarkers() {
    if (lookupMarker) {
        lookupMarker.remove();
        lookupMarker = null;
    }
    if (airportMarker) {
        airportMarker.remove();
        airportMarker = null;
    }
    clearStartMarkers();
    clearEndMarkers();
}

function clearAirportMarkerFitToElemId(marker,elemIdWithBounds) {
    if (marker) {
        marker.remove();
        marker = null;
        const elem = document.getElementById(elemIdWithBounds);
        if (elem) fitMapToElemWithBounds(elem);
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
    closeTripMarkerPopups();
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
    if (nonDeltaStartMarker) {
        nonDeltaStartMarker.remove();
        nonDeltaStartMarker = null;
    }
    airportStartMarkers.forEach((marker,iter) => {
        marker.remove();
    });
    airportStartMarkers.length = 0;
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
    if (nonDeltaEndMarker) {
        nonDeltaEndMarker.remove();
        nonDeltaEndMarker = null;
    }
    airportEndMarkers.forEach((marker,iter) => {
        marker.remove();
    });
    airportEndMarkers.length = 0;
}

function clearAirportMarkerMapFitToLocation(isStart) {
    if (isStart && airportStartMarker) {
        airportStartMarker.remove();
        airportStartMarker = null;
        const startSelect = document.getElementById('select-start-location');
        if (startSelect && startSelect.options && startSelect.options[startSelect.selectedIndex]) {
            fitMapToElemWithBounds(startSelect.options[startSelect.selectedIndex]);
        }
    } else if (!isStart && airportEndMarker) {
        airportEndMarker.remove();
        airportEndMarker = null;
        const endSelect = document.getElementById('select-end-location');
        if (endSelect && endSelect.options && endSelect.options[endSelect.selectedIndex]) {
            fitMapToElemWithBounds(endSelect.options[endSelect.selectedIndex]);
        }
    }
}