function postPinAirport(inputElemId,filteredListElemId){
    const inputElem = document.getElementById(inputElemId);
    const listElem = document.getElementById(filteredListElemId);
    if (listElem && listElem.options && listElem.options.namedItem(inputElem.value.toUpperCase())) {
        const match = listElem.options.namedItem(inputElem.value.toUpperCase());
        match.disabled = true;
        for (const option of listElem.options){
            if (!option.disabled) {
                inputElem.placeholder = 'Click to Select ' + option.value;
                break;
            }
        }
    }
    if (inputElem && inputElem.value && inputElem.value.length > 0) {
        inputElem.value = '';
    }
}

function setPlaceholderToEnabledOption(inputElemId,filteredListElemId){
    const inputElem = document.getElementById(inputElemId);
    const listElem = document.getElementById(filteredListElemId);
    if (inputElem && listElem && listElem.options) {
        for (const option of listElem.options){
            if (!option.disabled) {
                inputElem.placeholder = 'Click to Select ' + option.value;
                break;
            }
        }
    }
}

function postUnpinAirport(inputElemId,filteredListElemId,airportCode){
    const inputElem = document.getElementById(inputElemId);
    const listElem = document.getElementById(filteredListElemId);
    if (listElem && listElem.options && listElem.options.namedItem(airportCode)) {
        const match = listElem.options.namedItem(airportCode);
        match.disabled = false;
        for (const option of listElem.options){
            if (!option.disabled) {
                inputElem.placeholder = 'Click to Select ' + option.value;
                break;
            }
        }
        if (airportMarker && airportMarker.getPopup() && airportMarker.getPopup().getElement()
            && airportMarker.getPopup().getElement().querySelector('strong[data-name]')
            && airportMarker.getPopup().getElement().querySelector('strong[data-name]').dataset.name === airportCode) {
            airportMarker.remove();
            airportMarker = null;
            if (lookupMarker && lookupMarker.getPopup()) {
                lookupMarker.togglePopup();
                const popupElement = lookupMarker.getPopup().getElement();
                if (popupElement && popupElement.querySelector('strong[data-name]')
                    && popupElement.querySelector('strong[data-name]').dataset.name) {
                    const elemName = popupElement.querySelector('strong[data-name]').dataset.name;
                    const locationElem = getLocationElemByName(elemName);
                    if (locationElem) fitMapToElementBounds(locationElem);
                    else if (document.getElementById(elemName)) { // from search
                        const searchButtonElem = document.getElementById(elemName);
                        fitMapToElementBounds(searchButtonElem);
                    }
                }
            }
        }
    }
}

function removeAirportMarker(removeAirportMarker,isStart) {
    if (removeAirportMarker == null) return;
    closeTripMarkerPopups();
    var locationMarker = null;
    if (isStart) {
        locationMarker = startMarker;
        if (nonDeltaStartMarker && nonDeltaStartMarker == removeAirportMarker) {
            nonDeltaStartMarker.remove();
            nonDeltaStartMarker = null;
            if (airportStartMarker) return;
        } else if (airportStartMarker && airportStartMarker == removeAirportMarker) {
            airportStartMarker.remove();
            airportStartMarker = null;
            if (nonDeltaStartMarker) return;
        }
    } else {
        locationMarker = endMarker;
        if (nonDeltaEndMarker && nonDeltaEndMarker == removeAirportMarker) {
            nonDeltaEndMarker.remove();
            nonDeltaEndMarker = null;
            if (airportEndMarker) return;
        } else if (airportEndMarker && airportEndMarker == removeAirportMarker) {
            airportEndMarker.remove();
            airportEndMarker = null;
            if (nonDeltaEndMarker) return;
        }
    }
    if (locationMarker) {
        locationMarker.togglePopup();
        const popupElement = locationMarker.getPopup().getElement();
        if (popupElement && popupElement.querySelector('strong[data-name]')
            && popupElement.querySelector('strong[data-name]').dataset.name) {
            const elemName = popupElement.querySelector('strong[data-name]').dataset.name;
            const locationElem = getLocationElemByName(elemName);
            if (locationElem) fitMapToElementBounds(locationElem);
            else if (document.getElementById(elemName)) { // from search
                const searchButtonElem = document.getElementById(elemName);
                fitMapToElementBounds(searchButtonElem);
            }
        }
    }
}

function switchEngage(switchElement) {
    switchElement.value = !switchElement.value;
}