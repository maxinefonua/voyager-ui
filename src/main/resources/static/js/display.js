var airportStartMarkers = [];
var airportEndMarkers = [];

function removeMarkerAtIndex(markersArray, index) {
  if (index < 0 || index >= markersArray.length) {
    console.error(`Invalid index: ${index}`);
    return false;
  }
  const marker = markersArray[index];
  marker.remove();
  markersArray.splice(index, 1);
  return true;
}

function closeMarkerPopups() {
    airportStartMarkers.forEach((marker, index) => {
        if (marker.getPopup().isOpen()) marker.getPopup().remove();
    });
    airportEndMarkers.forEach((marker, index) => {
        if (marker.getPopup().isOpen()) marker.getPopup().remove();
    });
    if (routeAirportMarker) {
        routeAirportMarker.remove();
        routeAirportMarker = null;
    }
}

function reverseMarkers() {
    const tempMarker = startMarker;
    startMarker = endMarker;
    endMarker = tempMarker;

    const tempAirportMarkers = airportStartMarkers;
    airportStartMarkers = airportEndMarkers;
    airportEndMarkers = tempAirportMarkers;
}

function mapFitToMarkerToggleAirport(isStart,airportMarkerIndex) {
    event.preventDefault(); // prevents url mod
    closeMarkerPopups();
    if (isStart) {
        mapFit(airportStartMarkers,startMarker);
        airportStartMarkers[airportMarkerIndex].remove();
        airportStartMarkers[airportMarkerIndex].addTo(map);
        airportStartMarkers[airportMarkerIndex].togglePopup();
    } else {
        mapFit(airportEndMarkers,endMarker);
        airportEndMarkers[airportMarkerIndex].remove();
        airportEndMarkers[airportMarkerIndex].addTo(map);
        airportEndMarkers[airportMarkerIndex].togglePopup();
    }
}

function zoomToLocation(isStart,targetElemId,reviewElem) {
    event.preventDefault(); // prevents url mod
    closeMarkerPopups();
    const stringofBoundsArray = reviewElem.dataset.locationBounds;
    if (stringofBoundsArray) {
        const bounds = stringofBoundsArray.split(',').map(Number);
        if (isStart) {
            if (airportStartMarkers.length > 0) mapFit(airportStartMarkers,startMarker);
            else map.fitBounds(bounds);
        } else {
            if (airportEndMarkers.length > 0) mapFit(airportEndMarkers,endMarker);
            else map.fitBounds(bounds);
        }
    } else recenterMap();
    scrollElemToTarget(targetElemId);
}

function mapToCountry(aElem) {
    event.preventDefault(); // prevents url mod
    clearMarkers();
    const stringofBoundsArray = aElem.dataset.countryBounds;
    if (stringofBoundsArray) {
        const bounds = stringofBoundsArray.split(',').map(Number);
        map.fitBounds(bounds);
    } else recenterMap();
    scrollElemToHref(aElem);
}

function mapToLocation(aElem) {
    event.preventDefault(); // prevents url mod
    clearMarkers();
    const stringofBoundsArray = aElem.dataset.countryBounds;
    if (stringofBoundsArray) {
        const bounds = stringofBoundsArray.split(',').map(Number);
        map.fitBounds(bounds);
    } else recenterMap();
    const longitude = aElem.dataset.lng;
    const latitude = aElem.dataset.lat;
    if (lookupMarker) lookupMarker.remove();
    lookupMarker = new mapboxgl.Marker().setLngLat([longitude,latitude]).addTo(map);
}

function scrollElemToHref(aElem) {
    event.preventDefault();
    const targetElemId = aElem.getAttribute('href');
    const target = document.querySelector(targetElemId);
    const contentContainer = document.getElementById('scrollable-content');

    if (target && contentContainer) {
        const offset = target.offsetTop - contentContainer.offsetTop;
        contentContainer.scrollTo({
            top: offset,
            behavior: 'smooth'
        });
    }
}

function removeLocationFromMap(isStart) {
    if (isStart) clearStartMarkers();
    else clearEndMarkers();
    checkReverse(document.getElementById('reverse-button'),document.getElementById('start-input-value'),document.getElementById('end-input-value'));
    updateTabHxVals('trips-tab-link',isStart,null);
}

