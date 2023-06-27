package com.example.mp3downloader.downloader

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.mp3downloader.R
import com.example.mp3downloader.databinding.FragmentVideoDownloadBinding
import com.example.mp3downloader.utils.PermissionUtils
import com.example.mp3downloader.viewmodels.DownloadVM

class VideoDownloadFragment : Fragment() {
    private lateinit var viewModel : DownloadVM

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private lateinit var binding : FragmentVideoDownloadBinding
    private lateinit var thumbnailImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var viewsTextView: TextView
    private lateinit var likesTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var downloadTitleTextView: TextView
    private lateinit var downloadProgressTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVideoDownloadBinding.inflate(layoutInflater)

        init()

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        return binding.root
    }

    fun init(){
        viewModel = ViewModelProvider(this)[DownloadVM::class.java]
        // Initialize views
        thumbnailImageView = binding.thumbnailImageView
        titleTextView = binding.titleTextView
        viewsTextView = binding.viewsTextView
        likesTextView = binding.likesTextView
        progressBar = binding.progressBar
        downloadTitleTextView = binding.downloadTitleTextView
        downloadProgressTextView = binding.downloadProgressTextView
        binding.downloadProgressImageView.visibility = View.GONE
        disableBeforeSave()

        setVideoDetails()
        initObservers()
        initListeners()
    }

    private fun disableBeforeSave(){
        binding.infoLayout.visibility = View.GONE
        binding.downloadAnotherButton.visibility = View.GONE
    }

    private fun enableAfterSave(){
        binding.infoLayout.visibility = View.VISIBLE
        binding.downloadAnotherButton.visibility = View.VISIBLE
        binding.infoImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_save_icon))
        binding.infoTextView.text = "MP3 successfully saved into selected folder"
        binding.downloadProgressTextView.visibility = View.GONE
        binding.downloadProgressImageView.visibility = View.VISIBLE
        binding.downloadProgressImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_check))
        binding.downloadProgressImageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
        binding.downloadProgressImageView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
    }

    private fun enableAfterError(){
        binding.infoLayout.visibility = View.VISIBLE
        binding.downloadAnotherButton.visibility = View.VISIBLE
        binding.infoImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_error))
        binding.infoTextView.text = viewModel.failedMessage.value
        binding.downloadTitleTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
        binding.downloadProgressTextView.visibility = View.GONE
        binding.downloadProgressImageView.visibility = View.VISIBLE
        binding.downloadProgressImageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_clear))
        binding.downloadProgressImageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
        binding.downloadProgressImageView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
    }

    private fun setVideoDetails(){
        // Set video details
        val videoTitle = arguments?.getString(ARG_VIDEO_TITLE)
        val videoThumbnailUrl = arguments?.getString(ARG_VIDEO_THUMBNAIL_URL)
        val videoViews = arguments?.getInt(ARG_VIDEO_VIEWS, 0)
        val videoLikes = arguments?.getInt(ARG_VIDEO_LIKES, 0)
        val mp3Url = arguments?.getString(ARG_MP3_URL)
        val destinationFolder = arguments?.getParcelable<Uri>(ARG_DESTINATION_FOLDER)
        viewModel.setFields(videoTitle!!, videoThumbnailUrl!!, videoViews!!, videoLikes!!, mp3Url!!, destinationFolder!!)

        val views = convertNumberToWrittenFormat(videoViews!!.toLong()) + " views"
        val likes = convertNumberToWrittenFormat(videoLikes!!.toLong()) + " likes"

        titleTextView.text = videoTitle
        viewsTextView.text = views
        likesTextView.text = likes
        downloadTitleTextView.text = "Downloading..."

        // Load thumbnail image using a library like Glide or Picasso
        Glide.with(requireContext())
            .load(videoThumbnailUrl)
            .into(thumbnailImageView)

//        if(PermissionUtils.checkStoragePermission(requireContext())){
//            PermissionUtils.storagePermissionGranted.postValue(true)
//        } else {
//            PermissionUtils.requestStoragePermission(requireActivity())
//        }
        viewModel.downloadMedia(requireContext())
    }

    fun convertNumberToWrittenFormat(number: Long): String {
        val units = arrayOf("", "K", "M", "B", "T")
        val suffix = when {
            number < 0 -> "-"
            else -> ""
        }

        var value = Math.abs(number)
        var index = 0

        while (value >= 1000 && index < units.size - 1) {
            value /= 1000
            index++
        }

        return "$suffix$value${units[index]}"
    }

    private fun initObservers(){
//        PermissionUtils.storagePermissionGranted.observe(viewLifecycleOwner) {
//            if (it) {
//                PermissionUtils.afterStoragePermission()
//                viewModel.downloadMedia(requireContext())
//            }
//        }
        viewModel.downloadTotalBytes.observe(viewLifecycleOwner) {
            progressBar.progress = viewModel.downloadProgressPercentage.value!!
            val progress = viewModel.downloadProgressBytes.value.toString()+"/"+viewModel.downloadTotalBytes.value.toString()
            downloadProgressTextView.text = progress
        }
        viewModel.isDownloaded.observe(viewLifecycleOwner) {
            if (it) {
                progressBar.progress = 0
                downloadTitleTextView.text = "Converting..."
                progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.yellow)
                downloadProgressTextView.text = ""
                viewModel.convertFile(requireContext())
            }
        }
        viewModel.convertProgressBytes.observe(viewLifecycleOwner) {
            progressBar.progress = viewModel.convertProgressPercentage.value!!
            downloadProgressTextView.text = it
        }
        viewModel.converted.observe(viewLifecycleOwner) {
            if (it) {
                progressBar.progress = 0
                downloadTitleTextView.text = "Saving..."
                progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.light_blue)
                downloadProgressTextView.text = "0%"
                viewModel.saveFile(requireContext())
            }
        }
        viewModel.saveProcessPercentage.observe(viewLifecycleOwner) {
            progressBar.progress = it
            val progress = "$it%"
            downloadProgressTextView.text = progress
//            Log.i("Save", "Progress: $it%")
        }
        viewModel.processFailed.observe(viewLifecycleOwner) {
            if (it) {
                downloadTitleTextView.text = "Failed"
                progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)
                downloadProgressTextView.text = ""
                progressBar.progress = 100
                enableAfterError()
            }
        }
        viewModel.fileSaved.observe(viewLifecycleOwner) {
            if (it) {
                downloadTitleTextView.text = "Success"
                progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
                downloadProgressTextView.text = ""
                progressBar.progress = 100
                enableAfterSave()
            }
        }
    }

    private fun initListeners(){
        binding.downloadAnotherButton.setOnClickListener {
            val fragmentManager = requireActivity().supportFragmentManager
            viewModel.delete()
            fragmentManager.popBackStack()
        }
    }

    fun onBackPressed(): Boolean {
        val fragmentManager = requireActivity().supportFragmentManager
        viewModel.delete()
        fragmentManager.popBackStack()
        return true
    }

    companion object {
        private const val ARG_VIDEO_TITLE = "arg_video_title"
        private const val ARG_VIDEO_THUMBNAIL_URL = "arg_video_thumbnail_url"
        private const val ARG_VIDEO_VIEWS = "arg_video_views"
        private const val ARG_VIDEO_LIKES = "arg_video_likes"
        private const val ARG_MP3_URL = "arg_mp3_url"
        private const val ARG_DESTINATION_FOLDER = "arg_destination_folder"

        fun newInstance(
            videoTitle: String,
            videoThumbnailUrl: String,
            videoViews: Int,
            videoLikes: Int,
            mp3Url: String,
            destinationFolderUri : Uri
        ): VideoDownloadFragment {
            val fragment = VideoDownloadFragment()
            val args = Bundle().apply {
                putString(ARG_VIDEO_TITLE, videoTitle)
                putString(ARG_VIDEO_THUMBNAIL_URL, videoThumbnailUrl)
                putInt(ARG_VIDEO_VIEWS, videoViews)
                putInt(ARG_VIDEO_LIKES, videoLikes)
                putString(ARG_MP3_URL, mp3Url)
                putParcelable(ARG_DESTINATION_FOLDER, destinationFolderUri)
            }
            fragment.arguments = args
            return fragment
        }
    }
}