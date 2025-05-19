// TODO: implement loading data into the DOM https://docs.mapbox.com/help/tutorials/custom-markers-gl-js/?step=4

const regexIata = /^[a-zA-Z]{3}$/;
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

function checkAirportInput(airportInputElem) {
    if (airportMarker) {
        airportMarker.remove();
        airportMarker = null;
    }
    if (airportInputElem && airportInputElem.value && regexIata.test(airportInputElem.value)) {
        const allAirports = document.getElementById('all-airports');
        if (allAirports && allAirports.options && allAirports.options.namedItem(airportInputElem.value.toUpperCase())) {
            const airportMatch = allAirports.options.namedItem(airportInputElem.value.toUpperCase());
            if (airportMatch) airportInputElem.classList.remove('is-invalid');
            else airportInputElem.classList.add('is-invalid');
            airportMarker = addAirportMarker(airportMatch);
            if (lookupMarker) mapFitBothMarkers(lookupMarker,airportMarker);
            else centerMapOnMarker(airportMarker);
        } else {
            console.log('checkAirportInput called with index: ' + iterIndex + ', length: ' + airportInputElem.value.length + ', no match. Airport value: ' + airportInput.value);
        }
    }
};

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