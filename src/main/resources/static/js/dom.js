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

function reloadTrips(scrollingId) {
    recenterMap();
    const el = document.getElementById(scrollingId);
    if (el) bootstrap.ScrollSpy.getOrCreateInstance(el).refresh();
}