    val consecutiveNoSpendDays = remember(state.allExpenses) {
        val personalExpenses = state.allExpenses.filter { 
            (it.accountType == "SERBATOIO_PERSONALE" || it.accountType == "DISCREZIONALE_VARIABILE") && !it.excludeFromStats
        }
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        var streak = 0
        var currentDateMillis = cal.timeInMillis
        
        while (true) {
            val endOfDay = currentDateMillis + 24 * 60 * 60 * 1000 - 1
            val expensesOnDay = personalExpenses.filter { it.dateMillis in currentDateMillis..endOfDay }
            if (expensesOnDay.isEmpty()) {
                streak++
                currentDateMillis -= 24 * 60 * 60 * 1000
            } else {
                break
            }
        }
        streak
    }

    val peakDayOfWeek = remember(filteredExpenses) {
        if (filteredExpenses.isEmpty()) return@remember Pair("Nessuno", 0.0)
        
        val expensesByDayOfWeek = filteredExpenses.groupBy { 
            val c = Calendar.getInstance()
            c.timeInMillis = it.dateMillis
            c.get(Calendar.DAY_OF_WEEK)
        }
        
        val averageByDay = expensesByDayOfWeek.mapValues { (_, expenses) ->
            val uniqueDates = expenses.map { 
                val c = Calendar.getInstance()
                c.timeInMillis = it.dateMillis
                c.set(Calendar.HOUR_OF_DAY, 0)
                c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0)
                c.set(Calendar.MILLISECOND, 0)
                c.timeInMillis
            }.distinct().size
            
            val totalSpentDay = expenses.sumOf { kotlin.math.abs(it.amount) }
            totalSpentDay / uniqueDates.coerceAtLeast(1)
        }
        
        val maxDay = averageByDay.maxByOrNull { it.value }
        if (maxDay != null) {
            val dayName = when (maxDay.key) {
                Calendar.MONDAY -> "Lunedì"
                Calendar.TUESDAY -> "Martedì"
                Calendar.WEDNESDAY -> "Mercoledì"
                Calendar.THURSDAY -> "Giovedì"
                Calendar.FRIDAY -> "Venerdì"
                Calendar.SATURDAY -> "Sabato"
                Calendar.SUNDAY -> "Domenica"
                else -> ""
            }
            Pair(dayName, maxDay.value)
        } else {
            Pair("Nessuno", 0.0)
        }
    }

    val previousPeriodComparison = remember(selectedPeriod, state.allExpenses, cycleStart, cycleEnd, totalSpent, selectedAccountFilter) {
        val prevStart: Long
        val prevEnd: Long
        when (selectedPeriod) {
            "Questo Ciclo", "Ciclo Precedente" -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = cycleStart
                    add(Calendar.DAY_OF_MONTH, -5)
                }
                val (ps, pe) = BillingCycleUtils.getCycleRange(cal.timeInMillis, state.resetDay)
                prevStart = ps
                prevEnd = pe
            }
            "Ultimi 30 Giorni" -> {
                prevEnd = cycleStart - 1
                prevStart = prevEnd - 30L * 24 * 60 * 60 * 1000 + 1
            }
            else -> {
                prevStart = 0L
                prevEnd = 0L
            }
        }
        
        if (prevStart == 0L) {
            null
        } else {
            val prevExpenses = state.allExpenses.filter { exp ->
                val matchesTime = exp.dateMillis in prevStart..prevEnd
                val matchesAccount = when (selectedAccountFilter) {
                    "👤 Personale" -> exp.accountType == "SERBATOIO_PERSONALE" || exp.accountType == "DISCREZIONALE_VARIABILE"
                    "🏠 Familiare" -> exp.accountType == "SERBATOIO_GINEVRA"
                    "🏛️ Costi Fissi" -> exp.accountType == "ESSENZIALE_REALE"
                    "Spese Quotidiane" -> exp.accountType == "SERBATOIO_PERSONALE" || exp.accountType == "DISCREZIONALE_VARIABILE" || exp.accountType == "SERBATOIO_GINEVRA"
                    else -> true
                }
                matchesTime && matchesAccount && !exp.excludeFromStats
            }
            val prevTotal = prevExpenses.sumOf { kotlin.math.abs(it.amount) }
            val delta = totalSpent - prevTotal
            val percentage = if (prevTotal > 0) (delta / prevTotal) * 100 else if (totalSpent > 0) 100.0 else 0.0
            Triple(prevTotal, delta, percentage)
        }
    }
