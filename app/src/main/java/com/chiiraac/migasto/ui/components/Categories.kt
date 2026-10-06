package com.chiiraac.migasto.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CandlestickChart
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.chiiraac.migasto.R
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.data.model.MovementType.BILL
import com.chiiraac.migasto.data.model.MovementType.EXPENSE
import com.chiiraac.migasto.data.model.MovementType.INCOME
import com.chiiraac.migasto.data.model.MovementType.TRANSFER

/** Categoría predefinida. El [id] es lo que se guarda en la base de datos y no debe cambiar. */
data class CategoryInfo(
    val id: String,
    @StringRes val label: Int,
    val icon: ImageVector,
    val color: Color,
    val types: Set<MovementType>,
)

object Categories {
    const val TRANSFER_ID = "transfer"
    const val OTHER_ID = "other"

    private val OUT = setOf(EXPENSE, BILL)

    val all: List<CategoryInfo> = listOf(
        // Ingresos
        CategoryInfo("salary", R.string.cat_salary, Icons.Rounded.Payments, Color(0xFF10B981), setOf(INCOME)),
        CategoryInfo("extra_income", R.string.cat_extra_income, Icons.AutoMirrored.Rounded.TrendingUp, Color(0xFF34D399), setOf(INCOME)),
        CategoryInfo("sales", R.string.cat_sales, Icons.Rounded.Sell, Color(0xFF14B8A6), setOf(INCOME)),
        CategoryInfo("investments", R.string.cat_investments, Icons.Rounded.CandlestickChart, Color(0xFF06B6D4), setOf(INCOME)),
        CategoryInfo("refunds", R.string.cat_refunds, Icons.Rounded.CurrencyExchange, Color(0xFF22C55E), setOf(INCOME)),
        // Gastos y facturas
        CategoryInfo("groceries", R.string.cat_groceries, Icons.Rounded.ShoppingCart, Color(0xFF22C55E), setOf(EXPENSE)),
        CategoryInfo("restaurants", R.string.cat_restaurants, Icons.Rounded.Restaurant, Color(0xFFF97316), setOf(EXPENSE)),
        CategoryInfo("home", R.string.cat_home, Icons.Rounded.Home, Color(0xFFF59E0B), OUT),
        CategoryInfo("rent", R.string.cat_rent, Icons.Rounded.Key, Color(0xFFEAB308), OUT),
        CategoryInfo("electricity", R.string.cat_electricity, Icons.Rounded.Bolt, Color(0xFFFACC15), OUT),
        CategoryInfo("water", R.string.cat_water, Icons.Rounded.WaterDrop, Color(0xFF38BDF8), OUT),
        CategoryInfo("gas", R.string.cat_gas, Icons.Rounded.LocalFireDepartment, Color(0xFFFB923C), OUT),
        CategoryInfo("internet", R.string.cat_internet, Icons.Rounded.Wifi, Color(0xFF818CF8), OUT),
        CategoryInfo("insurance", R.string.cat_insurance, Icons.Rounded.Security, Color(0xFF64748B), OUT),
        CategoryInfo("transport", R.string.cat_transport, Icons.Rounded.DirectionsCar, Color(0xFF3B82F6), OUT),
        CategoryInfo("leisure", R.string.cat_leisure, Icons.Rounded.Celebration, Color(0xFFEF4444), setOf(EXPENSE)),
        CategoryInfo("health", R.string.cat_health, Icons.Rounded.MedicalServices, Color(0xFFF43F5E), OUT),
        CategoryInfo("clothing", R.string.cat_clothing, Icons.Rounded.Checkroom, Color(0xFFEC4899), setOf(EXPENSE)),
        CategoryInfo("beauty", R.string.cat_beauty, Icons.Rounded.Spa, Color(0xFFD946EF), setOf(EXPENSE)),
        CategoryInfo("education", R.string.cat_education, Icons.Rounded.School, Color(0xFF8B5CF6), OUT),
        CategoryInfo("kids", R.string.cat_kids, Icons.Rounded.ChildCare, Color(0xFFA855F7), OUT),
        CategoryInfo("pets", R.string.cat_pets, Icons.Rounded.Pets, Color(0xFFA16207), OUT),
        CategoryInfo("gifts", R.string.cat_gifts, Icons.Rounded.CardGiftcard, Color(0xFFE879F9), setOf(EXPENSE, INCOME)),
        CategoryInfo("travel", R.string.cat_travel, Icons.Rounded.Flight, Color(0xFF0EA5E9), setOf(EXPENSE)),
        CategoryInfo("subscriptions", R.string.cat_subscriptions, Icons.Rounded.Subscriptions, Color(0xFFDC2626), OUT),
        CategoryInfo("loans", R.string.cat_loans, Icons.Rounded.Handshake, Color(0xFF78716C), setOf(EXPENSE, BILL, INCOME)),
        CategoryInfo("taxes", R.string.cat_taxes, Icons.Rounded.RequestQuote, Color(0xFF94A3B8), OUT),
        CategoryInfo(OTHER_ID, R.string.cat_other, Icons.Rounded.Category, Color(0xFF9CA3AF), setOf(EXPENSE, BILL, INCOME)),
        CategoryInfo(TRANSFER_ID, R.string.cat_transfer, Icons.Rounded.SwapHoriz, Color(0xFF60A5FA), setOf(TRANSFER)),
    )

    private val byId = all.associateBy { it.id }

    val unknown = CategoryInfo("unknown", R.string.cat_unknown, Icons.Rounded.Category, Color(0xFF9CA3AF), emptySet())

    fun forType(type: MovementType): List<CategoryInfo> = all.filter { type in it.types }

    fun byId(id: String): CategoryInfo = byId[id] ?: unknown

    fun isValidFor(id: String, type: MovementType): Boolean = byId[id]?.types?.contains(type) == true
}
