package com.jaytux.grader.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jaytux.grader.viewmodel.immutable

interface MeasuredLazyListScope : LazyListScope {
    fun measuredWidth(): State<Dp>

    fun measuredItem(content: @Composable MeasuredLazyItemScope.() -> Unit)
}

interface MeasuredLazyItemScope : LazyItemScope {
    fun measuredWidth(): State<Dp>
}

@Composable
fun MeasuredLazyColumn(modifier: Modifier = Modifier, key: Any? = null, content: MeasuredLazyListScope.() -> Unit) {
    val measuredWidth = remember(key) { mutableStateOf(0.dp) }
    LazyColumn(modifier.onGloballyPositioned {
        measuredWidth.value = it.size.width.dp
    }) {
        val lisToMlis = { lis: LazyItemScope ->
            object : MeasuredLazyItemScope, LazyItemScope by lis {
                override fun measuredWidth(): State<Dp> = measuredWidth.immutable()
            }
        }


        val scope = object : MeasuredLazyListScope, LazyListScope by this {
            override fun measuredWidth(): State<Dp> = measuredWidth.immutable()

            override fun measuredItem(content: @Composable MeasuredLazyItemScope.() -> Unit) {
                item {
                    lisToMlis(this).content()
                }
            }
        }

        scope.content()
    }
}