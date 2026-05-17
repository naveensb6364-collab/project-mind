package com.app.kutira_kushala.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDetailsScreen(sellerId: String, navController: NavController) {
    val db = Firebase.firestore
    var sellerInfo by remember { mutableStateOf<SellerInfo?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var familyPhotoUrl by remember { mutableStateOf("") }

    LaunchedEffect(sellerId) {
        // Fetch seller details
        db.collection("users").document(sellerId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                sellerInfo = SellerInfo(
                    userId = doc.id,
                    businessName = doc.getString("businessName") ?: "Unknown Seller",
                    location = doc.getString("location") ?: "Unknown Location",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    capacity = (doc.getLong("capacity") ?: 0L).toInt(),
                    isAcceptingOrders = doc.getBoolean("isAcceptingOrders") ?: true,
                    skill = doc.getString("skill") ?: "",
                    familyPhotoUrl = doc.getString("familyPhotoUrl") ?: ""
                )
                familyPhotoUrl = doc.getString("familyPhotoUrl") ?: ""
            }
        }

        // Fetch seller products
        db.collection("products").whereEqualTo("userId", sellerId).get().addOnSuccessListener { snapshot ->
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
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seller Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SellerHeader(sellerInfo, familyPhotoUrl)
                }
                
                item {
                    Text(
                        text = "Products",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(products) { product ->
                    ProductItemForDetails(product)
                }
            }
        }
    }
}

@Composable
fun SellerHeader(seller: SellerInfo?, familyPhotoUrl: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (familyPhotoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = familyPhotoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = seller?.businessName ?: "Unknown Business",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = seller?.location ?: "Unknown Location", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = seller?.skill ?: "Artisan", style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val isAvailable = seller?.isAcceptingOrders == true
            Surface(
                color = if (isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                contentColor = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isAvailable) "Accepting Orders (${seller?.capacity} units)" else "At Capacity",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProductItemForDetails(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.1f)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "₹${product.price}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
