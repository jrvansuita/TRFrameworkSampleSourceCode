package com.utc.ccs.trfwsample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.utc.fs.trframework.TRDevice
import com.utc.fs.trframework.TRError
import com.utc.fs.trframework.TRFrameworkError

class DKMainActivity : AppCompatActivity() {
	private var nearbyDevices = ArrayList<TRDevice>()
	private lateinit var listView: ListView
	private lateinit var progressBar: ProgressBar
	private lateinit var operationText: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		listView = findViewById(R.id.list_view)
		listView.setOnItemClickListener { _, _, i, _ -> handleRowTapped(i) }

		progressBar = findViewById(R.id.scan_spinner)
		operationText = findViewById(R.id.operation_txt)

		val settings = findViewById<ImageButton>(R.id.settings_button)
		settings.setOnClickListener {
			val intent = Intent(applicationContext, DKSettingsActivity::class.java)
			startActivityForResult(intent, SETTINGS_REQUEST_CODE)
		}
		if (!DKFramework.hasAuthorizedWithServer()) {
			goToAuthorizationScreen()
		}
	}

	override fun onPause() {
		super.onPause()
		stopScanning()
	}

	override fun onResume() {
		super.onResume()
		startScanning()
	}

	public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_LOGGED_OUT) {
			goToAuthorizationScreen()
			return
		}
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun goToAuthorizationScreen() {
		val intent = Intent(applicationContext, DKAuthorizationActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		startActivity(intent)
	}

	private fun handleRowTapped(row: Int) {
		val device = nearbyDevices[row]
		stopScanning()
		showProgressWithText(getString(R.string.opening))
		DKFramework.openDevice(device) { error: TRError? ->
			hideProgress()
			if (error == null) {
				Toast.makeText(this, "Open success!", Toast.LENGTH_LONG).show()
			} else {
				Toast.makeText(this, error.errorMessage, Toast.LENGTH_LONG).show()
			}
			startScanning()
		}
	}

	private fun updateUi() {
		val arrayAdapter = ArrayAdapter(
			this,
			android.R.layout.simple_list_item_1,
			getRowTitlesFromDeviceList(nearbyDevices)
		)
		listView.adapter = arrayAdapter
	}

	private fun startScanning() {
		if (!DKFramework.sharedFramework().isBTLESupported) {
			Toast.makeText(this, "BTLE is not supported on this device.", Toast.LENGTH_LONG).show()
			return
		}
		showProgressWithText(getString(R.string.scanning))
		DKFramework.startScanning(
			{ handleScanStarted() },
			{ handleScanEnded() },
			{ error: TRError? -> handleScanError(error) }
		) { list: ArrayList<TRDevice> -> handleDevicesReturned(list) }
	}

	private fun handleScanStarted() {
		Log.d(LOG_TAG, "Scanning started")
	}

	private fun handleScanEnded() {
		Log.d(LOG_TAG, "Scanning ended")
		hideProgress()
	}

	private fun handleScanError(error: TRError?) {
		Log.d(LOG_TAG, "Scan error: " + error!!.errorMessage)
		hideProgress()
		if (error.errorCode == TRFrameworkError.TRFrameworkErrorDiscoveryCancelled) {
			Log.d(LOG_TAG, "Discovery cancelled error, ignoring it")
			return
		}
		if (error.errorCode == TRFrameworkError.TRFrameworkErrorInsufficientLocationPermissions) {
			Log.e(LOG_TAG, "TRFrameworkError.TRFrameworkErrorInsufficientLocationPermissions")
		}
	}

	private fun handleDevicesReturned(devices: ArrayList<TRDevice>) {
		nearbyDevices = devices
		updateUi()
	}

	private fun stopScanning() {
		DKFramework.stopScanning()
		hideProgress()
	}

	// Extra Helper methods
	private fun getRowTitlesFromDeviceList(devices: ArrayList<TRDevice>): ArrayList<String> {
		val serials = ArrayList<String>()
		for (device in devices) {
			serials.add(rowTitleFromDevice(device))
		}
		return serials
	}

	private fun rowTitleFromDevice(device: TRDevice): String {
		val name = device.deviceName
		val serial = device.serialNumber
		return if (name.isNotEmpty()) {
			"$name - $serial"
		} else {
			serial
		}
	}

	private fun showProgressWithText(text: String) {
		progressBar.visibility = View.VISIBLE
		operationText.text = text
	}

	private fun hideProgress() {
		progressBar.visibility = View.GONE
		operationText.text = ""
	}

	companion object {
		const val RESULT_LOGGED_OUT = 2
		private const val LOG_TAG = "MAIN_ACTIVITY"
		private const val SETTINGS_REQUEST_CODE = 0
	}
}