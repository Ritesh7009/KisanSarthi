import { MandiCenter } from '../types';

export interface UserCoordinates {
  lat: number;
  lng: number;
  accuracy?: number;
  source: 'GPS' | 'PRESET';
  label?: string;
}

export interface MandiWithDistance extends MandiCenter {
  distanceKm: number;
  distanceFormatted: string;
  travelEstimateMins: number;
}

/**
 * Calculate great-circle distance between two points on Earth using Haversine formula
 */
export function calculateHaversineDistanceKm(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371; // Earth's radius in km
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c * 10) / 10;
}

/**
 * Format distance in a human-friendly string (e.g. 850 m or 4.2 km)
 */
export function formatDistance(km: number): string {
  if (km < 1) {
    return `${Math.round(km * 1000)} m`;
  }
  return `${km.toFixed(1)} km`;
}

/**
 * Estimate tractor/tempo travel time given distance in km
 * Average rural road tractor speed approx 25 km/h + 5 mins buffer
 */
export function estimateTractorTravelMins(distanceKm: number): number {
  const travelMins = Math.round((distanceKm / 25) * 60) + 5;
  return Math.max(5, travelMins);
}

/**
 * Given a user's coordinate, compute distance to all Mandis and sort ascending
 */
export function sortMandisByDistance(
  coords: { lat: number; lng: number },
  mandis: MandiCenter[]
): MandiWithDistance[] {
  return mandis
    .map((m) => {
      const distanceKm = calculateHaversineDistanceKm(coords.lat, coords.lng, m.lat, m.lng);
      return {
        ...m,
        distanceKm,
        distanceFormatted: formatDistance(distanceKm),
        travelEstimateMins: estimateTractorTravelMins(distanceKm),
      };
    })
    .sort((a, b) => a.distanceKm - b.distanceKm);
}

/**
 * Find the single nearest Mandi center
 */
export function findNearestMandi(
  coords: { lat: number; lng: number },
  mandis: MandiCenter[]
): MandiWithDistance | null {
  if (!mandis.length) return null;
  const sorted = sortMandisByDistance(coords, mandis);
  return sorted[0] || null;
}

/**
 * Common MP Agricultural District coordinates for easy 1-click preset / fallback
 */
export const MP_LOCATION_PRESETS = [
  {
    name: 'Sehore Tehsil (Bilkisganj)',
    lat: 23.1842,
    lng: 77.0621,
    district: 'Sehore',
  },
  {
    name: 'Harda (Timarni Block)',
    lat: 22.3789,
    lng: 77.1422,
    district: 'Harda',
  },
  {
    name: 'Ujjain (Chimanganj Rural)',
    lat: 23.1895,
    lng: 75.7821,
    district: 'Ujjain',
  },
  {
    name: 'Bhopal Rural (Berasia)',
    lat: 23.6334,
    lng: 77.4325,
    district: 'Bhopal',
  },
  {
    name: 'Indore (Sanwer Tehsil)',
    lat: 22.9734,
    lng: 75.8271,
    district: 'Indore',
  },
  {
    name: 'Vidisha (Basoda Road)',
    lat: 23.5281,
    lng: 77.8124,
    district: 'Vidisha',
  },
];

/**
 * Request real device coordinates via browser Geolocation API
 */
export function getBrowserGeolocation(): Promise<{
  success: boolean;
  coords?: UserCoordinates;
  error?: string;
}> {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve({
        success: false,
        error: 'Geolocation is not supported by this browser.',
      });
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        resolve({
          success: true,
          coords: {
            lat: pos.coords.latitude,
            lng: pos.coords.longitude,
            accuracy: pos.coords.accuracy,
            source: 'GPS',
          },
        });
      },
      (err) => {
        let msg = 'Unable to retrieve your location.';
        if (err.code === err.PERMISSION_DENIED) {
          msg = 'Location permission was denied. Please allow location access in your browser.';
        } else if (err.code === err.POSITION_UNAVAILABLE) {
          msg = 'Location information is currently unavailable.';
        } else if (err.code === err.TIMEOUT) {
          msg = 'Location request timed out.';
        }
        resolve({
          success: false,
          error: msg,
        });
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000,
      }
    );
  });
}
