package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.kyant.shapes.Capsule

@Composable
internal fun MoreIconSwitchRow(
    label: String,
    checked: Boolean,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { glyph() }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        MoreSwitch(checked)
    }
}

@Composable
internal fun MoreIconChoiceRow(
    label: String,
    value: String,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { glyph() }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White.copy(alpha = 0.58f), fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        ChevronRightGlyph()
    }
}

@Composable
internal fun MoreLoopRow(loopMode: Int, onSelect: (Int) -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { LoopGlyph() }
        Spacer(Modifier.width(12.dp))
        Text("循环播放", color = Color.White, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        listOf("单集循环", "列表循环", "不循环").forEachIndexed { index, label ->
            if (index > 0) {
                Text(
                    text = "|",
                    color = Color.White.copy(alpha = 0.18f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 5.dp),
                )
            }
            Text(
                text = label,
                color = if (loopMode == index + 1 || (index == 2 && loopMode == 0)) {
                    BrandPink
                } else {
                    Color.White.copy(alpha = 0.55f)
                },
                fontSize = 12.sp,
                modifier = Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                ) {
                    onSelect(if (index == 2) 0 else index + 1)
                },
            )
        }
    }
}

@Composable
internal fun MoreSwitch(checked: Boolean) {

    Box(
        modifier = Modifier
            .width(40.dp)
            .height(22.dp)
            .clip(Capsule())
            .background(if (checked) BrandPink else Color.White.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .padding(start = if (checked) 20.dp else 2.dp, top = 2.dp)
                .size(18.dp)
                .clip(Capsule())
                .background(Color.White),
        )
    }
}

@Composable
internal fun QuickAction(
    label: String,
    modifier: Modifier = Modifier,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit,
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            glyph()
        }
        Spacer(Modifier.height(5.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
    }
}
