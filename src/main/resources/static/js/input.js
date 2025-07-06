function processTripInput(inputElem,inputValueElemId,tripFilterElemId,datalistElemId) {
    const inputValueElem = document.getElementById(inputValueElemId);
    if (inputElem.value == null || inputElem.value.trim().length == 0) {
        if (inputValueElem) inputValueElem.value = '';
    } else {
        const selectedOption = Array.from(inputElem.list.options)
            .find(option => option.value === inputElem.value);
        const tripFilterElem = document.getElementById(tripFilterElemId);
        if (selectedOption && inputValueElem) inputValueElem.value = selectedOption.dataset.value;
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
                        datalistElem.innerHTML = '';
                        temp.querySelectorAll('option').forEach(opt => {
                            datalistElem.appendChild(opt.cloneNode(true));
                        });
                    })
                    .catch(error => console.error('Error:', error));
        } else inputValueElem.value = '';
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