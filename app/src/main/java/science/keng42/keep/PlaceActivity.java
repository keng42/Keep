package science.keng42.keep;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import science.keng42.keep.bean.Location;
import science.keng42.keep.dao.LocationDao;
import science.keng42.keep.google.PlaceAutocompleteAdapter;

public class PlaceActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_PLACE_PICKER = 10010;
    // view
    private EditText mEtName;
    private AutoCompleteTextView mActvDescription;
    private Button mBtnPick;

    private LocationDao mLocationDao;
    private Location mLocation;

    /**
     * for AutoComplete
     */
    private GoogleApiClient mGoogleApiClient;
    private PlaceAutocompleteAdapter mAdapter;
    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));
    private AdapterView.OnItemClickListener mAutocompleteClickListener;
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place);


        initView();
        initData();
        setListener();
        initAutoComplete();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mEtName = (EditText) findViewById(R.id.et_place_name);
        mActvDescription = (AutoCompleteTextView) findViewById(R.id.autocomplete_places);
        mBtnPick = (Button) findViewById(R.id.btn_pick_a_place);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // 向上按钮
            actionBar.setTitle("");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.primary_dark));
        }
    }

    /**
     * 初始化数据
     */
    private void initData() {
        mLocationDao = new LocationDao(this);
        Intent intent = getIntent();
        long id = intent.getLongExtra("locationId", 0);
        if (id == 0) {
            // new location
            mLocation = new Location(0, "", "", "", "", "");
            return;
        }
        mLocation = mLocationDao.query(id);

        mEtName.setText(mLocation.getTitle());
        mActvDescription.setText(mLocation.getDescription());
        Log.i("1984", "initData");
    }

    /**
     * 设置监听器
     */
    private void setListener() {
        mBtnPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(
                        getApplicationContext());
                if (status != ConnectionResult.SUCCESS) {
                    Toast.makeText(getApplicationContext(), "Google play " +
                            "services not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    PlacePicker.IntentBuilder intentBuilder =
                            new PlacePicker.IntentBuilder();
                    Intent intent = intentBuilder.build(getApplicationContext());
                    startActivityForResult(intent, REQUEST_PLACE_PICKER);
                } catch (Exception e) {
                    Log.e(MyApp.TAG, "", e);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_place, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.save) {
            saveAndExit();
            return true;
        }
        if (id == android.R.id.home) {
            exitWithoutSave();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 保存并退出
     */
    private void saveAndExit() {
        mLocation.setTitle(mEtName.getText().toString());
        mLocation.setDescription(mActvDescription.getText().toString());
        if (mLocation.getTitle().equals("")) {
            mEtName.setError(getResources().getString(R.string.enter_a_place_name));
            return;
        }
        long id = mLocationDao.getIdByTitle(mLocation.getTitle());
        if (id != 0 && id != mLocation.getId()) {
            mEtName.setError(getResources().getString(R.string.place_exists));
            return;
        }
        if (mLocation.getId() == 0) {
            mLocationDao.insert(mLocation);
        } else {
            mLocationDao.update(mLocation);
        }
        Intent intent = new Intent();
        intent.putExtra("locationId", mLocation.getId());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 退出前询问是否保存
     */
    private void exitWithoutSave() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        CharSequence[] items = new String[]{getResources().getString(R.string.save),
                getResources().getString(R.string.exit)};
        builder.setTitle(R.string.exit_without_save)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            saveAndExit();
                        } else {
                            finish();
                        }
                    }
                }).create().show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exitWithoutSave();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PLACE_PICKER && resultCode == Activity.RESULT_OK) {
            final Place place = PlacePicker.getPlace(data, this);
            mLocation.setDescription(place.getName().toString());
            mLocation.setAddress(place.getAddress().toString());
            LatLng latLng = place.getLatLng();
            mLocation.setLat("" + latLng.latitude);
            mLocation.setLon("" + latLng.longitude);

            if (mEtName.getText().toString().equals("")) {
                mLocation.setTitle(mLocation.getDescription());
                mEtName.setText(mLocation.getTitle());
            } else {
                mLocation.setTitle(mEtName.getText().toString());
            }
            mActvDescription.setText(mLocation.getDescription());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * for AutoComplete
     */
    private void initAutoComplete() {
        initListenerAndCallBack();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addApi(Places.GEO_DATA_API)
                .build();

        mActvDescription.setOnItemClickListener(mAutocompleteClickListener);

        mAdapter = new PlaceAutocompleteAdapter(this, android.R.layout.simple_list_item_1,
                mGoogleApiClient, BOUNDS_GREATER_SYDNEY, null);
        mActvDescription.setAdapter(mAdapter);
    }

    /**
     * AutoComplete listener and callback
     */
    private void initListenerAndCallBack() {
        mAutocompleteClickListener
                = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final PlaceAutocompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }
        };

        mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(PlaceBuffer places) {
                if (!places.getStatus().isSuccess()) {
                    // Request did not complete successfully
                    places.release();
                    return;
                }
                // Get the Place object from the buffer.
                final Place place = places.get(0);
                setPlaceDetail(place);
                places.release();
            }
        };
    }

    /**
     * 保存位置信息到全局变量
     *
     * @param place 相关位置对象
     */
    private void setPlaceDetail(Place place) {
        mLocation.setDescription(place.getName().toString());
        mLocation.setAddress(place.getAddress().toString());
        LatLng latLng = place.getLatLng();
        mLocation.setLat("" + latLng.latitude);
        mLocation.setLon("" + latLng.longitude);

        // 位置名为空时才填充默认位置名
        if (mEtName.getText().toString().equals("")) {
            mLocation.setTitle(mLocation.getDescription());
            mEtName.setText(mLocation.getTitle());
        } else {
            mLocation.setTitle(mEtName.getText().toString());
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("1984", "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }
}
