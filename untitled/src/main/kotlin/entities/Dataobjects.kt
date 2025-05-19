package main.entities
import entities.entities.Product
import entities.entities.User
import entities.entities.Order

val newUser = User(
    id = "user001",
    name = "Ricardo X",
    email = "ricardo@exemplo.com",
    age = 21
)

val newProduct1 = Product(
    id = "product001",
    name = "Arroz",
    price = 20.0,
    stock = 1
)

val newProduct2 = Product(
    id = "product002",
    name = "Feijao",
    price = 40.0,
    stock = 2
)

val newOrder = Order(
    id = "order001",
    userID = "user001",
    produtoIDs = "product001,product002",
    total = 60.0
)