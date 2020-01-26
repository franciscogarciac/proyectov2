package com.example.taxis;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,NavigationView.OnNavigationItemSelectedListener {

    private int MY_PERMISSIONS_REQUEST_READ_CONTACTS;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mapa;
    private ArrayList<Marker> tmpRealTimeMarker = new ArrayList<>();
    private ArrayList<Marker> realTimeMarker = new ArrayList<>();
    private String usuario = "usuario10012";
    private int tiempo = 5000;
    Handler handler = new Handler();
    public NavigationView navView;
    DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UUID.randomUUID().toString();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.home);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        navView = (NavigationView)findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);
        inicializarFirebase();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        subirFirebaseLatLong();

        //Mapa
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //

        ejecutarTarea();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;

        mapa.getUiSettings().setZoomControlsEnabled(true);
        mapa.setMyLocationEnabled(true);

        CameraUpdate camUpd1 = CameraUpdateFactory.newLatLngZoom(new LatLng(-1.012606, -79.469107), 13);
        mapa.moveCamera(camUpd1);


        //databaseReference.child("Usuarios").addListenerForSingleValueEvent(new ValueEventListener() { //Solo lee los datos 1 vez

        //Este es para que siempre lea los datos
        databaseReference.child("Usuarios").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //Limpiar lista de marcadores reales
                for (Marker marker : realTimeMarker) {
                    marker.remove();
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Mapa mapaClase = snapshot.getValue(Mapa.class);

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(new LatLng(mapaClase.getLatitud(),  mapaClase.getLongitud()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi));

                    tmpRealTimeMarker.add(mapa.addMarker(markerOptions));
                }

                realTimeMarker.clear();
                realTimeMarker.addAll(tmpRealTimeMarker);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void inicializarFirebase() {
        FirebaseApp.initializeApp(this);
        firebaseDatabase = FirebaseDatabase.getInstance();

        databaseReference = firebaseDatabase.getReference();
    }

    private void ejecutarTarea()
    {
        handler.postDelayed(new Runnable() {
            public void run() {

                // función a ejecutar
                subirFirebaseLatLong(); // función para refrescar la ubicación del conductor

                handler.postDelayed(this, tiempo);
            }

        }, tiempo);
    }

    private void subirFirebaseLatLong() {

        //Pedir permisos
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        //


        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            //Log.e("Mensaje ","Latitud= "+location.getLatitude()+" Longitud= "+location.getLatitude());

                            Map<String, Object> LatLong = new HashMap<>();
                            LatLong.put("Latitud", location.getLatitude());
                            LatLong.put("Longitud", location.getLongitude());

                            //databaseReference.child("Usuarios").push().setValue(LatLong);
                            //databaseReference.child("Usuarios").child("usuario1001").setValue(LatLong);

                            databaseReference.child("Usuarios").child(usuario).updateChildren(LatLong);
                        }
                    }
                });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Fragment fragment = null;
        switch (menuItem.getItemId()) {
            case R.id.menu_actividad:
                fragment = new Registro_Actividad();

                Toast.makeText(this.getApplicationContext(),"Click Opcion 1", Toast.LENGTH_LONG).show();
                break;
            case R.id.menu_distancia:
                fragment = new Registro_Actividad();
                Toast.makeText(this.getApplicationContext(),"Click Opcion 2", Toast.LENGTH_LONG).show();
                break;
            case R.id.menu_ruta:
                fragment = new Registro_Actividad();
                Toast.makeText(this.getApplicationContext(),"Click Opcion 3", Toast.LENGTH_LONG).show();
                break;

        }
        if(fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.map, fragment)
                    .commit();
            menuItem.setChecked(true);
            getSupportActionBar().setTitle(menuItem.getTitle());
        }

        getSupportActionBar().setTitle(menuItem.getTitle());
        drawerLayout.closeDrawers();
        return false;
    }
}
