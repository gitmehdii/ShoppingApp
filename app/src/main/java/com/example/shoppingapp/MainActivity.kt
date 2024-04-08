package com.example.shoppingapp


import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shoppingapp.ui.theme.ShoppingAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import com.example.shoppingapp.*
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.round


data class Product(val id: String, val title: String, val price: String, val description: String, val category: String, val image: String)


fun logOut(context: Context) {
    val sharedPref = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    with (sharedPref.edit()) {
        remove("userId")
        apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "My Notification Channel"
            val descriptionText = "This is a channel for important notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        super.onCreate(savedInstanceState)
        setContent {
            val context = this
            ShoppingAppTheme {
                // A surface container using the 'background' color from the theme
                Surface{
                    AppNavigator(context)
                }
            }
        }
    }
}

@Composable
fun AppNavigator(context: Context) {
    val navController = rememberNavController()
    val auth = Firebase.auth
    val user = auth.currentUser
    if (user != null) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") { HomePage(navController, context) }
            composable("home/{category}") {
                val category = it.arguments?.getString("category")
                HomePage(navController, context, category)
            }
            composable("login") { LoginPage(navController,context) }
            composable("signup") { SignUpPage(navController) }
            composable("product/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val product = runBlocking { GetProductsWithId(id!!) }
                product?.let { ProductPage(it, navController, 0) }
            }
            composable("product/{id}/{quantity}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val quantity = backStackEntry.arguments?.getString("quantity")?.toInt()
                val product = runBlocking { GetProductsWithId(id!!) }
                product?.let { ProductPage(it, navController, quantity!!) }
            }
            composable("cart") { CartPage(navController, context) }
            composable("profile") { ProfilePage(navController, context) }
            composable("changePassword") { changePassword(navController) }
            composable("order") { OrderHistory(navController) }
        }
    }
    else {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") { LoginPage(navController,context) }
            composable("signup") { SignUpPage(navController) }
            composable("home") { HomePage(navController, context) }
            composable("home/{category}") {
                val category = it.arguments?.getString("category")
                HomePage(navController, context, category)
            }
            composable("product/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val product = runBlocking { GetProductsWithId(id!!) }
                product?.let { ProductPage(it, navController) }
            }
            composable("product/{id}/{quantity}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val quantity = backStackEntry.arguments?.getString("quantity")?.toInt()
                val product = runBlocking { GetProductsWithId(id!!) }
                product?.let { ProductPage(it, navController, quantity!!) }
            }
            composable("cart") { CartPage(navController, context) }
            composable("profile") { ProfilePage(navController, context) }
            composable("changePassword") { changePassword(navController) }
            composable("order") { OrderHistory(navController) }
        }
    }
}


@Composable
fun LoginPage(navController: NavHostController, context: Context) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errortext by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "Login", fontSize = 30.sp, modifier = Modifier.padding(16.dp))

        Spacer(modifier = Modifier.height(100.dp))
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if(email.isNotBlank() && password.isNotBlank()) {
                    if(logIn(email, password, navController, context)) {
                        Log.e("TAG2", "Login successful")
                        navController.popBackStack("home", true)
                        navController.navigate("home")
                    } else {
                        errortext = "Invalid username or password"
                    }
                } else {
                    errortext = "Email and password cannot be empty"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Text(text = errortext, modifier = Modifier.padding(8.dp), color = Color.Red)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                navController.popBackStack("signup", true)
                navController.navigate("signup")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Don't have an account? Sign Up")
        }
    }
}

@Composable
fun SignUpPage(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Sign Up", fontSize = 30.sp, modifier = Modifier.padding(16.dp))

        Spacer(modifier = Modifier.height(32.dp) )


        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(16.dp))

        var errortext by remember { mutableStateOf("") }
        Button(
            onClick = {
                signUp(email, password, username, firstName, lastName)
                navController.popBackStack("login", true)
                navController.navigate("login")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
        Text(text = errortext, modifier = Modifier.padding(8.dp), color = Color.Red)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                navController.popBackStack("login", true)
                navController.navigate("login")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Already have an account? Login")
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavHostController)
{
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFD62727),
            actionIconContentColor = Color.White,
        ),
        title = { Text("Watch & Shoes", textAlign = TextAlign.Center, color = Color.White) },
        actions = {
            IconButton(onClick = { navController.navigate("profile")}) {
                Icon(Icons.Filled.Person, contentDescription = null)
            }
        },

    )
}


