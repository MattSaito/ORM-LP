// src/main/kotlin/Main.kt
package main
// import das entidades
import entities.entities.User
import entities.entities.Order
import entities.entities.Product
import orm.RedisORM
import main.entities.*

fun main() {
    val orm = RedisORM()

    try {
        orm.createraw(newUser)
        println("Usuário criado: ${newUser.name} (ID: ${newUser.id})\n")
        orm.createraw(newProduct1)
        println("\nProduto cadastrado: -${newProduct1.name} (ID: ${newProduct1.id}) (R$ ${newProduct1.price} )\n")

        val carregado = orm.read(Product::class, "product001")
        println(carregado)

        val produto1alterado = newProduct1.copy(price = 10.0)
        orm.updateraw(produto1alterado)

        val carregado1 = orm.read(Product::class, "product001")
        println(carregado1)
        orm.createraw(newProduct2)

        val carregado2 = orm.read(Product::class, id = "product002")
        println(carregado2)

//        orm.delete(newProduct2)

        val carregado3 = orm.read(Product::class, id = "product002")
        println(carregado3)

        orm.create<User> {
            set("id", "user002")
            set("name", "Alice")
            set("email", "Alice@exemple.com")
            set("age", 30)
        }

        orm.update<User>("user002") {
            set("age", 31)
        }

        orm.create<Product> {
            set("id", "product003")
            set("name", "Abacate")
            set("price", 100.00)
            set("stock", 50)
        }


        val result = orm.query<User> {
            from("User")
            where("age") { ((it as? Int) ?: 0) > 10 }
            select("id", "name")
            orderBy("age", ascending = true)
        }
        println(result)

        val searchprod = orm.query<Product> {
            select("id", "name")
            from ("Product")
            where("stock") {((it as? Int) ?: 0) > 0}
//            orderBy("stock", ascending = true)
        }
        println(searchprod)

        orm.createraw(newOrder)

        val carregado4 = orm.read(Order::class, id = "order001")
        println(carregado4)

        val searchord = orm.query<Order> {
            select("id","userID")
            from("Order")
//            where("produtoIDs") {it?.toString()?.split(",")?.map {id->id.trim()}?.contains("product001") == true}
            where("produtoIDs") {it.toString().contains("product001")}
        }
        println(searchord)

    } finally {
        orm.close()
    }

}


