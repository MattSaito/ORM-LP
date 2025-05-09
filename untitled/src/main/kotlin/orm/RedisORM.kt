package orm

import entities.entities.Product
import entities.entities.User
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisClientConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort

// Para chamar os objetos criados la em entities voces precisam dar import nelas que nem User

class RedisORM {
    private val host = HostAndPort("localhost", 6379) //porta que o redis está rodando
    private val config = DefaultJedisClientConfig.builder().build() // config padrão
    private val jedis = Jedis(host, config) //comunicação com o redis e onde tem as operações (hset,get...)

    fun insertUser(user: User) {
        jedis.hset("user:${user.id}", mapOf(
            "id" to user.id,
            "name" to user.name,
            "email" to user.email,
            "age" to user.age.toString()
        ))
    }

    fun insertProduct(product: Product) {
        jedis.hset("product:${product.id}", mapOf(
            "id" to product.id,
            "name" to product.name,
            "price" to product.price.toString(),       //precisa converte os valores double e int para string
            "stock" to product.stock.toString()        //porque o redis só armazena string
        ))
    }

    fun close() {
        jedis.close()
    }
}