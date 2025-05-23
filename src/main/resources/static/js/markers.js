// TODO: implement loading data into the DOM https://docs.mapbox.com/help/tutorials/custom-markers-gl-js/?step=4

var lookupMarker = null;
var airportMarker = null;
var startMarker = null;
var airportStartMarker = null;
var endMarker = null;
var airportEndMarker = null;
var nonDeltaStartMarker = null;
var nonDeltaEndMarker = null;
var nonDeltaAirportMarker = null;

function setAirportPlaceHolder(iterIndex) {
    if (airportMarker) airportMarker.remove();
    const airportInput = document.getElementById("airport-code-"+iterIndex);
    const airportList = document.getElementById("filtered-list-"+iterIndex);
    if (airportInput && airportList && airportList.options && airportList.options[0]) {
        airportInput.placeholder = airportList.options[0].value;
        airportInput.value = '';
    }
}

function addNonDeltaAirportToMapNotTrips(selectedIata) {
    if (nonDeltaAirportMarker) {
        nonDeltaAirportMarker.remove();
        nonDeltaAirportMarker = null;
    }
    if (!regexIata.test(selectedIata)) return;
    const allAirports = document.getElementById('all-airports');
    if (allAirports && allAirports.options && allAirports.options.namedItem(selectedIata.toUpperCase())) {
        const selectedOption = allAirports.options.namedItem(selectedIata.toUpperCase());
        nonDeltaAirportMarker = addAirportMarkerWithColor(selectedOption);
    if (airportMarker) mapFitThreeMarkers(lookupMarker,airportMarker,nonDeltaAirportMarker);
        else mapFitBothMarkers(lookupMarker,nonDeltaAirportMarker);
    }
}

function addNonDeltaAirportToMap(selectedIata,isStart) {
    if (isStart && nonDeltaStartMarker) {
        nonDeltaStartMarker.remove();
        nonDeltaStartMarker = null;
    } else if (!isStart && nonDeltaEndMarker) {
        nonDeltaEndMarker.remove();
        nonDeltaEndMarker.null;
    }
    if (!regexIata.test(selectedIata)) return;
    const allAirports = document.getElementById('all-airports');
    if (allAirports && allAirports.options && allAirports.options.namedItem(selectedIata.toUpperCase())) {
        const selectedOption = allAirports.options.namedItem(selectedIata.toUpperCase());
        if (isStart) {
            nonDeltaStartMarker = addAirportMarkerWithColor(selectedOption);
            if (airportStartMarker) mapFitThreeMarkers(nonDeltaStartMarker,startMarker,airportStartMarker);
            else mapFitBothMarkers(startMarker,nonDeltaStartMarker);
        } else {
            nonDeltaEndMarker = addAirportMarkerWithColor(selectedOption);
            if (airportEndMarker) mapFitThreeMarkers(nonDeltaEndMarker,endMarker,airportEndMarker);
            else mapFitBothMarkers(endMarker,nonDeltaEndMarker);
        }
    }
}

function addAirportToMap(airportInputElem,isStart) {
    if (airportInputElem && airportInputElem.value && regexIata.test(airportInputElem.value)) {
        const allAirports = document.getElementById('all-airports');
        if (allAirports && allAirports.options && allAirports.options.namedItem(airportInputElem.value.toUpperCase())) {
            const airportMatch = allAirports.options.namedItem(airportInputElem.value.toUpperCase());
            if (isStart) {
                if (airportStartMarker) airportStartMarker.remove();
                airportStartMarker = addAirportMarker(airportMatch);
                airportInputElem.classList.remove('is-invalid');
                mapFitBothMarkers(startMarker,airportStartMarker);
            } else {
                if (airportEndMarker) airportEndMarker.remove();
                airportEndMarker = addAirportMarker(airportMatch);
                airportInputElem.classList.remove('is-invalid');
                mapFitBothMarkers(endMarker,airportEndMarker);
            }
        }
    } else {
        airportInputElem.classList.add('is-invalid');
        if (isStart && airportStartMarker) {
            airportStartMarker.remove();
            airportStartMarker = null;
        } else if (!isStart && airportEndMarker) {
            airportEndMarker.remove();
            airportEndMarker = null;
        }
    }
};

