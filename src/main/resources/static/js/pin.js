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
    }
}