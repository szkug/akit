import kotlin.test.Test
import kotlin.test.assertEquals

class StringsFileCodecTest {
    @Test
    fun parseAndBuildRoundTripKeepsEntries() {
        val input = linkedMapOf(
            "title" to "Hello",
            "subtitle" to "World",
        )

        val content = StringsFileCodec.build(input)
        val parsed = StringsFileCodec.parse(content)

        assertEquals(input, parsed)
    }
}
