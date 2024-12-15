package com.example.android.ui

import AuthManager
import PhotoPagerAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.example.android.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FollowingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FollowingFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var indicatorDots: Array<ImageView?>? = null
    private lateinit var viewPager: ViewPager
    private lateinit var followingList: JSONArray
    private lateinit var pinList: JSONArray
    private lateinit var loadingIndicator: ProgressBar
    private var currentUserId = 0
    private lateinit var auth: FirebaseAuth
    private var authUser: FirebaseUser? = null
    private var authToken: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_following, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        currentUserId = AuthManager.getUserId(requireContext()) ?: 1
        Log.d("currentUserId", "$currentUserId")

        auth = FirebaseAuth.getInstance()
        authUser = auth.currentUser
        authUser?.getIdToken(true)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Token retrieval successful
                    val tokenResult = task.result?.token
                    // Do something with the token
                    authToken = tokenResult
                    Log.d("feedToken", "$authToken")

                    CoroutineScope(Dispatchers.IO).launch {
                        makeRequest()
                    }
                } else {
                    // Token retrieval failed
                    Log.e("feedToken", "Failed to retrieve token: ${task.exception}")
                }
            }
    }

    private suspend fun fetchUserDetails(userId: String): JsonObject? {
        return try {
            val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users/$userId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            // Set authorization header if authToken is not null
            authToken?.let {
                connection.setRequestProperty("Authorization", "$it")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JsonParser.parseString(jsonString).asJsonObject
//                jsonObject["name"].asString // Extract and return the user's name
                jsonObject
            } else {
                Log.e("Response", "HTTP request failed with response code: $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e("Response", "Error fetching user details: ${e.message}", e)
            null
        }
    }

    private fun createBookmarkRequest(pinId: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/users/$currentUserId/bookmarks")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Set authorization header if authToken is not null
                authToken?.let {
                    connection.setRequestProperty("Authorization", "$it")
                }

                val requestBody = """
                {
                    "pinId": "$pinId",
                    "userId": "$currentUserId"
                }
            """.trimIndent()

                val outputStream: OutputStream = connection.outputStream
                outputStream.write(requestBody.toByteArray())
                outputStream.flush()

                val responseCode = connection.responseCode
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                Log.d("Likes", "Response code: $responseCode, Response body: $response")
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("ResponseData", "Error making request: ${e.message}", e)
            }
        }
    }

    private suspend fun makeRequest() {
        try {
            activity?.runOnUiThread { loadingIndicator.visibility = View.VISIBLE }
            val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/users/$currentUserId/feed")
            val connection = url.openConnection() as HttpURLConnection

            // Set request method
            connection.requestMethod = "GET"

            // Set authorization header if authToken is not null
            authToken?.let {
                connection.setRequestProperty("Authorization", "$it")
            }

            // Get response code
            val responseCode = connection.responseCode
            Log.d("FollowingPins", "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response body
                val inputStream = connection.inputStream
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                // Parse JSON
                val jsonArray = JsonParser.parseString(jsonString).asJsonArray
                Log.d("FollowingPins", "$jsonArray")
                processResponse(jsonArray)
            } else {
                Log.e("FollowingPins", "HTTP request failed with response code: $responseCode")
            }

        } catch (e: Exception) {
            // Log any errors
            Log.e("FollowingPins", "Error making request: ${e.message}", e)
        } finally {
            // Hide loading indicator when data loading ends (whether successful or not)
            activity?.runOnUiThread { loadingIndicator.visibility = View.GONE }
        }
    }

    private suspend fun processResponse(response: JsonArray) {
        for (element in response) {
            val bookmark = element.asJsonObject
            val pinId = bookmark["id"].asInt
            val userId = bookmark["userId"].asString
            val userObject = fetchUserDetails(userId)
            val creatorName = userObject?.getAsJsonPrimitive("name")?.asString ?: ""
            val bioPhoto = userObject?.getAsJsonPrimitive("bioPhoto")?.asString ?: ""
            val location = bookmark["address"].asString
            val lat = bookmark["lat"].asString
            val lon = bookmark["lon"].asString
            val title = bookmark["name"].asString
            val details = bookmark["desc"]?.asString ?: "No desc"
            val date = bookmark["createdAt"].asString.substringBefore("T")


            val photos = if (bookmark.has("photos") && bookmark["photos"].isJsonArray) {
                val photosArray = bookmark.getAsJsonArray("photos")
                if (photosArray.size() > 0) {
                    photosArray.map { it.asString }
                } else {
                    listOf(
                        "https://images.unsplash.com/photo-1642425149556-b6f90e946859?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                        "https://images.unsplash.com/photo-1690373403498-a29da916ecf9?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                        "https://images.unsplash.com/photo-1710179337706-f5e304f7740a?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
                    )
                }
            } else {
                // If "photos" field is empty or not present, use a default list of URLs
                listOf(
                    "https://images.unsplash.com/photo-1642425149556-b6f90e946859?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                    "https://images.unsplash.com/photo-1690373403498-a29da916ecf9?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                    "https://images.unsplash.com/photo-1710179337706-f5e304f7740a?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
                )
            }

            populateUI(pinId, creatorName, location, lat, lon, title, details, photos, date, bioPhoto)


        }
    }

    private fun populateUI(
        pinId: Int,
        creatorName: String,
        location: String,
        lat: String,
        lon: String,
        title: String,
        details: String,
        photos: List<String>,
        date: String,
        bioPhoto: String
    ) {
        activity?.runOnUiThread {
            // Inflate the layout for each bookmark dynamically
            val followingView = layoutInflater.inflate(R.layout.condensed_post, null)

            val viewPager = followingView.findViewById<ViewPager>(R.id.viewPager)
            val adapter = PhotoPagerAdapter(requireContext(), photos)
            viewPager.adapter = adapter

//            val editDeleteButtonsLayout = followingView.findViewById<LinearLayout>(R.id.editDeleteButtonsLayout)
//            editDeleteButtonsLayout.visibility = View.GONE



            // Get the indicator layout for this bookmark
            val indicatorLayout = followingView.findViewById<LinearLayout>(R.id.indicatorLayout)

            // Setup indicators for this bookmark
            setupIndicators(photos.size, indicatorLayout)

            setupViewPager(viewPager, indicatorLayout)

            // Find views inside bookmark layout
            val usernameTextView = followingView.findViewById<TextView>(R.id.usernameTextView)
            val locationTextView = followingView.findViewById<TextView>(R.id.locationTextView)
//            val coordsTextView = followingView.findViewById<TextView>(R.id.coordsTextView)
            val titleTextView = followingView.findViewById<TextView>(R.id.titleTextView)
            val detailsTextView = followingView.findViewById<TextView>(R.id.detailsTextView)
            val dateTextView = followingView.findViewById<TextView>(R.id.dateTextView)
            val userProfileImageView = followingView.findViewById<ImageView>(R.id.userProfileImageView)

//            val latLonString = "$lat, $lon"

            // Populate views with data
            usernameTextView.text = creatorName
            locationTextView.text = location
//            coordsTextView.text = latLonString
            titleTextView.text = title
            detailsTextView.text = details
            dateTextView.text = date
            if (bioPhoto.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(bioPhoto)
                    .placeholder(R.drawable.baseline_account_circle_24) // Placeholder image while loading
                    .error(R.drawable.baseline_account_circle_24)
                    .circleCrop()
                    .into(userProfileImageView!!)
            }

            val followingLayout = followingView.findViewById<LinearLayout>(R.id.bookmarkLayout)
            followingLayout.visibility = View.VISIBLE

            val bookmarkIcon = followingLayout.findViewById<ImageView>(R.id.bookmarkIcon)

            bookmarkIcon.setOnClickListener {
                createBookmarkRequest(pinId)
                bookmarkIcon.setImageResource(R.drawable.baseline_bookmark_added_24)
                Log.d("follower page", "bookmark icon clicked")
            }

            // Add bookmark layout to your main layout
            val containerLayout = view?.findViewById<LinearLayout>(R.id.containerLayout)
            containerLayout?.addView(followingView)
        }
    }

    private fun setupViewPager(viewPager: ViewPager, indicatorLayout: LinearLayout) {
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                updateIndicators(position, indicatorLayout)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun setupIndicators(numPages: Int, indicatorLayout: LinearLayout) {
        indicatorLayout.removeAllViews()

        val indicatorDots = arrayOfNulls<ImageView>(numPages)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Add padding between each circle
        params.setMargins(8, 0, 8, 0)

        for (i in indicatorDots.indices) {
            indicatorDots[i] = ImageView(requireContext())
            indicatorDots[i]?.setImageResource(R.drawable.indicator_dot)
            indicatorDots[i]?.layoutParams = params
            indicatorLayout.addView(indicatorDots[i])
        }

        updateIndicators(0, indicatorLayout)
    }

    private fun updateIndicators(position: Int, indicatorLayout: LinearLayout) {
        for (i in 0 until indicatorLayout.childCount) {
            val imageView = indicatorLayout.getChildAt(i) as ImageView
            imageView.alpha = if (i == position) 1.0f else 0.5f
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FollowingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FollowingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}