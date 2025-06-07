package org.torproject.android.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ustadmobile.meshrabiya.beta.BetaTestLogger
import com.ustadmobile.meshrabiya.beta.LogLevel
import org.torproject.android.R

class BetaConsentActivity : AppCompatActivity() {
    private lateinit var betaLogger: BetaTestLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beta_consent)

        betaLogger = BetaTestLogger.getInstance(this)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.beta_consent_title)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BetaConsentFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class BetaConsentFragment : Fragment() {
    private lateinit var betaLogger: BetaTestLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        betaLogger = BetaTestLogger.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_beta_consent, container, false)

        view.findViewById<TextView>(R.id.tvDescription).text = getString(R.string.beta_consent_description)

        // Log level selection
        val radioGroup = view.findViewById<RadioGroup>(R.id.rgLogLevel)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val level = when (checkedId) {
                R.id.rbBasic -> LogLevel.BASIC
                R.id.rbDetailed -> LogLevel.DETAILED
                R.id.rbFull -> LogLevel.FULL
                else -> LogLevel.BASIC
            }
            betaLogger.setLogLevel(level)
        }

        // Export logs button
        view.findViewById<Button>(R.id.btnExport).apply {
            text = getString(R.string.beta_consent_export)
            setOnClickListener {
                betaLogger.exportLogs()
            }
        }

        // Clear logs button
        view.findViewById<Button>(R.id.btnClear).apply {
            text = getString(R.string.beta_consent_clear)
            setOnClickListener {
                betaLogger.clearLogs()
            }
        }

        return view
    }
} 