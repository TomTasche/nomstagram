package at.tomtasche.nomstagram;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;

import java.util.List;

/**
 * Created by tom on 26.07.14.
 */
public class VenueActivity extends Activity {

    public static final String FOURSQUARE_URL_BASE = "https://foursquare.com/";
    public static final String FOURSQUARE_PATH_VENUE = "v/";

    public static final String EXTRA_VENUE_ID = "venue_id";
    public static final String EXTRA_VENUE_NAME = "venue_name";
    public static final String EXTRA_VENUE_PHOTO_URLS = "venue_photo_urls";

    private String venueId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String venueName = getIntent().getStringExtra(EXTRA_VENUE_NAME);
        setTitle(venueName);

        venueId = getIntent().getStringExtra(EXTRA_VENUE_ID);

        List<String> photoUrls = getIntent().getStringArrayListExtra(EXTRA_VENUE_PHOTO_URLS);

        setContentView(R.layout.activity_venue);

        SliderLayout spiderSlider = (SliderLayout) findViewById(R.id.slider);

        for (String url : photoUrls) {
            TextSliderView textSliderView = new TextSliderView(this);
            textSliderView
                    .image(url)
                    .setScaleType(BaseSliderView.ScaleType.FitCenterCrop);

            spiderSlider.addSlider(textSliderView);
        }

        spiderSlider.setPresetTransformer(SliderLayout.Transformer.Stack);
        spiderSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        spiderSlider.setDuration(5000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_venue, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_go:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(FOURSQUARE_URL_BASE + FOURSQUARE_PATH_VENUE + venueId));
                startActivity(intent);

                break;
        }

        return super.onMenuItemSelected(featureId, item);
    }
}