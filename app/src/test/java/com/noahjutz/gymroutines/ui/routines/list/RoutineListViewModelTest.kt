package com.noahjutz.gymroutines.ui.routines.list

import com.noahjutz.gymroutines.data.domain.Routine
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RoutineListViewModelTest {
    @Test
    fun `expands week ranges into individual opening-page entries`() {
        val routine =
            Routine(
                routineId = 10,
                name = "Essentials 4x - Upper A (Weeks 1-4)",
            )

        val entries = expandRoutineWeekEntries(routine)

        assertThat(entries.map { it.displayName })
            .containsExactly(
                "Essentials 4x - Upper A (Week 1)",
                "Essentials 4x - Upper A (Week 2)",
                "Essentials 4x - Upper A (Week 3)",
                "Essentials 4x - Upper A (Week 4)",
            )
        assertThat(entries.map { it.routine.routineId }).containsOnly(10)
    }

    @Test
    fun `keeps non-ranged routine names unchanged`() {
        val routine =
            Routine(
                routineId = 11,
                name = "Push Day",
            )

        val entries = expandRoutineWeekEntries(routine)

        assertThat(entries).hasSize(1)
        assertThat(entries.single().displayName).isEqualTo("Push Day")
        assertThat(entries.single().routine.routineId).isEqualTo(11)
    }
}
