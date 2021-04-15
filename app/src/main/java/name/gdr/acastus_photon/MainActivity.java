package name.gdr.acastus_photon;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Author: Daniel Barnett
 */

/**
 * The type Main activity.
 */
public class MainActivity extends AppCompatActivity implements LocationAvailableListener {
    /**
     * The Labels.
     */
    private ArrayList<String> labels = new ArrayList<>();
    /**
     * The Recents.
     */
    private ArrayList<String> recents = new ArrayList<>();
    /**
     * The constant lookupList.
     */
    public ArrayList<ResultNode> lookupList = new ArrayList<>();
    /**
     * The Can nav.
     */
    private boolean canNav = false;

    /**
     * The Cur lat.
     */
    public double curLat;
    /**
     * The Cur lon.
     */
    public double curLon;

    private Double[] geoCoordinates;
    /**
     * The Results.
     */
    private GetResults results = null;
    /**
     * The Map time.
     */
    private Boolean mapTime = false;
    /**
     * If there is a search query waiting
     */
    private boolean searching = false;
    private Intent intent;
    private String action;
    private String type;
    protected SharedPreferences prefs;
    /**
     * The Search text.
     */
    EditText searchText;
    /**
     * The Results list.
     */
    ListView resultsList;
    /**
     * The Toolbar.
     */
    Toolbar toolbar;
    /**
     * The Make request.
     */
    protected MakeAPIRequest makeRequest;
    /**
     * The Geo location.
     */
    protected GeoLocation geoLocation;
    /**
     * The Use location.
     */
    public static Boolean useLocation;

    // must be <= 21 chars
    final String LOG_TAG = "acastus_photon.Main";

    protected static Context context;

    LocationManager locationManager;

    ImageButton mViewMapButton;

