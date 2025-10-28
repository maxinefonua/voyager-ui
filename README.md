# <img src="/src/main/resources/static/images/logo.svg" width="30"> Voyager Web Application

### https://demo.voyagerapp.org
A personal project I took on as a referesher of full-cycle development, and to optimize my flying benefits as an airline employee. Built entirely on open-sourced data and frameworks.

### The Problem
Airline employees have interairline privileges that offer us standby seats on participating airlines at a discounted rate.

With the speed of this digital age, the commercial booking experience has swiftly outgrown our internal travel booking tools. We constantly switch back and forth between a separate search engine to find existing flight itineraries, and our internal employee platform where we must purchase the discounted seat.

### Minimum Viable Prototype
Employees can benefit from this search tool where:
- flight data consists only of participating airlines
- results can be filtered by airline preference
- nearby airports are recommended for widened search results
- itineraries are fully operated by a single airline in case of checked bags
- departure times for each leg is shown in case of standby rollovers

### Project Repos:
- Voyager UI: https://github.com/maxinefonua/voyager-ui
  - mapped requests and web feature functions
  - dynamic page injection and  targeted fragment reloads
- Voyager API: https://github.com/maxinefonua/voyager-api
    - standalone backend services
    - caching, request limits, auth tokens
- Voyager Commons: https://github.com/maxinefonua/voyager-commons
    - an SDK for API services
    - scripts and jars for data syncing
- Voyager Tests: https://github.com/maxinefonua/voyager-tests
  - functional tests built with JUnit 5
  - an uber jar deployed and used for application deployments


### Tech Stack:
- Geolocation Data
  - GeoNames - https://www.geonames.org.com/
  - OpenStreetMap - https://www.openstreetmap.org/

- Flight/Airport Data 
  - AirportsData - https://github.com/mborsetti/airportsdata/
  - FlightRadar24 - https://www.flightradar24.com/about 
  - OpenStreetMap - https://www.openstreetmap.org/

- Styling and Interaction 
  - Bootstrap v5.3.0 - https://getbootstrap.com/ 
  - Mapbox v1.13.3 - https://github.com/mapbox/mapbox-gl-js/ 
  - SVG Path Editor - https://yqnn.github.io/svg-path-editor/ 
  - Thymeleaf - https://www.thymeleaf.org/

- Development 
  - GitHub - https://github.com/
  - IntelliJ - https://www.jetbrains.com/idea/download/?section=mac 
  - Spring - https://spring.io/
  - HTMX - https://htmx.org/
  - PostgeSQL - https://www.postgresql.org/about/
  - pgAdmin - https://www.pgadmin.org/

Full README and LICENSE coming soon.
