package com.example.android.ui

import AuthManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.android.R
import com.example.android.databinding.ActivityMainBinding
import com.example.android.ui.login.LoginActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()

        // Check if authId is set
        val authId = AuthManager.getAuthId(applicationContext)
        val userId = AuthManager.getUserId(applicationContext)
        if (authId.isNullOrEmpty() || userId == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            replaceFragment(MapFragment())
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(MapFragment())
                }
                R.id.navigation_following -> {
                    replaceFragment(FollowingFragment())
                }
                R.id.navigation_bookmarks -> {
                    replaceFragment(BookmarksFragment())
                }
                R.id.navigation_profile -> {
                    replaceFragment(ProfileFragment())
                }
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
    }
}