package com.ar.markdowneditor.util;

import android.view.View;
import androidx.core.content.ContextCompat;
import com.ar.markdowneditor.R;
import com.google.android.material.card.MaterialCardView;

public class CardSelectionHelper {

    public static void applySelection(View itemView, boolean isSelected) {
        itemView.setActivated(isSelected);

        if (itemView instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) itemView;

            if (isSelected) {
                card.setStrokeWidth((int) (2.5f * itemView.getResources().getDisplayMetrics().density));
                card.setStrokeColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.md_text_primary));
            } else {
                card.setStrokeWidth((int) (1f * itemView.getResources().getDisplayMetrics().density));
                card.setStrokeColor(ContextCompat.getColor(
                        itemView.getContext(), R.color.md_outline));
            }
            card.setChecked(isSelected);
        }
    }
}