package orm

import entities.entities.Product
import entities.entities.User
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisClientConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

// Para chamar os objetos criados la em entities voces precisam dar import nelas que nem User

class RedisORM (){
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

    fun create(obj: Any){
        val kClass = obj::class
        val NameClass = kClass.simpleName ?: "Escondido"

        //precisamos encontrar o campo de ID da classe

        val idProperty = kClass.memberProperties.find { it.name == "id" }
            ?: throw IllegalArgumentException ("Classe ${NameClass} precisa ter um id")

        val id = (idProperty as KProperty1<Any, *>).get(obj)?.toString()
            ?: throw IllegalArgumentException("id nao pode ser nulo!")

        val redisKey = "$NameClass:$id"

        val hash = mutableMapOf<String, String>()
        for (prop in kClass.memberProperties) {
            val value = (prop as KProperty1<Any, *>).get(obj)?.toString() ?: continue
            hash[prop.name] = value
        }
        jedis.hset(redisKey, hash)
    }


    fun close() {
        jedis.close()
    }
}