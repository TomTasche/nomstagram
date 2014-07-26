package at.tomtasche.nomstagram;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.etsy.android.grid.StaggeredGridView;

import java.util.Collection;

/**
 * Created by tom on 26.07.14.
 */
public class VenueGridFragment extends Fragment implements
        AbsListView.OnScrollListener, AbsListView.OnItemClickListener, VenueProvider.VenueLoadCallback {

    private StaggeredGridView mGridView = null;
    private VenueAdapter mAdapter = null;

    private Collection<Venue> mData = null;

    private boolean mHasRequestedMore = false;
    private int offset;
    private VenueProvider venueProvider;

    public VenueGridFragment() {
        offset = 0;
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

        if (!(getActivity() instanceof  VenueProvider)) {
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

            View header = layoutInflater.inflate(R.layout.list_item_header_footer, null);
            View footer = layoutInflater.inflate(R.layout.list_item_header_footer, null);
            TextView txtHeaderTitle = (TextView) header.findViewById(R.id.txt_title);
            TextView txtFooterTitle = (TextView) footer.findViewById(R.id.txt_title);

            txtHeaderTitle.setText("food near you");
            txtFooterTitle.setText("still hungry? loading more...");

            mGridView.addHeaderView(header);
            mGridView.addFooterView(footer);
        }

        if (mAdapter == null) {
            mAdapter = new VenueAdapter(getActivity(), R.id.image);
        }

        if (mData == null) {
            venueProvider.loadVenues(this, offset);

            mHasRequestedMore = true;
        }

        mGridView.setAdapter(mAdapter);
        mGridView.setOnScrollListener(this);
        mGridView.setOnItemClickListener(this);
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
        Toast.makeText(getActivity(), "Item Clicked: " + position, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVenuesLoaded(Collection<Venue> venues) {
        mData = venues;

        mAdapter.clear();

        mAdapter.addAll(venues);

        mAdapter.notifyDataSetChanged();

        mHasRequestedMore = false;
    }
}