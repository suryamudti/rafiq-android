# API Endpoints

## Aladhan — prayer times

Base: `https://api.aladhan.com/` (`data/.../remote/AladhanApi.kt`)
`GET v1/timings/{date}?latitude=&longitude=&method=20`
Default coords: Jakarta (-6.2088, 106.8456). Method 20 (KEMENAG).

## Metals.live — gold/silver spot

Base: `https://api.metals.live/` (`data/.../remote/MetalPriceApi.kt`)
`GET v1/spot/gold`, `GET v1/spot/silver`. Prices are USD/oz; converted to
per-gram by dividing by 31.1035 in `MetalPriceApi`. Empty response falls back
to hardcoded defaults (gold 65.0, silver 0.75 USD/g).

## Overpass — nearby mosques

Base: `https://overpass-api.de/api/` (`data/.../remote/OverpassApi.kt`)
`POST interpreter?data=<query>`. `fetchMosques(lat, lon, radius=5000)` returns
`List<OverpassElement>` (type, id, lat/lon or center, tags).
