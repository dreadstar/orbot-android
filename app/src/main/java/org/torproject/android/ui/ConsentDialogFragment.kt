package org.torproject.android.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import org.torproject.android.R

class ConsentDialogFragment(
    private val onConsentSelected: (Boolean, LoggingLevel) -> Unit
) : DialogFragment() {
    private var selectedLevel: LoggingLevel = LoggingLevel.BASIC

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_beta_consent, container, false)
        val radioGroup = view.findViewById<RadioGroup>(R.id.rgLogLevelDialog)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedLevel = when (checkedId) {
                R.id.rbBasicDialog -> LoggingLevel.BASIC
                R.id.rbDetailedDialog -> LoggingLevel.DETAILED
                R.id.rbFullDialog -> LoggingLevel.FULL
                else -> LoggingLevel.BASIC
            }
        }
        view.findViewById<MaterialButton>(R.id.btnEnableLogging).setOnClickListener {
            onConsentSelected(true, selectedLevel)
            dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btnNotNow).setOnClickListener {
            dismiss()
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }
}
