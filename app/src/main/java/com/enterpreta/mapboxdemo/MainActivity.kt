package com.enterpreta.mapboxdemo

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager

import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap

import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.camera

import com.mapbox.maps.plugin.animation.easeTo

import com.mapbox.maps.plugin.locationcomponent.location2


/*
class MainActivity : AppCompatActivity() {

    private lateinit var permissionsManager: PermissionsManager

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = MapView(this)

        var permissionsListener: PermissionsListener = object : PermissionsListener {
            override fun onExplanationNeeded(permissionsToExplain: List<String>) {

            }

            override fun onPermissionResult(granted: Boolean) {
                if (granted) {

                    // Permission sensitive logic called here, such as activating the Maps SDK's LocationComponent to show the device's location

                } else {

                    // User denied the permission

                }
            }
        }
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            onMapReady()

        } else {
            permissionsManager = PermissionsManager(permissionsListener)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun onMapReady() {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(14.0)
                .build()
        )
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            initLocationComponent()
            setupGesturesListener()
        }
    }
    private fun setupGesturesListener() {
        mapView.gestures.addOnMoveListener(onMoveListener)
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.mapbox_user_puck_icon,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.mapbox_user_icon_shadow,
                ),
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
    }
    private fun onCameraTrackingDismissed() {
        Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

}

*/




/* START OF PREVIOUS MAIN ACTIVITY */

