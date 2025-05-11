package main.entities
import entities.entities.Product
import entities.entities.User

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
