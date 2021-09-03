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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.identity.sample.fido2.api.Credential
import com.google.android.gms.identity.sample.fido2.databinding.CredentialItemBinding

class CredentialAdapter(
    private val onDeleteClicked: (String) -> Unit
) : ListAdapter<Credential, CredentialViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialViewHolder {
        return CredentialViewHolder(
            CredentialItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onDeleteClicked
        )
    }

    override fun onBindViewHolder(holder: CredentialViewHolder, position: Int) {
        holder.binding.credential = getItem(position)
    }
}

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Credential>() {
    override fun areItemsTheSame(oldItem: Credential, newItem: Credential): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Credential, newItem: Credential): Boolean {
        return oldItem == newItem
    }
}

class CredentialViewHolder(
    val binding: CredentialItemBinding,
    onDeleteClicked: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.delete.setOnClickListener {
            binding.credential?.let { c ->
                onDeleteClicked(c.id)
            }
        }
    }
}
