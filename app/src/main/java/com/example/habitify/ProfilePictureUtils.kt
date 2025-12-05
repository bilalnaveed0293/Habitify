package com.example.habitify

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions

object ProfilePictureUtils {

    private val DEFAULT_PROFILE_PIC_RESOURCE = R.drawable.profile

    /**
     * Load profile picture from URL with Glide
     */
    fun loadProfilePicture(
        context: Context,
        imageView: ImageView,
        profilePictureUrl: String?,
        useCircleCrop: Boolean = true
    ) {
        val requestOptions = RequestOptions()
            .placeholder(DEFAULT_PROFILE_PIC_RESOURCE)
            .error(DEFAULT_PROFILE_PIC_RESOURCE)

        if (useCircleCrop) {
            requestOptions.transform(CircleCrop())
        }

        if (!profilePictureUrl.isNullOrEmpty()) {
            // If it's a relative URL, construct the full URL
            val fullUrl = if (profilePictureUrl.startsWith("http")) {
                profilePictureUrl
            } else {
                "${ApiConfig.BASE_IP}/habitify/$profilePictureUrl"
            }

            Glide.with(context)
                .load(fullUrl)
                .apply(requestOptions)
                .into(imageView)
        } else {
            // Load default placeholder
            imageView.setImageResource(DEFAULT_PROFILE_PIC_RESOURCE)
        }
    }

    /**
     * Load profile picture from session
     */
    fun loadProfilePictureFromSession(
        context: Context,
        imageView: ImageView,
        sessionManager: SessionManager,
        useCircleCrop: Boolean = true
    ) {
        val profilePictureUrl = sessionManager.getProfilePictureUrl()
        val profilePicturePath = sessionManager.getProfilePicture()

        val urlToLoad = profilePictureUrl ?: profilePicturePath

        loadProfilePicture(context, imageView, urlToLoad, useCircleCrop)
    }
}