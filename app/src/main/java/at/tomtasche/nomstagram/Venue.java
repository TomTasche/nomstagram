package at.tomtasche.nomstagram;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tom on 26.07.14.
 */
public class Venue {

    private final String id;

    private String name;
    private final List<String> photoUrls;

    public Venue(String id) {
        this.id = id;

        photoUrls = new ArrayList<String>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Venue venue = (Venue) o;

        if (!id.equals(venue.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void addPhotoUrl(String url) {
        photoUrls.add(url);
    }

    public void addPhotoUrls(List<String> urls) {
        photoUrls.addAll(urls);
    }
}
