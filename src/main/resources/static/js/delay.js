// ===== DEBOUNCE FUNCTION (REUSABLE) =====
function debounce(func, delay) {
    let timeoutId;
    return function(...args) {
        clearTimeout(timeoutId); // Cancel previous timeout
        timeoutId = setTimeout(() => {
            func.apply(this, args); // Run after delay
        }, delay);
    };
}

// Apply debounce (500ms delay)
const debouncedProcessLocationInput = debounce(processLocationInput, 500);
const debouncedProcessAirportInput = debounce(processAirportInput, 500);
