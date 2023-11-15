package com.enterpreta.mapboxdemo

import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

class GridCell (northWest: com.mapbox.geojson.Point, height: Double, width: Double)
{
    lateinit var northWest: com.mapbox.geojson.Point
    lateinit var southWest: com.mapbox.geojson.Point
    lateinit var northEast: com.mapbox.geojson.Point
    lateinit var southEast: com.mapbox.geojson.Point

    init{
        this.northWest = northWest
        southWest = TurfMeasurement.destination(northWest,height,180.0, TurfConstants.UNIT_METERS)
        southEast = TurfMeasurement.destination(southWest,width, 90.0, TurfConstants.UNIT_METERS)
        northEast =  TurfMeasurement.destination(southEast,height,0.0, TurfConstants.UNIT_METERS)
    }

    fun getListOfPoints(): List<com.mapbox.geojson.Point>{
        return listOf(this.southWest, this.southEast,this.northEast,this.northWest)
    }
}