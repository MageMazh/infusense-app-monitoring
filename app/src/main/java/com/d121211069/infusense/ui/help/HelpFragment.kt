package com.d121211069.infusense.ui.help

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.d121211069.infusense.adapter.HelpAdapter
import com.d121211069.infusense.data.HelpItem
import com.d121211069.infusense.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {

    private lateinit var helpAdapter: HelpAdapter
    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding as FragmentHelpBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        helpAdapter = HelpAdapter()
        binding.recyclerViewHelp.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewHelp.adapter = helpAdapter

        val faqList = listOf(
            HelpItem(
                "Apa fungsi utama dari aplikasi ini?",
                "Aplikasi ini digunakan untuk memantau volume dan tetesan infus secara real-time dari jarak jauh, agar Anda bisa mengetahui jika infus hampir habis atau terjadi gangguan aliran."
            ),
            HelpItem(
                "Bagaimana cara memulai penggunaan aplikasi?",
                "Pastikan perangkat infus sudah menyala dan terhubung ke Wi-Fi. Setelah itu, buka aplikasi dan masukkan ID perangkat yang tersedia untuk mulai memantau."
            ),
            HelpItem(
                "Apa arti volume infus mencapai batas minimum?",
                "Itu berarti cairan infus hampir habis. Anda akan menerima notifikasi agar segera memberi tahu perawat untuk mengganti infus."
            ),
            HelpItem(
                "Apa yang terjadi jika volume infus mencapai batas kritis?",
                "Aplikasi akan mengaktifkan alarm untuk memberi tahu bahwa infus hampir benar-benar habis dan perlu segera ditangani oleh perawat."
            ),
            HelpItem(
                "Apa arti laju tetesan tidak normal?",
                "Jika tetesan terlalu cepat, terlalu lambat, atau berhenti total, sistem akan memberi peringatan karena ini bisa berbahaya bagi pasien."
            ),
            HelpItem(
                "Apakah saya bisa mengatur sendiri batas volume dan tetesan?",
                "Ya, Anda bisa mengatur batas minimal volume dan laju tetesan sesuai dengan instruksi tenaga medis yang menangani pasien."
            ),
            HelpItem(
                "Apakah aplikasi tetap berfungsi jika tidak ada koneksi internet?",
                "Tidak. Aplikasi membutuhkan koneksi internet agar bisa menerima data real-time dari perangkat infus."
            ),
            HelpItem(
                "Apa yang harus saya lakukan saat menerima alarm atau notifikasi?",
                "Segera hubungi atau beri tahu perawat yang bertugas agar infus dapat diperiksa dan diganti jika perlu."
            ),
            HelpItem(
                "Bisakah saya memantau lebih dari satu pasien?",
                "Versi Android hanya dapat terhubung ke satu perangkat infus (ESP32) dalam satu waktu. Untuk banyak pasien, gunakan dashboard web."
            ),
            HelpItem(
                "Bagaimana jika data tidak muncul di aplikasi?",
                "Pastikan koneksi internet stabil dan perangkat infus menyala serta terhubung ke jaringan Wi-Fi yang sama. Jika masih bermasalah, coba muat ulang aplikasi atau hubungi teknisi."
            )
        )

        helpAdapter.submitList(faqList)
    }
}