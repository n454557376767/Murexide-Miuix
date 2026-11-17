package com.juhao.murexide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用自有的 Miuix 风格输入框（单行 / 多行共用）。
 *
 * 底色取 `secondaryContainer`——与 Miuix `TextFieldDefaults` 一致的输入框容器角色；
 * 占位文字用 `onSurfaceVariantSummary`，正文用 `onBackground`，光标用 `primary`。
 *
 * [String] 重载承载纯文本；需要精确控制光标 / 选区的场景用下面的 [TextFieldValue] 重载。
 */
@Composable
fun MiuixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minHeight: Int = 48,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    imeAction: ImeAction? = null,
    focusRequester: FocusRequester? = null,
) {
    MiuixTextFieldBox(
        modifier = modifier,
        hasContent = value.isNotEmpty(),
        placeholder = placeholder,
        enabled = enabled,
        singleLine = singleLine,
        minHeight = minHeight
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            enabled = enabled,
            textStyle = MiuixTheme.textStyles.body1.copy(
                color = MiuixTheme.colorScheme.onBackground
            ),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            keyboardOptions = imeAction?.let { keyboardOptions.copy(imeAction = it) }
                ?: keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary)
        )
    }
}

/**
 * [MiuixTextField] 的 [TextFieldValue] 重载。
 *
 * 用于需要在代码里改写光标位置 / 选区（例如在光标处插入 Markdown 片段）的编辑框，
 * 视觉与 [MiuixTextField] 完全一致。
 */
@Composable
fun MiuixTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minHeight: Int = 160,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    focusRequester: FocusRequester? = null,
) {
    MiuixTextFieldBox(
        modifier = modifier,
        hasContent = value.text.isNotEmpty(),
        placeholder = placeholder,
        enabled = enabled,
        singleLine = singleLine,
        minHeight = minHeight
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            enabled = enabled,
            textStyle = MiuixTheme.textStyles.body1.copy(
                color = MiuixTheme.colorScheme.onBackground
            ),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 4,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary)
        )
    }
}

/** 两个 [MiuixTextField] 重载共享的容器与占位文字排版。 */
@Composable
private fun MiuixTextFieldBox(
    modifier: Modifier,
    hasContent: Boolean,
    placeholder: String,
    enabled: Boolean,
    singleLine: Boolean,
    minHeight: Int,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
    ) {
        if (!hasContent && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1
            )
        }
        content()
    }
}
