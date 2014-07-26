package at.tomtasche.nomstagram;

import java.util.List;

/**
 * Created by tom on 26.07.14.
 */
public interface VenueProvider {

    public void loadVenues(VenueLoadCallback callback);

    public void loadVenues(VenueLoadCallback callback, int offset);

    public interface VenueLoadCallback {

        public void onVenuesLoaded(List<Venue> venues);
    }
}
