/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.rtbishop.look4sat.framework.local

import android.content.ContentResolver
import android.net.Uri
import com.rtbishop.look4sat.data.ILocalSource
import com.rtbishop.look4sat.domain.model.SatEntry
import com.rtbishop.look4sat.domain.model.SatItem
import com.rtbishop.look4sat.domain.model.Transmitter
import com.rtbishop.look4sat.domain.predict.Satellite
import com.rtbishop.look4sat.framework.toDomain
import com.rtbishop.look4sat.framework.toDomainItems
import com.rtbishop.look4sat.framework.toFramework
import com.rtbishop.look4sat.framework.toFrameworkEntries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream

class LocalSource(
    private val resolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher,
    private val entriesDao: EntriesDao,
    private val transmittersDao: TransmittersDao,
) : ILocalSource {

    override suspend fun getAllSatellites(): List<SatItem> {
        return entriesDao.getAllSatellites().toDomainItems()
    }

    override suspend fun getSelectedSatellites(catnums: List<Int>): List<Satellite> {
        return entriesDao.getSelectedSatellites(catnums).map { entry -> entry.tle.createSat() }
    }

    override suspend fun getTransmitters(catnum: Int): List<Transmitter> {
        return transmittersDao.getTransmitters(catnum).toDomain()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun getFileStream(uri: String): InputStream? {
        return withContext(ioDispatcher) { resolver.openInputStream(Uri.parse(uri)) }
    }

    override suspend fun updateEntries(entries: List<SatEntry>) {
        entriesDao.insertEntries(entries.toFrameworkEntries())
    }

    override suspend fun updateTransmitters(transmitters: List<Transmitter>) {
        transmittersDao.updateTransmitters(transmitters.toFramework())
    }

    override suspend fun clearAllData() {
        entriesDao.deleteEntries()
        transmittersDao.deleteTransmitters()
    }
}
