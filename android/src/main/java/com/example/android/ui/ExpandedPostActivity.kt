package com.example.android.ui

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.android.R

class ExpandedPostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expanded_post)
        val backArrowImageView: ImageView = findViewById(R.id.backArrowImageView)
        // Set click listener for the back arrow
        backArrowImageView.setOnClickListener {
            finish()
        }
        // Retrieve additional details from intent extras if needed
        val imageResourceId = intent.getIntExtra("imageResourceId", 0)
        val detailsText = intent.getStringExtra("detailsText")

        // Use imageResourceId and detailsText to display additional details
    }
}