    /** if true the app is called as geo-picker so that select location returns
     * to caller insted of show in map */
    private boolean isPickGeo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        useLocation = prefs.getBoolean("use_location", true);
        if (prefs.getBoolean("app_theme", false)){
            setTheme(R.style.DarkTheme_NoActionBar);
        }
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        makeRequest = new MakeAPIRequest();
        context = getApplicationContext();
        setupLocationUse();
        getInputs();
        setupMapButton();
        updateRecentsList();
        startTimer();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    useLocation = false;
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (this.isPickGeo) {
            menu.findItem(R.id.cmd_cancel).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
            searchText.setText("");
            return true;
        }
        if (id == R.id.action_share_location) {
            shareLocation();
            return true;
        }
        if (id == R.id.clear_recents) {
            clearRecents();
            return true;
        }
        if (id == R.id.donate) {
            Intent donateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://danielbarnett714.github.io/Acastus/"));
            startActivity(donateIntent);

            return true;
        }
        if (id == R.id.cmd_cancel) {
            onPickGeoCancel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setupMapButton(){
        mViewMapButton = (ImageButton) findViewById(R.id.imageButton);

        mViewMapButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                geoCoordinates = geoLocation.getLocation();
                if (geoCoordinates != null) {
                    curLat = geoCoordinates[0];
                    curLon = geoCoordinates[1];
                    Intent intent = new Intent(MainActivity.this, ViewMapActivity.class);

                    intent.putExtra("latitude", curLat);
                    intent.putExtra("longitude", curLon);


                    JSONArray jsonArray = new JSONArray();

                    try{
                        for (int i = 0; i < lookupList.size(); i++){
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("name", lookupList.get(i).name);
                            jsonObject.put("lat", lookupList.get(i).lat);
                            jsonObject.put("lon", lookupList.get(i).lon);
                            jsonArray.put(jsonObject);
                        }

                        intent.putExtra("lookup_list", jsonArray.toString());

                    }catch (JSONException e){

                    }
                    startActivity(intent);
                }


            }
        });
    }

    public void onLocationAvailable() {
        ImageView gpsIcon = (ImageView) findViewById(R.id.gps_icon);
        gpsIcon.setVisibility(View.VISIBLE);
    }

    /**
     * Get inputs.
     */
    void setupLocationUse(){
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        geoLocation = new GeoLocation(locationManager, getApplicationContext(), this);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        if (useLocation && isLocationEnabled()) {
            geoCoordinates = geoLocation.getLocation();
            if (geoCoordinates != null) {
                curLat = geoCoordinates[0];
                curLon = geoCoordinates[1];
            }
        }
    }


    private void getInputs() {
        searchText = (EditText) findViewById(R.id.searchText);
        handleIntent();
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                startSearch();
            }
        });




    }

    /**
     * Clear recents.
     */
    private void clearRecents() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("recents");
        editor.apply();
        recents.clear();
        String[] data = recents.toArray(new String[recents.size()]);  // terms is a List<String>
        updateList(data);
        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Object o = resultsList.getItemAtPosition(position);
                EditText searchQuery = (EditText) findViewById(R.id.searchText);
                searchQuery.setText(o.toString());
                if (lookupList.isEmpty()) {
                    return;
                }
                ResultNode tempNode = lookupList.get(position);
                setRecents(tempNode.getRecentsLabel());
                searchText.setText("");

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getAdressUrl(tempNode.lat, tempNode.lon, null)));
                try {
                    startActivity(browserIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.server_url_default),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public static boolean isLocationEnabled() {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    /**
     * Share location from gps.
     */
    protected void shareLocation() {
        Double[] coordinates1 = null;
        geoLocation.updateLocation();
        try {
            coordinates1 = geoLocation.getLocation();
        } catch (NullPointerException e) {
        }
        Double[] coordinates = coordinates1;
        if (coordinates != null) {
            double lat = coordinates[0];
            double lon = coordinates[1];


            String uri = getAdressUrl(lat, lon, null);

            if (isPickGeo) {
                Intent openInMaps = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                setResult(Activity.RESULT_OK, openInMaps);
                finish();
            } else {
                String shareBody = getResources().getString(R.string.my_current_location) + ":\n" + uri;
                Intent sharingLocation = new Intent(android.content.Intent.ACTION_SEND);
                sharingLocation.setType("text/plain");
                sharingLocation.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.my_current_location));
                sharingLocation.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingLocation, getResources().getString(R.string.share_my_location)));
            }
        }
    }

    /**
     * pick geo.
     */
    protected void onPickGeoCancel() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    protected String getAdressUrl(double lat, double lon, String label) {
        boolean useGoogle = prefs.getBoolean("use_google", false) == true;
        String location;
        if (useGoogle && !isPickGeo) {
            location = "http://maps.google.com/maps?q=+" + lat + "+" + lon;
            return location;
        }else {
            // isPickGeo always use geo format.
            location = "geo:" + lat + "," + lon + "?q="+ lat + "+" + lon;
            if (label != null) {
                label = label.replace(" " , "+");
                label = label.replace("," , "+");
                label = label.replace("++" , "+");
                location += "("+label+")";
            }
            return location;
        }
    }

    /**
     * Handle intent.
     */
    private void handleIntent() {
        intent = getIntent();
        action = (intent == null) ? null : intent.getAction();
        if (action != null) {
            type = intent.getType();
            String schema = intent.getScheme();
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if ("text/plain".equals(type)) {
                    handleSendText(intent); // Handle text being sent
                }
            } else if (Intent.ACTION_VIEW.equals(action)) {
                handleActionView(intent);
            } else if ((Intent.ACTION_PICK.compareTo(action) == 0) && (schema != null) && ("geo".compareTo(schema) == 0)) {
                handlePickGeoInit();
            }
        }
    }

    /**
     * Pick Geo Initialisation
     */
    private void handlePickGeoInit() {
        isPickGeo = true;
        String title = intent.getStringExtra(Intent.EXTRA_TITLE);
        if (title == null) getString(R.string.pick_from_adress);
        setTitle(title);
    }

    /**
     * Reset time.
     */
    private void resetTime() {
        mapTime = true;
        canNav = false;
        EditText searchQuery = (EditText) findViewById(R.id.searchText);
        String urlString = searchQuery.getText().toString();
        results = null;
        results = new GetResults();
        results.execute(urlString);
    }

    /**
     * Start timer.
     */
    private void startTimer() {
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                mapTime = false;
                if (searching == true) {
                    resetTime();
                    searching = false;
                }
            }
        }, 0, 3334);
    }

    /**
     * Start search.
     */
    private void startSearch() {
        if (searchText.getText().toString().isEmpty()) {
            updateRecentsList();
            return;
        }
        if (mapTime == false) {
            resetTime();
        } else {
            searching = true;
        }
    }

    /**
     * Handle send text.
     *
     * @param intent the intent
     */
    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        sharedText = sharedText.replace("+", " ");
        sharedText.replace("," , " ");
        if (sharedText != null) {
            EditText searchQuery = (EditText) findViewById(R.id.searchText);
            searchQuery.setText(sharedText);
            startSearch();
        }
    }

    /**
     * Handle action view.
     *
     * @param intent the intent
     */
    void handleActionView(Intent intent) {
        try {
            URI uri = new URI(intent.getData().toString());
            String q = uri.getQuery();
            if (q != null) {
                EditText searchQuery = (EditText) findViewById(R.id.searchText);
                String addr = "";
                addr = addr.replace("&", " ");
                int indexLoc = q.indexOf("loc");
                if (indexLoc >= 3){
                    addr = q.substring(q.indexOf("=") + 1, indexLoc).replace("\n", " ");
                }else{
                    addr = q.substring(q.indexOf("=") + 1).replace("\n", " ");
                }
                addr = addr.replace("+", " ");
                searchQuery.setText(addr);
                startSearch();
            }
        } catch (URISyntaxException e) {
            // Probably ought to put something here
        }
    }

    void sharePlace(String shareBody) {
        Intent sharingLocation = new Intent(android.content.Intent.ACTION_SEND);
        sharingLocation.setType("text/plain");
        sharingLocation.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.shared_location));
        sharingLocation.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingLocation, getResources().getString(R.string.share_this_location)));
    }

    void copyToClipboard(String copyBody){
        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(copyBody);
        Toast.makeText(MainActivity.this, getResources().getString(R.string.copied_to_clipboard),
                Toast.LENGTH_LONG).show();
    }

    void openInNavApp(String geoCoords){
        try {
            Intent openInMaps = new Intent(Intent.ACTION_VIEW, Uri.parse(geoCoords));
            if (isPickGeo) {
                setResult(Activity.RESULT_OK, openInMaps);
                finish();
            } else {
                startActivity(openInMaps);
                searchText.setText("");
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.need_nav_app),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Set recents.
     *
     * @param name the name
     */
    public void setRecents(String name) {
        SharedPreferences.Editor editor = prefs.edit();
        if (prefs.getBoolean("store_recents", true) == false) {
            return;
        }
        if (recents.contains(name)) {
            recents.remove(name);
        }
        recents.add(0, name);
        JSONArray mJSONArray = new JSONArray(recents);
        editor.remove("recents");
        editor.apply();
        editor.putString("recents", mJSONArray.toString());
        editor.apply();
    }

    /**
     * Update list.
     *
     * @param data the data
     */
    private void updateList(String[] data) {
        ArrayAdapter<?> adapter = new ArrayAdapter<Object>(this, android.R.layout.simple_selectable_list_item, data);
        resultsList = (ListView) findViewById(R.id.resultsList);
        resultsList.setAdapter(adapter);
        resultsList.setClickable(true);
    }

    /**
     * Update results list.
     */
    private void updateResultsList() {
        if (searchText.getText().toString().isEmpty()) {
            updateRecentsList();
            return;
        }
        String[] data;
        data = labels.toArray(new String[labels.size()]);  // terms is a List<String>
        updateList(data);
        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (lookupList.isEmpty()) {
                    return;
                }
                ResultNode tempNode = lookupList.get(position);
                setRecents(tempNode.getRecentsLabel());
                EditText searchQuery = (EditText) findViewById(R.id.searchText);
                searchQuery.setText(tempNode.getRecentsLabel());
                String geoCoords = getAdressUrl(tempNode.lat, tempNode.lon, tempNode.getNavLabel());
                geoCoords = geoCoords.replace(' ', '+');
                openInNavApp(geoCoords);
            }
        });

        resultsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long id) {
                // TODO Auto-generated method stub
                if (lookupList.isEmpty()) {
                    return true;
                }

                CharSequence list_options[] = new CharSequence[] {getResources().getString(R.string.navigate), getResources().getString(R.string.share_this_location), getResources().getString(R.string.copy_address_place), getResources().getString(R.string.copy_gps)};

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Choose option");
                builder.setItems(list_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ResultNode tempNode = lookupList.get(position);
                        setRecents(tempNode.getRecentsLabel());
                        if (which == 0){
                            String geoCoords = getAdressUrl(tempNode.lat, tempNode.lon, tempNode.getNavLabel());
                            geoCoords = geoCoords.replace(' ', '+');
                            openInNavApp(geoCoords);
                        }

                        if (which == 1){
                            String shareBody = tempNode.getNavLabel() + "\n" + getAdressUrl(tempNode.lat, tempNode.lon, tempNode.getNavLabel());
                            sharePlace(shareBody);
                        }

                        if (which == 2){
                            String copyBody = tempNode.getNavLabel();
                            copyToClipboard(copyBody);
                        }

                        if (which == 3){
                            String copyBody = getAdressUrl(tempNode.lat, tempNode.lon, null);
                            copyToClipboard(copyBody);
                        }
                    }
                });
                builder.show();

                return true;
            }
        });
    }

    /**
     * Update recents list.
     */
    private void updateRecentsList() {
        String recentsStore = prefs.getString("recents", null);
        JSONArray mJSONArray = null;
        resultsList = (ListView) findViewById(R.id.resultsList);
        if (recentsStore != null) {
            try {
                mJSONArray = new JSONArray(recentsStore);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            recents = null;
            recents = new ArrayList<>();
            if (mJSONArray != null) {
                for (int i = 0; i < mJSONArray.length(); i++) {
                    try {
                        recents.add(mJSONArray.get(i).toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            String[] data;
            data = recents.toArray(new String[recents.size()]);  // terms is a List<String>
            updateList(data);
            resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    Object result = resultsList.getItemAtPosition(position);
                    EditText searchQuery = (EditText) findViewById(R.id.searchText);
                    searchQuery.setText(result.toString());
                }
            });

            resultsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position, long id) {
                    // TODO Auto-generated method stub

                    CharSequence list_options[] = new CharSequence[] {getResources().getString(R.string.search_address_place), getResources().getString(R.string.share_address_place), getResources().getString(R.string.copy_address_place)};

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Choose option");
                    builder.setItems(list_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String name = recents.get(position);
                            setRecents(name);
                            EditText searchQuery = (EditText) findViewById(R.id.searchText);

                            if (which == 0){
                                searchQuery.setText(name);
                            }

                            if (which == 1){
                                String shareBody = name;
                                sharePlace(shareBody);
                            }

                            if (which == 2){
                                String copyBody = name;
                                copyToClipboard(copyBody);
                            }

                        }
                    });
                    builder.show();

                    return true;
                }
            });
        } else {
            resultsList = (ListView) findViewById(R.id.resultsList);
            resultsList.clearChoices();
        }
    }

    /**
     * Fill lists.
     *
     * @param object the object
     * @throws JSONException the json exception
     */
    void fillLists(JSONObject object) throws JSONException {
        JSONArray array = object.getJSONArray("features");
        lookupList.clear();
        labels.clear();
        double lat, lon;
        Boolean kilometers = prefs.getBoolean("unit_length", false);

        for (int i = 0; i < array.length(); i++) {
            JSONObject initialArray = array.getJSONObject(i);
            JSONObject geometry = initialArray.getJSONObject("geometry");
            JSONObject properties = initialArray.getJSONObject("properties");
            JSONArray coordinates = geometry.getJSONArray("coordinates");
            lat = coordinates.getDouble(1);
            lon = coordinates.getDouble(0);
            String name = "(unnamed)";
            try {
                name = properties.getString("label");
            } catch (JSONException e) {
                try {
                    name = properties.getString("name");
                } catch (JSONException ex) {
                    name = "";
                }
            }
            ResultNode tempNode = new ResultNode();
            tempNode.lat = lat;
            tempNode.lon = lon;
            tempNode.name = name;
            try {
                tempNode.city = properties.getString("city");
            } catch (JSONException e) {
                tempNode.city = null;
            }
            try {
                tempNode.street = properties.getString("street");
            } catch (JSONException e) {
                tempNode.street = null;
            }
            try {
                tempNode.housenumber = properties.getString("housenumber");
            } catch (JSONException e) {
                tempNode.housenumber = null;
            }
            try {
                tempNode.type = properties.getString("osm_value");
            } catch (JSONException e) {
                tempNode.type = null;
            }

            if (useLocation) {
                tempNode.setDistance(curLat, curLon, geoLocation, kilometers);
            }

            lookupList.add(tempNode);
        }

        if (useLocation) {
            Collections.sort(lookupList, new Comparator<ResultNode>() {
                @Override
                public int compare(ResultNode a, ResultNode b) {
                    return (int)(1000*a.distance - 1000*b.distance);
                }
            });
        }

        for (ResultNode tempNode: lookupList) {
            String thisLabel = tempNode.getLabel(useLocation, kilometers, curLat, curLon, geoLocation);
            labels.add(thisLabel);
        }
    }

    /**
     * Fetch search results.
     *
     * @param searchQuery the search query
     * @throws IOException   the io exception
     * @throws JSONException the json exception
     */
    private void fetchSearchResults(String searchQuery) throws IOException, JSONException {
        JSONObject object = makeRequest.fetchSearchResults(searchQuery);
        fillLists(object);
    }

    /**
     * Set search query string.
     *
     * @param input the input
     * @return the string
     */
    private String setSearchQuery(String input) {
        Double[] coordinates;
        String serverAddress = prefs.getString("server_url", null);
        if (serverAddress == null) {
            serverAddress = getResources().getString(R.string.server_url_default);
        }
        if (isLocationEnabled()){
            useLocation = prefs.getBoolean("use_location", true);
        }
        else {
            useLocation = false;
        }
        if (useLocation) {
            coordinates = geoLocation.getLocation();
            if (coordinates != null) {
                curLat = coordinates[0];
                curLon = coordinates[1];
            }else {
                Log.i(LOG_TAG, "Geolocation not ready, not using in search");
                useLocation = false;
            }
        }
        String searchQuery;
        if (useLocation) {
            searchQuery = serverAddress + "?" + "lat=" + curLat + "&lon=" + curLon + "&location_bias_scale=20" + "&q=" + input;
        } else {
            searchQuery = serverAddress + "?q=" + input;
        }
        System.out.println(searchQuery);
        searchQuery = searchQuery.replace(' ', '+');

        return searchQuery;
    }

    private class GetResults extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            canNav = true;
            updateResultsList();
        }
        @Override
        protected String doInBackground(String... strings) {
            String searchQuery = setSearchQuery(strings[0]);
            if (searchQuery != null){
                try {
                    fetchSearchResults(searchQuery);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