function mapFitTripScrollTo(targetElemId) {
    event.preventDefault();
    closeMarkerPopups();
    if (startMarker && endMarker) mapFitBothMarkers(startMarker,endMarker);
    else recenterMap();
    scrollElemToTarget(targetElemId);
}


function addLocationToMap(divElem,locationId,isStart,longitude,latitude,bounds,stringOfCodesArray) {
    const trimmedArray = stringOfCodesArray.replace(/[\[\]]/g, '');
    const airportCodes = trimmedArray.split(', '); // Split by comma and space
    const targetMarkers = isStart ? airportStartMarkers : airportEndMarkers;
    if (isStart) {
        if (startMarker) startMarker.remove();
        startMarker = new mapboxgl.Marker().setLngLat([longitude,latitude]).addTo(map);
    } else {
        if (endMarker) endMarker.remove();
        endMarker = new mapboxgl.Marker().setLngLat([longitude,latitude]).addTo(map);
    }

    targetMarkers.forEach(marker => marker.remove());
    targetMarkers.length = 0;
    const datalistElem = isStart ? document.getElementById('nearby-airports-start') : document.getElementById('nearby-airports-end');
    for (const code of airportCodes) {
        const matchOption = getMatchingAirportOption(datalistElem,code);
        if (matchOption) {
            const newMarker = addPlainAirportMarkerToMap(matchOption);
            if (isStart) targetMarkers.push(newMarker);
            else targetMarkers.push(newMarker);
        }
    }
    if (isStart) {
        airportStartMarkers = targetMarkers;
        if (airportStartMarkers.length > 0) mapFit(airportStartMarkers,startMarker);
        else map.fitBounds(bounds);
    } else {
        airportEndMarkers = targetMarkers;
        if (airportEndMarkers.length > 0) mapFit(airportEndMarkers,endMarker);
        else map.fitBounds(bounds);
    }
    divElem.removeAttribute('hx-on::after-settle');
    htmx.process(divElem);
    checkReverse(document.getElementById('reverse-button'),document.getElementById('start-input-value'),document.getElementById('end-input-value'));

    updateTabHxVals('trips-tab-link',isStart,locationId);
}

function toggleMap(buttonElem) {
    const scrollable = document.getElementById('scrollable-content');
    const reviewScrollable = document.getElementById('review-summary');
    const continentList = document.getElementById('continent-list');
    const scrollNavParent = document.getElementById('trips-scroll-nav').parentElement;
    if (buttonElem.getAttribute('aria-expanded') == 'false') {
        buttonElem.innerHTML = 'Show Map';
        if (scrollable) scrollable.style = 'height:auto;max-height:850px';
        if (reviewScrollable) reviewScrollable.style = 'height:auto;max-height:815px';
        if (scrollNavParent) scrollNavParent.classList.add('d-none');
        if (continentList) continentList.classList.remove('overflow-auto');
        updateTabShowMap('trips-tab-link',true);
    } else {
        buttonElem.innerHTML = 'Hide Map';
        if (scrollable) scrollable.style = 'height:auto;max-height:350px';
        if (reviewScrollable) reviewScrollable.style = 'height:auto;max-height:315px';
        if (scrollNavParent) scrollNavParent.classList.remove('d-none');
        if (continentList) continentList.classList.add('overflow-auto');
        updateTabShowMap('trips-tab-link',false);
    }
}

var routeAirportMarker = null;

function mapToAirportPopup(buttonElem) {
    event.preventDefault();
    closeMarkerPopups();
    if (routeAirportMarker) routeAirportMarker.remove();
    const latitude = buttonElem.dataset.airportLat;
    const longitude = buttonElem.dataset.airportLng;
    const markerMatch = findMarkerAt(latitude,longitude);
    if (markerMatch != null) {
        markerMatch.togglePopup();
        centerMapOnMarker(markerMatch);
    } else {
        routeAirportMarker = addRouteAirportMarkerToMap(buttonElem);
        routeAirportMarker.togglePopup();
        centerMapOnMarker(routeAirportMarker);
    }
}

function findMarkerAt(latitude,longitude) {
    var match = Array.from(airportStartMarkers)
        .find(marker => marker.getLngLat().lat == latitude && marker.getLngLat().lng == longitude);
    if (match != null) return match;
    match = Array.from(airportEndMarkers)
            .find(marker => marker.getLngLat().lat == latitude && marker.getLngLat().lng == longitude);
    return match;
}

