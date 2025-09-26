// TODO: implement loading data into the DOM https://docs.mapbox.com/help/tutorials/custom-markers-gl-js/?step=4

const regexIata = /^[a-zA-Z]{3}$/;

function reverseTripMarkers() { // swap start and end markers
    var tempStartMarker = startMarker;
    var tempAirportStartMarker = airportStartMarker;
    var tempNonDeltaMarker = nonDeltaStartMarker;

    startMarker = endMarker;
    airportStartMarker = airportEndMarker;
    nonDeltaStartMarker = nonDeltaEndMarker;

    endMarker = tempStartMarker;
    airportEndMarker = tempAirportStartMarker;
    nonDeltaEndMarker = tempNonDeltaMarker;

    closeTripMarkerPopups();
    if (startMarker && endMarker) mapFitBothMarkers(startMarker,endMarker);
    else if (startMarker && airportStartMarker && nonDeltaStartMarker)
        mapFitThreeMarkers(startMarker,airportStartMarker,nonDeltaStartMarker);
    else if (endMarker && airportEndMarker && nonDeltaStartMarker)
        mapFitThreeMarkers(endMarker,airportEndMarker,nonDeltaStartMarker);
}

function closeTripMarkerPopups() {
    if (airportStartMarker && airportStartMarker.getPopup()
        && airportStartMarker.getPopup().isOpen()) airportStartMarker.getPopup().remove();
    if (startMarker && startMarker.getPopup()
        && startMarker.getPopup().isOpen()) startMarker.getPopup().remove();
    if (nonDeltaStartMarker && nonDeltaStartMarker.getPopup()
        && nonDeltaStartMarker.getPopup().isOpen()) nonDeltaStartMarker.getPopup().remove();
    if (airportEndMarker && airportEndMarker.getPopup()
        && airportEndMarker.getPopup().isOpen()) airportEndMarker.getPopup().remove();
    if (endMarker && endMarker.getPopup()
        && endMarker.getPopup().isOpen()) endMarker.getPopup().remove();
    if (nonDeltaEndMarker && nonDeltaEndMarker.getPopup()
        && nonDeltaEndMarker.getPopup().isOpen()) nonDeltaEndMarker.getPopup().remove();
}

var lineFeature = null;

function addAirportToMap(airportInputElem,isStart) {
    if (isStart) clearStartMarkers();
    else clearEndMarkers();
    if (airportInputElem && airportInputElem.value && regexIata.test(airportInputElem.value)) {
        const allAirports = document.getElementById('all-airports');
        const deltaAirports = document.getElementById('delta-airports');
        if (allAirports && allAirports.options && allAirports.options.namedItem(airportInputElem.value.toUpperCase())) {
            const airportMatch = allAirports.options.namedItem(airportInputElem.value.toUpperCase());
            const deltaMatch = deltaAirports.options.namedItem(airportInputElem.value.toUpperCase());
            const isDelta = (deltaMatch != null);
            airportInputElem.classList.remove('is-invalid');
            if (isStart) {
                if (isDelta) airportStartMarker = addAirportMarker(airportMatch,isDelta);
                else nonDeltaStartMarker = addAirportMarker(airportMatch,isDelta);
                if (startMarker && airportStartMarker && nonDeltaStartMarker)
                    mapFitThreeMarkers(startMarker,nonDeltaStartMarker,airportStartMarker);
                else if (startMarker && airportStartMarker)
                    mapFitBothMarkers(startMarker,airportStartMarker);
                else if (startMarker && nonDeltaStartMarker)
                    mapFitBothMarkers(startMarker,nonDeltaStartMarker);
                else mapFitBothMarkers(airportStartMarker,nonDeltaStartMarker);
            } else {
                if (isDelta) airportEndMarker = addAirportMarker(airportMatch,isDelta);
                else nonDeltaEndMarker = addAirportMarker(airportMatch,isDelta);
                if (endMarker && airportEndMarker && nonDeltaEndMarker)
                    mapFitThreeMarkers(endMarker,airportEndMarker,nonDeltaEndMarker);
                else if (endMarker && airportEndMarker)
                    mapFitBothMarkers(endMarker,airportEndMarker);
                else if (endMarker && nonDeltaEndMarker)
                    mapFitBothMarkers(endMarker,nonDeltaEndMarker);
                else mapFitBothMarkers(airportEndMarker,nonDeltaEndMarker);
            }
            airportInputElem.value = airportInputElem.value.toUpperCase(); // sets IATA value
        }
    } else if (airportInputElem && (airportInputElem.value == null || airportInputElem.value.trim().length == 0)) {
        airportInputElem.classList.remove('is-invalid');
    } else airportInputElem.classList.add('is-invalid');
}

function reloadTrips(scrollingId) {
    recenterMap();
    const el = document.getElementById(scrollingId);
    if (el) bootstrap.ScrollSpy.getOrCreateInstance(el).refresh();
}