@Composable
fun BottomBar(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD62727)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Icons.Filled.Home.let { icon ->
            IconButton(onClick = { /* Handle home click */ },) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
        Icons.Filled.ShoppingCart.let { icon ->
            IconButton(onClick = { navController.navigate("cart") }) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
        Icons.Filled.Info.let { icon ->
            IconButton(onClick = { navController.navigate("order") }) {
                Icon(icon, contentDescription = null,tint = Color.White)
            }
        }
        Icons.Filled.Person.let { icon ->
            IconButton(onClick = { navController.navigate("profile") }) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
    }
}


@Composable
fun HomePage(navController: NavHostController, context: Context, category: String? = null) {
    // Check if user is logged in
    val auth = Firebase.auth
    val user = auth.currentUser
    if (user == null) {
        navController.popBackStack("login", true)
        navController.navigate("login")
    }
    ShoppingAppTheme {
        Text(text = "Home Page", fontSize = 30.sp, modifier = Modifier.padding(16.dp))
        Button(onClick = {
            //logout
            val auth: FirebaseAuth = Firebase.auth
            auth.signOut()
            logOut(context)
            navController.popBackStack("home", true)
            navController.navigate("login")
        }) {
            Text(text = "Logout")
        }
        var searchQuery by remember { mutableStateOf("") }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ScaffoldHomePage(navController, context, category)
    }
}



@Composable
fun ScaffoldHomePage(navController: NavHostController, context: Context, category: String? = null) {
    ShoppingAppTheme {
        Scaffold(
            topBar = {
                TopBar(navController)
            },
            bottomBar = {
                BottomBar(navController)
            }
        ) { innerPadding ->
            BodyContent(Modifier.padding(innerPadding), navController, category)
        }
    }
}

suspend fun GetProductsWithId(id: String): Product? {
    val db = FirebaseFirestore.getInstance()
    val document = db.collection("product").document(id).get().await()
    if (document.exists()) {
        val title = document.getString("title")
        val category = document.getString("category")
        val price = document.getString("price")
        val description = document.getString("description")
        return Product(id, title ?: "No title", price ?: "", description ?: "", category ?: "", "")
    }
    return null
}

@Composable
fun BodyContent(modifier: Modifier = Modifier, navController: NavHostController, category: String? = null) {
    ShoppingAppTheme {
        Column(modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .background(Color(0xFFF1F1F1))) {



            val db = FirebaseFirestore.getInstance()
            val products = remember { mutableStateOf(listOf<Product>()) }

            LaunchedEffect(Unit) {
                db.collection("product")
                    .get()
                    .addOnSuccessListener { result ->
                        Log.d("Firestore", "Documents fetched successfully")
                        val productList = result.map { document ->
                            val title = document.getString("title")
                            val id = document.id
                            val category = document.getString("category")
                            val price = document.getString("price")
                            val description = document.getString("description")
                            Product(id, title ?: "No title", price ?: "", description ?: "", category ?: "", "")
                        }
                        products.value = productList
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error getting documents: $exception")
                    }
            }
            Spacer(modifier = Modifier.height(58.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(8.dp)
                    ,
                horizontalArrangement = Arrangement.SpaceEvenly)
            {
                Button(onClick = {
                    navController.navigate("home")

                }, colors = if (category == null)
                    ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                        containerColor = Color.Red
                    )
                    else
                    ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                        containerColor = Color.Black
                    )){ Text("All") }

                Button(onClick = {
                    navController.navigate("home/Shoes")
                },
                    colors = if (category == "Shoes")
                        ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Color.Red
                        )
                    else
                        ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Color.Black
                        )) {
                    Text("Shoes")
                }

                Button(onClick = {
                    navController.navigate("home/Watch")
                },
                    colors = if (category == "Watch")
                        ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Color.Red
                        )
                    else
                        ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Color.Black
                        )) {
                    Text("Watch")
                }
            }
            for (productId in products.value) {
                if (category != null && productId.category != category) {
                    continue
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F1F1))
                        .height(470.dp)
                        .clickable { navController.navigate("product/${productId.id}") }
                    ,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF1F1F1) // Change this to Color.White
                    ),)
                {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)

                    )
                    {
                        Text(text = productId.title, fontSize = 20.sp, color = Color.Black)
                        Text(text = productId.category, color = Color.DarkGray,fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        DisplayImageFromFirestore(fileName = productId.id + ".png",
                            Modifier
                                .height(300.dp)
                                .fillMaxWidth()
                                .clickable { navController.navigate("product/${productId.id}") })
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        )
                        {
                            Button(
                                onClick = {
                                    navController.navigate("product/${productId.id}")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    contentColor = Color.White,
                                    containerColor = Color.Black
                                )
                            ) {
                                Text(text = productId.price + " €")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
            }
        }
    }
}

