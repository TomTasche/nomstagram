package at.tomtasche.nomstagram;


import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.etsy.android.grid.util.DynamicHeightImageView;
import com.squareup.picasso.Picasso;

import java.util.Collection;
import java.util.Random;

import at.tomtasche.nomstagram.engine.foursquareApi.model.Venue;

/**
 * ADAPTER
 */

public class VenueAdapter extends ArrayAdapter<Venue> {

    static class ViewHolder {
        DynamicHeightImageView image;
        TextView textName;
        ImageView imageSource;
    }

    private final LayoutInflater mLayoutInflater;
    private final Random mRandom;

    private static final SparseArray<Double> sPositionHeightRatios = new SparseArray<Double>();

    public VenueAdapter(final Context context, final int textViewResourceId) {
        super(context, textViewResourceId);

        mLayoutInflater = LayoutInflater.from(context);
        mRandom = new Random();
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_item_venue, parent, false);
            vh = new ViewHolder();
            vh.image = (DynamicHeightImageView) convertView.findViewById(R.id.image);
            vh.textName = (TextView) convertView.findViewById(R.id.text_name);
            vh.imageSource = (ImageView) convertView.findViewById(R.id.image_source);

            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        final Venue venue = getItem(position);

        vh.textName.setText(venue.getName());

        if (venue.getSource().equals("foursquare")) {
            vh.imageSource.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_foursquare));
        }

        double positionHeight = getPositionRatio(position);
        vh.image.setHeightRatio(positionHeight);

        Collection<String> photoUrls = venue.getPhotoUrls();
        if (photoUrls.size() > 0) {
            Picasso.with(getContext()).load(photoUrls.iterator().next()).into(vh.image);
        } else {
            Log.d("nomstagram", "venue with id " + venue.getId() + " has no photoUrls");

            Picasso.with(getContext()).load(R.drawable.ic_launcher).into(vh.image);
        }

        return convertView;
    }

    private double getPositionRatio(final int position) {
        double ratio = sPositionHeightRatios.get(position, 0.0);

        // TODO: i think i should actually implement this, but setting "scaleType=centerCrop" on the DynamicHeightImageView fixed the display of images for me right now
        // if not yet done generate and stash the columns height
        // in our real world scenario this will be determined by
        // some match based on the known height and width of the image
        // and maybe a helpful way to get the column height!
        if (ratio == 0) {
            ratio = getRandomHeightRatio();
            sPositionHeightRatios.append(position, ratio);
        }
        return ratio;
    }

    private double getRandomHeightRatio() {
        return (mRandom.nextDouble() / 2.0) + 1.0; // height will be 1.0 - 1.5 the width
    }
}