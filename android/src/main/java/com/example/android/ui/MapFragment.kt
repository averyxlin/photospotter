package com.example.android.ui

import AuthManager
import PhotoPagerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.example.android.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var pinList: JSONArray
    private lateinit var tags: JSONArray
    private var currentUserId = 0
    private lateinit var tagsContainer: LinearLayout
    private lateinit var popupView: View
    private lateinit var popupWindow: PopupWindow
    private var selectedImageView: ImageView? = null
    private var imageUris = ArrayList<Uri?>()

    private var storage: FirebaseStorage? = null
    private lateinit var auth: FirebaseAuth
    private var authUser: FirebaseUser? = null
    private var authToken: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storage = FirebaseStorage.getInstance();
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

        view.findViewById<FrameLayout>(R.id.loadingLayout).visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            getPinsRequest()
        }
        currentUserId = AuthManager.getUserId(requireContext()) ?: 1
        Log.d("currentUserId", "$currentUserId")

        val searchBar = view.findViewById<EditText>(R.id.search_bar)
        val clearButton = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_clear_24)
        tagsContainer = view.findViewById(R.id.tagsContainer)

        // Attach TextWatcher to the search bar
        searchBar.setOnTouchListener { _, event ->
            // Check if the touch event is inside the bounds of the clear button
            if (event.action == MotionEvent.ACTION_UP &&
                event.x >= ((searchBar.width - searchBar.paddingEnd - clearButton?.intrinsicWidth!!) ?: 0)
            ) {
                // Clear the text in the EditText
                searchBar.setText("")
                true // Consume the touch event
            } else {
                false // Continue with default touch handling
            }
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterSearchBar(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    // Convert Bitmap to URI
                    val imageUri = getImageUri(requireActivity(), imageBitmap)
                    selectedImageView?.setImageURI(imageUri)
                    selectedImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageUris.add(imageUri)

                }
                REQUEST_IMAGE_PICK -> {
                    val imageUri = data?.data
                    selectedImageView?.setImageURI(imageUri)
                    selectedImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageUris.add(imageUri)
                }
            }
        }
    }

    private suspend fun uploadImageToFirebaseStorage(imageUri: Uri?): String? {
        return suspendCoroutine { continuation ->
            if (imageUri != null) {
                val filename = UUID.randomUUID().toString() // Generate a unique filename
                val ref = storage?.getReference("images/$filename")

                ref?.putFile(imageUri)
                    ?.addOnSuccessListener { taskSnapshot ->
                        // Image uploaded successfully
                        ref.downloadUrl.addOnSuccessListener { uri ->
                            // Image URL retrieved
                            val imageUrl = uri.toString()
                            Log.d("ImageURL", imageUrl)
                            // Pass the imageUrl to the continuation
                            continuation.resume(imageUrl)
                        }?.addOnFailureListener {
                            // Handle failure to retrieve image URL
                            continuation.resume(null)
                        }
                    }?.addOnFailureListener {
                        // Handle unsuccessful uploads
                        Log.e("ImageURL", it.message ?: "Unknown error")
                        continuation.resume(null)
                    }
            } else {
                // If imageUri is null, return null
                continuation.resume(null)
            }
        }
    }

    private fun getImageUri(context: Context, image: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, image, "Title", null)
        return Uri.parse(path)
    }


    private fun filterSearchBar(query: String) {
        if (query == "") {
            tagsContainer.visibility = View.VISIBLE
        }
        mMap.clear()
        val queryLowerCase = query.toLowerCase()

        // Iterate through pinList and filter pins based on the query
        for (i in 0 until pinList.length()) {
            val pinObject = pinList.getJSONObject(i)
            val name = pinObject.getString("name").toLowerCase() // Convert pin name to lower case
            Log.d("Tags", "$pinObject")
            if (pinObject.has("tags")) {
                val tags = pinObject.getJSONArray("tags")
                if (name.contains(queryLowerCase) || containsTag(queryLowerCase, tags)) {
                    // Add the pin to the map if it matches the query
                    val lat = pinObject.getDouble("lat")
                    val lon = pinObject.getDouble("lon")
                    val coordinate = LatLng(lat, lon)
                    val markerOptions = MarkerOptions().position(coordinate)
                    val marker = mMap.addMarker(markerOptions)

                    if (marker != null) {
                        marker.tag = pinObject
                    }
                }
            } else {
                if (name.contains(queryLowerCase)) {
                    // Add the pin to the map if it matches the query
                    val lat = pinObject.getDouble("lat")
                    val lon = pinObject.getDouble("lon")
                    val coordinate = LatLng(lat, lon)
                    val markerOptions = MarkerOptions().position(coordinate)
                    val marker = mMap.addMarker(markerOptions)

                    if (marker != null) {
                        marker.tag = pinObject
                    }
                }
            }
        }
    }

