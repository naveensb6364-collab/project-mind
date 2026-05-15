package com.app.kutira_kushala.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.*
import androidx.compose.ui.text.style.TextOverflow
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import com.app.kutira_kushala.BuildConfig

// Predefined options for Skills and Categories
val artisanCategories = listOf(
    "Pottery",
    "Weaving",
    "Woodworking",
    "Jewelry",
    "Leatherwork",
    "Painting",
    "Knitting",
    "Candle Making",
    "Metalwork",
    "Embroidery",
    "Home Decor",
    "Other"
)

data class Product(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val price: String = ""
)

data class SellerInfo(
    val userId: String = "",
    val businessName: String = "",
    val location: String = "",
    val phoneNumber: String = "",
    val capacity: Int = 0,
    val isAcceptingOrders: Boolean = true,
    val skill: String = "",
    val familyPhotoUrl: String = ""
)

data class DiscoverProduct(
    val product: Product,
    val seller: SellerInfo?
)

@Composable
fun HomeScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) } // Default to Discover tab for buyers

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Discover") },
                    label = { Text("Discover") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Products") },
                    label = { Text("My Products") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> DiscoverScreen(navController)
                1 -> ProductsScreen()
                2 -> ProfileScreen(navController)
            }
        }
    }
}

@Composable
fun DiscoverScreen(navController: NavController) {
    val db = Firebase.firestore
    
    var searchQuery by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var sellers by remember { mutableStateOf<Map<String, SellerInfo>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch all products reactively
    LaunchedEffect(Unit) {
        db.collection("products").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                products = snapshot.documents.map { doc ->
                    Product(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        name = doc.getString("name") ?: "",
                        category = doc.getString("category") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getString("price") ?: ""
                    )
                }
            }
        }
    }

    // Fetch all sellers reactively
    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                sellers = snapshot.documents.associateBy({ it.id }, { doc ->
                    SellerInfo(
                        userId = doc.id,
                        businessName = doc.getString("businessName") ?: "Unknown Seller",
                        location = doc.getString("location") ?: "Unknown Location",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        capacity = (doc.getLong("capacity") ?: 0L).toInt(),
                        isAcceptingOrders = doc.getBoolean("isAcceptingOrders") ?: true,
                        skill = doc.getString("skill") ?: "",
                        familyPhotoUrl = doc.getString("familyPhotoUrl") ?: ""
                    )
                })
                isLoading = false
            }
        }
    }

    val discoverProducts = remember(products, sellers) {
        products.map { DiscoverProduct(it, sellers[it.userId]) }
    }

    val filteredProducts = remember(searchQuery, discoverProducts) {
        if (searchQuery.isEmpty()) {
            discoverProducts
        } else {
            discoverProducts.filter { item ->
                item.product.name.contains(searchQuery, ignoreCase = true) ||
                item.product.category.contains(searchQuery, ignoreCase = true) ||
                item.product.description.contains(searchQuery, ignoreCase = true) ||
                (item.seller?.businessName?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search by name, category, or seller...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            textStyle = MaterialTheme.typography.bodySmall,

            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("No products match your search.", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredProducts) { item ->
                    DiscoverProductItem(item, navController)
                }
            }
        }
    }
}

