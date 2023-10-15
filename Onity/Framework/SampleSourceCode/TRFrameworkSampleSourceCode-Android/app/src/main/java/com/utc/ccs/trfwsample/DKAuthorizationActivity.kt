package com.utc.ccs.trfwsample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DKAuthorizationActivity : AppCompatActivity() {
	private lateinit var authCodeEntry: EditText
	private lateinit var dnsEntry: EditText
	private lateinit var loginButton: Button
	private lateinit var progressBar: ProgressBar

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_authorization)

		progressBar = findViewById(R.id.progressBar)
		progressBar.visibility = View.GONE

		authCodeEntry = findViewById(R.id.auth_entry)
		dnsEntry = findViewById(R.id.dns_entry)

		loginButton = findViewById(R.id.login_button)
		loginButton.setOnClickListener { loginClicked() }
	}

	private fun loginClicked() {
		disableUi()

		DKFramework.authorize(dnsEntry.text.toString(), authCodeEntry.text.toString()) { trError ->
			enableUi()

			if (trError != null) {
				trError.errorMessage?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
			} else {
				val intent = Intent(applicationContext, DKMainActivity::class.java)
				intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
				startActivity(intent)
			}
		}
	}

	private fun disableUi() {
		progressBar.visibility = View.VISIBLE
		authCodeEntry.isEnabled = false
		dnsEntry.isEnabled = false
		loginButton.text = getString(R.string.logging_in)
		loginButton.isEnabled = false
	}

	private fun enableUi() {
		progressBar.visibility = View.GONE
		authCodeEntry.isEnabled = true
		dnsEntry.isEnabled = true
		loginButton.text = getString(R.string.login)
		loginButton.isEnabled = true
	}

}