fun truncateString(input: String, limit: Int): String {
    if (input.length <= limit) {
        return input
    }
    if (input[limit] == ' ') {
        return "${input.take(limit)}..."
    }
    val lastSpace = input.substring(0, limit).lastIndexOf(' ')
    return "${input.take(lastSpace)}..."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPageTopBar(navController: NavHostController, product: Product) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFD62727),
            actionIconContentColor = Color.White,
        ),
        title = { Text(truncateString(product.title, 30), style = TextStyle(textAlign = TextAlign.Justify), fontSize = 19.sp, color = Color.White) },
        navigationIcon = {
            IconButton(onClick = {
                // Handle navigation icon click
                navController.navigate("home")
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = { navController.navigate("cart") }) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color.White)
            }
        },
    )
}

@Composable
fun ProductPageContent(product: Product, modifier: Modifier, navController: NavHostController) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        DisplayImageFromFirestore(fileName = product.id + ".png",
            Modifier
                .height(450.dp)
                .fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = product.category, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = product.title, fontSize = 30.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text =product.description, fontSize = 16.sp)
    }
}

@Composable
fun ProductPageBottomBar(product: Product, quantity: Int = 0, navController: NavHostController) {
    var showDialog by remember { mutableStateOf(false) }
    var quantity2 by remember { mutableStateOf(quantity) }

    val db = FirebaseFirestore.getInstance()
    val auth = Firebase.auth
    val userId = auth.currentUser!!.uid
    Surface(color = Color(0xFFD62727)){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row (
                modifier = Modifier.fillMaxWidth()
                    ,
                horizontalArrangement = Arrangement.Center
                ){
                Image(

                    painter = painterResource(id = R.drawable.plusw),
                    contentDescription = "Plus Image",
                    modifier = Modifier
                        .height(30.dp) // Adjust the height
                        .width(30.dp)
                        .clickable(onClick = {
                            quantity2++;
                        }),
                    contentScale = ContentScale.Crop,
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(text = quantity2.toString(),
                    color = Color.White,
                    style = TextStyle(fontSize = 20.sp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Image(

                    painter = painterResource(id = R.drawable.minusw),
                    contentDescription = "Cinema Image",
                    modifier = Modifier
                        .height(30.dp) // Adjust the height
                        .width(30.dp)
                        .clickable(onClick = {
                            if (quantity2 > 0) {
                                quantity2--;
                            }
                        }),
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (quantity2 > 0)
                    {
                        showDialog = true
                        val docRef = db.collection("product_buy").document("$userId${product.id}")
                        docRef.get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    if (quantity2 == 0) {
                                        docRef.delete()
                                    }
                                    else
                                    {
                                        val newQuantity = quantity2
                                        val productBuy = hashMapOf(
                                            "userId" to userId,
                                            "productId" to product.id,
                                            "quantity" to newQuantity
                                        )
                                        docRef.set(productBuy, SetOptions.merge())
                                    }

                                } else {
                                    val productBuy = hashMapOf(
                                        "userId" to userId,
                                        "productId" to product.id,
                                        "quantity" to quantity2
                                    )
                                    docRef.set(productBuy, SetOptions.merge())
                                }

                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error getting document", e)
                            }
                    }
                          },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    containerColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = product.price + " €")
            }

            if (showDialog) {

                AlertDialog(
                    onDismissRequest = { showDialog = false
                        navController.popBackStack("home", true)
                        navController.navigate("home")},
                    title = { Text(text = "Add to Cart") },
                    text = { Text("You have successfully added ${quantity2} product to the cart.") },
                    confirmButton = {
                        Button(onClick = { showDialog = false
                            navController.popBackStack("home", true)
                            navController.navigate("home")}) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}



@Composable
fun ProductPage(product: Product, navController: NavHostController, quantity: Int = 0) {
    ShoppingAppTheme {
        Scaffold(
            topBar = {
                ProductPageTopBar(navController, product)
            },
            bottomBar = {
                ProductPageBottomBar(product, quantity, navController)
            }
        ) { innerPadding ->
            ProductPageContent(product, Modifier.padding(innerPadding), navController)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartPageTopBar(navController: NavHostController) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFD62727),
            actionIconContentColor = Color.White,
        ),
        title = { Text("My Cart", textAlign = TextAlign.Center, color = Color.White) },
        navigationIcon = {
            IconButton(onClick = {
                // Handle navigation icon click
                navController.navigate("home")
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
        },
    )
}

@Composable
fun CartPageBottomBar(navController: NavHostController, context: Context) {
    val db = FirebaseFirestore.getInstance()
    val auth = Firebase.auth
    val products = remember { mutableStateOf(listOf<Pair<Product, Int>>()) }
    val userID = auth.currentUser!!.uid
    LaunchedEffect(Unit) {
        db.collection("product_buy")
            .whereEqualTo("userId", userID)
            .get()
            .addOnSuccessListener { documents ->
                val productList = documents.mapNotNull { document ->
                    val productId = document.getString("productId")
                    val quantity = document.getLong("quantity")?.toInt()
                    if (productId != null && quantity != null) {
                        val product = runBlocking { GetProductsWithId(productId) }
                        product?.let { Pair(it, quantity) }
                    } else {
                        null
                    }
                }
                products.value = productList
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            if (products.value.isNotEmpty()) {
                 buyNow(context = context)
            }
            navController.navigate("home")
                         }, modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 8.dp), colors = ButtonDefaults.buttonColors(
            Color.Black,)) {
            Text("Buy Now", style = TextStyle(fontSize = 20.sp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD62727)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icons.Filled.Home.let { icon ->
                IconButton(onClick = { navController.navigate("home") }) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
            }
            Icons.Filled.ShoppingCart.let { icon ->
                IconButton(onClick = { /* Handle cart click */ }) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
            }
            Icons.Filled.Info.let { icon ->
                IconButton(onClick = { navController.navigate("order") }) {
                    Icon(icon, contentDescription = null,tint = Color.White)
                }
            }
            Icons.Filled.Person.let { icon ->
                IconButton(onClick = { navController.navigate("profile") }) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun CardCart(product: Product, quantity: Int, navController: NavHostController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF1F1F1),
            contentColor = Color.Black
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            DisplayImageFromFirestore(
                fileName = product.id + ".png", modifier = Modifier
                    .height(100.dp)
                    .width(100.dp)
                    .clickable { navController.navigate("product/${product.id}/${quantity}") }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = quantity.toString() + "x " + product.title,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = roundToTwoDecimals(product.price.replace(',','.').toDouble() * quantity).toString().replace('.',',') + " €", fontSize = 20.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                )
                {
                    Icons.Filled.Delete.let { icon ->
                        IconButton(onClick = {
                            val db = FirebaseFirestore.getInstance()
                            val auth = Firebase.auth
                            val userID = auth.currentUser!!.uid
                            db.collection("product_buy")
                                .whereEqualTo("userId", userID)
                                .whereEqualTo("productId", product.id)
                                .get()
                                .addOnSuccessListener { documents ->
                                    for (document in documents) {
                                        db.collection("product_buy").document(document.id).delete()
                                    }
                                    //refresh the page
                                    navController.popBackStack("cart", true)
                                    navController.navigate("cart")
                                }
                                .addOnFailureListener { exception ->
                                    Log.w("Firestore", "Error getting documents: ", exception)
                                }

                        }) {
                            Icon(icon, contentDescription = null)
                        }
                    }

                    Icons.Filled.Edit.let { icon ->
                        IconButton(onClick = {
                            // Handle edit click
                            navController.navigate("product/${product.id}/${quantity}")

                        }) {
                            Icon(icon, contentDescription = null)
                        }
                    }
                }

            }
        }
    }
}

fun roundToTwoDecimals(num: Double): Double {
    return round(num * 100) / 100
}

@Composable
fun CartPageContent(modifier: Modifier = Modifier, navController: NavHostController) {
    // Assume we have a list of cart items
    val db = FirebaseFirestore.getInstance()
    val auth = Firebase.auth
    val userID = auth.currentUser!!.uid
    val products = remember { mutableStateOf(listOf<Pair<Product, Int>>()) }
    var totalAmount = 0.0

    LaunchedEffect(Unit) {
        db.collection("product_buy")
            .whereEqualTo("userId", userID)
            .get()
            .addOnSuccessListener { documents ->
                val productList = documents.mapNotNull { document ->
                    val productId = document.getString("productId")
                    val quantity = document.getLong("quantity")?.toInt()
                    if (productId != null && quantity != null) {
                        val product = runBlocking { GetProductsWithId(productId) }
                        product?.let { Pair(it, quantity) }
                    } else {
                        null
                    }
                }
                products.value = productList
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(16.dp)
        .verticalScroll(rememberScrollState())){
        products.value.forEach { (product, quantity) ->
            CardCart(product, quantity, navController)
            totalAmount += product.price.replace(',','.').toDouble() * quantity
            Log.e("TAG", "Total Amount: ${totalAmount}")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (products.value.isEmpty()) {
            Text(text = "No items in the cart", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Total Amount: ${totalAmount} €", fontSize = 20.sp)
    }

}

fun buyNow(context: Context) {
    val db = FirebaseFirestore.getInstance()
    val auth = Firebase.auth
    val userID = auth.currentUser!!.uid

    db.collection("product_buy")
        .whereEqualTo("userId", userID)
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                val newElement = hashMapOf(
                    "userId" to userID,
                    "productId" to document.getString("productId"),
                    "quantity" to document.getLong("quantity"),
                    "date" to FieldValue.serverTimestamp()
                )
                db.collection("order_history").add(newElement).addOnSuccessListener { documentReference ->
                    Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
                }
                    .addOnFailureListener { e ->
                        Log.w("Firestore", "Error adding document", e)
                    }
                db.collection("product_buy").document(document.id).delete()
            }
            sendNotification(context, "Confirmation", "Your order has been placed successfully")
        }
        .addOnFailureListener { exception ->
            Log.w("Firestore", "Error getting documents: ", exception)
        }
}


fun sendNotification(context: Context, title: String, message: String) {
    val notificationBuilder = NotificationCompat.Builder(context, "channel_id")
        .setSmallIcon(R.drawable.af1) // Remplacez "votre_icone" par le nom de votre icône
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    val notificationManager = NotificationManagerCompat.from(context)
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED) {
        notificationManager.notify(1, notificationBuilder.build())
    } else {
        Log.e("TAG", "Permission not granted")
    }
}



@Composable
fun CartPage(navController: NavHostController, context: Context) {
    // Check if user is logged in
    val auth = Firebase.auth
    val user = auth.currentUser
    if (user == null) {
        navController.popBackStack("login", true)
        navController.navigate("login")
    }
    ShoppingAppTheme {
        Scaffold(
            topBar= {
                CartPageTopBar(navController)
            },
            bottomBar = {
                CartPageBottomBar(navController, context)
            }
        ) { innerPadding ->
            CartPageContent(Modifier.padding(innerPadding), navController)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(navController: NavHostController) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFD62727),
            actionIconContentColor = Color.White,
        ),
        title = { Text("Profile", textAlign = TextAlign.Center, color = Color.White) },
        navigationIcon = {
            IconButton(onClick = {
                // Handle navigation icon click
                navController.navigate("home")
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
        },
    )
}

@Composable
fun ProfileBottomBar(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD62727))
        ,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Icons.Filled.Home.let { icon ->
            IconButton(onClick = { navController.navigate("home") }) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
        Icons.Filled.ShoppingCart.let { icon ->
            IconButton(onClick = { navController.navigate("cart") }) {
                Icon(icon, contentDescription = null,tint = Color.White)
            }
        }
        Icons.Filled.Info.let { icon ->
            IconButton(onClick = { navController.navigate("order") }) {
                Icon(icon, contentDescription = null,tint = Color.White)
            }
        }

        Icons.Filled.Person.let { icon ->
            IconButton(onClick = { /* Handle profile click */ }) {
                Icon(icon, contentDescription = null,tint = Color.White)
            }
        }
    }
}

@Composable
fun ProfilePageContent(modifier: Modifier = Modifier, navController: NavHostController, context: Context) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val auth = Firebase.auth
        val user = auth.currentUser

        if (user != null) {
            Text(text = "User email: ", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = user.email ?: "", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(16.dp))
        }

        var username by remember { mutableStateOf("") }
        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }

        val db = FirebaseFirestore.getInstance()
        db.document("users/${user?.uid}")
            .get()
            .addOnSuccessListener { document ->
                username = document.getString("username").toString()
                firstName = document.getString("firstName").toString()
                lastName = document.getString("lastName").toString()
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
        Text(text = "Username: ", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = username ?: "", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "First Name: ", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = firstName ?: "", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Last Name: ", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = lastName ?: "", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(16.dp))

        Button(modifier = Modifier.fillMaxWidth() ,onClick = {
            navController.navigate("changePassword")
        },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Black
            )) {
            Text(text = "Change password")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                logOut(context)
                navController.popBackStack("profile", true)
                navController.navigate("login")
            },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Black
            ),

            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(16.dp))
        var showDialog by remember { mutableStateOf(false) }
        Button(onClick = {
            showDialog = true
        }, colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor = Color.Black
        ),modifier = Modifier.fillMaxWidth()) {
            Text(text = "Delete Account")
        }

        if (showDialog)
        {
            showDialog = AlertDialogWithPassword(navController, showDialog)
        }
    }
}

