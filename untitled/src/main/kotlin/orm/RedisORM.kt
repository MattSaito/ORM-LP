package orm

import com.google.gson.Gson
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

//builder para create com dsl
class GenericBuilder<T : Any>(private val kClass: KClass<T>) {
    private val values = mutableMapOf<String, Any?>()

    fun set(field: String, value: Any?) {
        values[field] = value
    }

    fun build(): T {
        val ctor = kClass.primaryConstructor
            ?: throw IllegalArgumentException("Classe precisa de construtor primário")

        val args = ctor.parameters.associateWith { param ->
            values[param.name]
        }

        return ctor.callBy(args)
    }
}

data class QueryPlan<T : Any>(
    val typeName: String,
    val filters: List<Pair<String, (Any?) -> Boolean>>,
    val selectedFields: List<String>?,
    val orderBy: Pair<String, Boolean>? // (campo, ascending)
)


class QueryBuilder<T : Any> {
    private var typeName: String? = null
    private var whereFilters: MutableList<Pair<String, (Any?) -> Boolean>> = mutableListOf()
    private var selectedFields: List<String>? = null
    private var orderByField : Pair<String, Boolean> ? = null


    fun from(type: String) {
        typeName = type
    }

    fun where(field: String, condition: (Any?) -> Boolean) {
        whereFilters.add(field to condition)
    }

    fun select(vararg fields: String) {
        selectedFields = fields.toList()
    }

    fun orderBy(field: String, ascending: Boolean = true ) {
        orderByField = field to ascending
    }
    fun build(): QueryPlan<T> {
        return QueryPlan(typeName!!, whereFilters, selectedFields, orderByField)
    }
}


class RedisORM (){
    private val host = HostAndPort("localhost", 6379) //porta que o redis está rodando
    private val config = DefaultJedisClientConfig.builder().build() // config padrão
    val jedis = Jedis(host, config) //comunicação com o redis e onde tem as operações (hset,get...)
    val gson = Gson() //precisa ser publico assim como o jedis acima esta precisando ser tambem

    // aqui utiliza-se kClass para refletir os atributos de uma classe, por meio de kClass e possivel saber
    // nome da classe, os campos, metodos e etc... isto nos permite criar genericamente as classes no banco
    fun createraw(obj: Any){
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

    inline fun <reified T : Any> create(block: GenericBuilder<T>.() -> Unit) {
        val builder = GenericBuilder(T::class)
        builder.block()
        val obj = builder.build()
        createraw(obj)  
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

    fun updateraw(obj: Any){
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

    inline fun <reified T : Any> update(id: String, block: GenericBuilder<T>.() -> Unit) {
        val existing = read(T::class, id)
            ?: throw IllegalArgumentException("Objeto com id=$id não encontrado")

        val kClass = T::class
        val builder = GenericBuilder(kClass)

        // Copia os campos do objeto atual para o builder
        kClass.memberProperties.forEach { prop ->
            val value = prop.get(existing)
            if (value != null) builder.set(prop.name, value)
        }

        // Aplica as alterações do usuário
        builder.block()

        // Cria novo objeto atualizado
        val updated = builder.build()

        // Salva no Redis
        updateraw(updated)
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

    inline fun <reified T : Any> query(block: QueryBuilder<T>.() -> Unit): List<Map<String, Any?>> {
        val builder = QueryBuilder<T>()
        builder.block()
        val plan = builder.build()

        //buscando chaves
        val keys = jedis.keys("${plan.typeName}:*")

        // 1. Construir a lista de objetos filtrados
        val objs = keys.mapNotNull { key ->
            val hash = jedis.hgetAll(key)
            if (hash.isEmpty()) return@mapNotNull null

            val json = gson.toJson(hash) // convertendo hash em json
            val obj = gson.fromJson(json, T::class.java)

            // Aplicando filtros
            val match = plan.filters.all { (field, condition) ->
                val value = T::class.memberProperties
                    .find { it.name == field }
                    ?.getter
                    ?.call(obj)
                condition(value)
            }

            if (!match) return@mapNotNull null

            obj
        }

        // 2. Aplicar ordenação sobre os objetos
        val sortedObjs = plan.orderBy?.let { (field, ascending) ->
            val comparator = compareBy<T> {
                T::class.memberProperties
                    .find { it.name == field }
                    ?.getter
                    ?.call(it) as? Comparable<Any>
            }

            if (ascending) objs.sortedWith(comparator)
            else objs.sortedWith(comparator.reversed())
        } ?: objs

        // 3. Converter para Map<String, Any?> de acordo com select
        return sortedObjs.map { obj ->
            if (plan.selectedFields != null) {
                plan.selectedFields.associateWith { fieldName ->
                    T::class.memberProperties.find { it.name == fieldName }?.getter?.call(obj)
                }
            } else {
                T::class.memberProperties.associate { prop ->
                    prop.name to prop.getter.call(obj)
                }
            }
        }
    }




    fun close() {
        jedis.close()
    }
}