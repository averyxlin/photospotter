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
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
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
 * Use the [BookmarksFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BookmarksFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var indicatorDots: Array<ImageView?>? = null
    private lateinit var viewPager: ViewPager
    private lateinit var bookmarksList: JSONArray
    private lateinit var loadingIndicator: ProgressBar
    private var currentUserId = 0
    private lateinit var auth: FirebaseAuth
    private var authUser: FirebaseUser? = null
    private var authToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        authUser = auth.currentUser
        authUser?.getIdToken(true)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Token retrieval successful
                    val tokenResult = task.result?.token
                    // Do something with the token
                    authToken = tokenResult
                    Log.d("authToken", "$authToken")
                } else {
                    // Token retrieval failed
                    Log.e("authToken", "Failed to retrieve token: ${task.exception}")
                }
            }

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
        return inflater.inflate(R.layout.fragment_bookmarks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        currentUserId = AuthManager.getUserId(requireContext()) ?: 1
        Log.d("currentUserId", "$currentUserId")

        // Call the function to make the HTTP request here or wherever it's appropriate in your fragment's lifecycle
        CoroutineScope(Dispatchers.IO).launch {
            getBookmarksRequest()
        }
    }

    private fun fetchUserDetails(userId: Int): JsonObject? {
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

    private suspend fun getBookmarksRequest() {
        try {

            activity?.runOnUiThread { loadingIndicator.visibility = View.VISIBLE }
            val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/users/$currentUserId/bookmarks")
            val connection = url.openConnection() as HttpURLConnection

            // Set request method
            connection.requestMethod = "GET"

            // Set authorization header if authToken is not null
            authToken?.let {
                connection.setRequestProperty("Authorization", "$it")
            }

            // Get response code
            val responseCode = connection.responseCode
            Log.d("Response", "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response body
                val inputStream = connection.inputStream
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                // Parse JSON
                val jsonArray = JsonParser.parseString(jsonString).asJsonArray
                processResponse(jsonArray)
            } else {
                Log.e("Response", "HTTP request failed with response code: $responseCode")
            }

        } catch (e: Exception) {
            // Log any errors
            Log.e("Response", "Error making request: ${e.message}", e)
        } finally {
            // Hide loading indicator when data loading ends (whether successful or not)
            activity?.runOnUiThread { loadingIndicator.visibility = View.GONE }
        }
    }

    private fun deleteBookmarkRequest(pinId: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/users/$currentUserId/bookmarks")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
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

    private suspend fun getUserRequest(id: Int): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users/$id")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                authToken?.let {
                    connection.setRequestProperty("Authorization", "$it")
                }

                // Get response code
                val responseCode = connection.responseCode

                // Read response body
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                val body = JSONObject(response.toString())
                Log.d("get user request", "json: $body")
                JSONObject(response.toString())

            } catch (e: Exception) {
                null
            }
        }
    }

    private fun processResponse(response: JsonArray) {
        Log.d("processResponse response", "$response")
        for (element in response) {
            val bookmark = element.asJsonObject
            val pinId = bookmark["id"].asInt
            val userId = bookmark["userId"].asInt
//            var creatorName: String? = null
//            var bioPhoto: String? = null
//
//            CoroutineScope(Dispatchers.IO).async {
//                val userObject = fetchUserDetails(userId)
//                if (userObject != null) {
//                    creatorName = userObject["name"].asString
//                    bioPhoto = userObject["bioPhoto"].asString
//                }
//            }
//            val userObject = fetchUserDetails(userId)
//            val creatorName = userObject?.getAsJsonPrimitive("name")?.asString ?: ""
//            val bioPhoto = userObject?.getAsJsonPrimitive("bioPhoto")?.asString ?: ""
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

            populateUI(userId, pinId, location, lat, lon, title, details, photos, date)
        }
    }

    private fun createLike(pinId: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/$pinId/likes")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

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
                Log.d("Likes", "Create like - Response code: $responseCode, Response body: $response")
                connection.disconnect()

            } catch (e: Exception) {
                Log.e("ResponseData", "Error making request: ${e.message}", e)
            }
        }
    }

    private fun deleteLike(pinId: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/$pinId/likes")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

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
                Log.d("likes", "Delete Response code: $responseCode, Response body: $response")
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("ResponseData", "Error making request: ${e.message}", e)
            }
        }
    }

    private suspend fun getPin(id: Int): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/$id")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                authToken?.let {
                    connection.setRequestProperty("Authorization", "$it")
                }

                // Get response code
                val responseCode = connection.responseCode

                // Read response body
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                val pin = JSONObject(response.toString())
                pin
            } catch (e: Exception) {
                Log.e("ResponseData", "Error making request: ${e.message}", e)
                null
            }
        }
    }

    private fun populateUI(
        userId: Int,
        pinId: Int,
        location: String,
        lat: String,
        lon: String,
        title: String,
        details: String,
        photos: List<String>,
        date: String,
    ) {
        activity?.runOnUiThread {
            val containerLayout = view?.findViewById<LinearLayout>(R.id.containerLayout)
            // Inflate the layout for each bookmark dynamically
            val bookmarkView = layoutInflater.inflate(R.layout.condensed_post, null)
            val viewPager = bookmarkView.findViewById<ViewPager>(R.id.viewPager)
            val adapter = PhotoPagerAdapter(requireContext(), photos)
            viewPager.adapter = adapter
            val editDeleteButtonsLayout = bookmarkView.findViewById<LinearLayout>(R.id.editDeleteButtonsLayout)
            editDeleteButtonsLayout.visibility = View.GONE

            // Get the indicator layout for this bookmark
            val indicatorLayout = bookmarkView.findViewById<LinearLayout>(R.id.indicatorLayout)

            // Setup indicators for this bookmark
            setupIndicators(photos.size, indicatorLayout)

            setupViewPager(viewPager, indicatorLayout)

            // Find views inside bookmark layout
            val usernameTextView = bookmarkView.findViewById<TextView>(R.id.usernameTextView)
            val locationTextView = bookmarkView.findViewById<TextView>(R.id.locationTextView)
            val titleTextView = bookmarkView.findViewById<TextView>(R.id.titleTextView)
            val detailsTextView = bookmarkView.findViewById<TextView>(R.id.detailsTextView)
            val dateTextView = bookmarkView.findViewById<TextView>(R.id.dateTextView)
            val userProfileImageView = bookmarkView.findViewById<ImageView>(R.id.userProfileImageView)
            CoroutineScope(Dispatchers.Main).launch {
                val pin = getPin(pinId)
                Log.d("getPin", "pin: $pin")
                val comments = pin?.getJSONArray("comments")
                Log.d("getPin", "comments: $comments")
                val likes = pin?.getInt("likes")
                Log.d("getPins", "likes: $likes")

                // likes
                val likeCount = bookmarkView.findViewById<TextView>(R.id.likeCount)
                val likeButton = bookmarkView.findViewById<ImageView>(R.id.thumbsUp)
                likeCount?.text = pin?.getInt("likes").toString()

                var isLiked = false
                if (likeButton != null) {
                    Log.d("likes", "like button not null")
//                        CoroutineScope(Dispatchers.Main).launch {
                    val likeArray = pin?.getJSONArray("likedByUserIds")
                    Log.d("likes", "likearray: $likeArray")
                    if (likeArray != null) {
                        for (i in 0 until likeArray.length()) {
                            val like = likeArray.getInt(i)
                            Log.d("likes", "like: $like")
                            if (like == currentUserId) {
                                Log.d("likes", "post liked by user before")
                                // Current user liked this post before
                                likeButton.setImageResource(R.drawable.baseline_thumb_up_24)
                                isLiked = true
                                likeButton.isEnabled = false
                            } else {
                                Log.d("likes", "post not liked by user before")
                                // Current user didn't like this post before
                                likeButton.setImageResource(R.drawable.baseline_thumb_up_off_alt_24)
                                isLiked = false
                                likeButton.isEnabled = true
                            }

                        }
                    }
//                        }
                }

                likeButton.setOnClickListener {
                    Log.d("Like", "bookmark Like icon clicked")
                    if (isLiked) {
                        Log.d("likes", "REMOVE LIKES")
                        deleteLike(pinId)
                        likeButton.setImageResource(R.drawable.baseline_thumb_up_off_alt_24)
                        isLiked = false
                        val currentLikes = likeCount?.text.toString().toIntOrNull() ?: 0
                        val newLikes = currentLikes - 1
                        likeCount?.text = newLikes.toString()

                    } else {
                        Log.d("likes", "CREATE LIKES")
                        createLike(pinId)
                        likeButton.setImageResource(R.drawable.baseline_thumb_up_24)
                        isLiked = true
                        val currentLikes = likeCount?.text.toString().toIntOrNull() ?: 0
                        val newLikes = currentLikes + 1
                        likeCount?.text = newLikes.toString()
                    }
                }

                // Fetch user details
                val userObject = getUserRequest(userId)
                val authorName = userObject?.optString("name")
                val bioPhoto = userObject?.optString("bioPhoto")

                // Load user profile image
                bioPhoto?.let {
                    Glide.with(requireContext())
                        .load(it)
                        .placeholder(R.drawable.baseline_account_circle_24)
                        .error(R.drawable.baseline_account_circle_24)
                        .circleCrop()
                        .into(userProfileImageView!!)
                }


                // Populate views with data
                usernameTextView.text = authorName
                locationTextView.text = location
                titleTextView.text = title
                detailsTextView.text = details
                dateTextView.text = date

                val bookmarkLayout = bookmarkView.findViewById<LinearLayout>(R.id.bookmarkLayout)
                bookmarkLayout.visibility = View.VISIBLE

                val bookmarkIcon = bookmarkLayout.findViewById<ImageView>(R.id.bookmarkIcon)
                bookmarkIcon.setImageResource(R.drawable.baseline_bookmark_remove_24)


                bookmarkIcon.setOnClickListener {
                    deleteBookmarkRequest(pinId)
                    containerLayout?.removeView(bookmarkView)
                    Log.d("follower page", "bookmark icon clicked")
                }

                // Add bookmark layout to your main layout
                containerLayout?.addView(bookmarkView)
            }
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

    private fun logJsonObject(jsonObject: JsonObject, prefix: String = "") {
        for ((key, value) in jsonObject.entrySet()) {
            if (value.isJsonObject) {
                // Recursively log nested JSON objects
                logJsonObject(value.asJsonObject, "$prefix$key.")
            } else if (value.isJsonArray) {
                // Handle arrays
                val jsonArray = value.asJsonArray
                for ((index, element) in jsonArray.withIndex()) {
                    if (element.isJsonObject) {
                        // Recursively log nested JSON objects within the array
                        logJsonObject(element.asJsonObject, "$prefix$key[$index].")
                    } else {
                        Log.d("Response", "$prefix$key[$index]: ${element.asString}")
                    }
                }
            } else {
                // Log non-nested properties
                Log.d("Response", "$prefix$key: ${value.asString}")
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BookmarksFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BookmarksFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
