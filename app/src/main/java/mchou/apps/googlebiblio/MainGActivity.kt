package mchou.apps.googlebiblio

import android.animation.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.squareup.picasso.Picasso
import cz.msebera.android.httpclient.Header
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

class MainGActivity : Activity() {
    private lateinit var go: Button
    private lateinit var progress: ProgressBar
    private lateinit var list: ListView
    private lateinit var adapter: BookGAdapter
    private lateinit var edt_search: EditText
    private lateinit var filter_options: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setContent()
        constraintAnimate()
    }

    private fun constraintAnimate() {
        val layout = findViewById<LinearLayout>(R.id.search_layout)
        val animatorSet = AnimatorSet()
        val fadeAnim: ValueAnimator = ObjectAnimator.ofFloat(layout, "alpha", 0f, 1f)
        fadeAnim.duration = 1000
        fadeAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {}
        })
        animatorSet.play(fadeAnim)
        animatorSet.start()
    }

    private fun setContent() {
        list = findViewById(R.id.list)
        val items = ArrayList<BookG>()
        adapter = BookGAdapter(
            this,
            items
        ) //new ArrayAdapter<Book>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter)

        setupBookSelectedListener()
        progress = findViewById(R.id.progress)
        progress.setVisibility(View.GONE)

        go = findViewById(R.id.go)
        go.setOnClickListener(View.OnClickListener {
            CallAsyncLooper()
            val root: ConstraintLayout = findViewById(R.id.root)
            val finishingConstraintSet = ConstraintSet()
            finishingConstraintSet.clone(this@MainGActivity, R.layout.activity_main_final)
            TransitionManager.beginDelayedTransition(root)
            finishingConstraintSet.applyTo(root)
        })
        edt_search = findViewById(R.id.edt_search)
        filter_options = findViewById(R.id.filter_options)
    }

    private fun setupBookSelectedListener() {
        list!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            // Launch the detail view passing book as an extra
            val intent = Intent(this@MainGActivity, BookGDetailActivity::class.java)
            intent.putExtra(BOOK_DETAIL_KEY, adapter!!.getItem(position))
            startActivity(intent)
        }
    }

    private class ViewHolder {
        // View lookup cache
        var ivCover: ImageView? = null
        var tvTitle: TextView? = null
        var tvAuthor: TextView? = null
    }

    internal inner class BookGAdapter(
        context: Context?,
        aBooks: MutableList<BookG>?
    ) : ArrayAdapter<BookG?>(context!!, 0, aBooks!! as List<BookG?>) {
        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            var convertView = convertView
            val book = getItem(position)
            val viewHolder: ViewHolder
            if (convertView == null) {
                viewHolder = ViewHolder()
                val inflater =
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                convertView = inflater.inflate(R.layout.item_book, parent, false)
                viewHolder.ivCover =
                    convertView!!.findViewById<View>(R.id.ivBookCover) as ImageView
                viewHolder.tvTitle =
                    convertView.findViewById<View>(R.id.tvTitle) as TextView
                viewHolder.tvAuthor =
                    convertView.findViewById<View>(R.id.tvAuthor) as TextView
                convertView.tag = viewHolder
            } else {
                viewHolder = convertView.tag as ViewHolder
            }
            viewHolder.tvTitle!!.text = book!!.getTitle()
            viewHolder.tvAuthor!!.text = book.getAuthors()
            Picasso.with(context).load(Uri.parse(book.coverUrl))
                .error(R.drawable.ic_nocover).into(viewHolder.ivCover)
            return convertView!!
        }
    }

    private fun CallAsyncLooper() {
        val mainHandler = Handler(Looper.getMainLooper())
        val myRunnable = Runnable { asyncGoogleFetchItems() }
        mainHandler.post(myRunnable)
    }

    /**
     * Google Api (Books)
     */
    private fun asyncGoogleFetchItems() { //Toast.makeText(MainGActivity.this, "asyncGoogleFetchItems..", Toast.LENGTH_SHORT).show();
        val client = MyHttpGoogleClient()
        client.getItems(edt_search!!.text.toString(), object : JsonHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<Header?>?,
                response: JSONObject?
            ) {
                try { //Toast.makeText(MainGActivity.this, "asyncGoogleFetchItems - onSuccess.."+response, Toast.LENGTH_SHORT).show();
                    progress!!.visibility = ProgressBar.VISIBLE
                    var docs: JSONArray? = null
                    if (response != null) {
                        docs = response.getJSONArray("items")
                        val books = BookG.fromJson(docs)
                        //Toast.makeText(MainGActivity.this, "Success - books : "+books.size(), Toast.LENGTH_SHORT).show();
                        progress!!.max = books.size
                        var i = 1
                        adapter!!.clear()
                        for (book in books) {
                            progress!!.progress = i++
                            /*try {
								Thread.sleep(100);
							} catch (InterruptedException e) {}*/Log.i(
                                "Async",
                                "book : $book"
                            )
                            adapter!!.add(book) // add book through the adapter
                        }
                        adapter!!.notifyDataSetChanged()
                    } else {
                        Toast.makeText(
                            this@MainGActivity,
                            "Success - Response Null !",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                progress!!.visibility = ProgressBar.GONE
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<Header?>?,
                responseString: String?,
                throwable: Throwable?
            ) {
                Toast.makeText(
                    this@MainGActivity,
                    "asyncGoogleFetchItems - onFailure..",
                    Toast.LENGTH_SHORT
                ).show()
                progress!!.visibility = ProgressBar.GONE
            }
        })
    }

    internal inner class MyHttpGoogleClient {
        private val client: AsyncHttpClient = AsyncHttpClient()
        private fun getApiUrl(relativeUrl: String): String {
            return Companion.API_BASE_URL + relativeUrl
        }

        // Method for accessing the search API
        fun getItems(query: String?, handler: JsonHttpResponseHandler?) {
            try {
                val url = getApiUrl("?q=")
                val filter = URLEncoder.encode(query, "utf-8")
                val search_url: String
                search_url = when (filter_options!!.selectedItemPosition) {
                    0 -> url + "inauthor:" + filter
                    1 -> url + "intitle:" + filter
                    else -> url + filter
                }
                Log.i("Async", "search_url : $search_url")
                client.get(search_url, handler)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }


    }

    companion object {
        const val BOOK_DETAIL_KEY = "book"
        const val API_BASE_URL = "https://www.googleapis.com/books/v1/volumes"
    }
}