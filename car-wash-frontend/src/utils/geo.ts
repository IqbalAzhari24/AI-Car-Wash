/** Promisified single-shot geolocation. */
export function getPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject(new Error('Geolocation is not supported by this browser.'));
      return;
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 10000 });
  });
}

/** Reverse-geocode via OSM Nominatim — free, no API key. Throws when no address is found. */
export async function reverseGeocode(lat: number, lng: number): Promise<string> {
  const res = await fetch(
    `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`,
    { headers: { Accept: 'application/json' } },
  );
  if (!res.ok) throw new Error('Reverse geocoding failed.');
  const data = await res.json();
  if (!data?.display_name) throw new Error('No address found for this location.');
  return data.display_name as string;
}
