package com.enterpreta.mapboxdemo

//for compass sensor
import android.Manifest
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
import android.os.PersistableBundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.enterpreta.mapboxdemo.data.ControlPoint
import com.enterpreta.mapboxdemo.data.ControlPointDao
import com.enterpreta.mapboxdemo.data.FieldCellDao
import com.enterpreta.mapboxdemo.data.FieldDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileRegion
import com.mapbox.common.TileRegionError
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.common.TileStoreOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.Style
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
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
import com.mapbox.turf.TurfJoins
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
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

    //private val cellWidth: Double = 10.0  //standard width of a field cell
    //private val cellHeight: Double = 10.0 // standard height of a field cell
    private val cellSide: Double = 15.0 //standard width or height of a field cell
    private var bboxCells: MutableList<GridCell> = mutableListOf<GridCell>()
    private var fieldCells: MutableList<GridCell> =
        mutableListOf<GridCell>() //List of cells for which at
    // least 50% of its area is inside field polygon

    //database
    private lateinit var db: FieldDatabase
    private lateinit var fieldCellDao: FieldCellDao
    private lateinit var controlPointDao: ControlPointDao

    //for testing purposes only. Should not be used for real development
    private lateinit var btnControlPoint: Button
    private lateinit var butBounds: Button
    private lateinit var btnRestore: Button
    private lateinit var btnOfflineDownload: Button
    private lateinit var btnCountMaps: Button
    private var clickCounter: Int = 0

    //to save state for resume
    private lateinit var cameraState: CameraState

    private lateinit var annotationApi: AnnotationPlugin

    //private val polygonAnnotationManager = annotationApi.createPolygonAnnotationManager()
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager
    private lateinit var circleAnnotationManager: CircleAnnotationManager
    private lateinit var polygonAnnotationManager: PolygonAnnotationManager
    private lateinit var gridlineAnnotationManager: PolylineAnnotationManager
    private lateinit var polygonPerimeter: com.mapbox.geojson.Polygon
    //private lateinit var lastKnownLocation: Location

    private lateinit var offlineManager: OfflineManager

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


        var permissionsListener: PermissionsListener = object: PermissionsListener {
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


        //INSTANTIATE ROOM RELATED VARIABLES
        db = Room.databaseBuilder(
            applicationContext,
            FieldDatabase::class.java, "FieldDatabase"
        ).allowMainThreadQueries().build()
        fieldCellDao = db.fieldCellDao()
        controlPointDao = db.controlPointDao()

        val numControlPointRecs = controlPointDao.count()

        mapView = findViewById(R.id.mapView)
        textView2 = findViewById(R.id.textView2)
        centerMe = findViewById(R.id.butCenter)
        btnControlPoint = findViewById(R.id.btnControlPoint)
        butBounds = findViewById(R.id.butBounds)
        btnRestore = findViewById(R.id.btnRestore)
        btnOfflineDownload = findViewById(R.id.btnOfflineDownload)
        btnCountMaps = findViewById(R.id.btnCountMaps)

        centerMe.setOnClickListener {
            movePuckToCenter(false)
        }
        btnControlPoint.setOnClickListener {
            val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
            // alertDialog.setTitle("Add Control Point")
            alertDialog.setMessage("Record a CONTROL POINT at current location?")
            alertDialog.setPositiveButton(
                "Yes"
            ) { _, _ ->
                makeNewControlPoint()  //This ws where new control point is made
            }
            alertDialog.setNegativeButton(
                "Cancel"
            ) { _, _ -> }
            val alert: AlertDialog = alertDialog.create()
            alert.setCanceledOnTouchOutside(false)
            alert.show()
            //makeNewControlPoint()
        }
        btnRestore.setOnClickListener {
            /* val intent = Intent(this, SecondActivity::class.java)
             startActivity(intent)*/
        }
        butBounds.setOnClickListener {
            lateinit var newPoint: com.mapbox.geojson.Point
            getMyLastLocation {
                calculateFieldCellsFromControlPoints()
            }

        }

        btnOfflineDownload.setOnClickListener {
            downloadOfflineMap()
        }

        btnCountMaps.setOnClickListener {
            // countMaps()
            checkStylePacks { stylePackName ->
                runOnUiThread(Runnable {
                    Toast.makeText(this, "STYLE PACK NAME: $stylePackName", Toast.LENGTH_SHORT)
                        .show()
                })
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

        lifecycleScope.launch {
            calculateGpsAccuracy()
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        cameraState = mapView.getMapboxMap().cameraState
        //outState.putSerializable("cameraState", cameraState)
        //outState.putSerializable("polylineAnnotationManager")
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
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
        //this method makes the orientation of device pointed to match the actual compass direction
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
                //textView2.text = azimuthInDegrees_prev.toString()
                val average = (azimuthInDegrees + azimuthInDegrees_prev) / 2.0
                var newCameraPosition = CameraOptions.Builder()
                    .bearing(average)
                    .build()
                mapView.getMapboxMap().setCamera(newCameraPosition)
            }

        }
    }

    //center location on screen
    private fun movePuckToCenter(zoomStatus: Boolean) {
        getMyLastLocation {lastKnownLocation ->
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
            }
        }
    }

    private fun makeNewControlPoint() {
        getMyLastLocation {lastKnownLocation ->
            val newControlPoint = ControlPoint(
                lastKnownLocation.longitude,
                lastKnownLocation.latitude,
                lastKnownLocation.altitude
            )
            //val numRecs = db.controlPointDao().count()
            db.controlPointDao().upsert(newControlPoint)
            val pointLastKnownLocation = com.mapbox.geojson.Point.fromLngLat(
                newControlPoint.longitude,
                newControlPoint.latitude
            )
            showCircle(pointLastKnownLocation)
        }
    }

    private fun getMyLastLocation(finishFunction: (lastKnowLocation: Location) -> Unit) {
        //This function is designed to update the lastKnownLocation variable
        //and accept the steps (in the form of a function parameter called finishFunction)
        //on what to do with the lastKnownLocation

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
            finishFunction(location)
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

    private fun showCircle(point1: com.mapbox.geojson.Point) {
        val circleAnnotationOption = CircleAnnotationOptions()
            .withCircleColor("#FF0000")
            .withPoint(point1)
            //.withPoint(  com.mapbox.geojson.Point.fromLngLat(120.99692,14.52203))
            .withCircleRadius(5.0)
        circleAnnotationManager.create(circleAnnotationOption)
    }

    private fun getListOfPointsFromBBoxArray(bboxArray: DoubleArray): List<com.mapbox.geojson.Point> {
        val southWest = com.mapbox.geojson.Point.fromLngLat(bboxArray[0], bboxArray[1])
        val northEast = com.mapbox.geojson.Point.fromLngLat(bboxArray[2], bboxArray[3])
        val southEast = com.mapbox.geojson.Point.fromLngLat(bboxArray[2], bboxArray[1])
        val northWest = com.mapbox.geojson.Point.fromLngLat(bboxArray[0], bboxArray[3])
        //return listOf(southWest, northWest, northEast, southEast)
        return listOf(southWest, southEast, northEast, northWest)
    }

    private fun calculateFieldCellsFromControlPoints() {
        val listControlPoints = db.controlPointDao().getAllControlPoints()
        val listPoints = mutableListOf<com.mapbox.geojson.Point>()
        /*    if(listControlPoints.size>7){
                Toast.makeText(this,"Number of CONTROL POINTS: ${listControlPoints.size}",Toast.LENGTH_SHORT).show()
                val removeCP = listControlPoints.last()
                db.controlPointDao().delete(removeCP)
            }else
            {
                Toast.makeText(this,"No more to remove",Toast.LENGTH_SHORT).show()
            }
            return*/

        for (_controlPoint in listControlPoints) {
            listPoints.add(
                com.mapbox.geojson.Point.fromLngLat(
                    _controlPoint.longitude,
                    _controlPoint.latitude
                )
            )
        }


        val polygonAnnotationOptions: PolygonAnnotationOptions =
            PolygonAnnotationOptions()
                .withPoints(listOf(listPoints))
                //.withGeometry(bboxPolygon)
                // Style the polygon that will be added to the map.
                .withFillColor("#ee4e8b")
                .withFillOpacity(0.4)
        // Add the resulting polygon to the map.
        polygonAnnotationManager.deleteAll()
        polygonAnnotationManager.create(polygonAnnotationOptions)

        //calculate bbox array
        polygonPerimeter =
            com.mapbox.geojson.Polygon.fromLngLats(mutableListOf(listPoints))

        bboxArray = TurfMeasurement.bbox(polygonPerimeter)

        val bboxSetOfPoints = getListOfPointsFromBBoxArray(bboxArray)
        val bboxWidth = TurfMeasurement.distance(
            bboxSetOfPoints[0],
            bboxSetOfPoints[1]
        ) * 1000  //southwest to southeast
        val bboxHeight = TurfMeasurement.distance(
            bboxSetOfPoints[1],
            bboxSetOfPoints[2]
        ) * 1000 //southeast to northeast
        val xDimension = (ceil(bboxWidth / cellSide)).toInt()
        val yDimension = (ceil(bboxHeight / cellSide)).toInt()

        // sub divide bbox into grid cells
        var nextNorthWestPoint =
            getListOfPointsFromBBoxArray(bboxArray)[3]  // corresponds to NW point
        lateinit var previousRowSouthWestPoint: com.mapbox.geojson.Point
        for (y in 0 until yDimension) {
            for (x in 0 until xDimension) {
                val thisCell = GridCell(nextNorthWestPoint, cellSide, cellSide)
                if (x == 0) {
                    //this means the first of the current row.  we need to save because the sw point will be used
                    //as the first nw point of the next row
                    previousRowSouthWestPoint = thisCell.southWest
                }
                bboxCells.add(thisCell)
                nextNorthWestPoint = thisCell.northEast
            }
            nextNorthWestPoint = previousRowSouthWestPoint
        }
        for (_cell in bboxCells) {
            if (TurfJoins.inside(_cell.getCenter(), polygonPerimeter)) {
                fieldCells.add(_cell)
            }
        }
        gridlineAnnotationManager.deleteAll()
        //polylineAnnotationManager.deleteAll()
        for (_cell in fieldCells) {

            val polylineAnnotationOptions = PolylineAnnotationOptions()
                .withPoints(_cell.getPointsForLine())
                .withLineColor("#FF0000")
                .withLineWidth(3.0)
            gridlineAnnotationManager.create(polylineAnnotationOptions)
        }


    }

    private fun downloadOfflineMap() {

        if (!this::offlineManager.isInitialized) {
            offlineManager = OfflineManager(MapInitOptions.getDefaultResourceOptions(this))
        }

        val stylePackLoadOptions = StylePackLoadOptions.Builder()
            .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
            //.metadata(Value(STYLE_PACK_METADATA))
            .metadata(Value("STREET1"))
            .build()

        val tilesetDescriptor = offlineManager.createTilesetDescriptor(
            TilesetDescriptorOptions.Builder()
                .styleURI(Style.MAPBOX_STREETS)
                .minZoom(10)
                .maxZoom(16)
                .build()
        )
        //Baguio
        //northwest  125.5710719235847,7.097329293232832
        //southwest  125.5707077748098, 7.042640458726822
        //southeast  125.61064275712118, 7.049747963678297
        //northeast  125.63843944693669, 7.086970231698748
        val baguioPoints = mutableListOf<com.mapbox.geojson.Point>()
        baguioPoints.add(com.mapbox.geojson.Point.fromLngLat(125.5710719235847, 7.097329293232832))
        baguioPoints.add(com.mapbox.geojson.Point.fromLngLat(125.5707077748098, 7.042640458726822))
        baguioPoints.add(com.mapbox.geojson.Point.fromLngLat(125.61064275712118, 7.049747963678297))
        baguioPoints.add(com.mapbox.geojson.Point.fromLngLat(125.63843944693669, 7.086970231698748))

        val baguioPolygon = com.mapbox.geojson.Polygon.fromLngLats(mutableListOf(baguioPoints))
        val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
            .geometry(baguioPolygon)
            .descriptors(listOf(tilesetDescriptor))
            .metadata(Value("DAVAO CITY"))
            .acceptExpired(true)
            .networkRestriction(NetworkRestriction.NONE)
            .build()

        //download the STYLE PACK
        val stylePackCancelable = offlineManager.loadStylePack(
            Style.MAPBOX_STREETS,
            // Build Style pack load options
            stylePackLoadOptions,
            { progress ->
                // Handle the download progress
            },
            { expected ->
                if (expected.isValue) {
                    expected.value?.let { stylePack ->
                        // Style pack download finished successfully
                        runOnUiThread(Runnable {
                            Toast.makeText(
                                this,
                                "STYLE PACK SUCCESSFULLY LOADED",
                                Toast.LENGTH_SHORT
                            ).show()
                        })
                    }
                }
                expected.error?.let {
                    // Handle errors that occurred during the style pack download.
                    runOnUiThread(Runnable {
                        Toast.makeText(this, "STYLE PACK LOADING ERROR", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        )
        // Cancel the download if needed
        //stylePackCancelable.cancel()

        //  DOWNLOAD THE TILE REGION
        // You need to keep a reference of the created tileStore and keep it during the download process.
        // You are also responsible for initializing the TileStore properly, including setting the proper access token.
        val tileStore = TileStore.create().also {
            // Set default access token for the created tile store instance
            it.setOption(
                TileStoreOptions.MAPBOX_ACCESS_TOKEN,
                TileDataDomain.MAPS,
                Value(getString(R.string.mapbox_access_token))
            )
        }
        val tileRegionCancelable = tileStore.loadTileRegion(
            "Baguio_Proper",
            tileRegionLoadOptions,
            { progress ->
                // Handle the download progress
            },
            { expected: Expected<TileRegionError, TileRegion> ->
                if (expected.isValue) {
                    // Tile region download finishes successfully
                    runOnUiThread(Runnable {
                        Toast.makeText(this, "TILE REGION SUCCESSFULLY LOADED", Toast.LENGTH_SHORT)
                            .show()
                    })

                }
                expected.error?.let {
                    // Handle errors that occurred during the tile region download.
                    runOnUiThread(Runnable {
                        Toast.makeText(this, "TILE REGION LOADING ERROR", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        )

    }

    private fun countMaps() {
        if (!this::offlineManager.isInitialized) {
            offlineManager = OfflineManager(MapInitOptions.getDefaultResourceOptions(this))
        }

        // Get a list of style packs that are currently available.
        offlineManager.getAllStylePacks { expected ->
            if (expected.isValue) {
                expected.value?.let { stylePackList ->
                    //Log.d("Existing style packs: $stylePackList")
                    var metadataStylePack = ""
                    offlineManager.getStylePackMetadata(stylePackList[0].styleURI) {
                        metadataStylePack = it.value.toString()
                    }
                }
            }
            expected.error?.let { stylePackError ->
                runOnUiThread(Runnable {
                    Toast.makeText(this, "ERROR RETRIEVING STYLE PACK", Toast.LENGTH_SHORT).show()
                })
            }
        }

        //Check TILE STORE
        val tileStore = TileStore.create().also {
            // Set default access token for the created tile store instance
            it.setOption(
                TileStoreOptions.MAPBOX_ACCESS_TOKEN,
                TileDataDomain.MAPS,
                Value(getString(R.string.mapbox_access_token))
            )
        }

        tileStore.getAllTileRegions { expected ->
            if (expected.isValue) {
                expected.value?.let { mutableListTileRegions ->
                    val regionId = mutableListTileRegions[0].id
                    regionId
                }
            }
        }


    }

    private fun checkStylePacks(function_StylePack: (stylePackName: String) -> Unit) {
        if (!this::offlineManager.isInitialized) {
            offlineManager = OfflineManager(MapInitOptions.getDefaultResourceOptions(this))
        }
        offlineManager.getAllStylePacks { expected ->
            if (expected.isValue) {
                expected.value?.let { stylePackList ->
                    //Log.d("Existing style packs: $stylePackList")
                    offlineManager.getStylePackMetadata(stylePackList[0].styleURI) {
                        val metadataStylePack = it.value.toString()
                        function_StylePack(metadataStylePack)
                    }
                }
            }
            expected.error?.let { stylePackError ->
                //enter handler here
            }

        }
    }

    private suspend fun calculateGpsAccuracy() {
        while (true) {
            getMyLastLocation {lastKnownLocation ->
                val accuracy = lastKnownLocation.accuracy
                //round accuracy to one decimal point number
                textView2.text =
                    accuracy.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
                        .toString()
            }

            delay(1500L)
        }

    }
}

//}

/*companion object {
    lateinit var db: FieldDatabase
}*/






