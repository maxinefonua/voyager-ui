function processLocationInput(inputElem,inputValueElemId,datalistElemId,oppositeInputValueElemId) {
    const inputValueElem = document.getElementById(inputValueElemId);
    if (inputElem.value.trim().length == 0) {
        if (inputValueElem.value.trim().length > 0) {
            inputValueElem.value = '';
            inputValueElem.dispatchEvent(new Event('input'));
        }
        return;
    }
    if (inputElem.value.trim().length < 5) return;
    const selectedOption = Array.from(inputElem.list.options)
        .find(option => option.value === inputElem.value);
    if (selectedOption && inputValueElem) {
        if (inputValueElem.value != selectedOption.dataset.value) {
            inputValueElem.value = selectedOption.dataset.value;
            inputValueElem.dispatchEvent(new Event('input'));
        }
        return;
    }
    const datalistElem = document.getElementById(datalistElemId);
    const oppositeInputValueElem = document.getElementById(oppositeInputValueElemId);
    // update datalist
    fetch(`/lookup?inputText=${encodeURIComponent(inputElem.value)}&excludeSourceId=${oppositeInputValueElem.value}`)
            .then(response => response.text())
            .then(html => {
                // Create temporary container to parse the fragment
                const temp = document.createElement('div');
                temp.innerHTML = html;

                // TODO: disable or send opposite location to disable already selected option

                // Replace datalist contents
                if (temp.querySelectorAll('option').length > 0) {
                    datalistElem.innerHTML = '';
                    temp.querySelectorAll('option').forEach(opt => {
                        datalistElem.appendChild(opt.cloneNode(true));
                    });
                    if (datalistElem.options.length == 1) {
                        if (inputValueElem.value !== datalistElem.options[0].dataset.value) {
                            inputElem.value = datalistElem.options[0].value;
                            inputValueElem.value = datalistElem.options[0].dataset.value;
                            inputValueElem.dispatchEvent(new Event('input'));
                        }
                    } else if (inputValueElem.value.trim().length > 0) {
                        inputValueElem.value = '';
                        inputValueElem.dispatchEvent(new Event('input'));
                    }
                } else {
                    if (inputValueElem.value.trim().length > 0) {
                        inputValueElem.value = '';
                        inputValueElem.dispatchEvent(new Event('input'));
                    }
                }
            })
            .catch(error => console.error('Error:', error));
    inputElem.focus();
    inputElem.click();
}

function airportIncluded(includeButtonElem,inputElemId) {
    const inputElem = document.getElementById(inputElemId);
    const selectedOption = Array.from(inputElem.list.options)
        .find(option => option.value === inputElem.value);
    if (selectedOption) {
        inputElem.value = '';
        inputElem.placeholder = 'Enter Additional Airport';
        selectedOption.disabled = true;
        includeButtonElem.disabled = true;
    }
}

function checkReverse(reverseButtonElem,startInputValueElem,endInputValueElem) {
    reverseButtonElem.disabled = startInputValueElem.value.length == 0 && endInputValueElem.value.length == 0;
}

function processAirportInput(isStart,inputElem,includeButtonElemId,locationMarker) {
    const selectedOption = Array.from(inputElem.list.options)
        .find(option => option.value === inputElem.value);
    const includeButtonElem = document.getElementById(includeButtonElemId);
    if (selectedOption) {
        includeButtonElem.value = selectedOption.dataset.value;
        includeButtonElem.disabled = false;
        closeMarkerPopups();
        const newMarker = addPlainAirportMarkerToMap(selectedOption);
        newMarker.togglePopup();
        mapFitBothMarkers(newMarker,locationMarker);
        if (isStart) airportStartMarkers.push(newMarker);
        else airportEndMarkers.push(newMarker);
        includeButtonElem.click();
    } else {
        includeButtonElem.value = '';
        includeButtonElem.disabled = true;
    }
}

function reverseTrip(startTextElemId,startValueElemId,endTextElemId,endValueElemId) {
    const startTextElem = document.getElementById(startTextElemId);
    const startValueElem = document.getElementById(startValueElemId);
    const startDatalistElem = document.getElementById('input-start-options');

    const endTextElem = document.getElementById(endTextElemId);
    const endValueElem = document.getElementById(endValueElemId);
    const endDatalistElem = document.getElementById('input-end-options');

    const tempText = startTextElem.value;
    const tempValue = startValueElem.value;
    const tempDatalist = startDatalistElem.innerHTML;

    startTextElem.value = endTextElem.value;
    startValueElem.value = endValueElem.value;
    startValueElem.dispatchEvent(new Event('input'));
    startDatalistElem.innerHTML = endDatalistElem.innerHTML;

    endTextElem.value = tempText;
    endValueElem.value = tempValue;
    endValueElem.dispatchEvent(new Event('input'));
    endDatalistElem.innerHTML = tempDatalist;

    reverseMarkers();
}

function filterLocationOption(isStart,excludeSourceId) {
    var oppositeDatalistElem;
    if (isStart) oppositeDatalistElem = document.getElementById('input-end-options');
    else oppositeDatalistElem = document.getElementById('input-start-options');
    var excludeOption = null;
    Array.from(oppositeDatalistElem.options)
        .forEach(option => {
            if (option.dataset.value === excludeSourceId) excludeOption = option;
            else option.disabled = false;
        });
    if (excludeOption) excludeOption.disabled = true;
}

function airportRemoved(isStart,iterIndex,airportCode,datalistElemId) {
    const datalistElem = document.getElementById(datalistElemId);
    const selectedOption = Array.from(datalistElem.options)
        .find(option => option.dataset.value === airportCode);
    if (selectedOption) {
        selectedOption.disabled = false;
        if (isStart) removeMarkerAtIndex(airportStartMarkers,iterIndex);
        else removeMarkerAtIndex(airportEndMarkers,iterIndex);
    }
}