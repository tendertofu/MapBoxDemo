package com.enterpreta.mapboxdemo

//for compass sensor
import android.Manifest
import android.R.attr.data
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
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
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location2
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Flow


/* START OF PREVIOUS MAIN ACTIVITY */

//var mapView: MapView? = null
class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var mapView: MapView
    private lateinit var textView2: TextView
    private lateinit var centerMe: Button
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mapboxMap: MapboxMap

    private lateinit var sensorManager: SensorManager
    private lateinit var magnetometer: Sensor
    private lateinit var accelerometer: Sensor
    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false
    private var rotationMatrix = FloatArray(9)
    private var orientation = FloatArray(3)
    private var azimuthInRadians: Float = 0f
    private var azimuthInDegrees: Float = 0f
    private var azimuthInDegrees_prev: Float = 0f
    private val rotationSensitivity: Float = 5f // the amount of bearing change to make map rotate
    private var fieldPerimeter: MutableList<com.mapbox.geojson.Point> = mutableListOf()

    //for testing purposes only. Should not be used for real development
    private lateinit var butTester: Button
    private lateinit var butBounds: Button
    private var clickCounter: Int = 0
    //private lateinit var  listOfPoints: List<com.mapbox.geojson.Point>
    private lateinit var annotationApi: AnnotationPlugin
    //private val polygonAnnotationManager = annotationApi.createPolygonAnnotationManager()
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager
    private lateinit var  circleAnnotationManager: CircleAnnotationManager
    private lateinit var lastKnownLocation: Location

    //private lateinit var permissionsListener : PermissionsListener

    @SuppressLint("MissingPermission", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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
        centerMe = findViewById(R.id.butCenter)
        butTester=findViewById(R.id.butTester)
        butBounds = findViewById(R.id.butBounds)

        centerMe.setOnClickListener{
            movePuckToCenter(false)
         /*   polylineAnnotationManager.deleteAll()
            circleAnnotationManager.deleteAll()*/

        }
        butTester.setOnClickListener{
            createPolygonTester()
        }
        butBounds.setOnClickListener{
            lateinit var newPoint: com.mapbox.geojson.Point
            getMyLastLocation {
                when(clickCounter){
                    0 ->{
                        newPoint = com.mapbox.geojson.Point.fromLngLat(lastKnownLocation.longitude, lastKnownLocation.latitude)
                        fieldPerimeter.add(newPoint)
                        showCircle(newPoint)
                        showLine(newPoint, fieldPerimeter[fieldPerimeter.size-1])
                        clickCounter+=1
                    }
                    1 ->{
                        newPoint = TurfMeasurement.destination(fieldPerimeter[0],100.0,0.0, TurfConstants.UNIT_METERS)
                        fieldPerimeter.add(newPoint)
                        val n= fieldPerimeter.size
                        showLine(fieldPerimeter[n-2],fieldPerimeter[n-1])
                        showCircle(fieldPerimeter[n-1])
                        clickCounter+=1
                    }
                    2 ->{
                        newPoint = TurfMeasurement.destination(fieldPerimeter[1],115.0,100.0, TurfConstants.UNIT_METERS)
                        fieldPerimeter.add(newPoint)
                        val n= fieldPerimeter.size
                        showLine(fieldPerimeter[n-2],fieldPerimeter[n-1])
                        showCircle(fieldPerimeter[n-1])
                        clickCounter+=1
                    }
                    3 -> {
                        newPoint = TurfMeasurement.destination(
                            fieldPerimeter[2],
                            100.0,
                            180.0,
                            TurfConstants.UNIT_METERS
                        )
                        fieldPerimeter.add(newPoint)
                        val n = fieldPerimeter.size
                        showLine(fieldPerimeter[n - 2], fieldPerimeter[n - 1])
                        showCircle(fieldPerimeter[n - 1])
                        clickCounter += 1
                    }
                    4 -> {
                        val n = fieldPerimeter.size
                        showLine(fieldPerimeter[n - 1], fieldPerimeter[0])
                        clickCounter += 1
                    }
                }

            }

            }


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mapboxMap = mapView.getMapboxMap()

        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS,
            // After the style is loaded, initialize the Location component.
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    mapView.location2.updateSettings {
                        enabled = true
                        pulsingColor = Color.parseColor("#FF0000")  //red
                        pulsingEnabled = true
                        //pulsingMaxRadius=15f

                    }

                }
            }
        )

        //Initialize whats needed to make annotations over the map
        annotationApi = mapView.annotations
        polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
        circleAnnotationManager = annotationApi.createCircleAnnotationManager()

        /*     mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
            val location: Location = task.result*/
        movePuckToCenter(true )  //zoom in when starting

        //mapView.location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)


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


    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this, magnetometer);
        sensorManager.unregisterListener(this, accelerometer);
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size);
            lastMagnetometerSet = true;
        } else if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size);
            lastAccelerometerSet = true;
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(rotationMatrix, orientation);

            azimuthInRadians = orientation[0]
            azimuthInDegrees = (Math.toDegrees(azimuthInRadians.toDouble()) + 360).toFloat() % 360

            if(Math.abs(azimuthInDegrees-azimuthInDegrees_prev)> rotationSensitivity) {
                azimuthInDegrees_prev=(azimuthInDegrees+azimuthInDegrees_prev)/2
                textView2.text=azimuthInDegrees.toString()
                var newCameraPosition = CameraOptions.Builder()
                    .bearing(azimuthInDegrees.toDouble())
                    .build()
                //Toast.makeText(this, "Bearing is: $bearing", Toast.LENGTH_SHORT).show()
                // set camera position
                mapView.getMapboxMap().setCamera(newCameraPosition)
            }

        }
    }

    //center location on screen
    fun movePuckToCenter(zoomStatus: Boolean) {
        //mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
            var location: Location = task.result
            if(zoomStatus) {
                var cameraOptions = CameraOptions.Builder()
                    .center(com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude))
                    .zoom(20.0)
                    .build()
                // Move the camera to the new center point.
                mapView.getMapboxMap().easeTo(cameraOptions)
            }else
            {
                //NO Zoom
                var cameraOptions = CameraOptions.Builder()
                    .center(com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude))
                    .build()
                // Move the camera to the new center point.
                mapView.getMapboxMap().easeTo(cameraOptions)
            }
        }
    }

    fun createPolygonTester(){
        val points2 =
            listOf (
                listOf(
                    com.mapbox.geojson.Point.fromLngLat(120.99833,14.51943),
                    com.mapbox.geojson.Point.fromLngLat(120.99692,14.52203),
                    com.mapbox.geojson.Point.fromLngLat(120.99402,14.52290)
                )
            )

        val listOfPoints = points2[0]

        val polylineAnnotationOptions2 = PolylineAnnotationOptions()
            .withPoints(listOfPoints)
            .withLineColor("#FF0000")
            .withLineWidth(3.0)
        polylineAnnotationManager.create(polylineAnnotationOptions2)

        //add circle
        val circleAnnotationOption = CircleAnnotationOptions()
            .withCircleColor("#FF0000")
             .withPoint(  com.mapbox.geojson.Point.fromLngLat(120.99833,14.51943))
            //.withPoint(  com.mapbox.geojson.Point.fromLngLat(120.99692,14.52203))
            .withCircleRadius(5.0)
        circleAnnotationManager.create(circleAnnotationOption)
    }

    fun getMyLastLocation(finishFunction: ()-> Unit) {
       var myPresentPoint = com.mapbox.geojson.Point.fromLngLat(0.0,0.0)
        var isLocationReturned: Boolean= false;
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            //return
        }
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location = task.result
                    lastKnownLocation = location
                    finishFunction()
                }
    }

    fun showLine(point1: com.mapbox.geojson.Point, point2: com.mapbox.geojson.Point){
        val points =
            listOf (
                listOf(
                    point1,
                    point2
                )
            )

        val polylineAnnotationOptions = PolylineAnnotationOptions()
            .withPoints(points[0])
            .withLineColor("#FF0000")
            .withLineWidth(3.0)
        polylineAnnotationManager.create(polylineAnnotationOptions)
    }
    fun showCircle(point1: com.mapbox.geojson.Point){
        val circleAnnotationOption = CircleAnnotationOptions()
            .withCircleColor("#FF0000")
            .withPoint(  point1)
            //.withPoint(  com.mapbox.geojson.Point.fromLngLat(120.99692,14.52203))
            .withCircleRadius(5.0)
        circleAnnotationManager.create(circleAnnotationOption)
    }

  /*  private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }*/

}


