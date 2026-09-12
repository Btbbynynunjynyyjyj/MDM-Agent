package uz.mdm.agent

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {

    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private var allowedApps: List<AppEntry> = emptyList()

    private val allowedPackages = setOf(
        "com.android.chrome", "com.google.android.gm", "com.google.android.youtube",
        "com.google.android.apps.maps", "org.telegram.messenger", "com.whatsapp",
        "com.google.android.calculator", "com.sec.android.app.popupcalculator",
        "com.google.android.calendar", "com.google.android.apps.docs",
        "com.google.android.apps.drive", "com.google.android.apps.meetings",
        "com.google.android.apps.photos", "com.android.vending",
        "com.google.android.documentsui", "com.google.android.contacts",
        "com.google.android.deskclock", "com.android.settings"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 17, 31)
        window.navigationBarColor = Color.rgb(5, 10, 18)
        buildUi()
        loadAllowedApps()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setBackgroundColor(Color.rgb(7, 17, 31))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "◈"; textSize = 28f; setTextColor(Color.rgb(79, 140, 255)); gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        titleBox.addView(TextView(this).apply { text = "MDM Agent"; textSize = 21f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        titleBox.addView(TextView(this).apply { text = "Managed device"; textSize = 12f; setTextColor(Color.rgb(137, 154, 177)) })
        header.addView(titleBox, LinearLayout.LayoutParams(0, dp(50), 1f))
        root.addView(header, LinearLayout.LayoutParams(-1, dp(52)))

        search = EditText(this).apply {
            hint = "Dastur qidirish…"; textSize = 15f; singleLine = true
            setTextColor(Color.WHITE); setHintTextColor(Color.rgb(120, 137, 161)); setPadding(dp(16), 0, dp(16), 0)
            background = rounded(Color.rgb(14, 28, 47), dp(14).toFloat())
            addTextChangedListener(SimpleTextWatcher { renderApps(it) })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10) })

        val update = Button(this).apply {
            text = "Barcha ilovalarni yangilash"; textSize = 14f; isAllCaps = false; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); background = rounded(Color.rgb(55, 110, 220), dp(13).toFloat())
            setOnClickListener { Toast.makeText(this@MainActivity, "Yangilanishlar tekshirilmoqda…", Toast.LENGTH_SHORT).show() }
        }
        root.addView(update, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10) })

        grid = GridLayout(this).apply { alignmentMode = GridLayout.ALIGN_BOUNDS; useDefaultMargins = false }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(grid, ViewGroup.LayoutParams(-1, -2)) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(14) })
        setContentView(root)
        root.viewTreeObserver.addOnGlobalLayoutListener { updateColumns() }
    }

    private fun loadAllowedApps() {
        val pm = packageManager
        allowedApps = allowedPackages.mapNotNull { pkg -> runCatching {
            val info = pm.getApplicationInfo(pkg, 0)
            AppEntry(pkg, pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info))
        }.getOrNull() }.sortedBy { it.label.lowercase() }
        renderApps(search.text?.toString().orEmpty())
    }

    private fun renderApps(query: String) {
        if (!::grid.isInitialized) return
        grid.removeAllViews(); updateColumns()
        val filtered = allowedApps.filter { it.label.contains(query.trim(), ignoreCase = true) }
        val columns = grid.columnCount.coerceAtLeast(1)
        filtered.forEachIndexed { index, app ->
            val params = GridLayout.LayoutParams(GridLayout.spec(index / columns), GridLayout.spec(index % columns, 1f)).apply {
                width = 0; height = dp(118); setMargins(dp(5), dp(5), dp(5), dp(5))
            }
            grid.addView(createAppCard(app), params)
        }
        if (filtered.isEmpty()) {
            val empty = TextView(this).apply { text = "Ruxsat berilgan ilova topilmadi"; textSize = 14f; setTextColor(Color.rgb(137, 154, 177)); gravity = Gravity.CENTER }
            grid.addView(empty, GridLayout.LayoutParams().apply { width = -1; height = dp(80); columnSpec = GridLayout.spec(0, columns) })
        }
    }

    private fun createAppCard(app: AppEntry): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = rounded(Color.rgb(13, 27, 45), dp(16).toFloat())
            setPadding(dp(8), dp(8), dp(8), dp(8)); isClickable = true; isFocusable = true
            setOnClickListener {
                val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) startActivity(intent) else Toast.makeText(this@MainActivity, "Ilovani ochib bo‘lmadi", Toast.LENGTH_SHORT).show()
            }
        }
        card.addView(ImageView(this).apply { setImageDrawable(app.icon); scaleType = ImageView.ScaleType.CENTER_INSIDE }, LinearLayout.LayoutParams(dp(52), dp(52)))
        card.addView(TextView(this).apply {
            text = app.label; textSize = 13f; maxLines = 1; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(4), dp(6), dp(4), 0)
        }, LinearLayout.LayoutParams(-1, dp(30)))
        return card
    }

    private fun updateColumns() {
        if (!::grid.isInitialized) return
        val widthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        grid.columnCount = when { widthDp < 500f -> 2; widthDp < 700f -> 3; widthDp < 950f -> 4; widthDp < 1200f -> 5; else -> 6 }
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply { setColor(color); cornerRadius = radius }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private data class AppEntry(val packageName: String, val label: String, val icon: Drawable)
    private class SimpleTextWatcher(private val onChanged: (String) -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged(s?.toString().orEmpty())
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}