@Composable
fun AlertDialogWithPassword(navController: NavHostController, showDialog: Boolean): Boolean {
    var password by remember { mutableStateOf("") }
    var showDialog2 by remember { mutableStateOf(showDialog) }

    if (showDialog2) {
        AlertDialog(
            onDismissRequest = { showDialog2 = false },
            title = { Text(text = "Delete Account") },
            text = {
                Column {
                    Text("Are you sure you want to delete your account?")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (password.isNotBlank()) {
                        deleteUser(navController.context, navController, password)
                        showDialog2 = false
                    }
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog2 = false }) {
                    Text("No")
                }
            }
        )
    }

    return showDialog2
}


fun deleteUser(context: Context, navController: NavHostController, password : String) {
    val auth = Firebase.auth
    val user = auth.currentUser
    val db = FirebaseFirestore.getInstance()
    db.collection("product_buy")
        .whereEqualTo("userId", user?.uid)
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                db.collection("product_buy").document(document.id).delete()
            }
        }
        .addOnFailureListener { exception ->
            Log.w("Firestore", "Error getting documents: ", exception)
        }
    db.collection("order_history")
        .whereEqualTo("userId", user?.uid)
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                db.collection("order_history").document(document.id).delete()
            }
        }

    db.collection("users").document(user?.uid!!)
        .delete()
        .addOnSuccessListener {
            Log.d("Firestore", "DocumentSnapshot successfully deleted!")
        }
        .addOnFailureListener { e ->
            Log.w("Firestore", "Error deleting document", e)
        }
    auth.signOut()
    auth.signInWithEmailAndPassword(user.email!!, password)
        .addOnSuccessListener {
            auth.currentUser!!.delete()
                .addOnSuccessListener {
                    logOut(context)
                    navController.popBackStack("profile", true)
                    navController.navigate("login")
                }
                .addOnFailureListener {
                    Log.e("TAG", "Error deleting user", it)
                }
        }
        .addOnFailureListener {
            Log.e("TAG", "Error signing in user", it)
        }

    /*user.delete()
        .addOnSuccessListener {
            logOut(context)
            navController.popBackStack("profile", true)
            navController.navigate("login")
        }
        .addOnFailureListener {
            Log.e("TAG", "Error deleting user", it)
        }*/
}

