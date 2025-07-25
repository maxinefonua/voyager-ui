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
