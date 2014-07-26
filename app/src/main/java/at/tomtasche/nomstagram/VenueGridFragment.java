package at.tomtasche.nomstagram;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import com.etsy.android.grid.StaggeredGridView;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Created by tom on 26.07.14.
 */
public class VenueGridFragment extends Fragment implements
        AbsListView.OnScrollListener, AbsListView.OnItemClickListener, VenueProvider.VenueLoadCallback {

    private StaggeredGridView mGridView = null;
    private VenueAdapter mAdapter = null;

    private Collection<Venue> mData;

    private boolean mHasRequestedMore = false;
    private int offset;
    private VenueProvider venueProvider;

    public VenueGridFragment() {
        offset = 0;
        mData = new HashSet<Venue>();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_venue, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(getActivity() instanceof VenueProvider)) {
            throw new IllegalArgumentException("activity has to implement VenueProvider");
        }

        venueProvider = (VenueProvider) getActivity();
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mGridView = (StaggeredGridView) getView().findViewById(R.id.grid_view);

        if (savedInstanceState == null) {
            final LayoutInflater layoutInflater = getActivity().getLayoutInflater();

            View footer = layoutInflater.inflate(R.layout.list_item_header_footer, null);
            TextView txtFooterTitle = (TextView) footer.findViewById(R.id.txt_title);

            txtFooterTitle.setText("still hungry? loading more...");

            mGridView.addFooterView(footer);

            mGridView.setEmptyView(getView().findViewById(android.R.id.empty));
        }

        mGridView.setOnScrollListener(this);
        mGridView.setOnItemClickListener(this);

        if (mAdapter == null) {
            mAdapter = new VenueAdapter(getActivity(), R.id.image);
        }

        mGridView.setAdapter(mAdapter);

        if (mData.size() == 0) {
            venueProvider.loadVenues(this, offset);

            mHasRequestedMore = true;
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
    }

    @Override
    public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
        if (!mHasRequestedMore) {
            int lastInScreen = firstVisibleItem + visibleItemCount;
            if (lastInScreen >= totalItemCount) {
                mHasRequestedMore = true;

                venueProvider.loadVenues(this, ++offset);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Venue venue = mAdapter.getItem(position);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(VenueActivity.FOURSQUARE_URL_BASE + VenueActivity.FOURSQUARE_PATH_VENUE + venue.getId()));
        getActivity().startActivity(intent);
    }

    @Override
    public void onVenuesLoaded(List<Venue> venues) {
        for (Venue venue : venues) {
            boolean added = mData.add(venue);
            if (added) {
                mAdapter.add(venue);
            }
        }

        mAdapter.notifyDataSetChanged();

        mHasRequestedMore = false;
    }
}