@Composable
fun ProfilePage(navController: NavHostController, context: Context) {
    // Check if user is logged in
    val auth = Firebase.auth
    val user = auth.currentUser
    if (user == null) {
        navController.popBackStack("login", true)
        navController.navigate("login")
    }
    ShoppingAppTheme {
        Scaffold(
            topBar =
            { ProfileTopBar(navController) },
            bottomBar = {
                ProfileBottomBar(navController)
            }
        ) {
            innerPadding ->
            ProfilePageContent(Modifier.padding(innerPadding), navController, context)
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun changePassword(navController: NavHostController)
{
    ShoppingAppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Change Password", textAlign = TextAlign.Center) },
                    navigationIcon = {
                        IconButton(onClick = {
                            // Handle navigation icon click
                            navController.navigate("profile")
                        }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                )
            },
        )
        {
            innerPadding ->
            ChangePasswordContent(Modifier.padding(innerPadding), navController)
        }
    }
}

@Composable
fun ChangePasswordContent(modifier: Modifier, navController: NavHostController)
{
        var email by remember { mutableStateOf("") }
        var errortext by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            var showDialog by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        if (changePassword(email)) {
                            showDialog = true
                            Log.e("TAG2", "Password changed successfully")
                        } else {
                            errortext = "Invalid username or password"
                        }
                    } else {
                        errortext = "Email and password cannot be empty"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Password")
            }
            if (showDialog) {

                AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                        val auth: FirebaseAuth = Firebase.auth
                        auth.signOut()
                        navController.popBackStack("profile", true)
                        navController.navigate("login")
                    },
                    title = { Text(text = "Password Reset") },
                    text = { Text("Check your email to reset your password") },
                    confirmButton = {
                        Button(onClick = { showDialog = false
                            val auth: FirebaseAuth = Firebase.auth
                            auth.signOut()
                            navController.popBackStack("profile", true)
                            navController.navigate("login") }) {
                            Text("OK")
                        }
                    }
                )
            }

            Text(text = errortext, modifier = Modifier.padding(8.dp), color = Color.Red)
        }
}



