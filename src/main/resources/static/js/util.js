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

function tripToEndLocation(tabElemId,locationId) {
    const tabElem = document.getElementById(tabElemId);
    const values = {
      endLocationId: locationId
    };
    tabElem.setAttribute('hx-vals', JSON.stringify(values));
    htmx.process(tabElem); // Re-process for HTMX
    tabElem.click();
    tabElem.removeAttribute('hx-vals');
}

function dispatchInputEnd(inputValueElemId,aElem) {
    const inputValueElem = document.getElementById(inputValueElemId);
    const inputTextElem = document.getElementById('end-input-text');
    const locationSourceId = aElem.getAttribute('')
    if (inputTextElem.value.trim() != '') {
        const datalistElem = document.getElementById('input-end-options');
        const match = Array.from(datalistElem.options)
            .find(option => option.value === inputTextElem.value);
        if (inputValueElem.value.trim() == '') {
            htmx.process(inputValueElem);
            inputValueElem.value = match.dataset.value;
            inputValueElem.dispatchEvent(new Event('input'));
        }
    }
}