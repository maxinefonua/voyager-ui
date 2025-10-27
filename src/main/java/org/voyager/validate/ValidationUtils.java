package org.voyager.validate;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.voyager.commons.model.airline.Airline;
import org.voyager.commons.model.AirportFilter;
import org.voyager.commons.model.airport.AirportType;
import org.voyager.commons.model.location.Status;

import java.util.Optional;

public class ValidationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationUtils.class);

    public static Optional<AirportType> resolveTypeOptional(Optional<String> typeOptional) {
        Optional<AirportType> airportType = Optional.empty();
        String type = typeOptional.orElse(null);
        if (StringUtils.isNotEmpty(type)) {
            try {
                airportType = Optional.of(AirportType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException e) {
                LOGGER.error(String.format("Cannot resolve given airport type: %s\nError message: %s",type,e.getMessage()),e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("Internal error occurred while resolving airport type: %s",type));
            }
        }
        return airportType;
    }

    public static Optional<Airline> resolveAirlineOptional(Optional<String> airlineOptional) {
        Optional<Airline> airline = Optional.empty();
        String airlineText = airlineOptional.orElse(null);
        if (StringUtils.isNotEmpty(airlineText)) {
            try {
                airline = Optional.of(Airline.valueOf(airlineText.toUpperCase()));
            } catch (IllegalArgumentException e) {
                LOGGER.error(String.format("Cannot resolve given airline: %s\nError message: %s",airlineText,e.getMessage()),e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("Internal error occurred while resolving airline: %s",airlineText));
            }
        }
        return airline;
    }

    public static AirportFilter getAirportFilterElseDefault(String filterString) {
        AirportFilter airportFilter = AirportFilter.ALL;
        if (StringUtils.isNotEmpty(filterString)) {
            try {
                airportFilter = AirportFilter.valueOf(filterString.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.error(String.format("Cannot resolve given airport filter: %s\nError message: %s",filterString,e.getMessage()),e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("Internal error occurred while resolving airport filter: %s",filterString));
            }
        }
        return airportFilter;
    }

    public static Status getLocationStatusElseDefault(String filterString) {
        Status status = Status.SAVED;
        if (StringUtils.isNotEmpty(filterString)) {
            try {
                status = Status.valueOf(filterString.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.error(String.format("Cannot resolve given airport filter: %s\nError message: %s",filterString,e.getMessage()),e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("Internal error occurred while resolving airport filter: %s",filterString));
            }
        }
        return status;
    }
}