function mapFit(targetMarkersArray,locationMarker) {
    const mapboxBounds = new mapboxgl.LngLatBounds().extend(locationMarker.getLngLat());
    for (const marker of targetMarkersArray) {
        mapboxBounds.extend(marker.getLngLat());
    }
    map.fitBounds(mapboxBounds, {padding: {top: 60, bottom: 30, left: 80, right: 80}});
}

function buildPopup(airportCode,airportName,airportCity,airportSubdivision,airportCountry) {
    return new mapboxgl.Popup({ offset: 16 }) // add popups
                         .setHTML(`<strong data-name='${airportCode}'>${airportCode}</strong> <i>${airportName}</i>
                           in ${airportCity}, ${airportSubdivision}
                            of ${airportCountry}`);
}

function buildMarkerElement() {
    const markerElement = document.createElement('div');
    markerElement.className = 'airport-marker';
    markerElement.innerHTML = `<svg display="block" height="41" width="27" viewBox="0 0 27 41" xmlns="http://www.w3.org/2000/svg">
                                   <!-- Shadow effect -->
                                   <g transform="translate(3.0, 29.0)" fill="#000000">
                                       <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="10.5" ry="5.25"/>
                                       <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="9.5" ry="4.77"/>
                                       <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="8.5" ry="4.3"/>
                                       <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="7.5" ry="3.82"/>
                                       <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="6.5" ry="3.34"/>
                                       <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="5.5" ry="2.86"/>
                                       <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="4.5" ry="2.39"/>
                                   </g>

                                   <!-- Main marker body -->
                                   <path fill="#3FB1CE" d="M27,13.5C27,19.07 20.25,27 14.75,34.5C14.02,35.5 12.98,35.5 12.25,34.5C6.75,27 0,19.22 0,13.5C0,6.04 6.04,0 13.5,0C20.96,0 27,6.04 27,13.5Z"/>

                                   <!-- Marker border -->
                                   <path opacity="0.25" fill="#000000" d="M13.5,0C6.04,0 0,6.04 0,13.5C0,19.22 6.75,27 12.25,34.5C13,35.52 14.02,35.5 14.75,34.5C20.25,27 27,19.07 27,13.5C27,6.04 20.96,0 13.5,0ZM13.5,1C20.42,1 26,6.58 26,13.5C26,15.9 24.5,19.18 22.22,22.74C19.95,26.3 16.71,30.14 13.94,33.91C13.74,34.18 13.61,34.32 13.5,34.44C13.39,34.32 13.26,34.18 13.06,33.91C10.28,30.13 7.41,26.31 5.02,22.77C2.62,19.23 1,15.95 1,13.5C1,6.58 6.58,1 13.5,1Z"/>

                                   <!-- Center dot -->
                                   <g transform="translate(5.5, 5.6) scale(1.5)">
                                       <circle fill="#FFFFFF" cx="5.5" cy="5.5" r="5.5"/>
                                   </g>
                                   <!-- plane dot -->
                                   <path fill="#3FB1CE"
                                         transform="scale(0.15) translate(93,35) rotate(45)"
                                         d="m34.228 12.148c1.368-15.768 10.112-16.112 11.312 0v16.428l34.36 17.136v5.78l-34.368-6.156v17.672l11.596 11.224v5.772l-17.12-5.708-17.12 5.732v-5.756l11.424-11.424v-17.512L.004 51.408v-5.692l34.228-17.048V12.176" />

                               </svg>`;
    return markerElement;
}

function addPlainAirportMarkerToMap(airportOptionElem) {
    var airportPopup = buildPopup(airportOptionElem.dataset.value,airportOptionElem.dataset.airport,
        airportOptionElem.dataset.city,airportOptionElem.dataset.subdivision,airportOptionElem.dataset.country);
    // Create a custom HTML element for the marker
    const markerElement = buildMarkerElement();

    // Create marker with custom element
    var airportMarker = new mapboxgl.Marker({
      element: markerElement
    })
        .setLngLat([airportOptionElem.dataset.longitude,airportOptionElem.dataset.latitude])
        .setPopup(airportPopup)
        .addTo(map);
    return airportMarker;
}

