// src/main/kotlin/Main.kt
package main
// import das entidades
import entities.entities.User
import entities.entities.Ordem
import entities.entities.Product
import orm.RedisORM

fun main() {
    val orm = RedisORM()

    try {
        //criando um usuário de exemplo
        val newUser = User(
            id = "user001",
            name = "Ricardo X",
            email = "ricardo@exemplo.com",
            age = 21
        )
        // Inserindo o usuario no Redis usando o ORM
        //orm.insertUser(newUser) //inserUser é o nome que voce colocou la no outro arquivo (RedisORM)
        orm.create(newUser)
        println("Usuário criado: ${newUser.name} (ID: ${newUser.id})\n")

        // crie só um produto para ver se ta funcionado
        val newProduct1 = Product(
            id = "product001",
            name = "Arroz",
            price = 20.0,
            stock = 1,
        )
        //orm.insertProduct(newProduct1)
        orm.create(newProduct1)
        println("\nProduto cadastrado: -${newProduct1.name} (ID: ${newProduct1.id}) (R$ ${newProduct1.price} )\n")

        val carregado = orm.read(Product::class, "product001")
        println(carregado)

    //para criar algo novo é so colocar um nome, referenciar a entidade e colocar todos os valores que foi colocado no RedisORM
    // igual em newUser e newProduct1 (nome) e User e Product(entidade)

    } finally {
        orm.close()
    }

}


