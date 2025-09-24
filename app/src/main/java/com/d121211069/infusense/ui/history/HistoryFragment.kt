package com.d121211069.infusense.ui.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.d121211069.infusense.adapter.HistoryAdapter
import com.d121211069.infusense.databinding.FragmentHistoryBinding
import com.d121211069.infusense.factory.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding as FragmentHistoryBinding
    private val historyAdapter = HistoryAdapter()

    private val viewModel: HistoryViewModel by viewModels {
        ViewModelFactory.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewHistory.adapter = historyAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.historyList.observe(viewLifecycleOwner) { data ->
                historyAdapter.submitList(data)
            }
        }

        binding.buttonDeleteAll.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle("Konfirmasi Menghapus")
                .setMessage("Apakah Anda yakin ingin menghapus semua riwayat ini?")
                .setPositiveButton("Ya") { dialog, _ ->
                    lifecycleScope.launch {
                        viewModel.deleteAllData()
                    }
                    dialog.dismiss()
                }.setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}