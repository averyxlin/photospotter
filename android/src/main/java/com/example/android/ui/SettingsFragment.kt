package com.example.android.ui

import AuthManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.android.R
import com.example.android.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val PICK_IMAGE_REQUEST_CODE = 100
private val storageReference: StorageReference = FirebaseStorage.getInstance().reference

class SettingsFragment : Fragment() {

    private var param1: String? = null
    private lateinit var logoutText: TextView
    private lateinit var editTextName: EditText
    private lateinit var profileImageView: ImageView
    private lateinit var editIcon: ImageView
    private lateinit var editTextBio: EditText
//    private lateinit var editTextInstagram: EditText
//    private lateinit var editTextSite: EditText
    private lateinit var buttonSaveChanges: Button
    private lateinit var buttonCancelChanges: Button
    private lateinit var auth: FirebaseAuth
    private var authUser: FirebaseUser? = null
    private var authToken: String? = null

    private var downloadUrl: String? = ""

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
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Initialize views
        logoutText = view.findViewById(R.id.textViewLogout)
        profileImageView = view.findViewById(R.id.profileImageView)
        editIcon = view.findViewById(R.id.editIcon)
        editTextName = view.findViewById(R.id.titleEditText)
        editTextBio = view.findViewById(R.id.captionEditText)
//        editTextInstagram = view.findViewById(R.id.tagsEditText)
//        editTextSite = view.findViewById(R.id.editTextSite)
        buttonSaveChanges = view.findViewById(R.id.buttonSaveChanges)
        buttonCancelChanges = view.findViewById(R.id.buttonCancelChanges)

        CoroutineScope(Dispatchers.IO).launch {
            initializeUI()
        }

        logoutText.setOnClickListener {
            AuthManager.clearAuthIdAndUserId(requireContext())
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        buttonCancelChanges.setOnClickListener {
            Log.d("Cancel", "Cancel profile changes button clicked")
            requireActivity().supportFragmentManager.popBackStack()
        }

        buttonSaveChanges.setOnClickListener {
            val loadingIndicator = view.findViewById<ProgressBar>(R.id.loadingProgressBar)
            loadingIndicator.visibility = View.VISIBLE
            Log.d("Save", "Save profile changes")
            val updatedName = editTextName?.text.toString()
            val updatedBio = editTextBio?.text.toString()
//            val updateigURL = editTextInstagram?.text.toString()
//            val updatesiteURL = editTextSite?.text.toString()
            var currentUserId = AuthManager.getUserId(requireContext()) ?: 1

            val updatedData = JSONObject().apply {
                put("id", currentUserId)
                put("name", updatedName)
                put("email", JSONObject(param1)["email"])
                put("bio", updatedBio)
//                put("igURL", updateigURL)
//                put("siteURL", updatesiteURL)

                if (!downloadUrl.isNullOrEmpty()) {
                    put("bioPhoto", downloadUrl)
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val responseCode = updateUserRequest(updatedData)
                    Log.d("updateUserRequest ResponseData", "$responseCode")
                    Log.d("updateUserRequest ResponseData", "Switching fragments now")
                    val newFragment = ProfileFragment()
                    val transaction = requireActivity().supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragment_container, newFragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                } catch (e: Exception) {
                    Log.e("ResponseData", "Error making request: ${e.message}", e)
                }
            }
        }
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Get the selected image URI
            val imageUri: Uri? = data?.data

            // Upload the image to Firebase Storage and get the download URL
            CoroutineScope(Dispatchers.Main).launch {
                downloadUrl = uploadImageToFirebaseStorage(imageUri)
                if (downloadUrl != null) {
                    // Update profileImageView with the uploaded image
                    Glide.with(requireContext())
                        .load(downloadUrl)
                        .transform(BlurTransformation(2, 3))
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.edit_pfp)
                        .centerCrop()
                        .into(profileImageView)
                } else {
                    // Handle error while uploading image
                    Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Upload the image to Firebase Storage
    private suspend fun uploadImageToFirebaseStorage(imageUri: Uri?): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (imageUri != null) {
                    val imageName = UUID.randomUUID().toString()
                    val imageRef = storageReference.child("images/$imageName")

                    val uploadTask: UploadTask = imageRef.putFile(imageUri)
                    uploadTask.await()

                    // Get the download URL of the uploaded image
                    downloadUrl = imageRef.downloadUrl.await().toString()

                    // Log the URL string
                    Log.d("UploadImage", "Download URL: $downloadUrl")

                    downloadUrl
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun initializeUI() {
        val jsonObject = JSONObject(param1)
        Log.d("Responsedata for settings!!", "Response: $jsonObject")
        val name = jsonObject.optString("name", "")
        val bio = jsonObject.optString("bio", "")
        val igURL = jsonObject.optString("igURL", "")
        val siteURL = jsonObject.optString("siteURL", "")
        val bioPhoto = jsonObject.optString("bioPhoto", "")

        CoroutineScope(Dispatchers.Main).launch {
            editTextName?.setText(name)
            editTextName.hint = name
            if (bio.isNotEmpty()) {
                editTextBio?.setText(bio)
            }
//            if (igURL.isNotEmpty()) {
//                editTextInstagram?.setText(igURL)
//            }
//            if (siteURL.isNotEmpty()) {
//                editTextSite?.setText(siteURL)
//            }
            if (bioPhoto.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(bioPhoto)
                    .transform(BlurTransformation(2, 3))
                    .placeholder(R.drawable.placeholder) // Placeholder image while loading
                    .error(R.drawable.edit_pfp) // Image to display if loading fails
                    .centerCrop()
                    .into(profileImageView)

                // Make editIcon visible
                editIcon.visibility = View.VISIBLE
            } else {
                // If bioPhoto is empty, hide editIcon
                editIcon.visibility = View.GONE
            }
            profileImageView.setOnClickListener {
                // Open gallery to pick an image
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
            }
        }
    }

    private suspend fun updateUserRequest(updatedData: JSONObject): Int {
        return withContext(Dispatchers.IO) {
            var responseCode = -1
            try {
                val currentUserId = AuthManager.getUserId(requireContext()) ?: 1
                val url = URL("https://northamerica-northeast2-docker-pkg-dev-o4o4h7tpiq-uc.a.run.app/users/$currentUserId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                authToken?.let {
                    connection.setRequestProperty("Authorization", "$it")
                }
                val requestBody = updatedData.toString()
                val outputStream: OutputStream = connection.outputStream
                outputStream.write(requestBody.toByteArray())
                outputStream.flush()
                responseCode = connection.responseCode
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("ResponseData", "Error making request: ${e.message}", e)
            }
            responseCode
        }
    }

    companion object {
        fun newInstance(param1: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}