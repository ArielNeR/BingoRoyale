package com.bingoroyale.ui.screens

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bingoroyale.R
import com.bingoroyale.databinding.FragmentPlayerBinding
import com.bingoroyale.model.BingoCard
import com.bingoroyale.model.NetworkEvent
import com.bingoroyale.ui.adapters.BingoCardAdapter
import com.bingoroyale.viewmodel.PlayerViewModel

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var cardAdapter: BingoCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            showExitConfirmation()
        }

        binding.btnConnect.setOnClickListener {
            if (viewModel.gameState.value?.isConnected == true) {
                viewModel.disconnect()
            } else {
                viewModel.searchAndConnect()
                Toast.makeText(requireContext(), getString(R.string.searching), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBingo.setOnClickListener {
            viewModel.callBingo()
            vibrate(longArrayOf(0, 100, 50, 100, 50, 200))
        }

        binding.btnNewCard.setOnClickListener {
            showNewCardConfirmation()
        }

        binding.btnClearMarks.setOnClickListener {
            viewModel.clearAllMarks()
            vibrate()
        }
    }

    private fun setupRecyclerView() {
        cardAdapter = BingoCardAdapter { position ->
            viewModel.toggleCellMark(position)
            vibrate()
        }

        binding.rvBingoCard.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = cardAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewModel.gameState.observe(viewLifecycleOwner) { state ->
            // Actualizar cartón
            cardAdapter.submitCard(state.card, state.markedCells)

            // Estado de conexión
            if (state.isConnected) {
                binding.tvConnectionStatus.text = getString(R.string.connected_to, state.serverName ?: "Cantador")
                binding.tvConnectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent_green)
                )
                binding.btnConnect.text = "🔌 Desconectar"
                binding.cardLastBall.visibility = View.VISIBLE
            } else {
                binding.tvConnectionStatus.text = getString(R.string.offline_mode)
                binding.tvConnectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
                binding.btnConnect.text = "🔗 Conectar"
                binding.cardLastBall.visibility = View.GONE
            }

            // Última bola recibida
            state.lastReceivedBall?.let { ball ->
                val letter = BingoCard.getLetterForNumber(ball)
                binding.tvReceivedBall.text = "$letter$ball"

                val bgRes = when (letter) {
                    "B" -> R.drawable.bg_ball_b
                    "I" -> R.drawable.bg_ball_i
                    "N" -> R.drawable.bg_ball_n
                    "G" -> R.drawable.bg_ball_g
                    "O" -> R.drawable.bg_ball_o
                    else -> R.drawable.bg_ball_empty
                }
                binding.receivedBallBg.setBackgroundResource(bgRes)
            }
        }

        viewModel.networkEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NetworkEvent.BallDrawn -> {
                    // Animar bola recibida
                    val bounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.ball_pop)
                    binding.receivedBallContainer.startAnimation(bounceAnim)
                    vibrate()
                }
                is NetworkEvent.NewGame -> {
                    Toast.makeText(requireContext(), "🔄 Nueva partida iniciada", Toast.LENGTH_SHORT).show()
                    viewModel.clearAllMarks()
                }
                is NetworkEvent.ConnectionLost -> {
                    Toast.makeText(requireContext(), "📴 Conexión perdida: ${event.reason}", Toast.LENGTH_LONG).show()
                }
                null -> { }
            }
            viewModel.clearNetworkEvent()
        }

        viewModel.showBingoAnimation.observe(viewLifecycleOwner) { show ->
            if (show) {
                showBingoDialog()
            }
        }
    }

    private fun showBingoDialog() {
        AlertDialog.Builder(requireContext(), R.style.BingoDialogTheme)
            .setTitle("🎉 ¡BINGO!")
            .setMessage("¡Felicidades! Has cantado BINGO.\n\nMuestra tu cartón al cantador para verificar.")
            .setPositiveButton("¡Genial!") { _, _ ->
                viewModel.dismissBingoAnimation()
            }
            .setCancelable(false)
            .show()
    }

    private fun showNewCardConfirmation() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("🔄 Nuevo Cartón")
            .setMessage("¿Generar un nuevo cartón? Se perderán las marcas actuales.")
            .setPositiveButton("Generar") { _, _ ->
                viewModel.generateNewCard()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("🚪 Salir")
            .setMessage("¿Deseas salir del modo jugador?")
            .setPositiveButton("Salir") { _, _ ->
                viewModel.disconnect()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun vibrate(pattern: LongArray = longArrayOf(0, 30)) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = requireContext().getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Vibrator::class.java)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}