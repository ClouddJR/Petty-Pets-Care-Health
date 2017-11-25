package com.clouddroid.pettypetscarehealth.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.clouddroid.pettypetscarehealth.R
import com.clouddroid.pettypetscarehealth.repositories.UserRepository
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*


class LoginActivity : AppCompatActivity(), UserRepository.OnLoggedListener {

    private val RC_SIGN_IN = 123
    private val providers = arrayListOf(
            AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
            AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build())

    private val googleProvider = arrayListOf(AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build())

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initUserRepository()
        initGoogleLoginButtonText()
        initLogInButtons()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            // Successfully signed in
            if (resultCode == RESULT_OK) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                if (response?.errorCode == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, R.string.login_activity_connection_problem, Toast.LENGTH_LONG).show()
                }
            }
        }

        progressBar.visibility = View.GONE
    }

    private fun initUserRepository() {
        userRepository.addOnLoggedListener(this)
    }


    private fun initGoogleLoginButtonText() {
        (signInWithGoogleButton.getChildAt(0) as TextView).text = getString(R.string.login_activity_google)
    }

    private fun initLogInButtons() {
        loginButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            userRepository.signInWithLoginAndPassword(emailEditText.text.toString(), passwordEditText.text.toString())
        }

        signInWithGoogleButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            startActivityForResult(buildAuthUi(googleProvider), RC_SIGN_IN)
        }

        createAccountButton.setOnClickListener {
            startActivityForResult(buildAuthUi(providers), RC_SIGN_IN)
        }
    }

    private fun buildAuthUi(type: ArrayList<AuthUI.IdpConfig>): Intent {
        return AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(type)
                .setTheme(R.style.AppTheme_NoActionBar_SignIn)
                .build()
    }

    override fun onLoggedResult(wasSuccessful: Boolean) {
        if (wasSuccessful) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, R.string.login_activity_wrong_credentials, Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
        }
    }
}
