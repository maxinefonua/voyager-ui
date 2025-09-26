var map = new mapboxgl.Map({
    container: 'map',
    style: {
        version: 8,
            sources: {
                osm: {
                    type: 'raster',
                    tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
                    tileSize: 256
                }
            },
            layers: [{
                id: 'osm',
                type: 'raster',
                source: 'osm',
            }],
        },
    projection: 'globe',
    attributionControl: false
}).addControl(new mapboxgl.AttributionControl({
    customAttribution: [
        '&copy; <a target="_blank" rel="license noopener noreferrer" href="https://www.mapbox.com/about/maps">Mapbox</a>',
        '&copy; <a target="_blank" rel="license noopener noreferrer" href="https://osm.org/copyright">OpenStreetMap</a>'],
    compact: false
}));

const nav = new mapboxgl.NavigationControl({
  showCompass: false,
  visualizePitch: false
});

map.addControl(nav);

function getLocationElemByName(locationElementName) {
    const allLocations = document.getElementById('all-locations');
    if (allLocations && allLocations.options && allLocations.options.namedItem(locationElementName)) {
        return allLocations.options.namedItem(locationElementName);
    }
    return null;
}

function fitMapToElementBounds(elementWithBounds) {
    const bounds = elementWithBounds.dataset.bounds
                .split(',')
                .map(Number.parseFloat)
                .filter(n => !isNaN(n));
    map.fitBounds(bounds);
}


function fitMapToElemWithBounds(elem) {
    const bounds = [elem.dataset.boundsWest,elem.dataset.boundsSouth,elem.dataset.boundsEast,elem.dataset.boundsNorth];
    map.fitBounds(bounds);
}

function scrollElemToTarget(targetElemId) {
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

function recenterMap() {
    map.flyTo({
        center: [0, 0],
        zoom: 0
    });
}

function centerMapOnMarker(marker) {
    map.flyTo({
        center: marker.getLngLat(),
        zoom: 9
    });
}