function processLocationInput(inputElem,inputValueElemId,datalistElemId) {
    const inputValueElem = document.getElementById(inputValueElemId);
    if (inputElem.value.trim().length == 0) {
        inputValueElem.value = '';
        inputValueElem.dispatchEvent(new Event('input'));
        return;
    }
    const selectedOption = Array.from(inputElem.list.options)
        .find(option => option.value === inputElem.value);
    if (selectedOption && inputValueElem) {
        inputValueElem.value = selectedOption.dataset.value;
        inputValueElem.dispatchEvent(new Event('input'));
        return;
    }
    const datalistElem = document.getElementById(datalistElemId);
    // update datalist
    fetch(`/lookup?inputText=${encodeURIComponent(inputElem.value)}`)
            .then(response => response.text())
            .then(html => {
                // Create temporary container to parse the fragment
                const temp = document.createElement('div');
                temp.innerHTML = html;

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
                    } else {
                        inputValueElem.value = '';
                        inputValueElem.dispatchEvent(new Event('input'));
                    }
                } else {
                    inputValueElem.value = '';
                    inputValueElem.dispatchEvent(new Event('input'));
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

function includeAirport(includeButtonElem,inputElemId) {
    const inputElem = document.getElementById(inputElemId);
    const selectedOption = Array.from(inputElem.list.options)
        .find(option => option.value === inputElem.value);
    if (selectedOption) {
        const airportCode = selectedOption.dataset.value;
        const parentElem = inputElem.parentElement;
        const newDiv = document.createElement('div');
        newDiv.innerHTML = `<div class="input-group-text pe-2 me-2">${airportCode}<button class="btn-close" style="transform: scale(0.7); transform-origin: center;" onclick="removeIncludedAirport(this.parentElement,\'${inputElemId}\')"></button></div>`;
        parentElem.insertBefore(newDiv,inputElem);
        inputElem.value = '';
        inputElem.placeholder = 'Enter Additional Airport';
        selectedOption.disabled = true;
    }
    includeButtonElem.disabled = true;
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

function removeIncludedAirport(includedAirportElem,inputElemId) {
    const inputElem = document.getElementById(inputElemId);
    const airportCode = includedAirportElem.textContent;
    const selectedOption = Array.from(inputElem.list.options)
        .find(option => option.dataset.value === airportCode);
    if (selectedOption) {
        selectedOption.disabled = false;
    }
    includedAirportElem.remove();
}

function processTripInput(inputElem,inputValueElemId,tripFilterElemId,datalistElemId) {
    const inputValueElem = document.getElementById(inputValueElemId);
    if (inputElem.value == null || inputElem.value.trim().length == 0) {
        if (inputValueElem) {
            inputValueElem.value = '';
            inputValueElem.dispatchEvent(new Event('input'));
        }
    } else {
        const selectedOption = Array.from(inputElem.list.options)
            .find(option => option.value === inputElem.value);
        const tripFilterElem = document.getElementById(tripFilterElemId);
        if (selectedOption && inputValueElem) {
            inputValueElem.value = selectedOption.dataset.value;
            inputValueElem.dispatchEvent(new Event('input'));
        }
        else if (tripFilterElem && tripFilterElem.value == 'LOCATION') {
            const datalistElem = document.getElementById(datalistElemId);
            // update datalist
            fetch(`/lookup?inputText=${encodeURIComponent(inputElem.value)}`)
                    .then(response => response.text())
                    .then(html => {
                        // Create temporary container to parse the fragment
                        const temp = document.createElement('div');
                        temp.innerHTML = html;

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
                            } else {
                                inputValueElem.value = '';
                                inputValueElem.dispatchEvent(new Event('input'));
                            }
                        } else {
                            inputValueElem.value = '';
                            inputValueElem.dispatchEvent(new Event('input'));
                        }
                    })
                    .catch(error => console.error('Error:', error));
            inputElem.focus();
            inputElem.click();
        } else {
            inputValueElem.value = '';
            inputValueElem.dispatchEvent(new Event('input'));
        }
    }
}

function resetTripInputElements(isStart,tripFilter,inputTextElemId,inputValueElemId) {
    const inputTextElem = document.getElementById(inputTextElemId);
    const inputValueElem = document.getElementById(inputValueElemId);
    inputTextElem.value = '';
    inputValueElem.value = '';
    if (tripFilter == 'AIRPORT') {
        inputTextElem.placeholder = 'Enter Airport'
    }
    if (tripFilter == 'LOCATION') {
        inputTextElem.placeholder = 'Search Location'
    }
}