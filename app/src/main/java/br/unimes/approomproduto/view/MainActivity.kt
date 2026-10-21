package br.unimes.approomproduto.view
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.unimes.approomproduto.dao.ProductDao
import br.unimes.approomproduto.databinding.ActivityMainBinding
import br.unimes.approomproduto.db.AppDatabase
import br.unimes.approomproduto.model.Product
import kotlinx.coroutines.launch
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        // Inicializa o ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        )
            .fallbackToDestructiveMigration()
            .build()
        productDao = db.productDao()
        // Exemplo de inserção de produto
        binding.btnInserirProduto.setOnClickListener {
            val name = binding.editProductName.text.toString()
            val priceText = binding.editProductPrice.text.toString()
            val description = binding.editProductDescription.text.toString()

            val termo = binding.editBuscarNome.text.toString().trim()

            if (termo.isEmpty()) {
                binding.editBuscarNome.error = "Digite um nome para buscar"
                return@setOnClickListener
            }





            lifecycleScope.launch {
                val products = productDao.getAll()

                if (products.isNotEmpty()) {
                    // Monta o texto com todos os produtos separados por quebra de linha
                    val textoFormatado = products.joinToString(separator = "\n\n") { produto ->
                        "ID: ${produto.id} | Nome: ${produto.name}\nPreço: R$ ${produto.price}\nDescrição: ${produto.description ?: "Sem descrição"}"
                    }

                    // Atualiza o TextView na tela (A3)
                    binding.textProductList.text = textoFormatado

                    val totalItens = products.size
                    val somaPrecos = products.sumOf { it.price }

                    binding.textStockTotal.text = "Total de produtos: $totalItens\nValor total em estoque: R$ ${"%.2f".format(somaPrecos)}"
                } else {
                    binding.textProductList.text = "Nenhum produto encontrado no banco."
                }
            }

            val price = priceText.toDoubleOrNull() ?: 0.0

            if (price <= 0) {
                binding.editProductPrice.error = "Informe um preço válido"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                productDao.deleteAll()
                Toast.makeText(this@MainActivity, "Todos os produtos excluídos", Toast.LENGTH_SHORT).show()
                binding.textProductList.text = "" // Limpa a tela
            }


            // Verifica se os campos estão preenchidos
            if (name.isNotEmpty() && priceText.isNotEmpty()) {
                val price = priceText.toDoubleOrNull() ?: 0.0
                val product = Product(name = name, price = price, description =
                description)

                lifecycleScope.launch {
                    productDao.insertAll(product)
                    Log.i("TAG", "Produto inserido: $name - R$$price")
                    Toast.makeText(this@MainActivity, "Produto inserido!",
                        Toast.LENGTH_SHORT).show()

                    binding.editProductName.text.clear()
                    binding.editProductPrice.text.clear()
                    binding.editProductDescription.text.clear()
                    binding.editProductName.requestFocus()
                }
            } else {
                Toast.makeText(this, "Preencha os campos obrigatórios!",
                    Toast.LENGTH_SHORT).show()
            }


            val quantityText = binding.editProductQuantity.text.toString().trim()


            // Validações
            if (name.isBlank()) {
                binding.editProductName.error = "Informe um nome válido"
                return@setOnClickListener
            }


            // Leitura da quantidade (A8)
            val quantity = quantityText.toIntOrNull() ?: 0

            // Criação do produto com o novo campo quantity
            val product = Product(
                name = name,
                price = price,
                description = description,
                quantity = quantity
            )

            lifecycleScope.launch {
                productDao.insertAll(product)
                Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()

                // Limpeza dos campos (incluindo o novo campo)
                binding.editProductName.text.clear()
                binding.editProductPrice.text.clear()
                binding.editProductQuantity.text.clear()
                binding.editProductDescription.text.clear()
                binding.editProductName.requestFocus()
            }
        }

        binding.btnAtualizarPreco.setOnClickListener {
            val idText = binding.editUpdateId.text.toString().trim()
            val newPriceText = binding.editUpdatePrice.text.toString().trim()

            val id = idText.toIntOrNull()
            if (id == null) {
                binding.editUpdateId.error = "Informe um ID válido"
                return@setOnClickListener
            }

            val newPrice = newPriceText.toDoubleOrNull()
            if (newPrice == null || newPrice <= 0.0) {
                binding.editUpdatePrice.error = "Informe um preço válido"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Localiza o produto existente na base de dados
                val existingProduct = productDao.findById(id)

                if (existingProduct != null) {
                    // Cria uma nova instância mantendo os dados e alterando apenas o preço
                    val updatedProduct = existingProduct.copy(price = newPrice)
                    productDao.update(updatedProduct)

                    Toast.makeText(this@MainActivity, "Preço atualizado com sucesso!", Toast.LENGTH_SHORT).show()

                    // Limpa os campos de atualização
                    binding.editUpdateId.text.clear()
                    binding.editUpdatePrice.text.clear()

                    // Simula o clique para refrescar a lista visual no ecrã
                    binding.btnListarProdutos.performClick()
                } else {
                    Toast.makeText(this@MainActivity, "Produto com ID $id não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Exemplo de recuperação de produtos
        binding.btnListarProdutos.setOnClickListener {
            // Recupera os produtos de forma assíncrona
            lifecycleScope.launch {
                val products = productDao.getAll()
                if (products.isNotEmpty()) {
                    products.forEach {
                        Log.i("TAG", "Produto = ${it.name}, Preço = R$${it.price}, Descrição = ${it.description}")
                    }
                } else {
                    Log.i("TAG", "Nenhum produto encontrado.")
                }
            }
        }
    }
}