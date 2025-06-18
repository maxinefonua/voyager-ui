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

function deepCopyMarker(marker) {
    if (marker == null) return marker;
    const element = marker.getElement().cloneNode(true);
    const lngLat = marker.getLngLat();
    const options = {
        element: element,
        offset: marker.getOffset(),
        draggable: marker.isDraggable(),
        rotation: marker.getRotation(),
        pitchAlignment: marker.getPitchAlignment(),
        rotationAlignment: marker.getRotationAlignment()
    };

    const newMarker = new mapboxgl.Marker(options)
        .setLngLat(lngLat);

    if (marker.getPopup()) {
        newMarker.setPopup(new mapboxgl.Popup().setText(marker.getPopup().getText()));
    }

    return newMarker;
}

function swapSelectionsAndInputs(selectStartElemId,selectEndElemId,startIataElemId,endIataElemId,startNonDeltaElemId,endNonDeltaElemId) {
    const selectStartElem = document.getElementById(selectStartElemId);
    const selectEndElem = document.getElementById(selectEndElemId);
    if (selectStartElem && selectEndElem) {
        const temp = selectStartElem.value;
        selectStartElem.value = selectEndElem.value;
        selectEndElem.value = temp;
    }

    const startIataElem = document.getElementById(startIataElemId);
    const endIataElem = document.getElementById(endIataElemId);
    if (startIataElem && endIataElem) {
        const temp = startIataElem.value;
        startIataElem.value = endIataElem.value;
        endIataElem.value = temp;
    }

    const startNonDeltaElem = document.getElementById(startNonDeltaElemId);
    const endNonDeltaElem = document.getElementById(endNonDeltaElemId);
    if (startNonDeltaElem && endNonDeltaElem) {
        const temp = startNonDeltaElem.value;
        startNonDeltaElem.value = endNonDeltaElem.value;
        endNonDeltaElem.value = temp;
    }
}

