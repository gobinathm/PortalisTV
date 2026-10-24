package io.github.gobinathm.portalistv.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryGroupsTest {
    private fun group(vararg titles: String) = groupCategories(titles.toList()) { it }

    private fun child(title: String, label: String) = GroupChild(title, label)

    @Test
    fun `groups by the first word, keeping the portal's order`() {
        val nodes = group("KIDS", "SPORTS NBA", "TAMIL", "SPORTS NFL", "TAMIL NEWS", "NEPAL")
        assertThat(nodes).containsExactly(
            CategoryNode.Single("KIDS"),
            CategoryNode.Group("SPORTS", "SPORTS", listOf(child("SPORTS NBA", "NBA"), child("SPORTS NFL", "NFL"))),
            CategoryNode.Group("TAMIL", "TAMIL", listOf(child("TAMIL", "TAMIL"), child("TAMIL NEWS", "NEWS"))),
            CategoryNode.Single("NEPAL"),
        ).inOrder()
    }

    @Test
    fun `understands other separators and letter case`() {
        val nodes = group("UK | News", "uk: Sports", "IN - Hindi")
        val uk = nodes.first() as CategoryNode.Group
        assertThat(uk.children.map { it.label }).containsExactly("News", "Sports").inOrder()
        assertThat(nodes.last()).isEqualTo(CategoryNode.Single("IN - Hindi"))
    }

    @Test
    fun `leaves names starting with numbers or symbols alone`() {
        assertThat(group("4K MOVIES", "4K SPORTS")).containsExactly(
            CategoryNode.Single("4K MOVIES"),
            CategoryNode.Single("4K SPORTS"),
        )
    }
}
