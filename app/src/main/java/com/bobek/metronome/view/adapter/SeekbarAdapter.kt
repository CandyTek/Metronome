/*
 * This file is part of Metronome.
 * Copyright (C) 2024 Philipp Bobek <philipp.bobek@mailbox.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metronome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bobek.metronome.view.adapter

import android.widget.SeekBar
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
/** databinding 已内置实现android:progressAttrChanged */
object SeekbarAdapter {

	@BindingAdapter("android:progress")
	@JvmStatic
	fun setValue(seekBar: SeekBar, value: Int) {
		if (seekBar.progress != value) {
			seekBar.progress = value
		}
	}

	@InverseBindingAdapter(attribute = "android:progress")
	@JvmStatic
	fun getValue(seekBar: SeekBar): Int = seekBar.progress

}