//var mapView: MapView? = null
class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var textView2: TextView
    private lateinit var myToggler : Button
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mapboxMap : MapboxMap

    //private lateinit var permissionsListener : PermissionsListener

    @SuppressLint("MissingPermission", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //NOTE:  the permissionListener and the permissionManager (code follows) are packages from MapBox
        //see for details: https://docs.mapbox.com/android/maps/guides/user-location/


        val callback = LocationListeningCallback(this)


        var permissionsListener: PermissionsListener = object : PermissionsListener {
            override fun onExplanationNeeded(permissionsToExplain: List<String>) {

            }

            override fun onPermissionResult(granted: Boolean) {
                if (granted) {

                    // Permission sensitive logic called here, such as activating the Maps SDK's LocationComponent to show the device's location

                } else {

                    // User denied the permission

                }
            }
        }


        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Permission sensitive logic called here, such as activating the Maps SDK's LocationComponent to show the device's location

            val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
            val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

            val locationEngine = LocationEngineProvider.getBestLocationEngine(this)
            var request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                .build()

            locationEngine.requestLocationUpdates(request, callback, mainLooper)
            locationEngine.getLastLocation(callback)


            //show actual device location on map


        } else {
            permissionsManager = PermissionsManager(permissionsListener)
            permissionsManager.requestLocationPermissions(this)
        }

        mapView = findViewById(R.id.mapView)
        textView2 = findViewById(R.id.textView2)
        myToggler= findViewById(R.id.Toggler)

        myToggler.setOnClickListener{

            var bearing = mapboxMap.cameraState.bearing
            if(bearing==180.0){
                bearing=0.0
            }else
            {
                bearing=180.0
            }
            var newCameraPosition = CameraOptions.Builder()
                .bearing(bearing)
                .build()
            //Toast.makeText(this, "Bearing is: $bearing", Toast.LENGTH_SHORT).show()
            // set camera position
            mapView.getMapboxMap().setCamera(newCameraPosition)
        }


        mapboxMap= mapView.getMapboxMap()

        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS,
            // After the style is loaded, initialize the Location component.
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    mapView.location2.updateSettings {
                        enabled = true
                        pulsingEnabled = true

                    }

                }
            }
        )



        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
            val location: Location = task.result
            var cameraOptions = CameraOptions.Builder()
                .center(com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude))
                .zoom(20.0)
                .build()

            // Move the camera to the new center point.
            mapView.getMapboxMap().easeTo(cameraOptions)

            //mapView.location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        }

      /*  var cameraOptions = CameraOptions.Builder()
            .center(com.mapbox.geojson.Point.fromLngLat(120.994083, 14.519185))
            .build()

        // Move the camera to the new center point.
        mapView.getMapboxMap().easeTo(cameraOptions)*/


  /*  var cameraOptions = CameraOptions.Builder()
        .center(com.mapbox.geojson.Point.fromLngLat(120.98478, 14.52758))
        .build()

    // Move the camera to the new center point.
    mapView.getMapboxMap().easeTo(cameraOptions)*/





        //added by Renan for map to rotate with heading
        //reference https://docs.mapbox.com/android/maps/guides/camera-and-animation/camera/


     /*   //  SHOULD BE THE START OF COMMENT OUT


        //mapView.getMapboxMap().loadStyleUri(Style.SATELLITE)
        //mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)

        //val locationPuck= LocationPuck2D()
        //val locationPuck = createDefault2DPuck(this@LocationComponentActivity, withBearing = true)




        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            // After the style is loaded, initialize the Location component.
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    mapView.location2.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }

                }
            }
        )

        // create a polygon layer

        //val polygonCoordinates = ArrayList<>()
        //polygonCoordinates.add(LatLong)


        //var points = ArrayList<com.mapbox.geojson.Point>()
        //points.add(com.mapbox.geojson.Point.fromLngLat(120.98478,14.52758))
        //points.add(com.mapbox.geojson.Point.fromLngLat(120.98009,14.52351))
        //points.add(com.mapbox.geojson.Point.fromLngLat(120.98582,14.52284))

        // Create an instance of the Annotation API and get the polygon manager.
        val annotationApi = mapView.annotations
        val polygonAnnotationManager = annotationApi.createPolygonAnnotationManager()
        //polygonAnnotationManager.

        // Define a list of geographic coordinates to be connected.
        val points = listOf(
            listOf(
                com.mapbox.geojson.Point.fromLngLat(120.98478,14.52758),
                com.mapbox.geojson.Point.fromLngLat(120.97996,14.52393),
                com.mapbox.geojson.Point.fromLngLat(120.98582,14.52284)
            )
        )

        val testPolygon = com.mapbox.geojson.Polygon.fromLngLats(points)
        val polyArea = TurfMeasurement.area(testPolygon)
        textView2.text= polyArea.toInt().toString() + " square meters"


        // Set options for the resulting fill layer.
        val polygonAnnotationOptions: PolygonAnnotationOptions = PolygonAnnotationOptions()
            .withPoints(points)
            // Style the polygon that will be added to the map.
            .withFillColor("#ee4e8b")
            .withFillOpacity(0.4)
            // Add the resulting polygon to the map.
        polygonAnnotationManager?.create(polygonAnnotationOptions)

        //change color to purple
        polygonAnnotationManager.annotations[0].fillColorString="#0000FF"


        var origPoint = com.mapbox.geojson.Point.fromLngLat(120.98167,14.52362)
        var origPolygon = com.mapbox.geojson.Polygon.fromLngLats(points)
        var answer: Boolean = TurfJoins.inside(origPoint, origPolygon)

        //var featureCollection = listOf(origPoint,origPolygon)

        //TurfJoins.pointsWithinPolygon()

        var thisPolygon = com.mapbox.geojson.Polygon.fromLngLats(points)
        var bboxArray = TurfMeasurement.bbox(thisPolygon)

        val southWest = com.mapbox.geojson.Point.fromLngLat(bboxArray[0],bboxArray[1])
        val northEast = com.mapbox.geojson.Point.fromLngLat(bboxArray[2],bboxArray[3])
        val southEast = com.mapbox.geojson.Point.fromLngLat(bboxArray[2],bboxArray[1])
        val northWest = com.mapbox.geojson.Point.fromLngLat(bboxArray[0],bboxArray[3])

        val points2 = listOf(
            listOf(
              southWest,northWest,northEast,southEast
            )
        )
        // Set options for the resulting fill layer.
        val polygonAnnotationOptions2: PolygonAnnotationOptions = PolygonAnnotationOptions()
            .withPoints(points2)
            // Style the polygon that will be added to the map.
            .withFillColor("#ee4e8b")
            //.withFillOpacity(0.0)
            .withFillOpacity(0.4)
            .withFillOutlineColor("#FF0000")


        //polygonAnnotationManager?.create(polygonAnnotationOptions2)

        //val listOfPoints: List<com.mapbox.geojson.Point> = listOf(southWest,northWest,northEast,southEast,southWest)
        val listOfPoints = points2[0]

        val polyLineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
            .withPoints(listOfPoints)
            .withLineColor("#FF0000")
            .withLineWidth(2.0)

       val polyLineAnnotationManager = annotationApi.createPolylineAnnotationManager()
        polyLineAnnotationManager.create(polyLineAnnotationOptions)

        val offsetPoint = TurfMeasurement.destination(northWest,10.0,90.0, "meters")

        //add a point marker
        // Create an instance of the Annotation API and get the PointAnnotationManager.
        bitmapFromDrawableRes(
            this@MainActivity,
            R.drawable.red_marker
        )?.let {
            val annotationApi = mapView?.annotations
            val pointAnnotationManager = annotationApi?.createPointAnnotationManager()
        // Set options for the resulting symbol layer.
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
        // Define a geographic coordinate.
                .withPoint(offsetPoint)
        // Specify the bitmap you assigned to the point annotation
        // The bitmap will be added to map style automatically.
                .withIconImage(it)
        // Add the resulting pointAnnotation to the map.
            pointAnnotationManager?.create(pointAnnotationOptions)
        }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
    // copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }

      */  //SHOULD MARK END OF COMMENT OUT

    }


  /*  private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }*/



}


