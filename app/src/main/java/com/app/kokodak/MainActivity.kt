package com.app.kokodak

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.app.kokodak.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var pageDao: PageDao
    private lateinit var thermalGuard: ThermalGuard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация на Room и термичния защитник
        db = AppDatabase.getDatabase(this)
        pageDao = db.pageDao()
        thermalGuard = ThermalGuard(this)

        // 1. Активиране на стабилната фонова автономия
        setupResilientAutoCrawler()

        // 2. Бутон за ръчно добавяне на начален URL в опашката
        binding.btnStartCrawl.setOnClickListener {
            val seedUrl = binding.etSeedUrl.text.toString().trim()
                .ifEmpty { "https://bg.wikipedia.org" }

            // Проверка за температура преди да стартираме ръчен процес
            if (thermalGuard.isOverheating()) {
                val temp = thermalGuard.getBatteryTemperature()
                Toast.makeText(this, "Xperia Z5 прегрява ($temp°C)! Охладете я.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Добавяне в опашката: $seedUrl", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch(Dispatchers.IO) {
                // Добавяме стартовия адрес с дълбочина 0 в базата данни
                val seedEntity = CrawlQueueEntity(
                    url = seedUrl,
                    depth = 0,
                    status = "PENDING"
                )
                pageDao.insertQueueItems(listOf(seedEntity))

                // Пускаме еднократна задача да започне обработката веднага
                val oneTimeWork = OneTimeWorkRequestBuilder<CrawlWorker>().build()
                WorkManager.getInstance(applicationContext).enqueue(oneTimeWork)
            }
        }

        // 3. Бутон за търсене в базата данни
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearchQuery.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Моля въведете дума за търсене!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performSearch(query)
        }

        // 4. Бутон за ръчно задействане на PageRank
        binding.btnPageRank.setOnClickListener {
            val pageRankWorkRequest = OneTimeWorkRequestBuilder<PageRankWorker>().build()
            WorkManager.getInstance(applicationContext).enqueue(pageRankWorkRequest)
            Toast.makeText(this, "PageRank се изчислява във фонов режим...", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Конфигурира устойчивия WorkManager, който прави опити за рестарт
     * дори при срив или изключване на приложението от системата.
     */
    private fun setupResilientAutoCrawler() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Задаване на периодична задача на всеки 15 минути с експоненциален backoff при грешка
        val crawlWorkRequest = PeriodicWorkRequestBuilder<CrawlWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "KokodakAutoCrawlTask",
            ExistingPeriodicWorkPolicy.KEEP,
            crawlWorkRequest
        )

        val currentTemp = thermalGuard.getBatteryTemperature()
        Toast.makeText(this, "Паякът е активен! Температура: $currentTemp°C", Toast.LENGTH_SHORT).show()
    }

    /**
     * Изпълнява търсене по ключова дума в индексираните страници.
     */
    private fun performSearch(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val searchResults = pageDao.searchPages(query)

            withContext(Dispatchers.Main) {
                if (searchResults.isEmpty()) {
                    binding.tvResults.text = "Няма намерени резултати за: \"$query\"\n\nПроверете дали паякът е завършил обхождането."
                } else {
                    val builder = StringBuilder()
                    builder.append("Намерени ${searchResults.size} резултата за \"$query\":\n\n")

                    searchResults.forEachIndexed { index, page ->
                        builder.append("${index + 1}. ${page.title ?: "Без заглавие"}\n")
                        builder.append("   URL: ${page.url}\n")
                        builder.append("   PageRank: ${String.format("%.4f", page.pageRank)}\n\n")
                    }
                    binding.tvResults.text = builder.toString()
                }
            }
        }
    }
}