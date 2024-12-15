package com.example.android.ui

import AuthManager
import PhotoPagerAdapter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.example.android.R
import com.example.android.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random
import android.content.Intent
import android.net.Uri
import java.io.OutputStream


class ProfileFragment : Fragment() {
    private lateinit var jsonObject: JSONObject
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var postLoadingIndicator: ProgressBar
    private lateinit var fragment_container: View
    private lateinit var popupWindow: PopupWindow
    private var currentUserId = 0
    private lateinit var auth: FirebaseAuth
    private var authUser: FirebaseUser? = null
    private var authToken: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        authUser = auth.currentUser
        authUser?.getIdToken(true)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Token retrieval successful
                    val tokenResult = task.result?.token
                    // Do something with the token
                    authToken = tokenResult
                    Log.d("profileAuth", "$authToken")
                } else {
                    // Token retrieval failed
                    Log.e("profileAuth", "Failed to retrieve token: ${task.exception}")
                }
            }

        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        postLoadingIndicator = view.findViewById(R.id.postLoadingIndicator)
        fragment_container = view.findViewById(R.id.fragment_container)
        val settingsImageView = view.findViewById<ImageView>(R.id.settingsImageView)

        currentUserId = AuthManager.getUserId(requireContext()) ?: 1
        Log.d("currentUserId", "$currentUserId")

        CoroutineScope(Dispatchers.IO).launch {
            getUserRequest()
            getPinsByUserIdRequest()
        }

        settingsImageView.setOnClickListener {
            val newFragment = SettingsFragment.newInstance(jsonObject.toString())
            val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
            transaction.replace(R.id.fragment_container, newFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    private fun getUserRequest() {
        try {
            activity?.runOnUiThread { loadingIndicator.visibility = View.VISIBLE }
            var currentUserId = AuthManager.getUserId(requireContext()) ?: 1
            val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users/$currentUserId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            authToken?.let {
                connection.setRequestProperty("Authorization", "$it")
            }

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
            Log.d("ResponseData", "Response body: $responseCode, $response")

            // Parse JSON response
            jsonObject = JSONObject(response.toString())

            // Update UI with parsed data
            updateUI(jsonObject)
        } catch (e: Exception) {
            // Log any errors
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
            activity?.finish()
            Log.e("ResponseData", "Error making request: ${e.message}", e)
        } finally {
            // Hide loading indicator when data loading ends (whether successful or not)
            activity?.runOnUiThread { loadingIndicator.visibility = View.GONE }
            activity?.runOnUiThread { fragment_container.visibility = View.VISIBLE }
        }
    }

    private suspend fun getPinsByUserIdRequest() {
        try {
            activity?.runOnUiThread{ postLoadingIndicator.visibility = View.VISIBLE}
            var currentUserId = AuthManager.getUserId(requireContext()) ?: 1
            val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/users/$currentUserId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            authToken?.let {
                connection.setRequestProperty("Authorization", "$it")
            }

            // Get response code
            val responseCode = connection.responseCode
            Log.d("ResponseData", "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response body
                val inputStream = connection.inputStream
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                // Parse JSON
                val jsonArray = JsonParser.parseString(jsonString).asJsonArray
                Log.d("Responsedata", "Json parsed: $jsonArray")
                processResponse(jsonArray)
            } else {
                Log.e("Response", "HTTP request failed with response code: $responseCode")
            }

        } catch (e: Exception) {
            // Log any errors
            Log.e("ResponseData", "Error making request: ${e.message}", e)
        } finally {
            // Hide loading indicator when data loading ends (whether successful or not)
            activity?.runOnUiThread { loadingIndicator.visibility = View.GONE }
            activity?.runOnUiThread { fragment_container.visibility = View.VISIBLE }
            activity?.runOnUiThread {postLoadingIndicator.visibility = View.GONE}
            Log.d("loading", "gone again")
        }
    }

    private fun getUserByUserIdRequest(): String? {
        return try {
            var currentUserId = AuthManager.getUserId(requireContext()) ?: 1
            val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users/$currentUserId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            authToken?.let {
                connection.setRequestProperty("Authorization", "$it")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JsonParser.parseString(jsonString).asJsonObject
                jsonObject["name"].asString // Extract and return the user's name
            } else {
                Log.e("Response", "HTTP request failed with response code: $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e("Response", "Error fetching user details: ${e.message}", e)
            null
        }
    }

    private suspend fun getPin(id: Int): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/$id")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

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

    private suspend fun openPopup(pinObject: JSONObject) {
        val popupView = layoutInflater.inflate(R.layout.popup_marker_info, null)
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val popupWidth = (screenWidth * 0.85).toInt() // 80% of screen width
        val popupHeight = (screenHeight * 0.78).toInt() // 60% of screen height
        popupWindow = PopupWindow(popupView, popupWidth, popupHeight, true)
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        val viewPager = popupView.findViewById<ViewPager>(R.id.viewPager)
//        val pinObject = marker.tag as JSONObject
        val pinId = pinObject.getInt("id")
        CoroutineScope(Dispatchers.Main).launch {
            val pin = getPin(pinId)
            val photos = pin?.optJSONArray("photos")
            val photoUrls = if (photos != null && photos.length() > 0) {
                val photoUrlsList = mutableListOf<String>()
                for (i in 0 until photos.length()) {
                    photoUrlsList.add(photos.getString(i))
                }
                photoUrlsList
            } else {
                listOf(
                    "https://images.unsplash.com/photo-1642425149556-b6f90e946859?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                    "https://images.unsplash.com/photo-1690373403498-a29da916ecf9?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                    "https://images.unsplash.com/photo-1710179337706-f5e304f7740a?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
                )
            }

            val adapter = PhotoPagerAdapter(requireContext(), photoUrls)
            viewPager.adapter = adapter
            val indicatorLayout = popupView.findViewById<LinearLayout>(R.id.indicatorLayout)
//            setupIndicators(photoUrls.size, indicatorLayout)
//            setupViewPager(viewPager, indicatorLayout)

            // Get views from layout file
            val backButton = popupView.findViewById<ImageView>(R.id.backButtonImageView)
            val bookmarkButton = popupView.findViewById<ImageView>(R.id.bookmarkIcon)
            val userProfileTextView = popupView.findViewById<TextView>(R.id.usernameTextView)
            val nameTextView = popupView.findViewById<TextView>(R.id.titleTextView)
            val addressTextView = popupView.findViewById<TextView>(R.id.locationTextView)
            val descTextView = popupView.findViewById<TextView>(R.id.detailsTextView)
            val dateTExtView = popupView.findViewById<TextView>(R.id.dateTextView)
            val userProfileView = popupView.findViewById<ImageView>(R.id.userProfileImageView)
            val userNameView = popupView.findViewById<TextView>(R.id.usernameTextView)

            // Data fields for expanded post
//            val pinObject = marker.tag as JSONObject
            val pinId = pinObject.getInt("id")
            val userId = pinObject.getInt("userId")
            val name = pinObject.getString("name")
            val address = pinObject.getString("address")
            val desc = if (pinObject.has("desc")) {
                pinObject.getString("desc")
            } else {
                "No desc"
            }
            val date = pinObject.getString("createdAt").substringBefore('T')

            userProfileView.setOnClickListener {
                Log.d("User profile", "User profile pic clicked")
//                handleUserProfileClick(userId)
            }

            userNameView.setOnClickListener {
                Log.d("User profile", "User profile name clicked")
//                handleUserProfileClick(userId)
            }

            bookmarkButton.setOnClickListener {
//                createBookmarkRequest(pinId)
                bookmarkButton.setImageResource(R.drawable.baseline_bookmark_added_24)
            }

            CoroutineScope(Dispatchers.Main).launch {
                val pin = getPin(pinId)
                Log.d("getPin", "pin: $pin")
                val comments = pin?.getJSONArray("comments")
                Log.d("getPin", "comments: $comments")
                val likes = pin?.getInt("likes")
                Log.d("getPins", "likes: $likes")

                // likes
                val likeCount = popupView.findViewById<TextView>(R.id.likeCount)
                val likeButton = popupView.findViewById<ImageView>(R.id.thumbsUp)
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
                    Log.d("Like", "Like icon clicked")
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

                // create comment
                val editComment = popupView.findViewById<EditText>(R.id.editComment)
                val sendIcon = popupView.findViewById<ImageView>(R.id.sendIcon)
                sendIcon.setOnClickListener {
                    Log.d("Edit comment", "clicked edit comment")
                    val commentField = editComment?.text.toString()
                    Log.d("Comments", "commentfield: $commentField")
//                    createComment(pinId, commentField)
                    Log.d("Comments", "set edit field to null")
                    editComment.text = null
                }

                if (comments != null) {
//                    populateComments(comments)
                }

                val tags = pin?.getJSONArray("tags")
                Log.d("getPin", "$tags")
                if (tags != null) {
                    for (i in 0 until tags.length()) {
                        val tagObject = tags.getJSONObject(i)
                        Log.d("getPin", "pin: $tagObject")
                        val tagName = tagObject.getString("name")
                        Log.d("getPin", "tagName: $tagName")


                        val tagsLayout = popupView.findViewById<LinearLayout>(R.id.tagsContainer)
                        val tagView = layoutInflater.inflate(R.layout.tag, null)
                        val tagNameView = tagView?.findViewById<Button>(R.id.button)

                        tagNameView?.text = tagName
                        tagNameView?.backgroundTintList =
                            ColorStateList.valueOf(getRandomColor(tagObject.getInt("tagId")))
                        Log.d("getPin", "set text in tags")
                        tagsLayout?.addView(tagView)

                    }
                }

                val loadingIndicatorFrame = popupView.findViewById<FrameLayout>(R.id.loadingLayout)
                val loadingIndicator = popupView.findViewById<ProgressBar>(R.id.loadingProgressBar)
                loadingIndicator.visibility = View.VISIBLE

                val authorNameDeferred = async(Dispatchers.IO) {
                    val userObject = getUserRequest(userId)
                    userObject?.optString("name")
                }
                val authorName = authorNameDeferred.await()
                Log.d("authorName", "name: $authorName")

                nameTextView.text = name
                addressTextView.text = address
//                latTextView.text = lat.toString() + ","
//                lonTextView.text = lon.toString()
                descTextView.text = desc.toString()
                dateTExtView.text = date
                userProfileTextView.text = authorName.toString()

                loadingIndicator.visibility = View.GONE
                loadingIndicatorFrame.visibility = View.GONE
            }

            backButton.setOnClickListener {
                popupWindow.dismiss() // Assuming popupWindow is accessible here
            }
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
                Log.d("Likes", "Response code: $responseCode, Response body: $response")
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

    private fun getRandomColor(tagId: Int): Int {
            Log.d("Tags", "tagId: $tagId")
            val existingColor = TagColours.getColor(tagId)
            if (existingColor != null) {
                return existingColor
            } else {
                val red = Random.nextInt(100, 200)
                val green = Random.nextInt(0, 101)
                val blue = Random.nextInt(150, 256)
                val newColor = Color.rgb(red, green, blue)
                TagColours.setColor(tagId, newColor)

                return newColor
            }
        }
        private suspend fun getUserRequest(id: Int): JSONObject? {
            return withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users/$id")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"

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

    private suspend fun processResponse(response: JsonArray) {
        Log.d("ResponseData", "Response body for post: $response")
        CoroutineScope(Dispatchers.Main).launch {
            val size = response.size()
            Log.d("User profile", "response size: $size")
            val pinCount = view?.findViewById<TextView>(R.id.pins_count)
            pinCount?.text = (response.size() ?: 0).toString()
        }
        for (el in response) {
            // Parse JSON response
            val jsonObject = JSONObject(el.toString())
            val pinId = jsonObject.getInt("id")
            val name = jsonObject.optString("name", "")
            val creatorName = getUserByUserIdRequest() ?: "Unknown User"
            val location = jsonObject.optString("address", "")
            val lat = jsonObject.optString("lat", "")
            val lon = jsonObject.optString("lon", "")
            val desc = jsonObject.optString("desc", "")
            val createdAt = jsonObject.getString("createdAt").substringBefore('T')
            val bioPhoto = jsonObject.optString("bioPhoto", "")

            Log.d("Responsedata", "jsonobject: $jsonObject" )


            val photos = if (jsonObject.has("photos")) {
                val photosArray = jsonObject.getJSONArray("photos")
                Log.d("hasPhotos", "$photosArray")
                if (photosArray.length() > 0) {
                    // If photos exist in the JSON array, extract the URLs
                    (0 until photosArray.length()).map { photosArray.getString(it) }
                } else {
                    // If photos array is empty, return an empty list
                    listOf(
                        "https://images.unsplash.com/photo-1642425149556-b6f90e946859?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                        "https://images.unsplash.com/photo-1690373403498-a29da916ecf9?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                        "https://images.unsplash.com/photo-1710179337706-f5e304f7740a?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
                    )
                }
            } else {
                // If "photos" field does not exist, or is not an array, use a default list of URLs
                listOf(
                    "https://images.unsplash.com/photo-1642425149556-b6f90e946859?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                    "https://images.unsplash.com/photo-1690373403498-a29da916ecf9?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
                    "https://images.unsplash.com/photo-1710179337706-f5e304f7740a?q=80&w=3870&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
                )
            }


            CoroutineScope(Dispatchers.Main).launch {
                val pin = getPin(pinId)
                val profileView = layoutInflater.inflate(R.layout.condensed_post, null)
                profileView.tag = pinId
                val viewPager = profileView.findViewById<ViewPager>(R.id.viewPager)
                val adapter = PhotoPagerAdapter(requireContext(), photos)
                viewPager.adapter = adapter
                val indicatorLayout = profileView.findViewById<LinearLayout>(R.id.indicatorLayout)
                setupIndicators(photos.size, indicatorLayout)
                setupViewPager(viewPager, indicatorLayout)

                // Find views inside bookmark layout
                val usernameTextView = profileView.findViewById<TextView>(R.id.usernameTextView)
                val locationTextView = profileView.findViewById<TextView>(R.id.locationTextView)
//                val coordsTextView = bookmarkView.findViewById<TextView>(R.id.coordsTextView)
                val titleTextView = profileView.findViewById<TextView>(R.id.titleTextView)
                val detailsTextView = profileView.findViewById<TextView>(R.id.detailsTextView)
                val dateTextView = profileView.findViewById<TextView>(R.id.dateTextView)
                val likeCount = profileView.findViewById<TextView>(R.id.likeCount)
                val likeButton = profileView.findViewById<ImageView>(R.id.thumbsUp)
                val userProfileImageView = profileView.findViewById<ImageView>(R.id.userProfileImageView)

                val commentLayout = profileView.findViewById<LinearLayout>(R.id.commentLayout)
                commentLayout.setOnClickListener {
                    Log.d("profile", "comment button clicked")
//                    CoroutineScope(Dispatchers.IO).launch {
//                        openPopup(jsonObject)
//                    }
                }

                if (bioPhoto.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(bioPhoto)
                        .placeholder(R.drawable.baseline_account_circle_24) // Placeholder image while loading
                        .error(R.drawable.baseline_account_circle_24)
                        .circleCrop()
                        .into(userProfileImageView!!)
                }

                // likes
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
                    Log.d("Like", "Like icon clicked")
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


                // Populate views with data
                usernameTextView.text = creatorName
                locationTextView.text = location
//                coordsTextView.text = latLonString
                titleTextView.text = name
                detailsTextView.text = desc
                dateTextView.text = createdAt

                val tags = jsonObject.optJSONArray("tags") // Provide an empty JSONArray as default if "tags" is null
                if (tags != null && tags.length() > 0) {
                    for (i in 0 until tags.length()) {
                        val tagObject = tags.getJSONObject(i)
                        Log.d("getPin", "pin: $tagObject")
                        val tagName = tagObject.getString("name")
                        Log.d("getPin", "tagName: $tagName")

                        val tagsLayout = profileView.findViewById<LinearLayout>(R.id.tagsContainer)
                        val tagView = layoutInflater.inflate(R.layout.tag, null)
                        val tagNameView = tagView?.findViewById<Button>(R.id.button)

                        tagNameView?.text = tagName
                        tagNameView?.backgroundTintList = ColorStateList.valueOf(getRandomColor(tagObject.getInt("tagId")))
                        Log.d("user profile", "set text in tags")
                        tagsLayout?.addView(tagView)
                    }
                }


                val editDeleteButtonsLayout = profileView.findViewById<LinearLayout>(R.id.editDeleteButtonsLayout)
                editDeleteButtonsLayout.visibility = View.VISIBLE
                val deleteIcon = profileView.findViewById<ImageView>(R.id.deleteIcon)

                deleteIcon.setOnClickListener {
                    deletePin(pinId)
                }

                val posts_layout = view?.findViewById<LinearLayout>(R.id.posts_layout )
                posts_layout?.addView(profileView)
            }
        }
    }

    private fun editPin(pinId: String) {
        Log.d("Profile", "Edit icon clicked, $pinId")
    }

    private fun deletePin(pinId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/$pinId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"

                authToken?.let {
                    connection.setRequestProperty("Authorization", "$it")
                }

                // Get response code
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("profile", "Delete request successful")

                } else {
                    Log.e("DeletePin", "HTTP request failed with response code: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("DeletePin", "Error making delete request: ${e.message}", e)
            }
        }
    }


    private fun updateUI(userObject: JSONObject) {
        activity?.runOnUiThread {
            // Update TextViews with parsed data
            val textViewName = view?.findViewById<TextView>(R.id.profile_name)
            val textViewFollowingCount = view?.findViewById<TextView>(R.id.following_count)
            val textViewFollowerCount = view?.findViewById<TextView>(R.id.followers_count)
            val profilePicture = view?.findViewById<ImageView>(R.id.profile_image)
            val textViewBio = view?.findViewById<TextView>(R.id.profile_description)
//            val linksContainer = view?.findViewById<LinearLayout>(R.id.linksContainer)
//
//            val linksLayout = view?.findViewById<LinearLayout>(R.id.linksContainer)
            val igLinkView = layoutInflater.inflate(R.layout.tag, null)
            val siteLinkView = layoutInflater.inflate(R.layout.tag, null)
            val igLinkNameView = igLinkView?.findViewById<Button>(R.id.button)
            val siteLinkNameView = siteLinkView?.findViewById<Button>(R.id.button)

            val followButton = view?.findViewById<Button>(R.id.followButton)
            followButton?.visibility = View.GONE

            val settingsImageView = view?.findViewById<ImageView>(R.id.settingsImageView)
            settingsImageView?.visibility = View.VISIBLE

            val backButtonImageView = view?.findViewById<ImageView>(R.id.backButtonImageView)
            backButtonImageView?.visibility = View.GONE


//            val igURLView = view?.findViewById<Button>(R.id.igURL)
//            val siteURLView = view?.findViewById<Button>(R.id.siteURL)

            val userName = userObject.getString("name")
            val followerCount = userObject.getInt("followerCount")
            val followingCount = userObject.getInt("followingCount")
            val bio = userObject.optString("bio")
            val igURL = userObject.optString("igURL")
            val siteURL = userObject.optString("siteURL")
            val profilePictureUrl = userObject.optString("bioPhoto")
            Log.d("User profile", "igurl: $igURL, siteURL: $siteURL")

            textViewName?.text = "$userName"
            textViewFollowerCount?.text = "$followerCount"
            textViewFollowingCount?.text = "$followingCount"
            textViewBio?.text = if (bio.isEmpty()) "No bio" else "$bio"

//            if (igURL != "") {
//                igLinkNameView?.text = igURL.substringAfterLast("/")
//                igLinkNameView?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DFDFDF"))
//                igLinkNameView?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.instagram_svgrepo_com, 0, 0, 0)
//                igLinkNameView?.setTextColor(Color.BLACK)
//                linksLayout?.addView(igLinkView)
//            }
//
//            igLinkNameView?.setOnClickListener {
//                Log.d("profile", "ig link clicked")
//                val urlSubstring = igLinkNameView?.text.toString().trim()
//                Log.d("profile", "ig substring: $urlSubstring")
//                if (urlSubstring.isNotEmpty()) {
//                    val fullUrl = "https://instagram.com/$urlSubstring"
//                    Log.d("profile", "ig string: $fullUrl")
//                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
////                    intent.data = Uri.parse(fullUrl)
//
//                    if (intent.resolveActivity(requireContext().packageManager) != null) {
//                        startActivity(intent)
//                    } else {
//                        Toast.makeText(requireContext(), "App cannot handle this Instagram link", Toast.LENGTH_SHORT).show()
//                    }
//                    } else {
//                        Toast.makeText(requireContext(), "Instagram link is empty", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            siteLinkNameView?.setOnClickListener {
//                Log.d("profile", "site link clicked")
//                val url = siteLinkNameView?.text.toString()
//                Log.d("profile", "site string: $url")
//                val intent = Intent(Intent.ACTION_VIEW)
//                intent.data = Uri.parse(url)
//
//                if (intent.resolveActivity(requireContext().packageManager) != null) {
//                    startActivity(intent)
//                } else {
//                    Toast.makeText(requireContext(), "Site link doesn't exist", Toast.LENGTH_SHORT).show()
//                }
//            }
//            if (siteURL != "") {
//                siteLinkNameView?.text = siteURL
//                siteLinkNameView?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DFDFDF"))
//                siteLinkNameView?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_link_24, 0, 0, 0)
//                siteLinkNameView?.setTextColor(Color.BLACK)
//                linksLayout?.addView(siteLinkView)
//            }
            if (profilePictureUrl.isNotEmpty()) {
                // Using Glide for image loading
                Glide.with(requireContext())
                    .load(profilePictureUrl)
                    .placeholder(R.drawable.baseline_account_circle_24) // Placeholder image while loading
                    .error(R.drawable.baseline_account_circle_24)
                    .circleCrop()// Error image if unable to load
                    .into(profilePicture!!)
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

    companion object {}
}