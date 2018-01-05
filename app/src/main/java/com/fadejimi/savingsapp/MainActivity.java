package com.fadejimi.savingsapp;

import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fadejimi.savingsapp.model.Outlet;
import com.fadejimi.savingsapp.model.User;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private EditText outletNameEditText, addressEditText, classItemEditText,
            contactNameEditText, contactMobileEditText, qtyEditText;
    private Button saveButton;
    private TextView txtDetails;
    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authListener;


    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        /*NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);*/

        auth = FirebaseAuth.getInstance();

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user == null) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        View appView = findViewById(R.id.app_view);
        View contentView = appView.findViewById(R.id.content_view);

        txtDetails = (TextView) contentView.findViewById(R.id.txt_user);
        outletNameEditText = (EditText) contentView.findViewById(R.id.outlet_name);
        addressEditText = (EditText) contentView.findViewById(R.id.outlet_address);
        classItemEditText = (EditText) contentView.findViewById(R.id.class_item);
        contactNameEditText = (EditText) contentView.findViewById(R.id.contact_name);
        contactMobileEditText = (EditText) contentView.findViewById(R.id.contact_mobile);
        qtyEditText = (EditText) contentView.findViewById(R.id.qty);
        saveButton = (Button) contentView.findViewById(R.id.btn_save);

        mFirebaseInstance = FirebaseDatabase.getInstance();

        // get reference to the users node
        mFirebaseDatabase = mFirebaseInstance.getReference("users");

        // store app to 'app-title' node
        mFirebaseInstance.getReference("app_title").setValue("Realtime Database");

        // app title change listener
        mFirebaseInstance.getReference("app_title").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, "APP title updated");

                String appTitle = dataSnapshot.getValue(String.class);

                // update toolbar title
                getSupportActionBar().setTitle(appTitle);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to read appTitle value", databaseError.toException());
            }
        });
        saveButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveUser();
                    }
                }
        );

        toggleButton();
    }

    private void saveUser() {
        if (!isGooglePlayServicesAvailable()) {
            try {
                LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String bestProvider = locationManager.getBestProvider(criteria, true);
                Location location = locationManager.getLastKnownLocation(bestProvider);

                if (location == null) {
                    Toast.makeText(this, "The location is not available try again later and location is null", Toast.LENGTH_SHORT).show();
                    finish();
                }

                String outletName = outletNameEditText.getText().toString();
                String address = addressEditText.getText().toString();
                String classItem = classItemEditText.getText().toString();
                String contactName = contactNameEditText.getText().toString();
                String contactMobile = contactMobileEditText.getText().toString();
                String quantity = qtyEditText.getText().toString();
                String userId = auth.getCurrentUser().getUid();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                createOutlet(userId, outletName, address, classItem, contactName, contactMobile,
                        quantity, latitude, longitude);

            }
            catch(SecurityException ex) {
                Toast.makeText(this, "The location is not available try again later", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        return true;
    }

    private void toggleButton() {
        if (TextUtils.isEmpty(userId)) {
            saveButton.setText("Save");
        }
        else {
            saveButton.setText("Update");
        }
    }

    private void createOutlet(String uId, String outletName, String address, String classItem, String contactName,
                              String contactMobile, String quantity, double latitude, double longitude) {
        if (TextUtils.isEmpty(userId)) {
            userId = mFirebaseDatabase.push().getKey();
        }

        if (isValid(outletName, address, classItem, contactName, contactMobile, quantity)) {
            double qty = Double.parseDouble(quantity);
            Outlet outlet = new Outlet(uId, outletName, address, classItem, contactName,
                    contactMobile, qty, latitude, longitude);

            mFirebaseDatabase.child(userId).setValue(outlet);

            addUserChangeListener();
        }

    }

    private boolean isValid(String outletName, String address, String classItem, String contactName, String contactMobile,
                            String qty) {
        StringBuilder stringBuilder = new StringBuilder();
        if (outletName.length() == 0) {
            stringBuilder.append("Please fill in the outlet name");
        }
        if (address.length() == 0) {
            stringBuilder.append("Please fill in the address");
        }
        if (classItem.length() == 0) {
            stringBuilder.append("Please fill in the class item");
        }
        if (contactName.length() == 0) {
            stringBuilder.append("Please fill in the contact name");
        }
        if (contactMobile.length() == 0) {
            stringBuilder.append("Please fill in the contact mobile");
        }
        if (qty.length() == 0) {
            stringBuilder.append("Please fill in the quantity");
        }
        if (!isDouble(qty)) {
            stringBuilder.append("The quantity must be a decimal number");
        }

        String error = stringBuilder.toString();

        if (error.length() == 0) {
            return true;
        }
        else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Input Error");
            alertDialogBuilder.setMessage(error).setCancelable(true);
            AlertDialog alertDialog = alertDialogBuilder.create();

            alertDialog.show();

            return false;
        }
    }

    private boolean isDouble(String qty) {
        try {
            Double.parseDouble(qty);
            return true;
        }
        catch(Exception ex) {
            return false;
        }
    }
    private void createUser(String name, String email) {
        // TODO
        // In real apps userId should be fetched by using firebase Auth
        if (TextUtils.isEmpty(userId)) {
            userId = mFirebaseDatabase.push().getKey();
        }

        User user = new User(name, email);

        mFirebaseDatabase.child(userId).setValue(user);

        addUserChangeListener();
    }

    private void addUserChangeListener() {
        mFirebaseDatabase.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Outlet outlet = dataSnapshot.getValue(Outlet.class);

                // check for null
                if (outlet == null) {
                    Log.e(TAG, "Outlet data is null");
                    return;
                }

                Log.e(TAG, "Outlet data is changed! " + outlet.latitude + ", " + outlet.longitude);

                // Display newly updated name and email
                txtDetails.setText(outlet.outletName);

                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("latitude", outlet.latitude);
                intent.putExtra("longitude", outlet.longitude);
                startActivity(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to read user", databaseError.toException());
            }
        });
    }

    private void updateUser(String name, String email) {
        // updating the user via child nodes
        if (!TextUtils.isEmpty(name)) {
            mFirebaseDatabase.child(userId).child("name").setValue(name);
        }
        if (!TextUtils.isEmpty(email)) {
            mFirebaseDatabase.child(userId).child("email").setValue(email);
        }
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
