/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.data.SatPass
import com.rtbishop.look4sat.databinding.ItemPassGeoBinding
import com.rtbishop.look4sat.databinding.ItemPassLeoBinding
import java.text.SimpleDateFormat
import java.util.*

class PassesAdapter(context: Context, private val shouldUseUTC: Boolean = false) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val satIdFormat = context.getString(R.string.pass_satId)
    private val azFormat = context.getString(R.string.pat_azimuth)
    private val elevFormat = context.getString(R.string.pat_elevation)
    private val aosAzFormat = context.getString(R.string.pass_aos_az)
    private val losAzFormat = context.getString(R.string.pass_los_az)
    private val dateFormat = context.getString(R.string.pass_dateTime)
    private val timeZoneUTC = TimeZone.getTimeZone("UTC")
    private val simpleDateFormat = SimpleDateFormat(dateFormat, Locale.getDefault())
    private var satPassList: MutableList<SatPass> = mutableListOf()

    fun setList(list: MutableList<SatPass>) {
        satPassList = list
        notifyDataSetChanged()
    }

    fun tickPasses(timeNow: Long) {
        val iterator = satPassList.listIterator()
        while (iterator.hasNext()) {
            val satPass = iterator.next()
            if (!satPass.tle.isDeepspace) {
                if (satPass.progress < 100) {
                    val timeStart = satPass.pass.startTime.time
                    if (timeNow > timeStart) {
                        satPass.active = true
                        val timeEnd = satPass.pass.endTime.time
                        val index = satPassList.indexOf(satPass)
                        val deltaNow = timeNow.minus(timeStart).toFloat()
                        val deltaTotal = timeEnd.minus(timeStart).toFloat()
                        satPass.progress = ((deltaNow / deltaTotal) * 100).toInt()
                        notifyItemChanged(index)
                    }
                } else {
                    val index = satPassList.indexOf(satPass)
                    iterator.remove()
                    notifyItemRemoved(index)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return satPassList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (satPassList[position].tle.isDeepspace) 1
        else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val bindingLeo = ItemPassLeoBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            SatPassLeoHolder(bindingLeo)
        } else {
            val bindingGeo = ItemPassGeoBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            SatPassGeoHolder(bindingGeo)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == 0) {
            (holder as SatPassLeoHolder).bind(satPassList[position])
        } else {
            (holder as SatPassGeoHolder).bind(satPassList[position])
        }
    }

    inner class SatPassLeoHolder(private val binding: ItemPassLeoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(satPass: SatPass) {
            binding.apply {
                if (shouldUseUTC) simpleDateFormat.timeZone = timeZoneUTC
                val startTime = simpleDateFormat.format(satPass.pass.startTime)
                val endTime = simpleDateFormat.format(satPass.pass.endTime)
                passLeoSatName.text = satPass.tle.name
                passLeoSatId.text = String.format(satIdFormat, satPass.tle.catnum)
                passLeoAosAz.text = String.format(aosAzFormat, startTime, satPass.pass.aosAzimuth)
                passLeoMaxEl.text = String.format(elevFormat, satPass.pass.maxEl)
                passLeoLosAz.text = String.format(losAzFormat, satPass.pass.losAzimuth, endTime)
                passLeoProgress.progress = satPass.progress
            }

            itemView.setOnClickListener {
                if (satPass.progress < 100) {
                    val bundle = bundleOf("index" to satPassList.indexOf(satPass))
                    itemView.findNavController().navigate(R.id.action_passes_to_polar, bundle)
                }
            }
        }
    }

    inner class SatPassGeoHolder(private val binding: ItemPassGeoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(satPass: SatPass) {
            val satPos = satPass.predictor.getSatPos(satPass.pass.startTime)
            val azimuth = Math.toDegrees(satPos.azimuth)

            binding.apply {
                passGeoName.text = satPass.tle.name
                passGeoId.text = String.format(satIdFormat, satPass.tle.catnum)
                passGeoAz.text = String.format(azFormat, azimuth)
                passGeoEl.text = String.format(elevFormat, satPass.pass.maxEl)
            }

            itemView.setOnClickListener {
                val bundle = bundleOf("index" to satPassList.indexOf(satPass))
                itemView.findNavController().navigate(R.id.action_passes_to_polar, bundle)
            }
        }
    }
}
