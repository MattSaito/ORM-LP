package entities.entities


// val é como se fosse o const, é imutavel
// para criar um novo objeto tem que usar data class e o nome
data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int
)

// Product.kt
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val stock: Int
)
data class Ordem (
    val id: String,
    val userID: String,
    val produtoIDs: List<Product>,
    val total: Double
)