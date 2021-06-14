/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tv.wiinvent.android.wiinvent_androidtv_sample_kotlin.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import tv.wiinvent.android.wiinvent_androidtv_sample_kotlin.Presenter.CardPresenter
import tv.wiinvent.android.wiinvent_androidtv_sample_kotlin.Presenter.DetailsDescriptionPresenter
import tv.wiinvent.android.wiinvent_androidtv_sample_kotlin.R
import tv.wiinvent.android.wiinvent_androidtv_sample_kotlin.activity.DetailsActivity
import tv.wiinvent.android.wiinvent_androidtv_sample_kotlin.activity.MainActivity
import tv.wiinvent.android.wiinvent_androidtv_sample_kotlin.activity.PlaybackActivity
import tv.wiinvent.android.wiinvent_androidtv_sample_kotlin.model.Movie
import tv.wiinvent.android.wiinvent_androidtv_sample_kotlin.model.MovieList
import java.lang.Integer.parseInt
import java.util.*

/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
class VideoDetailsFragment : DetailsSupportFragment() {

    private var mSelectedMovie: Movie? = null

    private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController
    private lateinit var mPresenterSelector: ClassPresenterSelector
    private lateinit var mAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        mDetailsBackground = DetailsSupportFragmentBackgroundController(this!!)

        mSelectedMovie = requireActivity().intent.getSerializableExtra(DetailsActivity.MOVIE) as Movie
        if (mSelectedMovie != null) {
            mPresenterSelector = ClassPresenterSelector()
            mAdapter = ArrayObjectAdapter(mPresenterSelector)
            setupDetailsOverviewRow()
            setupDetailsOverviewRowPresenter()
            setupRelatedMovieListRow()
            adapter = mAdapter
            initializeBackground(mSelectedMovie)
            onItemViewClickedListener = ItemViewClickedListener()
        } else {
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initializeBackground(movie: Movie?) {
        mDetailsBackground.enableParallax()
        Glide.with(context)
                .load(movie?.backgroundImageUrl)
                .asBitmap()
                .centerCrop()
                .error(R.drawable.default_background)
                .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(
                            bitmap: Bitmap,
                            glideAnimation: GlideAnimation<in Bitmap>
                    ) {
                        mDetailsBackground.coverBitmap = bitmap
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                    }
                })
    }

    private fun setupDetailsOverviewRow() {
        Log.d(TAG, "doInBackground: " + mSelectedMovie?.toString())
        val row = DetailsOverviewRow(mSelectedMovie)
        row.imageDrawable = ContextCompat.getDrawable(requireContext(),
                R.drawable.default_background
        )
        val width = convertDpToPixel(requireContext(),
                DETAIL_THUMB_WIDTH
        )
        val height = convertDpToPixel(requireContext(),
                DETAIL_THUMB_HEIGHT
        )
        Log.e("*** Width", width.toString())
        Log.e("*** height", height.toString())
        Glide.with(context)
                .load(mSelectedMovie?.cardImageUrl?.let { parseInt(it) })
                .centerCrop()
                .error(R.drawable.default_background)
                .into<SimpleTarget<GlideDrawable>>(object : SimpleTarget<GlideDrawable>(width, height) {
                    override fun onResourceReady(
                            resource: GlideDrawable,
                            glideAnimation: GlideAnimation<in GlideDrawable>
                    ) {
                        Log.d(TAG, "details overview card image url ready: " + resource)
                        row.imageDrawable = resource
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                    }
                })

        val actionAdapter = ArrayObjectAdapter()

        actionAdapter.add(
                Action(
                        ACTION_WATCH_TRAILER,
                        resources.getString(R.string.watch_trailer_1),
                        resources.getString(R.string.watch_trailer_2)
                )
        )
        row.actionsAdapter = actionAdapter

        mAdapter.add(row)
    }

    private fun setupDetailsOverviewRowPresenter() {
        // Set detail background.
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter())
        detailsPresenter.backgroundColor =
                ContextCompat.getColor(requireContext(),
                        R.color.selected_background
                )

        // Hook up transition element.
        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
        sharedElementHelper.setSharedElementEnterTransition(
                activity, DetailsActivity.SHARED_ELEMENT_NAME
        )
        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = true

        detailsPresenter.onActionClickedListener = OnActionClickedListener { action ->
            if (action.id == ACTION_WATCH_TRAILER) {
                val intent = Intent(context, PlaybackActivity::class.java)
                intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie)
                startActivity(intent)
            } else {
                Toast.makeText(context, action.toString(), Toast.LENGTH_SHORT).show()
            }
        }
        mPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    }

    private fun setupRelatedMovieListRow() {
        val subcategories = arrayOf(getString(R.string.related_movies))
        val list =
                MovieList.list

        Collections.shuffle(list)
        val listRowAdapter = ArrayObjectAdapter(CardPresenter())
        for (j in 0 until NUM_COLS) {
            listRowAdapter.add(list[j % 2])
        }

        val header = HeaderItem(0, subcategories[0])
        mAdapter.add(ListRow(header, listRowAdapter))
        mPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
    }

    private fun convertDpToPixel(context: Context, dp: Int): Int {
        val density = context.applicationContext.resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
                itemViewHolder: Presenter.ViewHolder?,
                item: Any?,
                rowViewHolder: RowPresenter.ViewHolder,
                row: Row
        ) {
            if (item is Movie) {
                Log.d(TAG, "Item: " + item.toString())
                val intent = Intent(context, DetailsActivity::class.java)
                intent.putExtra(resources.getString(R.string.movie), mSelectedMovie)

                val bundle =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                activity!!,
                                (itemViewHolder?.view as ImageCardView).mainImageView,
                                DetailsActivity.SHARED_ELEMENT_NAME
                        )
                                .toBundle()
                activity!!.startActivity(intent, bundle)
            }
        }
    }

    companion object {
        private val TAG = "VideoDetailsFragment"

        private val ACTION_WATCH_TRAILER = 1L
        private val ACTION_RENT = 2L
        private val ACTION_BUY = 3L

        private val DETAIL_THUMB_WIDTH = 274 * 155 / 100
        private val DETAIL_THUMB_HEIGHT = 274

        private val NUM_COLS = 10
    }
}