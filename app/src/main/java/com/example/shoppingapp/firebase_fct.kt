package com.example.shoppingapp

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.auth
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.firestore


fun signUp(email: String, password: String, username: String, firstName: String, lastName: String) {
    val auth: FirebaseAuth = Firebase.auth
    var success = false
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
        task ->
        if (task.isSuccessful) {
            Log.d("TAG", "createUserWithEmail:success")
            success = true
            addUserDetails(auth.currentUser!!.uid, username, firstName, lastName)
        } else {
            Log.w("TAG", "createUserWithEmail:failure", task.exception)
            success = false
        }
    }

}

fun addUserDetails(userId: String, username: String, firstName: String, lastName: String) {
    val db = FirebaseFirestore.getInstance()

    val user = hashMapOf(
        "username" to username,
        "firstName" to firstName,
        "lastName" to lastName
    )

    db.collection("users")
        .document(userId)
        .set(user)
        .addOnSuccessListener {
            Log.d("TAG", "DocumentSnapshot successfully written!")
        }
        .addOnFailureListener { e ->
            Log.w("TAG", "Error writing document", e)
        }
}

fun logIn(email: String, password: String, navController: NavHostController, context: Context): Boolean {
    val auth: FirebaseAuth = Firebase.auth
    if (email.isEmpty() || password.isEmpty()) {
        return false
    }
    var success = false
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("TAG", "signInWithEmail:success")
                navController.navigate("home")
                success = true
            } else {
                Log.w("TAG", "signInWithEmail:failure", task.exception)
                success = false
            }
        }
    return auth.currentUser != null
}

fun getImageUrl(fileName: String, onSuccess: (String) -> Unit) {
    val storageReference = FirebaseStorage.getInstance().reference
    val imageRef = storageReference.child(fileName)
    imageRef.downloadUrl.addOnSuccessListener { uri ->
        onSuccess(uri.toString())
    }
}

@Composable
fun DisplayImageFromFirestore(fileName: String, modifier: Modifier) {
    var imageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fileName) {
        getImageUrl(fileName) { url ->
            imageUrl = url
        }
    }

    imageUrl?.let { url ->
        val imagePainter = rememberImagePainter(data = url)

        Image(
            painter = imagePainter,
            contentDescription = "Image from Firestore",
            modifier = modifier,
            contentScale = ContentScale.FillWidth,
        )
    }
}