function mapFitBothMarkers(marker1,marker2) {
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

function addAirportMarkerWithColor(airportOptionElem) {
    const el = document.createElement('div');
    el.className = 'airport-marker-non-delta';
    el.innerHTML = ``
//    el.style.filter = 'drop-shadow(rgb(173, 181, 189) 3px 5px 1px) inverse(90%) contrast(%200)';
    var airportPopup =  new mapboxgl.Popup({ offset: 25 }) // add popups
              .setHTML(`<strong>${airportOptionElem.value} | </strong><i>${airportOptionElem.dataset.airport}</i><p>Located in ${airportOptionElem.dataset.city}, ${airportOptionElem.dataset.subdivision} of ${airportOptionElem.dataset.country}</p>(Add as hyperlink from airport name, add as another property of AirportDisplay class)Official website: <a href="https://www.finavia.fi/en/airports/helsinki-airport" target="_blank" title="Opens in a new window">Helsinki Airport</a>`);
    var airportMarker = new mapboxgl.Marker(el)
        .setLngLat([airportOptionElem.dataset.longitude,airportOptionElem.dataset.latitude])
        .setPopup(airportPopup)
        .addTo(map);
    return airportMarker;
}

function addAirportMarker(airportOptionElem) {
    const el = document.createElement('div');
    el.className = 'airport-marker';
    var airportPopup =  new mapboxgl.Popup({ offset: 25 }) // add popups
              .setHTML(`<strong>${airportOptionElem.value} | </strong><i>${airportOptionElem.dataset.airport}</i><p>Located in ${airportOptionElem.dataset.city}, ${airportOptionElem.dataset.subdivision} of ${airportOptionElem.dataset.country}</p>(Add as hyperlink from airport name, add as another property of AirportDisplay class)Official website: <a href="https://www.finavia.fi/en/airports/helsinki-airport" target="_blank" title="Opens in a new window">Helsinki Airport</a>`);
    var airportMarker = new mapboxgl.Marker(el)
        .setLngLat([airportOptionElem.dataset.longitude,airportOptionElem.dataset.latitude])
        .setPopup(airportPopup)
        .addTo(map);
    return airportMarker;
}

function updateWithAirportMap(selectedOption) {
    if (airportMarker) airportMarker.remove();
    if (lookupMarker) {
        const lngLat = lookupMarker.getLngLat();
        const south = lngLat.lat < selectedOption.dataset.latitude? lngLat.lat : selectedOption.dataset.latitude;
        const north = lngLat.lat > selectedOption.dataset.latitude? lngLat.lat : selectedOption.dataset.latitude;
        const west = lngLat.lng < selectedOption.dataset.longitude? lngLat.lng : selectedOption.dataset.longitude;
        const east = lngLat.lng > selectedOption.dataset.longitude? lngLat.lng : selectedOption.dataset.longitude;
        const bounds = [west,south,east,north];
        map.fitBounds(bounds, {padding: {top: 60, bottom: 30, left: 40, right: 40}});
        airportMarker = new mapboxgl.Marker({color:"#9B1631"})
            .setLngLat([selectedOption.dataset.longitude,selectedOption.dataset.latitude])
            .setPopup(
                new mapboxgl.Popup({ offset: 25 }) // add popups
                    .setHTML(
                        `<strong>${selectedOption.value}</strong> | <i>${selectedOption.dataset.airport}</i> Located in ${selectedOption.dataset.city}, ${selectedOption.dataset.subdivision} of ${selectedOption.dataset.country}.`
                )
            )
            .addTo(map);
    }
};

function clearMarkers() {
    if (lookupMarker) {
        lookupMarker.remove();
        lookupMarker = null;
    }
    if (airportMarker) {
        airportMarker.remove();
        airportMarker = null;
    }
    if (startMarker) {
        startMarker.remove();
        startMarker = null;
    }
    if (airportStartMarker) {
        airportStartMarker.remove();
        airportStartMarker = null;
    }
    if (endMarker) {
        endMarker.remove();
        endMarker = null;
    }
    if (airportEndMarker) {
        airportEndMarker.remove();
        airportEndMarker = null;
    }
    if (nonDeltaAirportMarker) {
        nonDeltaAirportMarker.remove();
        nonDeltaAirportMarker = null;
    }
    if (nonDeltaStartMarker) {
        nonDeltaStartMarker.remove();
        nonDeltaStartMarker = null;
    }
    if (nonDeltaEndMarker) {
        nonDeltaEndMarker.remove();
        nonDeltaEndMarker = null;
    }
}

function clearAirportMarkerFitToElemId(marker,elemIdWithBounds) {
    if (marker) {
        marker.remove();
        marker = null;
        const elem = document.getElementById(elemIdWithBounds);
        if (elem) fitMapToElemWithBounds(elem);
    }
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

