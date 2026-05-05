package com.noahjutz.gymroutines

import com.noahjutz.gymroutines.util.formatSimple
import com.noahjutz.gymroutines.util.pretty
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DateUtilTest {
    @Test
    fun `pretty renders hours and minutes`() {
        assertThat((2.hours + 15.minutes).pretty()).isEqualTo("2h 15min")
        assertThat(45.minutes.pretty()).isEqualTo("0h 45min")
    }

    @Test
    fun `formatSimple returns a non-blank localized date string`() {
        assertThat(Date(0).formatSimple()).isNotBlank
    }
}
