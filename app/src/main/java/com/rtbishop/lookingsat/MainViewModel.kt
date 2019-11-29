package com.rtbishop.lookingsat

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.SatNotFoundException
import com.github.amsacode.predict4java.TLE
import com.google.android.gms.location.LocationServices
import com.rtbishop.lookingsat.repo.Repository
import com.rtbishop.lookingsat.repo.SatPass
import com.rtbishop.lookingsat.repo.SatPassPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import javax.inject.Inject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val keyLat = "latitude"
    private val keyLon = "longitude"
    private val keyHeight = "height"
    private val keyHours = "hoursAhead"
    private val keyMaxEl = "maxElevation"
    private val tleFileName = "tleFile.txt"
    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val locationClient = LocationServices.getFusedLocationProviderClient(application)

    val debugMessage = MutableLiveData("")

    @Inject
    lateinit var repository: Repository

    init {
        (application as LookingSatApp).appComponent.inject(this)
    }

    val gsp = MutableLiveData<GroundStationPosition>(
        GroundStationPosition(
            preferences.getDouble(keyLat, 0.0),
            preferences.getDouble(keyLon, 0.0),
            preferences.getDouble(keyHeight, 0.0)
        )
    )

    var tleMainList = loadTwoLineElementFile()
    var tleSelectedMap = mutableMapOf<TLE, Boolean>()
    var passPrefs = SatPassPrefs(
        preferences.getInt(keyHours, 8),
        preferences.getDouble(keyMaxEl, 16.0)
    )

    private fun loadTwoLineElementFile(): List<TLE> {
        return try {
            TLE.importSat(getApplication<Application>().openFileInput(tleFileName))
                .sortedWith(compareBy { it.name })
        } catch (exception: FileNotFoundException) {
            debugMessage.postValue("TLE file wasn't found")
            emptyList()
        }
    }

    suspend fun getPasses(): List<SatPass> {
        val satPassList = mutableListOf<SatPass>()
        withContext(Dispatchers.Default) {
            tleSelectedMap.forEach { (tle, value) ->
                if (value) {
                    try {
                        val predictor = PassPredictor(tle, gsp.value)
                        val passes = predictor.getPasses(Date(), passPrefs.hoursAhead, false)
                        passes.forEach { satPassList.add(SatPass(tle, predictor, it)) }
                    } catch (exception: IllegalArgumentException) {
                        debugMessage.postValue("There was a problem with TLE")
                    } catch (exception: SatNotFoundException) {
                        debugMessage.postValue("Certain satellites shall not pass")
                    }
                }
            }
            satPassList.retainAll { it.pass.maxEl >= passPrefs.maxEl }
            satPassList.sortBy { it.pass.startTime }
        }
        return satPassList
    }

    fun updateSelectedSatMap(mutableMap: MutableMap<TLE, Boolean>) {
        tleSelectedMap = mutableMap
    }

    fun updatePassPrefs(hoursAhead: Int, maxEl: Double) {
        passPrefs = SatPassPrefs(hoursAhead, maxEl)
        preferences.edit {
            putInt(keyHours, hoursAhead)
            putDouble(keyMaxEl, maxEl)
            apply()
        }
    }

    fun updateLocation() {
        locationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val lat = location?.latitude ?: 0.0
            val lon = location?.longitude ?: 0.0
            val height = location?.altitude ?: 0.0

            preferences.edit {
                putDouble(keyLat, lat)
                putDouble(keyLon, lon)
                putDouble(keyHeight, height)
                apply()
            }
            gsp.postValue(GroundStationPosition(lat, lon, height))
            debugMessage.postValue("Location was updated")
        }
    }

    fun updateTwoLineElementFile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stream = repository.fetchTleStream()
                getApplication<Application>().openFileOutput(tleFileName, Context.MODE_PRIVATE)
                    .use {
                        it.write(stream.readBytes())
                    }
                tleMainList = loadTwoLineElementFile()
                debugMessage.postValue("TLE file was updated")
            } catch (exception: IOException) {
                debugMessage.postValue("Couldn't update TLE file")
            }
        }
    }

    fun updateTransmittersDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateTransmittersDatabase()
                debugMessage.postValue("Transmitters were updated")
            } catch (exception: IOException) {
                debugMessage.postValue("Couldn't update transmitters")
            }
        }
    }
}

fun SharedPreferences.Editor.putDouble(key: String, double: Double): SharedPreferences.Editor =
    putLong(key, java.lang.Double.doubleToRawLongBits(double))

fun SharedPreferences.getDouble(key: String, default: Double) =
    java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))