@Composable
fun DiscoverProductItem(item: DiscoverProduct, navController: NavController) {
    val context = LocalContext.current
    val product = item.product
    val seller = item.seller

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Whole image visible scale
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Header: Seller Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = seller?.businessName ?: "Unknown Business",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { 
                                if (seller?.userId != null) {
                                    navController.navigate("seller_details/${seller.userId}")
                                }
                            }
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = seller?.location ?: "Location not available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    
                    // Availability Status
                    val isAvailable = seller?.isAcceptingOrders == true
                    Surface(
                        color = if (isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        contentColor = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isAvailable) "Accepting Orders" else "At Capacity",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                // Product Details
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (product.category.isNotEmpty()) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text(product.category, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "₹${product.price}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (seller != null) {
                            Text(
                                text = "Available: ${seller.capacity} units",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (seller.capacity > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }

                    // Contact Button
                    Button(
                        onClick = {
                            if (seller?.phoneNumber?.isNotEmpty() == true) {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${seller.phoneNumber}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Seller")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductsScreen() {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val currentUser = auth.currentUser
    val context = LocalContext.current

    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    var capacity by remember { mutableIntStateOf(0) }
    var isAcceptingOrders by remember { mutableStateOf(true) }
    var isProfileUpdated by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        capacity = (snapshot.getLong("capacity") ?: 0L).toInt()
                        isAcceptingOrders = snapshot.getBoolean("isAcceptingOrders") ?: true
                        
                        val businessName = snapshot.getString("businessName") ?: ""
                        val skill = snapshot.getString("skill") ?: ""
                        val location = snapshot.getString("location") ?: ""
                        val phone = snapshot.getString("phoneNumber") ?: ""
                        
                        isProfileUpdated = businessName.isNotBlank() && skill.isNotBlank() && 
                                          location.isNotBlank() && phone.isNotBlank()
                    }
                }

            db.collection("products")
                .whereEqualTo("userId", currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        products = snapshot.documents.map { doc ->
                            Product(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                name = doc.getString("name") ?: "",
                                category = doc.getString("category") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: "",
                                description = doc.getString("description") ?: "",
                                price = doc.getString("price") ?: ""
                            )
                        }
                    }
                    isLoading = false
                }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                if (isProfileUpdated) {
                    showAddDialog = true 
                } else {
                    Toast.makeText(context, "Please complete your profile details first!", Toast.LENGTH_LONG).show()
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isProfileUpdated && !isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text("Finish updating your profile in the Profile tab to start listing products.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            CapacityMeter(
                capacity = capacity,
                isAcceptingOrders = isAcceptingOrders,
                onCapacityChange = { newCapacity ->
                    currentUser?.let {
                        db.collection("users").document(it.uid).update("capacity", newCapacity)
                    }
                },
                onToggleOrders = { newValue ->
                    currentUser?.let {
                        db.collection("users").document(it.uid).update("isAcceptingOrders", newValue)
                    }
                }
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No products added yet.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(products) { product ->
                        ProductItem(
                            product = product,
                            onEdit = { productToEdit = product },
                            onDelete = { productToDelete = product }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditProductDialog(
            onDismiss = { showAddDialog = false },
            onProductSaved = { showAddDialog = false }
        )
    }

    if (productToEdit != null) {
        AddEditProductDialog(
            product = productToEdit,
            onDismiss = { productToEdit = null },
            onProductSaved = { productToEdit = null }
        )
    }

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete '${productToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        productToDelete?.let {
                            db.collection("products").document(it.id).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                    productToDelete = null
                                }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CapacityMeter(
    capacity: Int,
    isAcceptingOrders: Boolean,
    onCapacityChange: (Int) -> Unit,
    onToggleOrders: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAcceptingOrders) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAcceptingOrders) "Accepting Orders" else "At Capacity / Not Accepting",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Weekly Units Available: $capacity",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = isAcceptingOrders,
                    onCheckedChange = onToggleOrders
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Update Capacity: ", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { if (capacity > 0) onCapacityChange(capacity - 1) }) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                }
                Text(
                    text = capacity.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { onCapacityChange(capacity + 1) }) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                }
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.1f)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${product.price}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (product.category.isNotEmpty()) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtisanDropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    product: Product? = null,
    onDismiss: () -> Unit,
    onProductSaved: () -> Unit
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val storage = Firebase.storage
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: artisanCategories.first()) }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var price by remember { mutableStateOf(product?.price ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isGeneratingAI by remember { mutableStateOf(false) }

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    Dialog(onDismissRequest = { if (!isUploading) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (product == null) "Add New Product" else "Edit Product",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (product?.imageUrl != null && product.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo", modifier = Modifier.size(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(8.dp))
                ArtisanDropdownField(
                    label = "Category",
                    options = artisanCategories,
                    selectedOption = category,
                    onOptionSelected = { category = it }
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            scope.launch {
                                isGeneratingAI = true
                                try {
                                    val prompt = "Write a creative and very concise product description for an artisan product called '$name' in the '$category' category. Keep it strictly under 40 words."
                                    val response = generativeModel.generateContent(prompt)
                                    val resultText = response.text
                                    if (!resultText.isNullOrBlank()) {
                                        description = resultText.trim()
                                    } else {
                                        Toast.makeText(context, "AI Error: Unexpected response format", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "AI Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isGeneratingAI = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter a product name first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGeneratingAI,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isGeneratingAI) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Write Description with AI")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it }, 
                    label = { Text("Description") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    minLines = 3
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Wholesale Price (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isUploading) {
                    CircularProgressIndicator()
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotEmpty() && price.isNotEmpty()) {
                                    isUploading = true
                                    val userId = auth.currentUser?.uid ?: return@Button
                                    
                                    val saveProductData: (String) -> Unit = { url ->
                                        val productMap = mapOf(
                                            "userId" to userId,
                                            "name" to name,
                                            "category" to category,
                                            "description" to description,
                                            "price" to price,
                                            "imageUrl" to url
                                        )
                                        
                                        val task = if (product == null) {
                                            db.collection("products").add(productMap)
                                        } else {
                                            db.collection("products").document(product.id).set(productMap)
                                        }
                                        
                                        task.addOnSuccessListener {
                                            onProductSaved()
                                            Toast.makeText(context, "Product Saved", Toast.LENGTH_SHORT).show()
                                        }.addOnFailureListener {
                                            isUploading = false
                                            Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    if (imageUri != null) {
                                        val fileName = UUID.randomUUID().toString()
                                        val ref = storage.reference.child("products/$userId/$fileName")
                                        ref.putFile(imageUri!!)
                                            .addOnSuccessListener {
                                                ref.downloadUrl.addOnSuccessListener { url ->
                                                    saveProductData(url.toString())
                                                }
                                            }
                                            .addOnFailureListener {
                                                isUploading = false
                                                Toast.makeText(context, "Image Upload Failed", Toast.LENGTH_SHORT).show()
                                            }
                                    } else if (product != null) {
                                        saveProductData(product.imageUrl)
                                    } else {
                                        isUploading = false
                                        Toast.makeText(context, "Please pick an image", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(navController: NavController) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val currentUser = auth.currentUser
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }

    // Form states
    var businessName by remember { mutableStateOf("") }
    var skill by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var familyPhotoUrl by remember { mutableStateOf("") }

    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && currentUser != null) {
            imageUri = uri
            val storageRef = Firebase.storage.reference
            val imageRef = storageRef.child("profile_images/${currentUser.uid}")

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        db.collection("users").document(currentUser.uid)
                            .set(mapOf("familyPhotoUrl" to downloadUri.toString()), com.google.firebase.firestore.SetOptions.merge())
                        Toast.makeText(context, "Photo Updated", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        businessName = document.getString("businessName") ?: ""
                        skill = document.getString("skill") ?: ""
                        location = document.getString("location") ?: ""
                        phoneNumber = document.getString("phoneNumber") ?: ""
                        familyPhotoUrl = document.getString("familyPhotoUrl") ?: ""
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null || familyPhotoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUri ?: familyPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = currentUser?.email ?: "No Email", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(32.dp))

            if (isEditing) {
                ProfileEditForm(
                    businessName = businessName, onBusinessNameChange = { businessName = it },
                    skill = skill, onSkillChange = { skill = it },
                    location = location, onLocationChange = { location = it },
                    phoneNumber = phoneNumber, onPhoneNumberChange = { phoneNumber = it },
                    onSave = {
                        val data = mapOf(
                            "businessName" to businessName,
                            "skill" to skill,
                            "location" to location,
                            "phoneNumber" to phoneNumber
                        )
                        currentUser?.let {
                            db.collection("users").document(it.uid).set(data, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener { 
                                    isEditing = false
                                    Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    onCancel = { isEditing = false }
                )
            } else {
                ProfileDisplay(
                    businessName = businessName, skill = skill, location = location, phoneNumber = phoneNumber,
                    onEdit = { isEditing = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate("login") { popUpTo("home") { inclusive = true } }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}

@Composable
fun ProfileDisplay(businessName: String, skill: String, location: String, phoneNumber: String, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileItem(label = "Business", value = businessName, icon = Icons.Default.Business)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            ProfileItem(label = "Skill", value = skill, icon = Icons.Default.Handyman)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            ProfileItem(label = "Location", value = location, icon = Icons.Default.LocationOn)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            ProfileItem(label = "Phone", value = phoneNumber, icon = Icons.Default.Phone)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Edit, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Edit Profile")
    }
}

@Composable
fun ProfileItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Text(value.ifEmpty { "Not set" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ProfileEditForm(
    businessName: String, onBusinessNameChange: (String) -> Unit,
    skill: String, onSkillChange: (String) -> Unit,
    location: String, onLocationChange: (String) -> Unit,
    phoneNumber: String, onPhoneNumberChange: (String) -> Unit,
    onSave: () -> Unit, onCancel: () -> Unit
) {
    Column {
        OutlinedTextField(value = businessName, onValueChange = onBusinessNameChange, label = { Text("Business Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        
        ArtisanDropdownField(
            label = "Skill",
            options = artisanCategories,
            selectedOption = if (skill.isEmpty()) artisanCategories.first() else skill,
            onOptionSelected = onSkillChange
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = location, onValueChange = onLocationChange, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = phoneNumber, onValueChange = onPhoneNumberChange, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onSave) { Text("Save") }
        }
    }
}
