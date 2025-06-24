package com.bobek.metronome

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity2 : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		checkFloat(this)
	}

	fun checkFloat(context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
			startFloatService()
			finish()
		} else {
			setContentView(R.layout.activity_float_help)
			val btn: Button = findViewById<Button>(R.id.btnRequestPermission)
			btn.setOnClickListener {
				val intent = Intent(
					Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
					Uri.parse("package:" + context.packageName)
				)
				startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
			}
		}
	}

	private fun startFloatService() {
		startService(Intent(this, FloatMetronomeService::class.java))
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
				startFloatService()
				finish()
			}
		}
	}

	companion object {
		private val TAG = MainActivity2::class.java.simpleName
		private val OVERLAY_PERMISSION_REQ_CODE = 1234
	}
}