function addRouteAirportMarkerToMap(buttonElem) {
    var airportPopup = buildPopup(buttonElem.dataset.airportCode,buttonElem.dataset.airportName,
        buttonElem.dataset.airportCity,buttonElem.dataset.airportSubd, buttonElem.dataset.airportCountry);
    // Create a custom HTML element for the marker
    const markerElement = buildMarkerElement();

    // Create marker with custom element
    var airportMarker = new mapboxgl.Marker({
      element: markerElement
    })
        .setLngLat([buttonElem.dataset.airportLng,buttonElem.dataset.airportLat])
        .setPopup(airportPopup)
        .addTo(map);
    return airportMarker;
}

function addPlainAirportMarkerToMapOLD(airportOptionElem) {
    var airportPopup =  new mapboxgl.Popup({ offset: 16 }) // add popups
              .setHTML(`<strong data-name='${airportOptionElem.dataset.value}'>${airportOptionElem.dataset.value}</strong> <i>${airportOptionElem.dataset.airport}</i>
                in ${airportOptionElem.dataset.city}, ${airportOptionElem.dataset.subdivision}
                 of ${airportOptionElem.dataset.country}`);
    const el = document.createElement('div');
    el.className = 'airport-marker';
    el.innerHTML = `<svg display="block" height="41" width="27" viewBox="0 0 27 41" xmlns="http://www.w3.org/2000/svg">
                         <!-- Shadow effect -->
                         <g transform="translate(3.0, 29.0)" fill="#000000">
                             <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="10.5" ry="5.25"/>
                             <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="9.5" ry="4.77"/>
                             <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="8.5" ry="4.3"/>
                             <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="7.5" ry="3.82"/>
                             <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="6.5" ry="3.34"/>
                             <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="5.5" ry="2.86"/>
                             <ellipse opacity="0.04" cx="10.5" cy="5.8" rx="4.5" ry="2.39"/>
                         </g>
                         <!-- Main marker body -->
                         <path fill="#3FB1CE" d="M27,13.5C27,19.07 20.25,27 14.75,34.5C14.02,35.5 12.98,35.5 12.25,34.5C6.75,27 0,19.22 0,13.5C0,6.04 6.04,0 13.5,0C20.96,0 27,6.04 27,13.5Z"/>
                         <!-- Marker border -->
                         <path opacity="0.25" fill="#000000" d="M13.5,0C6.04,0 0,6.04 0,13.5C0,19.22 6.75,27 12.25,34.5C13,35.52 14.02,35.5 14.75,34.5C20.25,27 27,19.07 27,13.5C27,6.04 20.96,0 13.5,0ZM13.5,1C20.42,1 26,6.58 26,13.5C26,15.9 24.5,19.18 22.22,22.74C19.95,26.3 16.71,30.14 13.94,33.91C13.74,34.18 13.61,34.32 13.5,34.44C13.39,34.32 13.26,34.18 13.06,33.91C10.28,30.13 7.41,26.31 5.02,22.77C2.62,19.23 1,15.95 1,13.5C1,6.58 6.58,1 13.5,1Z"/>
                         <!-- Center dot -->
                         <g transform="translate(5.5, 5.6) scale(1.5)">
                             <circle fill="#FFFFFF" cx="5.5" cy="5.5" r="5.5"/>
                         </g>
                         <!-- plane dot -->
                         <path fill="#3FB1CE"
                               transform="scale(0.15) translate(93,35) rotate(45)"
                               d="m34.228 12.148c1.368-15.768 10.112-16.112 11.312 0v16.428l34.36 17.136v5.78l-34.368-6.156v17.672l11.596 11.224v5.772l-17.12-5.708-17.12 5.732v-5.756l11.424-11.424v-17.512L.004 51.408v-5.692l34.228-17.048V12.176" />
                     </svg>`;
    var airportMarker = new mapboxgl.Marker(el)
        .setLngLat([airportOptionElem.dataset.longitude,airportOptionElem.dataset.latitude])
        .setPopup(airportPopup)
        .addTo(map);
    return airportMarker;
}

document.addEventListener('DOMContentLoaded', sliderFunction());