fun changePassword(email: String): Boolean {
    val auth: FirebaseAuth = Firebase.auth
    var success = true

    auth.sendPasswordResetEmail(email).addOnFailureListener() {
        Log.e("TAG", "Error sending password reset email", it)
        success = false
    }
    return success
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistory(navController: NavHostController)
{
    ShoppingAppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFD62727),
                        actionIconContentColor = Color.White,
                    ),
                    title = { Text("Order History", textAlign = TextAlign.Center, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = {
                            // Handle navigation icon click
                            navController.navigate("profile")
                        }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    },
                )
            },
            bottomBar = {
                OrderBottomBar(navController)
            }
        )
        {
            innerPadding ->
            OrderHistoryContent(Modifier.padding(innerPadding), navController)
        }
    }
}

@Composable
fun OrderHistoryContent(modifier: Modifier, navController: NavHostController)
{
    val db = FirebaseFirestore.getInstance()
    val auth = Firebase.auth
    val userID = auth.currentUser!!.uid
    val products = remember { mutableStateOf(listOf<Triple<Product, Int, Date>>()) }

    LaunchedEffect(Unit) {
        db.collection("order_history")
            .whereEqualTo("userId", userID)
            .get()
            .addOnSuccessListener { documents ->
                val productList = documents.mapNotNull { document ->
                    val productId = document.getString("productId")
                    val quantity = document.getLong("quantity")?.toInt()
                    val date = document.getDate("date")
                    if (productId != null && quantity != null) {
                        val product = runBlocking { GetProductsWithId(productId) }
                        product?.let { Triple(it, quantity, date!!) }
                    } else {
                        null
                    }
                }
                products.value = productList
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(16.dp)
        .verticalScroll(rememberScrollState())){
        Spacer(modifier = Modifier.height(25.dp))
        for ((product, quantity, date) in products.value) {
            OrderCard(product, quantity, date, navController)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (products.value.isEmpty()) {
            Text(text = "No items in the order history", fontSize = 20.sp)
        }

    }
}

@Composable
fun OrderCard(product: Product, quantity: Int, date: Date, navController: NavHostController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF1F1F1),
            contentColor = Color.Black
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            DisplayImageFromFirestore(
                fileName = product.id + ".png", modifier = Modifier
                    .height(100.dp)
                    .width(100.dp)
                    .clickable { navController.navigate("product/${product.id}/${quantity}") }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = quantity.toString() + "x " + product.title,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = roundToTwoDecimals(product.price.replace(',','.').toDouble() * quantity).toString().replace('.',',') + " €", fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = FormatDate(date), fontSize = 20.sp)
            }
        }
    }
}

fun FormatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    return formatter.format(date)
}

@Composable
fun OrderBottomBar(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD62727))
        ,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Icons.Filled.Home.let { icon ->
            IconButton(onClick = { navController.navigate("home") }) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
        Icons.Filled.ShoppingCart.let { icon ->
            IconButton(onClick = { navController.navigate("cart") }) {
                Icon(icon, contentDescription = null,tint = Color.White)
            }
        }
        Icons.Filled.Info.let { icon ->
            IconButton(onClick = { /* Handle order click */ }) {
                Icon(icon, contentDescription = null,tint = Color.White)
            }
        }

        Icons.Filled.Person.let { icon ->
            IconButton(onClick = { navController.navigate("profile") }) {
                Icon(icon, contentDescription = null,tint = Color.White)
            }
        }
    }
}