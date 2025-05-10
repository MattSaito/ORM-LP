package orm

import entities.entities.Product
import entities.entities.User
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisClientConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

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

    // aqui utiliza-se kClass para refletir os atributos de uma classe, por meio de kClass e possivel saber
    // nome da classe, os campos, metodos e etc... isto nos permite criar genericamente as classes no banco
    fun create(obj: Any){
        val kClass = obj::class
        val nameClass = kClass.simpleName ?: "Escondido"

        //precisamos encontrar o campo de ID da classe

        val idProperty = kClass.memberProperties.find { it.name == "id" }
            ?: throw IllegalArgumentException ("Classe ${nameClass} precisa ter um id")

        val id = (idProperty as KProperty1<Any, *>).get(obj)?.toString()
            ?: throw IllegalArgumentException("id nao pode ser nulo!")

        val redisKey = "$nameClass:$id"

        val hash = mutableMapOf<String, String>()
        for (prop in kClass.memberProperties) {
            val value = (prop as KProperty1<Any, *>).get(obj)?.toString() ?: continue
            hash[prop.name] = value
        }
        jedis.hset(redisKey, hash)
    }

    fun <T : Any > read (type : KClass<T>, id: String): T?{
       val redisKey = "${type.simpleName}:$id"
        val hash = jedis.hgetAll(redisKey)
        if (hash.isEmpty()) return null

        val constructor = type.primaryConstructor ?: throw IllegalArgumentException("Classe precisa de um construtor!")
        constructor.isAccessible = true

        val args = constructor.parameters.associateWith { param ->
            val value = hash[param.name]
            if (value != null){
                when (param.type.classifier){
                    Int::class -> value.toInt()
                    Double::class -> value.toDouble()
                    Boolean::class -> value.toBoolean()
                    String::class -> value
                    else -> throw IllegalArgumentException("Tipo ${param.type} nao e suportado")
                }
            }else null
        }
        return constructor.callBy(args)
    }

    fun update(obj: Any){
        val kClass = obj::class
        //precisamos encontrar o campo de ID da classe
        val idProperty = kClass.memberProperties.find { it.name == "id" }
            ?: throw IllegalArgumentException ("Classe precisa ter um id")

        val id = (idProperty as KProperty1<Any, *>).get(obj)?.toString()
            ?: throw IllegalArgumentException("id nao pode ser nulo!")

        val key = "${kClass.simpleName}:$id"
        val hash = mutableMapOf<String, String>()

        for (prop in kClass.memberProperties){
            (prop as KProperty1<Any, *>).get(obj)?.let {hash[prop.name] = it.toString()}
        }
        jedis.del(key)
        jedis.hset(key,hash)
    }

    fun delete (obj: Any ){
        val kClass = obj::class

        val idProperty = kClass.memberProperties.find {it.name == "id"}
            ?: throw IllegalArgumentException("Classe precisa ter um id")

        val id = (idProperty as KProperty1<Any, *>).get(obj)?.toString()
            ?:throw IllegalArgumentException("id nao pode ser nulo!")

        val key = "${kClass.simpleName}:$id"
        jedis.del(key)
    }

    fun close() {
        jedis.close()
    }
}