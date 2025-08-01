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

function removeClosestClassFrom(childElem,className) {
    const targetElem = childElem.closest(className);
    if (targetElem) targetElem.remove();
}

function checkForRemoval(childElem,className) {
    if (childElem.innerHTML.trim() === '') removeClosestClassFrom(childElem,className);
}

function replaceButtonWithFunctions(buttonElem,isArchived,locationId) {
    const parentElem = buttonElem.parentElement;
    if (isArchived) {
        parentElem.innerHTML = `<button class="ms-4 btn btn-outline-primary" hx-get="/location-status" hx-include="#includeArchived" hx-swap="outerHTML transition:true" hx-vals="{&quot;status&quot;:&quot;SAVED&quot;,&quot;locationId&quot;:${locationId}}">Unarchive</button>`;
    } else {
        parentElem.innerHTML = `<button class="ms-4 btn btn-outline-danger" hx-get="/location-status" hx-include="#includeArchived" hx-swap="outerHTML transition:true" hx-vals="{&quot;status&quot;:&quot;ARCHIVED&quot;,&quot;locationId&quot;:${locationId}}">Archive</button>`;
    }
    htmx.process(parentElem);
}

function getMapHidden() {
    return document.getElementById('toggle-map-button').getAttribute('aria-expanded');
}

function updateTabShowMap(tabElemId,isHidden) {
    const tabElem = document.getElementById(tabElemId);
    const currentVals = JSON.parse(tabElem.getAttribute('hx-vals') || '{}');
    currentVals.mapHidden = isHidden;
    tabElem.setAttribute('hx-vals', JSON.stringify(currentVals));
    htmx.process(tabElem); // Re-process for HTMX
}

function updateTabHxVals(tabElemId,isStart,locationId) {
    const tabElem = document.getElementById(tabElemId);
    const currentVals = JSON.parse(tabElem.getAttribute('hx-vals') || '{}');
    if (isStart) {
        if (locationId == null) delete currentVals.startLocationId;
        else currentVals.startLocationId = locationId;
    } else {
        if (locationId == null) delete currentVals.endLocationId;
        else currentVals.endLocationId = locationId;
    }
    tabElem.setAttribute('hx-vals', JSON.stringify(currentVals));
    htmx.process(tabElem); // Re-process for HTMX
}

function tripToEndLocation(tabElemId,locationId) {
    const tabElem = document.getElementById(tabElemId);
    const isHidden = getMapHidden();
    const currentVals = JSON.parse(tabElem.getAttribute('hx-vals') || '{}');
    if (currentVals.startLocationId != null) {
        if (currentVals.startLocationId == locationId) {
            currentVals.startLocationId = currentVals.endLocationId;
        }
    }
    currentVals.endLocationId = locationId;
    currentVals.mapHidden = isHidden;
    tabElem.setAttribute('hx-vals', JSON.stringify(currentVals));
    htmx.process(tabElem); // Re-process for HTMX
    tabElem.click();
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
        if (inputValueElem.value.trim() == '') {
            htmx.process(inputValueElem);
            inputValueElem.value = match.dataset.value;
            inputValueElem.dispatchEvent(new Event('input'));
        }
    }
}