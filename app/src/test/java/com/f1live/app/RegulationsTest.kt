package com.f1live.app

import com.f1live.app.data.repository.F1Repository
import com.f1live.app.data.repository.parseRegulations
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RegulationsTest {

    @Test
    fun parsesWikitextIntoCategories() {
        val wt = """
== Current rules and regulations ==

=== Technical ===

==== Chassis ====
An F1 car can be no more than 190 cm wide and 95 cm tall. {{cite web|url=x}}
The car must only have four wheels mounted externally of the body work.

==== Engine ====
The power unit must use a 1.6 litre V6 turbo hybrid engine [[Internal combustion engine|ICE]].

=== Sporting ===

==== Scoring ====
Points are awarded to the top 10 finishers of each race: 25, 18, 15, 12, 10, 8, 6, 4, 2, 1.

== History ==
Old stuff that must be ignored.
""".trimIndent()
        val regs = parseRegulations(wt, listOf("Current rules and regulations"))
        println("CATEGORIES: " + regs.categories.map { it.title to it.sections.map { s -> s.title to s.points.size } })
        regs.categories.forEach { c ->
            c.sections.forEach { sec -> println("[${c.title}] ${sec.title}:") ; sec.points.forEach { println("   - $it") } }
        }
        assertTrue(regs.categories.any { it.title.contains("Technical") })
        assertTrue(regs.categories.any { it.title.contains("Sporting") })
        assertTrue("history ignored", regs.categories.none { it.title.contains("History") })
        val chassis = regs.categories.first { it.title.contains("Technical") }.sections.first { it.title.contains("Chassis") }
        assertTrue(chassis.points.first().contains("190 cm"))
        assertTrue("refs stripped", !chassis.points.first().contains("{{"))
    }

    @Test
    fun liveRegulationsFromWikipedia() = runBlocking {
        val repo = F1Repository()
        val regs = repo.regulations(force = true)
        println("LIVE categories: " + regs.categories.map { "${it.title} (${it.sections.size} sections)" })
        regs.categories.firstOrNull()?.sections?.firstOrNull()?.let { sec ->
            println("sample [${regs.categories.first().title}] ${sec.title}: ${sec.points.firstOrNull()}")
        }
        assertTrue("expected categories", regs.categories.size >= 2)
        assertTrue(regs.categories.sumOf { it.sections.sumOf { s -> s.points.size } } > 10)
    }
}
