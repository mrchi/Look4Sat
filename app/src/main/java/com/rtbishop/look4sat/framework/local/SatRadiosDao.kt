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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rtbishop.look4sat.framework.model.SatRadio
import kotlinx.coroutines.flow.Flow

@Dao
interface SatRadiosDao {

    @Query("SELECT COUNT(*) FROM radios")
    fun getRadiosNumber(): Flow<Int>

    @Query("SELECT * FROM radios WHERE catnum = :catnum AND isAlive = 1")
    suspend fun getRadios(catnum: Int): List<SatRadio>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadios(radios: List<SatRadio>)

    @Query("DELETE FROM radios")
    suspend fun deleteRadios()
}
