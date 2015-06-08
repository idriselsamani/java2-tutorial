package course.labs.locationlab;

import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class PlaceViewActivity extends ListActivity implements LocationListener {
	private static final long FIVE_MINS = 5 * 60 * 1000;
	private static final String TAG = "Lab-Location";

	// False if you don't have network access
	public static boolean sHasNetwork = false;

	private Location mLastLocationReading;
	private PlaceViewAdapter mAdapter;
	private LocationManager mLocationManager;
	private boolean mMockLocationOn = false;

	// default minimum time between new readings
	private long mMinTime = 5000;

	// default minimum distance between old and new readings.
	private float mMinDistance = 1000.0f;

	// A fake location provider used for testing
	private MockLocationProvider mMockLocationProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mAdapter = new PlaceViewAdapter(getApplicationContext());
		setListAdapter(mAdapter);

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		ListView placesListView = getListView();

		View footerView = getLayoutInflater().inflate(R.layout.footer_view, null);
		footerView.setEnabled(false);
		placesListView.addFooterView(footerView);

		footerView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				for (PlaceRecord place : mAdapter.getList()) {
					if (place.intersects(mLastLocationReading)) {
						Toast.makeText(PlaceViewActivity.this, "You already have this location badge.", Toast.LENGTH_SHORT).show();
						return;
					}
				}
				
				new PlaceDownloaderTask(PlaceViewActivity.this, false).execute(mLastLocationReading);                
			}

		});

		placesListView.addFooterView(footerView);
		mAdapter = new PlaceViewAdapter(getApplicationContext());
		setListAdapter(mAdapter);

	}

	@Override
	protected void onResume() {
		super.onResume();

		startMockLocationManager();

		// TODO - Check NETWORK_PROVIDER for an existing location reading.
		// Only keep this last reading if it is fresh - less than 5 minutes old
		if (mLastLocationReading != null && ageInMilliseconds(mLastLocationReading) > FIVE_MINS) {
			mLastLocationReading = null;
		}
        
        mLastLocationReading = null;


		// TODO - register to receive location updates from NETWORK_PROVIDER
        if (null != mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER)) {
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance, this);
		}
        
	}

	@Override
	protected void onPause() {

		// TODO - unregister for location updates
		mLocationManager.removeUpdates(this);
		shutdownMockLocationManager();
		super.onPause();
	}

	// Callback method used by PlaceDownloaderTask
	public void addNewPlace(PlaceRecord place) {
	
		// TODO - Attempt to add place to the adapter, considering the following cases

		if (mAdapter.getList().contains(place)) {
			Toast.makeText(this, "You already have this location badge.", Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (place == null) {
			Toast.makeText(this, "PlaceBadge could not be acquired", Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (place.getCountryName().isEmpty()) {
			Toast.makeText(this, "There is no country at this location", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mAdapter.add(place);
        
	}

	// LocationListener methods
	@Override
	public void onLocationChanged(Location currentLocation) {

		if (mLastLocationReading == null || mLastLocationReading.getTime() < currentLocation.getTime()){
			mLastLocationReading = currentLocation;
			findViewById(R.id.footer).setEnabled(true);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// not implemented
	}

	@Override
	public void onProviderEnabled(String provider) {
		// not implemented
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// not implemented
	}

	// Returns age of location in milliseconds
	private long ageInMilliseconds(Location location) {
		return System.currentTimeMillis() - location.getTime();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete_badges:
			mAdapter.removeAllViews();
			return true;
		case R.id.place_one:
			mMockLocationProvider.pushLocation(37.422, -122.084);
			return true;
		case R.id.place_no_country:
			mMockLocationProvider.pushLocation(0, 0);
			return true;
		case R.id.place_two:
			mMockLocationProvider.pushLocation(38.996667, -76.9275);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void shutdownMockLocationManager() {
		if (mMockLocationOn) {
			mMockLocationProvider.shutdown();
		}
	}

	private void startMockLocationManager() {
		if (!mMockLocationOn) {
			mMockLocationProvider = new MockLocationProvider(
					LocationManager.NETWORK_PROVIDER, this);
		}
	}
}
