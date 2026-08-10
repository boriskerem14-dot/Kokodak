package com.app.kokodak.rank

import kotlin.math.abs

/**
 * Модел за представяне на уеб страница в графа.
 */
data class Page(val url: String)

/**
 * Изчислява PageRank върху граф от страници чрез итеративния метод (Power Iteration).
 * Приема тегла на връзките за отчитане на тяхната семантична позиция.
 */
class PageRankCalculator(
    private val dampingFactor: Double = 0.85,
    private val maxIterations: Int = 100,
    private val tolerance: Double = 0.0001
) {

    /**
     * Изчислява рейтинга на авторитет (PageRank) за всяка страница в графа.
     *
     * @param outgoingLinks Карта, където ключът е изходящата страница,
     *                      а стойността е друга карта с целевите страници и техните тегла.
     * @return Карта с изчислените PageRank стойности за всяка страница.
     */
    fun compute(outgoingLinks: Map<Page, Map<Page, Double>>): Map<Page, Double> {
        // 1. Събиране на абсолютно всички уникални страници в графа
        val allPages = mutableSetOf<Page>()
        allPages.addAll(outgoingLinks.keys)
        outgoingLinks.values.forEach { targets -> allPages.addAll(targets.keys) }

        val totalPages = allPages.size
        if (totalPages == 0) return emptyMap()

        // 2. Начално равномерно разпределение на рейтинга
        val initialRank = 1.0 / totalPages
        var currentRanks = allPages.associateWith { initialRank }.toMutableMap()

        // 3. Нормализиране на теглата на изходящите връзки
        val normalizedWeights = outgoingLinks.mapValues { (_, targets) ->
            val totalWeight = targets.values.sum()
            if (totalWeight > 0) {
                targets.mapValues { (_, weight) -> weight / totalWeight }
            } else {
                emptyMap()
            }
        }

        // Базов рейтинг за произволно прескачане (damping effect)
        val baseRank = (1.0 - dampingFactor) / totalPages

        // 4. Итеративен процес (Power Iteration)
        for (iteration in 1..maxIterations) {
            val nextRanks = allPages.associateWith { 0.0 }.toMutableMap()
            var danglingRankSum = 0.0

            // Намиране на "задънени" страници (без изходящи връзки)
            for (page in allPages) {
                val targets = normalizedWeights[page]
                if (targets.isNullOrEmpty()) {
                    danglingRankSum += currentRanks[page] ?: 0.0
                }
            }

            // Равномерно разпределение на рейтинга от задънените страници
            val danglingContribution = (dampingFactor * danglingRankSum) / totalPages

            // Пренасяне на рейтинг през изходящите връзки според теглата им
            for (page in allPages) {
                val rank = currentRanks[page] ?: 0.0
                val targets = normalizedWeights[page] ?: emptyMap()

                for ((targetPage, weight) in targets) {
                    val currentTargetRank = nextRanks[targetPage] ?: 0.0
                    nextRanks[targetPage] = currentTargetRank + (dampingFactor * rank * weight)
                }
            }

            // 5. Обновяване на рейтингите и проверка за конвергенция (сближаване)
            var maxDiff = 0.0
            for (page in allPages) {
                val newRank = baseRank + danglingContribution + (nextRanks[page] ?: 0.0)
                val oldRank = currentRanks[page] ?: 0.0
                val diff = abs(newRank - oldRank)
                if (diff > maxDiff) {
                    maxDiff = diff
                }
                nextRanks[page] = newRank
            }

            currentRanks = nextRanks

            // Ако промяната е по-малка от прага (tolerance), спираме итерациите по-рано
            if (maxDiff < tolerance) {
                break
            }
        }

        return currentRanks
    }
}