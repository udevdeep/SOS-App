package com.example.maps_test_1

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.firebase.auth.FirebaseAuth

private val SMS_PERMISSION_CODE = 101

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mGoogleMap: GoogleMap? = null
    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var currentLocationMarker: Marker? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        setContentView(R.layout.activity_main)

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        // Set up menu button to open drawer
        val menuButton: ImageButton = findViewById(R.id.btn_menu)
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(navigationView)
        }

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_profile -> {

                }
                R.id.nav_econt -> {
                    val intent = Intent(this, EmergencyContactsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_settings -> {

                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }

            }
            drawerLayout.closeDrawers()
            true
        }

        // Initialize Google Places API
        Places.initialize(applicationContext, getString(R.string.google_map_api_key))
        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {
                Toast.makeText(this@MainActivity, "Error: ${p0.statusMessage}", Toast.LENGTH_SHORT).show()
            }

            override fun onPlaceSelected(place: Place) {
                val latLng = place.latLng
                zoomOnMap(latLng)
            }
        })

        // Initialize Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Map Options Menu
        val mapOptionsButton: ImageButton = findViewById(R.id.mapOptionsMenu)
        val popUpMenu = PopupMenu(this, mapOptionsButton)
        popUpMenu.menuInflater.inflate(R.menu.map_options, popUpMenu.menu)
        popUpMenu.setOnMenuItemClickListener { menuItem ->
            changeMap(menuItem.itemId)
            true
        }
        mapOptionsButton.setOnClickListener {
            popUpMenu.show()
        }

        // Initialize Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (locationResult.locations.isNotEmpty()) {
                    lastLocation = locationResult.lastLocation
                    val userLatLng = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
                    zoomOnMap(userLatLng)
                }
            }
        }
        // Set up SOS Button
        findViewById<Button>(R.id.btn_sos).setOnClickListener {
            if (lastLocation != null) {
                val emergencyMessage = "SOS! I'm in danger. My location: https://maps.google.com/?q=${lastLocation!!.latitude},${lastLocation!!.longitude}"

                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val userId = currentUser.uid  // Get the current user's UID
                val db = FirebaseFirestore.getInstance()

                db.collection("users").document(userId).collection("emergency_contacts")
                    .get()
                    .addOnSuccessListener { result ->
                        val phoneNumbers = mutableListOf<String>()

                        for (document in result) {
                            val phone = document.getString("phone")
                            phone?.let { phoneNumbers.add(it) }
                        }

                        if (phoneNumbers.isNotEmpty()) {
                            sendSMS(phoneNumbers, emergencyMessage)
                        } else {
                            Toast.makeText(this, "No emergency contacts found!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error retrieving contacts: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Unable to retrieve location. Try again later.", Toast.LENGTH_SHORT).show()
            }
        }


    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        mGoogleMap?.uiSettings?.isZoomControlsEnabled = true
        mGoogleMap?.setPadding(0, 2300, 50, 200)






        // Check for Location Permission
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mGoogleMap?.isMyLocationEnabled = true
        startLocationUpdates()
    }

    private fun sendSMS(phoneNumbers: List<String>, message: String) {
        // Check for SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1)
            Toast.makeText(this, "Permission required to send SMS", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val smsManager = SmsManager.getDefault()

            for (phone in phoneNumbers) {
                smsManager.sendTextMessage(phone, null, message, null, null)
            }

            Toast.makeText(this, "SOS message sent to all emergency contacts!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f) // Update every 10 meters
            .build()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }

    private fun zoomOnMap(latLng: LatLng) {
        val screenHeight = resources.displayMetrics.heightPixels
        val verticalOffset = (screenHeight * 0.25).toInt() // Adjust to move the camera upwards

        val cameraPosition = CameraPosition.Builder()
            .target(latLng)
            .zoom(16f) // Increase zoom level
            .tilt(15f) // Optional: Add tilt for a 3D effect
            .build()

        mGoogleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), object : GoogleMap.CancelableCallback {
            override fun onFinish() {
                mGoogleMap?.animateCamera(CameraUpdateFactory.scrollBy(0f, verticalOffset.toFloat()))
            }

            override fun onCancel() {}
        })
    }


    private fun updateLocationMarker(latLng: LatLng, bearing: Float) {
        // Remove existing marker if present
        currentLocationMarker?.remove()

        // Create a new marker with the default icon
        val markerOptions = MarkerOptions()
            .position(latLng)
            .rotation(bearing) // Set the direction of the marker based on bearing

        // Add the marker to the map with the default icon
        currentLocationMarker = mGoogleMap?.addMarker(markerOptions)
    }

    private fun changeMap(itemId: Int) {
        when (itemId) {
            R.id.normal_map -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
            R.id.hybrid_map -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
            R.id.satellite_map -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
            R.id.terrain_map -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }


}
