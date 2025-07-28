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
