package org.notests.rxfeedbackexample.github.paginated.search

import org.junit.Assert
import org.junit.Test

/**
 * Created by juraj on 06/12/2017.
 */

class TestParseLink {

    @Test
    fun testParse() {
        val links = "<https://api.github.com/search/repositories?q=Rx&page=2>; rel=\"next\", <https://api.github.com/search/repositories?q=Rx&page=34>; rel=\"last\""
        val parsedLinks = parseLinks(links)

        Assert.assertArrayEquals(parsedLinks.keys.toTypedArray(), arrayOf("next", "last"))
        Assert.assertArrayEquals(parsedLinks.values.toTypedArray(), arrayOf("https://api.github.com/search/repositories?q=Rx&page=2", "https://api.github.com/search/repositories?q=Rx&page=34"))
    }
}