//    private fun filterTags(query: String) {
//        Log.d("Tag", "query: $query")
//        mMap.clear() // Clear the map before adding filtered pins
//        val queryLowerCase = query.toLowerCase() // Convert query to lower case for case-insensitive search
//
//        // Iterate through pinList and filter pins based on the query
//        for (i in 0 until pinList.length()) {
//            val pinObject = pinList.getJSONObject(i)
//            val tags = pinObject.getJSONArray("tags")
//
//            // Case-insensitive search by name
//            if (containsTag(queryLowerCase, tags)) {
//                // Add the pin to the map if it matches the query
//                val lat = pinObject.getDouble("lat")
//                val lon = pinObject.getDouble("lon")
//                val coordinate = LatLng(lat, lon)
//                val markerOptions = MarkerOptions().position(coordinate)
//                val marker = mMap.addMarker(markerOptions)
//
//                if (marker != null) {
//                    marker.tag = pinObject // Attach pin data to marker tag
//                }
//            }
//        }
//    }

    private fun containsTag(query: String, tags: JSONArray): Boolean {

        Log.d("Filter tag", "here")
        for (i in 0 until tags.length()) {
            val tagObject = tags.getJSONObject(i)
            val tagName = tagObject.getString("name").toLowerCase()
            if (tagName.contains(query)) {
                Log.d("Filter tag", "true")
                return true
            } else {
                Log.d("Filter tag", "false")
            }
        }
        return false
    }

    private fun getPinsRequest() {
        try {
            val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins")
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
            Log.d("ResponseData", "Response code: $responseCode, Response body: $response")

            // Parse JSON response
            pinList = JSONArray(response.toString())
            activity?.runOnUiThread {
                val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this@MapFragment)
            }
        } catch (e: Exception) {
            Log.e("ResponseData", "Error making request: ${e.message}", e)
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

    private fun createPinRequest(name: String, address: String, lat: Double, lon: Double, userId: Int, desc: String, photoUrls: List<String>, selectedTags: MutableList<String>) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                authToken?.let {
                    connection.setRequestProperty("Authorization", "$it")
                }

                val photoUrlsJson = photoUrls.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }

                val requestBody: String

                if (selectedTags.isEmpty()) {
                    requestBody = """
                        {
                            "name": "$name",
                            "address": "$address",
                            "lat": $lat,
                            "lon": $lon,
                            "userId": $userId,
                            "desc": "$desc",
                            "photos": $photoUrlsJson
                        }
                    """.trimIndent()
                } else {
                    val tagsList = selectedTags.map { tagName ->
                        mapOf("name" to tagName)
                    }

                    val tagsJson = tagsList.joinToString(separator = ",", prefix = "[", postfix = "]") { tag ->
                        """
                        {
                            "name": "${tag["name"]}"
                        }
                        """.trimIndent()
                    }
                    Log.d("createPinRequestResponse", "$tagsJson")
                    requestBody = """
                        {
                            "name": "$name",
                            "address": "$address",
                            "lat": $lat,
                            "lon": $lon,
                            "userId": $userId,
                            "desc": "$desc",
                            "tags": $tagsJson,
                            "photos": $photoUrlsJson
                        }
                    """.trimIndent()
                }

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
                pinList.put(response)
                connection.disconnect()
                Log.d("createPinRequestResponse", "$responseCode, $response")
            } catch (e: Exception) {
                Log.e("createPinRequestResponse", "Error making request: ${e.message}", e)
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

    private fun getTagsRequest() {
        try {
            val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/tags/options")
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
            Log.d("ResponseData", "Response code: $responseCode, Response body: $response")

            // Parse JSON response
            tags = JSONArray(response.toString())
//            activity?.runOnUiThread {
//                populateTags(tags)
//            }
        } catch (e: Exception) {
            Log.e("ResponseData", "Error making request: ${e.message}", e)
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

    private fun createComment(pinId: Int, content: String) {
        Log.d("comments", "createcomment here")
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/pins/$pinId/comments")
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
                    "content": "$content"
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
                Log.d("Comments", "Response code after creating: $responseCode, Response body: $response")
                connection.disconnect()

                val comment = JSONObject(response.toString())
                Log.d("Add comment", "$comment")
                addCommentToView(comment)


            } catch (e: Exception) {
                Log.e("ResponseData", "Error making request: ${e.message}", e)
//                null
            }
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

    private fun populateTags(tags: JSONArray) {
        Log.d("Tags", "$tags")
        for (i in 0 until tags.length()) {
            val tag = tags.getJSONObject(i)
            Log.d("Tags", "tag: $tag")
            val tagView = layoutInflater.inflate(R.layout.tag, null)
            val tagButton = tagView.findViewById<Button>(R.id.button)
            tagButton.text = tag.getString("name").capitalize()
            tagButton.backgroundTintList = ColorStateList.valueOf(getRandomColor(tag.getInt("tagId")))
            tagsContainer.addView(tagView)

            tagButton.setOnClickListener {
                tagsContainer.visibility = View.GONE
                Log.d("Tag", "tag clicked")
                val searchBar = view?.findViewById<EditText>(R.id.search_bar)
                searchBar?.setText(tag.getString("name"))
                filterSearchBar(tag.getString("name"))
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

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_PICK = 2
    }

    private fun populatePins(mMap: GoogleMap) {
        Log.d("ResponseDataPopulatePins", "${pinList.length()}")
        mMap.clear()
        for (i in 0 until pinList.length()) {
            val pinObject = pinList.getJSONObject(i)
            val lat = pinObject.getDouble("lat")
            val lon = pinObject.getDouble("lon")
            val coordinate = LatLng(lat, lon)
            val markerOptions = MarkerOptions().position(coordinate)
            val marker = mMap.addMarker(markerOptions)

            if (marker != null) {
                marker.tag = pinObject
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLng(coordinate))
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

    private suspend fun openPins(mMap: GoogleMap) {
        mMap.setOnMarkerClickListener { marker ->
            // Setup popup window
            popupView = layoutInflater.inflate(R.layout.popup_marker_info, null)
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val popupWidth = (screenWidth * 0.85).toInt() // 80% of screen width
            val popupHeight = (screenHeight * 0.78).toInt() // 60% of screen height
            popupWindow = PopupWindow(popupView, popupWidth, popupHeight, true)
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

            val viewPager = popupView.findViewById<ViewPager>(R.id.viewPager)
            val pinObject = marker.tag as JSONObject
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
                setupIndicators(photoUrls.size, indicatorLayout)
                setupViewPager(viewPager, indicatorLayout)

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
                val pinObject = marker.tag as JSONObject
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
                    handleUserProfileClick(userId)
                }

                userNameView.setOnClickListener {
                    Log.d("User profile", "User profile name clicked")
                    handleUserProfileClick(userId)
                }

                bookmarkButton.setOnClickListener {
                    createBookmarkRequest(pinId)
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
                        createComment(pinId, commentField)
                        Log.d("Comments", "set edit field to null")
                        editComment.text = null
                    }

                    if (comments != null) {
                        populateComments(comments)
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

                    CoroutineScope(Dispatchers.IO).async {
                        val userObject = getUserRequest(userId)
                        val authorName = userObject?.optString("name")
                        val bioPhoto = userObject?.optString("bioPhoto")

                        withContext(Dispatchers.Main) {
                            userProfileTextView.text = authorName.toString()
                            bioPhoto?.let {
                                Glide.with(requireContext())
                                    .load(it)
                                    .placeholder(R.drawable.baseline_account_circle_24)
                                    .error(R.drawable.baseline_account_circle_24)
                                    .circleCrop()
                                    .into(userProfileView!!)
                            }
                        }
                    }.await()

                    val authorNameDeferred = async(Dispatchers.IO) {
                        val userObject = getUserRequest(userId)
                        userObject?.optString("name")
                    }
                    val authorName = authorNameDeferred.await()
                    Log.d("authorName", "name: $authorName")
                    userProfileTextView.text = authorName.toString()

                    nameTextView.text = name
                    addressTextView.text = address
//                latTextView.text = lat.toString() + ","
//                lonTextView.text = lon.toString()
                    descTextView.text = desc.toString()
                    dateTExtView.text = date


                    loadingIndicator.visibility = View.GONE
                    loadingIndicatorFrame.visibility = View.GONE
                }

                backButton.setOnClickListener {
                    popupWindow.dismiss() // Assuming popupWindow is accessible here
                }
            }
            true
        }
    }

    private fun handleUserProfileClick(userId: Int) {
        popupWindow?.dismiss()
        CoroutineScope(Dispatchers.Main).launch {
            val userObject = getUserRequest(userId)
            if (userObject != null) {
                view?.findViewById<FloatingActionButton>(R.id.addPinButton)?.visibility = View.GONE
                if (userId == currentUserId) {
                    Log.d("profile", "current user")
                    val newFragment = ProfileFragment()
                    val transaction = requireActivity().supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.mapFragmentContainer, newFragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                } else {
                    Log.d("profile", "diff user")
                    val newFragment = UserProfileFragment.newInstance(userObject)
                    val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
                    transaction.replace(R.id.mapFragmentContainer, newFragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }
            }
        }
    }

    private fun populateComments(comments: JSONArray) {
        CoroutineScope(Dispatchers.Main).launch {
            for (i in 0 until comments.length()) {
                Log.d("getPin", "$i")
                val commentObject = comments.getJSONObject(i)
                Log.d("Comments", "comment: $commentObject")
                val content = commentObject.getString("content")
                val createdAt = commentObject.getString("createdAt").substringBefore('T')
                val userId = commentObject.getInt("userId")
                val creator = getUserRequest(userId)?.getString("name")

                val commentLayout = popupView.findViewById<LinearLayout>(R.id.commentsContainer)
                val commentView = layoutInflater.inflate(R.layout.comments, null)
                val userName = commentView?.findViewById<TextView>(R.id.commentUsername)
                val desc = commentView?.findViewById<TextView>(R.id.commentContent)
                val date = commentView?.findViewById<TextView>(R.id.dateTextView)
                val commentCount = popupView.findViewById<TextView>(R.id.commentCount)

                Log.d("Comments", "set text")
                userName?.text = creator
                desc?.text = content
                date?.text = createdAt
                commentCount?.text = comments.length().toString()
                Log.d("Comments", "set text")
                commentLayout?.addView(commentView)
            }
        }
    }

    private suspend fun addCommentToView(comment: JSONObject) {
        withContext(Dispatchers.Main) {
            val content = comment.getString("content")
            val createdAt = comment.getString("createdAt").substringBefore('T')
            val userId = comment.getInt("userId")
            val creator = getUserRequest(userId)?.getString("name")

            val commentLayout = popupView.findViewById<LinearLayout>(R.id.commentsContainer)
            val commentView = layoutInflater.inflate(R.layout.comments, null)
            val userName = commentView?.findViewById<TextView>(R.id.commentUsername)
            val desc = commentView?.findViewById<TextView>(R.id.commentContent)
            val date = commentView?.findViewById<TextView>(R.id.dateTextView)
            val commentCount = popupView.findViewById<TextView>(R.id.commentCount)

            Log.d("Add comment", "set text")
            userName?.text = creator
            desc?.text = content
            date?.text = createdAt
            val currentComments = commentCount?.text.toString().toIntOrNull() ?: 0
            val newCommentCount = currentComments + 1
            commentCount?.text = newCommentCount.toString()
            Log.d("Add comment", "set text")
            commentLayout?.addView(commentView)
        }
    }

    private fun createPin(mMap: GoogleMap) {
        view?.findViewById<FloatingActionButton>(R.id.addPinButton)?.setOnClickListener {
            // Setup popup window
            val popupView = layoutInflater.inflate(R.layout.popup_create_pin_info, null)
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val popupWidth = (screenWidth * 0.85).toInt() // 80% of screen width
            val popupHeight = (screenHeight * 0.78).toInt() // 60% of screen height
            val popupWindow = PopupWindow(popupView, popupWidth, popupHeight, true)
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

            // Retrieve references to EditText fields
            val backButton = popupView.findViewById<ImageView>(R.id.backButtonImageView)
            val titleEditText = popupView.findViewById<EditText>(R.id.titleEditText)
            val captionEditText = popupView.findViewById<EditText>(R.id.captionEditText)
            tagsContainer = popupView.findViewById(R.id.tagsContainer)
            val addressEditText = popupView.findViewById<EditText>(R.id.addressEditText)
            val longitudeEditText = popupView.findViewById<EditText>(R.id.longitudeEditText)
            val latitudeEditText = popupView.findViewById<EditText>(R.id.latitudeEditText)

            val addImageView1 = popupView.findViewById<ImageView>(R.id.addImageView1)
            val addImageView2 = popupView.findViewById<ImageView>(R.id.addImageView2)
            val addImageView3 = popupView.findViewById<ImageView>(R.id.addImageView3)
            val addImageView4 = popupView.findViewById<ImageView>(R.id.addImageView4)

            val imageViews = arrayOf(addImageView1, addImageView2, addImageView3, addImageView4)

            for (imageView in imageViews) {
                imageView.setOnClickListener {
                    selectImage(imageView)
                }
            }

            val createButton = popupView.findViewById<Button>(R.id.buttonSaveChanges)
            createButton.isEnabled = false

            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val allFieldsValid = checkAllFieldsValid(popupView)
                    val createButton = popupView.findViewById<Button>(R.id.buttonSaveChanges)
                    createButton.isEnabled = allFieldsValid
                }
            }

            titleEditText.addTextChangedListener(textWatcher)
            captionEditText.addTextChangedListener(textWatcher)
            val selectedTags = mutableListOf<String>()
            for (i in 0 until tags.length()) {
                val tag = tags.getJSONObject(i)
                val tagView = layoutInflater.inflate(R.layout.tag, null)
                val tagButton = tagView.findViewById<Button>(R.id.button)
                tagButton.text = tag.getString("name").capitalize()
                tagButton.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)
                tagsContainer.addView(tagView)

                tagButton.setOnClickListener {
                    val tagName = tag.getString("name")
                    if (selectedTags.contains(tagName)) {
                        selectedTags.remove(tagName)
                        tagButton.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)
                    } else {
                        selectedTags.add(tagName)
                        tagButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#800080"))
                    }
                }
            }
            addressEditText.addTextChangedListener(textWatcher)
            longitudeEditText.addTextChangedListener(textWatcher)
            latitudeEditText.addTextChangedListener(textWatcher)

            backButton.setOnClickListener {
                popupWindow.dismiss() // Assuming popupWindow is accessible here
            }

            // Set click listener for the create button inside the popup window
            createButton.setOnClickListener {
                // Get data from fields
                val name = titleEditText.text.toString().trim()
                val address = addressEditText.text.toString().trim()
                val desc = captionEditText.text.toString().trim()
                val lon: Double? = longitudeEditText.text?.toString()?.toDoubleOrNull()
                val lat: Double? = latitudeEditText.text?.toString()?.toDoubleOrNull()

                // Validate lat and lon
                if (lat != null && lon != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val photoUrls = ArrayList<String>()
                        // Upload images and get their URLs
                        for (uri in imageUris) {
                            val imageUrl = uploadImageToFirebaseStorage(uri)
                            Log.d("FireBaseImg", "$imageUrl")
                            if (imageUrl != null) {
                                photoUrls.add(imageUrl)
                            }
                        }

                        // Create pin request with the obtained photo URLs
                        createPinRequest(name, address, lat, lon, currentUserId, desc, photoUrls, selectedTags)

                        // Now that createPinRequest is finished, we can fetch the updated pins
                        getPinsRequest()

                        // Finally, populate pins on the main thread
                        populatePins(mMap)

                        // Dismiss the popup window
                        popupWindow.dismiss()
                    }
                }
            }
        }
    }

    private fun selectImage(imageView: ImageView) {
        selectedImageView = imageView
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setTitle("Choose your photo")

        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Take Photo" -> {
                    // Open camera intent
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    } else {
                        Toast.makeText(requireContext(), "No app available to take a photo", Toast.LENGTH_SHORT).show()
                    }
                }
                options[item] == "Choose from Gallery" -> {
                    // Open gallery intent
                    val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK)
                }
                options[item] == "Cancel" -> {
                    dialog.dismiss()
                }
            }
        }
        builder.show()
    }

    // Function to check if all EditText fields have valid values
    private fun checkAllFieldsValid(popupView: View): Boolean {
        val titleEditText = popupView.findViewById<EditText>(R.id.titleEditText)
        val captionEditText = popupView.findViewById<EditText>(R.id.captionEditText)
        val tagsEditText = popupView.findViewById<EditText>(R.id.tagsEditText)
        val addressEditText = popupView.findViewById<EditText>(R.id.addressEditText)
        val longitudeEditText = popupView.findViewById<EditText>(R.id.longitudeEditText)
        val latitudeEditText = popupView.findViewById<EditText>(R.id.latitudeEditText)

        val titleValid = titleEditText?.text.toString().trim().isNotEmpty()
        val captionValid = captionEditText?.text.toString().trim().isNotEmpty()
        val tagsValid = tagsEditText?.text.toString().trim().isNotEmpty()
        val addressValid = addressEditText?.text.toString().trim().isNotEmpty()
        val longitudeValid = longitudeEditText?.text.toString().trim().isNotEmpty()
        val latitudeValid = latitudeEditText?.text.toString().trim().isNotEmpty()

        // Return true only if all fields are valid
        return titleValid && captionValid && tagsValid && longitudeValid && latitudeValid
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        CoroutineScope(Dispatchers.IO).launch {
            getTagsRequest()
            activity?.runOnUiThread {
                populateTags(tags)
                CoroutineScope(Dispatchers.Main).launch {
                    openPins(mMap)
                }
            }
        }
        populatePins(mMap)
        createPin(mMap)
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setPadding(0, 0, 0, 1500)

        view?.findViewById<FrameLayout>(R.id.loadingLayout)?.visibility = View.GONE
    }
}