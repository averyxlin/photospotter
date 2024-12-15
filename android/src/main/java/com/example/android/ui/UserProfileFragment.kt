package com.example.android.ui

import AuthManager
import PhotoPagerAdapter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.example.android.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

private const val ARG_PARAM1 = "param1"
class UserProfileFragment : Fragment() {
    private var param1: String? = null
    private lateinit var jsonObject: JSONObject
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var postLoadingIndicator: ProgressBar
    private lateinit var fragment_container: View
    private var currentUserId = 0
    private lateinit var auth: FirebaseAuth
    private var authUser: FirebaseUser? = null
    private var authToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("User profile", "ENTERED USER PROFILE FRAGMENT")
        super.onViewCreated(view, savedInstanceState)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        postLoadingIndicator = view.findViewById(R.id.postLoadingIndicator)
        fragment_container = view.findViewById(R.id.fragment_container)
//        authId = AuthManager.getAuthId(requireContext()) ?: ""
        val backButtonImageView = view.findViewById<ImageView>(R.id.backButtonImageView)
        loadingIndicator.visibility = View.VISIBLE

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
                    Log.d("authToken", "$authToken")
                    CoroutineScope(Dispatchers.IO).launch {
//            currentUserId = getUserByAuthIdRequest(authId) ?: 1
                        Log.d("User profile", "param1: $param1")
                        val paramObject = JSONObject(param1)
                        val userId = paramObject.getInt("id")
                        makeUserRequest(userId)
                        makePostRequest(userId)
                        activity?.runOnUiThread {
                            loadingIndicator.visibility = View.GONE
                        }
                    }
                } else {
                    // Token retrieval failed
                    Log.e("authToken", "Failed to retrieve token: ${task.exception}")
                }
            }

        backButtonImageView.setOnClickListener {
            Log.d("User profile", "Back button clicked")
            requireActivity().supportFragmentManager.popBackStack()
        }

    }

    private fun createFollowing(id: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users/$currentUserId/following")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                authToken?.let {
                    Log.d("meow", "$authToken")
                    connection.setRequestProperty("Authorization", "$it")
                }

                val requestBody = """
                {
                    "followerId": "$currentUserId",
                    "followeeId": "$id"
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
                Log.d("Following", "Response code: $responseCode, Response body: $response")
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("ResponseData", "Error making request: ${e.message}", e)
            }
        }
    }

    private fun deleteFollowing(id: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users/$id/following")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "DELETE"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                authToken?.let {
                    connection.setRequestProperty("Authorization", "$it")
                }

                val requestBody = """
                {
                    "followerId": "$currentUserId",
                    "followeeId": "$id"
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
                Log.d("following", "Response code: $responseCode, Response body: $response")
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("ResponseData", "Error making request: ${e.message}", e)
            }
        }
    }

    private fun makeUserRequest(userId: Int) {
        try {
            activity?.runOnUiThread { loadingIndicator.visibility = View.VISIBLE }

            val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users/$userId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            // Set request properties before opening the connection
            authToken?.let {
                Log.d("meowmeow", "$authToken")
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
            Log.d("UserProfileData", "Response body: $responseCode, $response")

            // Parse JSON response
            val jsonObject = JSONObject(response.toString())
            // Update UI with parsed data
            updateUI(jsonObject)
        } catch (e: Exception) {
            // Log any errors
            Log.e("UserProfileData", "Error making request: ${e.message}", e)
        } finally {
            // Hide loading indicator when data loading ends (whether successful or not)
            activity?.runOnUiThread { loadingIndicator.visibility = View.GONE }
            activity?.runOnUiThread { fragment_container.visibility = View.VISIBLE }
        }
    }

    private suspend fun makePostRequest(userId: Int) {
        try {
            activity?.runOnUiThread{ postLoadingIndicator.visibility = View.VISIBLE}
            Log.d("loading", "visible")
            val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/users/$userId")
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
            activity?.runOnUiThread {postLoadingIndicator.visibility = View.GONE}
            Log.d("loading", "gone")
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

    private suspend fun processResponse(response: JsonArray) {
        Log.d("ResponseData", "Response body for post: $response")
        CoroutineScope(Dispatchers.Main).launch {
            val size = response.size()
            Log.d("User profile", "response size: $size")
            val pinCount = view?.findViewById<TextView>(R.id.pins_count)
            pinCount?.text = (response.size() ?: 0).toString()

        }

        for (el in response) {
            Log.d("ResponseData", "Current pin: $el")
            // Parse JSON response
            val jsonObject = JSONObject(el.toString())
            Log.d("ResponseData", "jsonObject")
            val pinId = jsonObject.getInt("id")
            val pinObject = getPin(pinId)

            val name = jsonObject.optString("name", "")
//            val userId = jsonObject.optString("userId", "")
            val creatorName = JSONObject(param1).getString("name") ?: "Unknown User"//fetchUserDetails(userId) ?: "Unknown User"
            val location = jsonObject.optString("address", "")
            val lat = jsonObject.optString("lat", "")
            val lon = jsonObject.optString("lon", "")
            val desc = if (jsonObject.has("desc")) {
                jsonObject.getString("desc")
            } else {
                "No desc"
            }
            val createdAt = jsonObject.getString("createdAt").substringBefore('T')

            Log.d("PinResponse", "jsonobject: $jsonObject" )

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

            activity?.runOnUiThread {
                // Inflate the layout for each bookmark dynamically
                val bookmarkView = layoutInflater.inflate(R.layout.condensed_post, null)
                bookmarkView.tag = pinId

                val viewPager = bookmarkView.findViewById<ViewPager>(R.id.viewPager)
                val adapter = PhotoPagerAdapter(requireContext(), photos)
                viewPager.adapter = adapter

                // Get the indicator layout for this bookmark
                val indicatorLayout = bookmarkView.findViewById<LinearLayout>(R.id.indicatorLayout)

                // Setup indicators for this bookmark
                setupIndicators(photos.size, indicatorLayout)

                setupViewPager(viewPager, indicatorLayout)

                // Find views inside bookmark layout
                val usernameTextView = bookmarkView.findViewById<TextView>(R.id.usernameTextView)
                val locationTextView = bookmarkView.findViewById<TextView>(R.id.locationTextView)
//                val coordsTextView = bookmarkView.findViewById<TextView>(R.id.coordsTextView)
                val titleTextView = bookmarkView.findViewById<TextView>(R.id.titleTextView)
                val detailsTextView = bookmarkView.findViewById<TextView>(R.id.detailsTextView)
                val dateTextView = bookmarkView.findViewById<TextView>(R.id.dateTextView)

//                val latLonString = "$lat, $lon"

                // Populate views with data
                usernameTextView.text = creatorName
                locationTextView.text = location
//                coordsTextView.text = latLonString
                titleTextView.text = name
                detailsTextView.text = desc
                dateTextView.text = createdAt

                val tags = pinObject?.optJSONArray("tags") // Provide an empty JSONArray as default if "tags" is null
                Log.d("user profile", "check if tags is null")
                if (tags != null) {
                    Log.d("user profile", "tags is not null")
                    for (i in 0 until tags.length()) {
                        val tagObject = tags.getJSONObject(i)
                        Log.d("getPin", "pin: $tagObject")
                        val tagName = tagObject.getString("name")
                        Log.d("getPin", "tagName: $tagName")

                        val tagsLayout = bookmarkView.findViewById<LinearLayout>(R.id.tagsContainer)
                        val tagView = layoutInflater.inflate(R.layout.tag, null)
                        val tagNameView = tagView?.findViewById<Button>(R.id.button)

                        tagNameView?.text = tagName
                        tagNameView?.backgroundTintList = ColorStateList.valueOf(getRandomColor(tagObject.getInt("tagId")))
                        Log.d("user profile", "set text in tags")
                        tagsLayout?.addView(tagView)
                    }
                }


                val bookmarkLayout = bookmarkView.findViewById<LinearLayout>(R.id.bookmarkLayout)
                bookmarkLayout.visibility = View.VISIBLE

                val bookmarkIcon = bookmarkLayout.findViewById<ImageView>(R.id.bookmarkIcon)

                bookmarkIcon.setOnClickListener {
                    Log.d("user profile", "bookmark icon clicked")
                }

                // Add bookmark layout to your main layout
                val posts_layout = view?.findViewById<LinearLayout>(R.id.posts_layout )
                posts_layout?.addView(bookmarkView)
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
            val green = Random.nextInt(100, 200)
            val blue = Random.nextInt(100, 200)
            val newColor = Color.rgb(red, green, blue)
            TagColours.setColor(tagId, newColor)

            return newColor
        }
    }

    private suspend fun getFollowing(): JsonArray? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users/$currentUserId/following")
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
                val following = JsonParser().parse(response.toString()).asJsonArray
                Log.d("following", "get following response body: $following")
                following
            } catch (e: Exception) {
                Log.e("ResponseData", "Error making request: ${e.message}", e)
                null
            }
        }
    }

    private fun updateUI(userObject: JSONObject) {
        activity?.runOnUiThread {
            // Update TextViews with parsed data
            val textViewName = view?.findViewById<TextView>(R.id.profile_name)
            val textViewFollowingCount = view?.findViewById<TextView>(R.id.following_count)
            val textViewFollowerCount = view?.findViewById<TextView>(R.id.followers_count)
            val textViewBio = view?.findViewById<TextView>(R.id.profile_description)
//            val linksContainer = view?.findViewById<LinearLayout>(R.id.linksContainer)

//            val linksLayout = view?.findViewById<LinearLayout>(R.id.linksContainer)
            val igLinkView = layoutInflater.inflate(R.layout.tag, null)
            val siteLinkView = layoutInflater.inflate(R.layout.tag, null)
            val igLinkNameView = igLinkView?.findViewById<Button>(R.id.button)
            val siteLinkNameView = siteLinkView?.findViewById<Button>(R.id.button)

//            val igURLView = view?.findViewById<Button>(R.id.igURL)
//            val siteURLView = view?.findViewById<Button>(R.id.siteURL)

            val userId = userObject.getInt("id")
            val userName = userObject.getString("name")
            val followerCount = userObject.getInt("followerCount")
            val followingCount = userObject.getInt("followingCount")
            val bio = userObject.optString("bio")
            val igURL = userObject.optString("igURL")
            val siteURL = userObject.optString("siteURL")
            Log.d("User profile", "igurl: $igURL, siteURL: $siteURL")


            var isFollowing = false
            // Follow button
            val followButton = view?.findViewById<Button>(R.id.followButton)
            if (followButton != null) {
                followButton.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.Main).launch {
                    val followingArray = getFollowing()
                    if (followingArray != null) {
                        for (i in 0 until followingArray.size()) {
                            val followingObject = followingArray[i]
                            val followingUserId = followingObject.asJsonObject["id"].asInt
                            Log.d("following", "following user id: $followingUserId")
                            if (userId == followingUserId) {
                                Log.d("following", "we follow this user")
//                                followButton.setBackgroundColor(context?.let { ContextCompat.getColor(it, R.color.light_grey) }
//                                    ?: Color.GRAY)
                                followButton.text = "Following"
                                followButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#808080"))
                                isFollowing = true
                                Log.d("following", "we follow this user UI")
                            } else {
                                Log.d("following", "not following")
                                isFollowing = false
                                followButton.text = "Follow"
                                followButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9C27B0"))
                                Log.d("following", "we dont follow this user UI")
                            }
                        }
                    }
                }
            }

            followButton?.setOnClickListener{
                Log.d("following", "follow button clicked")
                if (isFollowing) {
                    Log.d("following", "REMOVE FOLLOWING")
                    deleteFollowing(userId)
                    followButton.text = "Follow"
                    followButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9C27B0"))
                    isFollowing = false
                    val currentCount = textViewFollowerCount?.text?.toString()?.toIntOrNull() ?: 0
                    val newCount = currentCount - 1
                    textViewFollowerCount?.text = newCount.toString()
                } else {
                    Log.d("following", "CREATE FOLLOWING")
                    createFollowing(userId)
                    followButton.text = "Following"
                    followButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#808080"))
                    isFollowing = true
                    val currentCount = textViewFollowerCount?.text?.toString()?.toIntOrNull() ?: 0
                    val newCount = currentCount + 1
                    textViewFollowerCount?.text = newCount.toString()
                }

            }

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
//            if (siteURL != "") {
//                siteLinkNameView?.text = siteURL
//                siteLinkNameView?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DFDFDF"))
//                siteLinkNameView?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_link_24, 0, 0, 0)
//                siteLinkNameView?.setTextColor(Color.BLACK)
//                linksLayout?.addView(siteLinkView)
//            }

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
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: JSONObject) =
            UserProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1.toString())
                }
            }
    }
}