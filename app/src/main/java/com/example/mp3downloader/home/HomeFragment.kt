package com.example.mp3downloader.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.example.mp3downloader.R
import com.example.mp3downloader.databinding.FragmentHomeBinding
import com.example.mp3downloader.downloader.VideoDownloadFragment
import com.example.mp3downloader.viewmodels.MainVM

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: MainVM

    private val PICK_FOLDER_REQUEST_CODE = 123
//    private val videoUrl = "https://www.youtube.com/watch?v=6ZIzFZernGg"

    companion object {
        fun newInstance() = HomeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)

        init()

        return binding.root
    }

    fun init(){
        viewModel = ViewModelProvider(this)[MainVM::class.java]
        viewModel.reset()
        binding.youtubeLinkEditText.text = null
        binding.buttonLoadingIcon.visibility = View.GONE
        initListeners()
        initObservers()
        enableViews()
    }

    private fun initListeners(){
        binding.buttonLayout.setOnClickListener {
            val url = binding.youtubeLinkEditText.text.toString()
            if(url.isEmpty()){
                Toast.makeText(context, "Please enter a valid YouTube link", Toast.LENGTH_SHORT).show()
                enableViews()
                return@setOnClickListener
            }
            if(viewModel.destinationFolderSelected.value == false){
                Toast.makeText(context, "Please select a destination folder", Toast.LENGTH_SHORT).show()
                enableViews()
                return@setOnClickListener
            }
            disableViews()
            viewModel.getVideoContext(url, requireContext())
        }
        binding.destinationFolderButton.setOnClickListener {
            openFolderPicker()
        }
    }

    private fun initObservers(){
        viewModel.infoFetched.observe(viewLifecycleOwner) {
            if (it == true) {
                val title = viewModel.title.value!!.toString()
                val thumbnail = viewModel.thumbnail.value.toString()
                val viewCount = viewModel.viewCount.value.toString().toInt()
                val likeCount = viewModel.likeCount.value.toString().toInt()
                val mp3Url = viewModel.mp3Url.value.toString()
                viewModel.infoFetched.value = false
                goToDownloadFragment(title, thumbnail, viewCount, likeCount, mp3Url)
            }
        }
        viewModel.error.observe(viewLifecycleOwner) {
            if (it == true) {
                Toast.makeText(context, "Failed to grab video info", Toast.LENGTH_SHORT).show()
                viewModel.error.value = false
                enableViews()
            }
        }
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        startActivityForResult(intent, PICK_FOLDER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FOLDER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val folderUri = data?.data
            // Process the selected folder URI
            if (folderUri != null) {
                // Handle the selected folder URI here
                // Example: display the folder path
                val folderPath = folderUri.path
                viewModel.setDestinationFolder(folderUri)
                binding.destinationFolderButton.text = folderPath?.dropWhile { it != ':' }?.drop(1)
            }
        }
    }

    fun disableViews() {
        binding.buttonTextView.text = "Grabbing Info"
        binding.buttonLayout.isClickable = false
        binding.buttonLayout.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.button_inactive_color)
        binding.destinationFolderButton.isEnabled = false
        binding.destinationFolderButton.alpha = 0.5f
        binding.youtubeLinkTextInputLayout.isEnabled = false
        binding.youtubeLinkTextInputLayout.alpha = 0.5f
        binding.buttonLoadingIcon.visibility = View.VISIBLE
    }

    fun enableViews(){
        binding.buttonTextView.text = "Download"
        binding.buttonLayout.isClickable = true
        binding.buttonLayout.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.button_color)
        binding.destinationFolderButton.isEnabled = true
        binding.destinationFolderButton.alpha = 1f
        binding.youtubeLinkTextInputLayout.isEnabled = true
        binding.youtubeLinkTextInputLayout.alpha = 1f
        binding.buttonLoadingIcon.visibility = View.GONE
    }

    private fun goToDownloadFragment(title: String, thumbnail: String, viewCount: Int, likeCount: Int, mp3Url: String){
        val downloadFragment = VideoDownloadFragment.newInstance(title, thumbnail, viewCount, likeCount, mp3Url, viewModel.destinationFolder.value!!)
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        val currentFragment = fragmentManager.findFragmentById(R.id.container)
        fragmentTransaction.replace(R.id.container, downloadFragment)
        fragmentTransaction.addToBackStack(currentFragment?.javaClass?.simpleName)
        fragmentTransaction.commit()
    }

}