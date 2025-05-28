function postPinAirport(inputElemId){
    const inputElem = document.getElementById(inputElemId);
    if (inputElem && inputElem.value && inputElem.value.length > 0) {
        inputElem.value = '';
    }
}