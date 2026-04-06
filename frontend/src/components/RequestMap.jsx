import React, { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMapEvents, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Fix for default marker icons in React Leaflet
delete L.Icon.Default.prototype._getIconUrl;

L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// Helper component to dynamically re-center map
const MapUpdater = ({ center }) => {
  const map = useMap();
  useEffect(() => {
    if (center) {
      map.flyTo(center, 13);
    }
  }, [center, map]);
  return null;
};

// Interactive Map Click Handler
const LocationSelector = ({ onLocationSelect }) => {
  useMapEvents({
    click(e) {
      if (onLocationSelect) onLocationSelect(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
};

const RequestMap = ({ requests, currentUserId, onAccept, onLocationSelect, selectedLocation }) => {
  const defaultCenter = selectedLocation ? [selectedLocation.lat, selectedLocation.lng] : [40.7128, -74.0060];

  const handleUseCurrentLocation = () => {
    if (!onLocationSelect) return;
    if (!navigator.geolocation) {
      alert('Geolocation is not supported in this browser.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        onLocationSelect(position.coords.latitude, position.coords.longitude);
      },
      () => {
        alert('Unable to fetch your current location. Please allow location access.');
      },
      {
        enableHighAccuracy: true,
        timeout: 8000,
      }
    );
  };

  return (
    <div style={{ height: '300px', width: '100%', borderRadius: '8px', overflow: 'hidden' }}>
      {onLocationSelect && (
        <div className="d-flex justify-content-end mb-2">
          <button
            type="button"
            className="btn btn-sm btn-outline-info"
            onClick={handleUseCurrentLocation}
          >
            Use My Current Location
          </button>
        </div>
      )}
      <MapContainer center={defaultCenter} zoom={13} style={{ height: '100%', width: '100%' }}>
        <MapUpdater center={defaultCenter} />
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        />
        
        {/* Render Location Picker if active */}
        {onLocationSelect && <LocationSelector onLocationSelect={onLocationSelect} />}
        {selectedLocation && (
          <Marker position={[selectedLocation.lat, selectedLocation.lng]}>
             <Popup>Target Location</Popup>
          </Marker>
        )}

        {/* Existing Requests */}
        {requests && requests.map(req => (
          req.latitude && req.longitude ? (
            <Marker key={req.id} position={[req.latitude, req.longitude]}>
              <Popup>
                <div>
                  <h6 className="mb-1 fw-bold">{req.title}</h6>
                  <p className="mb-2 small">{req.description}</p>
                  <span className={`badge mb-3 ${
                    req.status === 'COMPLETED' ? 'bg-success' :
                    req.status === 'IN_PROGRESS' ? 'bg-info' : 'bg-warning'
                  }`}>
                    {req.status}
                  </span>
                  
                  {req.status === 'OPEN' && req.author?.id !== currentUserId && (
                    <div className="mt-2 w-100">
                      <button onClick={() => onAccept(req.id)} className="btn btn-sm btn-info text-white w-100 rounded-pill">
                        Volunteer
                      </button>
                    </div>
                  )}
                  {req.author?.id === currentUserId && (
                     <div className="mt-2 text-muted small fst-italic w-100 text-center">Your Request</div>
                  )}
                </div>
              </Popup>
            </Marker>
          ) : null
        ))}
      </MapContainer>
    </div>
  );
};

export default RequestMap;
