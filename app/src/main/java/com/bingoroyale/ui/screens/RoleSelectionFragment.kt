package com.bingoroyale.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bingoroyale.R
import com.bingoroyale.databinding.FragmentRoleSelectionBinding

class RoleSelectionFragment : Fragment() {

    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardCaller.setOnClickListener {
            animateClick(it) {
                findNavController().navigate(R.id.action_role_to_caller)
            }
        }

        binding.cardPlayer.setOnClickListener {
            animateClick(it) {
                findNavController().navigate(R.id.action_role_to_player)
            }
        }
    }

    private fun animateClick(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction { action() }
                    .start()
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}