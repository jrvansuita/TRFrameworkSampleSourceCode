package com.utc.ccs.trfwsample

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.utc.fs.trframework.TRSyncType

class DKSettingsActivity : AppCompatActivity() {
	private lateinit var syncButton: Button
	private lateinit var syncSpinner: ProgressBar
	private lateinit var logoutButton: Button

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_settings)
		setupUi()
	}

	private fun setupUi() {
		val authCode: TextView = findViewById(R.id.key_serial_txt)
		authCode.text = getString(R.string.key_serial_format, DKFramework.sharedFramework().localDeviceSerialNumber)

		val dns: TextView = findViewById(R.id.sync_dns_txt)
		dns.text = getString(R.string.sync_dns_format, DKFramework.sharedFramework().syncUrl)

		syncSpinner = findViewById(R.id.sync_spinner)
		syncSpinner.visibility = View.GONE

		syncButton = findViewById(R.id.sync_button)
		syncButton.setOnClickListener { syncClicked() }

		logoutButton = findViewById(R.id.logout_button)
		logoutButton.setOnClickListener { logoutClicked() }
	}

	private fun logoutClicked() {
		DKFramework.resetFramework()
		setResult(DKMainActivity.RESULT_LOGGED_OUT)
		finish()
	}

	private fun syncClicked() {
		syncButton.text = getString(R.string.syncing)
		lockButtons()

		DKFramework.updateKey(true, TRSyncType.TRSyncTypeFull) { _, error ->
			syncButton.text = getString(R.string.sync)
			unlockButtons()
			if (error != null) {
				Toast.makeText(this, error.errorMessage, Toast.LENGTH_LONG).show()
			} else {
				Toast.makeText(this, "Sync was successful", Toast.LENGTH_LONG).show()
			}
		}
	}

	private fun lockButtons() {
		syncButton.isEnabled = false
		logoutButton.isEnabled = false
		syncButton.visibility = View.INVISIBLE
		syncSpinner.visibility = View.VISIBLE
	}

	private fun unlockButtons() {
		syncButton.isEnabled = true
		logoutButton.isEnabled = true
		syncButton.visibility = View.VISIBLE
		syncSpinner.visibility = View.GONE
	}
}