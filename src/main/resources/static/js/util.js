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

function dispatchInput() {
    var inputValueElem = document.getElementById('end-input-value');
    var inputTextElem = document.getElementById('end-input-text');
    var datalistElem = document.getElementById('input-end-options');
    dispatchInputEvent(inputValueElem,inputTextElem,datalistElem);

    inputValueElem = document.getElementById('start-input-value');
    inputTextElem = document.getElementById('start-input-text');
    datalistElem = document.getElementById('input-start-options');
    dispatchInputEvent(inputValueElem,inputTextElem,datalistElem);
}

function dispatchInputEvent(inputValueElem,inputTextElem,datalistElem) {
    if (inputTextElem.value.trim() != '') {
        const match = Array.from(datalistElem.options)
            .find(option => option.value === inputTextElem.value);
        if (match) {
            htmx.process(inputValueElem);
            inputValueElem.value = match.dataset.value;
            inputValueElem.dispatchEvent(new Event('input'));
        }
    }
}