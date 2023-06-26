package io.pridetechnologies.businesscard

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.SetOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.squareup.picasso.Picasso
import io.pridetechnologies.businesscard.databinding.ActivityEditBusinessAddressBinding
import io.pridetechnologies.businesscard.databinding.CustomQrCodeDialogBinding
import java.io.File

class EditBusinessAddressActivity : AppCompatActivity(), OnMapReadyCallback {
    private val progressDialog by lazy { CustomProgressDialog(this) }
    private lateinit var binding: ActivityEditBusinessAddressBinding
    private val constants = Constants()
    private var businessId: String? = ""
    private lateinit var gMap: GoogleMap
    private lateinit var currentPosition: LatLng
    private var lat: Double = 0.0
    private var long: Double = 0.0
    private var businessLatitude: Double = 0.0
    private var businessLongitude: Double = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_LOCATION_PERMISSION = 12
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var finalPosition: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBusinessAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
            Animatoo.animateFade(this)
        }

        businessId = intent.getStringExtra("business_id")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding.addImageButton.setOnClickListener {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        // Got last known location. In some rare situations, this can be null.
                        if (location != null) {
                            // Use the location object to get latitude and longitude
                            latitude = location.latitude
                            longitude = location.longitude

                            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                            mapFragment.getMapAsync(this)
                            binding.mapsLayout.visibility = View.VISIBLE
                            binding.detailsLayout.visibility = View.GONE

                            // Now you can use the latitude and longitude to update the map or perform other tasks
                        }
                    }
                    .addOnFailureListener { e ->
                        // Handle any errors that occurred while trying to get location
                    }
            } else {
                // Location permission not granted, request again
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
            }
        }
        binding.saveLocationButton.setOnClickListener {
            binding.mapsLayout.visibility = View.GONE
            binding.detailsLayout.visibility = View.VISIBLE
        }

        binding.continueButton.setOnClickListener {
            saveAndContinue()
        }
        getBusinessAddress()
    }

    private fun getBusinessAddress() {
        constants.db.collection("businesses").document(businessId.toString())
            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    Log.w(ContentValues.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val buildingNumber = snapshot.get("building_number").toString()
                    val streetName = snapshot.get("street_name").toString()
                    val areaLocated = snapshot.get("area_located").toString()
                    val districtName = snapshot.get("district_name").toString()
                    val country = snapshot.get("country").toString()
                    businessLatitude = snapshot.getDouble("business_latitude") ?: 0.0
                    businessLongitude = snapshot.getDouble("business_longitude") ?: 0.0


                    binding.houseNumberTextField.editText?.setText(buildingNumber)
                    binding.streetNameTextField.editText?.setText(streetName)
                    binding.areaLocatedTextField.editText?.setText(areaLocated)
                    binding.districtTextField.editText?.setText(districtName)
                    binding.countryTextField.editText?.setText(country)
                    binding.latTextView.text = "Latitude : $businessLatitude"
                    binding.longTextView.text = "Longitude : $businessLongitude"
                }

            }
    }

    private fun saveAndContinue() {
        val buildingNumber = binding.houseNumberTextField.editText?.text.toString().trim()
        val streetName = binding.streetNameTextField.editText?.text.toString().trim()
        val areaLocated = binding.areaLocatedTextField.editText?.text.toString().trim()
        val districtName = binding.districtTextField.editText?.text.toString().trim()
        val country = binding.countryTextField.editText?.text.toString().trim()

        if (buildingNumber.isEmpty() && areaLocated.isEmpty() && country.isEmpty()){
            Toast.makeText(this, "Provide all details", Toast.LENGTH_SHORT).show()
        }else{
            val businessDetails = hashMapOf(
                "building_number" to buildingNumber,
                "street_name" to streetName,
                "area_located" to areaLocated,
                "district_name" to districtName,
                "country" to country,
                "business_id" to businessId,
                "business_latitude" to (finalPosition?.latitude ?: 0.0),
                "business_longitude" to (finalPosition?.longitude ?: 0.0)
            )

            constants.db.collection("businesses").document(businessId.toString())
                .set(businessDetails, SetOptions.merge())
                .addOnSuccessListener {
                    progressDialog.hide()
                    constants.showToast(this, "Save Successfully")
                    finish()
                    Animatoo.animateFade(this)
                }
                .addOnFailureListener { e ->
                    progressDialog.hide()
                    constants.showToast(this, "Failed to save details")
                    Log.w(ContentValues.TAG, "Error writing document", e) }
        }



    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        gMap.uiSettings.isZoomControlsEnabled = true
        gMap.uiSettings.isCompassEnabled = true
        gMap.uiSettings.isMyLocationButtonEnabled = true

        if (businessLatitude == 0.0 && businessLongitude == 0.0){
            currentPosition = LatLng(latitude!!, longitude!!)
        }else{
            currentPosition = LatLng(businessLatitude, businessLongitude)
        }

        gMap.addCircle(CircleOptions()
            .center(currentPosition)
            .radius(2.0)
            .strokeColor(Color.BLUE)
            .fillColor(Color.RED)
            .strokeWidth(15f))
        moveToCurrentPosition(currentPosition)

        gMap.setOnMapLongClickListener { point ->
            gMap.addMarker(MarkerOptions()
                .draggable(true)
                .position(point)
                .title("My Business Location"))
            binding.latTextView.text = "Latitude : ${point.latitude}"
            binding.longTextView.text = "Longitude : ${point.longitude}"
            gMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener{
                override fun onMarkerDrag(marker: Marker) {
                }

                override fun onMarkerDragEnd(marker: Marker) {
                    lat = marker.position.latitude
                    long = marker.position.longitude
                    finalPosition = LatLng(lat,long)
                    binding.latTextView.text = "Latitude : $lat"
                    binding.longTextView.text = "Longitude : $long"
                    //Log.w(ContentValues.TAG, "Marker Position: $finalPosition ")
                }

                override fun onMarkerDragStart(marker: Marker) {}
            })
        }

//        gMap.addMarker(
//            MarkerOptions()
//                .draggable(true)
//                .position(currentPosition)
//                .flat(true)
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
//                .title("My Business Location"))
//        moveToCurrentPosition(currentPosition)
//        gMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener{
//            override fun onMarkerDrag(marker: Marker) {
//            }
//
//            override fun onMarkerDragEnd(marker: Marker) {
//                val lat = marker.position.latitude
//                val long = marker.position.longitude
//                finalPosition = LatLng(lat,long)
//                //Log.w(ContentValues.TAG, "Marker Position: $finalPosition ")
//            }
//
//            override fun onMarkerDragStart(marker: Marker) {
//            }
//
//        })
    }
    private fun moveToCurrentPosition(position:LatLng){
        val cameraPosition = CameraPosition.builder()
            .target(position)
            .zoom(20f).build()
        gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    fun goBack(view: View) {
        binding.mapsLayout.visibility = View.GONE
        binding.detailsLayout.visibility = View.VISIBLE
    }
    override fun onDestroy() {
        gMap.clear()
        super.onDestroy()
    }
}