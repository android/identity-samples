/*
 * Copyright 2021 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.identity.sample.fido2.ui.home

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.gms.identity.sample.fido2.R

class DeleteConfirmationFragment : DialogFragment() {

    companion object {
        private const val ARG_CREDENTIAL_ID = "credential_id"

        fun newInstance(credentialId: String) = DeleteConfirmationFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CREDENTIAL_ID, credentialId)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val credentialId = arguments?.getString(ARG_CREDENTIAL_ID) ?: throw RuntimeException()
        return AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.delete_confirmation, credentialId))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                (parentFragment as Listener).onDeleteConfirmed(credentialId)
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                dismiss()
            }
            .create()
    }

    interface Listener {
        fun onDeleteConfirmed(credentialId: String)
    }

}