function addAirportFromSelect(selectElem,isStart) {
    if (selectElem && selectElem.options && selectElem.options[selectElem.selectedIndex] &&
        selectElem.options[selectElem.selectedIndex].value) {
        selectedAirportCode = selectElem.options[selectElem.selectedIndex].value;
        if (regexIata.test(selectedAirportCode)) {
            const allAirports = document.getElementById('all-airports');
            const deltaAirports = document.getElementById('delta-airports');
            if (allAirports && allAirports.options && allAirports.options.namedItem(selectedAirportCode)) {
                const airportMatch = allAirports.options.namedItem(selectedAirportCode);
                const deltaMatch = deltaAirports.options.namedItem(selectedAirportCode);
                const isDelta = (deltaMatch != null);
                closeTripMarkerPopups();
                if (isStart) {
                    if (isDelta) {
                        if (airportStartMarker) airportStartMarker.remove();
                        airportStartMarker = addAirportMarker(airportMatch,isDelta);
                        if (startMarker) {
                            if (nonDeltaStartMarker) mapFitThreeMarkers(startMarker,airportStartMarker,nonDeltaStartMarker);
                            else mapFitBothMarkers(startMarker,airportStartMarker);
                        } else if (nonDeltaStartMarker) {
                            mapFitBothMarkers(airportStartMarker,nonDeltaStartMarker);
                        }
                    } else {
                        if (nonDeltaStartMarker) nonDeltaStartMarker.remove();
                        nonDeltaStartMarker = addAirportMarker(airportMatch,isDelta);
                        if (startMarker) {
                            if (airportStartMarker) mapFitThreeMarkers(startMarker,airportStartMarker,nonDeltaStartMarker);
                            else mapFitBothMarkers(startMarker,nonDeltaStartMarker);
                        } else if (airportStartMarker) {
                            mapFitBothMarkers(airportStartMarker,nonDeltaStartMarker);
                        }
                    }
                } else {
                    if (isDelta) {
                        if (airportEndMarker) airportEndMarker.remove();
                        airportEndMarker = addAirportMarker(airportMatch,isDelta);
                        if (endMarker) {
                            if (nonDeltaEndMarker) mapFitThreeMarkers(endMarker,airportEndMarker,nonDeltaEndMarker);
                            mapFitBothMarkers(endMarker,airportEndMarker);
                        } else if (nonDeltaEndMarker) {
                            mapFitBothMarkers(nonDeltaEndMarker,airportEndMarker);
                        }
                    } else {
                        if (nonDeltaEndMarker) nonDeltaEndMarker.remove();
                        nonDeltaEndMarker = addAirportMarker(airportMatch,isDelta);
                        if (endMarker) {
                            if (airportEndMarker) mapFitThreeMarkers(endMarker,airportEndMarker,nonDeltaEndMarker);
                            mapFitBothMarkers(endMarker,nonDeltaEndMarker);
                        } else if (airportEndMarker) {
                            mapFitBothMarkers(airportEndMarker,nonDeltaEndMarker);
                        }
                    }
                }
            }
        }
    }
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

function addAirportToMapFromCode(airportCode) {
    if (airportMarker) airportMarker.remove();
    if (airportCode) {
        const allAirports = document.getElementById('all-airports');
        const deltaAirports = document.getElementById('delta-airports');
        if (allAirports && allAirports.options && allAirports.options.namedItem(airportCode)) {
            const airportMatch = allAirports.options.namedItem(airportCode);
            const deltaMatch = deltaAirports.options.namedItem(airportCode);
            const isDelta = (deltaMatch != null);
            if (airportMatch) {
                airportMarker = addAirportMarker(airportMatch,isDelta);
                if (lookupMarker) {
                    closeTripMarkerPopups();
                    if (lookupMarker.getPopup().isOpen()) lookupMarker.togglePopup();
                    mapFitBothMarkers(lookupMarker,airportMarker);
                }
                else centerMapOnMarker(airportMarker);
            }
        }
    }
}

function checkAirportInput(airportInputElem,iterIndex) {
    if (airportMarker) { // clears airport marker
        airportMarker.remove();
        airportMarker = null;
    }
    // if input is empty, remove invalid styling
    if (airportInputElem && (airportInputElem.value == null || airportInputElem.value.trim().length == 0)) {
        airportInputElem.classList.remove('is-invalid');
    }
    if (airportInputElem && airportInputElem.value && regexIata.test(airportInputElem.value)) {
        const allAirports = document.getElementById('all-airports');
        const deltaAirports = document.getElementById('delta-airports');
        if (allAirports && allAirports.options && allAirports.options.namedItem(airportInputElem.value.toUpperCase())) {
            const airportMatch = allAirports.options.namedItem(airportInputElem.value.toUpperCase());
            const deltaMatch = deltaAirports.options.namedItem(airportInputElem.value.toUpperCase());
            const isDelta = (deltaMatch != null);
            if (airportMatch) {
                airportInputElem.classList.remove('is-invalid');
                airportMarker = addAirportMarker(airportMatch,isDelta);
                if (lookupMarker) {
                    closeTripMarkerPopups();
                    if (lookupMarker.getPopup().isOpen()) lookupMarker.togglePopup();
                    mapFitBothMarkers(lookupMarker,airportMarker);
                }
                else centerMapOnMarker(airportMarker);
                airportInputElem.value = airportInputElem.value.toUpperCase(); // sets IATA value
            } else airportInputElem.classList.add('is-invalid');
        } else airportInputElem.classList.add('is-invalid');
    } else if (airportInputElem && airportInputElem.value && airportInputElem.value.trim().length > 0) airportInputElem.classList.add('is-invalid');
}

function setAirportPlaceHolderFromList(airportInputId,airportListId){
    const airportInput = document.getElementById(airportInputId);
    const airportList = document.getElementById(airportListId);
    if (airportInput && airportList && airportList.options && airportList.options[0]) {
        airportInput.placeholder = 'Click to Select ' + airportList.options[0].value;
        airportInput.value = '';
    }
}

function activate(element) {
    clearMarkers();
    const tabs = document.getElementsByClassName('tab-btn');
    for (let i = 0; i < tabs.length; i++) {
        tabs[i].classList.remove('active');
        tabs[i].disabled = false;
    }
    element.classList.add('active');
    element.disabled = true;
}

function reloadTrips(scrollingId) {
    recenterMap();
    const el = document.getElementById(scrollingId);
    if (el) bootstrap.ScrollSpy.getOrCreateInstance(el).refresh();
}

function reenableThisSelect(selectElem) {
    if (selectElem && selectElem.options && selectElem.options.length
        && selectElem.options.length > 1) {
        selectElem.disabled = false;
    }
    selectElem.disabled = true;
}