package com.enterpreta.mapboxdemo

import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

class GridCell (northWest: com.mapbox.geojson.Point, height: Double, width: Double)
{
    lateinit var northWest: com.mapbox.geojson.Point
    lateinit var southWest: com.mapbox.geojson.Point
    lateinit var northEast: com.mapbox.geojson.Point
    lateinit var southEast: com.mapbox.geojson.Point
    private var height: Double
    private var width: Double

    init{
        this.northWest = northWest
        southWest = TurfMeasurement.destination(northWest,height,180.0, TurfConstants.UNIT_METERS)
        southEast = TurfMeasurement.destination(southWest,width, 90.0, TurfConstants.UNIT_METERS)
        northEast =  TurfMeasurement.destination(southEast,height,0.0, TurfConstants.UNIT_METERS)
        this.height=height
        this.width=width
    }

    fun getListOfPoints(): List<com.mapbox.geojson.Point>{
        return listOf(this.southWest, this.southEast,this.northEast,this.northWest)
    }

    fun getCenter(): com.mapbox.geojson.Point{
        val pointMidway = TurfMeasurement.destination(northEast,this.width/2.0, 90.0, TurfConstants.UNIT_METERS)
        return TurfMeasurement.destination(pointMidway, this.height/2.0, 180.0, TurfConstants.UNIT_METERS )
    }
}