function sliderFunction() {
    const minValue = parseFloat(minRange.min);
    const maxValue = parseFloat(minRange.max);
    let activeTooltip = null;
    let isDragging = false;
    const progress = document.querySelector('.progress');
    const minGap = (maxValue - minValue) * .1; // Minimum gap between handles
    const minExchRateTooltip = new bootstrap.Tooltip(minRange, {
        trigger: 'hover focus touch',
        placement: 'bottom',
        offset: [0,15],
        title: 'Minimum: ' + minRange.value,
        template: '<div class="tooltip min-exchange-tooltip" role="tooltip">' +
              '<div class="tooltip-arrow"></div>' +
              '<div class="tooltip-inner"></div></div>'
    });
    const maxExchRateTooltip = new bootstrap.Tooltip(maxRange, {
        trigger: 'hover focus touch',
        placement: 'bottom',
        offset: [0,15],
        title: 'Maximum: ' + maxRange.value,
        template: '<div class="tooltip max-exchange-tooltip" role="tooltip">' +
              '<div class="tooltip-arrow"></div>' +
              '<div class="tooltip-inner"></div></div>'
    });

    // Update tooltip text
    function updateTooltip(isMinimum,newValue) {
        // Set new title attribute
        const minVal = parseFloat(newValue).toLocaleString('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });

        // Update the tooltip instance
        if (isMinimum) {
            minExchRateTooltip.setContent({ '.tooltip-inner': 'Minimum ' + minVal });
        } else {
            maxExchRateTooltip.setContent({ '.tooltip-inner': 'Maximum ' + minVal });
        }
    }

    function updateSlider() {
        // Ensure min doesn't exceed max and vice versa
        if (parseFloat(maxRange.value) < parseFloat(minRange.value)) {
            maxRange.value = minRange.value;
        }
        if (parseFloat(minRange.value) > parseFloat(maxRange.value)) {
            minRange.value = maxRange.value;
        }

        // Update progress bar
        const max = parseFloat(minRange.max);
        const minValNum = parseFloat(minRange.value);
        const maxValNum = parseFloat(maxRange.value);

        progress.style.left = (minValNum / (max+minGap)) * 100 + '%';
        progress.style.right = 100 - ((maxValNum+minGap)/ (max+minGap)) * 100 + '%';
    }

    minRange.addEventListener('mouseleave',function() {
        isDragging = false;
    });

    minRange.addEventListener('mouseenter',function() {
        maxExchRateTooltip.hide();
    });

    maxRange.addEventListener('mouseenter',function() {
        minExchRateTooltip.hide();
    });

    document.addEventListener('touchstart', (e) => {
        const isMinRange = e.target === minRange || minRange.contains(e.target);
        const isMaxRange = e.target === maxRange || maxRange.contains(e.target);

        if (!isMinRange && !isMaxRange && activeTooltip) {
            activeTooltip.hide();
            activeTooltip = null;
            isDragging = false;
        }
    }, { passive: true });

    maxRange.addEventListener('touchstart', () => {
        minExchRateTooltip.hide();
        maxExchRateTooltip.show();
        activeTooltip = maxExchRateTooltip;
    }, { passive: true });

    minRange.addEventListener('touchstart', () => {
        maxExchRateTooltip.hide();
        minExchRateTooltip.show();
        activeTooltip = minExchRateTooltip;
    }, { passive: true });

    maxRange.addEventListener('touchend', () => {
        isDragging = false;
        activeTooltip.show();
    }, { passive: true });

    minRange.addEventListener('touchend', () => {
        isDragging = false;
        activeTooltip.show();
    }, { passive: true });

    minRange.addEventListener('input', function() {
        isDragging = true;
        const minVal = parseFloat(this.value);
        const maxVal = parseFloat(maxRange.value);

        if (minVal > maxVal - minGap) {
            this.value = maxVal - minGap;
        }
        updateSlider();
        updateTooltip(true,this.value);
    });

    maxRange.addEventListener('input', function() {
        isDragging = true;
        const minVal = parseFloat(minRange.value);
        const maxVal = parseFloat(this.value);

        if (maxVal < minVal + minGap) {
            this.value = minVal + minGap;
        }
        updateSlider();
        updateTooltip(false,this.value);
    });
    // Initialize
    updateTooltip(true,minRange.value);
    updateTooltip(false,maxRange.value);
    updateSlider();
}