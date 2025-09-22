package com.juangilles123.monifly.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Decoración personalizada para los elementos de la lista de deudas
 * Proporciona espaciado consistente entre elementos
 */
class DebtItemDecoration(
    private val spacing: Int = 16
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount
        
        // Aplicar espaciado superior para todos los elementos excepto el primero
        if (position > 0) {
            outRect.top = spacing / 2
        }
        
        // Aplicar espaciado inferior para todos los elementos excepto el último
        if (position < itemCount - 1) {
            outRect.bottom = spacing / 2
        }
        
        // Espaciado horizontal mínimo
        outRect.left = spacing / 4
        outRect.right = spacing / 4
        
        // Espaciado extra para el primer elemento
        if (position == 0) {
            outRect.top = spacing
        }
        
        // Espaciado extra para el último elemento
        if (position == itemCount - 1) {
            outRect.bottom = spacing * 2 // Espacio extra para el FAB
        }
    }
}