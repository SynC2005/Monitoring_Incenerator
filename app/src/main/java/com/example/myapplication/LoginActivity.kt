package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    lateinit var binding : ActivityLoginBinding
    lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }



        binding.btnLogin.setOnClickListener {
            val email = binding.etEmailLogin.text.toString()
            val password = binding.etPasswordLogin.text.toString()

            if (email.isEmpty()){
                binding.etEmailLogin.error = "email harus diisi"
                binding.etEmailLogin.requestFocus()
                return@setOnClickListener
            }

            if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                binding.etEmailLogin.error = "email tidak valid"
                binding.etEmailLogin.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()){
                binding.etPasswordLogin.error = "password harus diisi"
                binding.etPasswordLogin.requestFocus()
                return@setOnClickListener
            }

            if(password.length<7){
                binding.etPasswordLogin.error = "password minimal 7 karakter"
                binding.etPasswordLogin.requestFocus()
                return@setOnClickListener
            }

            LoginFirebase(email,password)
        }
    }

    private fun LoginFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Selamat datang $email", Toast.LENGTH_SHORT).show()

                    // CHECK: Has the user seen the onboarding tutorial?
                    val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    val hasSeenOnboarding = prefs.getBoolean("hasSeenOnboarding", false)

                    val intent = if (!hasSeenOnboarding) {
                        // First time? Go to Tutorial Slides
                        Intent(this, TutorialActivity::class.java)
                    } else {
                        // Already seen? Go straight to Main
                        Intent(this, MainActivity::class.java)
                    }

                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}