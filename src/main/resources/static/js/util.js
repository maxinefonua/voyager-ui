function getMatchingAirportOption(datalistElem,airportCode) {
    return Array.from(datalistElem.options)
        .find(option => option.dataset.value === airportCode);
}

function replaceTargetWithLoading(targetElemId) {
    const targetElem = document.getElementById(targetElemId);
    targetElem.innerHTML = `<div class="list-group-item">
            <i role="status">Loading...</i>
            <div class="spinner-border spinner-border-sm" aria-hidden="true"></div>
        </div>`
}

function loadAirlineOptions(airlineOptionList) {
    if (airlineOptionList) {
        console.log(airlineOptionList);
    } else console.log('airlineOptionList null');
}