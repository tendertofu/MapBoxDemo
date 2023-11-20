package com.enterpreta.mapboxdemo

//for compass sensor
import android.Manifest
import android.R.attr.data
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.enterpreta.mapboxdemo.data.FieldCell
import com.enterpreta.mapboxdemo.data.FieldCellDao
import com.enterpreta.mapboxdemo.data.FieldDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location2
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfJoins
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Flow
import kotlin.math.ceil


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
    private lateinit var bboxArray: DoubleArray
    private val cellWidth: Double = 10.0  //standard width of a grid cell
    private val cellHeight: Double = 10.0 // standard height of a grid cell
    private var bboxCells: MutableList<GridCell> = mutableListOf<GridCell>()
    private var fieldCells:  MutableList<GridCell> = mutableListOf<GridCell>() //List of cells for which at
                                                                                // least 50% of its area is inside field polygon

    //database
    private lateinit var db: FieldDatabase

    //private lateinit var fieldCellDao = db.fieldCellDao()
    private lateinit var  fieldCellDao: FieldCellDao

    //for testing purposes only. Should not be used for real development
    private lateinit var butTester: Button
    private lateinit var butBounds: Button
    private lateinit var btnSecondActivity: Button
    private var clickCounter: Int = 0

    //to save state for resume
    private lateinit var  cameraState: CameraState

    //private lateinit var  listOfPoints: List<com.mapbox.geojson.Point>
    private lateinit var annotationApi: AnnotationPlugin

    //private val polygonAnnotationManager = annotationApi.createPolygonAnnotationManager()
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager
    private lateinit var circleAnnotationManager: CircleAnnotationManager
    //private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var gridlineAnnotationManager: PolylineAnnotationManager
    private lateinit var polygonPerimeter: com.mapbox.geojson.Polygon
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

        val db = Room.databaseBuilder(
            this,
            FieldDatabase::class.java,"FieldDatabase"
        ).allowMainThreadQueries().build()
        fieldCellDao = db.fieldCellDao()
        val numRecs = fieldCellDao.count()

        mapView = findViewById(R.id.mapView)
        textView2 = findViewById(R.id.textView2)
        centerMe = findViewById(R.id.butCenter)
        butTester = findViewById(R.id.butTester)
        butBounds = findViewById(R.id.butBounds)
        btnSecondActivity = findViewById(R.id.btnSecondActivity)

        centerMe.setOnClickListener {
            movePuckToCenter(false)
        }
        butTester.setOnClickListener {
            createPolygonTester()
        }
        btnSecondActivity.setOnClickListener{
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }
        butBounds.setOnClickListener {
            lateinit var newPoint: com.mapbox.geojson.Point
            getMyLastLocation {
                when (clickCounter) {
                    0 -> {
                        newPoint = com.mapbox.geojson.Point.fromLngLat(
                            lastKnownLocation.longitude,
                            lastKnownLocation.latitude
                        )
                        fieldPerimeter.add(newPoint)
                        showCircle(newPoint)
                        showLine(newPoint, fieldPerimeter[fieldPerimeter.size - 1])
                        clickCounter += 1
                    }

                    1 -> {
                        newPoint = TurfMeasurement.destination(
                            fieldPerimeter[0],
                            100.0,
                            0.0,
                            TurfConstants.UNIT_METERS
                        )
                        fieldPerimeter.add(newPoint)
                        val n = fieldPerimeter.size
                        showLine(fieldPerimeter[n - 2], fieldPerimeter[n - 1])
                        showCircle(fieldPerimeter[n - 1])
                        clickCounter += 1
                    }

                    2 -> {
                        newPoint = TurfMeasurement.destination(
                            fieldPerimeter[1],
                            115.0,
                            100.0,
                            TurfConstants.UNIT_METERS
                        )
                        fieldPerimeter.add(newPoint)
                        val n = fieldPerimeter.size
                        showLine(fieldPerimeter[n - 2], fieldPerimeter[n - 1])
                        showCircle(fieldPerimeter[n - 1])
                        clickCounter += 1
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
                        val poly1 =
                            com.mapbox.geojson.Polygon.fromLngLats(mutableListOf(fieldPerimeter))
                    }

                    4 -> {
                        val n = fieldPerimeter.size
                        showLine(fieldPerimeter[n - 1], fieldPerimeter[0])
                        clickCounter += 1
                    }

                    5 -> {
                        polylineAnnotationManager.deleteAll()
                        circleAnnotationManager.deleteAll()
                        val polygonAnnotationOptions: PolygonAnnotationOptions =
                            PolygonAnnotationOptions()
                                .withPoints(listOf(fieldPerimeter))
                                //.withGeometry(bboxPolygon)
                                // Style the polygon that will be added to the map.
                                .withFillColor("#ee4e8b")
                                .withFillOpacity(0.4)
                        // Add the resulting polygon to the map.
                        MainActivity.polygonAnnotationManager.create(polygonAnnotationOptions)
                        polygonPerimeter =
                            com.mapbox.geojson.Polygon.fromLngLats(mutableListOf(fieldPerimeter))

                        bboxArray = TurfMeasurement.bbox(polygonPerimeter)
                        var bboxPolygon =
                            com.mapbox.geojson.Polygon.fromLngLats(mutableListOf(fieldPerimeter))
                        val points = mutableListOf(fieldPerimeter)

                        val polylineAnnotationOptions = PolylineAnnotationOptions()
                            .withPoints(getListOfPointsFromBBoxArray(bboxArray))
                            .withLineColor("#023020")  //dark green
                            .withLineWidth(3.0)
                        polylineAnnotationManager.create(polylineAnnotationOptions)

                        clickCounter += 1
                    }

                    6 -> {
                        val bboxSetOfPoints = getListOfPointsFromBBoxArray(bboxArray)
                        val bboxHWidth = TurfMeasurement.distance(
                            bboxSetOfPoints[0],
                            bboxSetOfPoints[1]
                        ) * 1000  //southwest to southeast
                        val bboxHeight = TurfMeasurement.distance(
                            bboxSetOfPoints[1],
                            bboxSetOfPoints[2]
                        ) * 1000 //southeast to northeast
                        val xDimension = (ceil(bboxHWidth / cellWidth)).toInt()
                        val yDimension = (ceil(bboxHeight / cellHeight)).toInt()

                        // sub divide bbox into grid cells
                        var nextNorthWestPoint = getListOfPointsFromBBoxArray(bboxArray)[3]  // corresponds to NW point
                        lateinit var previousRowSouthWestPoint: com.mapbox.geojson.Point
                        for (y in 0 until yDimension) {
                            for (x in 0 until xDimension) {
                                val thisCell = GridCell(nextNorthWestPoint, cellHeight, cellWidth)
                                if(x==0){
                                    //this means the first of the current row.  we need to save because the sw point will be used
                                    //as the first nw point of the next row
                                    previousRowSouthWestPoint=thisCell.southWest
                                }
                                bboxCells.add(thisCell)
                                nextNorthWestPoint=thisCell.northEast
                            }
                            nextNorthWestPoint=previousRowSouthWestPoint
                        }
                        //Toast.makeText(this, "This is the width: $bboxHWidth", Toast.LENGTH_SHORT)

                        //Make line for each GridCell
                        for (bboxCell in bboxCells) {

                            val polylineAnnotationOptions = PolylineAnnotationOptions()
                                .withPoints(bboxCell.getPointsForLine())
                                .withLineColor("#FF0000")
                                .withLineWidth(3.0)
                            gridlineAnnotationManager.create(polylineAnnotationOptions)
                        }

                        clickCounter += 1
                    }

                    7 -> {
                        for (_cell in bboxCells){
                            if(TurfJoins.inside(_cell.getCenter(),polygonPerimeter)){
                                fieldCells.add(_cell)
                            }
                        }
                        gridlineAnnotationManager.deleteAll()
                        polylineAnnotationManager.deleteAll()
                        for (_cell in fieldCells) {

                            val polylineAnnotationOptions = PolylineAnnotationOptions()
                                .withPoints(_cell.getPointsForLine())
                                .withLineColor("#FF0000")
                                .withLineWidth(3.0)
                            gridlineAnnotationManager.create(polylineAnnotationOptions)
                        }

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
                    }

                }
            }
        )

        //Initialize whats needed to make annotations over the map
        annotationApi = mapView.annotations
        polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
        circleAnnotationManager = annotationApi.createCircleAnnotationManager()
        polygonAnnotationManager = annotationApi.createPolygonAnnotationManager()
        gridlineAnnotationManager = annotationApi.createPolylineAnnotationManager()


        movePuckToCenter(true)  //zoom in when starting

    }


    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        cameraState=mapView.getMapboxMap().cameraState
        //outState.putSerializable("cameraState", cameraState)
        //outState.putSerializable("polylineAnnotationManager")
    }

    override fun onRestoreInstanceState( savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
        //mapView.getMapboxMap().cameraState = savedInstanceState.getSerializable("cameraState") as CameraState
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
            SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                lastAccelerometer,
                lastMagnetometer
            );
            SensorManager.getOrientation(rotationMatrix, orientation);

            azimuthInRadians = orientation[0]
            azimuthInDegrees = (Math.toDegrees(azimuthInRadians.toDouble()) + 360).toFloat() % 360

            if (Math.abs(azimuthInDegrees - azimuthInDegrees_prev) > rotationSensitivity) {
                azimuthInDegrees_prev = (azimuthInDegrees + azimuthInDegrees_prev) / 2
                textView2.text = azimuthInDegrees.toString()
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
       getMyLastLocation {
           if (zoomStatus) {
           var cameraOptions = CameraOptions.Builder()
               .center(
                   com.mapbox.geojson.Point.fromLngLat(
                       lastKnownLocation.longitude,
                       lastKnownLocation.latitude
                   )
               )
               .zoom(20.0)
               .build()
           // Move the camera to the new center point.
           mapView.getMapboxMap().easeTo(cameraOptions)
       } else {
           //NO Zoom
           var cameraOptions = CameraOptions.Builder()
               .center(
                   com.mapbox.geojson.Point.fromLngLat(
                       lastKnownLocation.longitude,
                       lastKnownLocation.latitude,
                       //lastKnownLocation.altitude
                   )
               )
               .build()
           // Move the camera to the new center point.
           mapView.getMapboxMap().easeTo(cameraOptions)
       } }

    }

    fun createPolygonTester() {
        val points2 =
            listOf(
                listOf(
                    com.mapbox.geojson.Point.fromLngLat(120.99833, 14.51943),
                    com.mapbox.geojson.Point.fromLngLat(120.99692, 14.52203),
                    com.mapbox.geojson.Point.fromLngLat(120.99402, 14.52290)
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
            .withPoint(com.mapbox.geojson.Point.fromLngLat(120.99833, 14.51943))
            //.withPoint(  com.mapbox.geojson.Point.fromLngLat(120.99692,14.52203))
            .withCircleRadius(5.0)
        circleAnnotationManager.create(circleAnnotationOption)
    }

    fun getMyLastLocation(finishFunction: () -> Unit) {
        //This function is designed to update the lastKnownLocation variable
        //and accept the steps (in the form of a function parameter called finishFunction)
        //on what to do with the lastKnownLocation
        var myPresentPoint = com.mapbox.geojson.Point.fromLngLat(0.0, 0.0)
        var isLocationReturned: Boolean = false;
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

    fun showLine(point1: com.mapbox.geojson.Point, point2: com.mapbox.geojson.Point) {
        val points =
            listOf(
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

    fun showCircle(point1: com.mapbox.geojson.Point) {
        val circleAnnotationOption = CircleAnnotationOptions()
            .withCircleColor("#FF0000")
            .withPoint(point1)
            //.withPoint(  com.mapbox.geojson.Point.fromLngLat(120.99692,14.52203))
            .withCircleRadius(5.0)
        circleAnnotationManager.create(circleAnnotationOption)
    }

    fun getListOfPointsFromBBoxArray(bboxArray: DoubleArray): List<com.mapbox.geojson.Point> {
        val southWest = com.mapbox.geojson.Point.fromLngLat(bboxArray[0], bboxArray[1])
        val northEast = com.mapbox.geojson.Point.fromLngLat(bboxArray[2], bboxArray[3])
        val southEast = com.mapbox.geojson.Point.fromLngLat(bboxArray[2], bboxArray[1])
        val northWest = com.mapbox.geojson.Point.fromLngLat(bboxArray[0], bboxArray[3])
        //return listOf(southWest, northWest, northEast, southEast)
        return listOf(southWest, southEast, northEast, northWest)
    }

    companion object{
        lateinit var polygonAnnotationManager: PolygonAnnotationManager
    }


    /*  private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
          mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
      }*/

}


